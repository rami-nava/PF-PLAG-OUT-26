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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// ─────────────────────────────────────────────────────────────
// Paleta local (reemplazar por PlagOutColors cuando esté unificado)
// ─────────────────────────────────────────────────────────────
private val BgScreen = Color(0xFFF8F7F4)
private val TextPrimary = Color(0xFF2D5016)
private val TextSecondary = Color(0xFF718096)
private val Accent = Color(0xFFE8941A)
private val RiskOk = Color(0xFF38A169)
private val RiskWarn = Color(0xFFD69E2E)
private val RiskDanger = Color(0xFFE53E3E)
private val RiskUnknown = Color(0xFFA0AEC0)
private val Divider = Color(0xFFE2E8F0)

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

    androidx.compose.material3.Scaffold(
        containerColor = BgScreen,
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(
                onClick = {
                    nuevoTerrenoViewModel.resetState()
                    navController.navigate("datos_terreno")
                },
                containerColor = Accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
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
            Text(
                text = "Mis Terrenos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TextPrimary)
                    }
                }
                state.terrenos.isEmpty() -> {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Todavía no cargaste terrenos", color = TextSecondary)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.terrenos) { terreno ->
                            TerrenoCard(terreno, monitoreosState.monitoreos) {
                                navController.navigate("terreno/${terreno.terreno_id}")
                            }
                        }
                    }
                }
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
        0 -> Triple(RiskOk, Icons.Default.CheckCircle, "Normal")
        1 -> Triple(RiskWarn, Icons.Default.Warning, "Precaución")
        2 -> Triple(RiskDanger, Icons.Default.Warning, "Peligro")
        else -> Triple(RiskUnknown, Icons.Default.CheckCircle, "Sin datos")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Divider)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Encabezado: nombre + estado ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = terreno.terreno_nombre,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${"%.2f".format(terreno.terreno_latitud)}, " +
                                    "${"%.2f".format(terreno.terreno_longitud)}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                EstadoBadge(colorRiesgo, iconoRiesgo, textoRiesgo)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Estadísticas ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgScreen, RoundedCornerShape(10.dp))
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EstadisticaItem(
                    icono = Icons.Default.Home,
                    valor = "${terreno.terreno_area.toInt()}",
                    etiqueta = "Hectáreas"
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Divider)
                )

                EstadisticaItem(
                    icono = Icons.Default.Lock,
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
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
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
                .background(TextPrimary.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(valor, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(etiqueta, fontSize = 10.sp, color = TextSecondary)
    }
}