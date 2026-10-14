package com.example.plag_out

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdminMetricasModelTest {

    @Test
    fun `determinarCampaña asigna correctamente segun mes de corte de julio`() {
        // En o después del 1 de julio: año / año+1
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2025, 7, 1)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2025, 10, 15)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2025, 12, 31)))

        // Antes del 1 de julio: año-1 / año
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2026, 1, 1)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2026, 3, 20)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2026, 6, 30)))

        // Nueva campaña a partir de julio
        assertEquals("2026/2027", determinarCampaña(LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun `extraerAñoInicioCampaña parsea el año inicial`() {
        assertEquals(2025, extraerAñoInicioCampaña("2025/2026"))
        assertEquals(2026, extraerAñoInicioCampaña("2026/2027"))
        assertEquals(0, extraerAñoInicioCampaña("invalido"))
    }

    @Test
    fun `agruparPorCampaña con lista vacia retorna lista vacia`() {
        val resultado = agruparPorCampaña(emptyList())
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `agruparPorCampaña calcula plagas lideres e indices de riesgo correctamente`() {
        val terreno1 = TerrenoResponse(
            terreno_id = 1,
            terreno_nombre = "Lote Norte",
            terreno_latitud = -31.4f,
            terreno_longitud = -64.1f,
            terreno_area = 50.0f
        )
        val terreno2 = TerrenoResponse(
            terreno_id = 2,
            terreno_nombre = "Lote Sur",
            terreno_latitud = -31.5f,
            terreno_longitud = -64.2f,
            terreno_area = 30.0f
        )

        val monitoreos = listOf(
            // Campaña 2025/2026: 3 monitoreos (2 de Oruga cogollera, 1 de Barrenador)
            MonitoreoResponse(
                monitoreo_id = 1,
                plantacion_id = 1,
                terreno_id = 1,
                terreno_nombre = "Lote Norte",
                cultivo_nombre = "Maíz",
                plaga_nombre = "Oruga cogollera",
                plaga_nombre_cientifico = "Spodoptera frugiperda",
                gdd_diario = 12f,
                gdd_acumulado = 150f,
                gdd_objetivo = 300f,
                progreso = 50f,
                nivel_alerta = 2, // Alto
                fecha_actualizacion = LocalDate.of(2025, 11, 10)
            ),
            MonitoreoResponse(
                monitoreo_id = 2,
                plantacion_id = 1,
                terreno_id = 1,
                terreno_nombre = "Lote Norte",
                cultivo_nombre = "Maíz",
                plaga_nombre = "Oruga cogollera",
                plaga_nombre_cientifico = "Spodoptera frugiperda",
                gdd_diario = 10f,
                gdd_acumulado = 80f,
                gdd_objetivo = 300f,
                progreso = 26f,
                nivel_alerta = 0, // Bajo
                fecha_actualizacion = LocalDate.of(2025, 12, 5)
            ),
            MonitoreoResponse(
                monitoreo_id = 3,
                plantacion_id = 2,
                terreno_id = 2,
                terreno_nombre = "Lote Sur",
                cultivo_nombre = "Soja",
                plaga_nombre = "Barrenador del tallo",
                plaga_nombre_cientifico = "Diatraea saccharalis",
                gdd_diario = 8f,
                gdd_acumulado = 220f,
                gdd_objetivo = 300f,
                progreso = 73f,
                nivel_alerta = 1, // Moderado
                fecha_actualizacion = LocalDate.of(2026, 2, 14)
            ),
            // Campaña 2024/2025: 1 monitoreo
            MonitoreoResponse(
                monitoreo_id = 4,
                plantacion_id = 1,
                terreno_id = 1,
                terreno_nombre = "Lote Norte",
                cultivo_nombre = "Maíz",
                plaga_nombre = "Gusano cogollero",
                gdd_diario = 11f,
                gdd_acumulado = 290f,
                gdd_objetivo = 300f,
                progreso = 96f,
                nivel_alerta = 2, // Alto
                fecha_actualizacion = LocalDate.of(2024, 11, 20)
            )
        )

        val resultado = agruparPorCampaña(monitoreos, listOf(terreno1, terreno2))

        assertEquals(2, resultado.size)

        // Campaña 2025/2026 (más reciente primero)
        val c2025 = resultado[0]
        assertEquals("2025/2026", c2025.nombre)
        assertEquals(3, c2025.totalMonitoreos)
        assertEquals(80.0f, c2025.hectareasMonitoreadas, 0.01f) // Terreno 1 (50) + Terreno 2 (30)

        // Plaga líder de la campaña
        assertNotNull(c2025.plagaPredominante)
        assertEquals("Oruga cogollera", c2025.plagaPredominante?.plagaNombre)
        assertEquals("Spodoptera frugiperda", c2025.plagaPredominante?.plagaCientifico)
        assertEquals(2, c2025.plagaPredominante?.totalMonitoreos)
        assertEquals(66.66f, c2025.plagaPredominante?.porcentajeIncidencia ?: 0f, 0.1f)
        assertEquals(1, c2025.plagaPredominante?.enRiesgoAlto)

        // Distribución de riesgo en la campaña 2025/2026: 1 bajo, 1 moderado, 1 alto
        assertEquals(1, c2025.riesgoBajo)
        assertEquals(1, c2025.riesgoModerado)
        assertEquals(1, c2025.riesgoAlto)
        assertEquals(33.33f, c2025.porcentajeRiesgoBajo, 0.1f)
        assertEquals(33.33f, c2025.porcentajeRiesgoModerado, 0.1f)
        assertEquals(33.33f, c2025.porcentajeRiesgoAlto, 0.1f)

        // Campaña 2024/2025
        val c2024 = resultado[1]
        assertEquals("2024/2025", c2024.nombre)
        assertEquals(1, c2024.totalMonitoreos)
        assertEquals(50.0f, c2024.hectareasMonitoreadas, 0.01f)
        assertEquals("Gusano cogollero", c2024.plagaPredominante?.plagaNombre)
        assertEquals(1, c2024.riesgoAlto)
    }
}
