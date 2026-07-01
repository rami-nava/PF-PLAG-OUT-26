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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlantacionesPorTerreno(
    terrenoId: Int,
    plantacionesViewModel: PlantacionesViewModel,
    monitoreosViewModel: MonitoreosViewModel,
    terrenoViewModel: TerrenosViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val plantacionesState by plantacionesViewModel.state.collectAsState()
    val monitoreosState by monitoreosViewModel.state.collectAsState()
    val terreno = terrenoViewModel.state.collectAsState().value.terrenos.filter { t -> t.terreno_id == terrenoId  }[0]


    // Filtrar plantaciones de este terreno
    val plantacionesDelTerreno = plantacionesState.plantaciones.filter {
        it.terreno_id == terrenoId
    }

    androidx.compose.material3.Scaffold(
        containerColor = Color(0xFFF8F7F4),
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    navController.navigate("agregar_plantacion/$terrenoId")
                },
                containerColor = Color(0xFFE8941A),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Normal)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F7F4))
                .padding(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFF2d5016)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "🌱 Plantaciones (${plantacionesDelTerreno.size})",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d5016)
                    )
                }
            }

            if (plantacionesDelTerreno.isEmpty()) {
                item {
                    Text("Sin plantaciones", color = Color(0xFF718096))
                }
            } else {
                items(plantacionesDelTerreno) { plantacion ->
                    PlantacionConPlagasCard(
                        plantacion = plantacion,
                        monitoreos = monitoreosState.monitoreos.filter {
                            it.plantacion_id == plantacion.plantacion_id
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlantacionConPlagasCard(
    plantacion: PlantacionesResponse,
    monitoreos: List<MonitoreoResponse>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header plantación
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        plantacion.cultivo_nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2d5016)
                    )
                    Text(
                        plantacion.cultivo_nombre_cientifico,
                        fontSize = 11.sp,
                        color = Color(0xFF718096),
                        fontStyle = FontStyle.Italic
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (plantacion.activa) Color(0xFF38A169).copy(alpha = 0.15f)
                            else Color(0xFF718096).copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp)
                ) {
                    Text(
                        if (plantacion.activa) "✅" else "⏸️",
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fecha siembra
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Text(
                    "Siembra: ${(plantacion.fecha_siembra)}",
                    fontSize = 12.sp,
                    color = Color(0xFF718096)
                )
            }

            if (monitoreos.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Sin plagas monitoreadas",
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    modifier = Modifier
                        .background(Color(0xFFF8F7F4), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Plagas monitoreadas (${monitoreos.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2d5016),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    monitoreos.forEach { monitoreo ->
                        PlagaMiniCard(monitoreo)
                    }
                }
            }
        }
    }
}

@Composable
fun PlagaMiniCard(monitoreo: MonitoreoResponse) {
    val (colorAlerta, emojiAlerta) = when (monitoreo.nivel_alerta) {
        0 -> Pair(Color(0xFF38A169), "✅")
        1 -> Pair(Color(0xFFD69E2E), "⚠️")
        else -> Pair(Color(0xFFE53E3E), "🔴")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorAlerta.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, colorAlerta.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    monitoreo.plaga_nombre,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2d5016)
                )
                Text(
                    "${monitoreo.gdd_acumulado.toInt()}/${monitoreo.gdd_objetivo.toInt()} GDD",
                    fontSize = 11.sp,
                    color = colorAlerta
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                LinearProgressIndicator(
                    progress = {(monitoreo.progreso / 100f).coerceIn(0f, 1f)},
                    modifier = Modifier
                        .width(50.dp)
                        .height(6.dp),
                    color = colorAlerta,
                    trackColor = Color(0xFFE2E8F0)
                )
                Text(
                    " $emojiAlerta",
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}