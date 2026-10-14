# Especificación y Propuesta: Módulo de Dashboards y Visualización

**Proyecto:** PF-PLAG-OUT-26 (Plag-Out)  
**Rama de desarrollo:** `feature/dashboards-metricas`  
**Estado:** En Desarrollo (Fase 1: Histórico de Campañas Agrícolas y Plagas Predominantes implementada)

---

## 1. Objetivo del Módulo

Desarrollar un panel de control analítico y visualización de datos (Dashboards) integrado en la aplicación Android (bajo la sección **Métricas** del panel de administración y adaptable a vistas de productor). El objetivo es transformar los datos dispersos de terrenos, plantaciones, monitoreos GDD, reportes de campo y alertas de Machine Learning en **indicadores ejecutivos clave (KPIs)** y **gráficos interactivos** para la toma de decisiones fitosanitarias.

---

## 2. Dimensiones de Métricas Propuestas

### A. Cobertura Agrícola y Uso del Sistema
* **Superficie bajo Monitoreo:** Sumatoria del área total de terrenos activos (en hectáreas).
* **Distribución de Cultivos:** Cantidad de hectáreas y plantaciones por tipo de cultivo (Maíz, Trigo, Soja, etc.).
* **Usuarios del Ecosistema:**
  * Total de usuarios activos vs suspendidos.
  * Productores con monitoreos activos en la última semana.

### B. Estado Fitosanitario y Presencia de Plagas
* **Ranking de Plagas más Frecuentes:** Plagas con mayor cantidad de monitoreos activos en el sistema.
* **Distribución de Severidad de Reportes:** Porcentaje de reportes de campo según su nivel de daño (`Bajo`, `Medio`, `Alto`).
* **Etapas Biológicas Dominantes:** Proporción de ciclos en fase de incubación (`HUEVO`), desarrollo (`LARVA`/`NINFA`), o reproductiva (`ADULTO`).

### C. Riesgo Ambiental (GDD) y Predicciones de Machine Learning
* **Distribución por Índice de Riesgo Ambiental (IRA):**
  * Nivel 0 (Bajo / Verde)
  * Nivel 1 (Moderado / Amarillo)
  * Nivel 2 (Alto / Rojo)
* **Monitoreos en Estado Crítico:** Monitoreos cuyo ciclo térmico superó el umbral de riesgo o alcanzó la eclosión (`gdd_acumulado >= gdd_eclosion`).
* **Efectividad y Alertas de Machine Learning:**
  * Total de predicciones de brote severo disparadas (`ALERTA_ML_RIESGO`).
  * Tasa de feedback de los productores (confirmadas como `presente`, `no_observada` o `no_verificada`).

---

## 3. Métrica Implementada: Histórico de Campañas Agrícolas y Plagas Predominantes

### 3.1. Concepto y Ciclo de Campaña Agrícola
En la agronomía regional e institucional (INTA, Bolsas de Cereales), las campañas agrícolas no coinciden con el año calendario (enero a diciembre), sino con el ciclo biológico y de rotación de cultivos, establecido formalmente del **1 de julio de un año al 30 de junio del año siguiente** (ejemplo: *Campaña 2025/2026*, *Campaña 2026/2027*).

El sistema clasifica automáticamente cada monitoreo registrado (activo o archivado) dentro de su respectiva campaña utilizando su fecha de inicio o última actualización:
$$\text{Campaña}(F) = \begin{cases} 
\text{Año}(F)/\text{Año}(F)+1 & \text{si Mes}(F) \ge 7 \\ 
\text{Año}(F)-1/\text{Año}(F) & \text{si Mes}(F) < 7 
\end{cases}$$

### 3.2. Indicadores Calculados por Campaña

