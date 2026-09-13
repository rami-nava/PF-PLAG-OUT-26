package com.example.plag_out.AlmacenamientoLocal

import androidx.room.*

@Entity(tableName = "biofix_pendiente", primaryKeys = ["owner_id", "monitoreo_id"])
data class BiofixPendiente(val owner_id: String, val monitoreo_id: Int, val payload: String, @ColumnInfo(defaultValue = "'pendiente'") val estado: String = "pendiente")

@Dao
interface BiofixDao {
    @Query("SELECT * FROM biofix_pendiente WHERE owner_id = :owner AND monitoreo_id = :monitor LIMIT 1")
    suspend fun get(owner: String, monitor: Int): BiofixPendiente?
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(value: BiofixPendiente)
    @Query("DELETE FROM biofix_pendiente WHERE owner_id = :owner AND monitoreo_id = :monitor")
    suspend fun delete(owner: String, monitor: Int)
}
