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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import java.time.LocalDate
import java.time.ZoneId
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

    @Test fun `monitoreo que empezo hoy no ofrece un calendario sin otras fechas`() {
        val hoy = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
        val service = FakeGDDService().apply {
            getMonitoreoResult = { Response.success(Fixtures.monitoreo(fechaInicio = hoy).copy(ciclos = emptyList())) }
        }
        compose.setContent { BiofixDialog(1, {}, {}, service) }
        compose.waitForIdle()
        compose.onNodeWithText("Hoy · el monitoreo empezó hoy").assertExists()
        compose.onNodeWithTag("btnFechaBiofix").assertIsNotEnabled()
    }

    @Test fun `las etiquetas de los botones no se parten en dos lineas`() {
        val service = FakeGDDService().apply { getMonitoreoResult = { Response.success(Fixtures.monitoreo().copy(ciclos = emptyList())) } }
        compose.setContent { BiofixDialog(1, {}, {}, service) }
        compose.waitForIdle()
        for (etiqueta in listOf("Confirmar", "Cancelar")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(etiqueta).performScrollTo().fetchSemanticsNode()
                .config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
            assertEquals("$etiqueta en una sola línea", 1, layouts.single().lineCount)
        }
    }

    @Test fun `muestra guia orientativa de severidad en el modal de biofix`() {
        val service = FakeGDDService().apply {
            getMonitoreoResult = { Response.success(Fixtures.monitoreo().copy(ciclos = emptyList())) }
        }
        compose.setContent { BiofixDialog(1, {}, {}, service) }
        compose.waitForIdle()
        compose.onNodeWithText("Guía orientativa:", substring = true).performScrollTo().assertExists()
    }

    @Test fun `monitoreo finalizado muestra fechas e historial en MonitoreoCard y oculta etiqueta Para confirmar`() {
        val cicloCerrado = cycle.copy(id = 90, estado = "archivado", biofix_pendiente = true)
        val monFinalizado = Fixtures.monitoreo(
            id = 55,
            activo = false,
            fechaInicio = LocalDate.of(2026, 9, 1),
            ciclos = listOf(cicloCerrado)
        ).copy(
            fecha_fin = LocalDate.of(2026, 11, 5),
            observaciones = "Fin de campaña maicera"
        )

        compose.setContent {
            MonitoreoCard(
                monitoreo = monFinalizado,
                onClick = {}
            )
        }
        compose.waitForIdle()

        // En MonitoreoCard finalizado: muestra Inicio, Fin, Ciclos y observaciones, y NO muestra "Para confirmar"
        compose.onNodeWithText("Inicio").assertExists()
        compose.onNodeWithText("Fin").assertExists()
        compose.onNodeWithTag("observacionesMonitoreo_55", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("etiquetaBiofixPendiente_55", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun `monitoreo activo con ciclo listo para nuevo biofix muestra etiqueta Para confirmar en MonitoreoCard`() {
        val cicloListo = cycle.copy(id = 91, estado = "completado", biofix_pendiente = true)
        val monActivo = Fixtures.monitoreo(
            id = 56,
            activo = true,
            ciclos = listOf(cicloListo)
        )

        compose.setContent {
            MonitoreoCard(
                monitoreo = monActivo,
                onClick = {}
            )
        }
        compose.waitForIdle()

        compose.onNodeWithTag("etiquetaBiofixPendiente_56", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Para confirmar").assertExists()
    }
}
