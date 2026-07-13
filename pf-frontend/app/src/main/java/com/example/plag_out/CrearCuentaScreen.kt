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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

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

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PlagOutColors.TextOnDark)
                }
                Column {
                    Text(
                        "Plag-Out",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                    Text("Registro", color = PlagOutColors.TextOnDark, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
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
                    Text("Nueva Cuenta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PlagOutColors.TextMain)
                    Text("Completá tus datos profesionales", fontSize = 13.sp, color = PlagOutColors.TextSecondary)

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = state.nombre,
                        onValueChange = { authViewModel.actualizarNombre(it) },
                        label = { Text("Nombre") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PlagOutColors.Forest) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = camposColors(),
                        modifier = Modifier.fillMaxWidth().testTag("txtNombre")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.apellido,
                        onValueChange = { authViewModel.actualizarApellido(it) },
                        label = { Text("Apellido") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PlagOutColors.Forest) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = camposColors(),
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
                            leadingIcon = { Icon(Icons.Default.Work, contentDescription = null, tint = PlagOutColors.Forest) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownCargoExpanded) },
                            shape = RoundedCornerShape(16.dp),
                            colors = camposColors(),
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
                        label = { Text("Email corporativo") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PlagOutColors.Forest) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = camposColors(),
                        modifier = Modifier.fillMaxWidth().testTag("txtEmailRegistro")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var mostrarPassword by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { authViewModel.actualizarPasswordRegistro(it) },
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
                        modifier = Modifier.fillMaxWidth().testTag("txtPasswordRegistro")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var mostrarRepetirPassword by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = state.repetirPassword,
                        onValueChange = { authViewModel.actualizarRepetirPassword(it) },
                        label = { Text("Repetir contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PlagOutColors.Forest) },
                        trailingIcon = {
                            IconButton(onClick = { mostrarRepetirPassword = !mostrarRepetirPassword }) {
                                Icon(
                                    imageVector = if (mostrarRepetirPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = PlagOutColors.TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        isError = state.repetirPassword.isNotBlank() && !passwordsCoinciden,
                        visualTransformation = if (mostrarRepetirPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = camposColors(),
                        modifier = Modifier.fillMaxWidth().testTag("txtRepetirPassword")
                    )

                    AnimatedVisibility(
                        visible = state.repetirPassword.isNotBlank() && !passwordsCoinciden,
                        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
                    ) {
                        Text(
                            "Las contraseñas no coinciden",
                            color = PlagOutColors.RiskDanger,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = state.error != null,
                        enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                        exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
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
                                modifier = Modifier.testTag("txtErrorRegistro")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { authViewModel.crearCuenta(onSuccess = onCuentaCreada) },
                        enabled = formularioValido && !state.cargando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlagOutColors.Forest,
                            disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("btnCrearCuenta")
                    ) {
                        if (state.cargando) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PlagOutColors.TextOnDark, strokeWidth = 3.dp)
                        } else {
                            Text("CREAR CUENTA", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = PlagOutColors.TextOnDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
