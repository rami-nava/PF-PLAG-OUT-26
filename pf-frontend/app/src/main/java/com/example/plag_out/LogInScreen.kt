package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Paleta de Colores Dinámica ──────────────────────────────────────────────
private val ColorPrimary      = Color(0xFF1B5E20)
private val ColorSecondary    = Color(0xFF43A047)
private val ColorBackground   = Color(0xFFF1F4F1)
private val ColorTextMain     = Color(0xFF1A231E)
private val ColorTextSecondary= Color(0xFF5F6D66)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onCrearCuenta: () -> Unit
) {
    val state by authViewModel.loginState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        // Fondo decorativo superior
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ColorPrimary, ColorSecondary)
                    ),
                    shape = RoundedCornerShape(bottomStart = 64.dp, bottomEnd = 64.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 8.dp,
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_plagout),
                        contentDescription = "Logo",
                        modifier = Modifier.scale(2.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "PLAG-OUT",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                "Gestión Inteligente de Cultivos",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Login Form Card
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Bienvenido",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextMain
                    )
                    Text(
                        "Ingresá a tu cuenta",
                        fontSize = 14.sp,
                        color = ColorTextSecondary
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { authViewModel.actualizarEmail(it) },
                        label = { Text("Correo Electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ColorPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            focusedLabelColor = ColorPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("txtEmail")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var mostrarPassword by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { authViewModel.actualizarPassword(it) },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ColorPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                Icon(
                                    imageVector = if (mostrarPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimary,
                            focusedLabelColor = ColorPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("txtPassword")
                    )

                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp).testTag("txtErrorLogin")
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { authViewModel.iniciarSesion(onSuccess = onLoginSuccess) },
                        enabled = state.email.isNotBlank() && state.password.isNotBlank() && !state.cargando,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("btnIniciarSesion")
                    ) {
                        if (state.cargando) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        } else {
                            Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tenés cuenta?", color = ColorTextSecondary, fontSize = 14.sp)
                TextButton(onClick = onCrearCuenta, modifier = Modifier.testTag("btnIrACrearCuenta")) {
                    Text("Registrate ahora", color = ColorPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
