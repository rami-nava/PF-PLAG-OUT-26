package com.example.plag_out

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.AlmacenamientoLocal.PreferenciasUsuario
import com.example.plag_out.Service.FcmTokenRegistrar
import com.example.plag_out.ui.theme.EstadisticaCompacta
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SeparadorVertical
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.contadorAnimado
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Perfil del usuario. Al no haber pantalla de ajustes ni drawer, esta pantalla es también el lugar
 * donde viven los permisos, la ayuda y las acciones de cuenta.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    userViewModel: UserViewModel,
    terrenosViewModel: TerrenosViewModel,
    plantacionesViewModel: PlantacionesViewModel,
    monitoreosViewModel: MonitoreosViewModel,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onEditarPerfil: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val state by userViewModel.state.collectAsState()
    val terrenosState by terrenosViewModel.state.collectAsState()
    val plantacionesState by plantacionesViewModel.state.collectAsState()
    val monitoreosState by monitoreosViewModel.state.collectAsState()

    var confirmarCierre by remember { mutableStateOf(false) }
    var mostrarComoFunciona by remember { mutableStateOf(false) }
    var mostrarCambiarPassword by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userViewModel.getUsuario()
        // Cache-first: alimentan el resumen de actividad sin pegarle de más al backend
        terrenosViewModel.getTerrenos()
        plantacionesViewModel.getPlantaciones()
        monitoreosViewModel.getMonitoreos()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderPerfil(
            usuario = state.usuario,
            onBack = onBack,
            onEditar = onEditarPerfil
        )

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { userViewModel.refrescar() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                when {
                    state.usuario == null && state.isLoading -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PlagOutColors.Forest)
                        }
                    }

                    state.usuario == null -> {
                        ErrorPerfil(
                            mensaje = state.error ?: "No se pudo cargar tu perfil.",
                            onReintentar = { userViewModel.refrescar() }
                        )
                    }

                    else -> {
                        val usuario = state.usuario!!

                        StaggeredAppear(0) {
                            TarjetaInformacionPersonal(usuario, onEditar = onEditarPerfil)
                        }

                        Spacer(Modifier.height(12.dp))

                        StaggeredAppear(1) {
                            TarjetaActividad(
                                terrenos = terrenosState.terrenos.size,
                                plantacionesActivas = plantacionesState.plantaciones.count { it.activa },
                                monitoreos = monitoreosState.monitoreos.size
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        StaggeredAppear(2) {
                            TarjetaPermisos()
                        }

                        Spacer(Modifier.height(12.dp))

                        StaggeredAppear(3) {
                            TarjetaCuenta(onCambiarPassword = { mostrarCambiarPassword = true })
                        }

                        Spacer(Modifier.height(12.dp))

                        StaggeredAppear(4) {
                            TarjetaAyuda(
                                onComoFunciona = { mostrarComoFunciona = true },
                                onSincronizar = {
                                    userViewModel.refrescar()
                                    terrenosViewModel.refrescar()
                                    plantacionesViewModel.refrescar()
                                    monitoreosViewModel.refrescar()
                                }
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        StaggeredAppear(5) {
                            OutlinedButton(
                                onClick = { confirmarCierre = true },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, PlagOutColors.RiskDanger.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PlagOutColors.RiskDanger),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .testTag("btnCerrarSesion")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Cerrar sesión", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    if (confirmarCierre) {
        DialogoCerrarSesion(
            onConfirmar = {
                confirmarCierre = false
                onCerrarSesion()
            },
            onDismiss = { confirmarCierre = false }
        )
    }

    if (mostrarComoFunciona) {
        ComoFuncionaSheet(onDismiss = { mostrarComoFunciona = false })
    }

    if (mostrarCambiarPassword) {
        DialogoCambiarPassword(
            authViewModel = authViewModel,
            onDismiss = { mostrarCambiarPassword = false }
        )
    }
}

@Composable
private fun HeaderPerfil(usuario: UsuarioResponse?, onBack: () -> Unit, onEditar: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderPerfil")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativaPerfil"
    )

    val iniciales = if (usuario != null) {
        listOfNotNull(usuario.nombre.trim().firstOrNull(), usuario.apellido.trim().firstOrNull())
            .joinToString("")
            .uppercase()
            .ifEmpty { "?" }
    } else "…"

    val forma = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = forma
            )
            .clip(forma)
            // El degradado se dibuja detrás de la status bar; el contenido queda debajo
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 44.dp, y = (-34).dp)
                .size(90.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )

        // Una sola fila: el nombre hace de título, así el header queda en ~72dp en lugar de ~175dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btnVolverPerfil")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(PlagOutColors.TextOnDark.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    iniciales,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlagOutColors.TextOnDark,
                    modifier = Modifier.testTag("txtInicialesPerfil")
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    usuario?.let { "${it.nombre} ${it.apellido}".trim() } ?: "Cargando perfil…",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlagOutColors.TextOnDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (usuario != null && usuario.cargo.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    EtiquetaInfo(
                        icono = Icons.Outlined.Work,
                        texto = usuario.cargo,
                        color = PlagOutColors.Sun
                    )
                }
            }

            IconButton(
                onClick = onEditar,
                enabled = usuario != null,
                modifier = Modifier.testTag("btnEditarPerfil")
            ) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Editar perfil",
                    tint = PlagOutColors.TextOnDark
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TarjetaInformacionPersonal(usuario: UsuarioResponse, onEditar: () -> Unit) {
    TarjetaPerfil {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Información personal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )
            TextButton(
                onClick = onEditar,
                modifier = Modifier.testTag("btnEditarDatos")
            ) {
                Text("Editar", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        FilaDato(
            icono = Icons.Outlined.Badge,
            etiqueta = "Nombre y apellido",
            valor = "${usuario.nombre} ${usuario.apellido}".trim(),
            tag = "txtNombrePerfil"
        )
        DivisorFila()
        FilaDato(
            icono = Icons.Outlined.Email,
            etiqueta = "Email",
            valor = usuario.email,
            tag = "txtEmailPerfil",
            trailing = {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = PlagOutColors.RiskUnknown,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        DivisorFila()
        FilaDato(
            icono = Icons.Outlined.Work,
            etiqueta = "Cargo",
            valor = usuario.cargo.ifBlank { "Sin cargo asignado" },
            tag = "txtCargoPerfil"
        )
        DivisorFila()
        FilaDato(
            icono = Icons.Outlined.CalendarMonth,
            etiqueta = "Miembro desde",
            valor = formatearFecha(usuario.fecha_creacion),
            tag = "txtFechaCreacionPerfil",
            subvalor = antiguedad(usuario.fecha_creacion)
        )
    }
}

@Composable
private fun TarjetaActividad(terrenos: Int, plantacionesActivas: Int, monitoreos: Int) {
    TarjetaPerfil {
        Text(
            "Tu actividad",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextMain
        )
        Text(
            "Lo que estás gestionando en Plag-Out",
            fontSize = 12.sp,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EstadisticaCompacta("TERRENOS", "${contadorAnimado(terrenos)}")
            SeparadorVertical()
            EstadisticaCompacta("PLANTACIONES", "${contadorAnimado(plantacionesActivas)}")
            SeparadorVertical()
            EstadisticaCompacta("MONITOREOS", "${contadorAnimado(monitoreos)}")
        }
    }
}

/**
 * Permisos del sistema que usa la app.
 *
 * Las notificaciones son un switch real porque hay dos capas: el permiso del sistema y una
 * preferencia propia que se traduce en registrar o borrar el token FCM en el backend. La ubicación,
 * en cambio, no puede tener switch — una app no puede revocar un permiso ya otorgado, así que
 * queda como estado + acción.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TarjetaPermisos() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Debajo de API 33 no existe permiso de notificaciones en runtime: se considera otorgado.
    val estadoNotificaciones =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            rememberEstadoPermiso(Manifest.permission.POST_NOTIFICATIONS)
        } else null
    val permisoNotificaciones = estadoNotificaciones?.otorgado ?: true

    var prefNotificaciones by remember {
        mutableStateOf(PreferenciasUsuario.notificacionesActivadas(context))
    }

    val activarNotificaciones = {
        PreferenciasUsuario.setNotificacionesActivadas(context, true)
        prefNotificaciones = true
        scope.launch { FcmTokenRegistrar.registrar() }
    }

    val launcherNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        estadoNotificaciones?.revalidar()
        if (concedido) {
            activarNotificaciones()
        } else if (denegadoPermanentemente(context, Manifest.permission.POST_NOTIFICATIONS)) {
            abrirAjustesDeNotificaciones(context)
        }
    }

    val estadoUbicacion = rememberEstadoPermiso(Manifest.permission.ACCESS_FINE_LOCATION)
    val launcherUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        estadoUbicacion.revalidar()
        if (!concedido && denegadoPermanentemente(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            abrirAjustesDeApp(context)
        }
    }

    TarjetaPerfil {
        Text(
            "Permisos",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextMain
        )
        Text(
            "Qué le permitís a Plag-Out en este dispositivo",
            fontSize = 12.sp,
            color = PlagOutColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        FilaSwitch(
            icono = Icons.Outlined.Notifications,
            titulo = "Notificaciones",
            subtitulo = when {
                !permisoNotificaciones -> "Bloqueadas desde los ajustes del sistema"
                prefNotificaciones -> "Recibís alertas cuando el riesgo supera tu umbral"
                else -> "Desactivadas: no vas a recibir alertas"
            },
            activo = permisoNotificaciones && prefNotificaciones,
            tag = "switchNotificaciones",
            onCambio = { activar ->
                if (activar) {
                    when {
                        permisoNotificaciones -> activarNotificaciones()
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                            launcherNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> abrirAjustesDeNotificaciones(context)
                    }
                } else {
                    PreferenciasUsuario.setNotificacionesActivadas(context, false)
                    prefNotificaciones = false
                    scope.launch { FcmTokenRegistrar.desregistrar() }
                }
            }
        )

        if (!permisoNotificaciones) {
            TextButton(
                onClick = { abrirAjustesDeNotificaciones(context) },
                modifier = Modifier.testTag("btnAjustesNotificaciones")
            ) {
                Text("Abrir ajustes", color = PlagOutColors.Forest, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        DivisorFila()

        FilaAccion(
            icono = Icons.Outlined.LocationOn,
            titulo = "Ubicación",
            subtitulo = "Solo para ubicar tus terrenos en el mapa",
            tag = "filaPermisoUbicacion",
            onClick = {
                if (estadoUbicacion.otorgado) {
                    abrirAjustesDeApp(context)
                } else {
                    launcherUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            trailing = {
                if (estadoUbicacion.otorgado) {
                    EtiquetaInfo(
                        icono = Icons.Outlined.CheckCircle,
                        texto = "Permitida",
                        color = PlagOutColors.RiskOk
                    )
                } else {
                    Text(
                        "Permitir",
                        color = PlagOutColors.Forest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        )
    }
}

@Composable
private fun TarjetaCuenta(onCambiarPassword: () -> Unit) {
    TarjetaPerfil {
        Text(
            "Cuenta",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextMain
        )
        FilaAccion(
            icono = Icons.Outlined.Lock,
            titulo = "Cambiar contraseña",
            subtitulo = "Elegí una contraseña nueva para tu cuenta",
            tag = "btnCambiarPassword",
            onClick = onCambiarPassword
        )
    }
}

@Composable
private fun TarjetaAyuda(onComoFunciona: () -> Unit, onSincronizar: () -> Unit) {
    TarjetaPerfil {
        Text(
            "Ayuda y acerca de",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PlagOutColors.TextMain
        )
        FilaAccion(
            icono = Icons.AutoMirrored.Outlined.HelpOutline,
            titulo = "¿Cómo funciona Plag-Out?",
            subtitulo = "Qué hace la app y cómo se usa, en un minuto",
            tag = "btnComoFunciona",
            onClick = onComoFunciona
        )
        DivisorFila()
        FilaAccion(
            icono = Icons.Outlined.Sync,
            titulo = "Volver a sincronizar",
            subtitulo = "Descargá de nuevo terrenos, plantaciones y monitoreos",
            tag = "btnSincronizar",
            onClick = onSincronizar
        )
        DivisorFila()
        FilaDato(
            icono = Icons.Outlined.Info,
            etiqueta = "Versión",
            valor = BuildConfig.VERSION_NAME,
            tag = "txtVersionApp"
        )
    }
}

/** Cambio de contraseña contra Supabase, con el molde visual de [DialogoCerrarSesion]. */
@Composable
private fun DialogoCambiarPassword(authViewModel: AuthViewModel, onDismiss: () -> Unit) {
    var nueva by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    val coinciden = nueva == repetir
    val valido = nueva.length >= 6 && coinciden

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PlagOutColors.Surface,
        icon = {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = PlagOutColors.Forest)
        },
        title = {
            Text("Cambiar contraseña", fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = nueva,
                    onValueChange = { nueva = it; error = null },
                    label = { Text("Contraseña nueva") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = camposColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txtPasswordNueva")
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = repetir,
                    onValueChange = { repetir = it; error = null },
                    label = { Text("Repetir contraseña") },
                    singleLine = true,
                    isError = repetir.isNotBlank() && !coinciden,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(16.dp),
                    colors = camposColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("txtPasswordRepetir")
                )
                val mensaje = error ?: when {
                    repetir.isNotBlank() && !coinciden -> "Las contraseñas no coinciden"
                    nueva.isNotBlank() && nueva.length < 6 -> "Mínimo 6 caracteres"
                    else -> null
                }
                if (mensaje != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        mensaje,
                        color = PlagOutColors.RiskDanger,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("txtErrorPassword")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    enviando = true
                    authViewModel.cambiarPassword(
                        nueva = nueva,
                        onSuccess = {
                            enviando = false
                            onDismiss()
                        },
                        onError = {
                            enviando = false
                            error = it
                        }
                    )
                },
                enabled = valido && !enviando,
                modifier = Modifier.testTag("btnConfirmarCambiarPassword")
            ) {
                Text(
                    "Guardar",
                    // El color va atado al estado: si no, el botón se ve activo estando deshabilitado.
                    color = if (valido && !enviando) PlagOutColors.Forest
                            else PlagOutColors.Forest.copy(alpha = 0.38f),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PlagOutColors.TextSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

// ── Átomos de la pantalla ────────────────────────────────────────────────────

/** Tarjeta de sección: el recipe de tarjeta del repo (22.dp / elevación 2.dp / padding 20.dp). */
@Composable
private fun TarjetaPerfil(contenido: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), content = contenido)
    }
}

/** Chip circular con ícono, compartido por todas las filas. */
@Composable
private fun IconoFila(icono: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(PlagOutColors.Leaf.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icono, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilaDato(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    tag: String,
    subvalor: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoFila(icono)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                etiqueta,
                fontSize = 11.sp,
                color = PlagOutColors.TextSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
            Text(
                valor,
                fontSize = 14.sp,
                color = PlagOutColors.TextMain,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag(tag)
            )
            if (subvalor != null) {
                Text(
                    subvalor,
                    fontSize = 11.sp,
                    color = PlagOutColors.Leaf,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/** Fila que navega o dispara una acción. */
@Composable
private fun FilaAccion(
    icono: ImageVector,
    titulo: String,
    subtitulo: String?,
    tag: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoFila(icono)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                fontSize = 14.sp,
                color = PlagOutColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitulo != null) {
                Text(
                    subtitulo,
                    fontSize = 11.sp,
                    color = PlagOutColors.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/** Fila con interruptor. */
@Composable
private fun FilaSwitch(
    icono: ImageVector,
    titulo: String,
    subtitulo: String,
    activo: Boolean,
    tag: String,
    onCambio: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoFila(icono)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                titulo,
                fontSize = 14.sp,
                color = PlagOutColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                subtitulo,
                fontSize = 11.sp,
                color = PlagOutColors.TextSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = activo,
            onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PlagOutColors.Surface,
                checkedTrackColor = PlagOutColors.Forest,
                uncheckedThumbColor = PlagOutColors.Surface,
                uncheckedTrackColor = PlagOutColors.RiskUnknown
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun DivisorFila() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PlagOutColors.Divider)
    )
}

@Composable
private fun ErrorPerfil(mensaje: String, onReintentar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = PlagOutColors.RiskDanger,
            modifier = Modifier.size(44.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            mensaje,
            color = PlagOutColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onReintentar,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PlagOutColors.Forest,
                contentColor = PlagOutColors.TextOnDark
            ),
            modifier = Modifier.testTag("btnReintentarPerfil")
        ) {
            Text("Reintentar", fontWeight = FontWeight.Bold)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatearFecha(fecha: LocalDate): String {
    val formato = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-AR"))
    return fecha.format(formato)
}

@RequiresApi(Build.VERSION_CODES.O)
private fun antiguedad(desde: LocalDate): String {
    val periodo = Period.between(desde, LocalDate.now())
    return when {
        periodo.years > 0 -> "${periodo.years} ${if (periodo.years == 1) "año" else "años"} en Plag-Out"
        periodo.months > 0 -> "${periodo.months} ${if (periodo.months == 1) "mes" else "meses"} en Plag-Out"
        else -> "${periodo.days} ${if (periodo.days == 1) "día" else "días"} en Plag-Out"
    }
}
