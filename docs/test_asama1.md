# Aşama 1 Test Kaydı – Bildirim Dinleyici (MessageNotificationListener)

**Cihaz:** Android Studio emülatörü (Pixel)  **Android sürümü:** API 36.1 (Android 16)  **Test tarihi:** 13.11.2026  **Testi yapan:** Danışman öğretmen
**Uygulama sürümü:** 0.1.1-asama1

Sonuçlar uygulamanın ana ekranındaki **"Yakalanan bildirimler"** listesinde görünür (bilgisayar gerekmez). İsteğe bağlı: Logcat filtresi `tag:DK-Notif`.

> Etik kural: Testlerde yalnızca ekip içi, önceden hazırlanmış **sentetik** mesajlar kullanılır. Gerçek kişilerin mesajları kullanılmaz ve kaydedilmez.

| # | Senaryo | Beklenen | Gerçekleşen (Logcat satırı) | Sonuç (✅/❌) |
|---|---|---|---|---|
| T1 | İzin kapalıyken uygulama açılır | Kartta "KAPALI ❌" | (gözlenmedi, doğrudan izin verildi) | – |
| T2 | Bildirim erişimi verilir | `Servis bağlandı` | Kartta "AÇIK ✅" | ✅ |
| T3 | Emülatöre SMS (`adb emu sms send 5551234 "Merhaba bu bir deneme mesajidir"`) | paket, başlık/metin doğru | `com.google.android.apps.messaging`, Başlık: 555-1234, Metin doğru | ✅ |
| T4 | Başka uygulamanın bildirimi (Saat → Zamanlayıcı) | Paket adı tespit edilir | `com.google.android.deskclock`, metin boş (uygulama standart metin alanı kullanmıyor) | ✅ (metin: sınır) |
| T5 | "Kendi test bildirimimi gönder" | `Kendi bildirimimiz, analiz dışı bırakıldı.` | "Kendi bildirimimiz – analiz dışı" | ✅ |
| T6 | Aynı sohbetten art arda 3 mesaj | Grup özeti atlanır, mesajlar ayrı loglanır | | |
| T7 | Türkçe karakterli mesaj ("şğüıöç İ") | Karakterler bozulmadan görünür | `Türkçe test: şğüıöç İ` doğru okundu | ✅ |
| T8 | Mesaj uygulamasında bildirim içeriği gizli/kapalı | Metin boş veya "Yeni mesaj" → **sınır olarak belgelenir** | | |
| T9 | Ayarlardan izin geri alınır | `Servis bağlantısı kesildi` | | |

**Başarı kriteri:** T2, T3, T4, T5 başarılı.

## Gözlemler / Sorunlar
- **Sonuç:** Aşama 1 başarı kriteri (T2, T3, T4, T5) emülatörde sağlandı.
- **Teknik sınır:** Saat uygulamasının zamanlayıcı bildirimi standart metin alanı (EXTRA_TEXT) kullanmadığı için içerik okunamadı.
- **Kurulum sınırı:** Gerçek telefonda Google Play Protect, dışarıdan yüklenen APK'yı engelledi; emülatörde APK sürükle-bırak ile kuruldu.
- **Bekleyen:** T6, T8, T9 ve gerçek telefonda WhatsApp/Telegram testi.
- Kanıtlar: `ekran-goruntuleri/test_emulator_1_saat.png`, `test_emulator_2_sms.png`, `test_emulator_3_turkce.png`
