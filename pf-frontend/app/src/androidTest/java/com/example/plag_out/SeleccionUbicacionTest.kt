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
class SeleccionUbicacionTest {
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
            SelectLocationScreen(
                nuevoTerrenoViewModel = nuevoTerrenoViewModel,
                onBack = {},
                onConfirm = {}
            )
        }
    }


    @Test
    fun datos_de_ubicacion_validos() {


        // Caso 1: longitud vacía
        composeRule.onNodeWithTag("txtLatitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLongitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLatitud")
            .performTextInput("-54")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 2: latitud vacía
        composeRule.onNodeWithTag("txtLatitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLatitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLongitud")
            .performTextInput("-54")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 3: latitud no numerica
        composeRule.onNodeWithTag("txtLatitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLongitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLatitud")
            .performTextInput("sur")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 4: longitud no numerica
        composeRule.onNodeWithTag("txtLatitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLongitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLongitud")
            .performTextInput("norte")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 5: seleccion valida
        composeRule.onNodeWithTag("txtLatitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLongitud")
            .performTextClearance()
        composeRule.onNodeWithTag("txtLongitud")
            .performTextInput("-54.90")
        composeRule.onNodeWithTag("txtLatitud")
            .performTextInput("-54.55")
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsEnabled()


    }

    @After
    fun tearDown() {
        db.close()
    }
}