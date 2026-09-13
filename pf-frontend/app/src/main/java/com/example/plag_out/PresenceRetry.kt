package com.example.plag_out

import android.content.Context
import androidx.work.*
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.Service.RetrofitClient
import com.google.gson.Gson
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.TimeUnit

internal enum class RetryOutcome { DONE, RETRY }
internal suspend fun retryPresence(
    owner: String, currentOwner: () -> String?, send: suspend () -> Int,
    success: suspend () -> Unit, review: suspend () -> Unit
): RetryOutcome {
    if (currentOwner() != owner) return RetryOutcome.DONE
    return try {
        val code = send()
        when {
            code in 200..299 -> { success(); RetryOutcome.DONE }
            code == 408 || code == 429 || code >= 500 || code == 401 || code == 403 -> RetryOutcome.RETRY
            else -> { review(); RetryOutcome.DONE }
        }
    } catch (cancelled: CancellationException) { throw cancelled }
      catch (_: Exception) { RetryOutcome.RETRY }
}

data class PresenceRetryEvent(val owner: String, val kind: String, val id: Int, val result: BiofixResult? = null)
object PresenceRetryEvents {
    val events = MutableSharedFlow<PresenceRetryEvent>(extraBufferCapacity = 16)
}

/** Room is the payload authority. WorkManager holds only owner/resource/UUID. */
object PresenceRetryScheduler {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    fun start(context: Context) {
        val app = context.applicationContext
        scope.launch {
            SupabaseProvider.client.auth.sessionStatus.collectLatest { status ->
                if (status !is SessionStatus.Authenticated) return@collectLatest
                val owner = SupabaseProvider.client.auth.currentUserOrNull()?.id ?: return@collectLatest
                val dao = AppDatabase.getDatabase(app).presenceRetryDao()
                combine(dao.manual(owner), dao.feedback(owner)) { manual, feedback -> manual to feedback }
                    .collect { (manual, feedback) ->
                        manual.filter { it.estado == "pendiente" }.forEach { row ->
                            val key = runCatching { Gson().fromJson(row.payload, BiofixRequest::class.java).idempotency_key }.getOrNull()
                            if (key != null) enqueue(app, owner, "manual", row.monitoreo_id, key)
                        }
                        feedback.filter { it.estado == "pendiente" && it.respuesta == "presente" && it.biofix_json != null }
                            .forEach { enqueue(app, owner, "ml", it.prediccion_id, it.idempotency_key) }
                    }
            }
        }
    }
    internal fun request(owner: String, kind: String, id: Int, key: String) =
        OneTimeWorkRequestBuilder<PresenceRetryWorker>()
            .setInputData(workDataOf("owner" to owner, "kind" to kind, "id" to id, "key" to key))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
    private fun enqueue(context: Context, owner: String, kind: String, id: Int, key: String) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            "presence:$owner:$kind:$id:$key", ExistingWorkPolicy.KEEP, request(owner, kind, id, key))
    }
}

class PresenceRetryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try { runAttempt() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { Result.retry() }

    private suspend fun runAttempt(): Result {
        val owner = inputData.getString("owner") ?: return Result.failure()
        val kind = inputData.getString("kind") ?: return Result.failure()
        val key = inputData.getString("key") ?: return Result.failure()
        val id = inputData.getInt("id", -1)
        SupabaseProvider.client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        if (SupabaseProvider.client.auth.currentUserOrNull()?.id != owner) return Result.success()
        val db = AppDatabase.getDatabase(applicationContext)
        val dao = db.presenceRetryDao()
        val service = RetrofitClient.forPresenceRetry(owner)
        val gson = Gson()
        var result: BiofixResult? = null
        val send: suspend () -> Int
        val success: suspend () -> Unit
        val review: suspend () -> Unit
        if (kind == "manual") {
            val row = db.biofixDao().get(owner, id) ?: return Result.success()
            if (row.estado != "pendiente") return Result.success()
            val payload = runCatching { gson.fromJson(row.payload, BiofixRequest::class.java) }.getOrNull()
            if (payload == null) {
                dao.manualState(owner, id, row.payload, "requiere_revision"); return Result.success()
            }
            if (payload.idempotency_key != key) return Result.success()
            send = { service.registrarBiofix(id, payload).let { result = it.body(); if (it.isSuccessful && result == null) 503 else it.code() } }
            success = { dao.deleteManual(owner, id, row.payload) }
            review = { dao.manualState(owner, id, row.payload, "requiere_revision") }
        } else if (kind == "ml") {
            val row = db.feedbackPrediccionDao().get(owner, id) ?: return Result.success()
            if (row.estado != "pendiente" || row.idempotency_key != key || row.respuesta != "presente") return Result.success()
            val biofix = runCatching { gson.fromJson(row.biofix_json, BiofixRequest::class.java) }.getOrNull()
            if (biofix == null || biofix.idempotency_key != key) {
                dao.feedbackState(owner, id, key, "requiere_revision"); return Result.success()
            }
            val payload = PrediccionConfirmacionRequest(respuesta = row.respuesta, idempotency_key = key, biofix = biofix)
            send = { service.confirmarPrediccion(id, payload).let { result = it.body()?.biofix; if (it.isSuccessful && result == null) 503 else it.code() } }
            success = { dao.deleteFeedback(owner, id, key) }
            review = { dao.feedbackState(owner, id, key, "requiere_revision") }
        } else return Result.failure()
        val outcome = retryPresence(owner, { SupabaseProvider.client.auth.currentUserOrNull()?.id }, send, success, review)
        if (outcome == RetryOutcome.DONE) PresenceRetryEvents.events.emit(PresenceRetryEvent(owner, kind, id, result))
        return if (outcome == RetryOutcome.RETRY) Result.retry() else Result.success()
    }
}
