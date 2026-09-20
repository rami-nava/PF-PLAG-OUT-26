package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.AnilloProgreso
import com.example.plag_out.ui.theme.EstadisticaCompacta
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.SeparadorVertical
import com.example.plag_out.ui.theme.estiloDeNivel
import com.example.plag_out.ui.theme.rememberPressScale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

private const val ESTADO_CICLO_ACTIVO = "activo"

// ── Dominio ─────────────────────────────────────────────────────────────────

fun ciclosActivos(monitoreo: MonitoreoResponse): List<GddCicloResponse> =
    monitoreo.ciclos.orEmpty().filter { it.estado == ESTADO_CICLO_ACTIVO }

fun nivelAlertaDeCiclos(monitoreo: MonitoreoResponse): Int? =
    ciclosActivos(monitoreo).maxOfOrNull { it.nivel_alerta }

fun nivelAlertaEfectivo(monitoreo: MonitoreoResponse): Int = nivelAlertaDeCiclos(monitoreo) ?: -1

fun cicloEnAlerta(ciclo: GddCicloResponse, umbralRiesgo: Int?): Boolean =
    ciclo.progreso >= (umbralRiesgo ?: 100).toFloat()

fun ciclosEnAlerta(monitoreo: MonitoreoResponse): Int =
    ciclosActivos(monitoreo).count { cicloEnAlerta(it, monitoreo.umbral_riesgo) }

fun objetivoDelCiclo(ciclo: GddCicloResponse, objetivoMonitoreo: Float?): Float? = when {
    ciclo.gdd_eclosion > 0f -> ciclo.gdd_eclosion
    objetivoMonitoreo != null && objetivoMonitoreo > 0f -> objetivoMonitoreo
    else -> null
}

@RequiresApi(Build.VERSION_CODES.O)
fun fechaDeCiclo(valor: String?): LocalDate? =
    valor?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

@RequiresApi(Build.VERSION_CODES.O)
private fun ritmoDiarioDelCiclo(ciclo: GddCicloResponse): Float {
    val biofix = fechaDeCiclo(ciclo.fecha_biofix) ?: return ciclo.gdd_diario
    val actualizacion = fechaDeCiclo(ciclo.fecha_actualizacion) ?: return ciclo.gdd_diario
    val dias = ChronoUnit.DAYS.between(biofix, actualizacion)
    return if (dias > 0) ciclo.gdd_acumulado / dias else ciclo.gdd_diario
}

@RequiresApi(Build.VERSION_CODES.O)
fun diasEstimadosDelCiclo(ciclo: GddCicloResponse, objetivoMonitoreo: Float?): Int? {
    val objetivo = objetivoDelCiclo(ciclo, objetivoMonitoreo) ?: return null
    val restante = objetivo - ciclo.gdd_acumulado
    if (restante <= 0f || ciclo.progreso >= 100f) return null
    val ritmo = ritmoDiarioDelCiclo(ciclo)
    return if (ritmo > 0f) ceil(restante / ritmo).toInt() else null
}

fun numerosDeCiclo(ciclos: List<GddCicloResponse>): Map<Int, Int> =
    ciclos.sortedWith(compareBy({ it.fecha_biofix }, { it.id }))
        .withIndex()
        .associate { (indice, ciclo) -> ciclo.id to indice + 1 }

fun ordenarCiclosPorProgreso(ciclos: List<GddCicloResponse>): List<GddCicloResponse> =
    ciclos.sortedWith(compareByDescending<GddCicloResponse> { it.progreso }.thenBy { it.id })

