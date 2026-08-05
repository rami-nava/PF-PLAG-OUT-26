package com.example.plag_out.fakes

import com.example.plag_out.CargoResponse
import com.example.plag_out.CreatePlantacionRequest
import com.example.plag_out.CreateTerrenoRequest
import com.example.plag_out.CreateUserRequest
import com.example.plag_out.CreateUserResponse
import com.example.plag_out.CultivoResponse
import com.example.plag_out.DispositivoRequest
import com.example.plag_out.DispositivoResponse
import com.example.plag_out.MonitoreoRequest
import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlagaResponse
import com.example.plag_out.PlantacionCreateResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoCreateResponse
import com.example.plag_out.TerrenoResponse
import com.example.plag_out.UpdateMonitoreoRequest
import com.example.plag_out.UpdatePlantacionRequest
import com.example.plag_out.UpdateUserRequest
import com.example.plag_out.UsuarioResponse
import com.example.plag_out.CreateReporteRequest
import com.example.plag_out.ReporteResponse
import com.example.plag_out.ReporteDetalleResponse
import com.example.plag_out.Service.GDDService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.io.IOException

/**
 * Implementación falsa de [GDDService] para tests.
 *
 * Cada endpoint se define con un lambda reasignable, así cada test declara únicamente
 * los endpoints que usa.
 *
 * [llamadas] registra el nombre de cada endpoint invocado, para poder afirmar que
 * NO hubo llamada de red (por ejemplo, cuando el caché sigue vigente).
 */
class FakeGDDService : GDDService {

    val llamadas = mutableListOf<String>()

    fun vecesLlamado(endpoint: String): Int = llamadas.count { it == endpoint }

    var getMonitoreosResult: () -> Response<List<MonitoreoResponse>> = { noDeclarado("getMonitoreos") }
    var getTerrenosResult: () -> Response<List<TerrenoResponse>> = { noDeclarado("getTerrenos") }
    var getPlantacionesResult: () -> Response<List<PlantacionesResponse>> = { noDeclarado("getPlantaciones") }
    var createTerrenoResult: () -> Response<TerrenoCreateResponse> = { noDeclarado("createTerreno") }
    var eliminarTerrenoResult: () -> Response<Unit> = { noDeclarado("eliminarTerreno") }
    var getCultivosResult: () -> Response<List<CultivoResponse>> = { noDeclarado("getCultivos") }
    var getPlagasResult: () -> Response<List<PlagaResponse>> = { noDeclarado("getPlagas") }
    var createPlantacionResult: () -> Response<PlantacionCreateResponse> = { noDeclarado("createPlantacion") }
    var actualizarPlantacionResult: () -> Response<PlantacionesResponse> = { noDeclarado("actualizarPlantacion") }
    var eliminarPlantacionResult: () -> Response<Unit> = { noDeclarado("eliminarPlantacion") }
    var createMonitoreoResult: () -> Response<MonitoreoResponse> = { noDeclarado("createMonitoreo") }
    var getMonitoreoResult: () -> Response<MonitoreoResponse> = { noDeclarado("getMonitoreo") }
    var actualizarMonitoreoResult: () -> Response<MonitoreoResponse> = { noDeclarado("actualizarMonitoreo") }
    var createUserResult: () -> Response<CreateUserResponse> = { noDeclarado("createUser") }
    var getUsuarioActualResult: () -> Response<UsuarioResponse> = { noDeclarado("getUsuarioActual") }
    var actualizarUsuarioResult: () -> Response<UsuarioResponse> = { noDeclarado("actualizarUsuario") }
    var registrarDispositivoResult: () -> Response<DispositivoResponse> = { noDeclarado("registrarDispositivo") }
    var eliminarDispositivoResult: () -> Response<Unit> = { noDeclarado("eliminarDispositivo") }
    var getCargosResult: () -> Response<List<CargoResponse>> = { noDeclarado("getCargos") }
    var createReporteResult: () -> Response<ReporteResponse> = { noDeclarado("createReporte") }
    var createReporteApiResult: () -> Response<ReporteResponse> = { noDeclarado("createReporteApi") }
    var getReporteResult: () -> Response<ReporteDetalleResponse> = { noDeclarado("getReporte") }
    var healthResult: () -> Response<Unit> = { noDeclarado("health") }

    override suspend fun getMonitoreos(): Response<List<MonitoreoResponse>> {
        llamadas += "getMonitoreos"; return getMonitoreosResult()
    }

    override suspend fun getTerrenos(): Response<List<TerrenoResponse>> {
        llamadas += "getTerrenos"; return getTerrenosResult()
    }

    override suspend fun getPlantaciones(): Response<List<PlantacionesResponse>> {
        llamadas += "getPlantaciones"; return getPlantacionesResult()
    }

    override suspend fun createTerreno(data: CreateTerrenoRequest): Response<TerrenoCreateResponse> {
        llamadas += "createTerreno"; ultimoCreateTerreno = data; return createTerrenoResult()
    }

