package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrearReporteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var context: Context
    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: CrearReporteViewModel
    private var owner: String? = "fixture-owner"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        gddService = FakeGDDService()
        gddService.getTerrenosResult = { Response.success(listOf(Fixtures.terreno(id = 1, nombre = "Lote 1"))) }
        gddService.getPlantacionesResult = { Response.success(listOf(Fixtures.plantacion(id = 10, terrenoId = 1))) }
        gddService.getPlagasResult = { Response.success(listOf(Fixtures.plaga(id = 5, nombre = "Oruga"))) }
        viewModel = CrearReporteViewModel(context, gddService, currentOwner = { owner })
    }

    @Test
    fun `cargarDatosIniciales llena la lista de terrenos y plagas`() {
        val estado = esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }
        assertEquals(1, estado.terrenos.size)
        assertEquals(1, estado.plagas.size)
        assertNull(estado.error)
    }

    @Test
    fun `guardarReporte valida campos requeridos`() {
        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }

        var exitoLlamado = false
        viewModel.guardarReporte { exitoLlamado = true }

        val estado = viewModel.state.value
        assertEquals("Debes seleccionar un terreno", estado.error)
        assertTrue(!exitoLlamado)
    }

    @Test
    fun `guardarReporte con datos validos realiza POST exitoso`() {
        val t = Fixtures.terreno(id = 1)
        val p = Fixtures.plantacion(id = 10, terrenoId = 1)
        val plaga = Fixtures.plaga(id = 5, nombre = "Oruga")

        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }
        viewModel.seleccionarTerreno(t)
        viewModel.seleccionarPlantacion(p)
        viewModel.seleccionarPlaga(plaga)

        gddService.createReporteResult = {
            Response.success(
                ReporteResponse(
                    id = 99,
                    plantacion_id = 10,
                    plaga_id = 5,
                    nivel_severidad = "Alto",
                    latitud = -34.6,
                    longitud = -58.4,
                    timestamp_ms = 1700000000000L
                )
            )
        }

        var exitoLlamado = false
        viewModel.guardarReporte { exitoLlamado = true }

        val estado = esperarEstado(viewModel.state) { !it.isGuardando && it.reporteNavPayload != null && exitoLlamado }
        assertTrue(exitoLlamado)
        assertNotNull(estado.reporteNavPayload)
        assertEquals(99, estado.reporteNavPayload?.id)
        assertEquals("Oruga", estado.reporteNavPayload?.plaga_nombre)
        assertEquals("Alto", estado.reporteNavPayload?.nivel_severidad)
        assertEquals(1700000000000L, estado.reporteNavPayload?.timestamp_ms)
        assertEquals(-34.6, estado.reporteNavPayload?.latitud)
        assertEquals(1, gddService.vecesLlamado("createReporte"))
        val jsonEnviado = com.google.gson.Gson().toJson(gddService.ultimoCreateReporte)
        assertFalse(jsonEnviado.contains("terreno_id"))
        assertFalse(jsonEnviado.contains("latitud"))
        assertFalse(jsonEnviado.contains("longitud"))
    }

    private fun prepararReporte() {
        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }
        viewModel.seleccionarTerreno(Fixtures.terreno(id = 1))
        viewModel.seleccionarPlaga(Fixtures.plaga(id = 5, nombre = "Oruga"))
    }

    private fun reporteGuardado() = ReporteResponse(
        id = 99, plantacion_id = 10, plaga_id = 5, nivel_severidad = "Bajo",
        latitud = -34.6, longitud = -58.4, timestamp_ms = 1700000000000L,
    )

    @Test
    fun `respuesta perdida conserva fecha y reintento acepta reporte reutilizado`() {
        prepararReporte()
        val original = viewModel.state.value.timestampMs
        gddService.createReporteResult = { throw IOException("respuesta perdida tras guardar") }
        viewModel.guardarReporte {}
        esperarEstado(viewModel.state) { !it.isGuardando && it.error != null }
        val primerEnvio = gddService.ultimoCreateReporte
        viewModel.actualizarTimestamp()
        assertEquals(original, viewModel.state.value.timestampMs)
        gddService.createReporteResult = { Response.success(200, reporteGuardado()) }
        viewModel.guardarReporte {}
        val estado = esperarEstado(viewModel.state) { it.reporteNavPayload != null }
        assertEquals(primerEnvio, gddService.ultimoCreateReporte)
        assertEquals(original, gddService.ultimoCreateReporte?.timestamp_ms)
        assertEquals(1700000000000L, estado.reporteNavPayload?.timestamp_ms)
        assertEquals(99, estado.reporteNavPayload?.id)
        assertEquals(2, gddService.vecesLlamado("createReporte"))
    }

    @Test
    fun `doble toque no envia dos reportes`() {
        prepararReporte()
        val iniciado = CountDownLatch(1)
        val liberar = CountDownLatch(1)
        gddService.createReporteResult = {
            iniciado.countDown()
            check(liberar.await(5, TimeUnit.SECONDS))
            Response.success(201, reporteGuardado())
        }
        try {
            viewModel.guardarReporte {}
            assertTrue(iniciado.await(5, TimeUnit.SECONDS))
            viewModel.guardarReporte {}
            assertEquals(1, gddService.vecesLlamado("createReporte"))
        } finally { liberar.countDown() }
        esperarEstado(viewModel.state) { it.reporteNavPayload != null }
        viewModel.guardarReporte {}
        assertEquals(1, gddService.vecesLlamado("createReporte"))
    }

    @Test
    fun `cambio o cierre de sesion impide reenviar o abrir el reporte pendiente`() {
        prepararReporte()
        gddService.createReporteResult = {
            owner = "otra-cuenta"
            Response.success(reporteGuardado())
        }
        viewModel.guardarReporte {}
        esperarEstado(viewModel.state) { !it.isGuardando && it.error != null }
        viewModel.guardarReporte {}
        assertEquals(1, gddService.vecesLlamado("createReporte"))
        assertTrue(viewModel.state.value.error.orEmpty().contains("sesión cambió"))
        assertNull(viewModel.state.value.reporteNavPayload)
        owner = null
        viewModel.guardarReporte {}
        assertEquals(1, gddService.vecesLlamado("createReporte"))
    }

    @Test
    fun `acuse de otra plantacion no se presenta como guardado`() {
        prepararReporte()
        gddService.createReporteResult = { Response.success(reporteGuardado().copy(plantacion_id = 999)) }
        viewModel.guardarReporte {}
        val estado = esperarEstado(viewModel.state) { !it.isGuardando && it.error != null }
        assertNull(estado.reporteNavPayload)
    }

    @Test
    fun `seleccionarPlaga calcula las etapas biologicas correspondientes`() {
        val oruga = Fixtures.plaga(id = 1, nombre = "Oruga Cogollera", nombreCientifico = "Spodoptera frugiperda")
        val chicharrita = Fixtures.plaga(id = 2, nombre = "Chicharrita", nombreCientifico = "Dalbulus maidis")
        gddService.getPlagasResult = { Response.success(listOf(oruga, chicharrita)) }
        viewModel = CrearReporteViewModel(context, gddService, currentOwner = { "fixture-owner" })
        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }

        // Las plagas solo se pueden elegir con terreno y cultivo ya definidos
        viewModel.seleccionarTerreno(Fixtures.terreno(id = 1))
        viewModel.seleccionarPlantacion(Fixtures.plantacion(id = 10, terrenoId = 1))

        viewModel.seleccionarPlaga(oruga)
        assertEquals(listOf("HUEVO", "LARVA", "PUPA", "ADULTO"), viewModel.state.value.etapasDisponibles)
        assertEquals("HUEVO", viewModel.state.value.etapaBiologica)

        viewModel.seleccionarPlaga(chicharrita)
        assertEquals(listOf("HUEVO", "NINFA", "ADULTO"), viewModel.state.value.etapasDisponibles)
        assertEquals("HUEVO", viewModel.state.value.etapaBiologica)
    }

    @Test
    fun `filtrado de plagas por cultivo excluye plagas no aplicables`() {
        val plagaMaiz = Fixtures.plaga(id = 1, nombre = "Chicharrita del Maíz", cultivosAfectados = listOf(2))
        gddService.getPlagasResult = { Response.success(listOf(plagaMaiz)) }
        viewModel = CrearReporteViewModel(context, gddService, currentOwner = { "fixture-owner" })
        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }

        viewModel.seleccionarTerreno(Fixtures.terreno(id = 1))
        viewModel.seleccionarPlantacion(Fixtures.plantacion(id = 10, terrenoId = 1, cultivoId = 1, cultivo = "Trigo"))

        assertTrue(viewModel.state.value.plagasDisponibles.none { it.id == plagaMaiz.id })
    }

    @Test
    fun `sin plantacion no hay plagas para elegir`() {
        val estado = esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }

        // El catálogo llegó, pero nada está disponible hasta elegir un cultivo
        assertTrue(estado.plagas.isNotEmpty())
        assertTrue(estado.plagasDisponibles.isEmpty())
    }

    @Test
    fun `sin terreno no se puede elegir plantacion`() {
        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }

        viewModel.seleccionarPlantacion(Fixtures.plantacion(id = 10, terrenoId = 1))

        assertNull(viewModel.state.value.plantacionSeleccionada)
        assertTrue(viewModel.state.value.plagasDisponibles.isEmpty())
    }

    @Test
    fun `no se puede elegir una plaga de otro cultivo`() {
        val plagaMaiz = Fixtures.plaga(id = 7, nombre = "Gusano cogollero", cultivosAfectados = listOf(2))
        gddService.getPlagasResult = { Response.success(listOf(plagaMaiz)) }
        viewModel = CrearReporteViewModel(context, gddService, currentOwner = { "fixture-owner" })
        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }

        viewModel.seleccionarTerreno(Fixtures.terreno(id = 1))
        viewModel.seleccionarPlantacion(Fixtures.plantacion(id = 10, terrenoId = 1, cultivoId = 1, cultivo = "Trigo"))
        viewModel.seleccionarPlaga(plagaMaiz)

        assertNull(viewModel.state.value.plagaSeleccionada)
    }

    @Test
    fun `cambiar de terreno descarta plantacion y plaga`() {
        gddService.getTerrenosResult = {
            Response.success(listOf(Fixtures.terreno(id = 1, nombre = "Lote 1"), Fixtures.terreno(id = 2, nombre = "Lote 2")))
        }
        viewModel = CrearReporteViewModel(context, gddService, currentOwner = { "fixture-owner" })
        esperarEstado(viewModel.state) { !it.isLoadingInicial && it.terrenos.isNotEmpty() }

        viewModel.seleccionarTerreno(Fixtures.terreno(id = 1))
        viewModel.seleccionarPlantacion(Fixtures.plantacion(id = 10, terrenoId = 1))
        viewModel.seleccionarPlaga(Fixtures.plaga(id = 5, nombre = "Oruga"))
        assertNotNull(viewModel.state.value.plagaSeleccionada)

        viewModel.seleccionarTerreno(Fixtures.terreno(id = 2, nombre = "Lote 2"))

        val estado = viewModel.state.value
        assertNull(estado.plantacionSeleccionada)
        assertNull(estado.plagaSeleccionada)
        assertTrue(estado.plagasDisponibles.isEmpty())
        assertEquals("", estado.etapaBiologica)
    }
}
