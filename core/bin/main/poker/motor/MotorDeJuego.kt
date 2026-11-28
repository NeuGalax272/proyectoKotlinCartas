package poker.motor

import poker.modelo.Accion
import poker.modelo.Calle
import poker.modelo.Carta
import poker.modelo.EstadoJugador
import poker.modelo.GeneradorDeMazo
import poker.modelo.Jugador
import poker.modelo.MazoAleatorio
import poker.modelo.Variante
import poker.variantes.ReglasVariante
import poker.variantes.PorcionEvaluada

class MotorDeJuego(
    private val reglas: Map<Variante, ReglasVariante>,
    private val calculadorDeBotes: CalculadorDeBotes = CalculadorDeBotes()
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
        val mazo = generadorDeMazo.nuevoMazo()

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
        val repartoInicial = regla.repartirInicial(mazo, preparados)
        val repartidos = repartoInicial.jugadores
        val comunitarias = repartoInicial.comunitarias
        val primeraCalle = regla.calles.first()

        val primerTurnoIndex = (dealerIndex + 3) % n
        val asientoPrimerTurno = repartidos[primerTurnoIndex].asiento

        return EstadoMano(
            variante = variante,
            calle = primeraCalle,
            jugadores = repartidos,
            comunitarias = comunitarias,
            mazoRestante = repartoInicial.mazoRestante,
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

    fun aplicar(estado: EstadoMano, accion: Accion): ResultadoAccion {
        val legales = accionesLegales(estado)
        if (legales.none { coincideAccion(it, accion) }) {
            return ResultadoAccion.Rechazada(
                motivo = "Acción no legal: $accion",
                accionesLegales = legales
            )
        }
        return ResultadoAccion.Aplicada(aplicarAccionLegal(estado, accion))
    }

    private fun aplicarAccionLegal(estado: EstadoMano, accion: Accion): EstadoMano {
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
        val siguienteAsiento = buscarSiguienteTurno(estado, nuevosJugadores, estado.turnoActual!!, nuevaApuestaMasAlta)

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
        a is Accion.Call && b is Accion.Call -> a.monto == b.monto
        a is Accion.Bet && b is Accion.Bet -> a.monto == b.monto
        a is Accion.Raise && b is Accion.Raise -> a.monto == b.monto
        a is Accion.AllIn && b is Accion.AllIn -> a.monto == b.monto
        else -> false
    }

    private fun buscarSiguienteTurno(
        estado: EstadoMano,
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

        // Si nadie ha apostado ni subido en esta calle, los jugadores que van después
        // en el orden de acción todavía no han actuado (pasar o la opción de la ciega grande)
        if (apuestaMasAlta == apuestaInicialDeLaCalle(estado)) {
            val orden = ordenDeAccionEnCalle(estado)
            return orden
                .drop(orden.indexOf(asientoActual) + 1)
                .firstOrNull { asiento -> jugadores.first { it.asiento == asiento }.estado == EstadoJugador.Activo }
        }
        return null
    }

    // Apuesta con la que arranca la calle: la ciega grande en la primera, cero en las demás
    private fun apuestaInicialDeLaCalle(estado: EstadoMano): Int =
        if (estado.calle == reglasPara(estado.variante).calles.first()) estado.ciegaGrande else 0

    // Asientos en el orden en que actúan en la calle actual
    private fun ordenDeAccionEnCalle(estado: EstadoMano): List<Int> {
        val n = estado.jugadores.size
        val primerIndice = if (estado.calle == reglasPara(estado.variante).calles.first()) {
            estado.dealerIndex + 3
        } else {
            estado.dealerIndex + 1
        }
        return (0 until n).map { estado.jugadores[(primerIndice + it) % n].asiento }
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
        val reparto = regla.repartirCalle(
            calle = siguienteCalle,
            mazo = estado.mazoRestante,
            jugadores = jugadoresLimpio,
            comunitarias = estado.comunitarias
        )
        val jugadoresRepartidos = reparto.jugadores
        val nuevasComunitarias = reparto.comunitarias

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

        val nuevoEstado = estado.copy(
            calle = siguienteCalle,
            jugadores = jugadoresRepartidos,
            comunitarias = nuevasComunitarias,
            mazoRestante = reparto.mazoRestante,
            bote = totalBote,
            turnoActual = primerTurno,
            apuestaMasAlta = 0,
            subidaMinima = estado.ciegaGrande,
            historialAcciones = estado.historialAcciones + "--- Entrando a ${siguienteCalle.etiqueta} ---"
        )
        // Si todos los jugadores que siguen en la mano están all-in, no hay
        // más decisiones posibles: repartir las calles restantes y resolver.
        return if (primerTurno == null) avanzarCalle(nuevoEstado) else nuevoEstado
    }

    private fun resolverShowdown(estado: EstadoMano): EstadoMano {
        val regla = reglasPara(estado.variante)
        val contendientes = estado.jugadores.filter { it.estaEnJuego }
        val ganadores = mutableListOf<GanadorInfo>()
        val jugadoresActualizados = estado.jugadores.toMutableList()
        val distribucion = calculadorDeBotes.calcular(estado.jugadores)
        distribucion.devoluciones.forEach { (asiento, monto) ->
            val idx = jugadoresActualizados.indexOfFirst { it.asiento == asiento }
            check(idx >= 0) { "Devolución para asiento inexistente: $asiento" }
            val jugador = jugadoresActualizados[idx]
            jugadoresActualizados[idx] = jugador.copy(pila = jugador.pila + monto)
        }
        val ordenAsientos = ordenDesdeDealer(estado)

        distribucion.botes.forEachIndexed { indiceBote, bote ->
            val elegibles = contendientes.filter { it.asiento in bote.asientosElegibles }
            val porciones = regla.evaluarPorciones(elegibles, estado.comunitarias)
            require(porciones.isNotEmpty()) { "Las reglas deben devolver al menos una porción evaluada" }
            require(porciones.sumOf { it.peso } > 0) { "El peso total de porciones debe ser positivo" }
            val pesoTotal = porciones.sumOf { it.peso }
            val montosPorcion = repartirEntrePorciones(bote.monto, porciones, pesoTotal)

            porciones.forEachIndexed { indicePorcion, porcion ->
                distribuirPorcion(
                    porcion = porcion,
                    monto = montosPorcion[indicePorcion],
                    ordenAsientos = ordenAsientos,
                    jugadoresActualizados = jugadoresActualizados,
                    ganadores = ganadores,
                    etiquetaBote = if (distribucion.botes.size > 1) "Bote ${indiceBote + 1} · " else ""
                )
            }
        }

        val resumen = ganadores.joinToString(" y ") { "${it.jugador.nombre} (+$${it.montoGanado}: ${it.descripcionMano})" }

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

    private fun distribuirPorcion(
        porcion: PorcionEvaluada,
        monto: Int,
        ordenAsientos: List<Int>,
        jugadoresActualizados: MutableList<Jugador>,
        ganadores: MutableList<GanadorInfo>,
        etiquetaBote: String
    ) {
        val mejor = porcion.evaluaciones.maxOf { it.resultado }
        val empatados = porcion.evaluaciones.filter { it.resultado.compareTo(mejor) == 0 }
        val ordenados = empatados.sortedBy { ordenAsientos.indexOf(it.asiento) }
        val premioBase = monto / ordenados.size
        var fichasRestantes = monto % ordenados.size

        ordenados.forEach { evaluacion ->
            val premio = premioBase + if (fichasRestantes-- > 0) 1 else 0
            val jugadorIndex = jugadoresActualizados.indexOfFirst { it.asiento == evaluacion.asiento }
            check(jugadorIndex >= 0) { "Evaluación de asiento inexistente: ${evaluacion.asiento}" }
            val jugador = jugadoresActualizados[jugadorIndex]
            jugadoresActualizados[jugadorIndex] = jugador.copy(pila = jugador.pila + premio)
            ganadores.add(
                GanadorInfo(
                    jugador = jugador,
                    montoGanado = premio,
                    descripcionMano = "$etiquetaBote${porcion.nombre} (${evaluacion.resultado.categoria.nombre}: ${evaluacion.resultado.desempate.joinToString()})",
                    esBaja = porcion.esBaja
                )
            )
        }
    }

    private fun repartirEntrePorciones(
        total: Int,
        porciones: List<PorcionEvaluada>,
        pesoTotal: Int
    ): List<Int> {
        val montos = porciones.map { (total.toLong() * it.peso / pesoTotal).toInt() }.toMutableList()
        var restantes = total - montos.sum()
        val ordenResto = porciones.indices.sortedWith(
            compareByDescending<Int> { (total.toLong() * porciones[it].peso) % pesoTotal }
                .thenByDescending { porciones[it].prioridadResto }
                .thenBy { it }
        )
        var cursor = 0
        while (restantes > 0) {
            montos[ordenResto[cursor % ordenResto.size]]++
            restantes--
            cursor++
        }
        return montos
    }

    private fun ordenDesdeDealer(estado: EstadoMano): List<Int> {
        val jugadores = estado.jugadores
        if (jugadores.isEmpty()) return emptyList()
        return (1..jugadores.size).map { desplazamiento ->
            jugadores[(estado.dealerIndex + desplazamiento) % jugadores.size].asiento
        }
    }
}
