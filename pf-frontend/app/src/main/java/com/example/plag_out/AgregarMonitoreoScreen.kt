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
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

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
    var dropdownPlagaExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.cargarPlagas()
        viewModel.cargarTerrenos()
    }

    LaunchedEffect(state.terrenoSeleccionado) {
        if (state.terrenoSeleccionado != null) viewModel.cargarPlantaciones(state.terrenoSeleccionado!!.terreno_id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderNuevoMonitoreo(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PlagOutColors.Forest)
                }
            } else {
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        // ── Paso 1: Terreno ─────────────────────────────────────
                        PasoLabel(numero = 1, texto = "Terreno a monitorear")
                        Spacer(Modifier.height(10.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownTerrenoExpanded,
                            onExpandedChange = { dropdownTerrenoExpanded = !dropdownTerrenoExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = state.terrenoSeleccionado?.terreno_nombre ?: "",
                                onValueChange = {},
                                label = { Text("Seleccioná un terreno") },
                                leadingIcon = { Icon(Icons.Outlined.Landscape, contentDescription = null, tint = PlagOutColors.Forest) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownTerrenoExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtTerreno")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownTerrenoExpanded,
                                onDismissRequest = { dropdownTerrenoExpanded = false }
                            ) {
                                if (state.terrenos.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No tienes terrenos", color = PlagOutColors.TextSecondary) },
                                        onClick = { dropdownTerrenoExpanded = false }
                                    )
                                } else {
                                    state.terrenos.forEach { terreno ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(terreno.terreno_nombre, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
                                                    Text(
                                                        "Hectáreas: ${terreno.terreno_area}",
                                                        fontSize = 12.sp,
                                                        fontStyle = FontStyle.Italic,
                                                        color = PlagOutColors.TextSecondary
                                                    )
                                                    Text(
                                                        "Latitud: ${terreno.terreno_latitud}, Longitud: ${terreno.terreno_longitud}",
                                                        fontSize = 11.sp,
                                                        color = PlagOutColors.TextSecondary
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

                        state.terrenoSeleccionado?.let { terreno ->
                            Spacer(Modifier.height(10.dp))
                            PreviewFila(
                                icono = Icons.Outlined.Landscape,
                                titulo = terreno.terreno_nombre,
                                subtitulo = "${terreno.terreno_area} ha · ${"%.2f".format(terreno.terreno_latitud)}, ${"%.2f".format(terreno.terreno_longitud)}"
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        // ── Paso 2: Plantación ──────────────────────────────────
                        PasoLabel(numero = 2, texto = "Plantación a monitorear")
                        Spacer(Modifier.height(10.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownPlantacionExpanded,
                            onExpandedChange = { dropdownPlantacionExpanded = !dropdownPlantacionExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = state.plantacionSeleccionada?.cultivo_nombre ?: "",
                                onValueChange = {},
                                label = { Text("Seleccioná una plantación") },
                                leadingIcon = { Icon(Icons.Outlined.Grass, contentDescription = null, tint = PlagOutColors.Forest) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownPlantacionExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtPlantacion")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownPlantacionExpanded,
                                onDismissRequest = { dropdownPlantacionExpanded = false }
                            ) {
                                if (state.plantaciones.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (state.terrenoSeleccionado != null) "Sin plantaciones en este terreno" else "Seleccione un terreno",
                                                color = PlagOutColors.TextSecondary
                                            )
                                        },
                                        onClick = { dropdownPlantacionExpanded = false }
                                    )
                                } else {
                                    state.plantaciones.forEach { plantacion ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(plantacion.cultivo_nombre, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
                                                    Text(
                                                        plantacion.cultivo_nombre_cientifico,
                                                        fontSize = 12.sp,
                                                        fontStyle = FontStyle.Italic,
                                                        color = PlagOutColors.TextSecondary
                                                    )
                                                    Text(
                                                        "Sembrado: ${plantacion.fecha_siembra}",
                                                        fontSize = 11.sp,
                                                        color = PlagOutColors.TextSecondary
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

                        state.plantacionSeleccionada?.let { plantacion ->
                            Spacer(Modifier.height(10.dp))
                            PreviewFila(
                                icono = Icons.Outlined.Grass,
                                titulo = plantacion.cultivo_nombre,
                                subtitulo = "${plantacion.cultivo_nombre_cientifico} · Sembrado ${plantacion.fecha_siembra}"
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        // ── Paso 3: Plaga ───────────────────────────────────────
                        PasoLabel(numero = 3, texto = "Plaga a monitorear")
                        Spacer(Modifier.height(10.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownPlagaExpanded,
                            onExpandedChange = { dropdownPlagaExpanded = !dropdownPlagaExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = state.plagaSeleccionada?.nombre ?: "",
                                onValueChange = {},
                                label = { Text("Seleccioná una plaga") },
                                leadingIcon = { Icon(Icons.Outlined.BugReport, contentDescription = null, tint = PlagOutColors.Forest) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownPlagaExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtPlaga")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownPlagaExpanded,
                                onDismissRequest = { dropdownPlagaExpanded = false }
                            ) {
                                if (state.plagas.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hay plagas para monitorear", color = PlagOutColors.TextSecondary) },
                                        onClick = { dropdownPlagaExpanded = false }
                                    )
                                } else {
                                    state.plagas.forEach { plaga ->
                                        DropdownMenuItem(
                                            text = { Text(plaga.nombre, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain) },
                                            onClick = {
                                                viewModel.seleccionarPlaga(plaga)
                                                dropdownPlagaExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        state.plagaSeleccionada?.let { plaga ->
                            Spacer(Modifier.height(10.dp))
                            PreviewFila(
                                icono = Icons.Filled.BugReport,
                                titulo = plaga.nombre,
                                subtitulo = plaga.nombre_cientifico
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            UmbralDeRiesgoSlider(
                umbralActual = state.umbralDeRiesgo,
                onUmbralChange = { viewModel.actualizarUmbralDeRiesgo(it) }
            )

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

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { viewModel.guardarMonitoreo { onSuccess() } },
                enabled = !state.isGuardando && state.plantacionSeleccionada != null && state.plagaSeleccionada != null,
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
                    Icon(Icons.Filled.BugReport, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Iniciar monitoreo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Composables auxiliares ───────────────────────────────────────────────────

@Composable
private fun HeaderNuevoMonitoreo(onBack: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderMonitoreo")
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
                .size(50.dp)
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
                    "Seguimiento de plagas",
                    color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Text(
                    "Nuevo Monitoreo", 
                    color = PlagOutColors.TextOnDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun PasoLabel(numero: Int, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(PlagOutColors.Forest),
            contentAlignment = Alignment.Center
        ) {
            Text("$numero", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextOnDark)
        }
        Spacer(Modifier.width(10.dp))
        Text(texto, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
    }
}

@Composable
private fun PreviewFila(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, subtitulo: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(PlagOutColors.Forest.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
            Text(subtitulo, fontSize = 11.sp, color = PlagOutColors.TextSecondary)
        }
    }
}

@Composable
fun UmbralDeRiesgoSlider(
    umbralActual: Int,
    onUmbralChange: (Int) -> Unit
) {
    val colorRiesgo = when {
        umbralActual < 34 -> PlagOutColors.RiskOk
        umbralActual < 67 -> PlagOutColors.RiskWarn
        else -> PlagOutColors.RiskDanger
    }

    val etiquetaRiesgo = when {
        umbralActual < 34 -> "Riesgo bajo"
        umbralActual < 67 -> "Riesgo medio"
        else -> "Riesgo alto"
    }

    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cardUmbralRiesgo")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Umbral de riesgo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PlagOutColors.TextMain)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(colorRiesgo.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("$umbralActual%", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colorRiesgo)
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(etiquetaRiesgo, fontSize = 12.sp, color = colorRiesgo, fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(12.dp))

            Slider(
                value = umbralActual.toFloat(),
                onValueChange = { onUmbralChange(it.toInt()) },
                valueRange = 0f..100f,
                steps = 99,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sliderUmbralRiesgo"),
                colors = SliderDefaults.colors(
                    thumbColor = colorRiesgo,
                    activeTrackColor = colorRiesgo,
                    inactiveTrackColor = colorRiesgo.copy(alpha = 0.15f)
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
                Text("50", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
                Text("100", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
            }
        }
    }
}
