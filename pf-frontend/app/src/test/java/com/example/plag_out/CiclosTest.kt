package com.example.plag_out

import com.example.plag_out.fakes.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CiclosTest {

    @Test
    fun `el nivel efectivo es el mas alto entre los ciclos activos`() {
        val monitoreo = Fixtures.monitoreo(
            nivelAlerta = 0,
            ciclos = listOf(
                Fixtures.ciclo(id = 1, nivelAlerta = 0),
                Fixtures.ciclo(id = 2, nivelAlerta = 2),
                Fixtures.ciclo(id = 3, nivelAlerta = 1)
            )
        )

        assertEquals(2, nivelAlertaEfectivo(monitoreo))
    }

    @Test
    fun `los ciclos cerrados no levantan el nivel del monitoreo`() {
        val monitoreo = Fixtures.monitoreo(
            ciclos = listOf(
                Fixtures.ciclo(id = 1, estado = "activo", nivelAlerta = 0),
                Fixtures.ciclo(id = 2, estado = "finalizado", nivelAlerta = 2)
            )
        )

        assertEquals(0, nivelAlertaEfectivo(monitoreo))
    }

    @Test
    fun `sin ciclos activos el nivel es sin datos y no bajo`() {
        val monitoreo = Fixtures.monitoreo(nivelAlerta = 0, ciclos = null)

        assertNull(nivelAlertaDeCiclos(monitoreo))
        assertEquals(-1, nivelAlertaEfectivo(monitoreo))
    }

    @Test
    fun `cuenta los ciclos activos que pasaron el umbral del monitoreo`() {
        val monitoreo = Fixtures.monitoreo(
            umbralRiesgo = 80,
            ciclos = listOf(
                Fixtures.ciclo(id = 1, progreso = 95f),
                Fixtures.ciclo(id = 2, progreso = 80f),
                Fixtures.ciclo(id = 3, progreso = 79f),
                Fixtures.ciclo(id = 4, progreso = 99f, estado = "finalizado")
            )
        )

        assertEquals(2, ciclosEnAlerta(monitoreo))
    }

    @Test
    fun `sin umbral definido solo cuenta el ciclo que llego al objetivo`() {
        val monitoreo = Fixtures.monitoreo(
            umbralRiesgo = null,
            ciclos = listOf(Fixtures.ciclo(id = 1, progreso = 99f), Fixtures.ciclo(id = 2, progreso = 100f))
        )

        assertEquals(1, ciclosEnAlerta(monitoreo))
    }

    @Test
    fun `proyecta los dias al objetivo con el promedio desde el biofix`() {
        // 10 dias entre biofix y actualizacion, 200 acumulados -> 20/dia; faltan 200 -> 10 dias.
        // Con gdd_diario (5) darian 40, por eso importa el promedio.
        val ciclo = Fixtures.ciclo(
            fechaBiofix = "2026-01-01",
            fechaActualizacion = "2026-01-11",
            gddAcumulado = 200f,
            gddDiario = 5f,
            gddEclosion = 400f,
            progreso = 50f
        )

        assertEquals(10, diasEstimadosDelCiclo(ciclo, objetivoMonitoreo = null))
    }

    @Test
    fun `redondea hacia arriba los dias al objetivo`() {
        // 10 dias, 100 acumulados -> 10/dia; faltan 95 -> 9,5 dias -> 10.
        val ciclo = Fixtures.ciclo(
            fechaBiofix = "2026-01-01",
            fechaActualizacion = "2026-01-11",
            gddAcumulado = 100f,
            gddEclosion = 195f,
            progreso = 51f
        )

        assertEquals(10, diasEstimadosDelCiclo(ciclo, objetivoMonitoreo = null))
    }

    @Test
    fun `null cuando el ciclo ya llego al objetivo`() {
        val ciclo = Fixtures.ciclo(gddAcumulado = 400f, gddEclosion = 400f, progreso = 100f)

        assertNull(diasEstimadosDelCiclo(ciclo, objetivoMonitoreo = null))
    }

    @Test
    fun `null cuando no hay ritmo con el que proyectar`() {
        val ciclo = Fixtures.ciclo(
            fechaBiofix = "2026-01-01",
            fechaActualizacion = "2026-01-11",
            gddAcumulado = 0f,
            gddDiario = 0f,
            gddEclosion = 400f,
            progreso = 0f
        )

        assertNull(diasEstimadosDelCiclo(ciclo, objetivoMonitoreo = null))
    }

    @Test
    fun `cae al objetivo del monitoreo cuando el ciclo no trae gdd de eclosion`() {
        val ciclo = Fixtures.ciclo(gddAcumulado = 100f, gddEclosion = 0f, progreso = 20f)

        assertEquals(500f, objetivoDelCiclo(ciclo, objetivoMonitoreo = 500f))
        assertNull(objetivoDelCiclo(ciclo, objetivoMonitoreo = null))
    }

    @Test
    fun `ordena los ciclos de mayor a menor progreso y desempata por id`() {
        val ciclos = listOf(
            Fixtures.ciclo(id = 3, progreso = 40f),
            Fixtures.ciclo(id = 1, progreso = 90f),
            Fixtures.ciclo(id = 2, progreso = 90f)
        )

        assertEquals(listOf(1, 2, 3), ordenarCiclosPorProgreso(ciclos).map { it.id })
    }
}
