package com.example.plag_out

import android.app.Application
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

/** Tocar el push equivale a tocar el aviso en la campana. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class NotificacionLeidaDesdePushTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val alerta = Fixtures.notificacion(id = 140, tipo = "ALERTA_ML_RIESGO", entidadId = 53)
    private val otra = Fixtures.notificacion(id = 141, tipo = "ALERTA_GDD_CICLO", entidadId = 225)

    @Test fun `push de un aviso ya cargado lo saca de la campana y lo marca en el backend`() {
        val service = FakeGDDService().apply {
            getNotificacionesResult = { Response.success(listOf(alerta, otra)) }
            marcarNotificacionLeidaResult = { Response.success(MarcarLeidaResponse(alerta.id, true)) }
        }
        val viewModel = NotificacionesViewModel(service)
        viewModel.cargar()

        esperarHasta { viewModel.state.value.notificaciones.size == 2 }

        viewModel.marcarLeidaPorId(alerta.id)

        esperarHasta { service.ultimaNotificacionLeida == alerta.id }
        assertEquals(listOf(otra.id), viewModel.state.value.notificaciones.map { it.id })
    }

    @Test fun `push con la app cerrada marca el aviso aunque la campana no lo haya cargado`() {
        val service = FakeGDDService().apply {
            getNotificacionesResult = { Response.success(listOf(otra)) }
            marcarNotificacionLeidaResult = { Response.success(MarcarLeidaResponse(alerta.id, true)) }
        }
        val viewModel = NotificacionesViewModel(service)

        viewModel.marcarLeidaPorId(alerta.id)
        viewModel.cargar()

        esperarHasta(service, viewModel) { service.ultimaNotificacionLeida == alerta.id && !viewModel.state.value.isLoading }
        assertEquals(1, service.vecesLlamado("marcarNotificacionLeida"))
        assertEquals(alerta.id, service.ultimaNotificacionLeida)
        assertEquals(listOf(otra.id), viewModel.state.value.notificaciones.map { it.id })
    }

    /** El view model hace la llamada en Dispatchers.IO: esperar el resultado, con límite generoso para suites completas. */
    private fun esperarHasta(
        service: FakeGDDService? = null,
        viewModel: NotificacionesViewModel? = null,
        condicion: () -> Boolean
    ) {
        val limite = System.currentTimeMillis() + 30_000
        while (!condicion()) {
            check(System.currentTimeMillis() < limite) {
                "La condición no se cumplió en 30 s. service.ultimaNotificacionLeida=${service?.ultimaNotificacionLeida}, state=${viewModel?.state?.value}, llamadas=${service?.llamadas}"
            }
            try {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
            } catch (_: Throwable) {}
            Thread.sleep(10)
        }
    }
}
