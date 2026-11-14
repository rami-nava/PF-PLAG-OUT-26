package com.example.plag_out

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TratamientosYAlertasUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun marcoDialogo_avisoErrorSeMuestraEnPrimerPlano() {
        compose.setContent {
            MaterialTheme {
                MarcoDialogo(tag = "dialogTest", onDismissRequest = {}) {
                    Text("Diálogo activo")
                }
            }
        }
        compose.onNodeWithText("Diálogo activo").assertIsDisplayed()
        compose.runOnUiThread {
            Avisos.error("Error visible encima del diálogo")
        }
        compose.waitForIdle()
        compose.onNodeWithText("Error visible encima del diálogo").assertIsDisplayed()
    }

    @Test
    fun avisoSinConexion_reintentarEjecutaCallback() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                AvisoSinConexion(
                    reintentando = false,
                    onReintentar = { clicks++ },
                    tag = "offlineCard",
                    tagBotonReintentar = "offlineRetry"
                )
            }
        }
        compose.onNodeWithTag("offlineCard").assertIsDisplayed()
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithTag("offlineRetry").performClick()
        assertEquals(1, clicks)
    }
}
