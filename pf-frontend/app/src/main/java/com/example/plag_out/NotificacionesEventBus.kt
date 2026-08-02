package com.example.plag_out

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Aviso de "llegó un push de alerta" para que la campana se refresque sin esperar al próximo
 * ON_RESUME. El push en sí ya se ve en la bandeja del sistema (PlagOutMessagingService); esto
 * solo dispara un refetch del badge si la app está en primer plano y alguien lo está escuchando.
 */
object NotificacionesEventBus {

    private val _eventos = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventos: SharedFlow<Unit> = _eventos

    /** No es suspend: se llama desde onMessageReceived, que no corre dentro de una coroutine. */
    fun avisarNuevaNotificacion() {
        _eventos.tryEmit(Unit)
    }
}
