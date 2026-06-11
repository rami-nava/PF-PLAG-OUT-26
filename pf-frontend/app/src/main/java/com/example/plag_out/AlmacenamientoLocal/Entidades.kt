package com.example.plag_out.AlmacenamientoLocal

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "monitoreos")
data class MonitoreoEntidad(
    @PrimaryKey
    val monitoreo_id: Int,
    val plantacion_id: Int,
    val terreno_id: Int,
    val terreno_nombre: String,
    val cultivo_nombre: String,
    val plaga_nombre: String,
    val gdd_diario: Float,
    val gdd_acumulado: Float,
    val gdd_objetivo: Float,
    val progreso: Float,
    val nivel_alerta: Int
)