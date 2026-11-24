package poker.variantes

import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

class HoldemReglas(
    private val evaluador: EvaluadorAlto = EvaluadorAlto()
) : ReglasVariante {

    override val variante: Variante = Variante.HOLDEM
    override val esJuegoComunitario: Boolean = true
    override val calles: List<Calle> = listOf(Calle.PREFLOP, Calle.FLOP, Calle.TURN, Calle.RIVER)

    override fun repartirInicial(
        mazo: MutableList<Carta>,
        jugadores: List<Jugador>
    ): Pair<List<Jugador>, List<Carta>> {
        val actualizados = jugadores.map { j ->
            val c1 = mazo.removeAt(0)
            val c2 = mazo.removeAt(0)
            j.copy(
                cartas = listOf(c1, c2),
                cartasVisibles = listOf(false, false)
            )
        }
        return Pair(actualizados, emptyList())
    }

    override fun repartirCalle(
        calle: Calle,
        mazo: MutableList<Carta>,
        jugadores: List<Jugador>,
        comunitarias: List<Carta>
    ): Pair<List<Jugador>, List<Carta>> {
        val nuevasComunitarias = comunitarias.toMutableList()
        when (calle) {
            Calle.FLOP -> {
                repeat(3) { nuevasComunitarias.add(mazo.removeAt(0)) }
            }
            Calle.TURN, Calle.RIVER -> {
                nuevasComunitarias.add(mazo.removeAt(0))
            }
            else -> {}
        }
        return Pair(jugadores, nuevasComunitarias)
    }

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion {
        val todas = cartasPropias + comunitarias
        require(todas.size >= 5) { "Se requieren al menos 5 cartas para evaluar Hold'em" }
        return evaluador.evaluar(todas)
    }
}
