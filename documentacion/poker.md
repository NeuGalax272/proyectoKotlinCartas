::: center

------------------------------------------------------------------------

\
**Diseño orientado a objetos del proyecto**\
Póker HORSE para dispositivos móviles\
Kotlin -- Modelo de dominio, responsabilidades y polimorfismo\

------------------------------------------------------------------------

\
Eddy Ramírez Jiménez\
Entrega: miércoles 2 de diciembre
:::

# Propósito de este documento

Este documento concreta las características de programación orientada a
objetos (POO) que deben evidenciarse en el proyecto de póker HORSE. No
basta con que el programa esté escrito en Kotlin ni con que declare
algunas clases: el diseño debe asignar responsabilidades claras a los
objetos, proteger sus invariantes y permitir que las variantes del juego
se comporten de manera diferenciada mediante abstracción y polimorfismo.

El proyecto también propone inmutabilidad y funciones puras para el
motor de reglas. Estos enfoques son compatibles con la POO: un objeto
puede representar una entidad del dominio sin permitir que su estado se
modifique después de construirse. La POO no exige que todo objeto tenga
setters ni que toda operación modifique internamente una instancia.

::: idea
El motor debe modelar el dominio del póker. La interfaz debe presentar
el estado del juego, y la capa de aplicación debe coordinar las
operaciones. Las reglas de una variante no deben quedar dispersas en
botones, pantallas o condicionales gigantes.
:::

# Las características POO que deben demostrarse

## Abstracción

La abstracción consiste en representar los conceptos relevantes del
dominio y ocultar detalles que no son necesarios para quien utiliza cada
componente. En este proyecto, por ejemplo, una carta se representa
mediante su rango y palo; un jugador, mediante su asiento, pila, cartas
y estado; y una variante, mediante las reglas que la distinguen.

La abstracción debe verse en contratos que expresen qué puede hacer un
componente, sin obligar a sus clientes a conocer cómo lo hace.

    data class Carta(
        val rango: Rango,
        val palo: Palo
    )

    interface EvaluadorDeManos {
        fun evaluar(cartas: List<Carta>): ResultadoEvaluacion
    }

La interfaz `EvaluadorDeManos` expresa una responsabilidad. Una pantalla
o un componente de apuestas no necesita conocer cómo se comparan las
manos; solo necesita solicitar una evaluación y utilizar el resultado.

## Encapsulamiento

El encapsulamiento consiste en proteger el estado interno y asegurar que
las operaciones se realicen mediante reglas controladas. No debe ser
posible modificar directamente una pila, alterar un aporte o declarar
ganador a un jugador saltándose las reglas del motor.

Por ejemplo, conviene evitar que cualquier componente pueda ejecutar
`jugador.pila = -500`. En un modelo inmutable, la operación recibe el
estado actual y produce un nuevo estado válido o un error de dominio.

    data class Jugador(
        val asiento: Asiento,
        val nombre: String,
        val pila: Int,
        val aporteEnCalle: Int,
        val aporteTotal: Int,
        val cartas: List<Carta>,
        val estado: EstadoJugador
    ) {
        init {
            require(nombre.isNotBlank())
            require(pila >= 0)
            require(aporteEnCalle >= 0)
            require(aporteTotal >= aporteEnCalle)
        }
    }

El ejemplo muestra validaciones locales de construcción. Las reglas que
dependen del estado completo de la mano --por ejemplo, si un jugador
puede subir-- deben permanecer en el motor de reglas y no dentro de la
interfaz gráfica.

## Herencia y especialización

La herencia permite definir una abstracción general y especializarla en
tipos más concretos. En Kotlin puede expresarse mediante clases
abstractas o interfaces. En este proyecto debe utilizarse cuando exista
una relación real de "es-un" y cuando las implementaciones compartan un
contrato.

