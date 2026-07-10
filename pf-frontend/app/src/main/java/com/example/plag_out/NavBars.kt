package com.example.plag_out

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
                        navController.navigate(item.ruta) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
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
fun TopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.Forest)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "PLAG-OUT",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = PlagOutColors.TextOnDark,
            letterSpacing = 3.sp
        )
    }
}
