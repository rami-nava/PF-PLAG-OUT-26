package com.example.plag_out

import retrofit2.Response
import java.io.IOException

const val PREFIJO_RUTA_ADMIN = "admin_"
const val RUTA_ADMIN_PLAGAS = "admin_plagas"
const val RUTA_ADMIN_PLAGA_NUEVA = "admin_plaga_nueva"
const val RUTA_ADMIN_PLAGA = "admin_plaga/{plaga_id}"
const val RUTA_ADMIN_CULTIVOS = "admin_cultivos"
const val RUTA_ADMIN_CULTIVO_NUEVO = "admin_cultivo_nuevo"
const val RUTA_ADMIN_CULTIVO = "admin_cultivo/{cultivo_id}"
const val RUTA_ADMIN_USUARIOS = "admin_usuarios"
const val RUTA_ADMIN_USUARIO = "admin_usuario/{usuario_id}"
const val RUTA_ADMIN_METRICAS = "admin_metricas"
const val RUTA_ADMIN_CUENTA = "admin_cuenta"

const val RUTA_HOME_USUARIO = "monitoreos"

fun homePara(rol: String?): String = if (rol == ROL_ADMIN) RUTA_ADMIN_PLAGAS else RUTA_HOME_USUARIO

fun esRutaAdmin(ruta: String?): Boolean = ruta?.startsWith(PREFIJO_RUTA_ADMIN) == true


data class FalloAdmin(val mensaje: String, val sinPermiso: Boolean = false)

internal const val MENSAJE_SIN_CONEXION = "Sin conexión a internet."

fun falloDeRespuesta(response: Response<*>): FalloAdmin =
    falloDeCuerpo(response.code(), leerCuerpoDeError(response))

internal fun leerCuerpoDeError(response: Response<*>): String =
    runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()

fun falloDeCuerpo(codigo: Int, cuerpo: String): FalloAdmin = when (codigo) {
    403 -> when {
        cuerpo.contains(CuentaSuspendidaEventBus.DETALLE) ->
            FalloAdmin(CuentaSuspendidaEventBus.MENSAJE)
        cuerpo.contains("objetivo_admin") ->
            FalloAdmin("No se puede operar sobre una cuenta de administrador.")
        else -> FalloAdmin("Tu cuenta no tiene permisos de administrador.", sinPermiso = true)
    }
    404 -> when {
        cuerpo.contains("cultivo_no_encontrado") -> FalloAdmin("Ese cultivo ya no existe.")
        cuerpo.contains("plaga_no_encontrada") -> FalloAdmin("Esa plaga ya no existe.")
        else -> FalloAdmin(MENSAJE_NO_DISPONIBLE)
    }
    409 -> when {
        cuerpo.contains("plaga_duplicada") ->
            FalloAdmin("Ya existe una plaga con ese nombre o nombre científico.")
        cuerpo.contains("cultivo_duplicado") ->
            FalloAdmin("Ya existe un cultivo con ese nombre o nombre científico.")
        cuerpo.contains("ultimo_cultivo_de_plaga") ->
            FalloAdmin("Es el único cultivo de esta plaga. Agregale otro o dala de baja desde Plagas.")
        cuerpo.contains("plaga_inactiva") ->
            FalloAdmin("Esa plaga está dada de baja. Reactivala desde Plagas para vincularla.")
        else -> FalloAdmin("La operación entra en conflicto con datos existentes.")
    }
    422 -> FalloAdmin("El servidor rechazó los datos. Revisá los valores ingresados.")
    else -> FalloAdmin("No se pudo completar la operación (error $codigo).")
}

internal const val MENSAJE_NO_DISPONIBLE = "Esta función todavía no está disponible en el servidor."

internal fun esRutaNoDisponible(codigo: Int, cuerpo: String): Boolean =
    codigo == 404 && !cuerpo.contains("_no_encontrad")

fun falloDeExcepcion(e: Exception): FalloAdmin =
    if (e is IOException) FalloAdmin(MENSAJE_SIN_CONEXION)
    else FalloAdmin("Ocurrió un error inesperado. Intentá de nuevo.")
