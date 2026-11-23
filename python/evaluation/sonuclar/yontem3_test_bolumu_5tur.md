# Yöntem 3 – DistilBERTurk ince ayar: test bölümü

- Temel model: `dbmdz/distilbert-base-turkish-cased` · max_len=64, batch=16, tur=5, lr=5e-05
- Örnek sayısı: **319**
- Accuracy: **0.624**
- Macro-F1: **0.628**
- Yanlış alarm oranı: **0.383**
- Kaçırılan riskli içerik oranı: **0.062**
- Ortalama tahmin süresi: **6.5 ms** (bilgisayar CPU, 64'lük grup)

| Sınıf | Precision | Recall | F1 | Destek |
|---|---|---|---|---|
| NORMAL | 0.698 | 0.617 | 0.655 | 60 |
| KUFUR | 0.652 | 0.682 | 0.667 | 44 |
| HAKARET | 0.622 | 0.479 | 0.541 | 48 |
| SIBER_ZORBALIK | 0.460 | 0.644 | 0.537 | 45 |
| TEHDIT | 0.739 | 0.810 | 0.773 | 42 |
| UYGUNSUZ_YAKLASIM | 0.512 | 0.467 | 0.488 | 45 |
| KENDINE_ZARAR | 0.758 | 0.714 | 0.735 | 35 |

| gerçek \ tahmin | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT | UYGUNSUZ_YAKLASIM | KENDINE_ZARAR |
|---|---|---|---|---|---|---|---|
| NORMAL | 37 | 4 | 1 | 5 | 2 | 5 | 6 |
| KUFUR | 1 | 30 | 11 | 2 | 0 | 0 | 0 |
| HAKARET | 5 | 11 | 23 | 6 | 0 | 3 | 0 |
| SIBER_ZORBALIK | 6 | 0 | 0 | 29 | 5 | 5 | 0 |
| TEHDIT | 2 | 1 | 1 | 0 | 34 | 2 | 2 |
| UYGUNSUZ_YAKLASIM | 1 | 0 | 1 | 17 | 5 | 21 | 0 |
| KENDINE_ZARAR | 1 | 0 | 0 | 4 | 0 | 5 | 25 |

Zor örneklerde doğruluk: **0.548** (84 örnek)
