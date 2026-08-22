package com.example.plag_out

import com.example.plag_out.fakes.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Orden del listado de monitoreos por % de eclosión o por días al umbral, en las dos direcciones.
 *
 * Reglas que se fijan acá:
 *  - el umbral ya alcanzado es el menor de todos en días (0), no un dato faltante;
 *  - lo que no se puede proyectar queda al final vaya el orden de mayor a menor o al revés.
 */
class OrdenarMonitoreosTest {

    /** 10 días transcurridos: el promedio histórico es `gddAcumulado / 10`. */
    private fun monitoreo(
        id: Int,
        acumulado: Float,
        objetivo: Float = 500f,
        progreso: Float = 0f,
        activo: Boolean = true
    ) = Fixtures.monitoreo(
        id = id,
        gddAcumulado = acumulado,
        gddObjetivo = objetivo,
        progreso = progreso,
        gddDiario = 0f,
        activo = activo,
        fechaInicio = LocalDate.of(2026, 1, 1),
        fechaActualizacion = LocalDate.of(2026, 1, 11)
    )

    // ---------- % de eclosión ----------

    @Test
    fun `por progreso de mayor a menor`() {
        val monitoreos = listOf(
            monitoreo(id = 1, acumulado = 100f, progreso = 20f),
            monitoreo(id = 2, acumulado = 400f, progreso = 80f),
            monitoreo(id = 3, acumulado = 250f, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_PROGRESO, ascendente = false)

        assertEquals(listOf(2, 3, 1), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `por progreso de menor a mayor`() {
        val monitoreos = listOf(
            monitoreo(id = 1, acumulado = 100f, progreso = 20f),
            monitoreo(id = 2, acumulado = 400f, progreso = 80f),
            monitoreo(id = 3, acumulado = 250f, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_PROGRESO, ascendente = true)

        assertEquals(listOf(1, 3, 2), ordenados.map { it.monitoreo_id })
    }

    // ---------- días al umbral ----------

    @Test
    fun `por dias de menor a mayor pone primero al que ya alcanzo el umbral`() {
        val monitoreos = listOf(
            monitoreo(id = 1, acumulado = 100f),                        // 10/día, faltan 400 -> 40 días
            monitoreo(id = 2, acumulado = 500f, progreso = 100f),       // alcanzado -> 0
            monitoreo(id = 3, acumulado = 400f)                         // 40/día, faltan 100 -> 3 días
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_DIAS_AL_UMBRAL, ascendente = true)

        assertEquals(listOf(2, 3, 1), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `por dias de mayor a menor deja ultimo al que ya alcanzo el umbral`() {
        val monitoreos = listOf(
            monitoreo(id = 1, acumulado = 100f),
            monitoreo(id = 2, acumulado = 500f, progreso = 100f),
            monitoreo(id = 3, acumulado = 400f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_DIAS_AL_UMBRAL, ascendente = false)

        assertEquals(listOf(1, 3, 2), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `los monitoreos sin dias proyectables quedan al final en las dos direcciones`() {
        // Sin GDD acumulados en 10 días no hay ritmo para proyectar: días desconocidos.
        val sinProyeccion = monitoreo(id = 9, acumulado = 0f)
        val monitoreos = listOf(
            sinProyeccion,
            monitoreo(id = 1, acumulado = 100f),
            monitoreo(id = 2, acumulado = 500f, progreso = 100f)
        )

        val descendente = ordenarMonitoreos(monitoreos, ORDEN_DIAS_AL_UMBRAL, ascendente = false)
        val ascendente = ordenarMonitoreos(monitoreos, ORDEN_DIAS_AL_UMBRAL, ascendente = true)

        assertEquals(9, descendente.last().monitoreo_id)
        assertEquals(9, ascendente.last().monitoreo_id)
    }

    // ---------- finalizados al fondo ----------

    @Test
    fun `los finalizados van al fondo aunque tengan mas progreso`() {
        val monitoreos = listOf(
            monitoreo(id = 1, acumulado = 450f, progreso = 90f, activo = false),
            monitoreo(id = 2, acumulado = 100f, progreso = 20f),
            monitoreo(id = 3, acumulado = 250f, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_PROGRESO, ascendente = false, finalizadosAlFinal = true)

        assertEquals(listOf(3, 2, 1), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `los finalizados tambien se ordenan entre si por progreso`() {
        val monitoreos = listOf(
            monitoreo(id = 1, acumulado = 100f, progreso = 20f, activo = false),
            monitoreo(id = 2, acumulado = 450f, progreso = 90f, activo = false),
            monitoreo(id = 3, acumulado = 250f, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_PROGRESO, ascendente = false, finalizadosAlFinal = true)

        assertEquals(listOf(3, 2, 1), ordenados.map { it.monitoreo_id })
    }

    /** Sin la bandera, el estado del monitoreo no influye: es opt-in de la pantalla que lo pide. */
    @Test
    fun `sin finalizadosAlFinal el estado no cambia el orden`() {
        val monitoreos = listOf(
            monitoreo(id = 1, acumulado = 450f, progreso = 90f, activo = false),
            monitoreo(id = 2, acumulado = 100f, progreso = 20f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_PROGRESO, ascendente = false)

        assertEquals(listOf(1, 2), ordenados.map { it.monitoreo_id })
    }

    @Test
    fun `los empates se desempatan por id para que el orden no baile`() {
        val monitoreos = listOf(
            monitoreo(id = 3, acumulado = 250f, progreso = 50f),
            monitoreo(id = 1, acumulado = 250f, progreso = 50f),
            monitoreo(id = 2, acumulado = 250f, progreso = 50f)
        )

        val ordenados = ordenarMonitoreos(monitoreos, ORDEN_PROGRESO, ascendente = false)

        assertEquals(listOf(1, 2, 3), ordenados.map { it.monitoreo_id })
    }
}
