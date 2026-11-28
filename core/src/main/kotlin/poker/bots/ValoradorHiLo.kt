package poker.bots

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.motor.EstadoMano

/**
 * Seven Card Stud Hi-Lo 8-or-Better.
 * Combina la valoración alta (la de [ValoradorStud]) con la baja:
 * fuerza = max(alta, baja) + 0.15 * min(alta, baja), para premiar quedarse con ambas mitades (scoop).
 * - Baja con 5 o más cartas: mejor baja calificada (<= 8, sin pares); si no califica vale 0.
 * - Baja con 3 o 4 cartas: 0.15 por cada rango distinto <= 8 y 0.05 por cada uno <= 5 (máximo 0.75).
 */
class ValoradorHiLo(
    evaluarAlto: (List<Carta>) -> ResultadoEvaluacion,
    private val evaluarBajo: (List<Carta>) -> ResultadoEvaluacion?
) : ValoradorDeMano {

    private val alto = ValoradorStud(evaluarAlto)

    override fun fuerza(estado: EstadoMano, asiento: Int): Double {
        val cartas = Heuristicas.jugadorDe(asiento, estado.jugadores).cartas
        if (cartas.isEmpty()) return 0.0
        val a = alto.fuerza(estado, asiento)
        val b = fuerzaBaja(cartas)
        return (maxOf(a, b) + 0.15 * minOf(a, b)).coerceIn(0.0, 1.0)
    }

    private fun fuerzaBaja(cartas: List<Carta>): Double {
        if (cartas.size >= 5) {
            val resultado = evaluarBajo(cartas) ?: return 0.0
            return Heuristicas.fuerzaBaja(resultado.desempate)
        }
        val bajas = cartas.map(Heuristicas::valorBajo).filter { it <= 8 }.distinct()
        if (bajas.isEmpty()) return 0.0
        return (0.15 * bajas.size + 0.05 * bajas.count { it <= 5 }).coerceAtMost(0.75)
    }
}
