package poker.bots

import poker.evaluacion.Categoria
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Rango

/** Funciones comunes que usan los valoradores. Todas son puras. */
internal object Heuristicas {

    /** Fuerza (0..1) de una mano alta ya hecha, según su categoría. */
    fun fuerzaPorCategoria(categoria: Categoria, tabla: Map<Categoria, Double> = TABLA_ESTANDAR): Double =
        tabla[categoria] ?: 0.0

    val TABLA_ESTANDAR: Map<Categoria, Double> = mapOf(
        Categoria.CARTA_ALTA to 0.10,
        Categoria.PAR to 0.35,
        Categoria.DOBLE_PAR to 0.60,
        Categoria.TRIO to 0.72,
        Categoria.ESCALERA to 0.82,
        Categoria.COLOR to 0.86,
        Categoria.FULL_HOUSE to 0.92,
        Categoria.POKER to 0.97,
        Categoria.ESCALERA_COLOR to 0.99,
        Categoria.ESCALERA_REAL to 1.00
    )

    /** En Omaha hay más combinaciones posibles, por eso las manos medias valen menos. */
    val TABLA_OMAHA: Map<Categoria, Double> = mapOf(
        Categoria.CARTA_ALTA to 0.05,
        Categoria.PAR to 0.15,
        Categoria.DOBLE_PAR to 0.35,
        Categoria.TRIO to 0.50,
        Categoria.ESCALERA to 0.72,
        Categoria.COLOR to 0.80,
        Categoria.FULL_HOUSE to 0.90,
        Categoria.POKER to 0.97,
        Categoria.ESCALERA_COLOR to 0.99,
        Categoria.ESCALERA_REAL to 1.00
    )

    /** Valor bajo de una carta: el As cuenta 1. */
    fun valorBajo(carta: Carta): Int = if (carta.rango == Rango.AS) 1 else carta.rango.valor

    /**
     * Fuerza de una mano baja a partir de sus valores bajos (el As = 1).
     * Promedio 2 (A-2-3) es lo mejor; promedio 10 o más no vale nada. Cada rango repetido resta 0.12.
     */
    fun fuerzaBaja(valores: List<Int>): Double {
        if (valores.isEmpty()) return 0.0
        val promedio = valores.average()
        val repetidas = valores.size - valores.toSet().size
        return (((10.0 - promedio) / 8.0) - 0.12 * repetidas).coerceIn(0.0, 1.0)
    }

    /** Elige hasta 5 cartas para una mano baja: primero la menor de cada rango distinto, luego repetidas. */
    fun mejoresBajas(cartas: List<Carta>): List<Int> {
        val porRango = cartas.map(::valorBajo).sorted()
        val distintas = porRango.distinct()
        val repetidas = porRango.toMutableList().also { lista -> distintas.forEach { lista.remove(it) } }
        return (distintas + repetidas).take(5)
    }

    fun tieneColorProyecto(propias: List<Carta>, todas: List<Carta>): Boolean =
        propias.map { it.palo }.toSet().any { palo -> todas.count { it.palo == palo } == 4 }

    fun jugadorDe(asiento: Int, jugadores: List<Jugador>): Jugador =
        jugadores.first { it.asiento == asiento }

    /** Bonificación por proyectos tempranos (3 o 4 cartas): color (3 del mismo palo) y escalera (3 en ventana de 5). */
    fun bonoProyectosTempranos(cartas: List<Carta>): Double {
        var bono = 0.0
        if (cartas.groupingBy { it.palo }.eachCount().values.any { it >= 3 }) bono += 0.12
        val valores = cartas.map { it.rango.valor }.distinct().sorted()
        if (valores.indices.any { i -> i + 2 < valores.size && valores[i + 2] - valores[i] <= 4 }) bono += 0.08
        return bono
    }
}
