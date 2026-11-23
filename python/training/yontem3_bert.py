"""
Yöntem 3 – DistilBERTurk ince ayar (7 sınıf)

Çalıştırma (python klasöründen):  python training/yontem3_bert.py
Gerekli: pip install torch transformers   (CPU yeterli; ~15-25 dk)

- Temel model: dbmdz/distilbert-base-turkish-cased (Hugging Face)
- Eğitim: splits/train.csv | En iyi tur seçimi: splits/val.csv (Macro-F1) | Son ölçüm: splits/test.csv
- Çıktı: training/cikti/yontem3/ (model + tokenizer; GitHub'a eklenmez, büyük dosya)
- Not: Hazır DistilBERTurk bir zorbalık modeli DEĞİLDİR; burada etiketli veriyle ince ayar yapılır.
"""
import csv, json, random, sys, time
from pathlib import Path
import numpy as np
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification

KOK = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(KOK))
from evaluation.keyword_baseline import SINIFLAR, metrikler  # noqa: E402

TEMEL = "dbmdz/distilbert-base-turkish-cased"
CIKTI = KOK / "training/cikti/yontem3"
MAX_LEN, BATCH, EPOCH, LR, TOHUM = 64, 16, 10, 4e-5, 42
torch.manual_seed(TOHUM); random.seed(TOHUM); np.random.seed(TOHUM)
torch.set_num_threads(4)


def oku(bolum):
    rows = list(csv.DictReader(open(KOK / f"dataset/splits/{bolum}.csv", encoding="utf-8")))
    return [r["text"] for r in rows], [SINIFLAR.index(r["label"]) for r in rows], rows


def tahmin(model, tok, metinler):
    model.eval(); sonuc = []
    with torch.no_grad():
        for i in range(0, len(metinler), 64):
            enc = tok(metinler[i:i + 64], truncation=True, max_length=MAX_LEN, padding=True, return_tensors="pt")
            sonuc += model(**enc).logits.argmax(-1).tolist()
    return sonuc


def main():
    tok = AutoTokenizer.from_pretrained(TEMEL)
    model = AutoModelForSequenceClassification.from_pretrained(
        TEMEL, num_labels=len(SINIFLAR), id2label=dict(enumerate(SINIFLAR)),
        label2id={c: i for i, c in enumerate(SINIFLAR)})
    Xtr, ytr, _ = oku("train"); Xva, yva, _ = oku("val"); Xte, yte, rows_te = oku("test")
    opt = torch.optim.AdamW(model.parameters(), lr=LR, weight_decay=0.01)
    adim = (len(Xtr) + BATCH - 1) // BATCH * EPOCH
    sched = torch.optim.lr_scheduler.LambdaLR(opt, lambda s: min(1.0, (s + 1) / (0.1 * adim)) * max(0.0, (adim - s) / adim))

    en_iyi = -1.0
    for ep in range(1, EPOCH + 1):
        model.train(); idx = list(range(len(Xtr))); random.shuffle(idx); t0 = time.time(); kayip = 0.0
        for i in range(0, len(idx), BATCH):
            b = idx[i:i + BATCH]
            enc = tok([Xtr[j] for j in b], truncation=True, max_length=MAX_LEN, padding=True, return_tensors="pt")
            out = model(**enc, labels=torch.tensor([ytr[j] for j in b]))
            out.loss.backward(); torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
            opt.step(); sched.step(); opt.zero_grad(); kayip += out.loss.item()
        p = tahmin(model, tok, Xva)
        _, mf1, *_ = metrikler([SINIFLAR[i] for i in yva], [SINIFLAR[i] for i in p])
        print(f"Tur {ep}: kayıp={kayip / (len(idx) / BATCH):.3f}  doğrulama Macro-F1={mf1:.3f}  ({time.time() - t0:.0f} sn)", flush=True)
        if mf1 > en_iyi:
            en_iyi = mf1; CIKTI.mkdir(parents=True, exist_ok=True)
            model.save_pretrained(CIKTI); tok.save_pretrained(CIKTI)

    model = AutoModelForSequenceClassification.from_pretrained(CIKTI)
    t0 = time.perf_counter(); p = tahmin(model, tok, Xte); sure = (time.perf_counter() - t0) * 1000 / len(Xte)
    gercek = [SINIFLAR[i] for i in yte]; th = [SINIFLAR[i] for i in p]
    acc, mf1, sm, kar, ya, kr = metrikler(gercek, th)
    md = ["# Yöntem 3 – DistilBERTurk ince ayar: test bölümü", "",
          f"- Temel model: `{TEMEL}` · max_len={MAX_LEN}, batch={BATCH}, tur={EPOCH}, lr={LR}",
          f"- Örnek sayısı: **{len(yte)}**", f"- Accuracy: **{acc:.3f}**", f"- Macro-F1: **{mf1:.3f}**",
          f"- Yanlış alarm oranı: **{ya:.3f}**", f"- Kaçırılan riskli içerik oranı: **{kr:.3f}**",
          f"- Ortalama tahmin süresi: **{sure:.1f} ms** (bilgisayar CPU, 64'lük grup)", "",
          "| Sınıf | Precision | Recall | F1 | Destek |", "|---|---|---|---|---|"]
    for c in SINIFLAR:
        pp, r, f, n = sm[c]; md.append(f"| {c} | {pp:.3f} | {r:.3f} | {f:.3f} | {n} |")
    md += ["", "| gerçek \\ tahmin | " + " | ".join(SINIFLAR) + " |", "|---" * (len(SINIFLAR) + 1) + "|"]
    for g in SINIFLAR:
        md.append(f"| {g} | " + " | ".join(str(kar[(g, t)]) for t in SINIFLAR) + " |")
    zor = [(r["label"], t) for r, t in zip(rows_te, th) if r["difficulty"] == "zor"]
    md += ["", f"Zor örneklerde doğruluk: **{sum(g == t for g, t in zor) / len(zor):.3f}** ({len(zor)} örnek)"]
    (KOK / "evaluation/sonuclar/yontem3_test_bolumu.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    json.dump({"test_tahminleri": th}, open(CIKTI / "test_tahminleri.json", "w"))
    print("\n".join(md))


if __name__ == "__main__":
    main()
