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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerrenosScreen(
    terrenoViewModel: TerrenosViewModel,
    monitoreoViewModel: MonitoreosViewModel,
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    navController: NavController
) {
    val state by terrenoViewModel.state.collectAsState()
    val monitoreosState by monitoreoViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        terrenoViewModel.getTerrenos()
        state.isLoading = false
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    nuevoTerrenoViewModel.resetState()
                    navController.navigate("datos_terreno")
                },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Terreno", fontWeight = FontWeight.SemiBold)
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
                        "Gestión de Campo",
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

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TerrenoHeaderStat(
                            label = "Terrenos",
                            value = "${state.terrenos.size}",
                            icon = Icons.Default.Terrain,
                            modifier = Modifier.weight(1f)
                        )
                        val totalHectareas = state.terrenos.sumOf { it.terreno_area.toDouble() }.toInt()
                        TerrenoHeaderStat(
                            label = "Hectáreas",
                            value = "$totalHectareas",
                            icon = Icons.Default.SquareFoot,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = when {
                    state.isLoading -> "loading"
                    state.terrenos.isEmpty() -> "empty"
                    else -> "content"
                },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "terrenosContent",
                modifier = Modifier.weight(1f)
            ) { target ->
                when (target) {
                    "loading" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PlagOutColors.Forest)
                    }
                    "empty" -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Terrain, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(56.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Todavía no cargaste terrenos", color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium)
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(state.terrenos) { index, terreno ->
                                StaggeredAppear(index = index) {
                                    TerrenoCard(terreno, monitoreosState.monitoreos) {
                                        navController.navigate("terreno/${terreno.terreno_id}")
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

@Composable
private fun TerrenoHeaderStat(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = PlagOutColors.TextOnDark.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = PlagOutColors.TextOnDark, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, color = PlagOutColors.TextOnDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(label, color = PlagOutColors.TextOnDark.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TerrenoCard(
    terreno: TerrenoResponse,
    monitoreos: List<MonitoreoResponse>,
    onClick: () -> Unit
) {
    val monitoreosDelTerreno = monitoreos.filter { it.terreno_id == terreno.terreno_id }
    val alertaMaxima = monitoreosDelTerreno.maxOfOrNull { it.nivel_alerta } ?: 0

    val (colorRiesgo, iconoRiesgo, textoRiesgo) = when (alertaMaxima) {
        0 -> Triple(PlagOutColors.RiskOk, Icons.Default.CheckCircle, "Normal")
        1 -> Triple(PlagOutColors.RiskWarn, Icons.Default.Warning, "Precaución")
        2 -> Triple(PlagOutColors.RiskDanger, Icons.Default.Warning, "Peligro")
        else -> Triple(PlagOutColors.RiskUnknown, Icons.Default.CheckCircle, "Sin datos")
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
            // ── Encabezado: nombre + estado ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = terreno.terreno_nombre,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = PlagOutColors.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${"%.2f".format(terreno.terreno_latitud)}, " +
                                    "${"%.2f".format(terreno.terreno_longitud)}",
                            fontSize = 12.sp,
                            color = PlagOutColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                EstadoBadge(colorRiesgo, iconoRiesgo, textoRiesgo)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Estadísticas ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EstadisticaItem(
                    icono = Icons.Default.SquareFoot,
                    valor = "${terreno.terreno_area.toInt()}",
                    etiqueta = "Hectáreas"
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(PlagOutColors.Divider)
                )

                EstadisticaItem(
                    icono = Icons.Default.Science,
                    valor = "${monitoreosDelTerreno.size}",
                    etiqueta = "Monitoreos"
                )
            }
        }
    }
}

@Composable
private fun EstadoBadge(color: Color, icono: ImageVector, texto: String) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = texto,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun EstadisticaItem(icono: ImageVector, valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(PlagOutColors.Forest.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = PlagOutColors.Forest,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(valor, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
        Text(etiqueta, fontSize = 10.sp, color = PlagOutColors.TextSecondary)
    }
}
