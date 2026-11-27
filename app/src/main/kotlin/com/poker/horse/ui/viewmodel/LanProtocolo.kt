package com.poker.horse.ui.viewmodel

import org.json.JSONArray
import org.json.JSONObject
import poker.modelo.Accion
import poker.modelo.Carta
import poker.modelo.EstadoJugador
import poker.modelo.Palo
import poker.modelo.Rango
import poker.modelo.Variante
import poker.motor.EstadoMano
import poker.motor.GanadorInfo

data class AccionDeSala(val jugadorId: String, val accion: Accion)

object LanProtocolo {
    fun mensajeIngreso(id: String, nombre: String) = JSONObject()
        .put("tipo", "ingreso")
        .put("id", id)
        .put("nombre", nombre)

    fun mensajeLista(pin: String, nombreSala: String, modo: ModoPartida, jugadores: List<JugadorSala>) = JSONObject()
        .put("tipo", "jugadores")
        .put("pin", pin)
        .put("nombreSala", nombreSala)
        .put("modo", modo.name)
        .put("jugadores", JSONArray().apply { jugadores.forEach { put(it.aJson()) } })

    fun mensajeInicio(pin: String, modo: ModoPartida, jugadores: List<JugadorSala>) = JSONObject()
        .put("tipo", "inicio")
        .put("pin", pin)
        .put("modo", modo.name)
        .put("jugadores", JSONArray().apply { jugadores.forEach { put(it.aJson()) } })

    fun mensajeAccion(jugadorId: String, accion: Accion) = JSONObject()
        .put("tipo", "accion")
        .put("jugadorId", jugadorId)
        .put("accion", accion.aJson())

    fun leerAccion(json: JSONObject): AccionDeSala = AccionDeSala(
        jugadorId = json.getString("jugadorId"),
        accion = json.getJSONObject("accion").aAccion()
    )

    fun mensajeEstado(remoto: EstadoRemoto): JSONObject = JSONObject()
        .put("tipo", "estado")
        .put("estado", remoto.estado.aJson(remoto.idsPorAsiento))
        .put("modo", remoto.modo.name)
        .put("manoEnVariante", remoto.manoEnVariante)
        .put("manosPorVariante", remoto.manosPorVariante)

    fun leerEstado(json: JSONObject): EstadoRemoto {
        val pares = mutableMapOf<Int, String>()
        val estadoJson = json.getJSONObject("estado")
        val estado = estadoJson.aEstado { asiento, id -> pares[asiento] = id }
        return EstadoRemoto(
            estado = estado,
            idsPorAsiento = pares,
            modo = ModoPartida.valueOf(json.getString("modo")),
            manoEnVariante = json.getInt("manoEnVariante"),
            manosPorVariante = json.getInt("manosPorVariante")
        )
    }

    fun leerJugadores(json: JSONObject): List<JugadorSala> {
        val array = json.getJSONArray("jugadores")
        return List(array.length()) { array.getJSONObject(it).aJugadorSala() }
    }

    private fun JugadorSala.aJson() = JSONObject()
        .put("id", id)
        .put("nombre", nombre)
        .put("anfitrion", anfitrion)

    private fun JSONObject.aJugadorSala() = JugadorSala(
        id = getString("id"),
        nombre = getString("nombre"),
        anfitrion = optBoolean("anfitrion", false)
    )

    private fun Accion.aJson(): JSONObject = when (this) {
        Accion.Fold -> JSONObject().put("tipo", "fold")
        Accion.Check -> JSONObject().put("tipo", "check")
        is Accion.Call -> JSONObject().put("tipo", "call").put("monto", monto)
        is Accion.Bet -> JSONObject().put("tipo", "bet").put("monto", monto)
        is Accion.Raise -> JSONObject().put("tipo", "raise").put("monto", monto)
        is Accion.AllIn -> JSONObject().put("tipo", "allin").put("monto", monto)
    }

    private fun JSONObject.aAccion(): Accion = when (getString("tipo")) {
        "fold" -> Accion.Fold
        "check" -> Accion.Check
        "call" -> Accion.Call(getInt("monto"))
        "bet" -> Accion.Bet(getInt("monto"))
        "raise" -> Accion.Raise(getInt("monto"))
        "allin" -> Accion.AllIn(getInt("monto"))
        else -> error("Acción de sala desconocida")
    }