@RequiresApi(Build.VERSION_CODES.O)
private fun formatearFechaCiclo(valor: String?): String =
    fechaDeCiclo(valor)?.format(DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es")))
        ?: (valor ?: "—")

// ── Tarjeta de ciclo ────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CicloCard(
    ciclo: GddCicloResponse,
    modifier: Modifier = Modifier,
    numero: Int? = null,
    objetivoMonitoreo: Float? = null,
    umbralRiesgo: Int? = null,
    onClick: (() -> Unit)? = null
) {
    val activo = ciclo.estado == ESTADO_CICLO_ACTIVO
    val estilo = estiloDeNivel(if (activo) ciclo.nivel_alerta else -1)
    val enAlerta = activo && cicloEnAlerta(ciclo, umbralRiesgo)
    val objetivo = objetivoDelCiclo(ciclo, objetivoMonitoreo)
    val dias = diasEstimadosDelCiclo(ciclo, objetivoMonitoreo)

    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)

    val contenido: @Composable () -> Unit = {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(estilo.color)
            )
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SelloDeNivel(estilo, pulsante = activo && ciclo.nivel_alerta >= 2)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (numero != null) "Ciclo $numero" else "Ciclo",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain
                        )
                        Text(
                            "Biofix ${formatearFechaCiclo(ciclo.fecha_biofix)}" +
                                if (ciclo.estadio_biologico.isNotBlank()) " · ${ciclo.estadio_biologico}" else "",
                            fontSize = 12.sp,
                            color = PlagOutColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    AnilloProgreso(ciclo.progreso, estilo.color, tamano = 62.dp, grosor = 6.dp)
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EstadisticaCompacta("GDD actuales", "${ciclo.gdd_acumulado.toInt()}", Modifier.weight(1f))
                    SeparadorVertical()
                    EstadisticaCompacta(
                        "Objetivo",
                        objetivo?.let { "${it.toInt()}" } ?: "—",
                        Modifier.weight(1f)
                    )
                    SeparadorVertical()
                    EstadisticaCompacta(
                        "Al objetivo",
                        when {
                            ciclo.progreso >= 100f -> "Alcanzado"
                            dias != null -> "≈ $dias d"
                            else -> "—"
                        },
                        Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        enAlerta -> EtiquetaInfo(Icons.Filled.Flag, "Superó el umbral", PlagOutColors.RiskDanger)
                        ciclo.dias_pendientes > 0 -> EtiquetaInfo(
                            Icons.Outlined.HourglassEmpty,
                            "Cálculo pendiente · ${ciclo.dias_pendientes} d",
                            PlagOutColors.RiskWarn
                        )
                        !activo -> EtiquetaInfo(Icons.Filled.Flag, ciclo.estado, PlagOutColors.RiskUnknown)
                        else -> EtiquetaInfo(Icons.Outlined.Schedule, "En seguimiento", PlagOutColors.Forest)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Act. ${formatearFechaCiclo(ciclo.fecha_actualizacion)}",
                        fontSize = 11.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }
            }
        }
    }

    val forma = RoundedCornerShape(20.dp)
    val base = modifier
        .fillMaxWidth()
        .testTag("cicloCard_${ciclo.id}")
    if (onClick != null) {
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            color = PlagOutColors.Surface,
            shape = forma,
            shadowElevation = 2.dp,
            modifier = base.graphicsLayer { scaleX = escala; scaleY = escala }
        ) { contenido() }
    } else {
        Surface(
            color = PlagOutColors.Surface,
            shape = forma,
            shadowElevation = 2.dp,
            modifier = base
        ) { contenido() }
    }
}

// ── Estado vacío y acción ───────────────────────────────────────────────────

@Composable
fun EstadoEsperandoBiofix(modifier: Modifier = Modifier) {
    val respiracion = rememberInfiniteTransition(label = "esperandoBiofix")
    val pulso by respiracion.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulsoEsperandoBiofix"
    )

    Column(
        modifier.padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = PlagOutColors.Leaf.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier
                .size(108.dp)
                .graphicsLayer { scaleX = pulso; scaleY = pulso }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.HourglassEmpty,
                    contentDescription = null,
                    tint = PlagOutColors.Leaf,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Esperando biofix",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextMain,
            modifier = Modifier.testTag("txtEsperandoBiofix")
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "El conteo de GDD arranca cuando registrás la primera presencia de la plaga.",
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            color = PlagOutColors.TextSecondary
        )
    }
}

@Composable
fun BotonRegistrarBiofix(
    texto: String,
    habilitado: Boolean,
    cargando: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)
    val elevacion by animateFloatAsState(
        targetValue = if (habilitado) 6f else 0f,
        label = "elevacionBiofix"
    )

    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        interactionSource = interactionSource,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = PlagOutColors.Forest,
            contentColor = PlagOutColors.TextOnDark,
            disabledContainerColor = PlagOutColors.CreamDeep,
            disabledContentColor = PlagOutColors.TextSecondary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = elevacion.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .testTag("btnRegistrarBiofix")
    ) {
        if (cargando) {
            CircularProgressIndicator(color = PlagOutColors.TextOnDark, modifier = Modifier.size(20.dp))
        } else {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}
