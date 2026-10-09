package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.BiofixPendiente
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.fakes.FakeBiofixDao
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeMonitoreoDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Forma "offline-first"
 * Monitoreos usa `CacheTracker.consultadoHoy` (se refresca
 * cada día porque el backend recalcula los GDD a las 00:00 hora argentina)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MonitoreosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var context: Context
    private lateinit var gddService: FakeGDDService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CacheTracker.limpiarTodo(context)
        gddService = FakeGDDService()
    }

    private fun viewModelCon(cache: List<MonitoreoResponse>): MonitoreosViewModel {
        val dao = FakeMonitoreoDao(inicial = cache)
        return MonitoreosViewModel(context, MonitoreoRepository(dao), gddService)
    }

    @Test
    fun `primera carga trae de la red y marca consultado hoy`() {
        val vm = viewModelCon(cache = emptyList())
        gddService.getMonitoreosResult = { Response.success(listOf(Fixtures.monitoreo(id = 1))) }

        vm.getMonitoreos()

        esperarEstado(vm.state) { it.monitoreos.size == 1 && !it.isLoading }
        assertEquals(1, gddService.vecesLlamado("getMonitoreos"))
        assertTrue(CacheTracker.consultadoTrasUltimaActualizacion(context, CacheTracker.MONITOREOS))
    }

    @Test
    fun `si ya se consulto hoy no vuelve a la red`() {
        CacheTracker.marcarConsultado(context, CacheTracker.MONITOREOS)
        val vm = viewModelCon(cache = listOf(Fixtures.monitoreo(id = 3)))

        vm.getMonitoreos(forzar = false)

        val estado = esperarEstado(vm.state) { !it.isLoading }
        assertEquals(listOf(3), estado.monitoreos.map { it.monitoreo_id })
        assertTrue("Ya consultado hoy: no debe ir a la red", gddService.llamadas.isEmpty())
    }

    @Test
    fun `Monitoreos se refresca cuando yaConsultado es falso`() {
        val vm = viewModelCon(cache = listOf(Fixtures.monitoreo(id = 3)))
        assertTrue(CacheTracker.yaConsultado(context, CacheTracker.MONITOREOS).not())
        gddService.getMonitoreosResult = { Response.success(listOf(Fixtures.monitoreo(id = 9))) }

        vm.getMonitoreos(forzar = false)

        val estado = esperarEstado(vm.state) { it.monitoreos.map { m -> m.monitoreo_id } == listOf(9) }
        assertEquals(1, gddService.vecesLlamado("getMonitoreos"))
        assertEquals(listOf(9), estado.monitoreos.map { it.monitoreo_id })
    }

    @Test
    fun `sin conexion conserva el cache`() {
        val vm = viewModelCon(cache = listOf(Fixtures.monitoreo(id = 5)))
        gddService.getMonitoreosResult = { FakeGDDService.sinConexion() }

        vm.getMonitoreos()

        val estado = esperarEstado(vm.state) { !it.isLoading }
        assertEquals(listOf(5), estado.monitoreos.map { it.monitoreo_id })
    }

    // ---------- limpieza de biofix en cola ----------

    private fun pendiente(monitoreoId: Int) = BiofixPendiente("usuario-1", monitoreoId, "{}")

    private fun conBiofixEnCola(): Triple<MonitoreosViewModel, FakeMonitoreoDao, FakeBiofixDao> {
        val dao = FakeMonitoreoDao(inicial = listOf(
            Fixtures.monitoreo(id = 1, plantacionId = 10, terrenoId = 100),
            Fixtures.monitoreo(id = 2, plantacionId = 10, terrenoId = 100),
            Fixtures.monitoreo(id = 3, plantacionId = 20, terrenoId = 100),
            Fixtures.monitoreo(id = 4, plantacionId = 30, terrenoId = 200)
        ))
        val biofix = FakeBiofixDao(inicial = (1..4).map { pendiente(it) })
        val vm = MonitoreosViewModel(context, MonitoreoRepository(dao), gddService, biofix)
        return Triple(vm, dao, biofix)
    }

    @Test
    fun `purgarMonitoreo descarta solo el biofix en cola de ese monitoreo`() {
        val (vm, _, biofix) = conBiofixEnCola()

        vm.purgarMonitoreo(1)

        esperarCondicion { biofix.filas.size == 3 }
        assertEquals(listOf(2, 3, 4), biofix.filas.map { it.monitoreo_id })
    }

    @Test
    fun `purgarPorPlantacion descarta el biofix en cola de sus monitoreos`() {
        val (vm, dao, biofix) = conBiofixEnCola()

        vm.purgarPorPlantacion(10)

        esperarCondicion { biofix.filas.size == 2 }
        assertEquals(listOf(3, 4), biofix.filas.map { it.monitoreo_id })
        assertEquals(listOf(3, 4), runBlocking { dao.getAll() }.map { it.monitoreo_id })
    }

    @Test
    fun `purgarPorTerreno descarta el biofix en cola de todos sus monitoreos`() {
        val (vm, _, biofix) = conBiofixEnCola()

        vm.purgarPorTerreno(100)

        esperarCondicion { biofix.filas.size == 1 }
        assertEquals(listOf(4), biofix.filas.map { it.monitoreo_id })
    }

    private fun esperarCondicion(condicion: () -> Boolean) {
        val limite = System.currentTimeMillis() + 2_000
        while (!condicion() && System.currentTimeMillis() < limite) Thread.sleep(10)
        assertTrue(condicion())
    }
    @Test
    fun `cierre confirmado archiva ciclos activos en memoria y cache conservando otros estados`() {
        val first = Fixtures.monitoreo(id = 1, plantacionId = 11, ciclos = listOf(
            Fixtures.ciclo(id = 71, estado = "activo"),
            Fixtures.ciclo(id = 72, estado = "completado"),
            Fixtures.ciclo(id = 73, estado = "archivado")
        ))
        val inactive = Fixtures.monitoreo(id = 2, plantacionId = 11, activo = false,
            ciclos = listOf(Fixtures.ciclo(id = 74, estado = "activo")))
        val other = Fixtures.monitoreo(id = 3, plantacionId = 22,
            ciclos = listOf(Fixtures.ciclo(id = 75, estado = "activo")))
        val dao = FakeMonitoreoDao(listOf(first, inactive, other))
        val vm = MonitoreosViewModel(context, MonitoreoRepository(dao), gddService)
        vm.agregarEnMemoria(listOf(first, inactive, other))
        vm.finalizarPorPlantacion(11)
        val state = esperarEstado(vm.state) { it.monitoreos.first().ciclos?.first()?.estado == "archivado" }
        assertEquals(listOf("archivado", "completado", "archivado"), state.monitoreos.first().ciclos?.map { it.estado })
        assertEquals(false, state.monitoreos.first().activo)
        assertEquals("archivado", state.monitoreos[1].ciclos?.first()?.estado)
        assertEquals(other, state.monitoreos[2])
        val persisted = kotlinx.coroutines.runBlocking { dao.getAll() }.associateBy { it.monitoreo_id }
        assertEquals(state.monitoreos.first(), persisted[1])
        assertEquals(state.monitoreos[1], persisted[2])
        assertEquals(other, persisted[3])
        assertTrue(gddService.llamadas.isEmpty())
    }

}
