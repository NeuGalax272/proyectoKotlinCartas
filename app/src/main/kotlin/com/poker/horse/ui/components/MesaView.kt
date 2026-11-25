package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.BordeMesa
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.TextoSecundario
import com.poker.horse.ui.theme.VerdeMesa
import com.poker.horse.ui.theme.VerdeMesaClaro
import com.poker.horse.ui.theme.VerdeMesaOscuro
import poker.modelo.Carta
import poker.modelo.Jugador

@Composable
fun MesaView(
    jugadores: List<Jugador>,
    comunitarias: List<Carta>,
    bote: Int,
    calleEtiqueta: String,
    dealerIndex: Int,
    turnoActual: Int?,
    mostrarCartasRival: Boolean = false,
    evaluacionHumano: String? = null,
    modifier: Modifier = Modifier
) {
    val humano = jugadores.firstOrNull { it.esHumano }
    val rivales = jugadores.filterNot { it.esHumano }
    val dealerAsiento = jugadores.getOrNull(dealerIndex)?.asiento
    val marco = RoundedCornerShape(30.dp)
    val tapete = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(VerdeMesaOscuro, marco)
            .border(1.dp, BordeMesa, marco)
            .padding(5.dp)
            .background(
                Brush.radialGradient(listOf(VerdeMesaClaro, VerdeMesa, VerdeMesaOscuro)),
                tapete
            )
            .border(1.dp, BordeMesa.copy(alpha = 0.6f), tapete)
    ) {
        val compacta = maxHeight < 310.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = if (compacta) 6.dp else 14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (rivales.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    rivales.forEach { rival ->
                        JugadorSeatView(
                            jugador = rival,
                            esTurnoActual = rival.asiento == turnoActual,
                            esDealer = rival.asiento == dealerAsiento,
                            mostrarCartasPrivadas = mostrarCartasRival,
                            esCompacto = compacta,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(vertical = if (compacta) 4.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(calleEtiqueta, color = TextoSecundario, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .background(VerdeMesaOscuro, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "Bote ${'$'}$bote",
                            color = OroFichas,
                            fontSize = if (compacta) 15.sp else 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (comunitarias.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        comunitarias.forEach { carta ->
                            CartaView(
                                carta = carta,
                                ancho = if (compacta) 30.dp else 39.dp,
                                alto = if (compacta) 40.dp else 54.dp
                            )
                        }
                    }
                }
            }

            if (humano != null) {
                JugadorSeatView(
                    jugador = humano,
                    esTurnoActual = humano.asiento == turnoActual,
                    esDealer = humano.asiento == dealerAsiento,
                    mostrarCartasPrivadas = true,
                    esCompacto = compacta,
                    detalle = evaluacionHumano,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
