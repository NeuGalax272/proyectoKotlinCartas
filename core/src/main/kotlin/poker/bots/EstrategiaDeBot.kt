package poker.bots

import poker.modelo.Accion
import poker.motor.EstadoMano

/** Propone una acción para el jugador en turno. El motor la valida contra las acciones legales. */
interface EstrategiaDeBot {
    fun proponer(estado: EstadoMano, legales: Set<Accion>): Accion
}

/** Estima qué tan buena es la mano de un asiento en la variante en curso: 0.0 (débil) a 1.0 (muy fuerte). */
interface ValoradorDeMano {
    fun fuerza(estado: EstadoMano, asiento: Int): Double
}
