"""
Yöntem 3 modelini telefona aktarır: PyTorch → LiteRT (.tflite), dinamik int8 nicemleme.

Çalıştırma (python klasöründen):  python export/yontem3_litert.py [model_klasoru]
Gerekli: pip install litert-torch transformers  (CPU yeterli)

Çıktılar (uygulama içine):
  app/src/main/assets/models/yontem3_distilberturk.tflite
  app/src/main/assets/models/yontem3_vocab.txt
  app/src/main/assets/models/yontem3_meta.json
Doğrulama: LiteRT çıktısı PyTorch ile karşılaştırılır; Kotlin parçalayıcı testi için referans üretilir.
"""
import csv, json, sys
from pathlib import Path
import numpy as np
import torch
import litert_torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification

KOK = Path(__file__).resolve().parents[1]
MODEL = Path(sys.argv[1]) if len(sys.argv) > 1 else KOK / "training/cikti/yontem3"
ASSETS = KOK.parent / "app/src/main/assets/models"
MAX_LEN = 64


class TanhGelu(torch.nn.Module):
    """GELU'nun tanh yaklaşımı (dönüştürücünün desteklediği sade işlemlerle)."""
    def forward(self, x):
        return 0.5 * x * (1.0 + torch.tanh(0.7978845608028654 * (x + 0.044715 * x * x * x)))


def gelu_degistir(model):
    n = 0
    for mod in model.modules():
        if hasattr(mod, "activation") and hasattr(mod, "lin1"):   # DistilBERT FFN katmanı
            mod.activation = TanhGelu(); n += 1
    return n


class Sarmal(torch.nn.Module):
    """int32 girdi alan, logit döndüren sade model (telefon için)."""
    def __init__(self, m):
        super().__init__(); self.m = m
    def forward(self, input_ids, attention_mask):
        return self.m(input_ids=input_ids.long(), attention_mask=attention_mask.long()).logits


def encode(tok, text):
    e = tok(text, truncation=True, max_length=MAX_LEN, padding="max_length", return_tensors="pt")
    return e["input_ids"].int(), e["attention_mask"].int()


def main():
    tok = AutoTokenizer.from_pretrained(MODEL)
    model = AutoModelForSequenceClassification.from_pretrained(MODEL).eval()
    referans = Sarmal(AutoModelForSequenceClassification.from_pretrained(MODEL).eval()).eval()  # orijinal (karşılaştırma için)
    print("Değiştirilen GELU katmanı:", gelu_degistir(model))
    wrapped = Sarmal(model).eval()
    ids, mask = encode(tok, "seni okul çıkışında bekleyeceğim")

    quant = None
    try:
        from litert_torch.generative.quantize import quant_recipes
        quant = quant_recipes.full_dynamic_recipe()
    except Exception as e:
        print("Nicemleme tarifi bulunamadı, float32 aktarılacak:", e)
    edge = litert_torch.convert(wrapped, (ids, mask), quant_config=quant) if quant else litert_torch.convert(wrapped, (ids, mask))
    ASSETS.mkdir(parents=True, exist_ok=True)
    out = ASSETS / "yontem3_distilberturk.tflite"
    edge.export(str(out))
    print(f"LiteRT modeli: {out} ({out.stat().st_size / 1e6:.1f} MB)")

    # Kelime listesi ve meta
    (ASSETS / "yontem3_vocab.txt").write_text(Path(tok.vocab_file if hasattr(tok, "vocab_file") and tok.vocab_file else "").read_text(encoding="utf-8")
        if getattr(tok, "vocab_file", None) else "\n".join(k for k, _ in sorted(tok.get_vocab().items(), key=lambda x: x[1])) + "\n",
        encoding="utf-8")
    classes = [model.config.id2label[i] for i in range(model.config.num_labels)]
    (ASSETS / "yontem3_meta.json").write_text(json.dumps({"classes": classes, "max_len": MAX_LEN}), encoding="utf-8")

    # Doğrulama: PyTorch ile LiteRT aynı sınıfı veriyor mu? (test bölümünden 100 cümle)
    rows = list(csv.DictReader(open(KOK / "dataset/splits/test.csv", encoding="utf-8")))[:100]
    from ai_edge_litert.interpreter import Interpreter
    it = Interpreter(model_path=str(out)); it.allocate_tensors()
    ins = it.get_input_details(); outs = it.get_output_details()
    ayni, maxfark = 0, 0.0
    for r in rows:
        i, m = encode(tok, r["text"])
        with torch.no_grad():
            pt = referans(i, m).numpy()[0]
        for d in ins:   # girdi adları: args_0 = input_ids, args_1 = attention_mask
            is_mask = "mask" in d["name"].lower() or d["name"].endswith("args_1")
            it.set_tensor(d["index"], (m if is_mask else i).numpy())
        it.invoke()
        lt = it.get_tensor(outs[0]["index"])[0]
        ayni += int(pt.argmax() == lt.argmax()); maxfark = max(maxfark, float(np.abs(pt - lt).max()))
    print(f"PyTorch–LiteRT aynı tahmin: {ayni}/{len(rows)}, en büyük logit farkı: {maxfark:.4f}")
    print("Girdi tensörleri:", [(d["name"], d["shape"].tolist(), str(d["dtype"])) for d in ins])

    # Kotlin WordPiece testi için referans token kimlikleri
    ornek = [r["text"] for r in rows] + ["S.A.L.A.K mısın?? 😂", "İSTANBUL'da ışık", "ödev@okul.com 123"]
    ref = KOK.parent / "app/src/test/resources/yontem3_tokenlar.tsv"
    with open(ref, "w", encoding="utf-8") as f:
        for t in ornek:
            f.write(t.replace("\t", " ") + "\t" + " ".join(map(str, tok(t, add_special_tokens=False)["input_ids"])) + "\n")
    print("Parçalayıcı referansı:", ref)


if __name__ == "__main__":
    main()
