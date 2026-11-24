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
        mazo: MutableList<Carta>,
        jugadores: List<Jugador>
    ): Pair<List<Jugador>, List<Carta>> {
        val actualizados = jugadores.map { j ->
            val c1 = mazo.removeAt(0)
            val c2 = mazo.removeAt(0)
            val c3 = mazo.removeAt(0)
            // 3ra calle: 2 boca abajo, 1 boca arriba
            j.copy(
                cartas = listOf(c1, c2, c3),
                cartasVisibles = listOf(false, false, true)
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
        val actualizados = jugadores.map { j ->
            if (!j.estaEnJuego) return@map j
            val nueva = mazo.removeAt(0)
            val esVisible = calle != Calle.SEPTIMA // 7ma calle va boca abajo
            j.copy(
                cartas = j.cartas + nueva,
                cartasVisibles = j.cartasVisibles + esVisible
            )
        }
        return Pair(actualizados, emptyList())
    }

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion {
        require(cartasPropias.size >= 5) { "Se requieren al menos 5 cartas para evaluar Stud" }
        return evaluador.evaluar(cartasPropias)
    }
}
