"""
Yöntem 1 – Anahtar kelime filtreleme: değerlendirme

Kullanım (python klasöründen):
    python evaluation/keyword_baseline.py                      # test bölümü
    python evaluation/keyword_baseline.py --bolum hepsi
    python evaluation/keyword_baseline.py --metin "seni öldürecegim"
    python evaluation/keyword_baseline.py --csv baska_veri.csv  (text,label sütunları)
    python evaluation/keyword_baseline.py --csv dataset/dis_veri/offenseval_tr_test.csv --ikili
        (--ikili: yalnızca "riskli mi, değil mi" ölçülür; OffensEval gibi 2 etiketli veriler için)

Ölçülenler: Accuracy, sınıf bazında Precision/Recall/F1, Macro-F1,
yanlış alarm oranı, kaçırılan riskli içerik oranı, ortalama analiz süresi.
"""
import argparse
import csv
import sys
import time
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ortak.sozluk import AnahtarKelimeSiniflandirici  # noqa: E402

SINIFLAR = ["NORMAL", "KUFUR", "HAKARET", "SIBER_ZORBALIK", "TEHDIT", "UYGUNSUZ_YAKLASIM", "KENDINE_ZARAR"]
KOK = Path(__file__).resolve().parents[1]


def metrikler(gercek, tahmin):
    karisiklik = Counter(zip(gercek, tahmin))
    sonuc = {}
    for c in SINIFLAR:
        tp = karisiklik[(c, c)]
        fp = sum(karisiklik[(g, c)] for g in SINIFLAR if g != c)
        fn = sum(karisiklik[(c, t)] for t in SINIFLAR if t != c)
        p = tp / (tp + fp) if tp + fp else 0.0
        r = tp / (tp + fn) if tp + fn else 0.0
        f = 2 * p * r / (p + r) if p + r else 0.0
        sonuc[c] = (p, r, f, tp + fn)
    acc = sum(karisiklik[(c, c)] for c in SINIFLAR) / len(gercek)
    macro_f1 = sum(v[2] for v in sonuc.values()) / len(SINIFLAR)
    normal = [t for g, t in zip(gercek, tahmin) if g == "NORMAL"]
    riskli = [t for g, t in zip(gercek, tahmin) if g != "NORMAL"]
    yanlis_alarm = sum(t != "NORMAL" for t in normal) / len(normal) if normal else 0
    kacirilan = sum(t == "NORMAL" for t in riskli) / len(riskli) if riskli else 0
    return acc, macro_f1, sonuc, karisiklik, yanlis_alarm, kacirilan


def rapor(baslik, satirlar, sinif, yol_md, yol_hata):
    gercek, tahmin, hatalar, sureler = [], [], [], []
    for s in satirlar:
        t0 = time.perf_counter()
        sonuc = sinif.siniflandir(s["text"])
        sureler.append((time.perf_counter() - t0) * 1000)
        gercek.append(s["label"])
        tahmin.append(sonuc.etiket)
        if sonuc.etiket != s["label"]:
            hatalar.append({**{k: s.get(k, "") for k in ("id", "text", "label", "difficulty", "aug_type", "note")},
                            "tahmin": sonuc.etiket,
                            "eslesen_kurallar": "; ".join(f"{c}:{k}" for c, k in sonuc.eslesmeler)})
    acc, mf1, sinif_m, kar, ya, kr = metrikler(gercek, tahmin)
    md = [f"# Yöntem 1 – Anahtar kelime filtreleme: {baslik}", "",
          f"- Örnek sayısı: **{len(gercek)}**",
          f"- Accuracy: **{acc:.3f}**",
          f"- Macro-F1: **{mf1:.3f}**",
          f"- Yanlış alarm oranı (NORMAL → riskli): **{ya:.3f}**",
          f"- Kaçırılan riskli içerik oranı (riskli → NORMAL): **{kr:.3f}**",
          f"- Ortalama analiz süresi: **{sum(sureler)/len(sureler):.3f} ms** (bilgisayarda, Python)", "",
          "| Sınıf | Precision | Recall | F1 | Destek |", "|---|---|---|---|---|"]
    for c in SINIFLAR:
        p, r, f, n = sinif_m[c]
        md.append(f"| {c} | {p:.3f} | {r:.3f} | {f:.3f} | {n} |")
    md += ["", "## Karışıklık matrisi (satır: gerçek, sütun: tahmin)", "",
           "| gerçek \\ tahmin | " + " | ".join(SINIFLAR) + " |", "|---" * (len(SINIFLAR) + 1) + "|"]
    for g in SINIFLAR:
        md.append(f"| {g} | " + " | ".join(str(kar[(g, t)]) for t in SINIFLAR) + " |")
    zor = [(g, t) for s, g, t in zip(satirlar, gercek, tahmin) if s.get("difficulty") == "zor"]
    if zor:
        md += ["", f"Zor örneklerde doğruluk: **{sum(g == t for g, t in zor)/len(zor):.3f}** ({len(zor)} örnek)"]
    Path(yol_md).write_text("\n".join(md) + "\n", encoding="utf-8")
    with open(yol_hata, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["id", "text", "label", "tahmin", "difficulty", "aug_type", "note", "eslesen_kurallar"])
        w.writeheader()
        w.writerows(hatalar)
    print("\n".join(md))


