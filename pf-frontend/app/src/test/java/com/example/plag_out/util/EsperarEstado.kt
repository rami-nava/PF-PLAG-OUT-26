package com.example.plag_out.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.TimeSource

/**
 * Espera a que [condicion] se cumpla, o falla por timeout.
 *
 * Los ViewModels mandan la red a `Dispatchers.IO` (hilos reales) y vuelven a
 * `Dispatchers.Main`, que en los tests es el [MainDispatcherRule]: un
 * dispatcher que solo avanza cuando alguien lo empuja. Esta espera lo empuja
 * desde el hilo del test, así lo de Main corre siempre en un solo hilo, como en
 * la app. No usar `runBlocking { join() }` para esperar a un ViewModel: bloquea
 * el único hilo que puede avanzar Main y se cuelga.
 */
fun esperarQue(
    timeoutMs: Long = 10_000,
    descripcion: () -> String = { "la condición" },
    condicion: () -> Boolean
) {
    // Reloj real: Robolectric reemplaza System.currentTimeMillis() por un reloj
    // simulado que puede saltar y cortar la espera antes de tiempo.
    val inicio = TimeSource.Monotonic.markNow()
    while (true) {
        MainDispatcherRule.advanceUntilIdle()
        try {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        } catch (_: RuntimeException) {
            // Fuera de Robolectric no hay looper que vaciar.
        }
        if (condicion()) return
        val transcurrido = inicio.elapsedNow()
        if (transcurrido.inWholeMilliseconds >= timeoutMs) {
            throw AssertionError("Timeout ($transcurrido) esperando ${descripcion()}")
        }
        Thread.sleep(5)
    }
}

/** Espera a que el [StateFlow] cumpla [predicado] y devuelve ese valor. */
fun <T> esperarEstado(
    flujo: StateFlow<T>,
    timeoutMs: Long = 10_000,
    predicado: (T) -> Boolean
): T {
    esperarQue(timeoutMs, { "el estado. Último valor: ${flujo.value}" }) { predicado(flujo.value) }
    return flujo.value
}

/** Espera a que terminen las corrutinas que [viewModel] tiene en curso ahora. */
fun esperarCorrutinas(viewModel: ViewModel, timeoutMs: Long = 10_000) {
    val enCurso = viewModel.viewModelScope.coroutineContext[Job]!!.children.toList()
    esperarQue(timeoutMs, { "las corrutinas de ${viewModel::class.simpleName}" }) {
        enCurso.all { it.isCompleted }
    }
}

/** Cancela todo lo que [viewModel] tiene en curso y espera a que termine. */
fun cancelarCorrutinas(viewModel: ViewModel, timeoutMs: Long = 10_000) {
    val scope = viewModel.viewModelScope.coroutineContext[Job]!!
    scope.cancel()
    esperarQue(timeoutMs, { "la cancelación de ${viewModel::class.simpleName}" }) { scope.isCompleted }
}
