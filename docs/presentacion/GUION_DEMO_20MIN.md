# 🌾 PLAG-OUT: Guión de Demostración de Producto (20 Minutos)
**"El producto en el centro: Sincronizando biología, clima y comunidad"**  
*Enfoque: Demostración funcional en vivo con teoría agronómica integrada (GDD, Ciclos biológicos y Curva de eficacia)*  
*Trabajo Final de Grado – UTN FRBA 2026*

---

## ⏱️ Cronograma y Timing de la Demo (20 Minutos)

| Bloque | Minutos | Qué se muestra en pantalla | Concepto/Gráfico Técnico que se explica |
| :--- | :---: | :--- | :--- |
| **1. Apertura Express** | 00:00 - 02:00 | Portada & Tesis de Valor | El disparador: -USD 2.000M en maíz y el problema de "llegar tarde" |
| **2. Campo y Clima** | 02:00 - 05:30 | `01_gestion_terrenos.png` (Mis Terrenos) | Por qué georreferenciar: conexión con estaciones meteorológicas locales y persistencia offline |
| **3. Motor Biológico** | 05:30 - 09:30 | `02_monitoreo_cultivos.png` (Monitoreos) | **El Reloj Térmico**: Ciclo de vida, qué es el Biofix (Punto Cero) y fórmula matemática de Grados Día (GDD) |
| **4. Inteligencia Colectiva** | 09:30 - 13:30 | `03_alertas.png` & `04_mapa.png` | **Defensa en Anillo (20 km)**, efecto de red (+Usuarios ➔ +Datos ➔ Alertas) y clusterización espacial |
| **5. Tableros de Control** | 13:30 - 17:30 | `05_lotes`, `06_control`, `07_campanas` | **La Curva de Eficacia**: Errar la ventana (10-20% vs 90%), semáforo de superficie, días de siembra y prevalencia histórica |
| **6. Retorno y Preguntas** | 17:30 - 20:00 | ROI, Matriz Competitiva y Cierre | -25% costos de aplicación, +7-14 días de anticipación, 100% trazabilidad y ronda de preguntas (Q&A) |

---

## 🎬 Guión Detallado Paso a Paso para el Orador

---

### Bloque 1: Apertura Express (00:00 - 02:00) • 2 Minutos Máximo
* **Visual:** Portada oficial PLAG-OUT con métricas de impacto resumidas.
* **Objetivo:** Plantear la tesis de producto sin rodeos y dar paso inmediato a la aplicación.

#### 🗣️ Qué decir:
> *"Buenos días a todos. En la última campaña agrícola, Argentina perdió más de **2.000 millones de dólares** y casi 12 millones de toneladas de maíz por el ataque de la chicharrita. A nivel país, el manejo de plagas genera pérdidas anuales que superan los USD 2.500 millones.*
> 
> *¿Por qué ocurre esto si el productor invierte en la mejor tecnología de insumos? Porque el monitoreo actual tiene una falla crítica: **llega tarde**. Cuando el agrónomo ve la larva desarrollada o la hoja comida en el lote, el daño ya está hecho y pulverizar cuesta el doble y rinde la mitad.*
> 
> *Frente a esto desarrollamos **PLAG-OUT**: una plataforma móvil y en la nube que no es un simple cuaderno digital de notas, sino un **copiloto agronómico predictivo**. Nuestro mantra es: **'La IA acota el riesgo, el campo confirma'**.*
> 
> *Para entender cómo funciona, entremos directamente al producto."*

---

### Bloque 2: Módulo de Campo y el Vínculo Meteorológico (02:00 - 05:30)
* **Visual:** Captura real de la app: `01_gestion_terrenos.png` (Mis Terrenos: 114 ha, Buenos Aires y Jujuy, etiqueta *'1 esperando biofix'*).
* **Concepto Técnico Enganchado:** Georreferenciación precisa para captura de variables bioclimáticas locales y persistencia offline.

#### 🗣️ Qué decir:
> *"Comenzamos el recorrido en la primera pantalla operativa: **Mis Terrenos**.*
> 
> *Aquí el productor o la empresa digitaliza sus establecimientos. En este ejemplo vemos dos campos: 'Buenos Aires' con 58 hectáreas y 'Jujuy', totalizando 114 hectáreas administradas.*
> 
> *¿Por qué es fundamental la georreferenciación GPS exacta de cada lote? Porque los insectos no crecen según el promedio provincial del clima: se desarrollan según la **temperatura real y microclima del potrero**. Con las coordenadas, PLAG-OUT se conecta de forma invisible a las APIs de estaciones meteorológicas del Servicio Meteorológico y redes locales para descargar las temperaturas máximas y mínimas diarias de cada punto geográfico.*
> 
> *Fíjense en la pastilla: **'1 esperando biofix'**. La app no asume fechas al azar; le indica al productor qué potrero requiere marcar el punto de partida biológico.*
> 
> *Y un aspecto técnico esencial: toda esta capa fue construida **offline-first**. El agrónomo en el campo puede no tener señal 4G; los datos se almacenan en la base local SQLite con Room y se sincronizan en segundo plano en cuanto se restablece la conexión."*

