package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

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
        DialogoCambiarPassword(
            authViewModel = authViewModel,
            emailUsuario = usuario?.email,
            onDismiss = { mostrarCambiarPassword = false }
        )
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


