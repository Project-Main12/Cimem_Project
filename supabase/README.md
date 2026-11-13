# Supabase – Veli/Çocuk Bağlantısı

- Proje: `dijital-kalkan` (bölge: eu-central-1 / Frankfurt, ücretsiz plan)
- URL: `https://nreqibswrqqhlsqweeue.supabase.co`
- Uygulamada kullanılan anahtar: **publishable key** (`sb_publishable_...`). Bu anahtar uygulamaya
  gömülmek için tasarlanmıştır; güvenliği satır düzeyinde güvenlik (RLS) kuralları sağlar.
  **Secret / service_role anahtarı ASLA uygulamaya veya GitHub'a konmaz.**

## Tablolar
| Tablo | İçerik | Kim görür |
|---|---|---|
| `profiles` | kullanıcının rolü (veli/çocuk) | yalnızca kendisi |
| `pairing_codes` | 6 haneli, 10 dk geçerli, tek kullanımlık kod | doğrudan erişim yok (fonksiyonla) |
| `pairing_attempts` | kod deneme kaydı (saatte en fazla 10) | doğrudan erişim yok |
| `family_links` | veli ↔ çocuk cihazı bağlantısı | veli ve o çocuk |
| `alerts` | **yalnızca** kategori, seviye, uygulama, saat | veli ve o çocuk (şeffaflık) |
| `push_tokens` | bildirim adresi (Firebase) | yalnızca kendisi |

**Mesaj metni, gönderen kişi ve kullanıcı kelime listesi hiçbir tabloda tutulmaz.**

## Fonksiyonlar
- `create_pairing_code()` → veli çağırır, 6 haneli kod döner
- `redeem_pairing_code(kod, cocuk_adi)` → çocuk cihazı çağırır, bağlantı kurulur

## Doğrulanan senaryo (13.11.2026, SQL testi)
1. Veli kod üretir ✔ 2. Anonim hesap veli rolü alamaz ✔ 3. Yanlış kod reddedilir, doğru kod bağlar ✔
4. Kod tek kullanımlık ✔ 5. Çocuk uyarı gönderir, veli otomatik atanır ✔ 6. Veli görür ve okundu
işaretler; kategoriyi değiştiremez ✔ 7. Bağlantısız üçüncü kişi hiçbir uyarıyı/bağlantıyı/kodu göremez ✔

## Panel ayarları (elle yapılır)
- Authentication → Sign In / Providers → **Allow anonymous sign-ins: AÇIK** (çocuk cihazı için)
- Test döneminde: Email → **Confirm email: KAPALI** (ücretsiz planda e-posta gönderimi saatte birkaç adetle sınırlı)

## Bilinen sınırlar
- Ücretsiz planda 7 gün hiç kullanılmayan proje otomatik duraklatılır; panelden tek tıkla açılır.
- Uyarıların veli telefonuna uygulama kapalıyken düşmesi için Firebase Cloud Messaging eklenecek.
