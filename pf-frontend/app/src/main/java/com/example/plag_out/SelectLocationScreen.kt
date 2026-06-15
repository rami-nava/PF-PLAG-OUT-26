package com.example.plag_out

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SelectLocationScreen(
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val state by nuevoTerrenoViewModel.state.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(1) }
    val tabs = listOf("🗺️ Mapa", "🔢 Coordenadas")

    // Estados locales para los campos de texto
    var latInput by remember { mutableStateOf(state.latitud?.toString() ?: "") }
    var lonInput by remember { mutableStateOf(state.longitud?.toString() ?: "") }

    // Sincronización del tipeo manual hacia el ViewModel
    LaunchedEffect(latInput, lonInput) {
        val lat = latInput.toDoubleOrNull()
        val lon = lonInput.toDoubleOrNull()
        nuevoTerrenoViewModel.actualizarUbicacion(lat, lon)
    }

    // Rangos y validación de Argentina
    val latValida = state.latitud?.let { it in -55.0..-21.8 } ?: false
    val lonValida = state.longitud?.let { it in -73.6..-53.6 } ?: false
    val isValid = latValida && lonValida
    val showError = (state.latitud != null || state.longitud != null) && !isValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
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
                text = "Ubicación del Lote",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2d5016)
            )
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.White,
            contentColor = Color(0xFF2d5016),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedTabIndex == 0) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFF718096).copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🗺️ [Módulo de Google Maps en Standby]", fontWeight = FontWeight.Bold, color = Color(0xFF2d5016))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Por favor, utilice la pestaña 'Coordenadas' para ingresar datos.", fontSize = 12.sp, color = Color(0xFF718096))
                        }
                    }
                }
            } else {
                // Pestaña coordenadas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = latInput,
                            onValueChange = { newValue -> latInput = newValue },
                            label = { Text("Latitud (Ej: -34.60)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                if (latInput.isNotEmpty()) {
                                    IconButton(onClick = { latInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = lonInput,
                            onValueChange = { newValue -> lonInput = newValue },
                            label = { Text("Longitud (Ej: -58.38)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                if (lonInput.isNotEmpty()) {
                                    IconButton(onClick = { lonInput = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alerta error
        if (showError) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color(0xFFE53E3E).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "⚠️ Ubicación fuera de la República Argentina.\nLatitud permitida: -55.0 a -21.8\nLongitud permitida: -73.6 a -53.6",
                    color = Color(0xFFE53E3E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cultivo: ${state.cultivoSeleccionado ?: "Ninguno"}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2d5016)
                )
                Text(
                    text = "📍 ${state.latitud ?: "-"}°, ${state.longitud ?: "-"}°",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF718096)
                )
            }
        }

        Button(
            onClick = onConfirm,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8941A),
                disabledContainerColor = Color(0xFFE8941A).copy(alpha = 0.5f),
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = "✅ Confirmar ubicación",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}