package com.example.plag_out

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.plag_out.ui.theme.PlagOutColors
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.json.Json as KxJson

@SuppressLint("MissingPermission")
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

    // Ubicación GPS
    val context = LocalContext.current
    var isGranted by remember { mutableStateOf(false) }
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var obteniendoUbicacion by remember { mutableStateOf(false) }

    // Control de vista de mapa
    var mostrarMapa by remember { mutableStateOf(false) }
    var selectedLocation by remember {
        mutableStateOf<GeoPoint?>(
            if (state.latitud != null && state.longitud != null) GeoPoint(state.latitud!!, state.longitud!!) else null
        )
    }

    LaunchedEffect(state.latitud, state.longitud) {
        if (state.latitud != null && state.longitud != null) {
            selectedLocation = GeoPoint(state.latitud!!, state.longitud!!)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> isGranted = granted }
    )

    LaunchedEffect(Unit) {
        isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.actualizarTimestamp()
    }

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
                                        text = { Text("No tienes terrenos registrados", color = PlagOutColors.TextSecondary) },
                                        onClick = { dropdownTerrenoExpanded = false }
                                    )
                                } else {
                                    state.terrenos.forEach { terreno ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(terreno.terreno_nombre, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
                                                    Text(
                                                        "Hectáreas: ${terreno.terreno_area} ha",
                                                        fontSize = 12.sp,
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
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 2: Selección de Plantación ──────────────────────────────
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
                                "Plantación en el Terreno",
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
                            OutlinedTextField(
                                readOnly = true,
                                value = state.plantacionSeleccionada?.cultivo_nombre ?: "",
                                onValueChange = {},
                                label = { Text(if (state.terrenoSeleccionado == null) "Primero seleccioná un terreno" else "Seleccioná una plantación") },
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
                                enabled = state.terrenoSeleccionado != null,
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("txtPlantacionReporte")
                            )

                            ExposedDropdownMenu(
                                expanded = dropdownPlantacionExpanded,
                                onDismissRequest = { dropdownPlantacionExpanded = false }
                            ) {
                                if (state.plantaciones.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Sin plantaciones activas en este terreno", color = PlagOutColors.TextSecondary) },
                                        onClick = { dropdownPlantacionExpanded = false }
                                    )
                                } else {
                                    state.plantaciones.forEach { plantacion ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(plantacion.cultivo_nombre, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
                                                    Text(
                                                        "${plantacion.cultivo_nombre_cientifico} · Sembrado: ${plantacion.fecha_siembra}",
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
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 3: Selección de Tipo de Plaga [OBLIGATORIO] ─────────────────
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

                        ExposedDropdownMenuBox(
                            expanded = dropdownPlagaExpanded,
                            onExpandedChange = { dropdownPlagaExpanded = !dropdownPlagaExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                readOnly = true,
                                value = state.plagaSeleccionada?.nombre ?: "",
                                onValueChange = {},
                                label = { Text("Seleccioná un tipo de plaga") },
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
                                expanded = dropdownPlagaExpanded,
                                onDismissRequest = { dropdownPlagaExpanded = false }
                            ) {
                                if (state.plagas.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hay plagas disponibles", color = PlagOutColors.TextSecondary) },
                                        onClick = { dropdownPlagaExpanded = false }
                                    )
                                } else {
                                    state.plagas.forEach { plaga ->
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

                // ── Card 4: Nivel de Severidad ("Bajo", "Medio", "Alto") ────────────
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
                                "Nivel de Severidad",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
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

                // ── Card 5: Captura de Ubicación GPS / Mapa (Autocompletada por Terreno) ──
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PasoTag(numero = 5)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Ubicación Geográfica",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Muestra de coordenadas actuales (autocompletadas o modificadas)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PlagOutColors.CreamDeep, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PlagOutColors.Forest)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Coordenadas del reporte", fontSize = 11.sp, color = PlagOutColors.TextSecondary)
                                    val latStr = state.latitud?.let { "%.4f".format(it) } ?: "—"
                                    val lonStr = state.longitud?.let { "%.4f".format(it) } ?: "—"
                                    Text(
                                        "Lat: $latStr, Lon: $lonStr",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlagOutColors.TextMain
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Botón "Usar ubicación actual" (GPS)
                            OutlinedButton(
                                onClick = {
                                    if (isGranted) {
                                        obteniendoUbicacion = true
                                        fusedLocationClient
                                            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                            .addOnSuccessListener { loc ->
                                                obteniendoUbicacion = false
                                                loc?.let {
                                                    viewModel.actualizarUbicacion(it.latitude, it.longitude)
                                                    selectedLocation = GeoPoint(it.latitude, it.longitude)
                                                }
                                            }
                                            .addOnFailureListener { obteniendoUbicacion = false }
                                    } else {
                                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                enabled = !obteniendoUbicacion,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btnUbicacionGPS"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, PlagOutColors.Forest),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.Forest)
                            ) {
                                if (obteniendoUbicacion) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PlagOutColors.Forest)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("GPS Actual", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Botón "Ver mapa"
                            OutlinedButton(
                                onClick = { mostrarMapa = !mostrarMapa },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btnSeleccionarMapa"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, PlagOutColors.Forest),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (mostrarMapa) PlagOutColors.Forest.copy(alpha = 0.1f) else Color.Transparent,
                                    contentColor = PlagOutColors.Forest
                                )
                            ) {
                                Icon(Icons.Outlined.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (mostrarMapa) "Ocultar mapa" else "Ver mapa", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Vista de mapa interactiva opcional OSM (Lazy init)
                        AnimatedVisibility(
                            visible = mostrarMapa,
                            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                            exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
                        ) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Text(
                                    "Tocá en el mapa para ajustar la ubicación del reporte:",
                                    fontSize = 12.sp,
                                    color = PlagOutColors.TextSecondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = PlagOutColors.CreamDeep
                                ) {
                                    if (mostrarMapa) {
                                        AndroidView(
                                            modifier = Modifier.fillMaxSize(),
                                            factory = { ctx ->
                                                Configuration.getInstance().userAgentValue = "PlagOutMobileApp/1.0.1 (contacto@mi-app.com)"
                                                Configuration.getInstance().load(
                                                    ctx,
                                                    ctx.getSharedPreferences("plag_out_prefs", android.content.Context.MODE_PRIVATE)
                                                )

                                                val cartoTileSource = XYTileSource(
                                                    "CartoDB-Positron",
                                                    0, 19, 256, ".png",
                                                    arrayOf(
                                                        "https://a.basemaps.cartocdn.com/light_all/",
                                                        "https://b.basemaps.cartocdn.com/light_all/",
                                                        "https://c.basemaps.cartocdn.com/light_all/"
                                                    )
                                                )

                                                MapView(ctx).apply {
                                                    setMultiTouchControls(true)
                                                    setTileSource(cartoTileSource)
                                                    controller.setZoom(7.0)
                                                    controller.setCenter(selectedLocation ?: GeoPoint(-34.6037, -58.3816))

                                                    val receiver = object : MapEventsReceiver {
                                                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                                            p?.let {
                                                                selectedLocation = it
                                                                viewModel.actualizarUbicacion(it.latitude, it.longitude)
                                                            }
                                                            return true
                                                        }

                                                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                                                    }

                                                    overlays.add(MapEventsOverlay(receiver))
                                                    onResume()
                                                }
                                            },
                                            update = { map ->
                                                map.overlays.removeAll { it is Marker }
                                                val receiver = map.overlays.firstOrNull { it is MapEventsOverlay }
                                                map.overlays.clear()
                                                receiver?.let { map.overlays.add(it) }

                                                selectedLocation?.let { point ->
                                                    val marker = Marker(map)
                                                    marker.position = point
                                                    marker.title = "Ubicación del reporte"
                                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                    map.overlays.add(marker)
                                                }
                                                map.invalidate()
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Card 6: Captura Automática de Fecha y Hora (en ms) ──────────────
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
                                "Fecha y Hora",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
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
                                        fechaFormateada,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlagOutColors.TextMain
                                    )
                                    Text(
                                        "Timestamp: ${state.timestampMs} ms",
                                        fontSize = 10.sp,
                                        color = PlagOutColors.TextSecondary
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
                        state.plagaSeleccionada != null

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
                        "Seleccioná Terreno, Plantación y Plaga para habilitar el envío.",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
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
            "$numero",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextOnDark
        )
    }
}
