package poker.variantes

import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.EvaluadorBajoOcho
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

class SevenCardHiLoReglas(
    private val evaluadorAlto: EvaluadorAlto = EvaluadorAlto(),
    private val evaluadorBajo: EvaluadorBajoOcho = EvaluadorBajoOcho()
) : ReglasVariante {

    override val variante: Variante = Variante.SEVEN_CARD_HI_LO
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
            val esVisible = calle != Calle.SEPTIMA
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
        require(cartasPropias.size >= 5) { "Se requieren al menos 5 cartas para evaluar Stud Hi-Lo" }
        return evaluadorAlto.evaluar(cartasPropias)
    }

    override fun evaluarJugadorBajo(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion? {
        if (cartasPropias.size < 5) return null
        return evaluadorBajo.evaluar(cartasPropias)
    }

    override fun evaluarPorciones(
        contendientes: List<Jugador>,
        comunitarias: List<Carta>
    ): List<PorcionEvaluada> {
        val altas = contendientes.map { jugador ->
            EvaluacionJugador(jugador.asiento, evaluarJugador(jugador.cartas, comunitarias))
        }
        val bajas = contendientes.mapNotNull { jugador ->
            evaluarJugadorBajo(jugador.cartas, comunitarias)?.let { resultado ->
                EvaluacionJugador(jugador.asiento, resultado)
            }
        }

        if (bajas.isEmpty()) {
            return listOf(PorcionEvaluada("Mano Alta", 1, altas))
        }

        return listOf(
            PorcionEvaluada("Mano Alta", 1, altas, prioridadResto = 1),
            PorcionEvaluada("Mano Baja", 1, bajas, esBaja = true)
        )
    }
}
