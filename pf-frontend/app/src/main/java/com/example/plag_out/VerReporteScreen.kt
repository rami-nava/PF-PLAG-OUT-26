package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import com.example.plag_out.ui.theme.CargandoCentrado
import com.example.plag_out.ui.theme.NivelEstilo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import androidx.core.content.ContextCompat
import android.graphics.Color as AndroidColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val OsmTileSource = XYTileSource(
    "OSM_FR",
    0, 19, 256, ".png",
    arrayOf(
        "https://a.tile.openstreetmap.fr/osmfr/",
        "https://b.tile.openstreetmap.fr/osmfr/",
        "https://c.tile.openstreetmap.fr/osmfr/"
    )
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VerReporteScreen(
    reporteId: Int,
    reporteJsonFallback: String?,
    viewModel: VerReporteViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(reporteId) {
        viewModel.cargar(reporteId, reporteJsonFallback)
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (val s = state) {
                is VerReporteUiState.Cargando -> CargandoCentrado()

                is VerReporteUiState.Error -> ErrorVerReporte(
                    mensaje = s.mensaje,
                    onBack = onBack
                )

                is VerReporteUiState.Exito -> ContenidoVerReporte(
                    detalle = s.detalle,
                    terrenoReferencia = s.terrenoReferencia,
                    onBack = onBack
                )
            }
        }
    }
}

// ── Contenido principal ──────────────────────────────────────────────────────

