package poker.motor

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Accion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

data class GanadorInfo(
    val jugador: Jugador,
    val montoGanado: Int,
    val descripcionMano: String,
    val esBaja: Boolean = false
)

sealed interface ResultadoAccion {
    data class Aplicada(val estado: EstadoMano) : ResultadoAccion
    data class Rechazada(
        val motivo: String,
        val accionesLegales: Set<Accion>
    ) : ResultadoAccion
}

data class EstadoMano(
    val variante: Variante,
    val calle: Calle,
    val jugadores: List<Jugador>,
    val comunitarias: List<Carta> = emptyList(),
    val mazoRestante: List<Carta> = emptyList(),
    val bote: Int = 0,
    val dealerIndex: Int = 0,
    val turnoActual: Int? = null, // Asiento del jugador al que le toca actuar
    val apuestaMasAlta: Int = 0,
    val subidaMinima: Int = 20,
    val ciegaGrande: Int = 20,
    val manoTerminada: Boolean = false,
    val ganadores: List<GanadorInfo> = emptyList(),
    val historialAcciones: List<String> = emptyList()
) {
    val jugadorEnTurno: Jugador?
        get() = turnoActual?.let { asiento -> jugadores.firstOrNull { it.asiento == asiento } }

    val jugadoresEnJuego: List<Jugador>
        get() = jugadores.filter { it.estaEnJuego }

    val totalFichasEnMesa: Int
        get() = bote + jugadores.sumOf { it.pila + it.aporteEnCalle }
}
