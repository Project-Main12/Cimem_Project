"""
Üç yöntemin ve birleşik yöntemin AYNI test bölümünde karşılaştırması (TÜBİTAK raporu ana tablosu).

Çalıştırma (python klasöründen):  python evaluation/karsilastirma.py
Önce: training/yontem2_tfidf.py ve training/yontem3_bert.py çalıştırılmış olmalı.

Birleşik yöntem (uygulamadaki varsayılan): Kelime listesi risk bulursa onu kullan; bulamazsa modelin
riskli kararını yalnızca olasılık >= ESIK ise kabul et. Eşik doğrulama bölümünde seçilir.
"""
import csv, json, sys, time
from pathlib import Path
import numpy as np

KOK = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(KOK))
from ortak.sozluk import AnahtarKelimeSiniflandirici  # noqa: E402
from evaluation.keyword_baseline import SINIFLAR, metrikler  # noqa: E402

ESIKLER = [0.5, 0.6, 0.7, 0.8, 0.9]


def oku(b):
    return list(csv.DictReader(open(KOK / f"dataset/splits/{b}.csv", encoding="utf-8")))


def yontem1(rows):
    s = AnahtarKelimeSiniflandirici.dosyadan()
    t0 = time.perf_counter(); p = [s.siniflandir(r["text"]).etiket for r in rows]
    return p, (time.perf_counter() - t0) * 1000 / len(rows)


def yontem2_olasilik(rows):
    sys.path.insert(0, str(KOK / "training"))
    import importlib
    m = importlib.import_module("yontem2_tfidf")
    from sklearn.linear_model import LogisticRegression
    model = json.load(open(KOK.parent / "app/src/main/assets/models/yontem2_tfidf_lr.json", encoding="utf-8"))
    # JSON'daki modelle (telefondaki ile aynı) olasılık hesapla
    from sklearn.feature_extraction.text import TfidfVectorizer
    import scipy.sparse as sp
    def vec(js, analyzer, ngram):
        v = TfidfVectorizer(analyzer=analyzer, ngram_range=ngram, lowercase=False, sublinear_tf=True,
                            token_pattern=r"\S+" if analyzer == "word" else None)
        v.vocabulary_ = js["vocabulary"]; v.idf_ = np.array(js["idf"]); return v
    ch = vec(model["char"], "char_wb", (2, 5)); wd = vec(model["word"], "word", (1, 1))
    X = [m.hazirla(r["text"]) for r in rows]
    t0 = time.perf_counter()
    F = sp.hstack([ch.transform(X), wd.transform(X)]).tocsr()
    z = F @ np.array(model["coef"]).T + np.array(model["intercept"])
    p = np.exp(z - z.max(1, keepdims=True)); p /= p.sum(1, keepdims=True)
    return model["classes"], p, (time.perf_counter() - t0) * 1000 / len(rows)


def yontem3_olasilik(rows):
    import torch
    from transformers import AutoTokenizer, AutoModelForSequenceClassification
    d = KOK / "training/cikti/yontem3"
    tok = AutoTokenizer.from_pretrained(d); m = AutoModelForSequenceClassification.from_pretrained(d).eval()
    out = []; t0 = time.perf_counter()
    with torch.no_grad():
        for r in rows:   # telefondaki gibi tek tek
            e = tok(r["text"], truncation=True, max_length=64, return_tensors="pt")
            out.append(torch.softmax(m(**e).logits, -1)[0].numpy())
    return [m.config.id2label[i] for i in range(m.config.num_labels)], np.array(out), (time.perf_counter() - t0) * 1000 / len(rows)


def yontem3_litert_olasilik(rows):
    """Telefona giden int8 LiteRT modeli (aynı parçalayıcıyla)."""
    from transformers import AutoTokenizer
    from ai_edge_litert.interpreter import Interpreter
    tok = AutoTokenizer.from_pretrained(KOK / "training/cikti/yontem3")
    meta = json.load(open(KOK.parent / "app/src/main/assets/models/yontem3_meta.json"))
    it = Interpreter(model_path=str(KOK.parent / "app/src/main/assets/models/yontem3_distilberturk.tflite"), num_threads=2)
    it.allocate_tensors(); ins = it.get_input_details(); out = it.get_output_details()[0]["index"]
    res = []; t0 = time.perf_counter()
    for r in rows:
        e = tok(r["text"], truncation=True, max_length=64, padding="max_length", return_tensors="np")
        for d in ins:
            is_mask = "mask" in d["name"].lower() or d["name"].endswith("args_1")
            it.set_tensor(d["index"], (e["attention_mask"] if is_mask else e["input_ids"]).astype(np.int32))
        it.invoke(); z = it.get_tensor(out)[0]; p = np.exp(z - z.max()); res.append(p / p.sum())
    return meta["classes"], np.array(res), (time.perf_counter() - t0) * 1000 / len(rows)


