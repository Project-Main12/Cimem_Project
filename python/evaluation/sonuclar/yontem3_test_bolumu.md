# Yöntem 3 – DistilBERTurk ince ayar: test bölümü

- Temel model: `dbmdz/distilbert-base-turkish-cased` · max_len=64, batch=16, tur=10, lr=4e-05
- Örnek sayısı: **319**
- Accuracy: **0.643**
- Macro-F1: **0.647**
- Yanlış alarm oranı: **0.450**
- Kaçırılan riskli içerik oranı: **0.066**
- Ortalama tahmin süresi: **7.6 ms** (bilgisayar CPU, 64'lük grup)

| Sınıf | Precision | Recall | F1 | Destek |
|---|---|---|---|---|
| NORMAL | 0.660 | 0.550 | 0.600 | 60 |
| KUFUR | 0.682 | 0.682 | 0.682 | 44 |
| HAKARET | 0.667 | 0.583 | 0.622 | 48 |
| SIBER_ZORBALIK | 0.500 | 0.667 | 0.571 | 45 |
| TEHDIT | 0.723 | 0.810 | 0.764 | 42 |
| UYGUNSUZ_YAKLASIM | 0.639 | 0.511 | 0.568 | 45 |
| KENDINE_ZARAR | 0.675 | 0.771 | 0.720 | 35 |

| gerçek \ tahmin | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT | UYGUNSUZ_YAKLASIM | KENDINE_ZARAR |
|---|---|---|---|---|---|---|---|
| NORMAL | 33 | 8 | 1 | 5 | 2 | 4 | 7 |
| KUFUR | 1 | 30 | 12 | 1 | 0 | 0 | 0 |
| HAKARET | 6 | 5 | 28 | 5 | 1 | 0 | 3 |
| SIBER_ZORBALIK | 3 | 0 | 0 | 30 | 5 | 6 | 1 |
| TEHDIT | 2 | 1 | 1 | 0 | 34 | 2 | 2 |
| UYGUNSUZ_YAKLASIM | 1 | 0 | 0 | 16 | 5 | 23 | 0 |
| KENDINE_ZARAR | 4 | 0 | 0 | 3 | 0 | 1 | 27 |

Zor örneklerde doğruluk: **0.548** (84 örnek)
