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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.plag_out.ui.theme.estiloFinalizado
import com.example.plag_out.ui.theme.rememberPressScale
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

/** Valor de `filtro` que muestra los monitoreos finalizados (`activo == false`) en vez de los activos. */
private const val FILTRO_FINALIZADOS = 3

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreosScreen(
    monitoreosViewModel: MonitoreosViewModel,
    plantacionesViewModel: PlantacionesViewModel,
    terrenosViewModel: TerrenosViewModel,
    navController: NavHostController
) {
    val state by monitoreosViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        monitoreosViewModel.getMonitoreos()
        plantacionesViewModel.getPlantaciones()
        terrenosViewModel.getTerrenos()
    }

    // -1 = activos; 0/1/2 = nivel de alerta (activos); FILTRO_FINALIZADOS = finalizados
    var filtro by rememberSaveable { mutableStateOf(-1) }

    val ordenados = remember(state.monitoreos) {
        state.monitoreos.sortedWith(
            compareByDescending<MonitoreoResponse> { it.nivel_alerta }
                .thenByDescending { it.progreso }
        )
    }
    val filtrados = remember(ordenados, filtro) {
        when (filtro) {
            FILTRO_FINALIZADOS -> ordenados.filter { !it.activo }
            in 0..2 -> ordenados.filter { it.activo && it.nivel_alerta.coerceIn(0, 2) == filtro }
            else -> ordenados.filter { it.activo }
        }
    }
    val activos = remember(state.monitoreos) { state.monitoreos.filter { it.activo } }

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
            PanelDeCampo(monitoreos = activos)

            val opciones = remember(state.monitoreos, activos) {
                listOf(
                    OpcionFiltro(-1, "Activos", activos.size),
                    OpcionFiltro(0, "Saludable", activos.count { it.nivel_alerta == 0 }, estiloDeNivel(0).icono, estiloDeNivel(0).color),
                    OpcionFiltro(1, "Atención", activos.count { it.nivel_alerta == 1 }, estiloDeNivel(1).icono, estiloDeNivel(1).color),
                    OpcionFiltro(2, "Crítico", activos.count { it.nivel_alerta >= 2 }, estiloDeNivel(2).icono, estiloDeNivel(2).color),
                    OpcionFiltro(FILTRO_FINALIZADOS, "Finalizados", state.monitoreos.count { !it.activo }, Icons.Filled.Flag, PlagOutColors.TextSecondary)
                )
            }
            FiltroChipsRow(opciones = opciones, seleccionado = filtro, onSeleccion = { filtro = it })

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = when {
                        state.isLoading -> "cargando"
                        activos.isEmpty() && filtro != FILTRO_FINALIZADOS -> "vacio"
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
                                        navController.navigate("monitoreo/${monitoreo.monitoreo_id}")
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

// ── Dominio: proyección de días al umbral ───────────────────────────────────

/**
 * Días estimados hasta alcanzar el GDD objetivo, o null si ya se alcanzó o si no hay forma de
 * proyectar un ritmo diario. Única fuente de esta cuenta: la usan tanto [MonitoreoCard] como la
 * pantalla de detalle de monitoreo.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun diasEstimadosAlUmbral(monitoreo: MonitoreoResponse): Int? {
    val restante = monitoreo.gdd_objetivo - monitoreo.gdd_acumulado
    val umbralAlcanzado = restante <= 0f || monitoreo.progreso >= 100f
    if (umbralAlcanzado) return null

    val promedioDiario = promedioGddDiario(monitoreo)
    return if (promedioDiario > 0f) ceil(restante / promedioDiario).toInt() else null
}

/**
 * Promedio de GDD acumulados por día desde `fecha_inicio`. Más estable que `gdd_diario` (el
 * ritmo de un solo día), que puede variar mucho por el clima puntual de esa jornada. Si el
 * backend todavía no manda `fecha_inicio`, o el monitoreo arrancó hoy mismo (0 días
 * transcurridos), no hay historial para promediar y se cae al GDD del día como aproximación.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun promedioGddDiario(monitoreo: MonitoreoResponse): Float {
    val inicio = monitoreo.fecha_inicio ?: return monitoreo.gdd_diario
    val diasTranscurridos = ChronoUnit.DAYS.between(inicio, monitoreo.fecha_actualizacion)
    return if (diasTranscurridos > 0) monitoreo.gdd_acumulado / diasTranscurridos else monitoreo.gdd_diario
}

// ── Card de monitoreo ───────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreoCard(
    monitoreo: MonitoreoResponse,
    onClick: () -> Unit
) {
    val estilo = if (!monitoreo.activo) estiloFinalizado() else estiloDeNivel(monitoreo.nivel_alerta)
    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)

    val restante = monitoreo.gdd_objetivo - monitoreo.gdd_acumulado
    val umbralAlcanzado = monitoreo.activo && (restante <= 0f || monitoreo.progreso >= 100f)
    val diasEstimados = diasEstimadosAlUmbral(monitoreo)

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
                        SelloDeNivel(estilo, pulsante = monitoreo.activo && monitoreo.nivel_alerta >= 2)
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

private const val PAGINA_INFO_PLANTACION = 0
private const val PAGINA_MONITOREOS_PLANTACION = 1

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreosPorPlantacion(
    plantacionId: Int,
    viewModel: MonitoreosViewModel,
    plantacionesViewModel: PlantacionesViewModel,
    onBack: () -> Unit,
    onMonitoreoClick: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val plantacionesState by plantacionesViewModel.state.collectAsState()
    val monitoreosFiltrados = state.monitoreos.filter { it.plantacion_id == plantacionId }
    val referencia = monitoreosFiltrados.firstOrNull()
    val plantacion = plantacionesState.plantaciones.find { it.plantacion_id == plantacionId }

    LaunchedEffect(Unit) { plantacionesViewModel.getPlantaciones() }

    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(plantacionesState.error) {
        plantacionesState.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
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
                        plantacion?.cultivo_nombre ?: referencia?.cultivo_nombre ?: "Plantación",
                        color = PlagOutColors.TextOnDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val terrenoNombre = plantacion?.terreno_nombre ?: referencia?.terreno_nombre
                    if (terrenoNombre != null) {
                        Text(terrenoNombre, color = PlagOutColors.TextOnDark.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                }
            }
        }

        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = PlagOutColors.Cream,
            contentColor = PlagOutColors.Forest
        ) {
            Tab(
                selected = pagerState.currentPage == PAGINA_INFO_PLANTACION,
                onClick = { scope.launch { pagerState.animateScrollToPage(PAGINA_INFO_PLANTACION) } },
                text = { Text("Información", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = pagerState.currentPage == PAGINA_MONITOREOS_PLANTACION,
                onClick = { scope.launch { pagerState.animateScrollToPage(PAGINA_MONITOREOS_PLANTACION) } },
                text = { Text("Monitoreos", fontWeight = FontWeight.SemiBold) }
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { pagina ->
            when (pagina) {
                PAGINA_INFO_PLANTACION -> InformacionPlantacionTab(
                    plantacion = plantacion,
                    monitoreosDeLaPlantacion = monitoreosFiltrados,
                    viewModel = plantacionesViewModel,
                    monitoreosViewModel = viewModel,
                    onEliminada = onBack
                )
                else -> MonitoreosDePlantacionTab(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refrescar() },
                    monitoreosFiltrados = monitoreosFiltrados,
                    onMonitoreoClick = onMonitoreoClick
                )
            }
        }
    }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun InformacionPlantacionTab(
    plantacion: PlantacionesResponse?,
    monitoreosDeLaPlantacion: List<MonitoreoResponse>,
    viewModel: PlantacionesViewModel,
    monitoreosViewModel: MonitoreosViewModel,
    onEliminada: () -> Unit
) {
    val plantacionesState by viewModel.state.collectAsState()
    var mostrarDialogoFinalizar by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    val sanos = monitoreosDeLaPlantacion.count { it.nivel_alerta == 0 }
    val atencion = monitoreosDeLaPlantacion.count { it.nivel_alerta == 1 }
    val criticos = monitoreosDeLaPlantacion.count { it.nivel_alerta >= 2 }
    val total = monitoreosDeLaPlantacion.size
    val totalAnimado = contadorAnimado(total)
    val diasDesdeSiembra = plantacion?.let { ChronoUnit.DAYS.between(it.fecha_siembra, LocalDate.now()).toInt() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Surface(
            color = (if (plantacion?.activa != false) PlagOutColors.Leaf else PlagOutColors.Bark).copy(alpha = 0.12f),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                if (plantacion?.activa != false) "ACTIVA" else "PAUSADA",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                color = if (plantacion?.activa != false) PlagOutColors.Leaf else PlagOutColors.Bark
            )
        }
        Spacer(Modifier.height(6.dp))
        if (!plantacion?.cultivo_nombre_cientifico.isNullOrBlank()) {
            Text(
                plantacion?.cultivo_nombre_cientifico ?: "",
                fontSize = 13.sp,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(18.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            AnilloSegmentado(
                segmentos = listOf(
                    sanos to estiloDeNivel(0).color,
                    atencion to estiloDeNivel(1).color,
                    criticos to estiloDeNivel(2).color
                ),
                total = total,
                modifier = Modifier.size(104.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$totalAnimado", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
                    Text(
                        if (total == 1) "monitoreo" else "monitoreos",
                        fontSize = 10.sp,
                        color = PlagOutColors.TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LeyendaEstadoTerrenoClaro(estiloDeNivel(0), sanos)
                LeyendaEstadoTerrenoClaro(estiloDeNivel(1), atencion)
                LeyendaEstadoTerrenoClaro(estiloDeNivel(2), criticos)
            }
        }

        Spacer(Modifier.height(20.dp))

        Surface(
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                EstadisticaCompacta(
                    "Sembrada",
                    plantacion?.fecha_siembra?.format(DateTimeFormatter.ofPattern("dd MMM", Locale("es"))) ?: "—",
                    Modifier.weight(1f)
                )
                SeparadorVertical()
                EstadisticaCompacta("Días", diasDesdeSiembra?.let { "$it" } ?: "—", Modifier.weight(1f))
                SeparadorVertical()
                EstadisticaCompacta("Monitoreos", "$total", Modifier.weight(1f))
            }
        }

        if (total == 0) {
            Spacer(Modifier.height(10.dp))
            EtiquetaInfo(Icons.Outlined.BugReport, "Todavía no hay monitoreos en esta plantación", PlagOutColors.RiskUnknown)
        }

        if (plantacion != null) {
            Spacer(Modifier.height(20.dp))

            if (plantacion.activa) {
                OutlinedButton(
                    onClick = { mostrarDialogoFinalizar = true },
                    enabled = !plantacionesState.procesando,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.RiskWarn),
                    border = BorderStroke(1.dp, PlagOutColors.RiskWarn),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btnFinalizarPlantacion")
                ) {
                    if (plantacionesState.procesando) {
                        CircularProgressIndicator(color = PlagOutColors.RiskWarn, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Finalizar plantación", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = { mostrarDialogoEliminar = true },
                enabled = !plantacionesState.procesando,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.RiskDanger),
                border = BorderStroke(1.dp, PlagOutColors.RiskDanger),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btnEliminarPlantacion")
            ) {
                if (plantacionesState.procesando) {
                    CircularProgressIndicator(color = PlagOutColors.RiskDanger, modifier = Modifier.size(20.dp))
                } else {
                    Text("Eliminar plantación", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    if (mostrarDialogoFinalizar && plantacion != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoFinalizar = false },
            modifier = Modifier.testTag("dialogFinalizarPlantacion"),
            title = { Text("¿Finalizar plantación?") },
            text = {
                Text(
                    "Vas a marcar \"${plantacion.cultivo_nombre}\" como pausada. Vas a poder seguir viendo su " +
                        "historial, pero no vas a poder cargar nuevos monitoreos mientras esté pausada."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoFinalizar = false
                        viewModel.finalizarPlantacion(plantacion.plantacion_id) {
                            monitoreosViewModel.finalizarPorPlantacion(plantacion.plantacion_id)
                        }
                    },
                    modifier = Modifier.testTag("btnConfirmarFinalizarPlantacion")
                ) {
                    Text("Finalizar", color = PlagOutColors.RiskWarn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoFinalizar = false }) { Text("Cancelar") }
            }
        )
    }

    if (mostrarDialogoEliminar && plantacion != null) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            modifier = Modifier.testTag("dialogEliminarPlantacion"),
            title = { Text("¿Eliminar plantación?") },
            text = {
                Text(
                    "Se va a eliminar \"${plantacion.cultivo_nombre}\" de forma permanente, junto con sus monitoreos. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoEliminar = false
                        viewModel.eliminarPlantacion(plantacion.plantacion_id) {
                            monitoreosViewModel.purgarPorPlantacion(plantacion.plantacion_id)
                            onEliminada()
                        }
                    },
                    modifier = Modifier.testTag("btnConfirmarEliminarPlantacion")
                ) {
                    Text("Eliminar", color = PlagOutColors.RiskDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun LeyendaEstadoTerrenoClaro(estilo: NivelEstilo, cantidad: Int) {
    val valor = contadorAnimado(cantidad)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(estilo.icono, contentDescription = null, tint = estilo.color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            estilo.etiqueta,
            color = PlagOutColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp)
        )
        Text("$valor", color = PlagOutColors.TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MonitoreosDePlantacionTab(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    monitoreosFiltrados: List<MonitoreoResponse>,
    onMonitoreoClick: (Int) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
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
                        MonitoreoCard(monitoreo) { onMonitoreoClick(monitoreo.monitoreo_id) }
                    }
                }
            }
        }
    }
}
