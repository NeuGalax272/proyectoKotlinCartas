package poker.bots

import kotlin.random.Random
import poker.modelo.Accion
import poker.modelo.Variante
import poker.motor.EstadoMano

/**
 * Bot basado en reglas fijas: valora su mano (según la variante) y decide con umbrales del [PerfilBot].
 *
 * 1. Si es fuerte (o toca farol) y puede apostar/subir, lo hace.
 * 2. Si puede pasar gratis, pasa.
 * 3. Si hay que pagar, iguala solo si su fuerza supera las probabilidades del bote más el margen.
 * 4. En otro caso se retira.
 *
 * Siempre devuelve una acción contenida en `legales` (si no hay ninguna, propone Fold y el motor decide).
 */
class BotPorUmbrales(
    private val valoradorPara: (Variante) -> ValoradorDeMano,
    private val perfil: PerfilBot,
    private val random: Random = Random.Default
) : EstrategiaDeBot {

    override fun proponer(estado: EstadoMano, legales: Set<Accion>): Accion {
        if (legales.isEmpty()) return Accion.Fold
        val jugador = estado.jugadorEnTurno ?: return legales.first()
        val fuerza = fuerzaPercibida(estado, jugador.asiento)

        val agresiva = legales.firstOrNull { it is Accion.Raise }
            ?: legales.firstOrNull { it is Accion.Bet }
        val quiereAgredir = fuerza >= perfil.umbralSubir ||
            random.nextDouble() < perfil.frecuenciaFarol
        if (agresiva != null && quiereAgredir) return agresiva

        if (Accion.Check in legales) return Accion.Check

        val igualar = legales.filterIsInstance<Accion.Call>().firstOrNull()
            ?: legales.filterIsInstance<Accion.AllIn>().firstOrNull()
        if (igualar != null) {
            val costo = costoDe(igualar)
            val enMesa = estado.bote + estado.jugadores.sumOf { it.aporteEnCalle }
            val probabilidadesDelBote = costo.toDouble() / (enMesa + costo)
            if (fuerza >= probabilidadesDelBote + perfil.margenIgualar) return igualar
        }

        return if (Accion.Fold in legales) Accion.Fold else legales.first()
    }

    private fun fuerzaPercibida(estado: EstadoMano, asiento: Int): Double {
        val base = valoradorPara(estado.variante).fuerza(estado, asiento)
        val error = if (perfil.ruido == 0.0) 0.0 else random.nextDouble(-perfil.ruido, perfil.ruido)
        return (base + error).coerceIn(0.0, 1.0)
    }

    private fun costoDe(accion: Accion): Int = when (accion) {
        is Accion.Call -> accion.monto
        is Accion.AllIn -> accion.monto
        else -> 0
    }
}
