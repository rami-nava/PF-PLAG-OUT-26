package com.example.plag_out

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.BackoffPolicy
import androidx.work.NetworkType
import com.example.plag_out.AlmacenamientoLocal.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 28], application = Application::class)
class PresenceRetryTest {
    @Test fun `lost response retries identical persisted payload and UUID`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "presence-retry-restart"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        val payload = """{"idempotency_key":"same-uuid","fecha_observacion":"2026-09-11","accion":"asociar_ciclo","ciclo_id":7}"""
        db.biofixDao().insert(BiofixPendiente("A", 41, payload))
        val sent = mutableListOf<String>()
        val first = db.biofixDao().get("A",41)!!
        assertEquals(RetryOutcome.RETRY, retryPresence("A", { "A" }, {
            sent.add(first.payload); throw IOException("lost response")
        }, { fail("Must keep pending") }, { fail("Must not require review") }))
        db.close()
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        val recovered = db.biofixDao().get("A",41)!!
        assertEquals(RetryOutcome.DONE, retryPresence("A", { "A" }, {
            sent.add(recovered.payload); 200
        }, { db.presenceRetryDao().deleteManual("A",41,recovered.payload) }, { fail() }))
        assertEquals(listOf(payload,payload),sent)
        assertNull(db.biofixDao().get("A",41))
        db.close(); context.deleteDatabase(name); Unit
    }
    @Test fun `another account never sends pending operation`() = runBlocking {
        assertEquals(RetryOutcome.DONE, retryPresence("A", { "B" }, { error("must not send") }, { fail() }, { fail() }))
    }
    @Test fun `terminal responses require review and transient responses keep pending`() = runBlocking {
        for (code in listOf(400,404,409,410,422)) {
            var review = false
            assertEquals(RetryOutcome.DONE, retryPresence("A", { "A" }, { code }, { fail() }, { review = true }))
            assertTrue(review)
        }
        for (code in listOf(401,403,408,429,500,503)) {
            assertEquals(RetryOutcome.RETRY, retryPresence("A", { "A" }, { code }, { fail() }, { fail() }))
        }
    }
    @Test fun `cancellation preserves pending for WorkManager restart`() = runBlocking {
        try {
            retryPresence("A", { "A" }, { throw CancellationException() }, { fail() }, { fail() })
            fail()
        } catch (_: CancellationException) { }
    }
    @Test fun `old worker cannot remove or reject corrected operation`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).allowMainThreadQueries().build()
        db.biofixDao().insert(BiofixPendiente("A",41,"new-payload"))
        db.feedbackPrediccionDao().insert(FeedbackPrediccionPendiente("A",9,"presente",idempotency_key="new-uuid"))
        db.presenceRetryDao().deleteManual("A",41,"old-payload")
        db.presenceRetryDao().manualState("A",41,"old-payload","requiere_revision")
        db.presenceRetryDao().deleteFeedback("A",9,"old-uuid")
        db.presenceRetryDao().feedbackState("A",9,"old-uuid","requiere_revision")
        assertEquals("pendiente",db.biofixDao().get("A",41)!!.estado)
        assertEquals("new-uuid",db.feedbackPrediccionDao().get("A",9)!!.idempotency_key)
        db.close()
    }
    @Test fun `migration 13 to 14 preserves exact pending request`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "presence-version13"
        context.deleteDatabase(name)
        val original = Room.databaseBuilder(context,AppDatabase::class.java,name).allowMainThreadQueries().build()
        original.biofixDao().insert(BiofixPendiente("A",41,"original-uuid-and-payload"))
        original.close()
        val sqlite = android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,0)
        sqlite.execSQL("CREATE TABLE biofix_old (owner_id TEXT NOT NULL, monitoreo_id INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(owner_id,monitoreo_id))")
        sqlite.execSQL("INSERT INTO biofix_old SELECT owner_id,monitoreo_id,payload FROM biofix_pendiente")
        sqlite.execSQL("DROP TABLE biofix_pendiente")
        sqlite.execSQL("ALTER TABLE biofix_old RENAME TO biofix_pendiente")
        // La base nace con el esquema actual: hay que sacarle a `monitoreos` las columnas que
        // agrega 14->15 para que esa migracion pueda correr al reabrir. Se recrea la tabla porque
        // el SQLite de Robolectric no soporta DROP COLUMN. Va vacia, asi que no hay filas que copiar.
        val esquemaMonitoreos = sqlite.rawQuery("SELECT sql FROM sqlite_master WHERE name = 'monitoreos'", null).use {
            it.moveToFirst(); it.getString(0)
        }
        sqlite.execSQL("DROP TABLE `monitoreos`")
        sqlite.execSQL(
            esquemaMonitoreos
                .replace(", `observaciones` TEXT", "")
        )
        sqlite.version = 13
        sqlite.close()
        val migrated = Room.databaseBuilder(context,AppDatabase::class.java,name)
            .addMigrations(AppDatabase.MIGRATION_13_14, AppDatabase.MIGRATION_14_15).allowMainThreadQueries().build()
        val row = migrated.biofixDao().get("A",41)!!
        assertEquals("original-uuid-and-payload",row.payload)
        assertEquals("pendiente",row.estado)
        migrated.close(); context.deleteDatabase(name); Unit
    }
    @Test fun `automatic work waits for network and uses exponential backoff`() {
        val spec = PresenceRetryScheduler.request("A","manual",41,"uuid").workSpec
        assertEquals(NetworkType.CONNECTED,spec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL,spec.backoffPolicy)
        assertEquals(30000L,spec.initialDelay)
        assertEquals("uuid",spec.input.getString("key"))
        assertNull(spec.input.getString("token"))
    }
}
