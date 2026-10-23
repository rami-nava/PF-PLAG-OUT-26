package com.example.plag_out

import android.app.Application
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class BiofixDialogTest {
    @get:Rule val compose = createComposeRule()
    private val cycle = GddCicloResponse(73, 1, "2026-01-01", "activo", "2026-01-02", 10f,10f,100f,380f,10f,"Inicio declarado",1,0)

    private val opcionDeCiclo = isSelectable() and SemanticsMatcher("no es chip de severidad") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("optSeveridadBiofix_") != true
    }

    private fun elegirSeveridad(nivel: String) {
        compose.onNodeWithTag("optSeveridadBiofix_$nivel").performScrollTo().performClick()
    }

    @Test fun `sin ciclo confirma inicio declarado`() {
        var result: BiofixRequest? = null
        val service = FakeGDDService().apply { getMonitoreoResult = { Response.success(Fixtures.monitoreo().copy(ciclos=emptyList())) } }
        compose.setContent { BiofixDialog(1, {}, {result=it},service) }
        compose.waitForIdle()
        compose.onNodeWithTag("btnConfirmarBiofix").performScrollTo().assertIsNotEnabled()
        elegirSeveridad("Medio")
        compose.onNodeWithTag("btnConfirmarBiofix").performScrollTo().assertIsEnabled().performClick()
        assertEquals("iniciar_ciclo", result?.accion)
        assertEquals(false, result?.confirmar_ciclo_adicional)
        assertNotNull(result?.idempotency_key)
        assertEquals("Medio", result?.nivel_severidad)
    }

    @Test fun `ciclo existente requiere elegir antes de confirmar`() {
        var result: BiofixRequest? = null
        val service = FakeGDDService().apply { getMonitoreoResult = { Response.success(Fixtures.monitoreo().copy(ciclos=listOf(cycle))) } }
        compose.setContent { BiofixDialog(1, {}, {result=it},service) }
        compose.waitForIdle()
        compose.onNodeWithTag("btnConfirmarBiofix").performScrollTo().assertIsNotEnabled()
        elegirSeveridad("Alto")
        compose.onNodeWithTag("btnConfirmarBiofix").performScrollTo().assertIsNotEnabled()
        compose.onAllNodes(opcionDeCiclo)[0].performScrollTo().performClick()
        compose.onNodeWithTag("btnConfirmarBiofix").performScrollTo().assertIsEnabled().performClick()
        assertEquals("asociar_ciclo",result?.accion)
        assertEquals(73,result?.ciclo_id)
        assertEquals("Alto",result?.nivel_severidad)
    }

    @Test fun `otro ciclo requiere confirmacion expresa`() {
        var result: BiofixRequest? = null
        val service = FakeGDDService().apply { getMonitoreoResult = { Response.success(Fixtures.monitoreo().copy(ciclos=listOf(cycle))) } }
        compose.setContent { BiofixDialog(1, {}, {result=it},service) }
        compose.waitForIdle()
        elegirSeveridad("Bajo")
        compose.onAllNodes(opcionDeCiclo)[1].performScrollTo().performClick()
        compose.onNodeWithTag("btnConfirmarBiofix").performScrollTo().performClick()
        assertEquals("iniciar_ciclo",result?.accion)
        assertEquals(true,result?.confirmar_ciclo_adicional)
        assertNull(result?.ciclo_id)
        assertEquals("Bajo",result?.nivel_severidad)
    }

    @Test fun `tarjeta aviso biofix dispara confirmar y descartar`() {
        var confirmado = false
        var descartado = false
        compose.setContent {
            TarjetaAvisoBiofixPendiente(
                onConfirmar = { confirmado = true },
                onDescartar = { descartado = true }
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("tarjetaAvisoBiofixPendiente").assertIsDisplayed()
        compose.onNodeWithTag("btnConfirmarAvisoBiofix").assertIsDisplayed().performClick()
        assertTrue(confirmado)
        assertFalse(descartado)

        compose.onNodeWithTag("btnDescartarAvisoBiofix").assertIsDisplayed().performClick()
        assertTrue(descartado)
    }
}
