import json, urllib.request, urllib.error, uuid
URL="https://nreqibswrqqhlsqweeue.supabase.co"; KEY="sb_publishable_499WDodoNQreJHL1vUS8rA_92FXxv85"
def req(method, path, body=None, token=None, prefer=None):
    h={"apikey":KEY,"Content-Type":"application/json"}
    if token: h["Authorization"]="Bearer "+token
    if prefer: h["Prefer"]=prefer
    r=urllib.request.Request(URL+path, data=None if body is None else json.dumps(body).encode(), headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=30) as resp:
            t=resp.read().decode(); return resp.status, (json.loads(t) if t else None)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()
ok=lambda c,msg: print(("✔ " if c else "✘ ")+msg)
mail=f"dk-e2e-{uuid.uuid4().hex[:8]}@dijitalkalkan-test.com"
s,d=req("POST","/auth/v1/signup",{"email":mail,"password":"Test12345!"}); ok(s==200 and d.get("access_token"), f"1) veli kaydı ({s})")
veli=d["access_token"]; veli_id=d["user"]["id"]
s,_=req("POST","/rest/v1/profiles",{"id":veli_id,"role":"parent"},veli); ok(s==201, f"2) veli profili ({s})")
s,kod=req("POST","/rest/v1/rpc/create_pairing_code",{},veli); ok(s==200 and len(str(kod))==6, f"3) kod üretildi: {kod}")
s,d=req("POST","/auth/v1/signup",{}); ok(s==200 and d.get("user",{}).get("is_anonymous"), f"4) çocuk anonim giriş ({s})")
cocuk=d["access_token"]; cocuk_id=d["user"]["id"]
s,e=req("POST","/rest/v1/rpc/redeem_pairing_code",{"p_code":"000000","p_child_label":"x"},cocuk); ok(s>=400 and "geçersiz" in e, "5) yanlış kod reddedildi")
s,_=req("POST","/rest/v1/rpc/redeem_pairing_code",{"p_code":kod,"p_child_label":"Ali (test)"},cocuk); ok(s==200, f"6) çocuk bağlandı ({s})")
s,d=req("POST","/rest/v1/alerts",{"child_id":cocuk_id,"category":"TEHDIT","level":"YUKSEK","source_app":"com.whatsapp","method":"ANAHTAR_KELIME"},cocuk); ok(s==201, f"7) çocuk uyarı gönderdi ({s})")
s,d=req("GET","/rest/v1/alerts?select=*",None,veli); ok(s==200 and len(d)==1 and d[0]["category"]=="TEHDIT", f"8) veli uyarıyı görüyor: {d[0]['category'] if d else '-'} / {d[0]['source_app'] if d else '-'}")
ok(all(k in ("id","child_id","parent_id","category","level","source_app","method","detected_at","read_at","created_at") for k in d[0]), "9) uyarıda mesaj metni alanı YOK")
s,d=req("GET","/rest/v1/family_links?select=*",None,veli); ok(s==200 and d and d[0]["child_label"]=="Ali (test)", "10) veli bağlı cihazı görüyor")
s,d=req("GET","/rest/v1/alerts?select=*",None,cocuk); ok(s==200 and len(d)==1, "11) çocuk gönderilen uyarıyı görüyor (şeffaflık)")
s,d=req("POST","/auth/v1/signup",{}); yab=d["access_token"]
s,d=req("GET","/rest/v1/alerts?select=*",None,yab); ok(s==200 and d==[], "12) yabancı hiçbir uyarı göremiyor")
s,d=req("PATCH",f"/rest/v1/alerts?id=gt.0",{"read_at":"2026-11-16T12:00:00Z"},veli,"return=representation"); ok(s==200 and d and d[0]["read_at"], "13) veli okundu işaretledi")
print("TEST_KULLANICILARI", veli_id, cocuk_id)
