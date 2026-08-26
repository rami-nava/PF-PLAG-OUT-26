package com.example.plag_out.AlmacenamientoLocal

import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse
import com.example.plag_out.UsuarioResponse

class MonitoreoRepository(
    private val monitoreoDao: MonitoreoDao
) {

    suspend fun obtenerMonitoreos(): List<MonitoreoResponse> {
        return monitoreoDao.getAll()
    }

    suspend fun obtenerMonitoreo(id: Int): MonitoreoResponse? {
        return monitoreoDao.getById(id)
    }

    suspend fun guardarMonitoreo(monitoreo: MonitoreoResponse) {
        monitoreoDao.insert(monitoreo)
    }

    suspend fun borrarMonitoreo(id: Int) {
        monitoreoDao.deleteById(id)
    }

    suspend fun borrarPorTerreno(terrenoId: Int) {
        monitoreoDao.deleteByTerrenoId(terrenoId)
    }

    suspend fun borrarPorPlantacion(plantacionId: Int) {
        monitoreoDao.deleteByPlantacionId(plantacionId)
    }

    suspend fun renombrarTerreno(terrenoId: Int, nombre: String) {
        monitoreoDao.updateTerrenoNombre(terrenoId, nombre)
    }

    suspend fun guardarMonitoreos(monitoreos: List<MonitoreoResponse>) {
        monitoreoDao.insertAll(monitoreos)
    }

    /** GET /monitoreos devuelve la lista completa (activos y finalizados), así que el caché se reemplaza tal cual. */
    suspend fun reemplazarMonitoreos(monitoreos: List<MonitoreoResponse>) {
        monitoreoDao.deleteAll()
        monitoreoDao.insertAll(monitoreos)
    }

    suspend fun borrarTodos() {
        monitoreoDao.deleteAll()
    }
}

class TerrenoRepository(
    private val terrenoDao: TerrenoDao
) {

    suspend fun obtenerTerrenos(): List<TerrenoResponse> {
        return terrenoDao.getAll()
    }

    suspend fun guardarTerreno(terreno: TerrenoResponse) {
        terrenoDao.insert(terreno)
    }

    suspend fun borrarTerreno(id: Int) {
        terrenoDao.deleteById(id)
    }

    suspend fun guardarTerrenos(terrenos: List<TerrenoResponse>) {
        terrenoDao.insertAll(terrenos)
    }

    suspend fun reemplazarTerrenos(terrenos: List<TerrenoResponse>) {
        terrenoDao.deleteAll()
        terrenoDao.insertAll(terrenos)
    }

    suspend fun borrarTodos() {
        terrenoDao.deleteAll()
    }
}

class PlantacionRepository(
    private val plantacionDao: PlantacionDao
) {

    suspend fun obtenerPlantaciones(): List<PlantacionesResponse> {
        return plantacionDao.getAll()
    }

    suspend fun guardarPlantacion(plantacion: PlantacionesResponse) {
        plantacionDao.insert(plantacion)
    }

    suspend fun guardarPlantaciones(plantaciones: List<PlantacionesResponse>) {
        plantacionDao.insertAll(plantaciones)
    }

    suspend fun reemplazarPlantaciones(plantaciones: List<PlantacionesResponse>) {
        plantacionDao.deleteAll()
        plantacionDao.insertAll(plantaciones)
    }

    suspend fun borrarPlantacion(id: Int) {
        plantacionDao.deleteById(id)
    }

    suspend fun borrarPorTerreno(terrenoId: Int) {
        plantacionDao.deleteByTerrenoId(terrenoId)
    }

    suspend fun renombrarTerreno(terrenoId: Int, nombre: String) {
        plantacionDao.updateTerrenoNombre(terrenoId, nombre)
    }

    suspend fun borrarTodos() {
        plantacionDao.deleteAll()
    }
}

class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {

    suspend fun obtenerUsuario(): UsuarioResponse? {
        return usuarioDao.get()
    }

    suspend fun guardarUsuario(usuario: UsuarioResponse) {
        usuarioDao.deleteAll()
        usuarioDao.insert(usuario)
    }

    suspend fun borrarTodos() {
        usuarioDao.deleteAll()
    }
}