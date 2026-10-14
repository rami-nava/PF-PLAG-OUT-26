package com.example.plag_out

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ShieldAlert
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
            titulo = "Métricas y Campañas",
            subtitulo = if (state.campañas.isNotEmpty()) {
                "${state.campañas.size} ${if (state.campañas.size == 1) "campaña histórica registrada" else "campañas históricas registradas"}"
            } else {
                "Histórico de plagas e índices de riesgo por campaña agrícola"
            }
        )

        BannerErrorAdmin(
            mensaje = state.error,
            onCerrar = viewModel::descartarError,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        when {
            state.isLoading -> SkeletonCargando(alturaTarjeta = 160.dp, cantidad = 3)
            state.campañas.isEmpty() -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refrescar,
                modifier = Modifier.fillMaxSize()
            ) {
                EstadoVacioFlotante(
                    icono = Icons.Outlined.Insights,
                    titulo = "Sin monitoreos históricos",
                    subtitulo = "A medida que los productores inicien monitoreos en sus lotes, se agruparán automáticamente por campaña agrícola y se calcularán sus plagas predominantes."
                )
            }
            else -> {
                val campañas = state.campañas
                val seleccionada = state.campañaSeleccionada ?: campañas.first()
                val indiceSeleccionado = campañas.indexOfFirst { it.nombre == seleccionada.nombre }.coerceAtLeast(0)

                // Selector de campañas históricas
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
                    seleccionado = indiceSeleccionado,
                    onSeleccion = { nuevoIndice ->
                        if (nuevoIndice in campañas.indices) {
                            viewModel.seleccionarCampaña(campañas[nuevoIndice].nombre)
                        }
                    }
                )

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refrescar,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("listaMetricasCampañas")
                    ) {
                        // 1. Resumen global acumulado
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

                        // 2. Tarjeta destacada de la plaga predominante en la campaña activa
                        item {
                            StaggeredAppear(index = 1) {
                                TarjetaPlagaPredominanteCampaña(
                                    campaña = seleccionada
                                )
                            }
                        }

                        // 3. Distribución y prevalencia de plagas en la campaña
                        item {
                            StaggeredAppear(index = 2) {
                                TarjetaPrevalenciaPlagas(
                                    campaña = seleccionada
                                )
                            }
                        }

                        // 4. Perfil y distribución de Riesgo Ambiental (IRA)
                        item {
                            StaggeredAppear(index = 3) {
                                TarjetaPerfilRiesgoAmbiental(
                                    campaña = seleccionada
                                )
                            }
                        }
                    }
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
                        Icon(
                            Icons.Outlined.Assessment,
                            contentDescription = null,
                            tint = PlagOutColors.Forest,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "HISTÓRICO CONSOLIDADO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.Leaf,
                        letterSpacing = 0.6.sp
                    )
                    Text(
                        "Resumen de todas las campañas registradas",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ItemMetricaCompacta(
                    etiqueta = "Monitoreos",
                    valor = "${contadorAnimado(totalMonitoreos)}",
                    icono = Icons.Outlined.BugReport,
                    modifier = Modifier.weight(1f)
                )
                ItemMetricaCompacta(
                    etiqueta = "Hectáreas",
                    valor = if (totalHectareas > 0f) String.format(Locale.getDefault(), "%.1f ha", totalHectareas) else "0 ha",
                    icono = Icons.Outlined.Agriculture,
                    modifier = Modifier.weight(1f)
                )
                ItemMetricaCompacta(
                    etiqueta = "Campañas",
                    valor = "${contadorAnimado(totalCampañas)}",
                    icono = Icons.Outlined.CalendarMonth,
                    modifier = Modifier.weight(1f)
                )
            }

            if (!plagaLider.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.6f))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Mayor incidencia histórica:",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        plagaLider,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
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
            Text(
                valor,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            etiqueta,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PlagOutColors.TextSecondary
        )
    }
}

// ── Tarjeta: Plaga Predominante de la Campaña ─────────────────────────────────

