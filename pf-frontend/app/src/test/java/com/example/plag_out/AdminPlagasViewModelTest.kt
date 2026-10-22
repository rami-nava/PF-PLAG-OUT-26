package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
import com.google.gson.JsonParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class AdminPlagasViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: AdminPlagasViewModel

    private val carpocapsa = PlagaAdmin(
        id = 1, nombre = "Carpocapsa", nombre_cientifico = "Cydia pomonella",
        temp_base = 10.0, temp_max = 31.0, gdd_eclosion = 100.0, gdd_generacion = 550.0,
        cultivos_afectados = listOf(1), activo = true, monitoreos_activos = 3,
        tipo_desarrollo = "completo", temp_base_huevo = 11.0
    )
    private val formValido = FormularioPlaga(
        nombre = "Chicharrita", nombreCientifico = "Dalbulus maidis",
        tempBase = "10", tempMax = "32,5", gddEclosion = "120", gddGeneracion = "400",
        tipoDesarrollo = "incompleto", cultivos = setOf(2)
    )

    @Before
    fun setup() {
        gddService = FakeGDDService()
        gddService.adminGetCultivosResult = {
            Response.success(listOf(CultivoAdmin(1, "Manzano", "Malus domestica", activo = true)))
        }
        viewModel = AdminPlagasViewModel(gddService)
    }

    private fun <T> error(codigo: Int, detalle: String): Response<T> =
        Response.error(codigo, """{"detail":"$detalle"}""".toResponseBody("application/json".toMediaType()))

    @Test
    fun `cargar trae plagas y cultivos`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.cargado && it.cultivos.isNotEmpty() }
        assertEquals(listOf(carpocapsa), estado.plagas)
        assertFalse(estado.isLoading)
    }

    @Test
    fun `sin catalogo admin de cultivos usa los de cultivos`() {
        gddService.adminGetCultivosResult = { FakeGDDService.errorServidor(404) }
        gddService.getCultivosResult = { Response.success(listOf(CultivoResponse(2, "Maíz", "Zea mays"))) }
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.cargado && it.cultivos.isNotEmpty() }
        assertEquals(listOf(2), estado.cultivos.map { it.id })
        assertTrue(estado.cultivos.single().estaActivo)
    }

    @Test
    fun `marcarDesactualizado hace que la proxima carga vuelva a pedir plagas y cultivos`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado && it.cultivos.isNotEmpty() && !it.isLoading }

        viewModel.cargar()
        assertEquals(1, gddService.vecesLlamado("adminGetPlagas"))

        viewModel.marcarDesactualizado()
        viewModel.cargar()

        esperarEstado(viewModel.state) { !it.desactualizado && !it.isRefreshing }
        assertEquals(2, gddService.vecesLlamado("adminGetPlagas"))
        assertEquals(2, gddService.vecesLlamado("adminGetCultivos"))
    }

    @Test
    fun `403 requiere_admin marca sinPermiso`() {
        gddService.adminGetPlagasResult = { error(403, "requiere_admin") }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertTrue(estado.sinPermiso)
    }

    @Test
    fun `404 avisa que el backend no tiene el endpoint`() {
        gddService.adminGetPlagasResult = { FakeGDDService.errorServidor(404) }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertEquals("Esta función todavía no está disponible en el servidor.", estado.error)
        assertFalse(estado.sinPermiso)
    }

    @Test
    fun `crear manda todos los campos con coma decimal convertida y agrega la plaga`() {
        val creada = PlagaAdmin(id = 9, nombre = "Chicharrita", nombre_cientifico = "Dalbulus maidis", activo = true)
        gddService.adminCrearPlagaResult = { Response.success(creada) }
        var guardado = false

        viewModel.guardar(null, formValido) { guardado = true }

        esperarEstado(viewModel.state) { it.plagas.contains(creada) }
        assertTrue(guardado)
        val enviado = gddService.ultimaPlagaEnviada!!
        assertEquals(32.5, enviado.temp_max!!, 0.0)
        assertEquals(listOf(2), enviado.cultivos_afectados)
        assertNull(enviado.activo)
        assertEquals("incompleto", enviado.tipo_desarrollo)
        assertEquals("biofix", enviado.gdd_generacion_desde)
        // Sin base propia del huevo no se manda: el backend usa la temperatura base.
        assertNull(enviado.temp_base_huevo)
    }

    @Test
    fun `formulario invalido no llama al backend`() {
        viewModel.guardar(null, formValido.copy(tempBase = "40")) { }

        assertEquals(0, gddService.vecesLlamado("adminCrearPlaga"))
    }

    @Test
    fun `editar manda solo los campos cambiados`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado }
        gddService.adminActualizarPlagaResult = { Response.success(carpocapsa.copy(gdd_eclosion = 110.0)) }

        viewModel.guardar(carpocapsa, FormularioPlaga.desde(carpocapsa).copy(gddEclosion = "110")) { }

        esperarEstado(viewModel.state) { it.plagas.first().gdd_eclosion == 110.0 }
        assertEquals(JsonParser.parseString("""{"gdd_eclosion":110.0}"""), gddService.ultimaEdicionPlaga)
    }

    @Test
    fun `editar sin cambios no llama al backend`() {
        var guardado = false

        viewModel.guardar(carpocapsa, FormularioPlaga.desde(carpocapsa)) { guardado = true }

        assertTrue(guardado)
        assertEquals(0, gddService.vecesLlamado("adminActualizarPlaga"))
    }

    @Test
    fun `nombre duplicado muestra el error de la accion`() {
        gddService.adminCrearPlagaResult = { error(409, "plaga_duplicada") }

        viewModel.guardar(null, formValido) { }

        val estado = esperarEstado(viewModel.state) { it.errorAccion != null }
        assertEquals("Ya existe una plaga con ese nombre o nombre científico.", estado.errorAccion)
        assertFalse(estado.guardando)
    }

    @Test
    fun `dar de baja y reactivar reemplazan la plaga en la lista`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado }

        gddService.adminDesactivarPlagaResult = { Response.success(carpocapsa.copy(activo = false)) }
        viewModel.desactivar(carpocapsa.id)
        esperarEstado(viewModel.state) { !it.plagas.first().estaActiva }

        gddService.adminActualizarPlagaResult = { Response.success(carpocapsa) }
        viewModel.reactivar(carpocapsa.id)
        esperarEstado(viewModel.state) { it.plagas.first().estaActiva && !it.guardando }
        assertEquals(JsonParser.parseString("""{"activo":true}"""), gddService.ultimaEdicionPlaga)
    }

    @Test
    fun `vaciar la base del huevo manda null explicito para volver a la temperatura base`() {
        gddService.adminGetPlagasResult = { Response.success(listOf(carpocapsa)) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado }
        gddService.adminActualizarPlagaResult = { Response.success(carpocapsa.copy(temp_base_huevo = null)) }

        viewModel.guardar(carpocapsa, FormularioPlaga.desde(carpocapsa).copy(tempBaseHuevo = "")) { }

        esperarEstado(viewModel.state) { it.plagas.first().temp_base_huevo == null }
        val enviado = gddService.ultimaEdicionPlaga!!
        assertEquals(setOf("temp_base_huevo"), enviado.keySet())
        assertTrue(enviado.get("temp_base_huevo").isJsonNull)
    }

    @Test
    fun `cambiar la generacion y el tipo de desarrollo los manda como texto`() {
        val cambios = requestDeEdicion(
            carpocapsa,
            FormularioPlaga.desde(carpocapsa).copy(generacionDesde = "eclosion", tipoDesarrollo = "incompleto")
        )

        assertEquals(
            JsonParser.parseString("""{"gdd_generacion_desde":"eclosion","tipo_desarrollo":"incompleto"}"""),
            cambios
        )
    }

    @Test
    fun `una plaga sin generacion declarada se toma como desde el biofix`() {
        assertNull(requestDeEdicion(carpocapsa.copy(gdd_generacion_desde = null), FormularioPlaga.desde(carpocapsa)))
    }

    @Test
    fun `valida rangos como el backend y exige el tipo de desarrollo`() {
        val errores = validarPlaga(
            formValido.copy(tempBase = "-51", tempMax = "61", tempBaseHuevo = "x", gddGeneracion = "100001", tipoDesarrollo = null)
        )

        assertEquals(
            setOf(CampoPlaga.TEMP_BASE, CampoPlaga.TEMP_MAX, CampoPlaga.TEMP_BASE_HUEVO, CampoPlaga.GDD_GENERACION, CampoPlaga.TIPO_DESARROLLO),
            errores.keys
        )
        assertTrue(validarPlaga(formValido.copy(tempBaseHuevo = "9,5")).isEmpty())
        assertEquals(
            "Tiene que ser menor que la máxima",
            validarPlaga(formValido.copy(tempBaseHuevo = "33"))[CampoPlaga.TEMP_BASE_HUEVO]
        )
    }
}

