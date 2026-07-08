package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

// ─── Estilos de Alerta ───────────────────────────────────────────────────────
private data class AlertLevel(
    val color: Color,
    val label: String,
    val icon: String
)

private fun getAlertLevel(nivel: Int): AlertLevel = when (nivel) {
    2    -> AlertLevel(Color(0xFFD32F2F), "Crítico", "🚨")
    1    -> AlertLevel(Color(0xFFF57C00), "Atención", "⚠️")
    0    -> AlertLevel(Color(0xFF388E3C), "Saludable", "🌿")
    else -> AlertLevel(Color(0xFF78909C), "Sin datos", "⚪")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerrenoScreen(
    terrenoViewModel: TerrenosViewModel,
    monitoreoViewModel: MonitoreosViewModel,
    plantacionViewModel: PlantacionesViewModel,
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    navController: NavController
) {
    val state by terrenoViewModel.state.collectAsState()
    val monitoreosState by monitoreoViewModel.state.collectAsState()
    val plantacionesState by plantacionViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        terrenoViewModel.getTerrenos()
        plantacionViewModel.getPlantaciones()
    }

    val totalArea = state.terrenos.sumOf { it.terreno_area.toDouble() }
    val criticalCount = state.terrenos.count { t ->
        monitoreosState.monitoreos.any { it.terreno_id == t.terreno_id && it.nivel_alerta == 2 }
    }

    Scaffold(
        containerColor = ColorBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    nuevoTerrenoViewModel.resetState()
                    navController.navigate("datos_terreno")
                },
                containerColor = ColorPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Lote", fontWeight = FontWeight.SemiBold)
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
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Panel de Control",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Mis Terrenos",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🚜", fontSize = 24.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Quick Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickStatItem(
                            label = "Área Total",
                            value = "${totalArea.toInt()} ha",
                            icon = "📍",
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatItem(
                            label = "Lotes",
                            value = "${state.terrenos.size}",
                            icon = "📋",
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatItem(
                            label = "Críticos",
                            value = "$criticalCount",
                            icon = "🚨",
                            modifier = Modifier.weight(1f),
                            highlight = criticalCount > 0
                        )
                    }
                }
            }

            // ── Lista de Terrenos ───────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ColorPrimary)
                        }
                    }
                    state.terrenos.isEmpty() -> {
                        EmptyState()
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val sortedTerrenos = state.terrenos.sortedByDescending { t ->
                                monitoreosState.monitoreos
                                    .filter { it.terreno_id == t.terreno_id }
                                    .maxOfOrNull { it.nivel_alerta } ?: -1
                            }

                            items(sortedTerrenos) { terreno ->
                                val alerts = monitoreosState.monitoreos.filter { it.terreno_id == terreno.terreno_id }
                                val plants = plantacionesState.plantaciones.filter { it.terreno_id == terreno.terreno_id && it.activa }
                                val maxAlert = alerts.maxOfOrNull { it.nivel_alerta } ?: -1

                                TerrenoModernCard(
                                    terreno = terreno,
                                    maxAlert = maxAlert,
                                    activePlants = plants,
                                    onClick = { navController.navigate("terreno/${terreno.terreno_id}") }
                                )
                            }
                            item { Spacer(Modifier.height(32.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatItem(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        color = if (highlight) ColorAccent else Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Text(
                value,
                color = if (highlight) ColorPrimary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                label,
                color = if (highlight) ColorPrimary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TerrenoModernCard(
    terreno: TerrenoResponse,
    maxAlert: Int,
    activePlants: List<PlantacionesResponse>,
    onClick: () -> Unit
) {
    val alertInfo = getAlertLevel(maxAlert)
    
    Surface(
        onClick = onClick,
        color = ColorSurface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        terreno.terreno_nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextMain
                    )
                    Text(
                        "Area: ${terreno.terreno_area.toInt()} hectáreas",
                        fontSize = 13.sp,
                        color = ColorTextSecondary
                    )
                }
                
                // Alert Badge
                Surface(
                    color = alertInfo.color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(alertInfo.icon, fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            alertInfo.label,
                            color = alertInfo.color,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Info Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoTag(
                    icon = "🌱",
                    text = when {
                        activePlants.isEmpty() -> "Sin plantaciones"
                        activePlants.size == 1 -> "1 plantación activa"
                        else -> "${activePlants.size} plantaciones activas"
                    },
                    modifier = Modifier.weight(1f)
                )
                InfoTag(
                    icon = "📍",
                    text = "${terreno.terreno_latitud.toInt()}, ${terreno.terreno_longitud.toInt()}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            // Footer Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Ver detalles técnicos",
                    color = ColorSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ColorSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun InfoTag(icon: String, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(ColorBackground, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = ColorTextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = ColorSecondary.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("🚜", fontSize = 60.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Tu campo está vacío",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextMain
        )
        Text(
            "Comienza registrando tu primer lote de tierra para monitorear plagas y cultivos.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = ColorTextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
