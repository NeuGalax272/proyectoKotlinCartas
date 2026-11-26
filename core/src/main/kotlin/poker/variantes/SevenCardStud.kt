package poker.variantes

import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

class SevenCardStudReglas(
    private val evaluador: EvaluadorAlto = EvaluadorAlto()
) : ReglasVariante {

    override val variante: Variante = Variante.SEVEN_CARD_STUD
    override val esJuegoComunitario: Boolean = false
    override val calles: List<Calle> = listOf(
        Calle.TERCERA,
        Calle.CUARTA,
        Calle.QUINTA,
        Calle.SEXTA,
        Calle.SEPTIMA
    )

    override fun repartirInicial(
        mazo: List<Carta>,
        jugadores: List<Jugador>
    ): ResultadoReparto {
        val mazoMutable = mazo.toMutableList()
        val actualizados = jugadores.map { j ->
            val c1 = mazoMutable.removeAt(0)
            val c2 = mazoMutable.removeAt(0)
            val c3 = mazoMutable.removeAt(0)
            // 3ra calle: 2 boca abajo, 1 boca arriba
            j.copy(
                cartas = listOf(c1, c2, c3),
                cartasVisibles = listOf(false, false, true)
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
        val actualizados = jugadores.map { j ->
            if (!j.estaEnJuego) return@map j
            val nueva = mazoMutable.removeAt(0)
            val esVisible = calle != Calle.SEPTIMA // 7ma calle va boca abajo
            j.copy(
                cartas = j.cartas + nueva,
                cartasVisibles = j.cartasVisibles + esVisible
            )
        }
        return ResultadoReparto(actualizados, emptyList(), mazoMutable.toList())
    }

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion {
        require(cartasPropias.size >= 5) { "Se requieren al menos 5 cartas para evaluar Stud" }
        return evaluador.evaluar(cartasPropias)
    }
}
