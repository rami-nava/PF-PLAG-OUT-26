package com.example.plag_out

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import org.junit.After


import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Before
import org.junit.Rule

@RunWith(AndroidJUnit4::class)
class CreacionTerrenosTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var terrenoRepository: TerrenoRepository
    private lateinit var nuevoTerrenoViewModel: NuevoTerrenoViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Base de datos en memoria, no persiste y se destruye al terminar el test
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Evita problemas de threading
            .build()

        terrenoRepository = TerrenoRepository(db.terrenoDao())

        nuevoTerrenoViewModel = NuevoTerrenoViewModelFactory(context, terrenoRepository)
            .create(NuevoTerrenoViewModel::class.java)

        composeRule.setContent {
            DatosDelTerrenoScreen(
                nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                onBack = {},
                onContinue = {}
            )
        }
    }


    @Test
    fun datos_de_terreno_validos() {


        // Caso 1: nombre vacío
        composeRule.onNodeWithTag("txtHectareas")
            .performTextClearance()
        composeRule.onNodeWithTag("txtHectareas")
            .performTextInput("10")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 2: hectáreas vacías
        composeRule.onNodeWithTag("txtHectareas")
            .performTextClearance()
        composeRule.onNodeWithTag("txtNombre")
            .performTextClearance()
        composeRule.onNodeWithTag("txtNombre")
            .performTextInput("Lote Norte")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 3: hectáreas no numéricas
        composeRule.onNodeWithTag("txtHectareas")
            .performTextInput("abc")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 4: terreno bien definido
        composeRule.onNodeWithTag("txtHectareas")
            .performTextClearance()
        composeRule.onNodeWithTag("txtNombre")
            .performTextClearance()
        composeRule.onNodeWithTag("txtHectareas")
            .performTextInput("200")
        composeRule.onNodeWithTag("txtNombre")
            .performTextInput("Lote Norte")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsEnabled()
    }

    @After
    fun tearDown() {
        db.close()
    }
}