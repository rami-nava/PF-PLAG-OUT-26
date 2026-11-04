package com.example.plag_out.Service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Serializes one installation's token work; queued work belongs to a session generation. */
internal class TokenRegistrationCoordinator(
    private val owner: () -> String?,
    private val enabled: () -> Boolean,
    private val token: suspend () -> String,
    private val rotate: suspend () -> Unit,
    private val register: suspend (String) -> Int,
    private val unregister: suspend (String) -> Unit,
) {
    private val mutex = Mutex()
    @Volatile private var generation = 0L
    @Volatile private var closing = false
    @Volatile var pending = false
        private set

    fun beginSession() { closing = false }
    fun invalidateSession() { closing = true; generation++; pending = false }

    suspend fun register() {
        val expectedOwner = owner() ?: return
        val expectedGeneration = generation
        fun valid() = !closing && expectedGeneration == generation && owner() == expectedOwner && enabled()
        mutex.withLock {
            if (!valid()) return
            pending = true
            var current = token()
            if (!valid()) return
            var status = register(current)
            if (!valid()) return
            if (status == 409) {
                rotate()
                if (!valid()) return
                current = token()
                if (!valid()) return
                status = register(current) // Exactly one renewal per logical attempt.
            }
            if (valid()) pending = status !in 200..299
        }
    }

    /**
     * Retires this installation: the backend record first (while the JWT is still valid), then a
     * new FID. The work runs in [scope] and the caller waits at most [waitMs]: closing a session
     * must not hang on Firebase. Whatever is left keeps running, still under the mutex, so the next
     * session's register() only starts after the rotation and never inherits the old FID.
     *
     * Returns true when everything finished within [waitMs].
     */
    suspend fun unregister(
        remote: Boolean,
        scope: CoroutineScope,
        waitMs: Long,
        onFailure: (Throwable) -> Unit = {},
    ): Boolean {
        invalidateSession()
        val work = scope.launch {
            mutex.withLock {
                try {
                    if (remote) unregister(token())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onFailure(e)
                } finally {
                    try {
                        rotate()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        onFailure(e)
                    }
                }
            }
        }
        return withTimeoutOrNull(waitMs) { work.join() } != null
    }
}
