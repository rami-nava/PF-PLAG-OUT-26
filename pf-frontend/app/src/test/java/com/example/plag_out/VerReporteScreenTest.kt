package com.example.plag_out

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VerReporteScreenTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun `el detalle muestra el cultivo y la etapa biologica`() {
        val gddService = FakeGDDService().apply {
            getReporteResult = {
                Response.success(
                    Fixtures.reporteDetalle(id = 78).copy(cultivo_nombre = "Maíz", etapa_biologica = "HUEVO")
                )
            }
        }

        compose.setContent {
            MaterialTheme {
                VerReporteScreen(
                    reporteId = 78,
                    reporteJsonFallback = null,
                    viewModel = VerReporteViewModel(gddService),
                    onBack = {},
                    onVerTerreno = {}
                )
            }
        }

        compose.onNodeWithText("Maíz").performScrollTo().assertExists()
        compose.onNodeWithText("HUEVO").performScrollTo().assertExists()
    }
}
