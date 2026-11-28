package poker.bots

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.motor.EstadoMano

/**
 * Razz (lowball A-5, el As vale 1).
 * - Con 5 o más cartas usa la mejor mano baja del evaluador.
 * - Con 3 o 4 cartas toma las más bajas de rangos distintos.
 * Fuerza = (10 - promedio de valores) / 8, menos 0.12 por cada rango repetido.
 * No considera las cartas visibles de los rivales.
 */
class ValoradorRazz(
    private val evaluarBajo: (List<Carta>) -> ResultadoEvaluacion
) : ValoradorDeMano {

    override fun fuerza(estado: EstadoMano, asiento: Int): Double {
        val cartas = Heuristicas.jugadorDe(asiento, estado.jugadores).cartas
        if (cartas.isEmpty()) return 0.0
        val valores = if (cartas.size >= 5) evaluarBajo(cartas).desempate else Heuristicas.mejoresBajas(cartas)
        return Heuristicas.fuerzaBaja(valores)
    }
}