    override suspend fun eliminarTerreno(id: Int): Response<Unit> {
        llamadas += "eliminarTerreno"; return eliminarTerrenoResult()
    }

    override suspend fun getCultivos(): Response<List<CultivoResponse>> {
        llamadas += "getCultivos"; return getCultivosResult()
    }

    override suspend fun getPlagas(): Response<List<PlagaResponse>> {
        llamadas += "getPlagas"; return getPlagasResult()
    }

    override suspend fun createPlantacion(data: CreatePlantacionRequest): Response<PlantacionCreateResponse> {
        llamadas += "createPlantacion"; ultimoCreatePlantacion = data; return createPlantacionResult()
    }

    override suspend fun actualizarPlantacion(id: Int, data: UpdatePlantacionRequest): Response<PlantacionesResponse> {
        llamadas += "actualizarPlantacion"; ultimoActualizarPlantacion = data; return actualizarPlantacionResult()
    }

    override suspend fun eliminarPlantacion(id: Int): Response<Unit> {
        llamadas += "eliminarPlantacion"; return eliminarPlantacionResult()
    }

    override suspend fun createMonitoreo(data: MonitoreoRequest): Response<MonitoreoResponse> {
        llamadas += "createMonitoreo"; ultimoCreateMonitoreo = data; return createMonitoreoResult()
    }

    override suspend fun getMonitoreo(id: Int): Response<MonitoreoResponse> {
        llamadas += "getMonitoreo"; return getMonitoreoResult()
    }

    override suspend fun actualizarMonitoreo(id: Int, data: UpdateMonitoreoRequest): Response<MonitoreoResponse> {
        llamadas += "actualizarMonitoreo"; ultimoActualizarMonitoreo = data; return actualizarMonitoreoResult()
    }

    override suspend fun createUser(data: CreateUserRequest): Response<CreateUserResponse> {
        llamadas += "createUser"; return createUserResult()
    }

    override suspend fun getUsuarioActual(): Response<UsuarioResponse> {
        llamadas += "getUsuarioActual"; return getUsuarioActualResult()
    }

    override suspend fun actualizarUsuario(data: UpdateUserRequest): Response<UsuarioResponse> {
        llamadas += "actualizarUsuario"; ultimoActualizarUsuario = data; return actualizarUsuarioResult()
    }

    override suspend fun registrarDispositivo(data: DispositivoRequest): Response<DispositivoResponse> {
        llamadas += "registrarDispositivo"; ultimoRegistrarDispositivo = data; return registrarDispositivoResult()
    }

    override suspend fun eliminarDispositivo(fcmToken: String): Response<Unit> {
        llamadas += "eliminarDispositivo"; ultimoEliminarDispositivo = fcmToken; return eliminarDispositivoResult()
    }

    override suspend fun getCargos(): Response<List<CargoResponse>> {
        llamadas += "getCargos"; return getCargosResult()
    }

    override suspend fun createReporte(data: CreateReporteRequest): Response<ReporteResponse> {
        llamadas += "createReporte"; ultimoCreateReporte = data; return createReporteResult()
    }

    override suspend fun createReporteApi(data: CreateReporteRequest): Response<ReporteResponse> {
        llamadas += "createReporteApi"; ultimoCreateReporte = data; return createReporteApiResult()
    }

    override suspend fun getReporte(id: Int): Response<ReporteDetalleResponse> {
        llamadas += "getReporte"; return getReporteResult()
    }

    override suspend fun health(): Response<Unit> {
        llamadas += "health"; return healthResult()
    }

    // Cuerpos de las peticiones recibidas, para verificar que el ViewModel arma bien el request.
    var ultimoCreateTerreno: CreateTerrenoRequest? = null
        private set
    var ultimoCreatePlantacion: CreatePlantacionRequest? = null
        private set
    var ultimoCreateMonitoreo: MonitoreoRequest? = null
        private set
    var ultimoRegistrarDispositivo: DispositivoRequest? = null
        private set
    var ultimoActualizarUsuario: UpdateUserRequest? = null
        private set
    var ultimoActualizarMonitoreo: UpdateMonitoreoRequest? = null
        private set
    var ultimoActualizarPlantacion: UpdatePlantacionRequest? = null
        private set
    var ultimoCreateReporte: CreateReporteRequest? = null
        private set
    var ultimoEliminarDispositivo: String? = null
        private set

    private fun <T> noDeclarado(endpoint: String): Response<T> =
        throw AssertionError(
            "El test no declaró una respuesta para '$endpoint', pero el código la invocó."
        )

    companion object {
        /** Respuesta 500 del servidor. */
        fun <T> errorServidor(codigo: Int = 500): Response<T> =
            Response.error(codigo, "".toResponseBody("application/json".toMediaType()))

        /** Simula falta de conexión: la llamada lanza en lugar de responder. */
        fun sinConexion(): Nothing = throw IOException("Sin conexión")
    }
}
