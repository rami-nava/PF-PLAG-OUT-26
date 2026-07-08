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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.navigation.NavHostController
import java.time.format.DateTimeFormatter

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
fun MonitoreosScreen(
    monitoreosViewModel: MonitoreosViewModel,
    plantacionesViewModel: PlantacionesViewModel,
    navController: NavHostController
) {
    val state by monitoreosViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        monitoreosViewModel.getMonitoreos()
        plantacionesViewModel.getPlantaciones()
    }

    Scaffold(
        containerColor = ColorBackground,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("agregar_monitoreo") },
                containerColor = ColorPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Monitoreo", fontWeight = FontWeight.SemiBold)
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
                    Text(
                        "Estado de Cultivos",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Mis Monitoreos",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Resumen rápido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeaderStatItem(
                            label = "Activos",
                            value = "${state.monitoreos.size}",
                            icon = "🔬",
                            modifier = Modifier.weight(1f)
                        )
                        val criticos = state.monitoreos.count { it.nivel_alerta == 2 }
                        HeaderStatItem(
                            label = "Críticos",
                            value = "$criticos",
                            icon = "🚨",
                            modifier = Modifier.weight(1f),
                            highlight = criticos > 0
                        )
                    }
                }
            }

            // ── Lista de Monitoreos ─────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorPrimary)
                    }
                } else if (state.monitoreos.isEmpty()) {
                    EmptyMonitoreosState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.monitoreos.sortedByDescending { it.nivel_alerta }) { monitoreo ->
                            MonitoreoModernCard(monitoreo) {
                                navController.navigate("plantacion/${monitoreo.plantacion_id}")
                            }
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderStatItem(
    label: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        color = if (highlight) ColorAccent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
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
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreoModernCard(
    monitoreo: MonitoreoResponse,
    onClick: () -> Unit
) {
    val nivelColor = when (monitoreo.nivel_alerta) {
        0 -> Color(0xFF388E3C)
        1 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }
    val nivelLabel = when (monitoreo.nivel_alerta) {
        0 -> "Saludable"
        1 -> "Atención"
        else -> "Crítico"
    }
    val nivelIcon = when (monitoreo.nivel_alerta) {
        0 -> "🌿"
        1 -> "⚠️"
        else -> "🚨"
    }

    Surface(
        onClick = onClick,
        color = ColorSurface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header del Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        monitoreo.plaga_nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextMain
                    )
                    Text(
                        "${monitoreo.terreno_nombre} • ${monitoreo.cultivo_nombre}",
                        fontSize = 13.sp,
                        color = ColorTextSecondary
                    )
                }
                
                Surface(
                    color = nivelColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(nivelIcon, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            nivelLabel,
                            color = nivelColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // GDD Info Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoBlock(
                    label = "GDD Acumulado",
                    value = "${monitoreo.gdd_acumulado.toInt()}",
                    target = "/${monitoreo.gdd_objetivo.toInt()}",
                    modifier = Modifier.weight(1f)
                )
                InfoBlock(
                    label = "Crecimiento Diario",
                    value = "+${monitoreo.gdd_diario.toInt()}",
                    target = " GDD",
                    modifier = Modifier.weight(1f),
                    valueColor = ColorSecondary
                )
            }

            Spacer(Modifier.height(20.dp))

            // Barra de Progreso
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Progreso del ciclo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ColorTextSecondary
                    )
                    Text(
                        "${monitoreo.progreso.toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPrimary
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (monitoreo.progreso / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = ColorPrimary,
                    trackColor = ColorBackground
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Footer del Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Actualización: ${monitoreo.fecha_actualizacion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                    fontSize = 11.sp,
                    color = ColorTextSecondary
                )
                Text(
                    "Ver detalles ➜",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSecondary
                )
            }
        }
    }
}

@Composable
fun InfoBlock(
    label: String,
    value: String,
    target: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ColorPrimary
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = ColorTextSecondary, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(target, fontSize = 13.sp, color = ColorTextSecondary, modifier = Modifier.padding(bottom = 2.dp))
        }
    }
}

@Composable
fun EmptyMonitoreosState() {
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
                Text("🔬", fontSize = 60.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("No hay monitoreos activos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ColorTextMain)
        Text("Inicia un nuevo monitoreo para realizar el seguimiento de plagas.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = ColorTextSecondary, modifier = Modifier.padding(top = 8.dp))
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonitoreosPorPlantacion(
    plantacionId: Int,
    viewModel: MonitoreosViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val monitoreosFiltrados = state.monitoreos.filter { it.plantacion_id == plantacionId }

    Column(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPrimary)
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Text("Detalle de Monitoreo", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(monitoreosFiltrados) { monitoreo ->
                MonitoreoModernCard(monitoreo) { }
            }
        }
    }
}
