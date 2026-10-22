package com.example.plag_out

import com.example.plag_out.Service.PAGINA_CATALOGO
import com.example.plag_out.Service.catalogoCompleto
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class CatalogoCompletoTest {

    @Test
    fun `recorre todas las paginas hasta una corta`() = runBlocking {
        val todos = (1..(PAGINA_CATALOGO * 2 + 7)).toList()
        val pedidos = mutableListOf<Pair<Int, Int>>()

        val response = catalogoCompleto { limit, offset ->
            pedidos += limit to offset
            Response.success(todos.drop(offset).take(limit))
        }

        assertEquals(todos, response.body())
        assertEquals(listOf(0, PAGINA_CATALOGO, PAGINA_CATALOGO * 2), pedidos.map { it.second })
    }

    @Test
    fun `una pagina llena exacta pide la siguiente y termina vacia`() = runBlocking {
        val todos = (1..PAGINA_CATALOGO).toList()

        val response = catalogoCompleto { limit, offset -> Response.success(todos.drop(offset).take(limit)) }

        assertEquals(todos, response.body())
    }

    @Test
    fun `un error en cualquier pagina se devuelve tal cual`() = runBlocking {
        val response = catalogoCompleto<Int> { _, offset ->
            if (offset == 0) Response.success((1..PAGINA_CATALOGO).toList())
            else Response.error(500, "{}".toResponseBody("application/json".toMediaType()))
        }

        assertEquals(500, response.code())
    }
}
