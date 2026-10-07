package com.example.plag_out

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w411dp-h1000dp")
class MisReportesScreenTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var viewModel: MisReportesViewModel

    @After
    fun limpiar() {
        viewModel.viewModelScope.cancel()
    }

    private fun cambiarPestana(tag: String) {
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    @Test
    fun `filtros independientes se conservan al cargar o fallar y solo se limpian si desaparecen`() {
        val reportes = listOf(
            Fixtures.reporteDetalle(id = 1, terrenoNombre = "Lote Norte"),
            Fixtures.reporteDetalle(id = 2, terrenoNombre = "Lote Sur"),
            Fixtures.reporteDetalle(id = 3, esPropio = false, plagaNombre = "Plaga A"),
            Fixtures.reporteDetalle(id = 4, esPropio = false, plagaNombre = "Plaga B")
        )
        val service = FakeGDDService().apply {
            getUsuarioActualResult = { Response.success(Fixtures.usuario()) }
            getReportesResult = { Response.success(reportes) }
        }
        viewModel = MisReportesViewModel(service, reportesDispatcher = Dispatchers.Main.immediate)
        compose.setContent { MaterialTheme { MisReportesScreen(viewModel, rememberNavController()) } }
        compose.onNodeWithTag("btnToggleFiltros").performClick()
        compose.onNodeWithText("Lote Norte").performClick()
        compose.onNodeWithText("Mostrando 1 de 2 (Filtros activos)").assertExists()
        cambiarPestana("tabReportesComunidad")
        compose.onNodeWithText("Radio configurado: 20 km").assertExists()
        compose.onNode(hasText("Plaga A") and hasAnyAncestor(hasTestTag("panelFiltros"))).performClick()
        compose.onNodeWithText("Mostrando 1 de 2 (Filtros activos)").assertExists()

        // Cambiar fechas no debe perder selecciones que aún aparecen en la respuesta.
        compose.runOnIdle {
            viewModel.actualizarFechas(viewModel.state.value.fechaDesde.minusDays(1), viewModel.state.value.fechaHasta)
        }
        compose.onNodeWithText("Mostrando 1 de 2 (Filtros activos)").assertExists()
        cambiarPestana("tabReportesPropios")
        compose.onNodeWithText("Mostrando 1 de 2 (Filtros activos)").assertExists()

        val pendiente = CompletableDeferred<Response<List<ReporteDetalleResponse>>>()
        service.getReportesPaginaResult = { _, _, _, _ -> pendiente.await() }
        compose.runOnIdle {
            viewModel.actualizarFechas(viewModel.state.value.fechaDesde.minusDays(1), viewModel.state.value.fechaHasta)
        }
        compose.onNodeWithText("Mostrando 0 de 0 (Filtros activos)").assertExists()
        compose.runOnIdle { pendiente.complete(FakeGDDService.errorServidor(500)) }
        compose.onNodeWithText("Mostrando 0 de 0 (Filtros activos)").assertExists()
        service.getReportesPaginaResult = null
        compose.onNodeWithTag("reintentarCargaReportes").performClick()
        compose.onNodeWithText("Mostrando 1 de 2 (Filtros activos)").assertExists()
        cambiarPestana("tabReportesComunidad")
        compose.onNodeWithText("Mostrando 1 de 2 (Filtros activos)").assertExists()

        // Una respuesta completa sin las opciones elegidas limpia ambas pestañas.
        service.getReportesResult = { Response.success(listOf(reportes[1], reportes[3])) }
        compose.runOnIdle { viewModel.refrescar() }
        compose.onNodeWithText("Mostrando 1 reporte").assertExists()
        cambiarPestana("tabReportesPropios")
        compose.onNodeWithText("Mostrando 1 reporte").assertExists()
    }
}
