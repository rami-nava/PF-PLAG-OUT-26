package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MisReportesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: MisReportesViewModel

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = MisReportesViewModel(gddService)
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
        gddService.getReportesResult = { Response.success(emptyList()) }

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
    fun `guardarRadioNotificacion recarga los reportes con el radio nuevo`() {
        gddService.actualizarUsuarioResult = { Response.success(Fixtures.usuario().copy(radio_notificacion_km = 50.0)) }
        gddService.getReportesResult = {
            Response.success(listOf(Fixtures.reporteDetalle(id = 1), Fixtures.reporteDetalle(id = 2)))
        }

        viewModel.abrirConfiguracionRadio()
        viewModel.onRadioTemporalChange(50f)
        viewModel.guardarRadioNotificacion()

        val estado = esperarEstado(viewModel.state) { it.reportes.size == 2 && !it.isLoading && !it.isRefreshing }
        assertEquals(1, gddService.vecesLlamado("getReportes"))
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
    fun `guardarRadioNotificacion con error no recarga los reportes`() {
        gddService.actualizarUsuarioResult = { FakeGDDService.errorServidor(500) }

        viewModel.abrirConfiguracionRadio()
        viewModel.onRadioTemporalChange(80f)
        viewModel.guardarRadioNotificacion()

        esperarEstado(viewModel.state) { !it.guardandoRadio && it.error != null }
        assertEquals(0, gddService.vecesLlamado("getReportes"))
    }
}
