package com.example.plag_out.util

import kotlinx.coroutines.flow.StateFlow

/**
 * Espera a que el [StateFlow] cumpla una condición, o falla por timeout.
 *
 * Los ViewModels lanzan trabajo en `viewModelScope` y fijan `Dispatchers.IO`,
 * así que el flujo real no se puede "avanzar" de forma puramente
 * virtual.
 */
fun <T> esperarEstado(
    flujo: StateFlow<T>,
    timeoutMs: Long = 3000,
    predicado: (T) -> Boolean
): T {
    val limite = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < limite) {
        if (predicado(flujo.value)) return flujo.value
        Thread.sleep(5)
    }
    throw AssertionError("Timeout esperando el estado. Último valor: ${flujo.value}")
}
