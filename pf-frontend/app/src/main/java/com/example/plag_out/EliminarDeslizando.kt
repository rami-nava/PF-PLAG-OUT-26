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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
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
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

// ── Menú de acciones del header ──────────────────────────────────────────────
// Las acciones de un detalle (editar, finalizar, eliminar) viven en un ⋮ del header: quedan
// a mano desde todas las pestañas sin ocupar espacio fijo al pie del contenido.

data class OpcionMenuAccion(
    val icono: ImageVector,
    val titulo: String,
    val detalle: String,
    val color: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
fun MenuAccionesHeader(opciones: List<OpcionMenuAccion>, ocupado: Boolean, tag: String) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        if (ocupado) {
            CircularProgressIndicator(
                color = PlagOutColors.TextOnDark,
                strokeWidth = 2.dp,
                modifier = Modifier.padding(12.dp).size(22.dp)
            )
        } else {
            IconButton(onClick = { abierto = true }, modifier = Modifier.testTag(tag)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Más acciones", tint = PlagOutColors.TextOnDark)
            }
        }
        DropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false },
            shape = RoundedCornerShape(16.dp),
            containerColor = PlagOutColors.Surface
        ) {
            opciones.forEachIndexed { i, opcion ->
                if (i > 0) HorizontalDivider(color = PlagOutColors.Divider, modifier = Modifier.padding(horizontal = 12.dp))
                DropdownMenuItem(
                    onClick = { abierto = false; opcion.onClick() },
                    leadingIcon = { Icon(opcion.icono, contentDescription = null, tint = opcion.color) },
                    text = {
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text(opcion.titulo, color = opcion.color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(opcion.detalle, color = PlagOutColors.TextSecondary, fontSize = 12.sp)
                        }
                    },
                    modifier = Modifier.widthIn(min = 260.dp).testTag(opcion.tag)
                )
            }
        }
    }
}

// ── Confirmaciones ───────────────────────────────────────────────────────────
// Cada una se usa tanto desde el botón del detalle como desde el deslizamiento en la lista.

@Composable
fun DialogoConfirmarEliminacion(
    titulo: String,
    icono: ImageVector,
    nombre: String,
    detalle: String?,
    consecuencias: List<String>,
    tag: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    consejo: String? = null
) = DialogoConfirmarAccion(
    titulo = titulo,
    colorAccion = PlagOutColors.RiskDanger,
    iconoAccion = Icons.Outlined.DeleteOutline,
    textoConfirmar = "Eliminar",
    icono = icono,
    nombre = nombre,
    detalle = detalle,
    encabezadoConsecuencias = "AL ELIMINARLO",
    consecuencias = consecuencias,
    aviso = "Esta acción no se puede deshacer.",
    consejo = consejo,
    tagDialogo = "dialogEliminar$tag",
    tagConfirmar = "btnConfirmarEliminar$tag",
    tagCancelar = "btnCancelarEliminar$tag",
    onConfirmar = onConfirmar,
    onCancelar = onCancelar
)

/**
 * Esqueleto común de las confirmaciones de acciones fuertes (eliminar, finalizar): insignia
 * animada, ficha del elemento afectado, qué pasa al confirmar y los dos botones.
 * [contenidoExtra] va antes de los botones, para campos propios de cada acción.
 */
@Composable
fun DialogoConfirmarAccion(
    titulo: String,
    colorAccion: Color,
    iconoAccion: ImageVector,
    textoConfirmar: String,
    icono: ImageVector,
    nombre: String,
    detalle: String?,
    encabezadoConsecuencias: String,
    consecuencias: List<String>,
    aviso: String?,
    tagDialogo: String,
    tagConfirmar: String,
    tagCancelar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    consejo: String? = null,
    contenidoExtra: @Composable ColumnScope.() -> Unit = {}
) {
    MarcoDialogo(tag = tagDialogo, onDismissRequest = onCancelar) { rebote ->
        InsigniaAccion(iconoAccion, colorAccion, rebote)
        Spacer(Modifier.height(16.dp))
        Text(
            titulo,
            style = MaterialTheme.typography.titleLarge,
            color = PlagOutColors.TextMain,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))

        FichaElemento(icono, nombre, detalle)

        if (consecuencias.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(
                encabezadoConsecuencias,
                style = MaterialTheme.typography.labelSmall,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            consecuencias.forEach { ConsecuenciaAccion(it, colorAccion) }
        }

        if (aviso != null) {
            Spacer(Modifier.height(14.dp))
            AvisoDialogo(
                icono = Icons.Outlined.WarningAmber,
                texto = aviso,
                color = colorAccion,
                negrita = true
            )
        }
        if (consejo != null) {
            Spacer(Modifier.height(8.dp))
            AvisoDialogo(
                icono = Icons.Outlined.Lightbulb,
                texto = consejo,
                color = PlagOutColors.Leaf
            )
        }

        contenidoExtra()

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCancelar,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PlagOutColors.Divider),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.TextMain),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag(tagCancelar)
            ) {
                Text("Cancelar", fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onConfirmar,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorAccion,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag(tagConfirmar)
            ) {
                Icon(iconoAccion, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(textoConfirmar, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Marco común de los diálogos de la app: tarjeta redondeada que entra con un leve crecimiento.
 * [contenido] recibe el progreso del rebote (0→1) para animar la insignia de arriba.
 */
@Composable
fun MarcoDialogo(
    tag: String,
    onDismissRequest: () -> Unit,
    contenido: @Composable ColumnScope.(rebote: Float) -> Unit
) {
    // Entrada: el diálogo crece apenas y la insignia rebota después, para que la atención vaya a la acción.
    val entrada = remember { Animatable(0f) }
    val rebote = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { entrada.animateTo(1f, tween(220)) }
        delay(90)
        rebote.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = PlagOutColors.Surface,
            shadowElevation = 12.dp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val e = entrada.value
                    alpha = e
                    scaleX = 0.92f + 0.08f * e
                    scaleY = 0.92f + 0.08f * e
                }
                .testTag(tag)
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                contenido(rebote.value)
            }
        }
    }
}