---

### Bloque 3: El Motor Biológico: Biofix, Ciclo de Vida y Grados Día (05:30 - 09:30)
* **Visual:** Captura real de la app: `02_monitoreo_cultivos.png` (Mis Monitoreos: Soja en Buenos Aires, Oruga de las leguminosas, inicio 03 sept) + Diagrama del Ciclo Biológico y Ecuación de GDD.
* **Concepto Técnico Enganchado:** El Reloj Térmico, la integral térmica y el cruce del umbral de eclosión.

#### 🗣️ Qué decir:
> *"Entramos ahora a **Mis Monitoreos**, donde la biología se conecta con la matemática.*
> 
> *En pantalla vemos un lote de Soja bajo seguimiento de **Oruga de las leguminosas** (*Anticarsia gemmatalis*).*
> 
> *Para entender qué hace la app por detrás, veamos cómo funciona el **Reloj Térmico de Grados Día (GDD)**:*
> 1. *Los insectos son ectotérmicos: no tienen temperatura corporal constante, sino que su velocidad de desarrollo depende del calor del ambiente por encima de una temperatura base ($T_{base}$).*
> 2. *Para Anticarsia en Argentina, la literatura científica del INTA establece una $T_{base}$ de 10°C.*
> 3. *El agrónomo ingresa el **Biofix** (el punto cero: la fecha en que capturó los primeros adultos en trampa de luz o avistó posturas en el lote).*
> 4. *A partir de ese día, el motor de PLAG-OUT suma diariamente los grados día: $GDD = \frac{T_{max} + T_{min}}{2} - T_{base}$.*
> 
> *Cuando los GDD acumulados cruzan el umbral de eclosión (en este caso 420 GDD), la app proyecta con exactitud matemática el día en que nacerán las larvas.*
> 
> *El semáforo superior clasifica el estado de los lotes: verde para bajo control, naranja para moderado y rojo para alerta crítica. No obligamos al productor a mirar planillas complejas: le mostramos el semáforo operativo para actuar a tiempo."*

---

### Bloque 4: Inteligencia Comunitaria y Mapa Espacial (09:30 - 13:30)
* **Visual:** Capturas reales: `03_alertas_comunitarias.png` (tarjeta *'Chinche a 8.3 km de tus terrenos'*) y `04_mapa_georreferenciado.png` (OpenStreetMap con cluster de 11 reportes y lote propio).
* **Concepto Técnico Enganchado:** El principio de "Defensa en Anillo" (20 km), el efecto de red colaborativo y la clusterización espacial de focos.

#### 🗣️ Qué decir:
> *"El segundo pilar de PLAG-OUT es la **Red Comunitaria de Alertas Tempranas**, porque las plagas no respetan los alambrados.*
> 
> *En la sección de **Reportes**, implementamos el concepto de **'Defensa en Anillo'**: un radio de proximidad configurable (en este caso 20 km).*
> 
> *Miren la tarjeta destacada en la app: **'Chinche de la alfalfa - A 8.3 km de tus terrenos'**. Sin que el productor haya salido a recorrer su campo hoy, el sistema ya le avisó que la presión biológica externa aumentó peligrosamente en sus inmediaciones.*
> 
> *Esto activa lo que llamamos el **Efecto de Red Agronómico**:  
> **Más usuarios en la zona ➔ Más reportes cargados ➔ Más datos para el modelo ➔ Alertas mucho más precisas para todos**.*
> 
> *Si pasamos a la vista de **Mapa Georreferenciado**, la cartografía sobre OpenStreetMap agrupa los focos mediante algoritmos de clusterización: vemos un nodo crítico con **11 reportes activos en la zona núcleo** y la casa que marca nuestro lote. Podemos conmutar entre la vista de puntos y el mapa de calor (densidad) para identificar si el viento y las temperaturas están empujando la plaga hacia nuestros potreros.*
> 
> *Cumpliendo rigurosamente con la Ley de Protección de Datos Personales (Ley 25.326), todos los reportes comunitarios son 100% anónimos: la comunidad se protege sin exponer la identidad de ningún campo."*

---

### Bloque 5: Tableros de Control y Toma de Decisiones Estratégicas (13:30 - 17:30)
* **Visual:** Capturas reales: `05_tablero_lotes.png`, `06_tablero_control.png` y `07_tablero_campanas.png` + Gráfico de la **Curva de Eficacia fitosanitaria**.
* **Concepto Técnico Enganchado:** La sincronización fenológica (por qué pulverizar en L1-L3 y no en huevo/adulto), semáforo de hectáreas y prevalencia histórica interanual.

