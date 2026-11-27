package com.poker.horse.ui.viewmodel

import poker.modelo.Variante

enum class ModoPartida(val titulo: String, val variantes: List<Variante>) {
    HORSE("HORSE", listOf(
        Variante.HOLDEM,
        Variante.OMAHA,
        Variante.RAZZ,
        Variante.SEVEN_CARD_STUD,
        Variante.SEVEN_CARD_HI_LO
    )),
    HOLDEM("Texas Hold'em", listOf(Variante.HOLDEM)),
    OMAHA("Omaha Hi", listOf(Variante.OMAHA)),
    RAZZ("Razz", listOf(Variante.RAZZ)),
    SEVEN_CARD_STUD("Seven Card Stud", listOf(Variante.SEVEN_CARD_STUD)),
    SEVEN_CARD_HI_LO("Seven Card Stud Hi-Lo", listOf(Variante.SEVEN_CARD_HI_LO));

    companion object {
        fun desdeVariante(variante: Variante): ModoPartida = when (variante) {
            Variante.HOLDEM -> HOLDEM
            Variante.OMAHA -> OMAHA
            Variante.RAZZ -> RAZZ
            Variante.SEVEN_CARD_STUD -> SEVEN_CARD_STUD
            Variante.SEVEN_CARD_HI_LO -> SEVEN_CARD_HI_LO
        }
    }
}
