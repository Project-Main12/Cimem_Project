# Etiketleme Kılavuzu – Dijital Kalkan v1

Bu kılavuz, veri setindeki her cümleye **tek bir etiket** verilirken uyulacak kuralları tanımlar.
Yeni örnek eklerken veya iki kişi aynı cümleyi etiketlerken (uyum ölçümü) bu kurallar esas alınır.

## Sınıflar

| Etiket | Tanım | Örnek |
|---|---|---|
| `NORMAL` | Zarar vermeyen her içerik. Olumsuz eleştiri, oyun içi eylem anlatımı, dostça şaka, konu hakkında konuşma dahil. | "bu turda seni öldürdüm haha gg" |
| `KUFUR` | Müstehcen / argo küfür sözcüğü içeren içerik. Hedefli ya da hedefsiz olabilir. | "amk yine lag", "siktir git oyundan" |
| `HAKARET` | Küfür içermeden kişiyi aşağılayan, değersizleştiren ifade. | "sen tam bir aptalsın" |
| `SIBER_ZORBALIK` | Dışlama, toplu alay, ifşa/şantaj, kendine zarar vermeye yönlendirme, ısrarlı taciz, hedef gösterme. | "kimse seni sevmiyor", "fotoğraflarını yayarım" |
| `TEHDIT` | Fiziksel zarar, takip/konum, bedel ödetme veya aileye/eşyaya zarar bildirimi. | "okul çıkışı seni bekleyeceğim" |
| `UYGUNSUZ_YAKLASIM` | Yetişkin/yabancının çocuğa gizlilik isteme, yalnızlık sorgusu, görüntü/kamera isteme, buluşma, konum, platform değiştirme, ödül karşılığı talep (grooming). | "bu konuştuklarımızı kimseye söyleme", "evde yalnız mısın?" |
| `KENDINE_ZARAR` | Çocuğun **kendisi hakkında** ölüm isteği, kendine zarar, derin umutsuzluk veya veda ifadesi. Destek gerektirir. | "artık yaşamak istemiyorum" |

## Karar sırası (bir cümle birden fazla sınıfa uyuyorsa)

Yukarıdan aşağı sorun; **ilk "evet" cevabı etiketi belirler**:

1. Yazan kişi **kendisi için** ölüm/kendine zarar/derin umutsuzluk ifade ediyor mu? → `KENDINE_ZARAR`
2. Gizlilik, yalnızlık sorgusu, görüntü isteme, buluşma veya platform değiştirme talebi var mı? → `UYGUNSUZ_YAKLASIM`
3. Fiziksel zarar, takip veya bedel ödetme bildiriyor mu? → `TEHDIT`
4. Dışlıyor, toplu alaya alıyor, ifşa/şantaj yapıyor, ölüme/kendine zarara yönlendiriyor, ısrarla rahatsız ediyor veya birini hedef gösteriyor mu? → `SIBER_ZORBALIK`
5. Küfürlü (müstehcen/argo) bir sözcük içeriyor mu? → `KUFUR`
6. Küfürsüz biçimde kişiyi aşağılıyor mu? → `HAKARET`
7. Hiçbiri → `NORMAL`

Örnek: "git öl ezik" → 4. soru evet → `SIBER_ZORBALIK` ("ezik" hakaret olsa bile).

## Zor durumlar için kararlar

| Durum | Karar | Gerekçe |
|---|---|---|
| Oyun içi eylem ("seni öldürdüm gg") | `NORMAL` | Oyunun kuralı içinde, gerçek dünyaya yönelik değil |
| Oyundan gerçeğe taşan ("oyunda değil gerçekte öldüreceğim") | `TEHDIT` | Gerçek dünyaya yönelik |
| Şaka görünümlü hakaret ("salak mısın ya 😂 yine unuttun") | `HAKARET` | **Danışman kararı (13.11.2026):** şaka da olsa hakaret sözcüğü kişiye yöneliyorsa risktir. Alt tür: `saka_hakaret` |
| Hakaret sözcüğü olmayan mecaz ("öldüm gülmekten", "manyak gibi güldük") | `NORMAL` | Kişiye yönelik aşağılama yok |
| Övgü amaçlı küfür ("amk çok iyi oynadın") | `KUFUR` | Küfür sözcüğü var; ebeveyn için bilgi değeri taşır |
| Mağdurun aktarımı ("bana salak dedi, öğretmene söyledim") | `NORMAL` | Zararlı söz söylemiyor, aktarıyor |
| Fikir eleştirisi ("bu fikir işe yaramaz") | `NORMAL` | Kişiyi değil fikri hedefliyor |
| Kişiye "işe yaramazsın" | `HAKARET` | Kişiyi değersizleştiriyor |
| Kendine zarara yönlendirme ("git öl", "kendini öldür") | `SIBER_ZORBALIK` | Alt tür: `kendine_zarar_yonlendirme`. Tehdit değil, yönlendirme |
| Başkasına "git öl", "kendini öldür" | `SIBER_ZORBALIK` | Başkasını yönlendiriyor (alt tür: `kendine_zarar_yonlendirme`) |
| Kendisi için "ölmek istiyorum" | `KENDINE_ZARAR` | Yazanın kendi sıkıntısı |
| Mecaz ölüm ("yorgunluktan öldüm", "bu sınav beni öldürecek") | `NORMAL` | Gerçek ölüm isteği yok |
| Sürpriz için gizlilik ("annemin sürprizini kimseye söyleme") | `NORMAL` | Grooming değil; masum gizlilik |
| Arkadaştan ödev fotoğrafı isteme | `NORMAL` | Kişisel görüntü talebi değil |
| Nefret söylemi, dolandırıcılık | Ana sete **girmez** | `ek_kategoriler_v1.csv` (henüz karar verilmedi) |

## Ek sütunlar

| Sütun | Değerler | Anlamı |
|---|---|---|
| `subtype` | ör. `dislama`, `ifsa_santaj`, `fiziksel`, `hedefsiz` | Sınıf içi alt tür; hata analizinde kullanılır |
| `context` | `mesaj`, `oyun`, `youtube`, `sosyal` | Cümlenin geçtiği ortam |
| `difficulty` | `kolay`, `orta`, `zor` | `zor`: sözlükte olmayan, örtük, ironik veya yanlış alarm tuzağı |
| `augmented` | 0 / 1 | 1 ise otomatik üretilmiş yazım varyantı |
| `group_id` | `G0001`… | Aynı temel cümleden türeyen satırlar; **asla farklı bölümlere ayrılmaz** |

## Etik kurallar

- Veri setine **gerçek kişilerin mesajları eklenmez**; yalnızca sentetik veya açık lisanslı, anonimleştirilmiş veri.
- Gerçek isim, okul adı, telefon numarası, kullanıcı adı yazılmaz.
- Küfür ve hakaret içeren dosyalarla çalışma yalnızca danışman gözetiminde yapılır.
