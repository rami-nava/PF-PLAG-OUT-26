package com.example.plag_out.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Reemplaza Dispatchers.Main por un dispatcher de test.
 *
 * Por defecto es un [StandardTestDispatcher]: lo que los ViewModels hacen en
 * Main queda en cola hasta que el test lo avanza ([esperarQue], [esperarEstado]),
 * siempre desde el hilo del test. Con `UnconfinedTestDispatcher` lo de Main se
 * reanudaba en el hilo de IO que terminó la llamada, dos corrutinas escribían el
 * estado a la vez y los tests fallaban al azar.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {

    companion object {
        @Volatile
        var current: MainDispatcherRule? = null
            private set

        /** Corre lo pendiente en Main; no hace nada si no hay un test con la regla. */
        fun advanceUntilIdle() {
            current?.testDispatcher?.scheduler?.advanceUntilIdle()
        }
    }

    override fun starting(description: Description) {
        current = this
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        try {
            testDispatcher.scheduler.advanceUntilIdle()
        } finally {
            current = null
            Dispatchers.resetMain()
        }
    }
}
