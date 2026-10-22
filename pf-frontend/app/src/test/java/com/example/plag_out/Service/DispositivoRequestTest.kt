package com.example.plag_out.Service

import com.example.plag_out.DispositivoRequest
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Test

class DispositivoRequestTest {
    @Test
    fun `el dispositivo se registra por su installation id y no manda el token viejo`() {
        val json = Gson().toJson(DispositivoRequest("cX9kQ2Lm3nP4rS5tU6vW7x", "android"))

        // El backend exige exactamente uno de fid o fcm_token (DispositivoRequest en api/schemas.py).
        assertEquals(
            JsonParser.parseString("""{"fid":"cX9kQ2Lm3nP4rS5tU6vW7x","plataforma":"android"}"""),
            JsonParser.parseString(json)
        )
    }
}
