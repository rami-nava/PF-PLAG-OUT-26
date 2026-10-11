package com.example.plag_out

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import kotlin.math.roundToInt

private const val RADIO_MIN_KM = 5f
private const val RADIO_MAX_KM = 150f
private val RADIOS_SUGERIDOS_KM = listOf(10, 25, 50, 100)

/**
 * Diálogo de configuración para el radio máximo de alertas de plagas cercanas.
 * [radioActualKm] es el valor guardado: sirve para mostrar el cambio y no guardar si no lo hubo.
 */
@Composable
fun DialogoConfiguracionRadio(
    radioKm: Float,
    radioActualKm: Float,
    guardando: Boolean,
    onRadioChange: (Float) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    val radio = radioKm.roundToInt()
    val actual = radioActualKm.roundToInt()
    val cambio = radio != actual

    MarcoDialogo(tag = "dialogoConfiguracionRadio", onDismissRequest = { if (!guardando) onDismiss() }) { rebote ->
        InsigniaAccion(Icons.Outlined.Radar, PlagOutColors.Forest, rebote)
        Spacer(Modifier.height(16.dp))
        Text(
            "Rango de alerta de plagas",
            style = MaterialTheme.typography.titleLarge,
            color = PlagOutColors.TextMain,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Te avisamos cuando se reporten plagas dentro de esta distancia a cualquiera de tus lotes.",
            style = MaterialTheme.typography.bodyMedium,
            color = PlagOutColors.TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(18.dp))
        TarjetaValorRadio(radio, actual, cambio, guardando, onRadioChange)

        Spacer(Modifier.height(16.dp))
        Text(
            "ACCESOS RÁPIDOS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RADIOS_SUGERIDOS_KM.forEach { km ->
                ChipRadioSugerido(
                    km = km,
                    seleccionado = radio == km,
                    habilitado = !guardando,
                    onClick = { onRadioChange(km.toFloat()) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = alcanceRadio(radio),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "avisoAlcanceRadio"
        ) { alcance ->
            AvisoDialogo(Icons.Outlined.Info, alcance.texto, alcance.color)
        }

        Spacer(Modifier.height(22.dp))
        BotonesDialogo(
            textoConfirmar = "Guardar",
            iconoConfirmar = Icons.Outlined.Check,
            color = PlagOutColors.Forest,
            tagCancelar = "btnCancelarDialogoRadio",
            tagConfirmar = "btnGuardarDialogoRadio",
            onCancelar = onDismiss,
            onConfirmar = onConfirmar,
            habilitado = cambio,
            ocupado = guardando
        )
    }
}

@Composable
private fun TarjetaValorRadio(
    radio: Int,
    actual: Int,
    cambio: Boolean,
    guardando: Boolean,
    onRadioChange: (Float) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PlagOutColors.Cream)
            .border(1.dp, PlagOutColors.CreamDeep, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedContent(
                targetState = radio,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "valorRadio"
            ) { valor ->
                Text(
                    "$valor",
                    fontSize = 44.sp,
                    lineHeight = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlagOutColors.Forest,
                    modifier = Modifier.testTag("txtDialogoRadioValor")
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "km",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.Leaf,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        AnimatedVisibility(
            visible = cambio,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PlagOutColors.Sun.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$actual km", fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "pasa a",
                    tint = PlagOutColors.Terracotta,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(14.dp)
                )
                Text("$radio km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
            }
        }
        if (!cambio) {
            Text(
                "Valor actual",
                fontSize = 12.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(Modifier.height(8.dp))
        Slider(
            value = radio.toFloat(),
            onValueChange = onRadioChange,
            valueRange = RADIO_MIN_KM..RADIO_MAX_KM,
            steps = 28,
            enabled = !guardando,
            colors = SliderDefaults.colors(
                thumbColor = PlagOutColors.Forest,
                activeTrackColor = PlagOutColors.Forest,
                inactiveTrackColor = PlagOutColors.Forest.copy(alpha = 0.18f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sliderDialogoRadio")
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${RADIO_MIN_KM.roundToInt()} km", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
            Text("${RADIO_MAX_KM.roundToInt()} km", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
        }
    }
}

@Composable
private fun ChipRadioSugerido(
    km: Int,
    seleccionado: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fondo by animateColorAsState(
        if (seleccionado) PlagOutColors.Forest else PlagOutColors.Surface,
        label = "fondoChipRadio"
    )
    val texto by animateColorAsState(
        if (seleccionado) PlagOutColors.TextOnDark else PlagOutColors.TextMain,
        label = "textoChipRadio"
    )
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(12.dp),
        color = fondo,
        border = if (seleccionado) null else BorderStroke(1.dp, PlagOutColors.Divider),
        modifier = modifier
            .height(38.dp)
            .testTag("chipRadio$km")
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("$km km", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = texto)
        }
    }
}

private enum class AlcanceRadio(val texto: String, val color: Color) {
    CERCANO("Solo plagas muy próximas a tus lotes: pocas alertas, todas urgentes.", PlagOutColors.Leaf),
    REGIONAL("Rango equilibrado: te enterás de lo que avanza por la zona con tiempo para actuar.", PlagOutColors.Forest),
    AMPLIO("Rango amplio: vas a recibir más alertas, incluso de plagas lejanas.", PlagOutColors.Terracotta)
}

private fun alcanceRadio(km: Int) = when {
    km <= 20 -> AlcanceRadio.CERCANO
    km <= 75 -> AlcanceRadio.REGIONAL
    else -> AlcanceRadio.AMPLIO
}
