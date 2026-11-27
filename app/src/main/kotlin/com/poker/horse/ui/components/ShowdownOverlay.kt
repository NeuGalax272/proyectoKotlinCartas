package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.OroClaro
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.SuperficieOscura
import com.poker.horse.ui.theme.VerdeCall
import poker.motor.GanadorInfo

@Composable
fun ShowdownOverlay(
    ganadores: List<GanadorInfo>,
    onSiguienteMano: () -> Unit,
    esAnfitrion: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SuperficieOscura),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .border(1.dp, OroClaro, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF20362D), SuperficieOscura)
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Resultado de la mano",
                    color = OroFichas,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                ganadores.forEach { g ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0x33000000), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${g.jugador.nombre} gana +${'$'}${g.montoGanado}",
                            color = if (g.jugador.esHumano) Color(0xFF81C784) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = g.descripcionMano,
                            color = OroClaro,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (esAnfitrion) {
                    Button(
                        onClick = onSiguienteMano,
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeCall),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(0.8f).height(48.dp)
                    ) {
                        Text(
                            text = "Siguiente mano",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    Text("Esperando al anfitrión para continuar…", color = OroClaro, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
