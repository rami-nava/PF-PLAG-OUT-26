package com.example.plag_out

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.TratamientoPendiente
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import com.google.gson.JsonParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
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
import java.time.LocalDate

// Room en memoria corre en sus propios hilos: con la suite completa en paralelo, 3 s no alcanzan.
private const val ESPERA_MS = 10_000L

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class TratamientosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var service: FakeGDDService
    private lateinit var viewModel: TratamientosViewModel
    private var claves = 0

    private val form = FormularioTratamiento(
        fechaAplicacion = LocalDate.of(2026, 10, 1),
        producto = "Clorantraniliprole",
        conteoAntes = "12",
        fechaConteoAntes = LocalDate.of(2026, 9, 30)
    )

    private fun respuesta(alta: TratamientoRequest, id: Int = 50) = TratamientoResponse(
        id = id, monitoreo_id = 3, fecha_aplicacion = alta.fecha_aplicacion, producto = alta.producto,
        unidad_conteo = alta.unidad_conteo, conteo_antes = alta.conteo_antes, fecha_conteo_antes = alta.fecha_conteo_antes,
        conteo_despues = alta.conteo_despues, fecha_conteo_despues = alta.fecha_conteo_despues,
        testigo_antes = alta.testigo_antes, testigo_despues = alta.testigo_despues,
        sin_calculo = if (alta.conteo_despues == null) "falta_conteo_despues" else null,
        metodo = if (alta.conteo_despues == null) null else "reduccion_observada",
        reduccion_observada_pct = alta.conteo_despues?.let { (1 - it / alta.conteo_antes) * 100 }
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        service = FakeGDDService()
        service.getTratamientosResult = { Response.success(emptyList()) }
        viewModel = TratamientosViewModel(
            service = service,
            dao = db.tratamientoPendienteDao(),
            owner = { "owner" },
            servicioDe = { service },
            nuevaClave = { "clave-${++claves}" }
        )
        viewModel.cargar(3)
        esperarEstado(viewModel.state, ESPERA_MS) { !it.cargando }
    }

    @After
    fun cerrar() = db.close()

    private fun filas() = runBlocking { db.tratamientoPendienteDao().get("owner", "clave-1") }

    @Test
    fun `con senal se envia y no queda nada en la bandeja`() {
        service.registrarTratamientoResult = { Response.success(201, respuesta(it)) }
        var resultado: EnvioTratamiento? = null

        viewModel.registrar(form) { resultado = it }

        esperarEstado(viewModel.state, ESPERA_MS) { it.tratamientos.isNotEmpty() && !it.guardando }
        assertTrue(resultado is EnvioTratamiento.Enviado)
        assertEquals("clave-1", service.tratamientosEnviados.single().idempotency_key)
        assertNull(filas())
        assertTrue(viewModel.state.value.items.single() is ItemTratamiento.Enviado)
    }

    @Test
    fun `sin senal queda en la bandeja y se ve como pendiente`() {
        service.registrarTratamientoResult = { throw IOException("sin red") }
        var resultado: EnvioTratamiento? = null

        viewModel.registrar(form) { resultado = it }

        val estado = esperarEstado(viewModel.state, ESPERA_MS) { it.items.isNotEmpty() && !it.guardando }
        assertEquals(EnvioTratamiento.Reintentar, resultado)
        val item = estado.items.single() as ItemTratamiento.SinEnviar
        assertEquals("pendiente", item.fila.estado)
        assertEquals("Clorantraniliprole", item.alta.producto)
    }

    @Test
    fun `el conteo posterior de un alta sin enviar viaja en la misma alta con la misma clave`() {
        service.registrarTratamientoResult = { throw IOException("sin red") }
        viewModel.registrar(form)
        val item = esperarEstado(viewModel.state, ESPERA_MS) { it.items.isNotEmpty() && !it.guardando }.items.single()

        service.registrarTratamientoResult = { Response.success(201, respuesta(it)) }
        viewModel.editar(item, form.copy(conDespues = true, conteoDespues = "3", fechaConteoDespues = LocalDate.of(2026, 10, 8)))

        esperarEstado(viewModel.state, ESPERA_MS) { it.tratamientos.isNotEmpty() && it.pendientes.isEmpty() }
        val enviados = service.tratamientosEnviados
        assertEquals(listOf("clave-1", "clave-1"), enviados.map { it.idempotency_key })
        assertEquals(3.0, enviados.last().conteo_despues!!, 0.0)
        assertEquals(0, service.vecesLlamado("actualizarTratamiento"))
    }

    @Test
    fun `un rechazo del backend queda a revisar y no se reintenta`() {
        service.registrarTratamientoResult = {
            Response.error(409, """{"detail":"monitoring_inactive"}""".toResponseBody("application/json".toMediaType()))
        }
        var resultado: EnvioTratamiento? = null

        viewModel.registrar(form) { resultado = it }

        val estado = esperarEstado(viewModel.state, ESPERA_MS) { it.items.isNotEmpty() && !it.guardando }
        assertEquals(EnvioTratamiento.Rechazado("El monitoreo está finalizado: no admite tratamientos nuevos."), resultado)
        assertTrue((estado.items.single() as ItemTratamiento.SinEnviar).requiereRevision)
    }

    @Test
    fun `editar uno enviado manda el PATCH con los null explicitos`() {
        val enviado = respuesta(form.copy(conDespues = true, conteoDespues = "3", fechaConteoDespues = LocalDate.of(2026, 10, 8)).aRequest("x"), id = 9)
        service.getTratamientosResult = { Response.success(listOf(enviado)) }
        viewModel.cargar(3)
        val item = esperarEstado(viewModel.state, ESPERA_MS) { it.tratamientos.isNotEmpty() }.items.single()
        service.actualizarTratamientoResult = { Response.success(enviado.copy(conteo_despues = null, fecha_conteo_despues = null)) }

        viewModel.editar(item, FormularioTratamiento.desde(enviado).copy(conDespues = false))

        esperarEstado(viewModel.state, ESPERA_MS) { it.tratamientos.single().conteo_despues == null && it.pendientes.isEmpty() }
        val (id, cuerpo) = service.edicionesDeTratamiento.single()
        assertEquals(9, id)
        assertEquals(JsonParser.parseString("""{"conteo_despues":null,"fecha_conteo_despues":null}"""), cuerpo)
    }

    @Test
    fun `un alta que cambio mientras viajaba queda como edicion del tratamiento creado`() = runBlocking {
        val dao = db.tratamientoPendienteDao()
        val alta = form.aRequest("k1")
        val fila = TratamientoPendiente("k1", "owner", 3, payload = gsonTratamientos.toJson(alta))
        dao.guardar(fila)
        service.registrarTratamientoResult = {
            // El productor carga el conteo posterior justo mientras el alta está en el aire.
            runBlocking {
                dao.guardar(fila.copy(payload = gsonTratamientos.toJson(alta.copy(conteo_despues = 3.0, fecha_conteo_despues = "2026-10-08"))))
            }
            Response.success(201, respuesta(alta, id = 77))
        }

        val envio = enviarTratamientoPendiente(fila, service, dao)

        assertTrue(envio is EnvioTratamiento.Enviado)
        val queda = dao.get("owner", "k1")!!
        assertEquals(77, queda.tratamiento_id)
        assertEquals(3.0, JsonParser.parseString(queda.payload).asJsonObject.get("conteo_despues").asDouble, 0.0)
    }

    @Test
    fun `descartar un alta sin enviar la borra solo del telefono`() {
        service.registrarTratamientoResult = { throw IOException("sin red") }
        viewModel.registrar(form)
        val item = esperarEstado(viewModel.state, ESPERA_MS) { it.items.isNotEmpty() && !it.guardando }.items.single()

        viewModel.eliminar(item)

        esperarEstado(viewModel.state, ESPERA_MS) { it.items.isEmpty() }
        assertEquals(0, service.vecesLlamado("eliminarTratamiento"))
    }
}
