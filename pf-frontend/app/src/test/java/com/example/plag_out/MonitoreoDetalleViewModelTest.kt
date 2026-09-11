package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.CacheTracker
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeMonitoreoDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.Service.RetrofitClient
import com.example.plag_out.util.esperarEstado
import com.google.gson.JsonObject
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
import okhttp3.RequestBody
import okio.Buffer
import retrofit2.Converter
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

    @Test
    fun `guardar threshold ML respeta el minimo recomendado y envia entero`() {
        val monitoreo = Fixtures.monitoreo(
            umbralAlertaMlRecomendado = 24.67f,
            umbralAlertaMlEfectivo = 24.67f,
            modeloAlertaMlId = "modelo-1"
        )
        val (vm, _) = viewModelCon(listOf(monitoreo))
        vm.cargar(monitoreo.monitoreo_id)
        esperarEstado(vm.state) { it.monitoreo != null }
        gddService.actualizarUmbralAlertaMlResult = {
            Response.success(monitoreo.copy(umbral_alerta_ml = 25f, umbral_alerta_ml_efectivo = 25f))
        }

        vm.abrirEditorUmbralMl()
        assertEquals(25, vm.state.value.umbralMlEditado)
        vm.guardarUmbralMl {}

        esperarEstado(vm.state) { !it.guardandoUmbralMl }
        assertEquals(25, gddService.ultimoUmbralAlertaMl?.get("umbral_alerta_ml")?.asInt)
    }

    @Test
    fun `usar recomendado envia null explicito`() {
        val monitoreo = Fixtures.monitoreo(
            umbralAlertaMl = 40f,
            umbralAlertaMlRecomendado = 24.67f,
            umbralAlertaMlEfectivo = 40f,
            modeloAlertaMlId = "modelo-1"
        )
        val (vm, _) = viewModelCon(listOf(monitoreo))
        vm.cargar(monitoreo.monitoreo_id)
        esperarEstado(vm.state) { it.monitoreo != null }
        gddService.actualizarUmbralAlertaMlResult = {
            Response.success(monitoreo.copy(umbral_alerta_ml = null, umbral_alerta_ml_efectivo = 24.67f))
        }

        vm.usarUmbralMlRecomendado {}

        esperarEstado(vm.state) { !it.guardandoUmbralMl }
        assertTrue(gddService.ultimoUmbralAlertaMl?.get("umbral_alerta_ml")?.isJsonNull == true)
    }

    /**
     * Regresión: assertear sobre el [com.google.gson.JsonObject] no alcanza, porque el null se
     * perdía recién al serializar (Gson descarta las propiedades null salvo `serializeNulls`).
     * Este test toma el body que arma el ViewModel y lo pasa por el converter real de Retrofit,
     * el mismo que usa RetrofitClient, para verificar lo que sale al cable.
     */
    @Test
    fun `usar recomendado serializa null explicito en el body HTTP`() {
        val monitoreo = Fixtures.monitoreo(
            umbralAlertaMl = 40f,
            umbralAlertaMlRecomendado = 24.67f,
            umbralAlertaMlEfectivo = 40f,
            modeloAlertaMlId = "modelo-1"
        )
        val (vm, _) = viewModelCon(listOf(monitoreo))
        vm.cargar(monitoreo.monitoreo_id)
        esperarEstado(vm.state) { it.monitoreo != null }
        gddService.actualizarUmbralAlertaMlResult = {
            Response.success(monitoreo.copy(umbral_alerta_ml = null, umbral_alerta_ml_efectivo = 24.67f))
        }

        vm.usarUmbralMlRecomendado {}
        esperarEstado(vm.state) { !it.guardandoUmbralMl }

        val body = requireNotNull(gddService.ultimoUmbralAlertaMl)
        assertEquals("{\"umbral_alerta_ml\":null}", serializarComoRetrofit(body))
    }

    /**
     * Regresión del caché rancio: volver a entrar a la pantalla (mismo ViewModel, nuevo
     * LaunchedEffect) tiene que volver a pedir el monitoreo al backend. Antes el early-return
     * dejaba congelado el override ML hasta reiniciar el proceso.
     */
    @Test
    fun `volver a cargar el mismo monitoreo refresca contra el backend`() {
        val conOverride = Fixtures.monitoreo(
            id = 41,
            umbralAlertaMl = 50f,
            umbralAlertaMlRecomendado = 24.67f,
            umbralAlertaMlEfectivo = 50f,
            modeloAlertaMlId = "modelo-1"
        )
        val (vm, dao) = viewModelCon(cache = listOf(conOverride))
        gddService.getMonitoreoResult = { Response.success(conOverride) }
        vm.cargar(41)
        esperarEstado(vm.state) { it.monitoreo != null }

        // El override se limpió del lado del servidor (por ejemplo, desde otro dispositivo).
        val sinOverride = conOverride.copy(umbral_alerta_ml = null, umbral_alerta_ml_efectivo = 24.67f)
        gddService.getMonitoreoResult = { Response.success(sinOverride) }

        vm.cargar(41)

        esperarEstado(vm.state) { it.monitoreo?.umbral_alerta_ml == null }
        assertEquals(24.67f, vm.state.value.monitoreo?.umbral_alerta_ml_efectivo)
        val persistido = runBlocking { dao.getAll() }.find { it.monitoreo_id == 41 }
        assertEquals(null, persistido?.umbral_alerta_ml)
    }

    /**
     * Regresión del caché rancio en el listado: el listado se sirve del caché hasta el corte
     * diario de GDD, así que un cambio de threshold tiene que invalidar esa marca.
     */
    @Test
    fun `guardar el threshold ML invalida el cache del listado`() {
        val monitoreo = Fixtures.monitoreo(
            umbralAlertaMl = null,
            umbralAlertaMlRecomendado = 24.67f,
            umbralAlertaMlEfectivo = 24.67f,
            modeloAlertaMlId = "modelo-1"
        )
        val (vm, _) = viewModelCon(listOf(monitoreo))
        vm.cargar(monitoreo.monitoreo_id)
        esperarEstado(vm.state) { it.monitoreo != null }
        CacheTracker.marcarConsultado(context, CacheTracker.MONITOREOS)
        gddService.actualizarUmbralAlertaMlResult = {
            Response.success(monitoreo.copy(umbral_alerta_ml = 50f, umbral_alerta_ml_efectivo = 50f))
        }

        vm.abrirEditorUmbralMl()
        vm.actualizarUmbralMlEditado(50)
        vm.guardarUmbralMl {}

        esperarEstado(vm.state) { !it.guardandoUmbralMl }
        assertFalse(CacheTracker.yaConsultado(context, CacheTracker.MONITOREOS))
    }

    /** Usa el converter real de [RetrofitClient], el mismo que arma el body del PATCH. */
    private fun serializarComoRetrofit(body: JsonObject): String {
        val converter: Converter<JsonObject, RequestBody> =
            RetrofitClient.retrofit.requestBodyConverter(
                JsonObject::class.java,
                emptyArray(),
                emptyArray()
            )
        val buffer = Buffer()
        converter.convert(body)!!.writeTo(buffer)
        return buffer.readUtf8()
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
