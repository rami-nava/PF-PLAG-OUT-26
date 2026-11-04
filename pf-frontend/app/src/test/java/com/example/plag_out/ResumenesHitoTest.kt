package com.example.plag_out

import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.ui.theme.estiloDeNivel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The summaries mix pests: their words must hold for every monitoring counted in each level. */
class ResumenesHitoTest {

    private fun monitoreo(id: Int, nivel: Int, hito: String?, terreno: Int = 1, activo: Boolean = true) =
        Fixtures.monitoreo(
            id = id, terrenoId = terreno, activo = activo,
            ciclos = listOf(Fixtures.ciclo(id = id, monitoreoId = id, nivelAlerta = nivel, hito = hito))
        )

    private val oruga = monitoreo(1, nivel = 0, hito = "eclosion")
    private val chinche = monitoreo(2, nivel = 1, hito = "eclosion")
    private val pulgon = monitoreo(3, nivel = 0, hito = "generacion")
    private val pulgonCerca = monitoreo(4, nivel = 1, hito = "generacion")

    @Test
    fun `sin pulgones el resumen no cambia`() {
        val todos = listOf(oruga, chinche)
        listOf(0, 1, 2).forEach { assertEquals(estiloDeNivel(it), estiloDeNivelPara(it, todos)) }
    }

    @Test
    fun `cada nivel mira solo los monitoreos que cuenta`() {
        // Nivel 0 mezcla oruga y pulgón; nivel 1 solo tiene la chinche.
        val todos = listOf(oruga, chinche, pulgon)
        assertEquals("En desarrollo", estiloDeNivelPara(0, todos).etiqueta)
        assertEquals("Eclosión cerca", estiloDeNivelPara(1, todos).etiqueta)
    }

    @Test
    fun `solo pulgones usan las palabras de generación`() {
        val todos = listOf(pulgon, pulgonCerca)
        assertEquals("En desarrollo", estiloDeNivelPara(0, todos).etiqueta)
        assertEquals("Generación cerca", estiloDeNivelPara(1, todos).etiqueta)
    }

    @Test
    fun `una mezcla en el mismo nivel usa palabras que valen para ambos`() {
        val todos = listOf(chinche, pulgonCerca)
        val estilo = estiloDeNivelPara(1, todos)
        assertEquals("Objetivo cerca", estilo.etiqueta)
        assertEquals(estiloDeNivel(1).color, estilo.color)
        assertTrue(estilo.descripcion.contains("eclosión") && estilo.descripcion.contains("generación"))
    }

    @Test
    fun `un terreno lo definen sus monitoreos activos en el nivel máximo`() {
        val cerrado = monitoreo(5, nivel = 2, hito = "eclosion", activo = false)
        val otroTerreno = monitoreo(6, nivel = 2, hito = "eclosion", terreno = 2)
        val definen = monitoreosQueDefinenElTerreno(1, listOf(oruga, chinche, pulgonCerca, cerrado, otroTerreno))
        assertEquals(listOf(2, 4), definen.map { it.monitoreo_id })
        assertEquals("Objetivo cerca", estiloDeNivelPara(1, definen).etiqueta)
        assertTrue(monitoreosQueDefinenElTerreno(9, listOf(oruga)).isEmpty())
    }

    @Test
    fun `la frase de la card de terreno sigue al hito`() {
        assertEquals("1 con eclosión cerca", fraseDeNivel(1, 1, listOf(chinche)))
        assertEquals("1 con generación cerca", fraseDeNivel(1, 1, listOf(pulgonCerca)))
        assertEquals("2 con el objetivo cerca", fraseDeNivel(2, 1, listOf(chinche, pulgonCerca)))
        assertEquals("3 ya eclosionados", fraseDeNivel(3, 2, listOf(monitoreo(7, nivel = 2, hito = null))))
    }
}
