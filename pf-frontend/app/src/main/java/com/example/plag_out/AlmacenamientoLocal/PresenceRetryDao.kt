package com.example.plag_out.AlmacenamientoLocal

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PresenceRetryDao {
    @Query("SELECT * FROM biofix_pendiente WHERE owner_id = :owner")
    fun manual(owner: String): Flow<List<BiofixPendiente>>
    @Query("SELECT * FROM feedback_prediccion_pendiente WHERE owner_id = :owner")
    fun feedback(owner: String): Flow<List<FeedbackPrediccionPendiente>>
    @Query("UPDATE biofix_pendiente SET estado = :state WHERE owner_id = :owner AND monitoreo_id = :id AND payload = :payload")
    suspend fun manualState(owner: String, id: Int, payload: String, state: String)
    @Query("DELETE FROM biofix_pendiente WHERE owner_id = :owner AND monitoreo_id = :id AND payload = :payload")
    suspend fun deleteManual(owner: String, id: Int, payload: String)
    @Query("UPDATE feedback_prediccion_pendiente SET estado = :state WHERE owner_id = :owner AND prediccion_id = :id AND idempotency_key = :key")
    suspend fun feedbackState(owner: String, id: Int, key: String, state: String)
    @Query("DELETE FROM feedback_prediccion_pendiente WHERE owner_id = :owner AND prediccion_id = :id AND idempotency_key = :key")
    suspend fun deleteFeedback(owner: String, id: Int, key: String)
}
