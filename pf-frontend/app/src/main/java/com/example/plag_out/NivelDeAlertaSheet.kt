package com.example.plag_out

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.estiloDeNivel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NivelDeAlertaSheet(nivelActual: Int?, onDismiss: () -> Unit) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = estadoHoja,
        containerColor = PlagOutColors.Surface,
        contentWindowInsets = { WindowInsets.systemBars },
        dragHandle = { BottomSheetDefaults.DragHandle(color = PlagOutColors.Divider) },
        modifier = Modifier.testTag("hojaNivelAlerta")
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp)
        ) {
            Text(
                "¿Qué es el nivel de alerta?",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )
            Spacer(Modifier.height(12.dp))

            Text(
                "Resume en tres escalones qué tan favorables fueron las condiciones ambientales " +
                    "para la plaga. No mide lo que hay hoy en el lote: estima la densidad " +
                    "poblacional que podés esperar.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = PlagOutColors.TextSecondary
            )

            Spacer(Modifier.height(16.dp))

            TarjetaIRA()

            Spacer(Modifier.height(16.dp))

            Text(
                "Los tres niveles",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )
            Spacer(Modifier.height(10.dp))

            val destacados = listOf(
                nivelActual == 0,
                nivelActual == 1,
                nivelActual != null && nivelActual >= 2
            )
            // Solo el nivel del monitoreo abierto suma la recomendación.
            destacados.forEachIndexed { nivel, destacado ->
                FilaNivelDeAlerta(nivel, destacado = destacado, mostrarRecomendacion = destacado)
            }

            Spacer(Modifier.height(14.dp))

            Text(
                "Se recalcula una vez por día. El umbral de riesgo define desde qué porcentaje " +
                    "querés que te avisemos.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = PlagOutColors.TextSecondary
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnEntendidoNivelAlerta"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    contentColor = PlagOutColors.TextOnDark
                )
            ) {
                Text("Entendido", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tarjeta de concepto (GDD, IRA, biofix): mismo fondo, mismo chip de ícono y mismo título, para
 * que los bloques explicativos de la app se lean como una sola familia.
 */
@Composable
fun TarjetaConcepto(
    icono: ImageVector,
    titulo: String,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(PlagOutColors.Forest.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icono,
                    contentDescription = null,
                    tint = PlagOutColors.Forest,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )
        }
        Spacer(Modifier.height(8.dp))
        contenido()
    }
}

/** Bloque explicativo del IRA; se reusa en la ayuda general de la app. */
@Composable
fun TarjetaIRA(modifier: Modifier = Modifier) {
    TarjetaConcepto(Icons.Outlined.Insights, "¿Qué es el IRA?", modifier) {
        Text(
            "Índice de Riesgo Ambiental: combina los GDD acumulados con las condiciones que " +
                "atravesó la plaga para estimar cuánto la favoreció el ambiente. De ahí salen el " +
                "nivel de alerta y el porcentaje del anillo.",
            fontSize = 13.sp,
            color = PlagOutColors.TextSecondary
        )
    }
}

/** Bloque explicativo de los GDD; lo comparten la ayuda general y la hoja del umbral de riesgo. */
@Composable
fun TarjetaGDD(modifier: Modifier = Modifier) {
    TarjetaConcepto(Icons.Outlined.Thermostat, "¿Qué son los GDD?", modifier) {
        Text(
            "Grados Día de Crecimiento: el calor que se fue acumulando desde que arranca el conteo " +
                "(la siembra de tu cultivo o el biofix de la plaga). Cada plaga necesita juntar " +
                "cierta cantidad para eclosionar, y ese total es el objetivo del monitoreo.",
            fontSize = 13.sp,
            color = PlagOutColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Los valores se recalculan una vez por día.",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PlagOutColors.TextSecondary
        )
    }
}

/**
 * Una fila por nivel: chip real + qué significa + qué hacer. `destacado` marca el nivel del
 * monitoreo que se está viendo.
 */
@Composable
fun FilaNivelDeAlerta(
    nivel: Int,
    destacado: Boolean = false,
    mostrarRecomendacion: Boolean = true
) {
    val estilo = estiloDeNivel(nivel)
    val fondo = if (destacado) estilo.color.copy(alpha = 0.08f) else PlagOutColors.Surface

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(fondo, RoundedCornerShape(14.dp))
            .then(
                if (destacado) {
                    Modifier.border(1.dp, estilo.color.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelloDeNivel(estilo = estilo, pulsante = false)
            if (destacado) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "nivel actual",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = estilo.color
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            estilo.descripcion,
            fontSize = 12.sp,
            color = PlagOutColors.TextSecondary
        )
        if (mostrarRecomendacion && estilo.recomendacion.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                estilo.recomendacion,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PlagOutColors.TextMain
            )
        }
    }
}
