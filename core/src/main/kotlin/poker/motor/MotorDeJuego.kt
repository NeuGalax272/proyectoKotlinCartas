package poker.motor

import poker.evaluacion.ResultadoEvaluacion
import poker.modelo.Accion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.EstadoJugador
import poker.modelo.GeneradorDeMazo
import poker.modelo.Jugador
import poker.modelo.MazoAleatorio
import poker.modelo.Variante
import poker.variantes.HoldemReglas
import poker.variantes.OmahaReglas
import poker.variantes.RazzReglas
import poker.variantes.ReglasVariante
import poker.variantes.SevenCardHiLoReglas
import poker.variantes.SevenCardStudReglas

class MotorDeJuego(
    private val reglas: Map<Variante, ReglasVariante> = mapOf(
        Variante.HOLDEM to HoldemReglas(),
        Variante.OMAHA to OmahaReglas(),
        Variante.RAZZ to RazzReglas(),
        Variante.SEVEN_CARD_STUD to SevenCardStudReglas(),
        Variante.SEVEN_CARD_HI_LO to SevenCardHiLoReglas()
    )
) {

    fun reglasPara(variante: Variante): ReglasVariante =
        reglas[variante] ?: error("Variante no soportada: $variante")

    fun iniciarNuevaMano(
        variante: Variante,
        jugadoresActuales: List<Jugador>,
        dealerIndex: Int,
        generadorDeMazo: GeneradorDeMazo = MazoAleatorio(),
        ciegaGrande: Int = 20
    ): EstadoMano {
        require(jugadoresActuales.size >= 2) { "Se requieren al menos 2 jugadores" }
        val regla = reglasPara(variante)
        val mazo = generadorDeMazo.nuevoMazo().toMutableList()

        val ciegaChica = ciegaGrande / 2
        val n = jugadoresActuales.size
        val sbIndex = (dealerIndex + 1) % n
        val bbIndex = (dealerIndex + 2) % n

        // Reiniciar jugadores para la nueva mano (reseteando aportes y cartas)
        val preparados = jugadoresActuales.mapIndexed { index, j ->
            val aporteInicial: Int
            val pilaRestante: Int
            if (index == sbIndex) {
                aporteInicial = minOf(j.pila, ciegaChica)
                pilaRestante = j.pila - aporteInicial
            } else if (index == bbIndex) {
                aporteInicial = minOf(j.pila, ciegaGrande)
                pilaRestante = j.pila - aporteInicial
            } else {
                aporteInicial = 0
                pilaRestante = j.pila
            }
            val nuevoEstado = if (pilaRestante == 0 && aporteInicial > 0) {
                EstadoJugador.AllIn
            } else {
                EstadoJugador.Activo
            }

            j.copy(
                pila = pilaRestante,
                aporteEnCalle = aporteInicial,
                aporteTotal = aporteInicial,
                cartas = emptyList(),
                cartasVisibles = emptyList(),
                estado = nuevoEstado
            )
        }

        // Reparto inicial según la variante polimórfica
        val (repartidos, comunitarias) = regla.repartirInicial(mazo, preparados)
        val primeraCalle = regla.calles.first()

        val primerTurnoIndex = (dealerIndex + 3) % n
        val asientoPrimerTurno = repartidos[primerTurnoIndex].asiento

        return EstadoMano(
            variante = variante,
            calle = primeraCalle,
            jugadores = repartidos,
            comunitarias = comunitarias,
            mazoRestante = mazo.toList(),
            bote = 0,
            dealerIndex = dealerIndex,
            turnoActual = asientoPrimerTurno,
            apuestaMasAlta = ciegaGrande,
            subidaMinima = ciegaGrande,
            ciegaGrande = ciegaGrande,
            manoTerminada = false,
            ganadores = emptyList(),
            historialAcciones = listOf(
                "Inicia mano de ${variante.nombreCompleto}",
                "${repartidos[sbIndex].nombre} paga ciega chica ($ciegaChica)",
                "${repartidos[bbIndex].nombre} paga ciega grande ($ciegaGrande)"
            )
        )
    }

    fun accionesLegales(estado: EstadoMano): Set<Accion> {
        if (estado.manoTerminada || estado.turnoActual == null) return emptySet()

        val jugador = estado.jugadorEnTurno ?: return emptySet()
        if (jugador.estado != EstadoJugador.Activo || jugador.pila <= 0) return emptySet()

        val aIgualar = estado.apuestaMasAlta - jugador.aporteEnCalle
        val opciones = mutableSetOf<Accion>()

        if (aIgualar <= 0) {
            // No hay apuesta pendiente que igualar
            opciones.add(Accion.Check)
            if (jugador.pila > 0) {
                val montoApuesta = minOf(estado.subidaMinima, jugador.pila)
                opciones.add(Accion.Bet(montoApuesta))
                opciones.add(Accion.AllIn(jugador.pila))
            }
        } else {
            // Hay apuesta pendiente
            opciones.add(Accion.Fold)
            if (jugador.pila <= aIgualar) {
                // Solo puede ir All-In
                opciones.add(Accion.AllIn(jugador.pila))
            } else {
                opciones.add(Accion.Call(aIgualar))
                val montoSubida = aIgualar + estado.subidaMinima
                if (jugador.pila >= montoSubida) {
                    opciones.add(Accion.Raise(montoSubida))
                }
                opciones.add(Accion.AllIn(jugador.pila))
            }
        }
        return opciones
    }

    fun aplicar(estado: EstadoMano, accion: Accion): EstadoMano {
        val legales = accionesLegales(estado)
        require(legales.any { coincideAccion(it, accion) }) {
            "Acción no legal: $accion. Acciones legales: $legales"
        }

        val jugador = estado.jugadorEnTurno ?: error("No hay jugador en turno")
        val indexJugador = estado.jugadores.indexOfFirst { it.asiento == jugador.asiento }

        var nuevaPila = jugador.pila
        var nuevoAporteEnCalle = jugador.aporteEnCalle
        var nuevoAporteTotal = jugador.aporteTotal
        var nuevoEstadoJugador = jugador.estado
        var nuevaApuestaMasAlta = estado.apuestaMasAlta
        var nuevaSubidaMinima = estado.subidaMinima
        val mensajeHistorial: String

        when (accion) {
            is Accion.Fold -> {
                nuevoEstadoJugador = EstadoJugador.Retirado
                mensajeHistorial = "${jugador.nombre} se retira (Fold)"
            }
            is Accion.Check -> {
                mensajeHistorial = "${jugador.nombre} pasa (Check)"
            }
            is Accion.Call -> {
                val diferencia = minOf(accion.monto, jugador.pila)
                nuevaPila -= diferencia
                nuevoAporteEnCalle += diferencia
                nuevoAporteTotal += diferencia
                if (nuevaPila == 0) nuevoEstadoJugador = EstadoJugador.AllIn
                mensajeHistorial = "${jugador.nombre} iguala $diferencia"
            }
            is Accion.Bet -> {
                val apuesta = minOf(accion.monto, jugador.pila)
                nuevaPila -= apuesta
                nuevoAporteEnCalle += apuesta
                nuevoAporteTotal += apuesta
                nuevaApuestaMasAlta = nuevoAporteEnCalle
                nuevaSubidaMinima = maxOf(nuevaSubidaMinima, apuesta)
                if (nuevaPila == 0) nuevoEstadoJugador = EstadoJugador.AllIn
                mensajeHistorial = "${jugador.nombre} apuesta $apuesta"
            }
            is Accion.Raise -> {
                val subida = minOf(accion.monto, jugador.pila)
                nuevaPila -= subida
                nuevoAporteEnCalle += subida
                nuevoAporteTotal += subida
                val incremento = nuevoAporteEnCalle - estado.apuestaMasAlta
                nuevaApuestaMasAlta = nuevoAporteEnCalle
                if (incremento > 0) {
                    nuevaSubidaMinima = maxOf(nuevaSubidaMinima, incremento)
                }
                if (nuevaPila == 0) nuevoEstadoJugador = EstadoJugador.AllIn
                mensajeHistorial = "${jugador.nombre} sube a $nuevoAporteEnCalle"
            }
            is Accion.AllIn -> {
                val total = jugador.pila
                nuevaPila = 0
                nuevoAporteEnCalle += total
                nuevoAporteTotal += total
                nuevoEstadoJugador = EstadoJugador.AllIn
                if (nuevoAporteEnCalle > nuevaApuestaMasAlta) {
                    nuevaSubidaMinima = maxOf(nuevaSubidaMinima, nuevoAporteEnCalle - nuevaApuestaMasAlta)
                    nuevaApuestaMasAlta = nuevoAporteEnCalle
                }
                mensajeHistorial = "${jugador.nombre} va ¡ALL-IN! ($total)"
            }
        }

        val jugadorActualizado = jugador.copy(
            pila = nuevaPila,
            aporteEnCalle = nuevoAporteEnCalle,
            aporteTotal = nuevoAporteTotal,
            estado = nuevoEstadoJugador
        )

        val nuevosJugadores = estado.jugadores.toMutableList()
        nuevosJugadores[indexJugador] = jugadorActualizado

        val activos = nuevosJugadores.filter { it.estaEnJuego }
        // Si sólo queda uno activo, gana de inmediato
        if (activos.size == 1) {
            val ganador = activos.first()
            val totalBote = estado.bote + nuevosJugadores.sumOf { it.aporteEnCalle }
            val indexGanador = nuevosJugadores.indexOfFirst { it.asiento == ganador.asiento }
            nuevosJugadores[indexGanador] = ganador.copy(pila = ganador.pila + totalBote)

            return estado.copy(
                jugadores = nuevosJugadores.map { it.copy(aporteEnCalle = 0) },
                bote = 0,
                turnoActual = null,
                manoTerminada = true,
                ganadores = listOf(GanadorInfo(ganador, totalBote, "Todos los demás se retiraron")),
                historialAcciones = estado.historialAcciones + mensajeHistorial +
                        "¡${ganador.nombre} gana el bote de $totalBote por retiro!"
            )
        }

        // Buscar siguiente turno
        val siguienteAsiento = buscarSiguienteTurno(nuevosJugadores, estado.turnoActual!!, nuevaApuestaMasAlta)

        if (siguienteAsiento != null) {
            // Continúa la ronda de apuestas en esta misma calle
            return estado.copy(
                jugadores = nuevosJugadores,
                apuestaMasAlta = nuevaApuestaMasAlta,
                subidaMinima = nuevaSubidaMinima,
                turnoActual = siguienteAsiento,
                historialAcciones = estado.historialAcciones + mensajeHistorial
            )
        } else {
            // Ronda de apuestas completada: pasar al siguiente estado/calle
            return avanzarCalle(
                estado.copy(
                    jugadores = nuevosJugadores,
                    apuestaMasAlta = nuevaApuestaMasAlta,
                    subidaMinima = nuevaSubidaMinima,
                    historialAcciones = estado.historialAcciones + mensajeHistorial
                )
            )
        }
    }

    private fun coincideAccion(a: Accion, b: Accion): Boolean = when {
        a is Accion.Fold && b is Accion.Fold -> true
        a is Accion.Check && b is Accion.Check -> true
        a is Accion.Call && b is Accion.Call -> true
        a is Accion.Bet && b is Accion.Bet -> true
        a is Accion.Raise && b is Accion.Raise -> true
        a is Accion.AllIn && b is Accion.AllIn -> true
        else -> false
    }

    private fun buscarSiguienteTurno(
        jugadores: List<Jugador>,
        asientoActual: Int,
        apuestaMasAlta: Int
    ): Int? {
        val n = jugadores.size
        val actualIdx = jugadores.indexOfFirst { it.asiento == asientoActual }

        for (i in 1 until n) {
            val candidate = jugadores[(actualIdx + i) % n]
            if (candidate.estado == EstadoJugador.Activo && candidate.aporteEnCalle < apuestaMasAlta) {
                return candidate.asiento
            }
        }
        return null
    }

    private fun avanzarCalle(estado: EstadoMano): EstadoMano {
        val regla = reglasPara(estado.variante)
        val totalBote = estado.bote + estado.jugadores.sumOf { it.aporteEnCalle }
        val jugadoresLimpio = estado.jugadores.map { it.copy(aporteEnCalle = 0) }

        val calles = regla.calles
        val indiceCalleActual = calles.indexOf(estado.calle)

        if (indiceCalleActual >= calles.lastIndex) {
            // Llegamos al Showdown
            return resolverShowdown(estado.copy(jugadores = jugadoresLimpio, bote = totalBote))
        }

        // Siguiente calle
        val siguienteCalle = calles[indiceCalleActual + 1]
        val mazoMutable = estado.mazoRestante.toMutableList()
        val (jugadoresRepartidos, nuevasComunitarias) = regla.repartirCalle(
            calle = siguienteCalle,
            mazo = mazoMutable,
            jugadores = jugadoresLimpio,
            comunitarias = estado.comunitarias
        )

        // Primer jugador activo a la izquierda del dealer
        val n = jugadoresRepartidos.size
        var primerTurno: Int? = null
        for (i in 1..n) {
            val j = jugadoresRepartidos[(estado.dealerIndex + i) % n]
            if (j.estado == EstadoJugador.Activo) {
                primerTurno = j.asiento
                break
            }
        }

        return estado.copy(
            calle = siguienteCalle,
            jugadores = jugadoresRepartidos,
            comunitarias = nuevasComunitarias,
            mazoRestante = mazoMutable.toList(),
            bote = totalBote,
            turnoActual = primerTurno,
            apuestaMasAlta = 0,
            subidaMinima = estado.ciegaGrande,
            historialAcciones = estado.historialAcciones + "--- Entrando a ${siguienteCalle.etiqueta} ---"
        )
    }

    private fun resolverShowdown(estado: EstadoMano): EstadoMano {
        val regla = reglasPara(estado.variante)
        val contendientes = estado.jugadores.filter { it.estaEnJuego }
        val totalBote = estado.bote
        val ganadores = mutableListOf<GanadorInfo>()
        val jugadoresActualizados = estado.jugadores.toMutableList()

        if (estado.variante == Variante.SEVEN_CARD_HI_LO) {
            // Split Pot Hi-Lo
            val evalAltas = contendientes.map { j ->
                Pair(j, regla.evaluarJugador(j.cartas, estado.comunitarias))
            }
            val mejorAlta = evalAltas.maxByOrNull { it.second }!!

            val evalBajas = contendientes.mapNotNull { j ->
                regla.evaluarJugadorBajo(j.cartas, estado.comunitarias)?.let { Pair(j, it) }
            }

            if (evalBajas.isEmpty()) {
                // Ninguna mano baja califica (ninguna <= 8) -> Alta se lleva todo (Scoop)
                val idx = jugadoresActualizados.indexOfFirst { it.asiento == mejorAlta.first.asiento }
                jugadoresActualizados[idx] = jugadoresActualizados[idx].copy(
                    pila = jugadoresActualizados[idx].pila + totalBote
                )
                ganadores.add(
                    GanadorInfo(
                        mejorAlta.first,
                        totalBote,
                        "Mano Alta (${mejorAlta.second.categoria.nombre}) - No hubo baja calificada (Scoop)"
                    )
                )
            } else {
                val mejorBaja = evalBajas.maxByOrNull { it.second }!!
                val mitad = totalBote / 2
                val resto = totalBote - mitad

                val idxAlta = jugadoresActualizados.indexOfFirst { it.asiento == mejorAlta.first.asiento }
                jugadoresActualizados[idxAlta] = jugadoresActualizados[idxAlta].copy(
                    pila = jugadoresActualizados[idxAlta].pila + mitad
                )
                ganadores.add(GanadorInfo(mejorAlta.first, mitad, "Mano Alta (${mejorAlta.second.categoria.nombre})"))

                val idxBaja = jugadoresActualizados.indexOfFirst { it.asiento == mejorBaja.first.asiento }
                jugadoresActualizados[idxBaja] = jugadoresActualizados[idxBaja].copy(
                    pila = jugadoresActualizados[idxBaja].pila + resto
                )
                ganadores.add(GanadorInfo(mejorBaja.first, resto, "Mano Baja (Calificada 8-or-Better)", esBaja = true))
            }
        } else {
            // Bote normal
            val evaluaciones = contendientes.map { j ->
                Pair(j, regla.evaluarJugador(j.cartas, estado.comunitarias))
            }
            val maxEval = evaluaciones.maxByOrNull { it.second }!!.second
            val ganadoresMano = evaluaciones.filter { it.second.compareTo(maxEval) == 0 }

            val premioPorGanador = totalBote / ganadoresMano.size
            for (g in ganadoresMano) {
                val idx = jugadoresActualizados.indexOfFirst { it.asiento == g.first.asiento }
                jugadoresActualizados[idx] = jugadoresActualizados[idx].copy(
                    pila = jugadoresActualizados[idx].pila + premioPorGanador
                )
                ganadores.add(
                    GanadorInfo(
                        g.first,
                        premioPorGanador,
                        "${g.second.categoria.nombre} (${g.second.desempate.joinToString()})"
                    )
                )
            }
        }

        val resumen = ganadores.joinToString(" y ") { "${it.jugador.nombre} ($${it.montoGanado}: ${it.descripcionMano})" }

        return estado.copy(
            jugadores = jugadoresActualizados,
            bote = 0,
            turnoActual = null,
            manoTerminada = true,
            calle = Calle.SHOWDOWN,
            ganadores = ganadores,
            historialAcciones = estado.historialAcciones + "¡Showdown finalizado! Ganador(es): $resumen"
        )
    }

    // Inteligencia artificial simple para bots
    fun decidirAccionBot(estado: EstadoMano): Accion {
        val legales = accionesLegales(estado)
        if (legales.isEmpty()) return Accion.Fold

        if (legales.contains(Accion.Check)) {
            // 70% pasar, 30% apostar si es legal
            val bet = legales.filterIsInstance<Accion.Bet>().firstOrNull()
            return if (bet != null && (1..10).random() > 7) bet else Accion.Check
        }

        val call = legales.filterIsInstance<Accion.Call>().firstOrNull()
        if (call != null) {
            val jugador = estado.jugadorEnTurno ?: return Accion.Fold
            // Si el monto a igualar es menor o igual al 25% de su pila, suele igualar
            if (call.monto <= jugador.pila * 0.35 || call.monto <= estado.ciegaGrande * 2) {
                return call
            }
            // En ocasiones sube si tiene fichas
            val raise = legales.filterIsInstance<Accion.Raise>().firstOrNull()
            if (raise != null && (1..10).random() == 1) {
                return raise
            }
            return Accion.Fold
        }

        val allIn = legales.filterIsInstance<Accion.AllIn>().firstOrNull()
        if (allIn != null && (1..10).random() > 6) {
            return allIn
        }

        return legales.firstOrNull { it is Accion.Fold } ?: legales.first()
    }
}
