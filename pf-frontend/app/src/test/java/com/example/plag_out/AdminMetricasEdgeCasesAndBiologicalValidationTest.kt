package com.example.plag_out

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Pruebas unitarias de casos borde, validación biológica y límites geográficos
 * para métricas de administración y control agronómico.
 */
class AdminMetricasEdgeCasesAndBiologicalValidationTest {

    @Test
    fun `calcularSemaforoHectareas con cero lotes y cero hectareas no genera NaN ni division por cero`() {
        val semaforo = calcularSemaforoHectareas(
            monitoreos = emptyList(),
            terrenos = emptyList()
        )

        assertEquals(0f, semaforo.totalHectareas, 1e-5f)
        assertEquals(0, semaforo.totalLotes)
        assertEquals(0f, semaforo.hectareasVerde, 1e-5f)
        assertEquals(0f, semaforo.hectareasAmarillo, 1e-5f)
        assertEquals(0f, semaforo.hectareasRojo, 1e-5f)
        assertEquals(0, semaforo.lotesEsperandoBiofix)
        assertEquals(0f, semaforo.hectareasEsperandoBiofix, 1e-5f)
        assertEquals(0, semaforo.lotesSinMonitoreo)
        assertEquals(0f, semaforo.hectareasSinMonitoreo, 1e-5f)
        assertFalse("El total no debe ser NaN", semaforo.totalHectareas.isNaN())
        assertEquals(0f, semaforo.porcentajeVerde, 1e-5f)
        assertEquals(0f, semaforo.porcentajeAmarillo, 1e-5f)
        assertEquals(0f, semaforo.porcentajeRojo, 1e-5f)
    }

    @Test
    fun `calcularSemaforoHectareas calcula correctamente lotes sin monitoreo y esperando biofix`() {
        val terrenos = listOf(
            TerrenoResponse(10, "Lote Norte", -31.4f, -64.1f, 150.0f),
            TerrenoResponse(20, "Lote Sur", -31.5f, -64.2f, 80.0f),
            TerrenoResponse(30, "Lote Barbecho", -31.6f, -64.3f, 45.0f)
        )

        // Monitoreo 1: activo pero esperando biofix (ciclos vacíos)
        val monEsperando = MonitoreoResponse(
            monitoreo_id = 1, plantacion_id = 101, terreno_id = 10, terreno_nombre = "Lote Norte",
            cultivo_nombre = "Maíz", plaga_id = 1, plaga_nombre = "Dalbulus maidis", gdd_objetivo = 100f,
            ciclos = emptyList()
        )

        // Monitoreo 2: activo con ciclo en alerta amarilla
        val cicloActivo = GddCicloResponse(
            id = 2, monitoreo_id = 2, fecha_biofix = "2026-10-01", estado = "activo",
            fecha_actualizacion = "2026-10-15", gdd_acumulado = 145f, gdd_diario = 12f,
            gdd_eclosion = 120f, gdd_generacion = 350f, progreso = 85f, estadio_biologico = "Ninfas",
            nivel_alerta = 1, dias_pendientes = 0
        )
        val monActivo = MonitoreoResponse(
            monitoreo_id = 2, plantacion_id = 102, terreno_id = 20, terreno_nombre = "Lote Sur",
            cultivo_nombre = "Maíz", plaga_id = 2, plaga_nombre = "Dalbulus maidis", gdd_objetivo = 120f,
            ciclos = listOf(cicloActivo)
        )

        val semaforo = calcularSemaforoHectareas(listOf(monEsperando, monActivo), terrenos)

        assertEquals(80.0f, semaforo.hectareasAmarillo, 1e-4f)
        assertEquals(1, semaforo.lotesAmarillo)
        assertEquals(1, semaforo.lotesEsperandoBiofix)
        assertEquals(150.0f, semaforo.hectareasEsperandoBiofix, 1e-4f)
        assertEquals(1, semaforo.lotesSinMonitoreo)
        assertEquals(45.0f, semaforo.hectareasSinMonitoreo, 1e-4f)
    }

