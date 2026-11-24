package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.NegroCarta
import com.poker.horse.ui.theme.RojoCarta
import poker.modelo.Carta
import poker.modelo.Palo

@Composable
fun CartaView(
    carta: Carta?,
    visible: Boolean = true,
    ancho: Dp = 46.dp,
    alto: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(6.dp)

    Surface(
        modifier = modifier
            .size(ancho, alto)
            .shadow(4.dp, shape = forma)
            .clip(forma),
        shape = forma,
        color = Color.White
    ) {
        if (visible && carta != null) {
            val colorTexto = when (carta.palo) {
                Palo.CORAZONES, Palo.DIAMANTES -> RojoCarta
                Palo.TREBOLES, Palo.PICAS -> NegroCarta
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color(0xFFDDDDDD), forma)
                    .padding(3.dp)
            ) {
                // Rango y palo esquina superior izquierda
                Column(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = carta.rango.etiqueta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorTexto,
                        lineHeight = 11.sp
                    )
                    Text(
                        text = carta.palo.simbolo,
                        fontSize = 10.sp,
                        color = colorTexto,
                        lineHeight = 10.sp
                    )
                }

                // Símbolo grande al centro
                Text(
                    text = carta.palo.simbolo,
                    fontSize = 20.sp,
                    color = colorTexto.copy(alpha = 0.85f),
                    modifier = Modifier.align(Alignment.Center)
                )

                // Rango esquina inferior derecha
                Text(
                    text = carta.rango.etiqueta,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        } else {
            // Reverso de la carta (boca abajo)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF283593), Color(0xFF1A237E))
                        )
                    )
                    .border(2.dp, Color.White, forma)
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♠",
                        fontSize = 18.sp,
                        color = Color(0xFFFFD54F).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
