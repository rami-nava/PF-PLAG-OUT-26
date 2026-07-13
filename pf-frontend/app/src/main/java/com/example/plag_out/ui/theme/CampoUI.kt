package com.example.plag_out.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Vocabulario visual compartido por los dashboards principales (Monitoreos,
 * Terrenos, Plantaciones): estilos de nivel de alerta, anillos de progreso,
 * chips de filtro y los estados de carga/vacío/sin-resultados. Centralizar
 * estas piezas evita que cada pantalla reinvente el mismo anillo o chip con
 * ligeras variaciones — un solo lugar define cómo se ve "crítico" o "cargando".
 */

// ── Nivel de alerta ──────────────────────────────────────────────────────────

data class NivelEstilo(
    val color: Color,
    val colorSobreOscuro: Color,
    val etiqueta: String,
    val icono: ImageVector
)

/** nivel < 0 se interpreta como "sin datos" (p.ej. un terreno sin monitoreos). */
fun estiloDeNivel(nivel: Int): NivelEstilo = when {
    nivel < 0 -> NivelEstilo(PlagOutColors.RiskUnknown, Color(0xFFCFC5AC), "Sin datos", Icons.Filled.HelpOutline)
    nivel == 0 -> NivelEstilo(PlagOutColors.RiskOk, Color(0xFF8ACD86), "Saludable", Icons.Filled.CheckCircle)
    nivel == 1 -> NivelEstilo(PlagOutColors.RiskWarn, PlagOutColors.Sun, "Atención", Icons.Filled.WarningAmber)
    else -> NivelEstilo(PlagOutColors.RiskDanger, Color(0xFFE4795C), "Crítico", Icons.Filled.ErrorOutline)
}

/** Chip de estado: ícono + etiqueta; el ícono pulsa cuando `pulsante` es true. */
@Composable
fun SelloDeNivel(estilo: NivelEstilo, pulsante: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(estilo.color.copy(alpha = 0.12f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val alfaIcono = if (pulsante) {
            val pulso = rememberInfiniteTransition(label = "pulsoCritico")
            val alfa by pulso.animateFloat(
                initialValue = 1f,
                targetValue = 0.35f,
                animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                label = "alfaPulso"
            )
            alfa
        } else 1f
        Icon(
            estilo.icono,
            contentDescription = null,
            tint = estilo.color,
            modifier = Modifier.size(13.dp).alpha(alfaIcono)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            estilo.etiqueta,
            color = estilo.color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Anillos ──────────────────────────────────────────────────────────────────

/** Anillo segmentado (tipo donut) con barrido animado; el centro es libre. */
@Composable
fun AnilloSegmentado(
    segmentos: List<Pair<Int, Color>>,
    total: Int,
    modifier: Modifier = Modifier,
    grosor: Dp = 11.dp,
    contenidoCentral: @Composable () -> Unit
) {
    var iniciado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }
    val avance by animateFloatAsState(
        targetValue = if (iniciado) 1f else 0f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "anilloSegmentado"
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val grosorPx = grosor.toPx()
            val arcSize = Size(size.width - grosorPx, size.height - grosorPx)
            val topLeft = Offset(grosorPx / 2, grosorPx / 2)

            drawArc(
                color = Color.White.copy(alpha = 0.14f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = grosorPx)
            )

            if (total > 0) {
                val activos = segmentos.filter { it.first > 0 }
                val separacion = if (activos.size > 1) 10f else 0f
                val disponible = 360f - separacion * activos.size
                var inicio = -90f
                activos.forEach { (cantidad, color) ->
                    val barrido = disponible * cantidad / total
                    drawArc(
                        color = color,
                        startAngle = inicio,
                        sweepAngle = barrido * avance,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = grosorPx,
                            cap = if (activos.size == 1) StrokeCap.Round else StrokeCap.Butt
                        )
                    )
                    inicio += barrido + separacion
                }
            }
        }
        contenidoCentral()
    }
}

/** Anillo de progreso simple (0-100%) con barrido animado y porcentaje al centro. */
@Composable
fun AnilloProgreso(progreso: Float, color: Color, tamano: Dp, grosor: Dp = 7.dp) {
    var iniciado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }
    val avance by animateFloatAsState(
        targetValue = if (iniciado) (progreso / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "anilloProgreso"
    )
    Box(Modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val grosorPx = grosor.toPx()
            val arcSize = Size(size.width - grosorPx, size.height - grosorPx)
            val topLeft = Offset(grosorPx / 2, grosorPx / 2)
            drawArc(
                color = PlagOutColors.CreamDeep,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = grosorPx)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * avance,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = grosorPx, cap = StrokeCap.Round)
            )
        }
        Text(
            "${progreso.toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlagOutColors.TextMain
        )
    }
}