    private fun EstadoMano.aJson(ids: Map<Int, String>): JSONObject = JSONObject()
        .put("variante", variante.name)
        .put("calle", calle.name)
        .put("jugadores", JSONArray().apply {
            jugadores.forEach { jugador ->
                put(JSONObject()
                    .put("id", ids[jugador.asiento] ?: "bot-${jugador.asiento}")
                    .put("asiento", jugador.asiento)
                    .put("nombre", jugador.nombre)
                    .put("pila", jugador.pila)
                    .put("aporteEnCalle", jugador.aporteEnCalle)
                    .put("aporteTotal", jugador.aporteTotal)
                    .put("cartas", jugador.cartas.aJson())
                    .put("cartasVisibles", JSONArray().apply { jugador.cartasVisibles.forEach { put(it) } })
                    .put("estado", when (jugador.estado) {
                        EstadoJugador.Activo -> "Activo"
                        EstadoJugador.Retirado -> "Retirado"
                        EstadoJugador.AllIn -> "AllIn"
                    })
                )
            }
        })
        .put("comunitarias", comunitarias.aJson())
        .put("mazoRestante", mazoRestante.aJson())
        .put("bote", bote)
        .put("dealerIndex", dealerIndex)
        .put("turnoActual", turnoActual ?: JSONObject.NULL)
        .put("apuestaMasAlta", apuestaMasAlta)
        .put("subidaMinima", subidaMinima)
        .put("ciegaGrande", ciegaGrande)
        .put("manoTerminada", manoTerminada)
        .put("ganadores", JSONArray().apply {
            ganadores.forEach { ganador ->
                put(JSONObject()
                    .put("asiento", ganador.jugador.asiento)
                    .put("montoGanado", ganador.montoGanado)
                    .put("descripcionMano", ganador.descripcionMano)
                    .put("esBaja", ganador.esBaja)
                )
            }
        })
        .put("historial", JSONArray().apply { historialAcciones.forEach { put(it) } })

    private fun JSONObject.aEstado(registrarId: (Int, String) -> Unit): EstadoMano {
        val jugadoresArray = getJSONArray("jugadores")
        val jugadores = List(jugadoresArray.length()) { index ->
            val j = jugadoresArray.getJSONObject(index)
            val asiento = j.getInt("asiento")
            registrarId(asiento, j.getString("id"))
            val estado = when (j.getString("estado")) {
                "Retirado" -> EstadoJugador.Retirado
                "AllIn" -> EstadoJugador.AllIn
                else -> EstadoJugador.Activo
            }
            poker.modelo.Jugador(
                asiento = asiento,
                nombre = j.getString("nombre"),
                pila = j.getInt("pila"),
                aporteEnCalle = j.getInt("aporteEnCalle"),
                aporteTotal = j.getInt("aporteTotal"),
                cartas = j.getJSONArray("cartas").aCartas(),
                cartasVisibles = j.getJSONArray("cartasVisibles").let { a -> List(a.length()) { a.getBoolean(it) } },
                estado = estado
            )
        }
        val porAsiento = jugadores.associateBy { it.asiento }
        val ganadoresArray = getJSONArray("ganadores")
        return EstadoMano(
            variante = Variante.valueOf(getString("variante")),
            calle = poker.modelo.Calle.valueOf(getString("calle")),
            jugadores = jugadores,
            comunitarias = getJSONArray("comunitarias").aCartas(),
            mazoRestante = getJSONArray("mazoRestante").aCartas(),
            bote = getInt("bote"),
            dealerIndex = getInt("dealerIndex"),
            turnoActual = if (isNull("turnoActual")) null else getInt("turnoActual"),
            apuestaMasAlta = getInt("apuestaMasAlta"),
            subidaMinima = getInt("subidaMinima"),
            ciegaGrande = getInt("ciegaGrande"),
            manoTerminada = getBoolean("manoTerminada"),
            ganadores = List(ganadoresArray.length()) { index ->
                val g = ganadoresArray.getJSONObject(index)
                GanadorInfo(
                    jugador = porAsiento.getValue(g.getInt("asiento")),
                    montoGanado = g.getInt("montoGanado"),
                    descripcionMano = g.getString("descripcionMano"),
                    esBaja = g.getBoolean("esBaja")
                )
            },
            historialAcciones = getJSONArray("historial").let { a -> List(a.length()) { a.getString(it) } }
        )
    }

    private fun List<Carta>.aJson() = JSONArray().apply {
        forEach { carta -> put(JSONObject().put("rango", carta.rango.name).put("palo", carta.palo.name)) }
    }

    private fun JSONArray.aCartas() = List(length()) { index ->
        val carta = getJSONObject(index)
        Carta(Rango.valueOf(carta.getString("rango")), Palo.valueOf(carta.getString("palo")))
    }
}
