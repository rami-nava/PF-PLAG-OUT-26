package com.example.plag_out

import android.app.Application
import com.example.plag_out.util.esperarQue
import com.example.plag_out.util.MainDispatcherRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionRepository
import com.example.plag_out.fakes.FakeFeedbackPrediccionDao
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class PrediccionWorkflowScreenTest {
    @get:Rule(order = 0) val mainDispatcherRule = MainDispatcherRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    /** compose.waitUntil no avanza el dispatcher de Main; esperarQue sí. */
    private fun esperar(condicion: () -> Boolean) {
        esperarQue(condicion = condicion)
        compose.waitForIdle()
    }

    private fun viewModel(service: FakeGDDService) = PrediccionDetalleViewModel(
        FeedbackPrediccionRepository(FakeFeedbackPrediccionDao()), service,
        ownerIdProvider = { "11111111-1111-1111-1111-111111111111" }
    )

    @Test fun `carga fallida ofrece volver a cargar`() {
        val service = FakeGDDService().apply { getPrediccionResult = { FakeGDDService.errorServidor(503) } }
        val vm = viewModel(service)
        compose.setContent { PrediccionDetalleScreen(41, vm, {}) }
        esperar { !vm.state.value.isLoading && vm.state.value.error != null }
        compose.onNodeWithText("Volver a cargar").assertIsDisplayed()
    }

    @Test fun `push ML antiguo muestra no disponible y permite volver sin confirmar`() {
        val service = FakeGDDService().apply { getPrediccionResult = { FakeGDDService.errorServidor(404) } }
        val vm = viewModel(service)
        val destino = destinoDePush(tipo = "ALERTA_ML_RIESGO", prediccionId = "41")
        org.junit.Assert.assertEquals("prediccion/41", destino)
        var volvio = false
        compose.setContent { PrediccionDetalleScreen(destino!!.substringAfter('/').toInt(), vm, { volvio = true }) }
        esperar { vm.state.value.noDisponible }
        compose.onNodeWithText("Esta predicción no está disponible.").assertIsDisplayed()
        compose.onNodeWithText("Volver a cargar").assertDoesNotExist()
        compose.onNodeWithText("No pude verificar").assertDoesNotExist()
        compose.onNodeWithText("Reintentar envío").assertDoesNotExist()
        compose.onNodeWithContentDescription("Volver").performClick()
        org.junit.Assert.assertTrue(volvio)
        org.junit.Assert.assertNull(vm.state.value.error)
        org.junit.Assert.assertEquals(0, service.vecesLlamado("confirmarPrediccion"))
    }

    @Test fun `error de envio conserva respuesta y retry muestra guardado confirmado`() {
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = { FakeGDDService.sinConexion() }
        }
        val vm = viewModel(service)
        compose.setContent { PrediccionDetalleScreen(41, vm, {}) }
        esperar { vm.state.value.prediccion != null }
        compose.onNodeWithText("No pude verificar").performScrollTo().performClick()
        esperar { !vm.state.value.enviando && vm.state.value.error != null }
        compose.onNodeWithText("Respuesta pendiente").assertIsDisplayed()
        compose.onNodeWithText("Reintentar envío").assertIsDisplayed()
        val key = vm.state.value.feedbackPendiente!!.idempotency_key
        service.confirmarPrediccionResult = { Response.success(PrediccionConfirmacionResponse(
            id = "confirmacion", prediccion_id = 41, respuesta = "no_verificada", respondido_en = "2026-09-01T10:00:00Z"
        )) }
        compose.onNodeWithText("Reintentar envío").performScrollTo().performClick()
        esperar { vm.state.value.prediccion?.confirmacion?.estado == "respondida" }
        compose.onNodeWithText("Respuesta guardada").assertIsDisplayed()
        org.junit.Assert.assertEquals(key, service.ultimaConfirmacionPrediccion?.idempotency_key)
    }

    @Test fun `respuesta guardada se muestra como exito y no como advertencia`() {
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion(estado = "respondida", respuesta = "presente")) }
        }
        val vm = viewModel(service)
        compose.setContent { PrediccionDetalleScreen(41, vm, {}) }
        esperar { vm.state.value.prediccion != null }
        compose.onNodeWithText("Respuesta guardada").assertIsDisplayed()
        compose.onNodeWithTag("estadoPrediccion_Guardado", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("estadoPrediccion_Aviso", useUnmergedTree = true).assertDoesNotExist()
    }
}
