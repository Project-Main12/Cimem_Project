# Aşama 1 Test Kaydı – Bildirim Dinleyici (MessageNotificationListener)

**Cihaz:** ……………… (marka/model)  **Android sürümü:** ……  **Test tarihi:** ……  **Testi yapan:** ……
**Uygulama sürümü:** 0.1.0-asama1

Logcat filtresi: `tag:DK-Notif` (veya `adb logcat -s DK-Notif`)

> Etik kural: Testlerde yalnızca ekip içi, önceden hazırlanmış **sentetik** mesajlar kullanılır. Gerçek kişilerin mesajları kullanılmaz ve kaydedilmez.

| # | Senaryo | Beklenen | Gerçekleşen (Logcat satırı) | Sonuç (✅/❌) |
|---|---|---|---|---|
| T1 | İzin kapalıyken uygulama açılır | Kartta "KAPALI ❌" | | |
| T2 | Bildirim erişimi verilir | `Servis bağlandı` | | |
| T3 | `adb shell cmd notification post -S bigtext -t "Test Baslik" dk_test "Merhaba bu bir deneme mesajidir"` | `paket=com.android.shell`, başlık/metin doğru | | |
| T4 | Başka ekip telefonundan WhatsApp/Telegram test mesajı | `paket=com.whatsapp` / `org.telegram.messenger` | | |
| T5 | "Kendi test bildirimimi gönder" | `Kendi bildirimimiz, analiz dışı bırakıldı.` | | |
| T6 | Aynı sohbetten art arda 3 mesaj | Grup özeti atlanır, mesajlar ayrı loglanır | | |
| T7 | Türkçe karakterli mesaj ("şğüıöç İ") | Karakterler bozulmadan görünür | | |
| T8 | Mesaj uygulamasında bildirim içeriği gizli/kapalı | Metin boş veya "Yeni mesaj" → **sınır olarak belgelenir** | | |
| T9 | Ayarlardan izin geri alınır | `Servis bağlantısı kesildi` | | |

**Başarı kriteri:** T2, T3, T4, T5 başarılı.

## Gözlemler / Sorunlar
- 
