# OffensEval-TR Rehberi (Öğrenci 2)

## Bu veri seti nedir, neden kullanıyoruz?

**OffensEval-TR 2020**, Türkçe tweetlerden oluşan, akademik çalışmalarda yaygın kullanılan bir veri setidir
(Çöltekin, 2020). Yaklaşık **31.000 eğitim** ve **3.500 test** tweeti içerir. Her tweet iki etiketten birini taşır:

| Etiket | Anlamı | Bizdeki karşılığı |
|---|---|---|
| `NOT` | Saldırgan değil | `NORMAL` |
| `OFF` | Saldırgan (küfür, hakaret, hedefli saldırı) | `RISKLI` (bizim 6 risk sınıfının herhangi biri) |

Bizim veri setimiz **sentetik** ve kelime listesiyle **aynı kaynaktan** hazırlandı; bu yüzden Yöntem 1'in
orada aldığı puan iyimserdir. OffensEval **gerçek insanların yazdığı, bizim hiç görmediğimiz** veridir.
Projede üç amaçla kullanılır:

1. **Dış doğrulama:** Yöntem 1 (kelime listesi), Yöntem 2 (klasik ML) ve Yöntem 3 (BERT) gerçek veride ne yapıyor?
2. **Genelleme testi:** Bizim veriyle eğitilen model, hiç görmediği gerçek tweetlerde başarılı mı?
3. **Ön eğitim (isteğe bağlı):** BERT önce OffensEval'de "saldırgan mı?" sorusunu öğrenir, sonra bizim 7 sınıflı veriyle ince ayar yapılır.

> Sınırlılık: OffensEval yetişkinlerin Twitter dilidir; çocukların oyun/mesaj dili değildir. Grooming ve kendine zarar
> sınıflarını içermez. Bu yüzden yalnızca **ikili (riskli / değil)** karşılaştırmada kullanılır.

## Adım adım

### 1. Python ve kütüphane kurulumu (bir kez)
- https://www.python.org adresinden Python 3.10 veya üstünü kurun. Kurulumda **"Add Python to PATH"** kutusunu işaretleyin.
- Proje klasöründeki `python` klasöründe komut penceresi açın (klasörün adres çubuğuna `cmd` yazıp Enter).

```bash
pip install datasets
```

### 2. Veriyi indirme
```bash
python dataset/offenseval_indir.py
```
Beklenen çıktı (sayılar yaklaşık):
```
offenseval_tr_train.csv: 31756 örnek  {'NORMAL': ..., 'RISKLI': ...}
offenseval_tr_test.csv: 3528 örnek  {'NORMAL': ..., 'RISKLI': ...}
```
Dosyalar `python/dataset/dis_veri/` klasörüne kaydedilir. Bu klasör **GitHub'a yüklenmez** (lisans ve boyut).

### 3. Önce birkaç satıra göz atın
`dis_veri/offenseval_tr_test.csv` dosyasını Excel ile açın, 20–30 satır okuyun. Not edin:
- Kullanıcı adları `@USER` olarak gizlenmiş mi?
- Hangi tür ifadeler `RISKLI` sayılmış? Bizim kurallarımızla uyuşuyor mu?
- Siyasi/spor tartışmaları gibi bizim veri setimizde olmayan konular var mı?

### 4. Yöntem 1'i OffensEval testinde ölçün
```bash
python evaluation/keyword_baseline.py --csv dataset/dis_veri/offenseval_tr_test.csv --ikili
```
Sonuç `evaluation/sonuclar/yontem1_offenseval_tr_test_ikili.md` dosyasına,
yanlış tahminler `..._ikili_hatalar.csv` dosyasına yazılır.

### 5. Sonuçları tabloya işleyin
| Veri | Accuracy | RISKLI Precision | RISKLI Recall | RISKLI F1 | Yanlış alarm | Kaçırılan |
|---|---|---|---|---|---|---|
| Dijital Kalkan test (ikili) | | | | | | |
| OffensEval-TR test | | | | | | |

Kendi veri setimizin ikili sonucunu almak için:
```bash
python evaluation/keyword_baseline.py --bolum test --ikili
```

### 6. Hata analizi (raporda en değerli bölüm)
`..._ikili_hatalar.csv` dosyasından **rastgele 50 hata** seçip şu sınıflara ayırın:
- Kelime listesinde olmayan küfür/hakaret
- Bağlam gerektiren saldırganlık (ironi, ima)
- Yanlış alarm (masum kelime eşleşmesi)
- Etiket tartışmalı (bize göre yanlış etiketlenmiş)

Bu sayımlar "anahtar kelime yönteminin sınırları" bölümünün kanıtı olur.

## Kurallar
- OffensEval tweetleri **kelime listesini güncellemek için kullanılmaz** (yoksa test sonucu şişer).
  Liste güncellenecekse yalnızca `offenseval_tr_train.csv` incelenir; ölçüm yine `test` dosyasında yapılır.
- İndirilen dosyalar paylaşılmaz, depoya eklenmez.
- Raporda kaynak gösterilir: *Çöltekin, Ç. (2020). A Corpus of Turkish Offensive Language on Social Media. LREC 2020.*
