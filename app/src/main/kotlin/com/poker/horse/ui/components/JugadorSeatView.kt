package com.poker.horse.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.LineaSuperficie
import com.poker.horse.ui.theme.MarfilCarta
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.SuperficieOscura
import com.poker.horse.ui.theme.SuperficieVariante
import com.poker.horse.ui.theme.TextoSecundario
import poker.modelo.EstadoJugador
import poker.modelo.Jugador

@Composable
fun JugadorSeatView(
    jugador: Jugador,
    esTurnoActual: Boolean,
    esDealer: Boolean,
    mostrarCartasPrivadas: Boolean = false,
    esCompacto: Boolean = false,
    modifier: Modifier = Modifier,
    detalle: String? = null
) {
    val retirado = jugador.estado == EstadoJugador.Retirado
    val estado = when {
        retirado -> "Retirado"
        jugador.estado == EstadoJugador.AllIn -> "All-in"
        esTurnoActual -> if (jugador.esHumano) "Tu turno" else "En turno"
        else -> null
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val cantidad = jugador.cartas.size.coerceAtLeast(1)
        val anchoPreferido = when {
            esCompacto -> if (jugador.esHumano) 32.dp else 24.dp
            jugador.esHumano -> 39.dp
            else -> 29.dp
        }
        val usaFilasParaBot = !jugador.esHumano && cantidad > 4
        val separacion = if (cantidad > 2) 0.dp else 2.dp
        val anchoCarta = if (usaFilasParaBot) {
            minOf(if (esCompacto) 25.dp else 29.dp, (maxWidth - 12.dp) / 4)
        } else minOf(
            anchoPreferido,
            (maxWidth - separacion * (cantidad - 1)).coerceAtLeast(0.dp) /
                (1f + (cantidad - 1) * if (cantidad > 2) 0.55f else 1f)
        )
        val espacioEntreCartas = if (cantidad > 2) -anchoCarta * 0.45f else separacion
        val datos = listOfNotNull(
            estado,
            jugador.aporteEnCalle.takeIf { it > 0 }?.let { "Apuesta ${'$'}$it" }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (jugador.cartas.isNotEmpty()) {
                Column(
                    modifier = Modifier.alpha(if (retirado) 0.55f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val indicesPorFila = if (usaFilasParaBot) {
                        listOf(jugador.cartas.indices.take(4), jugador.cartas.indices.drop(4))
                    } else {
                        listOf(jugador.cartas.indices.toList())
                    }
                    indicesPorFila.filter { it.isNotEmpty() }.forEach { indices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            indices.forEach { index ->
                                CartaView(
                                    carta = jugador.cartas[index],
                                    visible = mostrarCartasPrivadas || jugador.esHumano ||
                                        jugador.cartasVisibles.getOrElse(index) { false },
                                    ancho = anchoCarta,
                                    alto = anchoCarta * 1.34f
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (esTurnoActual) SuperficieVariante else SuperficieOscura,
                border = BorderStroke(1.dp, if (esTurnoActual) OroFichas else LineaSuperficie)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (esDealer) "${jugador.nombre} · D" else jugador.nombre,
                        color = if (retirado) TextoSecundario else MarfilCarta,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = if (jugador.esHumano) 13.sp else 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${'$'}${jugador.pila}",
                        color = OroFichas,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (jugador.esHumano) 14.sp else 12.sp
                    )
                    if (datos.isNotEmpty()) {
                        Text(
                            datos.joinToString(" · "),
                            color = if (esTurnoActual) OroFichas else TextoSecundario,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (detalle != null) {
                Text(detalle, color = MarfilCarta, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
