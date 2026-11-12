package com.digitalkalkan.app.permissions

import android.content.Context
import androidx.core.app.NotificationManagerCompat

object NotificationAccess {
    /** Kullanıcı bu uygulamaya "Bildirim erişimi" verdi mi? */
    fun isGranted(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)
}
