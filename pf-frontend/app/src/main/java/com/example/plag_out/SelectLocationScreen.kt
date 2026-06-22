package com.example.plag_out

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectLocationScreen(
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val state by nuevoTerrenoViewModel.state.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(1) }
    val tabs = listOf("🗺️ Mapa", "🔢 Coordenadas")

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

        if(lat != null && lon != null){
            selectedLocation = GeoPoint(lat, lon)
        }

    }

    //SI NO TENGO PERMISO PARA ACCEDER A SU UBICACION SE LO PIDO
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {granted ->
            if(granted){
                isGranted = true
            }else{
                isGranted = false
            }
        }
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
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {

                    AndroidView(

                        modifier = Modifier.fillMaxSize(),

                        factory = { ctx ->

                            Configuration.getInstance().load(
                                ctx,
                                PreferenceManager.getDefaultSharedPreferences(ctx)
                            )

                            MapView(ctx).apply {

                                setMultiTouchControls(true)

                                setTileSource(
                                    TileSourceFactory.MAPNIK
                                )

                                controller.setZoom(5.0)

                                controller.setCenter(
                                    GeoPoint(
                                        -34.6037,
                                        -58.3816
                                    )
                                )

                                val receiver =
                                    object : MapEventsReceiver {

                                        override fun singleTapConfirmedHelper(
                                            p: GeoPoint?
                                        ): Boolean {

                                            p?.let {

                                                selectedLocation = it

                                                latInput =
                                                    it.latitude.toString()

                                                lonInput =
                                                    it.longitude.toString()
                                            }

                                            return true
                                        }

                                        override fun longPressHelper(
                                            p: GeoPoint?
                                        ): Boolean {
                                            return false
                                        }
                                    }

                                overlays.add(
                                    MapEventsOverlay(receiver)
                                )
                            }
                        },

                        update = { map ->

                            map.overlays.removeAll {
                                it is Marker
                            }

                            val receiver =
                                map.overlays.firstOrNull {
                                    it is MapEventsOverlay
                                }

                            map.overlays.clear()

                            receiver?.let {
                                map.overlays.add(it)
                            }

                            selectedLocation?.let { point ->

                                val marker = Marker(map)

                                marker.position = point

                                marker.title =
                                    "Ubicación del terreno"

                                marker.setAnchor(
                                    Marker.ANCHOR_CENTER,
                                    Marker.ANCHOR_BOTTOM
                                )

                                map.overlays.add(marker)
                            }

                            map.invalidate()
                        }
                    )
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
                        Button(
                            onClick = {
                                if (isGranted) {

                                    obteniendoUbicacion = true

                                    fusedLocationClient
                                        .getCurrentLocation(
                                            Priority.PRIORITY_HIGH_ACCURACY,
                                            null
                                        )
                                        .addOnSuccessListener { location ->

                                            obteniendoUbicacion = false

                                            location?.let {
                                                latInput = it.latitude.toString()
                                                lonInput = it.longitude.toString()
                                            }
                                        }
                                        .addOnFailureListener {
                                            obteniendoUbicacion = false
                                        }

                                } else {
                                    launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            enabled = !obteniendoUbicacion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (obteniendoUbicacion) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("📍 Usar ubicación actual")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Alerta error
        if (showError || state.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color(0xFFE53E3E).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (state.error != null) "⚠️ ${state.error}" else "⚠️ Ubicación fuera de la República Argentina.\nLatitud permitida: -55.0 a -21.8\nLongitud permitida: -73.6 a -53.6",
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
                    text = "Terreno: ${state.nombre.ifEmpty { "-" }}",
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
            onClick = { nuevoTerrenoViewModel.registrarTerreno { onConfirm() } },
            enabled = isValid && !state.isLoading,
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
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "💾 Guardar Terreno",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}