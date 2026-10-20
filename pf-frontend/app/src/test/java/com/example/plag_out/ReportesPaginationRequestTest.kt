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
    fun `retrofit envia ambito filtros paginacion y el mismo rango temporal`() = runBlocking {
        var request: Request? = null
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            request = chain.request()
            val cuerpo = if (chain.request().url.encodedPath.endsWith("/resumen")) "{}" else """{"items":[],"total":0}"""
            okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body(cuerpo.toResponseBody()).build()
        }.build()
        val service = Retrofit.Builder().baseUrl("https://example.test/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(GDDService::class.java)

        val pagina = service.getReportes(
            ambito = "comunidad",
            fechaDesde = "2026-09-01T00:00:00Z",
            fechaHasta = "2026-09-30T23:59:59.999999999Z",
            limit = 20,
            offset = 40,
            distanciaKm = 50,
            severidad = "Alto",
            plagaId = 7
        )
        val sent = requireNotNull(request)
        assertEquals("/reportes", sent.url.encodedPath)
        assertEquals("comunidad", sent.url.queryParameter("ambito"))
        assertEquals("20", sent.url.queryParameter("limit"))
        assertEquals("40", sent.url.queryParameter("offset"))
        assertEquals("2026-09-01T00:00:00Z", sent.url.queryParameter("fecha_desde"))
        assertEquals("2026-09-30T23:59:59.999999999Z", sent.url.queryParameter("fecha_hasta"))
        assertEquals("50", sent.url.queryParameter("distancia_km"))
        assertEquals("Alto", sent.url.queryParameter("severidad"))
        assertEquals("7", sent.url.queryParameter("plaga_id"))
        assertNull(sent.url.queryParameter("terreno_id"))
        assertEquals(0, pagina.body()?.total)

        service.getReportes(ambito = "propios", terrenoId = 3)
        val propios = requireNotNull(request)
        assertEquals("propios", propios.url.queryParameter("ambito"))
        assertEquals("3", propios.url.queryParameter("terreno_id"))
        assertNull(propios.url.queryParameter("distancia_km"))
        assertNull(propios.url.queryParameter("severidad"))

        service.getResumenReportes(
            ambito = "propios",
            fechaDesde = "2026-09-01T00:00:00Z",
            fechaHasta = "2026-09-30T23:59:59.999999999Z",
            severidad = "Bajo",
            terrenoId = 3
        )
        val resumen = requireNotNull(request)
        assertEquals("/reportes/resumen", resumen.url.encodedPath)
        assertEquals("propios", resumen.url.queryParameter("ambito"))
        assertEquals("Bajo", resumen.url.queryParameter("severidad"))
        assertEquals("3", resumen.url.queryParameter("terreno_id"))
        assertNull(resumen.url.queryParameter("distancia_km"))
        assertNull(resumen.url.queryParameter("limit"))
    }
}
