package com.example.plag_out

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

@Composable
fun DatosDelTerrenoScreen(
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {

    //EXPRESION REGULAR PARA NUMEROS REALES
    val regex = Regex("^\\d*\\.?\\d*$")

    val state by nuevoTerrenoViewModel.state.collectAsState()
    val formularioValido = state.nombre.trim().isNotEmpty() && state.areaHectareas.trim().isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderNuevoTerreno(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Información general",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "Dale un nombre a tu lote y contanos cuántas hectáreas ocupa.",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = state.nombre,
                        onValueChange = { nuevoTerrenoViewModel.actualizarNombre(it) },
                        label = { Text("Nombre del terreno") },
                        leadingIcon = { Icon(Icons.Outlined.Terrain, contentDescription = null, tint = PlagOutColors.Forest) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = camposColors(),
                        modifier = Modifier.fillMaxWidth().testTag("txtNombre")
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.areaHectareas,
                        onValueChange = { if (it.matches(regex)) nuevoTerrenoViewModel.actualizarArea(it) },
                        label = { Text("Hectáreas") },
                        leadingIcon = { Icon(Icons.Filled.SquareFoot, contentDescription = null, tint = PlagOutColors.Forest) },
                        trailingIcon = { Text("ha", color = PlagOutColors.TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(end = 12.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = camposColors(),
                        modifier = Modifier.fillMaxWidth().testTag("txtHectareas")
                    )
                }
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        state.error ?: "",
                        color = PlagOutColors.RiskDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            PasosIndicador(pasoActual = 0)

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onContinue,
                enabled = formularioValido,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnGuardar"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f),
                    contentColor = PlagOutColors.TextOnDark,
                    disabledContentColor = PlagOutColors.TextOnDark.copy(alpha = 0.6f)
                )
            ) {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
}

@Composable
private fun HeaderNuevoTerreno(onBack: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderNuevoTerreno")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativa"
    )

    val forma = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = forma
            )
            .clip(forma)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-40).dp)
                .size(50.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )

        Column(Modifier.padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                }
                Column {
                    Text(
                        "Nuevo terreno · Paso 1 de 2",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                    Text("Datos del Terreno", color = PlagOutColors.TextOnDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

/** Indicador de pasos compartido por el flujo "nuevo terreno" (2 pasos). */
@Composable
fun PasosIndicador(pasoActual: Int, totalPasos: Int = 2) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalPasos) { indice ->
            val activo = indice <= pasoActual
            Box(
                Modifier
                    .weight(1f)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (activo) PlagOutColors.Forest else PlagOutColors.Divider)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun camposColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PlagOutColors.Forest,
    focusedLabelColor = PlagOutColors.Forest,
    cursorColor = PlagOutColors.Forest,
    focusedLeadingIconColor = PlagOutColors.Forest,
    unfocusedLeadingIconColor = PlagOutColors.TextSecondary
)
