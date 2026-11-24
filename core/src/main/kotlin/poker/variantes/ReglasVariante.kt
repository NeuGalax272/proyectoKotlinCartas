package poker.variantes

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

interface ReglasVariante {
    val variante: Variante
    val esJuegoComunitario: Boolean
    val calles: List<Calle>

    fun repartirInicial(
        mazo: MutableList<Carta>,
        jugadores: List<Jugador>
    ): Pair<List<Jugador>, List<Carta>>

    fun repartirCalle(
        calle: Calle,
        mazo: MutableList<Carta>,
        jugadores: List<Jugador>,
        comunitarias: List<Carta>
    ): Pair<List<Jugador>, List<Carta>>

    fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion

    fun evaluarJugadorBajo(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion? = null
}
