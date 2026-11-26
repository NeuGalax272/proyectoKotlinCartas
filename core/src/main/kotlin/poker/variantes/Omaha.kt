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
        mazo: List<Carta>,
        jugadores: List<Jugador>
    ): ResultadoReparto {
        val mazoMutable = mazo.toMutableList()
        val actualizados = jugadores.map { j ->
            val cartasMano = (1..4).map { mazoMutable.removeAt(0) }
            j.copy(
                cartas = cartasMano,
                cartasVisibles = listOf(false, false, false, false)
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
