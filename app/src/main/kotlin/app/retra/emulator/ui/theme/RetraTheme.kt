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

val VoidBlack = Color(0xFF07080D)
val MidnightNavy = Color(0xFF101424)
val RetraIndigo = Color(0xFF7567FF)
val PrismCyan = Color(0xFF45D9FF)
val MemoryViolet = Color(0xFFB38BFF)
val SaveMint = Color(0xFF59E1AF)
val AdventureGold = Color(0xFFFFC766)
val ErrorCoral = Color(0xFFFF687C)
val CloudWhite = Color(0xFFF5F6FF)
val SoftCloud = Color(0xFFF6F6FC)
val DeepInk = Color(0xFF181925)
val MutedInk = Color(0xFF646675)

private val DarkBase = darkColorScheme(
    primary = RetraIndigo,
    onPrimary = CloudWhite,
    primaryContainer = Color(0xFF2C245F),
    onPrimaryContainer = Color(0xFFE7E1FF),
    secondary = PrismCyan,
    onSecondary = Color(0xFF003642),
    secondaryContainer = Color(0xFF004E5D),
    onSecondaryContainer = Color(0xFFB1ECFF),
    tertiary = MemoryViolet,
    background = VoidBlack,
    onBackground = CloudWhite,
    surface = MidnightNavy,
    onSurface = CloudWhite,
    surfaceVariant = Color(0xFF1A2034),
    onSurfaceVariant = Color(0xFFC7C9D7),
    outline = Color(0xFF8F93A6),
    error = ErrorCoral
)

private val LightBase = lightColorScheme(
    primary = Color(0xFF594AC9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E1FF),
    onPrimaryContainer = Color(0xFF19005E),
    secondary = Color(0xFF00677B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB1ECFF),
    onSecondaryContainer = Color(0xFF001F27),
    tertiary = Color(0xFF6B4E9B),
    background = SoftCloud,
    onBackground = DeepInk,
    surface = Color.White,
    onSurface = DeepInk,
    surfaceVariant = Color(0xFFE7E7EF),
    onSurfaceVariant = MutedInk,
    outline = Color(0xFF747585),
    error = Color(0xFFBA1A1A)
)

private data class Palette(val primary: Color, val secondary: Color, val tertiary: Color)

private fun palette(value: AccentPalette, dark: Boolean): Palette = when (value) {
    AccentPalette.RETRA_PRISM -> Palette(if (dark) RetraIndigo else Color(0xFF594AC9), if (dark) PrismCyan else Color(0xFF00677B), MemoryViolet)
    AccentPalette.AURORA -> Palette(Color(0xFF46D6B1), Color(0xFF70B7FF), Color(0xFFB98CFF))
    AccentPalette.EMERALD_CARTRIDGE -> Palette(Color(0xFF36C88A), Color(0xFF87D45A), Color(0xFFE0B94C))
    AccentPalette.SUNSET_GOLD -> Palette(Color(0xFFFFA640), Color(0xFFFF6E7E), Color(0xFFB98CFF))
    AccentPalette.ATOMIC_PURPLE -> Palette(Color(0xFFA56BFF), Color(0xFFFF74C8), Color(0xFF58D8FF))
    AccentPalette.GLACIER -> Palette(Color(0xFF5CA9FF), Color(0xFF55E2DD), Color(0xFFB3C7FF))
    AccentPalette.CLASSIC_GRAY -> Palette(Color(0xFF85889A), Color(0xFFA8ACB8), Color(0xFFD4B96C))
}

private fun themedColors(base: ColorScheme, accent: Palette, highContrast: Boolean): ColorScheme = base.copy(
    primary = accent.primary,
    secondary = accent.secondary,
    tertiary = accent.tertiary,
    outline = if (highContrast) base.onSurface else base.outline,
    onSurfaceVariant = if (highContrast) base.onSurface else base.onSurfaceVariant
)

private fun typography(scale: Float) = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = (36 * scale).sp, lineHeight = (42 * scale).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = (30 * scale).sp, lineHeight = (36 * scale).sp),
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
    extraSmall = RoundedCornerShape((8 * scale).dp),
    small = RoundedCornerShape((12 * scale).dp),
    medium = RoundedCornerShape((18 * scale).dp),
    large = RoundedCornerShape((24 * scale).dp),
    extraLarge = RoundedCornerShape((32 * scale).dp)
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
    val surfaceAlpha = if (settings.reduceTransparency) 1f else (0.72f + settings.glassIntensity.coerceIn(0f, 1f) * 0.28f)
    val colors = accented.copy(
        surfaceVariant = accented.surfaceVariant.copy(alpha = surfaceAlpha),
        primaryContainer = accented.primaryContainer.copy(alpha = surfaceAlpha)
    )
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
