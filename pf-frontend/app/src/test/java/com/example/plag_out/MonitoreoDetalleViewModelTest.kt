package com.example.plag_out

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
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
    private val viewModels = mutableListOf<MonitoreoDetalleViewModel>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CacheTracker.limpiarTodo(context)
        gddService = FakeGDDService()
    }

    @After
    fun teardown() = runBlocking {
        withTimeout(5_000) {
            viewModels.forEach { it.viewModelScope.coroutineContext[Job]!!.cancelAndJoin() }
        }
        CacheTracker.limpiarTodo(context)
    }

    private fun viewModelCon(cache: List<MonitoreoResponse>): Pair<MonitoreoDetalleViewModel, FakeMonitoreoDao> {
        // La carga siempre consulta la red, incluso cuando hay caché.
        gddService.getMonitoreoResult = { Response.success(cache.single()) }
        val dao = FakeMonitoreoDao(inicial = cache)
        val vm = MonitoreoDetalleViewModel(context, MonitoreoRepository(dao), gddService)
        viewModels += vm
        return vm to dao
    }

    private fun cargarYEsperar(viewModel: MonitoreoDetalleViewModel, id: Int) {
        viewModel.cargar(id)
        // El estado del caché no significa que haya terminado el GET. Esperar la
        // corrutina completa impide que su resultado compita con los PATCH del test.
        runBlocking {
            withTimeout(5_000) {
                viewModel.viewModelScope.coroutineContext[Job]!!.children.toList().joinAll()
            }
        }
    }

    // ---------- cargar ----------

    @Test
    fun `cargar muestra primero el cache y despues el dato de red`() {
        val cache = Fixtures.monitoreo(id = 1, progreso = 20f)
        val (vm, _) = viewModelCon(cache = listOf(cache))
        val deRed = Fixtures.monitoreo(id = 1, progreso = 55f)
        gddService.getMonitoreoResult = { Response.success(deRed) }

        cargarYEsperar(vm, 1)

        esperarEstado(vm.state) { it.monitoreo?.progreso == 55f }
        assertFalse(vm.state.value.datosDesactualizados)
    }

    @Test
    fun `cargar sin red se queda con el cache y marca datos desactualizados`() {
        val cache = Fixtures.monitoreo(id = 1, progreso = 20f)
        val (vm, _) = viewModelCon(cache = listOf(cache))
        gddService.getMonitoreoResult = { FakeGDDService.sinConexion() }

        cargarYEsperar(vm, 1)

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
        cargarYEsperar(vm, 1)
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
        cargarYEsperar(vm, 1)
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
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }

        gddService.actualizarMonitoreoResult = { FakeGDDService.errorServidor(401) }

        vm.abrirEditorUmbral()
        vm.actualizarUmbralEditado(60)
        vm.guardarUmbral {}

        esperarEstado(vm.state) { it.error != null }
        assertEquals("Tu sesión expiró. Volvé a iniciar sesión.", vm.state.value.error)
    }

    @Test
    fun `desactivar alertas envia solo la preferencia y conserva GDD en cache`() {
        val monitoreo = Fixtures.monitoreo(modeloAlertaMlId = "modelo-1").copy(alertas_ml_activas = true)
        val (vm, dao) = viewModelCon(listOf(monitoreo))
        cargarYEsperar(vm, monitoreo.monitoreo_id)
        CacheTracker.marcarConsultado(context, CacheTracker.MONITOREOS)
        gddService.actualizarUmbralAlertaMlResult = { Response.success(monitoreo.copy(alertas_ml_activas = false)) }
        vm.cambiarAlertasMl(false)
        esperarEstado(vm.state) { !it.guardandoAlertasMl }
        assertEquals("{\"alertas_ml_activas\":false}", serializarComoRetrofit(requireNotNull(gddService.ultimoUmbralAlertaMl)))
        assertEquals(false, vm.state.value.monitoreo?.alertas_ml_activas)
        val persistido = runBlocking { dao.getAll().single() }
        assertEquals(false, persistido.alertas_ml_activas)
        assertEquals(monitoreo.gdd_acumulado, persistido.gdd_acumulado)
        assertEquals(monitoreo.activo, persistido.activo)
        assertFalse(CacheTracker.yaConsultado(context, CacheTracker.MONITOREOS))
    }

    @Test
    fun `respuesta perdida conserva preferencia anterior y permite reintentar`() {
        val monitoreo = Fixtures.monitoreo(modeloAlertaMlId = "modelo-1").copy(alertas_ml_activas = true)
        val (vm, dao) = viewModelCon(listOf(monitoreo))
        cargarYEsperar(vm, monitoreo.monitoreo_id)
        gddService.actualizarUmbralAlertaMlResult = { throw java.io.IOException("response lost") }
        vm.cambiarAlertasMl(false)
        esperarEstado(vm.state) { it.error != null }
        assertEquals(true, vm.state.value.monitoreo?.alertas_ml_activas)
        assertEquals(true, runBlocking { dao.getAll().single().alertas_ml_activas })
        gddService.actualizarUmbralAlertaMlResult = { Response.success(monitoreo.copy(alertas_ml_activas = false)) }
        vm.cambiarAlertasMl(false)
        esperarEstado(vm.state) { !it.guardandoAlertasMl && it.monitoreo?.alertas_ml_activas == false }
        assertEquals(null, vm.state.value.error)
    }

    @Test
    fun `doble toque envia un solo cambio y respeta la respuesta del servidor`() {
        val monitoreo = Fixtures.monitoreo(modeloAlertaMlId = "modelo-1").copy(alertas_ml_activas = true)
        val (vm, _) = viewModelCon(listOf(monitoreo))
        cargarYEsperar(vm, monitoreo.monitoreo_id)
        val started = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        gddService.actualizarUmbralAlertaMlResult = {
            started.countDown()
            check(release.await(5, java.util.concurrent.TimeUnit.SECONDS))
            Response.success(monitoreo.copy(alertas_ml_activas = false))
        }
        try {
            vm.cambiarAlertasMl(false)
            assertTrue(started.await(5, java.util.concurrent.TimeUnit.SECONDS))
            vm.cambiarAlertasMl(true)
            assertEquals(1, gddService.llamadas.count { it == "actualizarUmbralAlertaMl" })
        } finally { release.countDown() }
        esperarEstado(vm.state) { !it.guardandoAlertasMl }
        assertEquals(false, vm.state.value.monitoreo?.alertas_ml_activas)
    }

    @Test
    fun `sin preferencia del servidor no permite cambiar alertas`() {
        val monitoreo = Fixtures.monitoreo(modeloAlertaMlId = "modelo-1")
        val (vm, _) = viewModelCon(listOf(monitoreo))
        cargarYEsperar(vm, monitoreo.monitoreo_id)
        vm.cambiarAlertasMl(true)
        assertFalse(gddService.llamadas.contains("actualizarUmbralAlertaMl"))
    }

    @Test
    fun `respuesta vacia no inventa que se guardo la preferencia`() {
        val monitoreo = Fixtures.monitoreo(modeloAlertaMlId = "modelo-1").copy(alertas_ml_activas = true)
        val (vm, _) = viewModelCon(listOf(monitoreo))
        cargarYEsperar(vm, monitoreo.monitoreo_id)
        gddService.actualizarUmbralAlertaMlResult = { Response.success(null) }
        vm.cambiarAlertasMl(false)
        esperarEstado(vm.state) { it.error != null }
        assertEquals(true, vm.state.value.monitoreo?.alertas_ml_activas)
    }

    @Test
    fun `recargar actualiza la preferencia cambiada desde otro dispositivo`() {
        val monitoreo = Fixtures.monitoreo(modeloAlertaMlId = "modelo-1").copy(alertas_ml_activas = true)
        val (vm, dao) = viewModelCon(listOf(monitoreo))
        cargarYEsperar(vm, monitoreo.monitoreo_id)
        gddService.getMonitoreoResult = { Response.success(monitoreo.copy(alertas_ml_activas = false)) }
        cargarYEsperar(vm, monitoreo.monitoreo_id)
        assertEquals(false, vm.state.value.monitoreo?.alertas_ml_activas)
        assertEquals(false, runBlocking { dao.getAll().single().alertas_ml_activas })
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

    // ---------- guardarObservaciones ----------

    @Test
    fun `la nota se recorta al maximo de caracteres`() {
        val (vm, _) = viewModelCon(cache = listOf(Fixtures.monitoreo(id = 1)))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }

        vm.abrirEditorObservaciones()
        vm.actualizarObservacionesEditadas("a".repeat(MAX_CARACTERES_OBSERVACIONES + 120))

        assertEquals(MAX_CARACTERES_OBSERVACIONES, vm.state.value.observacionesEditadas?.length)
    }

    @Test
    fun `guardarObservaciones no dispara PATCH si el texto no cambio`() {
        val monitoreo = Fixtures.monitoreo(id = 1, observaciones = "No apareció la plaga")
        val (vm, _) = viewModelCon(cache = listOf(monitoreo))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }

        vm.abrirEditorObservaciones()
        vm.actualizarObservacionesEditadas("  No apareció la plaga  ")

        var llamado = false
        vm.guardarObservaciones { llamado = true }

        assertTrue(llamado)
        assertEquals(0, gddService.vecesLlamado("actualizarMonitoreo"))
    }

    @Test
    fun `guardarObservaciones persiste en Room e invalida el cache del listado`() {
        val monitoreo = Fixtures.monitoreo(id = 1)
        val (vm, dao) = viewModelCon(cache = listOf(monitoreo))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }
        CacheTracker.marcarConsultado(context, CacheTracker.MONITOREOS)

        val nota = "Apliqué cipermetrina el 12/2 y funcionó"
        gddService.actualizarMonitoreoResult = { Response.success(monitoreo.copy(observaciones = nota)) }

        vm.abrirEditorObservaciones()
        vm.actualizarObservacionesEditadas("$nota  ")
        vm.guardarObservaciones {}

        esperarEstado(vm.state) { !it.guardandoObservaciones && it.observacionesEditadas == null }
        assertEquals(nota, gddService.ultimoActualizarMonitoreo?.observaciones)
        assertEquals(nota, vm.state.value.monitoreo?.observaciones)
        val persistido = runBlocking { dao.getAll() }.find { it.monitoreo_id == 1 }
        assertEquals(nota, persistido?.observaciones)
        assertFalse(CacheTracker.yaConsultado(context, CacheTracker.MONITOREOS))
    }

    @Test
    fun `guardarObservaciones funciona con el monitoreo finalizado`() {
        val monitoreo = Fixtures.monitoreo(id = 1, activo = false, observaciones = "Sin novedades")
        val (vm, _) = viewModelCon(cache = listOf(monitoreo))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }

        val nota = "Rebrotó en marzo, el tratamiento no alcanzó"
        gddService.actualizarMonitoreoResult = { Response.success(monitoreo.copy(observaciones = nota)) }

        vm.abrirEditorObservaciones()
        vm.actualizarObservacionesEditadas(nota)
        vm.guardarObservaciones {}

        esperarEstado(vm.state) { !it.guardandoObservaciones }
        assertEquals(nota, vm.state.value.monitoreo?.observaciones)
    }

    @Test
    fun `guardarObservaciones con error mantiene el editor abierto`() {
        val monitoreo = Fixtures.monitoreo(id = 1)
        val (vm, _) = viewModelCon(cache = listOf(monitoreo))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }
        gddService.actualizarMonitoreoResult = { FakeGDDService.errorServidor(500) }

        vm.abrirEditorObservaciones()
        vm.actualizarObservacionesEditadas("Algo que no se quiere perder")
        vm.guardarObservaciones {}

        esperarEstado(vm.state) { it.error != null }
        assertEquals("Algo que no se quiere perder", vm.state.value.observacionesEditadas)
    }

    // ---------- finalizarMonitoreo ----------

    @Test
    fun `finalizarMonitoreo con exito invoca el callback y actualiza Room`() {
        val monitoreo = Fixtures.monitoreo(id = 1, activo = true)
        val (vm, dao) = viewModelCon(cache = listOf(monitoreo))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }

        val actualizado = monitoreo.copy(activo = false)
        gddService.actualizarMonitoreoResult = { Response.success(actualizado) }

        var llamado = false
        vm.finalizarMonitoreo { llamado = true }

        esperarEstado(vm.state) { it.finalizado && llamado }
        assertTrue(llamado)
        val persistido = runBlocking { dao.getAll() }.find { it.monitoreo_id == 1 }
        assertFalse(persistido!!.activo)
    }

    @Test
    fun `finalizarMonitoreo manda la nota en el mismo PATCH`() {
        val monitoreo = Fixtures.monitoreo(id = 1, activo = true)
        val (vm, dao) = viewModelCon(cache = listOf(monitoreo))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }

        val nota = "La plaga no apareció en toda la campaña"
        gddService.actualizarMonitoreoResult = {
            Response.success(monitoreo.copy(activo = false, observaciones = nota))
        }

        vm.finalizarMonitoreo(observaciones = "$nota ") {}

        esperarEstado(vm.state) { it.finalizado }
        assertEquals(false, gddService.ultimoActualizarMonitoreo?.activo)
        assertEquals(nota, gddService.ultimoActualizarMonitoreo?.observaciones)
        val persistido = runBlocking { dao.getAll() }.find { it.monitoreo_id == 1 }
        assertEquals(nota, persistido?.observaciones)
    }

    @Test
    fun `finalizarMonitoreo sin tocar la nota no la manda`() {
        val monitoreo = Fixtures.monitoreo(id = 1, activo = true, observaciones = "Nota previa")
        val (vm, _) = viewModelCon(cache = listOf(monitoreo))
        cargarYEsperar(vm, 1)
        esperarEstado(vm.state) { it.monitoreo != null }
        gddService.actualizarMonitoreoResult = { Response.success(monitoreo.copy(activo = false)) }

        vm.finalizarMonitoreo(observaciones = "Nota previa") {}

        esperarEstado(vm.state) { it.finalizado }
        assertEquals(null, gddService.ultimoActualizarMonitoreo?.observaciones)
    }

    // ---------- eliminarMonitoreo ----------

    private fun error404(detalle: String): Response<Unit> =
        Response.error(404, """{"detail": "$detalle"}""".toResponseBody("application/json".toMediaType()))

    private fun cargado(): Pair<MonitoreoDetalleViewModel, FakeMonitoreoDao> {
        val (vm, dao) = viewModelCon(cache = listOf(Fixtures.monitoreo(id = 1), Fixtures.monitoreo(id = 2)))
        gddService.getMonitoreoResult = { Response.success(Fixtures.monitoreo(id = 1)) }
        vm.cargar(1)
        esperarEstado(vm.state) { it.monitoreo != null && !it.isLoading }
        return vm to dao
    }

    @Test
    fun `eliminarMonitoreo con exito lo borra de Room y avisa con su id`() {
        val (vm, dao) = cargado()
        gddService.eliminarMonitoreoResult = { Response.success(Unit) }

        var eliminadoId: Int? = null
        vm.eliminarMonitoreo { eliminadoId = it }

        esperarEstado(vm.state) { it.eliminado }
        assertEquals(1, eliminadoId)
        assertFalse(vm.state.value.eliminando)
        assertEquals(listOf(2), runBlocking { dao.getAll() }.map { it.monitoreo_id })
    }

    @Test
    fun `eliminarMonitoreo con 404 no encontrado lo purga igual porque ya no existe`() {
        val (vm, dao) = cargado()
        gddService.eliminarMonitoreoResult = { error404("Monitoreo no encontrado") }

        var llamado = false
        vm.eliminarMonitoreo { llamado = true }

        esperarEstado(vm.state) { it.eliminado }
        assertTrue(llamado)
        assertEquals(listOf(2), runBlocking { dao.getAll() }.map { it.monitoreo_id })
    }

    @Test
    fun `eliminarMonitoreo sin la ruta en el servidor avisa y no borra nada`() {
        val (vm, dao) = cargado()
        gddService.eliminarMonitoreoResult = { error404("Not Found") }

        var llamado = false
        vm.eliminarMonitoreo { llamado = true }

        val estado = esperarEstado(vm.state) { it.error != null }
        assertFalse(llamado)
        assertFalse(estado.eliminado)
        assertTrue(estado.error!!.contains("todavía no está disponible"))
        assertEquals(2, runBlocking { dao.getAll() }.size)
    }

    @Test
    fun `eliminarMonitoreo con error del servidor no borra nada`() {
        val (vm, dao) = cargado()
        gddService.eliminarMonitoreoResult = { FakeGDDService.errorServidor() }

        vm.eliminarMonitoreo {}

        val estado = esperarEstado(vm.state) { it.error != null }
        assertFalse(estado.eliminado)
        assertFalse(estado.eliminando)
        assertEquals(2, runBlocking { dao.getAll() }.size)
    }

    @Test
    fun `eliminarMonitoreo sin conexion muestra error y no borra nada`() {
        val (vm, dao) = cargado()
        gddService.eliminarMonitoreoResult = { FakeGDDService.sinConexion() }

        vm.eliminarMonitoreo {}

        val estado = esperarEstado(vm.state) { it.error != null }
        assertFalse(estado.eliminado)
        assertEquals(2, runBlocking { dao.getAll() }.size)
    }

    // ---------- monitoreo eliminado (push vieja) ----------

    private fun error404Monitoreo(detalle: String): Response<MonitoreoResponse> =
        Response.error(404, """{"detail": "$detalle"}""".toResponseBody("application/json".toMediaType()))

    @Test
    fun `cargar con 404 no encontrado purga el cache y marca no disponible`() {
        val (vm, dao) = viewModelCon(cache = listOf(Fixtures.monitoreo(id = 1), Fixtures.monitoreo(id = 2)))
        gddService.getMonitoreoResult = { error404Monitoreo("Monitoreo no encontrado") }

        vm.cargar(1)

        val estado = esperarEstado(vm.state) { it.noDisponible }
        assertEquals(null, estado.monitoreo)
        assertEquals(null, estado.error)
        assertFalse(estado.datosDesactualizados)
        assertEquals(listOf(2), runBlocking { dao.getAll() }.map { it.monitoreo_id })
    }

    @Test
    fun `cargar con 404 no encontrado sin cache marca no disponible`() {
        val (vm, _) = viewModelCon(cache = emptyList())
        gddService.getMonitoreoResult = { error404Monitoreo("Monitoreo no encontrado") }

        vm.cargar(99)

        val estado = esperarEstado(vm.state) { !it.isLoading }
        assertTrue(estado.noDisponible)
    }

    @Test
    fun `cargar sin cache con error del servidor no dice que fallo al guardar`() {
        val (vm, _) = viewModelCon(cache = emptyList())
        gddService.getMonitoreoResult = { FakeGDDService.errorServidor() }

        vm.cargar(1)

        val estado = esperarEstado(vm.state) { it.error != null }
        assertFalse(estado.noDisponible)
        assertEquals("No se pudo cargar el monitoreo. Intentá de nuevo.", estado.error)
    }
}
