package com.example.plag_out

import android.os.Build
import android.preference.PreferenceManager
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.plag_out.ui.theme.CargandoCentrado
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.estiloDeNivel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    Scaffold(containerColor = PlagOutColors.Cream) { padding ->
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Reporte de Plaga",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Outlined.BugReport,
                            contentDescription = null,
                            tint = PlagOutColors.Forest,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            detalle.plaga_nombre,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PlagOutColors.TextMain,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Chip de severidad
                    val nivelInt = when (detalle.nivel_severidad) {
                        "Alto"  -> 2
                        "Medio" -> 1
                        else    -> 0   // "Bajo"
                    }
                    val estilo = estiloDeNivel(nivelInt)
                    SelloDeNivel(
                        estilo = estilo,
                        pulsante = nivelInt >= 2,
                        modifier = Modifier.testTag("chipSeveridad")
                    )
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Datos del Reporte",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.TextSecondary,
                        letterSpacing = 0.5.sp
                    )

                    // Fecha y hora
                    val sdf = SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault())
                    val fechaStr = sdf.format(Date(detalle.timestamp_ms))
                    FilaDato(
                        icono = Icons.Outlined.AccessTime,
                        etiqueta = "Fecha y hora",
                        valor = fechaStr
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

                    // Badge colaborativo
                    EtiquetaInfo(
                        icono = Icons.Filled.Group,
                        texto = "Reporte Colaborativo",
                        color = PlagOutColors.Forest
                    )
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
                            "Ubicación del Reporte",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlagOutColors.TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        val context = LocalContext.current
                        val geoPoint = GeoPoint(detalle.latitud, detalle.longitud)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = PlagOutColors.CreamDeep
                        ) {
                            AndroidView(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("mapaReporte"),
                                factory = { ctx ->
                                    Configuration.getInstance().userAgentValue =
                                        "PlagOutMobileApp/1.0.1 (contacto@mi-app.com)"
                                    Configuration.getInstance().load(
                                        ctx,
                                        ctx.getSharedPreferences("plag_out_prefs", android.content.Context.MODE_PRIVATE)
                                    )

                                    val cartoSource = XYTileSource(
                                        "CartoDB-Positron",
                                        0, 19, 256, ".png",
                                        arrayOf(
                                            "https://a.basemaps.cartocdn.com/light_all/",
                                            "https://b.basemaps.cartocdn.com/light_all/",
                                            "https://c.basemaps.cartocdn.com/light_all/"
                                        )
                                    )

                                    MapView(ctx).apply {
                                        setMultiTouchControls(false)   // solo lectura
                                        setTileSource(cartoSource)
                                        controller.setZoom(13.0)
                                        controller.setCenter(geoPoint)

                                        val marker = Marker(this)
                                        marker.position = geoPoint
                                        marker.title = "Ubicación del reporte"
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
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
                                    map.overlays.add(marker)
                                    map.invalidate()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Fila de dato ─────────────────────────────────────────────────────────────

@Composable
private fun FilaDato(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    etiqueta: String,
    valor: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icono,
            contentDescription = null,
            tint = PlagOutColors.Forest,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                etiqueta,
                fontSize = 11.sp,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                valor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PlagOutColors.TextMain
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
