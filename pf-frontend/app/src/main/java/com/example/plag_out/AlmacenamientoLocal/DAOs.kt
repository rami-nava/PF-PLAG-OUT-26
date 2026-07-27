package com.example.plag_out.AlmacenamientoLocal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse

@Dao
interface MonitoreoDao {

    @Query("SELECT * FROM monitoreos")
    suspend fun getAll(): List<MonitoreoResponse>

    @Query("SELECT * FROM monitoreos WHERE monitoreo_id = :id")
    suspend fun getById(id: Int): MonitoreoResponse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(monitoreos: List<MonitoreoResponse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(monitoreo: MonitoreoResponse)

    @Query("DELETE FROM monitoreos")
    suspend fun deleteAll()

    @Query("DELETE FROM monitoreos WHERE monitoreo_id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM monitoreos WHERE terreno_id = :terrenoId")
    suspend fun deleteByTerrenoId(terrenoId: Int)

    @Query("DELETE FROM monitoreos WHERE plantacion_id = :plantacionId")
    suspend fun deleteByPlantacionId(plantacionId: Int)
}

@Dao
interface TerrenoDao {

    @Query("SELECT * FROM terrenos")
    suspend fun getAll(): List<TerrenoResponse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terrenos: List<TerrenoResponse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(terreno: TerrenoResponse)

    @Query("DELETE FROM terrenos")
    suspend fun deleteAll()

    @Query("DELETE FROM terrenos WHERE terreno_id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface PlantacionDao {

    @Query("SELECT * FROM plantaciones")
    suspend fun getAll(): List<PlantacionesResponse>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plantaciones: List<PlantacionesResponse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plantacion: PlantacionesResponse)

    @Query("DELETE FROM plantaciones")
    suspend fun deleteAll()

    @Query("DELETE FROM plantaciones WHERE plantacion_id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM plantaciones WHERE terreno_id = :terrenoId")
    suspend fun deleteByTerrenoId(terrenoId: Int)
}