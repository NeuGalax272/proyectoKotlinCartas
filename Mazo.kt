import kotlin.random.Random

interface generarMazo{
    fun nuevoMazo(): List<Carta>   
}

fun mazoOrdenado(): List<Carta> = Palo.entries.flatMap { palo -> Rango.entries.map { rango -> Carta(rango, palo) }}

class MazoAleatorio(semilla: Long? = null) : generarMazo {
    private val random: Random = if (semilla == null) Random.Default else Random(semilla)

    override fun nuevoMazo(): List<Carta>  = mazoOrdenado().shuffled(random)
}

class MazoFijo(cartas: List<Carta>) : generarMazo {
    private val cartas = cartas.toList()

    override fun nuevoMazo(): List<Carta> = cartas
}