package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale
import java.time.format.DateTimeFormatter

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
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val fabScale by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "fabScale"
            )
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("agregar_monitoreo") },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
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
                            colors = listOf(PlagOutColors.Forest, PlagOutColors.Leaf)
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Column {
                    Text(
                        "Estado de Cultivos",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Mis Monitoreos",
                        color = PlagOutColors.TextOnDark,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
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
                            icon = Icons.Default.Science,
                            modifier = Modifier.weight(1f)
                        )
                        val criticos = state.monitoreos.count { it.nivel_alerta == 2 }
                        HeaderStatItem(
                            label = "Críticos",
                            value = "$criticos",
                            icon = Icons.Default.WarningAmber,
                            modifier = Modifier.weight(1f),
                            highlight = criticos > 0
                        )
                    }
                }
            }

            // ── Lista de Monitoreos ─────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = when {
                        state.isLoading -> "loading"
                        state.monitoreos.isEmpty() -> "empty"
                        else -> "content"
                    },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "monitoreosContent"
                ) { target ->
                    when (target) {
                        "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PlagOutColors.Forest)
                        }
                        "empty" -> EmptyMonitoreosState()
                        else -> {
                            val sorted = state.monitoreos.sortedByDescending { it.nivel_alerta }
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(sorted) { index, monitoreo ->
                                    StaggeredAppear(index = index) {
                                        MonitoreoModernCard(monitoreo) {
                                            navController.navigate("plantacion/${monitoreo.plantacion_id}")
                                        }
                                    }
                                }
                                item { Spacer(Modifier.height(32.dp)) }
                            }
                        }
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        color = if (highlight) PlagOutColors.Sun.copy(alpha = 0.95f) else PlagOutColors.TextOnDark.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (highlight) PlagOutColors.ForestDark else PlagOutColors.TextOnDark,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    value,
                    color = if (highlight) PlagOutColors.ForestDark else PlagOutColors.TextOnDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    label,
                    color = if (highlight) PlagOutColors.ForestDark.copy(alpha = 0.7f) else PlagOutColors.TextOnDark.copy(alpha = 0.7f),
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
    val (nivelColor, nivelLabel, nivelIcon) = when (monitoreo.nivel_alerta) {
        0 -> Triple(PlagOutColors.RiskOk, "Saludable", Icons.Default.CheckCircle)
        1 -> Triple(PlagOutColors.RiskWarn, "Atención", Icons.Default.WarningAmber)
        else -> Triple(PlagOutColors.RiskDanger, "Crítico", Icons.Default.ErrorOutline)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
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
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "${monitoreo.terreno_nombre} • ${monitoreo.cultivo_nombre}",
                        fontSize = 13.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }

                Surface(
                    color = nivelColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(nivelIcon, contentDescription = null, tint = nivelColor, modifier = Modifier.size(14.dp))
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
                    valueColor = PlagOutColors.Leaf
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
                        color = PlagOutColors.TextSecondary
                    )
                    Text(
                        "${monitoreo.progreso.toInt()}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.Forest
                    )
                }
                Spacer(Modifier.height(8.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = (monitoreo.progreso / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    label = "progresoCiclo"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = PlagOutColors.Forest,
                    trackColor = PlagOutColors.CreamDeep
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
                    color = PlagOutColors.TextSecondary
                )
                Text(
                    "Ver detalles ➜",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.Leaf
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
    valueColor: Color = PlagOutColors.Forest
) {
    Column(
        modifier = modifier
            .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontSize = 11.sp, color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Text(target, fontSize = 13.sp, color = PlagOutColors.TextSecondary, modifier = Modifier.padding(bottom = 2.dp))
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
            color = PlagOutColors.Leaf.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Science,
                    contentDescription = null,
                    tint = PlagOutColors.Leaf,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("No hay monitoreos activos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
        Text(
            "Inicia un nuevo monitoreo para realizar el seguimiento de plagas.",
            textAlign = TextAlign.Center,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
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

    Column(modifier = Modifier.fillMaxSize().background(PlagOutColors.Cream)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf))
                )
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PlagOutColors.TextOnDark)
                }
                Text("Detalle de Monitoreo", color = PlagOutColors.TextOnDark, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (monitoreosFiltrados.isEmpty()) {
            EmptyMonitoreosState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(monitoreosFiltrados) { index, monitoreo ->
                    StaggeredAppear(index = index) {
                        MonitoreoModernCard(monitoreo) { }
                    }
                }
            }
        }
    }
}
