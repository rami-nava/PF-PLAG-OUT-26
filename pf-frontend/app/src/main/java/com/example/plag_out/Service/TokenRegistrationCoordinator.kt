package com.example.plag_out.Service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    suspend fun unregister(remote: Boolean) {
        invalidateSession()
        mutex.withLock {
            try {
                if (remote) unregister(token())
            } finally {
                rotate()
            }
        }
    }
}
