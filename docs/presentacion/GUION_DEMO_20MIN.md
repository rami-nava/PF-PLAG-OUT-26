# 🌾 PLAG-OUT: Guión Oficial de Demostración de Producto (20 Minutos) + Apéndice Técnico
**"El producto en el centro: Sincronizando biología, clima y comunidad"**  
*Enfoque: Demostración funcional en vivo dividida en 3 perspectivas operativas + Sesión de Apéndice Biológico y Técnico para la mesa evaluadora*  
*Trabajo Final de Grado – UTN FRBA 2026*

---

## ⏱️ Estructura y Cronograma General

| Bloque / Perspectiva | Diapositiva | Minutos | Qué se muestra en pantalla | Concepto Clave Vinculado |
| :--- | :---: | :---: | :--- | :--- |
| **Bloque 1: Apertura & Contexto** | Slide 1 | 00:00 - 02:00 | Portada oficial & Tesis de Valor | -USD 2.000M por Chicharrita. El problema de "llegar tarde". *"La IA acota el riesgo, el campo confirma"*. |
| **Bloque 2: Perspectiva 1 (Alta de Cero)** | Slide 2 | 02:00 - 03:30 | Creación de cuenta y Onboarding | Guía rápida (5 pasos a campo) vs Guía completa (GDD, Biofix, IRA, Niveles). |
| | Slide 3 | 03:30 - 05:30 | Terrenos, Cultivos, Monitoreos y Ciclos | Delimitación GPS de lotes (ha), siembra, plaga, umbral. Offline-First con Room. |
| | Slide 4 | 05:30 - 07:30 | Reporte a Campo & Modelo de ML para Biofix | Reporte en vivo (estadio, severidad, foto). Cómo la IA predice el Biofix y bucle de feedback. |
| **Bloque 3: Perspectiva 2 (Cuenta Activa)** | Slide 5 | 07:30 - 09:00 | Alerta Comunitaria en Tiempo Real | Recepción push de reporte vecino. Defensa en Anillo (20 km) y Ley 25.326. |
| | Slide 6 | 09:00 - 11:00 | Valores Avanzados GDD & Motor Diario | Monitor térmico lote por lote. Job nocturno batch (Open-Meteo/SMN) y push FCM. |
| | Slide 7 | 11:00 - 12:30 | Cartografía Espacial (OpenStreetMap) | Clusters en zona núcleo vs lote propio. Conmutación Puntos vs Densidad (Heatmap). |
| | Slide 8 | 12:30 - 15:00 | Tableros de Control: Lotes, Control & Campañas | Semáforo 58 ha, Curva de Eficacia (10-20% vs 90%), días desde siembra y rotación IRAC. |
| | Slide 9 | 15:00 - 16:00 | Perfil, Permisos y Consentimiento ML | Notificaciones nativas, GPS y toggle de uso ético de datos para reentrenamiento de IA. |
| **Bloque 4: Perspectiva 3 (Admin & Escala)** | Slide 10 | 16:00 - 17:00 | Escalabilidad Biológica (Panel Admin) | Alta de nuevas plagas/cultivos: Tb, constantes térmicas K, umbrales sin código. |
| | Slide 11 | 17:00 - 17:45 | Gestión de Usuarios (Admin) | Administración de roles (productor, agrónomo, admin) y estado de cuentas. |
| | Slide 12 | 17:45 - 18:45 | Dashboards Globales y Métricas Regionales | Inteligencia macro para cooperativas y acopios: hectáreas y propagación zonal. |
| | Slide 13 | 18:45 - 19:30 | Ecosistema Multiplataforma (Mobile vs Web) | Mobile (Android Kotlin offline en camioneta) + Web Desktop (gestión y auditoría). |
| **Bloque 5: Cierre & Retorno** | Slide 14 | 19:30 - 20:00 | ROI, Diferenciación y Agradecimiento | -25% costos, +7-14 días anticipación, 100% trazabilidad. Diapositiva de ¡Muchas Gracias! |
| **Bloque 6: Apéndice para Preguntas (Q&A)** | Slide 15 | Post-Demo | Apéndice B1: Dinámica y Ecuación GDD | Fórmulas térmicas, temperatura base Tb, integral térmica y calibración INTA. |
| | Slide 16 | Post-Demo | Apéndice B2: Modelos de Plagas y Eficacia | Dalbulus, Anticarsia, Spodoptera. Ventana óptima L1-L3 vs huevo/adulto. |
| | Slide 17 | Post-Demo | Apéndice T1: Arquitectura de Software | FastAPI, PostgreSQL/PostGIS, Room SQLite, WorkManager y tolerancia offline. |
| | Slide 18 | Post-Demo | Apéndice T2: Machine Learning para Biofix | Features bioclimáticas, modelo de regresión/clasificación y dataset activo. |
| | Slide 19 | Post-Demo | Apéndice T3: Job Diario y Notificaciones | Scheduler batch nocturno, APIs meteorológicas y despacho masivo FCM. |
| | Slide 20 | Post-Demo | Apéndice T4: Geoprivacidad y Ley 25.326 | Buffer espacial difuso de 20 km, anonimización y seguridad de sesiones. |

---

