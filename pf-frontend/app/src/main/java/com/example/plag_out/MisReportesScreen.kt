package com.example.plag_out

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.AnilloSegmentado
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.EncabezadoGrupoFiltro
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.NivelEstilo
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PanelFiltrosPlegable
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.contadorAnimado
import com.example.plag_out.ui.theme.estiloDeNivel
import com.example.plag_out.ui.theme.rememberPressScale
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FILTRO_TODOS = "Todos"

private const val DISTANCIA_TODAS = -1

private val RADIOS_KM = listOf(5, 10, 25, 50)

private const val TAB_PROPIOS = 0
private const val TAB_COMUNIDAD = 1

private val AzulComunidad = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MisReportesScreen(
    viewModel: MisReportesViewModel,
    navController: NavController
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarReportes()
    }

    var tabSeleccionado by rememberSaveable { mutableIntStateOf(TAB_PROPIOS) }
    var filtrosExpandidos by rememberSaveable { mutableStateOf(false) }

    // Filtros separados por ámbito: si se compartieran, cambiar de pestaña dejaría
    // aplicado un filtro que ahí no significa nada y la lista aparecería vacía.
    var filtroSeveridadPropios by rememberSaveable { mutableStateOf(FILTRO_TODOS) }
    var filtroTerreno by rememberSaveable { mutableStateOf(FILTRO_TODOS) }
    var filtroSeveridadComunidad by rememberSaveable { mutableStateOf(FILTRO_TODOS) }
    var filtroDistancia by rememberSaveable { mutableIntStateOf(DISTANCIA_TODAS) }
    var filtroPlaga by rememberSaveable { mutableStateOf(FILTRO_TODOS) }

    val reportesPropios = remember(state.reportes) { state.reportes.filter { it.es_propio } }
    val reportesComunidad = remember(state.reportes) { state.reportes.filterNot { it.es_propio } }

    val esComunidad = tabSeleccionado == TAB_COMUNIDAD
    val reportesAmbito = if (esComunidad) reportesComunidad else reportesPropios
    val filtroSeveridad = if (esComunidad) filtroSeveridadComunidad else filtroSeveridadPropios

    val terrenosDisponibles = remember(reportesPropios) {
        reportesPropios.map { it.terreno_nombre ?: "Sin terreno" }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val plagasDisponibles = remember(reportesComunidad) {
        reportesComunidad.map { it.plaga_nombre }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val reportesFiltrados = remember(
        reportesAmbito, esComunidad, filtroSeveridad, filtroTerreno, filtroDistancia, filtroPlaga
    ) {
        reportesAmbito.filter { r ->
            val coincideSeveridad =
                filtroSeveridad == FILTRO_TODOS || r.nivel_severidad.equals(filtroSeveridad, ignoreCase = true)
            val coincideAmbito = if (esComunidad) {
                val coincideDistancia = filtroDistancia == DISTANCIA_TODAS || dentroDelRadio(r, filtroDistancia)
                val coincidePlaga = filtroPlaga == FILTRO_TODOS || r.plaga_nombre.equals(filtroPlaga, ignoreCase = true)
                coincideDistancia && coincidePlaga
            } else {
                filtroTerreno == FILTRO_TODOS || (r.terreno_nombre ?: "Sin terreno").equals(filtroTerreno, ignoreCase = true)
            }
            coincideSeveridad && coincideAmbito
        }
    }

    val hayFiltroActivo = if (esComunidad) {
        filtroSeveridadComunidad != FILTRO_TODOS || filtroDistancia != DISTANCIA_TODAS || filtroPlaga != FILTRO_TODOS
    } else {
        filtroSeveridadPropios != FILTRO_TODOS || filtroTerreno != FILTRO_TODOS
    }

    val limpiarFiltrosDelAmbito = {
        if (esComunidad) {
            filtroSeveridadComunidad = FILTRO_TODOS
            filtroDistancia = DISTANCIA_TODAS
            filtroPlaga = FILTRO_TODOS
        } else {
            filtroSeveridadPropios = FILTRO_TODOS
            filtroTerreno = FILTRO_TODOS
        }
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val fabScale by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "fabScaleReportes"
            )
            FloatingActionButton(
                onClick = { navController.navigate("crear_reporte") },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                    .testTag("btnNuevoReporteFAB")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Reporte", modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Nuevo Reporte", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refrescar() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                PanelHeaderReportes(
                    tabSeleccionado = tabSeleccionado,
                    onTabChange = { tabSeleccionado = it },
                    reportesPropios = reportesPropios,
                    reportesComunidad = reportesComunidad
                )


                if (reportesAmbito.isNotEmpty()) {
                    val totalOriginal = reportesAmbito.size
                    val totalFiltrados = reportesFiltrados.size

                    PanelFiltrosPlegable(
                        expandido = filtrosExpandidos,
                        onToggleExpandido = { filtrosExpandidos = !filtrosExpandidos },
                        hayFiltroActivo = hayFiltroActivo,
                        etiquetaAbrir = if (esComunidad) "Filtrar reportes cercanos" else "Filtrar mis reportes",
                        resumen = if (hayFiltroActivo) {
                            "Mostrando $totalFiltrados de $totalOriginal (Filtros activos)"
                        } else {
                            "Mostrando $totalOriginal ${if (totalOriginal == 1) "reporte" else "reportes"}"
                        },
                        onLimpiar = limpiarFiltrosDelAmbito
                    ) {
                        // --- Categoría común: Nivel de Severidad ---
                        GrupoFiltroSeveridad(
                            reportes = reportesAmbito,
                            seleccionado = filtroSeveridad,
                            onSeleccion = { nivel ->
                                if (esComunidad) filtroSeveridadComunidad = nivel else filtroSeveridadPropios = nivel
                            }
                        )

                        if (esComunidad) {
                            EncabezadoGrupoFiltro(Icons.Outlined.Explore, "CERCANÍA")

                            val opcionesDistancia = remember(reportesComunidad) {
                                val list = mutableListOf(
                                    OpcionFiltro(
                                        DISTANCIA_TODAS, "Distancia: Toda", reportesComunidad.size,
                                        Icons.Outlined.Explore, AzulComunidad
                                    )
                                )
                                RADIOS_KM.forEach { radio ->
                                    val cant = reportesComunidad.count { dentroDelRadio(it, radio) }
                                    list.add(
                                        OpcionFiltro(radio, "Hasta $radio km", cant, Icons.Outlined.NearMe, AzulComunidad)
                                    )
                                }
                                list
                            }
                            FiltroChipsRow(
                                opciones = opcionesDistancia,
                                seleccionado = filtroDistancia,
                                onSeleccion = { id -> filtroDistancia = id },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )


                            if (plagasDisponibles.size > 1) {
                                EncabezadoGrupoFiltro(Icons.Outlined.BugReport, "PLAGA")

                                val opcionesPlagas = remember(reportesComunidad, plagasDisponibles) {
                                    val list = mutableListOf(
                                        OpcionFiltro(
                                            -1, "Plagas: Todas", reportesComunidad.size,
                                            Icons.Outlined.BugReport, PlagOutColors.Forest
                                        )
                                    )
                                    plagasDisponibles.forEachIndexed { index, plaga ->
                                        val cant = reportesComunidad.count { it.plaga_nombre == plaga }
                                        list.add(
                                            OpcionFiltro(index, plaga, cant, Icons.Outlined.BugReport, PlagOutColors.Forest)
                                        )
                                    }
                                    list
                                }
                                val chipPlagaSeleccionadaId =
                                    if (filtroPlaga == FILTRO_TODOS) -1 else plagasDisponibles.indexOf(filtroPlaga)
                                FiltroChipsRow(
                                    opciones = opcionesPlagas,
                                    seleccionado = chipPlagaSeleccionadaId,
                                    onSeleccion = { id ->
                                        filtroPlaga = if (id in plagasDisponibles.indices) plagasDisponibles[id] else FILTRO_TODOS
                                    },
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        } else if (terrenosDisponibles.isNotEmpty()) {
                            // --- Terreno / Lugar: sólo tiene sentido sobre los propios ---
                            EncabezadoGrupoFiltro(Icons.Outlined.Landscape, "TERRENO / LUGAR")

                            val opcionesTerrenos = remember(reportesPropios, terrenosDisponibles) {
                                val list = mutableListOf(
                                    OpcionFiltro(-1, "Terrenos: Todos", reportesPropios.size, Icons.Outlined.Landscape, PlagOutColors.Forest)
                                )
                                terrenosDisponibles.forEachIndexed { index, terreno ->
                                    val cant = reportesPropios.count { (it.terreno_nombre ?: "Sin terreno") == terreno }
                                    list.add(OpcionFiltro(index, terreno, cant, Icons.Outlined.Landscape, PlagOutColors.Forest))
                                }
                                list
                            }
                            val chipTerrenoSeleccionadoId =
                                if (filtroTerreno == FILTRO_TODOS) -1 else terrenosDisponibles.indexOf(filtroTerreno)
                            FiltroChipsRow(
                                opciones = opcionesTerrenos,
                                seleccionado = chipTerrenoSeleccionadoId,
                                onSeleccion = { id ->
                                    filtroTerreno = if (id in terrenosDisponibles.indices) terrenosDisponibles[id] else FILTRO_TODOS
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = when {
                            state.isLoading -> "cargando"
                            reportesAmbito.isEmpty() -> "vacio-$tabSeleccionado"
                            reportesFiltrados.isEmpty() -> "sin-resultados-$tabSeleccionado"
                            else -> "lista-$tabSeleccionado-$filtroSeveridad-$filtroTerreno-$filtroDistancia-$filtroPlaga"
                        },
                        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                        label = "reportesContent"
                    ) { target ->
                        when {
                            target == "cargando" -> SkeletonCargando(alturaTarjeta = 140.dp, cantidad = 4)

                            target.startsWith("vacio") -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                if (esComunidad) {
                                    EstadoVacioFlotante(
                                        icono = Icons.Filled.Group,
                                        titulo = "No hay reportes cerca de tus terrenos",
                                        subtitulo = "Acá vas a ver lo que reportan otros productores en la zona de tus terrenos."
                                    )
                                } else {
                                    EstadoVacioFlotante(
                                        icono = Icons.Outlined.BugReport,
                                        titulo = "No tenés reportes cargados",
                                        subtitulo = "Presioná el botón flotante '+' para registrar la presencia de plagas en tus terrenos."
                                    )
                                }
                            }

                            target.startsWith("sin-resultados") -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                val detalleFiltro = buildString {
                                    if (filtroSeveridad != FILTRO_TODOS) append("severidad '$filtroSeveridad'")
                                    if (esComunidad) {
                                        if (filtroDistancia != DISTANCIA_TODAS) {
                                            if (isNotEmpty()) append(" y ")
                                            append("a menos de $filtroDistancia km")
                                        }
                                        if (filtroPlaga != FILTRO_TODOS) {
                                            if (isNotEmpty()) append(" y ")
                                            append("plaga '$filtroPlaga'")
                                        }
                                    } else if (filtroTerreno != FILTRO_TODOS) {
                                        if (isNotEmpty()) append(" y ")
                                        append("terreno '$filtroTerreno'")
                                    }
                                }
                                EstadoSinResultados(subtitulo = "No hay reportes con $detalleFiltro.")
                            }

                            else -> LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                itemsIndexed(reportesFiltrados, key = { _, r -> r.id }) { index, reporte ->
                                    StaggeredAppear(index = index) {
                                        TarjetaReporteItem(
                                            reporte = reporte,
                                            onClick = {
                                                val payload = ReporteNavPayload(
                                                    id = reporte.id,
                                                    plaga_nombre = reporte.plaga_nombre,
                                                    nivel_severidad = reporte.nivel_severidad,
                                                    latitud = reporte.latitud,
                                                    longitud = reporte.longitud,
                                                    timestamp_ms = reporte.timestamp_ms
                                                )
                                                val jsonEncoded = Uri.encode(Gson().toJson(payload))
                                                navController.navigate("ver_reporte/${reporte.id}/$jsonEncoded")
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun dentroDelRadio(reporte: ReporteDetalleResponse, radioKm: Int): Boolean {
    val dist = reporte.distancia_km ?: return false
    return dist <= radioKm
}


private fun formatearKm(km: Float): String =
    String.format(Locale.getDefault(), if (km >= 10f) "%.0f" else "%.1f", km)


@Composable
private fun GrupoFiltroSeveridad(
    reportes: List<ReporteDetalleResponse>,
    seleccionado: String,
    onSeleccion: (String) -> Unit
) {
    EncabezadoGrupoFiltro(Icons.Outlined.Shield, "NIVEL DE SEVERIDAD")

    val opcionesSeveridad = remember(reportes) {
        listOf(
            OpcionFiltro(-1, "Severidad: Todas", reportes.size, Icons.Outlined.FilterList),
            OpcionFiltro(0, "Alto", reportes.count { it.nivel_severidad.equals("Alto", ignoreCase = true) }, Icons.Default.ErrorOutline, Color(0xFFC62828)),
            OpcionFiltro(1, "Medio", reportes.count { it.nivel_severidad.equals("Medio", ignoreCase = true) }, Icons.Default.WarningAmber, Color(0xFFEF6C00)),
            OpcionFiltro(2, "Bajo", reportes.count { it.nivel_severidad.equals("Bajo", ignoreCase = true) }, Icons.Default.CheckCircle, Color(0xFF2E7D32))
        )
    }
    val chipSeveridadSeleccionadoId = when (seleccionado) {
        "Alto" -> 0
        "Medio" -> 1
        "Bajo" -> 2
        else -> -1
    }
    FiltroChipsRow(
        opciones = opcionesSeveridad,
        seleccionado = chipSeveridadSeleccionadoId,
        onSeleccion = { id ->
            onSeleccion(
                when (id) {
                    0 -> "Alto"
                    1 -> "Medio"
                    2 -> "Bajo"
                    else -> FILTRO_TODOS
                }
            )
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PanelHeaderReportes(
    tabSeleccionado: Int,
    onTabChange: (Int) -> Unit,
    reportesPropios: List<ReporteDetalleResponse>,
    reportesComunidad: List<ReporteDetalleResponse>
) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderReportes")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativa"
    )

    val reportes = if (tabSeleccionado == TAB_COMUNIDAD) reportesComunidad else reportesPropios
    val totalReportes = reportes.size

    val conteoAlto = remember(reportes) { reportes.count { it.nivel_severidad.equals("Alto", ignoreCase = true) } }
    val conteoMedio = remember(reportes) { reportes.count { it.nivel_severidad.equals("Medio", ignoreCase = true) } }
    val conteoBajo = remember(reportes) { reportes.count { it.nivel_severidad.equals("Bajo", ignoreCase = true) } }

    val formaHeader = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = formaHeader
            )
            .clip(formaHeader)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-48).dp)
                .size(190.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-44).dp, y = 48.dp)
                .size(150.dp)
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.05f), CircleShape)
        )

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 22.dp)) {
            Text(
                "Gestión de Alertas",
                color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Text(
                "Reportes",
                color = PlagOutColors.TextOnDark,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(14.dp))

            SelectorAmbito(
                tabSeleccionado = tabSeleccionado,
                onTabChange = onTabChange,
                cantidadPropios = reportesPropios.size,
                cantidadComunidad = reportesComunidad.size
            )

            Spacer(Modifier.height(18.dp))

            // La severidad de un reporte se pinta con la misma paleta sobre oscuro que el
            // nivel de alerta de un monitoreo: Alto↔nivel 2, Medio↔nivel 1, Bajo↔nivel 0.
            val estiloAlto = estiloDeNivel(2)
            val estiloMedio = estiloDeNivel(1)
            val estiloBajo = estiloDeNivel(0)

            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalAnimado = contadorAnimado(totalReportes)
                AnilloSegmentado(
                    segmentos = listOf(
                        conteoAlto to estiloAlto.colorSobreOscuro,
                        conteoMedio to estiloMedio.colorSobreOscuro,
                        conteoBajo to estiloBajo.colorSobreOscuro
                    ),
                    total = totalReportes,
                    modifier = Modifier.size(110.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalAnimado", color = PlagOutColors.TextOnDark, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (totalReportes == 1) "reporte" else "reportes",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.width(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LeyendaSeveridad(estiloAlto, "Alto", conteoAlto)
                    LeyendaSeveridad(estiloMedio, "Medio", conteoMedio)
                    LeyendaSeveridad(estiloBajo, "Bajo", conteoBajo)
                }
            }
        }
    }
}

@Composable
private fun SelectorAmbito(
    tabSeleccionado: Int,
    onTabChange: (Int) -> Unit,
    cantidadPropios: Int,
    cantidadComunidad: Int
) {
    Surface(
        color = PlagOutColors.TextOnDark.copy(alpha = 0.14f),
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val anchoSegmento = maxWidth / 2
            val desplazamiento by animateDpAsState(
                targetValue = if (tabSeleccionado == TAB_COMUNIDAD) anchoSegmento else 0.dp,
                animationSpec = tween(320, easing = FastOutSlowInEasing),
                label = "indicadorAmbito"
            )
            Box(
                Modifier
                    .offset(x = desplazamiento)
                    .width(anchoSegmento)
                    .fillMaxHeight()
                    .background(PlagOutColors.Cream, CircleShape)
            )
            Row(Modifier.fillMaxSize()) {
                SegmentoAmbito(
                    activo = tabSeleccionado == TAB_PROPIOS,
                    icono = Icons.Outlined.Person,
                    etiqueta = "Míos",
                    cantidad = cantidadPropios,
                    onClick = { onTabChange(TAB_PROPIOS) },
                    modifier = Modifier.testTag("tabReportesPropios")
                )
                SegmentoAmbito(
                    activo = tabSeleccionado == TAB_COMUNIDAD,
                    icono = Icons.Filled.Group,
                    etiqueta = "Comunidad",
                    cantidad = cantidadComunidad,
                    onClick = { onTabChange(TAB_COMUNIDAD) },
                    modifier = Modifier.testTag("tabReportesComunidad")
                )
            }
        }
    }
}

@Composable
private fun RowScope.SegmentoAmbito(
    activo: Boolean,
    icono: ImageVector,
    etiqueta: String,
    cantidad: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tinta by animateColorAsState(
        targetValue = if (activo) PlagOutColors.Forest else PlagOutColors.TextOnDark.copy(alpha = 0.82f),
        animationSpec = tween(260),
        label = "tintaAmbito"
    )
    Row(
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(CircleShape)
            .clickable(enabled = !activo, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(etiqueta, color = tinta, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text("$cantidad", color = tinta.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun LeyendaSeveridad(estilo: NivelEstilo, etiqueta: String, cantidad: Int) {
    val valor = contadorAnimado(cantidad)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(estilo.icono, contentDescription = null, tint = estilo.colorSobreOscuro, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            etiqueta,
            color = PlagOutColors.TextOnDark.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text("$valor", color = PlagOutColors.TextOnDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TarjetaReporteItem(
    reporte: ReporteDetalleResponse,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    // Misma paleta que el nivel de alerta de un monitoreo: la severidad se lee en el
    // borde izquierdo de la tarjeta, no en un badge.
    val estiloSeveridad = when (reporte.nivel_severidad.lowercase()) {
        "alto" -> estiloDeNivel(2)
        "medio" -> estiloDeNivel(1)
        else -> estiloDeNivel(0)
    }

    val fechaFormateada = remember(reporte.timestamp_ms) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(reporte.timestamp_ms))
        } catch (e: Exception) {
            "Fecha no disp."
        }
    }

    val esPropio = reporte.es_propio
    val distanciaFormateada = reporte.distancia_km?.let { formatearKm(it) }
    val terrenoNombre = if (esPropio) {
        reporte.terreno_nombre?.takeIf { it.isNotBlank() } ?: "Terreno no especificado"
    } else {
        val tNom = reporte.terreno_nombre?.takeIf { it.isNotBlank() }
        if (tNom != null && distanciaFormateada != null) {
            "Cercano a $tNom ($distanciaFormateada km)"
        } else if (tNom != null) {
            "Cercano a $tNom"
        } else if (distanciaFormateada != null) {
            "A $distanciaFormateada km de tu terreno"
        } else {
            "Comunidad (área cercana)"
        }
    }
    val cultivoInfo = if (!reporte.cultivo_nombre.isNullOrBlank()) {
        "Cultivo de ${reporte.cultivo_nombre}"
    } else {
        "Cultivo ID: ${reporte.plantacion_id ?: "-"}"
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(22.dp),
        color = PlagOutColors.Surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .testTag("tarjetaReporte_${reporte.id}")
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(estiloSeveridad.color)
            )

            Column(
                modifier = Modifier.padding(start = 17.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)
            ) {
                Text(
                    text = reporte.plaga_nombre.ifBlank { "Plaga no especificada" },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                // Dentro de la pestaña "Míos" un chip "Propio" no aporta nada; en los
                // ajenos lo que informa es qué tan cerca cayó.
                if (!esPropio) {
                    Spacer(Modifier.height(8.dp))
                    Surface(color = AzulComunidad.copy(alpha = 0.12f), shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.NearMe,
                                contentDescription = null,
                                tint = AzulComunidad,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = distanciaFormateada?.let { "A $it km de tus terrenos" } ?: "Comunidad",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AzulComunidad
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.6f))
                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Landscape,
                        contentDescription = null,
                        tint = PlagOutColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$terrenoNombre • $cultivoInfo",
                        fontSize = 14.sp,
                        color = PlagOutColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = PlagOutColors.TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = fechaFormateada,
                            fontSize = 13.sp,
                            color = PlagOutColors.TextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Ver detalle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.Forest
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = PlagOutColors.Forest,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
