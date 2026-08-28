package com.example.plag_out

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.AlmacenamientoLocal.PlantacionRepository
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.fakes.FakeGDDService
import kotlinx.coroutines.runBlocking
import org.junit.After


import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Before
import org.junit.Rule
import retrofit2.Response
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class CreacionMonitoreoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var monitoreoRepository: MonitoreoRepository
    private lateinit var plantacionRepository: PlantacionRepository
    private lateinit var terrenoRepository: TerrenoRepository
    private lateinit var agregarMonitoreoViewModel: AgregarMonitoreoViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Base de datos en memoria, no persiste y se destruye al terminar el test
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Evita problemas de threading
            .build()

        monitoreoRepository = MonitoreoRepository(db.monitoreoDao())
        plantacionRepository = PlantacionRepository(db.plantacionDao())
        terrenoRepository = TerrenoRepository(db.terrenoDao())

        val terreno = TerrenoResponse(
            terreno_id = 1,
            terreno_nombre = "Lote Norte",
            terreno_latitud = (-54.0).toFloat(),
            terreno_longitud = (-54.66).toFloat(),
            terreno_area = 200.toFloat()
        )

        val trigo = CultivoResponse(id = 1, nombre = "Trigo", nombre_cientifico = "Triticum Aestivum")
        val maiz = CultivoResponse(id = 2, nombre = "Maíz", nombre_cientifico = "Zea Mays")

        val plantacion = PlantacionesResponse(
            plantacion_id = 1,
            terreno_id = 1,
            terreno_nombre = "Lote Norte",
            cultivo_id = trigo.id,
            cultivo_nombre = trigo.nombre,
            cultivo_nombre_cientifico = trigo.nombre_cientifico,
            fecha_siembra = LocalDate.of(2022,12,18),
            activa = true
        )

        val plantacion2 = PlantacionesResponse(
            plantacion_id = 2,
            terreno_id = 1,
            terreno_nombre = "Lote Norte",
            cultivo_id = 3,
            cultivo_nombre = "Soja",
            cultivo_nombre_cientifico = "Glycine Max",
            fecha_siembra = LocalDate.of(2022,12,18),
            activa = false
        )

        runBlocking {
            terrenoRepository.guardarTerreno(terreno)
            plantacionRepository.guardarPlantacion(plantacion)
            plantacionRepository.guardarPlantacion(plantacion2)
        }

        val plagaDelTrigo = PlagaResponse(
            id = 1,
            nombre = "Chicharrita",
            nombre_cientifico = "Dalbus Maidis",
            cultivos_afectados = listOf(trigo.id)
        )

        // Plaga de otro cultivo: no debe ofrecerse al elegir el cultivo de trigo.
        val plagaDelMaiz = PlagaResponse(
            id = 2,
            nombre = "Gusano cogollero",
            nombre_cientifico = "Spodoptera Frugiperda",
            cultivos_afectados = listOf(maiz.id)
        )

        // La pantalla llama a cargarPlagas() en su LaunchedEffect: el fake devuelve la
        // plaga esperada, así el test pasa porque los datos son correctos, no porque la
        // red falle.
        val gddService = FakeGDDService().apply {
            getPlagasResult = { Response.success(listOf(plagaDelTrigo, plagaDelMaiz)) }
        }

        agregarMonitoreoViewModel = AgregarMonitoreoViewModelFactory(
            context, monitoreoRepository, plantacionRepository, terrenoRepository, gddService
        ).create(AgregarMonitoreoViewModel::class.java)

        composeRule.setContent {
            AgregarMonitoreoScreen(
                agregarMonitoreoViewModel,
                onBack = {},
                onSuccess = {}
            )
        }
    }


    @Test
    fun datos_de_monitoreo_validos() {

        // Caso 1: los pasos se habilitan en orden, cada uno depende del anterior
        composeRule.onNodeWithTag("txtPlantacion")
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("txtPlaga")
            .assertIsNotEnabled()

        // Caso 2: solo especifica terreno
        composeRule.onNodeWithTag("txtTerreno")
            .performClick()
        composeRule.onNodeWithText("Lote Norte")
            .performClick()
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()
        composeRule.onNodeWithTag("txtPlaga")
            .assertIsNotEnabled()

        //Caso 3: solo pueden crearse monitoreos de cultivos activos
        composeRule.onNodeWithTag("txtPlantacion").performClick()
        composeRule.onNodeWithText("Trigo")
            .assertExists()
        composeRule.onNodeWithText("Soja")
            .assertDoesNotExist()

        // Caso 4: solo especifica terreno y plantacion
        composeRule.onNodeWithText("Trigo")
            .performClick()
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsNotEnabled()

        // Caso 5: las plagas ofrecidas son solo las del cultivo deel cultivo
        composeRule.onNodeWithTag("txtPlaga")
            .performClick()
        composeRule.onNodeWithText("Gusano cogollero")
            .assertDoesNotExist()

        // Caso 6: valido
        composeRule.onNodeWithText("Chicharrita")
            .performClick()
        composeRule.onNodeWithTag("btnGuardar")
            .assertIsEnabled()
    }

    @Test
    fun la_i_del_umbral_explica_que_es_antes_de_crear_el_monitoreo() {
        composeRule.onNodeWithTag("btnInfoUmbral").performClick()

        composeRule.onNodeWithTag("hojaUmbralRiesgo").assertExists()
    }

    @After
    fun tearDown() {
        db.close()
    }
}