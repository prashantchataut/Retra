package app.retra.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import app.retra.data.ThemeMode

val LocalRetraColors = staticCompositionLocalOf { LightColors }
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Shorthand accessors for the current tokens. */
object Retra {
    val colors: RetraColors
        @Composable @ReadOnlyComposable get() = LocalRetraColors.current
    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable get() = LocalReduceMotion.current
    val type = RetraType
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** True when the system animator duration scale is 0 (Settings > Accessibility > Remove animations). */
@Composable
fun systemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

@Composable
fun RetraTheme(
    dark: Boolean,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) DarkColors else LightColors
    val reduce = reduceMotion || systemReducedMotion()
    val scheme = remember(colors) {
        val base = if (colors.isDark) darkColorScheme() else lightColorScheme()
        base.copy(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentSoft,
            onPrimaryContainer = colors.onAccentSoft,
            secondary = colors.accent,
            onSecondary = colors.onAccent,
            background = colors.sky,
            onBackground = colors.ink,
            surface = colors.sky,
            onSurface = colors.ink,
            surfaceVariant = colors.accentSoft,
            onSurfaceVariant = colors.inkMuted,
            surfaceContainerHighest = colors.inkFaint,
            outline = colors.inkMuted,
            outlineVariant = colors.divider,
            error = colors.danger,
        )
    }
    val typography = remember {
        Typography(
            displayLarge = RetraType.display, displayMedium = RetraType.display, displaySmall = RetraType.display,
            headlineLarge = RetraType.title, headlineMedium = RetraType.title, headlineSmall = RetraType.headline,
            titleLarge = RetraType.headline, titleMedium = RetraType.bodyStrong, titleSmall = RetraType.label,
            bodyLarge = RetraType.body, bodyMedium = RetraType.body, bodySmall = RetraType.caption,
            labelLarge = RetraType.label, labelMedium = RetraType.caption, labelSmall = RetraType.caption,
        )
    }
    CompositionLocalProvider(
        LocalRetraColors provides colors,
        LocalReduceMotion provides reduce,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
