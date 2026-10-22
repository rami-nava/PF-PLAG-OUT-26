package com.example.plag_out

import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.util.MainDispatcherRule
import com.example.plag_out.util.esperarEstado
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
class AdminCultivosViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var gddService: FakeGDDService
    private lateinit var viewModel: AdminCultivosViewModel

    private val carpocapsa = PlagaDeCultivo(1, "Carpocapsa", "Cydia pomonella", activo = true, monitoreos_activos = 4)
    private val manzano = CultivoAdmin(
        id = 3, nombre = "Manzano", nombre_cientifico = "Malus domestica", activo = true,
        plantaciones_activas = 12, monitoreos_activos = 30, plagas = listOf(carpocapsa)
    )
    private val maiz = CultivoAdmin(id = 5, nombre = "Maíz", nombre_cientifico = "Zea mays", activo = false)

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = AdminCultivosViewModel(gddService)
    }

    private fun <T> error(codigo: Int, cuerpo: String): Response<T> =
        Response.error(codigo, cuerpo.toResponseBody("application/json".toMediaType()))

    private fun cargarLista(vararg cultivos: CultivoAdmin) {
        gddService.adminGetCultivosResult = { Response.success(cultivos.toList()) }
        viewModel.cargar()
        esperarEstado(viewModel.state) { it.cargado && !it.isLoading }
    }

    // ---------- funciones puras ----------

    @Test
    fun `filtrarCultivos busca por nombre o cientifico y pone los activos primero`() {
        val cultivos = listOf(maiz, manzano, CultivoAdmin(7, "Vid", "Vitis vinifera"))

        assertEquals(listOf(3, 7, 5), filtrarCultivos(cultivos, "", FiltroCultivos.TODOS).map { it.id })
        assertEquals(listOf(5), filtrarCultivos(cultivos, " zea ", FiltroCultivos.TODOS).map { it.id })
        assertEquals(listOf(5), filtrarCultivos(cultivos, "", FiltroCultivos.INACTIVOS).map { it.id })
        assertTrue(filtrarCultivos(cultivos, "malus", FiltroCultivos.INACTIVOS).isEmpty())
    }

    @Test
    fun `validarCultivo exige ambos nombres y limita el largo`() {
        assertEquals(
            setOf(CampoCultivo.NOMBRE, CampoCultivo.NOMBRE_CIENTIFICO),
            validarCultivo(FormularioCultivo(nombre = "  ", nombreCientifico = "")).keys
        )
        assertEquals(
            setOf(CampoCultivo.NOMBRE),
            validarCultivo(FormularioCultivo(nombre = "a".repeat(81), nombreCientifico = "Zea mays")).keys
        )
        assertTrue(validarCultivo(FormularioCultivo(nombre = "Maíz", nombreCientifico = "Zea mays")).isEmpty())
    }

    @Test
    fun `plagasParaAgregar ofrece solo activas que no lo afectan`() {
        val plagas = listOf(
            PlagaAdmin(1, "Carpocapsa", "Cydia pomonella", activo = true),
            PlagaAdmin(2, "Chicharrita", "Dalbulus maidis", activo = true),
            PlagaAdmin(3, "Pulgón", "Aphis pomi", activo = false)
        )

        assertEquals(listOf(2), plagasParaAgregar(plagas, vinculadas = setOf(1), busqueda = "").map { it.id })
        assertTrue(plagasParaAgregar(plagas, vinculadas = setOf(1), busqueda = "pulg").isEmpty())
    }

    // ---------- carga ----------

    @Test
    fun `404 sin endpoint marca no disponible`() {
        gddService.adminGetCultivosResult = { error(404, """{"detail":"Not Found"}""") }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.error != null && !it.isLoading }
        assertTrue(estado.noDisponible)
        assertFalse(estado.sinPermiso)
    }

    @Test
    fun `403 requiere_admin marca sinPermiso`() {
        gddService.adminGetCultivosResult = { error(403, """{"detail":"requiere_admin"}""") }

        viewModel.cargar()

        val estado = esperarEstado(viewModel.state) { it.error != null }
        assertTrue(estado.sinPermiso)
    }

    @Test
    fun `abrirDetalle trae contadores frescos y el catalogo de plagas`() {
        cargarLista(manzano.copy(plagas = listOf(carpocapsa.copy(monitoreos_activos = null))))
        gddService.adminGetCultivoResult = { Response.success(manzano) }
        gddService.adminGetPlagasResult = { Response.success(listOf(PlagaAdmin(2, "Chicharrita", "Dalbulus maidis"))) }

        viewModel.abrirDetalle(manzano.id)

        val estado = esperarEstado(viewModel.state) { !it.cargandoDetalle && it.plagas.isNotEmpty() }
        assertEquals(4, estado.cultivos.single().plagas!!.single().monitoreos_activos)
    }

    // ---------- alta y edición ----------

    @Test
    fun `crear manda nombres sin espacios y las plagas elegidas`() {
        cargarLista(manzano)
        val creado = CultivoAdmin(9, "Vid", "Vitis vinifera", activo = true)
        gddService.adminCrearCultivoResult = { Response.success(creado) }
        var guardado = false

        viewModel.guardar(null, FormularioCultivo(" Vid ", "Vitis vinifera ", plagas = setOf(2, 1))) { guardado = true }

        esperarEstado(viewModel.state) { it.cultivos.contains(creado) }
        assertTrue(guardado)
        assertEquals(CultivoAdminRequest("Vid", "Vitis vinifera", plagas = listOf(1, 2)), gddService.ultimoCultivoEnviado)
    }

    @Test
    fun `editar manda solo el campo cambiado`() {
        cargarLista(manzano)
        gddService.adminActualizarCultivoResult = { Response.success(manzano.copy(nombre = "Manzana")) }

        viewModel.guardar(manzano, FormularioCultivo.desde(manzano).copy(nombre = "Manzana")) { }

        esperarEstado(viewModel.state) { it.cultivos.single().nombre == "Manzana" }
        assertEquals(CultivoAdminRequest(nombre = "Manzana"), gddService.ultimoCultivoEnviado)
    }

    @Test
    fun `editar sin cambios no llama al backend`() {
        var guardado = false

        viewModel.guardar(manzano, FormularioCultivo.desde(manzano)) { guardado = true }

        assertTrue(guardado)
        assertEquals(0, gddService.vecesLlamado("adminActualizarCultivo"))
    }

    @Test
    fun `formulario invalido no llama al backend`() {
        viewModel.guardar(null, FormularioCultivo(nombre = "Vid")) { }

        assertEquals(0, gddService.vecesLlamado("adminCrearCultivo"))
    }

    @Test
    fun `duplicado marca el campo que choca`() {
        gddService.adminCrearCultivoResult = {
            error(409, """{"detail":"cultivo_duplicado","campo":"nombre_cientifico"}""")
        }

        viewModel.guardar(null, FormularioCultivo("Manzano 2", "Malus domestica")) { }

        val estado = esperarEstado(viewModel.state) { it.errorAccion != null && !it.guardando }
        assertEquals("Ya existe un cultivo con ese nombre o nombre científico.", estado.errorAccion)
        assertEquals(setOf(CampoCultivo.NOMBRE_CIENTIFICO), estado.erroresCampo.keys)

        viewModel.descartarErrorCampo(CampoCultivo.NOMBRE_CIENTIFICO)
        assertTrue(viewModel.state.value.erroresCampo.isEmpty())
    }

    @Test
    fun `dar de baja y reactivar reemplazan el cultivo`() {
        cargarLista(manzano)

        gddService.adminDesactivarCultivoResult = { Response.success(manzano.copy(activo = false)) }
        viewModel.desactivar(manzano.id)
        esperarEstado(viewModel.state) { !it.cultivos.single().estaActivo }

        gddService.adminActualizarCultivoResult = { Response.success(manzano) }
        viewModel.reactivar(manzano.id)
        esperarEstado(viewModel.state) { it.cultivos.single().estaActivo && !it.guardando }
        assertEquals(CultivoAdminRequest(activo = true), gddService.ultimoCultivoEnviado)
    }

    // ---------- plagas del cultivo ----------

    @Test
    fun `agregar y quitar plagas solo cambia el formulario`() {
        val form = FormularioCultivo.desde(manzano).agregar(2).quitar(carpocapsa.id)
        assertEquals(CultivoAdminRequest(plagas = listOf(2)), requestDeEdicionCultivo(manzano, form))
        // Volver a sumar la que se había quitado deja todo como estaba
        assertNull(requestDeEdicionCultivo(manzano, form.quitar(2).agregar(carpocapsa.id)))
        assertTrue(gddService.llamadas.isEmpty())
    }

    @Test
    fun `guardar manda nombres y plagas en un solo PATCH`() {
        cargarLista(manzano)
        val chicharrita = PlagaDeCultivo(2, "Chicharrita", "Dalbulus maidis", activo = true, monitoreos_activos = 0)
        gddService.adminActualizarCultivoResult = {
            Response.success(manzano.copy(nombre = "Manzana", plagas = listOf(carpocapsa, chicharrita)))
        }
        var guardado = false

        val form = FormularioCultivo.desde(manzano).copy(nombre = "Manzana").agregar(2)
        viewModel.guardar(manzano, form) { guardado = true }

        val estado = esperarEstado(viewModel.state) { !it.guardando && guardado }
        assertEquals(1, gddService.vecesLlamado("adminActualizarCultivo"))
        assertEquals(CultivoAdminRequest(nombre = "Manzana", plagas = listOf(1, 2)), gddService.ultimoCultivoEnviado)
        assertEquals(listOf(carpocapsa, chicharrita), estado.cultivos.single().plagas)
    }

    @Test
    fun `quitar la ultima plaga de un cultivo muestra el 409 y no cambia nada`() {
        cargarLista(manzano)
        gddService.adminActualizarCultivoResult = { error(409, """{"detail":"ultimo_cultivo_de_plaga"}""") }
        var guardado = false

        viewModel.guardar(manzano, FormularioCultivo.desde(manzano).quitar(carpocapsa.id).agregar(2)) { guardado = true }

        val estado = esperarEstado(viewModel.state) { it.errorAccion != null && !it.guardando }
        assertEquals(CultivoAdminRequest(plagas = listOf(2)), gddService.ultimoCultivoEnviado)
        assertEquals("Es el único cultivo de esta plaga. Agregale otro o dala de baja desde Plagas.", estado.errorAccion)
        assertEquals(listOf(carpocapsa), estado.cultivos.single().plagas)
        assertFalse(guardado)
    }

    @Test
    fun `agregar una plaga dada de baja explica como reactivarla`() {
        cargarLista(manzano)
        gddService.adminActualizarCultivoResult = { error(409, """{"detail":"plaga_inactiva"}""") }

        viewModel.guardar(manzano, FormularioCultivo.desde(manzano).agregar(3)) { }

        val estado = esperarEstado(viewModel.state) { it.errorAccion != null }
        assertEquals("Esa plaga está dada de baja. Reactivala desde Plagas para vincularla.", estado.errorAccion)
    }

    @Test
    fun `marcarDesactualizado hace que la proxima carga vaya al servidor`() {
        cargarLista(manzano)
        viewModel.cargar()
        assertEquals(1, gddService.vecesLlamado("adminGetCultivos"))

        viewModel.marcarDesactualizado()
        viewModel.cargar()

        esperarEstado(viewModel.state) { !it.desactualizado && !it.isRefreshing }
        assertEquals(2, gddService.vecesLlamado("adminGetCultivos"))
    }
}
