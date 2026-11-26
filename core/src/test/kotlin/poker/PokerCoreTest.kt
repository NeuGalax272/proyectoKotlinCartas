package poker

import poker.evaluacion.Categoria
import poker.evaluacion.EvaluadorAlto
import poker.evaluacion.EvaluadorBajoOcho
import poker.evaluacion.EvaluadorBajoRazz
import poker.modelo.Accion
import poker.modelo.Carta
import poker.modelo.EstadoJugador
import poker.modelo.Jugador
import poker.modelo.MazoAleatorio
import poker.modelo.Palo
import poker.modelo.Rango
import poker.modelo.Variante
import poker.motor.MotorDeJuego
import poker.variantes.HoldemReglas
import poker.variantes.OmahaReglas
import poker.variantes.RazzReglas
import poker.variantes.SevenCardHiLoReglas
import poker.variantes.SevenCardStudReglas
import poker.variantes.reglasPredeterminadas
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PokerCoreTest {

    @Test
    fun `test invariantes de jugador rechaza pila negativa`() {
        assertFailsWith<IllegalArgumentException> {
            Jugador(asiento = 0, nombre = "Juan", pila = -100)
        }
    }

    @Test
    fun `test invariantes de jugador rechaza nombre en blanco`() {
        assertFailsWith<IllegalArgumentException> {
            Jugador(asiento = 0, nombre = "   ", pila = 500)
        }
    }

    @Test
    fun `test polimorfismo reglas variantes`() {
        val holdem = HoldemReglas()
        val omaha = OmahaReglas()
        val razz = RazzReglas()
        val stud = SevenCardStudReglas()
        val hilo = SevenCardHiLoReglas()

        assertEquals(Variante.HOLDEM, holdem.variante)
        assertEquals(Variante.OMAHA, omaha.variante)
        assertEquals(Variante.RAZZ, razz.variante)
        assertEquals(Variante.SEVEN_CARD_STUD, stud.variante)
        assertEquals(Variante.SEVEN_CARD_HI_LO, hilo.variante)

        assertTrue(holdem.esJuegoComunitario)
        assertTrue(omaha.esJuegoComunitario)
        assertTrue(!razz.esJuegoComunitario)
        assertTrue(!stud.esJuegoComunitario)
        assertTrue(!hilo.esJuegoComunitario)
    }

    @Test
    fun `test omaha exige exactamente dos cartas propias y tres comunitarias`() {
        val omaha = OmahaReglas()
        // El jugador tiene 4 ases en mano, y en mesa hay 1 as y 4 reyes.
        // En Hold'em tendría poker de ases, pero en Omaha sólo puede usar 2 de sus ases + 3 comunitarias (As + Rey + Rey -> Full House Ases y Reyes)
        val propias = listOf(
            Carta(Rango.AS, Palo.CORAZONES),
            Carta(Rango.AS, Palo.DIAMANTES),
            Carta(Rango.AS, Palo.TREBOLES),
            Carta(Rango.AS, Palo.PICAS)
        )
        val comunitarias = listOf(
            Carta(Rango.REY, Palo.CORAZONES),
            Carta(Rango.REY, Palo.DIAMANTES),
            Carta(Rango.REY, Palo.TREBOLES),
            Carta(Rango.DOS, Palo.PICAS),
            Carta(Rango.TRES, Palo.PICAS)
        )

        val resultado = omaha.evaluarJugador(propias, comunitarias)
        // 2 Ases de la mano + 3 Reyes de la mesa = Full House de Reyes y Ases
        assertEquals(Categoria.FULL_HOUSE, resultado.categoria)
    }

    @Test
    fun `test razz evalua la mano mas baja`() {
        val razz = RazzReglas()
        // 7 cartas: 5, 4, 3, 2, A, K, Q
        val cartas = listOf(
            Carta(Rango.CINCO, Palo.CORAZONES),
            Carta(Rango.CUATRO, Palo.DIAMANTES),
            Carta(Rango.TRES, Palo.TREBOLES),
            Carta(Rango.DOS, Palo.PICAS),
            Carta(Rango.AS, Palo.CORAZONES),
            Carta(Rango.REY, Palo.DIAMANTES),
            Carta(Rango.REINA, Palo.TREBOLES)
        )

        val resultado = razz.evaluarJugador(cartas, emptyList())
        assertEquals(Categoria.BAJA, resultado.categoria)
        // La mejor baja de 5 cartas es 5-4-3-2-1
        assertEquals(listOf(5, 4, 3, 2, 1), resultado.desempate)
    }

    @Test
    fun `test stud hi-lo califica baja solo con ocho o menor`() {
        val hilo = SevenCardHiLoReglas()
        // Mano sin calificar para baja (cartas >= 9 o con pares)
        val manoSinBaja = listOf(
            Carta(Rango.NUEVE, Palo.CORAZONES),
            Carta(Rango.DIEZ, Palo.DIAMANTES),
            Carta(Rango.JOTA, Palo.TREBOLES),
            Carta(Rango.REINA, Palo.PICAS),
            Carta(Rango.REY, Palo.CORAZONES),
            Carta(Rango.DOS, Palo.DIAMANTES),
            Carta(Rango.TRES, Palo.TREBOLES)
        )
        assertNull(hilo.evaluarJugadorBajo(manoSinBaja, emptyList()))

        // Mano que sí califica para baja (8, 7, 6, 5, 4)
        val manoConBaja = listOf(
            Carta(Rango.OCHO, Palo.CORAZONES),
            Carta(Rango.SIETE, Palo.DIAMANTES),
            Carta(Rango.SEIS, Palo.TREBOLES),
            Carta(Rango.CINCO, Palo.PICAS),
            Carta(Rango.CUATRO, Palo.CORAZONES),
            Carta(Rango.REY, Palo.DIAMANTES),
            Carta(Rango.REINA, Palo.TREBOLES)
        )
        val resBaja = hilo.evaluarJugadorBajo(manoConBaja, emptyList())
        assertNotNull(resBaja)
        assertEquals(Categoria.BAJA, resBaja.categoria)
    }

    @Test
    fun `test reproducibilidad con semilla`() {
        val m1 = MazoAleatorio(12345).nuevoMazo()
        val m2 = MazoAleatorio(12345).nuevoMazo()
        assertEquals(m1, m2, "Dos mazos generados con la misma semilla deben ser idénticos")
    }

    @Test
    fun `test conservacion de fichas en motor de juego`() {
        val motor = MotorDeJuego(reglasPredeterminadas())
        val jugadores = listOf(
            Jugador(asiento = 0, nombre = "Jugador 1", pila = 500, esHumano = true),
            Jugador(asiento = 1, nombre = "Jugador 2", pila = 500, esHumano = false),
            Jugador(asiento = 2, nombre = "Jugador 3", pila = 500, esHumano = false)
        )

        val totalInicial = jugadores.sumOf { it.pila }
        val estadoInicial = motor.iniciarNuevaMano(
            variante = Variante.HOLDEM,
            jugadoresActuales = jugadores,
            dealerIndex = 0,
            generadorDeMazo = MazoAleatorio(999)
        )

        assertEquals(
            totalInicial,
            estadoInicial.totalFichasEnMesa,
            "El total de fichas (pilas + aportes + bote) debe conservarse exactamente"
        )
    }

    @Test
    fun `test acciones legales exige fold call raise o allin cuando hay apuesta pendiente`() {
        val motor = MotorDeJuego(reglasPredeterminadas())
        val jugadores = listOf(
            Jugador(asiento = 0, nombre = "P1", pila = 500, esHumano = true),
            Jugador(asiento = 1, nombre = "P2", pila = 500, esHumano = false)
        )

        val estado = motor.iniciarNuevaMano(
            variante = Variante.HOLDEM,
            jugadoresActuales = jugadores,
            dealerIndex = 0,
            ciegaGrande = 20
        )

        val legales = motor.accionesLegales(estado)
        // Ante una ciega pendiente, el jugador no puede hacer Check simple
        assertTrue(legales.none { it is Accion.Check })
        assertTrue(legales.any { it is Accion.Fold })
        assertTrue(legales.any { it is Accion.Call || it is Accion.AllIn })
    }
}
