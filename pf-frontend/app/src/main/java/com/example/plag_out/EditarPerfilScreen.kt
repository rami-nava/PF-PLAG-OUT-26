package com.example.plag_out

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.plag_out.ui.theme.PlagOutColors

/**
 * Edición de los datos personales: nombre, apellido y cargo.
 */
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    usuario: UsuarioResponse,
    viewModel: EditarPerfilViewModel,
    onBack: () -> Unit,
    onGuardado: (UsuarioResponse) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var dropdownCargoExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(usuario.usuario_id) { viewModel.precargar(usuario) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlagOutColors.Cream)
    ) {
        HeaderEditarPerfil(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(20.dp)
        ) {
            Surface(
                color = PlagOutColors.Surface,
                shape = RoundedCornerShape(22.dp),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        "Datos personales",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlagOutColors.TextMain
                    )
                    Text(
                        "Así te ve el resto del equipo en Plag-Out",
                        fontSize = 12.sp,
                        color = PlagOutColors.TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = state.nombre,
                        onValueChange = { viewModel.actualizarNombre(it) },
                        label = { Text("Nombre") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = PlagOutColors.Forest)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = camposColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("txtEditarNombre")
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.apellido,
                        onValueChange = { viewModel.actualizarApellido(it) },
                        label = { Text("Apellido") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = PlagOutColors.Forest)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = camposColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("txtEditarApellido")
                    )

                    Spacer(Modifier.height(16.dp))

                    ExposedDropdownMenuBox(
                        expanded = dropdownCargoExpanded,
                        onExpandedChange = { dropdownCargoExpanded = !dropdownCargoExpanded }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = state.cargo?.tipo ?: "",
                            onValueChange = {},
                            label = { Text("Cargo") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Work, contentDescription = null, tint = PlagOutColors.Forest)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownCargoExpanded)
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = camposColors(),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                                .testTag("txtEditarCargo")
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownCargoExpanded,
                            onDismissRequest = { dropdownCargoExpanded = false }
                        ) {
                            state.cargosDisponibles.forEach { cargo ->
                                DropdownMenuItem(
                                    text = { Text(cargo.tipo) },
                                    onClick = {
                                        viewModel.actualizarCargo(cargo)
                                        dropdownCargoExpanded = false
                                    },
                                    modifier = Modifier.testTag("opcionCargo_${cargo.tipo}")
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(PlagOutColors.RiskDanger.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = PlagOutColors.RiskDanger,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        state.error ?: "",
                        color = PlagOutColors.RiskDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("txtErrorEditarPerfil")
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { viewModel.guardar(onGuardado) },
                enabled = viewModel.puedeGuardar(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btnGuardarPerfil"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlagOutColors.Forest,
                    disabledContainerColor = PlagOutColors.Forest.copy(alpha = 0.4f),
                    contentColor = PlagOutColors.TextOnDark,
                    disabledContentColor = PlagOutColors.TextOnDark.copy(alpha = 0.6f)
                )
            ) {
                if (state.guardando) {
                    CircularProgressIndicator(
                        color = PlagOutColors.TextOnDark,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeaderEditarPerfil(onBack: () -> Unit) {
    val respiracion = rememberInfiniteTransition(label = "respiracionHeaderEditarPerfil")
    val escalaDecorativa by respiracion.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "escalaDecorativaEditarPerfil"
    )

    val forma = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(listOf(PlagOutColors.Forest, PlagOutColors.Leaf)),
                shape = forma
            )
            .clip(forma)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 50.dp, y = (-40).dp)
                .size(50.dp)
                .graphicsLayer { scaleX = escalaDecorativa; scaleY = escalaDecorativa }
                .background(PlagOutColors.TextOnDark.copy(alpha = 0.06f), CircleShape)
        )

        Column(Modifier.padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btnVolverEditarPerfil")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = PlagOutColors.TextOnDark
                    )
                }
                Column {
                    Text(
                        "Mi cuenta",
                        color = PlagOutColors.TextOnDark.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        "Editar perfil",
                        color = PlagOutColors.TextOnDark,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
