package com.example.plag_out

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesAdminTest {

    @Test
    fun `cada rol arranca en su home y sin rol se entra como usuario`() {
        assertEquals(RUTA_ADMIN_PLAGAS, homePara(ROL_ADMIN))
        assertEquals(RUTA_HOME_USUARIO, homePara(ROL_USUARIO))
        assertEquals(RUTA_HOME_USUARIO, homePara(null))
    }

    @Test
    fun `solo las rutas con prefijo admin son del panel`() {
        assertTrue(esRutaAdmin(RUTA_ADMIN_USUARIO))
        assertFalse(esRutaAdmin("monitoreos"))
        assertFalse(esRutaAdmin(null))
    }

    @Test
    fun `usuario sin campo rol en el JSON no es admin`() {
        val json = """{"id":"1","email":"a@b.com","nombre":"A","apellido":"B","cargo":"Productor","fecha_creacion":"2026-01-01"}"""
        val gson = GsonBuilder()
            .registerTypeAdapter(java.time.LocalDate::class.java, JsonDeserializer { j, _, _ -> java.time.LocalDate.parse(j.asString) })
            .create()
        val usuario = gson.fromJson(json, UsuarioResponse::class.java)
        assertNull(usuario.rol)
        assertFalse(usuario.esAdmin())
    }

    @Test
    fun `validacion exige temp base menor que la maxima, GDD positivos y cultivos`() {
        val errores = validarPlaga(
            FormularioPlaga(
                nombre = "X", nombreCientifico = "Y",
                tempBase = "30", tempMax = "10", gddEclosion = "0", gddGeneracion = "abc"
            )
        )
        assertEquals(
            setOf(CampoPlaga.TEMP_MAX, CampoPlaga.GDD_ECLOSION, CampoPlaga.GDD_GENERACION, CampoPlaga.CULTIVOS),
            errores.keys
        )
    }

    @Test
    fun `temperatura base negativa y coma decimal son validas`() {
        val errores = validarPlaga(
            FormularioPlaga(
                nombre = "X", nombreCientifico = "Y", tempBase = "-2,5", tempMax = "30",
                gddEclosion = "100", gddGeneracion = "500", cultivos = setOf(1)
            )
        )
        assertTrue(errores.isEmpty())
    }

    @Test
    fun `filtro de plagas busca por nombre cientifico y ordena activas primero`() {
        val plagas = listOf(
            PlagaAdmin(1, "Pulgón", "Aphis gossypii", activo = false),
            PlagaAdmin(2, "Carpocapsa", "Cydia pomonella", activo = true),
            PlagaAdmin(3, "Arañuela", "Tetranychus urticae", activo = true)
        )
        assertEquals(listOf(3, 2, 1), filtrarPlagas(plagas, "", FiltroPlagas.TODAS).map { it.id })
        assertEquals(listOf(2), filtrarPlagas(plagas, "cydia", FiltroPlagas.TODAS).map { it.id })
        assertEquals(listOf(1), filtrarPlagas(plagas, "", FiltroPlagas.INACTIVAS).map { it.id })
    }
}
