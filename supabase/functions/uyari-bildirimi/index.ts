// Dijital Kalkan – "uyari-bildirimi" Edge Function
// Çocuk cihazından gelen uyarıyı Firebase Cloud Messaging ile veli telefonuna bildirir.
// Bildirimde YALNIZCA: kategori, seviye, uygulama paket adı, çocuğun bağlantı adı. Mesaj metni yoktur.
//
// Gerekli gizli değer (Supabase → Edge Functions → Secrets): FIREBASE_SERVICE_ACCOUNT
// Çağıran: veritabanındaki "alerts_notify_parent" tetikleyicisi (her yeni uyarıda).
// Güvenlik: Her uyarı en fazla BİR kez ve yalnızca son 10 dakika içinde oluşturulmuşsa bildirilir.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const SA_RAW = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");

type ServiceAccount = { project_id: string; client_email: string; private_key: string };

const json = (o: unknown, status = 200) =>
  new Response(JSON.stringify(o), { status, headers: { "Content-Type": "application/json" } });

async function rest(path: string, init: RequestInit = {}) {
  const res = await fetch(`${SUPABASE_URL}/rest/v1/${path}`, {
    ...init,
    headers: {
      apikey: SERVICE_KEY,
      Authorization: `Bearer ${SERVICE_KEY}`,
      "Content-Type": "application/json",
      Prefer: "return=representation",
      ...(init.headers ?? {}),
    },
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`REST ${res.status}: ${text}`);
  return text ? JSON.parse(text) : null;
}

function b64url(data: ArrayBuffer | string): string {
  const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

let cached: { token: string; exp: number } | null = null;

/** Hizmet hesabı anahtarıyla Google'dan kısa ömürlü erişim izni alır. */
async function googleAccessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  if (cached && cached.exp > now + 60) return cached.token;
  const header = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({
    iss: sa.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const pem = sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey(
    "pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claim}`));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: `${header}.${claim}.${b64url(sig)}`,
    }),
  });
  const body = await res.json();
  if (!res.ok) throw new Error("Google erişim izni alınamadı: " + JSON.stringify(body));
  cached = { token: body.access_token, exp: now + (body.expires_in ?? 3600) };
  return cached.token;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "Yalnızca POST" }, 405);
  let payload: { alert_id?: unknown; self_test?: unknown };
  try {
    payload = await req.json();
  } catch {
    return json({ error: "Geçersiz gövde" }, 400);
  }
  if (!SA_RAW) return json({ sent: 0, reason: "FIREBASE_SERVICE_ACCOUNT tanımlı değil" });

  let sa: ServiceAccount;
  try {
    sa = JSON.parse(SA_RAW);
  } catch {
    return json({ sent: 0, reason: "FIREBASE_SERVICE_ACCOUNT geçerli JSON değil" });
  }

  // Kurulum testi: anahtar çalışıyor mu? (gizli bilgi döndürmez)
  if (payload.self_test === true) {
    // Yalnızca alan ADLARI ve dosya türü gösterilir; hiçbir değer (anahtar) gösterilmez.
    const fields = Object.keys(sa as Record<string, unknown>);
    const type = (sa as Record<string, unknown>).type ?? null;
    if (!sa.private_key || !sa.client_email) {
      return json({
        google_auth: "hata",
        reason: "Bu bir Firebase hizmet hesabı anahtarı değil (private_key/client_email yok). " +
          "Firebase → Project settings → Service accounts → Generate new private key dosyası yapıştırılmalı.",
        bulunan_alanlar: fields,
        dosya_turu: type,
      });
    }
    try {
      await googleAccessToken(sa);
      return json({ google_auth: "ok", firebase_project: sa.project_id });
    } catch (e) {
      return json({ google_auth: "hata", detail: String(e).slice(0, 300) });
    }
  }

  const alertId = Number(payload.alert_id);
  if (!Number.isInteger(alertId)) return json({ error: "alert_id gerekli" }, 400);

  try {
    // Uyarıyı yalnızca bir kez "bildirildi" olarak işaretle (son 10 dk içindeki uyarılar)
    const since = new Date(Date.now() - 10 * 60 * 1000).toISOString();
    const claimed = await rest(
      `alerts?id=eq.${alertId}&notified_at=is.null&created_at=gte.${encodeURIComponent(since)}` +
        `&select=id,child_id,parent_id,category,level,source_app`,
      { method: "PATCH", body: JSON.stringify({ notified_at: new Date().toISOString() }) },
    );
    if (!claimed?.length) return json({ sent: 0, reason: "uyarı yok veya zaten bildirildi" });
    const a = claimed[0];

    const tokens: { fcm_token: string }[] = await rest(`push_tokens?user_id=eq.${a.parent_id}&select=fcm_token`);
    if (!tokens?.length) return json({ sent: 0, reason: "velinin bildirim adresi yok" });

    const links = await rest(`family_links?child_id=eq.${a.child_id}&select=child_label`);
    const childLabel: string = links?.[0]?.child_label ?? "Çocuk";

    const access = await googleAccessToken(sa);
    let sent = 0;
    for (const t of tokens) {
      const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
        method: "POST",
        headers: { Authorization: `Bearer ${access}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          message: {
            token: t.fcm_token,
            data: { category: a.category, level: a.level, source_app: a.source_app, child_label: childLabel },
            android: { priority: "HIGH" },
          },
        }),
      });
      if (res.ok) {
        sent++;
      } else {
        const err = await res.text();
        console.error("FCM hatası", res.status, err.slice(0, 300));
        if (res.status === 404 || err.includes("UNREGISTERED")) {
          await rest(`push_tokens?fcm_token=eq.${encodeURIComponent(t.fcm_token)}`, { method: "DELETE" });
        }
      }
    }
    return json({ sent });
  } catch (e) {
    console.error(e);
    return json({ error: String(e).slice(0, 300) }, 500);
  }
});
