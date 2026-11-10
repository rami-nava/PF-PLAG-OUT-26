package com.example.plag_out

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.google.gson.Gson
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerReporteScreenTest {

    @get:Rule val compose = createComposeRule()

    @org.junit.After
    fun tearDown() {
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
    }

    @Test
    fun `el detalle muestra el cultivo y la etapa biologica`() {
        val gddService = FakeGDDService().apply {
            getReporteResult = {
                Response.success(
                    Fixtures.reporteDetalle(id = 78).copy(cultivo_nombre = "Maíz", etapa_biologica = "HUEVO")
                )
            }
            getTerrenosResult = { Response.success(emptyList()) }
        }

        val viewModel = VerReporteViewModel(gddService)
        compose.setContent {
            MaterialTheme {
                VerReporteScreen(
                    reporteId = 78,
                    reporteJsonFallback = null,
                    viewModel = viewModel,
                    onBack = {},
                    onVerTerreno = {}
                )
            }
        }

        compose.onNodeWithText("Maíz").performScrollTo().assertExists()
        compose.onNodeWithText("Huevo").performScrollTo().assertExists()
    }

    @Test
    fun `al abrir un reporte retirado desde el mapa se muestra error y no su ficha guardada`() {
        val gddService = FakeGDDService().apply {
            getReporteResult = { FakeGDDService.errorServidor(404) }
            getTerrenosResult = { Response.success(emptyList()) }
        }
        val viewModel = VerReporteViewModel(gddService)
        val fichaDelMapa = Gson().toJson(
            ReporteNavPayload(
                id = 79,
                plaga_nombre = "Alerta comunitaria retirada",
                nivel_severidad = "Alto",
                latitud = null,
                longitud = null,
                timestamp_ms = 1700000000000L,
                es_propio = false
            )
        )
        compose.setContent {
            MaterialTheme {
                VerReporteScreen(
                    reporteId = 79,
                    reporteJsonFallback = fichaDelMapa,
                    ignorarLimite = true,
                    viewModel = viewModel,
                    onBack = {},
                    onVerTerreno = {}
                )
            }
        }

        compose.waitUntil(timeoutMillis = 15000) { viewModel.state.value !is VerReporteUiState.Cargando }
        compose.onNodeWithText("No se pudieron cargar los datos de la alerta.").assertExists()
        compose.onNodeWithText("Alerta comunitaria retirada").assertDoesNotExist()
    }

    @Test
    fun `muestra Nivel de severidad unificado y permite navegar a terrenos cercanos`() {
        val gddService = FakeGDDService().apply {
            getReporteResult = { FakeGDDService.sinConexion() }
            getTerrenosResult = { Response.success(emptyList()) }
        }
        val fichaComunitaria = Gson().toJson(
            ReporteNavPayload(
                id = 80,
                plaga_nombre = "Chicharrita del maíz",
                nivel_severidad = "Alto",
                latitud = null,
                longitud = null,
                timestamp_ms = 1700000000000L,
                es_propio = false,
                terreno_nombre = "Lote La Esperanza",
                terreno_mas_cercano_id = 42,
                distancia_km = 2.4f
            )
        )

        val viewModel = VerReporteViewModel(gddService)
        compose.setContent {
            MaterialTheme {
                VerReporteScreen(
                    reporteId = 80,
                    reporteJsonFallback = fichaComunitaria,
                    ignorarLimite = true,
                    viewModel = viewModel,
                    onBack = {},
                    onVerTerreno = {}
                )
            }
        }

        compose.onNodeWithText("Nivel de severidad").performScrollTo().assertExists()
        compose.onNodeWithText("Tus terrenos cercanos").performScrollTo().assertExists()
        compose.onNodeWithText("Lote La Esperanza", substring = true).performScrollTo().assertExists()
    }
}