@Composable
fun InsigniaAccion(icono: ImageVector, color: Color, rebote: Float) {
    Box(
        Modifier
            .size(72.dp)
            .graphicsLayer { scaleX = rebote; scaleY = rebote }
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = Color.White,
                // Un leve bamboleo mientras rebota: de 0 a 1 pasa por -8° y vuelve a su lugar.
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { rotationZ = -8f * sin(rebote * PI.toFloat()) }
            )
        }
    }
}

// Sobre qué se actúa, con nombre propio: evita que se confirme sobre el elemento equivocado.
@Composable
private fun FichaElemento(icono: ImageVector, nombre: String, detalle: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PlagOutColors.Cream)
            .border(1.dp, PlagOutColors.CreamDeep, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PlagOutColors.Leaf.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                nombre,
                style = MaterialTheme.typography.titleMedium,
                color = PlagOutColors.TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!detalle.isNullOrBlank()) {
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlagOutColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConsecuenciaAccion(texto: String, color: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(10.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = PlagOutColors.TextMain)
    }
}

@Composable
private fun AvisoDialogo(icono: ImageVector, texto: String, color: Color, negrita: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.09f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            texto,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = if (negrita) color else PlagOutColors.TextMain,
            fontWeight = if (negrita) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun DialogoEliminarTerreno(terreno: TerrenoResponse, onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Eliminar este terreno?",
        icono = Icons.Outlined.Landscape,
        nombre = terreno.terreno_nombre,
        detalle = "Terreno",
        consecuencias = listOf(
            "Se borran todos los cultivos del terreno.",
            "Se borran sus monitoreos, con las alertas y el historial."
        ),
        tag = "Terreno",
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )

@Composable
fun DialogoEliminarPlantacion(plantacion: PlantacionesResponse, onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Eliminar este cultivo?",
        icono = Icons.Outlined.Spa,
        nombre = plantacion.cultivo_nombre,
        detalle = "En ${plantacion.terreno_nombre}",
        consecuencias = listOf("Se borran todos sus monitoreos, con las alertas y el historial."),
        tag = "Plantacion",
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )

@Composable
fun DialogoEliminarMonitoreo(monitoreo: MonitoreoResponse, onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Eliminar este monitoreo?",
        icono = Icons.Outlined.BugReport,
        nombre = monitoreo.plaga_nombre,
        detalle = "${monitoreo.cultivo_nombre} · ${monitoreo.terreno_nombre}",
        consecuencias = listOf(
            "Se borran sus ciclos y predicciones.",
            "Se borran las alertas que generó."
        ),
        consejo = "¿Solo querés dejar de recibir alertas? Usá Finalizar desde el detalle: " +
            "conserva el historial.",
        tag = "Monitoreo",
        onConfirmar = onConfirmar,
        onCancelar = onCancelar
    )

@Composable
fun DialogoEliminarReporte(reporte: ReporteDetalleResponse, onConfirmar: () -> Unit, onCancelar: () -> Unit) =
    DialogoConfirmarEliminacion(
        titulo = "¿Eliminar este reporte?",
        icono = Icons.Outlined.Campaign,
        nombre = reporte.plaga_nombre,
        detalle = listOfNotNull(reporte.cultivo_nombre, reporte.terreno_nombre)
            .joinToString(" · ")
            .ifBlank { "Severidad ${reporte.nivel_severidad.lowercase()}" },
        consecuencias = listOf("Deja de verse para los productores de tu zona."),
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
