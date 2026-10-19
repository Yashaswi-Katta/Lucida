package com.lucida.studio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkStudioScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = GunmetalBg,
    secondary = SlateMuted,
    onSecondary = GunmetalBg,
    tertiary = CrimsonAlert,
    onTertiary = StudioWhite,
    background = GunmetalBg,
    onBackground = StudioWhite,
    surface = CharcoalSurface,
    onSurface = StudioWhite,
    surfaceVariant = CharcoalSurface,
    onSurfaceVariant = SlateMuted,
    outline = CharcoalBorder,
    error = CrimsonAlert,
    onError = StudioWhite
)

@Composable
fun LucidaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkStudioScheme, typography = LucidaTypography, content = content)
}
