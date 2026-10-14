# Especificación y Propuesta: Módulo de Dashboards y Visualización

**Proyecto:** PF-PLAG-OUT-26 (Plag-Out)  
**Rama de desarrollo:** `feature/dashboards-metricas`  
**Estado:** Propuesta Inicial / Documento de Diseño

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

## 3. Componentes Visuales Propuestos (Jetpack Compose)

1. **Cabecera de Tarjetas KPI (Summary Cards):**
   * Superficie total monitoreada (ha).
   * Monitoreos activos totales.
   * Monitoreos en alerta crítica.
   * Alertas ML activas en la semana.
2. **Gráfico de Estado de Riesgo (Donut / Semicírculo):**
   * Proporción de lotes según su nivel IRA (Verde / Amarillo / Rojo) utilizando los tokens de `PlagOutColors`.
3. **Gráfico de Barras Horizontales:**
   * Incidencia de plagas principales en relación con los cultivos afectados.
4. **Línea Temporal de Severidad:**
   * Evolución de reportes cargados en los últimos 7, 14 o 30 días.
5. **Panel de Alertas Recientes y Focos Críticos:**
   * Listado rápido con acceso directo al monitoreo o predicción en riesgo.

---

## 4. Fuentes de Datos y Estrategia Técnica

* **Fase 1 (Frontend Driven):**
  * Consumir los endpoints actuales ya expuestos en `GDDService`:
    * `GET /monitoreos`: Para calcular totales, GDD acumulados, progreso, niveles de alerta y plagas.
    * `GET /reportes`: Para estadísticas de focos de infección y niveles de severidad.
    * `GET /admin/usuarios`: Para métricas de adopción y cuentas.
    * `GET /admin/plagas`: Para constantes térmicas y catálogo.
  * Agregar y procesar la información en el `DashboardViewModel` con corrutinas (`Dispatchers.Default`).
* **Fase 2 (Optimizaciones Backend):**
  * Evaluación de un endpoint específico `GET /admin/metricas` en el backend FastAPI que entregue agregaciones precalculadas si el volumen de datos crece.
