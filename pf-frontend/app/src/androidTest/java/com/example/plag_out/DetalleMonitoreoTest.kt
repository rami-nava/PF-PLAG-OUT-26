package com.example.plag_out

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class DetalleMonitoreoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var monitoreoRepository: MonitoreoRepository
    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: MonitoreoDetalleViewModel

    private val monitoreo = Fixtures.monitoreo(id = 1, progreso = 78f, nivelAlerta = 2, umbralRiesgo = 80)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        monitoreoRepository = MonitoreoRepository(db.monitoreoDao())

        runBlocking { monitoreoRepository.guardarMonitoreo(monitoreo) }

        // El caché de Room ya sirve el monitoreo al instante. El GET se declara con un 404
        // a propósito, para simular que el endpoint todavía no existe en el backend: la
        // pantalla tiene que degradar con gracia al caché, igual que el resto de la app.
        // (Dejarlo sin declarar no sirve: el fake lanza AssertionError, que no es Exception
        // y por lo tanto escapa del catch del ViewModel)
        gddService = FakeGDDService()
        gddService.getMonitoreoResult = { FakeGDDService.errorServidor(404) }

        viewModel = MonitoreoDetalleViewModelFactory(context, monitoreoRepository, gddService)
            .create(MonitoreoDetalleViewModel::class.java)

        composeRule.setContent {
            MonitoreoDetalleScreen(
                monitoreoId = monitoreo.monitoreo_id,
                viewModel = viewModel,
                onBack = {},
                onFinalizado = {},
                onVerPlantacion = {},
                onVerTerreno = {}
            )
        }
    }

    @Test
    fun muestra_el_porcentaje_de_riesgo_del_monitoreo() {
        composeRule.onNodeWithTag("anilloRiesgo").assertExists()
        composeRule.onNodeWithText("78%").assertExists()
    }

    @Test
    fun editar_umbral_abre_la_hoja_mueve_el_slider_y_guarda() {
        val actualizado = monitoreo.copy(umbral_riesgo = 60)
        gddService.actualizarMonitoreoResult = { Response.success(actualizado) }

        composeRule.onNodeWithTag("btnEditarUmbral").performClick()
        composeRule.onNodeWithTag("sheetUmbral").assertExists()

        composeRule.onNodeWithTag("sliderUmbralRiesgo").performClick()
        composeRule.onNodeWithTag("btnGuardarUmbral").performClick()

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sheetUmbral").assertDoesNotExist()
    }

    @Test
    fun la_i_del_umbral_abre_su_explicacion_sin_abrir_el_editor() {
        composeRule.onNodeWithTag("btnInfoUmbral").performClick()

        composeRule.onNodeWithTag("hojaUmbralRiesgo").assertExists()
        // La "i" no puede disparar la acción de la tarjeta: son dos gestos distintos.
        composeRule.onNodeWithTag("sheetUmbral").assertDoesNotExist()
    }

    @Test
    fun la_i_del_nivel_de_alerta_abre_su_explicacion() {
        composeRule.onNodeWithTag("btnInfoNivelAlerta").performClick()

        composeRule.onNodeWithTag("hojaNivelAlerta").assertExists()
    }

    @Test
    fun finalizar_muestra_dialogo_y_cancelar_no_finaliza() {
        composeRule.onNodeWithTag("btnFinalizarMonitoreo").performClick()
        composeRule.onNodeWithTag("dialogFinalizar").assertExists()

        composeRule.onNodeWithText("Cancelar").performClick()

        composeRule.onNodeWithTag("dialogFinalizar").assertDoesNotExist()
        // Al cancelar, no se disparó el PATCH y el botón de finalizar sigue disponible.
        composeRule.onNodeWithTag("btnFinalizarMonitoreo").assertExists()
        assert(gddService.vecesLlamado("actualizarMonitoreo") == 0)
    }

    @After
    fun tearDown() {
        db.close()
    }
}