# 🎬 Guión Detallado Paso a Paso para el Orador

---

## BLOQUE 1: APERTURA & CONTEXTO DEL PROBLEMA (00:00 - 02:00)
**Diapositiva 1 | Portada Oficial y Tesis de Producto**

* **En pantalla:** Portada PLAG-OUT con métricas de pérdidas y mantra oficial.
* **Objetivo:** Introducir la problemática real del agro en menos de 2 minutos sin rodeos comerciales y dar paso inmediato al producto funcional.

#### 🗣️ Texto del Orador:
> *"Buenos días a todos los miembros de la mesa evaluadora.*
> 
> *En la última campaña agrícola, la República Argentina sufrió una catástrofe productiva sin precedentes: perdimos más de **2.000 millones de dólares** y casi 12 millones de toneladas de maíz debido al ataque de la chicharrita (*Dalbulus maidis*). A nivel nacional, las pérdidas anuales directas e indirectas por manejo ineficiente de plagas superan los **2.500 millones de dólares**.*
> 
> *¿Por qué sucede esto si el productor compra la mejor genética de semillas y aplica los fitosanitarios más caros del mercado? Porque el sistema actual de monitoreo tiene un pecado de origen: **llega tarde**. Cuando el agrónomo o el recorredor ve la larva desarrollada o la hoja comida en el lote, el daño ya está consumado. Errar la ventana de aplicación reduce la eficacia del tratamiento a menos del 20% y duplica los costos operativos.*
> 
> *Para resolver esta falla estructural creamos **PLAG-OUT**: una plataforma integral que sincroniza la biología del insecto, el clima local y la inteligencia comunitaria para avisarle al productor **cuándo va a nacer la plaga antes de que empiece a comer**.*
> 
> *Nuestro principio rector es: **'La inteligencia artificial acota el riesgo; el campo confirma'**.*
> 
> *Para ver cómo funciona en la práctica, dividiremos la demostración en tres perspectivas: el alta de un productor desde cero, la operatoria en marcha de un campo activo recibiendo alertas en tiempo real, y la escalabilidad del sistema desde la administración."*

---

## BLOQUE 2: PERSPECTIVA 1 — ONBOARDING Y ALTA DESDE CERO (02:00 - 07:30)

---

### Diapositiva 2: Creación de Cuenta y Onboarding (02:00 - 03:30)
* **En pantalla:** Pantalla de Registro / Login + Modales interactivos de Guía Rápida y Guía Completa (`AyudaRapidaSheet` y `ComoFuncionaSheet`).
* **Concepto:** Accesibilidad para el usuario rural y claridad pedagógica entre el paso a paso operativo y el rigor agronómico.

#### 🗣️ Texto del Orador:
> *"Comenzamos el recorrido creando una cuenta nueva desde cero.*
> 
> *El registro es directo e intuitivo: correo electrónico, contraseña segura y validación instantánea. Diseñamos la interfaz bajo principios de usabilidad para el entorno agropecuario, donde el usuario necesita resolver rápido.*
> 
> *Apenas ingresa, el productor dispone de dos niveles de asistencia según su perfil:*
> 1. *En la barra superior tiene la **Guía Rápida** (el signo de interrogación): un resumen conciso de solo cinco pasos diseñado para quien está en la camioneta y quiere arrancar ya: cargá terrenos, sumá cultivos, iniciá monitoreo, confirmá biofix y realizá reportes.*
> 2. *Y desde el perfil o los ajustes, puede desplegar la **Guía Completa**: un manual interactivo con sustento científico que le explica en lenguaje claro qué son los Grados Día de Desarrollo (GDD), qué significa el Biofix como punto cero biológico, cómo se calcula el Índice de Riesgo Ambiental (IRA) y qué representa cada nivel del semáforo.*
> 
> *La aplicación educa y guía al usuario sin abrumarlo con tecnicismos innecesarios."*

---

### Diapositiva 3: Configuración Productiva: Terrenos, Cultivos, Monitoreos y Ciclos (03:30 - 05:30)
* **En pantalla:** Capturas reales `01_gestion_terrenos.png` y `09_plantaciones_cultivos.png`.
* **Concepto:** Estructuración territorial en hectáreas, coordenadas GPS para captura bioclimática y arquitectura Offline-First.

#### 🗣️ Texto del Orador:
> *"Ahora configuramos el establecimiento productivo.*
> 
> *Primero creamos un **Terreno**. Ingresamos el nombre del lote, su superficie exacta en hectáreas y marcamos su ubicación geográfica precisa en el mapa con el GPS. ¿Por qué es crucial la georreferenciación exacta? Porque las plagas no se desarrollan según el promedio del clima provincial: dependen de la **temperatura real y microclima del lote**. Al fijar las coordenadas, PLAG-OUT se vincula de manera autónoma con estaciones meteorológicas locales.*
> 
> *Luego asociamos la **Plantación o Cultivo**: indicamos la especie sembrada (por ejemplo, Soja o Maíz) y la fecha de siembra. Esto nos permite calcular con precisión los días transcurridos y correlacionar la fenología del cultivo con la aparición de plagas.*
> 
> *El tercer paso es dar de alta el **Monitoreo**: seleccionamos qué plaga queremos vigilar (por ejemplo, Oruga de las leguminosas o Chicharrita) y fijamos el umbral de riesgo térmico deseado.*
> 
> *Fíjense en el estado con el que nace el ciclo: **'Esperando Biofix'**. La aplicación no adivina fechas; necesita anclar el cronómetro térmico.*
> 
> *Y un detalle arquitectónico vital: todo este módulo fue desarrollado con arquitectura **Offline-First**. En el medio del campo no siempre hay cobertura 4G. El usuario puede crear lotes y cultivos sin conexión; la información se persiste en la base SQLite local mediante Room y se sincroniza en segundo plano ni bien recupera señal."*

