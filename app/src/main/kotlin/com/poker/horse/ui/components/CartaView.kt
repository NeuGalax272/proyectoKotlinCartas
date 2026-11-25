package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    ancho: Dp = 44.dp,
    alto: Dp = 62.dp,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(6.dp)

    Surface(
        modifier = modifier
            .size(ancho, alto)
            .shadow(6.dp, shape = forma)
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
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE0E0E0), forma)
                    .padding(2.dp)
            ) {
                // Rango y palo en la esquina superior izquierda
                Column(
                    modifier = Modifier.align(Alignment.TopStart),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = carta.rango.etiqueta,
                        fontSize = (ancho.value * 0.26f).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorTexto,
                        lineHeight = (ancho.value * 0.26f).sp
                    )
                    Text(
                        text = carta.palo.simbolo,
                        fontSize = (ancho.value * 0.22f).sp,
                        color = colorTexto,
                        lineHeight = (ancho.value * 0.22f).sp
                    )
                }

                // Símbolo central
                Text(
                    text = carta.palo.simbolo,
                    fontSize = (ancho.value * 0.45f).sp,
                    color = colorTexto.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center)
                )

                // Rango en la esquina inferior derecha
                Text(
                    text = carta.rango.etiqueta,
                    fontSize = (ancho.value * 0.22f).sp,
                    fontWeight = FontWeight.Bold,
                    color = colorTexto,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        } else {
            // Reverso de carta elegante
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E2640), Color(0xFF0F172A))
                        )
                    )
                    .border(1.5.dp, Color(0xFF388E3C), forma)
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "♠",
                        fontSize = (ancho.value * 0.4f).sp,
                        color = Color(0xFF81C784).copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
