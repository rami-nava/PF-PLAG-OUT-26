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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale

// ─── Estilos de Alerta ───────────────────────────────────────────────────────
private data class AlertLevel(
    val color: Color,
    val label: String,
    val icon: ImageVector
)

private fun getAlertLevel(nivel: Int): AlertLevel = when (nivel) {
    2 -> AlertLevel(PlagOutColors.RiskDanger, "Crítico", Icons.Default.ErrorOutline)
    1 -> AlertLevel(PlagOutColors.RiskWarn, "Atención", Icons.Default.WarningAmber)
    0 -> AlertLevel(PlagOutColors.RiskOk, "Saludable", Icons.Default.CheckCircle)
    else -> AlertLevel(PlagOutColors.RiskUnknown, "Sin datos", Icons.Default.HelpOutline)
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
                onClick = {
                    nuevoTerrenoViewModel.resetState()
                    navController.navigate("datos_terreno")
                },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
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
                            colors = listOf(PlagOutColors.Forest, PlagOutColors.Leaf)
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
                                color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Mis Terrenos",
                                color = PlagOutColors.TextOnDark,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Surface(
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Terrain, contentDescription = null, tint = PlagOutColors.TextOnDark, modifier = Modifier.size(24.dp))
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
                            icon = Icons.Default.LocationOn,
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatItem(
                            label = "Lotes",
                            value = "${state.terrenos.size}",
                            icon = Icons.Default.Terrain,
                            modifier = Modifier.weight(1f)
                        )
                        QuickStatItem(
                            label = "Críticos",
                            value = "$criticalCount",
                            icon = Icons.Default.WarningAmber,
                            modifier = Modifier.weight(1f),
                            highlight = criticalCount > 0
                        )
                    }
                }
            }

            // ── Lista de Terrenos ───────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = when {
                        state.isLoading -> "loading"
                        state.terrenos.isEmpty() -> "empty"
                        else -> "content"
                    },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                    label = "terrenoContent"
                ) { target ->
                    when (target) {
                        "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PlagOutColors.Forest)
                        }
                        "empty" -> EmptyState()
                        else -> {
                            val sortedTerrenos = state.terrenos.sortedByDescending { t ->
                                monitoreosState.monitoreos
                                    .filter { it.terreno_id == t.terreno_id }
                                    .maxOfOrNull { it.nivel_alerta } ?: -1
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(sortedTerrenos) { index, terreno ->
                                    val alerts = monitoreosState.monitoreos.filter { it.terreno_id == terreno.terreno_id }
                                    val plants = plantacionesState.plantaciones.filter { it.terreno_id == terreno.terreno_id && it.activa }
                                    val maxAlert = alerts.maxOfOrNull { it.nivel_alerta } ?: -1

                                    StaggeredAppear(index = index) {
                                        TerrenoModernCard(
                                            terreno = terreno,
                                            maxAlert = maxAlert,
                                            activePlants = plants,
                                            onClick = { navController.navigate("terreno/${terreno.terreno_id}") }
                                        )
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
fun QuickStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        color = if (highlight) PlagOutColors.Sun else PlagOutColors.TextOnDark.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (highlight) PlagOutColors.ForestDark else PlagOutColors.TextOnDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(2.dp))
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

@Composable
fun TerrenoModernCard(
    terreno: TerrenoResponse,
    maxAlert: Int,
    activePlants: List<PlantacionesResponse>,
    onClick: () -> Unit
) {
    val alertInfo = getAlertLevel(maxAlert)
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
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "Área: ${terreno.terreno_area.toInt()} hectáreas",
                        fontSize = 13.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }

                // Alert Badge
                Surface(
                    color = alertInfo.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(alertInfo.icon, contentDescription = null, tint = alertInfo.color, modifier = Modifier.size(14.dp))
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
                    icon = Icons.Default.Grass,
                    text = when {
                        activePlants.isEmpty() -> "Sin plantaciones"
                        activePlants.size == 1 -> "1 plantación activa"
                        else -> "${activePlants.size} plantaciones activas"
                    },
                    modifier = Modifier.weight(1f)
                )
                InfoTag(
                    icon = Icons.Default.LocationOn,
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
                    color = PlagOutColors.Leaf,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = PlagOutColors.Leaf,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun InfoTag(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(PlagOutColors.Cream, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = PlagOutColors.TextSecondary,
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
            color = PlagOutColors.Leaf.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Terrain,
                    contentDescription = null,
                    tint = PlagOutColors.Leaf,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Tu campo está vacío",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextMain
        )
        Text(
            "Comienza registrando tu primer lote de tierra para monitorear plagas y cultivos.",
            textAlign = TextAlign.Center,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
