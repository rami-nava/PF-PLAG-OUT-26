package com.example.plag_out

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AdminMetricasModelTest {

    @Test
    fun `determinarCampaña asigna correctamente segun mes de corte de julio`() {
        // En o después del 1 de julio: año / año+1
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2025, 7, 1)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2025, 10, 15)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2025, 12, 31)))

        // Antes del 1 de julio: año-1 / año
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2026, 1, 1)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2026, 3, 20)))
        assertEquals("2025/2026", determinarCampaña(LocalDate.of(2026, 6, 30)))

        // Nueva campaña a partir de julio
        assertEquals("2026/2027", determinarCampaña(LocalDate.of(2026, 7, 1)))
    }

    @Test
    fun `extraerAñoInicioCampaña parsea el año inicial`() {
        assertEquals(2025, extraerAñoInicioCampaña("2025/2026"))
        assertEquals(2026, extraerAñoInicioCampaña("2026/2027"))
        assertEquals(0, extraerAñoInicioCampaña("invalido"))
    }

    @Test
    fun `agruparPorCampaña con lista vacia retorna lista vacia`() {
        val resultado = agruparPorCampaña(emptyList())
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `agruparPorCampaña calcula plagas lideres e indices de riesgo correctamente`() {
        val terreno1 = TerrenoResponse(
            terreno_id = 1,
            terreno_nombre = "Lote Norte",
            terreno_latitud = -31.4f,
            terreno_longitud = -64.1f,
            terreno_area = 50.0f
        )
        val terreno2 = TerrenoResponse(
            terreno_id = 2,
            terreno_nombre = "Lote Sur",
            terreno_latitud = -31.5f,
            terreno_longitud = -64.2f,
            terreno_area = 30.0f
        )

        val monitoreos = listOf(
            monitoreoDePrueba(
                monitoreo_id = 1,
                plantacion_id = 1,
                terreno_id = 1,
                terreno_nombre = "Lote Norte",
                cultivo_nombre = "Maíz",
                plaga_nombre = "Oruga cogollera",
                plaga_nombre_cientifico = "Spodoptera frugiperda",
                gdd_diario = 12f,
                gdd_acumulado = 150f,
                gdd_objetivo = 300f,
                progreso = 50f,
                nivel_alerta = 2,
                fecha_actualizacion = LocalDate.of(2025, 11, 10)
            ),
            monitoreoDePrueba(
                monitoreo_id = 2,
                plantacion_id = 1,
                terreno_id = 1,
                terreno_nombre = "Lote Norte",
                cultivo_nombre = "Maíz",
                plaga_nombre = "Oruga cogollera",
                plaga_nombre_cientifico = "Spodoptera frugiperda",
                gdd_diario = 10f,
                gdd_acumulado = 80f,
                gdd_objetivo = 300f,
                progreso = 26f,
                nivel_alerta = 0,
                fecha_actualizacion = LocalDate.of(2025, 12, 5)
            ),
            monitoreoDePrueba(
                monitoreo_id = 3,
                plantacion_id = 2,
                terreno_id = 2,
                terreno_nombre = "Lote Sur",
                cultivo_nombre = "Soja",
                plaga_nombre = "Barrenador del tallo",
                plaga_nombre_cientifico = "Diatraea saccharalis",
                gdd_diario = 8f,
                gdd_acumulado = 220f,
                gdd_objetivo = 300f,
                progreso = 73f,
                nivel_alerta = 1,
                fecha_actualizacion = LocalDate.of(2026, 2, 14)
            ),
            monitoreoDePrueba(
                monitoreo_id = 4,
                plantacion_id = 1,
                terreno_id = 1,
                terreno_nombre = "Lote Norte",
                cultivo_nombre = "Maíz",
                plaga_nombre = "Gusano cogollero",
                gdd_diario = 11f,
                gdd_acumulado = 290f,
                gdd_objetivo = 300f,
                progreso = 96f,
                nivel_alerta = 2,
                fecha_actualizacion = LocalDate.of(2024, 11, 20)
            )
        )

        val resultado = agruparPorCampaña(monitoreos, listOf(terreno1, terreno2))

        assertEquals(2, resultado.size)

        val c2025 = resultado[0]
        assertEquals("2025/2026", c2025.nombre)
        assertEquals(3, c2025.totalMonitoreos)
        assertEquals(80.0f, c2025.hectareasMonitoreadas, 0.01f)

        assertNotNull(c2025.plagaPredominante)
        assertEquals("Oruga cogollera", c2025.plagaPredominante?.plagaNombre)
        assertEquals("Spodoptera frugiperda", c2025.plagaPredominante?.plagaCientifico)
        assertEquals(2, c2025.plagaPredominante?.totalMonitoreos)
        assertEquals(66.66f, c2025.plagaPredominante?.porcentajeIncidencia ?: 0f, 0.1f)
        assertEquals(1, c2025.plagaPredominante?.enRiesgoAlto)
        assertEquals(listOf("Maíz"), c2025.plagaPredominante?.cultivosAfectados)

        assertEquals(2, c2025.cultivos.size)
        assertEquals("Maíz", c2025.cultivos[0].cultivoNombre)
        assertEquals(2, c2025.cultivos[0].totalMonitoreos)
        assertEquals(1, c2025.cultivos[0].plagas.size)
        assertEquals("Oruga cogollera", c2025.cultivos[0].plagas[0].plagaNombre)
        assertEquals(2, c2025.cultivos[0].plagas[0].totalMonitoreos)
        assertEquals(100f, c2025.cultivos[0].plagas[0].porcentaje, 0.01f)
        assertEquals("Soja", c2025.cultivos[1].cultivoNombre)
        assertEquals(1, c2025.cultivos[1].totalMonitoreos)

        assertEquals(1, c2025.riesgoBajo)
        assertEquals(1, c2025.riesgoModerado)
        assertEquals(1, c2025.riesgoAlto)
        assertEquals(33.33f, c2025.porcentajeRiesgoBajo, 0.1f)
        assertEquals(33.33f, c2025.porcentajeRiesgoModerado, 0.1f)
        assertEquals(33.33f, c2025.porcentajeRiesgoAlto, 0.1f)

        val c2024 = resultado[1]
        assertEquals("2024/2025", c2024.nombre)
        assertEquals(1, c2024.totalMonitoreos)
        assertEquals(50.0f, c2024.hectareasMonitoreadas, 0.01f)
        assertEquals("Gusano cogollero", c2024.plagaPredominante?.plagaNombre)
        assertEquals(1, c2024.riesgoAlto)
    }

    @Test
    fun `agruparPorCampaña incluye monitoreos activos e inactivos en cultivos y ordena cultivos afectados`() {
        val terreno1 = TerrenoResponse(1, "Lote 1", -31.4f, -64.1f, 50f)
        val terreno2 = TerrenoResponse(2, "Lote 2", -31.5f, -64.2f, 30f)
        val terreno3 = TerrenoResponse(3, "Lote 3", -31.6f, -64.3f, 40f)

        val monitoreos = listOf(
            monitoreoDePrueba(
                monitoreo_id = 1, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Cogollero",
                fecha_actualizacion = LocalDate.of(2025, 11, 10), activo = true
            ),
            monitoreoDePrueba(
                monitoreo_id = 2, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Cogollero",
                fecha_actualizacion = LocalDate.of(2025, 11, 20), activo = false
            ),
            monitoreoDePrueba(
                monitoreo_id = 3, plantacion_id = 2, terreno_id = 2, terreno_nombre = "Lote 2",
                cultivo_nombre = "Maíz", plaga_nombre = "Barrenador",
                fecha_actualizacion = LocalDate.of(2025, 12, 1), activo = false
            ),
            monitoreoDePrueba(
                monitoreo_id = 4, plantacion_id = 3, terreno_id = 3, terreno_nombre = "Lote 3",
                cultivo_nombre = "Soja", plaga_nombre = "Cogollero",
                fecha_actualizacion = LocalDate.of(2026, 1, 15), activo = false
            )
        )

        val camp = agruparPorCampaña(monitoreos, listOf(terreno1, terreno2, terreno3)).single()

        // Cogollero afecta Maíz (2 monitoreos) y Soja (1 monitoreo)
        assertEquals("Cogollero", camp.plagaPredominante?.plagaNombre)
        assertEquals(listOf("Maíz", "Soja"), camp.plagaPredominante?.cultivosAfectados)

        // En cultivos de la campaña entran los 4 monitoreos (activos + finalizados) y las ha por lote no se duplican
        assertEquals(2, camp.cultivos.size)
        val maiz = camp.cultivos[0]
        assertEquals("Maíz", maiz.cultivoNombre)
        assertEquals(3, maiz.totalMonitoreos)
        assertEquals(80f, maiz.totalHectareas, 0.01f)
        assertEquals(listOf("Cogollero", "Barrenador"), maiz.plagas.map { it.plagaNombre })
        assertEquals(listOf(2, 1), maiz.plagas.map { it.totalMonitoreos })

        val soja = camp.cultivos[1]
        assertEquals("Soja", soja.cultivoNombre)
        assertEquals(1, soja.totalMonitoreos)
        assertEquals(40f, soja.totalHectareas, 0.01f)
    }

    @Test
    fun `calcularSemaforoHectareas clasifica la superficie segun el peor estado de cada lote`() {
        val terreno1 = TerrenoResponse(1, "Lote 1", -31.4f, -64.1f, 100f) // Alerta alta (Rojo)
        val terreno2 = TerrenoResponse(2, "Lote 2", -31.5f, -64.2f, 50f)  // Alerta moderada (Amarillo)
        val terreno3 = TerrenoResponse(3, "Lote 3", -31.6f, -64.3f, 30f)  // Alerta baja (Verde)

        val monitoreos = listOf(
            monitoreoDePrueba(
                monitoreo_id = 1, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Oruga", gdd_diario = 10f, gdd_acumulado = 50f,
                gdd_objetivo = 100f, progreso = 50f, nivel_alerta = 2, fecha_actualizacion = LocalDate.now(), activo = true
            ),
            monitoreoDePrueba(
                monitoreo_id = 2, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Chinche", gdd_diario = 10f, gdd_acumulado = 20f,
                gdd_objetivo = 100f, progreso = 20f, nivel_alerta = 0, fecha_actualizacion = LocalDate.now(), activo = true
            ),
            monitoreoDePrueba(
                monitoreo_id = 3, plantacion_id = 2, terreno_id = 2, terreno_nombre = "Lote 2",
                cultivo_nombre = "Soja", plaga_nombre = "Barrenador", gdd_diario = 10f, gdd_acumulado = 60f,
                gdd_objetivo = 100f, progreso = 60f, nivel_alerta = 1, fecha_actualizacion = LocalDate.now(), activo = true
            ),
            monitoreoDePrueba(
                monitoreo_id = 4, plantacion_id = 3, terreno_id = 3, terreno_nombre = "Lote 3",
                cultivo_nombre = "Trigo", plaga_nombre = "Pulgón", gdd_diario = 10f, gdd_acumulado = 10f,
                gdd_objetivo = 100f, progreso = 10f, nivel_alerta = 0, fecha_actualizacion = LocalDate.now(), activo = true
            )
        )

        val semaforo = calcularSemaforoHectareas(monitoreos, listOf(terreno1, terreno2, terreno3))

        assertEquals(180f, semaforo.totalHectareas, 0.01f)
        assertEquals(100f, semaforo.hectareasRojo, 0.01f)
        assertEquals(50f, semaforo.hectareasAmarillo, 0.01f)
        assertEquals(30f, semaforo.hectareasVerde, 0.01f)

        assertEquals(1, semaforo.lotesRojo)
        assertEquals(1, semaforo.lotesAmarillo)
        assertEquals(1, semaforo.lotesVerde)
        assertEquals(3, semaforo.totalLotes)

        assertEquals(55.55f, semaforo.porcentajeRojo, 0.1f)
        assertEquals(27.77f, semaforo.porcentajeAmarillo, 0.1f)
        assertEquals(16.66f, semaforo.porcentajeVerde, 0.1f)
    }

    @Test
    fun `calcularPlagasPorCultivo agrupa incidencia de plagas y fechas de siembra`() {
        val terreno = TerrenoResponse(1, "Lote 1", -31.4f, -64.1f, 80f)
        val plantacion = PlantacionesResponse(
            plantacion_id = 1,
            terreno_id = 1,
            terreno_nombre = "Lote 1",
            cultivo_id = 1,
            cultivo_nombre = "Maíz",
            cultivo_nombre_cientifico = "Zea mays",
            fecha_siembra = LocalDate.of(2026, 10, 1),
            activa = true
        )

        val monitoreos = listOf(
            monitoreoDePrueba(
                monitoreo_id = 1, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Oruga cogollera", gdd_diario = 10f, gdd_acumulado = 80f,
                gdd_objetivo = 100f, progreso = 80f, nivel_alerta = 2, fecha_actualizacion = LocalDate.now(), activo = true
            )
        )

        val fechaHoy = LocalDate.of(2026, 10, 17)
        val resultado = calcularPlagasPorCultivo(monitoreos, listOf(terreno), listOf(plantacion), fechaReferencia = fechaHoy)

        assertEquals(1, resultado.size)
        val maiz = resultado[0]
        assertEquals("Maíz", maiz.cultivoNombre)
        assertEquals(80f, maiz.totalHectareas, 0.01f)
        assertEquals(1, maiz.totalLotes)
        assertEquals("Oruga cogollera", maiz.plagaMasFrecuente)
        assertEquals(16L, maiz.diasDesdeSiembra)
    }

    @Test
    fun `calcularVentanasControlAgronomo muestra la etapa termica del ciclo y no ventanas por porcentaje`() {
        val monitoreos = listOf(
            monitoreoDePrueba(
                monitoreo_id = 1, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Oruga 1", gdd_diario = 10f, gdd_acumulado = 20f,
                gdd_objetivo = 100f, progreso = 20f, nivel_alerta = 0, fecha_actualizacion = LocalDate.now(), activo = true
            ),
            monitoreoDePrueba(
                monitoreo_id = 2, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Oruga 2", gdd_diario = 10f, gdd_acumulado = 60f,
                gdd_objetivo = 100f, progreso = 60f, nivel_alerta = 1, fecha_actualizacion = LocalDate.now(), activo = true
            ),
            monitoreoDePrueba(
                monitoreo_id = 3, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 1",
                cultivo_nombre = "Maíz", plaga_nombre = "Oruga 3", gdd_diario = 10f, gdd_acumulado = 95f,
                gdd_objetivo = 100f, progreso = 95f, nivel_alerta = 2, fecha_actualizacion = LocalDate.now(), activo = true
            )
        )

        val ventanas = calcularVentanasControlAgronomo(monitoreos)

        // Lo eclosionado primero; la etapa sale del nivel del ciclo, no de cortes por porcentaje.
        assertEquals(listOf(3, 2, 1), ventanas.map { it.monitoreoId })
        assertEquals(
            listOf(EstadoVentanaControl.ECLOSIONADA, EstadoVentanaControl.ECLOSION_CERCA, EstadoVentanaControl.HUEVO),
            ventanas.map { it.estadoVentana }
        )
        assertEquals(listOf(true, false, false), ventanas.map { it.requiereRecorrida })
        assertTrue(ventanas.none { "L1" in it.estadioEstimado || "L1" in it.recomendacionTecnica })
    }

    @Test
    fun `nivelAlertaResuelto y progresoResuelto salen del ciclo y sin ciclo no hay medicion`() {
        // Monitoreo sin ciclos usa valores base
        val mSinCiclos = monitoreoDePrueba(
            monitoreo_id = 10, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote Base",
            cultivo_nombre = "Soja", plaga_nombre = "Oruga", gdd_diario = 5f, gdd_acumulado = 50f,
            gdd_objetivo = 100f, progreso = 50f, nivel_alerta = 1, fecha_actualizacion = LocalDate.now(),
            ciclos = emptyList()
        )
        // Sin ciclos espera el biofix: no hay nada medido.
        assertNull(nivelAlertaResuelto(mSinCiclos))
        assertNull(progresoResuelto(mSinCiclos))
        assertNull(gddAcumuladoResuelto(mSinCiclos))

        // Monitoreo con ciclo activo toma los datos del ciclo
        val cicloActivo = GddCicloResponse(
            id = 1,
            monitoreo_id = 10,
            fecha_biofix = "2026-10-01",
            estado = "activo",
            fecha_actualizacion = "2026-10-10",
            gdd_acumulado = 85f,
            gdd_diario = 5f,
            gdd_eclosion = 100f,
            gdd_generacion = 350f,
            progreso = 85f,
            estadio_biologico = "Larva L2",
            nivel_alerta = 2,
            dias_pendientes = 3
        )
        val mConCiclo = mSinCiclos.copy(ciclos = listOf(cicloActivo))
        assertEquals(2, nivelAlertaResuelto(mConCiclo))
        assertEquals(85f, progresoResuelto(mConCiclo)!!, 0.01f)
        assertEquals(85f, gddAcumuladoResuelto(mConCiclo)!!, 0.01f)
    }

    @Test
    fun `el semaforo cuenta aparte los lotes que esperan el biofix y los que no tienen monitoreo`() {
        val terrenos = listOf(
            TerrenoResponse(1, "Medido", -31.4f, -64.1f, 120f),
            TerrenoResponse(2, "Esperando", -31.5f, -64.2f, 80f),
            TerrenoResponse(3, "Sin monitoreo", -31.6f, -64.3f, 25.5f)
        )
        val ciclo = GddCicloResponse(
            id = 1, monitoreo_id = 1, fecha_biofix = "2026-10-01", estado = "activo",
            fecha_actualizacion = "2026-10-15", gdd_acumulado = 90f, gdd_diario = 12f,
            gdd_eclosion = 100f, gdd_generacion = 350f, progreso = 90f, estadio_biologico = "Huevo",
            nivel_alerta = 1, dias_pendientes = 0
        )
        val medido = monitoreoDePrueba(
            monitoreo_id = 1, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Medido",
            cultivo_nombre = "Soja", plaga_nombre = "Chinche", gdd_diario = 12f, gdd_acumulado = 90f,
            gdd_objetivo = 100f, progreso = 90f, nivel_alerta = 1, fecha_actualizacion = LocalDate.now(),
            ciclos = listOf(ciclo)
        )
        val esperando = monitoreoDePrueba(
            monitoreo_id = 2, plantacion_id = 2, terreno_id = 2, terreno_nombre = "Esperando",
            cultivo_nombre = "Soja", plaga_nombre = "Chinche", gdd_diario = 0f, gdd_acumulado = 0f,
            gdd_objetivo = 100f, progreso = 0f, nivel_alerta = 0, fecha_actualizacion = LocalDate.now(),
            ciclos = emptyList()
        )

        val semaforo = calcularSemaforoHectareas(listOf(medido, esperando), terrenos)

        assertEquals(1, semaforo.totalLotes)
        assertEquals(1, semaforo.lotesAmarillo)
        assertEquals(1, semaforo.lotesEsperandoBiofix)
        assertEquals(80f, semaforo.hectareasEsperandoBiofix, 0.01f)
        assertEquals(1, semaforo.lotesSinMonitoreo)
        assertEquals(25.5f, semaforo.hectareasSinMonitoreo, 0.01f)
        assertEquals("3 lotes registrados", textoLotesRegistrados(semaforo))
        assertEquals(
            listOf(
                "1 con ciclos activos · 120 ha",
                "1 esperando biofix · 80 ha",
                "1 sin monitoreos activos · 25,5 ha"
            ),
            puntosLotesDelSemaforo(semaforo)
        )
    }

    @Test
    fun `un monitoreo activo con todos sus ciclos archivados no suma nivel en los tableros`() {
        val terrenos = listOf(TerrenoResponse(1, "Lote 90", -31.4f, -64.1f, 90f))
        val archivado = GddCicloResponse(
            id = 1, monitoreo_id = 1, fecha_biofix = "2026-09-01", estado = "archivado",
            fecha_actualizacion = "2026-10-15", gdd_acumulado = 90f, gdd_diario = 12f,
            gdd_eclosion = 100f, gdd_generacion = 350f, progreso = 90f, estadio_biologico = "Huevo",
            nivel_alerta = 1, dias_pendientes = 0
        )
        val monitoreo = monitoreoDePrueba(
            monitoreo_id = 1, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote 90",
            cultivo_nombre = "Soja", plaga_nombre = "Chinche", gdd_diario = 12f, gdd_acumulado = 90f,
            gdd_objetivo = 100f, progreso = 90f, nivel_alerta = 1, fecha_actualizacion = LocalDate.now(),
            activo = true, ciclos = listOf(archivado)
        )

        // Igual que en Mis Monitoreos: sin ciclos activos espera el biofix, no está "cerca de eclosión".
        val semaforo = calcularSemaforoHectareas(listOf(monitoreo), terrenos)
        assertEquals(0f, semaforo.hectareasAmarillo, 0.01f)
        assertEquals(1, semaforo.lotesEsperandoBiofix)
        assertEquals(90f, semaforo.hectareasEsperandoBiofix, 0.01f)
        assertTrue(calcularVentanasControlAgronomo(listOf(monitoreo)).isEmpty())
        assertTrue(calcularRankingLotesCriticos(listOf(monitoreo), terrenos).isEmpty())
    }

    @Test
    fun `las etapas se cuentan por monitoreo con el texto del panel`() {
        fun ventana(id: Int, etapa: EstadoVentanaControl) = VentanaControlBiologico(
            monitoreoId = id, terrenoNombre = "L", cultivoNombre = "Maíz", plagaNombre = "Cogollero",
            plagaCientifico = null, progresoGdd = 0f, estadioEstimado = "Huevo", requiereRecorrida = false,
            estadoVentana = etapa, recomendacionTecnica = ""
        )
        val conteo = conteoPorEtapa(
            listOf(ventana(1, EstadoVentanaControl.ECLOSIONADA), ventana(2, EstadoVentanaControl.ECLOSIONADA), ventana(3, EstadoVentanaControl.HUEVO))
        )

        assertEquals(mapOf(EstadoVentanaControl.HUEVO to 1, EstadoVentanaControl.ECLOSION_CERCA to 0, EstadoVentanaControl.ECLOSIONADA to 2), conteo)
        assertTrue(textoDeEtapa(EstadoVentanaControl.ECLOSIONADA).startsWith("Recorrer el lote"))
        assertEquals("87,5 %", formatearPorcentajeTratamiento(87.5f))
        assertEquals("—", formatearPorcentajeTratamiento(null))
    }

    @Test
    fun `calcularRankingLotesCriticos ordena por nivel de alerta descendente y progreso termico`() {
        val terreno1 = TerrenoResponse(1, "Lote Norte", -31.4f, -64.1f, 120f)
        val terreno2 = TerrenoResponse(2, "Lote Sur", -31.5f, -64.2f, 80f)
        val plantacion1 = PlantacionesResponse(
            plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote Norte",
            cultivo_id = 1, cultivo_nombre = "Maíz", cultivo_nombre_cientifico = "Zea mays",
            fecha_siembra = LocalDate.of(2026, 10, 1), activa = true
        )

        val mBajo = monitoreoDePrueba(
            monitoreo_id = 1, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote Norte",
            cultivo_nombre = "Maíz", plaga_nombre = "Chinche", gdd_diario = 5f, gdd_acumulado = 30f,
            gdd_objetivo = 100f, progreso = 30f, nivel_alerta = 0, fecha_actualizacion = LocalDate.now(), activo = true
        )
        val mAlto = monitoreoDePrueba(
            monitoreo_id = 2, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote Norte",
            cultivo_nombre = "Maíz", plaga_nombre = "Oruga", gdd_diario = 12f, gdd_acumulado = 92f,
            gdd_objetivo = 100f, progreso = 92f, nivel_alerta = 2, fecha_actualizacion = LocalDate.now(), activo = true
        )
        val mMedio = monitoreoDePrueba(
            monitoreo_id = 3, plantacion_id = 1, terreno_id = 1, terreno_nombre = "Lote Norte",
            cultivo_nombre = "Maíz", plaga_nombre = "Barrenador", gdd_diario = 8f, gdd_acumulado = 65f,
            gdd_objetivo = 100f, progreso = 65f, nivel_alerta = 1, fecha_actualizacion = LocalDate.now(), activo = true
        )

        val hoy = LocalDate.of(2026, 10, 17)
        val ranking = calcularRankingLotesCriticos(
            monitoreos = listOf(mBajo, mAlto, mMedio),
            terrenos = listOf(terreno1, terreno2),
            plantaciones = listOf(plantacion1),
            fechaReferencia = hoy
        )

        assertEquals(3, ranking.size)
        // Primero el de alerta 2
        assertEquals(2, ranking[0].monitoreoId)
        assertEquals(2, ranking[0].nivelAlerta)
        assertEquals(16L, ranking[0].diasDesdeSiembra)

        // Segundo el de alerta 1
        assertEquals(3, ranking[1].monitoreoId)
        assertEquals(1, ranking[1].nivelAlerta)

        // Tercero el de alerta 0
        assertEquals(1, ranking[2].monitoreoId)
        assertEquals(0, ranking[2].nivelAlerta)
    }
}

