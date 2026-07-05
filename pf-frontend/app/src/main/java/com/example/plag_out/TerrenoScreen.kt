package com.example.plag_out


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ─── Paleta Plag-Out ─────────────────────────────────────────────────────────
private val ColorVerde        = Color(0xFF2d5016)
private val ColorVerdeClaro   = Color(0xFF4a7c2a)
private val ColorVerdePastel  = Color(0xFFEAF3DE)
private val ColorNaranja      = Color(0xFFE8941A)
private val ColorNaranjaPastel= Color(0xFFFEF3E2)
private val ColorFondo        = Color(0xFFF8F7F4)
private val ColorCard         = Color.White
private val ColorTextoSub     = Color(0xFF718096)
private val ColorDivider      = Color(0xFFE2E8F0)
private val ColorFondoStat    = Color(0xFFF1EFE8)

// ─── Alerta ──────────────────────────────────────────────────────────────────
private data class AlertaStyle(
    val borderColor: Color,
    val badgeBg: Color,
    val badgeText: Color,
    val emoji: String,
    val label: String
)

private fun alertaStyle(nivel: Int): AlertaStyle = when (nivel) {
    2    -> AlertaStyle(Color(0xFFE53E3E).copy(alpha = 0.35f), Color(0xFFE53E3E).copy(alpha = 0.12f), Color(0xFFC53030), "🔴", "Peligro")
    1    -> AlertaStyle(Color(0xFFD69E2E).copy(alpha = 0.45f), Color(0xFFD69E2E).copy(alpha = 0.15f), Color(0xFFB7791F), "⚠️", "Precaución")
    0    -> AlertaStyle(Color(0xFF38A169).copy(alpha = 0.35f), Color(0xFF38A169).copy(alpha = 0.12f), Color(0xFF276749), "✅", "Normal")
    else -> AlertaStyle(Color(0xFF718096).copy(alpha = 0.30f), Color(0xFF718096).copy(alpha = 0.12f), Color(0xFF718096), "❓", "Sin datos")
}

