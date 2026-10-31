enum class Palo(val simbolo: String) {
   CORAZONES("♥"),
    DIAMANTES("♦"),
    TREBOLES("♣"),
    PICAS("♠")
}

data class Carta(val valor: Int, val palo: Palo)

fun main() {
    print("Con cuantos mazos se va a jugar? (Se recomienda de 1 a 4)"  )
    val numMazos = readln().toInt()
    val baraja = barajarBaraja(crearBaraja(numMazos))
    val reparto = repartir(baraja)
    println("Jugador: ${reparto.jugador.map { nombreCarta(it) }}")
    println("Máquina: ${reparto.maquina.map { nombreCarta(it) }}")
    println("Mesa: ${reparto.mesa.map { nombreCarta(it) }}") 



}

fun nombreCarta(carta: Carta): String {
    val nombreValor = when (carta.valor) {
        1 -> "As"
        11 -> "Jota"
        12 -> "Reina"
        13 -> "Rey"
        else -> carta.valor.toString()
    }
    return "$nombreValor de ${carta.palo.simbolo}"
}

fun crearBaraja(numMazos: Int): List<Carta> {
    val baraja = mutableListOf<Carta>()
    for (mazo in 1..numMazos) {
        for (palo in Palo.values()) {
            for (valor in 1..13) {
                baraja.add(Carta(valor, palo))
            }
        }
    }
    return baraja
}

fun barajarBaraja(baraja: List<Carta>): List<Carta> {
    return baraja.shuffled()
}

data class Reparto(val jugador: List<Carta>, val maquina: List<Carta>, val mesa: List<Carta>)

fun repartir(baraja: List<Carta>): Reparto {
    val jugador = baraja.take(2)
    val maquina = baraja.drop(2).take(2)
    val mesa = baraja.drop(4).take(5)
    return Reparto(jugador, maquina, mesa)
}

fun contarValores(cartas: List<Carta>): Map<Int, Int> {
    return cartas.groupingBy { it.valor }.eachCount()
}

fun buscarPar(cartas: List<Carta>): Boolean {
    val valores = contarValores(cartas)
    return valores.any { it.value >= 2 }
}

enum class Jugada {
    CARTA_ALTA, PAR, DOBLE_PAR, TRIO, ESCALERA,
    COLOR, FULL_HOUSE, POKER, ESCALERA_COLOR, ESCALERA_REAL
}

fun hayDoblePar(cartas: List<Carta>): Boolean {
    val valores = contarValores(cartas)
    return valores.filter { it.value >= 2 }.size >= 2
}

fun hayTrio(cartas: List<Carta>): Boolean {
    val valores = contarValores(cartas)
    return valores.any { it.value >= 3 }
}


