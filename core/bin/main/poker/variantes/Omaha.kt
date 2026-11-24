package poker.variantes

import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.ResultadoEvaluacion
import poker.evaluacion.combinaciones
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

class OmahaReglas(
    private val evaluador: EvaluadorAlto = EvaluadorAlto()
) : ReglasVariante {

    override val variante: Variante = Variante.OMAHA
    override val esJuegoComunitario: Boolean = true
    override val calles: List<Calle> = listOf(Calle.PREFLOP, Calle.FLOP, Calle.TURN, Calle.RIVER)

    override fun repartirInicial(
        mazo: MutableList<Carta>,
        jugadores: List<Jugador>
    ): Pair<List<Jugador>, List<Carta>> {
        val actualizados = jugadores.map { j ->
            val cartasMano = (1..4).map { mazo.removeAt(0) }
            j.copy(
                cartas = cartasMano,
                cartasVisibles = listOf(false, false, false, false)
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
        require(cartasPropias.size >= 2) { "Omaha requiere al menos 2 cartas en mano" }
        require(comunitarias.size >= 3) { "Omaha requiere al menos 3 cartas comunitarias" }

        val paresPropias = combinaciones(cartasPropias, 2)
        val triosComunitarias = combinaciones(comunitarias, 3)

        var mejor: ResultadoEvaluacion? = null
        for (dos in paresPropias) {
            for (tres in triosComunitarias) {
                val eval = evaluador.evaluar(dos + tres)
                if (mejor == null || eval > mejor) {
                    mejor = eval
                }
            }
        }
        return mejor ?: error("No se pudo evaluar la mano de Omaha")
    }
}
