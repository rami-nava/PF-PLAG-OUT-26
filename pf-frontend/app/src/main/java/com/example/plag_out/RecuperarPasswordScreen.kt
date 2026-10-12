package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RecuperarPasswordScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onPasswordRestablecida: () -> Unit
) {
    val estado by authViewModel.recuperarPasswordState.collectAsState()

    var mostrarPasswordNueva by remember { mutableStateOf(false) }
    var mostrarPasswordRepetir by remember { mutableStateOf(false) }

    val emailValido = remember(estado.email) {
        val trimmed = estado.email.trim()
        trimmed.isNotBlank() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    val passwordsValidas = remember(estado.nuevaPassword, estado.repetirPassword) {
        estado.nuevaPassword.length >= 6 &&
            estado.nuevaPassword == estado.repetirPassword
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FondoVerdeAuth()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(28.dp))

            // Barra superior con botón volver
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        authViewModel.limpiarEstadoRecuperacion()
                        onBack()
                    },
                    modifier = Modifier.testTag("btnVolverRecuperacion")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = PlagOutColors.TextOnDark
                    )
                }
                Column {
                    Text(
                        text = "Plag-Out",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = if (estado.sesionDeRecuperacionActiva) "Nueva contraseña" else "Recuperar contraseña",
                        color = PlagOutColors.TextOnDark,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    if (estado.sesionDeRecuperacionActiva) {
                        // ========================================================
                        // MODO 2: Ingreso de nueva contraseña (abierto desde link)
                        // ========================================================
                        Text(
                            text = "Establecer nueva contraseña",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ingresá y confirmá tu nueva contraseña para volver a ingresar a tu cuenta.",
                            fontSize = 13.sp,
                            color = PlagOutColors.TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = estado.nuevaPassword,
                            onValueChange = { authViewModel.actualizarNuevaPasswordRecuperacion(it) },
                            label = { Text("Nueva contraseña") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PlagOutColors.Forest
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { mostrarPasswordNueva = !mostrarPasswordNueva }) {
                                    Icon(
                                        imageVector = if (mostrarPasswordNueva) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = PlagOutColors.TextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !estado.cargando,
                            visualTransformation = if (mostrarPasswordNueva) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = camposColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("txtNuevaPassword")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = estado.repetirPassword,
                            onValueChange = { authViewModel.actualizarRepetirPasswordRecuperacion(it) },
                            label = { Text("Repetir nueva contraseña") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = PlagOutColors.Forest
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { mostrarPasswordRepetir = !mostrarPasswordRepetir }) {
                                    Icon(
                                        imageVector = if (mostrarPasswordRepetir) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = PlagOutColors.TextSecondary
                                    )
                                }
                            },
                            singleLine = true,
                            enabled = !estado.cargando,
                            visualTransformation = if (mostrarPasswordRepetir) VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = camposColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("txtRepetirPassword")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                authViewModel.restablecerPassword(
                                    onSuccess = { onPasswordRestablecida() },
                                    onError = {}
                                )
                            },
                            enabled = passwordsValidas && !estado.cargando,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlagOutColors.Forest,
                                disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("btnGuardarPassword")
                        ) {
                            if (estado.cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PlagOutColors.TextOnDark,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Text(
                                    text = "GUARDAR CONTRASEÑA",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = PlagOutColors.TextOnDark
                                )
                            }
                        }

                    } else if (estado.enlaceEnviado) {
                        // ========================================================
                        // MODO 1-B: Enlace enviado satisfactoriamente
                        // ========================================================
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(PlagOutColors.Forest.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = PlagOutColors.Forest,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "¡Enlace enviado!",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PlagOutColors.TextMain
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Te enviamos un correo a ${estado.email}.\n\nAbrí el mensaje en tu teléfono y presioná el enlace para definir tu nueva contraseña directamente en la aplicación.",
                                fontSize = 14.sp,
                                color = PlagOutColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedButton(
                                onClick = { authViewModel.enviarEnlaceRecuperacion() },
                                enabled = !estado.cargando,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text(
                                    text = "Reenviar correo",
                                    color = PlagOutColors.Forest,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { authViewModel.volverAIngresoEmail() },
                                enabled = !estado.cargando
                            ) {
                                Text(
                                    text = "Usar otro correo electrónico",
                                    color = PlagOutColors.TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                    } else {
                        // ========================================================
                        // MODO 1-A: Formulario para ingresar correo electrónico
                        // ========================================================
                        Text(
                            text = "Recuperar contraseña",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlagOutColors.TextMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ingresá tu correo electrónico registrado y te enviaremos un enlace seguro para restablecer tu contraseña.",
                            fontSize = 13.sp,
                            color = PlagOutColors.TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = estado.email,
                            onValueChange = { authViewModel.actualizarEmailRecuperacion(it) },
                            label = { Text("Correo electrónico") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = PlagOutColors.Forest
                                )
                            },
                            singleLine = true,
                            enabled = !estado.cargando,
                            isError = estado.error != null,
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = camposColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("txtEmailRecuperar")
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { authViewModel.enviarEnlaceRecuperacion() },
                            enabled = emailValido && !estado.cargando,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PlagOutColors.Forest,
                                disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("btnEnviarEnlaceRecuperar")
                        ) {
                            if (estado.cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PlagOutColors.TextOnDark,
                                    strokeWidth = 3.dp
                                )
                            } else {
                                Text(
                                    text = "ENVIAR ENLACE",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = PlagOutColors.TextOnDark
                                )
                            }
                        }
                    }

                    // Mensaje de error general si ocurre
                    AnimatedVisibility(
                        visible = estado.error != null,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = PlagOutColors.RiskDanger,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = estado.error ?: "",
                                color = PlagOutColors.RiskDanger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("txtErrorRecuperacionScreen")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
