package poker.variantes

import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador

/**
 * Cómo se estructura y se reparte una variante: sus calles y cómo se entregan las cartas.
 * Las reglas de cada variante lo reciben por delegación, así el reparto se escribe una sola vez.
 */
interface PlanDeReparto {
    val esJuegoComunitario: Boolean
    val calles: List<Calle>

    fun repartirInicial(
        mazo: List<Carta>,
        jugadores: List<Jugador>
    ): ResultadoReparto

    fun repartirCalle(
        calle: Calle,
        mazo: List<Carta>,
        jugadores: List<Jugador>,
        comunitarias: List<Carta>
    ): ResultadoReparto
}

/** Hold'em y Omaha: cartas privadas al inicio y luego flop (3), turn (1) y river (1) a la mesa. */
class RepartoComunitario(private val cartasPropias: Int) : PlanDeReparto {

    init {
        require(cartasPropias > 0) { "Se debe repartir al menos una carta propia" }
    }

    override val esJuegoComunitario: Boolean = true
    override val calles: List<Calle> = listOf(Calle.PREFLOP, Calle.FLOP, Calle.TURN, Calle.RIVER)

    override fun repartirInicial(
        mazo: List<Carta>,
        jugadores: List<Jugador>
    ): ResultadoReparto {
        val actualizados = jugadores.mapIndexed { i, jugador ->
            jugador.copy(
                cartas = mazo.subList(i * cartasPropias, (i + 1) * cartasPropias).toList(),
                cartasVisibles = List(cartasPropias) { false }
            )
        }
        return ResultadoReparto(actualizados, emptyList(), mazo.drop(cartasPropias * jugadores.size))
    }

    override fun repartirCalle(
        calle: Calle,
        mazo: List<Carta>,
        jugadores: List<Jugador>,
        comunitarias: List<Carta>
    ): ResultadoReparto {
        val cuantas = when (calle) {
            Calle.FLOP -> 3
            Calle.TURN, Calle.RIVER -> 1
            else -> 0
        }
        return ResultadoReparto(jugadores, comunitarias + mazo.take(cuantas), mazo.drop(cuantas))
    }
}

/**
 * Razz, Stud y Stud Hi-Lo: tercera calle con 2 cartas boca abajo y 1 boca arriba,
 * luego una carta por calle a cada jugador que sigue en la mano; la séptima va boca abajo.
 */
class RepartoStud : PlanDeReparto {

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
        val actualizados = jugadores.mapIndexed { i, jugador ->
            jugador.copy(
                cartas = mazo.subList(i * CARTAS_INICIALES, (i + 1) * CARTAS_INICIALES).toList(),
                cartasVisibles = listOf(false, false, true)
            )
        }
        return ResultadoReparto(actualizados, emptyList(), mazo.drop(CARTAS_INICIALES * jugadores.size))
    }

    override fun repartirCalle(
        calle: Calle,
        mazo: List<Carta>,
        jugadores: List<Jugador>,
        comunitarias: List<Carta>
    ): ResultadoReparto {
        var siguiente = 0
        val actualizados = jugadores.map { jugador ->
            if (!jugador.estaEnJuego) return@map jugador
            val nueva = mazo[siguiente++]
            jugador.copy(
                cartas = jugador.cartas + nueva,
                cartasVisibles = jugador.cartasVisibles + (calle != Calle.SEPTIMA)
            )
        }
        return ResultadoReparto(actualizados, emptyList(), mazo.drop(siguiente))
    }

    private companion object {
        const val CARTAS_INICIALES = 3
    }
}
