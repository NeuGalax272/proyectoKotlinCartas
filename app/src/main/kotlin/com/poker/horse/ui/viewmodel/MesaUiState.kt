package com.poker.horse.ui.viewmodel

import poker.modelo.Accion
import poker.modelo.Variante
import poker.motor.EstadoMano

data class MesaUiState(
    val estadoMano: EstadoMano,
    val accionesLegales: Set<Accion> = emptySet(),
    val esTurnoHumano: Boolean = false,
    val manoEnVariante: Int = 1,
    val manosPorVariante: Int = 5,
    val semillaConfigurada: Long? = null,
    val mostrarDialogoConfiguracion: Boolean = false,
    val mostrarDialogoReglas: Boolean = false,
    val evaluacionHumanoTexto: String? = null
)
