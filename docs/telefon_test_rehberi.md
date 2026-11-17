# Telefonda Deneme Rehberi – Dijital Kalkan 1.0.0

İki telefon gerekir: **veli telefonu** ve **çocuk telefonu**. (Biri emülatör olabilir; veli
emülatörü "Google Play" simgeli olmalı.) **Aynı APK** iki telefona da kurulur.

## 0. Ön koşul (bir kez): Firebase anahtarı
Veliye anlık bildirim ve veli→çocuk kelime gönderimi için Supabase'de
`FIREBASE_SERVICE_ACCOUNT` gizli değeri **hizmet hesabı anahtarı** olmalıdır
(Firebase → ⚙️ Project settings → **Service accounts** → Generate new private key).
`google-services.json` değil! Bu yapılmazsa eşleştirme ve uyarı listesi çalışır, anlık bildirim gelmez.

## 1. APK'yı kurma
1. APK'yı telefona gönderin (USB, e-posta, Drive) ve açın.
2. "Bilinmeyen kaynaklardan yükleme" izni isterse verin.
3. **Play Protect engellerse:** "Ayrıntılar → Yine de yükle". Seçenek yoksa: Play Store → profil →
   Play Protect → ⚙️ → "Uygulamaları tara" geçici kapat → kur → **tekrar aç**.
4. Hiçbiri olmazsa: bilgisayardan USB ile `adb install -r DijitalKalkan-1.0.0.apk`

## 2. Veli telefonu
1. Uygulamayı açın → **Veli telefonu**.
2. E-posta ve şifre (en az 6 karakter) → **Kayıt ol**. Bildirim izni sorulursa **İzin ver**.
3. **Çocuk cihazı ekle** → ekranda 6 haneli kod çıkar (10 dakika geçerli).

## 3. Çocuk telefonu
1. Uygulamayı açın → **Çocuk telefonu**.
2. **Tespitler** sekmesi → **Bildirim erişimi ver** → Dijital Kalkan'ı açın.
   (Anahtar griyse: Ayarlar → Uygulamalar → Dijital Kalkan → ⋮ → **Kısıtlanmış ayarlara izin ver**.)
3. **Veli** sekmesi → adınızı ve velinin ekranındaki kodu yazın → onay kutusu → **Bağlan**.
4. "Velinize bağlısınız ✅" görünmeli. Veli ekranında da birkaç saniye içinde cihaz görünür.

## 4. Testler (sentetik mesajlarla)
| # | Ne yapılır | Beklenen |
|---|---|---|
| T1 | Başka bir telefondan çocuk telefonuna SMS/WhatsApp: "seni okul çıkışında bekleyeceğim" | Çocuk: Tespitler'de **WhatsApp · Tehdit · Velinize bildirildi** (mesaj metni görünmez) |
| T2 | Veli telefonunu kilitli bekletin | Bildirim: **"⚠️ Uygunsuz mesaj tespit edildi – Ali · WhatsApp: Tehdit kategorisinde uygunsuz mesaj tespit edildi (yüksek risk)."** |
| T3 | Veli uygulamasını açın | Uyarı kartı + "Son 7 gün" özeti |
| T4 | Veli: Kelime listesi → "koca kafa", Hakaret → **Çocuğun telefonuna ekle** | "✅ Çocuğunuzun telefonuna iletildi." Çocuk telefonunda bilgi bildirimi |
| T5 | Çocuk: Kelimelerim | "Velinizin eklediği ifadeler" altında **koca kafa** |
| T6 | Çocuk telefonuna "bakın koca kafa geldi" mesajı | Hakaret tespiti + veliye bildirim |
| T7 | Normal mesaj: "yarın matematik ödevini getir" | Normal, veliye bildirim **gitmez** |
| T8 | Çocuk: Veli → **Veli bağlantısını kaldır** | Veli ekranından cihaz kaybolur, yeni uyarı gitmez |

Sonuçları tabloya (✔/✘) ve ekran görüntüleriyle `docs/` klasörüne kaydedin.

## Sık sorunlar
- **Bildirim gelmiyor:** Firebase anahtarı (madde 0); veli telefonunda bildirim izni; pil tasarrufu.
  Xiaomi/Huawei/Oppo: Ayarlar → Pil → Dijital Kalkan → "Kısıtlama yok".
- **"Çocuğun telefonu henüz hazır değil":** Çocuk telefonunda uygulamayı açıp Veli sekmesine girin.
- **Tespit yok:** Mesaj uygulamasında bildirim içeriği gizliyse metin okunamaz (bilinen sınır).
