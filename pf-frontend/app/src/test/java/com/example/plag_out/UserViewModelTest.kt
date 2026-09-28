package com.example.plag_out

import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeUsuarioDao
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    @Test
    fun `aceptar y revocar usan la version estable del contrato`() {
        val service = FakeGDDService()
        val vm = UserViewModel(UsuarioRepository(FakeUsuarioDao()), service)
        service.actualizarConsentimientoModeloResult = {
            val enviado = service.ultimoConsentimientoModelo!!
            Response.success(ConsentimientoModeloResponse(enviado.consentido, "2026-09-04T00:00:00Z", enviado.version_contrato))
        }

        vm.actualizarConsentimientoModelo(true)
        esperarEstado(vm.state) { it.consentimientoModelo?.consentido == true }
        assertEquals(VERSION_CONSENTIMIENTO_ML, service.ultimoConsentimientoModelo?.version_contrato)

        vm.actualizarConsentimientoModelo(false)
        esperarEstado(vm.state) { it.consentimientoModelo?.consentido == false }
        assertFalse(service.ultimoConsentimientoModelo!!.consentido)
    }

    @Test
    fun `conflicto de version mantiene el consentimiento anterior`() {
        val service = FakeGDDService()
        val vm = UserViewModel(UsuarioRepository(FakeUsuarioDao()), service)
        service.getConsentimientoModeloResult = {
            Response.success(ConsentimientoModeloResponse(false, null, VERSION_CONSENTIMIENTO_ML))
        }
        vm.cargarConsentimientoModelo()
        esperarEstado(vm.state) { it.consentimientoModelo != null }
        service.actualizarConsentimientoModeloResult = { FakeGDDService.errorServidor(409) }

        vm.actualizarConsentimientoModelo(true)
        esperarEstado(vm.state) { it.consentimientoError != null }

        assertFalse(vm.state.value.consentimientoModelo!!.consentido)
        assertEquals(
            "Actualizá la app para revisar la versión vigente del consentimiento.",
            vm.state.value.consentimientoError
        )
    }

    @Test
    fun `sin red y con perfil guardado muestra el cache marcado como sin conexion`() {
        val service = FakeGDDService()
        val guardado = Fixtures.usuario()
        val vm = UserViewModel(UsuarioRepository(FakeUsuarioDao(guardado)), service)
        service.getUsuarioActualResult = { FakeGDDService.sinConexion() }

        vm.getUsuario()
        esperarEstado(vm.state) { it.sinConexion && !it.isRefreshing }

        assertEquals(guardado, vm.state.value.usuario)
        assertTrue(vm.state.value.sinConexion)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `sin red y sin perfil guardado queda en estado sin conexion`() {
        val service = FakeGDDService()
        val vm = UserViewModel(UsuarioRepository(FakeUsuarioDao()), service)
        service.getUsuarioActualResult = { FakeGDDService.sinConexion() }

        vm.getUsuario()
        esperarEstado(vm.state) { !it.isLoading }

        assertNull(vm.state.value.usuario)
        assertTrue(vm.state.value.sinConexion)
    }

    @Test
    fun `al volver la red el refresco limpia el aviso de sin conexion`() {
        val service = FakeGDDService()
        val vm = UserViewModel(UsuarioRepository(FakeUsuarioDao(Fixtures.usuario())), service)
        service.getUsuarioActualResult = { FakeGDDService.sinConexion() }
        vm.getUsuario()
        esperarEstado(vm.state) { it.sinConexion }

        service.getUsuarioActualResult = { Response.success(Fixtures.usuario(nombre = "Nuevo")) }
        vm.refrescar()
        esperarEstado(vm.state) { !it.sinConexion && !it.isRefreshing }

        assertEquals("Nuevo", vm.state.value.usuario?.nombre)
    }

    @Test
    fun `un error del backend no se confunde con falta de conexion`() {
        val service = FakeGDDService()
        val vm = UserViewModel(UsuarioRepository(FakeUsuarioDao()), service)
        service.getUsuarioActualResult = { FakeGDDService.errorServidor(500) }

        vm.getUsuario()
        esperarEstado(vm.state) { !it.isLoading }

        assertFalse(vm.state.value.sinConexion)
        assertEquals("No se pudo cargar tu perfil.", vm.state.value.error)
    }
}
