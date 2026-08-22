package com.example.plag_out

import androidx.compose.material3.ExperimentalMaterial3Api
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Ventana de fechas que el calendario deja elegir como fecha de siembra: sin futuro y sin más de un
 * año de antigüedad.
 */
@OptIn(ExperimentalMaterial3Api::class)
class FechasDeSiembraTest {

    private val hoy = LocalDate.of(2026, 8, 22)
    private val seleccionables = fechasDeSiembraValidas(hoy)

    /** Los millis que entrega el DatePicker por celda, leídos en UTC. */
    private fun millisDe(fecha: LocalDate, hora: LocalTime = LocalTime.MIDNIGHT) =
        fecha.atTime(hora).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test
    fun `hoy se puede elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy)))
    }

    /** El borde de arriba: sin fechas por venir, aunque sea la de mañana. */
    @Test
    fun `manana no se puede elegir`() {
        assertFalse(seleccionables.isSelectableDate(millisDe(hoy.plusDays(1))))
    }

    @Test
    fun `una siembra de hace unos meses se puede elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.minusMonths(4))))
    }

    /** El borde de abajo es inclusivo: justo un año atrás todavía entra. */
    @Test
    fun `un ano atras se puede elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.minusYears(1))))
    }

    @Test
    fun `mas de un ano atras no se puede elegir`() {
        assertFalse(seleccionables.isSelectableDate(millisDe(hoy.minusYears(1).minusDays(1))))
    }

    /** La hora dentro del día no puede cambiar el veredicto: se compara por fecha. */
    @Test
    fun `la hora del dia no cambia si la fecha es elegible`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy, LocalTime.of(23, 59))))
        assertFalse(seleccionables.isSelectableDate(millisDe(hoy.plusDays(1), LocalTime.of(0, 1))))
    }

    @Test
    fun `el selector de anos ofrece solo el actual y el anterior`() {
        assertTrue(seleccionables.isSelectableYear(2026))
        assertTrue(seleccionables.isSelectableYear(2025))
        assertFalse(seleccionables.isSelectableYear(2027))
        assertFalse(seleccionables.isSelectableYear(2024))
    }
}
