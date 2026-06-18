/* package com.example.plag_out

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CultivoOption(
    val name: String,
    val emoji: String,
    val bgColor: Color
)

private val DiagonalShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width * 0.72f, 0f)
    lineTo(size.width * 0.50f, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
fun SelectCultivoScreen(
    nuevaPlantacionViewModel: NuevaPlantacionViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val state by nuevaPlantacionViewModel.state.collectAsState()

    val cultivoOptions = listOf(
        CultivoOption("Maíz", "🌽", Color(0xFFF5D87A)),
        CultivoOption("Soja", "🌱", Color(0xFFC8DFA8))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F7F4))
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver a mis terrenos",
                    tint = Color(0xFF2d5016)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Seleccione el Cultivo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2d5016)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            cultivoOptions.forEach { cultivo ->
                val isSelected = state.cultivoSeleccionado == cultivo.name

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clickable { nuevaPlantacionViewModel.seleccionarCultivo(cultivo.name) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF2d5016).copy(alpha = 0.08f) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = if (isSelected) BorderStroke(2.dp, Color(0xFF2d5016)) else null
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(DiagonalShape)
                                .background(cultivo.bgColor.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = cultivo.emoji,
                                fontSize = 52.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 20.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = cultivo.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2d5016)
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Seleccionado",
                                tint = Color(0xFF2d5016),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onContinue,
            enabled = state.cultivoSeleccionado != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE8941A),
                disabledContainerColor = Color(0xFFE8941A).copy(alpha = 0.5f),
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = "Continuar →",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

*/