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
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
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
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMetricasScreen(
    viewModel: AdminMetricasViewModel
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    Column(Modifier.fillMaxSize()) {
        EncabezadoSeccionAdmin(
            titulo = "Tableros de Control",
            subtitulo = when (state.pestañaActual) {
                PestañaDashboard.PRODUCTOR -> "Monitoreo operativo de lotes, cultivos y superficie en riesgo"
                PestañaDashboard.AGRONOMO -> "Ventana óptima de control, estadios biológicos (GDD) y eficacia"
                PestañaDashboard.HISTORICO -> "Evolución histórica y prevalencia de plagas por campaña agrícola"
            }
        )

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
                    subtitulo = "Cuando los productores den de alta lotes y monitoreos, los tableros analíticos se generarán automáticamente."
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
                            PestañaDashboard.AGRONOMO -> VistaDashboardAgronomo(state = state)
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
        // 1. Semáforo Donut de Superficie en Riesgo
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
                    Text("Semáforo de Superficie en Riesgo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Distribución de hectáreas según el peor nivel de alerta del lote", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
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
                        String.format(Locale.getDefault(), "%.0f ha", semaforo.totalHectareas),
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
                    etiqueta = "Bajo control",
                    hectareas = semaforo.hectareasVerde,
                    porcentaje = semaforo.porcentajeVerde,
                    lotes = semaforo.lotesVerde,
                    color = PlagOutColors.RiskOk,
                    modifier = Modifier.weight(1f)
                )
                ItemDesgloseSemaforo(
                    etiqueta = "Seguimiento",
                    hectareas = semaforo.hectareasAmarillo,
                    porcentaje = semaforo.porcentajeAmarillo,
                    lotes = semaforo.lotesAmarillo,
                    color = PlagOutColors.RiskWarn,
                    modifier = Modifier.weight(1f)
                )
                ItemDesgloseSemaforo(
                    etiqueta = "Alerta crítica",
                    hectareas = semaforo.hectareasRojo,
                    porcentaje = semaforo.porcentajeRojo,
                    lotes = semaforo.lotesRojo,
                    color = PlagOutColors.RiskDanger,
                    modifier = Modifier.weight(1f)
                )
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
                String.format(Locale.getDefault(), "%.1f ha", hectareas),
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
                    Text("Prioridad de Acción por Lote", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Ordenados por nivel de urgencia y daño fitosanitario", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (lotes.isEmpty()) {
                Text("No hay lotes con alertas activas.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
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
                            String.format(Locale.getDefault(), "· %.0f ha", lote.terrenoArea),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlagOutColors.TextSecondary
                        )
                    }
                }
                val infoCultivo = if (lote.diasDesdeSiembra != null) {
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

        Spacer(Modifier.height(4.dp))
        Text(
            lote.urgenciaAccion,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (lote.nivelAlerta >= 2) PlagOutColors.RiskDanger else PlagOutColors.TextSecondary
        )
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
                    Text("Prevalencia de plagas desde la fecha de siembra", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
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
                if (cultivo.diasDesdeSiembra != null) {
                    Text("Sembrado hace ${cultivo.diasDesdeSiembra} días", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
                }
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
                            "${plaga.totalLotes} lotes (${plaga.porcentaje.toInt()}%)",
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
                                .background(if (plaga.casosAlerta > 0) PlagOutColors.RiskDanger else PlagOutColors.Leaf)
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
private fun VistaDashboardAgronomo(state: AdminMetricasUIState) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Medidor de Ventana Óptima de Aplicación
        item {
            StaggeredAppear(index = 0) {
                TarjetaVentanaOptimaAplicacion(ventanas = state.ventanasControl)
            }
        }

        // 2. Eficacia de Tratamientos Fitosanitarios
        state.eficaciaTratamientos?.let { eficacia ->
            item {
                StaggeredAppear(index = 1) {
                    TarjetaEficaciaTratamientos(eficacia = eficacia)
                }
            }
        }

        // 3. Distribución de Riesgo Ambiental IRA
        state.campañaSeleccionada?.let { camp ->
            item {
                StaggeredAppear(index = 2) {
                    TarjetaPerfilRiesgoAmbiental(campaña = camp)
                }
            }
        }
    }
}

@Composable
private fun TarjetaVentanaOptimaAplicacion(ventanas: List<VentanaControlBiologico>) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Ventana Óptima de Aplicación", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Sincronización fenológica (GDD) y susceptibilidad biológica", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(14.dp))

            if (ventanas.isEmpty()) {
                Text("No hay monitoreos activos para evaluar estadios térmicos.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
            } else {
                ventanas.forEachIndexed { index, v ->
                    FilaVentanaBiologica(ventana = v)
                    if (index < ventanas.size - 1) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaVentanaBiologica(ventana: VentanaControlBiologico) {
    val (colorEstado, iconoEstado, etiquetaEstado) = when (ventana.estadoVentana) {
        EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA -> Triple(PlagOutColors.RiskOk, Icons.Outlined.CheckCircle, "VENTANA ÓPTIMA (L1 - L3)")
        EstadoVentanaControl.INCUBACION_ESPERA -> Triple(PlagOutColors.TextSecondary, Icons.Outlined.HourglassEmpty, "INCUBACIÓN (ESPERA)")
        EstadoVentanaControl.VENTANA_CERRADA_AVANZADO -> Triple(PlagOutColors.RiskDanger, Icons.Outlined.Warning, "VENTANA EN CIERRE (AVANZADA)")
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${ventana.plagaNombre} • ${ventana.cultivoNombre}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextMain
                )
                Text(ventana.terrenoNombre, fontSize = 12.sp, color = PlagOutColors.TextSecondary)
            }

            Surface(
                color = colorEstado.copy(alpha = 0.12f),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(iconoEstado, contentDescription = null, tint = colorEstado, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(etiquetaEstado, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = colorEstado)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Barra tricolor de progreso biológico
        BarraProgresoFasesBiologicas(progreso = ventana.progresoGdd)

        Spacer(Modifier.height(6.dp))

        Text(
            ventana.recomendacionTecnica,
            fontSize = 11.sp,
            color = PlagOutColors.TextSecondary,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun BarraProgresoFasesBiologicas(progreso: Float) {
    val fraccion = (progreso / 100f).coerceIn(0f, 1f)

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PlagOutColors.CreamDeep)
        ) {
            Row(Modifier.fillMaxSize()) {
                // Zona 1: Huevo (0-40%)
                Box(Modifier.fillMaxHeight().weight(0.40f).background(Color(0xFFD4C9B4)))
                // Zona 2: Óptima L1-L3 (40-85%)
                Box(Modifier.fillMaxHeight().weight(0.45f).background(PlagOutColors.LeafLight.copy(alpha = 0.6f)))
                // Zona 3: Adulto/Daño (85-100%)
                Box(Modifier.fillMaxHeight().weight(0.15f).background(PlagOutColors.Terracotta.copy(alpha = 0.5f)))
            }

            // Indicador de avance actual
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraccion)
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        Spacer(Modifier.height(3.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Huevo", fontSize = 9.sp, color = PlagOutColors.TextSecondary)
            Text("Ventana Óptima (L1-L3)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.Forest)
            Text("Adulto / Daño", fontSize = 9.sp, color = PlagOutColors.RiskDanger)
        }
    }
}

@Composable
private fun TarjetaEficaciaTratamientos(eficacia: EficaciaTratamientos) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Verified, contentDescription = null, tint = PlagOutColors.RiskOk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Eficacia de Tratamientos Aplicados", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Evaluación de supresión poblacional tras archivar ciclos", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        String.format(Locale.getDefault(), "%.0f%%", eficacia.tasaEficaciaPorcentaje),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = PlagOutColors.RiskOk
                    )
                    Text("Tasa de éxito de control", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextSecondary)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${eficacia.totalTratamientosArchivados} tratamientos evaluados",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "${eficacia.tratamientosExitosos} controlados sin reinfestación",
                        fontSize = 11.sp,
                        color = PlagOutColors.Forest
                    )
                }
            }
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

        // Plaga predominante en la campaña
        seleccionada?.let { camp ->
            item {
                StaggeredAppear(index = 1) {
                    TarjetaPlagaPredominanteCampaña(campaña = camp)
                }
            }

            item {
                StaggeredAppear(index = 2) {
                    TarjetaPrevalenciaPlagas(campaña = camp)
                }
            }

            item {
                StaggeredAppear(index = 3) {
                    TarjetaPerfilRiesgoAmbiental(campaña = camp)
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
                    valor = if (totalHectareas > 0f) String.format(Locale.getDefault(), "%.1f ha", totalHectareas) else "0 ha",
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

                Text("Campaña ${campaña.nombre}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextSecondary)
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
                    MiniIndicador(titulo = "Incidencia", valor = String.format(Locale.getDefault(), "%.1f%%", lider.porcentajeIncidencia), detalle = "del total monitoreado", color = PlagOutColors.Forest, modifier = Modifier.weight(1f))
                    MiniIndicador(titulo = "Casos en Alerta", valor = "${lider.enRiesgoAlto}", detalle = "${lider.porcentajeRiesgoAlto.toInt()}% con riesgo alto", color = if (lider.enRiesgoAlto > 0) PlagOutColors.RiskDanger else PlagOutColors.RiskOk, modifier = Modifier.weight(1f))
                    val estiloSeveridad = estiloDeNivel(lider.nivelRiesgoPromedio.roundToInt())
                    MiniIndicador(titulo = "Severidad Media", valor = estiloSeveridad.etiqueta, detalle = "índice ponderado", color = estiloSeveridad.color, modifier = Modifier.weight(1f))
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
            Text(titulo.uppercase(Locale.getDefault()), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.4.sp)
            Spacer(Modifier.height(4.dp))
            Text(valor, fontSize = 16.sp, fontWeight = FontWeight.Black, color = PlagOutColors.TextMain)
            Spacer(Modifier.height(2.dp))
            Text(detalle, fontSize = 10.sp, color = PlagOutColors.TextSecondary, maxLines = 1)
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
                    if (!plaga.plagaCientifico.isNullOrBlank()) {
                        Text(plaga.plagaCientifico, fontSize = 11.sp, fontStyle = FontStyle.Italic, color = PlagOutColors.TextSecondary)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(String.format(Locale.getDefault(), "%.1f%%", plaga.porcentajeIncidencia), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.Forest)
                Text("${plaga.totalMonitoreos} ${if (plaga.totalMonitoreos == 1) "lote" else "lotes"}", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
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

        if (plaga.enRiesgoAlto > 0) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(PlagOutColors.RiskDanger.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Outlined.Warning, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${plaga.enRiesgoAlto} ${if (plaga.enRiesgoAlto == 1) "caso alcanzó" else "casos alcanzaron"} nivel de alerta alta (${plaga.porcentajeRiesgoAlto.toInt()}%)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.RiskDanger
                )
            }
        }
    }
}

// ── Tarjeta: Perfil de Riesgo Ambiental (IRA) ─────────────────────────────────

@Composable
private fun TarjetaPerfilRiesgoAmbiental(campaña: CampañaHistorica) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = PlagOutColors.Terracotta, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Perfil de Riesgo Ambiental (IRA)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Distribución de severidad y presión de plagas en la campaña", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            BarraSegmentadaDistribucionRiesgo(
                porcBajo = campaña.porcentajeRiesgoBajo,
                porcModerado = campaña.porcentajeRiesgoModerado,
                porcAlto = campaña.porcentajeRiesgoAlto
            )

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ItemLeyendaRiesgo(color = PlagOutColors.RiskOk, titulo = "Bajo (<30%)", cantidad = campaña.riesgoBajo, porcentaje = campaña.porcentajeRiesgoBajo, modifier = Modifier.weight(1f))
                ItemLeyendaRiesgo(color = PlagOutColors.RiskWarn, titulo = "Moderado (30-70%)", cantidad = campaña.riesgoModerado, porcentaje = campaña.porcentajeRiesgoModerado, modifier = Modifier.weight(1f))
                ItemLeyendaRiesgo(color = PlagOutColors.RiskDanger, titulo = "Alto (≥70%)", cantidad = campaña.riesgoAlto, porcentaje = campaña.porcentajeRiesgoAlto, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PlagOutColors.Cream.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "El Índice de Riesgo Ambiental (IRA) pondera la acumulación térmica (GDD), el progreso fenológico del ciclo biológico y la proximidad a los umbrales de daño económico.",
                    fontSize = 11.sp,
                    color = PlagOutColors.TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun BarraSegmentadaDistribucionRiesgo(porcBajo: Float, porcModerado: Float, porcAlto: Float) {
    var iniciado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }

    val animBajo by animateFloatAsState(if (iniciado) porcBajo / 100f else 0f, tween(700), label = "animBajo")
    val animModerado by animateFloatAsState(if (iniciado) porcModerado / 100f else 0f, tween(700), label = "animModerado")
    val animAlto by animateFloatAsState(if (iniciado) porcAlto / 100f else 0f, tween(700), label = "animAlto")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(PlagOutColors.CreamDeep)
    ) {
        if (animBajo > 0f) {
            Box(Modifier.fillMaxHeight().weight(animBajo.coerceAtLeast(0.001f)).background(PlagOutColors.RiskOk))
        }
        if (animModerado > 0f) {
            Box(Modifier.fillMaxHeight().weight(animModerado.coerceAtLeast(0.001f)).background(PlagOutColors.RiskWarn))
        }
        if (animAlto > 0f) {
            Box(Modifier.fillMaxHeight().weight(animAlto.coerceAtLeast(0.001f)).background(PlagOutColors.RiskDanger))
        }
    }
}

@Composable
private fun ItemLeyendaRiesgo(color: Color, titulo: String, cantidad: Int, porcentaje: Float, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(5.dp))
            Text(titulo, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
        }
        Spacer(Modifier.height(2.dp))
        Text("$cantidad (${porcentaje.toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(start = 13.dp))
    }
}

// ── Previews para Android Studio (Diseño interactivo sin backend) ────────────

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
fun PreviewVistaDashboardProductor() {
    val semaforoEjemplo = SemaforoHectareas(
        hectareasVerde = 120f,
        hectareasAmarillo = 50f,
        hectareasRojo = 30f,
        totalHectareas = 200f,
        lotesVerde = 3,
        lotesAmarillo = 2,
        lotesRojo = 1,
        totalLotes = 6
    )

    val lotesCriticosEjemplo = listOf(
        LoteEnRiesgoCritico(
            monitoreoId = 1,
            terrenoId = 1,
            terrenoNombre = "Lote Norte",
            terrenoArea = 80f,
            cultivoNombre = "Maíz tardío",
            plagaNombre = "Oruga cogollera",
            plagaCientifico = "Spodoptera frugiperda",
            nivelAlerta = 2,
            progresoGdd = 88f,
            gddAcumulado = 265f,
            gddObjetivo = 300f,
            urgenciaAccion = "⚠️ Superó umbral - Aplicar / Revisar hoy",
            diasDesdeSiembra = 35
        ),
        LoteEnRiesgoCritico(
            monitoreoId = 2,
            terrenoId = 2,
            terrenoNombre = "Bajo Cañada",
            terrenoArea = 50f,
            cultivoNombre = "Soja 1ra",
            plagaNombre = "Barrenador del tallo",
            plagaCientifico = "Diatraea saccharalis",
            nivelAlerta = 1,
            progresoGdd = 55f,
            gddAcumulado = 165f,
            gddObjetivo = 300f,
            urgenciaAccion = "🌱 Ventana activa de desarrollo",
            diasDesdeSiembra = 22
        )
    )

    val plagasCultivoEjemplo = listOf(
        ResumenCultivoPlagas(
            cultivoNombre = "Maíz",
            cultivoCientifico = "Zea mays",
            totalLotes = 4,
            totalHectareas = 130f,
            plagas = listOf(
                ParPlagaFrecuencia("Oruga cogollera", "Spodoptera frugiperda", 3, 75f, 1),
                ParPlagaFrecuencia("Chicharrita del maíz", "Dalbulus maidis", 1, 25f, 0)
            ),
            plagaMasFrecuente = "Oruga cogollera",
            lotesEnRiesgoAlto = 1,
            diasDesdeSiembra = 35
        ),
        ResumenCultivoPlagas(
            cultivoNombre = "Soja",
            cultivoCientifico = "Glycine max",
            totalLotes = 2,
            totalHectareas = 70f,
            plagas = listOf(
                ParPlagaFrecuencia("Barrenador del tallo", "Diatraea saccharalis", 2, 100f, 0)
            ),
            plagaMasFrecuente = "Barrenador del tallo",
            lotesEnRiesgoAlto = 0,
            diasDesdeSiembra = 22
        )
    )

    val stateMock = AdminMetricasUIState(
        pestañaActual = PestañaDashboard.PRODUCTOR,
        semaforoHectareas = semaforoEjemplo,
        lotesCriticos = lotesCriticosEjemplo,
        plagasPorCultivo = plagasCultivoEjemplo
    )

    MaterialTheme {
        VistaDashboardProductor(state = stateMock)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390, heightDp = 800)
@Composable
fun PreviewVistaDashboardAgronomo() {
    val ventanasEjemplo = listOf(
        VentanaControlBiologico(
            monitoreoId = 1,
            terrenoNombre = "Lote Norte",
            cultivoNombre = "Maíz",
            plagaNombre = "Oruga cogollera",
            plagaCientifico = "Spodoptera frugiperda",
            progresoGdd = 65f,
            estadioEstimado = "Larva chica (L1 - L3)",
            enVentanaOptima = true,
            estadoVentana = EstadoVentanaControl.VENTANA_OPTIMA_ABIERTA,
            recomendacionTecnica = "Ventana óptima de control: máxima susceptibilidad a insecticidas y biológicos."
        ),
        VentanaControlBiologico(
            monitoreoId = 2,
            terrenoNombre = "Lote Sur",
            cultivoNombre = "Soja",
            plagaNombre = "Barrenador",
            plagaCientifico = null,
            progresoGdd = 25f,
            estadioEstimado = "Incubación (Huevo)",
            enVentanaOptima = false,
            estadoVentana = EstadoVentanaControl.INCUBACION_ESPERA,
            recomendacionTecnica = "No pulverizar aún: plaga en fase de huevo o no susceptible."
        )
    )

    val eficaciaMock = EficaciaTratamientos(
        totalTratamientosArchivados = 8,
        tratamientosExitosos = 7,
        tasaEficaciaPorcentaje = 87.5f,
        monitoreosConRebrote = 1
    )

    val campañaMock = CampañaHistorica(
        nombre = "2025/2026",
        añoInicio = 2025,
        totalMonitoreos = 10,
        hectareasMonitoreadas = 200f,
        plagaPredominante = null,
        plagas = emptyList(),
        riesgoBajo = 6,
        riesgoModerado = 3,
        riesgoAlto = 1
    )

    val stateMock = AdminMetricasUIState(
        pestañaActual = PestañaDashboard.AGRONOMO,
        ventanasControl = ventanasEjemplo,
        eficaciaTratamientos = eficaciaMock,
        campañaSeleccionada = campañaMock
    )

    MaterialTheme {
        VistaDashboardAgronomo(state = stateMock)
    }
}

