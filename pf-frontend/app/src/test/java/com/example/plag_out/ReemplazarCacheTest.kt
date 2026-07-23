package com.example.plag_out

import com.example.plag_out.AlmacenamientoLocal.TerrenoRepository
import com.example.plag_out.fakes.FakeTerrenoDao
import com.example.plag_out.fakes.Fixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifica el contrato de `reemplazar*` de los repositorios
 */
class ReemplazarCacheTest {

    @Test
    fun `reemplazar borra antes de insertar`() = runTest {
        val dao = FakeTerrenoDao(inicial = listOf(Fixtures.terreno(id = 1)))
        val repo = TerrenoRepository(dao)

        repo.reemplazarTerrenos(listOf(Fixtures.terreno(id = 2), Fixtures.terreno(id = 3)))

        // El borrado debe ocurrir estrictamente antes de la inserción.
        assertEquals(listOf("deleteAll", "insertAll"), dao.operaciones)
    }

    @Test
    fun `reemplazar deja solo los datos nuevos`() = runTest {
        val dao = FakeTerrenoDao(inicial = listOf(Fixtures.terreno(id = 1)))
        val repo = TerrenoRepository(dao)

        repo.reemplazarTerrenos(listOf(Fixtures.terreno(id = 2)))

        val resultado = repo.obtenerTerrenos()
        assertEquals(listOf(2), resultado.map { it.terreno_id })
    }
}
