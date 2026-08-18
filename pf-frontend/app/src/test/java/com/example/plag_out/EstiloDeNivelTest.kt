package com.example.plag_out

import com.example.plag_out.ui.theme.estiloDeNivel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


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
    fun `nivel 0 es Bajo`() {
        assertEquals("Bajo", estiloDeNivel(0).etiqueta)
    }

    @Test
    fun `nivel 1 es Moderado`() {
        assertEquals("Moderado", estiloDeNivel(1).etiqueta)
    }

    @Test
    fun `nivel 2 es Alto`() {
        assertEquals("Alto", estiloDeNivel(2).etiqueta)
    }

    @Test
    fun `cualquier nivel mayor a 1 es Alto`() {
        assertEquals("Alto", estiloDeNivel(99).etiqueta)
    }

    @Test
    fun `los tres niveles explican que significan`() {
        listOf(0, 1, 2).forEach { nivel ->
            val estilo = estiloDeNivel(nivel)
            assertTrue(
                "El nivel $nivel debería tener descripción y recomendación",
                estilo.descripcion.isNotBlank() && estilo.recomendacion.isNotBlank()
            )
        }
    }
}
