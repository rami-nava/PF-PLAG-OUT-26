package com.example.plag_out

import androidx.compose.material3.ExperimentalMaterial3Api
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
class FechasDeBiofixTest {

    private val hoy = LocalDate.of(2026, 8, 22)
    private val inicio = LocalDate.of(2026, 5, 10)
    private val seleccionables = fechasDeBiofixValidas(inicio, hoy)

    private fun millisDe(fecha: LocalDate, hora: LocalTime = LocalTime.MIDNIGHT) =
        fecha.atTime(hora).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `hoy se puede elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy)))
    }

    @Test
    fun `manana no se puede elegir`() {
        assertFalse(seleccionables.isSelectableDate(millisDe(hoy.plusDays(1))))
    }

    @Test
    fun `el dia de inicio del monitoreo se puede elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(inicio)))
    }

    @Test
    fun `el dia anterior al inicio no se puede elegir`() {
        assertFalse(seleccionables.isSelectableDate(millisDe(inicio.minusDays(1))))
    }

    @Test
    fun `una fecha intermedia se puede elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(LocalDate.of(2026, 7, 1))))
    }

    @Test
    fun `los anios fuera de la ventana no se ofrecen`() {
        assertTrue(seleccionables.isSelectableYear(2026))
        assertFalse(seleccionables.isSelectableYear(2025))
        assertFalse(seleccionables.isSelectableYear(2027))
    }
}
