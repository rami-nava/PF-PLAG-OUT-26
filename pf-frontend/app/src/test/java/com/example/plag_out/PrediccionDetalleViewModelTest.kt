package com.example.plag_out

import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionDao
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
        dao: FeedbackPrediccionDao = FakeFeedbackPrediccionDao(),
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
                            biofix = if (respuesta == "presente") BiofixResult("obs", 1, true, 1, true, Fixtures.ciclo()) else null,
                            respondido_en = "2026-09-01T10:00:00Z"
                        )
                    )
                }
            }
            val vm = viewModel(service)
            vm.cargar(41)
            esperarEstado(vm.state) { it.prediccion != null }

            vm.responder(respuesta, if (respuesta == "presente") BiofixRequest(java.util.UUID.randomUUID().toString(), "2026-09-01", "iniciar_ciclo") else null)

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
    fun `respuesta tardia de otra sesion conserva el feedback pendiente`() {
        val currentOwner = java.util.concurrent.atomic.AtomicReference(owner)
        val dao = FakeFeedbackPrediccionDao()
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = {
                currentOwner.set("otra-cuenta")
                Response.error(404, okhttp3.ResponseBody.create(null, ""))
            }
        }
        val vm = PrediccionDetalleViewModel(FeedbackPrediccionRepository(dao), service,
            ownerIdProvider = { currentOwner.get() })
        vm.cargar(41)
        esperarEstado(vm.state) { it.prediccion != null }
        vm.responder("no_observada")
        esperarEstado(vm.state) { it.prediccion == null && !it.enviando }
        assertEquals("no_observada", runBlocking { dao.get(owner, 41) }?.respuesta)
        assertNull(vm.state.value.biofixResultado)
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
    fun `borrado mientras el detalle esta abierto descarta datos y feedback sin reintentar`() {
        val dao = FakeFeedbackPrediccionDao()
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = { FakeGDDService.errorServidor(404) }
        }
        val vm = viewModel(service, dao)
        vm.cargar(41)
        esperarEstado(vm.state) { it.prediccion != null }
        vm.responder("no_verificada")
        val estado = esperarEstado(vm.state) { it.noDisponible }
        assertNull(estado.prediccion)
        assertNull(estado.feedbackPendiente)
        assertNull(estado.biofixResultado)
        assertNull(estado.error)
        assertTrue(!estado.enviando && !estado.isLoading)
        assertNull(runBlocking { dao.get(owner, 41) })
        vm.reintentar()
        vm.responder("no_observada")
        assertEquals(1, service.vecesLlamado("confirmarPrediccion"))
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
        vm409.responder("presente", BiofixRequest(java.util.UUID.randomUUID().toString(), "2026-09-01", "iniciar_ciclo"))
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
    @Test
    fun `presencia sin biofix no se envia`() {
        val service = FakeGDDService().apply { getPrediccionResult = { Response.success(Fixtures.prediccion()) } }
        val vm = viewModel(service)
        vm.cargar(41)
        esperarEstado(vm.state) { it.prediccion != null }
        vm.responder("presente")
        assertEquals(0, service.vecesLlamado("confirmarPrediccion"))
    }

    @Test
    fun `retry de biofix conserva fecha accion ciclo y UUID tras recrear viewmodel`() {
        val dao = FakeFeedbackPrediccionDao()
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = { FakeGDDService.sinConexion() }
        }
        val vm = viewModel(service, dao)
        vm.cargar(41)
        esperarEstado(vm.state) { it.prediccion != null }
        val payload = BiofixRequest(java.util.UUID.randomUUID().toString(), "2026-09-01", "asociar_ciclo", 73)
        vm.responder("presente", payload)
        esperarEstado(vm.state) { it.error != null && !it.enviando }
        val reopened = viewModel(service, dao)
        reopened.cargar(41)
        esperarEstado(reopened.state) { it.feedbackPendiente != null }
        reopened.reintentar()
        esperarEstado(reopened.state) { it.error != null && !it.enviando }
        assertEquals(payload, service.ultimaConfirmacionPrediccion?.biofix)
        assertEquals(payload.idempotency_key, service.ultimaConfirmacionPrediccion?.idempotency_key)
        assertEquals(2, service.vecesLlamado("confirmarPrediccion"))
    }

    @Test
    fun `push de ciclo abre el monitoreo de monitoreo_id, no el ciclo`() {
        for (tipo in listOf("ALERTA_GDD_CICLO", "BIOFIX_CICLO")) {
            assertEquals("monitoreo/41", destinoDePush(tipo, monitoreoId = "41", cicloId = "73"))
            // Sin monitoreo no hay a dónde ir: el ciclo_id no alcanza para abrir el monitoreo.
            assertNull(destinoDePush(tipo, cicloId = "73"))
            assertNull(destinoDePush(tipo, monitoreoId = "abc"))
        }
    }

    @Test
    fun `aviso de ciclo en la campana abre el monitoreo de entidad_id`() {
        for (tipo in listOf("ALERTA_GDD_CICLO", "BIOFIX_CICLO")) {
            assertEquals("monitoreo/41", destinoDe(Fixtures.notificacion(tipo = tipo, entidadId = 41)))
            assertNull(destinoDe(Fixtures.notificacion(tipo = tipo, entidadId = null)))
        }
    }

    @Test
    fun `tipos viejos siguen abriendo su entidad`() {
        assertEquals("monitoreo/41", destinoDe(Fixtures.notificacion(tipo = "ALERTA_GDD", entidadId = 41)))
        assertEquals("plantacion/7", destinoDe(Fixtures.notificacion(tipo = "BIOFIX", entidadId = 7)))
        assertEquals("ver_reporte/9", destinoDe(Fixtures.notificacion(tipo = "REPORTE_CERCANO", entidadId = 9)))
    }

    @Test
    fun `acuse vacio o de otra prediccion conserva borrador y UUID`() {
        listOf<PrediccionConfirmacionResponse?>(
            null,
            PrediccionConfirmacionResponse(id="confirmacion", prediccion_id=99, respuesta="no_observada", respondido_en="2026-09-01T10:00:00Z"),
            PrediccionConfirmacionResponse(id="confirmacion", prediccion_id=41, respuesta="no_verificada", respondido_en="2026-09-01T10:00:00Z")
        ).forEach { ack ->
            val dao = FakeFeedbackPrediccionDao()
            val service = FakeGDDService().apply {
                getPrediccionResult = { Response.success(Fixtures.prediccion()) }
                confirmarPrediccionResult = { Response.success(ack) }
            }
            val vm = viewModel(service, dao)
            vm.cargar(41)
            esperarEstado(vm.state) { it.prediccion != null }
            vm.responder("no_observada")
            val pending = esperarEstado(vm.state) { !it.enviando && it.error != null }.feedbackPendiente!!
            assertEquals("pendiente", vm.state.value.prediccion?.confirmacion?.estado)
            assertEquals(pending, runBlocking { dao.get(owner, 41) })
            service.confirmarPrediccionResult = { Response.success(
                PrediccionConfirmacionResponse(id = "confirmacion", prediccion_id = 41,
                    respuesta = "no_observada", respondido_en = "2026-09-01T10:00:00Z")
            ) }
            vm.reintentar()
            esperarEstado(vm.state) { it.prediccion?.confirmacion?.estado == "respondida" }
            assertEquals(pending.idempotency_key, service.ultimaConfirmacionPrediccion?.idempotency_key)
            assertNull(runBlocking { dao.get(owner, 41) })
        }
    }

    @Test
    fun `fallo de almacenamiento local conserva seleccion y retry guarda antes de enviar`() {
        val underlying = FakeFeedbackPrediccionDao()
        var fail = true
        val dao = object : FeedbackPrediccionDao by underlying {
            override suspend fun insert(feedback: FeedbackPrediccionPendiente) {
                if (fail) { fail = false; throw IllegalStateException("storage unavailable") }
                underlying.insert(feedback)
            }
        }
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
            confirmarPrediccionResult = {
                assertEquals(ultimaConfirmacionPrediccion?.idempotency_key, runBlocking { underlying.get(owner, 41) }?.idempotency_key)
                Response.success(PrediccionConfirmacionResponse(id = "confirmacion", prediccion_id = 41,
                    respuesta = "no_verificada", respondido_en = "2026-09-01T10:00:00Z"))
            }
        }
        val vm = viewModel(service, dao)
        vm.cargar(41)
        esperarEstado(vm.state) { it.prediccion != null }
        vm.responder("no_verificada")
        val pending = esperarEstado(vm.state) { !it.enviando && it.error != null }.feedbackPendiente!!
        assertEquals(0, service.vecesLlamado("confirmarPrediccion"))
        assertNull(runBlocking { underlying.get(owner, 41) })
        vm.reintentar()
        esperarEstado(vm.state) { it.prediccion?.confirmacion?.estado == "respondida" }
        assertEquals(pending.idempotency_key, service.ultimaConfirmacionPrediccion?.idempotency_key)
        assertNull(runBlocking { underlying.get(owner, 41) })
    }

}
