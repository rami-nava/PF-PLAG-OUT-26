package com.example.plag_out

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BubbleChart
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.EncabezadoGrupoFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.estiloDeNivel
import com.google.gson.Gson
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polygon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


private const val TODOS = "Todos"
private const val SIN_LIMITE = -1

private val RADIOS_MAPA_KM = listOf(25, 50, 100)
private val VENTANAS_DIAS = listOf(7, 30, 90)

private const val AMBITO_TODOS = 0
private const val AMBITO_PROPIOS = 1
private const val AMBITO_COMUNIDAD = 2

private const val MS_POR_DIA = 24L * 60L * 60L * 1000L

private const val CELDA_CLUSTER_PX = 78.0
private const val CELDA_NUBE_PX = 120.0

private val CENTRO_PAIS = GeoPoint(-38.4, -63.6)
private const val ZOOM_PAIS = 4.2

private const val ZOOM_NOMBRE_LOTE = 10.0
private const val ZOOM_SUPERFICIE_LOTE = 12.0

private val AzulMapa = Color(0xFF1565C0)

private const val FILAS_VISIBLES_SELECCION = 3

enum class ModoMapa { PINES, NUBES }

// ── Pantalla ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MapaReportesScreen(
    viewModel: MisReportesViewModel,
    terrenosViewModel: TerrenosViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val terrenosState by terrenosViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarReportes()
        viewModel.cargarCatalogoPlagas()
        terrenosViewModel.getTerrenos()
    }

    val terrenos = terrenosState.terrenos

    var modo by rememberSaveable { mutableStateOf(ModoMapa.PINES) }
    var mostrarFiltros by rememberSaveable { mutableStateOf(false) }

    var ambito by rememberSaveable { mutableIntStateOf(AMBITO_TODOS) }
    var radioKm by rememberSaveable { mutableIntStateOf(SIN_LIMITE) }
    var dias by rememberSaveable { mutableIntStateOf(SIN_LIMITE) }
    var severidad by rememberSaveable { mutableStateOf(TODOS) }
    var plaga by rememberSaveable { mutableStateOf(TODOS) }
    var cultivo by rememberSaveable { mutableStateOf(TODOS) }

    // Un reporte sin coordenadas no puede dibujarse; se cuenta aparte para poder avisarlo.
    val ubicados = remember(state.reportes) {
        state.reportes.filter { it.latitud != null && it.longitud != null }
    }
    val sinUbicacion = state.reportes.size - ubicados.size

    // El backend solo manda distancia_km en los reportes ajenos. Para que el filtro de cercanía
    // valga también sobre los propios, se completa con la distancia al lote más cercano.
    val distancias = remember(ubicados, terrenos) {
        ubicados.associate { it.id to distanciaAlLoteMasCercano(it, terrenos) }
    }

    val ahora = remember(state.reportes) { System.currentTimeMillis() }

    val plagasDisponibles = remember(ubicados, state.catalogoPlagas) {
        val enReportes = ubicados.map { it.plaga_nombre }.filter { it.isNotBlank() }.toSet()
        (state.catalogoPlagas + enReportes)
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(compareByDescending<String> { it in enReportes }.thenBy { it.lowercase() })
    }
    val cultivosDisponibles = remember(ubicados) {
        ubicados.mapNotNull { it.cultivo_nombre }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filtrados = remember(ubicados, distancias, ambito, radioKm, dias, severidad, plaga, cultivo) {
        ubicados.filter { r ->
            val okAmbito = when (ambito) {
                AMBITO_PROPIOS -> r.es_propio
                AMBITO_COMUNIDAD -> !r.es_propio
                else -> true
            }
            val okRadio = radioKm == SIN_LIMITE || (distancias[r.id]?.let { it <= radioKm } ?: false)
            val okFecha = dias == SIN_LIMITE || (ahora - r.timestamp_ms) <= dias * MS_POR_DIA
            val okSeveridad = severidad == TODOS || r.nivel_severidad.equals(severidad, ignoreCase = true)
            val okPlaga = plaga == TODOS || r.plaga_nombre.equals(plaga, ignoreCase = true)
            val okCultivo = cultivo == TODOS || (r.cultivo_nombre ?: "").equals(cultivo, ignoreCase = true)
            okAmbito && okRadio && okFecha && okSeveridad && okPlaga && okCultivo
        }
    }

    val filtrosActivos = listOf(
        ambito != AMBITO_TODOS,
        radioKm != SIN_LIMITE,
        dias != SIN_LIMITE,
        severidad != TODOS,
        plaga != TODOS,
        cultivo != TODOS
    ).count { it }

    val limpiarFiltros = {
        ambito = AMBITO_TODOS
        radioKm = SIN_LIMITE
        dias = SIN_LIMITE
        severidad = TODOS
        plaga = TODOS
        cultivo = TODOS
    }

    // El zoom lo publica el propio mapa: la agrupación tiene que rehacerse cada vez que cambia,
    // porque el tamaño de celda está expresado en píxeles de pantalla.
    var zoom by remember { mutableStateOf(ZOOM_PAIS) }
    var seleccion by remember { mutableStateOf<ClusterReportes?>(null) }
    val mapaRef = remember { mutableStateOf<MapView?>(null) }

    val clusters = remember(filtrados, zoom, modo) {
        agruparReportes(
            filtrados,
            zoom,
            if (modo == ModoMapa.NUBES) CELDA_NUBE_PX else CELDA_CLUSTER_PX
        )
    }

    LaunchedEffect(clusters) {
        val vigente = seleccion?.id
        if (vigente != null && clusters.none { it.id == vigente }) seleccion = null
    }

    // La pantalla es el mapa: todo lo demás flota encima. Sin reportes el mapa igual se ve
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        LienzoMapa(
            clusters = clusters,
            terrenos = terrenos,
            modo = modo,
            radioKm = radioKm,
            seleccionId = seleccion?.id,
            onZoom = { zoom = it },
            onSeleccion = { seleccion = it },
            onMapaListo = { mapaRef.value = it },
            zoom = zoom,
            modifier = Modifier
                .fillMaxSize()
                .testTag("mapaReportes")
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotonMapa(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Volver",
                onBack,
                "btnVolverMapaReportes"
            )
            BotonFiltrar(
                cantidadActiva = filtrosActivos,
                onClick = { mostrarFiltros = true }
            )
        }

        ControlesMapa(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(12.dp),
            onZoomIn = { mapaRef.value?.controller?.zoomIn() },
            onZoomOut = { mapaRef.value?.controller?.zoomOut() },
            onCentrar = {
                mapaRef.value?.let { mapa ->
                    encuadrar(mapa, clusters, terrenos)
                    zoom = mapa.zoomLevelDouble
                }
            }
        )

        val aviso = when {
            state.isLoading -> "Cargando reportes…"
            ubicados.isEmpty() -> "Todavía no hay reportes con ubicación"
            filtrados.isEmpty() -> "Ningún reporte entra en los filtros"
            sinUbicacion > 0 -> "$sinUbicacion ${if (sinUbicacion == 1) "reporte" else "reportes"} sin ubicación"
            else -> null
        }
        if (aviso != null) {
            AvisoFlotante(
                texto = aviso,
                cargando = state.isLoading,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 74.dp, start = 16.dp, end = 16.dp)
            )
        }

        LeyendaMapa(
            modo = modo,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(12.dp)
        )

        PildoraModoMapa(
            modo = modo,
            onModoChange = { modo = it; seleccion = null },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(12.dp)
        )

        SeleccionFlotante(
            cluster = seleccion,
            distancias = distancias,
            onCerrar = { seleccion = null },
            onAcercar = { cluster ->
                mapaRef.value?.let { mapa ->
                    mapa.controller.animateTo(GeoPoint(cluster.latitud, cluster.longitud))
                    mapa.controller.setZoom(mapa.zoomLevelDouble + 2.0)
                }
            },
            onAbrirReporte = { reporte ->
                val payload = ReporteNavPayload(
                    id = reporte.id,
                    plaga_nombre = reporte.plaga_nombre,
                    nivel_severidad = reporte.nivel_severidad,
                    latitud = reporte.latitud,
                    longitud = reporte.longitud,
                    timestamp_ms = reporte.timestamp_ms
                )
                val json = Uri.encode(Gson().toJson(payload))
                navController.navigate("ver_reporte/${reporte.id}/$json")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }

    if (mostrarFiltros) {
        HojaFiltrosMapa(
            onCerrar = { mostrarFiltros = false },
            totalVisible = filtrados.size,
            hayFiltroActivo = filtrosActivos > 0,
            onLimpiar = limpiarFiltros,
            plagasDisponibles = plagasDisponibles,
            cultivosDisponibles = cultivosDisponibles,
            sinTerrenos = terrenos.isEmpty(),
            ambito = ambito,
            onAmbito = { ambito = if (ambito == it) AMBITO_TODOS else it },
            radioKm = radioKm,
            onRadio = { radioKm = if (radioKm == it) SIN_LIMITE else it },
            dias = dias,
            onDias = { dias = if (dias == it) SIN_LIMITE else it },
            severidad = severidad,
            onSeveridad = { severidad = if (severidad.equals(it, true)) TODOS else it },
            plaga = plaga,
            onPlaga = { plaga = if (plaga == it) TODOS else it },
            cultivo = cultivo,
            onCultivo = { cultivo = if (cultivo == it) TODOS else it }
        )
    }
}

