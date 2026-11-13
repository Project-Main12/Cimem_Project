# Yöntem 1 – İki sınıflı değerlendirme: test_bolumu

- Örnek sayısı: **319** (riskli: 259, normal: 60)
- Accuracy: **0.765**
- RISKLI → Precision **1.000**, Recall **0.710**, F1 **0.831**
- Macro-F1: **0.723**
- Yanlış alarm oranı: **0.000**
- Kaçırılan riskli içerik oranı: **0.290**
- Ortalama analiz süresi: **0.416 ms**

| gerçek \ tahmin | NORMAL | RISKLI |
|---|---|---|
| NORMAL | 60 | 0 |
| RISKLI | 75 | 184 |
