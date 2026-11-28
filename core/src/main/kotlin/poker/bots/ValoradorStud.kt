package poker.bots

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.motor.EstadoMano

/**
 * Seven Card Stud (mano alta).
 * - Con 5 o más cartas: categoría de la mejor mano de 5, +0.10 si queda proyecto de color.
 * - Con 3 o 4 cartas: trío 0.80; pareja 0.40 + hasta 0.25 según su rango; si no, hasta 0.20 por la
 *   carta más alta. Se suman +0.12 por 3 del mismo palo y +0.08 por 3 en ventana de escalera.
 */
class ValoradorStud(
    private val evaluar: (List<Carta>) -> ResultadoEvaluacion
) : ValoradorDeMano {

    override fun fuerza(estado: EstadoMano, asiento: Int): Double {
        val cartas = Heuristicas.jugadorDe(asiento, estado.jugadores).cartas
        if (cartas.isEmpty()) return 0.0
        return if (cartas.size >= 5) fuerzaMadura(cartas) else fuerzaTemprana(cartas)
    }

    private fun fuerzaMadura(cartas: List<Carta>): Double {
        val base = Heuristicas.fuerzaPorCategoria(evaluar(cartas).categoria)
        val proyecto = cartas.size < 7 &&
            cartas.groupingBy { it.palo }.eachCount().values.any { it == 4 }
        return if (proyecto) (base + 0.10).coerceAtMost(1.0) else base
    }

    private fun fuerzaTemprana(cartas: List<Carta>): Double {
        val repeticiones = cartas.groupingBy { it.rango }.eachCount()
        val trio = repeticiones.entries.any { it.value >= 3 }
        val parejaMasAlta = repeticiones.filter { it.value == 2 }.keys.maxOfOrNull { it.valor }

        val base = when {
            trio -> 0.80
            parejaMasAlta != null -> 0.40 + (parejaMasAlta - 2) / 12.0 * 0.25
            else -> 0.05 + (cartas.maxOf { it.rango.valor } - 2) / 12.0 * 0.15
        }
        return (base + Heuristicas.bonoProyectosTempranos(cartas)).coerceIn(0.0, 1.0)
    }
}
