package com.poker.horse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import poker.modelo.Accion
import poker.modelo.GeneradorDeMazo
import poker.modelo.Jugador
import poker.modelo.MazoAleatorio
import poker.modelo.Variante
import poker.motor.EstadoMano
import poker.motor.MotorDeJuego
import poker.variantes.reglasPredeterminadas

class MesaViewModel(
    private val motor: MotorDeJuego = MotorDeJuego(reglasPredeterminadas())
) : ViewModel() {

    private val listaVariantesHORSE = listOf(
        Variante.HOLDEM,
        Variante.OMAHA,
        Variante.RAZZ,
        Variante.SEVEN_CARD_STUD,
        Variante.SEVEN_CARD_HI_LO
    )

    private var indiceVarianteActual = 0
    private var dealerIndexActual = 0
    private var generadorActual: GeneradorDeMazo = MazoAleatorio()
    private var botTurnJob: Job? = null

    private val _uiState: MutableStateFlow<MesaUiState>

    val uiState: StateFlow<MesaUiState>

    init {
        val jugadoresIniciales = listOf(
            Jugador(asiento = 0, nombre = "Tú", pila = 1000, esHumano = true),
            Jugador(asiento = 1, nombre = "Bot Sofía", pila = 1000, esHumano = false),
            Jugador(asiento = 2, nombre = "Bot Carlos", pila = 1000, esHumano = false),
            Jugador(asiento = 3, nombre = "Bot Mateo", pila = 1000, esHumano = false)
        )

        val primerEstado = motor.iniciarNuevaMano(
            variante = listaVariantesHORSE[indiceVarianteActual],
            jugadoresActuales = jugadoresIniciales,
            dealerIndex = dealerIndexActual,
            generadorDeMazo = generadorActual
        )

        val inicialesLegales = motor.accionesLegales(primerEstado)
        val esHumano = primerEstado.jugadorEnTurno?.esHumano == true

        _uiState = MutableStateFlow(
            MesaUiState(
                estadoMano = primerEstado,
                accionesLegales = if (esHumano) inicialesLegales else emptySet(),
                esTurnoHumano = esHumano,
                manoEnVariante = 1,
                manosPorVariante = 5,
                semillaConfigurada = null,
                evaluacionHumanoTexto = calcularEvaluacionHumano(primerEstado)
            )
        )
        uiState = _uiState.asStateFlow()

        revisarTurnoBot(primerEstado)
    }

    fun ejecutarAccionHumano(accion: Accion) {
        val estadoActual = _uiState.value.estadoMano
        if (estadoActual.manoTerminada || estadoActual.jugadorEnTurno?.esHumano != true) return

        val nuevoEstado = motor.aplicar(estadoActual, accion)
        actualizarEstado(nuevoEstado)
    }

    fun siguienteMano() {
        botTurnJob?.cancel()
        val estadoPrevio = _uiState.value.estadoMano

        // Siguiente dealer
        dealerIndexActual = (dealerIndexActual + 1) % estadoPrevio.jugadores.size

        // Comprobar si rotamos variante HORSE
        var manoNum = _uiState.value.manoEnVariante + 1
        if (manoNum > _uiState.value.manosPorVariante) {
            manoNum = 1
            indiceVarianteActual = (indiceVarianteActual + 1) % listaVariantesHORSE.size
        }

        val varianteActual = listaVariantesHORSE[indiceVarianteActual]

        // Recargar fichas si alguien se quedó en 0
        val jugadoresListos = estadoPrevio.jugadores.map { j ->
            if (j.pila <= 0) j.copy(pila = 1000) else j
        }

        val nuevoEstado = motor.iniciarNuevaMano(
            variante = varianteActual,
            jugadoresActuales = jugadoresListos,
            dealerIndex = dealerIndexActual,
            generadorDeMazo = generadorActual
        )

        _uiState.update {
            it.copy(
                estadoMano = nuevoEstado,
                manoEnVariante = manoNum,
                accionesLegales = if (nuevoEstado.jugadorEnTurno?.esHumano == true) {
                    motor.accionesLegales(nuevoEstado)
                } else emptySet(),
                esTurnoHumano = nuevoEstado.jugadorEnTurno?.esHumano == true,
                evaluacionHumanoTexto = calcularEvaluacionHumano(nuevoEstado)
            )
        }

        revisarTurnoBot(nuevoEstado)
    }

    fun configurarPartida(nuevaVariante: Variante, nuevaSemilla: Long?, reiniciarFichas: Boolean) {
        botTurnJob?.cancel()

        indiceVarianteActual = listaVariantesHORSE.indexOf(nuevaVariante).let { if (it >= 0) it else 0 }
        generadorActual = MazoAleatorio(nuevaSemilla)

        val estadoPrevio = _uiState.value.estadoMano
        val jugadoresActualizados = if (reiniciarFichas) {
            estadoPrevio.jugadores.map { it.copy(pila = 1000) }
        } else {
            estadoPrevio.jugadores.map { if (it.pila <= 0) it.copy(pila = 1000) else it }
        }

        val nuevoEstado = motor.iniciarNuevaMano(
            variante = nuevaVariante,
            jugadoresActuales = jugadoresActualizados,
            dealerIndex = dealerIndexActual,
            generadorDeMazo = generadorActual
        )

        _uiState.update {
            it.copy(
                estadoMano = nuevoEstado,
                manoEnVariante = 1,
                semillaConfigurada = nuevaSemilla,
                mostrarDialogoConfiguracion = false,
                accionesLegales = if (nuevoEstado.jugadorEnTurno?.esHumano == true) {
                    motor.accionesLegales(nuevoEstado)
                } else emptySet(),
                esTurnoHumano = nuevoEstado.jugadorEnTurno?.esHumano == true,
                evaluacionHumanoTexto = calcularEvaluacionHumano(nuevoEstado)
            )
        }

        revisarTurnoBot(nuevoEstado)
    }

    fun abrirDialogoConfiguracion(abrir: Boolean) {
        _uiState.update { it.copy(mostrarDialogoConfiguracion = abrir) }
    }

    fun abrirDialogoReglas(abrir: Boolean) {
        _uiState.update { it.copy(mostrarDialogoReglas = abrir) }
    }

    private fun actualizarEstado(nuevoEstado: EstadoMano) {
        val esHumano = nuevoEstado.jugadorEnTurno?.esHumano == true
        val legales = if (esHumano) motor.accionesLegales(nuevoEstado) else emptySet()

        _uiState.update {
            it.copy(
                estadoMano = nuevoEstado,
                accionesLegales = legales,
                esTurnoHumano = esHumano,
                evaluacionHumanoTexto = calcularEvaluacionHumano(nuevoEstado)
            )
        }

        revisarTurnoBot(nuevoEstado)
    }

    private fun revisarTurnoBot(estado: EstadoMano) {
        botTurnJob?.cancel()
        if (estado.manoTerminada) return

        val enTurno = estado.jugadorEnTurno
        if (enTurno != null && !enTurno.esHumano) {
            botTurnJob = viewModelScope.launch {
                delay(850) // Pausa para que el usuario aprecie el juego del bot
                val accionBot = motor.decidirAccionBot(estado)
                val nuevoEstado = motor.aplicar(estado, accionBot)
                actualizarEstado(nuevoEstado)
            }
        }
    }

    private fun calcularEvaluacionHumano(estado: EstadoMano): String? {
        val humano = estado.jugadores.firstOrNull { it.esHumano } ?: return null
        val regla = motor.reglasPara(estado.variante)

        return try {
            if (regla.esJuegoComunitario) {
                if (humano.cartas.isNotEmpty() && estado.comunitarias.size >= 3) {
                    val res = regla.evaluarJugador(humano.cartas, estado.comunitarias)
                    "Tu Mano: ${res.categoria.nombre}"
                } else null
            } else {
                if (humano.cartas.size >= 5) {
                    val res = regla.evaluarJugador(humano.cartas, emptyList())
                    "Tu Mano: ${res.categoria.nombre}"
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
