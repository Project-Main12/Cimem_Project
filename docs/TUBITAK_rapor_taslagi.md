# DİJİTAL KALKAN: Türkçe Yapay Zekâ Destekli Çoklu Ortam Çocuk Dijital Güvenlik Sistemi
### TÜBİTAK 2204-A Lise Öğrencileri Araştırma Projesi – Rapor Taslağı

> Bu taslak, proje çıktılarından otomatik üretilen ölçümlere dayanır. **[ ]** ile işaretli yerler
> gerçek telefon testlerinden sonra öğrencilerce doldurulacaktır.

## Özet
Çocukların Android telefonlarında karşılaştıkları küfür, hakaret, siber zorbalık, tehdit, uygunsuz yaklaşım
(grooming) ve kendine zarar ifadelerini **cihaz üzerinde** tespit eden ve ebeveyne **mesaj içeriğini göstermeden**
yalnızca risk kategorisini bildiren bir prototip geliştirilmiştir. Bildirim, ekran (OCR) ve ses (konuşma tanıma)
olmak üzere üç veri kaynağı desteklenmektedir. 7 sınıflı, 2115 örnekli sentetik bir Türkçe veri seti oluşturulmuş;
anahtar kelime filtreleme, TF-IDF + lojistik regresyon ve DistilBERTurk ince ayar yöntemleri aynı test verisinde
karşılaştırılmıştır. En yüksek başarı, kelime listesi ile TF-IDF modelinin birleştirildiği yöntemde elde edilmiştir
(Macro-F1 **0,852**, yanlış alarm **0,100**, kaçırılan riskli içerik **0,069**).

## 1. Amaç ve Araştırma Sorusu
"Android mobil cihazlarda farklı dijital ortamlardan elde edilen metin, ses ve görüntü verileri kullanılarak Türkçe
siber zorbalık ve zararlı içerikler ne doğrulukla tespit edilebilir?"

Alt sorular:
1. Anahtar kelime, geleneksel makine öğrenmesi ve Türkçe dil modeli yaklaşımları arasında başarı farkı nedir?
2. Yöntemler birleştirildiğinde başarı artar mı?
3. Modeller telefonda hangi gecikme, bellek ve pil maliyetiyle çalışır?

## 2. Özgün Değer
- Türkçe için **cihaz üzerinde** çalışan, **çok kaynaklı** (bildirim + ekran + ses) prototip.
- **Gizlilik öncelikli tasarım:** mesaj, görüntü ve ses cihazdan çıkmaz; veliye yalnızca kategori/uygulama/saat gider.
- **Kişiselleştirilebilir sözlük:** veli ve çocuk, çocuğa özgü lakapları ekleyebilir (sunucuda saklanmaz).
- Oyun dili, yazım hileleri (@ptal, s.a.l.a.k), şaka görünümlü hakaret ve "tuzak" cümleler içeren 7 sınıflı veri seti.
- Grooming ve kendine zarar sınıflarının Türkçe çocuk güvenliği bağlamında ele alınması.

## 3. Yöntem
### 3.1 Veri seti
- 635 elle yazılmış sentetik temel cümle + 1480 yazım varyantı = **2115 örnek**, 7 sınıf
  (Normal, Küfür, Hakaret, Siber zorbalık, Tehdit, Uygunsuz yaklaşım, Kendine zarar).
- %70 eğitim / %15 doğrulama / %15 test; **grup bazlı bölme** (bir cümlenin varyantları aynı bölümde).
- Etiketleme kuralları: `docs/etiketleme_kilavuzu.md`; dağılımlar: `python/dataset/ISTATISTIK.md`.
- Etik: Gerçek çocuk verisi kullanılmamıştır.

### 3.2 Karşılaştırılan yöntemler
| Yöntem | Açıklama | Telefonda |
|---|---|---|
| 1 – Anahtar kelime | 308 kurallı Türkçe sözlük; normalleştirme (Türkçe harf, harf uzatma, sembol hileleri) | Kotlin, Python ile 2143 cümlede birebir aynı |
| 2 – TF-IDF + LR | Harf 2–5 n-gram + kelime özellikleri, lojistik regresyon (C doğrulamada seçildi) | Kotlin, Python ile olasılık farkı < 10⁻⁶ |
| 3 – DistilBERTurk | `dbmdz/distilbert-base-turkish-cased`, 10 tur ince ayar, en iyi tur doğrulamada seçildi | LiteRT int8 (69 MB), parçalayıcı 103/103 aynı |
| Birleşik | Kelime listesi risk bulursa onu, bulmazsa modeli (olasılık ≥ eşik) kullanır | Uygulamanın varsayılanı |

### 3.3 Mobil sistem
Kotlin + Jetpack Compose, minSdk 29. NotificationListenerService (bildirim), MediaProjection + ML Kit OCR
(ekran), AudioPlaybackCapture + Vosk Türkçe (ses), Room (metinsiz olay kaydı), Supabase (eşleştirme, RLS) +
Firebase Cloud Messaging (veliye bildirim). Mimari: `README.md`.

## 4. Bulgular
### 4.1 Sınıflandırma başarısı (test bölümü, 319 örnek)
| Yöntem | Accuracy | Macro-F1 | Yanlış alarm | Kaçırılan riskli | Zor örnek doğruluğu |
|---|---|---|---|---|---|
| Yöntem 1 – Anahtar kelime | 0,737 | 0,759 | **0,000** | 0,290 | 0,536 |
| Yöntem 2 – TF-IDF + LR | 0,734 | 0,736 | 0,183 | 0,069 | 0,643 |
| Yöntem 3 – DistilBERTurk | 0,643 | 0,647 | 0,450 | 0,066 | 0,548 |
| Yöntem 3 – LiteRT int8 (telefondaki) | 0,639 | 0,642 | 0,400 | 0,073 | 0,571 |
| **Birleşik: kelime + TF-IDF** | **0,846** | **0,852** | 0,100 | 0,069 | **0,690** |
| Birleşik: kelime + DistilBERTurk (int8) | 0,806 | 0,809 | 0,350 | **0,050** | 0,667 |