// ─── Pantalla principal ───────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerrenosScreen(
    terrenoViewModel: TerrenosViewModel,
    monitoreoViewModel: MonitoreosViewModel,
    plantacionViewModel: PlantacionesViewModel,   // <-- nuevo
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    navController: NavController
) {
    val state            by terrenoViewModel.state.collectAsState()
    val monitoreosState  by monitoreoViewModel.state.collectAsState()
    val plantacionesState by plantacionViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        terrenoViewModel.getTerrenos()
        plantacionViewModel.getPlantaciones()
    }

    // Stats resumen
    val totalHa        = state.terrenos.sumOf { it.terreno_area.toDouble() }.toInt()
    val totalMonitoreos = monitoreosState.monitoreos.size
    val enPeligro      = state.terrenos.count { terreno ->
        (monitoreosState.monitoreos
            .filter { it.terreno_id == terreno.terreno_id }
            .maxOfOrNull { it.nivel_alerta } ?: -1) == 2
    }

    Scaffold(
        containerColor = ColorFondo,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    nuevoTerrenoViewModel.resetState()
                    navController.navigate("datos_terreno")
                },
                containerColor = ColorNaranja,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp),
                modifier       = Modifier.padding(bottom = 16.dp, end = 8.dp)
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Normal)
            }
        }
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────────
            Text(
                text       = "🌾 Mis Terrenos",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorVerde
            )
            Text(
                text     = "${state.terrenos.size} lote${if (state.terrenos.size != 1) "s" else ""} activo${if (state.terrenos.size != 1) "s" else ""}",
                fontSize = 12.sp,
                color    = ColorTextoSub,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // ── Pills de resumen ─────────────────────────────────────────────
            if (state.terrenos.isNotEmpty()) {
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryPill(emoji = "🗺️", value = "$totalHa",        label = "hectáreas",   modifier = Modifier.weight(1f))
                    SummaryPill(emoji = "🐛", value = "$totalMonitoreos", label = "monitoreos",  modifier = Modifier.weight(1f))
                    SummaryPill(emoji = "⚠️", value = "$enPeligro",       label = "en peligro",  modifier = Modifier.weight(1f))
                }
            }

            // ── Contenido ───────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    Box(
                        modifier        = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = ColorVerde)
                    }
                }

                state.terrenos.isEmpty() -> {
                    Box(
                        modifier        = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🌱", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text       = "Aún no tenés terrenos",
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color      = ColorVerde
                            )
                            Text(
                                text     = "Tocá + para agregar tu primer lote",
                                fontSize = 13.sp,
                                color    = ColorTextoSub,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                else -> {
                    // Separar terrenos en peligro del resto
                    val conAlerta  = state.terrenos.filter { t ->
                        (monitoreosState.monitoreos
                            .filter { it.terreno_id == t.terreno_id }
                            .maxOfOrNull { it.nivel_alerta } ?: -1) >= 1
                    }
                    val sinAlerta  = state.terrenos.filter { t ->
                        (monitoreosState.monitoreos
                            .filter { it.terreno_id == t.terreno_id }
                            .maxOfOrNull { it.nivel_alerta } ?: -1) < 1
                    }

                    LazyColumn(
                        modifier              = Modifier.weight(1f),
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        if (conAlerta.isNotEmpty()) {
                            item {
                                SectionLabel("Requieren atención")
                            }
                            items(conAlerta) { terreno ->
                                val monitoreos     = monitoreosState.monitoreos.filter { it.terreno_id == terreno.terreno_id }
                                val plantaciones   = plantacionesState.plantaciones.filter { it.terreno_id == terreno.terreno_id }
                                val nivelAlerta    = monitoreos.maxOfOrNull { it.nivel_alerta } ?: -1
                                TerrenoCard(
                                    terreno      = terreno,
                                    plantaciones = plantaciones,
                                    nivelAlerta  = nivelAlerta,
                                    onClick      = { navController.navigate("terreno/${terreno.terreno_id}") }
                                )
                            }
                        }

                        if (sinAlerta.isNotEmpty()) {
                            item {
                                SectionLabel(if (conAlerta.isEmpty()) "Tus lotes" else "Sin alertas")
                            }
                            items(sinAlerta) { terreno ->
                                val monitoreos     = monitoreosState.monitoreos.filter { it.terreno_id == terreno.terreno_id }
                                val plantaciones   = plantacionesState.plantaciones.filter { it.terreno_id == terreno.terreno_id }
                                val nivelAlerta    = monitoreos.maxOfOrNull { it.nivel_alerta } ?: -1
                                TerrenoCard(
                                    terreno      = terreno,
                                    plantaciones = plantaciones,
                                    nivelAlerta  = nivelAlerta,
                                    onClick      = { navController.navigate("terreno/${terreno.terreno_id}") }
                                )
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ─── Card de terreno ──────────────────────────────────────────────────────────
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerrenoCard(
    terreno: TerrenoResponse,
    plantaciones: List<PlantacionesResponse>,
    nivelAlerta: Int,
    onClick: () -> Unit
) {
    val estilo             = alertaStyle(nivelAlerta)
    val plantacionesActivas = plantaciones.filter { it.activa }
    val cultivoLabel       = when {
        plantacionesActivas.isEmpty() && plantaciones.isEmpty() -> null
        plantacionesActivas.isEmpty() -> "Sin plantaciones activas"
        plantacionesActivas.size == 1 -> plantacionesActivas.first().cultivo_nombre
        else -> "${plantacionesActivas.first().cultivo_nombre} +${plantacionesActivas.size - 1}"
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors    = CardDefaults.cardColors(containerColor = ColorCard),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border    = BorderStroke(1.5.dp, estilo.borderColor)
    ) {
        Column {
            // ── Header: nombre + badge ───────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = terreno.terreno_nombre,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = ColorVerde
                    )
                    Text(
                        text     = "📍 ${terreno.terreno_latitud.toInt()}°, ${terreno.terreno_longitud.toInt()}°",
                        fontSize = 11.sp,
                        color    = ColorTextoSub,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }

                // Badge de alerta
                Box(
                    modifier         = Modifier
                        .padding(start = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(estilo.badgeBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(estilo.emoji, fontSize = 18.sp, lineHeight = 20.sp)
                        Text(
                            text       = estilo.label,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color      = estilo.badgeText,
                            modifier   = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }

            // ── Separador ───────────────────────────────────────────────────
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 14.dp)
                    .height(1.dp)
                    .background(ColorFondoStat)
            )

            // ── Stats: hectáreas / plagas ────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Hectáreas
                StatBlock(
                    emoji       = "🌾",
                    iconBg      = ColorVerdePastel,
                    value       = "${terreno.terreno_area.toInt()}",
                    label       = "hectáreas",
                    modifier    = Modifier.weight(1f)
                )

                // Separador vertical
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(ColorDivider)
                )

                // Plagas
                StatBlock(
                    emoji       = "🐛",
                    iconBg      = ColorNaranjaPastel,
                    value       = "${plantacionesActivas.size}",
                    label       = "plantaciones",
                    modifier    = Modifier.weight(1f),
                    alignEnd    = true
                )
            }

            // ── Footer: cultivo + flecha ─────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .background(ColorFondoStat)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                if (cultivoLabel != null) {
                    Text(
                        text     = "🌱 $cultivoLabel",
                        fontSize = 11.sp,
                        color    = ColorTextoSub,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Text(
                        text      = "Sin plantaciones",
                        fontSize  = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color     = ColorTextoSub.copy(alpha = 0.6f),
                        modifier  = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text     = "Ver plantaciones",
                        fontSize = 11.sp,
                        color    = ColorNaranja,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint               = ColorNaranja,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Composables auxiliares ───────────────────────────────────────────────────

@Composable
private fun SummaryPill(
    emoji: String,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier  = modifier,
        colors    = CardDefaults.cardColors(containerColor = ColorCard),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border    = BorderStroke(1.dp, Color(0xFFEDE9E2))
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(emoji, fontSize = 18.sp)
            Column {
                Text(
                    text       = value,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = ColorVerde
                )
                Text(
                    text     = label,
                    fontSize = 10.sp,
                    color    = ColorTextoSub
                )
            }
        }
    }
}

@Composable
private fun StatBlock(
    emoji: String,
    iconBg: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Row(
        modifier              = modifier.padding(horizontal = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        if (alignEnd) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 10.dp)) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorVerde)
                Text(label, fontSize = 10.sp, color = ColorTextoSub)
            }
        }
        Box(
            modifier         = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        if (!alignEnd) {
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorVerde)
                Text(label, fontSize = 10.sp, color = ColorTextoSub)
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text.uppercase(),
        fontSize   = 11.sp,
        fontWeight = FontWeight.Bold,
        color      = Color(0xFFB0A99A),
        letterSpacing = 0.8.sp,
        modifier   = Modifier.padding(start = 2.dp, bottom = 6.dp, top = 4.dp)
    )
}