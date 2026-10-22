package com.example.plag_out

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Agriculture
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale

// ── Lista ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCultivosScreen(
    viewModel: AdminCultivosViewModel,
    onNuevoCultivo: () -> Unit,
    onCultivoClick: (Int) -> Unit,
    onSinPermiso: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(state.sinPermiso) { if (state.sinPermiso) onSinPermiso() }

    val visibles = remember(state.cultivos, state.busqueda, state.filtro) {
        filtrarCultivos(state.cultivos, state.busqueda, state.filtro)
    }
    val activos = state.cultivos.count { it.estaActivo }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            var mostrado by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { mostrado = true }
            val escala by animateFloatAsState(
                targetValue = if (mostrado) 1f else 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "escalaFabCultivo"
            )
            if (state.cargado) {
                ExtendedFloatingActionButton(
                    onClick = onNuevoCultivo,
                    containerColor = PlagOutColors.Sun,
                    contentColor = PlagOutColors.ForestDark,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nuevo cultivo", fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .graphicsLayer { scaleX = escala; scaleY = escala }
                        .testTag("btnNuevoCultivo")
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EncabezadoSeccionAdmin(
                titulo = "Cultivos",
                subtitulo = if (state.cargado) {
                    "$activos activos · ${state.cultivos.size - activos} dados de baja"
                } else {
                    "Los que se ofrecen al crear una plantación"
                }
            )
            BuscadorAdmin(
                valor = state.busqueda,
                onCambio = viewModel::buscar,
                placeholder = "Buscar por nombre o nombre científico",
                tag = "txtBuscarCultivo"
            )
            FiltroChipsRow(
                opciones = listOf(
                    OpcionFiltro(FiltroCultivos.TODOS.ordinal, "Todos", state.cultivos.size),
                    OpcionFiltro(FiltroCultivos.ACTIVOS.ordinal, "Activos", activos),
                    OpcionFiltro(FiltroCultivos.INACTIVOS.ordinal, "De baja", state.cultivos.size - activos)
                ),
                seleccionado = state.filtro.ordinal,
                onSeleccion = { viewModel.cambiarFiltro(FiltroCultivos.entries[it]) }
            )

            BannerErrorAdmin(
                mensaje = state.errorAccion,
                onCerrar = viewModel::descartarErrorAccion,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when {
                state.isLoading && !state.cargado -> SkeletonCargando(alturaTarjeta = 96.dp, cantidad = 4)
                state.noDisponible && !state.cargado -> EstadoErrorAdmin(
                    "La gestión de cultivos todavía no está disponible en el servidor."
                ) { viewModel.cargar() }
                state.error != null && !state.cargado -> EstadoErrorAdmin(state.error!!) { viewModel.cargar() }
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refrescar,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        state.cultivos.isEmpty() -> EstadoVacioFlotante(
                            icono = Icons.Outlined.Grass,
                            titulo = "Todavía no hay cultivos",
                            subtitulo = "Agregá el primero para que los productores puedan crear plantaciones."
                        )
                        visibles.isEmpty() -> EstadoSinResultados(
                            subtitulo = "Ningún cultivo coincide con la búsqueda o el filtro."
                        )
                        else -> LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("listaCultivosAdmin")
                        ) {
                            itemsIndexed(visibles, key = { _, cultivo -> cultivo.id }) { index, cultivo ->
                                StaggeredAppear(index = index, modifier = Modifier.animateItem()) {
                                    TarjetaCultivoAdmin(cultivo) { onCultivoClick(cultivo.id) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaCultivoAdmin(cultivo: CultivoAdmin, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interaccion)
    val acento = if (cultivo.estaActivo) PlagOutColors.Leaf else PlagOutColors.RiskUnknown

    Surface(
        onClick = onClick,
        interactionSource = interaccion,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .testTag("tarjetaCultivo_${cultivo.id}")
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(acento.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Grass, contentDescription = null, tint = acento, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (cultivo.estaActivo) 1f else 0.7f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        cultivo.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!cultivo.estaActivo) {
                        Spacer(Modifier.width(8.dp))
                        PastillaEstado("DE BAJA", PlagOutColors.RiskUnknown)
                    }
                }
                Text(
                    cultivo.nombre_cientifico,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = PlagOutColors.TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    cultivo.plagas?.let {
                        EtiquetaInfo(Icons.Outlined.BugReport, cantidad(it.size, "plaga", "plagas"), PlagOutColors.Leaf)
                    }
                    cultivo.plantaciones_activas?.let {
                        EtiquetaInfo(Icons.Outlined.Agriculture, cantidad(it, "plantación", "plantaciones"), PlagOutColors.Bark)
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = PlagOutColors.TextSecondary
            )
        }
    }
}

private fun cantidad(n: Int, singular: String, plural: String) = "$n ${if (n == 1) singular else plural}"

// ── Detalle / alta ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CultivoDetalleScreen(
    viewModel: AdminCultivosViewModel,
    cultivoId: Int?,
    onBack: () -> Unit,
    onCreado: () -> Unit,
    // Avisa a la pestaña de Plagas que cambió algo que muestra (cultivos afectados, nombres, bajas)
    onRelacionesCambiadas: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val original = remember(cultivoId, state.cultivos) { cultivoId?.let { viewModel.cultivo(it) } }

    // Se llegó a un cultivo que no está en la lista (p. ej. el proceso se recreó): volver.
    if (cultivoId != null && original == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var form by remember(cultivoId) { mutableStateOf(original?.let { FormularioCultivo.desde(it) } ?: FormularioCultivo()) }
    var intentoGuardar by remember { mutableStateOf(false) }
    var confirmarBaja by remember { mutableStateOf(false) }
    var confirmarQuitar by remember { mutableStateOf<PlagaDeCultivo?>(null) }
    var mostrarBuscador by remember { mutableStateOf(false) }

    val errores = remember(form) { validarCultivo(form) }
    fun errorDe(campo: CampoCultivo) = (if (intentoGuardar) errores[campo] else null) ?: state.erroresCampo[campo]
    val hayCambios = original == null || requestDeEdicionCultivo(original, form) != null

    LaunchedEffect(cultivoId) { viewModel.abrirDetalle(cultivoId) }

    // Las del servidor menos las quitadas, más las sumadas en el formulario: todo viaja al guardar.
    val plagasDelServidor = original?.plagas.orEmpty()
    val plagasVinculadas: List<PlagaDeCultivo> =
        plagasDelServidor.filter { it.id !in form.quitadas } +
            state.plagas.filter { it.id in form.plagas && plagasDelServidor.none { v -> v.id == it.id } }.map {
                PlagaDeCultivo(id = it.id, nombre = it.nombre, nombre_cientifico = it.nombre_cientifico, activo = it.activo)
            }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            HeaderDetalleAdmin(
                etiqueta = if (original == null) "Cultivos" else "Editar cultivo",
                titulo = if (original == null) "Nuevo cultivo" else original.nombre,
                onBack = onBack
            ) {
                if (original != null) {
                    val plantaciones = original.plantaciones_activas
                    val monitoreos = original.monitoreos_activos
                    if (plantaciones != null || monitoreos != null) {
                        Text(
                            listOfNotNull(
                                plantaciones?.let { cantidad(it, "plantación", "plantaciones") },
                                monitoreos?.let { cantidad(it, "monitoreo activo", "monitoreos activos") }
                            ).joinToString(" · "),
                            color = PlagOutColors.TextOnDark.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("txtContadoresCultivo")
                        )
                    }
                    if (!original.estaActivo) {
                        Spacer(Modifier.height(6.dp))
                        PastillaEstado("DADO DE BAJA", PlagOutColors.Sun)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (original != null && !original.estaActivo) {
                    AvisoImpacto(
                        "No se ofrece para plantaciones nuevas. Las plantaciones y monitoreos que ya existen siguen funcionando."
                    )
                }

                SeccionFormulario(titulo = "Datos", subtitulo = "Cómo lo ven los productores al crear una plantación.") {
                    CampoFormularioAdmin(
                        valor = form.nombre,
                        onCambio = {
                            form = form.copy(nombre = it)
                            viewModel.descartarErrorCampo(CampoCultivo.NOMBRE)
                        },
                        etiqueta = "Nombre",
                        placeholder = "Ej: Manzano",
                        error = errorDe(CampoCultivo.NOMBRE),
                        icono = Icons.Outlined.Grass,
                        tag = "txtNombreCultivo"
                    )
                    Spacer(Modifier.height(14.dp))
                    CampoFormularioAdmin(
                        valor = form.nombreCientifico,
                        onCambio = {
                            form = form.copy(nombreCientifico = it)
                            viewModel.descartarErrorCampo(CampoCultivo.NOMBRE_CIENTIFICO)
                        },
                        etiqueta = "Nombre científico",
                        placeholder = "Ej: Malus domestica",
                        error = errorDe(CampoCultivo.NOMBRE_CIENTIFICO),
                        icono = Icons.Outlined.Science,
                        tag = "txtNombreCientificoCultivo"
                    )
                }

                SeccionFormulario(
                    titulo = "Plagas que lo afectan",
                    subtitulo = "Son las que se ofrecen para monitorear en plantaciones de este cultivo."
                ) {
                    Column(Modifier.animateContentSize()) {
                        if (plagasVinculadas.isEmpty()) {
                            Text(
                                "Todavía no tiene plagas vinculadas.",
                                fontSize = 13.sp,
                                color = PlagOutColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        plagasVinculadas.forEach { plaga ->
                            FilaPlagaDeCultivo(
                                plaga = plaga,
                                habilitada = !state.guardando,
                                onQuitar = {
                                    // Solo se pregunta si ya estaba guardada y puede tener monitoreos
                                    // (sin el dato, también)
                                    val guardada = plagasDelServidor.any { it.id == plaga.id }
                                    if (guardada && plaga.monitoreos_activos != 0) confirmarQuitar = plaga
                                    else form = form.quitar(plaga.id)
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { mostrarBuscador = true },
                        enabled = !state.guardando,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlagOutColors.Leaf.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.Forest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btnAgregarPlaga")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Agregar plaga", fontWeight = FontWeight.Bold)
                    }
                }

                if (original != null) {
                    AccionBajaCultivo(
                        activo = original.estaActivo,
                        habilitada = !state.guardando,
                        onClick = {
                            if (original.estaActivo) confirmarBaja = true
                            else viewModel.reactivar(original.id) {
                                Avisos.exito("Cultivo reactivado.")
                                onRelacionesCambiadas()
                            }
                        }
                    )
                }
            }

            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                BannerErrorAdmin(
                    mensaje = state.errorAccion,
                    onCerrar = viewModel::descartarErrorAccion,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Button(
                    onClick = {
                        intentoGuardar = true
                        viewModel.guardar(original, form) {
                            // Plagas muestra los nombres de los cultivos de cada una
                            onRelacionesCambiadas()
                            if (original == null) {
                                onCreado()
                            } else {
                                Avisos.exito("Cambios guardados.")
                            }
                        }
                    },
                    enabled = !state.guardando && hayCambios,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btnGuardarCultivo"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlagOutColors.Forest,
                        disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f),
                        contentColor = PlagOutColors.TextOnDark,
                        disabledContentColor = PlagOutColors.TextOnDark.copy(alpha = 0.6f)
                    )
                ) {
                    if (state.guardando) {
                        CircularProgressIndicator(color = PlagOutColors.TextOnDark, modifier = Modifier.size(22.dp))
                    } else {
                        Text(if (original == null) "Crear cultivo" else "Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mostrarBuscador) {
        BuscadorPlagasSheet(
            plagas = state.plagas,
            vinculadas = plagasVinculadas.map { it.id }.toSet(),
            onElegir = { plaga ->
                mostrarBuscador = false
                form = form.agregar(plaga.id)
            },
            onDismiss = { mostrarBuscador = false }
        )
    }

    confirmarQuitar?.let { plaga ->
        val monitoreos = plaga.monitoreos_activos
        val nombreCultivo = original?.nombre.orEmpty()
        DialogoConfirmacionAdmin(
            icono = Icons.Outlined.LinkOff,
            titulo = "¿Quitar la plaga del cultivo?",
            iconoFicha = Icons.Outlined.BugReport,
            nombre = plaga.nombre,
            detalle = plaga.nombre_cientifico,
            consecuencias = listOf("Deja de ofrecerse para monitorear en plantaciones de $nombreCultivo"),
            tranquilidad = if (monitoreos != null && monitoreos > 0) {
                "${if (monitoreos == 1) "El monitoreo activo sigue" else "Los $monitoreos monitoreos activos siguen"} hasta terminar."
            } else if (monitoreos == null) {
                "Los monitoreos activos siguen hasta terminar."
            } else null,
            textoConfirmar = "Quitar",
            peligro = true,
            onConfirmar = {
                confirmarQuitar = null
                form = form.quitar(plaga.id)
            },
            onDismiss = { confirmarQuitar = null }
        )
    }

    if (confirmarBaja && original != null) {
        val plantaciones = original.plantaciones_activas ?: 0
        val monitoreos = original.monitoreos_activos ?: 0
        DialogoConfirmacionAdmin(
            icono = Icons.Outlined.Archive,
            titulo = "¿Dar de baja el cultivo?",
            iconoFicha = Icons.Outlined.Grass,
            nombre = original.nombre,
            detalle = original.nombre_cientifico,
            consecuencias = listOf("Deja de ofrecerse para plantaciones nuevas"),
            tranquilidad = (if (plantaciones > 0 || monitoreos > 0) {
                "Sus ${cantidad(plantaciones, "plantación", "plantaciones")} y " +
                    "${cantidad(monitoreos, "monitoreo activo", "monitoreos activos")} siguen funcionando. "
            } else "") + "Podés reactivarlo cuando quieras.",
            textoConfirmar = "Dar de baja",
            peligro = true,
            onConfirmar = {
                confirmarBaja = false
                viewModel.desactivar(original.id) {
                    Avisos.exito("Cultivo dado de baja.")
                    onRelacionesCambiadas()
                }
            },
            onDismiss = { confirmarBaja = false }
        )
    }
}

@Composable
private fun FilaPlagaDeCultivo(plaga: PlagaDeCultivo, habilitada: Boolean, onQuitar: () -> Unit) {
    val acento = if (plaga.estaActiva) PlagOutColors.Terracotta else PlagOutColors.RiskUnknown
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("filaPlagaCultivo_${plaga.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(acento.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.BugReport, contentDescription = null, tint = acento, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .alpha(if (plaga.estaActiva) 1f else 0.7f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plaga.nombre,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlagOutColors.TextMain,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!plaga.estaActiva) {
                    Spacer(Modifier.width(6.dp))
                    PastillaEstado("INACTIVA", PlagOutColors.RiskUnknown)
                }
            }
            Text(plaga.nombre_cientifico, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = PlagOutColors.TextSecondary)
            val monitoreos = plaga.monitoreos_activos
            if (monitoreos != null && monitoreos > 0) {
                Text(
                    cantidad(monitoreos, "monitoreo activo", "monitoreos activos"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlagOutColors.Forest
                )
            }
        }
        IconButton(
            onClick = onQuitar,
            enabled = habilitada,
            modifier = Modifier.testTag("btnQuitarPlaga_${plaga.id}")
        ) {
            Icon(Icons.Default.Close, contentDescription = "Quitar ${plaga.nombre}", tint = PlagOutColors.RiskDanger)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuscadorPlagasSheet(
    plagas: List<PlagaAdmin>,
    vinculadas: Set<Int>,
    onElegir: (PlagaAdmin) -> Unit,
    onDismiss: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val opciones = remember(plagas, vinculadas, busqueda) { plagasParaAgregar(plagas, vinculadas, busqueda) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PlagOutColors.Cream,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxHeight(0.8f)) {
            Text(
                "Agregar plaga",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PlagOutColors.TextMain,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            BuscadorAdmin(
                valor = busqueda,
                onCambio = { busqueda = it },
                placeholder = "Buscar plaga",
                tag = "txtBuscarPlagaCultivo"
            )
            val vacio = when {
                plagas.isEmpty() -> "No se pudieron cargar las plagas."
                opciones.isEmpty() && busqueda.isBlank() -> "Todas las plagas activas ya afectan a este cultivo."
                opciones.isEmpty() -> "Ninguna plaga coincide con la búsqueda."
                else -> null
            }
            if (vacio != null) {
                Text(
                    vacio,
                    textAlign = TextAlign.Center,
                    color = PlagOutColors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(opciones, key = { it.id }) { plaga ->
                        Surface(
                            onClick = { onElegir(plaga) },
                            color = PlagOutColors.Surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("itemPlagaBuscador_${plaga.id}")
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.BugReport, contentDescription = null, tint = PlagOutColors.Terracotta)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(plaga.nombre, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
                                    Text(
                                        plaga.nombre_cientifico,
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = PlagOutColors.TextSecondary
                                    )
                                }
                                Icon(Icons.Default.Add, contentDescription = null, tint = PlagOutColors.Forest)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccionBajaCultivo(activo: Boolean, habilitada: Boolean, onClick: () -> Unit) {
    val color = if (activo) PlagOutColors.RiskDanger else PlagOutColors.Forest
    OutlinedButton(
        onClick = onClick,
        enabled = habilitada,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(if (activo) "btnBajaCultivo" else "btnReactivarCultivo")
    ) {
        Icon(if (activo) Icons.Outlined.Archive else Icons.Outlined.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (activo) "Dar de baja cultivo" else "Reactivar cultivo", fontWeight = FontWeight.Bold)
    }
}
