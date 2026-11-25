package com.poker.horse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.AzulCheck
import com.poker.horse.ui.theme.MoradoAllIn
import com.poker.horse.ui.theme.NaranjaBet
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.RojoFold
import com.poker.horse.ui.theme.SuperficieOscura
import com.poker.horse.ui.theme.VerdeCall
import com.poker.horse.ui.theme.LineaSuperficie
import com.poker.horse.ui.theme.MarfilCarta
import com.poker.horse.ui.theme.SuperficieVariante
import com.poker.horse.ui.theme.TextoSecundario
import poker.modelo.Accion
import poker.modelo.Jugador

@Composable
fun PanelAccionesView(
    accionesLegales: Set<Accion>,
    jugadorHumano: Jugador?,
    esTurnoHumano: Boolean,
    apuestaMasAlta: Int,
    boteTotal: Int,
    onEjecutarAccion: (Accion) -> Unit,
    modifier: Modifier = Modifier,
    esLandscape: Boolean = false
) {
    if (!esTurnoHumano || jugadorHumano == null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = SuperficieOscura.copy(alpha = 0.95f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LineaSuperficie)
        ) {
            BoxLoadingTurno()
        }
        return
    }

    val pilaHumano = jugadorHumano.pila
    val aporteHumano = jugadorHumano.aporteEnCalle
    val aIgualar = maxOf(0, apuestaMasAlta - aporteHumano)

    val tieneBet = accionesLegales.any { it is Accion.Bet }
    val tieneRaise = accionesLegales.any { it is Accion.Raise }

    var montoSeleccionado by remember(apuestaMasAlta, pilaHumano) {
        val baseMinima = when {
            tieneBet -> accionesLegales.filterIsInstance<Accion.Bet>().firstOrNull()?.monto ?: 20
            tieneRaise -> accionesLegales.filterIsInstance<Accion.Raise>().firstOrNull()?.monto ?: (aIgualar + 20)
            else -> 20
        }
        mutableIntStateOf(minOf(baseMinima, pilaHumano))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SuperficieOscura,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LineaSuperficie),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(if (esLandscape) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tu turno",
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                color = MarfilCarta,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            if ((tieneBet || tieneRaise) && pilaHumano > aIgualar) {
                val minMonto = if (tieneBet) {
                    accionesLegales.filterIsInstance<Accion.Bet>().first().monto
                } else {
                    accionesLegales.filterIsInstance<Accion.Raise>().first().monto
                }
                val maxMonto = pilaHumano

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Monto",
                        color = OroFichas,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        BotonRapido("Mín.") { montoSeleccionado = minMonto }
                        BotonRapido("1/2 Bote") {
                            montoSeleccionado = maxOf(minMonto, minOf(maxMonto, boteTotal / 2))
                        }
                        BotonRapido("Bote") {
                            montoSeleccionado = maxOf(minMonto, minOf(maxMonto, boteTotal))
                        }
                        BotonRapido("Máx.") { montoSeleccionado = maxMonto }
                    }
                }

                Slider(
                    value = montoSeleccionado.toFloat(),
                    onValueChange = { montoSeleccionado = it.toInt() },
                    valueRange = minMonto.toFloat()..maxMonto.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = OroFichas,
                        activeTrackColor = NaranjaBet
                    ),
                    modifier = Modifier.height(32.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Fila de botones principales filtrados por acciones legales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // FOLD
                val accionFold = accionesLegales.firstOrNull { it is Accion.Fold }
                if (accionFold != null) {
                    BotonAccion(
                        texto = "Retirarse",
                        color = RojoFold,
                        modifier = Modifier.weight(1f)
                    ) {
                        onEjecutarAccion(accionFold)
                    }
                }

                // CHECK
                val accionCheck = accionesLegales.firstOrNull { it is Accion.Check }
                if (accionCheck != null) {
                    BotonAccion(
                        texto = "Pasar",
                        color = AzulCheck,
                        modifier = Modifier.weight(1f)
                    ) {
                        onEjecutarAccion(accionCheck)
                    }
                }

                // CALL
                val accionCall = accionesLegales.firstOrNull { it is Accion.Call }
                if (accionCall != null) {
                    BotonAccion(
                        texto = "Igualar\n$${(accionCall as Accion.Call).monto}",
                        color = VerdeCall,
                        modifier = Modifier.weight(1f)
                    ) {
                        onEjecutarAccion(accionCall)
                    }
                }

                // BET / RAISE
                if (tieneBet) {
                    BotonAccion(
                        texto = "Apostar\n$$montoSeleccionado",
                        color = NaranjaBet,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        onEjecutarAccion(Accion.Bet(montoSeleccionado))
                    }
                } else if (tieneRaise) {
                    BotonAccion(
                        texto = "Subir\n$$montoSeleccionado",
                        color = NaranjaBet,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        onEjecutarAccion(Accion.Raise(montoSeleccionado))
                    }
                }

                // All-in
                val accionAllIn = accionesLegales.firstOrNull { it is Accion.AllIn }
                if (accionAllIn != null) {
                    BotonAccion(
                        texto = "All-in\n$${pilaHumano}",
                        color = MoradoAllIn,
                        modifier = Modifier.weight(1f)
                    ) {
                        onEjecutarAccion(accionAllIn)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxLoadingTurno() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Esperando a los rivales",
            color = TextoSecundario,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BotonAccion(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(
            text = texto,
            color = if (color == NaranjaBet) Color.Black else MarfilCarta,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

@Composable
private fun BotonRapido(
    texto: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = SuperficieVariante,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = texto,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )
    }
}
