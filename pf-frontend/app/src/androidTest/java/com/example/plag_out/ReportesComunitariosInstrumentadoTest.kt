package com.example.plag_out

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class ReportesComunitariosInstrumentadoTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun la_lista_abre_el_reporte_comunitario_y_oculta_un_detalle_retirado() {
        val propio = Fixtures.reporteDetalle(id = 9802, plagaNombre = "Alerta propia de prueba")
        val ajeno = Fixtures.reporteDetalle(
            id = 9801,
            plantacionId = null,
            plagaNombre = "Alerta comunitaria de prueba",
            terrenoNombre = null,
            latitud = -34.65,
            longitud = -58.45,
            esPropio = false
        ).copy(etapa_biologica = "ADULTO", distancia_km = 3f, terreno_mas_cercano_id = 1)
        val service = FakeGDDService().apply {
            getUsuarioActualResult = { Response.success(Fixtures.usuario()) }
            getTerrenosResult = { Response.success(listOf(Fixtures.terreno(id = 1, nombre = "Mi lote de prueba"))) }
            getPlagasResult = { Response.success(listOf(Fixtures.plaga())) }
            reportesServidor = listOf(propio, ajeno)
            getReporteResult = { Response.success(ajeno) }
        }
        val listado = MisReportesViewModel(service)
        compose.setContent {
            MaterialTheme {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "reportes") {
                    composable("reportes") { MisReportesScreen(listado, nav) }
                    composable("ver_reporte/{reporte_id}/{reporte_json}") { entrada ->
                        VerReporteScreen(
                            reporteId = requireNotNull(entrada.arguments?.getString("reporte_id")).toInt(),
                            reporteJsonFallback = entrada.arguments?.getString("reporte_json"),
                            viewModel = viewModel(factory = VerReporteViewModelFactory(service)),
                            onBack = { nav.popBackStack() },
                            onVerTerreno = {}
                        )
                    }
                }
            }
        }

        compose.waitUntilExactlyOneExists(hasTestTag("tarjetaReporte_9802"), timeoutMillis = 8_000)
        compose.onNodeWithTag("tarjetaReporte_9801").assertDoesNotExist()
        compose.onNodeWithTag("tabReportesComunidad").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("tarjetaReporte_9801"), timeoutMillis = 8_000)
        compose.onNodeWithTag("tarjetaReporte_9802").assertDoesNotExist()
        compose.onNodeWithTag("tarjetaReporte_9801").performClick()
        compose.waitUntilExactlyOneExists(hasText("Alerta comunidad"), timeoutMillis = 8_000)
        compose.onNodeWithTag("btnMenuReporte").assertDoesNotExist()
        compose.onNodeWithText("Coordenadas aproximadas").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tus terrenos cercanos").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Ubicación aproximada de la alerta").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("mapaReporte").performScrollTo().assertIsDisplayed()

        compose.runOnIdle { service.getReporteResult = { FakeGDDService.errorServidor(404) } }
        compose.onNodeWithTag("btnVolverVerReporte").performClick()
        compose.waitUntilExactlyOneExists(hasTestTag("tarjetaReporte_9801"), timeoutMillis = 8_000)
        compose.onNodeWithTag("tarjetaReporte_9801").performClick()
        val mensaje = "No se pudieron cargar los datos de la alerta."
        compose.waitUntilExactlyOneExists(hasText(mensaje), timeoutMillis = 8_000)
        compose.onNodeWithText(mensaje).assertIsDisplayed()
        compose.onNodeWithText(ajeno.plaga_nombre).assertDoesNotExist()
        compose.onNodeWithTag("mapaReporte").assertDoesNotExist()
    }
}
