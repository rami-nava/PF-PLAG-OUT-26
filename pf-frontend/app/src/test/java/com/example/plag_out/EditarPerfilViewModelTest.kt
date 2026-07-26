package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
class EditarPerfilViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService

    /** Cargos que devuelve el backend; el segundo se usa para probar el cambio de cargo. */
    private val cargos = listOf(
        Fixtures.cargo(id = 1, tipo = "Ingeniero agrónomo"),
        Fixtures.cargo(id = 2, tipo = "Técnico de campo")
    )

    private val usuario = Fixtures.usuario(
        nombre = "Juan",
        apellido = "Perez",
        cargo = "Ingeniero agrónomo"
    )

    /** Registra lo que se espejó en Supabase, para no necesitar un cliente real. */
    private val espejados = mutableListOf<Triple<String, String, Int?>>()

    @Before
    fun setup() {
        gddService = FakeGDDService()
        gddService.getCargosResult = { Response.success(cargos) }
        espejados.clear()
    }

    private fun viewModel(): EditarPerfilViewModel =
        EditarPerfilViewModel(
            gddService = gddService,
            espejarEnSupabase = { nombre, apellido, cargoId ->
                espejados += Triple(nombre, apellido, cargoId)
            }
        )

    private fun viewModelPrecargado(): EditarPerfilViewModel {
        val vm = viewModel()
        vm.precargar(usuario)
        esperarEstado(vm.state) { it.cargosDisponibles.isNotEmpty() }
        return vm
    }

    // ---------- precargar ----------

    @Test
    fun `precargar llena los campos con los datos del usuario`() {
        val vm = viewModelPrecargado()

        assertEquals("Juan", vm.state.value.nombre)
        assertEquals("Perez", vm.state.value.apellido)
    }

    @Test
    fun `precargar resuelve el cargo de texto contra la lista de cargos`() {
        val vm = viewModelPrecargado()

        // UsuarioResponse trae el cargo como String y el request necesita el id.
        assertEquals(1, vm.state.value.cargo?.id)
        assertEquals("Ingeniero agrónomo", vm.state.value.cargo?.tipo)
    }

    @Test
    fun `precargar deja el cargo sin seleccionar si no coincide con ningun cargo`() {
        val vm = viewModel()
        vm.precargar(usuario.copy(cargo = "Cargo que ya no existe"))
        esperarEstado(vm.state) { it.cargosDisponibles.isNotEmpty() }

        assertNull(vm.state.value.cargo)
    }

    @Test
    fun `precargar no vuelve a pisar los campos del mismo usuario`() {
        val vm = viewModelPrecargado()
        vm.actualizarNombre("Juan editado")

        vm.precargar(usuario)

        assertEquals("Juan editado", vm.state.value.nombre)
        assertEquals(1, gddService.vecesLlamado("getCargos"))
    }

    // ---------- validación ----------

    @Test
    fun `no se puede guardar sin cambios`() {
        val vm = viewModelPrecargado()

        assertFalse(vm.hayCambios())
        assertFalse(vm.puedeGuardar())
    }

    @Test
    fun `no se puede guardar con el nombre vacio`() {
        val vm = viewModelPrecargado()
        vm.actualizarNombre("   ")

        assertFalse(vm.formularioValido())
        assertFalse(vm.puedeGuardar())
    }

    @Test
    fun `no se puede guardar con el apellido mas largo que el maximo`() {
        val vm = viewModelPrecargado()
        vm.actualizarApellido("a".repeat(61))

        assertFalse(vm.formularioValido())
    }

    @Test
    fun `se puede guardar al cambiar el cargo aunque el nombre no cambie`() {
        val vm = viewModelPrecargado()
        vm.actualizarCargo(cargos[1])

        assertTrue(vm.puedeGuardar())
    }

    // ---------- guardar ----------

    @Test
    fun `guardar manda el nombre recortado y el id del cargo`() {
        val vm = viewModelPrecargado()
        gddService.actualizarUsuarioResult = {
            Response.success(usuario.copy(nombre = "Juan Ariel", cargo = "Técnico de campo"))
        }
        vm.actualizarNombre("  Juan Ariel  ")
        vm.actualizarCargo(cargos[1])

        vm.guardar {}
        esperarEstado(vm.state) { !it.guardando }

        val enviado = gddService.ultimoActualizarUsuario
        assertNotNull(enviado)
        assertEquals("Juan Ariel", enviado?.nombre)
        assertEquals("Perez", enviado?.apellido)
        assertEquals(2, enviado?.cargo_id)
    }

    @Test
    fun `guardar exitoso devuelve el usuario del backend y espeja en Supabase`() {
        val vm = viewModelPrecargado()
        val devuelto = usuario.copy(nombre = "Juan Ariel")
        gddService.actualizarUsuarioResult = { Response.success(devuelto) }
        vm.actualizarNombre("Juan Ariel")

        var recibido: UsuarioResponse? = null
        vm.guardar { recibido = it }
        esperarEstado(vm.state) { !it.guardando }

        assertEquals(devuelto, recibido)
        assertEquals(listOf(Triple("Juan Ariel", "Perez", 1)), espejados)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `guardar arma el usuario localmente si el backend no devuelve cuerpo`() {
        val vm = viewModelPrecargado()
        // 204 sin cuerpo: el ViewModel no debe depender de la forma de la respuesta.
        gddService.actualizarUsuarioResult = { Response.success<UsuarioResponse>(204, null) }
        vm.actualizarNombre("Juan Ariel")
        vm.actualizarCargo(cargos[1])

        var recibido: UsuarioResponse? = null
        vm.guardar { recibido = it }
        esperarEstado(vm.state) { !it.guardando }

        assertEquals("Juan Ariel", recibido?.nombre)
        assertEquals("Técnico de campo", recibido?.cargo)
        assertEquals(usuario.email, recibido?.email)
    }

    @Test
    fun `guardar informa el error cuando no hay conexion`() {
        val vm = viewModelPrecargado()
        gddService.actualizarUsuarioResult = { FakeGDDService.sinConexion() }
        vm.actualizarNombre("Juan Ariel")

        vm.guardar {}
        esperarEstado(vm.state) { it.error != null }

        assertEquals(
            "No se pudieron guardar los cambios. Revisá tu conexión.",
            vm.state.value.error
        )
        assertFalse(vm.state.value.guardando)
    }

    @Test
    fun `guardar no le pega al backend si el formulario es invalido`() {
        val vm = viewModelPrecargado()
        vm.actualizarNombre("")

        vm.guardar {}

        assertEquals(0, gddService.vecesLlamado("actualizarUsuario"))
        assertEquals("Completá tu nombre y apellido.", vm.state.value.error)
    }
}
