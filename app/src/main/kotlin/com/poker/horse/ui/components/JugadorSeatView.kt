package com.poker.horse.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.MoradoAllIn
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
    modifier: Modifier = Modifier
) {
    val estaRetirado = jugador.estado == EstadoJugador.Retirado
    val estaAllIn = jugador.estado == EstadoJugador.AllIn

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fila de cartas del jugador
        Row(
            horizontalArrangement = Arrangement.spacedBy((-16).dp),
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
                    ancho = if (jugador.esHumano) 48.dp else 36.dp,
                    alto = if (jugador.esHumano) 68.dp else 50.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Caja de información del jugador (Nombre, Fichas, Estado)
        Box(contentAlignment = Alignment.TopEnd) {
            val fondo = when {
                estaRetirado -> Color(0x99333333)
                estaAllIn -> Color(0xDD4A148C)
                jugador.esHumano -> Color(0xDD1B5E20)
                else -> Color(0xDD212121)
            }

            val bordeColor = when {
                esTurnoActual -> OroClaro
                jugador.esHumano -> Color(0xFF66BB6A)
                else -> Color(0xFF555555)
            }

            val bordeAncho = if (esTurnoActual) 2.5.dp else 1.dp

            Column(
                modifier = Modifier
                    .width(if (jugador.esHumano) 130.dp else 105.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(fondo)
                    .border(bordeAncho, bordeColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = jugador.nombre,
                    color = if (estaRetirado) Color.LightGray else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (jugador.esHumano) 13.sp else 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "$${jugador.pila} 🪙",
                    color = OroFichas,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (jugador.esHumano) 12.sp else 10.sp
                )

                // Etiqueta de estado
                when {
                    estaRetirado -> {
                        Text(
                            text = "NO VA",
                            color = RojoFold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    estaAllIn -> {
                        Text(
                            text = "¡ALL-IN!",
                            color = OroClaro,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    jugador.aporteEnCalle > 0 -> {
                        Text(
                            text = "Apuesta: $${jugador.aporteEnCalle}",
                            color = Color(0xFF81C784),
                            fontSize = 9.sp
                        )
                    }
                }
            }

            // Botón de Dealer 'D'
            if (esDealer) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(1.dp, Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "D",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
