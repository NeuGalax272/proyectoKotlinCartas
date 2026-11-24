package poker.modelo

import kotlin.random.Random

interface GeneradorDeMazo {
    fun nuevoMazo(): List<Carta>
}

fun mazoOrdenado(): List<Carta> =
    Palo.entries.flatMap { palo ->
        Rango.entries.map { rango -> Carta(rango, palo) }
    }

class MazoAleatorio(semilla: Long? = null) : GeneradorDeMazo {
    private val random: Random = if (semilla == null) Random.Default else Random(semilla)

    override fun nuevoMazo(): List<Carta> = mazoOrdenado().shuffled(random)
}

class MazoFijo(cartas: List<Carta>) : GeneradorDeMazo {
    private val cartas = cartas.toList().also { mazo ->
        require(mazo.size == TOTAL_CARTAS) { "El mazo debe tener $TOTAL_CARTAS cartas" }
        require(mazo.toSet().size == TOTAL_CARTAS) { "El mazo no puede tener cartas repetidas" }
    }

    override fun nuevoMazo(): List<Carta> = cartas.toList()
}

private const val TOTAL_CARTAS = 52
