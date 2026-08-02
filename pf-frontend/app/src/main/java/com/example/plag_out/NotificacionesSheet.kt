package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.plag_out.ui.theme.PlagOutColors
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Cuánto tarda la hoja en deslizarse fuera de pantalla antes de desmontarse. */
private const val DURACION_SALIDA_MS = 200

/**
 * Historial de notificaciones sin leer. El backend solo devuelve las no leídas, así que la hoja
 * se vacía a medida que se leen: cada fila se marca leída al tocarla (y navega a su entidad si
 * la notificación trae una) o deslizándola hacia un costado.
 *
 * Baja desde arriba (no es un ModalBottomSheet) porque el timbre que la abre vive en la barra
 * superior: que el contenido aparezca pegado a ese botón es más intuitivo que traerlo desde abajo.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesSheet(
    state: NotificacionesUIState,
    onMarcarLeida: (NotificacionResponse) -> Unit,
    onNavegar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val cerrar: () -> Unit = { visible = false }
    LaunchedEffect(visible) {
        if (!visible) {
            delay(DURACION_SALIDA_MS.toLong())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = cerrar,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(DURACION_SALIDA_MS))
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = cerrar
                        )
                )
            }

            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(tween(260)) { -it } + fadeIn(tween(200)),
                exit = slideOutVertically(tween(DURACION_SALIDA_MS)) { -it } + fadeOut(tween(150)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                ) {
                    Column(
                        Modifier
                            .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp)
                            .testTag("hojaNotificaciones")
                    ) {
                        Text(
                            "Notificaciones",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PlagOutColors.TextMain
                        )
                        Text(
                            when {
                                state.notificaciones.isEmpty() -> "No tenés avisos pendientes"
                                state.notificaciones.size == 1 -> "1 aviso sin leer"
                                else -> "${state.notificaciones.size} avisos sin leer"
                            },
                            fontSize = 12.sp,
                            color = PlagOutColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(Modifier.height(16.dp))

                        when {
                            state.isLoading && state.notificaciones.isEmpty() -> {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PlagOutColors.Forest)
                                }
                            }

                            state.notificaciones.isEmpty() -> EstadoAlDia()

                            else -> {
                                if (state.error != null) {
                                    AvisoError(state.error)
                                    Spacer(Modifier.height(12.dp))
                                }
                                Text(
                                    "Deslizá una notificación para marcarla como leída",
                                    fontSize = 11.sp,
                                    color = PlagOutColors.TextSecondary,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                                // heightIn: con muchos avisos la lista scrollea dentro de la hoja
                                // en lugar de empujar el encabezado fuera de pantalla.
                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 420.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(state.notificaciones, key = { it.id }) { notificacion ->
                                        FilaNotificacion(
                                            notificacion = notificacion,
                                            onMarcarLeida = { onMarcarLeida(notificacion) },
                                            onClick = {
                                                onMarcarLeida(notificacion)
                                                val destino = destinoDe(notificacion)
                                                if (destino != null) {
                                                    cerrar()
                                                    onNavegar(destino)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fila deslizable. El swipe hacia cualquiera de los dos lados marca como leída: no hay otra
 * acción posible, así que no tiene sentido que cada dirección haga algo distinto.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaNotificacion(
    notificacion: NotificacionResponse,
    onMarcarLeida: () -> Unit,
    onClick: () -> Unit
) {
    val estadoSwipe = rememberSwipeToDismissBoxState()

    LaunchedEffect(estadoSwipe.currentValue) {
        if (estadoSwipe.currentValue != SwipeToDismissBoxValue.Settled) {
            onMarcarLeida()
        }
    }

    SwipeToDismissBox(
        state = estadoSwipe,
        backgroundContent = { FondoSwipe() },
        modifier = Modifier.testTag("notificacion_${notificacion.id}")
    ) {
        Surface(
            color = PlagOutColors.Cream,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(PlagOutColors.Sun.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = PlagOutColors.Terracotta,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        notificacion.titulo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        notificacion.mensaje,
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        tiempoRelativo(notificacion.fecha_envio),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.Leaf,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Solo se insinúa navegación si la notificación realmente lleva a algún lado
                if (destinoDe(notificacion) != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = PlagOutColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Lo que queda a la vista detrás de la fila mientras se la desliza. */
@Composable
private fun FondoSwipe() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(PlagOutColors.Leaf.copy(alpha = 0.18f))
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        EtiquetaLeida()
        EtiquetaLeida()
    }
}

@Composable
private fun EtiquetaLeida() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = PlagOutColors.Forest,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Leída", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.Forest)
    }
}

@Composable
private fun EstadoAlDia() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(PlagOutColors.Leaf.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = PlagOutColors.Forest,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Estás al día",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextMain
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Te avisamos acá cuando el riesgo de alguno de tus monitoreos supere tu umbral.",
            fontSize = 13.sp,
            color = PlagOutColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AvisoError(mensaje: String) {
    Text(
        mensaje,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = PlagOutColors.RiskDanger,
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("errorNotificaciones")
    )
}

/** "Hace 5 min" / "Hace 3 h" / fecha corta, según cuán vieja sea la notificación. */
@RequiresApi(Build.VERSION_CODES.O)
private fun tiempoRelativo(fecha: LocalDateTime): String {
    val transcurrido = Duration.between(fecha, LocalDateTime.now())
    val minutos = transcurrido.toMinutes()
    return when {
        // Reloj del dispositivo atrasado respecto del servidor: no tiene sentido "hace -2 min"
        minutos < 1 -> "Recién"
        minutos < 60 -> "Hace $minutos min"
        transcurrido.toHours() < 24 -> "Hace ${transcurrido.toHours()} h"
        transcurrido.toDays() < 7 -> "Hace ${transcurrido.toDays()} d"
        else -> fecha.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-AR")))
    }
}
