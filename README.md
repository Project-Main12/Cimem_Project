# DİJİTAL KALKAN

**Türkçe Yapay Zekâ Destekli Çocuk Dijital Güvenlik Sistemi**
TÜBİTAK 2204-A Lise Öğrencileri Araştırma Projesi · Prototip sürüm **2.0.0**

<img src="docs/ekran-goruntuleri/sekme1_bildirimler.png" width="230" alt="Çocuk telefonu"> <img src="docs/ekran-goruntuleri/veli_ana_ekran.png" width="230" alt="Veli telefonu">

## Ne yapar?
Tek uygulama, iki rol:

| Çocuk telefonu | Veli telefonu |
|---|---|
| Gelen bildirimler **telefonun içinde** Türkçe analiz edilir (7 sınıf) | E-posta ile hesap, **6 haneli kodla** çocuk cihazı bağlama |
| **Mesajlar hiçbir yerde gösterilmez/saklanmaz**; yalnızca uygulama, saat, kategori | Bildirim: **"Tehdit kategorisinde uygunsuz mesaj tespit edildi"** |
| "Metin dene": cümle yazıp 3 yöntemin sonucunu yan yana görme | Uyarı listesi, okundu işaretleme, **son 7 gün özeti** |
| **Ekran (OCR)** ve **ses (konuşma tanıma)** analizi – görünür bildirimle | |
| "Ayarlar": yöntem seçimi, ölçümler, CSV dışa aktarma | |
| "Kelimelerim": kendi lakap/ifade listesi + velinin eklediği ifadeler | **Kelime listesi**: lakap/özel ifadeyi çocuğun telefonuna ekleme/kaldırma |
| "Veli": açık onay, bağlanma, velisine giden uyarıları görme, bağlantıyı kaldırma | Bağlı cihazları görme / kaldırma |

Sınıflar: Normal, Küfür, Hakaret, Siber zorbalık, Tehdit, Uygunsuz yaklaşım (grooming), Kendine zarar.

## Araştırma sonuçları (test, 319 örnek)
| Yöntem | Macro-F1 | Yanlış alarm | Kaçırılan riskli |
|---|---|---|---|
| 1 – Anahtar kelime | 0,759 | 0,000 | 0,290 |
| 2 – TF-IDF + LR | 0,736 | 0,183 | 0,069 |
| 3 – DistilBERTurk (LiteRT int8) | 0,642 | 0,400 | 0,073 |
| **Birleşik: kelime + TF-IDF (uygulama varsayılanı)** | **0,852** | 0,100 | 0,069 |

Ayrıntılı tablo: `python/evaluation/sonuclar/karsilastirma.md`

## Gizlilik tasarımı
- **Mesaj metni, gönderen kişi ve kelime listeleri hiçbir sunucuda tutulmaz.**
- Ana kelime veritabanı (`app/src/main/assets/lexicon/tr_risk_lexicon.txt`) uygulamanın içindedir.
- Veliye giden tek bilgi: kategori, risk seviyesi, uygulama adı, saat.
- Velinin eklediği ifadeler sunucuda **saklanmadan** çocuğun telefonuna iletilir ve orada tutulur.
- Çocuk, bağlantıyı ve velisine giden uyarıları her zaman görür; bağlantıyı kaldırabilir.
- Sunucu (Supabase) yalnızca **eşleştirme ve uyarı iletimi** için kullanılır; ayrıntı: `supabase/README.md`.

## Kurulum ve test
- Telefonda deneme: **`docs/telefon_test_rehberi.md`**
- Aşama 1 test kaydı: `docs/test_asama1.md`
- Etiketleme kuralları: `docs/etiketleme_kilavuzu.md`
- OffensEval-TR: `docs/offenseval_rehberi.md`

## Proje yapısı
```
app/src/main/java/com/digitalkalkan/app/
  monitoring/notification/   bildirim dinleyici (NotificationListenerService)
  domain/analysis/           normalleştirme, Yöntem 1 (kelime), 2 (TF-IDF), 3 (BERT/LiteRT), birleşik
  monitoring/screen/         ekran (OCR) ve ses analizi servisi
  monitoring/audio/          Vosk Türkçe model yöneticisi
  data/local/db/             Room: metinsiz olay kaydı
  domain/alerts/             veliye uyarı gönderimi
  data/remote/               Supabase bağlantısı
  data/repository/           kullanıcı / veli kelime listeleri (cihazda)
  notifications/             Firebase bildirim servisi
  ui/                        çocuk ekranları, veli ekranları, rol seçimi
app/src/main/assets/lexicon/ Türkçe risk sözlüğü
app/src/main/assets/models/ Yöntem 2 (JSON) ve Yöntem 3 (LiteRT int8) modelleri
python/                      veri seti, eğitim (training/), dışa aktarım (export/), değerlendirme
supabase/                    veritabanı şeması, bildirim fonksiyonu, testler
docs/                        rehberler, test kayıtları, ekran görüntüleri
```

## Teknik bilgiler
Kotlin · Jetpack Compose · minSdk 29 (Android 10) · targetSdk 36 · AGP 9.5 · Gradle 9.8 · Room ·
ML Kit OCR · LiteRT · Vosk · Supabase (Auth, Postgres + RLS, Edge Function) · Firebase Cloud Messaging

Testler: `./gradlew testDebugUnitTest` (analiz motoru Python ile 2143 cümlede birebir aynı;
ekran görüntüleri `app/build/screenshots/`).

## Modelleri yeniden üretme
```bash
cd python
python dataset/build_dataset.py           # veri seti
python evaluation/keyword_baseline.py     # Yöntem 1
python training/yontem2_tfidf.py          # Yöntem 2 (+ telefon modeli)
python training/yontem3_bert.py           # Yöntem 3 (pip install torch transformers)
python export/yontem3_litert.py           # Yöntem 3 → LiteRT (pip install litert-torch)
python evaluation/karsilastirma.py        # karşılaştırma tablosu
```

## Uygulama turu (telefon olmadan)
`./gradlew testDebugUnitTest --tests '*UygulamaTuruTest*'` → `app/build/tur/` klasöründe 13 adımlık ekran görüntüleri
(gerçek arayüz ve gerçek analiz motoru; sunucu işlemleri örnek verilerle).

## APK
`./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` (telefon) ve
`app-x86_64-debug.apk` (emülatör).

## Bilinen eksikler
- Telefonda performans (süre, bellek, pil) ölçümleri ve ekran/ses testleri öğrencilerce yapılacak.
- Veliye anlık bildirim için Supabase'de `FIREBASE_SERVICE_ACCOUNT` hizmet hesabı anahtarı gereklidir.
