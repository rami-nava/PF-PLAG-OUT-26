package com.example.plag_out

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.BiofixPendiente
import com.example.plag_out.AlmacenamientoLocal.TratamientoPendiente
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class TratamientoRoomMigrationTest {
    @Test
    fun `la 18 a 19 suma la bandeja de tratamientos sin tocar las otras colas`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "tratamiento-migration-test"
        context.deleteDatabase(name)
        val actual = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        actual.biofixDao().insert(BiofixPendiente("owner", 41, "biofix-pendiente"))
        actual.close()

        // Así quedaba una base de la APK anterior: versión 18, sin la tabla nueva.
        SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { sqlite ->
            sqlite.execSQL("DROP TABLE tratamiento_pendiente")
            sqlite.version = 18
        }

        // Room valida que lo que crea la migración sea exactamente la entidad.
        val migrada = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_18_19).allowMainThreadQueries().build()
        assertEquals("biofix-pendiente", migrada.biofixDao().get("owner", 41)?.payload)
        val dao = migrada.tratamientoPendienteDao()
        dao.guardar(TratamientoPendiente("k1", "owner", 41, payload = "{}"))
        assertEquals("pendiente", dao.get("owner", "k1")?.estado)
        migrada.close()
        context.deleteDatabase(name)
        Unit
    }
}
