package com.example.plag_out

import com.example.plag_out.AlmacenamientoLocal.UsuarioRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeUsuarioDao
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

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
}
