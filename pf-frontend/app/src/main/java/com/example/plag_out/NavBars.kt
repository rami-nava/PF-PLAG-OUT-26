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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

data class BarItem(val nombre: String, val ruta: String, val icono: ImageVector)

@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar(
        containerColor = PlagOutColors.Forest,
        contentColor = PlagOutColors.TextOnDark
    ) {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

        val barItems = listOf(
            BarItem("Monitoreos", "monitoreos", Icons.Default.Science),
            BarItem("Terrenos", "terrenos", Icons.Default.Terrain)
        )

        barItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icono, contentDescription = item.nombre, modifier = Modifier.size(22.dp)) },
                label = { Text(item.nombre, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                selected = currentDestination == item.ruta,
                onClick = {
                    if (currentDestination != item.ruta) {
                        // Sin restoreState: cada tap en la barra debe llevar siempre al listado
                        // raíz de la pestaña, no restaurar la última pantalla visitada dentro de ella.
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

@Composable
fun TopBar(onPerfilClick: () -> Unit, onCerrarSesion: () -> Unit) {
    var menuAbierto by remember { mutableStateOf(false) }
    var confirmarCierre by remember { mutableStateOf(false) }

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

        Box(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) {
            IconButton(
                onClick = { menuAbierto = true },
                modifier = Modifier.testTag("btnMenuUsuario")
            ) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Cuenta",
                    tint = PlagOutColors.TextOnDark,
                    modifier = Modifier.size(28.dp)
                )
            }
            DropdownMenu(
                expanded = menuAbierto,
                onDismissRequest = { menuAbierto = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Mi perfil", fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PlagOutColors.Forest) },
                    onClick = {
                        menuAbierto = false
                        onPerfilClick()
                    },
                    modifier = Modifier.testTag("menuMiPerfil")
                )
                DropdownMenuItem(
                    text = { Text("Cerrar sesión", fontWeight = FontWeight.SemiBold, color = PlagOutColors.RiskDanger) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PlagOutColors.RiskDanger) },
                    onClick = {
                        menuAbierto = false
                        confirmarCierre = true
                    },
                    modifier = Modifier.testTag("menuCerrarSesion")
                )
            }
        }
    }

    if (confirmarCierre) {
        DialogoCerrarSesion(
            onConfirmar = {
                confirmarCierre = false
                onCerrarSesion()
            },
            onDismiss = { confirmarCierre = false }
        )
    }
}

/** Confirmación de cierre de sesión, compartida entre la TopBar y PerfilScreen. */
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
