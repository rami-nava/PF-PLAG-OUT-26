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
    entities = [MonitoreoResponse::class, TerrenoResponse::class, PlantacionesResponse::class, UsuarioResponse::class, FeedbackPrediccionPendiente::class],
    version = 11,
    exportSchema = false
)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

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
                ).addMigrations(MIGRATION_10_11)
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
    }
}

class Converters {
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
