package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.GroupOff
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.EstadoSinResultados
import com.example.plag_out.ui.theme.EstadoVacioFlotante
import com.example.plag_out.ui.theme.FiltroChipsRow
import com.example.plag_out.ui.theme.OpcionFiltro
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SkeletonCargando
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.rememberPressScale

// ── Lista ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsuariosScreen(
    viewModel: AdminUsuariosViewModel,
    onUsuarioClick: (String) -> Unit,
    onSinPermiso: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.cargar() }
    LaunchedEffect(state.sinPermiso) { if (state.sinPermiso) onSinPermiso() }

    // Scroll infinito: al acercarse al final de lo cargado se pide la página siguiente
    val cercaDelFinal by remember {
        derivedStateOf {
            val ultimoVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            ultimoVisible >= listState.layoutInfo.totalItemsCount - 4
        }
    }
    val visibles = state.usuariosVisibles
    LaunchedEffect(cercaDelFinal, state.usuarios.size) {
        if (cercaDelFinal && state.cargado) viewModel.cargarMas()
    }

    Column(Modifier.fillMaxSize()) {
        EncabezadoSeccionAdmin(
            titulo = "Usuarios",
            subtitulo = if (state.cargado) {
                "${state.total} ${if (state.total == 1) "cuenta" else "cuentas"} de productores y técnicos"
            } else {
                "Cuentas de productores y técnicos"
            }
        )
        BuscadorAdmin(
            valor = state.busqueda,
            onCambio = viewModel::buscar,
            placeholder = "Buscar por nombre o email",
            tag = "txtBuscarUsuario"
        )
        FiltroChipsRow(
            opciones = listOf(
                OpcionFiltro(FiltroUsuarios.TODOS.ordinal, "Todos"),
                OpcionFiltro(FiltroUsuarios.ACTIVOS.ordinal, "Activos"),
                OpcionFiltro(FiltroUsuarios.SUSPENDIDOS.ordinal, "Suspendidos", icono = Icons.Outlined.Block, colorIcono = PlagOutColors.RiskDanger)
            ),
            seleccionado = state.filtro.ordinal,
            onSeleccion = { viewModel.cambiarFiltro(FiltroUsuarios.entries[it]) }
        )

        BannerErrorAdmin(
            mensaje = state.errorAccion,
            onCerrar = viewModel::descartarErrorAccion,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        when {
            state.isLoading && visibles.isEmpty() -> SkeletonCargando(alturaTarjeta = 76.dp, cantidad = 6)
            state.error != null && state.usuarios.isEmpty() -> EstadoErrorAdmin(state.error!!) { viewModel.cargar(forzar = true) }
            else -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = viewModel::refrescar,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    visibles.isEmpty() && state.busqueda.isBlank() && state.filtro == FiltroUsuarios.TODOS ->
                        EstadoVacioFlotante(
                            icono = Icons.Outlined.GroupOff,
                            titulo = "Todavía no hay usuarios",
                            subtitulo = "Las cuentas aparecen acá cuando alguien se registra en la app."
                        )
                    visibles.isEmpty() -> EstadoSinResultados(
                        subtitulo = "Ninguna cuenta coincide con la búsqueda o el filtro."
                    )
                    else -> LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("listaUsuariosAdmin")
                    ) {
                        itemsIndexed(visibles, key = { _, u -> u.id }) { index, usuario ->
                            StaggeredAppear(index = index % TAMANIO_ANIMADO, modifier = Modifier.animateItem()) {
                                FilaUsuarioAdmin(usuario) { onUsuarioClick(usuario.id) }
                            }
                        }
                        if (state.cargandoMas) {
                            item(key = "cargandoMas") {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = PlagOutColors.Forest, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Las páginas nuevas entran escalonadas desde su propio primer ítem, no desde el índice global. */
private const val TAMANIO_ANIMADO = AdminUsuariosViewModel.TAMANIO_PAGINA

@Composable
private fun FilaUsuarioAdmin(usuario: UsuarioAdmin, onClick: () -> Unit) {
    val interaccion = remember { MutableInteractionSource() }
    val escala = rememberPressScale(interaccion)
    Surface(
        onClick = onClick,
        interactionSource = interaccion,
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .testTag("filaUsuario_${usuario.email}")
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarIniciales(
                usuario.nombreCompleto,
                fondo = if (usuario.estaActivo) PlagOutColors.Leaf else PlagOutColors.RiskUnknown
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        usuario.nombreCompleto,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!usuario.estaActivo) {
                        Spacer(Modifier.width(8.dp))
                        PastillaEstado("SUSPENDIDA", PlagOutColors.RiskDanger)
                    }
                }
                Text(
                    usuario.email,
                    fontSize = 12.sp,
                    color = PlagOutColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                usuario.cargo?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 11.sp, color = PlagOutColors.Leaf, fontWeight = FontWeight.SemiBold)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PlagOutColors.TextSecondary)
        }
    }
}

// ── Detalle ─────────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminUsuarioDetalleScreen(
    viewModel: AdminUsuariosViewModel,
    usuarioId: String,
    onBack: () -> Unit,
    onEliminado: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val usuario = state.usuarios.firstOrNull { it.id == usuarioId }

    // El detalle sale de la lista ya cargada; si no está (proceso recreado), se vuelve a la lista.
    if (usuario == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var confirmarSuspension by remember { mutableStateOf(false) }
    var confirmarEliminacion by remember { mutableStateOf(false) }
    val procesando = state.procesando == usuario.id

    LaunchedEffect(Unit) { viewModel.descartarErrorAccion() }

    Column(Modifier.fillMaxSize()) {
        HeaderDetalleAdmin(etiqueta = "Usuario", titulo = usuario.nombreCompleto, onBack = onBack) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PastillaEstado(
                    if (usuario.estaActivo) "ACTIVA" else "SUSPENDIDA",
                    if (usuario.estaActivo) PlagOutColors.TextOnDark else PlagOutColors.Sun,
                    modifier = Modifier.testTag("estadoUsuarioDetalle")
                )
                if (procesando) {
                    Spacer(Modifier.width(10.dp))
                    CircularProgressIndicator(color = PlagOutColors.TextOnDark, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BannerErrorAdmin(mensaje = state.errorAccion, onCerrar = viewModel::descartarErrorAccion)

            TarjetaPerfil {
                FilaInfoAdmin(Icons.Outlined.Email, "Email", usuario.email, tag = "txtEmailUsuarioDetalle")
                DivisorFila()
                FilaInfoAdmin(Icons.Outlined.Badge, "Cargo", usuario.cargo?.takeIf { it.isNotBlank() } ?: "Sin cargo")
                DivisorFila()
                FilaInfoAdmin(
                    Icons.Outlined.CalendarMonth,
                    "Alta",
                    usuario.fecha_creacion?.let { formatearFecha(it) } ?: "Sin dato"
                )
                DivisorFila()
                FilaInfoAdmin(
                    Icons.Outlined.History,
                    "Último acceso",
                    formatearFechaHora(usuario.ultimo_acceso) ?: "Sin dato"
                )
            }

            TarjetaPerfil {
                Text(
                    "Acciones",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlagOutColors.TextSecondary,
                    letterSpacing = 0.4.sp
                )
                if (usuario.estaActivo) {
                    FilaAccion(
                        icono = Icons.Outlined.Block,
                        titulo = "Suspender cuenta",
                        subtitulo = "No va a poder iniciar sesión hasta que la reactives.",
                        tag = "btnSuspenderUsuario",
                        onClick = { if (!procesando) confirmarSuspension = true },
                        acento = PlagOutColors.RiskWarn
                    )
                } else {
                    FilaAccion(
                        icono = Icons.Outlined.LockOpen,
                        titulo = "Reactivar cuenta",
                        subtitulo = "Vuelve a poder iniciar sesión con sus datos intactos.",
                        tag = "btnReactivarUsuario",
                        onClick = { if (!procesando) viewModel.reactivar(usuario.id) }
                    )
                }
                DivisorFila()
                FilaAccion(
                    icono = Icons.Outlined.DeleteForever,
                    titulo = "Eliminar cuenta",
                    subtitulo = "Borra la cuenta y todos sus datos. No se puede deshacer.",
                    tag = "btnEliminarUsuario",
                    onClick = { if (!procesando) confirmarEliminacion = true },
                    acento = PlagOutColors.RiskDanger
                )
            }

            Row(
                Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = PlagOutColors.TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Los roles se asignan desde la base de datos, no desde la app.",
                    fontSize = 11.sp,
                    color = PlagOutColors.TextSecondary
                )
            }
        }
    }

    if (confirmarSuspension) {
        DialogoConfirmacionAdmin(
            icono = Icons.Outlined.Block,
            titulo = "¿Suspender la cuenta?",
            iconoFicha = Icons.Outlined.Person,
            nombre = usuario.nombreCompleto,
            detalle = usuario.email,
            consecuencias = listOf(
                "Se cierra su sesión",
                "No va a poder volver a entrar hasta que la reactives",
                "Deja de recibir alertas"
            ),
            tranquilidad = "Sus datos se conservan y podés reactivarla cuando quieras.",
            textoConfirmar = "Suspender",
            peligro = true,
            onConfirmar = {
                confirmarSuspension = false
                viewModel.suspender(usuario.id)
            },
            onDismiss = { confirmarSuspension = false }
        )
    }

    if (confirmarEliminacion) {
        var confirmacion by remember { mutableStateOf("") }
        val coincide = confirmacion.trim().equals(usuario.email, ignoreCase = true)
        DialogoConfirmacionAdmin(
            icono = Icons.Outlined.DeleteForever,
            titulo = "¿Eliminar la cuenta?",
            iconoFicha = Icons.Outlined.Person,
            nombre = usuario.nombreCompleto,
            detalle = usuario.email,
            consecuencias = listOf(
                "Se borra la cuenta",
                "Se borran sus terrenos, cultivos y monitoreos",
                "No se puede deshacer"
            ),
            textoConfirmar = "Eliminar",
            peligro = true,
            confirmarHabilitado = coincide,
            onConfirmar = {
                confirmarEliminacion = false
                viewModel.eliminar(usuario.id, onEliminado)
            },
            onDismiss = { confirmarEliminacion = false }
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                "PARA CONFIRMAR, ESCRIBÍ SU EMAIL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 8.dp)
            )
            OutlinedTextField(
                value = confirmacion,
                onValueChange = { confirmacion = it },
                placeholder = { Text(usuario.email, color = PlagOutColors.TextSecondary.copy(alpha = 0.55f)) },
                singleLine = true,
                trailingIcon = if (coincide) {
                    { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = PlagOutColors.RiskDanger) }
                } else null,
                shape = RoundedCornerShape(14.dp),
                colors = camposAdminColors(acento = PlagOutColors.RiskDanger),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("txtConfirmarEliminarUsuario")
            )
        }
    }
}