---

### Diapositiva 4: Reporte a Campo & Modelo de ML para Biofix (05:30 - 07:30)
* **En pantalla:** Capturas reales `08_nuevo_reporte.png` y `10_detalle_reporte.png` + Diagrama del bucle de feedback con el modelo de Machine Learning.
* **Concepto:** Digitalización del scouting a campo, estimación inteligente del punto de partida biológico y confirmación activa del usuario.

#### 🗣️ Texto del Orador:
> *"Llegamos a un momento clave: el agrónomo o recorredor camina el lote y encuentra presencia de insectos.*
> 
> *Abrimos la pantalla de **Nuevo Reporte**: seleccionamos la plaga avistada, el estadio biológico (ninfa o adulto), el grado de severidad, adjuntamos fotografías de evidencia y confirmamos las coordenadas GPS del hallazgo.*
> 
> *Pero aquí entra en acción nuestro **Modelo de Machine Learning para Biofix**:  
> El biofix es el hito biológico de inicio —típicamente la primera emergencia de adultos fértiles—. Establecerlo a ojo humano suele ser tardío porque cuando el productor ve los primeros adultos, quizás ya llevan una semana oviponiendo en el cultivo.*
> 
> *Nuestro modelo de IA analiza series temporales meteorológicas de los últimos 60 días (rachas térmicas, acumulación de días cálidos invernales y fotoperíodo) y predice una **ventana estimada de biofix con hasta 14 días de anticipación**.*
> 
> *Cuando la app detecta condiciones favorables, emite una sugerencia de biofix. El agrónomo en el campo la evalúa y con un botón confirma o ajusta la fecha. Al confirmar el biofix, **se activa el acumulador térmico de grados día** para ese monitoreo, iniciando la proyección de eclosión.*
> 
> *Este reporte, además de guardarse localmente, se envía al backend... y desencadena el poder de la red comunitaria."*

---

## BLOQUE 3: PERSPECTIVA 2 — CUENTA ACTIVA: EL CAMPO EN MARCHA (07:30 - 15:00)

---

### Diapositiva 5: Alerta Comunitaria en Tiempo Real (07:30 - 09:00)
* **En pantalla:** Captura real `03_alertas_comunitarias.png` (Tarjeta: *"Chinche de la alfalfa - A 8.3 km de tus terrenos"*).
* **Concepto:** Principio de Defensa en Anillo (20 km), efecto de red colaborativo y anonimización de datos bajo Ley 25.326.

#### 🗣️ Texto del Orador:
> *"Cambiamos ahora a una segunda cuenta: la de un productor vecino que ya tiene sus campos cargados y monitoreados en la misma región agrícola.*
> 
> *En este preciso instante, el teléfono de este vecino **recibe una notificación push automática**:  
> **'Alerta Comunitaria: Plaga reportada a 8.3 km de tus terrenos'**.*
> 
> *Este es el concepto de **Defensa en Anillo de 20 km**: los insectos no reconocen alambrados ni títulos de propiedad. Si a 8 kilómetros un vecino detectó presencia activa, significa que la presión biológica zonal creció y el viento o la temperatura pueden desplazar la población en cuestión de días.*
> 
> *Esto materializa el **Efecto de Red Agronómico**:  
> Cuantos más productores monitorean en una cuenca agrícola, más robusta es la malla de alerta para toda la comunidad.*
> 
> *Y un punto legal innegociable: cumplimos con la **Ley Nacional N° 25.326 de Protección de Datos Personales**. El reporte del vecino es completamente anónimo. La app muestra la distancia radial difusa, pero jamás expone el nombre del campo, los datos fiscales ni la ubicación catastral del vecino."*

---

### Diapositiva 6: Valores Avanzados de GDD & El Motor Diario de Fondo (09:00 - 11:00)
* **En pantalla:** Captura real `02_monitoreo_cultivos.png` + Tarjetas de cálculo de GDD e infografía del Job Nocturno en backend.
* **Concepto:** Acumulación térmica fisiológica, fórmula matemática de GDD y pipeline batch automatizado.

