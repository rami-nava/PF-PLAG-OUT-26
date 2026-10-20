# Especificación y Propuesta: Módulo de Dashboards y Visualización

**Proyecto:** PF-PLAG-OUT-26 (Plag-Out)  
**Rama de desarrollo:** `feature/dashboards-metricas`  
**Estado:** Fase 1 y 2 implementadas (Tableros interactivos para Productor, Agrónomo e Histórico de Campañas)

---

## 1. Objetivo del Módulo

Desarrollar un panel de control analítico y visualización de datos (Dashboards) integrado en la aplicación Android. El objetivo es transformar los datos dispersos de terrenos, plantaciones, monitoreos GDD, reportes de campo y alertas de Machine Learning en **indicadores ejecutivos clave (KPIs)** y **gráficos interactivos** para la toma de decisiones fitosanitarias, adaptados según la perspectiva del usuario (Productor, Ingeniero Agrónomo y Administrador).

---

## 2. Jerarquía del Modelo de Datos en Campo

Para garantizar la consistencia en el cálculo de métricas y superficies:

* **Terreno (El "Lote" físico):** Parcela catastral con delimitación geográfica (`latitud`, `longitud`) y superficie fija en **hectáreas** (`area_hectareas`).
* **Plantación (La Siembra):** Ocupa la superficie total del terreno y asocia un cultivo (`cultivo_nombre`), fecha de siembra (`fecha_siembra`) y estado de actividad.
* **Monitoreo (Vigilancia de Plaga):** Asociación de una plantación con una plaga específica, registrando el umbral de alerta, los ciclos fenológicos y la acumulación de calor (`gdd_acumulado` vs `gdd_objetivo`).

---

## 3. Tableros y Vistas Implementadas

El sistema dispone de un selector superior dinámico que organiza el dashboard en tres perspectivas especializadas:

### 3.1. 🌾 Vista Productor (Operativa y Toma de Decisiones)
*Diseñada para resolver rápidamente el estado de urgencia en los lotes del campo:*

1. **Semáforo Donut de Superficie en Riesgo:**
   * Gráfico circular interactivo (`Canvas`) que clasifica la superficie total en:
     * 🟢 **Bajo control (Verde):** Lotes en nivel de alerta 0 ($IRA < 30\%$).
     * 🟡 **Seguimiento cercano (Amarillo):** Lotes en nivel de alerta 1 ($30\% \le IRA < 70\%$).
     * 🔴 **Alerta crítica (Rojo):** Lotes en nivel de alerta 2 o 3 ($IRA \ge 70\%$).
   * Indicador central con total de hectáreas y cantidad de lotes bajo monitoreo.
2. **Prioridad de Acción por Lote (Ranking de Criticidad):**
   * Ordena los lotes de mayor a menor urgencia fitosanitaria.
   * Visualiza nombre del lote, cultivo, plaga activa, hectáreas, porcentaje térmico GDD y badge de recomendación inmediata (*"⚠️ Superó umbral - Aplicar / Revisar hoy"* vs *"🌱 Ventana activa de desarrollo"*).
3. **Plagas Incidentes por Cultivo desde la Siembra:**
   * Comparativa por cultivo (Maíz, Soja, Trigo, etc.) que computa los días transcurridos desde la siembra (`ChronoUnit.DAYS`).
   * Barras proporcionales horizontales con la frecuencia relativa de cada plaga sobre ese cultivo.

---

### 3.2. 🔬 Vista Ingeniero Agrónomo (Fenología, GDD y Ventanas Biológicas)
*Diseñada con rigor técnico para la prescripción agronómica y control fitosanitario:*

1. **Ventana Óptima de Aplicación Fitosanitaria (GDD Lifecycle Gauge):**
   * Barra tricolor que mapea el ciclo de vida del insecto según el progreso de grados día:
     * **Fase 1: Incubación / Huevo (0% - 40% GDD):** No susceptible a tratamientos de contacto. Recomendación: *Esperar*.
     * **Fase 2: Ventana Óptima Abierta (40% - 85% GDD):** Estadio de larva chica (L1 - L3). Máxima vulnerabilidad a insecticidas y biológicos.
     * **Fase 3: Ventana en Cierre / Daño Avanzado (> 85% GDD):** Larva desarrollada (L4 - L6) o eclosión masiva. Riesgo de daño económico irreversible.
