package com.poker.horse.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.caverock.androidsvg.SVG
import com.poker.horse.ui.theme.BordeMesa
import com.poker.horse.ui.theme.OroFichas
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
        color = VerdeMesaOscuro,
        shadowElevation = 3.dp
    ) {
        if (frente != null) {
            val context = LocalContext.current
            val bitmap = remember(frente) {
                val archivo = "cartas/${frente.rango.etiqueta}_${frente.palo.nombreArchivo()}.svg"
                val svg = SVG.getFromAsset(context.assets, archivo)
                Bitmap.createBitmap(200, 280, Bitmap.Config.ARGB_8888).also { imagen ->
                    svg.renderToCanvas(Canvas(imagen))
                }.asImageBitmap()
            }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
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

private fun Palo.nombreArchivo(): String = when (this) {
    Palo.CORAZONES -> "corazones"
    Palo.DIAMANTES -> "diamantes"
    Palo.TREBOLES -> "treboles"
    Palo.PICAS -> "picas"
}