/** Número que "cuenta" desde 0 hasta su valor al aparecer en pantalla. */
@Composable
fun contadorAnimado(objetivo: Int): Int {
    var iniciado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }
    val valor by animateIntAsState(
        targetValue = if (iniciado) objetivo else 0,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "contadorAnimado"
    )
    return valor
}

// ── Atomos de tarjeta ────────────────────────────────────────────────────────

@Composable
fun SeparadorVertical(altura: Dp = 26.dp) {
    Box(
        Modifier
            .width(1.dp)
            .height(altura)
            .background(PlagOutColors.Divider)
    )
}

@Composable
fun EstadisticaCompacta(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
        Text(
            etiqueta,
            fontSize = 10.sp,
            color = PlagOutColors.TextSecondary,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun EtiquetaInfo(icono: ImageVector, texto: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(color.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(texto, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Filtros ──────────────────────────────────────────────────────────────────

data class OpcionFiltro(
    val id: Int,
    val etiqueta: String,
    val cantidad: Int,
    val icono: ImageVector? = null,
    val colorIcono: Color = PlagOutColors.TextMain
)

@Composable
fun FiltroChipsRow(
    opciones: List<OpcionFiltro>,
    seleccionado: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            val activo = seleccionado == opcion.id
            val fondo by animateColorAsState(
                targetValue = if (activo) PlagOutColors.Forest else PlagOutColors.Surface,
                animationSpec = tween(220),
                label = "chipFondo"
            )
            val tinta by animateColorAsState(
                targetValue = if (activo) PlagOutColors.TextOnDark else PlagOutColors.TextMain,
                animationSpec = tween(220),
                label = "chipTinta"
            )
            Surface(
                onClick = { onSeleccion(opcion.id) },
                shape = CircleShape,
                color = fondo,
                border = if (activo) null else BorderStroke(1.dp, PlagOutColors.Divider),
                shadowElevation = if (activo) 2.dp else 0.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (opcion.icono != null) {
                        Icon(
                            opcion.icono,
                            contentDescription = null,
                            tint = if (activo) PlagOutColors.TextOnDark else opcion.colorIcono,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(opcion.etiqueta, color = tinta, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${opcion.cantidad}",
                        color = tinta.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ── Estados de carga / vacío / sin resultados ───────────────────────────────

@Composable
fun SkeletonCargando(alturaTarjeta: Dp = 180.dp, cantidad: Int = 3) {
    val brillo = rememberInfiniteTransition(label = "shimmer")
    val alfa by brillo.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "shimmerAlfa"
    )
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        repeat(cantidad) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(alturaTarjeta)
                    .alpha(alfa)
                    .background(PlagOutColors.CreamDeep, RoundedCornerShape(22.dp))
            )
        }
    }
}

@Composable
fun EstadoVacioFlotante(icono: ImageVector, titulo: String, subtitulo: String) {
    val flote = rememberInfiniteTransition(label = "flote")
    val desplazamientoY by flote.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floteY"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = PlagOutColors.Leaf.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier
                .size(120.dp)
                .offset(y = desplazamientoY.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(54.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
        Text(
            subtitulo,
            textAlign = TextAlign.Center,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun EstadoSinResultados(titulo: String = "Sin resultados", subtitulo: String = "No hay elementos en este estado.") {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = PlagOutColors.RiskUnknown,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
        Text(
            subtitulo,
            textAlign = TextAlign.Center,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun CargandoCentrado() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PlagOutColors.Forest)
    }
}
