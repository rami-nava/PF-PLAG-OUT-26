package com.example.plag_out

import com.example.plag_out.AlmacenamientoLocal.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Tests de los `TypeConverter` de Room para `LocalDate`.
 */
class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `ida y vuelta preserva la fecha`() {
        val fecha = LocalDate.of(2026, 1, 15)
        val texto = converters.fromLocalDate(fecha)
        assertEquals(fecha, converters.toLocalDate(texto))
    }

    @Test
    fun `fromLocalDate usa formato ISO`() {
        assertEquals("2026-01-15", converters.fromLocalDate(LocalDate.of(2026, 1, 15)))
    }

    @Test
    fun `fromLocalDate de null es null`() {
        assertNull(converters.fromLocalDate(null))
    }

    @Test
    fun `toLocalDate de null es null`() {
        assertNull(converters.toLocalDate(null))
    }

    @Test
    fun `toLocalDate parsea un texto ISO`() {
        assertEquals(LocalDate.of(2020, 12, 31), converters.toLocalDate("2020-12-31"))
    }
}
