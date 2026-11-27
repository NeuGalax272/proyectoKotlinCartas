package com.poker.horse

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.poker.horse.ui.components.DialogoConfiguracion
import com.poker.horse.ui.components.DialogoReglas
import com.poker.horse.ui.components.HorseHeaderBar
import com.poker.horse.ui.components.MesaView
import com.poker.horse.ui.components.PantallaInicio
import com.poker.horse.ui.components.PanelAccionesView
import com.poker.horse.ui.components.ShowdownOverlay
import com.poker.horse.ui.theme.FondoOscuro
import com.poker.horse.ui.theme.PokerHorseTheme
import com.poker.horse.ui.viewmodel.MesaUiState
import com.poker.horse.ui.viewmodel.MesaViewModel
import com.poker.horse.ui.viewmodel.InicioViewModel
import com.poker.horse.ui.viewmodel.PantallaInicio as DestinoInicio
import poker.modelo.Jugador

class MainActivity : ComponentActivity() {

    private val inicioViewModel: InicioViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val colorBarras = FondoOscuro.toArgb()
        window.statusBarColor = colorBarras
        window.navigationBarColor = colorBarras
        window.decorView.setBackgroundColor(colorBarras)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = colorBarras
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        setContent {
            PokerHorseTheme {
                val inicio by inicioViewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(inicio.pantalla) {
                    val controlador = WindowCompat.getInsetsController(window, window.decorView)
                    if (inicio.pantalla == DestinoInicio.PARTIDA) {
                        controlador.systemBarsBehavior =
                            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        controlador.hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        controlador.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
                if (inicio.pantalla == DestinoInicio.PARTIDA) {
                    val claveMesa = "${inicio.pin}:${inicio.idJugadorLocal}"
                    val fabrica = remember(claveMesa, inicio.modoSeleccionado, inicio.esAnfitrion) {
                        object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                @Suppress("UNCHECKED_CAST")
                                return crearMesaViewModel(inicio) as T
                            }
                        }
                    }
                    val mesaViewModel: MesaViewModel = viewModel(key = claveMesa, factory = fabrica)
                    val mesa by mesaViewModel.uiState.collectAsStateWithLifecycle()
                    var estadoRemotoAplicado by remember(claveMesa) { mutableStateOf(false) }

                    LaunchedEffect(inicio.partidaRemota) {
                        inicio.partidaRemota?.let {
                            mesaViewModel.aplicarEstadoRemoto(it)
                            estadoRemotoAplicado = true
                        }
                    }
                    LaunchedEffect(inicio.esAnfitrion, claveMesa) {
                        inicioViewModel.accionesRemotas.collect { peticion ->
                            mesaViewModel.ejecutarAccionJugador(peticion.jugadorId, peticion.accion)
                        }
                    }
                    LaunchedEffect(inicio.esAnfitrion, claveMesa) {
                        inicioViewModel.jugadoresDesconectados.collect(mesaViewModel::desconectarJugador)
                    }
                    LaunchedEffect(mesa, inicio.esAnfitrion) {
                        if (inicio.esAnfitrion) inicioViewModel.publicarEstado(mesaViewModel.crearEstadoRemoto())
                    }

                    if (!inicio.esAnfitrion && !estadoRemotoAplicado) {
                        Column(
                            modifier = Modifier.fillMaxSize().background(FondoOscuro),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = com.poker.horse.ui.theme.OroFichas)
                            Spacer(Modifier.height(12.dp))
                            Text("Esperando el estado inicial del anfitrión…", color = Color.White)
                        }
                    } else {
                        PantallaPrincipal(
                            state = mesa,
                            esAnfitrion = inicio.esAnfitrion,
                            onEjecutarAccion = { accion ->
                                if (inicio.esAnfitrion) mesaViewModel.ejecutarAccionHumano(accion)
                                else inicioViewModel.enviarAccion(accion)
                            },
                            onSiguienteMano = mesaViewModel::siguienteMano,
                            onAbrirConfiguracion = { mesaViewModel.abrirDialogoConfiguracion(true) },
                            onCerrarConfiguracion = { mesaViewModel.abrirDialogoConfiguracion(false) },
                            onConfirmarConfiguracion = mesaViewModel::configurarPartida,
                            onAbandonarPartida = inicioViewModel::volverAlMenu,
                            onAbrirReglas = { mesaViewModel.abrirDialogoReglas(true) },
                            onCerrarReglas = { mesaViewModel.abrirDialogoReglas(false) }
                        )
                    }
                } else {
                    PantallaInicio(
                        state = inicio,
                        onRegistrarNombre = inicioViewModel::registrarNombre,
                        onSeleccionarModo = inicioViewModel::seleccionarModo,
                        onIniciarSolo = inicioViewModel::iniciarSolo,
                        onCrearSala = inicioViewModel::crearSala,
                        onAbrirIngreso = inicioViewModel::abrirIngreso,
                        onConectar = inicioViewModel::conectar,
                        onIniciarSala = inicioViewModel::iniciarSala,
                        onVolverMenu = inicioViewModel::volverAlMenu
                    )
                }
            }
        }
    }
}

