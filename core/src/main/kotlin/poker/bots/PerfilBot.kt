package poker.bots

/**
 * Personalidad de un bot. Son parámetros fijos elegidos a mano (no se aprenden de datos).
 *
 * - umbralSubir: fuerza mínima (0..1) de la mano para apostar o subir.
 * - margenIgualar: exigencia extra sobre las probabilidades del bote para igualar.
 * - frecuenciaFarol: probabilidad de apostar o subir aunque la mano sea débil.
 * - ruido: error máximo al valorar la mano (0 = valora perfecto).
 */
data class PerfilBot(
    val nombre: String,
    val umbralSubir: Double,
    val margenIgualar: Double,
    val frecuenciaFarol: Double,
    val ruido: Double
) {
    init {
        require(nombre.isNotBlank()) { "El perfil necesita nombre" }
        require(umbralSubir in 0.0..1.0) { "umbralSubir fuera de rango: $umbralSubir" }
        require(margenIgualar in 0.0..0.5) { "margenIgualar fuera de rango: $margenIgualar" }
        require(frecuenciaFarol in 0.0..1.0) { "frecuenciaFarol fuera de rango: $frecuenciaFarol" }
        require(ruido in 0.0..0.5) { "ruido fuera de rango: $ruido" }
    }

    companion object {
        val CONSERVADOR = PerfilBot("Conservador", 0.80, 0.15, 0.02, 0.05)
        val EQUILIBRADO = PerfilBot("Equilibrado", 0.70, 0.08, 0.08, 0.10)
        val AGRESIVO = PerfilBot("Agresivo", 0.60, 0.00, 0.20, 0.10)

        val todos: List<PerfilBot> = listOf(CONSERVADOR, EQUILIBRADO, AGRESIVO)
    }
}
