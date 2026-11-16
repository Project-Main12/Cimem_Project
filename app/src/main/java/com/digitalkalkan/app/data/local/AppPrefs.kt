package com.digitalkalkan.app.data.local

import android.content.Context

enum class AppRole { VELI, COCUK }

/** Cihazdaki küçük ayarlar: rol ve çocuk cihazının bağlı olup olmadığı. */
class AppPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dijital_kalkan", Context.MODE_PRIVATE)

    var role: AppRole?
        get() = prefs.getString(KEY_ROLE, null)?.let { runCatching { AppRole.valueOf(it) }.getOrNull() }
        set(value) = prefs.edit().putString(KEY_ROLE, value?.name).apply()

    /** Çocuk cihazı bir veliye bağlıysa true (uyarı gönderilsin mi?). */
    var childLinked: Boolean
        get() = prefs.getBoolean(KEY_LINKED, false)
        set(value) = prefs.edit().putBoolean(KEY_LINKED, value).apply()

    private companion object {
        const val KEY_ROLE = "rol"
        const val KEY_LINKED = "cocuk_bagli"
    }
}
