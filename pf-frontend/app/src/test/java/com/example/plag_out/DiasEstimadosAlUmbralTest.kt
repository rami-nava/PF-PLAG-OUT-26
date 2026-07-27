package com.example.plag_out

import com.example.plag_out.fakes.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DiasEstimadosAlUmbralTest {

    @Test
    fun `null cuando ya se alcanzo el umbral`() {
        val monitoreo = Fixtures.monitoreo(gddDiario = 10f, gddAcumulado = 500f, gddObjetivo = 500f, progreso = 100f)

        assertNull(diasEstimadosAlUmbral(monitoreo))
    }

    @Test
    fun `null cuando el promedio diario historico es cero`() {
        // gdd_acumulado 0 a lo largo de 9 dias transcurridos -> promedio 0, no hay como proyectar
        val monitoreo = Fixtures.monitoreo(
            gddDiario = 10f,
            gddAcumulado = 0f,
            gddObjetivo = 500f,
            progreso = 0f,
            fechaInicio = LocalDate.of(2026, 1, 1),
            fechaActualizacion = LocalDate.of(2026, 1, 10)
        )

        assertNull(diasEstimadosAlUmbral(monitoreo))
    }

    @Test
    fun `usa el promedio historico desde fecha de inicio y no el gdd de un solo dia`() {
        // 10 dias transcurridos, 200 acumulados -> promedio 20/dia. Si usara gdd_diario (5) el
        // resultado sería muy distinto (40 días en vez de 10).
        val monitoreo = Fixtures.monitoreo(
            gddDiario = 5f,
            gddAcumulado = 200f,
            gddObjetivo = 400f,
            progreso = 50f,
            fechaInicio = LocalDate.of(2026, 1, 1),
            fechaActualizacion = LocalDate.of(2026, 1, 11)
        )

        assertEquals(10, diasEstimadosAlUmbral(monitoreo))
    }

    @Test
    fun `redondea hacia arriba`() {
        // promedio = 100 acumulados / 9 dias = 11.11; restante = 145 - 100 = 45; 45 / 11.11 -> ceil = 5
        val monitoreo = Fixtures.monitoreo(
            gddDiario = 10f,
            gddAcumulado = 100f,
            gddObjetivo = 145f,
            progreso = 69f,
            fechaInicio = LocalDate.of(2026, 1, 1),
            fechaActualizacion = LocalDate.of(2026, 1, 10)
        )

        assertEquals(5, diasEstimadosAlUmbral(monitoreo))
    }

    @Test
    fun `sin fecha de inicio cae al gdd de hoy como aproximacion`() {
        // restante = 45; sin fecha_inicio no hay promedio historico posible -> usa gdd_diario (10)
        val monitoreo = Fixtures.monitoreo(
            gddDiario = 10f,
            gddAcumulado = 100f,
            gddObjetivo = 145f,
            progreso = 69f,
            fechaInicio = null
        )

        assertEquals(5, diasEstimadosAlUmbral(monitoreo))
    }

    @Test
    fun `si arranco hoy mismo sin dias transcurridos cae al gdd de hoy`() {
        val hoy = LocalDate.of(2026, 1, 10)
        val monitoreo = Fixtures.monitoreo(
            gddDiario = 8f,
            gddAcumulado = 50f,
            gddObjetivo = 90f,
            progreso = 55f,
            fechaInicio = hoy,
            fechaActualizacion = hoy
        )

        assertEquals(5, diasEstimadosAlUmbral(monitoreo))
    }
}
