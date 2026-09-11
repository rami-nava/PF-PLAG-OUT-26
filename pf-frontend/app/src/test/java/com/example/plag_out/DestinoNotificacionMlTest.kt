package com.example.plag_out

import com.example.plag_out.fakes.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinoNotificacionMlTest {
    @Test
    fun `alerta ML abre su prediccion`() {
        assertEquals(
            "prediccion/41",
            destinoDe(Fixtures.notificacion(tipo = "ALERTA_ML_RIESGO", entidadId = 41))
        )
        assertEquals(
            "prediccion/41",
            destinoDePush(tipo = "ALERTA_ML_RIESGO", prediccionId = "41")
        )
        assertEquals(
            "prediccion/41",
            destinoDePush(tipo = "ALERTA_ML_RIESGO", entidadId = "41")
        )
    }

    @Test
    fun `tipo desconocido no cae en reportes`() {
        assertNull(destinoDe(Fixtures.notificacion(tipo = "TIPO_FUTURO", entidadId = 41)))
        assertNull(destinoDePush(tipo = "TIPO_FUTURO", entidadId = "41"))
    }
}
