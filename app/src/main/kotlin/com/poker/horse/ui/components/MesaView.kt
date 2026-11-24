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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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

        // Tapete Verde de la Mesa de Póker con reborde de madera
        Box(
            modifier = Modifier
                .fillMaxSize(if (esLandscape) 0.95f else 0.98f)
                .shadow(16.dp, RoundedCornerShape(percent = 45))
                .clip(RoundedCornerShape(percent = 45))
                .background(
                    Brush.radialGradient(
                        colors = listOf(BordeMadera, BordeMaderaOscuro)
                    )
                )
                .padding(10.dp)
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
                verticalArrangement = Arrangement.Center
            ) {
                // Etiqueta de la variante y calle
                Text(
                    text = "${variante.sigla} - $calleEtiqueta",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Bote total
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x99000000))
                        .border(1.dp, OroFichas, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "BOTE: $$bote 🪙",
                        color = OroFichas,
                        fontSize = 13.sp,
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
                                ancho = if (esLandscape) 42.dp else 38.dp,
                                alto = if (esLandscape) 58.dp else 52.dp
                            )
                        }
                    }
                }

                // Evaluación actual de la mano del usuario (si está disponible)
                if (evaluacionHumano != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = evaluacionHumano,
                        color = OroClaro,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Rivales distribuidos
            if (esLandscape) {
                // Modo Horizontal: Rivales en curva superior, izquierda y derecha
                DistribucionLandscape(
                    rivales = rivales,
                    dealerAsiento = dealerAsiento,
                    turnoActual = turnoActual,
                    mostrarCartasRival = mostrarCartasRival
                )
            } else {
                // Modo Vertical: Rivales en fila/arco superior
                DistribucionPortrait(
                    rivales = rivales,
                    dealerAsiento = dealerAsiento,
                    turnoActual = turnoActual,
                    mostrarCartasRival = mostrarCartasRival
                )
            }

            // Asiento del jugador humano (Abajo al centro)
            if (humano != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                ) {
                    JugadorSeatView(
                        jugador = humano,
                        esTurnoActual = (turnoActual == humano.asiento),
                        esDealer = (dealerAsiento == humano.asiento),
                        mostrarCartasPrivadas = true
                    )
                }
            }
        }
    }
}

@Composable
private fun DistribucionPortrait(
    rivales: List<Jugador>,
    dealerAsiento: Int?,
    turnoActual: Int?,
    mostrarCartasRival: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Rivales superiores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 10.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
        ) {
            rivales.forEach { rival ->
                JugadorSeatView(
                    jugador = rival,
                    esTurnoActual = (turnoActual == rival.asiento),
                    esDealer = (dealerAsiento == rival.asiento),
                    mostrarCartasPrivadas = mostrarCartasRival
                )
            }
        }
    }
}

@Composable
private fun DistribucionLandscape(
    rivales: List<Jugador>,
    dealerAsiento: Int?,
    turnoActual: Int?,
    mostrarCartasRival: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (rivales.isNotEmpty()) {
            // Rivales en arco superior e izquierdo/derecho
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                rivales.forEach { rival ->
                    JugadorSeatView(
                        jugador = rival,
                        esTurnoActual = (turnoActual == rival.asiento),
                        esDealer = (dealerAsiento == rival.asiento),
                        mostrarCartasPrivadas = mostrarCartasRival
                    )
                }
            }
        }
    }
}
