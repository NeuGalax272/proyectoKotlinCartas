package poker.bots

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.motor.EstadoMano

/**
 * Omaha.
 * - Antes del flop: puntos por cartas altas (+1 c/u), parejas (+2), palos dobles (+1.5 por cada par de
 *   cartas del mismo palo), conectividad (+2) y castigo por tríos en mano (-3). Máximo práctico 11.
 * - Desde el flop: categoría de la mano exacta 2+3 con una tabla más exigente, porque en Omaha
 *   hay muchas más combinaciones y las manos medias pierden valor.
 */
class ValoradorOmaha(
    private val evaluar: (List<Carta>, List<Carta>) -> ResultadoEvaluacion
) : ValoradorDeMano {

    override fun fuerza(estado: EstadoMano, asiento: Int): Double {
        val propias = Heuristicas.jugadorDe(asiento, estado.jugadores).cartas
        if (propias.size < 4) return 0.0
        if (estado.comunitarias.size < 3) return fuerzaPreflop(propias)

        val resultado = evaluar(propias, estado.comunitarias)
        return Heuristicas.fuerzaPorCategoria(resultado.categoria, Heuristicas.TABLA_OMAHA)
    }

    internal fun fuerzaPreflop(cartas: List<Carta>): Double {
        val altas = cartas.count { it.rango.valor >= 10 }
        val repeticiones = cartas.groupingBy { it.rango }.eachCount().values
        val pares = repeticiones.count { it == 2 }
        val trio = repeticiones.any { it >= 3 }
        val palosDobles = cartas.groupingBy { it.palo }.eachCount().values.count { it == 2 }
        val valores = cartas.map { it.rango.valor }.distinct().sorted()
        val conectadas = valores.indices.any { i -> i + 2 < valores.size && valores[i + 2] - valores[i] <= 4 }

        var puntos = altas * 1.0 + pares * 2.0 + palosDobles * 1.5
        if (conectadas) puntos += 2.0
        if (trio) puntos -= 3.0
        return (puntos / 11.0).coerceIn(0.0, 1.0)
    }
}
