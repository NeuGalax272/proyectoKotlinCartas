enum class Palo(val simbolo: String) {
    CORAZONES("♥"), DIAMANTES("♦"), TREBOLES("♣"), PICAS("♠")
}

enum class Rango(val valor: Int, val etiqueta: String) {
    DOS(2, "2"), TRES(3, "3"), CUATRO(4, "4"), CINCO(5, "5"),
    SEIS(6, "6"), SIETE(7, "7"), OCHO(8, "8"), NUEVE(9, "9"),
    DIEZ(10, "10"), JOTA(11, "J"), REINA(12, "Q"), REY(13, "K"), AS(14, "A")
}

data class Carta(val rango: Rango, val palo: Palo) {
    override fun toString(): String = "${rango.etiqueta}${palo.simbolo}"
}