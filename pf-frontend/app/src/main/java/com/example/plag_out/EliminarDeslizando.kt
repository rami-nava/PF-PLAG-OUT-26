package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

// ── Deslizar para eliminar ───────────────────────────────────────────────────

val FormaTarjeta: Shape = RoundedCornerShape(22.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeslizableParaEliminar(
    procesando: Boolean,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier,
    forma: Shape = FormaTarjeta,
    contenido: @Composable () -> Unit
) {
    // El estado se recuerda una sola vez: sin esto se quedaría con el primer onEliminar.
    val onEliminarActual by rememberUpdatedState(onEliminar)
    val estado = rememberSwipeToDismissBoxState(
        confirmValueChange = { destino ->
            if (destino == SwipeToDismissBoxValue.EndToStart) onEliminarActual()
            false
        },
        positionalThreshold = { distanciaTotal -> distanciaTotal * 0.4f }
    )

    // Vibra una vez al cruzar el umbral, para que se sienta cuándo "ya alcanza" con soltar.
    val haptica = LocalHapticFeedback.current
    val pasoElUmbral = estado.targetValue == SwipeToDismissBoxValue.EndToStart
    LaunchedEffect(pasoElUmbral) {
        if (pasoElUmbral) haptica.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    SwipeToDismissBox(
        state = estado,
        enableDismissFromStartToEnd = false,
        gesturesEnabled = !procesando,
        backgroundContent = { FondoEliminar(estado, pasoElUmbral, forma) },
        modifier = modifier.graphicsLayer { alpha = if (procesando) 0.45f else 1f }
    ) {
        Box {
            contenido()
            if (procesando) {
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FondoEliminar(estado: SwipeToDismissBoxState, pasoElUmbral: Boolean, forma: Shape) {
    // progress vale 0 en reposo y crece mientras se arrastra hacia la izquierda.
    val arrastrando = estado.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val avance = if (arrastrando) estado.progress.coerceIn(0f, 1f) else 0f

    val rojo by animateColorAsState(
        targetValue = if (pasoElUmbral) PlagOutColors.RiskDanger else PlagOutColors.RiskDanger.copy(alpha = 0.55f),
        animationSpec = tween(180),
        label = "rojoEliminar"
    )
    val escalaIcono by animateFloatAsState(
        targetValue = if (pasoElUmbral) 1.25f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escalaIconoEliminar"
    )

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = (avance * 3f).coerceAtMost(1f) }
            .clip(forma)
            .background(rojo)
            .padding(end = 28.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { scaleX = escalaIcono; scaleY = escalaIcono }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (pasoElUmbral) "Soltá para eliminar" else "Eliminar",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ── Confirmaciones ───────────────────────────────────────────────────────────
// Cada una se usa tanto desde el botón del detalle como desde el deslizamiento en la lista.

@Composable
fun DialogoConfirmarEliminacion(
    titulo: String,
    mensaje: String,
    tag: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        modifier = Modifier.testTag("dialogEliminar$tag"),
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                modifier = Modifier.testTag("btnConfirmarEliminar$tag")
            ) {
                Text("Eliminar", color = PlagOutColors.RiskDanger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancelar,
                modifier = Modifier.testTag("btnCancelarEliminar$tag")
            ) { Text("Cancelar") }
        }
    )
}

@Composable
fun DialogoEliminarTerreno(terreno: TerrenoResponse, onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Eliminar terreno?",
        mensaje = "Se va a eliminar \"${terreno.terreno_nombre}\" de forma permanente, junto con sus " +
            "cultivos y monitoreos. Esta acción no se puede deshacer.",
        tag = "Terreno",
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )

@Composable
fun DialogoEliminarPlantacion(plantacion: PlantacionesResponse, onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Eliminar cultivo?",
        mensaje = "Se va a eliminar \"${plantacion.cultivo_nombre}\" de forma permanente, junto con sus monitoreos. " +
            "Esta acción no se puede deshacer.",
        tag = "Plantacion",
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )

@Composable
fun DialogoEliminarMonitoreo(monitoreo: MonitoreoResponse, onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Eliminar monitoreo?",
        mensaje = "Se va a eliminar el monitoreo de ${monitoreo.plaga_nombre} en ${monitoreo.cultivo_nombre} " +
            "de forma permanente, junto con sus ciclos, predicciones y alertas. " +
            "Esta acción no se puede deshacer.\n\nSi solo querés dejar de recibir alertas y " +
            "conservar el historial, usá Finalizar desde el detalle del monitoreo.",
        tag = "Monitoreo",
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )

@Composable
fun DialogoEliminarReporte(onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Estás seguro de que deseas eliminar este reporte?",
        mensaje = "Esta acción no se puede deshacer y si eliminas este reporte, dejará de ser visible " +
            "para los productores de tu zona.",
        tag = "Reporte",
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )

// ── Eliminación en cascada ───────────────────────────────────────────────────


@RequiresApi(Build.VERSION_CODES.O)
fun eliminarTerrenoEnCascada(
    terrenoId: Int,
    terrenos: TerrenosViewModel,
    plantaciones: PlantacionesViewModel,
    monitoreos: MonitoreosViewModel,
    onEliminado: () -> Unit = {}
) {
    terrenos.eliminarTerreno(terrenoId) {
        plantaciones.purgarPorTerreno(terrenoId)
        monitoreos.purgarPorTerreno(terrenoId)
        onEliminado()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun eliminarPlantacionEnCascada(
    plantacionId: Int,
    plantaciones: PlantacionesViewModel,
    monitoreos: MonitoreosViewModel,
    onEliminada: () -> Unit = {}
) {
    plantaciones.eliminarPlantacion(plantacionId) {
        monitoreos.purgarPorPlantacion(plantacionId)
        onEliminada()
    }
}
