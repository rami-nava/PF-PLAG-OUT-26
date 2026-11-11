package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.util.esperarQue
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.util.Collections
import java.util.concurrent.atomic.AtomicLong

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapaReportesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: MapaReportesViewModel

    /** Reloj falso: esperar no duerme, solo anota cuánto y adelanta el reloj. */
    private val reloj = AtomicLong(0)
    private val esperas: MutableList<Long> = Collections.synchronizedList(mutableListOf())

    /** Vista de todo el país, bien alejada. */
    private val vistaPais = VistaMapa(sur = -55.0, oeste = -75.0, norte = -21.0, este = -53.0, zoom = 4.2)

    /** Vista de unos pocos km alrededor de Buenos Aires, a zoom de reportes sueltos. */
    private val vistaCerca = VistaMapa(sur = -34.7, oeste = -58.5, norte = -34.5, este = -58.3, zoom = 12.0)

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = MapaReportesViewModel(
            gddService,
            esperar = { esperas += it; reloj.addAndGet(it) },
            ahora = { reloj.get() }
        )
    }

    @After
    fun tearDown() {
        viewModel.limpiar()
        mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `sin vista todavia no pide nada`() {
        assertEquals(0, gddService.llamadas.size)
        assertTrue(viewModel.state.value.cargando)
    }

    @Test
    fun `al cambiar de cuenta la misma vista se pide sin usar los pines propios previos`() {
        val propioA = Fixtures.reporteDetalle(id = 42, latitud = -34.60123, longitud = -58.40123)
        val comunitarioB = propioA.copy(es_propio = false, latitud = -34.65, longitud = -58.45)
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(reportes = listOf(propioA)))
        }
        viewModel.actualizarVista(vistaCerca)
        assertEquals(propioA, esperarEstado(viewModel.state) { !it.cargando }.reportes.single())

        viewModel.limpiar()
        assertTrue(viewModel.state.value.reportes.isEmpty())
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(reportes = listOf(comunitarioB)))
        }
        viewModel.actualizarVista(vistaCerca)

        val mapaB = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals(comunitarioB, mapaB.reportes.single())
        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `al cerrar sesion se cancela la solicitud en vuelo del mapa`() {
        val iniciada = CompletableDeferred<Unit>()
        val cancelada = CompletableDeferred<Unit>()
        val respuesta = CompletableDeferred<Response<ReportesMapaResponse>>()
        gddService.getReportesMapaResult = {
            iniciada.complete(Unit)
            try {
                respuesta.await()
            } finally {
                cancelada.complete(Unit)
            }
        }

        try {
            viewModel.actualizarVista(vistaCerca)
            esperarQue { iniciada.isCompleted }
            viewModel.limpiar()
            esperarQue { cancelada.isCompleted }
            assertTrue(viewModel.state.value.reportes.isEmpty())
        } finally {
            respuesta.cancel()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun `alejado pide grupos con la celda del zoom y un margen alrededor de lo visible`() {
        gddService.getReportesMapaResult = {
            Response.success(
                ReportesMapaResponse(
                    nivel = "grupos",
                    grupos = listOf(
                        GrupoReportesMapa(-34.6, -58.4, 37, "Alto"),
                        GrupoReportesMapa(-31.4, -64.2, 5, "Bajo")
                    )
                )
            )
        }

        viewModel.actualizarVista(vistaPais)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        val consulta = gddService.consultasMapa.single()
        assertEquals("grupos", consulta["nivel"])
        assertEquals(4, consulta["zoom"])
        assertEquals("90 / 2^4", 5.625, consulta["celda_grados"] as Double, 1e-12)
        assertNull(consulta["limite"])
        // Pide más de lo que se ve, para que un desplazamiento chico no dispare otro pedido.
        assertTrue((consulta["sur"] as Double) < vistaPais.sur)
        assertTrue((consulta["este"] as Double) > vistaPais.este)

        assertEquals(NivelMapa.GRUPOS, estado.nivel)
        assertEquals(2, estado.grupos.size)
        assertTrue(estado.reportes.isEmpty())
        assertEquals("sin total explícito se suman los grupos", 42, estado.total)
    }

    @Test
    fun `de cerca pide reportes sueltos con limite y la celda por si hay que agrupar`() {
        val reportes = listOf(
            Fixtures.reporteDetalle(id = 1, cultivoNombre = "Maíz"),
            Fixtures.reporteDetalle(id = 2, cultivoNombre = "Soja", latitud = null, longitud = null)
        )
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = reportes))
        }

        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        val consulta = gddService.consultasMapa.single()
        assertEquals("reportes", consulta["nivel"])
        assertEquals("90 / 2^12", 0.02197265625, consulta["celda_grados"] as Double, 1e-15)
        assertEquals(LIMITE_REPORTES_MAPA, consulta["limite"])

        assertEquals(NivelMapa.REPORTES, estado.nivel)
        assertEquals("el que no tiene coordenadas no se puede dibujar", listOf(1), estado.reportes.map { it.id })
        assertEquals(1, estado.total)
        assertEquals(listOf("Maíz"), estado.cultivos)
    }

    @Test
    fun `moverse dentro de la zona ya cargada no vuelve a pedir`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = emptyList())) }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        val corrida = vistaCerca.copy(sur = -34.68, norte = -34.48, zoom = 12.6)
        viewModel.actualizarVista(corrida)

        assertEquals(1, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `salir de la zona cargada o cruzar el umbral de zoom vuelve a pedir`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = emptyList())) }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        viewModel.actualizarVista(vistaCerca.copy(oeste = -60.5, este = -60.3))
        esperarEstado(viewModel.state) { !it.actualizando }
        viewModel.actualizarVista(vistaCerca.copy(zoom = 9.0))
        esperarEstado(viewModel.state) { !it.actualizando }

        assertEquals(3, gddService.vecesLlamado("getReportesMapa"))
        assertEquals("grupos", gddService.consultasMapa.last()["nivel"])
    }

    @Test
    fun `los grupos se vuelven a pedir al cambiar de zoom entero aunque la zona ya este cargada`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(grupos = emptyList())) }
        viewModel.actualizarVista(vistaPais)
        esperarEstado(viewModel.state) { !it.cargando }

        viewModel.actualizarVista(vistaPais.copy(zoom = 4.8))
        assertEquals("mismo zoom entero, misma celda", 1, gddService.vecesLlamado("getReportesMapa"))

        viewModel.actualizarVista(vistaPais.copy(sur = -40.0, norte = -30.0, zoom = 5.1))
        esperarEstado(viewModel.state) { !it.actualizando }
        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
        assertEquals(5, gddService.consultasMapa.last()["zoom"])
    }

    @Test
    fun `los filtros viajan como parametros y cambiarlos vuelve a pedir`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = emptyList())) }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        val consultaSinFiltros = gddService.consultasMapa.single()
        listOf("ambito", "radio_km", "dias", "severidad", "plaga", "cultivo").forEach {
            assertNull("sin filtro, '$it' no viaja", consultaSinFiltros[it])
        }

        viewModel.actualizarFiltros(
            FiltrosMapa(
                ambito = AmbitoMapa.COMUNIDAD,
                radioKm = 50,
                dias = 30,
                severidad = "Alto",
                plaga = "Chicharrita",
                cultivo = "Maíz"
            )
        )
        esperarEstado(viewModel.state) { !it.actualizando }

        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
        val consulta = gddService.consultasMapa.last()
        assertEquals("comunidad", consulta["ambito"])
        assertEquals(50, consulta["radio_km"])
        assertEquals(30, consulta["dias"])
        assertEquals("Alto", consulta["severidad"])
        assertEquals("Chicharrita", consulta["plaga"])
        assertEquals("Maíz", consulta["cultivo"])
    }

    @Test
    fun `con demasiados reportes el servidor responde grupos aunque se pidan reportes`() {
        gddService.getReportesMapaResult = {
            Response.success(
                ReportesMapaResponse(nivel = "grupos", grupos = listOf(GrupoReportesMapa(-34.6, -58.4, 1200, "Medio")))
            )
        }

        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals("reportes", gddService.consultasMapa.single()["nivel"])
        assertEquals(NivelMapa.GRUPOS, estado.nivel)
        assertEquals(1200, estado.total)
        assertTrue(estado.reportes.isEmpty())
    }

    @Test
    fun `si llegaron grupos por exceso, acercarse dentro de la zona vuelve a pedir`() {
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "grupos", grupos = listOf(GrupoReportesMapa(-34.6, -58.4, 900))))
        }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        // Mismo zoom entero y dentro de la zona: con grupos no cambia nada, no se pide.
        viewModel.actualizarVista(vistaCerca.copy(sur = -34.68, norte = -34.52, zoom = 12.4))
        assertEquals(1, gddService.vecesLlamado("getReportesMapa"))

        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle())))
        }
        viewModel.actualizarVista(vistaCerca.copy(sur = -34.65, norte = -34.55, oeste = -58.45, este = -58.35, zoom = 13.0))

        val estado = esperarEstado(viewModel.state) { it.nivel == NivelMapa.REPORTES && !it.actualizando }
        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
        assertEquals(1, estado.reportes.size)
    }

    @Test
    fun `con reportes sueltos acercarse dentro de la zona no vuelve a pedir`() {
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle())))
        }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        viewModel.actualizarVista(vistaCerca.copy(sur = -34.65, norte = -34.55, oeste = -58.45, este = -58.35, zoom = 13.0))

        assertEquals("ya se tienen todos los de la zona", 1, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `si el backend no tiene la ruta avisa`() {
        gddService.getReportesMapaResult = { FakeGDDService.errorServidor(404) }

        viewModel.actualizarVista(vistaPais)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals("El mapa de alertas todavía no está disponible en el servidor.", estado.error)
        assertEquals("un error del cliente no se arregla esperando", 1, gddService.vecesLlamado("getReportesMapa"))
        assertTrue(esperas.isEmpty())
    }

    @Test
    fun `un error no deja en pantalla las alertas de los filtros anteriores y refrescar vuelve a pedir`() {
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle(id = 7)), total = 1))
        }
        viewModel.actualizarVista(vistaCerca)
        assertEquals(1, esperarEstado(viewModel.state) { !it.cargando }.reportes.size)

        gddService.getReportesMapaResult = { FakeGDDService.errorServidor(503) }
        viewModel.actualizarFiltros(FiltrosMapa(severidad = "Alto"))
        val conError = esperarEstado(viewModel.state) { it.error != null }
        assertTrue("los pines eran de otro filtro", conError.reportes.isEmpty() && conError.grupos.isEmpty())
        assertEquals(0, conError.total)

        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle(id = 8)), total = 1))
        }
        val esperasPrevias = esperas.toList()
        viewModel.refrescar()
        val recuperado = esperarEstado(viewModel.state) { it.error == null && it.reportes.isNotEmpty() }
        assertEquals(listOf(8), recuperado.reportes.map { it.id })
        // 1 que anduvo, 1 + REINTENTOS_MAPA con 503 y el de "Reintentar".
        assertEquals(2 + 1 + REINTENTOS_MAPA, gddService.vecesLlamado("getReportesMapa"))
        assertEquals("Reintentar no espera si el servidor no lo pidió", esperasPrevias, esperas.toList())
    }

    @Test
    fun `una falla pasajera se reintenta con espera creciente y si se recupera no queda error`() {
        val respuestas = ArrayDeque(listOf(503, 502))
        gddService.getReportesMapaResult = {
            val codigo = synchronized(respuestas) { respuestas.removeFirstOrNull() }
            if (codigo != null) FakeGDDService.errorServidor(codigo)
            else Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle(id = 5)), total = 1))
        }

        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertNull(estado.error)
        assertEquals(listOf(5), estado.reportes.map { it.id })
        assertEquals(3, gddService.vecesLlamado("getReportesMapa"))
        assertEquals(listOf(1_000L, 2_000L), esperas.toList())
    }

    @Test
    fun `un 429 respeta el Retry-After del servidor incluso al reintentar a mano`() {
        gddService.getReportesMapaResult = { conRetryAfter(429, "60") }
        viewModel.actualizarVista(vistaCerca)
        assertEquals("No se pudieron obtener las alertas del servidor.", esperarEstado(viewModel.state) { !it.cargando }.error)
        assertEquals(List(REINTENTOS_MAPA) { 60_000L }, esperas.toList())

        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(nivel = "reportes", reportes = emptyList(), total = 0)) }
        viewModel.refrescar()

        esperarEstado(viewModel.state) { it.error == null && !it.actualizando }
        assertEquals("Reintentar espera lo que pidió el servidor", 60_000L, esperas.last())
        assertEquals(REINTENTOS_MAPA + 1, esperas.size)
    }

    @Test
    fun `moverse durante una caida no reinicia la espera`() {
        gddService.getReportesMapaResult = { FakeGDDService.errorServidor(503) }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { it.error != null }
        assertEquals(listOf(1_000L, 2_000L, 4_000L), esperas.toList())

        viewModel.actualizarVista(vistaPais)
        esperarEstado(viewModel.state) { it.error != null && gddService.vecesLlamado("getReportesMapa") == 8 }

        // El pedido de la vista nueva sale después de 8 s y sigue duplicando hasta el tope de 30 s.
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L), esperas.toList())
    }

    /** Cada llamada al mapa queda esperando hasta que el test la responde. */
    private fun respuestasManuales(): java.util.concurrent.LinkedBlockingQueue<CompletableDeferred<Response<ReportesMapaResponse>>> {
        val pendientes = java.util.concurrent.LinkedBlockingQueue<CompletableDeferred<Response<ReportesMapaResponse>>>()
        gddService.getReportesMapaResult = {
            val respuesta = CompletableDeferred<Response<ReportesMapaResponse>>()
            pendientes.put(respuesta)
            respuesta.await()
        }
        return pendientes
    }

    private fun <T> java.util.concurrent.LinkedBlockingQueue<T>.siguiente(): T {
        esperarQue(descripcion = { "el pedido esperado" }) { isNotEmpty() }
        return poll()
    }

    @Test
    fun `un paneo lento dentro del area que se esta pidiendo no cancela el pedido`() {
        val pendientes = respuestasManuales()
        viewModel.actualizarVista(vistaCerca)
        val primero = pendientes.siguiente()

        // Varios saltos chicos antes de que vuelva: siguen dentro de lo visible más el margen.
        for (paso in 1..4) {
            viewModel.actualizarVista(vistaCerca.copy(oeste = vistaCerca.oeste + paso * 0.01, este = vistaCerca.este + paso * 0.01))
        }
        assertEquals("ningún salto pidió de nuevo", 1, gddService.vecesLlamado("getReportesMapa"))
        assertTrue("el pedido sigue vivo", primero.isActive)

        primero.complete(Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle(id = 11)), total = 1)))
        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals(listOf(11), estado.reportes.map { it.id })
        assertEquals("lo que llegó cubre la vista final", 1, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `salir del area que se esta pidiendo cancela el pedido y pide la zona nueva`() {
        val pendientes = respuestasManuales()
        viewModel.actualizarVista(vistaCerca)
        val primero = pendientes.siguiente()

        viewModel.actualizarVista(vistaCerca.copy(oeste = vistaCerca.oeste + 1.0, este = vistaCerca.este + 1.0))
        val segundo = pendientes.siguiente()
        // La respuesta de la zona vieja llega igual, pero su pedido ya fue cancelado: no se muestra.
        primero.complete(Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle(id = 99)), total = 1)))
        assertTrue(viewModel.state.value.reportes.isEmpty())

        segundo.complete(Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle(id = 12)), total = 1)))
        assertEquals(listOf(12), esperarEstado(viewModel.state) { !it.cargando }.reportes.map { it.id })
        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `volver a la zona cargada cancela la consulta de la zona que ya no se ve`() {
        val propia = Fixtures.reporteDetalle(id = 11)
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(propia)))
        }
        viewModel.actualizarVista(vistaCerca)
        assertEquals(listOf(propia), esperarEstado(viewModel.state) { !it.cargando }.reportes)

        val iniciada = CompletableDeferred<Unit>()
        val cancelada = CompletableDeferred<Unit>()
        val respuesta = CompletableDeferred<Response<ReportesMapaResponse>>()
        gddService.getReportesMapaResult = {
            iniciada.complete(Unit)
            try {
                respuesta.await()
            } finally {
                cancelada.complete(Unit)
            }
        }
        try {
            viewModel.actualizarVista(vistaCerca.copy(oeste = vistaCerca.oeste + 1, este = vistaCerca.este + 1))
            esperarQue { iniciada.isCompleted }
            viewModel.actualizarVista(vistaCerca)
            esperarQue { cancelada.isCompleted }

            val mapa = viewModel.state.value
            assertEquals(listOf(propia), mapa.reportes)
            assertFalse(mapa.actualizando)
            assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
        } finally {
            respuesta.cancel()
            mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
        }
    }

    @Test
    fun `si mientras volvia el pedido se acerco y llegaron grupos, pide los del zoom nuevo`() {
        val pendientes = respuestasManuales()
        viewModel.actualizarVista(vistaCerca)
        val primero = pendientes.siguiente()

        // Se acerca un nivel sin salir del área: los reportes que se están pidiendo servirían.
        viewModel.actualizarVista(vistaCerca.copy(zoom = 13.0))
        assertEquals(1, gddService.vecesLlamado("getReportesMapa"))

        // Pero había demasiados y el servidor agrupó con la celda del zoom 12.
        primero.complete(Response.success(ReportesMapaResponse(nivel = "grupos", grupos = listOf(GrupoReportesMapa(-34.6, -58.4, 900, "Alto")), total = 900)))
        val segundo = pendientes.siguiente()
        assertEquals(13, gddService.consultasMapa.last()["zoom"])
        segundo.complete(Response.success(ReportesMapaResponse(nivel = "grupos", grupos = listOf(GrupoReportesMapa(-34.6, -58.4, 900, "Alto")), total = 900)))
        esperarEstado(viewModel.state) { !it.actualizando && !it.cargando }
        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `la espera entre reintentos`() {
        assertEquals(1_000L, esperaTrasFallos(1, null))
        assertEquals(4_000L, esperaTrasFallos(3, null))
        assertEquals("tope", ESPERA_MAXIMA_MAPA_MS, esperaTrasFallos(40, null))
        assertEquals(5_000L, esperaTrasFallos(1, " 5 "))
        assertEquals("un plazo del servidor puede superar el tope local", 999_000L, esperaTrasFallos(1, "999"))
        assertEquals("fecha HTTP: se usa la propia", 2_000L, esperaTrasFallos(2, "Wed, 21 Oct 2015 07:28:00 GMT"))
        assertEquals(1_000L, esperaTrasFallos(1, "-3"))
        assertTrue(esFallaPasajera(429) && esFallaPasajera(408) && esFallaPasajera(503))
        assertFalse(esFallaPasajera(404) || esFallaPasajera(422) || esFallaPasajera(401))
    }

    private fun conRetryAfter(codigo: Int, segundos: String): Response<ReportesMapaResponse> =
        Response.error(
            "".toResponseBody("application/json".toMediaType()),
            okhttp3.Response.Builder()
                .code(codigo).message("Too Many Requests").protocol(Protocol.HTTP_1_1)
                .request(Request.Builder().url("http://localhost/mapa/reportes").build())
                .header("Retry-After", segundos)
                .build()
        )

    @Test
    fun `sobre el antimeridiano pide un rango que el servidor acepta`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(nivel = "grupos", grupos = emptyList())) }
        // osmdroid con la vista sobre ±180: oeste > este.
        viewModel.actualizarVista(VistaMapa(sur = -60.0, oeste = 165.0, norte = -10.0, este = -5.0, zoom = 3.0))

        esperarEstado(viewModel.state) { !it.cargando }
        val consulta = gddService.consultasMapa.single()
        val (sur, oeste, norte, este) = listOf("sur", "oeste", "norte", "este").map { consulta[it] as Double }
        assertTrue("oeste <= este", oeste <= este)
        assertTrue(oeste >= -180.0 && este <= 180.0 && sur >= -85.0 && norte <= 85.0)
        assertEquals(-180.0 to 180.0, oeste to este)
    }

    @Test
    fun `al borde de las latitudes de osmdroid la zona cargada sigue sirviendo`() {
        // osmdroid llega a ±85,0511; el servidor acepta hasta ±85.
        val polo = VistaMapa(sur = -85.0511, oeste = -100.0, norte = 85.0511, este = -20.0, zoom = 3.0)
        val paneo = polo.copy(oeste = -99.0, este = -21.0)

        val cargada = polo.normalizada().ampliada()
        assertEquals(85.0, cargada.norte, 0.0)
        assertTrue("un paneo chico no vuelve a pedir", cargada.contiene(paneo.normalizada()))
        assertFalse("sin normalizar, la vista nunca cabe en lo cargado", polo.ampliada().contiene(paneo))
    }

    @Test
    fun `con el mundo entero a la vista pide todo el alto y no una franja del artico`() {
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "grupos", grupos = listOf(GrupoReportesMapa(-34.6, -58.4, 112, "Alto")), total = 112))
        }
        // Lo que devolvió osmdroid en el emulador a zoom 3,3: el sur por encima del norte.
        viewModel.actualizarVista(VistaMapa(sur = 81.94447148875369, oeste = -137.3264360139362, norte = 79.0716908288072, este = 16.812189755123796, zoom = 3.3))

        esperarEstado(viewModel.state) { !it.cargando }
        val consulta = gddService.consultasMapa.single()
        assertEquals(-85.0 to 85.0, consulta["sur"] to consulta["norte"])
        assertTrue("Argentina entra en lo pedido", (consulta["oeste"] as Double) < -58.4 && (consulta["este"] as Double) > -58.4)
        assertEquals(112, viewModel.state.value.total)
    }

    @Test
    fun `si el mundo es mas bajo que el mapa se pide todo el alto`() {
        // Emulador 1080x2400 a zoom 3: el mundo mide 2048 px y osmdroid devolvió de 74 a 84,5.
        val chico = vistaDeOsmdroid(73.97, -155.2, 84.52, 34.6, 3.0, altoMundoPx = 2048.0, altoMapaPx = 2400.0)
        assertEquals(-85.0 to 85.0, chico.sur to chico.norte)
        assertEquals(-155.2 to 34.6, chico.oeste to chico.este)
        // A zoom 5 el mundo mide 8192 px: la caja de osmdroid vale.
        val normal = vistaDeOsmdroid(-62.0, -79.5, 5.9, -41.0, 5.3, altoMundoPx = 8192.0, altoMapaPx = 2400.0)
        assertEquals(-62.0 to 5.9, normal.sur to normal.norte)
        // Antes del primer layout el mapa mide 0: no se inventa nada.
        assertEquals(1.0, vistaDeOsmdroid(1.0, 2.0, 3.0, 4.0, 3.0, 2048.0, 0.0).sur, 0.0)
    }

    @Test
    fun `un error del servidor se informa sin pasar a modo local`() {
        gddService.getReportesMapaResult = { FakeGDDService.errorServidor(500) }

        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals("No se pudieron obtener las alertas del servidor.", estado.error)
    }

    @Test
    fun `sin conexion informa el error y se puede reintentar`() {
        gddService.getReportesMapaResult = { FakeGDDService.sinConexion() }
        viewModel.actualizarVista(vistaCerca)
        assertNotNull(esperarEstado(viewModel.state) { !it.cargando }.error)

        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = listOf(Fixtures.reporteDetalle()))) }
        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { it.error == null && !it.actualizando }
        assertEquals(1, estado.reportes.size)
    }

    // ── piezas puras ──────────────────────────────────────────────────────────

    @Test
    fun `el nivel cambia en el umbral de zoom`() {
        assertEquals(NivelMapa.GRUPOS, nivelParaZoom(ZOOM_REPORTES_INDIVIDUALES - 0.01))
        assertEquals(NivelMapa.REPORTES, nivelParaZoom(ZOOM_REPORTES_INDIVIDUALES))
    }

    @Test
    fun `la celda se achica a la mitad con cada zoom y siempre la acepta el servidor`() {
        assertEquals(90.0, celdaGradosParaZoom(0), 0.0)
        assertEquals(celdaGradosParaZoom(5) / 2, celdaGradosParaZoom(6), 1e-12)
        // El servidor acepta celdas entre 1e-6 y 90 grados.
        (0..ZOOM_MAXIMO_SERVIDOR).forEach { zoom ->
            assertTrue("zoom $zoom", celdaGradosParaZoom(zoom) in 1e-6..90.0)
        }
    }

    @Test
    fun `la vista ampliada contiene a la original y no se sale del mundo`() {
        val ampliada = vistaCerca.ampliada()
        assertTrue(ampliada.contiene(vistaCerca))
        assertFalse(vistaCerca.contiene(ampliada))

        val mundo = VistaMapa(-89.0, -179.0, 89.0, 179.0, 3.0).ampliada()
        assertEquals(-85.0, mundo.sur, 0.0)
        assertEquals(180.0, mundo.este, 0.0)
    }

    @Test
    fun `un grupo sin severidad no se dibuja como bajo`() {
        val desconocido = PuntoMapa(-34.0, -60.0, 3, SEVERIDAD_DESCONOCIDA, null)
        val bajo = PuntoMapa(-34.0, -60.0, 1, "Bajo", null)

        val (solo) = agruparReportes(listOf(desconocido), zoom = 10.0, ladoPx = 78.0)
        val (mezcla) = agruparReportes(listOf(desconocido, bajo), zoom = 10.0, ladoPx = 78.0)

        assertEquals(-1, rangoSeveridad(solo.severidadDominante))
        assertEquals("una severidad conocida manda sobre la desconocida", "Bajo", mezcla.severidadDominante)
        assertEquals(4, mezcla.cantidad)
    }

    /** Distancia en píxeles del mapa entre dos grupos, con la misma proyección que osmdroid. */
    private fun distanciaPx(a: ClusterReportes, b: ClusterReportes, zoom: Double): Double {
        val mundo = 256.0 * Math.pow(2.0, zoom)
        fun x(lon: Double) = (lon + 180) / 360 * mundo
        fun y(lat: Double): Double {
            val s = Math.sin(Math.toRadians(lat))
            return (0.5 - Math.log((1 + s) / (1 - s)) / (4 * Math.PI)) * mundo
        }
        return Math.hypot(x(a.longitud) - x(b.longitud), y(a.latitud) - y(b.latitud))
    }

    @Test
    fun `dos reportes a metros a cada lado del borde de una celda quedan en un solo pin`() {
        val zoom = 12.0
        val lado = 168.0 // 64 dp con densidad 2,625
        // El borde de una columna de la grilla, en longitud, y un reporte a 2 m de cada lado.
        val bordeLon = (Math.floor((-60.0 + 180) / 360 * 256 * Math.pow(2.0, zoom) / lado) * lado) / (256 * Math.pow(2.0, zoom)) * 360 - 180
        val metro = 1.0 / 92_000 // grados de longitud por metro a 34° S, aproximado
        val oeste = PuntoMapa(-34.6, bordeLon - 2 * metro, 1, "Alto", Fixtures.reporteDetalle(id = 1))
        val este = PuntoMapa(-34.6, bordeLon + 2 * metro, 1, "Bajo", Fixtures.reporteDetalle(id = 2))

        val grupos = agruparReportes(listOf(oeste, este), zoom, lado)

        assertEquals("se dibujaban dos íconos encimados", 1, grupos.size)
        assertEquals(setOf(1, 2), grupos.single().reportes.map { it.id }.toSet())
        assertEquals("Alto", grupos.single().severidadDominante)
    }

    @Test
    fun `ningun par de pines queda mas cerca que la separacion y no se pierde ningun reporte`() {
        val azar = java.util.Random(7)
        val puntos = List(400) { i ->
            val reporte = if (i % 3 == 0) Fixtures.reporteDetalle(id = i) else null
            PuntoMapa(-35.0 + azar.nextDouble() * 2, -61.0 + azar.nextDouble() * 2, 1 + azar.nextInt(5), "Medio", reporte)
        }
        for (zoom in listOf(5.0, 8.3, 11.0, 14.0)) {
            val lado = 168.0
            val grupos = agruparReportes(puntos, zoom, lado)
            for (i in grupos.indices) for (j in i + 1 until grupos.size) {
                assertTrue("zoom $zoom: ${grupos[i].id} y ${grupos[j].id} encimados", distanciaPx(grupos[i], grupos[j], zoom) >= lado)
            }
            assertEquals(puntos.sumOf { it.cantidad }, grupos.sumOf { it.cantidad })
            assertEquals(puntos.mapNotNull { it.reporte?.id }.sorted(), grupos.flatMap { c -> c.reportes.map { it.id } }.sorted())
            assertEquals("ids únicos", grupos.size, grupos.map { it.id }.toSet().size)
        }
    }

    @Test
    fun `reportes lejanos siguen en pines separados`() {
        val a = PuntoMapa(-34.0, -60.0, 1, "Alto", Fixtures.reporteDetalle(id = 1))
        val b = PuntoMapa(-34.0, -59.0, 1, "Alto", Fixtures.reporteDetalle(id = 2))
        // A zoom 12, un grado son unos 2900 px.
        assertEquals(2, agruparReportes(listOf(a, b), zoom = 12.0, ladoPx = 168.0).size)
    }

    @Test
    fun `la tarjeta de seleccion muestra los reportes que quedan tras filtrar`() {
        val celda = { reportes: List<ReporteDetalleResponse> ->
            ClusterReportes("10:20", -34.0, -60.0, reportes.size, "Alto", reportes)
        }
        val antes = celda(listOf(Fixtures.reporteDetalle(id = 1), Fixtures.reporteDetalle(id = 2), Fixtures.reporteDetalle(id = 3)))
        val despues = celda(listOf(Fixtures.reporteDetalle(id = 1)))

        assertEquals(listOf(1), seleccionVigente(antes, listOf(despues))?.reportes?.map { it.id })
        assertNull("la celda ya no está", seleccionVigente(antes, emptyList()))
        assertNull(seleccionVigente(null, listOf(despues)))
    }

    @Test
    fun `contar filtros activos`() {
        assertEquals(0, FiltrosMapa().activos)
        assertEquals(2, FiltrosMapa(ambito = AmbitoMapa.PROPIOS, dias = 7).activos)
    }

    @Test
    fun `zoom excesivo se limita al maximo permitido por el servidor`() {
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(reportes = emptyList()))
        }

        val vistaConZoom29 = VistaMapa(sur = -34.7, oeste = -58.5, norte = -34.5, este = -58.3, zoom = 29.0)
        viewModel.actualizarVista(vistaConZoom29)

        esperarEstado(viewModel.state) { !it.cargando }
        val consulta = gddService.consultasMapa.single()
        assertEquals(ZOOM_MAXIMO_SERVIDOR, consulta["zoom"])
    }
}
