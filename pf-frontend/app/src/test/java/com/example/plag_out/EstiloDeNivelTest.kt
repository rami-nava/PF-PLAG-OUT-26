package com.example.plag_out

import com.example.plag_out.ui.theme.HITO_GENERACION
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
    fun `nivel 0 es En huevo`() {
        assertEquals("En huevo", estiloDeNivel(0).etiqueta)
    }

    @Test
    fun `nivel 1 es Eclosión cerca`() {
        assertEquals("Eclosión cerca", estiloDeNivel(1).etiqueta)
    }

    @Test
    fun `nivel 2 es Eclosionó`() {
        assertEquals("Eclosionó", estiloDeNivel(2).etiqueta)
    }

    @Test
    fun `cualquier nivel mayor a 1 es Eclosionó`() {
        assertEquals("Eclosionó", estiloDeNivel(99).etiqueta)
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

    @Test
    fun `un pulgón vivíparo no se describe con huevo ni eclosión`() {
        val etiquetas = listOf(0, 1, 2).map { estiloDeNivel(it, HITO_GENERACION) }
        assertEquals(listOf("En desarrollo", "Generación cerca", "Nueva generación"), etiquetas.map { it.etiqueta })
        etiquetas.forEach { estilo ->
            val texto = (estilo.etiqueta + " " + estilo.descripcion + " " + estilo.recomendacion).lowercase()
            assertTrue(texto, "huevo" !in texto && "eclosi" !in texto && "larva" !in texto)
        }
    }

    @Test
    fun `el hito cambia las palabras pero no el color ni el ícono`() {
        listOf(-1, 0, 1, 2).forEach { nivel ->
            val generacion = estiloDeNivel(nivel, HITO_GENERACION)
            assertEquals(estiloDeNivel(nivel).color, generacion.color)
            assertEquals(estiloDeNivel(nivel).icono, generacion.icono)
        }
    }

    @Test
    fun `eclosión o un backend sin hito mantienen las etiquetas de siempre`() {
        listOf(-1, 0, 1, 2).forEach { nivel ->
            assertEquals(estiloDeNivel(nivel), estiloDeNivel(nivel, "eclosion"))
            assertEquals(estiloDeNivel(nivel), estiloDeNivel(nivel, null))
        }
    }
}

