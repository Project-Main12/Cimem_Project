# Yöntem 1 – Anahtar kelime filtreleme: test_bolumu

- Örnek sayısı: **228**
- Accuracy: **0.763**
- Macro-F1: **0.770**
- Yanlış alarm oranı (NORMAL → riskli): **0.111**
- Kaçırılan riskli içerik oranı (riskli → NORMAL): **0.253**
- Ortalama analiz süresi: **0.229 ms** (bilgisayarda, Python)

| Sınıf | Precision | Recall | F1 | Destek |
|---|---|---|---|---|
| NORMAL | 0.522 | 0.889 | 0.658 | 54 |
| KUFUR | 1.000 | 1.000 | 1.000 | 45 |
| HAKARET | 0.762 | 0.762 | 0.762 | 42 |
| SIBER_ZORBALIK | 1.000 | 0.644 | 0.784 | 45 |
| TEHDIT | 1.000 | 0.476 | 0.645 | 42 |

## Karışıklık matrisi (satır: gerçek, sütun: tahmin)

| gerçek \ tahmin | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT |
|---|---|---|---|---|---|
| NORMAL | 48 | 0 | 6 | 0 | 0 |
| KUFUR | 0 | 45 | 0 | 0 | 0 |
| HAKARET | 10 | 0 | 32 | 0 | 0 |
| SIBER_ZORBALIK | 12 | 0 | 4 | 29 | 0 |
| TEHDIT | 22 | 0 | 0 | 0 | 20 |

Zor örneklerde doğruluk: **0.409** (66 örnek)
