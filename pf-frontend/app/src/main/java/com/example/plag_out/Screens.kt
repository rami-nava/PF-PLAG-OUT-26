package com.example.plag_out

import android.app.DatePickerDialog
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.IconButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GDDApp(viewModel: GDDViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val state = viewModel.state.collectAsState(initial = AppState()).value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2d5016))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌾 PLAG-OUT", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Predictor de desarrollo de plagas", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
        }

        // Tabs
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Simulador") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Configurar") }
            )
        }

        // Content
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> SimulatorScreen(state, viewModel)
                1 -> ConfigScreen(state, viewModel, {newTab -> selectedTab = newTab})
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SimulatorScreen(state: AppState, viewModel: GDDViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mensajes
        if (state.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE53E3E).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    state.errorMessage!!,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFFE53E3E),
                    fontSize = 13.sp
                )
            }
        }

        if (state.successMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF38A169).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    state.successMessage!!,
                    modifier = Modifier.padding(12.dp),
                    color = Color(0xFF38A169),
                    fontSize = 13.sp
                )
            }
        }

        // Estado actual
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estado Actual", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF718096))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text(
                            "${state.currentGDD}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2d5016)
                        )
                        Text("GDD Actual", fontSize = 12.sp, color = Color(0xFF718096))
                    }
                    Column {
                        Text(
                            formatDate(state.currentDate),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF718096)
                        )
                        Text("FECHA", fontSize = 12.sp, color = Color(0xFF718096))
                    }
                }
            }
        }

        // Progreso
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Progreso hacia Objetivo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    val progress = (state.currentGDD.toFloat() / state.targetGDD) * 100
                    Text("${progress.toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2d5016))
                }
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = (state.currentGDD.toFloat() / state.targetGDD).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    color = Color(0xFF2d5016),
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${state.currentGDD} GDD", fontSize = 12.sp, color = Color(0xFF718096))
                    Text("/ ${state.targetGDD} GDD", fontSize = 12.sp, color = Color(0xFF718096))
                }
            }
        }

        // Clima
        if (state.lastResponse != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4a7c2c).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Clima Hoy:", fontWeight = FontWeight.Bold, color = Color(0xFF2d5016))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Temp Prom: ${state.lastResponse!!.avgTemp.toInt()}°C | GDD Ganados: ${state.lastResponse!!.gddGained.toInt()}",
                        fontSize = 13.sp,
                        color = Color(0xFF2d5016)
                    )
                }
            }
        }

        // Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow("Temp Base (Tbase):", "${state.baseTemp}°C")
                InfoRow("GDD Objetivo:", "${state.targetGDD} GDD")
                InfoRow("Cultivo/Plaga:", state.cropName)
            }
        }

        // Botones
        Button(
            onClick = { viewModel.simulateDay() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8941A)),
            enabled = !state.isLoading
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.height(24.dp))
            } else {
                Text("➕ Simular 1 Día", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = { viewModel.resetSimulation() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
        ) {
            Text("🔄 Resetear", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2d5016))
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ConfigScreen(state: AppState, viewModel: GDDViewModel, onTabChange: (Int) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var isDatePickerForStart by remember { mutableStateOf(true) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // UBICACIÓN
        Text(
            "📍 Ubicación",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = state.latitude.toString(),
            onValueChange = {
                viewModel.updateConfig(latitude = it.toDoubleOrNull() ?: 0.0)
            },
            label = { Text("Latitud") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (state.latitude != 0.0) {
                    IconButton(onClick = { viewModel.updateConfig(latitude = -34.6037) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )

        OutlinedTextField(
            value = state.longitude.toString(),
            onValueChange = {
                viewModel.updateConfig(longitude = it.toDoubleOrNull() ?: 0.0)
            },
            label = { Text("Longitud") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (state.longitude != -58.3816) {
                    IconButton(onClick = { viewModel.updateConfig(longitude = -58.3816) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // FECHAS
        Text(
            "📅 Fechas",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = state.startDate,
            onValueChange = {},
            label = { Text("Fecha Inicio") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showDatePicker = true
                },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = {
                    showDatePickerDialog(context, LocalDate.parse(state.startDate)) { newDate ->
                        viewModel.updateConfig(startDate = newDate)
                    }
                }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Seleccionar fecha")
                }
            }
        )





        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // PARÁMETROS GDD
        Text(
            "🌡️ Parámetros GDD",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = if (state.baseTemp == 0.0) "" else state.baseTemp.toString(),
            onValueChange = {
                viewModel.updateConfig(baseTemp = it.toDoubleOrNull() ?: 0.0)
            },
            label = { Text("Temperatura Base (Tbase)") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (state.baseTemp != 0.0) {
                    IconButton(onClick = { viewModel.updateConfig(baseTemp = 10.0) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )

        OutlinedTextField(
            value = state.initialGDD.toString(),
            onValueChange = {
                viewModel.updateConfig(initialGDD = it.toIntOrNull() ?: 0)
            },
            label = { Text("GDD Inicial") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (state.initialGDD != 0) {
                    IconButton(onClick = { viewModel.updateConfig(initialGDD = 0) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )

        OutlinedTextField(
            value = if (state.targetGDD == 0) "" else state.targetGDD.toString(),
            onValueChange = {
                viewModel.updateConfig(targetGDD = it.toIntOrNull() ?: 0)
            },
            label = { Text("GDD Objetivo") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (state.targetGDD != 500) {
                    IconButton(onClick = { viewModel.updateConfig(targetGDD = 500) }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // CULTIVO Y NOTAS
        Text(
            "🌾 Cultivo y Notas",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = state.cropName,
            onValueChange = { viewModel.updateConfig(cropName = it) },
            label = { Text("Cultivo / Plaga") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (state.cropName.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateConfig(cropName = "") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )

        OutlinedTextField(
            value = state.notes,
            onValueChange = { viewModel.updateConfig(notes = it) },
            label = { Text("Notas") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            maxLines = 3,
            trailingIcon = {
                if (state.notes.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateConfig(notes = "") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                    }
                }
            }
        )

        Button(
            onClick = {
                viewModel.resetSimulation()
                onTabChange(0)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8941A))
        ) {
            Text("💾 Guardar Configuración", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Valores típicos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2d5016).copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Valores Típicos por Plaga:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mosca Blanca: Tbase=10°C, GDD=300-500", fontSize = 12.sp)
                Text("Pulgón: Tbase=7°C, GDD=200-400", fontSize = 12.sp)
                Text("Trips: Tbase=8°C, GDD=250-450", fontSize = 12.sp)
                Text("Arañita Roja: Tbase=11°C, GDD=350-600", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Composable para el selector de fechas
@Composable
fun DatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Fecha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Año
                OutlinedTextField(
                    value = year.toString(),
                    onValueChange = { year = it.toIntOrNull() ?: year },
                    label = { Text("Año") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Mes
                OutlinedTextField(
                    value = (month + 1).toString(),
                    onValueChange = { m ->
                        val newMonth = m.toIntOrNull() ?: (month + 1)
                        if (newMonth in 1..12) month = newMonth - 1
                    },
                    label = { Text("Mes (1-12)") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Día
                OutlinedTextField(
                    value = day.toString(),
                    onValueChange = { d ->
                        val newDay = d.toIntOrNull() ?: day
                        if (newDay in 1..31) day = newDay
                    },
                    label = { Text("Día") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                onDateSelected(selectedDate)
            }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF718096), fontWeight = FontWeight.Bold)
        Text(value, fontSize = 12.sp, color = Color(0xFF2d3748), fontWeight = FontWeight.Bold)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        date.format(DateTimeFormatter.ofPattern("dd MMM"))
    } catch (e: Exception) {
        dateStr
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun showDatePickerDialog(
    context: android.content.Context,
    initialDate: LocalDate,
    onDateSelected: (String) -> Unit
) {
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
            onDateSelected(selectedDate)
        },
        initialDate.year,
        initialDate.monthValue - 1,
        initialDate.dayOfMonth
    )
    datePickerDialog.show()
}