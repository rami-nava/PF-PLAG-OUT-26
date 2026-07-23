package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeTerrenoDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests del comportamiento "offline-first" de [TerrenosViewModel]:
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TerrenosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var context: Context
    private lateinit var gddService: FakeGDDService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CacheTracker.limpiarTodo(context) // prefs limpias entre tests
        gddService = FakeGDDService()
    }

    private fun viewModelCon(cache: List<TerrenoResponse>): Pair<TerrenosViewModel, FakeTerrenoDao> {
        val dao = FakeTerrenoDao(inicial = cache)
        val vm = TerrenosViewModel(context, TerrenoRepository(dao), gddService)
        return vm to dao
    }

    @Test
    fun `primera carga trae de la red y actualiza el cache`() {
        val (vm, dao) = viewModelCon(cache = emptyList())
        gddService.getTerrenosResult = { Response.success(listOf(Fixtures.terreno(id = 1), Fixtures.terreno(id = 2))) }

        vm.getTerrenos()

        val estado = esperarEstado(vm.state) { it.terrenos.size == 2 && !it.isLoading }
        assertEquals(listOf(1, 2), estado.terrenos.map { it.terreno_id })
        assertEquals(1, gddService.vecesLlamado("getTerrenos"))
        // El caché se reemplazó (borrar + insertar) y se marcó como consultado.
        assertTrue(dao.operaciones.containsAll(listOf("deleteAll", "insertAll")))
        assertTrue(CacheTracker.yaConsultado(context, CacheTracker.TERRENOS))
    }

    @Test
    fun `si ya se consulto y no se fuerza no hay llamada de red`() {
        CacheTracker.marcarConsultado(context, CacheTracker.TERRENOS)
        val (vm, _) = viewModelCon(cache = listOf(Fixtures.terreno(id = 7)))

        vm.getTerrenos(forzar = false)

        val estado = esperarEstado(vm.state) { !it.isLoading }
        assertEquals(listOf(7), estado.terrenos.map { it.terreno_id })
        assertTrue("El caché vigente no debe ir a la red", gddService.llamadas.isEmpty())
    }

    @Test
    fun `forzar consulta la red aunque ya se haya consultado`() {
        CacheTracker.marcarConsultado(context, CacheTracker.TERRENOS)
        val (vm, _) = viewModelCon(cache = listOf(Fixtures.terreno(id = 7)))
        gddService.getTerrenosResult = { Response.success(listOf(Fixtures.terreno(id = 8))) }

        vm.refrescar() // forzar = true

        val estado = esperarEstado(vm.state) { it.terrenos.map { t -> t.terreno_id } == listOf(8) }
        assertEquals(1, gddService.vecesLlamado("getTerrenos"))
        assertFalse(estado.isRefreshing)
        assertFalse(estado.isLoading)
    }

    @Test
    fun `sin conexion conserva el cache ya mostrado`() {
        val (vm, _) = viewModelCon(cache = listOf(Fixtures.terreno(id = 5)))
        gddService.getTerrenosResult = { FakeGDDService.sinConexion() }

        vm.getTerrenos()

        val estado = esperarEstado(vm.state) { !it.isLoading }
        assertEquals(listOf(5), estado.terrenos.map { it.terreno_id })
        assertFalse(estado.isRefreshing)
    }

    @Test
    fun `error HTTP no corrompe el estado y conserva el cache`() {
        val (vm, _) = viewModelCon(cache = listOf(Fixtures.terreno(id = 5)))
        gddService.getTerrenosResult = { FakeGDDService.errorServidor(500) }

        vm.getTerrenos()

        val estado = esperarEstado(vm.state) { !it.isLoading }
        assertEquals(listOf(5), estado.terrenos.map { it.terreno_id })
        assertFalse(estado.isRefreshing)
        // Un error del servidor NO debe marcar el recurso como consultado.
        assertFalse(CacheTracker.yaConsultado(context, CacheTracker.TERRENOS))
    }
}
