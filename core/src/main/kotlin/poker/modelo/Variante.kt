package poker.modelo

enum class Variante(val sigla: Char, val nombreCompleto: String, val descripcion: String) {
    HOLDEM('H', "Texas Hold'em", "2 cartas propias, 5 comunitarias, mejor mano alta de 5"),
    OMAHA('O', "Omaha Hi", "4 cartas propias, 5 comunitarias (exactamente 2 propias y 3 comunitarias)"),
    RAZZ('R', "Razz", "7 cartas repartidas (3 boca abajo, 4 boca arriba), mejor mano baja A-5"),
    SEVEN_CARD_STUD('S', "Seven Card Stud", "7 cartas repartidas (3 boca abajo, 4 boca arriba), mejor mano alta"),
    SEVEN_CARD_HI_LO('E', "Seven Card Stud Hi-Lo 8-or-Better", "7 cartas repartidas, divide el bote entre mejor alta y mejor baja (<= 8)")
}

enum class Calle(val etiqueta: String) {
    PREFLOP("Pre-Flop"),
    FLOP("Flop"),
    TURN("Turn"),
    RIVER("River"),
    TERCERA("3ra Calle"),
    CUARTA("4ta Calle"),
    QUINTA("5ta Calle"),
    SEXTA("6ta Calle"),
    SEPTIMA("7ma Calle"),
    SHOWDOWN("Showdown")
}
