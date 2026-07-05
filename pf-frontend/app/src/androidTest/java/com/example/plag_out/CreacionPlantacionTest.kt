package com.example.plag_out

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import kotlinx.coroutines.runBlocking
import org.junit.After


import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Before
import org.junit.Rule
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class CreacionPlantacionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var monitoreoRepository: MonitoreoRepository
    private lateinit var plantacionRepository: PlantacionRepository
    private lateinit var terrenoRepository: TerrenoRepository
    private lateinit var agregarPlantacionViewModel: AgregarPlantacionViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Base de datos en memoria, no persiste y se destruye al terminar el test
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Evita problemas de threading
            .build()

        plantacionRepository = PlantacionRepository(db.plantacionDao())
        terrenoRepository = TerrenoRepository(db.terrenoDao())

        val terreno = TerrenoResponse(
            terreno_id = 1,
            terreno_nombre = "Lote Norte",
            terreno_latitud = (-54.0).toFloat(),
            terreno_longitud = (-54.66).toFloat(),
            terreno_area = 200.toFloat()
        )

        runBlocking {
            terrenoRepository.guardarTerreno(terreno)
        }

        agregarPlantacionViewModel = AgregarPlantacionViewModelFactory(context, plantacionRepository, terrenoRepository)
            .create(AgregarPlantacionViewModel::class.java)

        val cultivo = CultivoResponse(
            id = 1,
            nombre = "Trigo",
            nombre_cientifico = "Triticum Aestivum"
        )

        agregarPlantacionViewModel.actualizarCultivo(cultivo)

        composeRule.setContent {
            AgregarPlantacionScreen(
                1,
                agregarPlantacionViewModel,
                onBack = {},
                onSuccess = {}
            )
        }
    }


    @Test
    fun datos_de_plantacion_validos() {

        // Caso 1: solo especifica cultivo
        composeRule.onNodeWithTag("txtCultivo")
            .performClick()
        composeRule.onNodeWithText("Trigo")
            .performClick()
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 2: valido
        agregarPlantacionViewModel.seleccionarFecha(LocalDate.of(2026,1,1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsEnabled()
    }

    @After
    fun tearDown() {
        db.close()
    }
}