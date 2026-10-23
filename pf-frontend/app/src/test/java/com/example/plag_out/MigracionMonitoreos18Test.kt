package com.example.plag_out

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionPendiente
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class MigracionMonitoreos18Test {

    @Test
    fun `la cache de monitoreos se recrea vacia, se invalida y la cola pendiente se conserva`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "monitoreos-18-migration-test"
        context.deleteDatabase(name)
        val actual = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        actual.feedbackPrediccionDao().insert(FeedbackPrediccionPendiente("owner", 41, "presente", idempotency_key = "same-uuid"))
        actual.close()

        // Versión 17: la tabla de monitoreos todavía tenía los escalares legados.
        val sqlite = SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE)
        sqlite.execSQL("DROP TABLE `monitoreos`")
        sqlite.execSQL(
            "CREATE TABLE `monitoreos` (`monitoreo_id` INTEGER NOT NULL, `plantacion_id` INTEGER NOT NULL, " +
                "`terreno_id` INTEGER NOT NULL, `terreno_nombre` TEXT NOT NULL, `cultivo_nombre` TEXT NOT NULL, " +
                "`plaga_nombre` TEXT NOT NULL, `gdd_diario` REAL NOT NULL, `gdd_acumulado` REAL NOT NULL, " +
                "`gdd_objetivo` REAL NOT NULL, `progreso` REAL NOT NULL, `nivel_alerta` INTEGER NOT NULL, " +
                "`fecha_actualizacion` TEXT NOT NULL, PRIMARY KEY(`monitoreo_id`))"
        )
        sqlite.execSQL(
            "INSERT INTO `monitoreos` VALUES (1, 1, 1, 'Lote', 'Maíz', 'Chicharrita', 0, 0, 100, 0, 1, '2026-10-01')"
        )
        sqlite.version = 17
        sqlite.close()
        CacheTracker.marcarConsultado(context, CacheTracker.MONITOREOS)

        val migrada = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.migracion17a18(context), AppDatabase.MIGRATION_18_19)
            .allowMainThreadQueries()
            .build()

        assertEquals(emptyList<MonitoreoResponse>(), migrada.monitoreoDao().getAll())
        assertNotNull(migrada.feedbackPrediccionDao().get("owner", 41))
        // Sin la marca, la próxima pantalla vuelve a pedir los monitoreos al backend.
        assertFalse(CacheTracker.yaConsultado(context, CacheTracker.MONITOREOS))
        migrada.close()
    }
}
