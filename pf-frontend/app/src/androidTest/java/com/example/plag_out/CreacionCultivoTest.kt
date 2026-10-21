package com.example.plag_out

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.fakes.FakeGDDService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CreacionCultivoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var gddService: FakeGDDService
    private var creado = false

    @Before
    fun setup() {
        gddService = FakeGDDService().apply {
            adminGetPlagasResult = {
                Response.success(
                    listOf(
                        PlagaAdmin(1, "Carpocapsa", "Cydia pomonella", activo = true),
                        PlagaAdmin(2, "Pulgón lanígero", "Eriosoma lanigerum", activo = true),
                        PlagaAdmin(3, "Chicharrita", "Dalbulus maidis", activo = false)
                    )
                )
            }
            adminCrearCultivoResult = {
                Response.success(CultivoAdmin(9, "Manzano", "Malus domestica", activo = true))
            }
        }
        val viewModel = AdminCultivosViewModel(gddService)

        composeRule.setContent {
            CultivoDetalleScreen(
                viewModel = viewModel,
                cultivoId = null,
                onBack = {},
                onCreado = { creado = true },
                onRelacionesCambiadas = {}
            )
        }
    }

    private fun agregarPlagaDesdeBuscador(id: Int) {
        composeRule.onNodeWithTag("btnAgregarPlaga").performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag("itemPlagaBuscador_$id"))
        composeRule.onNodeWithTag("itemPlagaBuscador_$id").performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag("filaPlagaCultivo_$id"))
    }

    @Test
    fun alta_con_dos_plagas_elegidas_desde_el_buscador() {
        composeRule.onNodeWithTag("txtNombreCultivo").performTextInput("Manzano")
        composeRule.onNodeWithTag("txtNombreCientificoCultivo").performTextInput("Malus domestica")

        agregarPlagaDesdeBuscador(1)
        agregarPlagaDesdeBuscador(2)

        // Las dos activas ya están vinculadas y la dada de baja no se ofrece: el buscador queda vacío
        composeRule.onNodeWithTag("btnAgregarPlaga").performClick()
        composeRule.waitUntilExactlyOneExists(hasTestTag("txtBuscarPlagaCultivo"))
        composeRule.onNodeWithText("Todas las plagas activas ya afectan a este cultivo.").assertExists()
        composeRule.onNodeWithTag("itemPlagaBuscador_3").assertDoesNotExist()
    }

    @Test
    fun crear_manda_las_plagas_elegidas() {
        composeRule.onNodeWithTag("txtNombreCultivo").performTextInput("Manzano")
        composeRule.onNodeWithTag("txtNombreCientificoCultivo").performTextInput("Malus domestica")
        agregarPlagaDesdeBuscador(2)
        agregarPlagaDesdeBuscador(1)

        composeRule.onNodeWithTag("btnGuardarCultivo").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { creado }
        assertTrue(creado)
        assertEquals(
            CultivoAdminRequest("Manzano", "Malus domestica", plagas = listOf(1, 2)),
            gddService.ultimoCultivoEnviado
        )
    }
}
