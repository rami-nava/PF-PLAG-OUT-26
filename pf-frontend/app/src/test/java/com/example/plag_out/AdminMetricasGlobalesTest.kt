package com.example.plag_out

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import org.junit.Assert.assertFalse
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

    @get:Rule
    val compose = createComposeRule()

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
            "cultivos": [{"cultivo": "Maíz", "cultivo_cientifico": "Zea mays", "monitoreos": 3,
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

    @Test
    fun `la pantalla de tableros renderiza lotes sin dias de siembra y campanas con monitoreos por cultivo`() {
        val gddService = FakeGDDService().apply {
            adminGetMetricasResult = { Response.success(metricas) }
            adminGetMetricasAlertasResult = { Response.success(MetricasAlertasResponse()) }
        }
        val viewModel = AdminMetricasViewModel(gddService, mainDispatcherRule.testDispatcher)

        compose.setContent {
            MaterialTheme {
                AdminMetricasScreen(viewModel = viewModel, global = true)
            }
        }

        // Pestaña Lotes: muestra plagas incidentes por cultivo sin "Sembrado hace X días"
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Plagas Incidentes por Cultivo"))
        compose.onNodeWithText("Plagas Incidentes por Cultivo").assertExists()
        compose.onNodeWithText("Sembrado hace", substring = true).assertDoesNotExist()
        compose.onNodeWithText("(30 días)", substring = true).assertDoesNotExist()

        // Cambiar a pestaña Campañas
        compose.onNodeWithText("Campañas").performClick()

        // En Campañas: muestra cultivo que afecta y monitoreos por cultivo, sin etapas ni eclosionados
        // En pantallas angostas (la de Robolectric lo es) la píldora se acorta a "PLAGA PREDOMINANTE".
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("PLAGA PREDOMINANTE", substring = true))
        compose.onNodeWithText("PLAGA PREDOMINANTE", substring = true).assertExists()
        compose.onNodeWithText("CULTIVO QUE AFECTA").assertExists()
        compose.onNodeWithText("YA ECLOSIONADOS", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("ETAPA TÍPICA", ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("Etapas de la campaña", ignoreCase = true).assertDoesNotExist()

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Monitoreos por Cultivo y Plaga"))
        compose.onNodeWithText("Monitoreos por Cultivo y Plaga").assertExists()
    }

    @Test
    fun `la pestaña Control del admin pide las alertas de todos los reportes a su endpoint`() {
        val gddAdmin = FakeGDDService().apply {
            adminGetMetricasResult = { Response.success(metricas) }
            adminGetMetricasAlertasResult = {
                Response.success(
                    MetricasAlertasResponse(
                        total = 2, alto = 1, medio = 1, bajo = 0,
                        plagas = listOf(PlagaAlertasResponse(1, "Oruga cogollera", total = 2, alto = 1, medio = 1))
                    )
                )
            }
        }
        val vmAdmin = AdminMetricasViewModel(gddAdmin, mainDispatcherRule.testDispatcher)
        vmAdmin.cargar(global = true)
        vmAdmin.cargarDatosControl(global = true)

        val estadoAdmin = esperarEstado(vmAdmin.state) { it.metricasAlertas != null }
        assertEquals(2, estadoAdmin.metricasAlertas!!.totalAlertas)
        assertEquals(1, estadoAdmin.metricasAlertas!!.severidadAlta)
        assertEquals(1, estadoAdmin.metricasAlertas!!.severidadMedia)
        assertEquals("Oruga cogollera", estadoAdmin.metricasAlertas!!.plagasReportadas.single().plagaNombre)
        assertEquals(100.0f, estadoAdmin.metricasAlertas!!.plagasReportadas.single().porcentaje, 0.01f)
        assertFalse(estadoAdmin.alertasNoDisponibles)
        assertFalse("getMetricasAlertas" in gddAdmin.llamadas)
        assertFalse("getReportesMapa" in gddAdmin.llamadas)
    }

    @Test
    fun `la pestaña Control del productor pide sus alertas a su endpoint`() {
        val gdd = FakeGDDService().apply {
            getMetricasAlertasResult = {
                Response.success(
                    MetricasAlertasResponse(
                        total = 3, alto = 0, medio = 1, bajo = 2,
                        plagas = listOf(PlagaAlertasResponse(5, "Chicharrita del maíz", total = 3, medio = 1, bajo = 2))
                    )
                )
            }
        }
        val vm = AdminMetricasViewModel(gdd, mainDispatcherRule.testDispatcher)
        vm.cargarDatosControl(global = false)

        val estado = esperarEstado(vm.state) { it.metricasAlertas != null }
        assertEquals(3, estado.metricasAlertas!!.totalAlertas)
        assertEquals(2, estado.metricasAlertas!!.severidadBaja)
        assertFalse("adminGetMetricasAlertas" in gdd.llamadas)
        assertFalse("getReportes" in gdd.llamadas)
    }

    @Test
    fun `si el endpoint de alertas falla la tarjeta queda como no disponible`() {
        val gdd = FakeGDDService().apply {
            getMetricasAlertasResult = { Response.error(404, "{\"detail\":\"Not Found\"}".toResponseBody(null)) }
        }
        val vm = AdminMetricasViewModel(gdd, mainDispatcherRule.testDispatcher)
        vm.cargarDatosControl(global = false)

        val estado = esperarEstado(vm.state) { it.alertasNoDisponibles }
        assertNull(estado.metricasAlertas)
    }

    // Forma de GET /admin/reportes/metricas con el desglose por campaña.
    private val jsonAlertas = """
        {
          "total": 8, "alto": 3, "medio": 2, "bajo": 2,
          "plagas": [
            {"plaga_id": 1, "plaga_nombre": "Oruga cogollera", "total": 6, "alto": 3, "medio": 2, "bajo": 1},
            {"plaga_id": 2, "plaga_nombre": "Chinche verde",   "total": 2, "alto": 0, "medio": 0, "bajo": 1}
          ],
          "campanias": [
            {
              "nombre": "2026/2027", "anio_inicio": 2026,
              "total": 5, "alto": 2, "medio": 0, "bajo": 2,
              "plagas": [
                {"plaga_id": 2, "plaga_nombre": "Chinche verde",   "total": 2, "alto": 0, "medio": 0, "bajo": 1},
                {"plaga_id": 1, "plaga_nombre": "Oruga cogollera", "total": 3, "alto": 2, "medio": 0, "bajo": 1}
              ]
            },
            {
              "nombre": "2024/2025", "anio_inicio": 2024,
              "total": 3, "alto": 1, "medio": 2, "bajo": 0,
              "plagas": [
                {"plaga_id": 1, "plaga_nombre": "Oruga cogollera", "total": 3, "alto": 1, "medio": 2, "bajo": 0}
              ]
            }
          ]
        }
    """.trimIndent()

    private val alertas: MetricasAlertasResponse =
        GsonBuilder().create().fromJson(jsonAlertas, MetricasAlertasResponse::class.java)

    @Test
    fun `las alertas traen el resumen de cada campaña con su propio ranking`() {
        val resumen = resumenDeMetricasAlertas(alertas)

        // Los totales de primer nivel siguen igual que antes.
        assertEquals(8, resumen.totalAlertas)
        assertEquals("Oruga cogollera", resumen.plagasReportadas.first().plagaNombre)

        val actual = resumen.porCampaña!!.getValue("2026/2027")
        assertEquals(5, actual.totalAlertas)
        assertEquals(2, actual.severidadAlta)
        // El backend no garantiza orden: se ordena y el porcentaje es sobre el total de la campaña.
        assertEquals(listOf("Oruga cogollera", "Chinche verde"), actual.plagasReportadas.map { it.plagaNombre })
        assertEquals(60f, actual.plagasReportadas.first().porcentaje, 0.01f)
        assertEquals(3, resumen.porCampaña!!.getValue("2024/2025").totalAlertas)
    }

    @Test
    fun `un backend sin desglose por campaña deja porCampaña en null`() {
        val viejo = resumenDeMetricasAlertas(MetricasAlertasResponse(total = 1, alto = 1))
        assertNull(viejo.porCampaña)
        assertTrue(resumenDeMetricasAlertas(MetricasAlertasResponse(campanias = emptyList())).porCampaña!!.isEmpty())
    }

    @Test
    fun `abrir la pestaña Campañas pide las alertas sin pedir tratamientos`() {
        val gdd = FakeGDDService().apply {
            adminGetMetricasResult = { Response.success(metricas) }
            adminGetMetricasAlertasResult = { Response.success(alertas) }
        }
        val vm = AdminMetricasViewModel(gdd, mainDispatcherRule.testDispatcher)
        vm.cambiarPestaña(PestañaDashboard.HISTORICO)
        vm.cargar(global = true)

        val estado = esperarEstado(vm.state) { it.metricasAlertas != null }
        assertEquals(5, estado.metricasAlertas!!.porCampaña!!.getValue("2026/2027").totalAlertas)
        assertEquals(listOf("adminGetMetricas", "adminGetMetricasAlertas"), gdd.llamadas)

        // Volver a pedirlas (otra pestaña que las usa) no repite la llamada.
        vm.cargarAlertas()
        assertEquals(1, gdd.llamadas.count { it == "adminGetMetricasAlertas" })
    }

    @Test
    fun `la pestaña Campañas muestra las alertas de la campaña elegida`() {
        val gdd = FakeGDDService().apply {
            adminGetMetricasResult = { Response.success(metricas) }
            adminGetMetricasAlertasResult = { Response.success(alertas) }
        }
        val vm = AdminMetricasViewModel(gdd, mainDispatcherRule.testDispatcher)

        compose.setContent {
            MaterialTheme {
                AdminMetricasScreen(viewModel = vm, global = true)
            }
        }
        compose.onNodeWithText("Campañas").performClick()

        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Alertas de la campaña"))
        compose.onNodeWithText("Alertas de la campaña").assertExists()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("3 alertas (60%)"))
        compose.onNodeWithText("3 alertas (60%)").assertExists()

        // 2025/2026 tiene monitoreos pero ningún reporte: la tarjeta lo dice en vez de quedar vacía.
        vm.seleccionarCampaña("2025/2026")
        compose.onNode(hasScrollToNodeAction())
            .performScrollToNode(hasText("No se registraron alertas en la campaña 2025/2026."))
        compose.onNodeWithText("No se registraron alertas en la campaña 2025/2026.").assertExists()
    }

    @Test
    fun `si el backend no manda campanias la tarjeta de la campaña lo aclara`() {
        val gdd = FakeGDDService().apply {
            adminGetMetricasResult = { Response.success(metricas) }
            adminGetMetricasAlertasResult = { Response.success(MetricasAlertasResponse(total = 2, alto = 2)) }
        }
        val vm = AdminMetricasViewModel(gdd, mainDispatcherRule.testDispatcher)

        compose.setContent {
            MaterialTheme {
                AdminMetricasScreen(viewModel = vm, global = true)
            }
        }
        compose.onNodeWithText("Campañas").performClick()

        val texto = "El detalle de alertas por campaña todavía no está disponible."
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(texto))
        compose.onNodeWithText(texto).assertExists()
    }
}
