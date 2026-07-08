package com.example.plag_out

import androidx.compose.material3.Text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

// ─── Colores Base Coincidentes ──────────────────────────────────────────────
private val ColorPrimary = Color(0xFF1B5E20)

data class BarItem(val nombre: String, val icono: String)

@Composable
fun BottomNavigationBar(navController: NavController){
    NavigationBar (
        containerColor = ColorPrimary,
        contentColor = Color.White,
        modifier = Modifier.height(65.dp)
    ) {
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

        val barItems = arrayOf(
            BarItem("Monitoreos", "🔬"),
            BarItem("Terrenos", "🚜")
        )

        barItems.forEach { item ->
            NavigationBarItem(
                icon = { Text(item.icono, fontSize = 20.sp) },
                label = { Text(item.nombre, fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                selected = currentDestination == item.nombre,
                onClick = { navController.navigate(item.nombre) },
                colors = NavigationBarItemColors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    selectedIndicatorColor = Color.White.copy(alpha = 0.15f),
                    disabledIconColor = Color.Transparent,
                    disabledTextColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun TopBar() {
    // TOP BAR sin padding vertical excesivo para evitar separaciones visuales
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPrimary)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "PLAG-OUT",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = 2.sp
        )
    }
}
