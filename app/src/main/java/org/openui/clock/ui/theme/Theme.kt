package org.openui.clock.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ClockPrimaryDark,
    onPrimary = ClockOnPrimaryDark,
    primaryContainer = ClockPrimaryContainerDark,
    onPrimaryContainer = ClockOnPrimaryContainerDark,
    background = ClockDarkBackground,
    surface = ClockDarkSurface,
    surfaceVariant = ClockDarkSurfaceVariant,
    onBackground = Color(0xFFE2E2E6),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0)
)

private val LightColorScheme = lightColorScheme(
    primary = ClockPrimaryLight,
    onPrimary = ClockOnPrimaryLight,
    primaryContainer = ClockPrimaryContainerLight,
    onPrimaryContainer = ClockOnPrimaryContainerLight,
    background = ClockLightBackground,
    surface = ClockLightSurface,
    surfaceVariant = ClockLightSurfaceVariant,
    onBackground = Color(0xFF191C20),
    onSurface = Color(0xFF191C20),
    onSurfaceVariant = Color(0xFF44474F)
)

@Composable
fun ClockAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
