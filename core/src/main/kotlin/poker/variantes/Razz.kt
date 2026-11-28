package poker.variantes

import poker.bots.ValoradorDeMano
import poker.bots.ValoradorRazz
import poker.evaluacion.EvaluadorBajoRazz
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.modelo.Variante

class RazzReglas(
    private val evaluador: EvaluadorBajoRazz = EvaluadorBajoRazz()
) : ReglasVariante, PlanDeReparto by RepartoStud() {

    override val variante: Variante = Variante.RAZZ
    override val valorador: ValoradorDeMano =
        ValoradorRazz { cartas -> evaluarJugador(cartas, emptyList()) }

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion = evaluador.evaluar(cartasPropias)
}
