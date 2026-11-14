package com.example.plag_out

import org.junit.Assert.assertEquals
import org.junit.Test

class TextoNormalizadoTest {

    @Test
    fun `normalizarClaveUnica ignora tildes mayusculas y espacios extra`() {
        assertEquals("maiz dulce", normalizarClaveUnica("  Maíz   Dulce "))
        assertEquals("maiz dulce", normalizarClaveUnica("MAIZ DULCE"))
        assertEquals("pulgon verde", normalizarClaveUnica("Pulgón Verde"))
    }
}
