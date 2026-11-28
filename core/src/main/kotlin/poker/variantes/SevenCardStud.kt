package poker.variantes

import poker.bots.ValoradorDeMano
import poker.bots.ValoradorStud
import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.modelo.Variante

class SevenCardStudReglas(
    private val evaluador: EvaluadorAlto = EvaluadorAlto()
) : ReglasVariante, PlanDeReparto by RepartoStud() {

    override val variante: Variante = Variante.SEVEN_CARD_STUD
    override val valorador: ValoradorDeMano =
        ValoradorStud { cartas -> evaluarJugador(cartas, emptyList()) }

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion = evaluador.evaluar(cartasPropias)
}
