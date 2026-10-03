package com.example.plag_out

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import kotlin.math.roundToInt

/**
 * Diálogo de configuración para el radio máximo de alertas de plagas cercanas.
 */
@Composable
fun DialogoConfiguracionRadio(
    radioKm: Float,
    guardando: Boolean,
    onRadioChange: (Float) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Radar,
                contentDescription = null,
                tint = PlagOutColors.Forest,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Rango de alerta de plagas",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Recibirás alertas cuando se reporten plagas dentro de esta distancia a cualquiera de tus lotes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "${radioKm.roundToInt()} km",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlagOutColors.Forest,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("txtDialogoRadioValor")
                )

                Spacer(Modifier.height(12.dp))

                Slider(
                    value = radioKm,
                    onValueChange = onRadioChange,
                    valueRange = 5f..150f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = PlagOutColors.Forest,
                        activeTrackColor = PlagOutColors.Forest,
                        inactiveTrackColor = PlagOutColors.Forest.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sliderDialogoRadio")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "5 km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "150 km",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmar,
                enabled = !guardando,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    contentColor = PlagOutColors.TextOnDark
                ),
                modifier = Modifier.testTag("btnGuardarDialogoRadio")
            ) {
                if (guardando) {
                    CircularProgressIndicator(
                        color = PlagOutColors.TextOnDark,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !guardando,
                modifier = Modifier.testTag("btnCancelarDialogoRadio")
            ) {
                Text(
                    "Cancelar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        modifier = Modifier.testTag("dialogoConfiguracionRadio")
    )
}
