package com.example.plag_out

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

private val ColorTextoSub = Color(0xFF718096)

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onCrearCuenta: () -> Unit
) {
    val state by authViewModel.loginState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //Spacer(modifier = Modifier.height(48.dp))

        // Logo grande centrado
        Image(
            painter = painterResource(id = R.drawable.logo_plagout),
            contentDescription = "Logo Plag-Out",
            modifier = Modifier
                .size(300.dp)
                .scale(1.5f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Bienvenido",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2d5016)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Iniciá sesión para continuar",
            fontSize = 14.sp,
            color = ColorTextoSub
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { authViewModel.actualizarEmail(it) },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = outlinedColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txtEmail")
        )

        Spacer(modifier = Modifier.height(16.dp))

        var mostrarPassword by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = state.password,
            onValueChange = { authViewModel.actualizarPassword(it) },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                    Icon(
                        imageVector = if (mostrarPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Mostrar/ocultar contraseña"
                    )
                }
            },
            colors = outlinedColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txtPassword")
        )

        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.error!!,
                color = Color(0xFFA13A2E),
                fontSize = 13.sp,
                modifier = Modifier.testTag("txtErrorLogin")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                authViewModel.iniciarSesion(onSuccess = onLoginSuccess)
            },
            enabled = state.email.isNotBlank() && state.password.isNotBlank() && !state.cargando,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2d5016)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btnIniciarSesion")
        ) {
            if (state.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Iniciar sesión")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row (verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "¿No tenés una cuenta?",
                fontSize = 13.sp,
                color = ColorTextoSub
            )
            TextButton(
                onClick =  onCrearCuenta ,
                modifier = Modifier.testTag("btnIrACrearCuenta")
            ) {
                Text(
                    text = "Creá una",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2d5016)
                )
            }
        }
    }
}