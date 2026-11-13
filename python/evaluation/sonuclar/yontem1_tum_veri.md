# Yöntem 1 – Anahtar kelime filtreleme: tum_veri

- Örnek sayısı: **2115**
- Accuracy: **0.719**
- Macro-F1: **0.748**
- Yanlış alarm oranı (NORMAL → riskli): **0.061**
- Kaçırılan riskli içerik oranı (riskli → NORMAL): **0.312**
- Ortalama analiz süresi: **0.360 ms** (bilgisayarda, Python)

| Sınıf | Precision | Recall | F1 | Destek |
|---|---|---|---|---|
| NORMAL | 0.407 | 0.939 | 0.568 | 393 |
| KUFUR | 1.000 | 0.883 | 0.938 | 299 |
| HAKARET | 0.859 | 0.772 | 0.813 | 324 |
| SIBER_ZORBALIK | 1.000 | 0.578 | 0.733 | 294 |
| TEHDIT | 0.964 | 0.600 | 0.740 | 270 |
| UYGUNSUZ_YAKLASIM | 0.944 | 0.554 | 0.698 | 305 |
| KENDINE_ZARAR | 1.000 | 0.591 | 0.743 | 230 |

## Karışıklık matrisi (satır: gerçek, sütun: tahmin)

| gerçek \ tahmin | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT | UYGUNSUZ_YAKLASIM | KENDINE_ZARAR |
|---|---|---|---|---|---|---|---|
| NORMAL | 369 | 0 | 15 | 0 | 6 | 3 | 0 |
| KUFUR | 35 | 264 | 0 | 0 | 0 | 0 | 0 |
| HAKARET | 74 | 0 | 250 | 0 | 0 | 0 | 0 |
| SIBER_ZORBALIK | 103 | 0 | 21 | 170 | 0 | 0 | 0 |
| TEHDIT | 105 | 0 | 0 | 0 | 162 | 3 | 0 |
| UYGUNSUZ_YAKLASIM | 136 | 0 | 0 | 0 | 0 | 169 | 0 |
| KENDINE_ZARAR | 85 | 0 | 5 | 0 | 0 | 4 | 136 |

Zor örneklerde doğruluk: **0.450** (598 örnek)
