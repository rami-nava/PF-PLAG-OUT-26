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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors
import com.example.plag_out.ui.theme.StaggeredAppear

// ── Cuenta del admin ────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AdminCuentaScreen(
    userViewModel: UserViewModel,
    authViewModel: AuthViewModel,
    onCerrarSesion: () -> Unit
) {
    val state by userViewModel.state.collectAsState()
    val usuario = state.usuario
    var mostrarCambiarPassword by remember { mutableStateOf(false) }
    var confirmarCierre by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { userViewModel.getUsuario() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = PlagOutColors.Surface,
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarIniciales(
                    usuario?.let { "${it.nombre} ${it.apellido}" } ?: "",
                    tamano = 72,
                    fondo = PlagOutColors.Forest
                )
                Spacer(Modifier.height(12.dp))
                if (usuario == null && state.isLoading) {
                    CircularProgressIndicator(color = PlagOutColors.Forest, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        usuario?.let { "${it.nombre} ${it.apellido}".trim() }?.ifEmpty { null } ?: "Administrador",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlagOutColors.TextMain,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("txtNombreAdmin")
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .background(PlagOutColors.Sun.copy(alpha = 0.18f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, tint = PlagOutColors.Bark, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Administrador", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.Bark)
                }
            }
        }

        state.error?.let { BannerErrorAdmin(mensaje = it) }

        if (usuario != null) {
            TarjetaPerfil {
                FilaInfoAdmin(Icons.Outlined.Email, "Email", usuario.email)
                DivisorFila()
                FilaInfoAdmin(Icons.Outlined.CalendarMonth, "Alta", formatearFecha(usuario.fecha_creacion))
            }
        }

        TarjetaPerfil {
            FilaAccion(
                icono = Icons.Outlined.Lock,
                titulo = "Cambiar contraseña",
                subtitulo = null,
                tag = "btnCambiarPasswordAdmin",
                onClick = { mostrarCambiarPassword = true }
            )
            DivisorFila()
            FilaAccion(
                icono = Icons.AutoMirrored.Filled.Logout,
                titulo = "Cerrar sesión",
                subtitulo = null,
                tag = "btnCerrarSesionAdmin",
                onClick = { confirmarCierre = true },
                acento = PlagOutColors.RiskDanger
            )
        }
    }

    if (mostrarCambiarPassword) {
        DialogoCambiarPassword(authViewModel = authViewModel, onDismiss = { mostrarCambiarPassword = false })
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

// ── Métricas (próximamente) ─────────────────────────────────────────────────

private data class MetricaPrevista(val icono: ImageVector, val titulo: String, val descripcion: String)

private val METRICAS_PREVISTAS = listOf(
    MetricaPrevista(Icons.Outlined.People, "Usuarios activos", "Altas y uso de la app por semana."),
    MetricaPrevista(Icons.Outlined.BugReport, "Monitoreos por plaga", "Qué plagas se siguen más y en qué cultivos."),
    MetricaPrevista(Icons.Outlined.NotificationsActive, "Alertas disparadas", "Cuántos monitoreos llegaron a riesgo alto."),
    MetricaPrevista(Icons.Outlined.Map, "Reportes por región", "Dónde se están reportando focos.")
)

@Composable
fun AdminMetricasScreen() {
    val flote = rememberInfiniteTransition(label = "floteMetricas")
    val desplazamientoY by flote.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floteMetricasY"
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        Surface(
            color = PlagOutColors.Leaf.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier
                .size(108.dp)
                .offset(y = desplazamientoY.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Insights, contentDescription = null, tint = PlagOutColors.Leaf, modifier = Modifier.size(50.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Métricas globales", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = PlagOutColors.TextMain)
        Spacer(Modifier.height(6.dp))
        PastillaEstado("PRÓXIMAMENTE", PlagOutColors.Bark, modifier = Modifier.testTag("etiquetaProximamente"))
        Text(
            "Un tablero con el estado de todo el sistema. Esto es lo que va a mostrar:",
            textAlign = TextAlign.Center,
            color = PlagOutColors.TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp, bottom = 20.dp)
        )
        METRICAS_PREVISTAS.forEachIndexed { index, metrica ->
            StaggeredAppear(index = index) {
                Surface(
                    color = PlagOutColors.Surface,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .alpha(0.85f)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconoFila(metrica.icono)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(metrica.titulo, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain, fontSize = 14.sp)
                            Text(metrica.descripcion, color = PlagOutColors.TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
