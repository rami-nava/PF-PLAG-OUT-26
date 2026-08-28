package com.example.plag_out

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.BotonInfoCampo
import com.example.plag_out.ui.theme.PlagOutColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CrearReporteScreen(
    viewModel: CrearReporteViewModel,
    onBack: () -> Unit,
    onSuccess: (reporteId: Int, reporteJson: String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    // Navegar a VerReporteScreen en cuanto el ViewModel tenga el payload listo
    LaunchedEffect(state.reporteNavPayload) {
        state.reporteNavPayload?.let { payload ->
            val json = Uri.encode(com.google.gson.Gson().toJson(payload))
            onSuccess(payload.id, json)
        }
    }

    var dropdownTerrenoExpanded by remember { mutableStateOf(false) }
    var dropdownPlantacionExpanded by remember { mutableStateOf(false) }
    var dropdownPlagaExpanded by remember { mutableStateOf(false) }
    var dropdownEtapaExpanded by remember { mutableStateOf(false) }
    var mostrarInfoSeveridad by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.actualizarTimestamp()
    }

    LaunchedEffect(state.terrenoSeleccionado) { dropdownPlantacionExpanded = false }
    LaunchedEffect(state.plantacionSeleccionada) { dropdownPlagaExpanded = false }
    LaunchedEffect(state.plagaSeleccionada) { dropdownEtapaExpanded = false }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderCrearReporte(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            if (state.isLoadingInicial) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PlagOutColors.Forest)
                }
            } else {
                // ── Card 1: Selección de Terreno ──────────────────────────────────
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PasoTag(numero = 1)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Terreno del Reporte",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

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
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Landscape,
                                        contentDescription = null,
                                        tint = PlagOutColors.Forest
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownTerrenoExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtTerrenoReporte")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownTerrenoExpanded,
                                onDismissRequest = { dropdownTerrenoExpanded = false }
                            ) {
                                if (state.terrenos.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hay terrenos cargados", color = PlagOutColors.TextSecondary) },
                                        onClick = { dropdownTerrenoExpanded = false }
                                    )
                                } else {
                                    state.terrenos.forEach { terreno ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    terreno.terreno_nombre,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = PlagOutColors.TextMain
                                                )
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
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 2: Selección de Cultivo ──────────────────────────────
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PasoTag(numero = 2)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Cultivo",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownPlantacionExpanded,
                            onExpandedChange = {
                                if (state.terrenoSeleccionado != null) {
                                    dropdownPlantacionExpanded = !dropdownPlantacionExpanded
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val plantacionTexto = when {
                                state.terrenoSeleccionado == null -> "Seleccioná un terreno primero"
                                state.plantacionSeleccionada != null -> "${state.plantacionSeleccionada!!.cultivo_nombre} (${state.terrenoSeleccionado!!.terreno_nombre})"
                                else -> ""
                            }

                            OutlinedTextField(
                                readOnly = true,
                                value = plantacionTexto,
                                onValueChange = {},
                                enabled = state.terrenoSeleccionado != null,
                                label = { Text("Seleccioná un cultivo") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Grass,
                                        contentDescription = null,
                                        tint = PlagOutColors.Forest
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownPlantacionExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtPlantacionReporte")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownPlantacionExpanded && state.terrenoSeleccionado != null,
                                onDismissRequest = { dropdownPlantacionExpanded = false }
                            ) {
                                if (state.plantaciones.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Este terreno no tienecultivos activos", color = PlagOutColors.TextSecondary) },
                                        onClick = { dropdownPlantacionExpanded = false }
                                    )
                                } else {
                                    state.plantaciones.forEach { plantacion ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        plantacion.cultivo_nombre,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PlagOutColors.TextMain
                                                    )
                                                    Text(
                                                        "Siembra: ${plantacion.fecha_siembra}",
                                                        fontSize = 12.sp,
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
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 3: Selección de Tipo de Plaga (Filtrada por Cultivo) ──────
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PasoTag(numero = 3)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Tipo de Plaga",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        val plantacionElegida = state.plantacionSeleccionada
                        val plagasDropdown = state.plagasDisponibles

                        ExposedDropdownMenuBox(
                            expanded = dropdownPlagaExpanded,
                            onExpandedChange = {
                                if (plantacionElegida != null) dropdownPlagaExpanded = !dropdownPlagaExpanded
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                enabled = plantacionElegida != null,
                                value = state.plagaSeleccionada?.nombre ?: "",
                                onValueChange = {},
                                label = {
                                    Text(
                                        if (plantacionElegida != null) "Seleccioná un tipo de plaga"
                                        else "Elegí un cultivo primero"
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.BugReport,
                                        contentDescription = null,
                                        tint = PlagOutColors.Forest
                                    )
                                },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownPlagaExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtTipoPlaga")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownPlagaExpanded && plantacionElegida != null,
                                onDismissRequest = { dropdownPlagaExpanded = false }
                            ) {
                                if (plagasDropdown.isEmpty()) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (plantacionElegida == null) "Seleccioná un cultivo"
                                                else "No hay plagas registradas para ${plantacionElegida.cultivo_nombre}",
                                                color = PlagOutColors.TextSecondary
                                            )
                                        },
                                        onClick = { dropdownPlagaExpanded = false }
                                    )
                                } else {
                                    plagasDropdown.forEach { plaga ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(
                                                        plaga.nombre,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = PlagOutColors.TextMain
                                                    )
                                                    if (plaga.nombre_cientifico.isNotEmpty()) {
                                                        Text(
                                                            plaga.nombre_cientifico,
                                                            fontSize = 12.sp,
                                                            color = PlagOutColors.TextSecondary
                                                        )
                                                    }
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
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 4: Etapa Biológica / Fenológica ────────────────────────────
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PasoTag(numero = 4)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Etapa Biológica de la Plaga",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        ExposedDropdownMenuBox(
                            expanded = dropdownEtapaExpanded,
                            onExpandedChange = {
                                if (state.plagaSeleccionada != null) {
                                    dropdownEtapaExpanded = !dropdownEtapaExpanded
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = if (state.plagaSeleccionada == null) "Seleccioná una plaga primero" else state.etapaBiologica.ifEmpty { "Seleccionar etapa..." },
                                onValueChange = {},
                                enabled = state.plagaSeleccionada != null,
                                label = { Text("Etapa Biológica") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownEtapaExpanded) },
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtEtapaBiologica")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownEtapaExpanded && state.plagaSeleccionada != null,
                                onDismissRequest = { dropdownEtapaExpanded = false }
                            ) {
                                state.etapasDisponibles.forEach { etapa ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                etapa,
                                                fontWeight = if (state.etapaBiologica == etapa) FontWeight.Bold else FontWeight.Normal,
                                                color = PlagOutColors.TextMain
                                            )
                                        },
                                        onClick = {
                                            viewModel.seleccionarEtapaBiologica(etapa)
                                            dropdownEtapaExpanded = false
                                        },
                                        modifier = Modifier.testTag("optEtapa_$etapa")
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 5: Nivel de Severidad ("Bajo", "Medio", "Alto") ────────────
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PasoTag(numero = 5)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Nivel de Severidad",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain,
                                modifier = Modifier.weight(1f)
                            )
                            BotonInfoCampo(
                                onClick = { mostrarInfoSeveridad = true },
                                contentDescription = "Información sobre Nivel de Severidad",
                                modifier = Modifier.testTag("btnInfoSeveridad")
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        val opciones = listOf("Bajo", "Medio", "Alto")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            opciones.forEach { opcion ->
                                val seleccionado = state.nivelSeveridad == opcion
                                val colorBorde = when (opcion) {
                                    "Bajo" -> PlagOutColors.RiskOk
                                    "Medio" -> PlagOutColors.RiskWarn
                                    else -> PlagOutColors.RiskDanger
                                }

                                val colorFondo = if (seleccionado) colorBorde else PlagOutColors.Surface
                                val colorTexto = if (seleccionado) PlagOutColors.TextOnDark else colorBorde

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("optSeveridad_$opcion"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = colorFondo,
                                    border = BorderStroke(1.5.dp, colorBorde),
                                    onClick = { viewModel.actualizarNivelSeveridad(opcion) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (seleccionado) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = PlagOutColors.TextOnDark,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(
                                            text = opcion,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = colorTexto
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 6: Ubicación Geográfica (Asignada automáticamente) ─────────
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PasoTag(numero = 6)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Ubicación Geográfica",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PlagOutColors.CreamDeep.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = PlagOutColors.Forest
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Coordenadas del terreno (Asignadas automáticamente)",
                                    fontSize = 11.sp,
                                    color = PlagOutColors.TextSecondary
                                )
                                val latStr = state.latitud?.let { "%.4f".format(it) } ?: "—"
                                val lonStr = state.longitud?.let { "%.4f".format(it) } ?: "—"
                                Text(
                                    text = "Lat: $latStr, Lon: $lonStr",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PlagOutColors.TextMain
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 7: Captura Automática de Fecha y Hora ──────────────────────
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PasoTag(numero = 7)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Fecha y Hora",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
                        val fechaFormateada = remember(state.timestampMs) {
                            sdf.format(Date(state.timestampMs))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = PlagOutColors.Forest)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Captura automática", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
                                    Text(
                                        text = fechaFormateada,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlagOutColors.TextMain
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.actualizarTimestamp() }) {
                                Icon(Icons.Filled.AccessTime, contentDescription = "Actualizar hora", tint = PlagOutColors.Forest)
                            }
                        }
                    }
                }

                // Mensaje de Error
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
                        Icon(
                            Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = PlagOutColors.RiskDanger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            state.error ?: "",
                            color = PlagOutColors.RiskDanger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Botón de envío inferior ─────────────────────────────────────
                val formularioValido = state.terrenoSeleccionado != null &&
                        state.plantacionSeleccionada != null &&
                        state.plagaSeleccionada != null &&
                        state.etapaBiologica.isNotBlank()

                Button(
                    onClick = { viewModel.guardarReporte {} },
                    enabled = formularioValido && !state.isGuardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btnGuardarReporte"),
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar Reporte", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!formularioValido) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Seleccioná Terreno, Cultivo, Plaga y Etapa Biológica para habilitar el envío.",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (mostrarInfoSeveridad) {
        SeveridadInfoSheet(onDismiss = { mostrarInfoSeveridad = false })
    }
}

// ── Composables Auxiliares ──────────────────────────────────────────────────

@Composable
private fun HeaderCrearReporte(onBack: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderReporte")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativaReporte"
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
        // Círculo decorativo animado
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
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btnVolverCrearReporte")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = PlagOutColors.TextOnDark
                )
            }
            Column {
                Text(
                    text = "Reporte de plagas",
                    color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "Nuevo Reporte", 
                    color = PlagOutColors.TextOnDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun PasoTag(numero: Int) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(PlagOutColors.Forest),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$numero",
            color = PlagOutColors.TextOnDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeveridadInfoSheet(onDismiss: () -> Unit) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = estadoHoja,
        containerColor = PlagOutColors.Surface,
        contentWindowInsets = { WindowInsets.systemBars },
        dragHandle = { BottomSheetDefaults.DragHandle(color = PlagOutColors.Divider) }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp)
                .testTag("sheetInfoSeveridad")
        ) {
            Text(
                text = "¿Qué es el Nivel de Severidad?",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Representa la intensidad, grado de infestación o la magnitud del daño causado por la plaga en el terreno monitoreado.",
                fontSize = 14.sp,
                color = PlagOutColors.TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Categorías de Severidad",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )

            Spacer(Modifier.height(10.dp))

            // 🟢 Bajo
            CategoriaSeveridadItem(
                badgeColor = PlagOutColors.RiskOk,
                titulo = "Bajo",
                descripcion = "Avistamiento inicial o presencias mínimas aisladas."
            )

            Spacer(Modifier.height(10.dp))

            // 🟡 Medio
            CategoriaSeveridadItem(
                badgeColor = PlagOutColors.RiskWarn,
                titulo = "Medio",
                descripcion = "Presencia moderada o visible en varias plantas."
            )

            Spacer(Modifier.height(10.dp))

            // 🔴 Alto
            CategoriaSeveridadItem(
                badgeColor = PlagOutColors.RiskDanger,
                titulo = "Alto",
                descripcion = "Infestación severa o daño crítico que pone en riesgo el rendimiento del lote."
            )

            Spacer(Modifier.height(20.dp))

            // Nota Agronómica
            Surface(
                color = PlagOutColors.Cream,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, PlagOutColors.CreamDeep),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = PlagOutColors.Forest,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Otras aclaraciones",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.Forest
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "• Incidencia: Porcentaje de plantas o muestras afectadas respecto al total.\n" +
                               "• Severidad: Porcentaje de área foliar o tejido alterado/dañado en la planta.\n\n" +
                               "Plag-Out las sintetiza para facilitarte la priorización y toma de decisiones rápidamente.",
                        fontSize = 13.sp,
                        color = PlagOutColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Botón de Cierre
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btnCerrarInfoSeveridad"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    contentColor = PlagOutColors.TextOnDark
                )
            ) {
                Text(
                    text = "Entendido",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CategoriaSeveridadItem(
    badgeColor: Color,
    titulo: String,
    descripcion: String
) {
    Surface(
        color = PlagOutColors.CreamDeep.copy(alpha = 0.3f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, PlagOutColors.Divider.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(badgeColor, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = titulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextMain
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = descripcion,
                fontSize = 13.sp,
                color = PlagOutColors.TextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 18.dp)
            )
        }
    }
}