def ikili_rapor(baslik, satirlar, sinif, yol_md, yol_hata):
    """İki sınıflı değerlendirme: NORMAL / RISKLI (OffensEval-TR: NOT / OFF)."""
    tp = fp = fn = tn = 0
    hatalar, sureler = [], []
    for s in satirlar:
        g = "NORMAL" if s["label"].upper() in ("NORMAL", "NOT", "0") else "RISKLI"
        t0 = time.perf_counter()
        sonuc = sinif.siniflandir(s["text"])
        sureler.append((time.perf_counter() - t0) * 1000)
        t = "NORMAL" if sonuc.etiket == "NORMAL" else "RISKLI"
        tp += g == t == "RISKLI"; tn += g == t == "NORMAL"
        fp += g == "NORMAL" and t == "RISKLI"; fn += g == "RISKLI" and t == "NORMAL"
        if g != t:
            hatalar.append({"id": s.get("id", ""), "text": s["text"], "label": g, "tahmin": t,
                            "eslesen_kurallar": "; ".join(f"{c}:{k}" for c, k in sonuc.eslesmeler)})
    n = tp + tn + fp + fn
    p = tp / (tp + fp) if tp + fp else 0
    r = tp / (tp + fn) if tp + fn else 0
    f1 = 2 * p * r / (p + r) if p + r else 0
    pn = tn / (tn + fn) if tn + fn else 0
    rn = tn / (tn + fp) if tn + fp else 0
    f1n = 2 * pn * rn / (pn + rn) if pn + rn else 0
    md = [f"# Yöntem 1 – İki sınıflı değerlendirme: {baslik}", "",
          f"- Örnek sayısı: **{n}** (riskli: {tp + fn}, normal: {tn + fp})",
          f"- Accuracy: **{(tp + tn) / n:.3f}**",
          f"- RISKLI → Precision **{p:.3f}**, Recall **{r:.3f}**, F1 **{f1:.3f}**",
          f"- Macro-F1: **{(f1 + f1n) / 2:.3f}**",
          f"- Yanlış alarm oranı: **{fp / (tn + fp) if tn + fp else 0:.3f}**",
          f"- Kaçırılan riskli içerik oranı: **{fn / (tp + fn) if tp + fn else 0:.3f}**",
          f"- Ortalama analiz süresi: **{sum(sureler) / len(sureler):.3f} ms**", "",
          "| gerçek \\ tahmin | NORMAL | RISKLI |", "|---|---|---|",
          f"| NORMAL | {tn} | {fp} |", f"| RISKLI | {fn} | {tp} |"]
    Path(yol_md).write_text("\n".join(md) + "\n", encoding="utf-8")
    with open(yol_hata, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=["id", "text", "label", "tahmin", "eslesen_kurallar"])
        w.writeheader()
        w.writerows(hatalar)
    print("\n".join(md))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--bolum", default="test", choices=["train", "val", "test", "hepsi"])
    ap.add_argument("--csv", help="text,label sütunlu başka bir CSV")
    ap.add_argument("--metin", help="tek bir cümleyi sınıflandır")
    ap.add_argument("--ikili", action="store_true", help="NORMAL / RISKLI olarak değerlendir")
    a = ap.parse_args()
    sinif = AnahtarKelimeSiniflandirici.dosyadan()
    if a.metin:
        s = sinif.siniflandir(a.metin)
        print(s.etiket, s.eslesmeler)
        return
    if a.csv:
        yol, baslik = Path(a.csv), Path(a.csv).stem
    elif a.bolum == "hepsi":
        yol, baslik = KOK / "dataset/dijitalkalkan_v1.csv", "tum_veri"
    else:
        yol, baslik = KOK / f"dataset/splits/{a.bolum}.csv", f"{a.bolum}_bolumu"
    satirlar = list(csv.DictReader(open(yol, encoding="utf-8")))
    out = KOK / "evaluation/sonuclar"
    out.mkdir(exist_ok=True)
    if a.ikili:
        ikili_rapor(baslik, satirlar, sinif, out / f"yontem1_{baslik}_ikili.md", out / f"yontem1_{baslik}_ikili_hatalar.csv")
    else:
        rapor(baslik, satirlar, sinif, out / f"yontem1_{baslik}.md", out / f"yontem1_{baslik}_hatalar.csv")


if __name__ == "__main__":
    main()