No se recomienda crear una jerarquía artificial para cada carta, jugador
o pantalla. Para las variantes HORSE, una interfaz de estrategia es más
flexible que una clase base cargada de condicionales.

    interface ReglasVariante {
        val variante: Variante

        fun estructuraDeApuestas(
            configuracion: ConfiguracionPartida,
            calle: Calle
        ): EstructuraApuestas

        fun planDeReparto(calle: Calle): PlanDeReparto

        fun determinarOrdenDeAccion(
            estado: EstadoMano
        ): List<Asiento>

        fun evaluarJugador(
            cartasPropias: List<Carta>,
            comunitarias: List<Carta>
        ): ResultadoEvaluacion
    }

Cada variante concreta implementa ese contrato. Las cinco
implementaciones deben expresar las diferencias de reparto, apuestas,
orden de acción y evaluación que correspondan.

## Polimorfismo

El polimorfismo permite utilizar objetos de tipos concretos distintos a
través de un mismo contrato. El motor puede trabajar con una referencia
de tipo `ReglasVariante`, sin preguntar continuamente si está jugando
Hold'em, Omaha o Razz.

    class MotorDeJuego(
        private val reglas: ReglasVariante,
        private val evaluador: EvaluadorDeManos
    ) {
        fun ordenDeAccion(estado: EstadoMano): List<Asiento> =
            reglas.determinarOrdenDeAccion(estado)

        fun evaluar(
            propias: List<Carta>,
            comunitarias: List<Carta>
        ): ResultadoEvaluacion =
            reglas.evaluarJugador(propias, comunitarias)
    }

El mismo método `evaluar` produce comportamientos diferentes según el
objeto concreto que implemente `ReglasVariante`. Por ejemplo, Omaha debe
usar exactamente dos cartas propias y tres comunitarias, mientras que
Hold'em busca la mejor combinación de cinco cartas entre las
disponibles.

::: criterio
Debe ser posible incorporar una nueva implementación de `ReglasVariante`
y utilizarla desde el motor mediante el mismo contrato, sin modificar la
lógica general del motor para agregar una cadena de `if`/`when`
específica para esa variante.
:::

## Composición

La composición consiste en construir objetos complejos a partir de otros
objetos que colaboran. Es preferible a una jerarquía de herencia cuando
la relación es "tiene-un" y no "es-un".

Un motor de juego puede estar compuesto por un evaluador, un proveedor
de mazo, un calculador de botes y un conjunto de reglas de variante.
Cada pieza tiene una responsabilidad acotada.

    class MotorDeJuego(
        private val evaluador: EvaluadorDeManos,
        private val calculadorBotes: CalculadorDeBotes,
        private val reglas: ReglasVariante
    )

La composición facilita sustituir implementaciones en pruebas. Por
ejemplo, se puede inyectar un mazo previamente ordenado para reproducir
una mano sin depender de un generador aleatorio.

# Modelo de objetos propuesto

La siguiente tabla identifica responsabilidades que conviene separar. No
implica que cada fila deba convertirse obligatoriamente en una clase
concreta: algunos conceptos pueden modelarse como interfaces,
enumeraciones, tipos sellados o funciones puras.

## Tipos de dominio

Los tipos sellados de Kotlin son apropiados para conjuntos cerrados de
alternativas. Permiten que el compilador ayude a verificar que se
contemplen todos los casos.

    sealed interface EstadoJugador {
        data object Activo : EstadoJugador
        data object Retirado : EstadoJugador
        data object AllIn : EstadoJugador
    }

    sealed interface Accion {
        data object Fold : Accion
        data object Check : Accion
        data class Call(val monto: Int) : Accion
        data class Bet(val monto: Int) : Accion
        data class Raise(val monto: Int) : Accion
        data class AllIn(val monto: Int) : Accion
    }

Estos tipos no sustituyen el polimorfismo de comportamiento de
`ReglasVariante`. Son herramientas complementarias: los tipos sellados
representan alternativas conocidas, mientras que las interfaces de
estrategia permiten intercambiar implementaciones con un contrato común.

# Arquitectura y separación de responsabilidades

