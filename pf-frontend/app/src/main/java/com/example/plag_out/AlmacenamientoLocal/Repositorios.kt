package com.example.plag_out.AlmacenamientoLocal

import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse

class MonitoreoRepository(
    private val monitoreoDao: MonitoreoDao
) {

    suspend fun obtenerMonitoreos(): List<MonitoreoResponse> {
        return monitoreoDao.getAll()
    }

    suspend fun guardarMonitoreo(monitoreo: MonitoreoResponse) {
        monitoreoDao.insert(monitoreo)
    }

    suspend fun guardarMonitoreos(monitoreos: List<MonitoreoResponse>) {
        monitoreoDao.insertAll(monitoreos)
    }

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

    suspend fun borrarTodos() {
        plantacionDao.deleteAll()
    }
}