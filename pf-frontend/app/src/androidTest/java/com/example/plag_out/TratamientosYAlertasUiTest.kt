package com.example.plag_out

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TratamientosYAlertasUiTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun marcoDialogo_avisoErrorSeMuestraEnPrimerPlano() {
        compose.setContent {
            MaterialTheme {
                MarcoDialogo(tag = "dialogTest", onDismissRequest = {}) {
                    Text("Diálogo activo")
                }
            }
        }
        compose.onNodeWithText("Diálogo activo").assertIsDisplayed()
        compose.runOnUiThread {
            Avisos.error("Error visible encima del diálogo")
        }
        compose.waitForIdle()
        compose.onNodeWithText("Error visible encima del diálogo").assertIsDisplayed()
    }

    @Test
    fun avisoSinConexion_reintentarEjecutaCallback() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                AvisoSinConexion(
                    reintentando = false,
                    onReintentar = { clicks++ },
                    tag = "offlineCard",
                    tagBotonReintentar = "offlineRetry"
                )
            }
        }
        compose.onNodeWithTag("offlineCard").assertIsDisplayed()
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithTag("offlineRetry").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun terrenoScreen_responsiveEnPantallaChicaNoCortaEtiquetasNiAnillo() {
        val terreno1 = TerrenoResponse(
            terreno_id = 1,
            terreno_nombre = "Lote Norte",
            terreno_latitud = -26.50f,
            terreno_longitud = -64.74f,
            terreno_area = 100f
        )
        val terreno2 = TerrenoResponse(
            terreno_id = 2,
            terreno_nombre = "Establecimiento La Esperanza",
            terreno_latitud = -26.50f,
            terreno_longitud = -64.74f,
            terreno_area = 180f
        )
        val cicloPulgon = GddCicloResponse(
            id = 1,
            monitoreo_id = 1,
            fecha_biofix = "2026-11-10",
            estado = "activo",
            fecha_actualizacion = "2026-11-14",
            gdd_acumulado = 10f,
            gdd_diario = 5f,
            gdd_eclosion = 100f,
            gdd_generacion = 100f,
            progreso = 0.1f,
            estadio_biologico = "Ninfa",
            nivel_alerta = 0,
            dias_pendientes = 0,
            hito = "generacion"
        )
        val cicloOruga = GddCicloResponse(
            id = 2,
            monitoreo_id = 2,
            fecha_biofix = "2026-11-10",
            estado = "activo",
            fecha_actualizacion = "2026-11-14",
            gdd_acumulado = 95f,
            gdd_diario = 5f,
            gdd_eclosion = 100f,
            gdd_generacion = 200f,
            progreso = 0.95f,
            estadio_biologico = "Larva",
            nivel_alerta = 2,
            dias_pendientes = 0,
            hito = "eclosion"
        )
        val m1 = MonitoreoResponse(
            monitoreo_id = 1,
            plantacion_id = 1,
            terreno_id = 1,
            terreno_nombre = terreno1.terreno_nombre,
            cultivo_nombre = "Trigo",
            plaga_id = 1,
            plaga_nombre = "Pulgón",
            gdd_objetivo = 100f,
            activo = true,
            ciclos = listOf(cicloPulgon)
        )
        val m2 = MonitoreoResponse(
            monitoreo_id = 2,
            plantacion_id = 2,
            terreno_id = 2,
            terreno_nombre = terreno2.terreno_nombre,
            cultivo_nombre = "Maíz",
            plaga_id = 2,
            plaga_nombre = "Oruga",
            gdd_objetivo = 100f,
            activo = true,
            ciclos = listOf(cicloOruga)
        )
        val m3 = m2.copy(monitoreo_id = 3)
        val todosMonitoreos = listOf(m1, m2, m3)
        compose.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(baseDensity.density, fontScale = 1.15f)
            ) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        Column {
                            PanelDeCampoTerrenos(
                                terrenos = listOf(terreno1, terreno2),
                                monitoreos = todosMonitoreos
                            )
                            TerrenoCard(
                                terreno = terreno2,
                                monitoreos = listOf(m2, m3),
                                plantacionesActivas = 2,
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        // "En desarrollo" y "Eclosión cerca" no deben partirse en múltiples renglones
        // (1 renglón con fontScale=1.15f mide ~27.7.dp; 2 renglones superarían ~50.dp)
        val hDesarrollo = compose.onNodeWithText("En desarrollo", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot().height
        val hEclosion = compose.onNodeWithText("Eclosión cerca", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot().height
        val hAnillo = compose.onNodeWithText("monitoreos\nactivos", useUnmergedTree = true).assertIsDisplayed().getUnclippedBoundsInRoot().height
        assertTrue("En desarrollo se partió en varios renglones ($hDesarrollo)", hDesarrollo <= 34.dp)
        assertTrue("Eclosión cerca se partió en varios renglones ($hEclosion)", hEclosion <= 34.dp)
        assertTrue("monitoreos\\nactivos se partió en 3 renglones ($hAnillo)", hAnillo <= 30.dp)
    }
}
