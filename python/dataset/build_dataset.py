"""
Dijital Kalkan – Sentetik veri seti oluşturucu (v1)

Çalıştırma (python klasöründen):
    python dataset/build_dataset.py

Girdi : dataset/kaynak/temel_ornekler.txt, dataset/kaynak/ek_kategoriler.txt
Çıktı : dataset/dijitalkalkan_v1.csv            (tamamı)
        dataset/splits/{train,val,test}.csv      (%70 / %15 / %15)
        dataset/ek_kategoriler_v1.csv
        dataset/ISTATISTIK.md

Bilimsel notlar:
- Her temel cümleden 2 "yazım varyantı" üretilir (Türkçe karaktersiz, harf uzatma,
  sembol, kısaltma, yazım hatası...). Gerçek sohbet dilini taklit eder.
- Bir cümle ve varyantları AYNI bölüme (train/val/test) düşer (group_id).
  Böylece model test cümlesinin bir varyantını eğitimde görüp "ezberleyemez".
- Normal cümlelere de varyant uygulanır; model "yazım bozukluğu = risk" öğrenmesin.
"""
import csv
import random
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ortak.turkce_metin import ascii_fold, tr_lower, tr_upper  # noqa: E402

KOK = Path(__file__).resolve().parent
TOHUM = 42
VARYANT_SAYISI = 2
ORAN = {"train": 0.70, "val": 0.15, "test": 0.15}
ANA_SINIFLAR = ["NORMAL", "KUFUR", "HAKARET", "SIBER_ZORBALIK", "TEHDIT"]


# ------------------------------------------------------------------ okuma
def oku(yol: Path):
    kayitlar, sinif = [], None
    for no, satir in enumerate(yol.read_text(encoding="utf-8").splitlines(), 1):
        s = satir.strip()
        if not s or s.startswith("#"):
            continue
        m = re.fullmatch(r"\[(\w+)\]", s)
        if m:
            sinif = m.group(1)
            continue
        parca = [p.strip() for p in s.split("|")]
        parca += [""] * (5 - len(parca))
        metin, alt, baglam, zorluk, notu = parca[:5]
        if not metin or sinif is None:
            raise ValueError(f"{yol.name}:{no} hatalı satır: {satir}")
        kayitlar.append(dict(text=metin, label=sinif, subtype=alt, context=baglam,
                             difficulty=zorluk or "orta", note=notu))
    return kayitlar


# ------------------------------------------------------------- varyantlar
SOHBET = [  # (kalıp, yerine) – konuşma dili kısaltmaları
    (r"\bbir\b", "bi"), (r"\bdeğil\b", "diil"), (r"\btamam\b", "tmm"), (r"\byok\b", "yk"),
    (r"\bneden\b", "niye"), (r"\bmısın\b", "mısn"), (r"\bmisin\b", "misn"),
    (r"\bşey\b", "şy"), (r"\bkardeşim\b", "kardşm"), (r"\bherkes\b", "herks"),
    (r"([a-zçğıöşü]+?)(?:acağım|eceğim)\b", r"\1cam"),   # yapacağım -> yapcam
    (r"([a-zçğıöşü]+?)(?:ıyorum|iyorum|uyorum|üyorum)\b", r"\1iyom"),
    (r"\bben\b", "bn"), (r"\bsen\b", "sn"), (r"\bçok\b", "çk"),
]
SEMBOL = {"a": ["4", "@"], "e": ["3"], "i": ["1"], "o": ["0"], "s": ["$", "5"]}
EMOJI = ["😂", "😡", "🤬", "😤", "!!!", "??", "😒", "👍", "💀", "🙄"]


def _kelime_sec(r, kelimeler, min_len=3):
    adaylar = [i for i, w in enumerate(kelimeler) if len(re.sub(r"\W", "", w)) >= min_len]
    return r.choice(adaylar) if adaylar else None


def v_turkcesiz(t, r):
    return ascii_fold(tr_lower(t))


def v_uzatma(t, r):
    k = t.split()
    i = _kelime_sec(r, k)
    if i is None:
        return None
    w = k[i]
    sesliler = [j for j, c in enumerate(w) if c.lower() in "aeıioöuü"]
    if not sesliler:
        return None
    j = sesliler[-1]
    k[i] = w[:j + 1] + w[j] * r.randint(2, 4) + w[j + 1:]
    return " ".join(k)


def v_sembol(t, r):
    k = t.split()
    i = _kelime_sec(r, k, 4)
    if i is None:
        return None
    w = k[i]
    yerler = [j for j, c in enumerate(w) if c in SEMBOL]
    if not yerler:
        return None
    j = r.choice(yerler)
    k[i] = w[:j] + r.choice(SEMBOL[w[j]]) + w[j + 1:]
    return " ".join(k)


def v_sohbet(t, r):
    s = tr_lower(t)
    for kalip, yeni in SOHBET:
        s = re.sub(kalip, yeni, s)
    return s if s != tr_lower(t) else None


def v_yazim_hatasi(t, r):
    k = t.split()
    i = _kelime_sec(r, k, 5)
    if i is None:
        return None
    w = list(k[i])
    j = r.randint(1, len(w) - 2)
    if r.random() < 0.5:
        w[j], w[j + 1] = w[j + 1], w[j]      # harf yer değiştirme
    else:
        del w[j]                              # harf düşürme
    k[i] = "".join(w)
    return " ".join(k)


def v_buyuk_emoji(t, r):
    s = tr_upper(t) if r.random() < 0.5 else t
    return f"{s} {r.choice(EMOJI)}"


