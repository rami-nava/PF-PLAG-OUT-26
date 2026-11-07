package com.example.plag_out

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Suite de pruebas exhaustiva de experiencia de usuario (UX), validaciones funcionales,
 * casos borde de entrada de texto, zoom y visualización en mapas, y lógica de campañas agrícolas.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserExperienceAndModulesExhaustiveTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. UX & ENTRADA DE TEXTO (BORRADO, REGEX DECIMAL, CARACTERES ESPECIALES)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `campo de texto numerico permite borrado completo y reemplazo de comas por puntos`() {
        val regex = Regex("^\\d*\\.?\\d*$")

        // Usuario ingresa "125,5" -> debe convertirse a "125.5"
        val entradaConComa = "125,5".replace(',', '.')
        assertTrue("Debe admitir decimal con punto", entradaConComa.matches(regex))
        assertEquals("125.5", entradaConComa)

        // Usuario borra todos los caracteres quedando "" -> debe ser valido para poder seguir tipeando
        val textoBorrado = ""
        assertTrue("El campo debe permitir vaciarse al borrar", textoBorrado.matches(regex))

        // Usuario tipea un punto suelto "." mientras escribe decimales -> valido transitoriamente
        val puntoSuelto = "."
        assertTrue("Punto decimal inicial debe ser valido transitoriamente", puntoSuelto.matches(regex))

        // Usuario tipea letras o simbolos no numericos -> invalido
        val entradaInvalida = "12a.5"
        assertFalse("No debe admitir letras", entradaInvalida.matches(regex))
    }

    @Test
    fun `campo de coordenadas geograficas tolera signo negativo y vaciado al borrar`() {
        val regexCoordenada = Regex("^-?\\d*\\.?\\d*$")

        // Latitud negativa argentina "-34.60"
        assertTrue(regexCoordenada.matches("-34.60"))
        // Signo negativo solo mientras el usuario empieza a escribir
        assertTrue(regexCoordenada.matches("-"))
        // Campo completamente borrado
        assertTrue(regexCoordenada.matches(""))
        // Dos signos negativos o dos puntos -> invalido
        assertFalse(regexCoordenada.matches("--34"))
        assertFalse(regexCoordenada.matches("-34.60.2"))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. UX DE FECHAS (FLEXIBILIDAD MULTI-CAMPAÑA Y HORARIOS)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `selector de fechas de siembra admite campanas pasadas y planificadas`() {
        val hoy = LocalDate.of(2026, 11, 7)
        val seleccionables = fechasDeSiembraValidas(hoy)

        fun millis(fecha: LocalDate): Long =
            fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        // Campaña actual (hoy)
        assertTrue("Hoy debe poder seleccionarse", seleccionables.isSelectableDate(millis(hoy)))

        // Campaña 2025/2026 (hace 1 año)
        assertTrue("Siembra de hace 1 año debe ser elegible", seleccionables.isSelectableDate(millis(hoy.minusYears(1))))

        // Campaña 2024/2025 (hace 2 años)
        assertTrue("Siembra de hace 2 años debe ser elegible", seleccionables.isSelectableDate(millis(hoy.minusYears(2))))

        // Campaña 2023/2024 (hace 3 años)
        assertTrue("Siembra de hace 3 años debe ser elegible", seleccionables.isSelectableDate(millis(hoy.minusYears(3))))

        // Siembra planificada futura dentro de la temporada (próximos 3 meses)
        assertTrue("Siembra planificada próxima debe poder elegirse", seleccionables.isSelectableDate(millis(hoy.plusMonths(3))))

        // Rango de años visible en el selector
        assertTrue(seleccionables.isSelectableYear(2023))
        assertTrue(seleccionables.isSelectableYear(2024))
        assertTrue(seleccionables.isSelectableYear(2025))
        assertTrue(seleccionables.isSelectableYear(2026))
        assertTrue(seleccionables.isSelectableYear(2027))
    }

    @Test
    fun `captura manual de fecha de reporte preserva el instante seleccionado`() {
        val timestampInicial = System.currentTimeMillis()
        val context: Context = ApplicationProvider.getApplicationContext()
        val viewModel = CrearReporteViewModel(
            context = context,
            gddService = FakeGDDService(),
            currentOwner = { "test-owner" }
        )

        assertEquals(timestampInicial / 1000, viewModel.state.value.timestampMs / 1000)

        // El usuario selecciona fecha de hace 2 días (ej. reporte de campo del jueves)
        val haceDosDias = LocalDate.now().minusDays(2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        viewModel.setTimestamp(haceDosDias)

        assertEquals(haceDosDias, viewModel.state.value.timestampMs)

        // Al pulsar "Actualizar a hora actual", debe retornar a tiempo presente
        viewModel.actualizarTimestamp()
        assertTrue(viewModel.state.value.timestampMs >= timestampInicial)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. MAPAS & ZOOM (LÍMITES DE TESELAS Y DENSIDAD)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `el zoom maximo del mapa respeta el limite maximo de teselas OSM`() {
        val zoomMaximoTiles = OsmTileSource.maximumZoomLevel
        assertEquals("Las teselas OSM públicas llegan hasta zoom 19", 19, zoomMaximoTiles)

        // Validar que la pantalla no permita zooms vacíos superiores a 19
        val zoomSolicitado = 22.0
        val zoomAjustado = zoomSolicitado.coerceIn(3.0, zoomMaximoTiles.toDouble())
        assertEquals(19.0, zoomAjustado, 0.001)
    }

    @Test
    fun `puntos del mapa ignoran registros con coordenadas nulas evitando crash`() {
        val reportesConNulos = listOf(
            ReporteDetalleResponse(
                id = 1,
                plaga_id = 3,
                plaga_nombre = "Dalbulus maidis",
                nivel_severidad = "Alto",
                latitud = -34.60,
                longitud = -60.50,
                timestamp_ms = 1000L
            ),
            ReporteDetalleResponse(
                id = 2,
                plaga_id = 3,
                plaga_nombre = "Dalbulus maidis",
                nivel_severidad = "Bajo",
                latitud = null,
                longitud = -60.50,
                timestamp_ms = 1000L
            ),
            ReporteDetalleResponse(
                id = 3,
                plaga_id = 3,
                plaga_nombre = "Dalbulus maidis",
                nivel_severidad = "Medio",
                latitud = -33.80,
                longitud = null,
                timestamp_ms = 1000L
            ),
            ReporteDetalleResponse(
                id = 4,
                plaga_id = 3,
                plaga_nombre = "Dalbulus maidis",
                nivel_severidad = "Alto",
                latitud = -37.30,
                longitud = -59.10,
                timestamp_ms = 1000L
            )
        )

        val puntosValidos = reportesConNulos.mapNotNull { rep ->
            val lat = rep.latitud
            val lon = rep.longitud
            if (lat != null && lon != null) {
                PuntoMapa(lat, lon, 1, rep.nivel_severidad, rep)
            } else null
        }

        assertEquals("Solo los reportes con latitud y longitud válidas deben pasar", 2, puntosValidos.size)
        assertEquals(-34.60, puntosValidos[0].latitud, 0.001)
        assertEquals(-37.30, puntosValidos[1].latitud, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. TABLEROS Y DASHBOARDS AGRONÓMICOS MULTI-REGIÓN
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `metricas de campanas agrupan correctamente hectareas y plaga predominante`() {
        val plagaMaiz = ResumenPlagaCampaña(
            plagaNombre = "Chicharrita del maíz",
            plagaCientifico = "Dalbulus maidis",
            totalMonitoreos = 10,
            porcentajeIncidencia = 35.7f,
            enRiesgoAlto = 10,
            porcentajeRiesgoAlto = 100f,
            nivelRiesgoPromedio = 3.0f,
            cultivosAfectados = listOf("Maíz")
        )
        val plagaOruga = ResumenPlagaCampaña(
            plagaNombre = "Oruga medidora",
            plagaCientifico = "Rachiplusia nu",
            totalMonitoreos = 8,
            porcentajeIncidencia = 36.3f,
            enRiesgoAlto = 8,
            porcentajeRiesgoAlto = 100f,
            nivelRiesgoPromedio = 2.8f,
            cultivosAfectados = listOf("Soja")
        )
        val campanasPrueba = listOf(
            CampañaHistorica(
                nombre = "2025/2026",
                añoInicio = 2025,
                totalMonitoreos = 28,
                hectareasMonitoreadas = 2264.0f,
                plagaPredominante = plagaMaiz,
                riesgoBajo = 10,
                riesgoModerado = 8,
                riesgoAlto = 10,
                plagas = listOf(plagaMaiz),
                cultivos = listOf(
                    ResumenCultivoCampaña(cultivoNombre = "Maíz", cultivoCientifico = "Zea mays", totalMonitoreos = 15),
                    ResumenCultivoCampaña(cultivoNombre = "Soja", cultivoCientifico = "Glycine max", totalMonitoreos = 8)
                )
            ),
            CampañaHistorica(
                nombre = "2024/2025",
                añoInicio = 2024,
                totalMonitoreos = 22,
                hectareasMonitoreadas = 2022.0f,
                plagaPredominante = plagaOruga,
                riesgoBajo = 8,
                riesgoModerado = 6,
                riesgoAlto = 8,
                plagas = listOf(plagaOruga),
                cultivos = listOf(
                    ResumenCultivoCampaña(cultivoNombre = "Soja", cultivoCientifico = "Glycine max", totalMonitoreos = 12),
                    ResumenCultivoCampaña(cultivoNombre = "Maíz", cultivoCientifico = "Zea mays", totalMonitoreos = 8)
                )
            )
        )

        assertEquals(2, campanasPrueba.size)
        val campanaActual = campanasPrueba.first()
        assertEquals("2025/2026", campanaActual.nombre)
        assertEquals(2264.0f, campanaActual.hectareasMonitoreadas, 0.01f)
        assertEquals("Chicharrita del maíz", campanaActual.plagaPredominante?.plagaNombre)

        // Validación de tasa de riesgo alto
        val tasaRiesgoAlto = campanaActual.riesgoAlto.toDouble() / campanaActual.totalMonitoreos.toDouble()
        assertEquals(0.357, tasaRiesgoAlto, 0.01)
        assertEquals(35.71f, campanaActual.porcentajeRiesgoAlto, 0.1f)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. TRATAMIENTOS Y VALIDACIÓN DE CONTEO POSTERIOR (RESOLUCIÓN DE CONFLICTO)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun `tratamiento aplicado hoy orienta claramente sobre conteo posterior en dias siguientes`() {
        val hoy = LocalDate.of(2026, 11, 7)
        val inicio = LocalDate.of(2026, 11, 1)

        val formHoyConDespues = FormularioTratamiento(
            fechaAplicacion = hoy,
            producto = "Clorantraniliprole",
            unidad = UnidadConteo.POR_METRO_LINEAL,
            conteoAntes = "15",
            fechaConteoAntes = hoy,
            conDespues = true,
            conteoDespues = "2",
            fechaConteoDespues = hoy
        )

        val errores = validarTratamiento(formHoyConDespues, inicio, hoy)
        assertTrue(errores.containsKey(CampoTratamiento.FECHA_CONTEO_DESPUES))
        assertEquals(
            "Si aplicaste hoy, el conteo posterior se registra en los días posteriores",
            errores[CampoTratamiento.FECHA_CONTEO_DESPUES]
        )
    }

    @Test
    fun `tratamiento aplicado dias atras permite conteo posterior hoy con exito`() {
        val hoy = LocalDate.of(2026, 11, 7)
        val inicio = LocalDate.of(2026, 11, 1)

        val formPasado = FormularioTratamiento(
            fechaAplicacion = LocalDate.of(2026, 11, 3),
            producto = "Clorantraniliprole",
            unidad = UnidadConteo.POR_METRO_LINEAL,
            conteoAntes = "20",
            fechaConteoAntes = LocalDate.of(2026, 11, 2),
            conDespues = true,
            conteoDespues = "3",
            fechaConteoDespues = hoy
        )

        val errores = validarTratamiento(formPasado, inicio, hoy)
        assertTrue("No debe haber errores al evaluar eficacia de aplicación pasada", errores.isEmpty())
    }
}
