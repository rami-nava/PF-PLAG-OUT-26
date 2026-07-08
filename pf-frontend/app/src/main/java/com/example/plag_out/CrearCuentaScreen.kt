package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCuentaScreen(
    authViewModel: AuthViewModel,
    onCuentaCreada: () -> Unit,
    onBack: () -> Unit
) {

    LaunchedEffect(Unit) {
        authViewModel.cargarCargos()
    }

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

    Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
        // Fondo decorativo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ColorPrimary, ColorSecondary)
                    ),
                    shape = RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Text(
                    "Registro",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "Nueva Cuenta",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextMain
                    )
                    Text(
                        "Completá tus datos profesionales",
                        fontSize = 13.sp,
                        color = ColorTextSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = state.nombre,
                        onValueChange = { authViewModel.actualizarNombre(it) },
                        label = { Text("Nombre") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ColorPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorPrimary, focusedLabelColor = ColorPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("txtNombre")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.apellido,
                        onValueChange = { authViewModel.actualizarApellido(it) },
                        label = { Text("Apellido") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ColorPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorPrimary, focusedLabelColor = ColorPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("txtApellido")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownCargoExpanded,
                        onExpandedChange = { dropdownCargoExpanded = !dropdownCargoExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = state.cargo?.tipo ?: "",
                            onValueChange = {},
                            label = { Text("Cargo") },
                            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = ColorPrimary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownCargoExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorPrimary, focusedLabelColor = ColorPrimary),
                            modifier = Modifier.menuAnchor().fillMaxWidth().testTag("txtCargo")
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownCargoExpanded,
                            onDismissRequest = { dropdownCargoExpanded = false }
                        ) {
                            state.cargosDisponibles.forEach { cargo ->
                                DropdownMenuItem(
                                    text = { Text(cargo.tipo) },
                                    onClick = {
                                        authViewModel.actualizarCargo(cargo)
                                        dropdownCargoExpanded = false
                                    },
                                    modifier = Modifier.testTag("opcionCargo_${cargo.tipo}")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = { authViewModel.actualizarEmailRegistro(it) },
                        label = { Text("Email Corporativo") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ColorPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorPrimary, focusedLabelColor = ColorPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("txtEmailRegistro")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var mostrarPassword by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { authViewModel.actualizarPasswordRegistro(it) },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ColorPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                                Icon(imageVector = if (mostrarPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorPrimary, focusedLabelColor = ColorPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("txtPasswordRegistro")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var mostrarRepetirPassword by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.repetirPassword,
                        onValueChange = { authViewModel.actualizarRepetirPassword(it) },
                        label = { Text("Repetir Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ColorPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarRepetirPassword = !mostrarRepetirPassword }) {
                                Icon(imageVector = if (mostrarRepetirPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = state.repetirPassword.isNotBlank() && !passwordsCoinciden,
                        visualTransformation = if (mostrarRepetirPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorPrimary, focusedLabelColor = ColorPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("txtRepetirPassword")
                    )

                    if (state.repetirPassword.isNotBlank() && !passwordsCoinciden) {
                        Text("Las contraseñas no coinciden", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    if (state.error != null) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp).testTag("txtErrorRegistro"))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { authViewModel.crearCuenta(onSuccess = onCuentaCreada) },
                        enabled = formularioValido && !state.cargando,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("btnCrearCuenta")
                    ) {
                        if (state.cargando) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 3.dp)
                        } else {
                            Text("CREAR CUENTA", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
