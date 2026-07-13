package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.AnilloProgreso
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.estiloDeNivel
import com.example.plag_out.ui.theme.rememberPressScale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val FILTRO_TODAS = -1
private const val FILTRO_ACTIVAS = 1
private const val FILTRO_PAUSADAS = 0

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlantacionesPorTerreno(
    terrenoId: Int,
    plantacionesViewModel: PlantacionesViewModel,
    monitoreosViewModel: MonitoreosViewModel,
    terrenoViewModel: TerrenosViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    val plantacionesState by plantacionesViewModel.state.collectAsState()
    val monitoreosState by monitoreosViewModel.state.collectAsState()
    val terrenosState by terrenoViewModel.state.collectAsState()

    val terreno = terrenosState.terrenos.find { it.terreno_id == terrenoId }
    val plantacionesDelTerreno = plantacionesState.plantaciones.filter { it.terreno_id == terrenoId }

    var filtro by rememberSaveable { mutableStateOf(FILTRO_TODAS) }

    val ordenadas = remember(plantacionesDelTerreno, monitoreosState.monitoreos) {
        plantacionesDelTerreno.sortedWith(
            compareByDescending<PlantacionesResponse> { it.activa }
                .thenByDescending { p ->
                    monitoreosState.monitoreos.filter { it.plantacion_id == p.plantacion_id }.maxOfOrNull { it.nivel_alerta } ?: -1
                }
        )
    }
    val filtradas = remember(ordenadas, filtro) {
        when (filtro) {
            FILTRO_ACTIVAS -> ordenadas.filter { it.activa }
            FILTRO_PAUSADAS -> ordenadas.filter { !it.activa }
            else -> ordenadas
        }
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("agregar_plantacion/$terrenoId") },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nueva Plantación", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                    }
                    Column {
                        Text(
                            terreno?.terreno_nombre ?: "Terreno",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text("Plantaciones", color = PlagOutColors.TextOnDark, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(4.dp))
                        val activas = plantacionesDelTerreno.count { it.activa }
                        Text(
                            if (plantacionesDelTerreno.isEmpty()) "Sin plantaciones registradas"
                            else "$activas activa${if (activas == 1) "" else "s"} de ${plantacionesDelTerreno.size}",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            val opciones = remember(plantacionesDelTerreno) {
                listOf(
                    OpcionFiltro(FILTRO_TODAS, "Todas", plantacionesDelTerreno.size),
                    OpcionFiltro(FILTRO_ACTIVAS, "Activas", plantacionesDelTerreno.count { it.activa }, colorIcono = PlagOutColors.Leaf),
                    OpcionFiltro(FILTRO_PAUSADAS, "Pausadas", plantacionesDelTerreno.count { !it.activa }, colorIcono = PlagOutColors.Bark)
                )
            }
            FiltroChipsRow(opciones = opciones, seleccionado = filtro, onSeleccion = { filtro = it })

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = when {
                        plantacionesDelTerreno.isEmpty() -> "vacio"
                        filtradas.isEmpty() -> "sin-resultados"
                        else -> "lista-$filtro"
                    },
                    transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                    label = "plantacionesContent"
                ) { target ->
                    when (target) {
                        "vacio" -> EstadoVacioFlotante(
                            icono = Icons.Outlined.Grass,
                            titulo = "No hay plantaciones",
                            subtitulo = "Registrá un cultivo en este terreno para empezar a monitorear plagas."
                        )
                        "sin-resultados" -> EstadoSinResultados(subtitulo = "No hay plantaciones en este estado.")
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(filtradas, key = { _, p -> p.plantacion_id }) { index, plantacion ->
                                StaggeredAppear(index = index) {
                                    PlantacionCard(
                                        plantacion = plantacion,
                                        monitoreos = monitoreosState.monitoreos.filter { it.plantacion_id == plantacion.plantacion_id },
                                        onClick = { navController.navigate("plantacion/${plantacion.plantacion_id}") }
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

// ── Card de plantación ───────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PlantacionCard(
    plantacion: PlantacionesResponse,
    monitoreos: List<MonitoreoResponse>,
    onClick: () -> Unit = {}
) {
    val nivelMax = monitoreos.maxOfOrNull { it.nivel_alerta } ?: -1
    val estadoColor = if (plantacion.activa) estiloDeNivel(nivelMax).color else PlagOutColors.RiskUnknown

    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)

    val diasDesdeSiembra = ChronoUnit.DAYS.between(plantacion.fecha_siembra, LocalDate.now()).toInt()

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = escala; scaleY = escala }
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(estadoColor)
            )

            Column(Modifier.padding(start = 17.dp, end = 18.dp, top = 16.dp, bottom = 14.dp).fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Surface(
                            color = (if (plantacion.activa) PlagOutColors.Leaf else PlagOutColors.Bark).copy(alpha = 0.12f),
                            shape = CircleShape
                        ) {
                            Text(
                                if (plantacion.activa) "ACTIVA" else "PAUSADA",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp,
                                color = if (plantacion.activa) PlagOutColors.Leaf else PlagOutColors.Bark
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            plantacion.cultivo_nombre,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            plantacion.cultivo_nombre_cientifico,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            color = PlagOutColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (monitoreos.isNotEmpty()) {
                        Spacer(Modifier.width(14.dp))
                        AnilloProgreso(
                            progreso = monitoreos.maxOf { it.progreso },
                            color = estiloDeNivel(nivelMax).color,
                            tamano = 58.dp,
                            grosor = 6.dp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PlagOutColors.Cream, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Sembrada el ${plantacion.fecha_siembra.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es")))}",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (diasDesdeSiembra >= 0) "$diasDesdeSiembra días" else "—",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.Forest
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (monitoreos.isEmpty()) {
                    EtiquetaInfo(Icons.Outlined.Grass, "Sin plagas bajo seguimiento", PlagOutColors.RiskUnknown)
                } else {
                    Text(
                        "Monitoreos (${monitoreos.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        monitoreos.forEach { monitoreo ->
                            PlagaMiniFila(monitoreo)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlagaMiniFila(monitoreo: MonitoreoResponse) {
    val estilo = estiloDeNivel(monitoreo.nivel_alerta)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(monitoreo.plaga_nombre, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
            Text(
                "${monitoreo.gdd_acumulado.toInt()} / ${monitoreo.gdd_objetivo.toInt()} GDD",
                fontSize = 11.sp,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.width(8.dp))
        SelloDeNivel(estilo, pulsante = monitoreo.nivel_alerta >= 2)
    }
}
