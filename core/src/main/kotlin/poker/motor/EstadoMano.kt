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
    init {
        require(jugadores.size >= 2) { "Una mano requiere al menos dos jugadores" }
        require(jugadores.map { it.asiento }.toSet().size == jugadores.size) {
            "Los asientos de los jugadores deben ser únicos"
        }
        require(dealerIndex in jugadores.indices) { "El índice del dealer no corresponde a un jugador" }
        require(turnoActual == null || jugadores.any { it.asiento == turnoActual }) {
            "El turno debe corresponder a un asiento existente"
        }
        require(bote >= 0) { "El bote no puede ser negativo" }
        require(apuestaMasAlta >= 0) { "La apuesta más alta no puede ser negativa" }
        require(subidaMinima > 0) { "La subida mínima debe ser positiva" }
        require(ciegaGrande > 0) { "La ciega grande debe ser positiva" }
        require(jugadores.all { it.aporteTotal >= it.aporteEnCalle }) {
            "El aporte total de cada jugador debe cubrir su aporte en la calle actual"
        }
        require(jugadores.all { it.cartasVisibles.isEmpty() || it.cartasVisibles.size == it.cartas.size }) {
            "La visibilidad debe estar definida para cada carta del jugador"
        }
        val cartasEnEstado = jugadores.flatMap { it.cartas } + comunitarias + mazoRestante
        require(cartasEnEstado.toSet().size == cartasEnEstado.size) {
            "Una carta no puede aparecer más de una vez en el estado de la mano"
        }
        if (!manoTerminada) {
            require(bote + jugadores.sumOf { it.aporteEnCalle } == jugadores.sumOf { it.aporteTotal }) {
                "El bote y los aportes de la calle deben coincidir con los aportes acumulados"
            }
            require(apuestaMasAlta >= (jugadores.maxOfOrNull { it.aporteEnCalle } ?: 0)) {
                "La apuesta más alta no puede ser menor que un aporte en la calle"
            }
        }
    }

    val jugadorEnTurno: Jugador?
        get() = turnoActual?.let { asiento -> jugadores.firstOrNull { it.asiento == asiento } }

    val jugadoresEnJuego: List<Jugador>
        get() = jugadores.filter { it.estaEnJuego }

    val totalFichasEnMesa: Int
        get() = bote + jugadores.sumOf { it.pila + it.aporteEnCalle }
}
