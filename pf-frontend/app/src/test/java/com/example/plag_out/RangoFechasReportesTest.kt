package com.example.plag_out

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RangoFechasReportesTest {
    private val hoy = LocalDate.of(2027, 1, 15)

    @Test
    fun `mover desde lejos acerca hasta para no superar el maximo del backend`() {
        val desde = LocalDate.of(2026, 1, 1)

        assertEquals(desde to desde.plusDays(RANGO_MAXIMO_DIAS), ajustarRangoFechas(desde, hoy, movioDesde = true))
    }

    @Test
    fun `mover hasta lejos acerca desde`() {
        val desde = LocalDate.of(2025, 6, 1)

        assertEquals(hoy.minusDays(RANGO_MAXIMO_DIAS) to hoy, ajustarRangoFechas(desde, hoy, movioDesde = false))
    }

    @Test
    fun `desde posterior a hasta lleva ambos al mismo dia`() {
        val dia = LocalDate.of(2027, 1, 10)

        assertEquals(dia to dia, ajustarRangoFechas(dia, LocalDate.of(2027, 1, 5), movioDesde = true))
        assertEquals(dia to dia, ajustarRangoFechas(LocalDate.of(2027, 1, 12), dia, movioDesde = false))
    }

    @Test
    fun `un rango valido no cambia`() {
        val desde = hoy.minusDays(30)

        assertEquals(desde to hoy, ajustarRangoFechas(desde, hoy, movioDesde = true))
        assertEquals(desde to hoy, ajustarRangoFechas(desde, hoy, movioDesde = false))
    }
}
