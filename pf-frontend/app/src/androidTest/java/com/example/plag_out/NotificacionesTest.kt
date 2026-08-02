package com.example.plag_out

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class NotificacionesTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val alerta = Fixtures.notificacion(id = 1, titulo = "Riesgo alto en Lote Norte")
    private val otra = Fixtures.notificacion(id = 2, titulo = "Riesgo alto en Lote Sur")

    // ── Badge del timbre ─────────────────────────────────────────────────────

    @Test
    fun el_timbre_muestra_la_cantidad_sin_leer() {
        composeRule.setContent { TopBar(noLeidas = 3, onNotificacionesClick = {}) }

        composeRule.onNodeWithTag("btnNotificaciones").assertIsDisplayed()
        composeRule.onNodeWithTag("badgeNotificaciones").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun sin_notificaciones_el_timbre_no_tiene_badge() {
        composeRule.setContent { TopBar(noLeidas = 0, onNotificacionesClick = {}) }

        composeRule.onNodeWithTag("btnNotificaciones").assertIsDisplayed()
        composeRule.onNodeWithTag("badgeNotificaciones").assertDoesNotExist()
    }

    // ── Hoja de notificaciones ───────────────────────────────────────────────

    @Test
    fun la_hoja_lista_las_notificaciones_sin_leer() {
        composeRule.setContent {
            NotificacionesSheet(
                state = NotificacionesUIState(notificaciones = listOf(alerta, otra)),
                onMarcarLeida = {},
                onNavegar = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("2 avisos sin leer").assertIsDisplayed()
        composeRule.onNodeWithText("Riesgo alto en Lote Norte").assertIsDisplayed()
        composeRule.onNodeWithText("Riesgo alto en Lote Sur").assertIsDisplayed()
    }

    @Test
    fun sin_pendientes_la_hoja_muestra_el_estado_al_dia() {
        composeRule.setContent {
            NotificacionesSheet(
                state = NotificacionesUIState(notificaciones = emptyList()),
                onMarcarLeida = {},
                onNavegar = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("Estás al día").assertIsDisplayed()
    }

    @Test
    fun tocar_una_alerta_gdd_la_marca_leida_y_navega_al_monitoreo() {
        var marcada: NotificacionResponse? = null
        var destino: String? = null

        composeRule.setContent {
            NotificacionesSheet(
                state = NotificacionesUIState(notificaciones = listOf(alerta)),
                onMarcarLeida = { marcada = it },
                onNavegar = { destino = it },
                onDismiss = {}
            )
        }

        composeRule.onNodeWithText("Riesgo alto en Lote Norte").performClick()
        composeRule.waitForIdle()

        assertEquals(alerta.id, marcada?.id)
        // entidad_id = 7 en el fixture: la alerta lleva al detalle de ese monitoreo
        assertEquals("monitoreo/7", destino)
    }

    @Test
    fun deslizar_una_notificacion_la_marca_leida() {
        var marcada: NotificacionResponse? = null

        composeRule.setContent {
            NotificacionesSheet(
                state = NotificacionesUIState(notificaciones = listOf(alerta)),
                onMarcarLeida = { marcada = it },
                onNavegar = {},
                onDismiss = {}
            )
        }

        composeRule.onNodeWithTag("notificacion_1").performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(alerta.id, marcada?.id)
    }

    // ── ViewModel ────────────────────────────────────────────────────────────

    @Test
    fun cargar_trae_las_no_leidas_y_alimenta_el_badge() = runTest {
        val gddService = FakeGDDService()
        gddService.getNotificacionesResult = { Response.success(listOf(alerta, otra)) }
        val viewModel = NotificacionesViewModel(gddService)

        viewModel.cargar()
        composeRule.waitUntil { viewModel.state.value.noLeidas == 2 }

        assertEquals(2, viewModel.state.value.noLeidas)
    }

    @Test
    fun marcar_leida_manda_el_patch_y_la_saca_de_la_lista() = runTest {
        val gddService = FakeGDDService()
        gddService.getNotificacionesResult = { Response.success(listOf(alerta, otra)) }
        gddService.marcarNotificacionLeidaResult = { Response.success(MarcarLeidaResponse(1, true)) }
        val viewModel = NotificacionesViewModel(gddService)

        viewModel.cargar()
        composeRule.waitUntil { viewModel.state.value.noLeidas == 2 }

        viewModel.marcarLeida(alerta)
        composeRule.waitUntil { gddService.vecesLlamado("marcarNotificacionLeida") == 1 }

        assertEquals(alerta.id, gddService.ultimaNotificacionLeida)
        assertEquals(listOf(otra.id), viewModel.state.value.notificaciones.map { it.id })
    }

    @Test
    fun si_el_patch_falla_la_notificacion_vuelve_a_la_lista() = runTest {
        val gddService = FakeGDDService()
        gddService.getNotificacionesResult = { Response.success(listOf(alerta, otra)) }
        gddService.marcarNotificacionLeidaResult = { FakeGDDService.errorServidor(500) }
        val viewModel = NotificacionesViewModel(gddService)

        viewModel.cargar()
        composeRule.waitUntil { viewModel.state.value.noLeidas == 2 }

        viewModel.marcarLeida(alerta)
        composeRule.waitUntil { viewModel.state.value.error != null }

        // El badge no puede quedar mintiendo: si el backend no la marcó, sigue pendiente
        assertEquals(listOf(alerta.id, otra.id), viewModel.state.value.notificaciones.map { it.id })
    }
}
