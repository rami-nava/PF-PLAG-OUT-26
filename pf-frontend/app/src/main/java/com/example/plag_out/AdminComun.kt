package com.example.plag_out

import retrofit2.Response
import java.io.IOException

const val PREFIJO_RUTA_ADMIN = "admin_"
const val RUTA_ADMIN_PLAGAS = "admin_plagas"
const val RUTA_ADMIN_PLAGA_NUEVA = "admin_plaga_nueva"
const val RUTA_ADMIN_PLAGA = "admin_plaga/{plaga_id}"
const val RUTA_ADMIN_USUARIOS = "admin_usuarios"
const val RUTA_ADMIN_USUARIO = "admin_usuario/{usuario_id}"
const val RUTA_ADMIN_METRICAS = "admin_metricas"
const val RUTA_ADMIN_CUENTA = "admin_cuenta"

const val RUTA_HOME_USUARIO = "monitoreos"

fun homePara(rol: String?): String = if (rol == ROL_ADMIN) RUTA_ADMIN_PLAGAS else RUTA_HOME_USUARIO

fun esRutaAdmin(ruta: String?): Boolean = ruta?.startsWith(PREFIJO_RUTA_ADMIN) == true


data class FalloAdmin(val mensaje: String, val sinPermiso: Boolean = false)

internal const val MENSAJE_SIN_CONEXION = "Sin conexión a internet."

fun falloDeRespuesta(response: Response<*>): FalloAdmin {
    val cuerpo = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
    return when (response.code()) {
        403 -> when {
            cuerpo.contains(CuentaSuspendidaEventBus.DETALLE) ->
                FalloAdmin(CuentaSuspendidaEventBus.MENSAJE)
            cuerpo.contains("objetivo_admin") ->
                FalloAdmin("No se puede operar sobre una cuenta de administrador.")
            else -> FalloAdmin("Tu cuenta no tiene permisos de administrador.", sinPermiso = true)
        }
        // El backend todavía no tiene el endpoint desplegado
        404 -> FalloAdmin("Esta función todavía no está disponible en el servidor.")
        409 -> when {
            cuerpo.contains("plaga_duplicada") ->
                FalloAdmin("Ya existe una plaga con ese nombre o nombre científico.")
            else -> FalloAdmin("La operación entra en conflicto con datos existentes.")
        }
        422 -> FalloAdmin("El servidor rechazó los datos. Revisá los valores ingresados.")
        else -> FalloAdmin("No se pudo completar la operación (error ${response.code()}).")
    }
}

fun falloDeExcepcion(e: Exception): FalloAdmin =
    if (e is IOException) FalloAdmin(MENSAJE_SIN_CONEXION)
    else FalloAdmin("Ocurrió un error inesperado. Intentá de nuevo.")
