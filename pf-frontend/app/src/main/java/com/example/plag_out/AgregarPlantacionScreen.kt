package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarPlantacionScreen(
    terrenoId: Int,
    viewModel: AgregarPlantacionViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { viewModel.seleccionarFecha(it) }
                        showDatePicker = false
                    }
                ) { Text("Aceptar", color = PlagOutColors.Forest) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = PlagOutColors.Forest) }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = PlagOutColors.Forest,
                    todayContentColor = PlagOutColors.Forest,
                    titleContentColor = PlagOutColors.Forest,
                    headlineContentColor = PlagOutColors.Forest
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderAgregarPlantacion(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    if (state.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PlagOutColors.Forest)
                        }
                    } else {
                        Text(
                            "Detalle del cultivo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain
                        )
                        Text(
                            "Elegí qué se sembró y cuándo empezó el ciclo.",
                            fontSize = 12.sp,
                            color = PlagOutColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = state.cultivoSeleccionado?.nombre ?: "",
                                onValueChange = {},
                                label = { Text("Seleccionar cultivo") },
                                leadingIcon = { Icon(Icons.Outlined.Grass, contentDescription = null, tint = PlagOutColors.Forest) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("txtCultivo")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                state.cultivos.forEach { cultivo ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(cultivo.nombre, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
                                                Text(
                                                    cultivo.nombre_cientifico,
                                                    fontSize = 11.sp,
                                                    color = PlagOutColors.TextSecondary
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.actualizarCultivo(cultivo)
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = state.fechaSiembra,
                                onValueChange = {},
                                label = { Text("Fecha de siembra") },
                                readOnly = true,
                                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PlagOutColors.Forest) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showDatePicker = true }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(state.error ?: "", color = PlagOutColors.RiskDanger, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.guardarPlantacion(terrenoId) { onSuccess() } },
                enabled = !state.isGuardando && state.cultivoSeleccionado != null && state.fechaSiembra.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnGuardar"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f),
                    contentColor = PlagOutColors.TextOnDark,
                    disabledContentColor = PlagOutColors.TextOnDark.copy(alpha = 0.6f)
                )
            ) {
                if (state.isGuardando) {
                    CircularProgressIndicator(color = PlagOutColors.TextOnDark, modifier = Modifier.size(22.dp))
                } else {
                    Text("Guardar plantación", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeaderAgregarPlantacion(onBack: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderPlantacion")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativa"
    )

    val forma = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = forma
            )
            .clip(forma)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-40).dp)
                .size(150.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )

        Row(
            modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
            }
            Column {
                Text(
                    "Nueva plantación",
                    color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Text("Agregar Plantación", color = PlagOutColors.TextOnDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
