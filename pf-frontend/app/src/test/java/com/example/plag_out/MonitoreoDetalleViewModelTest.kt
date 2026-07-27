package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeMonitoreoDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
class MonitoreoDetalleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var context: Context
    private lateinit var gddService: FakeGDDService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        gddService = FakeGDDService()
    }

    private fun viewModelCon(cache: List<MonitoreoResponse> = emptyList()): Pair<MonitoreoDetalleViewModel, FakeMonitoreoDao> {
        val dao = FakeMonitoreoDao(inicial = cache)
        val vm = MonitoreoDetalleViewModel(context, MonitoreoRepository(dao), gddService)
        return vm to dao
    }

    // ---------- cargar ----------

    @Test
    fun `cargar muestra primero el cache y despues el dato de red`() {
        val cache = Fixtures.monitoreo(id = 1, progreso = 20f)
        val (vm, _) = viewModelCon(cache = listOf(cache))
        val deRed = Fixtures.monitoreo(id = 1, progreso = 55f)
        gddService.getMonitoreoResult = { Response.success(deRed) }

        vm.cargar(1)

        esperarEstado(vm.state) { it.monitoreo?.progreso == 55f }
        assertFalse(vm.state.value.datosDesactualizados)
    }

    @Test
    fun `cargar sin red se queda con el cache y marca datos desactualizados`() {
        val cache = Fixtures.monitoreo(id = 1, progreso = 20f)
        val (vm, _) = viewModelCon(cache = listOf(cache))
        gddService.getMonitoreoResult = { FakeGDDService.sinConexion() }

        vm.cargar(1)

        // El caché ya deja isLoading=false antes de que la llamada de red termine de fallar:
        // hay que esperar la señal que realmente nos interesa, no isLoading, para no leer el
        // estado intermedio.
        val estado = esperarEstado(vm.state) { it.datosDesactualizados }
        assertEquals(20f, estado.monitoreo?.progreso)
    }

    // ---------- guardarUmbral ----------

    @Test
    fun `guardarUmbral no dispara PATCH si el valor no cambio`() {
        val monitoreo = Fixtures.monitoreo(id = 1, umbralRiesgo = 80)
        val (vm, _) = viewModelCon(cache = listOf(monitoreo))
        vm.cargar(1)
        esperarEstado(vm.state) { it.monitoreo != null }

        vm.abrirEditorUmbral()
        vm.actualizarUmbralEditado(80)

        var llamado = false
        vm.guardarUmbral { llamado = true }

        assertTrue(llamado)
        assertEquals(0, gddService.vecesLlamado("actualizarMonitoreo"))
    }

    @Test
    fun `guardarUmbral con exito actualiza el state y persiste en Room`() {
        val monitoreo = Fixtures.monitoreo(id = 1, umbralRiesgo = 80)
        val (vm, dao) = viewModelCon(cache = listOf(monitoreo))
        vm.cargar(1)
        esperarEstado(vm.state) { it.monitoreo != null }

        val actualizado = monitoreo.copy(umbral_riesgo = 60)
        gddService.actualizarMonitoreoResult = { Response.success(actualizado) }

        vm.abrirEditorUmbral()
        vm.actualizarUmbralEditado(60)
        vm.guardarUmbral {}

        esperarEstado(vm.state) { !it.guardandoUmbral }
        assertEquals(60, vm.state.value.monitoreo?.umbral_riesgo)
        val persistido = runBlocking { dao.getAll() }.find { it.monitoreo_id == 1 }
        assertEquals(60, persistido?.umbral_riesgo)
    }

    @Test
    fun `guardarUmbral con 401 informa que la sesion expiro`() {
        val monitoreo = Fixtures.monitoreo(id = 1, umbralRiesgo = 80)
        val (vm, _) = viewModelCon(cache = listOf(monitoreo))
        vm.cargar(1)
        esperarEstado(vm.state) { it.monitoreo != null }

        gddService.actualizarMonitoreoResult = { FakeGDDService.errorServidor(401) }

        vm.abrirEditorUmbral()
        vm.actualizarUmbralEditado(60)
        vm.guardarUmbral {}

        esperarEstado(vm.state) { it.error != null }
        assertEquals("Tu sesión expiró. Volvé a iniciar sesión.", vm.state.value.error)
    }

    // ---------- finalizarMonitoreo ----------

    @Test
    fun `finalizarMonitoreo con exito invoca el callback y actualiza Room`() {
        val monitoreo = Fixtures.monitoreo(id = 1, activo = true)
        val (vm, dao) = viewModelCon(cache = listOf(monitoreo))
        vm.cargar(1)
        esperarEstado(vm.state) { it.monitoreo != null }

        val actualizado = monitoreo.copy(activo = false)
        gddService.actualizarMonitoreoResult = { Response.success(actualizado) }

        var llamado = false
        vm.finalizarMonitoreo { llamado = true }

        esperarEstado(vm.state) { it.finalizado }
        assertTrue(llamado)
        val persistido = runBlocking { dao.getAll() }.find { it.monitoreo_id == 1 }
        assertFalse(persistido!!.activo)
    }
}
