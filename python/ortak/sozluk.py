"""
Yöntem 1: Anahtar kelime (sözlük) tabanlı sınıflandırıcı.
Sözlük dosyası: app/src/main/assets/lexicon/tr_risk_lexicon.txt (Android ile ortak)
"""
from dataclasses import dataclass, field
from pathlib import Path
from .turkce_metin import tokens

ONCELIK = ["TEHDIT", "SIBER_ZORBALIK", "KUFUR", "HAKARET"]   # soldaki baskın
SOZLUK_YOLU = Path(__file__).resolve().parents[2] / "app/src/main/assets/lexicon/tr_risk_lexicon.txt"


@dataclass
class Kural:
    ham: str
    kategori: str
    parcalar: list
    onek: bool          # son kelime * ile bitiyor mu
    harf_duyarli: bool  # ! ile başlıyor mu

    def eslesir_mi(self, kelimeler_sade, kelimeler_tr, i):
        k = kelimeler_tr if self.harf_duyarli else kelimeler_sade
        if i + len(self.parcalar) > len(k):
            return False
        for j, p in enumerate(self.parcalar):
            w = k[i + j]
            son = j == len(self.parcalar) - 1
            if son and self.onek:
                if not w.startswith(p):
                    return False
            elif w != p:
                return False
        return True


@dataclass
class Sonuc:
    etiket: str
    eslesmeler: list = field(default_factory=list)   # (kategori, kural)


class AnahtarKelimeSiniflandirici:
    def __init__(self, sozluk_metni: str):
        self.kurallar, self.istisnalar = [], []
        kategori = None
        for satir in sozluk_metni.splitlines():
            s = satir.strip()
            if not s or s.startswith("#"):
                continue
            if s.startswith("[") and s.endswith("]"):
                kategori = s[1:-1]
                continue
            harf_duyarli = s.startswith("!")
            govde = s[1:] if harf_duyarli else s
            onek = govde.endswith("*")
            govde = govde.rstrip("*")
            parcalar = tokens(govde, fold=not harf_duyarli)
            if not parcalar:
                continue
            kural = Kural(s, kategori, parcalar, onek, harf_duyarli)
            (self.istisnalar if kategori == "ISTISNA" else self.kurallar).append(kural)

    @classmethod
    def dosyadan(cls, yol: Path = SOZLUK_YOLU):
        return cls(Path(yol).read_text(encoding="utf-8"))

    def _istisna_mi(self, sade, tr, i):
        return any(len(x.parcalar) == 1 and x.eslesir_mi(sade, tr, i) for x in self.istisnalar)

    def siniflandir(self, metin: str) -> Sonuc:
        sade, tr = tokens(metin, fold=True), tokens(metin, fold=False)
        bulunan = []
        for i in range(len(sade)):
            for k in self.kurallar:
                if k.eslesir_mi(sade, tr, i):
                    if len(k.parcalar) == 1 and self._istisna_mi(sade, tr, i):
                        continue
                    bulunan.append((k.kategori, k.ham))
        if not bulunan:
            return Sonuc("NORMAL")
        kategoriler = {b[0] for b in bulunan}
        etiket = next(c for c in ONCELIK if c in kategoriler)
        return Sonuc(etiket, bulunan)
