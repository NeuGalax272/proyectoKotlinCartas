package poker.variantes

import poker.bots.ValoradorDeMano
import poker.bots.ValoradorHiLo
import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.EvaluadorBajoOcho
import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Carta
import poker.modelo.Jugador
import poker.modelo.Variante

class SevenCardHiLoReglas(
    private val evaluadorAlto: EvaluadorAlto = EvaluadorAlto(),
    private val evaluadorBajo: EvaluadorBajoOcho = EvaluadorBajoOcho()
) : ReglasVariante, PlanDeReparto by RepartoStud() {

    override val variante: Variante = Variante.SEVEN_CARD_HI_LO
    override val valorador: ValoradorDeMano = ValoradorHiLo(
        evaluarAlto = { cartas -> evaluarJugador(cartas, emptyList()) },
        evaluarBajo = { cartas -> evaluarJugadorBajo(cartas, emptyList()) }
    )

    override fun evaluarJugador(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion = evaluadorAlto.evaluar(cartasPropias)

    override fun evaluarJugadorBajo(
        cartasPropias: List<Carta>,
        comunitarias: List<Carta>
    ): ResultadoEvaluacion? {
        if (cartasPropias.size < 5) return null
        return evaluadorBajo.evaluar(cartasPropias)
    }

    override fun evaluarPorciones(
        contendientes: List<Jugador>,
        comunitarias: List<Carta>
    ): List<PorcionEvaluada> {
        val altas = contendientes.map { jugador ->
            EvaluacionJugador(jugador.asiento, evaluarJugador(jugador.cartas, comunitarias))
        }
        val bajas = contendientes.mapNotNull { jugador ->
            evaluarJugadorBajo(jugador.cartas, comunitarias)?.let { resultado ->
                EvaluacionJugador(jugador.asiento, resultado)
            }
        }

        if (bajas.isEmpty()) {
            return listOf(PorcionEvaluada("Mano Alta", 1, altas))
        }

        return listOf(
            PorcionEvaluada("Mano Alta", 1, altas, prioridadResto = 1),
            PorcionEvaluada("Mano Baja", 1, bajas, esBaja = true)
        )
    }
}
