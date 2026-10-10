package com.example.plag_out

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.*
import com.example.plag_out.fakes.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlertasMlScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `alerta automatica se puede silenciar sin mostrar un editor de porcentajes`() {
        val monitoring = Fixtures.monitoreo(modeloAlertaMlId = "model", umbralAlertaMlRecomendado = 19.76f)
            .copy(alertas_ml_activas = true, horizonte_alerta_ml_dias = 14)
        var requested: Boolean? = null
        compose.setContent {
            var current by remember { mutableStateOf(monitoring) }
            AlertasMlCard(current, guardando = false, editable = true) { value ->
                requested = value
                current = current.copy(alertas_ml_activas = value)
            }
        }
        compose.onNodeWithTag("switchAlertasMl").assertIsOn().performClick()
        compose.onNodeWithTag("switchAlertasMl").assertIsOff()
        assertEquals(false, requested)
        compose.onNodeWithTag("sliderUmbralMl").assertDoesNotExist()
        compose.onNodeWithText("Alerta de brote severo").assertExists()
        compose.onNodeWithText("El modelo estima capturas elevadas a 14 días.", substring = true).assertExists()
    }

    @Test
    fun `preferencia ausente no ofrece un control activo en clientes de backend anterior`() {
        compose.setContent { AlertasMlCard(Fixtures.monitoreo(modeloAlertaMlId = "model"), false, true) {} }
        compose.onNodeWithTag("switchAlertasMl").assertIsNotEnabled()
        compose.onNodeWithText("Actualizá el monitoreo para consultar esta preferencia.").assertExists()
    }
}
