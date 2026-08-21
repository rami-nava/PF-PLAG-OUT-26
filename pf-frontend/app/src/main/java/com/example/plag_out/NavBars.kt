package com.example.plag_out

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import com.example.plag_out.ui.theme.rememberPressScale
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.plag_out.ui.theme.PlagOutColors

import androidx.compose.material.icons.outlined.BugReport

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Surface

data class BarItem(val nombre: String, val ruta: String, val icono: ImageVector)

/** Alto de la píldora flotante; deja 5.dp de aire a cada lado del botón de 48.dp del medio. */
private val ALTO_BARRA = 58.dp
private val INDICADOR_ANCHO = 44.dp
private val INDICADOR_ALTO = 40.dp

/**
 * Barra inferior flotante: píldora sobre el fondo de la pantalla, solo íconos, y un indicador
 * que se desliza de una ranura a otra en vez de aparecer y desaparecer.
 *
 * Las cinco ranuras son de igual ancho; la del medio la ocupa el botón de nuevo reporte, así que
 * los ítems navegables viven en las ranuras 0, 1, 3 y 4. El indicador se mueve con el mismo
 * `spring` que usa [rememberPressScale], para que todo el movimiento de la app se sienta igual.
 */
@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

    val items = listOf(
        0 to BarItem("Monitoreos", "monitoreos", Icons.Default.Science),
        1 to BarItem("Terrenos", "terrenos", Icons.Default.Terrain),
        3 to BarItem("Reportes", "reportes", Icons.Outlined.BugReport),
        4 to BarItem("Perfil", "perfil", Icons.Default.Person)
    )

    val ranuraActiva = items.firstOrNull { it.second.ruta == currentDestination }?.first

    // Hay rutas que muestran la barra sin tener ítem propio (p.ej. terreno/{terreno_id}): ahí el
    // indicador se desvanece donde está en vez de saltar al inicio, y vuelve al mismo lugar.
    val ultimaRanura = remember { mutableStateOf(0) }
    if (ranuraActiva != null) ultimaRanura.value = ranuraActiva

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = PlagOutColors.Forest,
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(ALTO_BARRA)
        ) {
            BoxWithConstraints(Modifier.padding(horizontal = 8.dp)) {
                val anchoRanura = maxWidth / 5
                val destino = anchoRanura * ultimaRanura.value + (anchoRanura - INDICADOR_ANCHO) / 2

                // Los dos bordes de la píldora viajan al mismo destino con resortes distintos: el
                // de atrás llega más tarde, así la píldora se estira mientras cruza y se recompone
                // al llegar. Sin ese estiramiento el salto es tan corto que se lee como un corte.
                val bordeLider by animateDpAsState(
                    targetValue = destino,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    label = "bordeLiderIndicador"
                )
                val bordeRezagado by animateDpAsState(
                    targetValue = destino,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 220f),
                    label = "bordeRezagadoIndicador"
                )
                val bordeIzquierdo = minOf(bordeLider, bordeRezagado)
                val anchoIndicador = INDICADOR_ANCHO + (maxOf(bordeLider, bordeRezagado) - bordeIzquierdo)

                val alfaIndicador by animateFloatAsState(
                    targetValue = if (ranuraActiva != null) 1f else 0f,
                    animationSpec = tween(220),
                    label = "alfaIndicador"
                )

                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = bordeIzquierdo)
                        .size(width = anchoIndicador, height = INDICADOR_ALTO)
                        .alpha(alfaIndicador)
                        .background(PlagOutColors.Sun, RoundedCornerShape(percent = 50))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ALTO_BARRA),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { (ranura, item) ->
                        if (ranura == 3) BotonNuevoReporte(navController, currentDestination)
                        ItemDeBarra(
                            item = item,
                            seleccionado = ranuraActiva == ranura,
                            onClick = {
                                if (currentDestination != item.ruta) {
                                    navController.navigate(item.ruta) {
                                        popUpTo(item.ruta) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Ítem solo-ícono: el tinte cruza a Sun al seleccionarse y la presión lo achica apenas. */
@Composable
private fun RowScope.ItemDeBarra(item: BarItem, seleccionado: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)
    val tinte by animateColorAsState(
        targetValue = if (seleccionado) PlagOutColors.ForestDark else PlagOutColors.TextOnDark.copy(alpha = 0.62f),
        animationSpec = tween(260),
        label = "tinteItemBarra"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag("navBar${item.nombre}"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            item.icono,
            contentDescription = item.nombre,
            tint = tinte,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { scaleX = escala; scaleY = escala }
        )
    }
}

/** Botón circular Sun del medio para crear un reporte: se mantiene tal cual estaba. */
@Composable
private fun RowScope.BotonNuevoReporte(navController: NavController, currentDestination: String?) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {
                if (currentDestination != "crear_reporte") {
                    navController.navigate("crear_reporte")
                }
            },
            shape = CircleShape,
            color = PlagOutColors.Sun,
            shadowElevation = 6.dp,
            modifier = Modifier
                .size(48.dp)
                .testTag("btnNavBarCrearReporte")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Nuevo Reporte",
                    tint = PlagOutColors.ForestDark,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Barra superior: la marca y el timbre de notificaciones. El perfil vive en la barra inferior
 * y el resto de los ajustes, en su drawer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(noLeidas: Int = 0, onNotificacionesClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.Forest)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "PLAG-OUT",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlagOutColors.TextOnDark,
            letterSpacing = 3.sp
        )

        // El BadgedBox va por fuera del IconButton a propósito: adentro, el clip circular del
        // botón le come la esquina al badge. El padding derecho deja lugar a que sobresalga.
        BadgedBox(
            badge = {
                // El backend solo devuelve las no leídas: sin badge no hay nada pendiente
                if (noLeidas > 0) {
                    Badge(
                        containerColor = PlagOutColors.Sun,
                        contentColor = PlagOutColors.ForestDark,
                        modifier = Modifier.testTag("badgeNotificaciones")
                    ) {
                        Text(
                            if (noLeidas > 9) "9+" else "$noLeidas",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        ) {
            IconButton(
                onClick = onNotificacionesClick,
                modifier = Modifier.testTag("btnNotificaciones")
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = if (noLeidas > 0) {
                        "Notificaciones, $noLeidas sin leer"
                    } else {
                        "Notificaciones"
                    },
                    tint = PlagOutColors.TextOnDark,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

/** Confirmación de cierre de sesión, disparada desde el drawer del perfil. */
@Composable
fun DialogoCerrarSesion(onConfirmar: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PlagOutColors.Surface,
        icon = {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PlagOutColors.RiskDanger)
        },
        title = {
            Text("¿Cerrar sesión?", fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
        },
        text = {
            Text(
                "Se borrarán los datos guardados en este dispositivo y vas a tener que iniciar sesión de nuevo.",
                color = PlagOutColors.TextSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                modifier = Modifier.testTag("btnConfirmarCerrarSesion")
            ) {
                Text("Cerrar sesión", color = PlagOutColors.RiskDanger, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PlagOutColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
