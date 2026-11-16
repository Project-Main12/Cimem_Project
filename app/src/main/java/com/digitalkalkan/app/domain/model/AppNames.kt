package com.digitalkalkan.app.domain.model

/** Paket adını veliye gösterilecek okunur uygulama adına çevirir. */
object AppNames {
    private val known = mapOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "org.telegram.messenger" to "Telegram",
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube",
        "com.discord" to "Discord",
        "com.roblox.client" to "Roblox",
        "com.mojang.minecraftpe" to "Minecraft",
        "com.google.android.apps.messaging" to "Mesajlar (SMS)",
        "com.samsung.android.messaging" to "Mesajlar (SMS)",
        "com.snapchat.android" to "Snapchat",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.facebook.orca" to "Messenger",
        "com.facebook.katana" to "Facebook",
        "com.twitter.android" to "X (Twitter)",
        "com.supercell.brawlstars" to "Brawl Stars",
        "com.supercell.clashofclans" to "Clash of Clans",
        "com.tencent.ig" to "PUBG Mobile",
        "com.dts.freefireth" to "Free Fire",
        "com.google.android.deskclock" to "Saat"
    )

    fun label(packageName: String): String = known[packageName] ?: packageName
}
