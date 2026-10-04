package com.example.plag_out

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionRepository
import com.example.plag_out.fakes.FakeFeedbackPrediccionDao
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    @get:Rule(order = 0) val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())
    @get:Rule(order = 1) val compose = createComposeRule()

    private fun viewModel(service: FakeGDDService) = PrediccionDetalleViewModel(
        FeedbackPrediccionRepository(FakeFeedbackPrediccionDao()), service,
        ownerIdProvider = { "11111111-1111-1111-1111-111111111111" }
    )

    @Test fun `carga fallida ofrece volver a cargar`() {
        val service = FakeGDDService().apply { getPrediccionResult = { FakeGDDService.errorServidor(503) } }
        val vm = viewModel(service)
        compose.setContent { PrediccionDetalleScreen(41, vm, {}) }
        compose.waitUntil(timeoutMillis = 5000) { !vm.state.value.isLoading && vm.state.value.error != null }
        compose.onNodeWithText("Volver a cargar").assertIsDisplayed()
    }

    @Test fun `error de envio conserva respuesta y retry muestra guardado confirmado`() {
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = { FakeGDDService.sinConexion() }
        }
        val vm = viewModel(service)
        compose.setContent { PrediccionDetalleScreen(41, vm, {}) }
        compose.waitUntil(timeoutMillis = 5000) { vm.state.value.prediccion != null }
        compose.onNodeWithText("No pude verificar").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5000) { !vm.state.value.enviando && vm.state.value.error != null }
        compose.onNodeWithText("Respuesta pendiente").assertIsDisplayed()
        compose.onNodeWithText("Reintentar envío").assertIsDisplayed()
        val key = vm.state.value.feedbackPendiente!!.idempotency_key
        service.confirmarPrediccionResult = { Response.success(PrediccionConfirmacionResponse(
            id = "confirmacion", prediccion_id = 41, respuesta = "no_verificada", respondido_en = "2026-09-01T10:00:00Z"
        )) }
        compose.onNodeWithText("Reintentar envío").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5000) { vm.state.value.prediccion?.confirmacion?.estado == "respondida" }
        compose.onNodeWithText("Respuesta guardada").assertIsDisplayed()
        org.junit.Assert.assertEquals(key, service.ultimaConfirmacionPrediccion?.idempotency_key)
    }
}