@Composable
private fun ContenidoVerReporte(
    detalle: ReporteDetalleResponse,
    terrenoReferencia: TerrenoResponse?,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderVerReporte(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Card de Resumen ────────────────────────────────────────────────
            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Plaga reportada",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain
                        )

                        // Chip de tipo: Propio vs Reporte comunidad
                        val esPropio = detalle.es_propio
                        Surface(
                            color = if (esPropio) PlagOutColors.Forest.copy(alpha = 0.12f) else Color(0xFF1565C0).copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = if (esPropio) "Propio" else "Reporte comunidad",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (esPropio) PlagOutColors.Forest else Color(0xFF1565C0),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.BugReport,
                            contentDescription = null,
                            tint = PlagOutColors.Forest,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            detalle.plaga_nombre,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PlagOutColors.TextMain,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Card de Datos ──────────────────────────────────────────────────
            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Datos del reporte",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )

                    // Fecha y hora
                    val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
                    val fechaStr = sdf.format(Date(detalle.timestamp_ms))
                    FilaDato(
                        icono = Icons.Outlined.AccessTime,
                        etiqueta = "Fecha y hora",
                        valor = fechaStr
                    )

                    // Nivel de severidad
                    val estiloSeveridad = when (detalle.nivel_severidad) {
                        "Alto"  -> NivelEstilo(PlagOutColors.RiskDanger, Color(0xFFE4795C), "Alto", Icons.Filled.ErrorOutline)
                        "Medio" -> NivelEstilo(PlagOutColors.RiskWarn, PlagOutColors.Sun, "Medio", Icons.Filled.WarningAmber)
                        else    -> NivelEstilo(PlagOutColors.RiskOk, Color(0xFF8ACD86), "Bajo", Icons.Filled.CheckCircle)
                    }
                    FilaDatoSeveridad(
                        estilo = estiloSeveridad,
                        pulsante = detalle.nivel_severidad == "Alto"
                    )

                    // Coordenadas
                    val coordStr = if (detalle.latitud != null && detalle.longitud != null)
                        "Lat: ${"%.5f".format(detalle.latitud)}  ·  Lon: ${"%.5f".format(detalle.longitud)}"
                    else "Sin coordenadas registradas"
                    FilaDato(
                        icono = Icons.Outlined.LocationOn,
                        etiqueta = "Coordenadas",
                        valor = coordStr
                    )

                    // Terreno / Cercanía
                    val esPropio = detalle.es_propio
                    if (esPropio) {
                        val nom = detalle.terreno_nombre?.takeIf { it.isNotBlank() } ?: "Terreno no especificado"
                        FilaDato(
                            icono = Icons.Outlined.Landscape,
                            etiqueta = "Terreno asignado",
                            valor = nom
                        )
                    } else {
                        val nom = detalle.terreno_nombre?.takeIf { it.isNotBlank() }
                        val dist = detalle.distancia_km
                        val cercaniaStr = if (nom != null && dist != null) {
                            "Cercano a $nom, a ${formatearDistancia(dist)}"
                        } else if (nom != null) {
                            "Cercano a $nom"
                        } else if (dist != null) {
                            "A ${formatearDistancia(dist)} de tu terreno"
                        } else {
                            "Área cercana"
                        }
                        FilaDato(
                            icono = Icons.Filled.Group,
                            etiqueta = "Terreno más cercano",
                            valor = cercaniaStr,
                            colorIcono = Color(0xFF1565C0)
                        )
                    }
                }
            }

            // ── Mapa compacto ──────────────────────────────────────────────────
            if (detalle.latitud != null && detalle.longitud != null) {
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "Ubicación del reporte",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain
                        )
                        Spacer(Modifier.height(14.dp))

                        val context = LocalContext.current
                        val geoPoint = GeoPoint(detalle.latitud, detalle.longitud)
                        var mostrarMapaExpandido by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("mapaReporte"),
                                factory = { ctx ->
                                    Configuration.getInstance().userAgentValue =
                                        "com.example.plag_out/1.0.1 (Android; App Agro; contacto@plagout.app)"
                                    Configuration.getInstance().load(
                                        ctx,
                                        ctx.getSharedPreferences("plag_out_prefs", android.content.Context.MODE_PRIVATE)
                                    )

                                    Configuration.getInstance().cacheMapTileCount = 12
                                    Configuration.getInstance().cacheMapTileOvershoot = 2

                                    MapView(ctx).apply {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            clipToOutline = true
                                        }
                                        setMultiTouchControls(false)   // solo lectura
                                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                        setTileSource(OsmTileSource)
                                        controller.setZoom(13.0)
                                        controller.setCenter(geoPoint)

                                        val marker = Marker(this)
                                        marker.position = geoPoint
                                        marker.title = "Ubicación del reporte"
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        marker.icon = MapMarkerUtils.getMarkerIcon(ctx, isGreen = false)
                                        overlays.add(marker)

                                        onResume()
                                    }
                                },
                                update = { map ->
                                    map.controller.setCenter(geoPoint)
                                    map.overlays.removeAll { it is Marker }
                                    val marker = Marker(map)
                                    marker.position = geoPoint
                                    marker.title = "Ubicación del reporte"
                                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    marker.icon = MapMarkerUtils.getMarkerIcon(map.context, isGreen = false)
                                    map.overlays.add(marker)
                                    map.invalidate()
                                }
                            )

                            // Capa transparente para atrapar los toques y abrir el modal
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { mostrarMapaExpandido = true }
                            )
                        }

                        if (mostrarMapaExpandido) {
                            Dialog(
                                onDismissRequest = { mostrarMapaExpandido = false },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = PlagOutColors.Cream
                                ) {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .systemBarsPadding()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(PlagOutColors.Surface)
                                                .padding(horizontal = 8.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { mostrarMapaExpandido = false }) {
                                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = PlagOutColors.TextMain)
                                            }
                                            Text(
                                                "Ubicación del reporte",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PlagOutColors.TextMain,
                                                modifier = Modifier.padding(start = 8.dp)
                                            )
                                        }

                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                            AndroidView(
                                                modifier = Modifier.fillMaxSize(),
                                                factory = { ctx ->
                                                    Configuration.getInstance().userAgentValue = "com.example.plag_out/1.0.1 (Android; App Agro; contacto@plagout.app)"
                                                    Configuration.getInstance().cacheMapTileCount = 12
                                                    Configuration.getInstance().cacheMapTileOvershoot = 2

                                                    MapView(ctx).apply {
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                            clipToOutline = true
                                                        }
                                                        setMultiTouchControls(true)
                                                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                                        setTileSource(OsmTileSource)
                                                        
                                                        val marker = Marker(this)
                                                        marker.position = geoPoint
                                                        marker.title = "${detalle.plaga_nombre} - Severidad: ${detalle.nivel_severidad}"
                                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                        marker.icon = MapMarkerUtils.getMarkerIcon(ctx, isGreen = false)
                                                        overlays.add(marker)

                                                        if (!detalle.es_propio && terrenoReferencia != null) {
                                                            val terrenoGeoPoint = GeoPoint(terrenoReferencia.terreno_latitud.toDouble(), terrenoReferencia.terreno_longitud.toDouble())
                                                            
                                                            val markerTerreno = Marker(this)
                                                            markerTerreno.position = terrenoGeoPoint
                                                            markerTerreno.title = "Tu lote: ${terrenoReferencia.terreno_nombre}"
                                                            markerTerreno.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                                            markerTerreno.icon = MapMarkerUtils.getMarkerIcon(ctx, isGreen = true)
                                                            overlays.add(markerTerreno)

                                                            val polyline = Polyline(this)
                                                            polyline.addPoint(geoPoint)
                                                            polyline.addPoint(terrenoGeoPoint)
                                                            polyline.outlinePaint.color = AndroidColor.BLUE
                                                            polyline.outlinePaint.strokeWidth = 5f
                                                            overlays.add(polyline)
                                                            
                                                            post {
                                                                val box = BoundingBox.fromGeoPoints(listOf(geoPoint, terrenoGeoPoint))
                                                                this@apply.zoomToBoundingBox(box.increaseByScale(1.2f), true)
                                                            }
                                                        } else {
                                                            controller.setZoom(14.0)
                                                            controller.setCenter(geoPoint)
                                                        }
                                                    }
                                                }
                                            )
                                            
                                            // Card Flotante
                                            if (!detalle.es_propio && terrenoReferencia != null && detalle.distancia_km != null) {
                                                androidx.compose.material3.Card(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = PlagOutColors.Surface)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PlagOutColors.Forest)
                                                        Spacer(Modifier.width(8.dp))
                                                        val distKm = detalle.distancia_km
                                                        val textDist = formatearDistancia(distKm)

                                                        Text(
                                                            "A $textDist de tu lote ${terrenoReferencia.terreno_nombre}",
                                                            color = PlagOutColors.TextMain,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatearDistancia(km: Float): String {
    return if (km < 1.0f) {
        "${(km * 1000).toInt()} m"
    } else if (km >= 10.0f) {
        String.format(Locale.getDefault(), "%.0f km", km)
    } else {
        String.format(Locale.getDefault(), "%.1f km", km)
    }
}

// ── Fila de dato ─────────────────────────────────────────────────────────────

@Composable
private fun FilaDato(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    valor: String,
    colorIcono: Color = PlagOutColors.Forest
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icono,
                contentDescription = null,
                tint = colorIcono,
                modifier = Modifier.size(18.dp)
            )
            Text(
                etiqueta,
                fontSize = 14.sp,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            valor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PlagOutColors.TextMain,
            modifier = Modifier.padding(start = 26.dp)
        )
    }
}

