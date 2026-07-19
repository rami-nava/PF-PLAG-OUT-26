package com.example.plag_out

import com.example.plag_out.ui.theme.estiloDeNivel
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests de `estiloDeNivel(nivel: Int)`, el mapeo de nivel de alerta.
 */
class EstiloDeNivelTest {

    @Test
    fun `nivel negativo es Sin datos`() {
        assertEquals("Sin datos", estiloDeNivel(-1).etiqueta)
    }

    @Test
    fun `nivel muy negativo sigue siendo Sin datos`() {
        assertEquals("Sin datos", estiloDeNivel(-100).etiqueta)
    }

    @Test
    fun `nivel 0 es Saludable`() {
        assertEquals("Saludable", estiloDeNivel(0).etiqueta)
    }

    @Test
    fun `nivel 1 es Atencion`() {
        assertEquals("Atención", estiloDeNivel(1).etiqueta)
    }

    @Test
    fun `nivel 2 es Critico`() {
        assertEquals("Crítico", estiloDeNivel(2).etiqueta)
    }

    @Test
    fun `cualquier nivel mayor a 1 es Critico`() {
        assertEquals("Crítico", estiloDeNivel(99).etiqueta)
    }
}
