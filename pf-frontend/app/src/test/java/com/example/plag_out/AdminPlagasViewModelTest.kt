package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
class AdminPlagasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: AdminPlagasViewModel

    private val carpocapsa = PlagaAdmin(
        id = 1, nombre = "Carpocapsa", nombre_cientifico = "Cydia pomonella",
        temp_base = 10.0, temp_max = 31.0, gdd_eclosion = 100.0, gdd_generacion = 550.0,
        cultivos_afectados = listOf(1), activo = true, monitoreos_activos = 3
    )
    private val formValido = FormularioPlaga(
        nombre = "Chicharrita", nombreCientifico = "Dalbulus maidis",
        tempBase = "10", tempMax = "32,5", gddEclosion = "120", gddGeneracion = "400",
        cultivos = setOf(2)
    )

    @Before
    fun setup() {
        gddService = FakeGDDService()
        gddService.getCultivosResult = { Response.success(listOf(CultivoResponse(1, "Manzano", "Malus domestica"))) }
        viewModel = AdminPlagasViewModel(gddService)
    }

    private fun <T> error(codigo: Int, detalle: String): Response<T> =
        Response.error(codigo, """{"detail":"$detalle"}""".toResponseBody("application/json".toMediaType()))

    @Test
    fun `cargar trae plagas y cultivos`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.cargado && it.cultivos.isNotEmpty() }
        assertEquals(listOf(carpocapsa), estado.plagas)
        assertFalse(estado.isLoading)
    }

    @Test
    fun `403 requiere_admin marca sinPermiso`() {
        gddService.adminGetPlagasResult = { error(403, "requiere_admin") }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertTrue(estado.sinPermiso)
    }

    @Test
    fun `404 avisa que el backend no tiene el endpoint`() {
        gddService.adminGetPlagasResult = { FakeGDDService.errorServidor(404) }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertEquals("Esta función todavía no está disponible en el servidor.", estado.error)
        assertFalse(estado.sinPermiso)
    }

    @Test
    fun `crear manda todos los campos con coma decimal convertida y agrega la plaga`() {
        val creada = PlagaAdmin(id = 9, nombre = "Chicharrita", nombre_cientifico = "Dalbulus maidis", activo = true)
        gddService.adminCrearPlagaResult = { Response.success(creada) }
        var guardado = false

        viewModel.guardar(null, formValido) { guardado = true }

        esperarEstado(viewModel.state) { it.plagas.contains(creada) }
        assertTrue(guardado)
        val enviado = gddService.ultimaPlagaEnviada!!
        assertEquals(32.5, enviado.temp_max!!, 0.0)
        assertEquals(listOf(2), enviado.cultivos_afectados)
        assertNull(enviado.activo)
    }

    @Test
    fun `formulario invalido no llama al backend`() {
        viewModel.guardar(null, formValido.copy(tempBase = "40")) { }

        assertEquals(0, gddService.vecesLlamado("adminCrearPlaga"))
    }

    @Test
    fun `editar manda solo los campos cambiados`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado }
        gddService.adminActualizarPlagaResult = { Response.success(carpocapsa.copy(gdd_eclosion = 110.0)) }

        viewModel.guardar(carpocapsa, FormularioPlaga.desde(carpocapsa).copy(gddEclosion = "110")) { }

        esperarEstado(viewModel.state) { it.plagas.first().gdd_eclosion == 110.0 }
        assertEquals(PlagaAdminRequest(gdd_eclosion = 110.0), gddService.ultimaPlagaEnviada)
    }

    @Test
    fun `editar sin cambios no llama al backend`() {
        var guardado = false

        viewModel.guardar(carpocapsa, FormularioPlaga.desde(carpocapsa)) { guardado = true }

        assertTrue(guardado)
        assertEquals(0, gddService.vecesLlamado("adminActualizarPlaga"))
    }

    @Test
    fun `nombre duplicado muestra el error de la accion`() {
        gddService.adminCrearPlagaResult = { error(409, "plaga_duplicada") }

        viewModel.guardar(null, formValido) { }

        val estado = esperarEstado(viewModel.state) { it.errorAccion != null }
        assertEquals("Ya existe una plaga con ese nombre o nombre científico.", estado.errorAccion)
        assertFalse(estado.guardando)
    }

    @Test
    fun `dar de baja y reactivar reemplazan la plaga en la lista`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado }

        gddService.adminDesactivarPlagaResult = { Response.success(carpocapsa.copy(activo = false)) }
        viewModel.desactivar(carpocapsa.id)
        esperarEstado(viewModel.state) { !it.plagas.first().estaActiva }

        gddService.adminActualizarPlagaResult = { Response.success(carpocapsa) }
        viewModel.reactivar(carpocapsa.id)
        esperarEstado(viewModel.state) { it.plagas.first().estaActiva && !it.guardando }
        assertEquals(PlagaAdminRequest(activo = true), gddService.ultimaPlagaEnviada)
    }
}
