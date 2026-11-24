package poker.modelo

sealed interface Accion {
    data object Fold : Accion
    data object Check : Accion
    data class Call(val monto: Int) : Accion
    data class Bet(val monto: Int) : Accion
    data class Raise(val monto: Int) : Accion
    data class AllIn(val monto: Int) : Accion

    fun nombreSimple(): String = when (this) {
        is Fold -> "No ir (Fold)"
        is Check -> "Pasar (Check)"
        is Call -> "Igualar $monto"
        is Bet -> "Apostar $monto"
        is Raise -> "Subir a $monto"
        is AllIn -> "All-In ($monto)"
    }
}