// ── Hoja de filtros ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun HojaFiltrosMapa(
    onCerrar: () -> Unit,
    totalVisible: Int,
    hayFiltroActivo: Boolean,
    onLimpiar: () -> Unit,
    plagasDisponibles: List<String>,
    cultivosDisponibles: List<String>,
    sinTerrenos: Boolean,
    ambito: Int,
    onAmbito: (Int) -> Unit,
    radioKm: Int,
    onRadio: (Int) -> Unit,
    dias: Int,
    onDias: (Int) -> Unit,
    severidad: String,
    onSeveridad: (String) -> Unit,
    plaga: String,
    onPlaga: (String) -> Unit,
    cultivo: String,
    onCultivo: (String) -> Unit
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = estadoHoja,
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = { WindowInsets.systemBars },
        dragHandle = { BottomSheetDefaults.DragHandle(color = PlagOutColors.Divider) }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .testTag("hojaFiltrosMapa")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Filtrar el mapa", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
                    Text(
                        "Tocá una celda elegida para quitar ese filtro",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary
                    )
                }
                if (hayFiltroActivo) {
                    Surface(
                        onClick = onLimpiar,
                        shape = CircleShape,
                        color = PlagOutColors.RiskDanger.copy(alpha = 0.1f),
                        modifier = Modifier.testTag("btnLimpiarFiltrosMapa")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Limpiar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.RiskDanger)
                        }
                    }
                }
            }

            EncabezadoGrupoFiltro(Icons.Outlined.Groups, "ÁMBITO")
            GrillaFiltro(
                listOf(
                    CeldaFiltro(
                        etiqueta = "Míos",
                        icono = Icons.Outlined.Person,
                        color = PlagOutColors.Forest,
                        activa = ambito == AMBITO_PROPIOS,
                        onClick = { onAmbito(AMBITO_PROPIOS) }
                    ),
                    CeldaFiltro(
                        etiqueta = "Comunidad",
                        icono = Icons.Filled.Group,
                        color = AzulMapa,
                        activa = ambito == AMBITO_COMUNIDAD,
                        onClick = { onAmbito(AMBITO_COMUNIDAD) }
                    )
                )
            )

            EncabezadoGrupoFiltro(Icons.Outlined.Explore, "CERCANÍA A MIS LOTES")
            GrillaFiltro(
                RADIOS_MAPA_KM.map { km ->
                    CeldaFiltro(
                        etiqueta = "A menos de\n$km km",
                        icono = Icons.Outlined.NearMe,
                        color = AzulMapa,
                        activa = radioKm == km,
                        onClick = { onRadio(km) }
                    )
                }
            )
            if (sinTerrenos) {
                NotaFiltro("Cargá un terreno para poder filtrar por cercanía.")
            }

            // Una plaga reportada hace medio año no describe el riesgo de hoy: por eso la ventana
            // temporal es un filtro de primera línea y no un detalle escondido.
            EncabezadoGrupoFiltro(Icons.Outlined.AccessTime, "ANTIGÜEDAD")
            GrillaFiltro(
                VENTANAS_DIAS.map { d ->
                    CeldaFiltro(
                        etiqueta = "Últimos\n$d días",
                        icono = Icons.Outlined.AccessTime,
                        color = PlagOutColors.Bark,
                        activa = dias == d,
                        onClick = { onDias(d) }
                    )
                }
            )

            EncabezadoGrupoFiltro(Icons.Outlined.Shield, "NIVEL DE SEVERIDAD")
            GrillaFiltro(
                listOf(
                    Triple("Alto", Icons.Default.ErrorOutline, Color(0xFFC62828)),
                    Triple("Medio", Icons.Default.WarningAmber, Color(0xFFEF6C00)),
                    Triple("Bajo", Icons.Default.CheckCircle, Color(0xFF2E7D32))
                ).map { (nivel, icono, color) ->
                    CeldaFiltro(
                        etiqueta = nivel,
                        icono = icono,
                        color = color,
                        activa = severidad.equals(nivel, true),
                        onClick = { onSeveridad(nivel) }
                    )
                }
            )

            if (plagasDisponibles.isNotEmpty()) {
                EncabezadoGrupoFiltro(Icons.Outlined.BugReport, "PLAGA")
                GrillaFiltro(
                    plagasDisponibles.map { nombre ->
                        CeldaFiltro(
                            etiqueta = nombre,
                            icono = Icons.Outlined.BugReport,
                            color = PlagOutColors.Forest,
                            activa = plaga == nombre,
                            onClick = { onPlaga(nombre) }
                        )
                    }
                )
            }

            if (cultivosDisponibles.isNotEmpty()) {
                EncabezadoGrupoFiltro(Icons.Outlined.Grass, "CULTIVO")
                GrillaFiltro(
                    cultivosDisponibles.map { nombre ->
                        CeldaFiltro(
                            etiqueta = nombre,
                            icono = Icons.Outlined.Grass,
                            color = PlagOutColors.Leaf,
                            activa = cultivo == nombre,
                            onClick = { onCultivo(nombre) }
                        )
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = onCerrar,
                shape = RoundedCornerShape(18.dp),
                color = PlagOutColors.Forest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .testTag("btnAplicarFiltrosMapa")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        when (totalVisible) {
                            0 -> "Ningún reporte coincide"
                            1 -> "Ver 1 reporte en el mapa"
                            else -> "Ver $totalVisible reportes en el mapa"
                        },
                        color = PlagOutColors.TextOnDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class CeldaFiltro(
    val etiqueta: String,
    val icono: ImageVector,
    val color: Color,
    val activa: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun GrillaFiltro(
    celdas: List<CeldaFiltro>,
    columnas: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        celdas.chunked(columnas).forEach { fila ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                fila.forEach { celda -> VistaCeldaFiltro(celda, Modifier.weight(1f)) }
                repeat(columnas - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun VistaCeldaFiltro(celda: CeldaFiltro, modifier: Modifier = Modifier) {
    val fondo by animateColorAsState(
        targetValue = if (celda.activa) celda.color.copy(alpha = 0.14f) else PlagOutColors.Surface,
        animationSpec = tween(220),
        label = "fondoCeldaFiltro"
    )
    val borde by animateColorAsState(
        targetValue = if (celda.activa) celda.color else PlagOutColors.Divider,
        animationSpec = tween(220),
        label = "bordeCeldaFiltro"
    )
    Surface(
        onClick = celda.onClick,
        shape = RoundedCornerShape(16.dp),
        color = fondo,
        border = BorderStroke(if (celda.activa) 2.dp else 1.dp, borde),
        modifier = modifier.height(76.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                celda.icono,
                contentDescription = null,
                tint = if (celda.activa) celda.color else PlagOutColors.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(5.dp))
            Text(
                celda.etiqueta,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NotaFiltro(texto: String) {
    Text(
        texto,
        fontSize = 12.sp,
        color = PlagOutColors.TextSecondary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp)
    )
}


@Composable
private fun AvisoFlotante(texto: String, cargando: Boolean, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = PlagOutColors.Surface.copy(alpha = 0.95f),
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, PlagOutColors.Divider),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    color = PlagOutColors.Forest,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = PlagOutColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                texto,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = PlagOutColors.TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Mapa ──────────────────────────────────────────────────────────────────────

@Composable
private fun LienzoMapa(
    clusters: List<ClusterReportes>,
    terrenos: List<TerrenoResponse>,
    modo: ModoMapa,
    radioKm: Int,
    seleccionId: String?,
    onZoom: (Double) -> Unit,
    onSeleccion: (ClusterReportes) -> Unit,
    onMapaListo: (MapView) -> Unit,
    zoom: Double,
    modifier: Modifier = Modifier
) {
    val densidad = LocalContext.current.resources.displayMetrics.density

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().userAgentValue =
                "com.example.plag_out/1.0.1 (Android; App Agro; contacto@plagout.app)"
            Configuration.getInstance().load(
                ctx,
                ctx.getSharedPreferences("plag_out_prefs", Context.MODE_PRIVATE)
            )
            MapView(ctx).apply {
                setTileSource(OsmTileSource)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                setUseDataConnection(true)
                minZoomLevel = 3.0
                controller.setZoom(ZOOM_PAIS)
                controller.setCenter(CENTRO_PAIS)
                addMapListener(
                    DelayedMapListener(
                        object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean = false
                            override fun onZoom(event: ZoomEvent?): Boolean {
                                onZoom(zoomLevelDouble)
                                return false
                            }
                        },
                        180
                    )
                )
                onResume()
                onMapaListo(this)
            }
        },
        update = { map ->
            map.overlays.clear()
            val nombreVisible = zoom >= ZOOM_NOMBRE_LOTE
            terrenos.forEach { terreno ->
                val centro = GeoPoint(terreno.terreno_latitud.toDouble(), terreno.terreno_longitud.toDouble())
                if (radioKm != SIN_LIMITE) {
                    val anillo = Polygon(map).apply {
                        points = Polygon.pointsAsCircle(centro, radioKm * 1000.0)
                        fillPaint.color = 0x14264A2B
                        outlinePaint.color = 0x66264A2B
                        outlinePaint.strokeWidth = 2f * densidad
                        setOnClickListener { _, _, _ -> false }
                    }
                    map.overlays.add(anillo)
                }
                if (zoom >= ZOOM_SUPERFICIE_LOTE) {
                    val lote = Polygon(map).apply {
                        points = Polygon.pointsAsCircle(centro, radioLotePorArea(terreno.terreno_area))
                        fillPaint.color = 0x554C7A3D
                        outlinePaint.color = 0xFF264A2B.toInt()
                        outlinePaint.strokeWidth = 2f * densidad
                        title = terreno.terreno_nombre
                        setOnClickListener { _, _, _ -> false }
                    }
                    map.overlays.add(lote)
                }
                val insignia = Marker(map).apply {
                    position = centro
                    setInfoWindow(null)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = iconoTerreno(map.context, terreno.terreno_nombre, nombreVisible)
                    setOnMarkerClickListener { _, _ -> true }
                }
                map.overlays.add(insignia)
            }

            when (modo) {
                ModoMapa.NUBES -> {
                    map.overlays.add(
                        NubesReportesOverlay(
                            nubes = clusters.map { it.aNube(densidad) },
                            onTap = onSeleccion
                        )
                    )
                }
                ModoMapa.PINES -> {
                    clusters.forEach { cluster ->
                        val marcador = Marker(map).apply {
                            position = GeoPoint(cluster.latitud, cluster.longitud)
                            setInfoWindow(null)
                            if (cluster.cantidad > 1) {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                icon = iconoCluster(
                                    map.context,
                                    cluster.cantidad,
                                    colorSeveridad(cluster.severidadDominante),
                                    resaltado = cluster.id == seleccionId
                                )
                            } else {
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                icon = MapMarkerUtils.getMarkerIcon(
                                    map.context,
                                    hueSeveridad(cluster.severidadDominante)
                                )
                            }
                            setOnMarkerClickListener { _, _ ->
                                onSeleccion(cluster)
                                true
                            }
                        }
                        map.overlays.add(marcador)
                    }
                }
            }

            map.invalidate()
        },
        onRelease = { it.onDetach() }
    )
}

private fun encuadrar(map: MapView, clusters: List<ClusterReportes>, terrenos: List<TerrenoResponse>) {
    val puntos = mutableListOf<GeoPoint>()
    clusters.forEach { c -> c.reportes.forEach { r ->
        if (r.latitud != null && r.longitud != null) puntos.add(GeoPoint(r.latitud, r.longitud))
    } }
    terrenos.forEach { puntos.add(GeoPoint(it.terreno_latitud.toDouble(), it.terreno_longitud.toDouble())) }

    when {
        puntos.isEmpty() -> return
        puntos.size == 1 -> {
            map.controller.setZoom(13.0)
            map.controller.setCenter(puntos.first())
        }
        else -> {
            val caja = BoundingBox.fromGeoPoints(puntos)
            val degenerada = abs(caja.latNorth - caja.latSouth) < 1e-4 && abs(caja.lonEast - caja.lonWest) < 1e-4
            if (degenerada) {
                map.controller.setZoom(13.0)
                map.controller.setCenter(GeoPoint(caja.centerLatitude, caja.centerLongitude))
            } else {
                map.zoomToBoundingBox(caja.increaseByScale(1.35f), true)
            }
        }
    }
}

// ── Agrupación ────────────────────────────────────────────────────────────────

private data class ClusterReportes(
    val id: String,
    val latitud: Double,
    val longitud: Double,
    val reportes: List<ReporteDetalleResponse>
) {
    val cantidad: Int get() = reportes.size

    /** El peor nivel presente: en un grupo, lo que define el riesgo es el reporte más grave. */
    val severidadDominante: String
        get() = when {
            reportes.any { it.nivel_severidad.equals("Alto", true) } -> "Alto"
            reportes.any { it.nivel_severidad.equals("Medio", true) } -> "Medio"
            else -> "Bajo"
        }

    fun aNube(densidad: Float): NubeReportes {
        val fraccion = fraccionDensidad(cantidad)
        val radio = ((34f + 17f * sqrt(cantidad.toFloat())) * densidad).coerceAtMost(150f * densidad)
        val alfa = (160 + 75 * fraccion).toInt().coerceIn(160, 235)
        return NubeReportes(latitud, longitud, radio, colorDensidad(fraccion), alfa, this)
    }
}


private fun agruparReportes(
    reportes: List<ReporteDetalleResponse>,
    zoom: Double,
    ladoPx: Double
): List<ClusterReportes> {
    if (reportes.isEmpty()) return emptyList()

    val gradosPorPixel = 360.0 / (256.0 * 2.0.pow(zoom))
    val celdaLon = (gradosPorPixel * ladoPx).coerceAtLeast(1e-7)
    val latReferencia = reportes.mapNotNull { it.latitud }.average()
    val celdaLat = (celdaLon * cos(Math.toRadians(latReferencia)).coerceAtLeast(0.05)).coerceAtLeast(1e-7)

    return reportes
        .groupBy { r ->
            val fila = floor((r.latitud ?: 0.0) / celdaLat).toLong()
            val columna = floor((r.longitud ?: 0.0) / celdaLon).toLong()
            fila to columna
        }
        .map { (clave, lista) ->
            ClusterReportes(
                id = "${clave.first}:${clave.second}",
                latitud = lista.mapNotNull { it.latitud }.average(),
                longitud = lista.mapNotNull { it.longitud }.average(),
                reportes = lista.sortedByDescending { it.timestamp_ms }
            )
        }
}

// ── Capa de nubes ─────────────────────────────────────────────────────────────

private data class NubeReportes(
    val latitud: Double,
    val longitud: Double,
    val radioPx: Float,
    val color: Int,
    val alfa: Int,
    val cluster: ClusterReportes
)

private class NubesReportesOverlay(
    private val nubes: List<NubeReportes>,
    private val onTap: (ClusterReportes) -> Unit
) : Overlay() {

    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val punto = Point()

    override fun draw(pCanvas: Canvas, pProjection: Projection) {
        nubes.forEach { nube ->
            pProjection.toPixels(GeoPoint(nube.latitud, nube.longitud), punto)
            val x = punto.x.toFloat()
            val y = punto.y.toFloat()
            pincel.shader = RadialGradient(
                x, y, nube.radioPx,
                intArrayOf(
                    conAlfa(nube.color, nube.alfa),
                    conAlfa(nube.color, nube.alfa),
                    conAlfa(nube.color, (nube.alfa * 0.7f).toInt()),
                    conAlfa(nube.color, 0)
                ),
                floatArrayOf(0f, 0.3f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )
            pCanvas.drawCircle(x, y, nube.radioPx, pincel)
        }
        pincel.shader = null
    }

    override fun onSingleTapConfirmed(e: MotionEvent?, mapView: MapView?): Boolean {
        if (e == null || mapView == null) return false
        val proyeccion = mapView.projection
        val impacto = nubes
            .map { nube ->
                proyeccion.toPixels(GeoPoint(nube.latitud, nube.longitud), Point()).let { p ->
                    nube to hypot((p.x - e.x).toDouble(), (p.y - e.y).toDouble())
                }
            }
            .filter { (nube, d) -> d <= nube.radioPx * 0.75 }
            .minByOrNull { it.second }
        return if (impacto != null) {
            onTap(impacto.first.cluster)
            true
        } else {
            false
        }
    }
}

private fun conAlfa(color: Int, alfa: Int): Int =
    (color and 0x00FFFFFF) or ((alfa.coerceIn(0, 255)) shl 24)

// ── Íconos ────────────────────────────────────────────────────────────────────

private fun iconoCluster(context: Context, cantidad: Int, color: Int, resaltado: Boolean): Drawable {
    val densidad = context.resources.displayMetrics.density
    val radio = when {
        cantidad >= 50 -> 29f
        cantidad >= 20 -> 26f
        cantidad >= 10 -> 23f
        cantidad >= 5 -> 20f
        else -> 18f
    } * densidad
    val halo = radio * 1.55f
    val lado = (halo * 2f).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(bitmap)
    val centro = halo
    val pincel = Paint(Paint.ANTI_ALIAS_FLAG)

    pincel.color = conAlfa(color, if (resaltado) 80 else 46)
    lienzo.drawCircle(centro, centro, halo, pincel)
    pincel.color = conAlfa(color, if (resaltado) 130 else 92)
    lienzo.drawCircle(centro, centro, radio * 1.25f, pincel)
    pincel.color = conAlfa(color, 255)
    lienzo.drawCircle(centro, centro, radio, pincel)

    pincel.style = Paint.Style.STROKE
    pincel.strokeWidth = (if (resaltado) 3f else 2f) * densidad
    pincel.color = conAlfa(0xFFFFFF, 255)
    lienzo.drawCircle(centro, centro, radio, pincel)
    pincel.style = Paint.Style.FILL

    pincel.color = conAlfa(0xFFFFFF, 255)
    pincel.textAlign = Paint.Align.CENTER
    pincel.textSize = (if (cantidad > 99) 11f else 13f) * densidad
    pincel.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    val metricas = pincel.fontMetrics
    val texto = if (cantidad > 99) "99+" else cantidad.toString()
    lienzo.drawText(texto, centro, centro - (metricas.ascent + metricas.descent) / 2f, pincel)

    return BitmapDrawable(context.resources, bitmap)
}

private fun iconoTerreno(context: Context, nombre: String, conNombre: Boolean): Drawable {
    val d = context.resources.displayMetrics.density
    val lado = 30f * d

    val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    pincel.textSize = 11f * d
    pincel.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    val etiqueta = if (conNombre && nombre.isNotBlank()) nombre.take(20) else null
    val anchoEtiqueta = etiqueta?.let { pincel.measureText(it) + 14f * d } ?: 0f
    val altoEtiqueta = if (etiqueta != null) 18f * d else 0f
    val separacion = if (etiqueta != null) 4f * d else 0f

    val ancho = maxOf(lado, anchoEtiqueta).toInt().coerceAtLeast(1)
    val alto = (lado + 2f * (separacion + altoEtiqueta)).toInt().coerceAtLeast(1)

    val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(bitmap)
    val cx = ancho / 2f
    val arriba = separacion + altoEtiqueta
    val caja = RectF(cx - lado / 2f, arriba, cx + lado / 2f, arriba + lado)

    pincel.style = Paint.Style.FILL
    pincel.color = 0xFF264A2B.toInt()
    lienzo.drawRoundRect(caja, 9f * d, 9f * d, pincel)
    pincel.style = Paint.Style.STROKE
    pincel.strokeWidth = 2.5f * d
    pincel.color = 0xFFFFFFFF.toInt()
    lienzo.drawRoundRect(caja, 9f * d, 9f * d, pincel)

    // Surcos: lo que hace que se lea como "campo" y no como un botón cualquiera.
    pincel.strokeWidth = 2f * d
    pincel.strokeCap = Paint.Cap.ROUND
    val margen = 8f * d
    listOf(0.35f, 0.5f, 0.65f).forEach { fraccion ->
        val y = caja.top + lado * fraccion
        lienzo.drawLine(caja.left + margen, y, caja.right - margen, y, pincel)
    }

    if (etiqueta != null) {
        pincel.style = Paint.Style.FILL
        val cajaTexto = RectF(
            cx - anchoEtiqueta / 2f,
            caja.bottom + separacion,
            cx + anchoEtiqueta / 2f,
            caja.bottom + separacion + altoEtiqueta
        )
        pincel.color = 0xF2FFFDF7.toInt()
        lienzo.drawRoundRect(cajaTexto, altoEtiqueta / 2f, altoEtiqueta / 2f, pincel)
        pincel.style = Paint.Style.STROKE
        pincel.strokeWidth = 1f * d
        pincel.color = 0x33264A2B
        lienzo.drawRoundRect(cajaTexto, altoEtiqueta / 2f, altoEtiqueta / 2f, pincel)

        pincel.style = Paint.Style.FILL
        pincel.color = 0xFF264A2B.toInt()
        pincel.textAlign = Paint.Align.CENTER
        val metricas = pincel.fontMetrics
        lienzo.drawText(etiqueta, cx, cajaTexto.centerY() - (metricas.ascent + metricas.descent) / 2f, pincel)
    }

    return BitmapDrawable(context.resources, bitmap)
}


private val RAMPA_DENSIDAD = listOf(
    0xFFFFC24B.toInt(),
    0xFFF57C00.toInt(),
    0xFFC62828.toInt(),
    0xFF7B1416.toInt()
)


private val CORTES_DENSIDAD = listOf(1, 3, 10, 30)

private fun fraccionDensidad(cantidad: Int): Float {
    val n = cantidad.coerceAtLeast(1)
    val ultimo = CORTES_DENSIDAD.size - 1
    if (n >= CORTES_DENSIDAD[ultimo]) return 1f

    var tramo = 0
    while (tramo < ultimo - 1 && n >= CORTES_DENSIDAD[tramo + 1]) tramo++

    // Logarítmica dentro del tramo: de 1 a 3 reportes se nota tanto como de 10 a 30.
    val desde = CORTES_DENSIDAD[tramo].toFloat()
    val hasta = CORTES_DENSIDAD[tramo + 1].toFloat()
    val avance = ((ln(n.toFloat()) - ln(desde)) / (ln(hasta) - ln(desde))).coerceIn(0f, 1f)
    return (tramo + avance) / ultimo
}

private fun colorDensidad(fraccion: Float): Int {
    val tramos = RAMPA_DENSIDAD.size - 1
    val posicion = fraccion.coerceIn(0f, 1f) * tramos
    val indice = floor(posicion).toInt().coerceIn(0, tramos - 1)
    return mezclarColor(RAMPA_DENSIDAD[indice], RAMPA_DENSIDAD[indice + 1], posicion - indice)
}

private fun mezclarColor(desde: Int, hasta: Int, t: Float): Int {
    fun canal(desplazamiento: Int): Int {
        val a = (desde shr desplazamiento) and 0xFF
        val b = (hasta shr desplazamiento) and 0xFF
        return (a + (b - a) * t).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (canal(16) shl 16) or (canal(8) shl 8) or canal(0)
}

private fun colorSeveridad(nivel: String): Int = when (nivel.lowercase(Locale.ROOT)) {
    "alto" -> 0xFFC62828.toInt()
    "medio" -> 0xFFEF6C00.toInt()
    else -> 0xFF2E7D32.toInt()
}

private fun hueSeveridad(nivel: String): Float = when (nivel.lowercase(Locale.ROOT)) {
    "alto" -> 0f
    "medio" -> 28f
    else -> 125f
}

/** Radio aproximado (m) de un lote circular con la misma superficie que sus hectáreas. */
private fun radioLotePorArea(hectareas: Float): Double {
    val m2 = (hectareas.coerceAtLeast(0.1f)) * 10_000.0
    return sqrt(m2 / Math.PI).coerceIn(80.0, 4_000.0)
}

// ── Distancias ────────────────────────────────────────────────────────────────

private fun distanciaAlLoteMasCercano(
    reporte: ReporteDetalleResponse,
    terrenos: List<TerrenoResponse>
): Float? {
    reporte.distancia_km?.let { return it }
    val lat = reporte.latitud ?: return null
    val lon = reporte.longitud ?: return null
    if (terrenos.isEmpty()) return null
    return terrenos.minOf { t ->
        distanciaKm(lat, lon, t.terreno_latitud.toDouble(), t.terreno_longitud.toDouble())
    }
}

private fun distanciaKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val radioTierra = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return (2 * radioTierra * atan2(sqrt(a), sqrt(1 - a))).toFloat()
}

private fun formatearDistanciaMapa(km: Float): String = when {
    km < 1f -> "${(km * 1000).toInt()} m"
    km >= 10f -> String.format(Locale.getDefault(), "%.0f km", km)
    else -> String.format(Locale.getDefault(), "%.1f km", km)
}

// ── Controles flotantes ───────────────────────────────────────────────────────

@Composable
private fun PildoraModoMapa(
    modo: ModoMapa,
    onModoChange: (ModoMapa) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = PlagOutColors.Surface.copy(alpha = 0.95f),
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, PlagOutColors.Divider),
        modifier = modifier
    ) {
        Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            SegmentoModo(
                activo = modo == ModoMapa.PINES,
                icono = Icons.Outlined.Place,
                etiqueta = "Puntos",
                onClick = { onModoChange(ModoMapa.PINES) },
                tag = "modoMapaPines"
            )
            SegmentoModo(
                activo = modo == ModoMapa.NUBES,
                icono = Icons.Outlined.BubbleChart,
                etiqueta = "Densidad",
                onClick = { onModoChange(ModoMapa.NUBES) },
                tag = "modoMapaNubes"
            )
        }
    }
}

@Composable
private fun SegmentoModo(
    activo: Boolean,
    icono: ImageVector,
    etiqueta: String,
    onClick: () -> Unit,
    tag: String
) {
    val fondo by animateColorAsState(
        targetValue = if (activo) PlagOutColors.Forest else Color.Transparent,
        animationSpec = tween(240),
        label = "fondoModoMapa"
    )
    val tinta by animateColorAsState(
        targetValue = if (activo) PlagOutColors.TextOnDark else PlagOutColors.TextSecondary,
        animationSpec = tween(240),
        label = "tintaModoMapa"
    )
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(fondo)
            .clickable(enabled = !activo, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = tinta, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(etiqueta, color = tinta, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ControlesMapa(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCentrar: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BotonMapa(Icons.Outlined.CenterFocusStrong, "Centrar en mis lotes y reportes", onCentrar, "btnCentrarMapa")
        BotonMapa(Icons.Default.Add, "Acercar", onZoomIn, "btnZoomInMapa")
        BotonMapa(Icons.Default.Remove, "Alejar", onZoomOut, "btnZoomOutMapa")
    }
}

@Composable
private fun BotonFiltrar(cantidadActiva: Int, onClick: () -> Unit) {
    val activo = cantidadActiva > 0
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (activo) PlagOutColors.Forest else PlagOutColors.Surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, if (activo) PlagOutColors.Forest else PlagOutColors.Divider),
        modifier = Modifier
            .height(42.dp)
            .testTag("btnFiltrarMapa")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = "Filtrar el mapa",
                tint = if (activo) PlagOutColors.TextOnDark else PlagOutColors.Forest,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                "Filtrar",
                color = if (activo) PlagOutColors.TextOnDark else PlagOutColors.TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (activo) {
                Spacer(Modifier.width(7.dp))
                Surface(shape = CircleShape, color = PlagOutColors.TextOnDark.copy(alpha = 0.22f)) {
                    Text(
                        "$cantidadActiva",
                        color = PlagOutColors.TextOnDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonMapa(icono: ImageVector, descripcion: String, onClick: () -> Unit, tag: String) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = PlagOutColors.Surface,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, PlagOutColors.Divider),
        modifier = Modifier.size(42.dp).testTag(tag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = PlagOutColors.Forest, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LeyendaMapa(modo: ModoMapa, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PlagOutColors.Surface.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            Modifier
                .width(IntrinsicSize.Max)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                if (modo == ModoMapa.PINES) "Severidad" else "Reportes por zona",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextSecondary,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.height(6.dp))

            if (modo == ModoMapa.PINES) {
                ItemLeyenda(Color(0xFFC62828), "Alto")
                ItemLeyenda(Color(0xFFEF6C00), "Medio")
                ItemLeyenda(Color(0xFF2E7D32), "Bajo")
            } else {
                // Barra continua en vez de puntos discretos: es lo que avisa de un vistazo que acá
                // el color mide una cantidad y no una categoría.
                Box(
                    Modifier
                        .width(116.dp)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(RAMPA_DENSIDAD.map { Color(it) }))
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.width(116.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CORTES_DENSIDAD.forEachIndexed { indice, corte ->
                        Text(
                            if (indice == CORTES_DENSIDAD.lastIndex) "$corte+" else "$corte",
                            fontSize = 10.sp,
                            color = PlagOutColors.TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = PlagOutColors.Divider)
            Spacer(Modifier.height(6.dp))
            ItemLeyenda(PlagOutColors.Leaf, "Mis lotes")
        }
    }
}

@Composable
private fun ItemLeyenda(color: Color, etiqueta: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(7.dp))
        Text(etiqueta, fontSize = 12.sp, color = PlagOutColors.TextMain, fontWeight = FontWeight.Medium)
    }
}

// ── Tarjeta de selección ──────────────────────────────────────────────────────

@Composable
private fun SeleccionFlotante(
    cluster: ClusterReportes?,
    distancias: Map<Int, Float?>,
    onCerrar: () -> Unit,
    onAcercar: (ClusterReportes) -> Unit,
    onAbrirReporte: (ReporteDetalleResponse) -> Unit,
    modifier: Modifier = Modifier
) {
    // Se retiene el último grupo para que la animación de salida no se quede sin contenido.
    var ultimo by remember { mutableStateOf<ClusterReportes?>(null) }
    LaunchedEffect(cluster) { if (cluster != null) ultimo = cluster }

    AnimatedVisibility(
        visible = cluster != null,
        enter = slideInVertically { it } + fadeIn(tween(200)),
        exit = slideOutVertically { it } + fadeOut(tween(160)),
        modifier = modifier
    ) {
        ultimo?.let { actual ->
            TarjetaSeleccionMapa(
                cluster = actual,
                distancias = distancias,
                onCerrar = onCerrar,
                onAcercar = { onAcercar(actual) },
                onAbrirReporte = onAbrirReporte
            )
        }
    }
}

@Composable
private fun TarjetaSeleccionMapa(
    cluster: ClusterReportes,
    distancias: Map<Int, Float?>,
    onCerrar: () -> Unit,
    onAcercar: () -> Unit,
    onAbrirReporte: (ReporteDetalleResponse) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        color = PlagOutColors.Surface,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth().testTag("tarjetaSeleccionMapa")
    ) {
        Column(Modifier.padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val estilo = estiloDeNivel(
                    when (cluster.severidadDominante.lowercase(Locale.ROOT)) {
                        "alto" -> 2; "medio" -> 1; else -> 0
                    }
                )
                Box(Modifier.size(10.dp).background(estilo.color, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (cluster.cantidad == 1) "Reporte" else "${cluster.cantidad} reportes en la zona",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextMain,
                    modifier = Modifier.weight(1f)
                )
                if (cluster.cantidad > 1) {
                    Surface(
                        onClick = onAcercar,
                        shape = CircleShape,
                        color = PlagOutColors.Forest.copy(alpha = 0.12f)
                    ) {
                        Text(
                            "Acercar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.Forest,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = PlagOutColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }

            if (cluster.cantidad > FILAS_VISIBLES_SELECCION) {
                Text(
                    "Deslizá para ver los ${cluster.cantidad}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PlagOutColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Varios reportes pueden caer exactamente en el mismo punto (el mismo lote, la misma
            // plantación): ahí acercar el mapa no los separa nunca, así que la lista tiene que
            // poder recorrerse entera desde acá.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 248.dp)
                    .testTag("listaSeleccionMapa")
            ) {
                itemsIndexed(cluster.reportes, key = { _, r -> r.id }) { indice, reporte ->
                    if (indice > 0) {
                        HorizontalDivider(color = PlagOutColors.Divider.copy(alpha = 0.5f))
                    }
                    FilaReporteMapa(
                        reporte = reporte,
                        distanciaKm = distancias[reporte.id],
                        onClick = { onAbrirReporte(reporte) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaReporteMapa(
    reporte: ReporteDetalleResponse,
    distanciaKm: Float?,
    onClick: () -> Unit
) {
    val estilo = estiloDeNivel(
        when (reporte.nivel_severidad.lowercase(Locale.ROOT)) {
            "alto" -> 2; "medio" -> 1; else -> 0
        }
    )
    val fecha = remember(reporte.timestamp_ms) {
        try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(reporte.timestamp_ms))
        } catch (e: Exception) {
            "Sin fecha"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(estilo.icono, contentDescription = null, tint = estilo.color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                reporte.plaga_nombre.ifBlank { "Plaga no especificada" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detalle = buildString {
                append(reporte.nivel_severidad)
                append(" • ")
                append(fecha)
                if (!reporte.es_propio) {
                    distanciaKm?.let { append(" • a ${formatearDistanciaMapa(it)}") }
                }
            }
            Text(detalle, fontSize = 12.sp, color = PlagOutColors.TextSecondary)
        }
        if (!reporte.es_propio) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = AzulMapa, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(20.dp))
    }
}
