"""
"uyari-bildirimi" fonksiyonunun yetki testleri (gerçek Supabase).
Not: Test sonunda 3 test hesabı oluşur (e-postası dk-e2e-... olan + 2 anonim).
Supabase → Authentication → Users ekranından silinebilir.
"""
import json, urllib.request, urllib.error, uuid
URL = "https://nreqibswrqqhlsqweeue.supabase.co"; KEY = "sb_publishable_499WDodoNQreJHL1vUS8rA_92FXxv85"
FN = URL + "/functions/v1/uyari-bildirimi"

def req(method, url, body=None, token=None):
    h = {"apikey": KEY, "Content-Type": "application/json", "Prefer": "return=representation"}
    if token: h["Authorization"] = "Bearer " + token
    r = urllib.request.Request(url, data=None if body is None else json.dumps(body).encode(), headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=40) as resp:
            t = resp.read().decode(); return resp.status, (json.loads(t) if t else None)
    except urllib.error.HTTPError as e:
        t = e.read().decode()
        try: return e.code, json.loads(t)
        except Exception: return e.code, t

ok = lambda c, m: print(("✔ " if c else "✘ ") + m)
mail = f"dk-e2e-{uuid.uuid4().hex[:8]}@dijitalkalkan-test.com"
s, d = req("POST", URL + "/auth/v1/signup", {"email": mail, "password": "Test12345!"}); veli, veli_id = d["access_token"], d["user"]["id"]
req("POST", URL + "/rest/v1/profiles", {"id": veli_id, "role": "parent"}, veli)
s, kod = req("POST", URL + "/rest/v1/rpc/create_pairing_code", {}, veli)
s, d = req("POST", URL + "/auth/v1/signup", {}); cocuk, cocuk_id = d["access_token"], d["user"]["id"]
req("POST", URL + "/rest/v1/rpc/redeem_pairing_code", {"p_code": kod, "p_child_label": "Test Çocuk"}, cocuk)
s, d = req("POST", URL + "/auth/v1/signup", {}); yabanci = d["access_token"]

s, d = req("POST", URL + "/rest/v1/alerts?select=id", {"child_id": cocuk_id, "category": "TEHDIT", "level": "YUKSEK", "source_app": "com.whatsapp"}, cocuk)
alert_id = d[0]["id"]; ok(s == 201, f"1) çocuk uyarı kaydetti (id={alert_id})")
s, d = req("POST", FN, {"action": "uyari", "alert_id": alert_id}, yabanci); ok(s == 403, f"2) yabancı başkasının uyarısını bildiremez ({s}: {d})")
s, d = req("POST", FN, {"action": "uyari", "alert_id": alert_id}, cocuk); ok(s == 200, f"3) çocuk kendi uyarısını bildirdi → {d}")
s, d = req("POST", FN, {"action": "kelime_ekle", "child_id": cocuk_id, "phrase": "koca kafa", "category": "HAKARET", "suffixes": True}, yabanci)
ok(s == 403, f"4) yabancı çocuğa kelime gönderemez ({s}: {d})")
s, d = req("POST", FN, {"action": "kelime_ekle", "child_id": cocuk_id, "phrase": "koca kafa", "category": "HAKARET", "suffixes": True}, veli)
ok(s == 200, f"5) veli kendi çocuğuna kelime gönderdi → {d}")
s, d = req("POST", FN, {"action": "kelime_ekle", "child_id": cocuk_id, "phrase": "x]", "category": "HAKARET"}, veli)
ok(s == 400, f"6) geçersiz ifade reddedildi ({s})")
s, d = req("POST", FN, {"action": "kelime_ekle", "child_id": cocuk_id, "phrase": "koca kafa", "category": "NORMAL"}, veli)
ok(s == 400, f"7) geçersiz kategori reddedildi ({s})")
s, d = req("GET", URL + "/rest/v1/alerts?select=*", None, veli)
ok(s == 200 and all("text" not in k and "message" not in k for k in d[0]), "8) veritabanında mesaj metni yok")
