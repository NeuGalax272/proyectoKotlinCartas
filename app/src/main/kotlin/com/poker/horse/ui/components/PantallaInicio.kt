package com.poker.horse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.horse.ui.theme.FondoOscuro
import com.poker.horse.ui.theme.OroFichas
import com.poker.horse.ui.theme.SuperficieOscura
import com.poker.horse.ui.theme.SuperficieVariante
import com.poker.horse.ui.theme.TextoSecundario
import com.poker.horse.ui.viewmodel.InicioUiState
import com.poker.horse.ui.viewmodel.JugadorSala
import com.poker.horse.ui.viewmodel.ModoPartida
import com.poker.horse.ui.viewmodel.PantallaInicio

@Composable
fun PantallaInicio(
    state: InicioUiState,
    onRegistrarNombre: (String) -> Unit,
    onSeleccionarModo: (ModoPartida) -> Unit,
    onIniciarSolo: () -> Unit,
    onCrearSala: () -> Unit,
    onAbrirIngreso: () -> Unit,
    onConectar: (String) -> Unit,
    onIniciarSala: () -> Unit,
    onVolverMenu: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(FondoOscuro).windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (state.pantalla == PantallaInicio.LOBBY) Modifier.fillMaxSize() else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("PÓKER HORSE", color = OroFichas, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(8.dp))
            when (state.pantalla) {
                PantallaInicio.REGISTRO -> RegistroNombre(onRegistrarNombre)
                PantallaInicio.MENU -> MenuPartida(
                    nombre = state.nombreUsuario,
                    modo = state.modoSeleccionado,
                    alElegirModo = onSeleccionarModo,
                    onIniciarSolo = onIniciarSolo,
                    onCrearSala = onCrearSala,
                    onAbrirIngreso = onAbrirIngreso
                )
                PantallaInicio.ESPERA_CODIGO -> IngresoSala(
                    conectando = state.conectando,
                    mensaje = state.mensaje,
                    onConectar = onConectar,
                    onVolverMenu = onVolverMenu
                )
                PantallaInicio.LOBBY -> SalaEspera(
                    state = state,
                    onIniciarSala = onIniciarSala,
                    onVolverMenu = onVolverMenu,
                    modifier = Modifier.weight(1f)
                )
                PantallaInicio.PARTIDA -> Unit
            }
        }
    }
}

@Composable
private fun RegistroNombre(onRegistrar: (String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    Spacer(Modifier.height(26.dp))
    Text("¿Cómo te llamas?", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text("Tu nombre aparecerá en la mesa y en las partidas que crees.", color = TextoSecundario, textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(
        value = nombre,
        onValueChange = { nombre = it.take(24) },
        label = { Text("Nombre de jugador") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onRegistrar(nombre.trim()) },
        enabled = nombre.trim().isNotEmpty(),
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = OroFichas)
    ) { Text("Guardar nombre", color = Color.Black, fontWeight = FontWeight.Bold) }
}

@Composable
private fun MenuPartida(
    nombre: String,
    modo: ModoPartida,
    alElegirModo: (ModoPartida) -> Unit,
    onIniciarSolo: () -> Unit,
    onCrearSala: () -> Unit,
    onAbrirIngreso: () -> Unit
) {
    Spacer(Modifier.height(10.dp))
    Text("Hola, $nombre", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Text("Elige la modalidad y cómo quieres jugar.", color = TextoSecundario)
    Spacer(Modifier.height(20.dp))
    SelectorModo(modo = modo, onSeleccionar = alElegirModo)
    Spacer(Modifier.height(18.dp))
    OpcionMenu("Iniciar partida de un solo jugador", "Juega contra bots.", onIniciarSolo)
    OpcionMenu("Crear partida multijugador", "Abre una sala en esta red local.", onCrearSala)
    OpcionMenu("Conectarse a una partida multijugador", "Ingresa el PIN de quien creó la sala.", onAbrirIngreso)
}

@Composable
private fun SelectorModo(modo: ModoPartida, onSeleccionar: (ModoPartida) -> Unit) {
    var desplegado by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = SuperficieOscura), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Modalidad", color = TextoSecundario, fontSize = 13.sp)
            Spacer(Modifier.height(5.dp))
            Box {
                OutlinedButton(onClick = { desplegado = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(modo.titulo, color = OroFichas, fontWeight = FontWeight.Bold)
                }
                DropdownMenu(expanded = desplegado, onDismissRequest = { desplegado = false }) {
                    ModoPartida.entries.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion.titulo) },
                            onClick = { onSeleccionar(opcion); desplegado = false }
                        )
                    }
                }
            }
            Text(
                if (modo == ModoPartida.HORSE) "Rota por las cinco variantes HORSE." else "Se juega esta variante en cada mano.",
                color = TextoSecundario,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun OpcionMenu(titulo: String, detalle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = SuperficieOscura),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(titulo, color = OroFichas, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(detalle, color = TextoSecundario, fontSize = 13.sp)
        }
    }
}

@Composable
private fun IngresoSala(
    conectando: Boolean,
    mensaje: String?,
    onConectar: (String) -> Unit,
    onVolverMenu: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    Spacer(Modifier.height(25.dp))
    Text("Conectarse a una sala", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Debes estar en la misma red local que quien creó la partida.", color = TextoSecundario, textAlign = TextAlign.Center)
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(
        value = pin,
        onValueChange = { pin = it.filter(Char::isDigit).take(6) },
        label = { Text("PIN de 6 dígitos") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    if (mensaje != null) {
        Spacer(Modifier.height(8.dp))
        Text(mensaje, color = OroFichas, textAlign = TextAlign.Center)
    }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { onConectar(pin) },
        enabled = pin.length == 6 && !conectando,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = OroFichas)
    ) { Text(if (conectando) "Buscando sala…" else "Buscar sala", color = Color.Black, fontWeight = FontWeight.Bold) }
    TextButton(onClick = onVolverMenu) { Text("Volver al menú", color = TextoSecundario) }
}

@Composable
private fun SalaEspera(
    state: InicioUiState,
    onIniciarSala: () -> Unit,
    onVolverMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Text(state.nombreSala, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Modalidad: ${state.modoSeleccionado.titulo}", color = TextoSecundario)
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(containerColor = SuperficieOscura), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PIN DE LA SALA", color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(state.pin, color = OroFichas, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Jugadores · ${state.jugadoresSala.size}/4", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    state.jugadoresSala.forEach { JugadorEnSala(it) }
                    repeat((4 - state.jugadoresSala.size).coerceAtLeast(0)) {
                        JugadorEnSala(null)
                    }
                }
            }
            if (state.mensaje != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.mensaje, color = OroFichas, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (state.esAnfitrion) {
            Button(
                onClick = onIniciarSala,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OroFichas)
            ) { Text("Iniciar partida", color = Color.Black, fontWeight = FontWeight.Bold) }
        } else {
            Text("Esperando a que ${state.nombreSala.removePrefix("Partida de ")} inicie la partida…", color = TextoSecundario, textAlign = TextAlign.Center)
        }
        TextButton(onClick = onVolverMenu) { Text("Salir de la sala", color = TextoSecundario) }
    }
}

@Composable
private fun JugadorEnSala(jugador: JugadorSala?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(if (jugador == null) SuperficieVariante.copy(alpha = 0.45f) else SuperficieVariante, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (jugador == null) "Asiento libre · Bot" else jugador.nombre, color = if (jugador == null) TextoSecundario else Color.White, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(5.dp))
        if (jugador?.anfitrion == true) Text("· Anfitrión", color = OroFichas, fontSize = 12.sp)
    }
}
