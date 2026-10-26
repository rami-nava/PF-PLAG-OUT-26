package com.example.plag_out

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.contadorAnimado
import com.example.plag_out.ui.theme.estiloDeNivel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMetricasScreen(
    viewModel: AdminMetricasViewModel,
    onVolver: (() -> Unit)? = null,
    // true en el panel admin: tableros de todos los productores, con lotes anónimos.
    global: Boolean = false
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(global) {
        viewModel.cargar(global = global)
    }

    Column(Modifier.fillMaxSize()) {
        if (onVolver != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 8.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onVolver) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = PlagOutColors.Forest
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "Tableros de Control",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        when (state.pestañaActual) {
                            PestañaDashboard.PRODUCTOR -> "Monitoreo operativo de lotes, cultivos y superficie en riesgo"
                            PestañaDashboard.AGRONOMO -> "Etapas de la plaga y eficacia de tratamientos"
                            PestañaDashboard.HISTORICO -> "Evolución histórica y prevalencia de plagas por campaña agrícola"
                        },
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        } else {
            EncabezadoSeccionAdmin(
                titulo = "Tableros de Control",
                subtitulo = when (state.pestañaActual) {
                    PestañaDashboard.PRODUCTOR -> "Monitoreo operativo de lotes, cultivos y superficie en riesgo"
                    PestañaDashboard.AGRONOMO -> "Etapas de la plaga y eficacia de tratamientos"
                    PestañaDashboard.HISTORICO -> "Evolución histórica y prevalencia de plagas por campaña agrícola"
                }
            )
        }

        // Selector principal de perspectiva de dashboard
        SelectorPerspectivaDashboard(
            pestañaActual = state.pestañaActual,
            onSeleccionar = viewModel::cambiarPestaña,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        BannerErrorAdmin(
            mensaje = state.error,
            onCerrar = viewModel::descartarError,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        when {
            state.isLoading -> SkeletonCargando(alturaTarjeta = 160.dp, cantidad = 3)
            state.campañas.isEmpty() && state.lotesCriticos.isEmpty() -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refrescar,
                modifier = Modifier.fillMaxSize()
            ) {
                EstadoVacioFlotante(
                    icono = Icons.Outlined.Insights,
                    titulo = "Sin datos registrados",
                    subtitulo = if (global) {
                        "Cuando los productores den de alta lotes y monitoreos, los tableros analíticos se generarán automáticamente."
                    } else {
                        "Cuando des de alta lotes y monitoreos, tus tableros analíticos se generarán automáticamente."
                    }
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refrescar,
                    modifier = Modifier.fillMaxSize()
                ) {
                    AnimatedContent(
                        targetState = state.pestañaActual,
                        transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(180)) },
                        label = "cambioPestañaDashboard"
                    ) { pestaña ->
                        when (pestaña) {
                            PestañaDashboard.PRODUCTOR -> VistaDashboardProductor(state = state)
                            PestañaDashboard.AGRONOMO -> VistaDashboardAgronomo(state = state, global = global)
                            PestañaDashboard.HISTORICO -> VistaDashboardHistorico(
                                state = state,
                                onSeleccionarCampaña = viewModel::seleccionarCampaña
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Selector de Perspectiva ──────────────────────────────────────────────────

@Composable
private fun SelectorPerspectivaDashboard(
    pestañaActual: PestañaDashboard,
    onSeleccionar: (PestañaDashboard) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlagOutColors.CreamDeep.copy(alpha = 0.6f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ItemBotonPerspectiva(
            titulo = "Lotes",
            icono = Icons.Outlined.Agriculture,
            activo = pestañaActual == PestañaDashboard.PRODUCTOR,
            onClick = { onSeleccionar(PestañaDashboard.PRODUCTOR) },
            modifier = Modifier.weight(1f)
        )
        ItemBotonPerspectiva(
            titulo = "Control",
            icono = Icons.Outlined.Schedule,
            activo = pestañaActual == PestañaDashboard.AGRONOMO,
            onClick = { onSeleccionar(PestañaDashboard.AGRONOMO) },
            modifier = Modifier.weight(1f)
        )
        ItemBotonPerspectiva(
            titulo = "Campañas",
            icono = Icons.Outlined.CalendarMonth,
            activo = pestañaActual == PestañaDashboard.HISTORICO,
            onClick = { onSeleccionar(PestañaDashboard.HISTORICO) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ItemBotonPerspectiva(
    titulo: String,
    icono: ImageVector,
    activo: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo by animateColorAsState(
        targetValue = if (activo) PlagOutColors.Forest else Color.Transparent,
        animationSpec = tween(220),
        label = "fondoPestaña"
    )
    val colorTexto by animateColorAsState(
        targetValue = if (activo) PlagOutColors.TextOnDark else PlagOutColors.TextMain,
        animationSpec = tween(220),
        label = "textoPestaña"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = fondo,
        shadowElevation = if (activo) 2.dp else 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(titulo, color = colorTexto, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 1. VISTA PRODUCTOR: OPERATIVA, LOTES Y SUPERFICIE EN RIESGO
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VistaDashboardProductor(state: AdminMetricasUIState) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Semáforo de superficie por etapa térmica
        state.semaforoHectareas?.let { semaforo ->
            item {
                StaggeredAppear(index = 0) {
                    TarjetaDonutSemaforoHectareas(semaforo = semaforo)
                }
            }
        }

        // 2. Ranking de Lotes en Alerta (¿Dónde actuar primero?)
        item {
            StaggeredAppear(index = 1) {
                TarjetaRankingLotesCriticos(lotes = state.lotesCriticos)
            }
        }

        // 3. Plagas por Cultivo desde la Siembra
        item {
            StaggeredAppear(index = 2) {
                TarjetaPlagasPorCultivo(cultivos = state.plagasPorCultivo)
            }
        }
    }
}

@Composable
private fun TarjetaDonutSemaforoHectareas(semaforo: SemaforoHectareas) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Assessment, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Superficie por etapa de la plaga", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Hectáreas según la etapa más avanzada de cada lote", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(18.dp))

            // Donut Chart central
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                contentAlignment = Alignment.Center
            ) {
                GraficoDonutSemaforo(
                    porcVerde = semaforo.porcentajeVerde,
                    porcAmarillo = semaforo.porcentajeAmarillo,
                    porcRojo = semaforo.porcentajeRojo,
                    tamano = 160.dp,
                    grosor = 18.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format(LocalConfiguration.current.locales[0], "%.0f ha", semaforo.totalHectareas),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "${semaforo.totalLotes} ${if (semaforo.totalLotes == 1) "lote" else "lotes"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Leyendas de desglose
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ItemDesgloseSemaforo(
                    etiqueta = "En huevo",
                    hectareas = semaforo.hectareasVerde,
                    porcentaje = semaforo.porcentajeVerde,
                    lotes = semaforo.lotesVerde,
                    color = PlagOutColors.RiskOk,
                    modifier = Modifier.weight(1f)
                )
                ItemDesgloseSemaforo(
                    etiqueta = "Eclosión cerca",
                    hectareas = semaforo.hectareasAmarillo,
                    porcentaje = semaforo.porcentajeAmarillo,
                    lotes = semaforo.lotesAmarillo,
                    color = PlagOutColors.RiskWarn,
                    modifier = Modifier.weight(1f)
                )
                ItemDesgloseSemaforo(
                    etiqueta = "Eclosionó",
                    hectareas = semaforo.hectareasRojo,
                    porcentaje = semaforo.porcentajeRojo,
                    lotes = semaforo.lotesRojo,
                    color = PlagOutColors.RiskDanger,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().testTag("txtLotesFueraDelGrafico")
            ) {
                Text(
                    textoLotesRegistrados(semaforo),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextMain
                )
                puntosLotesDelSemaforo(semaforo).forEach { punto ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text("•", fontSize = 12.sp, color = PlagOutColors.TextSecondary, modifier = Modifier.padding(start = 2.dp, end = 8.dp))
                        Text(punto, fontSize = 12.sp, color = PlagOutColors.TextSecondary, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun GraficoDonutSemaforo(
    porcVerde: Float,
    porcAmarillo: Float,
    porcRojo: Float,
    tamano: Dp,
    grosor: Dp
) {
    var iniciado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }

    val animVerde by animateFloatAsState(if (iniciado) porcVerde else 0f, tween(900, easing = FastOutSlowInEasing), label = "animDonutV")
    val animAmarillo by animateFloatAsState(if (iniciado) porcAmarillo else 0f, tween(900, easing = FastOutSlowInEasing), label = "animDonutA")
    val animRojo by animateFloatAsState(if (iniciado) porcRojo else 0f, tween(900, easing = FastOutSlowInEasing), label = "animDonutR")

    Canvas(Modifier.size(tamano)) {
        val grosorPx = grosor.toPx()
        val arcSize = Size(size.width - grosorPx, size.height - grosorPx)
        val topLeft = Offset(grosorPx / 2, grosorPx / 2)

        // Fondo neutro
        drawArc(
            color = Color(0xFFECE3CC),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = grosorPx)
        )

        var anguloInicio = -90f
        val segmentos = listOf(
            Triple(animVerde, PlagOutColors.RiskOk, "verde"),
            Triple(animAmarillo, PlagOutColors.RiskWarn, "amarillo"),
            Triple(animRojo, PlagOutColors.RiskDanger, "rojo")
        ).filter { it.first > 0.5f }

        val separacion = if (segmentos.size > 1) 4f else 0f
        val disponible = 360f - (separacion * segmentos.size)

        segmentos.forEach { (pct, color, _) ->
            val barrido = (disponible * (pct / 100f)).coerceAtLeast(1f)
            drawArc(
                color = color,
                startAngle = anguloInicio,
                sweepAngle = barrido,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = grosorPx, cap = StrokeCap.Round)
            )
            anguloInicio += barrido + separacion
        }
    }
}

@Composable
private fun ItemDesgloseSemaforo(
    etiqueta: String,
    hectareas: Float,
    porcentaje: Float,
    lotes: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(5.dp))
                Text(etiqueta, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                String.format(LocalConfiguration.current.locales[0], "%.1f ha", hectareas),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )
            Text(
                "${porcentaje.toInt()}% · $lotes ${if (lotes == 1) "lote" else "lotes"}",
                fontSize = 10.sp,
                color = PlagOutColors.TextSecondary
            )
        }
    }
}

@Composable
private fun TarjetaRankingLotesCriticos(lotes: List<LoteEnRiesgoCritico>) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = PlagOutColors.Terracotta, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Lotes por etapa", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Del más avanzado al menos avanzado según la etapa y el avance hacia la eclosión", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (lotes.isEmpty()) {
                Text("No hay monitoreos activos con mediciones.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
            } else {
                lotes.take(5).forEachIndexed { index, lote ->
                    ItemFilaLoteCritico(lote = lote)
                    if (index < lotes.take(5).size - 1) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f))
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemFilaLoteCritico(lote: LoteEnRiesgoCritico) {
    val estilo = estiloDeNivel(lote.nivelAlerta)

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lote.terrenoNombre, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
                    if (lote.terrenoArea > 0f) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            String.format(LocalConfiguration.current.locales[0], "· %.0f ha", lote.terrenoArea),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlagOutColors.TextSecondary
                        )
                    }
                }
                val infoCultivo = if (lote.diasDesdeSiembra != null && lote.monitoreoId >= 0) {
                    "${lote.cultivoNombre} (${lote.diasDesdeSiembra} días) • ${lote.plagaNombre}"
                } else {
                    "${lote.cultivoNombre} • ${lote.plagaNombre}"
                }
                Text(
                    infoCultivo,
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary
                )
            }

            Surface(
                color = estilo.color.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(estilo.color))
                    Spacer(Modifier.width(4.dp))
                    Text(estilo.etiqueta, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = estilo.color)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Barra de progreso térmico (GDD)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PlagOutColors.CreamDeep)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((lote.progresoGdd / 100f).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(estilo.color)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${lote.progresoGdd.toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )
        }
    }
}

@Composable
private fun TarjetaPlagasPorCultivo(cultivos: List<ResumenCultivoPlagas>) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Grass, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Plagas Incidentes por Cultivo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Prevalencia de plagas en los lotes monitoreados", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (cultivos.isEmpty()) {
                Text("No hay cultivos registrados con monitoreos.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
            } else {
                cultivos.forEachIndexed { index, cultivo ->
                    BloqueCultivoPlagas(cultivo = cultivo)
                    if (index < cultivos.size - 1) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f))
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BloqueCultivoPlagas(cultivo: ResumenCultivoPlagas) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(cultivo.cultivoNombre, fontSize = 15.sp, fontWeight = FontWeight.Black, color = PlagOutColors.TextMain)
            }
            Text(
                "${cultivo.totalLotes} ${if (cultivo.totalLotes == 1) "lote" else "lotes"} · ${cultivo.totalHectareas.toInt()} ha",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.Forest
            )
        }

        Spacer(Modifier.height(8.dp))

        cultivo.plagas.forEach { plaga ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(plaga.plagaNombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                        Text(
                            "${plaga.totalLotes} ${if (plaga.totalLotes == 1) "lote" else "lotes"} (${plaga.porcentaje.toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlagOutColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PlagOutColors.CreamDeep)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((plaga.porcentaje / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(PlagOutColors.Leaf)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 2. VISTA AGRÓNOMO: FENOLOGÍA, GDD Y VENTANA ÓPTIMA DE CONTROL
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VistaDashboardAgronomo(state: AdminMetricasUIState, global: Boolean) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            StaggeredAppear(index = 0) {
                TarjetaEtapasMonitoreos(conteo = conteoPorEtapa(state.ventanasControl))
            }
        }
        item {
            StaggeredAppear(index = 1) {
                if (global) TarjetaTratamientos(tratamientos = state.tratamientos)
                else TarjetaTratamientosDelProductor()
            }
        }
    }
}

/** Cuántos monitoreos activos hay en cada etapa, con el mismo texto que el panel. */
@Composable
private fun TarjetaEtapasMonitoreos(conteo: Map<EstadoVentanaControl, Int>) {
    TarjetaMetrica(
        icono = Icons.Outlined.Schedule,
        titulo = "Etapas de los monitoreos activos",
        subtitulo = "Cuántos monitoreos con mediciones hay en cada etapa según los grados-día acumulados."
    ) {
        EstadoVentanaControl.entries.forEachIndexed { index, etapa ->
            val (color, etiqueta) = when (etapa) {
                EstadoVentanaControl.HUEVO -> PlagOutColors.RiskOk to "Huevo"
                EstadoVentanaControl.ECLOSION_CERCA -> PlagOutColors.RiskWarn to "Eclosión cerca"
                EstadoVentanaControl.ECLOSIONADA -> PlagOutColors.RiskDanger to "Eclosionó"
            }
            val cantidad = conteo[etapa] ?: 0
            Column(Modifier.fillMaxWidth().testTag("etapa_${etapa.name}")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(etiqueta, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.weight(1f))
                    Text(
                        "$cantidad ${if (cantidad == 1) "monitoreo" else "monitoreos"}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                }
            }
            if (index < EstadoVentanaControl.entries.size - 1) {
                HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))
            }
        }
    }
}