def birlesik(k_pred, classes, P, esik):
    sonuc = []
    for k, p in zip(k_pred, P):
        if k != "NORMAL":
            sonuc.append(k); continue
        c = classes[int(p.argmax())]
        sonuc.append(c if c != "NORMAL" and p.max() >= esik else "NORMAL")
    return sonuc


def esik_sec(val, k_val, classes, Pval):
    en = max(ESIKLER, key=lambda e: metrikler([r["label"] for r in val], birlesik(k_val, classes, Pval, e))[1])
    return en


def main():
    val, test = oku("val"), oku("test")
    y = [r["label"] for r in test]
    zor = [i for i, r in enumerate(test) if r["difficulty"] == "zor"]
    k_test, t1 = yontem1(test); k_val, _ = yontem1(val)
    c2, P2, t2 = yontem2_olasilik(test); _, P2v, _ = yontem2_olasilik(val)
    satirlar = [("Yöntem 1 – Anahtar kelime", k_test, t1)]
    satirlar.append(("Yöntem 2 – TF-IDF + LR", [c2[i] for i in P2.argmax(1)], t2))
    e2 = esik_sec(val, k_val, c2, P2v)
    satirlar.append((f"Birleşik: kelime + Yöntem 2 (eşik {e2})", birlesik(k_test, c2, P2, e2), t1 + t2))
    try:
        c3, P3, t3 = yontem3_olasilik(test); _, P3v, _ = yontem3_olasilik(val)
        satirlar.append(("Yöntem 3 – DistilBERTurk", [c3[i] for i in P3.argmax(1)], t3))
        e3 = esik_sec(val, k_val, c3, P3v)
        satirlar.append((f"Birleşik: kelime + Yöntem 3 (eşik {e3})", birlesik(k_test, c3, P3, e3), t1 + t3))
    except Exception as e:
        print("Yöntem 3 atlandı:", e)
    try:
        c4, P4, t4 = yontem3_litert_olasilik(test); _, P4v, _ = yontem3_litert_olasilik(val)
        satirlar.append(("Yöntem 3 – LiteRT int8 (telefondaki)", [c4[i] for i in P4.argmax(1)], t4))
        e4 = esik_sec(val, k_val, c4, P4v)
        satirlar.append((f"Birleşik: kelime + LiteRT int8 (eşik {e4}) – uygulama", birlesik(k_test, c4, P4, e4), t1 + t4))
    except Exception as e:
        print("LiteRT ölçümü atlandı:", e)

    md = ["# Yöntem Karşılaştırması – test bölümü (319 örnek, 7 sınıf)", "",
          "Hepsi AYNI test verisinde ölçülmüştür. Eşikler doğrulama bölümünde seçilmiştir.", "",
          "| Yöntem | Accuracy | Macro-F1 | Yanlış alarm | Kaçırılan riskli | Zor örnek doğruluğu | Süre/örnek (PC) |",
          "|---|---|---|---|---|---|---|"]
    for ad, p, t in satirlar:
        acc, mf1, _, _, ya, kr = metrikler(y, p)
        zd = sum(y[i] == p[i] for i in zor) / len(zor)
        md.append(f"| {ad} | {acc:.3f} | {mf1:.3f} | {ya:.3f} | {kr:.3f} | {zd:.3f} | {t:.2f} ms |")
    md += ["", "## Sınıf bazında F1", "", "| Yöntem | " + " | ".join(SINIFLAR) + " |", "|---" * (len(SINIFLAR) + 1) + "|"]
    for ad, p, _ in satirlar:
        sm = metrikler(y, p)[2]
        md.append(f"| {ad} | " + " | ".join(f"{sm[c][2]:.2f}" for c in SINIFLAR) + " |")
    out = KOK / "evaluation/sonuclar/karsilastirma.md"
    out.write_text("\n".join(md) + "\n", encoding="utf-8")
    print("\n".join(md))


if __name__ == "__main__":
    main()
