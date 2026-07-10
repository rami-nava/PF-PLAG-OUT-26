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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale

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
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("agregar_plantacion/$terrenoId") },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
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
                            colors = listOf(PlagOutColors.Forest, PlagOutColors.Leaf)
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            terreno?.terreno_nombre ?: "Terreno",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Plantaciones",
                            color = PlagOutColors.TextOnDark,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // ── Contenido ───────────────────────────────────────────────────
            AnimatedContent(
                targetState = plantacionesDelTerreno.isEmpty(),
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "plantacionesContent",
                modifier = Modifier.weight(1f)
            ) { isEmpty ->
                if (isEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Grass, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No hay plantaciones", color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(plantacionesDelTerreno) { index, plantacion ->
                            StaggeredAppear(index = index) {
                                PlantacionModernCard(
                                    plantacion = plantacion,
                                    monitoreos = monitoreosState.monitoreos.filter {
                                        it.plantacion_id == plantacion.plantacion_id
                                    }
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

@Composable
fun PlantacionModernCard(
    plantacion: PlantacionesResponse,
    monitoreos: List<MonitoreoResponse>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
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
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        plantacion.cultivo_nombre_cientifico,
                        fontSize = 13.sp,
                        color = PlagOutColors.TextSecondary,
                        fontStyle = FontStyle.Italic
                    )
                }

                Surface(
                    color = if (plantacion.activa) PlagOutColors.Leaf.copy(alpha = 0.12f) else PlagOutColors.TextSecondary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (plantacion.activa) "ACTIVA" else "PAUSADA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plantacion.activa) PlagOutColors.Leaf else PlagOutColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(PlagOutColors.Cream, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Fecha de Siembra: ${plantacion.fecha_siembra}",
                    fontSize = 13.sp,
                    color = PlagOutColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Seccion Monitoreos
            Text(
                "Monitoreos Activos (${monitoreos.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )

            if (monitoreos.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Sin plagas bajo seguimiento",
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlagOutColors.Cream, RoundedCornerShape(12.dp))
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
        0 -> Triple(PlagOutColors.RiskOk, "Saludable", Icons.Default.CheckCircle)
        1 -> Triple(PlagOutColors.RiskWarn, "Atención", Icons.Default.WarningAmber)
        else -> Triple(PlagOutColors.RiskDanger, "Crítico", Icons.Default.ErrorOutline)
    }

    Surface(
        color = PlagOutColors.Cream,
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
                    color = PlagOutColors.TextMain
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
                    Icon(iconAlerta, contentDescription = labelAlerta, tint = colorAlerta, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${monitoreo.progreso.toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.Forest
                    )
                }
                Spacer(Modifier.height(4.dp))
                val animatedProgress by animateFloatAsState(
                    targetValue = (monitoreo.progreso / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                    label = "miniProgreso"
                )
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .width(60.dp)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = colorAlerta,
                    trackColor = PlagOutColors.Surface
                )
            }
        }
    }
}