Ayrıntı ve sınıf bazında F1: `python/evaluation/sonuclar/karsilastirma.md`.

### 4.2 Yorum
- **Kelime listesi** hiç yanlış alarm üretmemiş ama riskli içeriklerin %29'unu kaçırmıştır (örtük tehditler,
  sözlükte olmayan ifadeler). Açık küfürde (F1 0,96) en başarılı yöntemdir.
- **Makine öğrenmesi** kaçırma oranını %29'dan %7'ye düşürmüş, karşılığında yanlış alarm artmıştır.
- **Birleşik yöntem** iki yaklaşımın güçlü yönlerini birleştirerek tüm ölçütlerde dengeli en iyi sonucu vermiştir.
- **DistilBERTurk beklenenin altında kalmıştır.** Olası nedenler: küçük ve sentetik eğitim verisi (~1500 örnek,
  çoğu varyant), sınıf sayısının fazla olması, varyantların aşırı öğrenmeye yol açması. Bu, büyük dil modellerinin
  az veriyle her zaman üstün olmadığını gösteren önemli bir bulgudur.
- **Nicemleme (int8)** modeli 272 MB'tan 69 MB'a küçültmüş, başarıyı yalnızca ~0,005 Macro-F1 düşürmüştür.

### 4.3 Telefonda performans [telefon testlerinden sonra doldurulacak]
| Ölçüt | Yöntem 1 | Yöntem 2 | Yöntem 3 | Birleşik |
|---|---|---|---|---|
| Ortalama analiz süresi (ms) | [ ] | [ ] | [ ] | [ ] |
| Bellek (MB, `dumpsys meminfo`) | [ ] | [ ] | [ ] | [ ] |
| 1 saatlik pil tüketimi (%) – ekran analizi açık/kapalı | [ ] | | | |

Kaynak: Uygulama → Ayarlar → Ölçümler → CSV dışa aktar; ölçüm adımları: `docs/telefon_test_rehberi.md` (bölüm 8).

### 4.4 Ortam bazında gözlemler (emülatör ve telefon)
- Bildirim: paket adı, başlık ve metin okunabildi; Türkçe karakterler doğru (`docs/test_asama1.md`).
- Bazı uygulamalar metni standart alanda vermiyor (ör. Saat zamanlayıcısı) → okunamıyor.
- [ ] Ekran (OCR) ve ses testlerinin sonuçları (T12–T18).

## 5. Sınırlılıklar
1. Veri seti sentetiktir ve sözlükle aynı kaynaktan hazırlanmıştır → Yöntem 1 başarısı iyimserdir.
   Bağımsız değerlendirme için OffensEval-TR (`docs/offenseval_rehberi.md`) ve öğrencilerin yazacağı yeni test seti önerilir.
2. Android kısıtları: gizlenmiş bildirimler okunamaz; Android 14+ her ekran paylaşımında onay ister; bazı oyunlar ses
   yakalamayı engeller; bankacılık vb. uygulamalar ekran görüntüsünü engeller.
3. Dışarıdan yüklenen uygulamalar Google Play Protect tarafından engellenebilir.
4. Bağlam (şaka/ciddi, oyun içi/gerçek) her yöntem için zordur.

## 6. Etik ve Gizlilik
- Gizli izleme yoktur: çocuk bağlantıyı, velisine giden uyarıları ve ekran analizinin açık olduğunu her zaman görür.
- Açık rıza ekranı; bağlantı çocuk tarafından da kaldırılabilir.
- Mesaj/görüntü/ses hiçbir sunucuya gönderilmez ve cihazda saklanmaz; yerel kayıtta yalnızca kategori/süre vardır.
- Sunucu: satır düzeyinde güvenlik (her kullanıcı yalnızca kendi verisi), 12 + 8 adımlı güvenlik testleri geçti.

## 7. Sonuç
Türkçe zararlı içerik tespitinde tek bir yöntem yerine **kural tabanlı ve öğrenen yöntemlerin birleşimi** en iyi
sonucu vermiştir (Macro-F1 0,852). Tüm analiz telefonda, mesaj içeriği paylaşılmadan yapılabilmektedir.

## 8. Gelecek Çalışmalar
- Gerçek ve anonimleştirilmiş, etik kurul onaylı veriyle modelin yeniden eğitilmesi.
- BERT için daha fazla veri, sınıf dengeleme ve bilgi damıtma; görüntü içeriği (uygunsuz görsel) analizi.
- Tekrarlanan olaylar için risk yükseltme (ör. 24 saatte 3 hakaret → yüksek risk).

## Kaynaklar
- Çöltekin, Ç. (2020). A Corpus of Turkish Offensive Language on Social Media. LREC 2020.
- Schweter, S. (2020). BERTurk – BERT models for Turkish (dbmdz). Zenodo.
- Sanh, V. vd. (2019). DistilBERT, a distilled version of BERT. arXiv:1910.01108.
- Google. ML Kit Text Recognition v2; LiteRT (TensorFlow Lite) belgeleri.
- Alpha Cephei. Vosk Speech Recognition Toolkit.
- Android Developers. NotificationListenerService, MediaProjection, AudioPlaybackCapture belgeleri.
