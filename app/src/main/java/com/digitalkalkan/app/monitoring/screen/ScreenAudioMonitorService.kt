package com.digitalkalkan.app.monitoring.screen

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.digitalkalkan.app.MainActivity
import com.digitalkalkan.app.data.local.db.EventRecorder
import com.digitalkalkan.app.domain.alerts.AlertSender
import com.digitalkalkan.app.domain.analysis.AnalyzerProvider
import com.digitalkalkan.app.domain.model.AnalysisResult
import com.digitalkalkan.app.monitoring.audio.VoskModelManager
import com.digitalkalkan.app.monitoring.notification.CapturedNotificationLog
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer

/**
 * Ekran (OCR) ve ses analizi – kullanıcının onayladığı MediaProjection oturumu ile.
 * GİZLİLİK: Ekran görüntüsü, okunan yazı ve ses KAYDEDİLMEZ; yalnızca analiz sonucu kullanılır.
 * Servis çalışırken "ekran analiz ediliyor" bildirimi her zaman görünür.
 */
class ScreenAudioMonitorService : Service() {

    companion object {
        private const val TAG = "DK-Ekran"
        const val EXTRA_CODE = "kod"
        const val EXTRA_DATA = "veri"
        const val EXTRA_OCR = "ocr"
        const val EXTRA_AUDIO = "ses"
        const val SOURCE_SCREEN = "dijitalkalkan.ekran"
        const val SOURCE_AUDIO = "dijitalkalkan.ses"
        private const val CHANNEL = "dk_ekran_ses"
        private const val INTERVAL_MS = 4_000L

        /** Arayüzün gösterdiği durum. */
        val running = mutableStateOf(false)
        val status = mutableStateOf("Kapalı")

        fun start(context: Context, resultCode: Int, data: Intent, ocr: Boolean, audio: Boolean) {
            val i = Intent(context, ScreenAudioMonitorService::class.java)
                .putExtra(EXTRA_CODE, resultCode).putExtra(EXTRA_DATA, data)
                .putExtra(EXTRA_OCR, ocr).putExtra(EXTRA_AUDIO, audio)
            ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenAudioMonitorService::class.java))
        }
    }

    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private val main = Handler(Looper.getMainLooper())
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val recentTexts = LinkedHashSet<String>()
    private var audioThread: Thread? = null
    @Volatile private var audioRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data: Intent? = intent?.getParcelableExtra(EXTRA_DATA)
        val ocr = intent?.getBooleanExtra(EXTRA_OCR, true) ?: true
        val audio = intent?.getBooleanExtra(EXTRA_AUDIO, false) ?: false
        if (data == null) { stopSelf(); return START_NOT_STICKY }

        // Android 14+: getMediaProjection'dan ÖNCE mediaProjection türünde ön plan servisi başlatılmalı
        startForeground(1201, notification(ocr, audio), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)

        val mpm = getSystemService(MediaProjectionManager::class.java)
        val p = mpm.getMediaProjection(code, data) ?: run { stopSelf(); return START_NOT_STICKY }
        projection = p
        worker = HandlerThread("dk-ekran").also { it.start() }
        handler = Handler(worker!!.looper)
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { main.post { stopSelf() } }
        }, handler)

        if (ocr) startOcr(p)
        if (audio) startAudio(p)
        running.value = true
        status.value = listOfNotNull(if (ocr) "Ekran" else null, if (audio) "Ses" else null).joinToString(" + ") + " analizi açık"
        return START_NOT_STICKY
    }

    // ------------------------------------------------------------------ OCR
    private fun startOcr(p: MediaProjection) {
        val bounds = getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        val w = bounds.width() / 2
        val h = bounds.height() / 2
        val dpi = resources.displayMetrics.densityDpi / 2
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        display = p.createVirtualDisplay("dk-ocr", w, h, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler)
        handler?.postDelayed(object : Runnable {
            override fun run() {
                runCatching { captureAndAnalyze() }.onFailure { Log.w(TAG, "OCR hatası: ${it.message}") }
                handler?.postDelayed(this, INTERVAL_MS)
            }
        }, INTERVAL_MS)
    }

    private fun captureAndAnalyze() {
        val image = reader?.acquireLatestImage() ?: return
        val bitmap = image.use {
            val plane = it.planes[0]
            val rowPadding = plane.rowStride - plane.pixelStride * it.width
            val full = Bitmap.createBitmap(it.width + rowPadding / plane.pixelStride, it.height, Bitmap.Config.ARGB_8888)
            full.copyPixelsFromBuffer(plane.buffer)
            Bitmap.createBitmap(full, 0, 0, it.width, it.height).also { if (it !== full) full.recycle() }
        }
        val text = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
        bitmap.recycle()
        // Her yazı bloğu ayrı analiz edilir (sohbet balonları, yorumlar); aynı yazı tekrar analiz edilmez
        val engine = AnalyzerProvider.engine(this)
        for (block in text.textBlocks) {
            val t = block.text.replace('\n', ' ').trim()
            if (t.length < 3 || !remember(t)) continue
            val r = engine.analyze(t)
            if (r.isRisky) report(SOURCE_SCREEN, "EKRAN", r)
        }
    }

    /** Son görülen yazıları hatırlar (aynı ekran tekrar tekrar uyarı üretmesin). */
    private fun remember(t: String): Boolean {
        if (t in recentTexts) return false
        recentTexts.add(t)
        while (recentTexts.size > 200) recentTexts.remove(recentTexts.first())
        return true
    }

    // ----------------------------------------------------------------- SES
    private fun startAudio(p: MediaProjection) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Ses izni yok"); return
        }
        if (!VoskModelManager.isReady(this)) { Log.w(TAG, "Vosk modeli indirilmemiş"); return }
        val config = AudioPlaybackCaptureConfiguration.Builder(p)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val rate = 16_000
        val bufSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2
        val record = try {
            AudioRecord.Builder()
                .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
                .setBufferSizeInBytes(bufSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()
        } catch (e: SecurityException) { Log.w(TAG, "Ses yakalanamadı: ${e.message}"); return }
        audioRunning = true
        audioThread = Thread {
            val model = Model(VoskModelManager.modelDir(this).absolutePath)
            val rec = Recognizer(model, rate.toFloat())
            val buf = ByteArray(4096)
            record.startRecording()
            try {
                while (audioRunning) {
                    val n = record.read(buf, 0, buf.size)
                    if (n > 0 && rec.acceptWaveForm(buf, n)) handleSpeech(JSONObject(rec.result).optString("text"))
                }
                handleSpeech(JSONObject(rec.finalResult).optString("text"))
            } finally {
                record.stop(); record.release(); rec.close(); model.close()
            }
        }.also { it.name = "dk-ses"; it.start() }
    }

    private fun handleSpeech(text: String) {
        if (text.isBlank()) return
        val r = AnalyzerProvider.engine(this).analyze(text)
        if (r.isRisky) report(SOURCE_AUDIO, "SES", r)
    }

    // ------------------------------------------------------------- ortak
    private fun report(sourceApp: String, source: String, r: AnalysisResult) {
        val sent = AlertSender.maybeSend(this, sourceApp, r)
        EventRecorder.record(this, source, sourceApp, r, sent)
        main.post { CapturedNotificationLog.add(sourceApp, result = r, sentToParent = sent) }
        Log.i(TAG, "$source: ${r.category} (${r.method})")
    }

    private fun notification(ocr: Boolean, audio: Boolean): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Ekran ve ses analizi", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val what = listOfNotNull(if (ocr) "ekran" else null, if (audio) "ses" else null).joinToString(" ve ")
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Dijital Kalkan $what analizi açık")
            .setContentText("Görüntü, yazı ve ses kaydedilmez. Kapatmak için uygulamayı açın.")
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    override fun onDestroy() {
        audioRunning = false
        handler?.removeCallbacksAndMessages(null)
        display?.release()
        reader?.close()
        projection?.stop()
        worker?.quitSafely()
        running.value = false
        status.value = "Kapalı"
        super.onDestroy()
    }
}
