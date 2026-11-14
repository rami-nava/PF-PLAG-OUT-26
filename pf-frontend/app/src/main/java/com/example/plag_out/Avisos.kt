package com.example.plag_out

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.plag_out.ui.theme.PlagOutColors
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class TipoAviso { EXITO, ERROR }

data class Aviso(val mensaje: String, val tipo: TipoAviso, val id: Long = System.nanoTime())


object Avisos {
    private val _eventos = MutableSharedFlow<Aviso>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventos: SharedFlow<Aviso> = _eventos

    fun exito(mensaje: String) {
        _eventos.tryEmit(Aviso(mensaje, TipoAviso.EXITO))
    }

    fun error(mensaje: String) {
        _eventos.tryEmit(Aviso(mensaje, TipoAviso.ERROR))
    }
}

// Muestra el último aviso abajo de la pantalla; uno nuevo reemplaza al anterior.
@Composable
fun AvisosHost() {
    val separacion = with(LocalDensity.current) { 96.dp.roundToPx() }
    // Popup: se dibuja sobre toda la pantalla y no bloquea toques fuera del aviso
    AvisosOverlay(
        conPopup = { contenido ->
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -separacion),
                properties = PopupProperties(focusable = false),
                content = contenido
            )
        }
    )
}

/**
 * Muestra avisos en primer plano dentro de la ventana de un diálogo, para que ningún
 * `Avisos.error(...)` quede oculto detrás del `Dialog`.
 */
@Composable
fun AvisosEnDialogo() {
    AvisosOverlay(conPopup = { contenido -> contenido() })
}

@Composable
private fun AvisosOverlay(conPopup: @Composable (@Composable () -> Unit) -> Unit) {
    var actual by remember { mutableStateOf<Aviso?>(null) }
    val visibilidad = remember { MutableTransitionState(false) }

    LaunchedEffect(Unit) {
        Avisos.eventos.collect {
            actual = it
            visibilidad.targetState = true
        }
    }
    LaunchedEffect(actual?.id) {
        val aviso = actual ?: return@LaunchedEffect
        // Los errores suelen ser más largos y conviene darles más tiempo de lectura
        delay(if (aviso.tipo == TipoAviso.EXITO) 2500 else 4500)
        visibilidad.targetState = false
    }
    // Al terminar la salida hay que desmontar el Popup: si queda vivo con el contenido vacío, su
    // ventana conserva el último cuadro de la animación y queda un aviso fantasma semitransparente.
    LaunchedEffect(visibilidad) {
        snapshotFlow { visibilidad.isIdle && !visibilidad.currentState }
            .collect { termino -> if (termino) actual = null }
    }

    val aviso = actual ?: return
    conPopup {
        AnimatedVisibility(
            visibleState = visibilidad,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            PastillaAviso(aviso = aviso, onCerrar = { visibilidad.targetState = false })
        }
    }
}

@Composable
private fun PastillaAviso(aviso: Aviso, onCerrar: () -> Unit) {
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 480.dp)
            .shadow(6.dp, forma)
            .clip(forma)
            .background(
                when (aviso.tipo) {
                    TipoAviso.EXITO -> PlagOutColors.Forest
                    TipoAviso.ERROR -> PlagOutColors.RiskDanger
                }
            )
            .clickable { onCerrar() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = when (aviso.tipo) {
                TipoAviso.EXITO -> Icons.Filled.CheckCircle
                TipoAviso.ERROR -> Icons.Filled.ErrorOutline
            },
            contentDescription = null,
            tint = PlagOutColors.TextOnDark,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = aviso.mensaje,
            color = PlagOutColors.TextOnDark,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
