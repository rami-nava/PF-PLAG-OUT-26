package com.example.plag_out.Service

import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.Pagina
import com.example.plag_out.PlantacionesResponse
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

suspend fun GDDService.todosLosMonitoreos(): Response<List<MonitoreoResponse>> =
    todasLasPaginas({ it.monitoreo_id }, ::getMonitoreos)

suspend fun GDDService.todosLosTerrenos(): Response<List<TerrenoResponse>> =
    todasLasPaginas({ it.terreno_id }, ::getTerrenos)

suspend fun GDDService.todasLasPlantaciones(): Response<List<PlantacionesResponse>> =
    todasLasPaginas({ it.plantacion_id }, ::getPlantaciones)

// Lista completa como un único Response: si una página falla, devuelve ese error.
private suspend fun <T> todasLasPaginas(
    id: (T) -> Int,
    pedir: suspend (limit: Int, offset: Int) -> Response<Pagina<T>>
): Response<List<T>> {
    val recibidos = LinkedHashMap<Int, T>()
    val error = traerTodasLasPaginas(recibidos, id, pedir = pedir)
        ?: return Response.success(recibidos.values.toList())
    return Response.error(error.errorBody() ?: "".toResponseBody(), error.raw())
}
