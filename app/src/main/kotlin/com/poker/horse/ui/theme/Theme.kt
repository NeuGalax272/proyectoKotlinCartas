package com.poker.horse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OroFichas,
    onPrimary = FondoOscuro,
    secondary = TextoSecundario,
    onSecondary = FondoOscuro,
    background = FondoOscuro,
    onBackground = MarfilCarta,
    surface = SuperficieOscura,
    onSurface = MarfilCarta,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    outline = BordeMesa,
    outlineVariant = LineaSuperficie
)

@Composable
fun PokerHorseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
