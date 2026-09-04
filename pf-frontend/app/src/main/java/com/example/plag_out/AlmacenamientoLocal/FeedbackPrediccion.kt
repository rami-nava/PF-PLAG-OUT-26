package com.example.plag_out.AlmacenamientoLocal

import androidx.room.Entity

@Entity(
    tableName = "feedback_prediccion_pendiente",
    primaryKeys = ["owner_id", "prediccion_id"]
)
data class FeedbackPrediccionPendiente(
    val owner_id: String,
    val prediccion_id: Int,
    val respuesta: String,
    val idempotency_key: String,
    val estado: String = "pendiente",
    val creado_en_ms: Long = System.currentTimeMillis()
)

class FeedbackPrediccionRepository(private val dao: FeedbackPrediccionDao) {
    suspend fun obtener(ownerId: String, prediccionId: Int) =
        dao.get(ownerId, prediccionId)

    suspend fun guardar(feedback: FeedbackPrediccionPendiente) = dao.insert(feedback)

    suspend fun eliminar(ownerId: String, prediccionId: Int) =
        dao.delete(ownerId, prediccionId)

    suspend fun eliminarPorUsuario(ownerId: String) = dao.deleteByOwner(ownerId)
}
