package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.EstadisticaCompacta
import com.example.plag_out.ui.theme.EtiquetaInfo
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.SeparadorVertical
import com.example.plag_out.ui.theme.StaggeredAppear
import com.example.plag_out.ui.theme.contadorAnimado
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PerfilScreen(
    userViewModel: UserViewModel,
    terrenosViewModel: TerrenosViewModel,
    plantacionesViewModel: PlantacionesViewModel,
    monitoreosViewModel: MonitoreosViewModel,
    onBack: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val state by userViewModel.state.collectAsState()
    val terrenosState by terrenosViewModel.state.collectAsState()
    val plantacionesState by plantacionesViewModel.state.collectAsState()
    val monitoreosState by monitoreosViewModel.state.collectAsState()
    var confirmarCierre by remember { mutableStateOf(false) }

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
        HeaderPerfil(usuario = state.usuario, onBack = onBack)

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
                        TarjetaInformacionPersonal(usuario)
                    }

                    Spacer(Modifier.height(12.dp))

                    StaggeredAppear(1) {
                        TarjetaActividad(
                            terrenos = terrenosState.terrenos.size,
                            plantacionesActivas = plantacionesState.plantaciones.count { it.activa },
                            monitoreos = monitoreosState.monitoreos.size
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    StaggeredAppear(2) {
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

    if (confirmarCierre) {
        DialogoCerrarSesion(
            onConfirmar = {
                confirmarCierre = false
                onCerrarSesion()
            },
            onDismiss = { confirmarCierre = false }
        )
    }
}

@Composable
private fun HeaderPerfil(usuario: UsuarioResponse?, onBack: () -> Unit) {
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
                .offset(x = 50.dp, y = (-40).dp)
                .size(120.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )

        Column(Modifier.padding(start = 8.dp, end = 20.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("btnVolverPerfil")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                }
                Text(
                    "Mi cuenta",
                    color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(PlagOutColors.TextOnDark.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        iniciales,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlagOutColors.TextOnDark,
                        modifier = Modifier.testTag("txtInicialesPerfil")
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    usuario?.let { "${it.nombre} ${it.apellido}".trim() } ?: "Cargando perfil…",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PlagOutColors.TextOnDark,
                    textAlign = TextAlign.Center
                )
                if (usuario != null && usuario.cargo.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    EtiquetaInfo(
                        icono = Icons.Outlined.Work,
                        texto = usuario.cargo,
                        color = PlagOutColors.Sun
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TarjetaInformacionPersonal(usuario: UsuarioResponse) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Información personal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PlagOutColors.TextMain
            )
            Spacer(Modifier.height(6.dp))

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
                tag = "txtEmailPerfil"
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
}

@Composable
private fun TarjetaActividad(terrenos: Int, plantacionesActivas: Int, monitoreos: Int) {
    Surface(
        color = PlagOutColors.Surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
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
}

@Composable
private fun FilaDato(
    icono: ImageVector,
    etiqueta: String,
    valor: String,
    tag: String,
    subvalor: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(PlagOutColors.Leaf.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, contentDescription = null, tint = PlagOutColors.Forest, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
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
