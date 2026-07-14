package com.example.plag_out

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.plag_out.ui.theme.AnilloProgreso
import com.example.plag_out.ui.theme.AnilloSegmentado
import com.example.plag_out.ui.theme.CargandoCentrado
import com.example.plag_out.ui.theme.EstadisticaCompacta
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.NivelEstilo
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.SeparadorVertical
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.contadorAnimado
import com.example.plag_out.ui.theme.estiloDeNivel
import com.example.plag_out.ui.theme.rememberPressScale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreosScreen(
    monitoreosViewModel: MonitoreosViewModel,
    plantacionesViewModel: PlantacionesViewModel,
    navController: NavHostController
) {
    val state by monitoreosViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        monitoreosViewModel.getMonitoreos()
        plantacionesViewModel.getPlantaciones()
    }

    // -1 = todos; 0/1/2 = nivel de alerta
    var filtro by rememberSaveable { mutableStateOf(-1) }

    val ordenados = remember(state.monitoreos) {
        state.monitoreos.sortedWith(
            compareByDescending<MonitoreoResponse> { it.nivel_alerta }
                .thenByDescending { it.progreso }
        )
    }
    val filtrados = remember(ordenados, filtro) {
        if (filtro < 0) ordenados
        else ordenados.filter { it.nivel_alerta.coerceIn(0, 2) == filtro }
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
                label = "fabScale"
            )
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("agregar_monitoreo") },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
                    .testTag("btnNuevoMonitoreo")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Monitoreo", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { monitoreosViewModel.refrescar() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PanelDeCampo(monitoreos = state.monitoreos)

            val opciones = remember(state.monitoreos) {
                listOf(
                    OpcionFiltro(-1, "Todos", state.monitoreos.size),
                    OpcionFiltro(0, "Saludable", state.monitoreos.count { it.nivel_alerta == 0 }, estiloDeNivel(0).icono, estiloDeNivel(0).color),
                    OpcionFiltro(1, "Atención", state.monitoreos.count { it.nivel_alerta == 1 }, estiloDeNivel(1).icono, estiloDeNivel(1).color),
                    OpcionFiltro(2, "Crítico", state.monitoreos.count { it.nivel_alerta >= 2 }, estiloDeNivel(2).icono, estiloDeNivel(2).color)
                )
            }
            FiltroChipsRow(opciones = opciones, seleccionado = filtro, onSeleccion = { filtro = it })

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = when {
                        state.isLoading -> "cargando"
                        state.monitoreos.isEmpty() -> "vacio"
                        filtrados.isEmpty() -> "sin-resultados"
                        else -> "lista-$filtro"
                    },
                    transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                    label = "monitoreosContent"
                ) { target ->
                    when (target) {
                        "cargando" -> SkeletonCargando()
                        // Box con scroll para que el gesto de pull-to-refresh también funcione sin lista
                        "vacio" -> Box(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            EstadoVacioFlotante(
                                icono = Icons.Outlined.BugReport,
                                titulo = "Sin monitoreos activos",
                                subtitulo = "Creá un monitoreo para seguir el riesgo de plagas en tus plantaciones."
                            )
                        }
                        "sin-resultados" -> Box(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            EstadoSinResultados(subtitulo = "No hay monitoreos en este estado.")
                        }
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(filtrados, key = { _, m -> m.monitoreo_id }) { index, monitoreo ->
                                StaggeredAppear(index = index) {
                                    MonitoreoCard(monitoreo) {
                                        navController.navigate("plantacion/${monitoreo.plantacion_id}")
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
}

// ── Header: panel de estado general ─────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PanelDeCampo(monitoreos: List<MonitoreoResponse>) {
    val total = monitoreos.size
    val sanos = monitoreos.count { it.nivel_alerta <= 0 }
    val atencion = monitoreos.count { it.nivel_alerta == 1 }
    val criticos = monitoreos.count { it.nivel_alerta >= 2 }

    val respiracion = rememberInfiniteTransition(label = "respiracionHeader")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativa"
    )

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

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 22.dp)) {
            val hoy = remember {
                LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es")))
                    .replaceFirstChar { it.uppercase() }
            }
            Text(
                hoy,
                color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Text(
                "Mis Monitoreos",
                color = PlagOutColors.TextOnDark,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalAnimado = contadorAnimado(total)
                AnilloSegmentado(
                    segmentos = listOf(
                        sanos to estiloDeNivel(0).colorSobreOscuro,
                        atencion to estiloDeNivel(1).colorSobreOscuro,
                        criticos to estiloDeNivel(2).colorSobreOscuro
                    ),
                    total = total,
                    modifier = Modifier.size(110.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalAnimado", color = PlagOutColors.TextOnDark, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (total == 1) "activo" else "activos",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.width(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LeyendaEstado(estiloDeNivel(0), sanos)
                    LeyendaEstado(estiloDeNivel(1), atencion)
                    LeyendaEstado(estiloDeNivel(2), criticos)
                }
            }
        }
    }
}

@Composable
private fun LeyendaEstado(estilo: NivelEstilo, cantidad: Int) {
    val valor = contadorAnimado(cantidad)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(estilo.icono, contentDescription = null, tint = estilo.colorSobreOscuro, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            estilo.etiqueta,
            color = PlagOutColors.TextOnDark.copy(alpha = 0.85f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text("$valor", color = PlagOutColors.TextOnDark, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Card de monitoreo ───────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreoCard(
    monitoreo: MonitoreoResponse,
    onClick: () -> Unit
) {
    val estilo = estiloDeNivel(monitoreo.nivel_alerta)
    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)

    val restante = monitoreo.gdd_objetivo - monitoreo.gdd_acumulado
    val umbralAlcanzado = restante <= 0f || monitoreo.progreso >= 100f
    val diasEstimados =
        if (!umbralAlcanzado && monitoreo.gdd_diario > 0f) ceil(restante / monitoreo.gdd_diario).toInt()
        else null

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala }
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(estilo.color)
            )

            Column(Modifier.padding(start = 17.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        SelloDeNivel(estilo, pulsante = monitoreo.nivel_alerta >= 2)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            monitoreo.plaga_nombre,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Grass, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                monitoreo.cultivo_nombre,
                                fontSize = 13.sp,
                                color = PlagOutColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Outlined.Landscape, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                monitoreo.terreno_nombre,
                                fontSize = 13.sp,
                                color = PlagOutColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    AnilloProgreso(progreso = monitoreo.progreso, color = estilo.color, tamano = 66.dp)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EstadisticaCompacta("Acumulado", "${monitoreo.gdd_acumulado.toInt()}", Modifier.weight(1f))
                    SeparadorVertical()
                    EstadisticaCompacta("Objetivo", "${monitoreo.gdd_objetivo.toInt()}", Modifier.weight(1f))
                    SeparadorVertical()
                    EstadisticaCompacta("GDD hoy", "+${monitoreo.gdd_diario.toInt()}", Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        umbralAlcanzado -> EtiquetaInfo(Icons.Filled.Flag, "Umbral alcanzado", PlagOutColors.RiskDanger)
                        diasEstimados != null -> EtiquetaInfo(Icons.Outlined.Schedule, "≈ $diasEstimados días al umbral", PlagOutColors.Forest)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        monitoreo.fecha_actualizacion.format(DateTimeFormatter.ofPattern("dd MMM", Locale("es"))),
                        fontSize = 11.sp,
                        color = PlagOutColors.TextSecondary
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Monitoreos de una plantación ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreosPorPlantacion(
    plantacionId: Int,
    viewModel: MonitoreosViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val monitoreosFiltrados = state.monitoreos.filter { it.plantacion_id == plantacionId }
    val referencia = monitoreosFiltrados.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                }
                Column {
                    Text(
                        "Detalle de plantación",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        referencia?.cultivo_nombre ?: "Monitoreos",
                        color = PlagOutColors.TextOnDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (referencia != null) {
                        Text(referencia.terreno_nombre, color = PlagOutColors.TextOnDark.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }
            }
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.refrescar() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (monitoreosFiltrados.isEmpty()) {
                // Box con scroll para que el gesto de pull-to-refresh también funcione sin lista
                Box(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    EstadoVacioFlotante(
                        icono = Icons.Outlined.BugReport,
                        titulo = "Sin monitoreos activos",
                        subtitulo = "Creá un monitoreo para seguir el riesgo de plagas en esta plantación."
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(monitoreosFiltrados, key = { _, m -> m.monitoreo_id }) { index, monitoreo ->
                        StaggeredAppear(index = index) {
                            MonitoreoCard(monitoreo) { }
                        }
                    }
                }
            }
        }
    }
}
