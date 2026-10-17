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
class AdminUsuariosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: AdminUsuariosViewModel

    private fun usuario(n: Int, activo: Boolean = true) =
        UsuarioAdmin(id = "u$n", email = "u$n@campo.com", nombre = "Usuario", apellido = "$n", activo = activo)

    private fun pagina(desde: Int, cantidad: Int, total: Int) = Pagina(
        items = (desde until desde + cantidad).map { usuario(it) },
        total = total
    )

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = AdminUsuariosViewModel(gddService, esperaBusquedaMs = 0L)
    }

    @Test
    fun `cargar y cargarMas acumulan paginas hasta el total`() {
        gddService.adminGetUsuariosResult = { Response.success(pagina(0, 20, total = 25)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
        assertTrue(viewModel.state.value.hayMas)

        gddService.adminGetUsuariosResult = { Response.success(pagina(20, 5, total = 25)) }
        viewModel.cargarMas()

        val estado = esperarEstado(viewModel.state) { it.usuarios.size == 25 && !it.cargandoMas }
        assertFalse(estado.hayMas)
        assertEquals(20, gddService.ultimaBusquedaUsuarios?.second)
    }

    @Test
    fun `buscar reinicia desde el offset 0 con el texto`() {
        gddService.adminGetUsuariosResult = { Response.success(pagina(0, 1, total = 1)) }

        viewModel.buscar("  ana ")

        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
        assertEquals("ana" to 0, gddService.ultimaBusquedaUsuarios)
    }

    @Test
    fun `filtrarUsuarios responde al instante con lo ya cargado`() {
        val usuarios = listOf(
            UsuarioAdmin(id = "a", email = "ana@campo.com", nombre = "Ana", apellido = "López"),
            UsuarioAdmin(id = "b", email = "beto@campo.com", nombre = "Beto", apellido = "Gómez", activo = false)
        )

        assertEquals(listOf("a"), filtrarUsuarios(usuarios, " ana lóp ", FiltroUsuarios.TODOS).map { it.id })
        assertEquals(listOf("b"), filtrarUsuarios(usuarios, "BETO@", FiltroUsuarios.TODOS).map { it.id })
        assertEquals(listOf("b"), filtrarUsuarios(usuarios, "", FiltroUsuarios.SUSPENDIDOS).map { it.id })
        assertTrue(filtrarUsuarios(usuarios, "gómez", FiltroUsuarios.ACTIVOS).isEmpty())
    }

    @Test
    fun `suspender reemplaza al usuario con la respuesta`() {
        gddService.adminGetUsuariosResult = { Response.success(pagina(0, 2, total = 2)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
        gddService.adminSuspenderUsuarioResult = { Response.success(usuario(1, activo = false)) }

        viewModel.suspender("u1")

        val estado = esperarEstado(viewModel.state) {
            it.procesando == null && it.usuarios.any { u -> u.id == "u1" && !u.estaActivo }
        }
        assertTrue(estado.usuarios.first { it.id == "u0" }.estaActivo)
    }

    @Test
    fun `operar sobre un admin muestra el error del backend`() {
        gddService.adminGetUsuariosResult = { Response.success(pagina(0, 1, total = 1)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
        gddService.adminSuspenderUsuarioResult = {
            Response.error(403, """{"detail":"objetivo_admin"}""".toResponseBody("application/json".toMediaType()))
        }

        viewModel.suspender("u0")

        val estado = esperarEstado(viewModel.state) { it.errorAccion != null && it.procesando == null }
        assertEquals("No se puede operar sobre una cuenta de administrador.", estado.errorAccion)
        assertFalse(estado.sinPermiso)
    }

    @Test
    fun `eliminar pide desde lo que queda en el servidor para recuperar al que se corrio`() {
        gddService.adminGetUsuariosResult = { Response.success(pagina(0, 20, total = 25)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
        gddService.adminEliminarUsuarioResult = {
            Response.success(AccountDeletionResponse(deleted = true, auth_identity_deleted = true, retained_reports = 0))
        }
        // En el servidor u5 ya no está: desde el offset 19 siguen u20..u24
        gddService.adminGetUsuariosResult = { Response.success(pagina(20, 5, total = 24)) }

        viewModel.eliminar("u5") {}

        val estado = esperarEstado(viewModel.state) { it.usuarios.any { u -> u.id == "u20" } && !it.cargandoMas }
        assertEquals(19, gddService.ultimaBusquedaUsuarios?.second)
        assertEquals(24, estado.usuarios.size)
        assertEquals(24, estado.usuarios.map { it.id }.distinct().size)
        assertEquals(24, estado.total)
        assertFalse(estado.hayMas)
    }

    @Test
    fun `suspender con el filtro de activos completa la pagina sin sacarlo del detalle`() {
        gddService.adminGetUsuariosResult = { Response.success(pagina(0, 20, total = 25)) }
        viewModel.cambiarFiltro(FiltroUsuarios.ACTIVOS)
        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
        gddService.adminSuspenderUsuarioResult = { Response.success(usuario(3, activo = false)) }
        gddService.adminGetUsuariosResult = { Response.success(pagina(20, 4, total = 24)) }

        viewModel.suspender("u3")

        val estado = esperarEstado(viewModel.state) {
            it.usuarios.any { u -> u.id == "u20" } && !it.cargandoMas && it.procesando == null
        }
        assertFalse(viewModel.usuario("u3")!!.estaActivo)
        assertEquals(19, gddService.ultimaBusquedaUsuarios?.second)
        assertEquals(23, estado.usuariosVisibles.size)
        assertTrue(estado.hayMas)
    }

    @Test
    fun `eliminar saca al usuario de la lista y baja el total`() {
        gddService.adminGetUsuariosResult = { Response.success(pagina(0, 2, total = 2)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
        gddService.adminEliminarUsuarioResult = {
            Response.success(AccountDeletionResponse(deleted = true, auth_identity_deleted = true, retained_reports = 0))
        }
        var eliminado = false

        viewModel.eliminar("u0") { eliminado = true }

        val estado = esperarEstado(viewModel.state) { it.usuarios.size == 1 }
        assertTrue(eliminado)
        assertEquals(1, estado.total)
    }
}
