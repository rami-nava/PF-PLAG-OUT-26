package com.example.plag_out.AlmacenamientoLocal

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Bandeja de salida de tratamientos: se guarda antes de enviar, así un conteo cargado sin señal
 * no se pierde. Sin [tratamiento_id] es un alta (POST idempotente por [idempotency_key]); con él,
 * una edición (PATCH) de un tratamiento que ya está en el backend.
 */
@Entity(tableName = "tratamiento_pendiente", indices = [Index(value = ["owner_id", "monitoreo_id"])])
data class TratamientoPendiente(
    @PrimaryKey val idempotency_key: String,
    val owner_id: String,
    val monitoreo_id: Int,
    val tratamiento_id: Int? = null,
    val payload: String,
    @ColumnInfo(defaultValue = "'pendiente'") val estado: String = "pendiente",
    val creado_en: Long = System.currentTimeMillis()
)

@Dao
interface TratamientoPendienteDao {
    @Query("SELECT * FROM tratamiento_pendiente WHERE owner_id = :owner ORDER BY creado_en")
    fun todos(owner: String): Flow<List<TratamientoPendiente>>

    @Query("SELECT * FROM tratamiento_pendiente WHERE owner_id = :owner AND monitoreo_id = :monitoreo ORDER BY creado_en")
    fun delMonitoreo(owner: String, monitoreo: Int): Flow<List<TratamientoPendiente>>

    @Query("SELECT * FROM tratamiento_pendiente WHERE owner_id = :owner AND idempotency_key = :clave")
    suspend fun get(owner: String, clave: String): TratamientoPendiente?

    @Query("SELECT * FROM tratamiento_pendiente WHERE owner_id = :owner AND tratamiento_id = :tratamiento LIMIT 1")
    suspend fun edicionDe(owner: String, tratamiento: Int): TratamientoPendiente?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(fila: TratamientoPendiente)

    /** Solo si nadie la cambió mientras se enviaba: si cambió, queda para el próximo envío. */
    @Query("DELETE FROM tratamiento_pendiente WHERE owner_id = :owner AND idempotency_key = :clave AND payload = :payload")
    suspend fun borrarSiNoCambio(owner: String, clave: String, payload: String): Int

    /** El alta ya llegó al backend pero cambió mientras se enviaba: lo nuevo viaja como edición. */
    @Query("UPDATE tratamiento_pendiente SET tratamiento_id = :tratamiento WHERE owner_id = :owner AND idempotency_key = :clave")
    suspend fun pasarAEdicion(owner: String, clave: String, tratamiento: Int)

    @Query("UPDATE tratamiento_pendiente SET estado = :estado WHERE owner_id = :owner AND idempotency_key = :clave AND payload = :payload")
    suspend fun marcar(owner: String, clave: String, payload: String, estado: String)

    @Query("DELETE FROM tratamiento_pendiente WHERE owner_id = :owner AND idempotency_key = :clave")
    suspend fun borrar(owner: String, clave: String)

    @Query("DELETE FROM tratamiento_pendiente WHERE monitoreo_id IN (:monitoreos)")
    suspend fun borrarDeMonitoreos(monitoreos: List<Int>)
}