#### 🗣️ Texto del Orador:
> *"Entramos al detalle del monitoreo en esta cuenta en marcha para ver el núcleo fisiológico: **los Grados Día de Desarrollo (GDD)**.*
> 
> *En pantalla vemos el lote de Soja con Oruga de las leguminosas. Fíjense en la barra de progreso térmico:*
> 1. *Los insectos son ectotérmicos: su velocidad de crecimiento depende del calor ambiental por encima de una temperatura base ($T_b$), que para Anticarsia está fijada en **10°C** según calibraciones del INTA.*
> 2. *La fórmula aplicada diariamente es: $\text{GDD} = \frac{T_{\max} + T_{\min}}{2} - T_b$.*
> 3. *Para esta especie, completar la incubación del huevo y alcanzar la eclosión requiere acumular **420 GDD**.*
> 
> *¿Cómo se mantiene esto actualizado sin que el usuario tenga que ingresar a mano las temperaturas?  
> A través de nuestro **Motor Diario en Segundo Plano**: todas las noches a las 03:00 AM, un servicio batch en el backend consulta automáticamente las estaciones meteorológicas de Open-Meteo y el Servicio Meteorológico Nacional, computa las integrales térmicas de cada parcela, suma los GDD correspondientes y recalcula el Índice de Riesgo Ambiental.*
> 
> *Si el progreso cruza el umbral de alerta configurado por el usuario, el sistema despacha automáticamente una notificación push a su celular a primera hora de la mañana."*

---

### Diapositiva 7: Cartografía Espacial: Mapa de Presión por Zona (11:00 - 12:30)
* **En pantalla:** Captura real `04_mapa_georreferenciado.png` (OpenStreetMap con nodo de 11 reportes y ubicación de campo propio).
* **Concepto:** Georreferenciación cartográfica con OSMDroid, algoritmos de clusterización y mapa de calor de densidad.

#### 🗣️ Texto del Orador:
> *"Pasemos ahora al **Mapa Georreferenciado**, donde la información adquiere dimensión territorial.*
> 
> *La aplicación integra cartografía nativa basada en OpenStreetMap. Mediante algoritmos de clusterización espacial, agrupamos los reportes comunitarios para no saturar la pantalla.*
> 
> *Vean lo que ocurre en pantalla:  
> Hacia el noreste se identifica un **cluster rojo con 11 reportes activos en zona núcleo**, mientras que el ícono de la casa señala la ubicación de nuestro propio campo.*
> 
> *El agrónomo puede conmutar con un toque entre la vista de **Puntos individuales** y el **Mapa de Calor (Densidad)**. Esto le permite visualizar vectores de propagación: si los focos de calor avanzan en la dirección de los vientos predominantes hacia su potrero, puede anticipar las recorridas de campo y preparar las defensas con días de antelación."*

---

### Diapositiva 8: Tableros de Control Ejecutivos: Lotes, Control & Campañas (12:30 - 15:00)
* **En pantalla:** Capturas reales `05_tablero_lotes.png`, `06_tablero_control.png` y `07_tablero_campanas.png` + Gráfico de la **Curva de Eficacia Fitosanitaria**.
* **Concepto:** Semáforo de superficie, sincronización fenológica óptima (L1-L3 vs huevo/adulto) y analítica histórica de rotación IRAC.

#### 🗣️ Texto del Orador:
> *"Llegamos al módulo analítico por excelencia: los **Tableros de Control**, que traducen los datos agronómicos en decisiones económicas ejecutivas.*
> 
> *El selector superior ofrece tres perspectivas especializadas:*
> 
> *1. **Solapa Lotes (Vista Productor):**  
> El **Semáforo Donut** clasifica las 58 hectáreas monitoreadas según su nivel de urgencia. Y el ranking de criticidad le dice al agrónomo exactamente a qué lote ir primero hoy. Observen el dato: **'Soja (87 días)'**: vincula el ciclo biológico del insecto con los días transcurridos desde la siembra, detectando si la plaga coincide con los estadios reproductivos más sensibles del cultivo (R1-R5).*
> 
> *2. **Solapa Control (Vista Ingeniero Agrónomo):**  
> Resuelve la pregunta más costosa del campo: **¿Cuándo aplicar?**  
> Miremos la **Curva de Eficacia Fitosanitaria**:*
> * 🥚 *Si aplicamos temprano, con la plaga en huevo (-10 días): **Eficacia de apenas 10% a 20%**. El producto se lava con el rocío y se degrada al sol. Dinero tirado.*
> * 🎯 *Si aplicamos en la **Ventana Óptima (larvas L1 a L3)**: La oruga es pequeña y muy susceptible. **Eficacia superior al 90%**.*
> * ⚠️ *Si aplicamos tarde (+8 días, larvas L5-L6 o adultos): Eficacia cae al 20-30%, generamos resistencia biológica y el cultivo ya perdió rinde foliar.*
> 
> *Fíjense en el badge que muestra hoy la app: **'INCUBACIÓN (ESPERA) - No pulverizar aún'**. Solo esa advertencia le ahorra al productor entre 15 y 30 dólares por hectárea al evitar una aplicación inútil.*
> 
> *3. **Solapa Campañas (Vista Histórica):**  
> Consolida la memoria del establecimiento: detecta que en la campaña 2026/27 la plaga predominante fue Anticarsia con un 50% de incidencia. Esto le da al gerente datos auditados para rotar modos de acción según el comité **IRAC** y negociar compras anticipadas de insumos con descuento de pre-campaña."*

---

