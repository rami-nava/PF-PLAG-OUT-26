package com.example.plag_out

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `pestanas sin numero, encabezado con el resumen y filtros que consultan al servidor`() {
        val service = FakeGDDService().apply {
            getUsuarioActualResult = { Response.success(Fixtures.usuario()) }
            getTerrenosResult = { Response.success(listOf(Fixtures.terreno(id = 1, nombre = "Lote Norte"))) }
            getPlagasResult = { Response.success(listOf(Fixtures.plaga(id = 7, nombre = "Plaga A"))) }
            reportesServidor = (1..25).map {
                Fixtures.reporteDetalle(id = it, nivelSeveridad = if (it <= 5) "Bajo" else "Alto")
            } + Fixtures.reporteDetalle(id = 100, esPropio = false, plagaId = 7, plagaNombre = "Plaga A")
        }
        viewModel = MisReportesViewModel(service, reportesDispatcher = Dispatchers.Main.immediate)
        compose.setContent { MaterialTheme { MisReportesScreen(viewModel, rememberNavController()) } }

        // Las pastillas no llevan cantidad: el total está en el encabezado, que viene del resumen.
        compose.onNodeWithTag("tabReportesPropios").assertTextEquals("Míos")
        compose.onNodeWithTag("tabReportesComunidad").assertTextEquals("Comunidad")
        compose.onNodeWithText("Mostrando", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Filtros activos").assertDoesNotExist()
        compose.onNodeWithText("25").assertExists()

        // Un filtro rehace la consulta y el total pasa a ser el filtrado.
        compose.onNodeWithTag("btnToggleFiltros").performClick()
        compose.onNode(hasText("Bajo") and hasAnyAncestor(hasTestTag("panelFiltros"))).performClick()
        compose.onNodeWithText("Filtros activos").assertExists()
        compose.runOnIdle {
            assertEquals("Bajo", service.consultasReportes.last().severidad)
            assertEquals("Bajo", service.consultasResumen.last().severidad)
        }
        // El encabezado también respeta el filtro: 5 en el centro y 5 en "Bajo".
        compose.onNodeWithText("25").assertDoesNotExist()
        compose.onAllNodesWithText("5").assertCountEquals(2)

        compose.onNode(hasText("Lote Norte") and hasAnyAncestor(hasTestTag("panelFiltros"))).performClick()
        compose.runOnIdle { assertEquals(1, service.consultasReportes.last().terrenoId) }

        // La comunidad tiene su propia consulta y sus propios filtros.
        cambiarPestana("tabReportesComunidad")
        compose.onNodeWithText("Filtros activos").assertDoesNotExist()
        compose.runOnIdle {
            val ultima = service.consultasReportes.last()
            assertEquals("comunidad", ultima.ambito)
            assertNull(ultima.severidad)
        }
        compose.onNode(hasText("Plaga A") and hasAnyAncestor(hasTestTag("panelFiltros"))).performClick()
        compose.runOnIdle { assertEquals(7, service.consultasReportes.last().plagaId) }
        compose.onNodeWithText("Filtros activos").assertExists()

        cambiarPestana("tabReportesPropios")
        compose.onNodeWithText("Filtros activos").assertExists()
        compose.onNodeWithTag("btnLimpiarFiltros").performClick()
        compose.onNodeWithText("Filtros activos").assertDoesNotExist()
        compose.onNodeWithText("25").assertExists()
    }
}
