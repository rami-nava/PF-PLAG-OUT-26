package com.example.plag_out

import android.app.Application
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.plag_out.fakes.Fixtures
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], application = Application::class)
class CicloHitoTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `la tarjeta de un pulgón dice En desarrollo y no En huevo`() {
        val ciclo = Fixtures.ciclo(estadioBiologico = "Ninfa", nivelAlerta = 0, progreso = 10f, hito = "generacion")
        compose.setContent { MaterialTheme { CicloCard(ciclo, numero = 1) } }

        compose.onNodeWithText("En desarrollo").assertIsDisplayed()
        compose.onNodeWithText("En huevo").assertDoesNotExist()
    }

    @Test
    fun `la tarjeta de una plaga con huevo no cambia`() {
        val ciclo = Fixtures.ciclo(nivelAlerta = 1, progreso = 80f, hito = "eclosion")
        compose.setContent { MaterialTheme { CicloCard(ciclo, numero = 1) } }

        compose.onNodeWithText("Eclosión cerca").assertIsDisplayed()
    }

    @Test
    fun `el monitoreo toma el hito de sus ciclos activos`() {
        val activo = Fixtures.ciclo(id = 2, hito = "generacion")
        val finalizado = Fixtures.ciclo(id = 1, estado = "completado", hito = "eclosion")
        assertEquals("generacion", hitoDeCiclos(Fixtures.monitoreo(ciclos = listOf(finalizado, activo))))
        assertNull(hitoDeCiclos(Fixtures.monitoreo(ciclos = emptyList())))
    }

    @Test
    fun `el ciclo lee el hito del backend y tolera que falte`() {
        val conHito = """{"id":1,"monitoreo_id":1,"fecha_biofix":"2026-11-04","estado":"activo",
            "fecha_actualizacion":"2026-11-04","gdd_acumulado":0,"gdd_diario":0,"gdd_eclosion":148,
            "gdd_generacion":148,"progreso":0,"estadio_biologico":"Ninfa","nivel_alerta":0,
            "dias_pendientes":0,"hito":"generacion"}"""
        assertEquals("generacion", Gson().fromJson(conHito, GddCicloResponse::class.java).hito)
        val sinHito = conHito.replace(",\"hito\":\"generacion\"", "")
        assertNull(Gson().fromJson(sinHito, GddCicloResponse::class.java).hito)
    }
}
