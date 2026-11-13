# Yöntem 1 – Anahtar kelime filtreleme: test_bolumu

- Örnek sayısı: **319**
- Accuracy: **0.737**
- Macro-F1: **0.759**
- Yanlış alarm oranı (NORMAL → riskli): **0.000**
- Kaçırılan riskli içerik oranı (riskli → NORMAL): **0.290**
- Ortalama analiz süresi: **0.351 ms** (bilgisayarda, Python)

| Sınıf | Precision | Recall | F1 | Destek |
|---|---|---|---|---|
| NORMAL | 0.444 | 1.000 | 0.615 | 60 |
| KUFUR | 1.000 | 0.932 | 0.965 | 44 |
| HAKARET | 0.791 | 0.708 | 0.747 | 48 |
| SIBER_ZORBALIK | 1.000 | 0.533 | 0.696 | 45 |
| TEHDIT | 1.000 | 0.690 | 0.817 | 42 |
| UYGUNSUZ_YAKLASIM | 1.000 | 0.622 | 0.767 | 45 |
| KENDINE_ZARAR | 1.000 | 0.543 | 0.704 | 35 |

## Karışıklık matrisi (satır: gerçek, sütun: tahmin)

| gerçek \ tahmin | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT | UYGUNSUZ_YAKLASIM | KENDINE_ZARAR |
|---|---|---|---|---|---|---|---|
| NORMAL | 60 | 0 | 0 | 0 | 0 | 0 | 0 |
| KUFUR | 3 | 41 | 0 | 0 | 0 | 0 | 0 |
| HAKARET | 14 | 0 | 34 | 0 | 0 | 0 | 0 |
| SIBER_ZORBALIK | 12 | 0 | 9 | 24 | 0 | 0 | 0 |
| TEHDIT | 13 | 0 | 0 | 0 | 29 | 0 | 0 |
| UYGUNSUZ_YAKLASIM | 17 | 0 | 0 | 0 | 0 | 28 | 0 |
| KENDINE_ZARAR | 16 | 0 | 0 | 0 | 0 | 0 | 19 |

Zor örneklerde doğruluk: **0.536** (84 örnek)