/** Los tratamientos de todos los productores, por producto (como Control del panel). */
@Composable
private fun TarjetaTratamientos(tratamientos: TratamientosAdmin?) {
    TarjetaMetrica(
        icono = Icons.Outlined.Verified,
        titulo = "Eficacia de tratamientos",
        subtitulo = "Con franja sin tratar (testigo): eficacia de Henderson-Tilton. Sin testigo: reducción observada, que incluye la mortalidad natural y no es una eficacia."
    ) {
        if (tratamientos == null || tratamientos.registrados == 0) {
            Text("Todavía no hay tratamientos registrados.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
            return@TarjetaMetrica
        }
        Text(
            "${tratamientos.registrados} registrados · ${tratamientos.evaluados} con conteo posterior · ${tratamientos.con_testigo} con testigo",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PlagOutColors.TextMain
        )
        tratamientos.productos.forEach { producto ->
            HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))
            Column(Modifier.fillMaxWidth().testTag("tratamiento_${producto.producto}")) {
                Text(producto.producto, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                Text(
                    "${producto.tratamientos} ${if (producto.tratamientos == 1) "tratamiento" else "tratamientos"} · ${producto.evaluados} ${if (producto.evaluados == 1) "evaluado" else "evaluados"}",
                    fontSize = 11.sp,
                    color = PlagOutColors.TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    CifraTratamiento("Eficacia con testigo", producto.eficacia_promedio, Modifier.weight(1f))
                    CifraTratamiento("Reducción sin testigo", producto.reduccion_observada_promedio, Modifier.weight(1f))
                }
            }
        }
        Text(
            "Promedios simples por producto. Un valor negativo indica que la plaga aumentó. Fuente: ${tratamientos.cita}.",
            fontSize = 10.sp,
            color = PlagOutColors.TextSecondary,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun CifraTratamiento(etiqueta: String, porcentaje: Float?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            formatearPorcentajeTratamiento(porcentaje),
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlagOutColors.TextMain
        )
        Text(etiqueta, fontSize = 10.sp, color = PlagOutColors.TextSecondary)
    }
}

/** El productor ve y carga sus tratamientos en cada monitoreo, no acá. */
@Composable
private fun TarjetaTratamientosDelProductor() {
    TarjetaMetrica(
        icono = Icons.Outlined.Verified,
        titulo = "Eficacia de tratamientos",
        subtitulo = "Registrá cada tratamiento con el conteo de la plaga antes y después desde el detalle del monitoreo, en la pestaña Tratamientos. Ahí se calcula su eficacia."
    ) {}
}

@Composable
private fun TarjetaMetrica(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(icono, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text(subtitulo, fontSize = 12.sp, color = PlagOutColors.TextSecondary, lineHeight = 16.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            contenido()
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 3. VISTA HISTÓRICO: CAMPAÑAS Y EVOLUCIÓN MACRO (INTA / BOLSA DE CEREALES)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun VistaDashboardHistorico(
    state: AdminMetricasUIState,
    onSeleccionarCampaña: (String) -> Unit
) {
    val campañas = state.campañas
    val seleccionada = state.campañaSeleccionada ?: campañas.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Selector horizontal de campañas
        if (campañas.size > 1) {
            item {
                val indiceSel = campañas.indexOfFirst { it.nombre == seleccionada?.nombre }.coerceAtLeast(0)
                FiltroChipsRow(
                    opciones = campañas.mapIndexed { index, camp ->
                        val etiqueta = if (index == 0) "${camp.nombre} (Actual)" else camp.nombre
                        OpcionFiltro(
                            id = index,
                            etiqueta = etiqueta,
                            cantidad = camp.totalMonitoreos,
                            icono = Icons.Outlined.CalendarMonth
                        )
                    },
                    seleccionado = indiceSel,
                    onSeleccion = { nuevoIndice ->
                        if (nuevoIndice in campañas.indices) {
                            onSeleccionarCampaña(campañas[nuevoIndice].nombre)
                        }
                    }
                )
            }
        }

        // Resumen global consolidado
        item {
            StaggeredAppear(index = 0) {
                TarjetaResumenGlobal(
                    totalMonitoreos = state.totalMonitoreosHistoricos,
                    totalHectareas = state.totalHectareasHistoricas,
                    totalCampañas = state.campañas.size,
                    plagaLider = state.plagaHistoricaLider
                )
            }
        }

        // Plaga predominante y desgloses de la campaña
        seleccionada?.let { camp ->
            item {
                StaggeredAppear(index = 1) {
                    TarjetaPlagaPredominanteCampaña(campaña = camp)
                }
            }

            item {
                StaggeredAppear(index = 2) {
                    TarjetaMonitoreosPorCultivoCampaña(campaña = camp)
                }
            }

            item {
                StaggeredAppear(index = 3) {
                    TarjetaPrevalenciaPlagas(campaña = camp)
                }
            }
        }
    }
}

// ── Tarjeta: Resumen Global Acumulado ──────────────────────────────────────────

@Composable
private fun TarjetaResumenGlobal(
    totalMonitoreos: Int,
    totalHectareas: Float,
    totalCampañas: Int,
    plagaLider: String?
) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = PlagOutColors.Forest.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Assessment, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("HISTÓRICO CONSOLIDADO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.Leaf, letterSpacing = 0.6.sp)
                    Text("Resumen de todas las campañas registradas", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ItemMetricaCompacta(etiqueta = "Monitoreos", valor = "${contadorAnimado(totalMonitoreos)}", icono = Icons.Outlined.BugReport, modifier = Modifier.weight(1f))
                ItemMetricaCompacta(
                    etiqueta = "Hectáreas",
                    valor = if (totalHectareas > 0f) String.format(LocalConfiguration.current.locales[0], "%.1f ha", totalHectareas) else "0 ha",
                    icono = Icons.Outlined.Agriculture,
                    modifier = Modifier.weight(1f)
                )
                ItemMetricaCompacta(etiqueta = "Campañas", valor = "${contadorAnimado(totalCampañas)}", icono = Icons.Outlined.CalendarMonth, modifier = Modifier.weight(1f))
            }

            if (!plagaLider.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.6f))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mayor incidencia histórica:", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                    Spacer(Modifier.width(6.dp))
                    Text(plagaLider, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                }
            }
        }
    }
}

