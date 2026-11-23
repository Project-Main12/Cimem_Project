# Yöntem Karşılaştırması – test bölümü (319 örnek, 7 sınıf)

Hepsi AYNI test verisinde ölçülmüştür. Eşikler doğrulama bölümünde seçilmiştir.

| Yöntem | Accuracy | Macro-F1 | Yanlış alarm | Kaçırılan riskli | Zor örnek doğruluğu | Süre/örnek (PC) |
|---|---|---|---|---|---|---|
| Yöntem 1 – Anahtar kelime | 0.737 | 0.759 | 0.000 | 0.290 | 0.536 | 0.33 ms |
| Yöntem 2 – TF-IDF + LR | 0.734 | 0.736 | 0.183 | 0.069 | 0.643 | 0.11 ms |
| Birleşik: kelime + Yöntem 2 (eşik 0.5) | 0.846 | 0.852 | 0.100 | 0.069 | 0.690 | 0.45 ms |
| Yöntem 3 – DistilBERTurk | 0.643 | 0.647 | 0.450 | 0.066 | 0.548 | 12.84 ms |
| Birleşik: kelime + Yöntem 3 (eşik 0.6) | 0.803 | 0.804 | 0.417 | 0.039 | 0.655 | 13.17 ms |
| Yöntem 3 – LiteRT int8 (telefondaki) | 0.639 | 0.642 | 0.400 | 0.073 | 0.571 | 6.20 ms |
| Birleşik: kelime + LiteRT int8 (eşik 0.6) – uygulama | 0.806 | 0.809 | 0.350 | 0.050 | 0.667 | 6.53 ms |

## Sınıf bazında F1

| Yöntem | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT | UYGUNSUZ_YAKLASIM | KENDINE_ZARAR |
|---|---|---|---|---|---|---|---|
| Yöntem 1 – Anahtar kelime | 0.62 | 0.96 | 0.75 | 0.70 | 0.82 | 0.77 | 0.70 |
| Yöntem 2 – TF-IDF + LR | 0.77 | 0.68 | 0.67 | 0.69 | 0.80 | 0.75 | 0.79 |
| Birleşik: kelime + Yöntem 2 (eşik 0.5) | 0.82 | 0.94 | 0.75 | 0.80 | 0.90 | 0.88 | 0.87 |
| Yöntem 3 – DistilBERTurk | 0.60 | 0.68 | 0.62 | 0.57 | 0.76 | 0.57 | 0.72 |
| Birleşik: kelime + Yöntem 3 (eşik 0.6) | 0.67 | 0.91 | 0.84 | 0.70 | 0.93 | 0.82 | 0.76 |
| Yöntem 3 – LiteRT int8 (telefondaki) | 0.63 | 0.70 | 0.61 | 0.57 | 0.77 | 0.53 | 0.68 |
| Birleşik: kelime + LiteRT int8 (eşik 0.6) – uygulama | 0.70 | 0.95 | 0.83 | 0.69 | 0.93 | 0.79 | 0.78 |
