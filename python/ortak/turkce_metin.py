"""
Türkçe metin normalleştirme (Dijital Kalkan).

Android tarafındaki Kotlin kodu (TurkishTextNormalizer.kt) ile AYNI kuralları uygular.
Bir tarafta kural değişirse diğer tarafta da değiştirilmelidir.

Adımlar:
 1. Türkçe küçük harf (I -> ı, İ -> i)
 2. Sembol/rakam hileleri: @ptal, s4lak, $erefsiz, 0rospu  (yalnızca harfe bitişikse)
 3. Harf arası yıldız sansürü silinir: or*spu -> orspu
 4. Harf ve rakam dışındaki her şey boşluk olur, kelimelere bölünür
 5. Tek harflik art arda kelimeler birleştirilir: "s.a.l.a.k" -> "salak"
 6. Harf tekrarları teke indirilir: "salaaak" -> "salak", "eşşek" -> "eşek"
 7. (İsteğe bağlı) Türkçe harfler sadeleştirilir: ş->s, ı->i ... "gerizekalı" -> "gerizekali"
"""
import re

_LEET = {"@": "a", "4": "a", "3": "e", "1": "i", "!": "i", "0": "o",
         "5": "s", "$": "s", "7": "t", "€": "e"}
_FOLD = str.maketrans({"ç": "c", "ğ": "g", "ı": "i", "ö": "o", "ş": "s", "ü": "u",
                       "â": "a", "î": "i", "û": "u"})


def tr_lower(text: str) -> str:
    return text.replace("I", "ı").replace("İ", "i").lower()


def tr_upper(text: str) -> str:
    return text.replace("i", "İ").replace("ı", "I").upper()


def ascii_fold(text: str) -> str:
    return text.translate(_FOLD)


def _map_leet(text: str) -> str:
    out = []
    n = len(text)
    for i, ch in enumerate(text):
        prev_alpha = i > 0 and text[i - 1].isalpha()
        next_alpha = i + 1 < n and text[i + 1].isalpha()
        if ch == "*" and prev_alpha and next_alpha:
            continue                                  # or*spu -> orspu
        if ch in _LEET and (prev_alpha or next_alpha):
            out.append(_LEET[ch])
        else:
            out.append(ch)
    return "".join(out)


def _squeeze(token: str) -> str:
    return re.sub(r"(.)\1+", r"\1", token)


def tokens(text: str, fold: bool = True) -> list[str]:
    t = _map_leet(tr_lower(text))
    raw = [w for w in re.split(r"[^\w]+|_", t) if w]
    merged, buf = [], []
    for w in raw + [""]:
        if len(w) == 1 and w.isalpha():
            buf.append(w)
            continue
        if buf:
            merged.append("".join(buf)) if len(buf) >= 2 else merged.extend(buf)
            buf = []
        if w:
            merged.append(w)
    result = [_squeeze(w) for w in merged]
    if fold:
        result = [ascii_fold(w) for w in result]
    return result
