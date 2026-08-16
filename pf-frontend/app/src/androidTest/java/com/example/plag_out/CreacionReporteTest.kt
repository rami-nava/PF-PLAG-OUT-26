package com.example.plag_out

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

/**
 * Los pasos del formulario de reporte se habilitan en cadena: sin terreno no hay
 * plantación, sin plantación no hay plaga, y sin plaga no hay etapa biológica.
 */
@RunWith(AndroidJUnit4::class)
class CreacionReporteTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var crearReporteViewModel: CrearReporteViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val terreno = Fixtures.terreno(id = 1, nombre = "Lote Norte")

        // Dos plantaciones activas a propósito: con una sola el ViewModel la elegiría
        // solo y no se podría verificar que la plaga queda bloqueada tras el terreno.
        val plantacionTrigo = Fixtures.plantacion(
            id = 1, terrenoId = 1, terrenoNombre = "Lote Norte",
            cultivoId = 1, cultivo = "Trigo"
        )
        val plantacionMaiz = Fixtures.plantacion(
            id = 2, terrenoId = 1, terrenoNombre = "Lote Norte",
            cultivoId = 2, cultivo = "Maíz", cultivoCientifico = "Zea Mays"
        )

        val plagaDelTrigo = Fixtures.plaga(
            id = 1,
            nombre = "Roya amarilla",
            nombreCientifico = "Puccinia striiformis",
            cultivosAfectados = listOf(1)
        )

        // Plaga de otro cultivo: no debe ofrecerse al elegir la plantación de trigo.
        val plagaDelMaiz = Fixtures.plaga(
            id = 2,
            nombre = "Gusano cogollero",
            nombreCientifico = "Spodoptera Frugiperda",
            cultivosAfectados = listOf(2)
        )

        val gddService = FakeGDDService().apply {
            getTerrenosResult = { Response.success(listOf(terreno)) }
            getPlantacionesResult = { Response.success(listOf(plantacionTrigo, plantacionMaiz)) }
            getPlagasResult = { Response.success(listOf(plagaDelTrigo, plagaDelMaiz)) }
        }

        crearReporteViewModel = CrearReporteViewModel(context, gddService)

        composeRule.setContent {
            CrearReporteScreen(
                viewModel = crearReporteViewModel,
                onBack = {},
                onSuccess = { _, _ -> }
            )
        }
    }

    @Test
    fun pasos_del_reporte_se_habilitan_en_cadena() {

        // Caso 1: al entrar, todo lo que depende de una selección previa está bloqueado
        composeRule.onNodeWithTag("txtPlantacionReporte").assertIsNotEnabled()
        composeRule.onNodeWithTag("txtTipoPlaga").assertIsNotEnabled()
        composeRule.onNodeWithTag("txtEtapaBiologica").assertIsNotEnabled()
        composeRule.onNodeWithTag("btnGuardarReporte").assertIsNotEnabled()

        // Caso 2: con el terreno elegido la plaga sigue bloqueada
        composeRule.onNodeWithTag("txtTerrenoReporte").performClick()
        composeRule.onNodeWithText("Lote Norte").performClick()
        composeRule.onNodeWithTag("txtTipoPlaga").assertIsNotEnabled()
        composeRule.onNodeWithTag("btnGuardarReporte").assertIsNotEnabled()

        // Caso 3: recién con la plantación elegida se habilita la plaga
        composeRule.onNodeWithTag("txtPlantacionReporte").performClick()
        composeRule.onNodeWithText("Trigo").performClick()
        composeRule.onNodeWithTag("txtTipoPlaga").assertIsEnabled()
        composeRule.onNodeWithTag("btnGuardarReporte").assertIsNotEnabled()

        // Caso 4: las plagas ofrecidas son solo las del cultivo de la plantación
        composeRule.onNodeWithTag("txtTipoPlaga").performClick()
        composeRule.onNodeWithText("Gusano cogollero").assertDoesNotExist()

        // Caso 5: elegida la plaga, la etapa se autocompleta y el formulario queda válido
        composeRule.onNodeWithText("Roya amarilla").performClick()
        composeRule.onNodeWithTag("txtEtapaBiologica").assertIsEnabled()
        composeRule.onNodeWithTag("btnGuardarReporte").assertIsEnabled()
    }
}