### Diapositiva 9: Perfil de Usuario, Permisos y Consentimiento Ético de ML (15:00 - 16:00)
* **En pantalla:** Pantalla de Perfil y Drawer de Ajustes (`PerfilScreen.kt`) con switches de Notificaciones, Ubicación y Consentimiento ML.
* **Concepto:** Gobernanza de datos, respeto de la privacidad y control ético sobre el reentrenamiento de algoritmos.

#### 🗣️ Texto del Orador:
> *"Pasamos al módulo de **Perfil y Ajustes**, diseñado bajo estrictos estándares de ciberseguridad y soberanía del dato.*
> 
> *Desde aquí, el productor gestiona:*
> 1. *El canal de **Notificaciones Push**: un switch nativo vinculado a Firebase Cloud Messaging que registra o revoca los tokens de su dispositivo.*
> 2. *Los permisos de **Geolocalización GPS** con verificación en tiempo real del estado en Android.*
> 3. *Y fundamentalmente, el switch de **Consentimiento ML**:  
> En PLAG-OUT no usamos los datos de los productores a sus espaldas. El usuario decide explícitamente si sus reportes agronómicos pueden ser anonimizados para retroalimentar y mejorar los modelos de predicción regional. Si desactiva el switch, sus reportes se mantienen estrictamente privados y aislados.*
> 
> *Transparencia total para generar confianza en el hombre de campo."*

---

## BLOQUE 4: PERSPECTIVA 3 — ESCALABILIDAD, ADMIN Y VISIÓN MULTIPLATAFORMA (16:00 - 19:30)

---

### Diapositiva 10: Escalabilidad del Catálogo Biológico desde el Panel de Admin (16:00 - 17:00)
* **En pantalla:** Interfaz del Panel de Administrador (`AdminPlagasScreen.kt` y formulario de alta de nueva plaga).
* **Concepto:** Arquitectura modular y extensible sin dependencia de desarrollo para nuevas plagas o cultivos.

#### 🗣️ Texto del Orador:
> *"Veamos ahora la tercera perspectiva: **el Panel de Administración y la Escalabilidad del Producto**.*
> 
> *Una limitación de muchos sistemas agrícolas es que sus reglas biológicas están 'cableadas' en el código. Si aparece una nueva plaga o una mutación térmica, requieren semanas de desarrollo y desplegar una nueva versión de la app.*
> 
> *En PLAG-OUT, el panel de administración permite dar de alta **nuevas plagas y cultivos de manera dinámica**:*
> * *El agrónomo administrador ingresa el nombre común y científico.*
> * *Define los parámetros biológicos: la temperatura base ($T_b$), la acumulación térmica requerida para cada estadio ($K$ en GDD) y los umbrales de alerta.*
> * *Vincula los cultivos hospedantes correspondientes.*
> 
> *Al guardar, la base de datos central propaga el nuevo modelo biológico a todos los usuarios de la plataforma en segundos, sin que nadie tenga que actualizar su aplicación desde Google Play Store."*

---

### Diapositiva 11: Gestión de Usuarios y Gobernanza Operativa (17:00 - 17:45)
* **En pantalla:** Vista de administración de usuarios (`AdminUsuariosScreen.kt`) con filtros de roles y estados.
* **Concepto:** Control de accesos basado en roles (RBAC) y seguridad institucional.

#### 🗣️ Texto del Orador:
> *"De forma muy rápida, mostramos también la **Gestión de Usuarios**.*
> 
> *El administrador visualiza la nómina de usuarios registrados, puede filtrar por estado (activos, pendientes o suspendidos) y asignar roles jerárquicos: productor estándar, ingeniero agrónomo certificador o administrador de cooperativa.*
> 
> *Esto permite a las empresas agropecuarias y asociaciones de productores auditar quién tiene permisos para validar biofix o emitir reportes certificados en la zona."*

---

### Diapositiva 12: Métricas Macro y Dashboards de Administración (17:45 - 18:45)
* **En pantalla:** Pantalla de analítica global de administración (`AdminMetricasScreen.kt`).
* **Concepto:** Inteligencia agregada a nivel regional para cooperativas, acopios y entidades fitosanitarias (INTA/SENASA).

#### 🗣️ Texto del Orador:
> *"El panel de administración consolida además los **Dashboards Globales de Inteligencia Regional**.*
> 
> *Aquí la información deja de ser de un solo lote y se convierte en **inteligencia epidemiológica para la cuenca agrícola**:*
> * *Superficie total monitoreada a nivel regional.*
> * *Índice de velocidad de propagación de cada plaga.*
> * *Cultivos con mayor presión sanitaria acumulada en la campaña.*
> 
> *Esta herramienta está pensada para cooperativas agrícolas, centros de acopio y organismos como el INTA o SENASA, que necesitan anticipar brotes masivos y coordinar alertas zonales antes de que se declaren emergencias agrícolas."*

---

### Diapositiva 13: Ecosistema Multiplataforma: Mobile vs Web (18:45 - 19:30)
* **En pantalla:** Diagrama comparativo y mockups del ecosistema: Aplicación Móvil (Android) + Portal Web Desktop.
* **Concepto:** Coexistencia armónica según el caso de uso: trabajo rústico a campo vs análisis gerencial en oficina.