def v_noktali(t, r):
    k = t.split()
    i = _kelime_sec(r, k, 4)
    if i is None:
        return None
    w = re.sub(r"\W", "", k[i])
    k[i] = r.choice([".", " ", "-"]).join(w)
    return " ".join(k)


VARYANTLAR = {
    "turkce_karaktersiz": v_turkcesiz, "harf_uzatma": v_uzatma, "sembol_rakam": v_sembol,
    "sohbet_kisaltma": v_sohbet, "yazim_hatasi": v_yazim_hatasi,
    "buyuk_harf_emoji": v_buyuk_emoji, "harf_ayirma": v_noktali,
}


def varyant_uret(kayit, r):
    sonuc, gorulen = [], {kayit["text"]}
    adlar = list(VARYANTLAR)
    r.shuffle(adlar)
    for ad in adlar:
        if len(sonuc) >= VARYANT_SAYISI:
            break
        yeni = VARYANTLAR[ad](kayit["text"], r)
        if yeni and yeni not in gorulen:
            gorulen.add(yeni)
            sonuc.append((ad, yeni))
    return sonuc


# --------------------------------------------------------------- bölme
def grup_bol(gruplar_sinif, r):
    """Her sınıf içinde grupları %70/%15/%15 dağıt (tabakalı, grup bazlı)."""
    atama = {}
    for sinif, gruplar in gruplar_sinif.items():
        g = sorted(gruplar)
        r.shuffle(g)
        n = len(g)
        n_test = round(n * ORAN["test"])
        n_val = round(n * ORAN["val"])
        for i, gid in enumerate(g):
            atama[gid] = "test" if i < n_test else "val" if i < n_test + n_val else "train"
    return atama


ALANLAR = ["id", "group_id", "text", "label", "subtype", "context", "difficulty",
           "augmented", "aug_type", "split", "note", "source"]


def yaz(yol, satirlar):
    yol.parent.mkdir(parents=True, exist_ok=True)
    with open(yol, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=ALANLAR)
        w.writeheader()
        w.writerows(satirlar)


def main():
    r = random.Random(TOHUM)
    temel = oku(KOK / "kaynak/temel_ornekler.txt")
    tekrar = [m for m, c in Counter(k["text"] for k in temel).items() if c > 1]
    if tekrar:
        print("UYARI – tekrar eden cümleler:", tekrar)

    satirlar, gruplar = [], defaultdict(list)
    for gi, k in enumerate(temel, 1):
        gid = f"G{gi:04d}"
        gruplar[k["label"]].append(gid)
        satirlar.append({**k, "group_id": gid, "augmented": 0, "aug_type": "", "source": "sentetik"})
        for ad, metin in varyant_uret(k, r):
            satirlar.append({**k, "text": metin, "group_id": gid, "augmented": 1,
                             "aug_type": ad, "source": "sentetik_varyant"})

    atama = grup_bol(gruplar, r)
    for i, s in enumerate(satirlar, 1):
        s["id"] = f"DK{i:05d}"
        s["split"] = atama[s["group_id"]]

    yaz(KOK / "dijitalkalkan_v1.csv", satirlar)
    for b in ORAN:
        yaz(KOK / f"splits/{b}.csv", [s for s in satirlar if s["split"] == b])

    ek = oku(KOK / "kaynak/ek_kategoriler.txt")
    for i, s in enumerate(ek, 1):
        s.update(id=f"EK{i:04d}", group_id=f"E{i:04d}", augmented=0, aug_type="",
                 split="", source="sentetik")
    yaz(KOK / "ek_kategoriler_v1.csv", ek)

    # ---- istatistik
    tablo = Counter((s["label"], s["split"]) for s in satirlar)
    temel_say = Counter(k["label"] for k in temel)
    zor = Counter(k["label"] for k in temel if k["difficulty"] == "zor")
    md = ["# Veri Seti İstatistikleri – dijitalkalkan_v1", "",
          f"Toplam örnek: **{len(satirlar)}**  (temel cümle: {len(temel)}, varyant: {len(satirlar)-len(temel)})",
          "", "| Sınıf | Temel | Zor temel | Toplam | Train | Val | Test |", "|---|---|---|---|---|---|---|"]
    for c in ANA_SINIFLAR:
        top = sum(tablo[(c, b)] for b in ORAN)
        md.append(f"| {c} | {temel_say[c]} | {zor[c]} | {top} | {tablo[(c,'train')]} | {tablo[(c,'val')]} | {tablo[(c,'test')]} |")
    md += ["", "## Bağlam dağılımı (temel cümleler)", ""]
    md += [f"- {b}: {n}" for b, n in Counter(k["context"] for k in temel).most_common()]
    md += ["", "## Alt türler (temel cümleler)", "", "| Sınıf | Alt tür | Sayı |", "|---|---|---|"]
    for (c, a), n in sorted(Counter((k["label"], k["subtype"]) for k in temel).items()):
        md.append(f"| {c} | {a} | {n} |")
    md += ["", "## Varyant türleri", ""]
    md += [f"- {a}: {n}" for a, n in Counter(s["aug_type"] for s in satirlar if s["augmented"]).most_common()]
    md += ["", "## Ek kategoriler (ana sete dahil değil)", ""]
    md += [f"- {c}: {n}" for c, n in Counter(k["label"] for k in ek).items()]
    (KOK / "ISTATISTIK.md").write_text("\n".join(md) + "\n", encoding="utf-8")
    print("\n".join(md[:12]))


if __name__ == "__main__":
    main()