private fun crearMesaViewModel(inicio: com.poker.horse.ui.viewmodel.InicioUiState): MesaViewModel {
    val nombresBots = listOf("Bot Sofía", "Bot Carlos", "Bot Mateo")
    val humanos = inicio.jugadoresSala.take(4).mapIndexed { asiento, jugador ->
        Jugador(asiento = asiento, nombre = jugador.nombre, pila = 1000, esHumano = jugador.id == inicio.idJugadorLocal)
    }
    val jugadores = (humanos + nombresBots.take((4 - humanos.size).coerceAtLeast(0)).mapIndexed { indice, nombre ->
        Jugador(asiento = humanos.size + indice, nombre = nombre, pila = 1000)
    }).mapIndexed { asiento, jugador -> jugador.copy(asiento = asiento) }
    val ids = inicio.jugadoresSala.take(4).mapIndexed { asiento, jugador -> asiento to jugador.id }.toMap()
    return MesaViewModel(
        modoPartida = inicio.modoSeleccionado,
        jugadoresDeInicio = jugadores,
        idsPorAsiento = ids,
        idJugadorLocal = inicio.idJugadorLocal,
        esAnfitrion = inicio.esAnfitrion
    )
}

@Composable
fun PantallaPrincipal(
    state: MesaUiState,
    esAnfitrion: Boolean = true,
    onEjecutarAccion: (poker.modelo.Accion) -> Unit,
    onSiguienteMano: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    onCerrarConfiguracion: () -> Unit,
    onConfirmarConfiguracion: (poker.modelo.Variante, Long?, Boolean) -> Unit,
    onAbandonarPartida: () -> Unit,
    onAbrirReglas: () -> Unit,
    onCerrarReglas: () -> Unit
) {
    val mano = state.estadoMano
    val humano = mano.jugadores.firstOrNull { it.esHumano }

    val configuration = LocalConfiguration.current
    val esLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            HorseHeaderBar(
                varianteActual = mano.variante,
                manoEnVariante = state.manoEnVariante,
                manosPorVariante = state.manosPorVariante,
                onAbrirConfiguracion = onAbrirConfiguracion,
                onAbrirReglas = onAbrirReglas,
                mostrarConfiguracion = esAnfitrion
            )
        },
        containerColor = FondoOscuro,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FondoOscuro)
        ) {
            if (esLandscape) {
                // Layout Horizontal (Landscape): Mesa a la izquierda, Panel de acciones a la derecha
                Row(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
                    // Mesa de póker ocupando el espacio principal
                    Box(
                        modifier = Modifier
                            .weight(0.64f)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MesaView(
                            jugadores = mano.jugadores,
                            comunitarias = mano.comunitarias,
                            bote = mano.bote,
                            calleEtiqueta = mano.calle.etiqueta,
                            dealerIndex = mano.dealerIndex,
                            turnoActual = mano.turnoActual,
                            mostrarCartasRival = mano.manoTerminada,
                            evaluacionHumano = state.evaluacionHumanoTexto
                        )
                    }

                    // Panel lateral de acciones
                    Box(
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PanelAccionesView(
                            accionesLegales = state.accionesLegales,
                            jugadorHumano = humano,
                            esTurnoHumano = state.esTurnoHumano,
                            apuestaMasAlta = mano.apuestaMasAlta,
                            boteTotal = mano.bote,
                            onEjecutarAccion = onEjecutarAccion,
                            esLandscape = true,
                            jugadorEnTurno = mano.jugadorEnTurno,
                            calleEtiqueta = mano.calle.etiqueta,
                            historialAcciones = mano.historialAcciones
                        )
                    }
                }
            } else {
                // Layout Vertical (Portrait): Mesa en el centro, Panel de acciones abajo
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MesaView(
                            jugadores = mano.jugadores,
                            comunitarias = mano.comunitarias,
                            bote = mano.bote,
                            calleEtiqueta = mano.calle.etiqueta,
                            dealerIndex = mano.dealerIndex,
                            turnoActual = mano.turnoActual,
                            mostrarCartasRival = mano.manoTerminada,
                            evaluacionHumano = state.evaluacionHumanoTexto
                        )
                    }

                    PanelAccionesView(
                        accionesLegales = state.accionesLegales,
                        jugadorHumano = humano,
                        esTurnoHumano = state.esTurnoHumano,
                        apuestaMasAlta = mano.apuestaMasAlta,
                        boteTotal = mano.bote,
                        onEjecutarAccion = onEjecutarAccion,
                        esLandscape = false,
                        jugadorEnTurno = mano.jugadorEnTurno,
                        calleEtiqueta = mano.calle.etiqueta,
                        historialAcciones = mano.historialAcciones
                    )
                }
            }

            // Superposición de fin de mano (Showdown / Ganadores)
            if (mano.manoTerminada) {
                ShowdownOverlay(
                    ganadores = mano.ganadores,
                    onSiguienteMano = onSiguienteMano,
                    esAnfitrion = esAnfitrion,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Diálogo de configuración
            if (state.mostrarDialogoConfiguracion) {
                DialogoConfiguracion(
                    varianteActual = mano.variante,
                    semillaActual = state.semillaConfigurada,
                    onConfirmar = onConfirmarConfiguracion,
                    onAbandonarPartida = onAbandonarPartida,
                    onDismiss = onCerrarConfiguracion
                )
            }

            // Diálogo de reglas
            if (state.mostrarDialogoReglas) {
                DialogoReglas(
                    variante = mano.variante,
                    onDismiss = onCerrarReglas
                )
            }
        }
    }
}
