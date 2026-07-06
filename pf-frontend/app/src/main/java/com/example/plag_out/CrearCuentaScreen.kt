package com.example.plag_out

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

private val ColorTextoSub = Color(0xFF718096)
private val ColorVerde        = Color(0xFF2d5016)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCuentaScreen(
    authViewModel: AuthViewModel,
    onCuentaCreada: () -> Unit,
    onBack: () -> Unit
) {
    val state by authViewModel.crearCuentaState.collectAsState()
    var dropdownCargoExpanded by remember { mutableStateOf(false) }

    val passwordsCoinciden = state.password == state.repetirPassword
    val formularioValido = state.nombre.isNotBlank() &&
            state.apellido.isNotBlank() &&
            state.cargo != null &&
            state.email.isNotBlank() &&
            state.password.isNotBlank() &&
            state.repetirPassword.isNotBlank() &&
            passwordsCoinciden

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            IconButton(
                onClick = onBack
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = ColorVerde)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Crear cuenta",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2d5016)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Completá tus datos para registrarte",
            fontSize = 14.sp,
            color = ColorTextoSub
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = state.nombre,
            onValueChange = { authViewModel.actualizarNombre(it) },
            label = { Text("Nombre") },
            singleLine = true,
            colors = outlinedColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txtNombre")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.apellido,
            onValueChange = { authViewModel.actualizarApellido(it) },
            label = { Text("Apellido") },
            singleLine = true,
            colors = outlinedColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txtApellido")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Dropdown de cargo
        ExposedDropdownMenuBox(
            expanded = dropdownCargoExpanded,
            onExpandedChange = { dropdownCargoExpanded = !dropdownCargoExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                readOnly = true,
                value = state.cargo?.label ?: "",
                onValueChange = {},
                label = { Text("Cargo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownCargoExpanded) },
                colors = outlinedColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .testTag("txtCargo")
            )

            ExposedDropdownMenu(
                expanded = dropdownCargoExpanded,
                onDismissRequest = { dropdownCargoExpanded = false }
            ) {
                Cargo.entries.forEach { cargo ->
                    DropdownMenuItem(
                        text = { Text(cargo.label) },
                        onClick = {
                            authViewModel.actualizarCargo(cargo)
                            dropdownCargoExpanded = false
                        },
                        modifier = Modifier.testTag("opcionCargo_${cargo.name}")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = { authViewModel.actualizarEmailRegistro(it) },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = outlinedColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txtEmailRegistro")
        )

        Spacer(modifier = Modifier.height(16.dp))

        var mostrarPassword by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = state.password,
            onValueChange = { authViewModel.actualizarPasswordRegistro(it) },
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
                .testTag("txtPasswordRegistro")
        )

        Spacer(modifier = Modifier.height(16.dp))

        var mostrarRepetirPassword by remember { mutableStateOf(false) }

        OutlinedTextField(
            value = state.repetirPassword,
            onValueChange = { authViewModel.actualizarRepetirPassword(it) },
            label = { Text("Repetir contraseña") },
            singleLine = true,
            isError = state.repetirPassword.isNotBlank() && !passwordsCoinciden,
            visualTransformation = if (mostrarRepetirPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { mostrarRepetirPassword = !mostrarRepetirPassword }) {
                    Icon(
                        imageVector = if (mostrarRepetirPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Mostrar/ocultar contraseña"
                    )
                }
            },
            colors = outlinedColors(),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("txtRepetirPassword")
        )

        if (state.repetirPassword.isNotBlank() && !passwordsCoinciden) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Las contraseñas no coinciden",
                color = Color(0xFFA13A2E),
                fontSize = 12.sp,
                modifier = Modifier.testTag("txtErrorPasswords")
            )
        }

        if (state.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.error!!,
                color = Color(0xFFA13A2E),
                fontSize = 13.sp,
                modifier = Modifier.testTag("txtErrorRegistro")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = 
                //authViewModel.crearCuenta(onSuccess = onCuentaCreada)
                onCuentaCreada
            ,
            enabled = formularioValido && !state.cargando,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2d5016)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("btnCrearCuenta")
        ) {
            if (state.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("Crear cuenta")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}