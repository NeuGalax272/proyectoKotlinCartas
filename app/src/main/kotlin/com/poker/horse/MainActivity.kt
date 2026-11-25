package com.poker.horse

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poker.horse.ui.components.DialogoConfiguracion
import com.poker.horse.ui.components.DialogoReglas
import com.poker.horse.ui.components.HorseHeaderBar
import com.poker.horse.ui.components.MesaView
import com.poker.horse.ui.components.PanelAccionesView
import com.poker.horse.ui.components.ShowdownOverlay
import com.poker.horse.ui.theme.FondoOscuro
import com.poker.horse.ui.theme.PokerHorseTheme
import com.poker.horse.ui.viewmodel.MesaUiState
import com.poker.horse.ui.viewmodel.MesaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MesaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PokerHorseTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                PantallaPrincipal(
                    state = state,
                    onEjecutarAccion = viewModel::ejecutarAccionHumano,
                    onSiguienteMano = viewModel::siguienteMano,
                    onAbrirConfiguracion = { viewModel.abrirDialogoConfiguracion(true) },
                    onCerrarConfiguracion = { viewModel.abrirDialogoConfiguracion(false) },
                    onConfirmarConfiguracion = viewModel::configurarPartida,
                    onAbrirReglas = { viewModel.abrirDialogoReglas(true) },
                    onCerrarReglas = { viewModel.abrirDialogoReglas(false) }
                )
            }
        }
    }
}

@Composable
fun PantallaPrincipal(
    state: MesaUiState,
    onEjecutarAccion: (poker.modelo.Accion) -> Unit,
    onSiguienteMano: () -> Unit,
    onAbrirConfiguracion: () -> Unit,
    onCerrarConfiguracion: () -> Unit,
    onConfirmarConfiguracion: (poker.modelo.Variante, Long?, Boolean) -> Unit,
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
                onAbrirReglas = onAbrirReglas
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
                            .weight(1f)
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
                            .fillMaxWidth(0.38f)
                            .fillMaxHeight()
                            .padding(8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        PanelAccionesView(
                            accionesLegales = state.accionesLegales,
                            jugadorHumano = humano,
                            esTurnoHumano = state.esTurnoHumano,
                            apuestaMasAlta = mano.apuestaMasAlta,
                            boteTotal = mano.bote,
                            onEjecutarAccion = onEjecutarAccion,
                            esLandscape = true
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
                        esLandscape = false
                    )
                }
            }

            // Superposición de fin de mano (Showdown / Ganadores)
            if (mano.manoTerminada) {
                ShowdownOverlay(
                    ganadores = mano.ganadores,
                    onSiguienteMano = onSiguienteMano,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Diálogo de configuración
            if (state.mostrarDialogoConfiguracion) {
                DialogoConfiguracion(
                    varianteActual = mano.variante,
                    semillaActual = state.semillaConfigurada,
                    onConfirmar = onConfirmarConfiguracion,
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
