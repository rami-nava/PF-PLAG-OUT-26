package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ─── Paleta de Colores Dinámica ──────────────────────────────────────────────
private val ColorPrimary      = Color(0xFF1B5E20)
private val ColorSecondary    = Color(0xFF43A047)
private val ColorAccent       = Color(0xFFFFB300)
private val ColorSurface      = Color(0xFFFFFFFF)
private val ColorBackground   = Color(0xFFF1F4F1)
private val ColorTextMain     = Color(0xFF1A231E)
private val ColorTextSecondary= Color(0xFF5F6D66)

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
    val terrenosState by terrenoViewModel.state.collectAsState()
    
    val terreno = terrenosState.terrenos.find { it.terreno_id == terrenoId }

    val plantacionesDelTerreno = plantacionesState.plantaciones.filter {
        it.terreno_id == terrenoId
    }

    Scaffold(
        containerColor = ColorBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("agregar_plantacion/$terrenoId") },
                containerColor = ColorPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nueva Plantación", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Header con Gradiente ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(ColorPrimary, ColorSecondary)
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            terreno?.terreno_nombre ?: "Terreno",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Plantaciones",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Contenido ───────────────────────────────────────────────────
            if (plantacionesDelTerreno.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🌱", fontSize = 60.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("No hay plantaciones", color = ColorTextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(plantacionesDelTerreno) { plantacion ->
                        PlantacionModernCard(
                            plantacion = plantacion,
                            monitoreos = monitoreosState.monitoreos.filter {
                                it.plantacion_id == plantacion.plantacion_id
                            }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun PlantacionModernCard(
    plantacion: PlantacionesResponse,
    monitoreos: List<MonitoreoResponse>
) {
    Surface(
        color = ColorSurface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plantacion.cultivo_nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextMain
                    )
                    Text(
                        plantacion.cultivo_nombre_cientifico,
                        fontSize = 13.sp,
                        color = ColorTextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                }

                Surface(
                    color = if (plantacion.activa) ColorSecondary.copy(alpha = 0.1f) else ColorTextSecondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (plantacion.activa) "ACTIVA" else "PAUSADA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plantacion.activa) ColorSecondary else ColorTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📅", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Fecha de Siembra: ${plantacion.fecha_siembra}",
                    fontSize = 13.sp,
                    color = ColorTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Seccion Monitoreos
            Text(
                "Monitoreos Activos (${monitoreos.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ColorTextMain
            )
            
            if (monitoreos.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Sin plagas bajo seguimiento",
                    fontSize = 12.sp,
                    color = ColorTextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorBackground, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    monitoreos.forEach { monitoreo ->
                        PlagaModernMiniCard(monitoreo)
                    }
                }
            }
        }
    }
}

@Composable
fun PlagaModernMiniCard(monitoreo: MonitoreoResponse) {
    val (colorAlerta, labelAlerta, iconAlerta) = when (monitoreo.nivel_alerta) {
        0 -> Triple(Color(0xFF388E3C), "Saludable", "🌿")
        1 -> Triple(Color(0xFFF57C00), "Atención", "⚠️")
        else -> Triple(Color(0xFFD32F2F), "Crítico", "🚨")
    }

    Surface(
        color = ColorBackground,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    monitoreo.plaga_nombre,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextMain
                )
                Text(
                    "${monitoreo.gdd_acumulado.toInt()} / ${monitoreo.gdd_objetivo.toInt()} GDD",
                    fontSize = 11.sp,
                    color = colorAlerta,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(iconAlerta, fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${monitoreo.progreso.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (monitoreo.progreso / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(60.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = colorAlerta,
                    trackColor = Color.White
                )
            }
        }
    }
}
