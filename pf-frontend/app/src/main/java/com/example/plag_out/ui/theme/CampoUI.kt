package com.example.plag_out.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.platform.testTag
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    val icono: ImageVector,
    val descripcion: String = "",
    val recomendacion: String = ""
)

fun estiloDeNivel(nivel: Int): NivelEstilo = when {
    nivel < 0 -> NivelEstilo(
        PlagOutColors.RiskUnknown,
        Color(0xFFCFC5AC),
        "Sin datos",
        Icons.AutoMirrored.Filled.HelpOutline,
        "Todavía no hay monitoreos con datos suficientes para calcular el IRA."
    )
    nivel == 0 -> NivelEstilo(
        PlagOutColors.RiskOk,
        Color(0xFF8ACD86),
        "Bajo",
        Icons.Filled.CheckCircle,
        "Las condiciones ambientales no favorecieron a la plaga: se espera una densidad poblacional baja.",
        "Seguí el monitoreo con la frecuencia habitual."
    )
    nivel == 1 -> NivelEstilo(
        PlagOutColors.RiskWarn,
        PlagOutColors.Sun,
        "Moderado",
        Icons.Filled.WarningAmber,
        "Hubo períodos con condiciones favorables: la densidad poblacional esperada es intermedia.",
        "Revisá la plantación más seguido y prepará el control por las dudas."
    )
    else -> NivelEstilo(
        PlagOutColors.RiskDanger,
        Color(0xFFE4795C),
        "Alto",
        Icons.Filled.ErrorOutline,
        "Las condiciones fueron muy favorables para la plaga: se espera una densidad poblacional alta.",
        "Inspeccioná la plantación cuanto antes y evaluá aplicar control."
    )
}

/** Estilo fijo para un monitoreo finalizado: prevalece por sobre su nivel de alerta. */
fun estiloFinalizado(): NivelEstilo =
    NivelEstilo(
        PlagOutColors.Bark,
        PlagOutColors.BarkLight,
        "Finalizado",
        Icons.Filled.Flag,
        "El monitoreo se cerró: ya no se calcula el IRA ni se envían alertas."
    )

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

/**
 * Versión ampliada de [AnilloProgreso] para la pantalla de detalle de monitoreo: más grande,
 * con una marca de umbral en el arco y un color que anima entre saludable/atención/crítico según
 * el nivel de alerta (en vez de recibir un color fijo).
 */
@Composable
fun AnilloRiesgoGrande(
    progreso: Float,
    nivelAlerta: Int,
    umbralRiesgo: Int?,
    modifier: Modifier = Modifier,
    tamano: Dp = 220.dp,
    grosor: Dp = 18.dp
) {
    var iniciado by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { iniciado = true }
    val avance by animateFloatAsState(
        targetValue = if (iniciado) (progreso / 100f).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "anilloRiesgoGrandeAvance"
    )
    val color by animateColorAsState(
        targetValue = estiloDeNivel(nivelAlerta).color,
        animationSpec = tween(500),
        label = "anilloRiesgoGrandeColor"
    )
    val contador = contadorAnimado(progreso.toInt())

    val pulsante = nivelAlerta >= 2
    val alfaHalo = if (pulsante) {
        val pulso = rememberInfiniteTransition(label = "haloRiesgo")
        val alfa by pulso.animateFloat(
            initialValue = 0.18f,
            targetValue = 0.02f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "haloRiesgoAlfa"
        )
        alfa
    } else 0f

    Box(modifier.size(tamano), contentAlignment = Alignment.Center) {
        if (pulsante) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(color.copy(alpha = alfaHalo), CircleShape)
            )
        }
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

            if (umbralRiesgo != null) {
                val anguloUmbral = -90f + 360f * (umbralRiesgo / 100f).coerceIn(0f, 1f)
                val radianes = anguloUmbral * (PI.toFloat() / 180f)
                val radio = arcSize.width / 2f
                val centro = Offset(size.width / 2f, size.height / 2f)
                val mitadGrosor = grosorPx / 2f
                val margen = 4.dp.toPx()
                val inicio = Offset(
                    centro.x + (radio - mitadGrosor - margen) * cos(radianes),
                    centro.y + (radio - mitadGrosor - margen) * sin(radianes)
                )
                val fin = Offset(
                    centro.x + (radio + mitadGrosor + margen) * cos(radianes),
                    centro.y + (radio + mitadGrosor + margen) * sin(radianes)
                )
                drawLine(
                    color = PlagOutColors.TextMain,
                    start = inicio,
                    end = fin,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        val fontPrincipal = (tamano.value * 56f / 220f).coerceAtLeast(24f).sp
        val fontSecundaria = (tamano.value * 13f / 220f).coerceAtLeast(10f).sp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$contador%",
                fontSize = fontPrincipal,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )
            Text(
                "riesgo",
                fontSize = fontSecundaria,
                fontWeight = FontWeight.Medium,
                color = PlagOutColors.TextSecondary
            )
        }
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

@Composable
fun PanelFiltrosPlegable(
    expandido: Boolean,
    onToggleExpandido: () -> Unit,
    hayFiltroActivo: Boolean,
    etiquetaAbrir: String,
    resumen: String,
    onLimpiar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PlagOutColors.Cream)
    ) {
        Surface(
            onClick = onToggleExpandido,
            color = PlagOutColors.Cream,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btnToggleFiltros")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (hayFiltroActivo) PlagOutColors.Forest.copy(alpha = 0.15f) else PlagOutColors.Surface,
                            shape = CircleShape,
                            border = if (hayFiltroActivo) null else BorderStroke(1.dp, PlagOutColors.Divider),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Tune,
                                    contentDescription = etiquetaAbrir,
                                    tint = if (hayFiltroActivo) PlagOutColors.Forest else PlagOutColors.TextMain,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (expandido) "Ocultar filtros" else etiquetaAbrir,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = resumen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (hayFiltroActivo) PlagOutColors.Forest else PlagOutColors.TextSecondary,
                        modifier = Modifier.padding(start = 42.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hayFiltroActivo) {
                        Surface(
                            onClick = onLimpiar,
                            shape = CircleShape,
                            color = PlagOutColors.RiskDanger.copy(alpha = 0.1f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("btnLimpiarFiltros")
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
                        imageVector = if (expandido) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expandido) "Ocultar filtros" else "Mostrar filtros",
                        tint = PlagOutColors.TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = expandido,
            enter = expandVertically() + fadeIn(tween(220)),
            exit = shrinkVertically() + fadeOut(tween(180))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .testTag("panelFiltros"),
                content = contenido
            )
        }
    }
}

/** Título de una categoría dentro de [PanelFiltrosPlegable] (p.ej. "NIVEL DE SEVERIDAD"). */
@Composable
fun EncabezadoGrupoFiltro(icono: ImageVector, titulo: String) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icono,
            contentDescription = null,
            tint = PlagOutColors.TextSecondary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            titulo,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlagOutColors.TextSecondary,
            letterSpacing = 0.6.sp
        )
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
