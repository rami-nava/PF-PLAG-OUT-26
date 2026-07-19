package com.example.plag_out.fakes

import com.example.plag_out.AlmacenamientoLocal.MonitoreoDao
import com.example.plag_out.AlmacenamientoLocal.PlantacionDao
import com.example.plag_out.AlmacenamientoLocal.TerrenoDao
import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse

/**
 * DAOs falsos respaldados por una lista en memoria.
 *
 * [operaciones] registra el orden de las llamadas.
 */
class FakeTerrenoDao(inicial: List<TerrenoResponse> = emptyList()) : TerrenoDao {
    private val filas = inicial.toMutableList()
    val operaciones = mutableListOf<String>()

    override suspend fun getAll(): List<TerrenoResponse> {
        operaciones += "getAll"; return filas.toList()
    }

    override suspend fun insertAll(terrenos: List<TerrenoResponse>) {
        operaciones += "insertAll"; filas += terrenos
    }

    override suspend fun insert(terreno: TerrenoResponse) {
        operaciones += "insert"
        filas.removeAll { it.terreno_id == terreno.terreno_id }
        filas += terreno
    }

    override suspend fun deleteAll() {
        operaciones += "deleteAll"; filas.clear()
    }
}

class FakePlantacionDao(inicial: List<PlantacionesResponse> = emptyList()) : PlantacionDao {
    private val filas = inicial.toMutableList()
    val operaciones = mutableListOf<String>()

    override suspend fun getAll(): List<PlantacionesResponse> {
        operaciones += "getAll"; return filas.toList()
    }

    override suspend fun insertAll(plantaciones: List<PlantacionesResponse>) {
        operaciones += "insertAll"; filas += plantaciones
    }

    override suspend fun insert(plantacion: PlantacionesResponse) {
        operaciones += "insert"
        filas.removeAll { it.plantacion_id == plantacion.plantacion_id }
        filas += plantacion
    }

    override suspend fun deleteAll() {
        operaciones += "deleteAll"; filas.clear()
    }
}

class FakeMonitoreoDao(inicial: List<MonitoreoResponse> = emptyList()) : MonitoreoDao {
    private val filas = inicial.toMutableList()
    val operaciones = mutableListOf<String>()

    override suspend fun getAll(): List<MonitoreoResponse> {
        operaciones += "getAll"; return filas.toList()
    }

    override suspend fun insertAll(monitoreos: List<MonitoreoResponse>) {
        operaciones += "insertAll"; filas += monitoreos
    }

    override suspend fun insert(monitoreo: MonitoreoResponse) {
        operaciones += "insert"
        filas.removeAll { it.monitoreo_id == monitoreo.monitoreo_id }
        filas += monitoreo
    }

    override suspend fun deleteAll() {
        operaciones += "deleteAll"; filas.clear()
    }
}
