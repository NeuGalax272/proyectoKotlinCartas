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
import poker.bots.BotPorUmbrales
import poker.bots.EstrategiaDeBot
import poker.bots.PerfilBot
import poker.modelo.Accion
import poker.modelo.GeneradorDeMazo
import poker.modelo.Jugador
import poker.modelo.MazoAleatorio
import poker.modelo.Variante
import poker.motor.EstadoMano
import poker.motor.MotorDeJuego
import poker.motor.ResultadoAccion
import poker.variantes.reglasPredeterminadas

class MesaViewModel(
    private val motor: MotorDeJuego = MotorDeJuego(reglasPredeterminadas()),
    private var modoPartida: ModoPartida = ModoPartida.HORSE,
    private val jugadoresDeInicio: List<Jugador> = listOf(
        Jugador(asiento = 0, nombre = "Tú", pila = 1000, esHumano = true),
        Jugador(asiento = 1, nombre = "Bot Sofía", pila = 1000),
        Jugador(asiento = 2, nombre = "Bot Carlos", pila = 1000),
        Jugador(asiento = 3, nombre = "Bot Mateo", pila = 1000)
    ),
    idsPorAsiento: Map<Int, String> = emptyMap(),
    private val idJugadorLocal: String? = null,
    private val esAnfitrion: Boolean = true
) : ViewModel() {

    private val idsActivosPorAsiento = idsPorAsiento.toMutableMap()
    val idsPorAsiento: Map<Int, String>
        get() = idsActivosPorAsiento.toMap()

    private var listaVariantes = modoPartida.variantes

    private var indiceVarianteActual = 0
    private var dealerIndexActual = 0
    private var generadorActual: GeneradorDeMazo = MazoAleatorio()
    private var botTurnJob: Job? = null

    private val estrategiaEstandar: EstrategiaDeBot =
        BotPorUmbrales({ motor.reglasPara(it).valorador }, PerfilBot.EQUILIBRADO)

    private val estrategiasPorAsiento: Map<Int, EstrategiaDeBot> = mapOf(
        1 to BotPorUmbrales({ motor.reglasPara(it).valorador }, PerfilBot.CONSERVADOR),
        2 to estrategiaEstandar,
        3 to BotPorUmbrales({ motor.reglasPara(it).valorador }, PerfilBot.AGRESIVO)
    )

    private val _uiState: MutableStateFlow<MesaUiState>

    val uiState: StateFlow<MesaUiState>

    init {
        val jugadoresIniciales = jugadoresDeInicio.map { jugador ->
            val esLocal = idJugadorLocal?.let { idsActivosPorAsiento[jugador.asiento] == it } ?: jugador.esHumano
            jugador.copy(esHumano = esLocal)
        }

        val primerEstado = motor.iniciarNuevaMano(
            variante = listaVariantes[indiceVarianteActual],
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
                manosPorVariante = if (modoPartida == ModoPartida.HORSE) 5 else 1,
                semillaConfigurada = null,
                evaluacionHumanoTexto = calcularEvaluacionHumano(primerEstado)
            )
        )
        uiState = _uiState.asStateFlow()

        if (esAnfitrion) revisarTurnoBot(primerEstado)
    }

    fun ejecutarAccionHumano(accion: Accion) {
        val estadoActual = _uiState.value.estadoMano
        if (estadoActual.manoTerminada || estadoActual.jugadorEnTurno?.esHumano != true) return

        when (val resultado = motor.aplicar(estadoActual, accion)) {
            is ResultadoAccion.Aplicada -> actualizarEstado(resultado.estado)
            is ResultadoAccion.Rechazada -> _uiState.update {
                it.copy(accionesLegales = resultado.accionesLegales)
            }
        }
    }

    fun siguienteMano() {
        if (!esAnfitrion) return
        botTurnJob?.cancel()
        val estadoPrevio = _uiState.value.estadoMano

        // Siguiente dealer
        dealerIndexActual = (dealerIndexActual + 1) % estadoPrevio.jugadores.size

        // Comprobar si rotamos variante HORSE
        var manoNum = _uiState.value.manoEnVariante + 1
        if (manoNum > _uiState.value.manosPorVariante) {
            manoNum = 1
            indiceVarianteActual = (indiceVarianteActual + 1) % listaVariantes.size
        }

        val varianteActual = listaVariantes[indiceVarianteActual]

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
        if (!esAnfitrion) return
        botTurnJob?.cancel()

        modoPartida = ModoPartida.desdeVariante(nuevaVariante)
        listaVariantes = modoPartida.variantes
        indiceVarianteActual = 0
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
                manosPorVariante = 1,
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
        if (!esAnfitrion) return
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
        if (!esAnfitrion || estado.manoTerminada) return

        val enTurno = estado.jugadorEnTurno
        val esAsientoHumano = enTurno != null && idsActivosPorAsiento.containsKey(enTurno.asiento)
        if (enTurno != null && !enTurno.esHumano && !esAsientoHumano) {
            botTurnJob = viewModelScope.launch {
                delay(850) // Pausa para que el usuario aprecie el juego del bot
                val legales = motor.accionesLegales(estado)
                val estrategia = estrategiasPorAsiento[enTurno.asiento] ?: estrategiaEstandar
                val accionBot = estrategia.proponer(estado, legales)
                when (val resultado = motor.aplicar(estado, accionBot)) {
                    is ResultadoAccion.Aplicada -> actualizarEstado(resultado.estado)
                    is ResultadoAccion.Rechazada -> actualizarEstado(estado)
                }
            }
        }
    }

    fun ejecutarAccionJugador(jugadorId: String, accion: Accion) {
        if (!esAnfitrion) return
        val estadoActual = _uiState.value.estadoMano
        val asientoEnTurno = estadoActual.turnoActual ?: return
        if (idsActivosPorAsiento[asientoEnTurno] != jugadorId || estadoActual.manoTerminada) return
        when (val resultado = motor.aplicar(estadoActual, accion)) {
            is ResultadoAccion.Aplicada -> actualizarEstado(resultado.estado)
            is ResultadoAccion.Rechazada -> Unit
        }
    }

    fun desconectarJugador(jugadorId: String) {
        if (!esAnfitrion) return
        val asiento = idsActivosPorAsiento.entries.firstOrNull { it.value == jugadorId }?.key ?: return
        idsActivosPorAsiento.remove(asiento)
        val estadoActual = _uiState.value.estadoMano
        if (estadoActual.turnoActual == asiento && !estadoActual.manoTerminada) {
            _uiState.update { it.copy(accionesLegales = emptySet(), esTurnoHumano = false) }
            revisarTurnoBot(estadoActual)
        }
    }

    fun aplicarEstadoRemoto(remoto: EstadoRemoto) {
        if (esAnfitrion) return
        botTurnJob?.cancel()
        val jugadores = remoto.estado.jugadores.map { jugador ->
            jugador.copy(esHumano = remoto.idsPorAsiento[jugador.asiento] == idJugadorLocal)
        }
        val porAsiento = jugadores.associateBy { it.asiento }
        val estado = remoto.estado.copy(
            jugadores = jugadores,
            ganadores = remoto.estado.ganadores.map { ganador ->
                ganador.copy(jugador = porAsiento[ganador.jugador.asiento] ?: ganador.jugador)
            }
        )
        val esMiTurno = estado.jugadorEnTurno?.let { remoto.idsPorAsiento[it.asiento] == idJugadorLocal } == true
        _uiState.update {
            it.copy(
                estadoMano = estado,
                manoEnVariante = remoto.manoEnVariante,
                manosPorVariante = remoto.manosPorVariante,
                accionesLegales = if (esMiTurno) motor.accionesLegales(estado) else emptySet(),
                esTurnoHumano = esMiTurno,
                evaluacionHumanoTexto = calcularEvaluacionHumano(estado)
            )
        }
    }

    fun crearEstadoRemoto(): EstadoRemoto = EstadoRemoto(
        estado = _uiState.value.estadoMano,
        idsPorAsiento = idsPorAsiento,
        modo = modoPartida,
        manoEnVariante = _uiState.value.manoEnVariante,
        manosPorVariante = _uiState.value.manosPorVariante
    )

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