    @Test
    fun `etiquetaDeEtapa distingue correctamente entre ciclos normales y viviparos`() {
        val ventanaHuevo = VentanaControlBiologico(
            monitoreoId = 1,
            terrenoNombre = "Lote 1",
            cultivoNombre = "Maíz",
            plagaNombre = "Spodoptera frugiperda",
            plagaCientifico = null,
            progresoGdd = 20f,
            estadioEstimado = "Huevo",
            requiereRecorrida = false,
            estadoVentana = EstadoVentanaControl.HUEVO,
            recomendacionTecnica = "Monitorear ovipostura",
            hito = "Eclosión de huevos"
        )
        val ventanaCerca = VentanaControlBiologico(
            monitoreoId = 2,
            terrenoNombre = "Lote 2",
            cultivoNombre = "Maíz",
            plagaNombre = "Helicoverpa zea",
            plagaCientifico = null,
            progresoGdd = 85f,
            estadioEstimado = "Huevo maduro",
            requiereRecorrida = true,
            estadoVentana = EstadoVentanaControl.ECLOSION_CERCA,
            recomendacionTecnica = "Preparar control",
            hito = "Eclosión de huevos"
        )
        val ventanaEclosion = VentanaControlBiologico(
            monitoreoId = 3,
            terrenoNombre = "Lote 3",
            cultivoNombre = "Maíz",
            plagaNombre = "Helicoverpa zea",
            plagaCientifico = null,
            progresoGdd = 105f,
            estadioEstimado = "Larva L1",
            requiereRecorrida = true,
            estadoVentana = EstadoVentanaControl.ECLOSIONADA,
            recomendacionTecnica = "Controlar orugas neonatas",
            hito = "Eclosión de huevos"
        )

        // En orugas (holometábolos normales) debe mostrar la terminología de eclosión
        assertEquals("Huevo", etiquetaDeEtapa(EstadoVentanaControl.HUEVO, listOf(ventanaHuevo)))
        assertEquals("Eclosión cerca", etiquetaDeEtapa(EstadoVentanaControl.ECLOSION_CERCA, listOf(ventanaCerca)))
        assertEquals("Eclosionó", etiquetaDeEtapa(EstadoVentanaControl.ECLOSIONADA, listOf(ventanaEclosion)))
    }

    @Test
    fun `validacion de coordenadas limites para Argentina`() {
        fun esValida(lat: Double, lon: Double): Boolean =
            lat in -55.0..-21.8 && lon in -73.6..-53.6

        // Puntos extremos y agrícolas reales de Argentina (válidos)
        assertTrue("La Quiaca, Jujuy", esValida(-22.10, -65.59))
        assertTrue("Pergamino, Buenos Aires", esValida(-33.89, -60.57))
        assertTrue("Burruyacú, Tucumán", esValida(-26.50, -64.74))
        assertTrue("Río Cuarto, Córdoba", esValida(-33.12, -64.35))
        assertTrue("Ushuaia, Tierra del Fuego", esValida(-54.80, -68.30))
        assertTrue("Bernardo de Irigoyen, Misiones", esValida(-26.25, -53.64))

        // Puntos fuera de Argentina (inválidos por superar el rectángulo limítrofe nacional)
        assertFalse("Océano Pacífico oeste", esValida(-33.45, -80.00)) // Muy al oeste (lon < -73.6)
        assertFalse("Océano Atlántico este", esValida(-38.00, -50.00)) // Muy al este (lon > -53.6)
        assertFalse("Bolivia central", esValida(-17.80, -63.18)) // Muy al norte (lat > -21.8)
        assertFalse("Antártida sur", esValida(-65.00, -64.00)) // Muy al sur (lat < -55.0)
        assertFalse("Ecuador / Trópico", esValida(0.0, -60.0))
        assertFalse("Greenwich", esValida(51.5, 0.0))
    }
}
