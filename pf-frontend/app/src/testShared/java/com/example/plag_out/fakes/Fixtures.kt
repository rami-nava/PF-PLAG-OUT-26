package com.example.plag_out.fakes

import com.example.plag_out.CargoResponse
import com.example.plag_out.CultivoResponse
import com.example.plag_out.MonitoreoResponse
import com.example.plag_out.PlagaResponse
import com.example.plag_out.PlantacionesResponse
import com.example.plag_out.TerrenoResponse
import com.example.plag_out.UsuarioResponse
import java.time.LocalDate

/** Constructores de datos de prueba con valores por defecto razonables. */
object Fixtures {

    fun terreno(
        id: Int = 1,
        nombre: String = "Lote Norte",
        latitud: Float = -34.6f,
        longitud: Float = -58.4f,
        area: Float = 200f
    ) = TerrenoResponse(
        terreno_id = id,
        terreno_nombre = nombre,
        terreno_latitud = latitud,
        terreno_longitud = longitud,
        terreno_area = area
    )

    fun plantacion(
        id: Int = 1,
        terrenoId: Int = 1,
        terrenoNombre: String = "Lote Norte",
        cultivo: String = "Trigo",
        cultivoCientifico: String = "Triticum aestivum",
        fechaSiembra: LocalDate = LocalDate.of(2026, 1, 1),
        activa: Boolean = true
    ) = PlantacionesResponse(
        plantacion_id = id,
        terreno_id = terrenoId,
        terreno_nombre = terrenoNombre,
        cultivo_nombre = cultivo,
        cultivo_nombre_cientifico = cultivoCientifico,
        fecha_siembra = fechaSiembra,
        activa = activa
    )

    fun monitoreo(
        id: Int = 1,
        plantacionId: Int = 1,
        terrenoId: Int = 1,
        terrenoNombre: String = "Lote Norte",
        cultivo: String = "Trigo",
        plaga: String = "Chicharrita",
        gddDiario: Float = 10f,
        gddAcumulado: Float = 100f,
        gddObjetivo: Float = 500f,
        progreso: Float = 0.2f,
        nivelAlerta: Int = 0,
        fechaActualizacion: LocalDate = LocalDate.of(2026, 1, 10),
        umbralRiesgo: Int? = 80,
        activo: Boolean = true,
        fechaInicio: LocalDate? = LocalDate.of(2026, 1, 1),
        plagaNombreCientifico: String? = "Dalbulus maidis"
    ) = MonitoreoResponse(
        monitoreo_id = id,
        plantacion_id = plantacionId,
        terreno_id = terrenoId,
        terreno_nombre = terrenoNombre,
        cultivo_nombre = cultivo,
        plaga_nombre = plaga,
        gdd_diario = gddDiario,
        gdd_acumulado = gddAcumulado,
        gdd_objetivo = gddObjetivo,
        progreso = progreso,
        nivel_alerta = nivelAlerta,
        fecha_actualizacion = fechaActualizacion,
        umbral_riesgo = umbralRiesgo,
        activo = activo,
        fecha_inicio = fechaInicio,
        plaga_nombre_cientifico = plagaNombreCientifico
    )

    fun cultivo(
        id: Int = 1,
        nombre: String = "Trigo",
        nombreCientifico: String = "Triticum aestivum"
    ) = CultivoResponse(id = id, nombre = nombre, nombre_cientifico = nombreCientifico)

    fun plaga(
        id: Int = 1,
        nombre: String = "Chicharrita",
        nombreCientifico: String = "Dalbulus maidis"
    ) = PlagaResponse(id = id, nombre = nombre, nombre_cientifico = nombreCientifico)

    fun usuario(
        id: String = "11111111-1111-1111-1111-111111111111",
        email: String = "tecnico@plagout.com",
        nombre: String = "Juan",
        apellido: String = "Perez",
        cargo: String = "Ingeniero agrónomo",
        fechaCreacion: LocalDate = LocalDate.of(2026, 1, 1)
    ) = UsuarioResponse(
        usuario_id = id,
        email = email,
        nombre = nombre,
        apellido = apellido,
        cargo = cargo,
        fecha_creacion = fechaCreacion
    )

    fun cargo(id: Int = 1, tipo: String = "Ingeniero agrónomo") =
        CargoResponse(id = id, tipo = tipo)

    fun reporteDetalle(
        id: Int = 1,
        plantacionId: Int? = 1,
        plagaId: Int = 1,
        plagaNombre: String = "Chicharrita",
        terrenoNombre: String? = "Lote Norte",
        cultivoNombre: String? = "Maíz",
        nivelSeveridad: String = "Alto",
        latitud: Double? = -34.6,
        longitud: Double? = -58.4,
        timestampMs: Long = 1700000000000L
    ) = com.example.plag_out.ReporteDetalleResponse(
        id = id,
        plantacion_id = plantacionId,
        plaga_id = plagaId,
        plaga_nombre = plagaNombre,
        terreno_nombre = terrenoNombre,
        cultivo_nombre = cultivoNombre,
        nivel_severidad = nivelSeveridad,
        latitud = latitud,
        longitud = longitud,
        timestamp_ms = timestampMs
    )
}
