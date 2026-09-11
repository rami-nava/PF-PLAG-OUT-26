package com.example.plag_out

import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionPendiente
import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionRepository
import com.example.plag_out.fakes.FakeFeedbackPrediccionDao
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class PrediccionDetalleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val owner = "11111111-1111-1111-1111-111111111111"

    private fun viewModel(
        service: FakeGDDService,
        dao: FakeFeedbackPrediccionDao = FakeFeedbackPrediccionDao(),
        ownerId: String = owner
    ) = PrediccionDetalleViewModel(
        FeedbackPrediccionRepository(dao),
        service,
        ownerIdProvider = { ownerId }
    )

    @Test
    fun `envia las tres respuestas contractuales`() {
        PrediccionDetalleViewModel.RESPUESTAS.forEach { respuesta ->
            val service = FakeGDDService().apply {
                getPrediccionResult = { Response.success(Fixtures.prediccion()) }
                confirmarPrediccionResult = {
                    Response.success(
                        PrediccionConfirmacionResponse(
                            id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                            prediccion_id = 41,
                            respuesta = respuesta,
                            respondido_en = "2026-09-01T10:00:00Z"
                        )
                    )
                }
            }
            val vm = viewModel(service)
            vm.cargar(41)
            esperarEstado(vm.state) { it.prediccion != null }

            vm.responder(respuesta)

            val estado = esperarEstado(vm.state) { it.prediccion?.confirmacion?.estado == "respondida" }
            assertEquals(respuesta, estado.prediccion?.confirmacion?.respuesta)
            assertEquals(respuesta, service.ultimaConfirmacionPrediccion?.respuesta)
        }
    }

    @Test
    fun `retry reutiliza el UUID persistido`() {
        var falla = true
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = {
                if (falla) {
                    falla = false
                    FakeGDDService.sinConexion()
                }
                Response.success(
                    PrediccionConfirmacionResponse(
                        id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                        prediccion_id = 41,
                        respuesta = "no_observada",
                        respondido_en = "2026-09-01T10:00:00Z"
                    )
                )
            }
        }
        val vm = viewModel(service)
        vm.cargar(41)
        esperarEstado(vm.state) { it.prediccion != null }

        vm.responder("no_observada")
        val pendiente = esperarEstado(vm.state) { it.feedbackPendiente != null && it.error != null }
            .feedbackPendiente!!
        val primerUuid = pendiente.idempotency_key

        vm.reintentar()
        esperarEstado(vm.state) { it.prediccion?.confirmacion?.estado == "respondida" }

        assertEquals(primerUuid, service.ultimaConfirmacionPrediccion?.idempotency_key)
        assertEquals(2, service.vecesLlamado("confirmarPrediccion"))
    }

    @Test
    fun `feedback de otro usuario no aparece ni se elimina`() {
        val ajeno = FeedbackPrediccionPendiente(
            owner_id = "22222222-2222-2222-2222-222222222222",
            prediccion_id = 41,
            respuesta = "presente",
            idempotency_key = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
        )
        val dao = FakeFeedbackPrediccionDao(listOf(ajeno))
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
        }
        val vm = viewModel(service, dao)

        vm.cargar(41)

        val estado = esperarEstado(vm.state) { it.prediccion != null }
        assertNull(estado.feedbackPendiente)
        assertEquals(ajeno, runBlocking { dao.get(ajeno.owner_id, 41) })
    }

    @Test
    fun `410 marca vencida y 404 oculta la prediccion`() {
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = { FakeGDDService.errorServidor(410) }
        }
        val vm = viewModel(service)
        vm.cargar(41)
        esperarEstado(vm.state) { it.prediccion != null }
        vm.responder("no_verificada")
        assertEquals("vencida", esperarEstado(vm.state) {
            it.prediccion?.confirmacion?.estado == "vencida"
        }.prediccion?.confirmacion?.estado)

        service.getPrediccionResult = { FakeGDDService.errorServidor(404) }
        vm.cargar(99)
        assertTrue(esperarEstado(vm.state) { it.noDisponible }.noDisponible)
    }

    @Test
    fun `409 recarga el estado real y 422 descarta el intento invalido`() {
        val service409 = FakeGDDService().apply {
            getPrediccionResult = {
                if (vecesLlamado("getPrediccion") == 1) Response.success(Fixtures.prediccion())
                else Response.success(Fixtures.prediccion(estado = "respondida", respuesta = "presente"))
            }
            confirmarPrediccionResult = { FakeGDDService.errorServidor(409) }
        }
        val vm409 = viewModel(service409)
        vm409.cargar(41)
        esperarEstado(vm409.state) { it.prediccion != null }
        vm409.responder("presente")
        assertEquals("respondida", esperarEstado(vm409.state) {
            it.prediccion?.confirmacion?.estado == "respondida"
        }.prediccion?.confirmacion?.estado)

        val dao422 = FakeFeedbackPrediccionDao()
        val service422 = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = { FakeGDDService.errorServidor(422) }
        }
        val vm422 = viewModel(service422, dao422)
        vm422.cargar(41)
        esperarEstado(vm422.state) { it.prediccion != null }
        vm422.responder("no_verificada")
        esperarEstado(vm422.state) { it.error?.contains("respuesta válida") == true }
        assertNull(runBlocking { dao422.get(owner, 41) })
    }
}