@Composable
private fun FilaDatoSeveridad(
    estilo: NivelEstilo,
    pulsante: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint = estilo.color,
                modifier = Modifier.size(18.dp)
            )
            Text(
                "Nivel de severidad",
                fontSize = 14.sp,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(modifier = Modifier.padding(start = 26.dp)) {
            SelloDeNivel(
                estilo = estilo,
                pulsante = pulsante,
                modifier = Modifier.testTag("chipSeveridad")
            )
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun HeaderVerReporte(onBack: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderReporte")
    val escala by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativaVerReporte"
    )

    val forma = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(1f)
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = forma
            )
            .clip(forma)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // Decoración animada
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-40).dp)
                .size(50.dp)
                .graphicsLayer { scaleX = escala; scaleY = escala }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )

        Row(
            modifier = Modifier.padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btnVolverVerReporte")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = PlagOutColors.TextOnDark
                )
            }
            Column {
                Text(
                    "Reportes colaborativos",
                    color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
                Text(
                    "Detalle de Reporte",
                    color = PlagOutColors.TextOnDark,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

// ── Estado de error ───────────────────────────────────────────────────────────

@Composable
private fun ErrorVerReporte(mensaje: String, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = PlagOutColors.TextMain
            )
        }
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = PlagOutColors.RiskUnknown,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            mensaje,
            color = PlagOutColors.TextSecondary,
            fontSize = 15.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Volver", fontWeight = FontWeight.Bold)
        }
    }
}
