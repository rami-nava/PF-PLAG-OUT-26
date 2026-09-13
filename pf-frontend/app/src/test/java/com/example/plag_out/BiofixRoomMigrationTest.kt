package com.example.plag_out

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class BiofixRoomMigrationTest {
    @Test
    fun `migration preserves pending UUID and requires review for legacy presence`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "biofix-migration-test"
        context.deleteDatabase(name)
        val first = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        first.feedbackPrediccionDao().insert(FeedbackPrediccionPendiente("owner", 41, "presente", idempotency_key="same-uuid"))
        first.close()
        val sqlite = SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE)
        fun recreateWithout(table: String, omitted: Set<String>) {
            val original = sqlite.rawQuery("SELECT sql FROM sqlite_master WHERE name = ?", arrayOf(table)).use { cursor ->
                cursor.moveToFirst(); cursor.getString(0)
            }
            var schema = original.replace("`$table`", "`${table}_old`")
            omitted.forEach { column -> schema = schema.replace(", `$column` TEXT", "") }
            val columns = mutableListOf<String>()
            sqlite.rawQuery("PRAGMA table_info(`$table`)", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val column = cursor.getString(1)
                    if (column !in omitted) columns.add("`$column`")
                }
            }
            sqlite.execSQL(schema)
            val selected = columns.joinToString(",")
            sqlite.execSQL("INSERT INTO `${table}_old` ($selected) SELECT $selected FROM `$table`")
            sqlite.execSQL("DROP TABLE `$table`")
            sqlite.execSQL("ALTER TABLE `${table}_old` RENAME TO `$table`")
        }
        recreateWithout("feedback_prediccion_pendiente", setOf("biofix_json"))
        recreateWithout("monitoreos", setOf("ciclos", "estado_seguimiento"))
        sqlite.execSQL("DROP TABLE biofix_pendiente")
        sqlite.version = 12
        sqlite.close()
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_12_13, AppDatabase.MIGRATION_13_14).allowMainThreadQueries().build()
        val pending = migrated.feedbackPrediccionDao().get("owner",41)!!
        assertEquals("same-uuid",pending.idempotency_key)
        assertEquals("requiere_revision",pending.estado)
        assertNull(pending.biofix_json)
        migrated.biofixDao().insert(BiofixPendiente("owner",41,"exact-payload"))
        assertEquals("exact-payload",migrated.biofixDao().get("owner",41)?.payload)
        assertNull(migrated.biofixDao().get("other-owner",41))
        migrated.close()
        context.deleteDatabase(name)
        Unit
    }
}
