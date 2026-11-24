package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.OroClaro
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.SuperficieOscura
import poker.modelo.Variante

@Composable
fun HorseHeaderBar(
    varianteActual: Variante,
    manoEnVariante: Int,
    manosPorVariante: Int = 5,
    onAbrirConfiguracion: () -> Unit,
    onAbrirReglas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SuperficieOscura,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Indicador de rotación HORSE
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Variante.entries.forEach { v ->
                    val esActual = v == varianteActual
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (esActual) OroFichas else Color(0xFF2A2A2A))
                            .border(
                                width = if (esActual) 1.5.dp else 1.dp,
                                color = if (esActual) OroClaro else Color(0xFF444444),
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = v.sigla.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (esActual) Color.Black else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = varianteActual.nombreCompleto,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Mano $manoEnVariante de $manosPorVariante",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }
            }

            // Botones de acción
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAbrirReglas) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Reglas de la variante",
                        tint = OroFichas
                    )
                }
                IconButton(onClick = onAbrirConfiguracion) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración de partida",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