1. **Plaga Predominante (Líder de Campaña):**
   * Plaga con mayor cantidad de monitoreos registrados en el ciclo.
   * **Incidencia Relativa:**
     $$\text{Incidencia}(\%) = \frac{\text{Monitoreos de la plaga}}{\text{Total de monitoreos de la campaña}} \times 100$$
   * **Tasa de Riesgo Alto / Alerta Crítica:**
     $$\text{Tasa Riesgo Alto}(\%) = \frac{\text{Monitoreos de la plaga con Nivel Alerta } \ge 2}{\text{Total de monitoreos de la plaga}} \times 100$$
   * **Nivel de Severidad Ponderado:** Promedio del nivel de alerta biológico (0 = Bajo, 1 = Moderado, 2 = Alto).

2. **Prevalencia y Ranking Relativo de Plagas:**
   * Comparativa visual de todas las especies monitoreadas durante la campaña.
   * Barra de proporción normalizada respecto al volumen total de la campaña.
   * Alertas tempranas indicando cuántos lotes entraron en zona de riesgo fitosanitario.

3. **Perfil de Riesgo Ambiental (IRA):**
   * Barra horizontal segmentada y continua que visualiza la proporción de lotes según la presión térmica (GDD acumulados y eclosión):
     * **Bajo ($IRA < 30\%$):** Nivel 0 — Condición controlada.
     * **Moderado ($30\% \le IRA < 70\%$):** Nivel 1 — Ventana de monitoreo intensivo.
     * **Alto / Crítico ($IRA \ge 70\%$):** Nivel 2 o 3 — Superación de umbral fenológico y riesgo de daño económico.

4. **Cobertura Territorial:**
   * Sumatoria de la superficie (en hectáreas) de los terrenos únicos monitoreados en la campaña, vinculando `terreno_id` con el catastro agronómico del productor.

---

## 4. Arquitectura y Componentes Técnicos

El desarrollo sigue rigurosamente los estándares del proyecto (Clean Architecture, MVVM, Jetpack Compose Material 3, UDF):

* **Modelos (`AdminMetricasModel.kt`):**
  * `ResumenPlagaCampaña`: Datos agregados por especie de plaga (nombre común, científico, incidencia, casos en alerta alta, promedio).
  * `CampañaHistorica`: Entidad que agrupa las plagas, totales, hectáreas y distribución de riesgo IRA.
  * `AdminMetricasUIState`: Estado reactivo inmutable consumido por la UI.
  * Funciones puras testeadas: `determinarCampaña(fecha)`, `agruparPorCampaña(monitoreos, terrenos)`.

* **ViewModel (`AdminMetricasViewModel.kt`):**
  * Recuperación concurrente no bloqueante (`async`/`await` sobre `Dispatchers.IO`) de monitoreos y terrenos desde `GDDService`.
  * Agrupación automática en segundo plano y selección reactiva de campañas.
  * Manejo estandarizado de fallos de red (`falloDeRespuesta`, `falloDeExcepcion`) y ciclo de vida de sesión (`limpiar()`).

* **Interfaz de Usuario (`AdminMetricasScreen.kt`):**
  * Integrada en la barra de navegación del Administrador (`RUTA_ADMIN_METRICAS`).
  * Soporte de pull-to-refresh (`PullToRefreshBox`).
  * Selector interactivo de chips horizontales (`FiltroChipsRow`).
  * Tokens de color y átomos compartidos de `PlagOutColors` y `CampoUI.kt`.
  * Shimmer de carga (`SkeletonCargando`) y animaciones de conteo (`contadorAnimado`).

* **Tests Unitarios (`AdminMetricasModelTest.kt`):**
  * Cobertura de la regla agronómica de corte de campaña (1 de julio).
  * Verificación de agregación de plagas predominantes, cálculo de hectáreas sin duplicación y distribución de riesgo ambiental.

---

## 5. Próximos Pasos en el Tablero de Dashboards

1. **Evolución Temporal de Reportes y Focos Fitosanitarios:**
   * Gráfico de dispersión/barras temporales por semana/mes para monitorear la curva de infestación de las plagas líderes.
2. **Monitoreo de Eficacia de Tratamientos:**
   * Comparación de GDD antes y después de la finalización/archivado de ciclos de monitoreo.
3. **Métricas de Adopción y Actividad de Productores:**
   * KPIs de retención, cobertura por departamento/región y tiempos de respuesta ante alertas de Machine Learning.
