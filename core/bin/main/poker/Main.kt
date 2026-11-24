package poker

import poker.modelo.Carta
import poker.modelo.GeneradorDeMazo
import poker.modelo.MazoAleatorio
import poker.modelo.MazoFijo
import poker.modelo.mazoOrdenado

fun leerSemilla(): Long? {
    while (true) {
        print("Semilla (vacío = aleatoria): ")
        val texto = readln().trim()
        if (texto.isEmpty()) return null
        val semilla = texto.toLongOrNull()
        if (semilla != null) return semilla
        println("La semilla debe ser un número entero. Intenta de nuevo.")
    }
}

fun verificarMazos() {   //esto lo anadi pq me estaba dando mazos repetidos 
    val a = MazoAleatorio(42).nuevoMazo()
    val b = MazoAleatorio(42).nuevoMazo()
    val c = MazoAleatorio(7).nuevoMazo()
    check(a.size == 52) { "El mazo debe tener 52 cartas" }
    check(a.toSet().size == 52) { "Hay cartas repetidas" }
    check(a == b) { "La misma semilla debe dar el mismo mazo" }
    check(a != c) { "Semillas distintas deben dar mazos distintos" }

    val p1 = MazoAleatorio(42)
    val p2 = MazoAleatorio(42)
    check(p1.nuevoMazo() == p2.nuevoMazo()) { "La mano 1 de la sesión debe coincidir" }
    check(p1.nuevoMazo() == p2.nuevoMazo()) { "La mano 2 de la sesión debe coincidir" }
    check(p1.nuevoMazo() != p1.nuevoMazo()) { "Dos manos seguidas no deben repetir mazo" }

    val orden = mazoOrdenado()
    check(MazoFijo(orden).nuevoMazo() == orden) { "El mazo fijo debe conservar su orden" }

    println("Verificaciones del mazo: OK")
}

fun main() {
    verificarMazos()

    val semilla = leerSemilla()
    val proveedor: GeneradorDeMazo = MazoAleatorio(semilla)
    val mazo = proveedor.nuevoMazo()

    println(if (semilla == null) "Mazo aleatorio" else "Mazo con semilla $semilla")
    println("Cartas: ${mazo.size}")
    println(mazo.take(7))
}