package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakePlantacionDao
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
class PlantacionesViewModelTest {

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

    private fun viewModelCon(cache: List<PlantacionesResponse>): Pair<PlantacionesViewModel, FakePlantacionDao> {
        val dao = FakePlantacionDao(inicial = cache)
        return PlantacionesViewModel(context, PlantacionRepository(dao), gddService) to dao
    }

    @Test
    fun `trae todas las paginas y reemplaza el cache con el total`() {
        val (vm, dao) = viewModelCon(cache = listOf(Fixtures.plantacion(id = 999)))
        gddService.getPlantacionesResult = { Response.success((1..45).map { Fixtures.plantacion(id = it) }) }

        vm.getPlantaciones(forzar = true)

        val estado = esperarEstado(vm.state) { !it.isRefreshing && !it.cargandoMas && it.plantaciones.size == 45 }
        assertEquals(listOf(20 to 0, 100 to 20), gddService.paginasPlantaciones)
        assertEquals((1..45).toList(), estado.plantaciones.map { it.plantacion_id })
        assertEquals((1..45).toList(), runBlocking { dao.getAll() }.map { it.plantacion_id }.sorted())
        assertTrue(CacheTracker.yaConsultado(context, CacheTracker.PLANTACIONES))
    }

    @Test
    fun `si falla una pagina intermedia no pierde el cache offline`() {
        val (vm, dao) = viewModelCon(cache = listOf(Fixtures.plantacion(id = 1), Fixtures.plantacion(id = 50)))
        gddService.getPlantacionesPaginaResult = { limit, offset ->
            if (offset > 0) FakeGDDService.sinConexion()
            Response.success(Pagina((1..limit).map { Fixtures.plantacion(id = it) }, 40))
        }

        vm.getPlantaciones()

        val estado = esperarEstado(vm.state) { !it.isLoading && !it.cargandoMas && it.plantaciones.size == 21 }
        // Las 20 recibidas más la que solo estaba en caché
        assertEquals((1..20).toList() + 50, estado.plantaciones.map { it.plantacion_id })
        assertEquals((1..20).toList() + 50, runBlocking { dao.getAll() }.map { it.plantacion_id }.sorted())
        assertFalse(CacheTracker.yaConsultado(context, CacheTracker.PLANTACIONES))
    }
}