#### 🗣️ Texto del Orador:
> *"Para cerrar la visión técnica del sistema, destaquemos la estrategia **Multiplataforma** de PLAG-OUT:*
> 
> *1. **La versión Mobile (Android Nativo en Kotlin):**  
> Es la herramienta de batalla para el ingeniero y el productor arriba de la camioneta. Diseñada con botones amplios aptos para uso con una sola mano bajo el sol, geolocalización por hardware, captura fotográfica directa y capacidad 100% Offline-First con Room.*
> 
> *2. **La versión Web Desktop:**  
> Es el centro de mando para la oficina y la administración. Permite visualizar los mapas en monitores de alta resolución, exportar informes fitosanitarios para certificar Buenas Prácticas Agrícolas, gestionar usuarios masivamente y alimentar el catálogo de plagas.*
> 
> *Ambos clientes comparten la misma API REST asíncrona en FastAPI y la misma base de datos relacional y espacial en PostgreSQL con PostGIS."*

---

## BLOQUE 5: RETORNO DE INVERSIÓN Y CIERRE OFICIAL (19:30 - 20:00)
**Diapositiva 14 | Retorno de Inversión, Diferenciación y Cierre**

* **En pantalla:** Los 3 pilares de ROI, matriz competitiva contra el mercado y placa oficial de agradecimiento y preguntas.
* **Objetivo:** Sintetizar el valor económico/ambiental y dejar la mesa servida para las preguntas del jurado.

#### 🗣️ Texto del Orador:
> *"Sintetizando la propuesta de valor de PLAG-OUT para el campo argentino:*
> 1. *💰 **-25% en costos fitosanitarios:** Al evitar aplicaciones a destiempo en estadios invulnerables.*
> 2. *⏱️ **+7 a 14 días de anticipación:** Alertas bioclimáticas y comunitarias que permiten contratar pulverizadoras sin sobrecostos de urgencia.*
> 3. *📋 **100% registro trazable:** Para certificar Buenas Prácticas Agrícolas y cumplir exigencias de exportación.*
> 
> *¿Por qué PLAG-OUT se diferencia de herramientas como xarvio, Auravant o FieldView?  
> Porque las plataformas existentes son **reactivas**: alertan cuando el satélite detecta que el índice verde cayó porque la hoja ya fue comida. PLAG-OUT **anticipa biológicamente**: calcula el momento exacto de nacimiento mediante acumulación térmica, IA y defensa colaborativa entre vecinos.*
> 
> *Muchísimas gracias a todos por su tiempo y atención. El equipo queda a completa disposición de la mesa evaluadora para responder cualquier pregunta."*

---

# 📚 Bloque 6: Diapositivas Complementarias / Apéndice (Post-Gracias para Q&A)

---

### Diapositiva 15: Apéndice B1 — Dinámica y Fórmulas de Grados Día de Desarrollo (GDD)
* **Tipo:** Biológico
* **En pantalla:** Ecuaciones de cálculo térmico, gráfico de acumulación sinusoidal y parámetros de corte superior e inferior.
* **Respaldo para preguntas como:** *"¿Cómo calculan matemáticamente los Grados Día y qué pasa si la temperatura es extrema?"*

#### 🗣️ Argumentación Técnica:
> *"Los insectos, al ser organismos poiquilotermos, carecen de mecanismos endógenos de regulación térmica; su tasa metabólica y velocidad de desarrollo celular son una función directa de la temperatura ambiente.*
> 
> *La formulación matemática base implementada en el motor de PLAG-OUT es el método del promedio simple con restricciones de umbral:*
> $$\text{GDD} = \max\left(0, \frac{\min(T_{\max}, T_{\text{corte}}) + \max(T_{\min}, T_b)}{2} - T_b\right)$$
> 
> *Donde:*
> * *$T_b$ (Temperatura Base): Es el umbral térmico inferior bajo el cual el desarrollo fisiológico se detiene por completo (diapausa o latencia).*
> * *$T_{\text{corte}}$ (Temperatura Umbral Superior): Temperatura a partir de la cual el desarrollo deja de acelerarse y se estabiliza (típicamente entre 32°C y 35°C según especie).*
> * *$K$ (Constante Térmica): Es la integral térmica total que requiere la especie para completar una fase ontogénica: $K = \sum_{t=1}^{n} \text{GDD}_t$.*
> 
> *Para garantizar la representatividad en el campo argentino, los parámetros térmicos de nuestro catálogo fueron calibrados utilizando series empíricas publicadas por las Estaciones Experimentales Agropecuarias del INTA (Pergamino, Manfredi y Las Breñas)."*

---

### Diapositiva 16: Apéndice B2 — Modelos Biológicos por Especie y Curva de Eficacia
* **Tipo:** Biológico
* **En pantalla:** Tabla de parámetros ontogénicos (*Dalbulus maidis*, *Anticarsia gemmatalis*, *Spodoptera frugiperda*) y Gráfico detallado de la Curva de Eficacia del Control Químico y Biológico.
* **Respaldo para preguntas como:** *"¿Por qué la ventana de aplicación tiene tan poco margen y por qué el modelo sugiere esperar en incubación?"*

