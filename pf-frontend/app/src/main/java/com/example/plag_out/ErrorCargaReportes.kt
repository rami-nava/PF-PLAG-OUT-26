package com.example.plag_out

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors


@Composable
internal fun EstadoErrorReportes(
    mensaje: String,
    sinConexion: Boolean,
    reintentando: Boolean,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = if (sinConexion) PlagOutColors.RiskWarn else PlagOutColors.RiskDanger
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .testTag("errorCargaReportes"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .background(color.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (sinConexion) Icons.Outlined.CloudOff else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(42.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (sinConexion) "Sin conexión" else "No pudimos cargar las alertas",
            color = PlagOutColors.TextMain,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (sinConexion) "Revisá tu conexión a internet y volvé a intentar." else mensaje,
            color = PlagOutColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onReintentar,
            enabled = !reintentando,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.6f),
                disabledContentColor = PlagOutColors.TextOnDark
            ),
            modifier = Modifier.testTag("reintentarCargaReportes")
        ) {
            if (reintentando) {
                CircularProgressIndicator(
                    color = PlagOutColors.TextOnDark,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(if (reintentando) "Reintentando…" else "Reintentar", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Aviso compacto para cuando la carga falla pero quedan reportes en pantalla (o sobre el mapa).
 * Fondo opaco para que se lea igual encima del mapa.
 */
@Composable
internal fun ErrorCargaReportes(
    mensaje: String,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    sinConexion: Boolean = false,
    reintentando: Boolean = false
) {
    val color = if (sinConexion) PlagOutColors.RiskWarn else PlagOutColors.RiskDanger
    val forma = RoundedCornerShape(16.dp)
    Surface(
        color = PlagOutColors.Surface,
        shape = forma,
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, forma)
            .testTag("errorCargaReportes")
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (sinConexion) Icons.Outlined.CloudOff else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    if (sinConexion) "Sin conexión" else "No se pudo actualizar",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PlagOutColors.TextMain
                )
                Text(
                    if (sinConexion) "Mostrando las alertas que ya estaban cargadas" else mensaje,
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary
                )
            }
            if (reintentando) {
                CircularProgressIndicator(
                    color = color,
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .size(18.dp)
                )
            } else {
                TextButton(onClick = onReintentar, modifier = Modifier.testTag("reintentarCargaReportes")) {
                    Text("Reintentar", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
