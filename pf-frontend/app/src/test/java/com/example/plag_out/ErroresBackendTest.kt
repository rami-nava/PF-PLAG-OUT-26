package com.example.plag_out

import org.junit.Assert.assertEquals
import org.junit.Test

class ErroresBackendTest {

    @Test
    fun `detail propio del endpoint tiene prioridad`() {
        val error = errorDeCuerpo(
            409, """{"detail": "plantation already active"}""",
            porDetalle = mapOf("plantation already active" to "Ya tenés esta plantación activa.")
        )
        assertEquals("Ya tenés esta plantación activa.", error.mensaje)
    }

    @Test
    fun `422 marca cada campo segun su code`() {
        val error = errorDeCuerpo(
            422,
            """{"detail": "invalid_request", "errors": [
                {"field": "body.nombre", "code": "string_too_short"},
                {"field": "fecha_siembra", "code": "fecha_siembra_future"}]}"""
        )
        assertEquals("Revisá el nombre: no puede quedar en blanco.", error.mensaje)
        assertEquals("La fecha de siembra no puede ser futura.", error.campos["fecha_siembra"])
    }

    @Test
    fun `422 sobre un campo que el usuario no edita no expone el detalle`() {
        val error = errorDeCuerpo(422, """{"detail": "invalid_request", "errors": [{"field": "monitoreo_id", "code": "int_parsing"}]}""")
        assertEquals("No se pudieron procesar los datos. Intentá de nuevo.", error.mensaje)
    }

    @Test
    fun `429 usa Retry-After y tolera el formato de slowapi`() {
        assertEquals(
            "Demasiados intentos, probá de nuevo en 30 segundos.",
            errorDeCuerpo(429, """{"detail": "rate_limit_exceeded"}""", retryAfterSegundos = 30).mensaje
        )
        val slowapi = errorDeCuerpo(429, """{"error": "Rate limit exceeded: 300 per 1 minute"}""")
        assertEquals("Demasiados intentos, probá de nuevo en unos segundos.", slowapi.mensaje)
        assertEquals("Rate limit exceeded: 300 per 1 minute", slowapi.detalle)
    }

    @Test
    fun `cuerpo que no es JSON cae en el mensaje generico`() {
        assertEquals(MENSAJE_ERROR_GENERICO, errorDeCuerpo(500, "<html>Bad Gateway</html>").mensaje)
    }
}
