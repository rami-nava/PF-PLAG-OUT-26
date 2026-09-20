package com.example.plag_out

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.plag_out.AlmacenamientoLocal.AppDatabase
import com.example.plag_out.AlmacenamientoLocal.MonitoreoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate


@RunWith(AndroidJUnit4::class)
class EclosionMonitoreoTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: AppDatabase

    @After
    fun tearDown() {
        if (::db.isInitialized) db.close()
    }

    private fun montar(monitoreo: MonitoreoResponse) {
        val context = ApplicationProvider.getApplicationContext<Context>()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = MonitoreoRepository(db.monitoreoDao())
        runBlocking { repository.guardarMonitoreo(monitoreo) }

        // 404 a propósito: la pantalla tiene que servir el caché de Room igual que en el resto de
        // los tests de detalle.
        val gddService = FakeGDDService()
        gddService.getMonitoreoResult = { FakeGDDService.errorServidor(404) }

        val viewModel = MonitoreoDetalleViewModelFactory(context, repository, gddService)
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
    fun activo_y_sin_llegar_al_objetivo_no_muestra_la_fila() {
        montar(Fixtures.monitoreo(activo = true, progreso = 78f, fechaEclosion = null))

        composeRule.onNodeWithTag("txtEstadoEclosion").assertDoesNotExist()
    }

    @Test
    fun al_cien_por_ciento_sin_fecha_avisa_que_no_quedo_registrada() {
        montar(Fixtures.monitoreo(progreso = 100f, gddAcumulado = 500f, fechaEclosion = null))

        composeRule.onNodeWithText("Eclosión alcanzada").assertExists()
        composeRule.onNodeWithText("Fecha no registrada").assertExists()
    }

    @Test
    fun con_fecha_la_muestra_formateada_bajo_el_estado() {
        montar(
            Fixtures.monitoreo(
                progreso = 100f,
                gddAcumulado = 500f,
                fechaEclosion = LocalDate.of(2026, 3, 14)
            )
        )

        composeRule.onNodeWithText("Eclosión alcanzada").assertExists()
        composeRule.onNodeWithText("Fecha no registrada").assertDoesNotExist()
        composeRule.onNodeWithTag("txtFechaEclosion").assertExists()
    }

    @Test
    fun finalizado_sin_llegar_al_objetivo_dice_que_no_se_alcanzo() {
        montar(
            Fixtures.monitoreo(activo = false, progreso = 62f, gddAcumulado = 310f, fechaEclosion = null)
        )

        composeRule.onNodeWithText("Eclosión no alcanzada").assertExists()
    }
}
