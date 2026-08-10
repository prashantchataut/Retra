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

/**
 * Retra Design System: Sophisticated Deep Aubergine / Liquid Glass Palette.
 *
 * Grounded in near-black aubergine and deep plum-charcoal with electric lilac,
 * raspberry, warm coral, mint aqua, and warm nostalgic cream accents.
 */
val VoidBlack = Color(0xFF09070D)
val NightPlum = Color(0xFF13101A)
val SurfaceMidnight = Color(0xFF1B1624)
val SurfaceElevated = Color(0xFF261F33)
val SurfaceHighlight = Color(0xFF332A44)
val Graphite = Color(0xFF14131A)
val InkBlue = Color(0xFF161522)

// Accents
val ElectricLilac = Color(0xFFC7ACFC)
val SoftViolet = Color(0xFFB898F8)
val RaspberryPink = Color(0xFFFF6B8B)
val MemoryCoral = Color(0xFFFF9376)
val PeachGlow = Color(0xFFFFB088)
val SaveMint = Color(0xFF5EEAD4)
val MemoryAqua = Color(0xFF48D1B0)
val AdventureGold = Color(0xFFFFD166)
val WarmCream = Color(0xFFFFF4E0)
val ErrorCoral = Color(0xFFFF6B7A)

// Text & Surfaces
val CloudWhite = Color(0xFFF7F4FB)
val SoftCloud = Color(0xFFEDE8F5)
val IceMist = Color(0xFFDCD4E8)
val DeepInk = Color(0xFF0F0D14)
val MutedInk = Color(0xFFA197B4)
val SubduedInk = Color(0xFF746A88)

// Compatibility aliases
val RetraBlue = ElectricLilac
val FrostBlue = SoftViolet
val RetraIndigo = ElectricLilac
val PrismCyan = SaveMint
val MemoryViolet = SoftViolet

private val DarkBase = darkColorScheme(
    primary = ElectricLilac,
    onPrimary = Color(0xFF28104E),
    primaryContainer = Color(0xFF442B70),
    onPrimaryContainer = Color(0xFFEBDCFF),
    secondary = SaveMint,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF1B4E47),
    onSecondaryContainer = Color(0xFF86F7E5),
    tertiary = MemoryCoral,
    onTertiary = Color(0xFF3E1208),
    tertiaryContainer = Color(0xFF65291C),
    onTertiaryContainer = Color(0xFFFFDBD2),
    background = VoidBlack,
    onBackground = CloudWhite,
    surface = NightPlum,
    onSurface = CloudWhite,
    surfaceVariant = SurfaceMidnight,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFF5A4F6E),
    outlineVariant = Color(0xFF2C243B),
    error = ErrorCoral,
    onError = Color(0xFF41000C),
    errorContainer = Color(0xFF651B27),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightBase = lightColorScheme(
    primary = Color(0xFF6748A8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDCFF),
    onPrimaryContainer = Color(0xFF23005A),
    secondary = Color(0xFF006B5F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF73F8E4),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF944535),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD2),
    onTertiaryContainer = Color(0xFF3B0801),
    background = SoftCloud,
    onBackground = DeepInk,
    surface = Color(0xFFFCFAFF),
    onSurface = DeepInk,
    surfaceVariant = IceMist,
    onSurfaceVariant = SubduedInk,
    outline = Color(0xFF7E7292),
    outlineVariant = Color(0xFFCEC2E0),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private data class Palette(val primary: Color, val secondary: Color, val tertiary: Color)

private fun palette(value: AccentPalette, dark: Boolean): Palette = when (value) {
    AccentPalette.RETRA_INDIGO -> Palette(
        primary = if (dark) ElectricLilac else Color(0xFF6748A8),
        secondary = if (dark) SaveMint else Color(0xFF006B5F),
        tertiary = if (dark) MemoryCoral else Color(0xFF944535)
    )
    AccentPalette.GRAPHITE -> Palette(
        primary = if (dark) Color(0xFFDED8EB) else Color(0xFF534C60),
        secondary = if (dark) ElectricLilac else Color(0xFF6748A8),
        tertiary = if (dark) AdventureGold else Color(0xFF7A5900)
    )
    AccentPalette.SOFT_VIOLET -> Palette(
        primary = if (dark) SoftViolet else Color(0xFF593E94),
        secondary = if (dark) RaspberryPink else Color(0xFF9E2A4D),
        tertiary = if (dark) MemoryCoral else Color(0xFF944535)
    )
    AccentPalette.CLASSIC_GRAY -> Palette(
        primary = if (dark) Color(0xFFECE7F4) else Color(0xFF5E576B),
        secondary = if (dark) Color(0xFFCCC4D9) else Color(0xFF645D70),
        tertiary = if (dark) AdventureGold else Color(0xFF7A5900)
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
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = (38 * scale).sp,
        lineHeight = (42 * scale).sp,
        letterSpacing = (-1.2 * scale).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = (30 * scale).sp,
        lineHeight = (34 * scale).sp,
        letterSpacing = (-0.7 * scale).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = (24 * scale).sp,
        lineHeight = (29 * scale).sp,
        letterSpacing = (-0.3 * scale).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = (20 * scale).sp,
        lineHeight = (25 * scale).sp,
        letterSpacing = (-0.15 * scale).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (16 * scale).sp,
        lineHeight = (22 * scale).sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (14 * scale).sp,
        lineHeight = (20 * scale).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * scale).sp,
        lineHeight = (24 * scale).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * scale).sp,
        lineHeight = (21 * scale).sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = (12 * scale).sp,
        lineHeight = (18 * scale).sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = (14 * scale).sp,
        lineHeight = (20 * scale).sp,
        letterSpacing = (0.05 * scale).sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = (12 * scale).sp,
        lineHeight = (16 * scale).sp,
        letterSpacing = (0.1 * scale).sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = (11 * scale).sp,
        lineHeight = (16 * scale).sp,
        letterSpacing = (0.15 * scale).sp
    )
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
    val base = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> {
            dynamicDarkColorScheme(context)
        }
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }
        settings.themeMode == ThemeMode.OLED -> DarkBase.copy(
            background = Color.Black,
            surface = Color(0xFF08060B),
            surfaceVariant = Color(0xFF130F1A)
        )
        dark -> DarkBase
        else -> LightBase
    }
    val colors = themedColors(base, palette(settings.accentPalette, dark), settings.highContrast)
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
