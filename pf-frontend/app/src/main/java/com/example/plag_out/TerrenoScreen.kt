package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.plag_out.ui.theme.AnilloSegmentado
import com.example.plag_out.ui.theme.EstadisticaCompacta
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.NivelEstilo
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SelloDeNivel
import com.example.plag_out.ui.theme.SeparadorVertical
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.contadorAnimado
import com.example.plag_out.ui.theme.estiloDeNivel
import com.example.plag_out.ui.theme.rememberPressScale

/** ids del filtro por estado — distintos de los niveles 0/1/2 para poder incluir "Todos" y "Sin datos". */
private const val FILTRO_TODOS = -1
private const val FILTRO_SIN_DATOS = 3

private fun bucketDeAlerta(nivelMax: Int): Int = when {
    nivelMax < 0 -> FILTRO_SIN_DATOS
    nivelMax == 0 -> 0
    nivelMax == 1 -> 1
    else -> 2
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerrenoScreen(
    terrenoViewModel: TerrenosViewModel,
    monitoreoViewModel: MonitoreosViewModel,
    plantacionViewModel: PlantacionesViewModel,
    nuevoTerrenoViewModel: NuevoTerrenoViewModel,
    navController: NavController
) {
    val state by terrenoViewModel.state.collectAsState()
    val monitoreosState by monitoreoViewModel.state.collectAsState()
    val plantacionesState by plantacionViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        terrenoViewModel.getTerrenos()
        plantacionViewModel.getPlantaciones()
        monitoreoViewModel.getMonitoreos()
    }

    var filtro by rememberSaveable { mutableStateOf(FILTRO_TODOS) }

    fun nivelMaxDe(terreno: TerrenoResponse): Int =
        monitoreosState.monitoreos.filter { it.terreno_id == terreno.terreno_id }.maxOfOrNull { it.nivel_alerta } ?: -1

    val ordenados = remember(state.terrenos, monitoreosState.monitoreos) {
        state.terrenos.sortedByDescending { nivelMaxDe(it) }
    }
    val filtrados = remember(ordenados, filtro, monitoreosState.monitoreos) {
        if (filtro == FILTRO_TODOS) ordenados
        else ordenados.filter { bucketDeAlerta(nivelMaxDe(it)) == filtro }
    }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val fabScale by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "fabScale"
            )
            ExtendedFloatingActionButton(
                onClick = {
                    nuevoTerrenoViewModel.resetState()
                    navController.navigate("datos_terreno")
                },
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer { scaleX = fabScale; scaleY = fabScale }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Nuevo Terreno", fontWeight = FontWeight.SemiBold)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            PanelDeCampoTerrenos(terrenos = state.terrenos, monitoreos = monitoreosState.monitoreos)

            val opciones = remember(state.terrenos, monitoreosState.monitoreos) {
                val buckets = state.terrenos.map { bucketDeAlerta(nivelMaxDe(it)) }
                listOf(
                    OpcionFiltro(FILTRO_TODOS, "Todos", state.terrenos.size),
                    OpcionFiltro(0, "Saludable", buckets.count { it == 0 }, estiloDeNivel(0).icono, estiloDeNivel(0).color),
                    OpcionFiltro(1, "Atención", buckets.count { it == 1 }, estiloDeNivel(1).icono, estiloDeNivel(1).color),
                    OpcionFiltro(2, "Crítico", buckets.count { it == 2 }, estiloDeNivel(2).icono, estiloDeNivel(2).color),
                    OpcionFiltro(FILTRO_SIN_DATOS, "Sin datos", buckets.count { it == FILTRO_SIN_DATOS }, estiloDeNivel(-1).icono, estiloDeNivel(-1).color)
                )
            }
            FiltroChipsRow(opciones = opciones, seleccionado = filtro, onSeleccion = { filtro = it })

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = when {
                        state.isLoading -> "cargando"
                        state.terrenos.isEmpty() -> "vacio"
                        filtrados.isEmpty() -> "sin-resultados"
                        else -> "lista-$filtro"
                    },
                    transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(180)) },
                    label = "terrenosContent"
                ) { target ->
                    when (target) {
                        "cargando" -> SkeletonCargando()
                        "vacio" -> EstadoVacioFlotante(
                            icono = Icons.Outlined.Terrain,
                            titulo = "No hay terrenos cargados",
                            subtitulo = "Comenzá registrando tu primer lote de tierra para monitorear plagas y cultivos."
                        )
                        "sin-resultados" -> EstadoSinResultados(subtitulo = "No hay terrenos en este estado.")
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(filtrados, key = { _, t -> t.terreno_id }) { index, terreno ->
                                val monitoreosDelTerreno = monitoreosState.monitoreos.filter { it.terreno_id == terreno.terreno_id }
                                val plantacionesActivas = plantacionesState.plantaciones.filter {
                                    it.terreno_id == terreno.terreno_id && it.activa
                                }
                                StaggeredAppear(index = index) {
                                    TerrenoCard(
                                        terreno = terreno,
                                        monitoreos = monitoreosDelTerreno,
                                        plantacionesActivas = plantacionesActivas.size,
                                        onClick = { navController.navigate("terreno/${terreno.terreno_id}") }
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

// ── Header: panel de estado general ─────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun PanelDeCampoTerrenos(terrenos: List<TerrenoResponse>, monitoreos: List<MonitoreoResponse>) {
    val total = terrenos.size
    val nivelesMax = terrenos.map { t -> monitoreos.filter { it.terreno_id == t.terreno_id }.maxOfOrNull { it.nivel_alerta } ?: -1 }
    val sanos = nivelesMax.count { it == 0 }
    val atencion = nivelesMax.count { it == 1 }
    val criticos = nivelesMax.count { it >= 2 }
    val sinDatos = nivelesMax.count { it < 0 }
    val areaTotal = terrenos.sumOf { it.terreno_area.toDouble() }.toInt()

    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderTerrenos")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativa"
    )

    val formaHeader = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = formaHeader
            )
            .clip(formaHeader)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 56.dp, y = (-48).dp)
                .size(190.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-44).dp, y = 48.dp)
                .size(150.dp)
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.05f), CircleShape)
        )

        Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 20.dp)) {
            Text(
                "Gestión de Campo",
                color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp
            )
            Text("Mis Terrenos", color = PlagOutColors.TextOnDark, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)

            Spacer(Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val totalAnimado = contadorAnimado(total)
                AnilloSegmentado(
                    segmentos = listOf(
                        sanos to estiloDeNivel(0).colorSobreOscuro,
                        atencion to estiloDeNivel(1).colorSobreOscuro,
                        criticos to estiloDeNivel(2).colorSobreOscuro,
                        sinDatos to estiloDeNivel(-1).colorSobreOscuro
                    ),
                    total = total,
                    modifier = Modifier.size(110.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalAnimado", color = PlagOutColors.TextOnDark, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (total == 1) "lote" else "lotes",
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.width(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LeyendaEstadoTerreno(estiloDeNivel(0), sanos)
                    LeyendaEstadoTerreno(estiloDeNivel(1), atencion)
                    LeyendaEstadoTerreno(estiloDeNivel(2), criticos)
                    LeyendaEstadoTerreno(estiloDeNivel(-1), sinDatos)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier
                    .background(PlagOutColors.TextOnDark.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SquareFoot, contentDescription = null, tint = PlagOutColors.TextOnDark, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Área total", color = PlagOutColors.TextOnDark.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text("$areaTotal ha", color = PlagOutColors.TextOnDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LeyendaEstadoTerreno(estilo: NivelEstilo, cantidad: Int) {
    val valor = contadorAnimado(cantidad)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(estilo.icono, contentDescription = null, tint = estilo.colorSobreOscuro, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            estilo.etiqueta,
            color = PlagOutColors.TextOnDark.copy(alpha = 0.85f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp)
        )
        Text("$valor", color = PlagOutColors.TextOnDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Card de terreno ──────────────────────────────────────────────────────────

@Composable
fun TerrenoCard(
    terreno: TerrenoResponse,
    monitoreos: List<MonitoreoResponse>,
    plantacionesActivas: Int,
    onClick: () -> Unit
) {
    val nivelMax = monitoreos.maxOfOrNull { it.nivel_alerta } ?: -1
    val estilo = estiloDeNivel(nivelMax)
    val interactionSource = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interactionSource)

    val sanos = monitoreos.count { it.nivel_alerta == 0 }
    val atencion = monitoreos.count { it.nivel_alerta == 1 }
    val criticos = monitoreos.count { it.nivel_alerta >= 2 }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala }
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(estilo.color)
            )

            Column(Modifier.padding(start = 17.dp, end = 18.dp, top = 16.dp, bottom = 14.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        SelloDeNivel(estilo, pulsante = nivelMax >= 2)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            terreno.terreno_nombre,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "${"%.2f".format(terreno.terreno_latitud)}, ${"%.2f".format(terreno.terreno_longitud)}",
                                fontSize = 12.sp,
                                color = PlagOutColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    AnilloSegmentado(
                        segmentos = listOf(
                            sanos to estiloDeNivel(0).color,
                            atencion to estiloDeNivel(1).color,
                            criticos to estiloDeNivel(2).color
                        ),
                        total = monitoreos.size,
                        modifier = Modifier.size(66.dp),
                        grosor = 7.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${monitoreos.size}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
                            Text("monit.", fontSize = 9.sp, color = PlagOutColors.TextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(PlagOutColors.Cream, RoundedCornerShape(14.dp))
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EstadisticaCompacta("Hectáreas", "${terreno.terreno_area.toInt()}", Modifier.weight(1f))
                    SeparadorVertical()
                    EstadisticaCompacta("Plantaciones", "$plantacionesActivas", Modifier.weight(1f))
                    SeparadorVertical()
                    EstadisticaCompacta("Monitoreos", "${monitoreos.size}", Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    when {
                        criticos > 0 -> EtiquetaInfo(Icons.Default.WarningAmber, "$criticos en estado crítico", PlagOutColors.RiskDanger)
                        atencion > 0 -> EtiquetaInfo(Icons.Default.WarningAmber, "$atencion en atención", PlagOutColors.RiskWarn)
                        monitoreos.isEmpty() -> EtiquetaInfo(Icons.Outlined.HelpOutline, "Sin monitoreos", PlagOutColors.RiskUnknown)
                        else -> EtiquetaInfo(Icons.Default.CheckCircle, "Todo en orden", PlagOutColors.RiskOk)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Plantaciones", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.Leaf)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = PlagOutColors.Leaf,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
