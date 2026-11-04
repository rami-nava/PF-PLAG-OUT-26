package com.example.plag_out

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.TileSystem
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * La capa de densidad calcula una vez en coordenadas del mundo y reusa el resultado al mover el
 * mapa; los íconos se dibujan una vez. Estos tests fijan lo que hace correcto ese reuso.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapaRendimientoTest {

    private val zoom = 8.0
    private val mapa = TileSystem.MapSize(zoom)
    private val puntos = listOf(
        PuntoMapa(-34.60, -58.40, 1, "Alto", null),
        PuntoMapa(-34.62, -58.45, 12, "Medio", null),
        PuntoMapa(-34.90, -57.95, 3, "Bajo", null)
    )

    @Test
    fun `la proyeccion coincide con la de osmdroid`() {
        assertEquals(0.0, xMundo(-180.0, mapa), 1e-9)
        assertEquals(mapa / 2, xMundo(0.0, mapa), 1e-9)
        assertEquals(mapa / 2, yMundo(0.0, mapa), 1e-9)
        assertTrue("el sur está más abajo", yMundo(-34.6, mapa) > mapa / 2)
        for (latitud in listOf(-54.8, -34.6, 0.0, 45.0)) {
            val osmdroid = (1000.0 / TileSystem.GroundResolutionMapSize(latitud, mapa)).toFloat()
            assertEquals(osmdroid, metrosAPixeles(1000f, latitud, mapa), osmdroid * 1e-5f)
        }
    }

    @Test
    fun `desplazar el origen solo desplaza la grilla`() {
        // Lo que se reusa al mover el mapa: el mismo calor, corrido las celdas que se movió.
        val x0 = xMundo(-58.9, mapa)
        val y0 = yMundo(-34.2, mapa)
        val base = calcularCalor(puntos, zoom, mapa, x0, y0, 120, 90, 3f)
        val corrida = calcularCalor(puntos, zoom, mapa, x0 + 10 * PASO, y0 + 7 * PASO, 120, 90, 3f)
        var conCalor = 0
        for (y in 0 until 83) for (x in 0 until 110) {
            assertEquals(base[x + 10, y + 7], corrida[x, y], 1e-4f)
            if (corrida[x, y] > 0f) conCalor++
        }
        assertTrue("la prueba tiene que mirar calor de verdad", conCalor > 100)
    }

    @Test
    fun `el margen no cambia el calor de lo visible`() {
        val x0 = xMundo(-58.9, mapa)
        val y0 = yMundo(-34.2, mapa)
        val justa = calcularCalor(puntos, zoom, mapa, x0, y0, 60, 60, 3f)
        val conMargen = calcularCalor(puntos, zoom, mapa, x0 - 30 * PASO, y0 - 30 * PASO, 120, 120, 3f)
        for (y in 0 until 60) for (x in 0 until 60) {
            assertEquals(justa[x, y], conMargen[x + 30, y + 30], 1e-4f)
        }
    }

    @Test
    fun `con zoom lejano un punto junto al antimeridiano aparece del otro lado`() {
        val lejos = 3.0
        val mundo = TileSystem.MapSize(lejos)
        val pacifico = listOf(PuntoMapa(-17.7, 179.9, 5, "Alto", null))
        // La grilla empieza justo al oeste de -180: el punto de 179,9° cae en su borde izquierdo.
        val origen = xMundo(-180.0, mundo) - 40.0
        val grilla = calcularCalor(pacifico, lejos, mundo, origen, yMundo(-10.0, mundo), 60, 60, 3f)
        val columnaDelPunto = ((xMundo(179.9, mundo) - mundo - origen) / PASO).toInt()
        val filaDelPunto = ((yMundo(-17.7, mundo) - yMundo(-10.0, mundo)) / PASO).toInt()
        assertTrue(grilla[columnaDelPunto, filaDelPunto] > 0f)
    }

    @Test
    fun `un icono se dibuja una vez y se reusa`() {
        var dibujos = 0
        val crear = { dibujos++; Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888) }
        val primero = CacheIconos.obtener("prueba:grupo:8", crear)
        val segundo = CacheIconos.obtener("prueba:grupo:8", crear)
        assertSame(primero, segundo)
        assertEquals(1, dibujos)
        CacheIconos.obtener("prueba:grupo:9", crear)
        assertEquals("otra cantidad es otro ícono", 2, dibujos)
    }

    private companion object {
        const val PASO = 4
    }
}
