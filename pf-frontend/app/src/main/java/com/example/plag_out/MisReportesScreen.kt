package com.example.plag_out

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val FILTRO_TODOS = "Todos"

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

    var filtroSeveridad by rememberSaveable { mutableStateOf(FILTRO_TODOS) }
    var filtroTerreno by rememberSaveable { mutableStateOf(FILTRO_TODOS) }
    var filtrosExpandidos by rememberSaveable { mutableStateOf(false) }

    val terrenosDisponibles = remember(state.reportes) {
        state.reportes.map { it.terreno_nombre ?: "Sin terreno" }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val reportesFiltrados = remember(state.reportes, filtroSeveridad, filtroTerreno) {
        state.reportes.filter { r ->
            val coincideSeveridad = filtroSeveridad == FILTRO_TODOS || r.nivel_severidad.equals(filtroSeveridad, ignoreCase = true)
            val coincideTerreno = filtroTerreno == FILTRO_TODOS || (r.terreno_nombre ?: "Sin terreno").equals(filtroTerreno, ignoreCase = true)
            coincideSeveridad && coincideTerreno
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
                // Header unificado con gradiente verde
                PanelHeaderReportes(totalReportes = state.reportes.size, reportes = state.reportes)

                // Panel superior de filtros (Barra Toggle + Contenido Plegable)
                val filtroActivo = filtroSeveridad != FILTRO_TODOS || filtroTerreno != FILTRO_TODOS
                val totalOriginal = state.reportes.size
                val totalFiltrados = reportesFiltrados.size

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlagOutColors.Cream)
                ) {
                    // Botón principal Toggle ("Filtrar reportes" / "Ocultar filtros")
                    Surface(
                        onClick = { filtrosExpandidos = !filtrosExpandidos },
                        color = PlagOutColors.Cream,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    color = if (filtroActivo) PlagOutColors.Forest.copy(alpha = 0.15f) else PlagOutColors.Surface,
                                    shape = CircleShape,
                                    border = if (filtroActivo) null else BorderStroke(1.dp, PlagOutColors.Divider),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Tune,
                                            contentDescription = "Filtrar reportes",
                                            tint = if (filtroActivo) PlagOutColors.Forest else PlagOutColors.TextMain,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (filtrosExpandidos) "Ocultar filtros" else "Filtrar reportes",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlagOutColors.TextMain
                                    )
                                    Text(
                                        text = if (filtroActivo) "Mostrando $totalFiltrados de $totalOriginal (Filtros activos)" else "Mostrando $totalOriginal reportes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (filtroActivo) PlagOutColors.Forest else PlagOutColors.TextSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (filtroActivo) {
                                    Surface(
                                        onClick = {
                                            filtroSeveridad = FILTRO_TODOS
                                            filtroTerreno = FILTRO_TODOS
                                        },
                                        shape = CircleShape,
                                        color = PlagOutColors.RiskDanger.copy(alpha = 0.1f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Limpiar filtros",
                                                tint = PlagOutColors.RiskDanger,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Limpiar",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PlagOutColors.RiskDanger
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = if (filtrosExpandidos) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (filtrosExpandidos) "Ocultar filtros" else "Mostrar filtros",
                                    tint = PlagOutColors.TextSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Contenido expandible de chips de filtros
                    AnimatedVisibility(
                        visible = filtrosExpandidos,
                        enter = expandVertically() + fadeIn(tween(220)),
                        exit = shrinkVertically() + fadeOut(tween(180))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            // --- Categoría 1: Nivel de Severidad ---
                            Row(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = PlagOutColors.TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "NIVEL DE SEVERIDAD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PlagOutColors.TextSecondary,
                                    letterSpacing = 0.6.sp
                                )
                            }

                            val opcionesSeveridad = remember(state.reportes) {
                                listOf(
                                    OpcionFiltro(-1, "Severidad: Todas", state.reportes.size, Icons.Outlined.FilterList),
                                    OpcionFiltro(0, "Alto", state.reportes.count { it.nivel_severidad.equals("Alto", ignoreCase = true) }, Icons.Default.ErrorOutline, Color(0xFFC62828)),
                                    OpcionFiltro(1, "Medio", state.reportes.count { it.nivel_severidad.equals("Medio", ignoreCase = true) }, Icons.Default.WarningAmber, Color(0xFFEF6C00)),
                                    OpcionFiltro(2, "Bajo", state.reportes.count { it.nivel_severidad.equals("Bajo", ignoreCase = true) }, Icons.Default.CheckCircle, Color(0xFF2E7D32))
                                )
                            }
                            val chipSeveridadSeleccionadoId = when (filtroSeveridad) {
                                "Alto" -> 0
                                "Medio" -> 1
                                "Bajo" -> 2
                                else -> -1
                            }
                            FiltroChipsRow(
                                opciones = opcionesSeveridad,
                                seleccionado = chipSeveridadSeleccionadoId,
                                onSeleccion = { id ->
                                    filtroSeveridad = when (id) {
                                        0 -> "Alto"
                                        1 -> "Medio"
                                        2 -> "Bajo"
                                        else -> FILTRO_TODOS
                                    }
                                },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            // --- Categoría 2: Terreno / Lugar ---
                            if (terrenosDisponibles.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Landscape,
                                        contentDescription = null,
                                        tint = PlagOutColors.TextSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "TERRENO / LUGAR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PlagOutColors.TextSecondary,
                                        letterSpacing = 0.6.sp
                                    )
                                }

                                val opcionesTerrenos = remember(state.reportes, terrenosDisponibles) {
                                    val list = mutableListOf(
                                        OpcionFiltro(-1, "Terrenos: Todos", state.reportes.size, Icons.Outlined.Landscape, PlagOutColors.Forest)
                                    )
                                    terrenosDisponibles.forEachIndexed { index, terreno ->
                                        val cant = state.reportes.count { (it.terreno_nombre ?: "Sin terreno") == terreno }
                                        list.add(OpcionFiltro(index, terreno, cant, Icons.Outlined.Landscape, PlagOutColors.Forest))
                                    }
                                    list
                                }
                                val chipTerrenoSeleccionadoId = if (filtroTerreno == FILTRO_TODOS) -1 else terrenosDisponibles.indexOf(filtroTerreno)
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
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = when {
                            state.isLoading -> "cargando"
                            state.reportes.isEmpty() -> "vacio"
                            reportesFiltrados.isEmpty() -> "sin-resultados"
                            else -> "lista-$filtroSeveridad-$filtroTerreno"
                        },
                        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                        label = "reportesContent"
                    ) { target ->
                        when (target) {
                            "cargando" -> SkeletonCargando(alturaTarjeta = 140.dp, cantidad = 4)
                            "vacio" -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                EstadoVacioFlotante(
                                    icono = Icons.Outlined.BugReport,
                                    titulo = "No tenés reportes cargados",
                                    subtitulo = "Presioná el botón flotante '+' para registrar la presencia de plagas en tus terrenos."
                                )
                            }
                            "sin-resultados" -> Box(
                                Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                val detalleFiltro = buildString {
                                    if (filtroSeveridad != FILTRO_TODOS) append("severidad '$filtroSeveridad'")
                                    if (filtroTerreno != FILTRO_TODOS) {
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

@Composable
private fun PanelHeaderReportes(totalReportes: Int, reportes: List<ReporteDetalleResponse>) {
    val gradienteHeader = Brush.verticalGradient(
        colors = listOf(PlagOutColors.Forest, PlagOutColors.ForestDark)
    )
    val conteoAlto = remember(reportes) { reportes.count { it.nivel_severidad.equals("Alto", ignoreCase = true) } }
    val conteoMedio = remember(reportes) { reportes.count { it.nivel_severidad.equals("Medio", ignoreCase = true) } }
    val conteoBajo = remember(reportes) { reportes.count { it.nivel_severidad.equals("Bajo", ignoreCase = true) } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradienteHeader)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.BugReport,
                            contentDescription = null,
                            tint = PlagOutColors.TextOnDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Mis Reportes",
                        color = PlagOutColors.TextOnDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "$totalReportes reportes de plaga registrados",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResumenSeveridadMiniChip(etiqueta = "Alto", cantidad = conteoAlto, colorBadge = Color(0xFFE4795C), modifier = Modifier.weight(1f))
                ResumenSeveridadMiniChip(etiqueta = "Medio", cantidad = conteoMedio, colorBadge = PlagOutColors.Sun, modifier = Modifier.weight(1f))
                ResumenSeveridadMiniChip(etiqueta = "Bajo", cantidad = conteoBajo, colorBadge = Color(0xFF8ACD86), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResumenSeveridadMiniChip(etiqueta: String, cantidad: Int, colorBadge: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colorBadge, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "$etiqueta: $cantidad",
                color = PlagOutColors.TextOnDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TarjetaReporteItem(
    reporte: ReporteDetalleResponse,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale = rememberPressScale(interactionSource)

    val (badgeColor, badgeFondo, emojiSeveridad) = when (reporte.nivel_severidad.lowercase()) {
        "alto" -> Triple(Color(0xFFC62828), Color(0xFFFFEBEE), "🔴")
        "medio" -> Triple(Color(0xFFEF6C00), Color(0xFFFFF3E0), "🟡")
        else -> Triple(Color(0xFF2E7D32), Color(0xEBF0F8EC), "🟢")
    }

    val fechaFormateada = remember(reporte.timestamp_ms) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(reporte.timestamp_ms))
        } catch (e: Exception) {
            "Fecha no disp."
        }
    }

    val terrenoNombre = reporte.terreno_nombre ?: "Terreno sin especificar"
    val plantacionInfo = if (!reporte.cultivo_nombre.isNullOrBlank()) {
        "Plantación de ${reporte.cultivo_nombre}"
    } else {
        "Plantación ID: ${reporte.plantacion_id ?: "-"}"
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        color = PlagOutColors.Surface,
        border = BorderStroke(1.dp, PlagOutColors.Divider),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .testTag("tarjetaReporte_${reporte.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = PlagOutColors.Forest.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.BugReport,
                                contentDescription = null,
                                tint = PlagOutColors.Forest,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = reporte.plaga_nombre.ifBlank { "Plaga no especificada" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badge de Severidad con color (Bajo 🟢, Medio 🟡, Alto 🔴)
                Surface(
                    color = badgeFondo,
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emojiSeveridad, fontSize = 10.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = reporte.nivel_severidad,
                            color = badgeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.6f))
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Landscape,
                    contentDescription = null,
                    tint = PlagOutColors.TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "$terrenoNombre • $plantacionInfo",
                    fontSize = 13.sp,
                    color = PlagOutColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(6.dp))

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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = fechaFormateada,
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ver detalle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.Forest
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PlagOutColors.Forest,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
