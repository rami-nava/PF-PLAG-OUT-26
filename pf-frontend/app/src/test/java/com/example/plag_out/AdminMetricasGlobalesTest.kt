package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.LocalDate

/** Admin dashboards from GET /admin/metricas instead of the admin's own (empty) field data. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminMetricasGlobalesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // Same shape the backend sends (api/modules/users/admin_schemas.py, MetricasAdmin).
    private val json = """
        {
          "generado_en": "2026-10-19T15:30:00Z",
          "fecha_referencia": "2026-10-19",
          "monitoreos_activos": 3,
          "monitoreos_esperando_biofix": 1,
          "semaforo": {
            "verde": {"lotes": 1, "hectareas": 10.0},
            "amarillo": {"lotes": 0, "hectareas": 0.0},
            "rojo": {"lotes": 1, "hectareas": 30.0},
            "esperando_biofix": {"lotes": 1, "hectareas": 5.0},
            "sin_monitoreo_activo": {"lotes": 2, "hectareas": 7.5},
            "total_lotes": 2,
            "total_hectareas": 40.0
          },
          "cultivos": [{
            "cultivo": "Maíz", "cultivo_cientifico": "Zea mays", "lotes": 2, "hectareas": 40.0,
            "monitoreos_riesgo_alto": 1, "plaga_mas_frecuente": "Cogollero",
            "fecha_siembra": "2026-09-01", "dias_desde_siembra": 48,
            "plagas": [{"plaga": "Cogollero", "plaga_cientifico": null, "lotes": 2, "porcentaje": 100.0, "casos_alerta": 1}]
          }],
          "lotes_criticos": [{
            "cultivo": "Maíz", "plaga": "Cogollero", "plaga_cientifico": "Spodoptera frugiperda",
            "hectareas": 30.0, "nivel_alerta": 2, "progreso": 62.5, "gdd_acumulado": 75.0,
            "gdd_objetivo": 120.0, "urgencia": "eclosionada", "dias_desde_siembra": null
          }, {
            "cultivo": "Maíz", "plaga": "Cogollero", "plaga_cientifico": null,
            "hectareas": 10.0, "nivel_alerta": 1, "progreso": 82.0, "gdd_acumulado": 98.4,
            "gdd_objetivo": 120.0, "urgencia": "eclosion_cerca", "dias_desde_siembra": 30
          }],
          "ventanas": [{
            "cultivo": "Maíz", "plaga": "Cogollero", "plaga_cientifico": null,
            "progreso": 100.0, "estadio": "Larva", "estado": "eclosionada"
          }],
          "eficacia": {"ciclos_archivados": 0, "exitosos": 0, "tasa": null, "monitoreos_con_rebrote": 0},
          "tratamientos": {
            "registrados": 2, "evaluados": 1, "con_testigo": 1,
            "cita": "Henderson & Tilton 1955, J. Econ. Entomol. 48(2):157-161, doi:10.1093/jee/48.2.157",
            "productos": [{"producto": "Clorantraniliprole", "tratamientos": 2, "evaluados": 1, "con_testigo": 1,
                           "eficacia_promedio": 87.5, "reduccion_observada_promedio": null}]
          },
          "historico": {"monitoreos": 4, "hectareas": 45.0, "campanias": 2, "plaga_lider": "Cogollero"},
          "campanias": [{
            "nombre": "2026/2027", "anio_inicio": 2026, "monitoreos": 3, "hectareas": 40.0,
            "plaga_predominante": "Cogollero", "riesgo_bajo": 1, "riesgo_moderado": 0, "riesgo_alto": 1,
            "esperando_biofix": 1,
            "plagas": [{"plaga": "Cogollero", "plaga_cientifico": null, "monitoreos": 3, "incidencia": 100.0,
                        "riesgo_alto": 1, "porcentaje_riesgo_alto": null, "nivel_promedio": null, "cultivos": ["Maíz"]}],
            "cultivos": [{"cultivo": "Maíz", "cultivo_cientifico": "Zea mays", "monitoreos": 3, "hectareas": 40.0,
                          "plagas": [{"plaga": "Cogollero", "plaga_cientifico": null, "monitoreos": 3, "porcentaje": 100.0}]}]
          }, {
            "nombre": "2025/2026", "anio_inicio": 2025, "monitoreos": 1, "hectareas": 5.0,
            "plaga_predominante": null, "riesgo_bajo": 0, "riesgo_moderado": 0, "riesgo_alto": 0,
            "esperando_biofix": 0, "plagas": [], "cultivos": []
          }]
        }
    """.trimIndent()

    private val metricas: MetricasAdmin = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { value, _, _ -> LocalDate.parse(value.asString) })
        .create()
        .fromJson(json, MetricasAdmin::class.java)

    @Test
    fun `el contrato del backend se lee completo`() {
        assertEquals(1, metricas.monitoreos_esperando_biofix)
        assertEquals(LocalDate.of(2026, 9, 1), metricas.cultivos.single().fecha_siembra)
        val producto = metricas.tratamientos!!.productos.single()
        assertEquals(87.5f, producto.eficacia_promedio!!, 0.01f)
        assertNull(producto.reduccion_observada_promedio)
        assertNull(metricas.lotes_criticos.first().dias_desde_siembra)
    }

    @Test
    fun `las metricas globales llenan los mismos modelos de la pantalla`() {
        val estado = AdminMetricasUIState().conMetricasGlobales(metricas)

        val semaforo = estado.semaforoHectareas!!
        assertEquals(30f, semaforo.hectareasRojo)
        assertEquals(2, semaforo.totalLotes)
        assertEquals(75f, semaforo.porcentajeRojo, 0.01f)

        val (critico, inminente) = estado.lotesCriticos
        assertEquals("Lote anónimo 1", critico.terrenoNombre)
        assertEquals(2, critico.nivelAlerta)
        assertEquals(82f, inminente.progresoGdd, 0.01f)
        assertTrue(estado.lotesCriticos.all { it.monitoreoId < 0 })

        val ventana = estado.ventanasControl.single()
        assertEquals(EstadoVentanaControl.ECLOSIONADA, ventana.estadoVentana)
        assertTrue(ventana.requiereRecorrida)
        assertEquals("Larva", ventana.estadioEstimado)

        // La eficacia sale de los tratamientos registrados, no de los ciclos archivados.
        assertEquals(2, estado.tratamientos!!.registrados)
        assertEquals(1, semaforo.lotesEsperandoBiofix)
        assertEquals(2, semaforo.lotesSinMonitoreo)
        assertEquals(7.5f, semaforo.hectareasSinMonitoreo, 0.01f)

        assertEquals(listOf("2026/2027", "2025/2026"), estado.campañas.map { it.nombre })
        assertEquals("2026/2027", estado.campañaSeleccionada?.nombre)
        assertEquals("Cogollero", estado.campañas.first().plagaPredominante?.plagaNombre)
        assertEquals(listOf("Maíz"), estado.campañas.first().plagaPredominante?.cultivosAfectados)
        assertEquals(1, estado.campañas.first().cultivos.size)
        assertEquals("Maíz", estado.campañas.first().cultivos.single().cultivoNombre)
        assertEquals(3, estado.campañas.first().cultivos.single().totalMonitoreos)
        assertEquals("Cogollero", estado.campañas.first().cultivos.single().plagas.single().plagaNombre)
        assertEquals(0f, estado.campañas.first().plagas.single().porcentajeRiesgoAlto)
        assertNull(estado.campañas.last().plagaPredominante)
        assertEquals("Cogollero", estado.plagaHistoricaLider)
        assertEquals(45f, estado.totalHectareasHistoricas)
    }

    @Test
    fun `se mantiene la campaña elegida si sigue existiendo`() {
        val antes = AdminMetricasUIState().conMetricasGlobales(metricas)
        val elegida = antes.copy(campañaSeleccionada = antes.campañas.last())

        assertEquals("2025/2026", elegida.conMetricasGlobales(metricas).campañaSeleccionada?.nombre)
    }

    @Test
    fun `el admin carga las metricas globales y no sus propios monitoreos`() {
        val gddService = FakeGDDService()
        gddService.adminGetMetricasResult = { Response.success(metricas) }
        val viewModel = AdminMetricasViewModel(gddService, mainDispatcherRule.testDispatcher)

        viewModel.cargar(global = true)

        val estado = esperarEstado(viewModel.state) { it.campañas.isNotEmpty() }
        assertEquals(2, estado.campañas.size)
        assertNull(estado.error)
        assertEquals(listOf("adminGetMetricas"), gddService.llamadas)
    }

    @Test
    fun `un error del backend se muestra en el banner`() {
        val gddService = FakeGDDService()
        gddService.adminGetMetricasResult = {
            Response.error(403, """{"detail":"requiere_admin"}""".toResponseBody("application/json".toMediaType()))
        }
        val viewModel = AdminMetricasViewModel(gddService, mainDispatcherRule.testDispatcher)

        viewModel.cargar(global = true)

        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertTrue(estado.campañas.isEmpty())
    }
}
