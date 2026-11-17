package com.digitalkalkan.app.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.OffsetDateTime

/** Kullanıcıya gösterilecek, anlaşılır hata. */
class BackendException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Supabase ile tüm iletişim burada. Uygulamanın geri kalanı yalnızca bu nesneyi kullanır.
 * Sunucuya giden tek "içerik" bilgisi: kategori, seviye, uygulama paket adı. Mesaj metni gitmez.
 */
object Backend {

    private val ALERT_COLUMNS = Columns.list("id", "child_id", "category", "level", "source_app", "detected_at", "read_at")

    val client by lazy {
        createSupabaseClient(SupabaseConfig.URL, SupabaseConfig.PUBLISHABLE_KEY) {
            install(Auth)
            install(Postgrest)
        }
    }

    // ------------------------------------------------------------ oturum
    suspend fun awaitSession() = client.auth.awaitInitialization()

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    fun currentEmail(): String? = client.auth.currentUserOrNull()?.email

    suspend fun signOut(): Unit = call { client.auth.signOut() }

    // -------------------------------------------------------------- veli
    suspend fun parentSignUp(email: String, password: String): Unit = call {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        val uid = currentUserId() ?: throw BackendException(
            "Kayıt oluşturuldu ama oturum açılmadı. Supabase panelinde 'Confirm email' ayarı açık olabilir."
        )
        client.from("profiles").insert(ProfileInsert(uid, "parent"))
    }

    suspend fun parentSignIn(email: String, password: String): Unit = call {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun createPairingCode(): String = call {
        client.postgrest.rpc("create_pairing_code").decodeAs<String>()
    }

    suspend fun listLinks(): List<FamilyLink> = call {
        client.from("family_links").select {
            order("created_at", Order.DESCENDING)
        }.decodeList<FamilyLink>()
    }

    suspend fun removeLink(linkId: String): Unit = call {
        client.from("family_links").delete { filter { eq("id", linkId) } }
    }

    suspend fun listAlerts(limit: Long = 100): List<AlertRow> = call {
        client.from("alerts").select(ALERT_COLUMNS) {
            order("created_at", Order.DESCENDING)
            limit(limit)
        }.decodeList<AlertRow>()
    }

    suspend fun markAlertRead(id: Long): Unit = call {
        client.from("alerts").update({ set("read_at", OffsetDateTime.now().toString()) }) {
            filter { eq("id", id) }
        }
    }

    suspend fun savePushToken(token: String): Unit = call {
        val uid = currentUserId() ?: return@call
        client.from("push_tokens").upsert(PushTokenRow(uid, token))
    }

    // ------------------------------------------------------------- çocuk
    /** Çocuk cihazı e-posta istemeden anonim hesapla oturum açar. */
    suspend fun childEnsureSession(): Unit = call {
        awaitSession()
        if (currentUserId() == null) client.auth.signInAnonymously()
    }

    suspend fun redeemPairingCode(code: String, childLabel: String): Unit = call {
        client.postgrest.rpc("redeem_pairing_code", buildJsonObject {
            put("p_code", code.filter { it.isDigit() })
            put("p_child_label", childLabel.trim())
        })
    }

    suspend fun childLink(): FamilyLink? = call {
        val uid = currentUserId() ?: return@call null
        client.from("family_links").select { filter { eq("child_id", uid) } }
            .decodeList<FamilyLink>().firstOrNull()
    }

    suspend fun sendAlert(category: String, level: String, sourceApp: String): Unit = call {
        val uid = currentUserId() ?: throw BackendException("Oturum yok")
        client.from("alerts").insert(NewAlert(uid, category, level, sourceApp.take(120)))
    }

    // ------------------------------------------------------------ yardımcı
    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: BackendException) {
        throw e
    } catch (e: Exception) {
        throw BackendException(friendly(e), e)
    }

    /** Sunucu hatasını Türkçe ve kısa hâle getirir. */
    internal fun friendly(e: Exception): String {
        val raw = e.message.orEmpty()
        val known = listOf(
            "Kod geçersiz veya süresi dolmuş", "Çok fazla deneme", "Veli hesabı çocuk cihazı",
            "Bu hesap veli hesabı değil", "Bu cihaz bir veliye bağlı değil", "veli hesabıyla giriş"
        )
        known.firstOrNull { raw.contains(it, ignoreCase = true) }?.let { k ->
            return raw.substring(raw.indexOf(k)).substringBefore("\n").substringBefore("\"").trim()
        }
        return when {
            raw.contains("Invalid login credentials", true) -> "E-posta veya şifre hatalı."
            raw.contains("already registered", true) || raw.contains("already been registered", true) ->
                "Bu e-posta ile zaten kayıt var. 'Giriş yap' seçeneğini kullanın."
            raw.contains("Password should be", true) || raw.contains("weak", true) ->
                "Şifre en az 6 karakter olmalı."
            raw.contains("Anonymous sign-ins are disabled", true) ->
                "Supabase panelinde 'Allow anonymous sign-ins' ayarı kapalı."
            raw.contains("Unable to resolve host", true) || raw.contains("timeout", true) ||
                raw.contains("UnknownHost", true) -> "İnternet bağlantısı yok."
            raw.contains("Email not confirmed", true) -> "E-posta onaylanmamış. Panelde 'Confirm email' kapatılmalı."
            else -> "Sunucu hatası: " + raw.substringBefore("\n").take(160)
        }
    }
}
