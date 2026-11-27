package com.poker.horse.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import poker.modelo.Accion
import kotlin.coroutines.resume

class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("perfil_poker", Context.MODE_PRIVATE)
    private val nsdManager = application.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val _uiState = MutableStateFlow(
        InicioUiState(
            pantalla = if (prefs.getString(CLAVE_NOMBRE, null).isNullOrBlank()) {
                PantallaInicio.REGISTRO
            } else PantallaInicio.MENU,
            nombreUsuario = prefs.getString(CLAVE_NOMBRE, "").orEmpty()
        )
    )
    val uiState: StateFlow<InicioUiState> = _uiState.asStateFlow()

    private val _accionesRemotas = MutableSharedFlow<AccionDeSala>(extraBufferCapacity = 32)
    val accionesRemotas = _accionesRemotas.asSharedFlow()
    private val _jugadoresDesconectados = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val jugadoresDesconectados = _jugadoresDesconectados.asSharedFlow()

    private val jugadoresHumanos = mutableListOf<JugadorSala>()
    private val clientes = ConcurrentHashMap<String, ClienteLan>()
    private val socketsAbiertos = ConcurrentHashMap.newKeySet<Socket>()
    @Volatile private var socketServidor: ServerSocket? = null
    @Volatile private var clienteLan: ClienteLan? = null
    @Volatile private var listenerRegistro: NsdManager.RegistrationListener? = null
    private var bucleServidor: Job? = null
    @Volatile private var salaIniciada = false

    fun registrarNombre(nombre: String) {
        val limpio = nombre.trim().take(24)
        if (limpio.isEmpty()) return
        prefs.edit().putString(CLAVE_NOMBRE, limpio).apply()
        _uiState.update { it.copy(nombreUsuario = limpio, pantalla = PantallaInicio.MENU, mensaje = null) }
    }

    fun seleccionarModo(modo: ModoPartida) {
        _uiState.update { it.copy(modoSeleccionado = modo) }
    }

    fun iniciarSolo() {
        detenerRed()
        val id = UUID.randomUUID().toString()
        val yo = JugadorSala(id = id, nombre = _uiState.value.nombreUsuario, anfitrion = true)
        _uiState.update {
            it.copy(
                pantalla = PantallaInicio.PARTIDA,
                esAnfitrion = true,
                idJugadorLocal = id,
                jugadoresSala = listOf(yo),
                pin = "",
                nombreSala = "",
                mensaje = null,
                partidaRemota = null
            )
        }
    }

    fun abrirIngreso() {
        _uiState.update { it.copy(pantalla = PantallaInicio.ESPERA_CODIGO, mensaje = null, conectando = false) }
    }

    fun crearSala() {
        detenerRed()
        val actual = _uiState.value
        val pin = (100000..999999).random().toString()
        val id = UUID.randomUUID().toString()
        val anfitrion = JugadorSala(id = id, nombre = actual.nombreUsuario, anfitrion = true)
        synchronized(jugadoresHumanos) {
            jugadoresHumanos.clear()
            jugadoresHumanos += anfitrion
        }
        salaIniciada = false
        val nombreSala = "Partida de ${actual.nombreUsuario}"
        _uiState.update {
            it.copy(
                pantalla = PantallaInicio.LOBBY,
                modoSeleccionado = actual.modoSeleccionado,
                pin = pin,
                nombreSala = nombreSala,
                esAnfitrion = true,
                idJugadorLocal = id,
                jugadoresSala = listOf(anfitrion),
                conectando = false,
                mensaje = "Buscando dispositivos de la misma red local…",
                partidaRemota = null
            )
        }
        iniciarServidor(pin, nombreSala, actual.modoSeleccionado)
    }

    private fun iniciarServidor(pin: String, nombreSala: String, modo: ModoPartida) {
        bucleServidor?.cancel()
        bucleServidor = viewModelScope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(0).also { socketServidor = it }
                if (!isActive) {
                    server.close()
                    if (socketServidor === server) socketServidor = null
                    return@launch
                }
                registrarServicio(pin, server.localPort)
                while (isActive && !server.isClosed) {
                    val socket = server.accept()
                    socketsAbiertos += socket
                    launch(Dispatchers.IO) { atenderCliente(socket, pin, nombreSala, modo) }
                }
            } catch (e: Exception) {
                if (isActive) {
                    _uiState.update { it.copy(mensaje = "No se pudo abrir la sala en esta red: ${e.message ?: "error de red"}") }
                }
            }
        }
    }

    private fun registrarServicio(pin: String, puerto: Int) {
        val info = NsdServiceInfo().apply {
            serviceName = "PokerHorse-$pin"
            serviceType = TIPO_SERVICIO
            port = puerto
        }
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                listenerRegistro = this
                _uiState.update { it.copy(mensaje = "Comparte el PIN con quienes estén en la misma red Wi-Fi.") }
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                _uiState.update { it.copy(mensaje = "No se pudo anunciar la sala en esta red local (código $errorCode).") }
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit
        }
        try {
            nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
            listenerRegistro = listener
        } catch (e: Exception) {
            _uiState.update { it.copy(mensaje = "No se pudo anunciar la sala: ${e.message ?: "error de red"}") }
        }
    }

    private suspend fun atenderCliente(
        socket: Socket,
        pin: String,
        nombreSala: String,
        modo: ModoPartida
    ) {
        val cliente = ClienteLan(socket)
        try {
            val primeraLinea = cliente.entrada.readLine() ?: return
            val ingreso = JSONObject(primeraLinea)
            if (ingreso.optString("tipo") != "ingreso") return
            val jugador = JugadorSala(
                id = ingreso.getString("id"),
                nombre = ingreso.getString("nombre").trim().take(24)
            )
            if (_uiState.value.pin != pin || !_uiState.value.esAnfitrion) return
            val aceptado = synchronized(jugadoresHumanos) {
                val repetido = jugadoresHumanos.any { it.id == jugador.id || it.nombre.equals(jugador.nombre, ignoreCase = true) }
                if (salaIniciada || jugadoresHumanos.size >= MAX_JUGADORES || repetido) false
                else {
                    jugadoresHumanos += jugador
                    true
                }
            }
            if (!aceptado) {
                cliente.enviar(JSONObject().put("tipo", "error").put("mensaje", "La sala ya inició, está llena o el nombre ya está en uso."))
                return
            }
            clientes[jugador.id] = cliente
            enviarListaJugadores(pin, nombreSala, modo)

            while (!socket.isClosed) {
                val linea = cliente.entrada.readLine() ?: break
                val mensaje = JSONObject(linea)
                when (mensaje.optString("tipo")) {
                    "accion" -> runCatching {
                        val accionSolicitada = LanProtocolo.leerAccion(mensaje)
                        if (accionSolicitada.jugadorId == jugador.id) {
                            _accionesRemotas.emit(accionSolicitada)
                        }
                    }
                    "salir" -> break
                }
            }
        } catch (_: Exception) {
            // Si un dispositivo pierde Wi-Fi, se retira del lobby y el asiento vuelve a ser un bot.
        } finally {
            val id = clientes.entries.firstOrNull { it.value === cliente }?.key
            if (id != null) {
                clientes.remove(id)
                synchronized(jugadoresHumanos) { jugadoresHumanos.removeAll { it.id == id } }
                if (salaIniciada) _jugadoresDesconectados.emit(id)
                if (!salaIniciada && _uiState.value.pin == pin) enviarListaJugadores(pin, nombreSala, modo)
            }
            cliente.cerrar()
            socketsAbiertos.remove(socket)
        }
    }

    private fun enviarListaJugadores(pin: String, nombreSala: String, modo: ModoPartida) {
        if (_uiState.value.pin != pin || !_uiState.value.esAnfitrion) return
        val lista = synchronized(jugadoresHumanos) { jugadoresHumanos.toList() }
        val mensaje = LanProtocolo.mensajeLista(pin, nombreSala, modo, lista)
        clientes.values.forEach { it.enviar(mensaje) }
        _uiState.update { it.copy(jugadoresSala = lista) }
    }

    fun conectar(pinIngresado: String) {
        val pin = pinIngresado.filter(Char::isDigit).take(6)
        if (pin.length != 6) return
        detenerRed()
        val id = UUID.randomUUID().toString()
        val nombre = _uiState.value.nombreUsuario
        _uiState.update { it.copy(conectando = true, mensaje = "Buscando sala $pin en la red local…", pin = pin, idJugadorLocal = id) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val servicio = withTimeout(TIEMPO_BUSQUEDA_MS) { descubrirServicio(pin) }
                val socket = Socket().apply { connect(java.net.InetSocketAddress(servicio.host, servicio.port), 8000) }
                val cliente = ClienteLan(socket)
                clienteLan = cliente
                cliente.enviar(LanProtocolo.mensajeIngreso(id, nombre))
                val respuesta = cliente.entrada.readLine() ?: error("El anfitrión cerró la conexión.")
                val primerMensaje = JSONObject(respuesta)
                if (primerMensaje.optString("tipo") == "error") {
                    error(primerMensaje.optString("mensaje", "No fue posible entrar a la sala."))
                }
                procesarMensajeCliente(primerMensaje)
                while (isActive && !socket.isClosed) {
                    val linea = cliente.entrada.readLine() ?: break
                    procesarMensajeCliente(JSONObject(linea))
                }
                if (isActive) _uiState.update { it.copy(mensaje = "Se perdió la conexión con la sala.", conectando = false) }
            } catch (e: Exception) {
                clienteLan?.cerrar()
                clienteLan = null
                _uiState.update {
                    it.copy(
                        conectando = false,
                        mensaje = when {
                            e is kotlinx.coroutines.TimeoutCancellationException -> "No se encontró una sala con ese PIN en la red local."
                            e.message?.contains("La sala ya inició", ignoreCase = true) == true -> e.message
                            else -> e.message ?: "No fue posible conectarse a la sala."
                        }
                    )
                }
            }
        }
    }

    private fun procesarMensajeCliente(mensaje: JSONObject) {
        when (mensaje.optString("tipo")) {
            "jugadores" -> {
                val jugadores = LanProtocolo.leerJugadores(mensaje)
                _uiState.update {
                    it.copy(
                        pantalla = PantallaInicio.LOBBY,
                        pin = mensaje.getString("pin"),
                        nombreSala = mensaje.getString("nombreSala"),
                        modoSeleccionado = ModoPartida.valueOf(mensaje.getString("modo")),
                        esAnfitrion = false,
                        jugadoresSala = jugadores,
                        conectando = false,
                        mensaje = null
                    )
                }
            }
            "inicio" -> {
                val jugadores = LanProtocolo.leerJugadores(mensaje)
                _uiState.update {
                    it.copy(
                        pantalla = PantallaInicio.PARTIDA,
                        modoSeleccionado = ModoPartida.valueOf(mensaje.getString("modo")),
                        jugadoresSala = jugadores,
                        esAnfitrion = false,
                        conectando = false,
                        mensaje = null
                    )
                }
            }
            "estado" -> runCatching { LanProtocolo.leerEstado(mensaje) }
                .onSuccess { estado -> _uiState.update { it.copy(partidaRemota = estado, mensaje = null) } }
                .onFailure { error -> _uiState.update { it.copy(mensaje = "Estado de partida inválido: ${error.message ?: "datos incompletos"}") } }
            "error" -> _uiState.update { it.copy(conectando = false, mensaje = mensaje.optString("mensaje", "Error de sala")) }
        }
    }

    private suspend fun descubrirServicio(pin: String): NsdServiceInfo = suspendCancellableCoroutine { continuation ->
        var discoveryListener: NsdManager.DiscoveryListener? = null
        val buscando = AtomicBoolean(false)
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (!serviceInfo.serviceName.endsWith("-$pin") || !buscando.compareAndSet(false, true)) return
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        terminarDescubrimiento(discoveryListener)
                        if (continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("No se pudo localizar el teléfono anfitrión.")))
                    }

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        terminarDescubrimiento(discoveryListener)
                        if (continuation.isActive) continuation.resume(resolved)
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                if (continuation.isActive) continuation.resumeWith(Result.failure(IllegalStateException("No se pudo buscar en la red local (código $errorCode).")))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
        }
        discoveryListener = listener
        try {
            nsdManager.discoverServices(TIPO_SERVICIO, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            continuation.resumeWith(Result.failure(e))
        }
        continuation.invokeOnCancellation {
            discoveryListener?.let { runCatching { nsdManager.stopServiceDiscovery(it) } }
        }
    }

    private fun terminarDescubrimiento(listener: NsdManager.DiscoveryListener?) {
        if (listener != null) runCatching { nsdManager.stopServiceDiscovery(listener) }
    }

    fun iniciarSala() {
        val actual = _uiState.value
        if (!actual.esAnfitrion || actual.pantalla != PantallaInicio.LOBBY) return
        val jugadores = synchronized(jugadoresHumanos) {
            salaIniciada = true
            jugadoresHumanos.toList()
        }
        val mensaje = LanProtocolo.mensajeInicio(actual.pin, actual.modoSeleccionado, jugadores)
        clientes.values.forEach { it.enviar(mensaje) }
        _uiState.update { it.copy(pantalla = PantallaInicio.PARTIDA, jugadoresSala = jugadores, mensaje = null) }
    }

    fun enviarAccion(accion: Accion) {
        val actual = _uiState.value
        if (actual.esAnfitrion || actual.pantalla != PantallaInicio.PARTIDA) return
        clienteLan?.enviar(LanProtocolo.mensajeAccion(actual.idJugadorLocal, accion))
    }

    fun publicarEstado(remoto: EstadoRemoto) {
        if (!_uiState.value.esAnfitrion || _uiState.value.pantalla != PantallaInicio.PARTIDA) return
        val mensaje = LanProtocolo.mensajeEstado(remoto)
        clientes.values.forEach { it.enviar(mensaje) }
    }

    fun volverAlMenu() {
        detenerRed()
        val nombre = _uiState.value.nombreUsuario
        _uiState.value = InicioUiState(pantalla = PantallaInicio.MENU, nombreUsuario = nombre)
    }

    private fun detenerRed() {
        try { listenerRegistro?.let { nsdManager.unregisterService(it) } } catch (_: Exception) { }
        listenerRegistro = null
        bucleServidor?.cancel()
        bucleServidor = null
        try { socketServidor?.close() } catch (_: Exception) { }
        socketServidor = null
        clientes.values.forEach { it.cerrar() }
        clientes.clear()
        socketsAbiertos.forEach { runCatching { it.close() } }
        socketsAbiertos.clear()
        clienteLan?.cerrar()
        clienteLan = null
        synchronized(jugadoresHumanos) { jugadoresHumanos.clear() }
        salaIniciada = false
    }

    override fun onCleared() {
        detenerRed()
        super.onCleared()
    }

    private class ClienteLan(val socket: Socket) {
        val entrada = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        private val salida = PrintWriter(socket.getOutputStream(), true)

        @Synchronized fun enviar(json: JSONObject) {
            salida.println(json.toString())
        }

        fun cerrar() {
            runCatching { socket.close() }
        }
    }

    companion object {
        private const val CLAVE_NOMBRE = "nombre_jugador"
        private const val TIPO_SERVICIO = "_pokerhorse._tcp."
        private const val MAX_JUGADORES = 4
        private const val TIEMPO_BUSQUEDA_MS = 12_000L
    }
}
