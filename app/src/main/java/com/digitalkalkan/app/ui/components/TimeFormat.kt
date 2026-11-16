package com.digitalkalkan.app.ui.components

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm")

/** Sunucudaki ISO zaman bilgisini telefonun saatine göre "16.11 14:06" biçimine çevirir. */
fun formatServerTime(iso: String): String = runCatching {
    OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(FMT)
}.getOrDefault(iso.take(16))
