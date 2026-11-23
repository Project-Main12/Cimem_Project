package com.digitalkalkan.app.monitoring.audio

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Vosk Türkçe konuşma tanıma modeli (vosk-model-small-tr-0.3, ~35 MB, Apache 2.0).
 * APK'yı büyütmemek için ilk kullanımda bir kez indirilir; sonra tamamen çevrimdışı çalışır.
 */
object VoskModelManager {
    private const val URL_ZIP = "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip"
    private const val DIR = "vosk-model-small-tr-0.3"

    fun modelDir(context: Context) = File(context.filesDir, "vosk/$DIR")

    fun isReady(context: Context) = File(modelDir(context), "HAZIR").exists()

    /** İndirir ve açar. [progress] 0..100 ile çağrılır. Arka plan iş parçacığında çağrılmalı. */
    fun download(context: Context, progress: (Int) -> Unit) {
        val root = File(context.filesDir, "vosk").apply { mkdirs() }
        val conn = URL(URL_ZIP).openConnection() as HttpURLConnection
        conn.connectTimeout = 20_000
        conn.readTimeout = 60_000
        val total = conn.contentLengthLong.takeIf { it > 0 } ?: 40_000_000L
        var read = 0L
        val counting = object : java.io.FilterInputStream(conn.inputStream) {
            override fun read(b: ByteArray, off: Int, len: Int): Int =
                super.read(b, off, len).also { if (it > 0) { read += it; progress((read * 100 / total).toInt().coerceAtMost(99)) } }
        }
        ZipInputStream(counting.buffered()).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val out = File(root, e.name)
                if (!out.canonicalPath.startsWith(root.canonicalPath)) continue   // güvenli açma
                if (e.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zip.copyTo(it) }
                }
            }
        }
        File(modelDir(context), "HAZIR").writeText("ok")
        progress(100)
    }
}
