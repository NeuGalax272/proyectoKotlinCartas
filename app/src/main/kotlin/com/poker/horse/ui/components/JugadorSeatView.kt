package com.poker.horse.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.OroClaro
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.RojoFold
import poker.modelo.EstadoJugador
import poker.modelo.Jugador

@Composable
fun JugadorSeatView(
    jugador: Jugador,
    esTurnoActual: Boolean,
    esDealer: Boolean,
    mostrarCartasPrivadas: Boolean = false,
    esCompacto: Boolean = false,
    modifier: Modifier = Modifier
) {
    val estaRetirado = jugador.estado == EstadoJugador.Retirado
    val estaAllIn = jugador.estado == EstadoJugador.AllIn

    // Dimensiones dinámicas según rol y número de cartas
    val cantCartas = jugador.cartas.size
    val anchoCarta: Dp = when {
        jugador.esHumano -> if (cantCartas > 5) 36.dp else 42.dp
        esCompacto -> 28.dp
        else -> if (cantCartas > 5) 30.dp else 34.dp
    }
    val altoCarta: Dp = when {
        jugador.esHumano -> if (cantCartas > 5) 52.dp else 60.dp
        esCompacto -> 40.dp
        else -> if (cantCartas > 5) 44.dp else 48.dp
    }

    val solapamiento: Dp = when {
        cantCartas <= 2 -> 6.dp
        cantCartas <= 4 -> 12.dp
        else -> 18.dp
    }

    // Animación de pulso cuando es el turno del jugador
    val infiniteTransition = rememberInfiniteTransition(label = "TurnoGlow")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaGlow"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fila de cartas con solapamiento adaptable
        Row(
            horizontalArrangement = Arrangement.spacedBy(-solapamiento),
            verticalAlignment = Alignment.CenterVertically
        ) {
            jugador.cartas.forEachIndexed { index, carta ->
                val esVisible = if (mostrarCartasPrivadas || jugador.esHumano) {
                    true
                } else {
                    jugador.cartasVisibles.getOrElse(index) { false }
                }
                CartaView(
                    carta = carta,
                    visible = esVisible,
                    ancho = anchoCarta,
                    alto = altoCarta
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Badge de información del jugador
        Box(contentAlignment = Alignment.TopEnd) {
            val fondo = when {
                estaRetirado -> Color(0xAA212121)
                estaAllIn -> Color(0xDD4A148C)
                jugador.esHumano -> Color(0xEE1B5E20)
                else -> Color(0xEE2A2A2A)
            }

            val colorBordeTurno = if (esTurnoActual) OroClaro.copy(alpha = alphaGlow) else Color.Transparent
            val colorBordeBase = when {
                jugador.esHumano -> Color(0xFF66BB6A)
                else -> Color(0xFF555555)
            }

            val anchoCaja = when {
                jugador.esHumano -> 120.dp
                esCompacto -> 90.dp
                else -> 100.dp
            }

            Column(
                modifier = Modifier
                    .width(anchoCaja)
                    .shadow(if (esTurnoActual) 8.dp else 4.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(fondo)
                    .border(
                        width = if (esTurnoActual) 2.5.dp else 1.dp,
                        color = if (esTurnoActual) colorBordeTurno else colorBordeBase,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = jugador.nombre,
                    color = if (estaRetirado) Color.Gray else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (jugador.esHumano) 12.sp else 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$${jugador.pila} 🪙",
                    color = OroFichas,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (jugador.esHumano) 11.sp else 9.sp
                )

                when {
                    estaRetirado -> {
                        Text(
                            text = "NO IR",
                            color = RojoFold,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    estaAllIn -> {
                        Text(
                            text = "ALL-IN",
                            color = OroClaro,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    jugador.aporteEnCalle > 0 -> {
                        Text(
                            text = "Apuesta: $${jugador.aporteEnCalle}",
                            color = Color(0xFF81C784),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Ficha de Dealer 'D'
            if (esDealer) {
                Box(
                    modifier = Modifier
                        .offset(x = 5.dp, y = (-5).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
