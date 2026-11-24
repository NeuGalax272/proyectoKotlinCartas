package com.poker.horse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poker.horse.ui.components.DialogoConfiguracion
import com.poker.horse.ui.components.DialogoReglas
import com.poker.horse.ui.components.HorseHeaderBar
import com.poker.horse.ui.components.MesaView
import com.poker.horse.ui.components.PanelAccionesView
import com.poker.horse.ui.components.ShowdownOverlay
import com.poker.horse.ui.theme.FondoOscuro
import com.poker.horse.ui.theme.PokerHorseTheme
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
    state: com.poker.horse.ui.viewmodel.MesaUiState,
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
        containerColor = FondoOscuro
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(FondoOscuro)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mesa de póker
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    MesaView(
                        jugadores = mano.jugadores,
                        comunitarias = mano.comunitarias,
                        bote = mano.bote,
                        calleEtiqueta = mano.calle.etiqueta,
                        variante = mano.variante,
                        dealerIndex = mano.dealerIndex,
                        turnoActual = mano.turnoActual,
                        mostrarCartasRival = mano.manoTerminada,
                        evaluacionHumano = state.evaluacionHumanoTexto
                    )
                }

                // Panel de acciones en la parte inferior
                PanelAccionesView(
                    accionesLegales = state.accionesLegales,
                    jugadorHumano = humano,
                    esTurnoHumano = state.esTurnoHumano,
                    apuestaMasAlta = mano.apuestaMasAlta,
                    boteTotal = mano.bote,
                    onEjecutarAccion = onEjecutarAccion
                )
            }

            // Superposición al terminar la mano (Showdown / Ganadores)
            if (mano.manoTerminada) {
                ShowdownOverlay(
                    ganadores = mano.ganadores,
                    onSiguienteMano = onSiguienteMano,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Diálogo de configuración y semilla
            if (state.mostrarDialogoConfiguracion) {
                DialogoConfiguracion(
                    varianteActual = mano.variante,
                    semillaActual = state.semillaConfigurada,
                    onConfirmar = onConfirmarConfiguracion,
                    onDismiss = onCerrarConfiguracion
                )
            }

            // Diálogo informativo de reglas
            if (state.mostrarDialogoReglas) {
                DialogoReglas(
                    variante = mano.variante,
                    onDismiss = onCerrarReglas
                )
            }
        }
    }
}