2. **Eficacia de Tratamientos Aplicados (Post-Control):**
   * Evaluación de supresión poblacional analizando los ciclos archivados y rebrotes.
   * Tasa porcentual de éxito de las aplicaciones realizadas.
3. **Distribución del Riesgo Ambiental (IRA):**
   * Barra continua segmentada con la severidad bioclimática ponderada.

---

### 3.3. 📜 Vista Histórico & Campañas Agrícolas (Macro / INTA)
*Visión estratégica interanual para cooperativas, instituciones y administradores:*

1. **Corte Agronómico de Campaña (1 de Julio al 30 de Junio):**
   * Agrupación automática de todos los monitoreos registrados en ciclos agrícolas anuales.
2. **Plaga Predominante de la Temporada:**
   * Especie líder por volumen de monitoreos, porcentaje de incidencia y tasa de casos que alcanzaron riesgo alto.
3. **Ranking Relativo de Prevalencia:**
   * Comparativa de incidencia entre especies dentro de cada campaña.
4. **Histórico Consolidado:**
   * Superficie total acumulada (ha), cantidad de campañas registradas y plaga más recurrente de todos los tiempos.

---

## 4. Arquitectura y Componentes Técnicos

El desarrollo respeta la arquitectura y tokens del proyecto:

* **Modelos (`AdminMetricasModel.kt`):**
  * `SemaforoHectareas`, `ResumenCultivoPlagas`, `LoteEnRiesgoCritico`, `VentanaControlBiologico`, `EficaciaTratamientos`, `CampañaHistorica`.
  * Funciones puras testeadas: `calcularSemaforoHectareas`, `calcularPlagasPorCultivo`, `calcularRankingLotesCriticos`, `calcularVentanasControlAgronomo`, `calcularEficaciaTratamientos`, `agruparPorCampaña`, `determinarCampaña`.
* **ViewModel (`AdminMetricasViewModel.kt`):**
  * Consulta concurrente no bloqueante (`async`/`await` sobre `Dispatchers.IO`) de `getMonitoreos()`, `getTerrenos()` y `getPlantaciones()`.
  * Exposición de StateFlow inmutable `AdminMetricasUIState`.
  * Selector reactivo de pestañas (`cambiarPestaña`) y campañas.
* **Interfaz de Usuario (`AdminMetricasScreen.kt`):**
  * Implementada con Jetpack Compose y Material 3.
  * Selector interactivo de perspectivas (`SelectorPerspectivaDashboard`).
  * Gráfico Donut en `Canvas` con animación de barrido (`GraficoDonutSemaforo`).
  * Barras de avance fenológico tricolor y ranking de criticidad.
  * Shimmer de carga (`SkeletonCargando`) y animaciones fluidas con `AnimatedContent`.
* **Tests Unitarios (`AdminMetricasModelTest.kt`):**
  * Cobertura de clasificación de hectáreas en semáforo, agrupación de plagas por cultivo, cálculo de días desde siembra, ventanas térmicas GDD y corte de campañas.

---

## 5. Métricas globales del administrador

Un admin no tiene lotes propios: con `/monitoreos`, `/terrenos` y `/plantaciones` (que la base filtra por dueño) sus tableros quedaban vacíos. Desde el panel admin, la pantalla ahora lee `GET /admin/metricas`, que calcula los mismos tableros en el backend sobre todos los productores (`AdminMetricasScreen(global = true)`). El productor sigue calculando los suyos en la app.

- **Lotes anónimos:** el backend no manda dueño, nombre de terreno ni ubicación. La app los muestra como "Lote anónimo 1, 2…" en el orden del ranking.
- **Mismas definiciones en ambos cálculos** (`docs/admin-metrics.md` del backend). El backend dejó de mandar el avance, el nivel y el GDD acumulado del monitoreo: viven en sus ciclos.
  - un monitoreo sin ciclo GDD (esperando biofix) no tiene nivel: queda fuera del semáforo, el ranking y las ventanas, y no cuenta en el riesgo de la campaña;
  - el ciclo que describe un monitoreo es el activo o, si no hay, el de biofix más reciente (`cicloQueDescribe`);
  - en las métricas globales, sin ciclos archivados la tasa de eficacia es desconocida (se muestra "—").
- El mapeo del contrato a los modelos de la pantalla está en `conMetricasGlobales` (`AdminMetricasModel.kt`) y se prueba en `AdminMetricasGlobalesTest.kt`.
- El panel web de administración muestra los mismos tableros y los exporta a PDF y Excel.
