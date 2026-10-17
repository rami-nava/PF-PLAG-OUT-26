package com.example.plag_out.Service

import com.example.plag_out.Pagina
import com.example.plag_out.TerrenoResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response


const val PAGINA_INICIAL = 20

const val PAGINA_SIGUIENTE = 100


suspend fun <K, T> traerTodasLasPaginas(
    recibidos: MutableMap<K, T>,
    id: (T) -> K,
    alQuedarPaginas: () -> Unit = {},
    pedir: suspend (limit: Int, offset: Int) -> Response<Pagina<T>>
): Response<Pagina<T>>? {
    var offset = 0
    var limite = PAGINA_INICIAL
    while (true) {
        val response = pedir(limite, offset)
        if (!response.isSuccessful) return response

        val pagina = response.body()
        val items = pagina?.items.orEmpty()
        items.forEach { recibidos[id(it)] = it }
        offset += items.size
        // Sin items no hay forma de avanzar: se corta aunque `total` diga que faltan
        if (items.isEmpty() || offset >= (pagina?.total ?: 0)) return null

        alQuedarPaginas()
        limite = PAGINA_SIGUIENTE
    }
}

suspend fun GDDService.todosLosTerrenos(): Response<List<TerrenoResponse>> {
    val recibidos = LinkedHashMap<Int, TerrenoResponse>()
    val error = traerTodasLasPaginas(recibidos, { it.terreno_id }, pedir = ::getTerrenos)
        ?: return Response.success(recibidos.values.toList())
    return Response.error(error.errorBody() ?: "".toResponseBody(), error.raw())
}
