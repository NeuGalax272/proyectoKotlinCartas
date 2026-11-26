package poker.motor

import poker.modelo.Jugador

data class Bote(
    val monto: Int,
    val asientosElegibles: Set<Int>
)

data class DistribucionBotes(
    val botes: List<Bote>,
    val devoluciones: Map<Int, Int>
)

/** Construye botes principal/secundarios desde los aportes acumulados de la mano. */
class CalculadorDeBotes {
    fun calcular(jugadores: List<Jugador>): DistribucionBotes {
        val niveles = jugadores.map { it.aporteTotal }.filter { it > 0 }.distinct().sorted()
        var nivelAnterior = 0
        val botes = mutableListOf<Bote>()
        val devoluciones = mutableMapOf<Int, Int>()

        for (nivel in niveles) {
            val participantes = jugadores.filter { it.aporteTotal >= nivel }
            val monto = (nivel - nivelAnterior) * participantes.size
            val elegibles = participantes.filter { it.estaEnJuego }.map { it.asiento }.toSet()
            if (elegibles.isEmpty()) {
                // Aporte no igualado: se devuelve a quien lo puso, aunque se haya retirado.
                val contribuyentesEnNivel = participantes.filter { it.aporteTotal == nivel }
                val porJugador = monto / contribuyentesEnNivel.size
                var resto = monto % contribuyentesEnNivel.size
                contribuyentesEnNivel.forEach { jugador ->
                    val devolucion = porJugador + if (resto-- > 0) 1 else 0
                    devoluciones[jugador.asiento] = devoluciones.getOrDefault(jugador.asiento, 0) + devolucion
                }
            } else {
                botes += Bote(monto, elegibles)
            }
            nivelAnterior = nivel
        }

        return DistribucionBotes(botes, devoluciones)
    }
}
