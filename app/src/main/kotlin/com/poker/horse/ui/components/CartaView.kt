package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.BordeMesa
import com.poker.horse.ui.theme.MarfilCarta
import com.poker.horse.ui.theme.NegroCarta
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.RojoCarta
import com.poker.horse.ui.theme.VerdeMesaClaro
import com.poker.horse.ui.theme.VerdeMesaOscuro
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
    val frente = carta?.takeIf { visible }
    val forma = RoundedCornerShape(5.dp)
    Surface(
        modifier = modifier
            .size(ancho, alto)
            .clearAndSetSemantics {
                contentDescription = frente?.let { "${it.rango.etiqueta} de ${it.palo.name.lowercase()}" }
                    ?: "Carta oculta"
            },
        shape = forma,
        color = if (frente != null) MarfilCarta else VerdeMesaOscuro,
        shadowElevation = 3.dp
    ) {
        if (frente != null) {
            val tinta = when (frente.palo) {
                Palo.CORAZONES, Palo.DIAMANTES -> RojoCarta
                Palo.TREBOLES, Palo.PICAS -> NegroCarta
            }

            Box(Modifier.fillMaxSize().padding(start = 2.dp, top = 1.dp)) {
                Text(
                    frente.rango.etiqueta,
                    color = tinta,
                    fontSize = (ancho.value * 0.37f).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    frente.palo.simbolo,
                    modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 1.dp),
                    color = tinta,
                    fontSize = (ancho.value * 0.42f).sp,
                    maxLines = 1
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(VerdeMesaClaro, VerdeMesaOscuro)))
                    .border(1.dp, BordeMesa, forma)
                    .padding(3.dp)
                    .border(1.dp, OroFichas.copy(alpha = 0.42f), RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("♦", color = OroFichas, fontSize = (ancho.value * 0.35f).sp)
            }
        }
    }
}
