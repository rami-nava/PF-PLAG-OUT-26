package com.example.plag_out

import androidx.test.core.app.ApplicationProvider
import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.fakes.FakeGDDService
import com.example.plag_out.fakes.FakeTerrenoDao
import com.example.plag_out.util.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NuevoTerrenoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: NuevoTerrenoViewModel
    private lateinit var gddService: FakeGDDService

    // Límites del recuadro de Argentina, según NuevoTerrenoViewModel.registrarTerreno
    private val latMin = -55.0
    private val latMax = -21.8
    private val lonMin = -73.6
    private val lonMax = -53.6

    @Before
    fun setup() {
        gddService = FakeGDDService()
        viewModel = NuevoTerrenoViewModel(
            context = ApplicationProvider.getApplicationContext(),
            repository = TerrenoRepository(FakeTerrenoDao()),
            gddService = gddService
        )
    }

    /** Carga un terreno válido y sobreescribe sólo la ubicación. */
    private fun conUbicacion(lat: Double, lon: Double) {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("200")
        viewModel.actualizarUbicacion(lat, lon)
    }

    private fun registrar(): NuevoTerrenoUIState {
        viewModel.registrarTerreno(onSuccess = {})
        return viewModel.state.value
    }

    private fun assertUbicacionAceptada(lat: Double, lon: Double) {
        conUbicacion(lat, lon)
        val estado = registrar()
        assertNull("Se esperaba que ($lat, $lon) fuera válida", estado.error)
        assertTrue("Se esperaba que ($lat, $lon) avanzara al guardado", estado.isLoading)
    }

    private fun assertUbicacionRechazada(lat: Double, lon: Double) {
        conUbicacion(lat, lon)
        val estado = registrar()
        assertEquals(
            "Se esperaba que ($lat, $lon) fuera rechazada por estar fuera de Argentina",
            "Ubicación fuera de la República Argentina",
            estado.error
        )
        assertTrue(
            "Una ubicación inválida no debe llegar a la red",
            gddService.llamadas.isEmpty()
        )
    }

    // ---------- Particiones de equivalencia: nombre ----------

    @Test
    fun `nombre vacio es rechazado`() {
        viewModel.actualizarNombre("")
        viewModel.actualizarArea("200")
        viewModel.actualizarUbicacion(-34.6, -58.4)

        assertEquals("El nombre del terreno no puede estar vacío", registrar().error)
    }

    @Test
    fun `nombre de solo espacios es rechazado`() {
        viewModel.actualizarNombre("    ")
        viewModel.actualizarArea("200")
        viewModel.actualizarUbicacion(-34.6, -58.4)

        assertEquals("El nombre del terreno no puede estar vacío", registrar().error)
    }

    // ---------- Particiones de equivalencia: hectáreas ----------

    @Test
    fun `hectareas no numericas son rechazadas`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("abc")
        viewModel.actualizarUbicacion(-34.6, -58.4)

        assertEquals("Las hectáreas deben ser un número decimal mayor a 0", registrar().error)
    }

    @Test
    fun `hectareas en cero son rechazadas`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("0")
        viewModel.actualizarUbicacion(-34.6, -58.4)

        assertEquals("Las hectáreas deben ser un número decimal mayor a 0", registrar().error)
    }

    @Test
    fun `hectareas negativas son rechazadas`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("-5")
        viewModel.actualizarUbicacion(-34.6, -58.4)

        assertEquals("Las hectáreas deben ser un número decimal mayor a 0", registrar().error)
    }

    @Test
    fun `hectareas decimales son aceptadas`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("0.5")
        viewModel.actualizarUbicacion(-34.6, -58.4)

        assertNull(registrar().error)
    }

    // ---------- Ubicación ausente ----------

    @Test
    fun `sin ubicacion es rechazado`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("200")

        assertEquals("Debe seleccionar la ubicación", registrar().error)
    }

    @Test
    fun `con latitud pero sin longitud es rechazado`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("200")
        viewModel.actualizarUbicacion(-34.6, null)

        assertEquals("Debe seleccionar la ubicación", registrar().error)
    }

    // ---------- Valores límite: recuadro de Argentina ----------

    @Test
    fun `ubicacion en el centro del pais es aceptada`() {
        assertUbicacionAceptada(-34.6, -58.4) // Buenos Aires
    }

    @Test
    fun `los limites del recuadro son inclusivos`() {
        assertUbicacionAceptada(latMin, lonMin)
        setup(); assertUbicacionAceptada(latMax, lonMax)
        setup(); assertUbicacionAceptada(latMin, lonMax)
        setup(); assertUbicacionAceptada(latMax, lonMin)
    }

    @Test
    fun `latitud apenas por debajo del minimo es rechazada`() {
        assertUbicacionRechazada(latMin - 0.1, -58.4)
    }

    @Test
    fun `latitud apenas por encima del maximo es rechazada`() {
        assertUbicacionRechazada(latMax + 0.1, -58.4)
    }

    @Test
    fun `longitud apenas por debajo del minimo es rechazada`() {
        assertUbicacionRechazada(-34.6, lonMin - 0.1)
    }

    @Test
    fun `longitud apenas por encima del maximo es rechazada`() {
        assertUbicacionRechazada(-34.6, lonMax + 0.1)
    }

    @Test
    fun `ubicacion en el hemisferio norte es rechazada`() {
        assertUbicacionRechazada(40.4, -3.7) // Madrid
    }

    @Test
    fun `ubicacion en Brasil es rechazada`() {
        assertUbicacionRechazada(-23.5, -46.6) // Sao Paulo: latitud válida, longitud fuera
    }

    // ---------- Precedencia de validaciones ----------

    @Test
    fun `el error de nombre tiene precedencia sobre el de hectareas`() {
        viewModel.actualizarNombre("")
        viewModel.actualizarArea("abc")

        assertEquals("El nombre del terreno no puede estar vacío", registrar().error)
    }

    @Test
    fun `el error de hectareas tiene precedencia sobre el de ubicacion`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("abc")
        // sin ubicación

        assertEquals("Las hectáreas deben ser un número decimal mayor a 0", registrar().error)
    }

    // ---------- Transición de estados ----------

    @Test
    fun `editar el nombre limpia el error previo`() {
        viewModel.actualizarNombre("")
        registrar()
        assertEquals("El nombre del terreno no puede estar vacío", viewModel.state.value.error)

        viewModel.actualizarNombre("Lote Norte")

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `resetState vuelve al estado inicial`() {
        viewModel.actualizarNombre("Lote Norte")
        viewModel.actualizarArea("200")
        viewModel.actualizarUbicacion(-34.6, -58.4)

        viewModel.resetState()

        assertEquals(NuevoTerrenoUIState(), viewModel.state.value)
    }
}
