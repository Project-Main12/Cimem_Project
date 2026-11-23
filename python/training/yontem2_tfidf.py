"""
Yöntem 2 – Geleneksel makine öğrenmesi: TF-IDF (harf + kelime n-gram) + Lojistik Regresyon

Çalıştırma (python klasöründen):  python training/yontem2_tfidf.py
Gerekli: pip install scikit-learn numpy

Bilimsel yöntem:
- Eğitim: splits/train.csv | Hiperparametre (C) seçimi: splits/val.csv | Son ölçüm: splits/test.csv
- Metin önce Python/Kotlin ortak normalleştirmeden geçer (ortak/turkce_metin.py)
- Model, telefonda aynı sonucu verecek şekilde JSON'a aktarılır:
  app/src/main/assets/models/yontem2_tfidf_lr.json
"""
import csv, json, sys, time
from pathlib import Path
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import FeatureUnion

KOK = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(KOK))
from ortak.turkce_metin import tokens  # noqa: E402
from evaluation.keyword_baseline import SINIFLAR, metrikler  # noqa: E402

MODEL_YOLU = KOK.parent / "app/src/main/assets/models/yontem2_tfidf_lr.json"


def hazirla(metin: str) -> str:
    return " ".join(tokens(metin, fold=True))


def oku(bolum):
    rows = list(csv.DictReader(open(KOK / f"dataset/splits/{bolum}.csv", encoding="utf-8")))
    return [hazirla(r["text"]) for r in rows], [r["label"] for r in rows], rows


def vektorlestirici():
    char = TfidfVectorizer(analyzer="char_wb", ngram_range=(2, 5), min_df=2, max_features=20000,
                           sublinear_tf=True, lowercase=False)
    word = TfidfVectorizer(analyzer="word", ngram_range=(1, 1), min_df=1, token_pattern=r"\S+",
                           sublinear_tf=True, lowercase=False)
    return char, word


def birlestir(char, word, X):
    from scipy.sparse import hstack
    return hstack([char.transform(X), word.transform(X)]).tocsr()


def main():
    Xtr, ytr, _ = oku("train")
    Xva, yva, _ = oku("val")
    Xte, yte, rows_te = oku("test")

    char, word = vektorlestirici()
    char.fit(Xtr); word.fit(Xtr)
    Ftr, Fva, Fte = (birlestir(char, word, X) for X in (Xtr, Xva, Xte))

    en_iyi = None
    print("C seçimi (doğrulama Macro-F1):")
    for C in [0.5, 1, 2, 5, 10, 20, 50]:
        m = LogisticRegression(C=C, max_iter=3000, class_weight="balanced")
        m.fit(Ftr, ytr)
        _, mf1, *_ = metrikler(yva, list(m.predict(Fva)))
        print(f"  C={C:<5} Macro-F1={mf1:.3f}")
        if en_iyi is None or mf1 > en_iyi[0]:
            en_iyi = (mf1, C, m)
    _, C, model = en_iyi
    print(f"Seçilen C={C}")

    t0 = time.perf_counter()
    tahmin = list(model.predict(Fte))
    sure = (time.perf_counter() - t0) * 1000 / len(Xte)
    acc, mf1, sinif_m, kar, ya, kr = metrikler(yte, tahmin)

    md = ["# Yöntem 2 – TF-IDF + Lojistik Regresyon: test bölümü", "",
          f"- Örnek sayısı: **{len(yte)}**", f"- Seçilen C: {C}",
          f"- Özellik sayısı: {Fte.shape[1]} (harf n-gram {len(char.vocabulary_)}, kelime {len(word.vocabulary_)})",
          f"- Accuracy: **{acc:.3f}**", f"- Macro-F1: **{mf1:.3f}**",
          f"- Yanlış alarm oranı: **{ya:.3f}**", f"- Kaçırılan riskli içerik oranı: **{kr:.3f}**",
          f"- Ortalama tahmin süresi: **{sure:.3f} ms** (bilgisayarda, toplu)", "",
          "| Sınıf | Precision | Recall | F1 | Destek |", "|---|---|---|---|---|"]
    for c in SINIFLAR:
        p, r, f, n = sinif_m[c]
        md.append(f"| {c} | {p:.3f} | {r:.3f} | {f:.3f} | {n} |")
    md += ["", "| gerçek \\ tahmin | " + " | ".join(SINIFLAR) + " |", "|---" * (len(SINIFLAR) + 1) + "|"]
    for g in SINIFLAR:
        md.append(f"| {g} | " + " | ".join(str(kar[(g, t)]) for t in SINIFLAR) + " |")
    zor = [(r["label"], t) for r, t in zip(rows_te, tahmin) if r["difficulty"] == "zor"]
    md += ["", f"Zor örneklerde doğruluk: **{sum(g == t for g, t in zor) / len(zor):.3f}** ({len(zor)} örnek)"]
    out = KOK / "evaluation/sonuclar/yontem2_test_bolumu.md"
    out.write_text("\n".join(md) + "\n", encoding="utf-8")
    print("\n".join(md))

    # ---- telefona aktarım (JSON)
    def vec_json(v):
        return {"vocabulary": {k: int(i) for k, i in v.vocabulary_.items()}, "idf": [float(x) for x in v.idf_]}
    sinif_sirasi = list(model.classes_)
    disari = {
        "format": "dijitalkalkan-tfidf-lr-v1",
        "classes": sinif_sirasi,
        "char": {**vec_json(char), "ngram_min": 2, "ngram_max": 5, "analyzer": "char_wb"},
        "word": vec_json(word),
        "coef": [[round(float(x), 6) for x in row] for row in model.coef_],
        "intercept": [float(x) for x in model.intercept_],
        "test_macro_f1": round(mf1, 4),
    }
    MODEL_YOLU.parent.mkdir(parents=True, exist_ok=True)
    MODEL_YOLU.write_text(json.dumps(disari, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"\nModel: {MODEL_YOLU} ({MODEL_YOLU.stat().st_size // 1024} KB)")

    # ---- Kotlin eşitlik testi için referans olasılıklar
    ornekler = [r["text"] for r in rows_te[:60]] + ["s.a.l.a.k mısın", "evde yalnız mısın", "yarın sınav var"]
    F = birlestir(char, word, [hazirla(t) for t in ornekler])
    P = model.predict_proba(F)
    ref = KOK.parent / "app/src/test/resources/yontem2_referans.tsv"
    with open(ref, "w", encoding="utf-8") as f:
        for t, p in zip(ornekler, P):
            f.write(t.replace("\t", " ") + "\t" + " ".join(f"{x:.6f}" for x in p) + "\n")
    print(f"Referans: {ref}")


if __name__ == "__main__":
    main()
