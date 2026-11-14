package com.example.plag_out

import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineYAlertasHardeningTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun hayRed_sinPermisoConnectivityManager_retornaTruePorFallback() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val ctx = object : ContextWrapper(base) {
            override fun getSystemService(name: String): Any? {
                if (name == Context.CONNECTIVITY_SERVICE) throw SecurityException("no perm")
                return super.getSystemService(name)
            }
        }
        assertTrue(hayRed(ctx))
    }

    @Test
    fun hayRed_sinRedActiva_retornaFalse() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val cm = ctx.getSystemService(ConnectivityManager::class.java)
        Shadows.shadowOf(cm).setDefaultNetworkActive(false)
        assertFalse(hayRed(ctx))
    }

    @Test
    fun avisoSinConexion_muestraTextoYDisparaReintentar() {
        var reintentos = 0
        compose.setContent {
            MaterialTheme {
                AvisoSinConexion(
                    reintentando = false,
                    onReintentar = { reintentos++ },
                    tag = "bannerOffline",
                    tagBotonReintentar = "btnReintentarOffline",
                    detalle = "Trabajando con caché local"
                )
            }
        }
        compose.onNodeWithTag("bannerOffline").assertIsDisplayed()
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithText("Trabajando con caché local").assertIsDisplayed()
        compose.onNodeWithTag("btnReintentarOffline").performClick()
        assertEquals(1, reintentos)
    }

    @Test
    fun avisoSinConexion_reintentando_ocultaBotonReintentar() {
        compose.setContent {
            MaterialTheme {
                AvisoSinConexion(
                    reintentando = true,
                    onReintentar = {},
                    tag = "bannerOffline",
                    tagBotonReintentar = "btnReintentarOffline"
                )
            }
        }
        compose.onNodeWithTag("bannerOffline").assertIsDisplayed()
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
        compose.onNodeWithTag("btnReintentarOffline").assertDoesNotExist()
    }

    @Test
    fun marcoDialogo_muestraAvisoDeErrorEnPrimerPlanoDentroDelDialogo() {
        compose.setContent {
            MaterialTheme {
                MarcoDialogo(tag = "dialogPrueba", onDismissRequest = {}) {
                    Text("Contenido del diálogo")
                }
            }
        }
        compose.onNodeWithText("Contenido del diálogo").assertIsDisplayed()
        compose.runOnUiThread {
            Avisos.error("No se pudo guardar sin conexión")
        }
        compose.waitForIdle()
        compose.onNodeWithText("No se pudo guardar sin conexión").assertIsDisplayed()
    }

    @Test
    fun validarTratamiento_casosExtremosYReglasDeFechas() {
        val hoy = LocalDate.of(2026, 11, 14)
        val inicio = hoy.minusDays(10)

        // Hoy + conteo posterior => debe rechazar fecha posterior
        val formHoyConDespues = FormularioTratamiento.nuevo(hoy).copy(
            producto = "Clorantraniliprol",
            conteoAntes = "5",
            conDespues = true,
            conteoDespues = "1",
            fechaConteoDespues = hoy
        )
        val errHoy = validarTratamiento(formHoyConDespues, inicio, hoy)
        assertTrue(errHoy.containsKey(CampoTratamiento.FECHA_CONTEO_DESPUES))

        // Ayer + conteo posterior hoy => válido
        val formAyerValido = FormularioTratamiento.nuevo(hoy)
            .conFechaAplicacion(hoy.minusDays(1))
            .copy(
                producto = "Clorantraniliprol",
                conteoAntes = "5",
                conDespues = true,
                conteoDespues = "1",
                fechaConteoDespues = hoy
            )
        assertTrue(validarTratamiento(formAyerValido, inicio, hoy).isEmpty())

        // Límites de conteo, texto vacío y longitud > 120
        val formInvalido = formAyerValido.copy(
            producto = "   ",
            conteoAntes = "-5",
            fechaConteoAntes = hoy, // posterior a aplicación (ayer)
            fechaAplicacion = inicio.minusDays(1)
        )
        val errLimites = validarTratamiento(formInvalido, inicio, hoy)
        assertTrue(errLimites.containsKey(CampoTratamiento.PRODUCTO))
        assertTrue(errLimites.containsKey(CampoTratamiento.CONTEO_ANTES))
        assertTrue(errLimites.containsKey(CampoTratamiento.FECHA_CONTEO_ANTES))
        assertTrue(errLimites.containsKey(CampoTratamiento.FECHA_APLICACION))

        assertEquals(12.5, leerConteo(" 12,5 ")!!, 0.001)
        assertEquals(0.0, leerConteo("0")!!, 0.001)
        assertEquals(100_000.0, leerConteo("100000")!!, 0.001)
        assertNull(leerConteo("-1"))
        assertNull(leerConteo("100000.1"))
        assertNull(leerConteo("abc"))
    }
}
