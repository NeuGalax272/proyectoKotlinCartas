package poker.modelo

sealed interface EstadoJugador {
    data object Activo : EstadoJugador
    data object Retirado : EstadoJugador
    data object AllIn : EstadoJugador
}

data class Jugador(
    val asiento: Int,
    val nombre: String,
    val pila: Int,
    val aporteEnCalle: Int = 0,
    val aporteTotal: Int = 0,
    val cartas: List<Carta> = emptyList(),
    val cartasVisibles: List<Boolean> = emptyList(), // true = boca arriba, false = boca abajo
    val estado: EstadoJugador = EstadoJugador.Activo,
    val esHumano: Boolean = false
) {
    init {
        require(nombre.isNotBlank()) { "El nombre no puede estar en blanco" }
        require(pila >= 0) { "La pila no puede ser negativa: $pila" }
        require(aporteEnCalle >= 0) { "El aporte en calle no puede ser negativo: $aporteEnCalle" }
        require(aporteTotal >= aporteEnCalle) {
            "El aporte total ($aporteTotal) debe ser >= aporte en calle ($aporteEnCalle)"
        }
    }

    val estaEnJuego: Boolean
        get() = estado == EstadoJugador.Activo || estado == EstadoJugador.AllIn

    fun cartasBocaArriba(): List<Carta> =
        cartas.filterIndexed { index, _ -> cartasVisibles.getOrElse(index) { true } }
}
