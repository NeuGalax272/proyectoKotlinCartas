package com.poker.horse.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.BordeMadera
import com.poker.horse.ui.theme.BordeMaderaOscuro
import com.poker.horse.ui.theme.OroClaro
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.VerdeMesa
import com.poker.horse.ui.theme.VerdeMesaClaro
import com.poker.horse.ui.theme.VerdeMesaOscuro
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

@Composable
fun MesaView(
    jugadores: List<Jugador>,
    comunitarias: List<Carta>,
    bote: Int,
    calleEtiqueta: String,
    variante: Variante,
    dealerIndex: Int,
    turnoActual: Int?,
    mostrarCartasRival: Boolean = false,
    evaluacionHumano: String? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val esLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val humano = jugadores.firstOrNull { it.esHumano }
    val rivales = jugadores.filter { !it.esHumano }
    val dealerAsiento = jugadores.getOrNull(dealerIndex)?.asiento

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxAncho = maxWidth
        val maxAlto = maxHeight

        // Tapete Verde de la Mesa de Póker Ovalada con reborde de madera
        Box(
            modifier = Modifier
                .fillMaxSize(if (esLandscape) 0.96f else 0.98f)
                .shadow(16.dp, RoundedCornerShape(percent = 45))
                .clip(RoundedCornerShape(percent = 45))
                .background(
                    Brush.radialGradient(
                        colors = listOf(BordeMadera, BordeMaderaOscuro)
                    )
                )
                .padding(8.dp)
                .clip(RoundedCornerShape(percent = 42))
                .background(
                    Brush.radialGradient(
                        colors = listOf(VerdeMesaClaro, VerdeMesa, VerdeMesaOscuro)
                    )
                )
                .border(2.dp, Color(0x33FFFFFF), RoundedCornerShape(percent = 42)),
            contentAlignment = Alignment.Center
        ) {
            // Centro de la mesa: Bote, Cartas Comunitarias, Calle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                // Etiqueta de la variante y calle
                Text(
                    text = "${variante.sigla} • $calleEtiqueta",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = if (esLandscape) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Bote total
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xBB000000))
                        .border(1.5.dp, OroFichas, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "BOTE: $$bote 🪙",
                        color = OroFichas,
                        fontSize = if (esLandscape) 12.sp else 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Cartas comunitarias (Hold'em / Omaha)
                if (comunitarias.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        comunitarias.forEach { carta ->
                            CartaView(
                                carta = carta,
                                visible = true,
                                ancho = if (esLandscape) 38.dp else 36.dp,
                                alto = if (esLandscape) 54.dp else 50.dp
                            )
                        }
                    }
                }

                // Evaluación de mano del humano
                if (evaluacionHumano != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x88000000))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = evaluacionHumano,
                            color = OroClaro,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Distribución responsiva de rivales en la mesa
            DistribucionRivales(
                rivales = rivales,
                dealerAsiento = dealerAsiento,
                turnoActual = turnoActual,
                mostrarCartasRival = mostrarCartasRival,
                esLandscape = esLandscape
            )

            // Asiento del jugador humano (Parte inferior)
            if (humano != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                ) {
                    JugadorSeatView(
                        jugador = humano,
                        esTurnoActual = (turnoActual == humano.asiento),
                        esDealer = (dealerAsiento == humano.asiento),
                        mostrarCartasPrivadas = true,
                        esCompacto = esLandscape
                    )
                }
            }
        }
    }
}

@Composable
private fun DistribucionRivales(
    rivales: List<Jugador>,
    dealerAsiento: Int?,
    turnoActual: Int?,
    mostrarCartasRival: Boolean,
    esLandscape: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (rivales.size) {
            1 -> {
                // 1 rival: Centro superior
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                ) {
                    SeatRival(rivales[0], dealerAsiento, turnoActual, mostrarCartasRival, esLandscape)
                }
            }
            2 -> {
                // 2 rivales: Arriba izquierda y Arriba derecha
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 12.dp)
                ) {
                    SeatRival(rivales[0], dealerAsiento, turnoActual, mostrarCartasRival, esLandscape)
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                ) {
                    SeatRival(rivales[1], dealerAsiento, turnoActual, mostrarCartasRival, esLandscape)
                }
            }
            else -> {
                // 3 o más rivales: Izquierda, Centro Superior, Derecha
                val rivalIzquierda = rivales.getOrNull(0)
                val rivalCentro = rivales.getOrNull(1)
                val rivalDerecha = rivales.getOrNull(2)

                if (rivalIzquierda != null) {
                    Box(
                        modifier = Modifier
                            .align(if (esLandscape) Alignment.CenterStart else Alignment.TopStart)
                            .padding(start = 6.dp, top = if (esLandscape) 0.dp else 8.dp)
                    ) {
                        SeatRival(rivalIzquierda, dealerAsiento, turnoActual, mostrarCartasRival, esLandscape)
                    }
                }

                if (rivalCentro != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                    ) {
                        SeatRival(rivalCentro, dealerAsiento, turnoActual, mostrarCartasRival, esLandscape)
                    }
                }

                if (rivalDerecha != null) {
                    Box(
                        modifier = Modifier
                            .align(if (esLandscape) Alignment.CenterEnd else Alignment.TopEnd)
                            .padding(end = 6.dp, top = if (esLandscape) 0.dp else 8.dp)
                    ) {
                        SeatRival(rivalDerecha, dealerAsiento, turnoActual, mostrarCartasRival, esLandscape)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeatRival(
    rival: Jugador,
    dealerAsiento: Int?,
    turnoActual: Int?,
    mostrarCartasRival: Boolean,
    esCompacto: Boolean
) {
    JugadorSeatView(
        jugador = rival,
        esTurnoActual = (turnoActual == rival.asiento),
        esDealer = (dealerAsiento == rival.asiento),
        mostrarCartasPrivadas = mostrarCartasRival,
        esCompacto = esCompacto
    )
}
