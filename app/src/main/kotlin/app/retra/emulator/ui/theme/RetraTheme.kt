package app.retra.emulator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.retra.core.model.AccentPalette
import app.retra.core.model.AppSettings
import app.retra.core.model.ThemeMode

val VoidBlack = Color(0xFF0D0E12)
val Graphite = Color(0xFF17181E)
val RetraIndigo = Color(0xFF675CF5)
val SoftViolet = Color(0xFF8D86F7)
val SaveMint = Color(0xFF39A77B)
val AdventureGold = Color(0xFFC7963E)
val ErrorCoral = Color(0xFFE45D6F)
val CloudWhite = Color(0xFFF8F8FA)
val SoftCloud = Color(0xFFF2F2F5)
val DeepInk = Color(0xFF14151A)
val MutedInk = Color(0xFF5E606A)

// Retained aliases for status/artwork accents used outside the brand accent system.
val PrismCyan = RetraIndigo
val MemoryViolet = SoftViolet

private val DarkBase = darkColorScheme(
    primary = RetraIndigo,
    onPrimary = CloudWhite,
    primaryContainer = Color(0xFF2B2859),
    onPrimaryContainer = Color(0xFFE5E2FF),
    secondary = Color(0xFFB9BBC5),
    onSecondary = Color(0xFF202127),
    secondaryContainer = Color(0xFF26272E),
    onSecondaryContainer = Color(0xFFE2E2E8),
    tertiary = Color(0xFFB9BBC5),
    background = VoidBlack,
    onBackground = CloudWhite,
    surface = Graphite,
    onSurface = CloudWhite,
    surfaceVariant = Color(0xFF222329),
    onSurfaceVariant = Color(0xFFBFC0C8),
    outline = Color(0xFF777983),
    outlineVariant = Color(0xFF303138),
    error = ErrorCoral
)

private val LightBase = lightColorScheme(
    primary = Color(0xFF5147D8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E4FF),
    onPrimaryContainer = Color(0xFF211866),
    secondary = Color(0xFF5C5E67),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8EC),
    onSecondaryContainer = Color(0xFF24252B),
    tertiary = Color(0xFF5C5E67),
    background = SoftCloud,
    onBackground = DeepInk,
    surface = Color.White,
    onSurface = DeepInk,
    surfaceVariant = Color(0xFFEDEDF1),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFF747680),
    outlineVariant = Color(0xFFD7D7DC),
    error = Color(0xFFBA1A1A)
)

private data class Palette(val primary: Color, val secondary: Color, val tertiary: Color)

private fun palette(value: AccentPalette, dark: Boolean): Palette = when (value) {
    AccentPalette.RETRA_INDIGO -> Palette(
        primary = if (dark) RetraIndigo else Color(0xFF5147D8),
        secondary = if (dark) Color(0xFFB9BBC5) else Color(0xFF5C5E67),
        tertiary = if (dark) Color(0xFFB9BBC5) else Color(0xFF5C5E67)
    )
    AccentPalette.GRAPHITE -> Palette(
        primary = if (dark) Color(0xFFC2C3CA) else Color(0xFF4F5159),
        secondary = if (dark) Color(0xFF9B9DA6) else Color(0xFF686A73),
        tertiary = if (dark) Color(0xFF9B9DA6) else Color(0xFF686A73)
    )
    AccentPalette.SOFT_VIOLET -> Palette(
        primary = if (dark) SoftViolet else Color(0xFF665DCB),
        secondary = if (dark) Color(0xFFB9BBC5) else Color(0xFF5C5E67),
        tertiary = if (dark) Color(0xFFB9BBC5) else Color(0xFF5C5E67)
    )
    AccentPalette.CLASSIC_GRAY -> Palette(
        primary = if (dark) Color(0xFFC2C3CA) else Color(0xFF51535B),
        secondary = if (dark) Color(0xFF9B9DA6) else Color(0xFF686A73),
        tertiary = if (dark) Color(0xFF9B9DA6) else Color(0xFF686A73)
    )
}

private fun themedColors(base: ColorScheme, accent: Palette, highContrast: Boolean): ColorScheme = base.copy(
    primary = accent.primary,
    secondary = accent.secondary,
    tertiary = accent.tertiary,
    outline = if (highContrast) base.onSurface else base.outline,
    onSurfaceVariant = if (highContrast) base.onSurface else base.onSurfaceVariant
)

private fun typography(scale: Float) = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (34 * scale).sp, lineHeight = (42 * scale).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (29 * scale).sp, lineHeight = (36 * scale).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (24 * scale).sp, lineHeight = (30 * scale).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (20 * scale).sp, lineHeight = (26 * scale).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (16 * scale).sp, lineHeight = (22 * scale).sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = (12 * scale).sp, lineHeight = (17 * scale).sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = (11 * scale).sp, lineHeight = (16 * scale).sp)
)

private fun shapes(scale: Float) = Shapes(
    extraSmall = RoundedCornerShape((6 * scale).dp),
    small = RoundedCornerShape((8 * scale).dp),
    medium = RoundedCornerShape((12 * scale).dp),
    large = RoundedCornerShape((16 * scale).dp),
    extraLarge = RoundedCornerShape((16 * scale).dp)
)

@Composable
fun RetraTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
    }
    val context = LocalContext.current
    val dynamic = settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        settings.themeMode == ThemeMode.OLED -> DarkBase.copy(background = Color.Black, surface = Color(0xFF050507), surfaceVariant = Color(0xFF101014))
        dark -> DarkBase
        else -> LightBase
    }
    val accented = themedColors(base, palette(settings.accentPalette, dark), settings.highContrast)
    // Keep the Material color scheme opaque for text fields, dialogs, and accessibility.
    // Liquid-glass translucency is applied deliberately by GlassPanel instead of globally.
    val colors = accented
    MaterialTheme(
        colorScheme = colors,
        typography = typography(settings.fontScale.coerceIn(0.85f, 1.3f)),
        shapes = shapes(settings.cornerScale.coerceIn(0.75f, 1.35f)),
        content = content
    )
}

@Composable
fun RetraTheme(themeMode: ThemeMode, dynamicColor: Boolean, content: @Composable () -> Unit) {
    RetraTheme(AppSettings(themeMode = themeMode, dynamicColor = dynamicColor), content)
}