Se recomienda separar el proyecto, como mínimo, en dos módulos:

-   **`:core`:** modelo de dominio, reglas HORSE, evaluación de manos,
    apuestas, botes, bots y transiciones. No depende de Android ni de
    Jetpack Compose.

-   **`:app`:** interfaz móvil, navegación, ciclo de vida, persistencia
    y coordinación de las interacciones con el usuario.

La interfaz no debe decidir quién gana una mano ni calcular por su
cuenta si una subida es legal. Debe consultar al motor y representar el
resultado.

::: center
:::

# Principios de diseño que orientan la implementación

Además de los cuatro pilares clásicos, se espera que el diseño respete
estos criterios:

1.  **Responsabilidad única:** cada clase o componente debe tener una
    razón principal para cambiar. El evaluador no administra la interfaz
    ni el estado de las apuestas.

2.  **Abierto/cerrado:** agregar una variante debe consistir,
    idealmente, en implementar el contrato de reglas, no en modificar
    todos los componentes existentes.

3.  **Sustitución:** cualquier implementación de `ReglasVariante` debe
    poder utilizarse donde se espera ese contrato y cumplir sus
    condiciones.

4.  **Segregación de interfaces:** es preferible tener contratos
    pequeños y específicos antes que una interfaz enorme que obligue a
    implementar operaciones irrelevantes.

5.  **Inversión de dependencias:** el motor debe depender de
    abstracciones para colaboradores intercambiables, no de detalles de
    la interfaz o de Android.

6.  **Inmutabilidad:** las entidades del dominio se construyen con
    valores válidos y las transiciones producen nuevos estados.

# Inmutabilidad y estilo funcional: relación con la POO

El proyecto requiere que las operaciones del motor puedan expresarse
como transformaciones controladas:

    // Consulta: no modifica el estado.
    fun accionesLegales(estado: EstadoMano): Set<Accion>

    // Transicion: produce un estado nuevo o un error.
    fun aplicar(
        estado: EstadoMano,
        accion: Accion
    ): Resultado<EstadoMano>

Este diseño no elimina los objetos. `EstadoMano`, `Jugador`, `Carta` y
las implementaciones de `ReglasVariante` siguen modelando conceptos y
colaboraciones del dominio. La diferencia es que se evita modificar
objetos compartidos de forma incontrolada.

En consecuencia, el proyecto debe combinar deliberadamente:

-   **POO:** abstracciones, encapsulamiento, contratos, composición y
    polimorfismo.

-   **Estilo funcional:** inmutabilidad, funciones puras, tipos
    algebraicos y ausencia de efectos secundarios en el núcleo.

# Requisitos mínimos para demostrar POO

Durante la revisión y la defensa se comprobará que el equipo puede:

1.  Identificar las clases, interfaces y tipos principales del dominio y
    justificar la responsabilidad de cada uno.

2.  Explicar qué datos están encapsulados y qué invariantes protegen.

3.  Mostrar al menos una abstracción con varias implementaciones
    concretas utilizadas polimórficamente.

4.  Justificar dónde se utiliza herencia y por qué no se usa en otros
    lugares donde la composición resulta más apropiada.

5.  Incorporar o sustituir una implementación de reglas sin introducir
    condicionales específicos en el motor general.

6.  Explicar cómo se evita que la interfaz modifique directamente el
    estado del juego.

7.  Mostrar pruebas que verifiquen tanto el comportamiento de cada
    implementación como las invariantes del dominio.

::: criterio
Crear muchas clases no demuestra por sí mismo un buen diseño orientado a
objetos. Se evaluará que las abstracciones sean útiles, que las
responsabilidades estén bien ubicadas y que el polimorfismo reduzca el
acoplamiento en lugar de añadir complejidad innecesaria.
:::

# Criterios de evaluación

La calificación se distribuye de la siguiente manera. Cada componente se
valora sobre el código entregado y sobre lo que el equipo pueda explicar
durante la cita de revisión.

