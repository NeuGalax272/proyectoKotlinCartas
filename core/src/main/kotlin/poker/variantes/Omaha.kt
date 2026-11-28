package poker.variantes

import poker.bots.ValoradorDeMano
import poker.bots.ValoradorOmaha
import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.ResultadoEvaluacion
import poker.evaluacion.combinaciones
import poker.modelo.Carta
import poker.modelo.Variante

class OmahaReglas(
    private val evaluador: EvaluadorAlto = EvaluadorAlto()
) : ReglasVariante, PlanDeReparto by RepartoComunitario(cartasPropias = 4) {

    override val variante: Variante = Variante.OMAHA
    override val valorador: ValoradorDeMano =
        ValoradorOmaha { propias, comunitarias -> evaluarJugador(propias, comunitarias) }

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion {
        require(cartasPropias.size >= 2) { "Omaha requiere al menos 2 cartas en mano" }
        require(comunitarias.size >= 3) { "Omaha requiere al menos 3 cartas comunitarias" }

        val paresPropias = combinaciones(cartasPropias, 2)
        val triosComunitarias = combinaciones(comunitarias, 3)

        var mejor: ResultadoEvaluacion? = null
        for (dos in paresPropias) {
            for (tres in triosComunitarias) {
                val eval = evaluador.evaluar(dos + tres)
                if (mejor == null || eval > mejor) {
                    mejor = eval
                }
            }
        }
        return mejor ?: error("No se pudo evaluar la mano de Omaha")
    }
}
