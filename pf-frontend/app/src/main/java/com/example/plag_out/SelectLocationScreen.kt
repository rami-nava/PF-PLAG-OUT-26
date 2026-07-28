package com.example.plag_out

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.plag_out.ui.theme.PlagOutColors
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import android.util.Log
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectLocationScreen(
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val state by nuevoTerrenoViewModel.state.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(1) }

    //EXPRESION REGULAR PARA NUMEROS REALES
    val regex = Regex("^-?\\d*\\.?\\d*$")

    //UBICACION DE USUARIO
    var isGranted by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    var obteniendoUbicacion by remember { mutableStateOf(false) }

    //UBICACION EN MAPA
    var selectedLocation by remember { mutableStateOf<GeoPoint?>(null) }

    // Estados locales para los campos de texto
    var latInput by remember { mutableStateOf(state.latitud?.toString() ?: "") }
    var lonInput by remember { mutableStateOf(state.longitud?.toString() ?: "") }

    // Sincronización del tipeo manual hacia el ViewModel
    LaunchedEffect(latInput, lonInput) {
        val lat = latInput.toDoubleOrNull()
        val lon = lonInput.toDoubleOrNull()
        nuevoTerrenoViewModel.actualizarUbicacion(lat, lon)

        if (lat != null && lon != null) {
            selectedLocation = GeoPoint(lat, lon)
        }
    }

    //SI NO TENGO PERMISO PARA ACCEDER A SU UBICACION SE LO PIDO
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> isGranted = granted }
    )

    LaunchedEffect(Unit) {
        //Consultar si ya tengo permiso para acceder a la ubicacion
        isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // Rangos y validación de Argentina
    val latValida = state.latitud?.let { it in -55.0..-21.8 } ?: false
    val lonValida = state.longitud?.let { it in -73.6..-53.6 } ?: false
    val isValid = latValida && lonValida
    val showError = (state.latitud != null || state.longitud != null) && !isValid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderUbicacion(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = PlagOutColors.Surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = PlagOutColors.Surface,
                    contentColor = PlagOutColors.Forest
                ) {
                    listOf("Mapa" to Icons.Outlined.Map, "Coordenadas" to Icons.Outlined.Numbers).forEachIndexed { index, (title, icono) ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            selectedContentColor = PlagOutColors.Forest,
                            unselectedContentColor = PlagOutColors.TextSecondary,
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icono, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedTabIndex == 0) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(22.dp),
                        color = PlagOutColors.Surface,
                        shadowElevation = 2.dp
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                Configuration.getInstance().userAgentValue = "PlagOutMobileApp/1.0.1 (contacto@mi-app.com)"
                                Configuration.getInstance().load(
                                    ctx,
                                    PreferenceManager.getDefaultSharedPreferences(ctx)
                                )
                                Log.d("OSM_DEBUG", "User-Agent en MapView: ${Configuration.getInstance().userAgentValue}")

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
                                    controller.setZoom(5.0)
                                    controller.setCenter(selectedLocation ?: GeoPoint(-34.6037, -58.3816))

                                    val receiver = object : MapEventsReceiver {
                                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                            p?.let {
                                                selectedLocation = it
                                                latInput = it.latitude.toString()
                                                lonInput = it.longitude.toString()
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
                                    marker.title = "Ubicación del terreno"
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    map.overlays.add(marker)
                                }

                                map.invalidate()
                            }
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = PlagOutColors.Surface,
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Ingresá las coordenadas",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )

                            OutlinedTextField(
                                value = latInput,
                                onValueChange = { newValue -> if (newValue.matches(regex)) latInput = newValue },
                                label = { Text("Latitud (Ej: -34.60)") },
                                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PlagOutColors.Forest) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                trailingIcon = {
                                    if (latInput.isNotEmpty()) {
                                        IconButton(onClick = { latInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = PlagOutColors.TextSecondary)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("txtLatitud")
                            )

                            OutlinedTextField(
                                value = lonInput,
                                onValueChange = { newValue -> if (newValue.matches(regex)) lonInput = newValue },
                                label = { Text("Longitud (Ej: -58.38)") },
                                leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PlagOutColors.Forest) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(16.dp),
                                colors = camposColors(),
                                trailingIcon = {
                                    if (lonInput.isNotEmpty()) {
                                        IconButton(onClick = { lonInput = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = PlagOutColors.TextSecondary)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("txtLongitud")
                            )

                            OutlinedButton(
                                onClick = {
                                    if (isGranted) {
                                        obteniendoUbicacion = true
                                        fusedLocationClient
                                            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                            .addOnSuccessListener { location ->
                                                obteniendoUbicacion = false
                                                location?.let {
                                                    latInput = it.latitude.toString()
                                                    lonInput = it.longitude.toString()
                                                }
                                            }
                                            .addOnFailureListener { obteniendoUbicacion = false }
                                    } else {
                                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                enabled = !obteniendoUbicacion,
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.Forest),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PlagOutColors.Forest),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                if (obteniendoUbicacion) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PlagOutColors.Forest)
                                } else {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Usar ubicación actual", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedVisibility(
                visible = showError || state.error != null,
                enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (state.error != null) state.error ?: "" else "Ubicación fuera de la República Argentina.\nLatitud: -55.0 a -21.8 · Longitud: -73.6 a -53.6",
                        color = PlagOutColors.RiskDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PlagOutColors.CreamDeep, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Terreno", fontSize = 10.sp, color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        state.nombre.ifEmpty { "—" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Coordenadas", fontSize = 10.sp, color = PlagOutColors.TextSecondary, fontWeight = FontWeight.Medium)
                    Text(
                        "${state.latitud ?: "-"}°, ${state.longitud ?: "-"}°",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.Forest
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            PasosIndicador(pasoActual = 1)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { nuevoTerrenoViewModel.registrarTerreno { onConfirm() } },
                enabled = isValid && !state.isLoading,
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
                if (state.isLoading) {
                    CircularProgressIndicator(color = PlagOutColors.TextOnDark, modifier = Modifier.size(22.dp))
                } else {
                    Text("Guardar Terreno", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HeaderUbicacion(onBack: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderUbicacion")
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
                    "Nuevo terreno · Paso 2 de 2",
                    color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Text("Ubicación del Lote", color = PlagOutColors.TextOnDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