::: tabularx
\@L4.5cmL9.4cmL1.45cm@ **Componente** & **Qué se observa** & **Valor**\
Abstracción y modelo de dominio & Se identifican las abstracciones
relevantes y cada una expresa un contrato; no hay clases vacías ni
jerarquías decorativas. & 12 %\
Encapsulamiento e invariantes & Las entidades solo se construyen en
estados válidos; no es posible alterar una pila, un aporte ni declarar
un ganador saltándose el motor. & 13 %\
Polimorfismo de las variantes HORSE & Las cinco variantes se comportan
de forma distinta a través del mismo contrato, sin condicionales por
variante dentro del motor general. & 20 %\
Composición y asignación de responsabilidades & Cada colaborador tiene
un límite claro; la interfaz no decide reglas ni calcula resultados por
su cuenta. & 15 %\
Principios de diseño y acoplamiento & Se justifica dónde se usa herencia
y dónde composición; agregar una variante no obliga a editar los
componentes existentes. & 10 %\
Motor de reglas, apuestas y botes & Las acciones legales, los botes
secundarios y el reparto conservan las fichas y rechazan toda jugada
ilegal sin corromper el estado. & 15 %\
Interfaz móvil y usabilidad & La aplicación se ejecuta en el
dispositivo, se adapta a ambas orientaciones y no ofrece acciones que el
motor rechazaría. & 8 %\
Pruebas y defensa & Existen pruebas del comportamiento polimórfico y de
las invariantes, y cada integrante explica con soltura la parte del
código que le corresponde. & 7 %\
**Total** & & **100 %**\
:::

::: criterio
El porcentaje asignado a pruebas y defensa no se limita a que el
programa funcione. Un proyecto correcto que el equipo no puede explicar
pierde ese componente completo, y un diseño mal ubicado no se compensa
con una interfaz vistosa.
:::

# Entrega y citas de revisión

-   **Fecha de entrega:** miércoles 2 de diciembre.

-   La entrega se realiza antes del inicio de la cita asignada e incluye
    el repositorio con los módulos `:core` y `:app`, las instrucciones
    de compilación y el registro de pruebas ejecutadas.

-   Las **citas de revisión** se atienden en el horario de clases. La
    asignación de horarios se publica con anticipación y cada equipo
    asiste en el bloque que le corresponde.

-   La revisión es presencial y dura aproximadamente veinte minutos por
    equipo. Se espera la presencia de todos los integrantes.

-   La aplicación debe estar instalada y funcionando en un dispositivo
    móvil durante la cita. No se aceptan demostraciones sobre
    diapositivas ni capturas de pantalla en lugar del programa en
    ejecución.

-   Cada integrante debe poder abrir el proyecto, ubicar una clase
    concreta y justificar sus decisiones de diseño sin apoyo de otro
    compañero.

::: idea
Un recorrido corto por el código: el contrato de `ReglasVariante` y sus
cinco implementaciones, el punto donde se validan las acciones legales,
el cálculo de botes secundarios y una prueba que falle si se introduce
una acción ilegal. Con eso cubierto, las preguntas de la sección
siguiente se responden sin improvisar.
:::

# Preguntas para la defensa

Cada integrante debe poder responder, con referencia al código (además
de contestar cierto quiz):

-   ¿Qué responsabilidad tiene esta clase y qué responsabilidad
    deliberadamente no tiene?

-   ¿Qué ocurriría si se cambiara la implementación concreta de esta
    interfaz?

-   ¿Dónde se protege la pila de un jugador para impedir valores
    negativos?

-   ¿Cómo se agrega una variante sin alterar el funcionamiento general
    del motor?

-   ¿Por qué se eligió composición en este caso y no herencia?

-   ¿Qué diferencia existe entre el polimorfismo de `ReglasVariante` y
    las alternativas de un tipo sellado?

-   ¿Cómo se prueba una mano reproducible sin generar azar dentro del
    motor?