class ParametrosCitadosTest {
    @org.junit.Test
    fun `muestra cada valor con su unidad y los textos con la etiqueta del panel`() {
        org.junit.Assert.assertEquals("12,6 °C", mostrarValorTermico("temp_base", 12.6))
        org.junit.Assert.assertEquals("648 GDD", mostrarValorTermico("gdd_generacion", 648.0))
        org.junit.Assert.assertEquals("Desde la eclosión", mostrarValorTermico("gdd_generacion_desde", "eclosion"))
        org.junit.Assert.assertEquals("Metamorfosis incompleta", mostrarValorTermico("tipo_desarrollo", "incompleto"))
        org.junit.Assert.assertEquals("—", mostrarValorTermico("temp_base_huevo", null))
    }

    @org.junit.Test
    fun `la fuente enlaza al doi y si no a su url`() {
        org.junit.Assert.assertEquals("https://doi.org/10.1111/afe.12118", FuenteTermica("a", "Cita", doi = "10.1111/afe.12118").enlace)
        org.junit.Assert.assertEquals("https://x.org", FuenteTermica("b", "Cita", url = "https://x.org").enlace)
        org.junit.Assert.assertNull(FuenteTermica("c", "Cita").enlace)
    }
}

class AvisoDeFuenteTest {
    private fun plaga(referencia: ReferenciaTermica?) =
        PlagaAdmin(id = 1, nombre = "Chinche", nombre_cientifico = "Nezara viridula", referencia_termica = referencia)

    @org.junit.Test
    fun `avisa si no hay modelo o si una edicion se aparto de la fuente`() {
        org.junit.Assert.assertNull(avisoDeFuente(plaga(null)))
        org.junit.Assert.assertNull(avisoDeFuente(plaga(ReferenciaTermica("validado", emptyList()))))
        org.junit.Assert.assertEquals(
            "Un parámetro difiere de su fuente citada",
            avisoDeFuente(plaga(ReferenciaTermica("validado", listOf("temp_base"))))
        )
        org.junit.Assert.assertEquals(
            "Sin modelo térmico validado: no admite monitoreos nuevos",
            avisoDeFuente(plaga(ReferenciaTermica("sin_modelo", emptyList())))
        )
    }
}
