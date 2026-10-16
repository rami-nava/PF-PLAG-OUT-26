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
        assertNull(estado.error)
        assertEquals(true, estado.mostrarDialogoRadio)
    }

    private fun propio(id: Int, severidad: String = "Alto", plagaId: Int = 1) =
        Fixtures.reporteDetalle(id = id, nivelSeveridad = severidad, plagaId = plagaId)

    private fun ajeno(id: Int, severidad: String = "Alto", plagaId: Int = 1) =
        Fixtures.reporteDetalle(id = id, nivelSeveridad = severidad, plagaId = plagaId, esPropio = false)

    @Test
    fun `cargarReportes pide el resumen y la primera pagina de 20 de la pestana seleccionada`() {
        gddService.reportesServidor = (1..45).map { propio(it) } + (100..102).map { ajeno(it) }

        viewModel.cargarReportes()

        val estado = esperarEstado(viewModel.state) { !it.isLoading && it.propios.conteo != null }
        assertEquals((1..20).toList(), estado.reportes.map { it.id })
        assertEquals(45, estado.propios.total)
        assertTrue(estado.hayMas)
        assertEquals(45, estado.conteo(AmbitoReportes.PROPIOS)?.total)
        // La comunidad todavía no se pidió: ni lista ni totales.
        assertNull(estado.conteo(AmbitoReportes.COMUNIDAD))
        val consulta = gddService.consultasReportes.single()
        assertEquals("propios", consulta.ambito)
        assertEquals(REPORTES_PAGE_SIZE, consulta.limit)
        assertEquals(0, consulta.offset)
        assertNull(consulta.distanciaKm)
        // El resumen usa la misma consulta, sin paginar.
        assertEquals(consulta.copy(limit = 0), gddService.consultasResumen.single())
    }

    @Test
    fun `cargarMas agrega paginas hasta llegar al total con la misma consulta`() {
        gddService.reportesServidor = (1..45).map { propio(it) }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }

        viewModel.cargarMas()
        esperarEstado(viewModel.state) { !it.cargandoMas && it.reportes.size == 40 }
        viewModel.cargarMas()
        val fin = esperarEstado(viewModel.state) { !it.cargandoMas && it.reportes.size == 45 }

        assertEquals((1..45).toList(), fin.reportes.map { it.id })
        assertEquals(false, fin.hayMas)
        assertEquals(listOf(0, 20, 40), gddService.consultasReportes.map { it.offset })
        assertEquals(1, gddService.consultasReportes.map { it.copy(offset = 0) }.distinct().size)
        viewModel.cargarMas()
        assertEquals(3, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `cada pestana pide su ambito y se carga recien al abrirla`() {
        gddService.reportesServidor = listOf(propio(1), ajeno(2))
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }

        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        val comunidad = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(listOf(2), comunidad.reportes.map { it.id })

        viewModel.seleccionarAmbito(AmbitoReportes.PROPIOS)
        assertEquals(listOf(1), viewModel.state.value.reportes.map { it.id })
        assertEquals(listOf("propios", "comunidad"), gddService.consultasReportes.map { it.ambito })
    }

    @Test
    fun `aplicar un filtro rehace la consulta desde la primera pagina con el filtro`() {
        gddService.reportesServidor = (1..30).map { propio(it, severidad = if (it % 3 == 0) "Bajo" else "Alto") }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.cargarMas()
        esperarEstado(viewModel.state) { !it.cargandoMas && it.reportes.size == 30 }
        gddService.consultasReportes.clear()

        viewModel.actualizarFiltros(FiltrosReportes(severidad = "Bajo", terrenoId = 4))

        val estado = esperarEstado(viewModel.state) { !it.isLoading && it.propios.conteo != null }
        assertEquals((1..30).filter { it % 3 == 0 }, estado.reportes.map { it.id })
        assertEquals(10, estado.propios.total)
        val consulta = gddService.consultasReportes.single()
        assertEquals(0, consulta.offset)
        assertEquals("Bajo", consulta.severidad)
        assertEquals(4, consulta.terrenoId)
        // El encabezado también se vuelve a pedir con los filtros, severidad incluida.
        assertEquals(consulta.copy(limit = 0), gddService.consultasResumen.last())
        assertEquals(ConteoReportes(total = 10, alto = 0, medio = 0, bajo = 10), estado.conteo(AmbitoReportes.PROPIOS))

        viewModel.limpiarFiltros()
        val limpio = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(30, limpio.propios.total)
        assertNull(gddService.consultasReportes.last().severidad)
    }

    @Test
    fun `los filtros son independientes por pestana`() {
        gddService.reportesServidor = listOf(propio(1, plagaId = 1), ajeno(2, plagaId = 7), ajeno(3, plagaId = 8))
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        esperarEstado(viewModel.state) { !it.isLoading }

        viewModel.actualizarFiltros(FiltrosReportes(plagaId = 7))
        val comunidad = esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(listOf(2), comunidad.reportes.map { it.id })
        assertEquals(FiltrosReportes(), comunidad.filtrosPropios)
        assertEquals(7, gddService.consultasReportes.last().plagaId)

        viewModel.seleccionarAmbito(AmbitoReportes.PROPIOS)
        assertEquals(listOf(1), viewModel.state.value.reportes.map { it.id })
    }

    @Test
    fun `la distancia solo afecta a la comunidad y al resumen`() {
        gddService.reportesServidor = listOf(propio(1), ajeno(2))
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }

        viewModel.actualizarDistanciaListado(50)
        assertEquals(listOf(1), viewModel.state.value.reportes.map { it.id })
        assertEquals(1, gddService.vecesLlamado("getReportes"))
        assertEquals(1, gddService.vecesLlamado("getResumenReportes"))

        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        esperarEstado(viewModel.state) { !it.isLoading }
        assertEquals(50, gddService.consultasReportes.last().distanciaKm)
        assertEquals(50, gddService.consultasResumen.last().distanciaKm)
        assertEquals(20.0, viewModel.state.value.radioNotificacionKm, 0.0)
        assertEquals(0, gddService.vecesLlamado("actualizarUsuario"))
    }

    @Test
    fun `limpiar filtros en la comunidad vuelve al radio configurado en una sola recarga`() {
        gddService.reportesServidor = listOf(ajeno(2, severidad = "Bajo"))
        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.actualizarDistanciaListado(50)
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.actualizarFiltros(FiltrosReportes(severidad = "Bajo"))
        esperarEstado(viewModel.state) { !it.isLoading }
        gddService.consultasReportes.clear()

        viewModel.limpiarFiltros()

        val estado = esperarEstado(viewModel.state) { !it.isLoading }
        assertNull(estado.distanciaListadoKm)
        assertEquals(FiltrosReportes(), estado.filtrosComunidad)
        val consulta = gddService.consultasReportes.single()
        assertNull(consulta.distanciaKm)
        assertNull(consulta.severidad)
    }

    @Test
    fun `guardarRadioNotificacion recarga la comunidad con el radio nuevo`() {
        gddService.actualizarUsuarioResult = { Response.success(Fixtures.usuario().copy(radio_notificacion_km = 50.0)) }
        gddService.reportesServidor = listOf(ajeno(1), ajeno(2))
        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        esperarEstado(viewModel.state) { !it.isLoading }

        viewModel.abrirConfiguracionRadio()
        viewModel.onRadioTemporalChange(50f)
        viewModel.guardarRadioNotificacion()

        val estado = esperarEstado(viewModel.state) { !it.guardandoRadio && !it.isLoading && it.reportes.size == 2 }
        assertEquals(2, gddService.vecesLlamado("getReportes"))
        assertEquals(listOf(1, 2), estado.reportes.map { it.id })
    }

    @Test
    fun `guardarRadioNotificacion sin cambiar el radio no recarga los reportes`() {
        gddService.actualizarUsuarioResult = { Response.success(Fixtures.usuario().copy(radio_notificacion_km = 20.0)) }

        viewModel.abrirConfiguracionRadio()
        viewModel.guardarRadioNotificacion()

        esperarEstado(viewModel.state) { !it.guardandoRadio && !it.mostrarDialogoRadio }
        assertEquals(0, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `guardarRadioNotificacion no recarga si el listado usa una distancia propia`() {
        gddService.reportesServidor = listOf(ajeno(1))
        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        viewModel.actualizarDistanciaListado(50)
        esperarEstado(viewModel.state) { !it.isLoading && !it.isRefreshing }
        gddService.consultasReportes.clear()
        gddService.actualizarUsuarioResult = { Response.success(Fixtures.usuario().copy(radio_notificacion_km = 80.0)) }

        viewModel.abrirConfiguracionRadio()
        viewModel.onRadioTemporalChange(80f)
        viewModel.guardarRadioNotificacion()

        val estado = esperarEstado(viewModel.state) { !it.guardandoRadio && !it.mostrarDialogoRadio }
        assertEquals(80.0, estado.radioNotificacionKm, 0.001)
        assertEquals(50, estado.distanciaListadoKm)
        assertTrue(gddService.consultasReportes.isEmpty())
    }

    @Test
    fun `guardarRadioNotificacion con error no recarga los reportes`() {
        gddService.actualizarUsuarioResult = { FakeGDDService.errorServidor(500) }

        viewModel.abrirConfiguracionRadio()
        viewModel.onRadioTemporalChange(80f)
        viewModel.guardarRadioNotificacion()

        esperarEstado(viewModel.state) { !it.guardandoRadio }
        assertEquals(0, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `el rango incluye el dia local completo en Argentina y es el mismo en todas las consultas`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"))
        gddService.reportesServidor = (1..30).map { propio(it) }
        viewModel.actualizarFechas(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10))
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.cargarMas()
        esperarEstado(viewModel.state) { !it.cargandoMas && !it.hayMas }

        (gddService.consultasReportes.map { it.desde to it.hasta } +
            gddService.consultasResumen.map { it.desde to it.hasta }).forEach {
            assertEquals("2026-09-01T03:00:00Z", it.first)
            assertEquals("2026-09-11T02:59:59.999999999Z", it.second)
        }
    }

    @Test
    fun `rango local respeta un dia de 23 horas al cambiar horario de verano`() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        val dia = LocalDate.of(2026, 3, 8)
        viewModel.actualizarFechas(dia, dia)
        esperarEstado(viewModel.state) { !it.isLoading }
        val consulta = gddService.consultasReportes.single()
        assertEquals("2026-03-08T05:00:00Z", consulta.desde)
        assertEquals("2026-03-09T03:59:59.999999999Z", consulta.hasta)
    }

    @Test
    fun `cambiar fechas descarta lo cargado de las dos pestanas y una respuesta vieja`() {
        val iniciado = CompletableDeferred<Unit>()
        val liberado = CompletableDeferred<Unit>()
        gddService.reportesServidor = listOf(propio(1), ajeno(2))
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        esperarEstado(viewModel.state) { !it.isLoading }
        gddService.getReportesPaginaResult = {
            iniciado.complete(Unit)
            withContext(NonCancellable) { liberado.await() }
            Response.success(PaginaReportes(listOf(ajeno(444)), 1))
        }
        viewModel.actualizarFechas(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10))
        runBlocking { withTimeout(3000) { iniciado.await() } }
        assertTrue(viewModel.state.value.reportes.isEmpty())
        assertEquals(false, viewModel.state.value.propios.cargado)

        gddService.getReportesPaginaResult = null
        gddService.reportesServidor = listOf(ajeno(888))
        viewModel.actualizarFechas(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))
        esperarEstado(viewModel.state) { !it.isLoading }
        liberado.complete(Unit)
        assertEquals(listOf(888), viewModel.state.value.reportes.map { it.id })
    }

    @Test
    fun `error en la primera pagina se muestra y refrescar fallido conserva lo anterior`() {
        gddService.getReportesPaginaResult = { FakeGDDService.errorServidor(500) }
        viewModel.cargarReportes()
        val fallido = esperarEstado(viewModel.state) { it.error != null }
        assertTrue(fallido.reportes.isEmpty())

        gddService.getReportesPaginaResult = null
        gddService.reportesServidor = listOf(propio(999))
        viewModel.refrescar()
        esperarEstado(viewModel.state) { !it.isLoading && !it.isRefreshing && it.reportes.isNotEmpty() }

        gddService.getReportesPaginaResult = { FakeGDDService.errorServidor(429) }
        viewModel.refrescar()
        val refrescoFallido = esperarEstado(viewModel.state) { !it.isRefreshing && it.error != null }
        assertEquals(listOf(999), refrescoFallido.reportes.map { it.id })
    }

    @Test
    fun `sin conexion marca el estado offline`() {
        gddService.getReportesPaginaResult = { FakeGDDService.sinConexion() }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertTrue(estado.sinConexion)
    }

    @Test
    fun `error en la pagina siguiente conserva lo cargado y permite reintentar`() {
        gddService.reportesServidor = (1..25).map { propio(it) }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        gddService.getReportesPaginaResult = { FakeGDDService.errorServidor(500) }
        viewModel.cargarMas()
        val fallido = esperarEstado(viewModel.state) { !it.cargandoMas && it.errorCargarMas != null }
        assertEquals(20, fallido.reportes.size)
        assertNull(fallido.error)
        assertTrue(fallido.hayMas)

        gddService.getReportesPaginaResult = null
        viewModel.cargarMas()
        val recuperado = esperarEstado(viewModel.state) { !it.cargandoMas && !it.hayMas }
        assertEquals((1..25).toList(), recuperado.reportes.map { it.id })
        assertNull(recuperado.errorCargarMas)
    }

    @Test
    fun `servidor que ignora offset da error en vez de bucle`() {
        gddService.getReportesPaginaResult = { Response.success(PaginaReportes((1..20).map { propio(it) }, 100)) }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.cargarMas()
        val estado = esperarEstado(viewModel.state) { !it.cargandoMas }
        assertEquals(20, estado.reportes.size)
        assertNotNull(estado.errorCargarMas)
        assertEquals(2, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `respuesta sin body da error`() {
        gddService.getReportesPaginaResult = { Response.success<PaginaReportes>(null) }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertTrue(estado.reportes.isEmpty())
    }

    @Test
    fun `refrescar deja la otra pestana desactualizada y la recarga al abrirla`() {
        gddService.reportesServidor = listOf(propio(1), ajeno(2))
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading }
        viewModel.seleccionarAmbito(AmbitoReportes.COMUNIDAD)
        esperarEstado(viewModel.state) { !it.isLoading }

        viewModel.refrescar()
        esperarEstado(viewModel.state) { !it.isRefreshing }
        viewModel.seleccionarAmbito(AmbitoReportes.PROPIOS)
        esperarEstado(viewModel.state) { !it.isLoading && !it.isRefreshing && !it.propios.desactualizado }

        assertEquals(listOf("propios", "comunidad", "comunidad", "propios"), gddService.consultasReportes.map { it.ambito })
    }

    @Test
    fun `eliminar descuenta de la lista el total y el encabezado y corre el offset`() {
        gddService.reportesServidor = (1..30).map { propio(it, severidad = if (it == 5) "Medio" else "Alto") }
        viewModel.cargarReportes()
        esperarEstado(viewModel.state) { !it.isLoading && it.propios.conteo != null }

        gddService.reportesServidor = gddService.reportesServidor.filter { it.id != 5 }
        var resultado: String? = "pendiente"
        viewModel.eliminarReporte(5) { resultado = it }
        val trasEliminar = esperarEstado(viewModel.state) { 5 !in it.eliminando && it.reportes.none { r -> r.id == 5 } }
        assertNull(resultado)
        assertEquals(29, trasEliminar.propios.total)
        assertEquals(ConteoReportes(total = 29, alto = 29, medio = 0, bajo = 0), trasEliminar.conteo(AmbitoReportes.PROPIOS))

        viewModel.cargarMas()
        val estado = esperarEstado(viewModel.state) { !it.cargandoMas && !it.hayMas }
        assertEquals((1..30).filter { it != 5 }, estado.reportes.map { it.id })
        assertEquals(listOf(0, 19), gddService.consultasReportes.map { it.offset })
    }

    @Test
    fun `opciones de filtros vienen de los terrenos del usuario y el catalogo de plagas`() {
        gddService.getTerrenosResult = { Response.success(listOf(Fixtures.terreno(id = 2, nombre = "Sur"), Fixtures.terreno(id = 1, nombre = "Norte"))) }
        gddService.getPlagasResult = { Response.success(listOf(Fixtures.plaga(id = 7, nombre = "Trips"), Fixtures.plaga(id = 3, nombre = "Oruga"))) }
        viewModel.cargarReportes()
        val estado = esperarEstado(viewModel.state) { it.terrenos.isNotEmpty() && it.plagas.isNotEmpty() }
        assertEquals(listOf("Norte", "Sur"), estado.terrenos.map { it.terreno_nombre })
        assertEquals(listOf("Oruga", "Trips"), estado.plagas.map { it.nombre })
    }

    @Test
    fun `limpiar cancela carga pendiente y no repuebla los reportes de la sesion anterior`() {
        val iniciado = CompletableDeferred<Unit>()
        val liberado = CompletableDeferred<Unit>()
        gddService.getReportesPaginaResult = {
            iniciado.complete(Unit)
            withContext(NonCancellable) { liberado.await() }
            Response.success(PaginaReportes(listOf(propio(444)), 1))
        }
        viewModel.cargarReportes()
        runBlocking { withTimeout(3000) { iniciado.await() } }
        viewModel.limpiar()
        liberado.complete(Unit)
        assertTrue(viewModel.state.value.reportes.isEmpty())
        assertNull(viewModel.state.value.error)
    }
}
