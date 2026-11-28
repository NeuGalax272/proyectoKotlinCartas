package poker.bots

import kotlin.math.ceil
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.modelo.Rango
import poker.motor.EstadoMano

/**
 * Hold'em.
 * - Antes del flop: fórmula de Chen sobre las 2 cartas propias, normalizada de -1 a 20.
 * - Desde el flop: categoría de la mejor mano de 5 (tabla estándar) y +0.12 si hay proyecto de color.
 */
class ValoradorHoldem(
    private val evaluar: (List<Carta>, List<Carta>) -> ResultadoEvaluacion
) : ValoradorDeMano {

    override fun fuerza(estado: EstadoMano, asiento: Int): Double {
        val propias = Heuristicas.jugadorDe(asiento, estado.jugadores).cartas
        if (propias.size < 2) return 0.0
        if (estado.comunitarias.size < 3) return fuerzaPreflop(propias[0], propias[1])

        val base = Heuristicas.fuerzaPorCategoria(evaluar(propias, estado.comunitarias).categoria)
        val todas = propias + estado.comunitarias
        val proyecto = estado.comunitarias.size < 5 && Heuristicas.tieneColorProyecto(propias, todas)
        return if (proyecto) (base + 0.12).coerceAtMost(1.0) else base
    }

    internal fun fuerzaPreflop(a: Carta, b: Carta): Double =
        ((puntosChen(a, b) + 1) / 21.0).coerceIn(0.0, 1.0)

    internal fun puntosChen(a: Carta, b: Carta): Int {
        val alta = maxOf(a.rango.valor, b.rango.valor)
        val baja = minOf(a.rango.valor, b.rango.valor)
        if (a.rango == b.rango) return ceil(maxOf(5.0, 2 * puntosCarta(alta))).toInt()

        var puntos = puntosCarta(alta)
        if (a.palo == b.palo) puntos += 2.0
        val hueco = alta - baja - 1
        puntos -= when (hueco) {
            0 -> 0.0
            1 -> 1.0
            2 -> 2.0
            3 -> 4.0
            else -> 5.0
        }
        if (hueco <= 1 && alta < Rango.REINA.valor) puntos += 1.0
        return ceil(puntos).toInt()
    }

    private fun puntosCarta(valor: Int): Double = when (valor) {
        14 -> 10.0
        13 -> 8.0
        12 -> 7.0
        11 -> 6.0
        else -> valor / 2.0
    }
}