#### 🗣️ Argumentación Técnica:
> *"La ventana de aplicación fitosanitaria está condicionada por la anatomía del insecto en cada estadio ontogénico:*
> 
> | Especie | Nombre Común | $T_b$ (°C) | GDD Huevo a Adulto | Período Crítico de Daño |
> | :--- | :--- | :---: | :---: | :--- |
> | *Dalbulus maidis* | Chicharrita del maíz | 10.0°C | 420 GDD | V1 a V6 (Transmisión de Spiroplasma y Corn Stunt) |
> | *Anticarsia gemmatalis* | Oruga de las leguminosas | 10.0°C | 380 GDD | R1 a R5 (Defoliación masiva en floración y llenado) |
> | *Spodoptera frugiperda* | Oruga cogollera | 12.0°C | 350 GDD | V2 a V8 (Destrucción del meristema apical en cogollo) |
> 
> *La razón por la cual la eficacia cae drásticamente fuera de la ventana óptima es biofísica:*
> 1. *En **Fase de Huevo (Incubación)**: El corion protector del huevo es impermeable a la mayoría de los insecticidas de contacto. Aplicar en este momento tiene apenas 10-20% de eficacia, genera mortandad de enemigos naturales benéficos (parasitoides) y expone el principio activo a fotodegradación antes de que la larva eclosione.*
> 2. *En **Estadios L1 a L3 (Larva Chica / Ninfa)**: La cutícula del insecto es sumamente delgada, su ingesta de superficie foliar es continua y responde con el 90-95% de mortandad a dosis bajas de reguladores de crecimiento (IGR) o biológicos basados en Bacillus thuringiensis.*
> 3. *En **Estadios L4 a L6 o Adultos**: La oruga posee una cutícula esclerotizada, enzimas detoxificantes activas y ya se introdujo en el cogollo o las vainas, haciendo inaccesible el blanco para las gotas pulverizadas.*
> 
> *Por esto, el badge de 'INCUBACIÓN - Esperar' es una herramienta de ahorro económico y preservación ambiental directa."*

---

### Diapositiva 17: Apéndice T1 — Arquitectura General del Sistema & Resiliencia Offline
* **Tipo:** No Biológico / Técnico
* **En pantalla:** Diagrama de bloques de la arquitectura: Clean Architecture en Android, persistencia con Room, cola de sincronización con WorkManager, API REST en FastAPI y PostgreSQL con PostGIS.
* **Respaldo para preguntas como:** *"¿Cómo resuelven la falta de internet en el campo y la concurrencia en la nube?"*

#### 🗣️ Argumentación Técnica:
> *"La arquitectura fue diseñada bajo el patrón de **Clean Architecture y MVVM** en el cliente móvil, combinado con microservicios asíncronos en el backend:*
> 
> 1. *Capa Móvil (Android Nativo):*
>    * *Framework UI: Jetpack Compose con Material Design 3.*
>    * *Persistencia: SQLite gestionada con Room ORM.*
>    * *Manejo de estado: `StateFlow` inmutables y corrutinas de Kotlin (`Dispatchers.IO`).*
> 2. *Mecanismo de Resiliencia Offline:*
>    * *Toda operación de campo (creación de lote, reporte, confirmación de biofix) se persiste primero en la base local con un identificador UUID y un flag de estado (`PENDIENTE_ENVIO`).*
>    * *Un servicio en segundo plano con `WorkManager` monitorea el estado de conectividad mediante `ConnectivityManager`.*
>    * *Al detectar red (Wi-Fi o celular), despacha la cola de eventos mediante peticiones idempotentes con reintentos exponenciales automáticos, garantizando que jamás se pierda un dato registrado en el campo.*
> 3. *Capa Backend y Base de Datos:*
>    * *Backend: FastAPI en Python 3.11 con ejecución asíncrona no bloqueante (`asyncio`).*
>    * *Base de Datos: PostgreSQL 15 con la extensión espacial **PostGIS**.*
>    * *Consultas espaciales: La búsqueda de reportes vecinos se ejecuta mediante la función nativa `ST_DWithin` sobre índices espaciales R-Tree (`GIST`), logrando tiempos de respuesta sub-milisegundo incluso con miles de registros concurrentes."*

---

### Diapositiva 18: Apéndice T2 — Modelo de Machine Learning para Biofix
* **Tipo:** No Biológico / Técnico
* **En pantalla:** Pipeline de preprocesamiento, matriz de features climáticas, arquitectura del modelo predictivo y curva ROC de validación.
* **Respaldo para preguntas como:** *"¿Qué modelo de Machine Learning usan, qué datos toma y cómo aprende de las correcciones del usuario?"*

