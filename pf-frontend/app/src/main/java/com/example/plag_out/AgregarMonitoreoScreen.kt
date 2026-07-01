package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ColorVerde        = Color(0xFF2d5016)
private val ColorNaranja      = Color(0xFFE8941A)
private val ColorFondo        = Color(0xFFF8F7F4)
private val ColorFondoStat    = Color(0xFFF1EFE8)
private val ColorTextoSub     = Color(0xFF718096)
private val ColorError        = Color(0xFFE53E3E)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarMonitoreoScreen(
    viewModel: AgregarMonitoreoViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    var dropdownTerrenoExpanded by remember { mutableStateOf(false) }
    var dropdownPlantacionExpanded by remember { mutableStateOf(false) }
    var dropdownPlagaExpanded      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.cargarPlagas()
        viewModel.cargarTerrenos()
    }

    LaunchedEffect(state.terrenoSeleccionado) {
        if(state.terrenoSeleccionado != null) viewModel.cargarPlantaciones(state.terrenoSeleccionado!!.terreno_id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorFondo)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint               = ColorVerde
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text       = "Nuevo Monitoreo",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorVerde
            )
        }


        if (state.isLoading) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ColorVerde)
            }
        } else {
            // ── Paso 1: Terreno ────────────────────────────────────────────
            StepLabel(numero = "1", texto = "Terreno a monitorear")

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded        = dropdownTerrenoExpanded,
                onExpandedChange = { dropdownTerrenoExpanded = !dropdownTerrenoExpanded },
                modifier        = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly    = true,
                    value       = state.terrenoSeleccionado?.terreno_nombre ?: "",
                    onValueChange = {},
                    label       = { Text("Seleccioná un terreno") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownTerrenoExpanded) },
                    colors      = outlinedColors(),
                    modifier    = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded        = dropdownTerrenoExpanded,
                    onDismissRequest = { dropdownTerrenoExpanded = false }
                ) {
                    if (state.terrenos.isEmpty()) {
                        DropdownMenuItem(
                            text    = { Text("No tienes terrenos", color = ColorTextoSub) },
                            onClick = { dropdownTerrenoExpanded = false }
                        )
                    } else {
                        state.terrenos.forEach { terreno ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text       = terreno.terreno_nombre,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = ColorVerde
                                        )
                                        Text(
                                            text     = "Hectáreas: ${terreno.terreno_area}",
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic,
                                            color    = ColorTextoSub
                                        )
                                        Text(
                                            text     = "Latitud: ${terreno.terreno_latitud}, Longitud: ${terreno.terreno_longitud}",
                                            fontSize = 11.sp,
                                            color    = ColorTextoSub
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.seleccionarTerreno(terreno)
                                    dropdownTerrenoExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Preview de terreno seleccionado
            state.terrenoSeleccionado?.let { terreno ->
                Spacer(modifier = Modifier.height(10.dp))
                TerrenoPreviewCard(terreno)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Paso 2: Plantación ────────────────────────────────────────────
            StepLabel(numero = "2", texto = "Plantación a monitorear")

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded        = dropdownPlantacionExpanded,
                onExpandedChange = { dropdownPlantacionExpanded = !dropdownPlantacionExpanded },
                modifier        = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly    = true,
                    value       = state.plantacionSeleccionada?.cultivo_nombre ?: "",
                    onValueChange = {},
                    label       = { Text("Seleccioná una plantación") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownPlantacionExpanded) },
                    colors      = outlinedColors(),
                    modifier    = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded        = dropdownPlantacionExpanded,
                    onDismissRequest = { dropdownPlantacionExpanded = false }
                ) {
                    if (state.plantaciones.isEmpty()) {
                        DropdownMenuItem(
                            text    = { Text("Sin plantaciones en este terreno", color = ColorTextoSub) },
                            onClick = { dropdownPlantacionExpanded = false }
                        )
                    } else {
                        state.plantaciones.forEach { plantacion ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text       = plantacion.cultivo_nombre,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = ColorVerde
                                        )
                                        Text(
                                            text     = plantacion.cultivo_nombre_cientifico,
                                            fontSize = 12.sp,
                                            fontStyle = FontStyle.Italic,
                                            color    = ColorTextoSub
                                        )
                                        Text(
                                            text     = "Sembrado: ${plantacion.fecha_siembra}",
                                            fontSize = 11.sp,
                                            color    = ColorTextoSub
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.seleccionarPlantacion(plantacion)
                                    dropdownPlantacionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Preview de plantación seleccionada
            state.plantacionSeleccionada?.let { plantacion ->
                Spacer(modifier = Modifier.height(10.dp))
                PlantacionPreviewCard(plantacion)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Paso 2: Plaga ─────────────────────────────────────────────────
            StepLabel(numero = "3", texto = "Plaga a monitorear")

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded         = dropdownPlagaExpanded,
                onExpandedChange = { dropdownPlagaExpanded = !dropdownPlagaExpanded },
                modifier         = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly      = true,
                    value         = state.plagaSeleccionada?.nombre ?: "",
                    onValueChange = {},
                    label         = { Text("Seleccioná una plaga") },
                    trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownPlagaExpanded) },
                    colors        = outlinedColors(),
                    modifier      = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded         = dropdownPlagaExpanded,
                    onDismissRequest = { dropdownPlagaExpanded = false }
                ) {
                    if (state.plagas.isEmpty()) {
                        DropdownMenuItem(
                            text    = { Text("No hay plagas para monitorear", color = ColorTextoSub) },
                            onClick = { dropdownPlantacionExpanded = false }
                        )
                    }else{
                    state.plagas.forEach { plaga ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = plaga.nombre,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ColorVerde
                                    )
                                }
                            },
                            onClick = {
                                viewModel.seleccionarPlaga(plaga)
                                dropdownPlagaExpanded = false
                            }
                        )
                    }
                    }
                }
            }

            // Preview de plaga seleccionada
            state.plagaSeleccionada?.let { plaga ->
                Spacer(modifier = Modifier.height(10.dp))
                PlagaPreviewCard(plaga)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Error ─────────────────────────────────────────────────────────────
        if (state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(ColorError.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text       = "⚠️ ${state.error}",
                    color      = ColorError,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Botón guardar ─────────────────────────────────────────────────────
        Button(
            onClick  = { viewModel.guardarMonitoreo { onSuccess() } },
            enabled  = !state.isGuardando
                    && state.plantacionSeleccionada != null
                    && state.plagaSeleccionada != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape  = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor         = ColorNaranja,
                disabledContainerColor = ColorNaranja.copy(alpha = 0.5f),
                contentColor           = Color.White,
                disabledContentColor   = Color.White.copy(alpha = 0.5f)
            )
        ) {
            if (state.isGuardando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text       = "🐛 Iniciar monitoreo",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Composables auxiliares ───────────────────────────────────────────────────

@Composable
private fun StepLabel(numero: String, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier         = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ColorVerde),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = numero,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.White
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text       = texto,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color      = ColorVerde
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TerrenoPreviewCard(terreno: TerrenoResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ColorFondoStat)
            .border(1.dp, Color(0xFFD4E8B8), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📍", fontSize = 28.sp)
        Column {
            Text(
                text       = terreno.terreno_nombre,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorVerde
            )
            Text(
                text      = "Hectáreas: ${terreno.terreno_area}",
                fontSize  = 11.sp,
                fontStyle = FontStyle.Italic,
                color     = ColorTextoSub
            )
            Text(
                text     = "Latitud: ${terreno.terreno_latitud}, Longitud: ${terreno.terreno_longitud}",
                fontSize = 11.sp,
                color    = ColorTextoSub
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PlantacionPreviewCard(plantacion: PlantacionesResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ColorFondoStat)
            .border(1.dp, Color(0xFFD4E8B8), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🌱", fontSize = 28.sp)
        Column {
            Text(
                text       = plantacion.cultivo_nombre,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorVerde
            )
            Text(
                text      = plantacion.cultivo_nombre_cientifico,
                fontSize  = 11.sp,
                fontStyle = FontStyle.Italic,
                color     = ColorTextoSub
            )
            Text(
                text     = "📅 Sembrado: ${plantacion.fecha_siembra}",
                fontSize = 11.sp,
                color    = ColorTextoSub
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        // Badge activa
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Color(0xFF38A169).copy(alpha = 0.12f)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text       = "Activa",
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFF276749)
            )
        }
    }
}

@Composable
private fun PlagaPreviewCard(plaga: PlagaResponse) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ColorFondoStat)
            .border(1.dp, Color(0xFFD4E8B8), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🐛", fontSize = 28.sp)
        Column {
            Text(
                text       = plaga.nombre,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = ColorVerde
            )
             Text(
                 text      = plaga.nombre_cientifico,
                 fontSize  = 11.sp,
                 fontStyle = FontStyle.Italic,
                 color     = ColorTextoSub
             )
        }
    }
}

// ─── Helper de colores para OutlinedTextField ─────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ColorVerde,
    focusedLabelColor  = ColorVerde,
    cursorColor        = ColorVerde
)