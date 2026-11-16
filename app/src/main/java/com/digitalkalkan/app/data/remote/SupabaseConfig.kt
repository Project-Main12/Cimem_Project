package com.digitalkalkan.app.data.remote

/**
 * Supabase proje bilgileri.
 * Publishable key uygulamaya gömülmek için tasarlanmıştır; güvenliği veritabanındaki
 * satır düzeyinde güvenlik (RLS) kuralları sağlar. Secret/service_role anahtarı ASLA buraya konmaz.
 */
object SupabaseConfig {
    const val URL = "https://nreqibswrqqhlsqweeue.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_499WDodoNQreJHL1vUS8rA_92FXxv85"
}
