package com.example.plag_out

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.AlmacenamientoLocal.FeedbackPrediccionRepository
import com.example.plag_out.fakes.FakeFeedbackPrediccionDao
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class PrediccionDetalleScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun muestra_datos_y_las_tres_respuestas() {
        val service = FakeGDDService().apply {
            getPrediccionResult = { Response.success(Fixtures.prediccion()) }
        }
        val viewModel = PrediccionDetalleViewModel(
            FeedbackPrediccionRepository(FakeFeedbackPrediccionDao()),
            service,
            ownerIdProvider = { "11111111-1111-1111-1111-111111111111" }
        )

        composeRule.setContent {
            PrediccionDetalleScreen(41, viewModel, onBack = {})
        }

        composeRule.waitUntil { viewModel.state.value.prediccion != null }
        composeRule.onNodeWithText("30,00%").assertIsDisplayed()
        composeRule.onNodeWithText("Sí, está presente").assertIsDisplayed()
        composeRule.onNodeWithText("Revisé y no la observé").assertIsDisplayed()
        composeRule.onNodeWithText("No pude verificar").assertIsDisplayed()
    }
}
