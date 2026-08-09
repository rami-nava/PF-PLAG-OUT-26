package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakePlantacionDao
import com.example.plag_out.fakes.FakeTerrenoDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgregarPlantacionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var context: Context
    private lateinit var gddService: FakeGDDService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        gddService = FakeGDDService()
        // init { cargarCultivos() } dispara esta llamada apenas se construye el VM.
        gddService.getCultivosResult = { Response.success(listOf(Fixtures.cultivo())) }
    }

    private fun viewModel(
        plantacionDao: FakePlantacionDao = FakePlantacionDao(),
        terrenos: List<TerrenoResponse> = emptyList()
    ): AgregarPlantacionViewModel =
        AgregarPlantacionViewModel(
            context = context,
            plantacionRepository = PlantacionRepository(plantacionDao),
            terrenoRepository = TerrenoRepository(FakeTerrenoDao(inicial = terrenos)),
            gddService = gddService,
            ioDispatcher = UnconfinedTestDispatcher()
        )

    private fun millisUtc(fecha: LocalDate): Long =
        fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    // ---------- seleccionarFecha ----------

    @Test
    fun `seleccionarFecha con medianoche UTC guarda la misma fecha`() {
        val vm = viewModel()

        vm.seleccionarFecha(millisUtc(LocalDate.of(2026, 1, 1)))

        assertEquals("2026-01-01", vm.state.value.fechaSiembra)
    }

    @Test
    fun `seleccionarFecha usa la zona UTC, no la argentina`() {
        val vm = viewModel()

        // 2026-01-01 02:00 UTC == 2026-12-31 23:00 en Argentina (UTC-3).
        // El VM lo bucketea en UTC => "2026-01-01", pero CacheTracker usa hora argentina,
        // así que una fecha elegida cerca de medianoche puede quedar en un día distinto
        // al que espera el resto de la app. Se documenta el comportamiento actual.
        val millis = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()
            .plusSeconds(2 * 60 * 60).toEpochMilli()

        vm.seleccionarFecha(millis)

        assertEquals("2026-01-01", vm.state.value.fechaSiembra)
    }

    // ---------- guardarPlantacion: validaciones ----------

    @Test
    fun `sin cultivo el guardado falla`() {
        val vm = viewModel()

        vm.guardarPlantacion(terrenoId = 1, onSuccess = {})

        assertEquals("Debe seleccionar un cultivo", vm.state.value.error)
    }

    @Test
    fun `con cultivo pero sin fecha el guardado falla`() {
        val vm = viewModel()
        vm.actualizarCultivo(Fixtures.cultivo())

        vm.guardarPlantacion(terrenoId = 1, onSuccess = {})

        assertEquals("Debe seleccionar una fecha de siembra", vm.state.value.error)
    }

    // ---------- guardarPlantacion: fallback de nombre de terreno ----------

    @Test
    fun `si el terreno no esta en cache usa el nombre por defecto`() {
        val plantacionDao = FakePlantacionDao()
        // Sin terrenos en cache => debe usar "Terreno 42".
        val vm = viewModel(plantacionDao = plantacionDao, terrenos = emptyList())
        vm.actualizarCultivo(Fixtures.cultivo())
        vm.seleccionarFecha(millisUtc(LocalDate.of(2026, 1, 1)))
        gddService.createPlantacionResult = {
            Response.success(
                PlantacionCreateResponse(
                    id = 10,
                    terreno_id = 42,
                    cultivo_id = 1,
                    fecha_siembra = LocalDate.of(2026, 1, 1),
                    activa = true,
                    cultivo = Fixtures.cultivo()
                )
            )
        }

        var exito = false
        vm.guardarPlantacion(terrenoId = 42, onSuccess = { exito = true })

        esperarEstado(vm.state) { !it.isGuardando && exito }
        val guardada = kotlinx.coroutines.runBlocking { PlantacionRepository(plantacionDao).obtenerPlantaciones() }
        assertEquals("Terreno 42", guardada.single().terreno_nombre)
    }
}
