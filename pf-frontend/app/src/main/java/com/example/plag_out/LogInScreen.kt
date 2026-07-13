package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

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
    ) {
        FondoVerdeAuth()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(56.dp))


                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_plagout),
                        contentDescription = "Logo"
                    )
                }


            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "PLAG-OUT",
                color = PlagOutColors.TextOnDark,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp
            )
            Text(
                "Gestión Inteligente de Cultivos",
                color = PlagOutColors.TextOnDark.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(36.dp))

            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Bienvenido", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Ingresá a tu cuenta", fontSize = 14.sp, color = PlagOutColors.TextSecondary)

                    Spacer(modifier = Modifier.height(28.dp))

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { authViewModel.actualizarEmail(it) },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PlagOutColors.Forest) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = camposColors(),
                        modifier = Modifier.fillMaxWidth().testTag("txtEmail")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var mostrarPassword by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { authViewModel.actualizarPassword(it) },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PlagOutColors.Forest) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                Icon(
                                    imageVector = if (mostrarPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = PlagOutColors.TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = camposColors(),
                        modifier = Modifier.fillMaxWidth().testTag("txtPassword")
                    )

                    AnimatedVisibility(
                        visible = state.error != null,
                        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = PlagOutColors.RiskDanger, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = state.error ?: "",
                                color = PlagOutColors.RiskDanger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("txtErrorLogin")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { authViewModel.iniciarSesion(onSuccess = onLoginSuccess) },
                        enabled = state.email.isNotBlank() && state.password.isNotBlank() && !state.cargando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlagOutColors.Forest,
                            disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("btnIniciarSesion")
                    ) {
                        if (state.cargando) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PlagOutColors.TextOnDark, strokeWidth = 3.dp)
                        } else {
                            Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = PlagOutColors.TextOnDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tenés cuenta?", color = PlagOutColors.TextOnDark, fontSize = 14.sp)
                TextButton(onClick = onCrearCuenta, modifier = Modifier.testTag("btnIrACrearCuenta")) {
                    Text("Registrate ahora", color = PlagOutColors.Terracotta, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/** Fondo verde de campo, a pantalla completa, con círculos decorativos flotantes. */
@Composable
fun FondoVerdeAuth() {
    val respiracion = rememberInfiniteTransition(label = "respiracionFondoAuth")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativa"
    )
    val escalaDecorativaLenta by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(5600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativaLenta"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf, PlagOutColors.Forest)))
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-60).dp)
                .size(220.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.07f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-90).dp)
                .size(180.dp)
                .graphicsLayer { scaleX = escalaDecorativaLenta; scaleY = escalaDecorativaLenta }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 40.dp, y = 60.dp)
                .size(120.dp)
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.05f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 70.dp)
                .size(200.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 30.dp)
                .size(140.dp)
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.05f), CircleShape)
        )
    }
}
