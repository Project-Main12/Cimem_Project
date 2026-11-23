# Yöntem 2 – TF-IDF + Lojistik Regresyon: test bölümü

- Örnek sayısı: **319**
- Seçilen C: 50
- Özellik sayısı: 9447 (harf n-gram 8241, kelime 1206)
- Accuracy: **0.734**
- Macro-F1: **0.736**
- Yanlış alarm oranı: **0.183**
- Kaçırılan riskli içerik oranı: **0.069**
- Ortalama tahmin süresi: **0.001 ms** (bilgisayarda, toplu)

| Sınıf | Precision | Recall | F1 | Destek |
|---|---|---|---|---|
| NORMAL | 0.731 | 0.817 | 0.772 | 60 |
| KUFUR | 0.889 | 0.545 | 0.676 | 44 |
| HAKARET | 0.647 | 0.688 | 0.667 | 48 |
| SIBER_ZORBALIK | 0.585 | 0.844 | 0.691 | 45 |
| TEHDIT | 0.825 | 0.786 | 0.805 | 42 |
| UYGUNSUZ_YAKLASIM | 0.739 | 0.756 | 0.747 | 45 |
| KENDINE_ZARAR | 1.000 | 0.657 | 0.793 | 35 |

| gerçek \ tahmin | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT | UYGUNSUZ_YAKLASIM | KENDINE_ZARAR |
|---|---|---|---|---|---|---|---|
| NORMAL | 49 | 0 | 8 | 3 | 0 | 0 | 0 |
| KUFUR | 2 | 24 | 9 | 6 | 3 | 0 | 0 |
| HAKARET | 6 | 3 | 33 | 6 | 0 | 0 | 0 |
| SIBER_ZORBALIK | 0 | 0 | 1 | 38 | 3 | 3 | 0 |
| TEHDIT | 0 | 0 | 0 | 6 | 33 | 3 | 0 |
| UYGUNSUZ_YAKLASIM | 10 | 0 | 0 | 0 | 1 | 34 | 0 |
| KENDINE_ZARAR | 0 | 0 | 0 | 6 | 0 | 6 | 23 |

Zor örneklerde doğruluk: **0.643** (84 örnek)
