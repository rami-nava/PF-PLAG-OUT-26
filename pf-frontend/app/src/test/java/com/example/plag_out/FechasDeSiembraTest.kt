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

    /** El borde de arriba: permite planificar siembras hasta 6 meses hacia adelante. */
    @Test
    fun `manana y fechas proximas se pueden elegir para siembras planificadas`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.plusDays(1))))
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.plusMonths(3))))
        assertFalse(seleccionables.isSelectableDate(millisDe(hoy.plusMonths(7))))
    }

    @Test
    fun `una siembra de hace unos meses se puede elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.minusMonths(4))))
    }

    /** Permite registrar campañas históricas de hasta 3 años atrás. */
    @Test
    fun `campanas anteriores se pueden elegir`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.minusYears(1))))
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.minusYears(2))))
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.minusYears(3))))
    }

    @Test
    fun `mas de tres anos atras queda fuera`() {
        assertFalse(seleccionables.isSelectableDate(millisDe(hoy.minusYears(3).minusDays(1))))
    }

    /** La hora dentro del día no puede cambiar el veredicto: se compara por fecha. */
    @Test
    fun `la hora del dia no cambia si la fecha es elegible`() {
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy, LocalTime.of(23, 59))))
        assertTrue(seleccionables.isSelectableDate(millisDe(hoy.plusDays(1), LocalTime.of(0, 1))))
    }

    @Test
    fun `el selector de anos ofrece rango amplio para campanas historicas y proximas`() {
        assertTrue(seleccionables.isSelectableYear(2027))
        assertTrue(seleccionables.isSelectableYear(2026))
        assertTrue(seleccionables.isSelectableYear(2025))
        assertTrue(seleccionables.isSelectableYear(2024))
        assertTrue(seleccionables.isSelectableYear(2023))
        assertFalse(seleccionables.isSelectableYear(2022))
    }
}
