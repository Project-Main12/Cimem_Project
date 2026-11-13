# Dijital Kalkan – Veri Seti v1

> ⚠ Bu klasör, zararlı içerik tespiti araştırması için **küfür, hakaret ve tehdit içeren
> sentetik cümleler** barındırır. Tüm cümleler yapay olarak yazılmıştır, gerçek kişilere ait değildir.

## Dosyalar

| Dosya | İçerik |
|---|---|
| `kaynak/temel_ornekler.txt` | Elle yazılmış 635 temel cümle (7 sınıf). **Düzenleme buradan yapılır.** |
| `kaynak/ek_kategoriler.txt` | Ana set dışı 20 örnek: nefret söylemi, dolandırıcılık |
| `build_dataset.py` | Temel cümlelerden varyantları üretir, train/val/test böler |
| `dijitalkalkan_v1.csv` | Tüm veri: 2115 örnek |
| `offenseval_indir.py` | OffensEval-TR indirici (internet gerekir) → `dis_veri/` |
| `splits/train.csv`, `val.csv`, `test.csv` | %70 / %15 / %15 (grup bazlı, sınıf dengeli) |
| `ek_kategoriler_v1.csv` | Ek kategoriler (CSV) |
| `ISTATISTIK.md` | Sınıf, alt tür, bağlam ve varyant dağılımları |

Etiket kuralları: `docs/etiketleme_kilavuzu.md`

## Yeniden oluşturma
```bash
cd python
python dataset/build_dataset.py
```
Ek kütüphane gerekmez (Python 3.10+). Rastgelelik tohumu sabittir (42); her çalıştırmada aynı çıktı.

## Yöntem 1 (anahtar kelime) değerlendirmesi
```bash
cd python
python evaluation/keyword_baseline.py                 # test bölümü
python evaluation/keyword_baseline.py --metin "seni bulcam"
```
Sonuçlar: `python/evaluation/sonuclar/`

## Bilinen sınırlılıklar (raporda belirtilmeli)

1. **Sentetik veri:** Gerçek çocuk sohbetlerinin çeşitliliğini tam yansıtmaz.
2. **Aynı yazar etkisi:** Sözlük ve veri seti aynı kaynaktan hazırlandığı için Yöntem 1'in
   bu veri setindeki başarısı **iyimser**dir. Bağımsız değerlendirme için:
   - Öğrencilerin, sözlüğü görmeden yazacağı yeni bir test seti (önerilen: 150 cümle), ve
   - OffensEval-TR 2020 (gerçek tweetler, ikili etiket: NOT/OFF) kullanılmalıdır.
3. **Varyantlar** temel cümlenin kopyasıdır; bu yüzden aynı gruptakiler aynı bölüme konmuştur.

## Sınıflar (7)
NORMAL, KUFUR, HAKARET, SIBER_ZORBALIK, TEHDIT, UYGUNSUZ_YAKLASIM, KENDINE_ZARAR

## OffensEval-TR (gerçek dış veri)
Adım adım anlatım: `docs/offenseval_rehberi.md`
