package com.digitalkalkan.app.data.local.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Yerel olay kaydı. GİZLİLİK: Mesaj/ekran/ses METNİ YOKTUR.
 * Araştırma ölçümleri için yalnızca kaynak, uygulama, sonuç, yöntem ve süre tutulur.
 */
@Entity(tableName = "olaylar")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "zaman") val timeMillis: Long,
    /** BILDIRIM, EKRAN, SES, METIN_DENE */
    @ColumnInfo(name = "kaynak") val source: String,
    @ColumnInfo(name = "uygulama") val app: String,
    @ColumnInfo(name = "kategori") val category: String,
    @ColumnInfo(name = "seviye") val level: String,
    @ColumnInfo(name = "yontem") val method: String,
    @ColumnInfo(name = "sure_us") val durationMicros: Long,
    @ColumnInfo(name = "guven") val confidence: Float?,
    @ColumnInfo(name = "veliye_gonderildi") val sentToParent: Boolean
)

data class CategoryCount(
    @ColumnInfo(name = "kategori") val category: String,
    @ColumnInfo(name = "adet") val count: Int
)

data class MethodStat(
    @ColumnInfo(name = "yontem") val method: String,
    @ColumnInfo(name = "adet") val count: Int,
    @ColumnInfo(name = "ort_sure_us") val avgMicros: Double,
    @ColumnInfo(name = "maks_sure_us") val maxMicros: Long
)

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity)

    @Query("SELECT * FROM olaylar ORDER BY zaman DESC LIMIT :limit")
    suspend fun recent(limit: Int = 200): List<EventEntity>

    @Query("SELECT kategori, COUNT(*) AS adet FROM olaylar WHERE zaman >= :since AND kategori != 'NORMAL' GROUP BY kategori")
    suspend fun riskCountsSince(since: Long): List<CategoryCount>

    @Query("SELECT yontem, COUNT(*) AS adet, AVG(sure_us) AS ort_sure_us, MAX(sure_us) AS maks_sure_us FROM olaylar GROUP BY yontem")
    suspend fun methodStats(): List<MethodStat>

    @Query("SELECT kaynak, COUNT(*) AS adet FROM olaylar WHERE zaman >= :since GROUP BY kaynak")
    suspend fun sourceCountsSince(since: Long): List<SourceCount>

    @Query("SELECT * FROM olaylar ORDER BY zaman")
    suspend fun all(): List<EventEntity>

    @Query("DELETE FROM olaylar WHERE zaman < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM olaylar")
    suspend fun clear()
}

data class SourceCount(
    @ColumnInfo(name = "kaynak") val source: String,
    @ColumnInfo(name = "adet") val count: Int
)

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class EventDatabase : RoomDatabase() {
    abstract fun events(): EventDao

    companion object {
        @Volatile private var instance: EventDatabase? = null

        fun get(context: Context): EventDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, EventDatabase::class.java, "dijital_kalkan_olaylar.db")
                .build().also { instance = it }
        }
    }
}