#### 🗣️ Qué decir:
> *"Llegamos al módulo más potente del sistema: los **Tableros de Control**, donde los datos se convierten en decisiones ejecutivas.*
> 
> *Veamos primero la solapa **Lotes**:  
> El **Semáforo de Superficie en Riesgo** traduce el estado biológico en impacto económico real: 58 hectáreas bajo seguimiento. Y en la **Prioridad de Acción por Lote**, el sistema clasifica qué potrero visitar primero. Noten el dato clave: **'Soja (87 días)'**. Correlaciona los días desde la siembra con el ciclo reproductivo del cultivo y el avance de la plaga.*
> 
> *Pasemos a la solapa **Control**, que resuelve la pregunta del millón: **¿Cuándo aplicar?**.*
> 
> *Para entender el impacto de esta pantalla, miremos la **Curva de Eficacia Fitosanitaria**:*
> * 🥚 **Aplicación temprana (-12 a -6 días de eclosión):** La plaga está en fase de huevo. Eficacia de apenas 10% a 20%. El fitosanitario se lava con el rocío o se degrada por radiación UV. Dinero tirado.*
> * 🎯 **Ventana Útil (Eclosión a +3 días, estadios L1 a L3):** La larva recién nacida es diminuta y altamente vulnerable a dosis estándar. **Eficacia del 90%**.*
> * ⚠️ **Aplicación tardía (+6 a +12 días):** La larva ya es grande (L4 a L6) o adulta. Eficacia cae al 20-30%, genera resistencia biológica y el daño foliar ya causó la pérdida de rinde.*
> 
> *Ahora miren lo que muestra la app en pantalla:  
> Un badge gris que dice: **'INCUBACIÓN (ESPERA) - No pulverizar aún: plaga en fase de huevo o no susceptible'**.*
> 
> *Solo ese cartel le ahorra al productor entre 15 y 30 dólares por hectárea en una aplicación innecesaria que no habría tenido efecto. La barra térmica avisará exactamente cuando entre en verde en la **Ventana Óptima (L1-L3)**.*
> 
> *Finalmente, la solapa **Campañas** consolida la visión gerencial histórica: detecta que la plaga predominante en la campaña 2026/2027 fue Anticarsia con 50% de incidencia y severidad moderada. El gráfico de prevalencia relativa permite al agrónomo rotar los modos de acción (IRAC) y planificar compras anticipadas de insumos con descuento para la próxima siembra."*

---

### Bloque 6: Retorno de Inversión, Diferenciación y Cierre (17:30 - 20:00) • 2:30 Minutos
* **Visual:** Diapositiva de ROI (+7 a 14 días, -25% aplicaciones, 100% trazabilidad), Matriz *"Nosotros Anticipamos"* y pantalla final de contacto y preguntas.
* **Objetivo:** Cerrar con el valor de negocio y abrir con solidez las preguntas de la mesa.

#### 🗣️ Qué decir:
> *"Sintetizando la propuesta de valor de PLAG-OUT para el productor agropecuario:*
> 1. *💰 **Menos aplicaciones:** Reducción de hasta un **25% en costos de fitosanitarios** al eliminar pulverizaciones fuera de ventana.*
> 2. *⏱️ **+7 a 14 días de anticipación:** Alertas bioclimáticas y comunitarias que permiten coordinar contratistas y comprar producto con tiempo.*
> 3. *📋 **100% registro trazable:** Bitácora digital que profesionaliza el establecimiento y cumple las Buenas Prácticas Agrícolas.*
> 
> *¿Por qué PLAG-OUT y no las soluciones del mercado como xarvio, Auravant o FieldView? Porque las herramientas actuales son **reactivas** (muestran la anomalía en el satélite cuando la hoja ya fue comida) o son cajas negras no calibradas para Argentina.*
> 
> *PLAG-OUT **anticipa**: es la única que une el estado fenológico de la plaga por Grados Día con modelos de IA, red colaborativa entre vecinos y modelos científicos locales.*
> 
> *Hoy el prototipo funcional está 100% validado; en la campaña 2026/2027 avanzaremos en pilotos a campo en NOA y NEA, proyectando para 2028 la expansión regional a Brasil y Paraguay.*
> 
> *Muchísimas gracias por su atención. El equipo queda a entera disposición para responder las preguntas de la mesa evaluadora."*

---

## 🛡️ Preguntas Frecuentes Clave para la Defensa (Q&A Técnico)

1. **¿Qué pasa con el cálculo de GDD si no hay estación meteorológica en el campo exacto?**
   * *R:* El sistema toma las coordenadas del lote y consulta la estación más cercana mediante APIs climáticas, interpolando temperatura máxima y mínima con datos de reanálisis si la distancia supera el umbral de representatividad.
2. **¿Por qué la barra fenológica marca L1-L3 como óptima?**
   * *R:* Porque en los estadios larvales iniciales (L1 a L3), el exoesqueleto de la oruga o ninfa es delgado y susceptible a dosis bajas de reguladores de crecimiento (IGR) o insecticidas selectivos, logrando 90% de mortandad con mínimo impacto ambiental.
3. **¿Cómo garantizan que la base de datos soporte la carga de múltiples usuarios?**
   * *R:* La arquitectura desacopla el cliente móvil (offline-first con Room) del backend (FastAPI / PostgreSQL con extensiones espaciales PostGIS), optimizando las consultas geográficas por radios de cobertura mediante índices espaciales R-Tree.
