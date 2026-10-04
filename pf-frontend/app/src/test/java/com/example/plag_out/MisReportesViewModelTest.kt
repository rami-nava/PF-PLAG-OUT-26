package com.example.plag_out

import androidx.lifecycle.viewModelScope
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import java.time.LocalDate
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MisReportesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: MisReportesViewModel

    private lateinit var zonaOriginal: TimeZone

    @After
    fun restaurarEstado() = runBlocking {
        try {
            withTimeout(5_000) {
                viewModel.viewModelScope.coroutineContext[Job]!!.cancelAndJoin()
            }
        } finally {
            TimeZone.setDefault(zonaOriginal)
        }
    }

    @Before
    fun setup() {
        zonaOriginal = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        gddService = FakeGDDService()
        gddService.getUsuarioActualResult = { Response.success(Fixtures.usuario()) }
        viewModel = MisReportesViewModel(gddService, reportesDispatcher = mainDispatcherRule.testDispatcher)
        // La precarga del perfil debe terminar antes de editar el radio; con Main
        // unconfined sus respuestas pueden reanudarse en otro hilo de Dispatchers.IO.
        runBlocking {
            withTimeout(5_000) {
                viewModel.viewModelScope.coroutineContext[Job]!!.children.toList().joinAll()
            }
        }
    }

    @Test
    fun `cargarReportes exitoso obtiene lista de reportes`() {
        val listaReportes = listOf(
            Fixtures.reporteDetalle(id = 10, plagaNombre = "Chicharrita", nivelSeveridad = "Alto"),
            Fixtures.reporteDetalle(id = 11, plagaNombre = "Oruga", nivelSeveridad = "Bajo")
        )
        gddService.getReportesResult = { Response.success(listaReportes) }

        viewModel.cargarReportes()

        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(2, estado.reportes.size)
        assertEquals("Chicharrita", estado.reportes[0].plaga_nombre)
        assertEquals("Oruga", estado.reportes[1].plaga_nombre)
        assertNull(estado.error)
        assertEquals(1, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `cargarReportes maneja error de servidor`() {
        gddService.getReportesResult = { FakeGDDService.errorServidor(500) }

        viewModel.cargarReportes()

        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(0, estado.reportes.size)
        assertNotNull(estado.error)
        assertEquals(1, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `cargarReportes sin conexion maneja excepcion`() {
        gddService.getReportesResult = { FakeGDDService.sinConexion() }

        viewModel.cargarReportes()

        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(0, estado.reportes.size)
        assertNotNull(estado.error)
    }

    @Test
    fun `refrescar fuerza la recarga de datos`() {
        gddService.getReportesResult = { Response.success(listOf(Fixtures.reporteDetalle(id = 1))) }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }

        gddService.getReportesResult = { Response.success(listOf(Fixtures.reporteDetalle(id = 1), Fixtures.reporteDetalle(id = 2))) }
        viewModel.refrescar()

        val estadoRefrescado = esperarEstado(viewModel.state) { !it.isRefreshing && !it.isLoading }
        assertEquals(2, estadoRefrescado.reportes.size)
        assertEquals(2, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `abrirConfiguracionRadio y onRadioTemporalChange actualizan el estado del dialogo`() {
        viewModel.abrirConfiguracionRadio()
        var estado = viewModel.state.value
        assertEquals(true, estado.mostrarDialogoRadio)
        assertEquals(20f, estado.radioTemporalKm)

        viewModel.onRadioTemporalChange(50f)
        estado = viewModel.state.value
        assertEquals(50f, estado.radioTemporalKm)

        viewModel.cerrarConfiguracionRadio()
        estado = viewModel.state.value
        assertEquals(false, estado.mostrarDialogoRadio)
    }

    @Test
    fun `guardarRadioNotificacion actualiza radio en backend y estado`() {
        val userActualizado = Fixtures.usuario().copy(radio_notificacion_km = 45.0)
        gddService.actualizarUsuarioResult = { Response.success(userActualizado) }

        viewModel.abrirConfiguracionRadio()
        viewModel.onRadioTemporalChange(45f)
        viewModel.guardarRadioNotificacion()

        val estado = esperarEstado(viewModel.state) { !it.guardandoRadio && !it.mostrarDialogoRadio }
        assertEquals(45.0, estado.radioNotificacionKm, 0.001)
        assertEquals(45f, estado.radioTemporalKm)
        assertEquals(false, estado.mostrarDialogoRadio)
        assertEquals(1, gddService.vecesLlamado("actualizarUsuario"))
        assertEquals(45.0, gddService.ultimoActualizarUsuario?.radio_notificacion_km)
    }

    @Test
    fun `guardarRadioNotificacion maneja error de backend`() {
        gddService.actualizarUsuarioResult = { FakeGDDService.errorServidor(500) }

        viewModel.abrirConfiguracionRadio()
        viewModel.onRadioTemporalChange(35f)
        viewModel.guardarRadioNotificacion()

        val estado = esperarEstado(viewModel.state) { !it.guardandoRadio }
        assertNotNull(estado.error)
        assertEquals(false, estado.guardandoRadio)
    }
    @Test
    fun `120 reportes incluyen los de la segunda pagina en el estado compartido`() {
        val reportes = (1..120).map { Fixtures.reporteDetalle(id = it) }
        gddService.getReportesPaginaResult = { _, _, limit, offset ->
            Response.success(reportes.drop(offset).take(limit))
        }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { !it.isLoading && !it.isRefreshing }
        assertEquals(reportes, estado.reportes)
        assertEquals(listOf(0, 100), gddService.consultasReportes.map { it.offset })
        assertTrue(gddService.consultasReportes.all { it.limit == 100 })
        assertEquals(1, gddService.consultasReportes.map { it.desde to it.hasta }.distinct().size)
        assertNull(estado.error)
    }

    @Test
    fun `todas las paginas incluyen el dia local completo en Argentina`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
        val reportes = (1..120).map { Fixtures.reporteDetalle(id = it) }
        gddService.getReportesPaginaResult = { _, _, limit, offset ->
            Response.success(reportes.drop(offset).take(limit))
        }
        viewModel.actualizarFechas(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10))
        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(reportes, estado.reportes)
        assertEquals(2, gddService.consultasReportes.size)
        gddService.consultasReportes.forEach {
            assertEquals("2026-09-01T03:00:00Z", it.desde)
            assertEquals("2026-09-11T02:59:59Z", it.hasta)
        }
    }

    @Test
    fun `rango local respeta un dia de 23 horas al cambiar horario de verano`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        gddService.getReportesResult = { Response.success(emptyList()) }
        val dia = LocalDate.of(2026, 3, 8)
        viewModel.actualizarFechas(dia, dia)
        esperarEstado(viewModel.state) { !it.isLoading }
        val consulta = gddService.consultasReportes.single()
        assertEquals("2026-03-08T05:00:00Z", consulta.desde)
        assertEquals("2026-03-09T03:59:59Z", consulta.hasta)
    }

    @Test
    fun `multiplo exacto pide una pagina vacia para confirmar el fin`() {
        val reportes = (1..200).map { Fixtures.reporteDetalle(id = it) }
        gddService.getReportesPaginaResult = { _, _, limit, offset ->
            Response.success(reportes.drop(offset).take(limit))
        }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(reportes, estado.reportes)
        assertEquals(listOf(0, 100, 200), gddService.consultasReportes.map { it.offset })
    }

    @Test
    fun `error en segunda pagina nunca publica la primera como lista completa`() {
        gddService.getReportesPaginaResult = { _, _, _, offset ->
            if (offset == 0) Response.success((1..100).map { Fixtures.reporteDetalle(id = it) })
            else FakeGDDService.errorServidor(500)
        }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertTrue(estado.reportes.isEmpty())
        assertNotNull(estado.error)
    }

    @Test
    fun `refresco fallido mantiene la ultima carga completa y permite reintentar`() {
        val anterior = listOf(Fixtures.reporteDetalle(id = 999))
        gddService.getReportesResult = { Response.success(anterior) }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        gddService.getReportesPaginaResult = { _, _, _, offset ->
            if (offset == 0) Response.success((1..100).map { Fixtures.reporteDetalle(id = it) })
            else FakeGDDService.errorServidor(429)
        }
        viewModel.refrescar()
        val fallido = esperarEstado(viewModel.state) { !it.isRefreshing }
        assertEquals(anterior, fallido.reportes)
        assertNotNull(fallido.error)
        gddService.getReportesPaginaResult = { _, _, _, _ -> Response.success(listOf(Fixtures.reporteDetalle(id = 120))) }
        viewModel.refrescar()
        val recuperado = esperarEstado(viewModel.state) { !it.isRefreshing }
        assertEquals(listOf(120), recuperado.reportes.map { it.id })
        assertNull(recuperado.error)
    }

    @Test
    fun `deduplica ids repetidos sin retroceder el offset`() {
        gddService.getReportesPaginaResult = { _, _, _, offset ->
            Response.success((if (offset == 0) 1..100 else 100..105).map { Fixtures.reporteDetalle(id = it) })
        }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals((1..105).toList(), estado.reportes.map { it.id })
        assertEquals(listOf(0, 100), gddService.consultasReportes.map { it.offset })
    }

    @Test
    fun `servidor que ignora offset da error en vez de bucle o lista parcial`() {
        gddService.getReportesResult = { Response.success((1..100).map { Fixtures.reporteDetalle(id = it) }) }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertTrue(estado.reportes.isEmpty())
        assertNotNull(estado.error)
        assertEquals(2, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `respuesta sin body no se interpreta como fin de reportes`() {
        gddService.getReportesResult = { Response.success<List<ReporteDetalleResponse>>(null) }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertTrue(estado.reportes.isEmpty())
        assertNotNull(estado.error)
    }

    @Test
    fun `segunda pagina pendiente no muestra resultados parciales y cambiar fechas descarta respuesta vieja`() {
        val segundaPagina = CompletableDeferred<Unit>()
        val liberarRespuestaVieja = CompletableDeferred<Unit>()
        gddService.getReportesPaginaResult = { _, _, _, offset ->
            if (offset == 0) Response.success((1..100).map { Fixtures.reporteDetalle(id = it) })
            else {
                segundaPagina.complete(Unit)
                withContext(NonCancellable) { liberarRespuestaVieja.await() }
                Response.success(listOf(Fixtures.reporteDetalle(id = 101)))
            }
        }
        viewModel.cargarReportes()
        runBlocking { withTimeout(3000) { segundaPagina.await() } }
        assertTrue(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.reportes.isEmpty())
        gddService.getReportesPaginaResult = { _, _, _, _ -> Response.success(listOf(Fixtures.reporteDetalle(id = 888))) }
        val desde = LocalDate.of(2026, 9, 1)
        val hasta = LocalDate.of(2026, 9, 10)
        viewModel.actualizarFechas(desde, hasta)
        esperarEstado(viewModel.state) { !it.isLoading }
        liberarRespuestaVieja.complete(Unit)
        assertEquals(listOf(888), viewModel.state.value.reportes.map { it.id })
        assertEquals("2026-09-01T00:00:00Z", gddService.consultasReportes.last().desde)
        assertEquals("2026-09-10T23:59:59Z", gddService.consultasReportes.last().hasta)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `limpiar cancela carga pendiente y no repuebla los reportes de la sesion anterior`() {
        val iniciado = CompletableDeferred<Unit>()
        val liberado = CompletableDeferred<Unit>()
        gddService.getReportesPaginaResult = { _, _, _, _ ->
            iniciado.complete(Unit)
            withContext(NonCancellable) { liberado.await() }
            Response.success(listOf(Fixtures.reporteDetalle(id = 444)))
        }
        viewModel.cargarReportes()
        runBlocking { withTimeout(3000) { iniciado.await() } }
        viewModel.limpiar()
        liberado.complete(Unit)
        assertTrue(viewModel.state.value.reportes.isEmpty())
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `cambiar fechas retira resultados del rango anterior aunque la nueva carga falle`() {
        gddService.getReportesResult = { Response.success(listOf(Fixtures.reporteDetalle(id = 1))) }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        gddService.getReportesResult = { FakeGDDService.errorServidor(500) }
        viewModel.actualizarFechas(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))
        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertTrue(estado.reportes.isEmpty())
        assertNotNull(estado.error)
    }
}
