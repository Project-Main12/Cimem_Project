package com.digitalkalkan.app.domain.analysis

import android.content.Context
import com.digitalkalkan.app.domain.model.AnalysisResult
import com.digitalkalkan.app.domain.model.RiskCategory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * Yöntem 3: İnce ayarlı DistilBERTurk, LiteRT ile tamamen cihaz üzerinde çalışır.
 * Model python/export/yontem3_litert.py ile üretilir (girdi: input_ids, attention_mask [1, maxLen] int32;
 * çıktı: [1, 7] logit).
 */
class BertAnalyzer(model: ByteBuffer, vocab: String, metaJson: String) : TextAnalyzer {

    override val methodName = "DistilBERTurk"

    @Serializable
    private data class Meta(val classes: List<String>, val max_len: Int)

    private val meta = Json { ignoreUnknownKeys = true }.decodeFromString<Meta>(metaJson)
    private val classes = meta.classes.map { RiskCategory.fromKey(it) ?: RiskCategory.NORMAL }
    private val tokenizer = WordPieceTokenizer(vocab, meta.max_len)
    private val interpreter = Interpreter(model, Interpreter.Options().setNumThreads(2))
    private val maskIndex: Int
    private val idsIndex: Int

    init {
        val names = (0 until interpreter.inputTensorCount).map { interpreter.getInputTensor(it).name().lowercase() }
        // Dışa aktarımda adlar "args_0" (input_ids) ve "args_1" (attention_mask) olur
        maskIndex = names.indexOfFirst { "mask" in it || it.endsWith("args_1") }.takeIf { it >= 0 } ?: 1
        idsIndex = if (maskIndex == 0) 1 else 0
    }

    @Synchronized
    fun probabilities(text: String): FloatArray {
        val (ids, mask) = tokenizer.encode(text)
        val inputs = arrayOfNulls<Any>(2)
        inputs[idsIndex] = arrayOf(ids)
        inputs[maskIndex] = arrayOf(mask)
        val out = Array(1) { FloatArray(classes.size) }
        interpreter.runForMultipleInputsOutputs(inputs, mapOf(0 to out))
        val logits = out[0]
        val max = logits.max()
        val e = logits.map { exp((it - max).toDouble()) }
        val sum = e.sum()
        return FloatArray(e.size) { (e[it] / sum).toFloat() }
    }

    override fun analyze(text: String): AnalysisResult {
        val start = System.nanoTime()
        val p = probabilities(text)
        val best = p.indices.maxBy { p[it] }
        return AnalysisResult(
            classes[best], emptyList(), (System.nanoTime() - start) / 1000, methodName,
            classes.indices.associate { classes[it] to p[it] }
        )
    }

    companion object {
        fun fromAssets(context: Context): BertAnalyzer {
            val fd = context.assets.openFd(AnalyzerProvider.BERT_ASSET)
            val buffer = FileInputStream(fd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
            fun read(a: String) = context.assets.open(a).bufferedReader(Charsets.UTF_8).use { it.readText() }
            return BertAnalyzer(buffer, read(AnalyzerProvider.BERT_VOCAB_ASSET), read(AnalyzerProvider.BERT_META_ASSET))
        }
    }
}
