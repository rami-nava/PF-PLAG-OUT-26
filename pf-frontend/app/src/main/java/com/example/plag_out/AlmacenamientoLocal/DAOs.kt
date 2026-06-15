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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(monitoreos: List<MonitoreoResponse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(monitoreo: MonitoreoResponse)

    @Query("DELETE FROM monitoreos")
    suspend fun deleteAll()
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
}