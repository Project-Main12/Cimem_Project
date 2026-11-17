package com.digitalkalkan.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Veli ↔ çocuk cihazı bağlantısı. */
@Serializable
data class FamilyLink(
    val id: String,
    @SerialName("parent_id") val parentId: String,
    @SerialName("child_id") val childId: String,
    @SerialName("child_label") val childLabel: String,
    @SerialName("created_at") val createdAt: String
)

/** Sunucudaki uyarı kaydı. Mesaj metni YOKTUR. */
@Serializable
data class AlertRow(
    val id: Long,
    @SerialName("child_id") val childId: String,
    val category: String,
    val level: String,
    @SerialName("source_app") val sourceApp: String,
    @SerialName("detected_at") val detectedAt: String,
    @SerialName("read_at") val readAt: String? = null
)

@Serializable
internal data class NewAlert(
    @SerialName("child_id") val childId: String,
    val category: String,
    val level: String,
    @SerialName("source_app") val sourceApp: String,
    val method: String = "ANAHTAR_KELIME"
)

@Serializable
internal data class ProfileInsert(val id: String, val role: String)

@Serializable
internal data class PushTokenRow(
    @SerialName("user_id") val userId: String,
    @SerialName("fcm_token") val token: String
)

@Serializable
internal data class IdRow(val id: Long)

/** Bildirim fonksiyonunun cevabı. */
@Serializable
data class FunctionReply(val sent: Int = 0, val reason: String? = null, val error: String? = null)
