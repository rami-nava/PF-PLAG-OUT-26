package com.example.plag_out.AlmacenamientoLocal

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse
import com.example.plag_out.UsuarioResponse
import java.time.LocalDate
import java.util.Date

@Database(
    entities = [MonitoreoResponse::class, TerrenoResponse::class, PlantacionesResponse::class, UsuarioResponse::class, FeedbackPrediccionPendiente::class, BiofixPendiente::class],
    version = 14,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun presenceRetryDao(): PresenceRetryDao
    abstract fun biofixDao(): BiofixDao
    abstract fun monitoreoDao(): MonitoreoDao
    abstract fun terrenoDao(): TerrenoDao
    abstract fun plantacionDao(): PlantacionDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun feedbackPrediccionDao(): FeedbackPrediccionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "gdd_database"
                ).addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    // Si una versión futura cae en la migración destructiva, también se invalidan
                    // las marcas que describían el caché eliminado.
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            CacheTracker.limpiarTodo(appContext)
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE biofix_pendiente ADD COLUMN estado TEXT NOT NULL DEFAULT 'pendiente'")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feedback_prediccion_pendiente ADD COLUMN biofix_json TEXT")
                db.execSQL("UPDATE feedback_prediccion_pendiente SET estado = 'requiere_revision' WHERE respuesta = 'presente'")
                db.execSQL("ALTER TABLE monitoreos ADD COLUMN estado_seguimiento TEXT")
                db.execSQL("ALTER TABLE monitoreos ADD COLUMN ciclos TEXT")
                db.execSQL("CREATE TABLE biofix_pendiente (owner_id TEXT NOT NULL, monitoreo_id INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(owner_id, monitoreo_id))")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `feedback_prediccion_pendiente` (
                        `owner_id` TEXT NOT NULL,
                        `prediccion_id` INTEGER NOT NULL,
                        `respuesta` TEXT NOT NULL,
                        `idempotency_key` TEXT NOT NULL,
                        `estado` TEXT NOT NULL,
                        `creado_en_ms` INTEGER NOT NULL,
                        PRIMARY KEY(`owner_id`, `prediccion_id`)
                    )""".trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `monitoreos` ADD COLUMN `umbral_alerta_ml` REAL")
                db.execSQL("ALTER TABLE `monitoreos` ADD COLUMN `umbral_alerta_ml_recomendado` REAL")
                db.execSQL("ALTER TABLE `monitoreos` ADD COLUMN `umbral_alerta_ml_efectivo` REAL")
                db.execSQL("ALTER TABLE `monitoreos` ADD COLUMN `modelo_alerta_ml_id` TEXT")
                db.execSQL("ALTER TABLE `monitoreos` ADD COLUMN `horizonte_alerta_ml_dias` INTEGER")
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromCiclos(value: List<com.example.plag_out.GddCicloResponse>?): String? =
        value?.let { com.google.gson.Gson().toJson(it) }
    @TypeConverter
    fun toCiclos(value: String?): List<com.example.plag_out.GddCicloResponse>? = value?.let {
        com.google.gson.Gson().fromJson(it, Array<com.example.plag_out.GddCicloResponse>::class.java).toList()
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()  // "2026-06-11"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }
}
