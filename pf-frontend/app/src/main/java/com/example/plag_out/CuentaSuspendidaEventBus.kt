package com.example.plag_out

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow


object CuentaSuspendidaEventBus {

    const val DETALLE = "cuenta_suspendida"
    const val MENSAJE = "Tu cuenta está suspendida. Contactá al administrador."

    private val _eventos = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventos: SharedFlow<Unit> = _eventos

    fun avisar() {
        _eventos.tryEmit(Unit)
    }
}
