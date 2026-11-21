enum class Categoria(val nombre: String) {
    CARTA_ALTA("Carta alta"),
    PAR("Par"),
    DOBLE_PAR("Doble par"),
    TRIO("Trío"),
    ESCALERA("Escalera"),
    COLOR("Color"),
    FULL_HOUSE("Full house"),
    POKER("Póker"),
    ESCALERA_COLOR("Escalera de color"),
    ESCALERA_REAL("Escalera real")
}


class ResultadoEvaluacion(
    val categoria: Categoria,
    desempate: List<Int>,
    cartas: List<Carta>
) : Comparable<ResultadoEvaluacion> {

    val desempate: List<Int> = desempate.toList()
    val cartas: List<Carta> = cartas.toList()

    override fun compareTo(other: ResultadoEvaluacion): Int {
        val porCategoria = categoria.compareTo(other.categoria)
        if (porCategoria != 0) return porCategoria
        for (i in 0 until minOf(desempate.size, other.desempate.size)) {
            val porValor = desempate[i].compareTo(other.desempate[i])
            if (porValor != 0) return porValor
        }
        return 0
    }
}

interface EvaluadorDeManos {
    fun evaluar(cartas: List<Carta>): ResultadoEvaluacion
}

fun <T> combinaciones(elementos: List<T>, k: Int): List<List<T>> {
    if (k == 0) return listOf(emptyList())          
    if (elementos.size < k) return emptyList()     
    val primero = elementos.first()
    val resto = elementos.drop(1)
    val conPrimero = combinaciones(resto, k - 1).map { listOf(primero) + it }
    val sinPrimero = combinaciones(resto, k)
    return conPrimero + sinPrimero
}

class EvaluadorAlto : EvaluadorDeManos {

    override fun evaluar(cartas: List<Carta>): ResultadoEvaluacion {
        require(cartas.size >= 5) { "Se necesitan al menos 5 cartas" }
        return combinaciones(cartas, 5).map { evaluarCinco(it) }.max()
    }

    private fun evaluarCinco(cartas: List<Carta>): ResultadoEvaluacion {
        val grupos = cartas.groupingBy { it.rango.valor }.eachCount().entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }
                .thenByDescending { it.key })
        val patron = grupos.map { it.value }
        val valoresAgrupados = grupos.map { it.key }

        val esColor = cartas.map { it.palo }.toSet().size == 1
        val alturaEscalera = alturaDeEscalera(cartas)

        return when {
            alturaEscalera != null && esColor && alturaEscalera == 14 ->
                ResultadoEvaluacion(Categoria.ESCALERA_REAL, listOf(14), cartas)
            alturaEscalera != null && esColor ->
                ResultadoEvaluacion(Categoria.ESCALERA_COLOR, listOf(alturaEscalera), cartas)
            patron == listOf(4, 1) ->
                ResultadoEvaluacion(Categoria.POKER, valoresAgrupados, cartas)
            patron == listOf(3, 2) ->
                ResultadoEvaluacion(Categoria.FULL_HOUSE, valoresAgrupados, cartas)
            esColor ->
                ResultadoEvaluacion(Categoria.COLOR, valoresAgrupados, cartas)
            alturaEscalera != null ->
                ResultadoEvaluacion(Categoria.ESCALERA, listOf(alturaEscalera), cartas)
            patron == listOf(3, 1, 1) ->
                ResultadoEvaluacion(Categoria.TRIO, valoresAgrupados, cartas)
            patron == listOf(2, 2, 1) ->
                ResultadoEvaluacion(Categoria.DOBLE_PAR, valoresAgrupados, cartas)
            patron == listOf(2, 1, 1, 1) ->
                ResultadoEvaluacion(Categoria.PAR, valoresAgrupados, cartas)
            else ->
                ResultadoEvaluacion(Categoria.CARTA_ALTA, valoresAgrupados, cartas)
        }
    }
    private fun alturaDeEscalera(cartas: List<Carta>): Int? {
        val valores = cartas.map { it.rango.valor }.toSet()
        if (valores.size != 5) return null
        if (valores.max() - valores.min() == 4) return valores.max()
        if (valores == setOf(14, 2, 3, 4, 5)) return 5
        return null
    }
}