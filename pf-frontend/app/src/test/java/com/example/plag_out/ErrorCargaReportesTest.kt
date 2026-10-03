package com.example.plag_out

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ErrorCargaReportesTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `error de carga se muestra y permite reintentar en lista o mapa`() {
        var reintentos = 0
        val mensaje = "No se pudieron cargar todos los reportes. Reintentá."
        compose.setContent { MaterialTheme { ErrorCargaReportes(mensaje, { reintentos++ }) } }
        compose.onNodeWithText(mensaje).assertIsDisplayed()
        compose.onNodeWithTag("reintentarCargaReportes").performClick()
        assertEquals(1, reintentos)
    }
}
