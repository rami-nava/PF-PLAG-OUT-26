package com.example.plag_out

import androidx.compose.foundation.background
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
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

import androidx.compose.ui.text.style.TextOverflow

data class BarItem(val nombre: String, val ruta: String, val icono: ImageVector)

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = PlagOutColors.Forest,
        contentColor = PlagOutColors.TextOnDark
    ) {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

        val leftItems = listOf(
            BarItem("Monitoreos", "monitoreos", Icons.Default.Science),
            BarItem("Terrenos", "terrenos", Icons.Default.Terrain)
        )
        val rightItems = listOf(
            BarItem("Reportes", "reportes", Icons.Outlined.BugReport),
            BarItem("Perfil", "perfil", Icons.Default.Person)
        )

        leftItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icono, contentDescription = item.nombre, modifier = Modifier.size(20.dp)) },
                label = {
                    Text(
                        text = item.nombre,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = currentDestination == item.ruta,
                onClick = {
                    if (currentDestination != item.ruta) {
                        navController.navigate(item.ruta) {
                            popUpTo(item.ruta) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = PlagOutColors.ForestDark,
                    unselectedIconColor = PlagOutColors.TextOnDark.copy(alpha = 0.65f),
                    selectedTextColor = PlagOutColors.TextOnDark,
                    unselectedTextColor = PlagOutColors.TextOnDark.copy(alpha = 0.65f),
                    selectedIndicatorColor = PlagOutColors.Sun,
                    disabledIconColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledTextColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }

        // Botón circular amarillo/ocre en el medio para crear nuevo reporte
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

        rightItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icono, contentDescription = item.nombre, modifier = Modifier.size(20.dp)) },
                label = {
                    Text(
                        text = item.nombre,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = currentDestination == item.ruta,
                onClick = {
                    if (currentDestination != item.ruta) {
                        navController.navigate(item.ruta) {
                            popUpTo(item.ruta) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = PlagOutColors.ForestDark,
                    unselectedIconColor = PlagOutColors.TextOnDark.copy(alpha = 0.65f),
                    selectedTextColor = PlagOutColors.TextOnDark,
                    unselectedTextColor = PlagOutColors.TextOnDark.copy(alpha = 0.65f),
                    selectedIndicatorColor = PlagOutColors.Sun,
                    disabledIconColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledTextColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
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