@Composable
private fun ItemMetricaCompacta(
    etiqueta: String,
    valor: String,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(valor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
        }
        Spacer(Modifier.height(2.dp))
        Text(etiqueta, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = PlagOutColors.TextSecondary)
    }
}

// ── Tarjeta: Plaga Predominante de la Campaña ─────────────────────────────────

@Composable
private fun TarjetaPlagaPredominanteCampaña(campaña: CampañaHistorica) {
    val lider = campaña.plagaPredominante

    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.5.dp, PlagOutColors.Leaf.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(PlagOutColors.Leaf.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.BugReport, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("PLAGA PREDOMINANTE DE LA CAMPAÑA", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.Leaf, letterSpacing = 0.5.sp)
                }

                Text(campaña.nombre, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextSecondary)
            }

            Spacer(Modifier.height(14.dp))

            if (lider != null) {
                Text(text = lider.plagaNombre, fontSize = 22.sp, fontWeight = FontWeight.Black, color = PlagOutColors.TextMain)
                if (!lider.plagaCientifico.isNullOrBlank()) {
                    Text(text = lider.plagaCientifico, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = PlagOutColors.TextSecondary)
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniIndicador(
                        titulo = "Incidencia",
                        valor = String.format(LocalConfiguration.current.locales[0], "%.1f%%", lider.porcentajeIncidencia),
                        detalle = "${lider.totalMonitoreos} ${if (lider.totalMonitoreos == 1) "monitoreo" else "monitoreos"}",
                        color = PlagOutColors.Forest,
                        modifier = Modifier.weight(1f)
                    )
                    val cultivosTexto = if (lider.cultivosAfectados.isNotEmpty()) {
                        lider.cultivosAfectados.joinToString(", ")
                    } else {
                        "—"
                    }
                    val detalleCultivos = when (lider.cultivosAfectados.size) {
                        0 -> "sin cultivo asociado"
                        1 -> "cultivo afectado"
                        else -> "${lider.cultivosAfectados.size} cultivos afectados"
                    }
                    MiniIndicador(
                        titulo = if (lider.cultivosAfectados.size <= 1) "Cultivo que afecta" else "Cultivos que afecta",
                        valor = cultivosTexto,
                        detalle = detalleCultivos,
                        color = PlagOutColors.Leaf,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text("No se registraron plagas en esta campaña.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun MiniIndicador(titulo: String, valor: String, detalle: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(titulo.uppercase(LocalConfiguration.current.locales[0]), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.4.sp)
            Spacer(Modifier.height(4.dp))
            Text(valor, fontSize = 16.sp, fontWeight = FontWeight.Black, color = PlagOutColors.TextMain)
            Spacer(Modifier.height(2.dp))
            Text(detalle, fontSize = 10.sp, color = PlagOutColors.TextSecondary, maxLines = 1)
        }
    }
}

// ── Tarjeta: Monitoreos por Cultivo y Plaga en la Campaña ───────────────────

@Composable
private fun TarjetaMonitoreosPorCultivoCampaña(campaña: CampañaHistorica) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Grass, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Monitoreos por Cultivo y Plaga", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Total de monitoreos registrados en la campaña (activos y finalizados)", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (campaña.cultivos.isEmpty()) {
                Text("No hay cultivos con monitoreos en esta campaña.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
            } else {
                campaña.cultivos.forEachIndexed { index, cultivo ->
                    BloqueCultivoCampaña(cultivo = cultivo)
                    if (index < campaña.cultivos.size - 1) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f))
                        Spacer(Modifier.height(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BloqueCultivoCampaña(cultivo: ResumenCultivoCampaña) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(cultivo.cultivoNombre, fontSize = 15.sp, fontWeight = FontWeight.Black, color = PlagOutColors.TextMain)
                if (!cultivo.cultivoCientifico.isNullOrBlank()) {
                    Text(cultivo.cultivoCientifico, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = PlagOutColors.TextSecondary)
                }
            }
            val textoMonitoreos = "${cultivo.totalMonitoreos} ${if (cultivo.totalMonitoreos == 1) "monitoreo" else "monitoreos"}"
            val textoDetalle = if (cultivo.totalHectareas > 0f) {
                "$textoMonitoreos · ${cultivo.totalHectareas.toInt()} ha"
            } else {
                textoMonitoreos
            }
            Text(
                textoDetalle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.Forest
            )
        }

        Spacer(Modifier.height(8.dp))

        cultivo.plagas.forEach { plaga ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(plaga.plagaNombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                        Text(
                            "${plaga.totalMonitoreos} ${if (plaga.totalMonitoreos == 1) "monitoreo" else "monitoreos"} (${plaga.porcentaje.toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlagOutColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(PlagOutColors.CreamDeep)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((plaga.porcentaje / 100f).coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(PlagOutColors.Leaf)
                        )
                    }
                }
            }
        }
    }
}

