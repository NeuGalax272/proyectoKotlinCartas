package poker.variantes

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

/** Una evaluación individual dentro de una porción del bote. */
data class EvaluacionJugador(
    val asiento: Int,
    val resultado: ResultadoEvaluacion
)

/**
 * Grupo de jugadores que compite por una fracción ponderada del bote.
 * Las reglas de una variante devuelven una porción para juegos normales y
 * dos porciones (alta/baja) cuando la variante divide el bote.
 */
data class PorcionEvaluada(
    val nombre: String,
    val peso: Int,
    val evaluaciones: List<EvaluacionJugador>,
    val esBaja: Boolean = false,
    /** Prioridad para repartir fichas indivisibles entre porciones empatadas. */
    val prioridadResto: Int = 0
) {
    init {
        require(nombre.isNotBlank())
        require(peso > 0)
        require(evaluaciones.isNotEmpty())
    }
}

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

    /** Política de evaluación de la variante expresada como porciones del bote. */
    fun evaluarPorciones(
        contendientes: List<Jugador>,
        comunitarias: List<Carta>
    ): List<PorcionEvaluada> {
        val evaluaciones = contendientes.map { jugador ->
            EvaluacionJugador(
                asiento = jugador.asiento,
                resultado = evaluarJugador(jugador.cartas, comunitarias)
            )
        }
        return listOf(PorcionEvaluada("Mano", 1, evaluaciones))
    }
}

/** Registro de implementaciones concretas, creado fuera del motor. */
fun reglasPredeterminadas(): Map<Variante, ReglasVariante> = listOf(
    HoldemReglas(),
    OmahaReglas(),
    RazzReglas(),
    SevenCardStudReglas(),
    SevenCardHiLoReglas()
).associateBy { it.variante }
