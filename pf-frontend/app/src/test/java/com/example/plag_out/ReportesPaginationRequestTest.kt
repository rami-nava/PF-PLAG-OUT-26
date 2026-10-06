package com.example.plag_out

import com.example.plag_out.Service.GDDService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ReportesPaginationRequestTest {
    @Test
    fun `retrofit envia limites offset y el mismo rango temporal`() = runBlocking {
        var request: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            request = chain.request()
            okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("[]".toResponseBody()).build()
        }.build()
        val service = Retrofit.Builder().baseUrl("https://example.test/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(GDDService::class.java)
        service.getReportes("2026-09-01T00:00:00Z", "2026-09-30T23:59:59.999999999Z", limit = 100, offset = 100, distanciaKm = 50)
        val sent = requireNotNull(request)
        assertEquals("/reportes", sent.url.encodedPath)
        assertEquals("100", sent.url.queryParameter("limit"))
        assertEquals("100", sent.url.queryParameter("offset"))
        assertEquals("2026-09-01T00:00:00Z", sent.url.queryParameter("fecha_desde"))
        assertEquals("2026-09-30T23:59:59.999999999Z", sent.url.queryParameter("fecha_hasta"))
        assertEquals("50", sent.url.queryParameter("distancia_km"))
        service.getReportes()
        assertNull(requireNotNull(request).url.queryParameter("distancia_km"))
    }
}
