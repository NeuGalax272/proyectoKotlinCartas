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
        mazo: List<Carta>,
        jugadores: List<Jugador>
    ): ResultadoReparto {
        val mazoMutable = mazo.toMutableList()
        val actualizados = jugadores.map { j ->
            val c1 = mazoMutable.removeAt(0)
            val c2 = mazoMutable.removeAt(0)
            j.copy(
                cartas = listOf(c1, c2),
                cartasVisibles = listOf(false, false)
            )
        }
        return ResultadoReparto(actualizados, emptyList(), mazoMutable.toList())
    }

    override fun repartirCalle(
        calle: Calle,
        mazo: List<Carta>,
        jugadores: List<Jugador>,
        comunitarias: List<Carta>
    ): ResultadoReparto {
        val mazoMutable = mazo.toMutableList()
        val nuevasComunitarias = comunitarias.toMutableList()
        when (calle) {
            Calle.FLOP -> {
                repeat(3) { nuevasComunitarias.add(mazoMutable.removeAt(0)) }
            }
            Calle.TURN, Calle.RIVER -> {
                nuevasComunitarias.add(mazoMutable.removeAt(0))
            }
            else -> {}
        }
        return ResultadoReparto(jugadores, nuevasComunitarias, mazoMutable.toList())
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
