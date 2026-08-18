package com.example.plag_out

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Landscape
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

                PanelFiltrosPlegable(
                    expandido = filtrosExpandidos,
                    onToggleExpandido = { filtrosExpandidos = !filtrosExpandidos },
                    hayFiltroActivo = filtroActivo,
                    etiquetaAbrir = "Filtrar reportes",
                    resumen = if (filtroActivo) {
                        "Mostrando $totalFiltrados de $totalOriginal (Filtros activos)"
                    } else {
                        "Mostrando $totalOriginal reportes"
                    },
                    onLimpiar = {
                        filtroSeveridad = FILTRO_TODOS
                        filtroTerreno = FILTRO_TODOS
                    }
                ) {
                    // --- Categoría 1: Nivel de Severidad ---
                    EncabezadoGrupoFiltro(Icons.Outlined.Shield, "NIVEL DE SEVERIDAD")

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
                        EncabezadoGrupoFiltro(Icons.Outlined.Landscape, "TERRENO / LUGAR")

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
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderReportes")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativa"
    )

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

/** Fila de la leyenda del header, en el mismo formato que la de Monitoreos y Terrenos. */
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
    val terrenoNombre = if (esPropio) {
        reporte.terreno_nombre?.takeIf { it.isNotBlank() } ?: "Terreno no especificado"
    } else {
        val tNom = reporte.terreno_nombre?.takeIf { it.isNotBlank() }
        val dist = reporte.distancia_km
        if (tNom != null && dist != null) {
            "Cercano a $tNom (${dist} km)"
        } else if (tNom != null) {
            "Cercano a $tNom"
        } else if (dist != null) {
            "A ${dist} km de tu terreno"
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

                Spacer(Modifier.height(6.dp))

                // Chip distintivo: Propio vs Comunidad
                Surface(
                    color = if (esPropio) PlagOutColors.Forest.copy(alpha = 0.12f) else Color(0xFF1565C0).copy(alpha = 0.12f),
                    shape = CircleShape
                ) {
                    Text(
                        text = if (esPropio) "Propio" else "Comunidad",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (esPropio) PlagOutColors.Forest else Color(0xFF1565C0),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
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