// ── Tarjeta: Prevalencia y Ranking de Plagas ──────────────────────────────────

@Composable
private fun TarjetaPrevalenciaPlagas(campaña: CampañaHistorica) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Insights, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Prevalencia de Plagas en la Campaña", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("${campaña.cantidadPlagasDistintas} plagas monitoreadas • ${campaña.totalMonitoreos} seguimientos", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            campaña.plagas.forEachIndexed { index, plaga ->
                FilaPlagaPrevalencia(posicion = index + 1, plaga = plaga)
                if (index < campaña.plagas.size - 1) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f))
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun FilaPlagaPrevalencia(posicion: Int, plaga: ResumenPlagaCampaña) {
    var iniciado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }

    val progresoAnimado by animateFloatAsState(
        targetValue = if (iniciado) (plaga.porcentajeIncidencia / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progresoPlaga"
    )

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    color = if (posicion == 1) PlagOutColors.Leaf else PlagOutColors.CreamDeep,
                    shape = CircleShape,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$posicion", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (posicion == 1) PlagOutColors.TextOnDark else PlagOutColors.TextMain)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(plaga.plagaNombre, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    val subtitulo = buildList {
                        if (!plaga.plagaCientifico.isNullOrBlank()) add(plaga.plagaCientifico)
                        if (plaga.cultivosAfectados.isNotEmpty()) add("Afecta: ${plaga.cultivosAfectados.joinToString(", ")}")
                    }.joinToString(" • ")
                    if (subtitulo.isNotBlank()) {
                        Text(subtitulo, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = PlagOutColors.TextSecondary)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(LocalConfiguration.current.locales[0], "%.1f%%", plaga.porcentajeIncidencia), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.Forest)
                Text("${plaga.totalMonitoreos} ${if (plaga.totalMonitoreos == 1) "monitoreo" else "monitoreos"}", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
            }
        }

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PlagOutColors.CreamDeep)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progresoAnimado)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (posicion == 1) PlagOutColors.Leaf else PlagOutColors.BarkLight)
            )
        }
    }
}