/**
 * Monitoreo de prueba con los parámetros de antes: el avance y el nivel se cargan como un ciclo
 * activo, que es donde viven ahora. Con [ciclos] explícitos se usan esos.
 */
@Suppress("UNUSED_PARAMETER")
private fun monitoreoDePrueba(
    monitoreo_id: Int,
    plantacion_id: Int,
    terreno_id: Int,
    terreno_nombre: String,
    cultivo_nombre: String,
    plaga_nombre: String,
    plaga_nombre_cientifico: String? = null,
    gdd_diario: Float = 0f,
    gdd_acumulado: Float = 0f,
    gdd_objetivo: Float = 100f,
    progreso: Float = 0f,
    nivel_alerta: Int = 0,
    fecha_actualizacion: LocalDate = LocalDate.now(),
    fecha_inicio: LocalDate? = null,
    activo: Boolean = true,
    ciclos: List<GddCicloResponse>? = null
) = MonitoreoResponse(
    monitoreo_id = monitoreo_id,
    plantacion_id = plantacion_id,
    terreno_id = terreno_id,
    terreno_nombre = terreno_nombre,
    cultivo_nombre = cultivo_nombre,
    plaga_id = plaga_nombre.hashCode(),
    plaga_nombre = plaga_nombre,
    plaga_nombre_cientifico = plaga_nombre_cientifico,
    gdd_objetivo = gdd_objetivo,
    activo = activo,
    fecha_inicio = fecha_inicio ?: fecha_actualizacion,
    ciclos = ciclos ?: listOf(
        GddCicloResponse(
            id = monitoreo_id * 100,
            monitoreo_id = monitoreo_id,
            fecha_biofix = fecha_actualizacion.toString(),
            estado = "activo",
            fecha_actualizacion = fecha_actualizacion.toString(),
            gdd_acumulado = gdd_acumulado,
            gdd_diario = 0f,
            gdd_eclosion = gdd_objetivo,
            gdd_generacion = gdd_objetivo * 3,
            progreso = progreso,
            estadio_biologico = "",
            nivel_alerta = nivel_alerta,
            dias_pendientes = 0
        )
    )
)
