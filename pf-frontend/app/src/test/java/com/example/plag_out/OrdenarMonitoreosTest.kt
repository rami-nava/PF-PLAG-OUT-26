package com.example.plag_out

import com.example.plag_out.fakes.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test


class OrdenarMonitoreosTest {

    private fun monitoreo(id: Int, progreso: Float, activo: Boolean = true) =
        Fixtures.monitoreo(id = id, progreso = progreso, activo = activo)

    @Test
    fun `por progreso de mayor a menor`() {
        val monitoreos = listOf(
            monitoreo(id = 1, progreso = 20f),
            monitoreo(id = 2, progreso = 80f),
            monitoreo(id = 3, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos)

        assertEquals(listOf(2, 3, 1), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `los finalizados van al fondo aunque tengan mas progreso`() {
        val monitoreos = listOf(
            monitoreo(id = 1, progreso = 90f, activo = false),
            monitoreo(id = 2, progreso = 20f),
            monitoreo(id = 3, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, finalizadosAlFinal = true)

        assertEquals(listOf(3, 2, 1), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `los finalizados tambien se ordenan entre si por progreso`() {
        val monitoreos = listOf(
            monitoreo(id = 1, progreso = 20f, activo = false),
            monitoreo(id = 2, progreso = 90f, activo = false),
            monitoreo(id = 3, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, finalizadosAlFinal = true)

        assertEquals(listOf(3, 2, 1), ordenados.map { it.monitoreo_id })
    }

    /** Sin la bandera, el estado del monitoreo no influye: es opt-in de la pantalla que lo pide. */
    @Test
    fun `sin finalizadosAlFinal el estado no cambia el orden`() {
        val monitoreos = listOf(
            monitoreo(id = 1, progreso = 90f, activo = false),
            monitoreo(id = 2, progreso = 20f)
        )

        val ordenados = ordenarMonitoreos(monitoreos)

        assertEquals(listOf(1, 2), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `los empates se desempatan por id para que el orden no baile`() {
        val monitoreos = listOf(
            monitoreo(id = 3, progreso = 50f),
            monitoreo(id = 1, progreso = 50f),
            monitoreo(id = 2, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos)

        assertEquals(listOf(1, 2, 3), ordenados.map { it.monitoreo_id })
    }
}
