"""
OffensEval-TR 2020 veri setini indirip Dijital Kalkan biçimine çevirir.

Kaynak : Çöltekin (2020), "A Corpus of Turkish Offensive Language on Social Media"
         Hugging Face: coltekin/offenseval2020_tr
İçerik : Türkçe tweetler; etiket NOT (saldırgan değil) / OFF (saldırgan)

Kullanım (internet gerekir, python klasöründen):
    pip install datasets
    python dataset/offenseval_indir.py

Çıktı (DEPOYA EKLENMEZ, .gitignore'da):
    dataset/dis_veri/offenseval_tr_train.csv
    dataset/dis_veri/offenseval_tr_test.csv
    Sütunlar: id, text, label  (label: NORMAL veya RISKLI)
"""
import csv
from pathlib import Path

HEDEF = Path(__file__).resolve().parent / "dis_veri"


def main():
    try:
        from datasets import load_dataset
    except ImportError:
        raise SystemExit("Önce kurun:  pip install datasets")

    print("İndiriliyor: coltekin/offenseval2020_tr ...")
    ds = load_dataset("coltekin/offenseval2020_tr")
    HEDEF.mkdir(exist_ok=True)

    for bolum in ds:
        veri = ds[bolum]
        sutunlar = veri.column_names
        metin_s = next(c for c in ("tweet", "text") if c in sutunlar)
        etiket_s = next(c for c in ("subtask_a", "label") if c in sutunlar)
        isimler = getattr(veri.features[etiket_s], "names", None)   # ör. ['NOT', 'OFF']

        yol = HEDEF / f"offenseval_tr_{bolum}.csv"
        sayac = {"NORMAL": 0, "RISKLI": 0}
        with open(yol, "w", encoding="utf-8", newline="") as f:
            w = csv.writer(f)
            w.writerow(["id", "text", "label"])
            for i, ornek in enumerate(veri):
                ham = ornek[etiket_s]
                ad = isimler[ham] if isimler and isinstance(ham, int) else str(ham)
                etiket = "NORMAL" if ad.upper() in ("NOT", "0") else "RISKLI"
                sayac[etiket] += 1
                w.writerow([ornek.get("id", i), ornek[metin_s], etiket])
        print(f"{yol.name}: {len(veri)} örnek  {sayac}")

    print("\nŞimdi Yöntem 1'i bu veride ölçün:")
    print("  python evaluation/keyword_baseline.py --csv dataset/dis_veri/offenseval_tr_test.csv --ikili")


if __name__ == "__main__":
    main()
