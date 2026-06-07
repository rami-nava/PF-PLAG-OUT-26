package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreosScreen(monitoreosViewModel: MonitoreosViewModel, plantacionesViewModel: PlantacionesViewModel,navController: NavHostController) {
    val state by monitoreosViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        monitoreosViewModel.getMonitoreos()
        plantacionesViewModel.getPlantaciones()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
            .padding(16.dp)


    ) {
        Text(
            "🌾 Mis Plagas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2d5016),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else if (state.monitoreos.isEmpty()) {
            Text("Sin monitoreos", color = Color(0xFF718096))
        } else {
            state.monitoreos.forEach { monitoreo ->
                MonitoreoCard(monitoreo, {navController.navigate("plantacion/${monitoreo.plantacion_id}")})
            }
        }
    }
}

@Composable
fun MonitoreoCard(
    monitoreo: MonitoreoResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        monitoreo.plaga_nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d5016)
                    )
                    Text(
                        "${monitoreo.terreno_nombre} - ${monitoreo.cultivo_nombre}",
                        fontSize = 12.sp,
                        color = Color(0xFF718096)
                    )
                }

                // Nivel de alerta
                val nivelColor = when (monitoreo.nivel_alerta) {
                    0 -> Color(0xFF38A169)  // Bajo
                    1 -> Color(0xFFD69E2E)  // Medio
                    else -> Color(0xFFE53E3E) // Alto
                }
                Box(
                    modifier = Modifier
                        .background(nivelColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        when (monitoreo.nivel_alerta) {
                            0 -> "Normal"
                            1 -> "Precacion"
                            else -> "Peligro"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = nivelColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // GDD Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("GDD Acumulado", fontSize = 11.sp, color = Color(0xFF718096))
                    Text(
                        "${monitoreo.gdd_acumulado.toInt()}/${monitoreo.gdd_objetivo.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d5016)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Diario", fontSize = 11.sp, color = Color(0xFF718096))
                    Text(
                        "+${monitoreo.gdd_diario.toInt()} GDD",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4a7c2c)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = (monitoreo.progreso / 100f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp),
                    color = Color(0xFF2d5016),
                    trackColor = Color(0xFFE2E8F0)
                )
                Text(
                    " ${monitoreo.progreso.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2d5016),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun MonitoreosPorPlantacion(
    plantacionId: Int,
    viewModel: MonitoreosViewModel,  // Mismo ViewModel
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    // Filtrar monitoreos de esta plantación
    val monitoreosFiltrados = state.monitoreos.filter {
        it.plantacion_id == plantacionId
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
            .padding(16.dp)
    ) {
        item {
            Button(onClick = onBack) { Text("← Volver") }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        monitoreosFiltrados.firstOrNull()?.terreno_nombre ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        monitoreosFiltrados.firstOrNull()?.cultivo_nombre ?: "",
                        fontSize = 14.sp
                    )
                }
            }
        }

        items(monitoreosFiltrados) { monitoreo ->
            MonitoreoCard(monitoreo) { }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerrenosScreen(
    terrenoViewModel: TerrenosViewModel,
    monitoreoViewModel: MonitoreosViewModel,
    navController: NavController
) {
    val state by terrenoViewModel.state.collectAsState()
    val monitoreosState by monitoreoViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        terrenoViewModel.getTerrenos()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
            .padding(16.dp)
    ) {
        item {
            Text(
                "📍 Mis Terrenos",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2d5016),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (state.isLoading) {
            item {
                CircularProgressIndicator()//modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        } else if (state.terrenos.isEmpty()) {
            item {
                Text("Sin terrenos", color = Color(0xFF718096))
            }
        } else {
            items(state.terrenos) { terreno ->
                TerrenoCard(terreno,monitoreosState.monitoreos) {
                    navController.navigate("terreno/${terreno.terreno_id}")
                }
            }
        }
    }
}

@Composable
fun TerrenoCard(
    terreno: TerrenoResponse,
    monitoreos: List<MonitoreoResponse>,  // Pasar desde pantalla
    onClick: () -> Unit
) {
    // Monitoreos de este terreno
    val monitoreosDelTerreno = monitoreos.filter { it.terreno_id == terreno.terreno_id }
    val alertaMaxima = monitoreosDelTerreno.maxOfOrNull { it.nivel_alerta } ?: 0

    val (colorAlerta, emojiAlerta, textAlerta) = when (alertaMaxima) {
        0 -> Triple(Color(0xFF38A169), "✅", "Normal")
        1 -> Triple(Color(0xFFD69E2E), "⚠️", "Precaucion")
        2 -> Triple(Color(0xFFD69E2E), "🔴️", "Peligro")
        else -> Triple(Color.Gray, "", "Sin Monitorear")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, colorAlerta.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con nombre y alerta
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        terreno.terreno_nombre,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d5016)
                    )
                    Text(
                        "📍 ${terreno.terreno_latitud.toInt()}°, ${terreno.terreno_longitud.toInt()}°",
                        fontSize = 11.sp,
                        color = Color(0xFF718096)
                    )
                }

                // Estado del terreno
                Box(
                    modifier = Modifier
                        .background(colorAlerta.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emojiAlerta, fontSize = 20.sp)
                        Text(
                            textAlerta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorAlerta
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estadísticas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F7F4), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Hectáreas
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌾", fontSize = 20.sp)
                    Text(
                        "${terreno.terreno_area.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d5016)
                    )
                    Text("ha", fontSize = 10.sp, color = Color(0xFF718096))
                }

                // Separator
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Monitoreos
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🐛", fontSize = 20.sp)
                    Text(
                        "${monitoreosDelTerreno.size}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d5016)
                    )
                    Text("plagas", fontSize = 10.sp, color = Color(0xFF718096))
                }
            }
        }
    }
}