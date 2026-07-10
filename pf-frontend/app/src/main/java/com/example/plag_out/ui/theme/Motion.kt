package com.example.plag_out.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Utilidades de animación compartidas por las pantallas principales
 * (Monitoreos, Plantaciones, Terrenos) para que todas se sientan parte
 * de un mismo sistema de movimiento: entradas escalonadas y feedback táctil.
 */

/** Envuelve un ítem de lista con una entrada escalonada (fade + slide up). */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    var visible by remember(index) { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((index * 55L).coerceAtMost(420L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(360)) + slideInVertically(
            animationSpec = tween(360),
            initialOffsetY = { it / 5 }
        ),
        content = content
    )
}

/** Escala sutil de "presión" para dar feedback táctil a tarjetas clickeables. */
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource): Float {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return scale
}
