// Dijital Kalkan – "uyari-bildirimi" Edge Function (sürüm 3)
//
// Görevleri (hepsi Firebase Cloud Messaging ile, VERİ SAKLAMADAN taşır):
//   action "uyari"       : Çocuk telefonu → veli telefonu
//                          "Tehdit kategorisinde uygunsuz mesaj tespit edildi" bildirimi.
//                          İçerik: kategori, seviye, uygulama paket adı, çocuğun bağlantı adı. MESAJ METNİ YOK.
//   action "kelime_ekle" : Veli telefonu → çocuk telefonu (veli lakap/özel ifade ekler)
//   action "kelime_sil"  : Veli telefonu → çocuk telefonu (veli eklediği ifadeyi kaldırır)
//   self_test            : Firebase anahtarı doğru mu? (hiçbir gizli değer döndürmez)
//
// Kelimeler veritabanına YAZILMAZ; yalnızca çocuğun telefonuna iletilir ve orada saklanır.
// Gerekli gizli değer (Supabase → Edge Functions → Secrets): FIREBASE_SERVICE_ACCOUNT
// Çağrı: Uygulama, oturum anahtarı (JWT) ile çağırır; verify_jwt açık.
import "jsr:@supabase/functions-js/edge-runtime.d.ts";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const SA_RAW = Deno.env.get("FIREBASE_SERVICE_ACCOUNT");

const CATEGORIES = ["HAKARET", "KUFUR", "SIBER_ZORBALIK", "TEHDIT", "UYGUNSUZ_YAKLASIM", "KENDINE_ZARAR"];
const ALERT_WINDOW_MS = 3 * 60 * 1000;      // yalnızca son 3 dakikadaki uyarılar bildirilir
const notifiedAlerts = new Set<number>();    // aynı uyarının tekrar bildirimini önler (en iyi çaba)

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

/** İmzası Supabase ağ geçidinde doğrulanmış oturum anahtarından kullanıcı kimliğini okur. */
function callerId(req: Request): string | null {
  const auth = req.headers.get("Authorization") ?? "";
  const token = auth.replace(/^Bearer\s+/i, "");
  const part = token.split(".")[1];
  if (!part) return null;
  try {
    const payload = JSON.parse(atob(part.replace(/-/g, "+").replace(/_/g, "/")));
    return typeof payload.sub === "string" ? payload.sub : null;
  } catch {
    return null;
  }
}

let cached: { token: string; exp: number } | null = null;

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

function serviceAccount(): ServiceAccount | string {
  if (!SA_RAW) return "FIREBASE_SERVICE_ACCOUNT tanımlı değil";
  try {
    const sa = JSON.parse(SA_RAW);
    if (!sa.private_key || !sa.client_email || !sa.project_id) {
      return "FIREBASE_SERVICE_ACCOUNT bir Firebase hizmet hesabı anahtarı değil " +
        "(Firebase → Project settings → Service accounts → Generate new private key)";
    }
    return sa;
  } catch {
    return "FIREBASE_SERVICE_ACCOUNT geçerli JSON değil";
  }
}

/** Bir kullanıcının tüm cihazlarına veri mesajı gönderir; geçersiz adresleri temizler. */
async function sendToUser(userId: string, data: Record<string, string>): Promise<{ sent: number; reason?: string }> {
  const sa = serviceAccount();
  if (typeof sa === "string") return { sent: 0, reason: sa };
  const tokens: { fcm_token: string }[] = await rest(`push_tokens?user_id=eq.${userId}&select=fcm_token`);
  if (!tokens?.length) return { sent: 0, reason: "alıcı cihazın bildirim adresi yok" };
  const access = await googleAccessToken(sa);
  let sent = 0;
  for (const t of tokens) {
    const res = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${access}`, "Content-Type": "application/json" },
      body: JSON.stringify({ message: { token: t.fcm_token, data, android: { priority: "HIGH" } } }),
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
  return sent > 0 ? { sent } : { sent, reason: "Firebase gönderimi başarısız" };
}

async function handleAlert(uid: string, alertId: number) {
  if (notifiedAlerts.has(alertId)) return json({ sent: 0, reason: "zaten bildirildi" });
  const rows = await rest(`alerts?id=eq.${alertId}&select=id,child_id,parent_id,category,level,source_app,created_at`);
  const a = rows?.[0];
  if (!a || a.child_id !== uid) return json({ error: "Bu uyarı size ait değil" }, 403);
  if (Date.now() - Date.parse(a.created_at) > ALERT_WINDOW_MS) return json({ sent: 0, reason: "uyarı çok eski" });
  notifiedAlerts.add(alertId);
  const links = await rest(`family_links?child_id=eq.${a.child_id}&select=child_label`);
  const childLabel: string = links?.[0]?.child_label ?? "Çocuk";
  const result = await sendToUser(a.parent_id, {
    type: "uyari",
    category: a.category,
    level: a.level,
    source_app: a.source_app,
    child_label: childLabel,
    detected_at: a.created_at,
  });
  return json(result);
}

async function handleWord(uid: string, body: Record<string, unknown>, remove: boolean) {
  const childId = String(body.child_id ?? "");
  const phrase = String(body.phrase ?? "").trim().replace(/\s+/g, " ");
  const category = String(body.category ?? "");
  const suffixes = body.suffixes === true ? "1" : "0";
  if (!/^[0-9a-f-]{36}$/i.test(childId)) return json({ error: "child_id geçersiz" }, 400);
  if (phrase.length < 2 || phrase.length > 60 || /[\[\]#*!|\n\r]/.test(phrase)) {
    return json({ error: "İfade 2-60 karakter olmalı ve [ ] # * ! | içermemeli" }, 400);
  }
  if (!CATEGORIES.includes(category)) return json({ error: "Kategori geçersiz" }, 400);
  const links = await rest(`family_links?child_id=eq.${childId}&parent_id=eq.${uid}&select=id`);
  if (!links?.length) return json({ error: "Bu cihaz size bağlı değil" }, 403);
  const result = await sendToUser(childId, {
    type: remove ? "kelime_sil" : "kelime_ekle",
    phrase,
    category,
    suffixes,
  });
  return json(result);
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "Yalnızca POST" }, 405);
  let body: Record<string, unknown>;
  try {
    body = await req.json();
  } catch {
    return json({ error: "Geçersiz gövde" }, 400);
  }

  if (body.self_test === true) {
    const sa = serviceAccount();
    if (typeof sa === "string") return json({ google_auth: "hata", reason: sa });
    try {
      await googleAccessToken(sa);
      return json({ google_auth: "ok", firebase_project: sa.project_id });
    } catch (e) {
      return json({ google_auth: "hata", reason: String(e).slice(0, 300) });
    }
  }

  const uid = callerId(req);
  if (!uid) return json({ error: "Oturum gerekli" }, 401);

  try {
    switch (body.action) {
      case "uyari": {
        const alertId = Number(body.alert_id);
        if (!Number.isInteger(alertId)) return json({ error: "alert_id gerekli" }, 400);
        return await handleAlert(uid, alertId);
      }
      case "kelime_ekle":
        return await handleWord(uid, body, false);
      case "kelime_sil":
        return await handleWord(uid, body, true);
      default:
        return json({ error: "Bilinmeyen işlem" }, 400);
    }
  } catch (e) {
    console.error(e);
    return json({ error: String(e).slice(0, 300) }, 500);
  }
});
