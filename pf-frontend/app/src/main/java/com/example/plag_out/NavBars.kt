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

data class BarItem(val nombre: String)//, val logoSeleccionado: Int, val logoNoSeleccionado: Int)
@Composable
fun BottomNavigationBar(navController: NavController){
    NavigationBar (containerColor = Color(0xFF2d5016),
        contentColor = Color.White,
        modifier = Modifier.height(70.dp)){
        val currentDestination = navController.currentBackStackEntryAsState().value?.destination?.route

        val barItems = arrayOf(
            BarItem("Monitoreos"),
            BarItem("Terrenos")
            )

        barItems.forEach { item ->
            NavigationBarItem(
                icon = { Text(item.nombre)},
                selected = currentDestination == item.nombre,
                onClick = {navController.navigate(item.nombre)},
                colors = NavigationBarItemColors(
                    selectedIconColor = Color.Gray,
                    unselectedIconColor = Color.White,
                    selectedTextColor = Color.Gray,
                    unselectedTextColor = Color.White,
                    selectedIndicatorColor = Color.Transparent,
                    disabledIconColor = Color.Transparent,
                    disabledTextColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun TopBar(){
        // TOP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2d5016))
                .padding(35.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "🌾 PLAG-OUT",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
}