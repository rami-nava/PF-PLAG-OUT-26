package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeMonitoreoDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        assertTrue(CacheTracker.consultadoHoy(context, CacheTracker.MONITOREOS))
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
}