#### 🗣️ Argumentación Técnica:
> *"El objetivo del modelo de Machine Learning no es reemplazar el criterio del agrónomo, sino anticipar la ventana de emergencia de adultos cuando aún no es evidente a simple vista.*
> 
> *1. Vector de Características (Features de Entrada):*
> * *Series térmicas acumuladas: Medias móviles de temperatura a 7, 15 y 30 días.*
> * *Anomalías térmicas invernales: Días con $T_{\min} > 15^\circ\text{C}$ durante los meses fríos (factor crítico de supervivencia de invierno de Dalbulus maidis).*
> * *Precipitaciones acumuladas e índice de humedad relativa.*
> * *Fotoperíodo astronómico (horas de luz por latitud).*
> * *Fecha de siembra y antecedentes de presión de plaga en la cuenca agrícola durante campañas previas.*
> 
> *2. Arquitectura del Modelo:*
> * *Implementamos un ensamble basado en Gradient Boosted Trees (XGBoost / LightGBM) acoplado a un clasificador logístico que predice la probabilidad de oviposición activa en una ventana de 14 días.*
> * *Entrenamiento y Validación: Validado contra datasets históricos de monitoreo del INTA y de redes de trampas de luz, alcanzando un AUC-ROC superior a 0.88 en detección de primera generación estival.*
> 
> *3. Bucle de Aprendizaje Activo (Active Feedback Loop):*
> * *Cuando la app emite una alerta de posible biofix, el usuario responde 'Confirmar' o 'Rechazar' en base a lo observado en el lote.*
> * *Esta retroalimentación genera un registro de validación supervisada (`FeedbackPrediccion`) que se almacena localmente y se transmite al backend para el reentrenamiento periódico de los pesos zonales del modelo."*

---

### Diapositiva 19: Apéndice T3 — Pipeline del Job Diario de Fondo & Despacho Push
* **Tipo:** No Biológico / Técnico
* **En pantalla:** Diagrama de secuencia del batch nocturno: Scheduler Cron, llamadas a APIs meteorológicas, cálculo de GDD y despacho masivo por Firebase Cloud Messaging.
* **Respaldo para preguntas como:** *"¿Cómo escala el recálculo diario si hay miles de parcelas y cómo llegan las alertas al celular?"*

#### 🗣️ Argumentación Técnica:
> *"El recálculo biológico se ejecuta mediante un pipeline batch automatizado en el backend:*
> 
> 1. *Disparador Cron (03:00 AM hora local):*
>    * *Se inicia el proceso batch nocturno para aprovechar la menor carga de red y cuando ya se dispone del cierre térmico ($T_{\max}$ y $T_{\min}$) del día previo.*
> 2. *Consulta Concurrente de Meteorología:*
>    * *El backend agrupa las parcelas mediante clustering geoespacial para no consultar la misma coordenada varias veces.*
>    * *Se realizan peticiones asíncronas concurrentes (`httpx.AsyncClient`) a las APIs meteorológicas de Open-Meteo y estaciones del SMN.*
> 3. *Cálculo y Persistencia:*
>    * *Se computa la fórmula de GDD para cada monitoreo activo según la plaga asociada y su temperatura base.*
>    * *Se actualiza el acumulador `gdd_acumulado` en la base de datos y se recalcula el Índice de Riesgo Ambiental (IRA).*
> 4. *Evaluación de Reglas y Notificaciones Push:*
>    * *Si un monitoreo supera el umbral configurado por el usuario o entra en la ventana óptima de aplicación, se genera un evento de alerta.*
>    * *El despacho se realiza a través de **Firebase Cloud Messaging (FCM)** utilizando tokens individuales y topics regionales, garantizando una entrega inmediata y con mínimo consumo de batería en el dispositivo móvil."*

---

### Diapositiva 20: Apéndice T4 — Geoprivacidad, Anonimización y Ley 25.326
* **Tipo:** No Biológico / Técnico
* **En pantalla:** Diagrama del algoritmo de radio difuso (Buffer aleatorio), cláusulas de la Ley 25.326 y arquitectura de seguridad JWT/Supabase.
* **Respaldo para preguntas como:** *"¿Cómo protegen los datos del productor y evitan que un vecino sepa exactamente qué pasa en mi campo?"*

#### 🗣️ Argumentación Técnica:
> *"En el sector agropecuario, los datos de producción y presencia de plagas son información de altísima sensibilidad comercial. Por ello, diseñamos el sistema en estricto cumplimiento de la **Ley Nacional N° 25.326 de Protección de Datos Personales de la República Argentina**:*
> 
> 1. *Algoritmo de Perturbación Espacial (Buffer Difuso):*
>    * *Cuando un usuario genera un reporte de campo, sus coordenadas GPS precisas quedan guardadas únicamente en su cuenta privada para su propia gestión de lote.*
>    * *Para la capa comunitaria, el backend aplica un algoritmo de anonimización espacial: la coordenada compartida se desplaza aleatoriamente dentro de un radio difuso de dispersión.*
>    * *El mapa y las alertas muestran 'Presencia a 8.3 km', pero es matemáticamente imposible derivar el catastro exacto o la identidad del productor que originó el aviso.*
> 2. *Seguridad de Acceso y Sesión:*
>    * *Autenticación robusta basada en JSON Web Tokens (JWT) con expiración y renovación transparente.*
>    * *Cifrado de datos en tránsito mediante TLS 1.3 y en reposo mediante AES-256.*
> 3. *Consentimiento Granular:*
>    * *El usuario puede en cualquier momento revocar el consentimiento de uso de datos para entrenamiento de IA desde su panel de perfil, haciendo cumplir su derecho de autodeterminación informativa."*
