package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Loop
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun AdminPlagasScreen(
    viewModel: AdminPlagasViewModel,
    onNuevaPlaga: () -> Unit,
    onEditarPlaga: (Int) -> Unit,
    onSinPermiso: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(state.sinPermiso) { if (state.sinPermiso) onSinPermiso() }

    val visibles = remember(state.plagas, state.busqueda, state.filtro) {
        filtrarPlagas(state.plagas, state.busqueda, state.filtro)
    }
    val activas = state.plagas.count { it.estaActiva }

    Scaffold(
        containerColor = PlagOutColors.Cream,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            var mostrado by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { mostrado = true }
            val escala by animateFloatAsState(
                targetValue = if (mostrado) 1f else 0f,
                animationSpec = tween(360, easing = FastOutSlowInEasing),
                label = "escalaFabPlaga"
            )
            if (state.cargado) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaPlaga,
                    containerColor = PlagOutColors.Sun,
                    contentColor = PlagOutColors.ForestDark,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Nueva plaga", fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .graphicsLayer { scaleX = escala; scaleY = escala }
                        .testTag("btnNuevaPlaga")
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
                titulo = "Catálogo de plagas",
                subtitulo = if (state.cargado) {
                    "$activas activas · ${state.plagas.size - activas} dadas de baja"
                } else {
                    "Parámetros que usa el cálculo de GDD"
                }
            )
            BuscadorAdmin(
                valor = state.busqueda,
                onCambio = viewModel::buscar,
                placeholder = "Buscar por nombre o nombre científico",
                tag = "txtBuscarPlaga"
            )
            FiltroChipsRow(
                opciones = listOf(
                    OpcionFiltro(FiltroPlagas.TODAS.ordinal, "Todas", state.plagas.size),
                    OpcionFiltro(FiltroPlagas.ACTIVAS.ordinal, "Activas", activas),
                    OpcionFiltro(FiltroPlagas.INACTIVAS.ordinal, "De baja", state.plagas.size - activas)
                ),
                seleccionado = state.filtro.ordinal,
                onSeleccion = { viewModel.cambiarFiltro(FiltroPlagas.entries[it]) },
                modifier = Modifier.padding(top = 0.dp)
            )

            BannerErrorAdmin(
                mensaje = state.errorAccion,
                onCerrar = viewModel::descartarErrorAccion,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when {
                state.isLoading && !state.cargado -> SkeletonCargando(alturaTarjeta = 112.dp, cantidad = 4)
                state.error != null && !state.cargado -> EstadoErrorAdmin(state.error!!) { viewModel.cargar() }
                else -> PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refrescar,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        state.plagas.isEmpty() -> EstadoVacioFlotante(
                            icono = Icons.Outlined.BugReport,
                            titulo = "Todavía no hay plagas",
                            subtitulo = "Agregá la primera para que los productores puedan monitorearla."
                        )
                        visibles.isEmpty() -> EstadoSinResultados(
                            subtitulo = "Ninguna plaga coincide con la búsqueda o el filtro."
                        )
                        else -> LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("listaPlagasAdmin")
                        ) {
                            itemsIndexed(visibles, key = { _, plaga -> plaga.id }) { index, plaga ->
                                StaggeredAppear(index = index, modifier = Modifier.animateItem()) {
                                    TarjetaPlagaAdmin(plaga, cultivos = state.cultivos) { onEditarPlaga(plaga.id) }
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
private fun TarjetaPlagaAdmin(plaga: PlagaAdmin, cultivos: List<CultivoAdmin>, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interaccion)
    val acento = if (plaga.estaActiva) PlagOutColors.Leaf else PlagOutColors.RiskUnknown
    val nombresCultivos = plaga.cultivos_afectados.orEmpty()
        .mapNotNull { id -> cultivos.firstOrNull { it.id == id }?.nombre }

    Surface(
        onClick = onClick,
        interactionSource = interaccion,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .testTag("tarjetaPlaga_${plaga.id}")
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .background(acento.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.BugReport, contentDescription = null, tint = acento, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(
                Modifier
                    .weight(1f)
                    .alpha(if (plaga.estaActiva) 1f else 0.7f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plaga.nombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!plaga.estaActiva) {
                        Spacer(Modifier.width(8.dp))
                        PastillaEstado("DE BAJA", PlagOutColors.RiskUnknown)
                    }
                }
                Text(
                    plaga.nombre_cientifico,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = PlagOutColors.TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    EtiquetaInfo(Icons.Outlined.Thermostat, rangoTermico(plaga), PlagOutColors.Terracotta)
                    plaga.gdd_eclosion?.let {
                        EtiquetaInfo(Icons.Outlined.Egg, "Huevo ${formatearNumero(it)} GDD", PlagOutColors.Bark)
                    }
                    plaga.gdd_generacion?.let {
                        EtiquetaInfo(Icons.Outlined.Loop, "Adulto ${formatearNumero(it)} GDD", PlagOutColors.Bark)
                    }
                    if (nombresCultivos.isNotEmpty()) {
                        EtiquetaInfo(Icons.Outlined.Grass, nombresCultivos.joinToString(", "), PlagOutColors.Leaf)
                    }
                }
                avisoDeFuente(plaga)?.let { aviso ->
                    Text(
                        aviso,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.Terracotta,
                        modifier = Modifier.padding(top = 6.dp).testTag("avisoFuente_${plaga.id}")
                    )
                }
                val monitoreos = plaga.monitoreos_activos
                if (monitoreos != null && monitoreos > 0) {
                    Text(
                        "$monitoreos ${if (monitoreos == 1) "monitoreo activo" else "monitoreos activos"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlagOutColors.Forest,
                        modifier = Modifier.padding(top = 6.dp)
                    )
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

/** Aviso cuando la plaga no tiene modelo citado o una edición la apartó de su fuente. */
fun avisoDeFuente(plaga: PlagaAdmin): String? {
    val referencia = plaga.referencia_termica ?: return null
    if (!referencia.validada) return "Sin modelo térmico validado: no admite monitoreos nuevos"
    val difiere = referencia.difiere.orEmpty()
    return if (difiere.isEmpty()) null
    else if (difiere.size == 1) "Un parámetro difiere de su fuente citada"
    else "${difiere.size} parámetros difieren de su fuente citada"
}

private fun rangoTermico(plaga: PlagaAdmin): String {
    val base = plaga.temp_base?.let(::formatearNumero) ?: "?"
    val max = plaga.temp_max?.let(::formatearNumero) ?: "?"
    return "$base – $max °C"
}

// ── Formulario de alta / edición ────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlagaFormScreen(
    viewModel: AdminPlagasViewModel,
    plagaId: Int?,
    onBack: () -> Unit,
    onGuardado: () -> Unit,
    onCatalogoCambiado: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val original = remember(plagaId, state.plagas) { plagaId?.let { viewModel.plaga(it) } }

    // Se llegó a una plaga que no está en la lista (p. ej. el proceso se recreó): volver.
    if (plagaId != null && original == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var form by remember(plagaId) { mutableStateOf(original?.let { FormularioPlaga.desde(it) } ?: FormularioPlaga()) }
    var intentoGuardar by remember { mutableStateOf(false) }
    var confirmarBaja by remember { mutableStateOf(false) }
    val errores = remember(form) { validarPlaga(form) }
    fun errorDe(campo: CampoPlaga) = if (intentoGuardar) errores[campo] else null

    LaunchedEffect(Unit) { viewModel.descartarErrorAccion() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
            .imePadding()
    ) {
        HeaderDetalleAdmin(
            etiqueta = if (original == null) "Catálogo de plagas" else original.nombre,
            titulo = if (original == null) "Nueva Plaga" else "Editar Plaga",
            onBack = onBack
        ) {
            if (original != null && !original.estaActiva) {
                PastillaEstado("DADA DE BAJA", PlagOutColors.Sun)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val monitoreos = original?.monitoreos_activos ?: 0
            if (monitoreos > 0) {
                AvisoImpacto(
                    "Esta plaga tiene $monitoreos ${if (monitoreos == 1) "monitoreo activo" else "monitoreos activos"}. " +
                        "Los cambios en los parámetros se aplican desde el próximo cálculo diario, sin recalcular lo acumulado."
                )
            }

            SeccionFormulario(titulo = "Identificación", subtitulo = "Cómo la ven los productores al elegirla.") {
                CampoFormularioAdmin(
                    valor = form.nombre,
                    onCambio = { form = form.copy(nombre = it) },
                    etiqueta = "Nombre común",
                    placeholder = "Ej: Carpocapsa",
                    error = errorDe(CampoPlaga.NOMBRE),
                    icono = Icons.Outlined.BugReport,
                    tag = "txtNombrePlaga"
                )
                Spacer(Modifier.height(14.dp))
                CampoFormularioAdmin(
                    valor = form.nombreCientifico,
                    onCambio = { form = form.copy(nombreCientifico = it) },
                    etiqueta = "Nombre científico",
                    placeholder = "Ej: Cydia pomonella",
                    error = errorDe(CampoPlaga.NOMBRE_CIENTIFICO),
                    icono = Icons.Outlined.Science,
                    tag = "txtNombreCientifico"
                )
            }

            SeccionFormulario(
                titulo = "Parámetros térmicos",
                subtitulo = "Umbrales de temperatura entre los que la plaga acumula grados-día."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    CampoFormularioAdmin(
                        valor = form.tempBase,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(tempBase = nuevo) },
                        etiqueta = "Temp. base",
                        unidad = "°C",
                        error = errorDe(CampoPlaga.TEMP_BASE),
                        tag = "txtTempBase",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    CampoFormularioAdmin(
                        valor = form.tempMax,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(tempMax = nuevo) },
                        etiqueta = "Temp. máxima",
                        unidad = "°C",
                        error = errorDe(CampoPlaga.TEMP_MAX),
                        tag = "txtTempMax",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(14.dp))
                CampoFormularioAdmin(
                    valor = form.tempBaseHuevo,
                    onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(tempBaseHuevo = nuevo) },
                    etiqueta = "Temp. base del huevo (opcional)",
                    unidad = "°C",
                    error = errorDe(CampoPlaga.TEMP_BASE_HUEVO),
                    tag = "txtTempBaseHuevo",
                    placeholder = "Vacío: usa la temp. base",
                    teclado = KeyboardType.Decimal
                )
                Text(
                    "Solo si la fuente mide el huevo con otra base.",
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            SeccionFormulario(
                titulo = "Grados-día del ciclo",
                subtitulo = "GDD del huevo hasta la eclosión, sobre la base del huevo, y GDD hasta el adulto."
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    CampoFormularioAdmin(
                        valor = form.gddEclosion,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(gddEclosion = nuevo) },
                        etiqueta = "Huevo",
                        unidad = "GDD",
                        error = errorDe(CampoPlaga.GDD_ECLOSION),
                        tag = "txtGddEclosion",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                    CampoFormularioAdmin(
                        valor = form.gddGeneracion,
                        onCambio = { nuevo -> if (esNumeroPlaga(nuevo)) form = form.copy(gddGeneracion = nuevo) },
                        etiqueta = "Hasta el adulto",
                        unidad = "GDD",
                        error = errorDe(CampoPlaga.GDD_GENERACION),
                        tag = "txtGddGeneracion",
                        placeholder = "0",
                        teclado = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Hasta el adulto, contados",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PlagOutColors.TextMain,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                OpcionesAdmin(
                    opciones = OPCIONES_GENERACION_DESDE,
                    elegida = form.generacionDesde,
                    onElegir = { form = form.copy(generacionDesde = it) },
                    tag = "opcionGeneracion"
                )
            }

            SeccionFormulario(
                titulo = "Tipo de desarrollo",
                subtitulo = "Define qué etapas biológicas se pueden elegir al reportar esta plaga."
            ) {
                OpcionesAdmin(
                    opciones = OPCIONES_TIPO_DESARROLLO,
                    elegida = form.tipoDesarrollo,
                    onElegir = { form = form.copy(tipoDesarrollo = it) },
                    tag = "opcionDesarrollo"
                )
                errorDe(CampoPlaga.TIPO_DESARROLLO)?.let {
                    Text(it, color = PlagOutColors.RiskDanger, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            original?.referencia_termica?.let { referencia ->
                SeccionParametrosCitados(original, referencia)
            }

            SeccionFormulario(
                titulo = "Cultivos afectados",
                subtitulo = "Solo se ofrece para monitorear en estos cultivos."
            ) {
                // Los dados de baja solo aparecen si la plaga ya los tenía, para poder quitarlos
                val cultivosOfrecidos = state.cultivos.filter {
                    it.estaActivo || it.id in original?.cultivos_afectados.orEmpty()
                }
                if (cultivosOfrecidos.isEmpty()) {
                    Text("No se pudieron cargar los cultivos.", fontSize = 13.sp, color = PlagOutColors.TextSecondary)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        cultivosOfrecidos.forEach { cultivo ->
                            val elegido = cultivo.id in form.cultivos
                            FilterChip(
                                selected = elegido,
                                onClick = {
                                    form = form.copy(
                                        cultivos = if (elegido) form.cultivos - cultivo.id else form.cultivos + cultivo.id
                                    )
                                },
                                label = { Text(if (cultivo.estaActivo) cultivo.nombre else "${cultivo.nombre} (de baja)") },
                                leadingIcon = if (elegido) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PlagOutColors.Forest,
                                    selectedLabelColor = PlagOutColors.TextOnDark,
                                    selectedLeadingIconColor = PlagOutColors.TextOnDark,
                                    labelColor = PlagOutColors.TextMain
                                ),
                                modifier = Modifier.testTag("chipCultivo_${cultivo.id}")
                            )
                        }
                    }
                }
                errorDe(CampoPlaga.CULTIVOS)?.let {
                    Text(it, color = PlagOutColors.RiskDanger, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            if (original != null) {
                AccionBajaPlaga(
                    activa = original.estaActiva,
                    habilitada = !state.guardando,
                    onClick = {
                        if (original.estaActiva) confirmarBaja = true
                        else viewModel.reactivar(original.id) {
                            Avisos.exito("Plaga reactivada.")
                            onCatalogoCambiado()
                        }
                    }
                )
            }
        }

        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
        ) {
            BannerErrorAdmin(
                mensaje = state.errorAccion,
                onCerrar = viewModel::descartarErrorAccion,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Button(
                onClick = {
                    intentoGuardar = true
                    viewModel.guardar(original, form) {
                        onCatalogoCambiado()
                        onGuardado()
                    }
                },
                enabled = !state.guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnGuardarPlaga"),
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
                    Text(if (original == null) "Crear plaga" else "Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmarBaja && original != null) {
        val monitoreos = original.monitoreos_activos ?: 0
        DialogoConfirmacionAdmin(
            icono = Icons.Outlined.Archive,
            titulo = "¿Dar de baja la plaga?",
            iconoFicha = Icons.Outlined.BugReport,
            nombre = original.nombre,
            detalle = original.nombre_cientifico,
            consecuencias = listOf(
                "Deja de ofrecerse para monitoreos nuevos",
                "Deja de ofrecerse para reportes nuevos"
            ),
            tranquilidad = (if (monitoreos > 0) {
                "${if (monitoreos == 1) "El monitoreo activo sigue" else "Los ${monitoreos} monitoreos activos siguen"} funcionando. "
            } else "") + "Podés reactivarla cuando quieras.",
            textoConfirmar = "Dar de baja",
            peligro = true,
            onConfirmar = {
                confirmarBaja = false
                viewModel.desactivar(original.id) {
                    Avisos.exito("Plaga dada de baja.")
                    onCatalogoCambiado()
                }
            },
            onDismiss = { confirmarBaja = false }
        )
    }
}

internal data class OpcionAdmin(val valor: String, val titulo: String, val detalle: String)

// Las mismas opciones y textos que el panel web (src/lib/plagas.ts).
internal val OPCIONES_GENERACION_DESDE = listOf(
    OpcionAdmin(GENERACION_DESDE_BIOFIX, "Desde el biofix", "La constante de la fuente va de huevo a adulto"),
    OpcionAdmin(GENERACION_DESDE_ECLOSION, "Desde la eclosión", "La constante de la fuente va de larva a adulto")
)

internal val OPCIONES_TIPO_DESARROLLO = listOf(
    OpcionAdmin(DESARROLLO_COMPLETO, "Metamorfosis completa", "Huevo, larva y adulto (orugas, isocas, barrenadores)"),
    OpcionAdmin(DESARROLLO_INCOMPLETO, "Metamorfosis incompleta", "Huevo, ninfa y adulto (pulgones, chinches, chicharritas)")
)

@Composable
private fun OpcionesAdmin(opciones: List<OpcionAdmin>, elegida: String?, onElegir: (String) -> Unit, tag: String) {
    Column(Modifier.selectableGroup()) {
        opciones.forEach { opcion ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(
                        selected = opcion.valor == elegida,
                        onClick = { onElegir(opcion.valor) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 6.dp)
                    .testTag("${tag}_${opcion.valor}")
            ) {
                RadioButton(
                    selected = opcion.valor == elegida,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = PlagOutColors.Forest)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(opcion.titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PlagOutColors.TextMain)
                    Text(opcion.detalle, fontSize = 12.sp, color = PlagOutColors.TextSecondary)
                }
            }
        }
    }
}

// Etiquetas como en el panel (CAMPOS_TERMICOS); el orden es el del archivo de parámetros.
internal val CAMPOS_TERMICOS = listOf(
    "temp_base" to "Temperatura base",
    "temp_max" to "Temperatura máxima",
    "temp_base_huevo" to "Temperatura base del huevo",
    "gdd_eclosion" to "Grados-día del huevo",
    "gdd_generacion" to "Grados-día hasta el adulto",
    "gdd_generacion_desde" to "Generación contada",
    "tipo_desarrollo" to "Tipo de desarrollo"
)

internal fun valorDelCatalogo(plaga: PlagaAdmin, campo: String): Any? = when (campo) {
    "temp_base" -> plaga.temp_base
    "temp_max" -> plaga.temp_max
    "temp_base_huevo" -> plaga.temp_base_huevo
    "gdd_eclosion" -> plaga.gdd_eclosion
    "gdd_generacion" -> plaga.gdd_generacion
    "gdd_generacion_desde" -> plaga.gdd_generacion_desde
    "tipo_desarrollo" -> plaga.tipo_desarrollo
    else -> null
}

internal fun mostrarValorTermico(campo: String, valor: Any?): String = when {
    valor == null -> "—"
    campo == "gdd_generacion_desde" -> OPCIONES_GENERACION_DESDE.firstOrNull { it.valor == valor }?.titulo ?: valor.toString()
    campo == "tipo_desarrollo" -> OPCIONES_TIPO_DESARROLLO.firstOrNull { it.valor == valor }?.titulo ?: valor.toString()
    valor is Number -> formatearNumero(valor.toDouble()).replace('.', ',') + if (campo.startsWith("temp")) " °C" else " GDD"
    else -> valor.toString()
}

/** Los valores citados de la plaga frente a los del catálogo, con sus fuentes (como el panel). */
@Composable
private fun SeccionParametrosCitados(plaga: PlagaAdmin, referencia: ReferenciaTermica) {
    val uriHandler = LocalUriHandler.current
    val difiere = referencia.difiere.orEmpty().toSet()
    SeccionFormulario(
        titulo = if (referencia.validada) "Parámetros citados" else "Sin modelo térmico validado",
        subtitulo = when {
            !referencia.validada ->
                (referencia.motivo ?: "La especie no está en el archivo de parámetros citados.") +
                    " Se puede reportar, pero no admite monitoreos GDD nuevos; los que ya existen siguen calculando."
            difiere.isEmpty() -> "Los valores del catálogo coinciden con su fuente."
            difiere.size == 1 -> "Un valor del catálogo difiere de su fuente. El cálculo usa los del catálogo."
            else -> "${difiere.size} valores del catálogo difieren de su fuente. El cálculo usa los del catálogo."
        }
    ) {
        if (referencia.validada) {
            CAMPOS_TERMICOS.forEach { (campo, etiqueta) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .testTag("filaCitada_$campo")
                ) {
                    Text(etiqueta, fontSize = 13.sp, color = PlagOutColors.TextMain, modifier = Modifier.weight(1.3f))
                    Text(
                        mostrarValorTermico(campo, valorDelCatalogo(plaga, campo)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (campo in difiere) PlagOutColors.RiskDanger else PlagOutColors.TextMain,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        mostrarValorTermico(campo, referencia.valores?.get(campo)),
                        fontSize = 13.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Text(
                "Catálogo · Fuente. En rojo, lo que difiere.",
                fontSize = 11.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
        referencia.fuentes.orEmpty().forEach { fuente ->
            val enlace = fuente.enlace
            Text(
                fuente.cita + (fuente.doi?.let { " doi:$it" } ?: ""),
                fontSize = 12.sp,
                color = if (enlace != null) PlagOutColors.Forest else PlagOutColors.TextMain,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .then(if (enlace != null) Modifier.clickable { uriHandler.openUri(enlace) } else Modifier)
                    .testTag("fuenteCitada_${fuente.clave}")
            )
        }
    }
}

@Composable
internal fun SeccionFormulario(titulo: String, subtitulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
            Text(
                subtitulo,
                fontSize = 12.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            contenido()
        }
    }
}

// Solo dígitos, signo y un separador decimal: la temperatura base puede ser negativa.
private fun esNumeroPlaga(texto: String) = texto.all { it.isDigit() || it in "-.," }

@Composable
internal fun AvisoImpacto(texto: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PlagOutColors.Sun.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("avisoImpactoPlaga"),
        verticalAlignment = Alignment.Top
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, fontSize = 13.sp, color = PlagOutColors.TextMain)
    }
}

@Composable
private fun AccionBajaPlaga(activa: Boolean, habilitada: Boolean, onClick: () -> Unit) {
    val color = if (activa) PlagOutColors.RiskDanger else PlagOutColors.Forest
    OutlinedButton(
        onClick = onClick,
        enabled = habilitada,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(if (activa) "btnDarDeBajaPlaga" else "btnReactivarPlaga")
    ) {
        Icon(if (activa) Icons.Outlined.Archive else Icons.Outlined.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(if (activa) "Dar de baja" else "Reactivar plaga", fontWeight = FontWeight.Bold)
    }
}
