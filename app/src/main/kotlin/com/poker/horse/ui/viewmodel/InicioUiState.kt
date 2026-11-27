package com.poker.horse.ui.viewmodel

import poker.motor.EstadoMano

enum class PantallaInicio { REGISTRO, MENU, ESPERA_CODIGO, LOBBY, PARTIDA }

data class JugadorSala(val id: String, val nombre: String, val anfitrion: Boolean = false)

data class EstadoRemoto(
    val estado: EstadoMano,
    val idsPorAsiento: Map<Int, String>,
    val modo: ModoPartida,
    val manoEnVariante: Int,
    val manosPorVariante: Int
)

data class InicioUiState(
    val pantalla: PantallaInicio = PantallaInicio.REGISTRO,
    val nombreUsuario: String = "",
    val modoSeleccionado: ModoPartida = ModoPartida.HORSE,
    val pin: String = "",
    val nombreSala: String = "",
    val esAnfitrion: Boolean = false,
    val idJugadorLocal: String = "",
    val jugadoresSala: List<JugadorSala> = emptyList(),
    val conectando: Boolean = false,
    val mensaje: String? = null,
    val partidaRemota: EstadoRemoto? = null
)
