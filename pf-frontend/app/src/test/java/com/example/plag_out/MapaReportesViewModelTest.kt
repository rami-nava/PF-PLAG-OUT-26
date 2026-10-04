package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.Fixtures
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MapaReportesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: MapaReportesViewModel

    /** Vista de todo el país, bien alejada. */
    private val vistaPais = VistaMapa(sur = -55.0, oeste = -75.0, norte = -21.0, este = -53.0, zoom = 4.2)

    /** Vista de unos pocos km alrededor de Buenos Aires, a zoom de reportes sueltos. */
    private val vistaCerca = VistaMapa(sur = -34.7, oeste = -58.5, norte = -34.5, este = -58.3, zoom = 12.0)

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = MapaReportesViewModel(gddService)
    }

    @Test
    fun `sin vista todavia no pide nada`() {
        assertEquals(0, gddService.llamadas.size)
        assertTrue(viewModel.state.value.cargando)
    }

    @Test
    fun `alejado pide grupos con la celda del zoom y un margen alrededor de lo visible`() {
        gddService.getReportesMapaResult = {
            Response.success(
                ReportesMapaResponse(
                    nivel = "grupos",
                    grupos = listOf(
                        GrupoReportesMapa(-34.6, -58.4, 37, "Alto"),
                        GrupoReportesMapa(-31.4, -64.2, 5, "Bajo")
                    )
                )
            )
        }

        viewModel.actualizarVista(vistaPais)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        val consulta = gddService.consultasMapa.single()
        assertEquals("grupos", consulta["nivel"])
        assertEquals(4, consulta["zoom"])
        assertEquals(celdaGradosParaZoom(4), consulta["celda_grados"] as Double, 1e-9)
        assertNull(consulta["limite"])
        // Pide más de lo que se ve, para que un desplazamiento chico no dispare otro pedido.
        assertTrue((consulta["sur"] as Double) < vistaPais.sur)
        assertTrue((consulta["este"] as Double) > vistaPais.este)

        assertEquals(NivelMapa.GRUPOS, estado.nivel)
        assertEquals(2, estado.grupos.size)
        assertTrue(estado.reportes.isEmpty())
        assertEquals("sin total explícito se suman los grupos", 42, estado.total)
    }

    @Test
    fun `de cerca pide reportes sueltos con limite y la celda por si hay que agrupar`() {
        val reportes = listOf(
            Fixtures.reporteDetalle(id = 1, cultivoNombre = "Maíz"),
            Fixtures.reporteDetalle(id = 2, cultivoNombre = "Soja", latitud = null, longitud = null)
        )
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = reportes))
        }

        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        val consulta = gddService.consultasMapa.single()
        assertEquals("reportes", consulta["nivel"])
        assertEquals(celdaGradosParaZoom(12), consulta["celda_grados"] as Double, 1e-9)
        assertEquals(LIMITE_REPORTES_MAPA, consulta["limite"])

        assertEquals(NivelMapa.REPORTES, estado.nivel)
        assertEquals("el que no tiene coordenadas no se puede dibujar", listOf(1), estado.reportes.map { it.id })
        assertEquals(1, estado.total)
        assertEquals(listOf("Maíz"), estado.cultivos)
    }

    @Test
    fun `moverse dentro de la zona ya cargada no vuelve a pedir`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = emptyList())) }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        val corrida = vistaCerca.copy(sur = -34.68, norte = -34.48, zoom = 12.6)
        viewModel.actualizarVista(corrida)

        assertEquals(1, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `salir de la zona cargada o cruzar el umbral de zoom vuelve a pedir`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = emptyList())) }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        viewModel.actualizarVista(vistaCerca.copy(oeste = -60.5, este = -60.3))
        esperarEstado(viewModel.state) { !it.actualizando }
        viewModel.actualizarVista(vistaCerca.copy(zoom = 9.0))
        esperarEstado(viewModel.state) { !it.actualizando }

        assertEquals(3, gddService.vecesLlamado("getReportesMapa"))
        assertEquals("grupos", gddService.consultasMapa.last()["nivel"])
    }

    @Test
    fun `los grupos se vuelven a pedir al cambiar de zoom entero aunque la zona ya este cargada`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(grupos = emptyList())) }
        viewModel.actualizarVista(vistaPais)
        esperarEstado(viewModel.state) { !it.cargando }

        viewModel.actualizarVista(vistaPais.copy(zoom = 4.8))
        assertEquals("mismo zoom entero, misma celda", 1, gddService.vecesLlamado("getReportesMapa"))

        viewModel.actualizarVista(vistaPais.copy(sur = -40.0, norte = -30.0, zoom = 5.1))
        esperarEstado(viewModel.state) { !it.actualizando }
        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
        assertEquals(5, gddService.consultasMapa.last()["zoom"])
    }

    @Test
    fun `los filtros viajan como parametros y cambiarlos vuelve a pedir`() {
        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = emptyList())) }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        val consultaSinFiltros = gddService.consultasMapa.single()
        listOf("ambito", "radio_km", "dias", "severidad", "plaga", "cultivo").forEach {
            assertNull("sin filtro, '$it' no viaja", consultaSinFiltros[it])
        }

        viewModel.actualizarFiltros(
            FiltrosMapa(
                ambito = AmbitoMapa.COMUNIDAD,
                radioKm = 50,
                dias = 30,
                severidad = "Alto",
                plaga = "Chicharrita",
                cultivo = "Maíz"
            )
        )
        esperarEstado(viewModel.state) { !it.actualizando }

        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
        val consulta = gddService.consultasMapa.last()
        assertEquals("comunidad", consulta["ambito"])
        assertEquals(50, consulta["radio_km"])
        assertEquals(30, consulta["dias"])
        assertEquals("Alto", consulta["severidad"])
        assertEquals("Chicharrita", consulta["plaga"])
        assertEquals("Maíz", consulta["cultivo"])
    }

    @Test
    fun `con demasiados reportes el servidor responde grupos aunque se pidan reportes`() {
        gddService.getReportesMapaResult = {
            Response.success(
                ReportesMapaResponse(nivel = "grupos", grupos = listOf(GrupoReportesMapa(-34.6, -58.4, 1200, "Medio")))
            )
        }

        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals("reportes", gddService.consultasMapa.single()["nivel"])
        assertEquals(NivelMapa.GRUPOS, estado.nivel)
        assertEquals(1200, estado.total)
        assertTrue(estado.reportes.isEmpty())
    }

    @Test
    fun `si llegaron grupos por exceso, acercarse dentro de la zona vuelve a pedir`() {
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "grupos", grupos = listOf(GrupoReportesMapa(-34.6, -58.4, 900))))
        }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        // Mismo zoom entero y dentro de la zona: con grupos no cambia nada, no se pide.
        viewModel.actualizarVista(vistaCerca.copy(sur = -34.68, norte = -34.52, zoom = 12.4))
        assertEquals(1, gddService.vecesLlamado("getReportesMapa"))

        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle())))
        }
        viewModel.actualizarVista(vistaCerca.copy(sur = -34.65, norte = -34.55, oeste = -58.45, este = -58.35, zoom = 13.0))

        val estado = esperarEstado(viewModel.state) { it.nivel == NivelMapa.REPORTES && !it.actualizando }
        assertEquals(2, gddService.vecesLlamado("getReportesMapa"))
        assertEquals(1, estado.reportes.size)
    }

    @Test
    fun `con reportes sueltos acercarse dentro de la zona no vuelve a pedir`() {
        gddService.getReportesMapaResult = {
            Response.success(ReportesMapaResponse(nivel = "reportes", reportes = listOf(Fixtures.reporteDetalle())))
        }
        viewModel.actualizarVista(vistaCerca)
        esperarEstado(viewModel.state) { !it.cargando }

        viewModel.actualizarVista(vistaCerca.copy(sur = -34.65, norte = -34.55, oeste = -58.45, este = -58.35, zoom = 13.0))

        assertEquals("ya se tienen todos los de la zona", 1, gddService.vecesLlamado("getReportesMapa"))
    }

    @Test
    fun `si el backend no tiene la ruta avisa y no cae a otro endpoint`() {
        gddService.getReportesMapaResult = { FakeGDDService.errorServidor(404) }

        viewModel.actualizarVista(vistaPais)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals("El mapa de reportes todavía no está disponible en el servidor.", estado.error)
        assertTrue(estado.grupos.isEmpty() && estado.reportes.isEmpty())
        assertEquals(0, gddService.vecesLlamado("getReportes"))
    }

    @Test
    fun `un error del servidor se informa sin pasar a modo local`() {
        gddService.getReportesMapaResult = { FakeGDDService.errorServidor(500) }

        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { !it.cargando }
        assertEquals("No se pudieron obtener los reportes del servidor.", estado.error)
    }

    @Test
    fun `sin conexion informa el error y se puede reintentar`() {
        gddService.getReportesMapaResult = { FakeGDDService.sinConexion() }
        viewModel.actualizarVista(vistaCerca)
        assertNotNull(esperarEstado(viewModel.state) { !it.cargando }.error)

        gddService.getReportesMapaResult = { Response.success(ReportesMapaResponse(reportes = listOf(Fixtures.reporteDetalle()))) }
        viewModel.actualizarVista(vistaCerca)

        val estado = esperarEstado(viewModel.state) { it.error == null && !it.actualizando }
        assertEquals(1, estado.reportes.size)
    }

    // ── piezas puras ──────────────────────────────────────────────────────────

    @Test
    fun `el nivel cambia en el umbral de zoom`() {
        assertEquals(NivelMapa.GRUPOS, nivelParaZoom(ZOOM_REPORTES_INDIVIDUALES - 0.01))
        assertEquals(NivelMapa.REPORTES, nivelParaZoom(ZOOM_REPORTES_INDIVIDUALES))
    }

    @Test
    fun `la celda se achica a la mitad con cada nivel de zoom`() {
        assertEquals(celdaGradosParaZoom(5) / 2, celdaGradosParaZoom(6), 1e-12)
    }

    @Test
    fun `la vista ampliada contiene a la original y no se sale del mundo`() {
        val ampliada = vistaCerca.ampliada()
        assertTrue(ampliada.contiene(vistaCerca))
        assertFalse(vistaCerca.contiene(ampliada))

        val mundo = VistaMapa(-89.0, -179.0, 89.0, 179.0, 3.0).ampliada()
        assertEquals(-85.0, mundo.sur, 0.0)
        assertEquals(180.0, mundo.este, 0.0)
    }

    @Test
    fun `contar filtros activos`() {
        assertEquals(0, FiltrosMapa().activos)
        assertEquals(2, FiltrosMapa(ambito = AmbitoMapa.PROPIOS, dias = 7).activos)
    }
}
