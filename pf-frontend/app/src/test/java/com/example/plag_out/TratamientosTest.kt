package com.example.plag_out

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TratamientosTest {
    private val hoy = LocalDate.of(2026, 10, 22)
    private val inicio = LocalDate.of(2026, 9, 1)
    private val valido = FormularioTratamiento(
        fechaAplicacion = LocalDate.of(2026, 10, 1),
        producto = "  Clorantraniliprole ",
        unidad = UnidadConteo.POR_METRO_LINEAL,
        conteoAntes = "12",
        fechaConteoAntes = LocalDate.of(2026, 9, 30)
    )
    private val enviado = TratamientoResponse(
        id = 7, monitoreo_id = 3, fecha_aplicacion = "2026-10-01", producto = "Clorantraniliprole",
        unidad_conteo = "por_metro_lineal", conteo_antes = 12.0, fecha_conteo_antes = "2026-09-30",
        conteo_despues = 3.0, fecha_conteo_despues = "2026-10-08", testigo_antes = 10.0, testigo_despues = 9.0
    )

    @Test
    fun `un formulario valido no tiene errores y el alta recorta el producto`() {
        assertTrue(validarTratamiento(valido, inicio, hoy).isEmpty())
        val alta = valido.aRequest("clave")
        assertEquals("Clorantraniliprole", alta.producto)
        assertEquals("2026-10-01", alta.fecha_aplicacion)
        assertNull(alta.conteo_despues)
        assertNull(alta.testigo_antes)
    }

    @Test
    fun `valida fechas y conteos con las reglas del backend`() {
        val errores = validarTratamiento(
            valido.copy(
                fechaAplicacion = LocalDate.of(2026, 8, 31), // antes del inicio del monitoreo
                producto = "",
                conteoAntes = "-1",
                conTestigo = true,
                testigoAntes = "x",
                conDespues = true,
                conteoDespues = "100001",
                fechaConteoDespues = LocalDate.of(2026, 8, 31)
            ),
            inicio,
            hoy
        )
        assertEquals(
            setOf(
                CampoTratamiento.FECHA_APLICACION, CampoTratamiento.PRODUCTO, CampoTratamiento.CONTEO_ANTES,
                CampoTratamiento.FECHA_CONTEO_ANTES, CampoTratamiento.TESTIGO_ANTES, CampoTratamiento.CONTEO_DESPUES,
                CampoTratamiento.FECHA_CONTEO_DESPUES
            ),
            errores.keys
        )
        assertEquals(
            "No puede ser futura",
            validarTratamiento(valido.copy(fechaAplicacion = hoy.plusDays(1)), inicio, hoy)[CampoTratamiento.FECHA_APLICACION]
        )
        // El conteo posterior el mismo día de la aplicación no sirve.
        assertEquals(
            "Tiene que ser después de aplicar",
            validarTratamiento(valido.copy(conDespues = true, conteoDespues = "2", fechaConteoDespues = valido.fechaAplicacion), inicio, hoy)[CampoTratamiento.FECHA_CONTEO_DESPUES]
        )
    }

    @Test
    fun `el testigo posterior es opcional y la coma decimal vale`() {
        val form = valido.copy(conTestigo = true, testigoAntes = "10,5", conDespues = true, conteoDespues = "2,5", fechaConteoDespues = hoy)
        assertTrue(validarTratamiento(form, inicio, hoy).isEmpty())
        val alta = form.aRequest("k")
        assertEquals(10.5, alta.testigo_antes!!, 0.0)
        assertEquals(2.5, alta.conteo_despues!!, 0.0)
        assertNull(alta.testigo_despues)
    }

    @Test
    fun `la edicion manda solo lo cambiado y null explicito para deshacer`() {
        val sinCambios = FormularioTratamiento.desde(enviado)
        assertNull(cambiosDeTratamiento(enviado, sinCambios))

        val sinPosterior = sinCambios.copy(conDespues = false)
        assertEquals(
            JsonParser.parseString("""{"conteo_despues":null,"fecha_conteo_despues":null,"testigo_despues":null}"""),
            cambiosDeTratamiento(enviado, sinPosterior)
        )
        assertEquals(
            JsonParser.parseString("""{"producto":"Spinetoram"}"""),
            cambiosDeTratamiento(enviado, sinCambios.copy(producto = "Spinetoram"))
        )
    }

    @Test
    fun `un alta que cambio mientras viajaba se reenvia como edicion sin la clave`() {
        val alta = gsonTratamientos.toJson(valido.copy(conDespues = true, conteoDespues = "3", fechaConteoDespues = hoy).aRequest("k"))
        val edicion = JsonParser.parseString(altaComoEdicion(alta)).asJsonObject
        assertTrue("idempotency_key" !in edicion.keySet())
        assertEquals(3.0, edicion.get("conteo_despues").asDouble, 0.0)
        // Los null viajan explícitos: la edición deja el tratamiento exactamente como el alta.
        assertTrue(edicion.get("testigo_antes").isJsonNull)
    }

    @Test
    fun `el resultado dice que metodo uso y por que falta la cifra`() {
        val ht = resultadoDeTratamiento(enviado.copy(metodo = "henderson_tilton", eficacia_pct = 87.5, reduccion_observada_pct = 90.0))
        assertEquals("Eficacia: 87,5 %", ht.titulo)
        assertTrue(ht.detalle!!.contains("franja sin tratar"))

        val reduccion = resultadoDeTratamiento(enviado.copy(metodo = "reduccion_observada", reduccion_observada_pct = 75.0))
        assertEquals("Reducción observada: 75 %", reduccion.titulo)
        assertTrue(reduccion.detalle!!.contains("mortalidad natural"))

        val sinTestigoUsable = resultadoDeTratamiento(
            enviado.copy(metodo = "reduccion_observada", reduccion_observada_pct = 75.0, sin_calculo = "falta_testigo_despues")
        )
        assertTrue(sinTestigoUsable.detalle!!.startsWith("Falta el conteo posterior de la franja sin tratar."))

        assertEquals("La plaga aumentó un 50 %", resultadoDeTratamiento(enviado.copy(metodo = "reduccion_observada", reduccion_observada_pct = -50.0)).titulo)
        assertEquals("Falta el conteo posterior", resultadoDeTratamiento(enviado.copy(metodo = null, sin_calculo = "falta_conteo_despues")).titulo)
        assertEquals("Sin plaga antes de aplicar", resultadoDeTratamiento(enviado.copy(metodo = null, sin_calculo = "conteo_antes_cero")).titulo)
    }
}
