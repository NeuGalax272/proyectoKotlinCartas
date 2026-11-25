package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.SuperficieOscura
import poker.modelo.Variante

@Composable
fun DialogoConfiguracion(
    varianteActual: Variante,
    semillaActual: Long?,
    onConfirmar: (nuevaVariante: Variante, nuevaSemilla: Long?, reiniciarFichas: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var varianteSeleccionada by remember { mutableStateOf(varianteActual) }
    var textoSemilla by remember { mutableStateOf(semillaActual?.toString() ?: "") }
    var reiniciarFichas by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SuperficieOscura,
        title = {
            Text(
                text = "Configurar partida",
                color = OroFichas,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Variante",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Variante.entries.forEach { v ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (v == varianteSeleccionada),
                            onClick = { varianteSeleccionada = v },
                            colors = RadioButtonDefaults.colors(selectedColor = OroFichas)
                        )
                        Text(
                            text = "${v.sigla} - ${v.nombreCompleto}",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Semilla del mazo",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Usa la misma semilla para repetir el reparto.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = textoSemilla,
                    onValueChange = { textoSemilla = it.filter { char -> char.isDigit() } },
                    label = { Text("Número de semilla") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { reiniciarFichas = !reiniciarFichas },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (reiniciarFichas) OroFichas else Color.LightGray
                    )
                ) {
                    Text(if (reiniciarFichas) "✓ Reiniciar fichas a $1000" else "Reiniciar fichas a $1000")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val semilla = textoSemilla.toLongOrNull()
                    onConfirmar(varianteSeleccionada, semilla, reiniciarFichas)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OroFichas)
            ) {
                Text("Aplicar", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}

@Composable
fun DialogoReglas(
    variante: Variante,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SuperficieOscura,
        title = {
            Text(
                text = "${variante.nombreCompleto}",
                color = OroFichas,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = variante.descripcion,
                    color = Color.White,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                val detalle = when (variante) {
                    Variante.HOLDEM ->
                        "• Se reparten 2 cartas privadas a cada jugador.\n• 5 cartas comunitarias (Flop 3, Turn 1, River 1).\n• Gana la mejor mano de 5 cartas combinando cualquier combinación de cartas propias y comunitarias."
                    Variante.OMAHA ->
                        "• Se reparten 4 cartas privadas.\n• 5 comunitarias (Flop 3, Turn 1, River 1).\n• REGLA ESTRICTA: Debes usar EXACTAMENTE 2 de tus 4 cartas en mano y 3 de las 5 comunitarias."
                    Variante.RAZZ ->
                        "• Variante Lowball A-5 de Seven Card Stud.\n• 7 cartas: 3ra calle (2 abajo, 1 arriba), 4ta, 5ta, 6ta (arriba), 7ma (abajo).\n• No hay comunitarias.\n• As cuenta como 1 (la carta más baja).\n• Las escaleras y colores no perjudican la mano baja.\n• La mejor mano posible es 5-4-3-2-A."
                    Variante.SEVEN_CARD_STUD ->
                        "• 7 cartas personales repartidas a lo largo de 5 calles.\n• 3ra calle (2 abajo, 1 arriba), 4ta, 5ta, 6ta (arriba), 7ma (abajo).\n• Sin comunitarias.\n• Gana la mejor mano alta de 5 cartas."
                    Variante.SEVEN_CARD_HI_LO ->
                        "• Reparto idéntico a Stud (7 cartas).\n• Split Pot: El bote se divide 50% para la mejor mano alta y 50% para la mejor mano baja calificada (8 o menor sin pares).\n• Si nadie califica para baja, la mano alta se lleva el 100% (Scoop)."
                }

                Text(
                    text = detalle,
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = OroFichas)
            ) {
                Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}
