package poker.variantes

import poker.bots.ValoradorDeMano
import poker.bots.ValoradorHoldem
import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.modelo.Variante

class HoldemReglas(
    private val evaluador: EvaluadorAlto = EvaluadorAlto()
) : ReglasVariante, PlanDeReparto by RepartoComunitario(cartasPropias = 2) {

    override val variante: Variante = Variante.HOLDEM
    override val valorador: ValoradorDeMano =
        ValoradorHoldem { propias, comunitarias -> evaluarJugador(propias, comunitarias) }

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion = evaluador.evaluar(cartasPropias + comunitarias)
}