@Composable
private fun TarjetaPlagaPredominanteCampaña(
    campaña: CampañaHistorica
) {
    val lider = campaña.plagaPredominante

    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.5.dp, PlagOutColors.Leaf.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            // Header del bloque destacado
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
                    Icon(
                        Icons.Outlined.BugReport,
                        contentDescription = null,
                        tint = PlagOutColors.Leaf,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "PLAGA PREDOMINANTE DE LA CAMPAÑA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlagOutColors.Leaf,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    "Campaña ${campaña.nombre}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextSecondary
                )
            }

            Spacer(Modifier.height(14.dp))

            if (lider != null) {
                // Título con nombre común y científico
                Text(
                    text = lider.plagaNombre,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = PlagOutColors.TextMain
                )
                if (!lider.plagaCientifico.isNullOrBlank()) {
                    Text(
                        text = lider.plagaCientifico,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic,
                        color = PlagOutColors.TextSecondary
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Métricas clave del líder de la campaña
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MiniIndicador(
                        titulo = "Incidencia",
                        valor = String.format(Locale.getDefault(), "%.1f%%", lider.porcentajeIncidencia),
                        detalle = "del total monitoreado",
                        color = PlagOutColors.Forest,
                        modifier = Modifier.weight(1f)
                    )
                    MiniIndicador(
                        titulo = "Casos en Alerta",
                        valor = "${lider.enRiesgoAlto}",
                        detalle = "${lider.porcentajeRiesgoAlto.toInt()}% con riesgo alto",
                        color = if (lider.enRiesgoAlto > 0) PlagOutColors.RiskDanger else PlagOutColors.RiskOk,
                        modifier = Modifier.weight(1f)
                    )
                    val estiloSeveridad = estiloDeNivel(lider.nivelRiesgoPromedio.roundToInt())
                    MiniIndicador(
                        titulo = "Severidad Media",
                        valor = estiloSeveridad.etiqueta,
                        detalle = "índice ponderado",
                        color = estiloSeveridad.color,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Text(
                    "No se registraron plagas en esta campaña.",
                    fontSize = 13.sp,
                    color = PlagOutColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MiniIndicador(
    titulo: String,
    valor: String,
    detalle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                titulo.uppercase(Locale.getDefault()),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                letterSpacing = 0.4.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                valor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = PlagOutColors.TextMain
            )
            Spacer(Modifier.height(2.dp))
            Text(
                detalle,
                fontSize = 10.sp,
                color = PlagOutColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}

// ── Tarjeta: Prevalencia y Ranking de Plagas ──────────────────────────────────

@Composable
private fun TarjetaPrevalenciaPlagas(
    campaña: CampañaHistorica
) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Insights,
                    contentDescription = null,
                    tint = PlagOutColors.Forest,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Prevalencia de Plagas en la Campaña",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "${campaña.cantidadPlagasDistintas} plagas monitoreadas • ${campaña.totalMonitoreos} seguimientos",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            campaña.plagas.forEachIndexed { index, plaga ->
                FilaPlagaPrevalencia(
                    posicion = index + 1,
                    plaga = plaga
                )
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
private fun FilaPlagaPrevalencia(
    posicion: Int,
    plaga: ResumenPlagaCampaña
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (posicion == 1) PlagOutColors.Leaf else PlagOutColors.CreamDeep,
                    shape = CircleShape,
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "$posicion",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (posicion == 1) PlagOutColors.TextOnDark else PlagOutColors.TextMain
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        plaga.plagaNombre,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    if (!plaga.plagaCientifico.isNullOrBlank()) {
                        Text(
                            plaga.plagaCientifico,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            color = PlagOutColors.TextSecondary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format(Locale.getDefault(), "%.1f%%", plaga.porcentajeIncidencia),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlagOutColors.Forest
                )
                Text(
                    "${plaga.totalMonitoreos} ${if (plaga.totalMonitoreos == 1) "lote" else "lotes"}",
                    fontSize = 11.sp,
                    color = PlagOutColors.TextSecondary
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Barra de progreso de incidencia relativa
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
                Icon(
                    Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = PlagOutColors.RiskDanger,
                    modifier = Modifier.size(11.dp)
                )
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
private fun TarjetaPerfilRiesgoAmbiental(
    campaña: CampañaHistorica
) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ShieldAlert,
                    contentDescription = null,
                    tint = PlagOutColors.Terracotta,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Perfil de Riesgo Ambiental (IRA)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "Distribución de severidad y presión de plagas en la campaña",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Barra continua segmentada de riesgo
            BarraSegmentadaDistribucionRiesgo(
                porcBajo = campaña.porcentajeRiesgoBajo,
                porcModerado = campaña.porcentajeRiesgoModerado,
                porcAlto = campaña.porcentajeRiesgoAlto
            )

            Spacer(Modifier.height(14.dp))

            // Leyenda explicativa con conteo y porcentajes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ItemLeyendaRiesgo(
                    color = PlagOutColors.RiskOk,
                    titulo = "Bajo (<30%)",
                    cantidad = campaña.riesgoBajo,
                    porcentaje = campaña.porcentajeRiesgoBajo,
                    modifier = Modifier.weight(1f)
                )
                ItemLeyendaRiesgo(
                    color = PlagOutColors.RiskWarn,
                    titulo = "Moderado (30-70%)",
                    cantidad = campaña.riesgoModerado,
                    porcentaje = campaña.porcentajeRiesgoModerado,
                    modifier = Modifier.weight(1f)
                )
                ItemLeyendaRiesgo(
                    color = PlagOutColors.RiskDanger,
                    titulo = "Alto (≥70%)",
                    cantidad = campaña.riesgoAlto,
                    porcentaje = campaña.porcentajeRiesgoAlto,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Pie de página aclaratorio
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PlagOutColors.Cream.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = PlagOutColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
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
private fun BarraSegmentadaDistribucionRiesgo(
    porcBajo: Float,
    porcModerado: Float,
    porcAlto: Float
) {
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
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(animBajo.coerceAtLeast(0.001f))
                    .background(PlagOutColors.RiskOk)
            )
        }
        if (animModerado > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(animModerado.coerceAtLeast(0.001f))
                    .background(PlagOutColors.RiskWarn)
            )
        }
        if (animAlto > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(animAlto.coerceAtLeast(0.001f))
                    .background(PlagOutColors.RiskDanger)
            )
        }
    }
}

@Composable
private fun ItemLeyendaRiesgo(
    color: Color,
    titulo: String,
    cantidad: Int,
    porcentaje: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                titulo,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlagOutColors.TextMain
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            "$cantidad (${porcentaje.toInt()}%)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(start = 13.dp)
        )
    }
}
