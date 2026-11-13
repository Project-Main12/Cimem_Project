# Yöntem 1 – Anahtar kelime filtreleme: tum_veri

- Örnek sayısı: **1505**
- Accuracy: **0.774**
- Macro-F1: **0.786**
- Yanlış alarm oranı (NORMAL → riskli): **0.059**
- Kaçırılan riskli içerik oranı (riskli → NORMAL): **0.257**
- Ortalama analiz süresi: **0.231 ms** (bilgisayarda, Python)

| Sınıf | Precision | Recall | F1 | Destek |
|---|---|---|---|---|
| NORMAL | 0.529 | 0.941 | 0.678 | 354 |
| KUFUR | 1.000 | 0.913 | 0.955 | 299 |
| HAKARET | 0.845 | 0.774 | 0.808 | 288 |
| SIBER_ZORBALIK | 1.000 | 0.595 | 0.746 | 294 |
| TEHDIT | 0.982 | 0.596 | 0.742 | 270 |

## Karışıklık matrisi (satır: gerçek, sütun: tahmin)

| gerçek \ tahmin | NORMAL | KUFUR | HAKARET | SIBER_ZORBALIK | TEHDIT |
|---|---|---|---|---|---|
| NORMAL | 333 | 0 | 18 | 0 | 3 |
| KUFUR | 26 | 273 | 0 | 0 | 0 |
| HAKARET | 65 | 0 | 223 | 0 | 0 |
| SIBER_ZORBALIK | 96 | 0 | 23 | 175 | 0 |
| TEHDIT | 109 | 0 | 0 | 0 | 161 |

Zor örneklerde doğruluk: **0.444** (383 örnek)
