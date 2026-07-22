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

/** Retra 1.0 "Archive Glass" palette: mineral, cool, and deliberately non-purple. */
val VoidBlack = Color(0xFF050A0D)
val NightNavy = Color(0xFF091219)
val Graphite = Color(0xFF101A20)
val InkBlue = Color(0xFF16262F)
val RetraBlue = Color(0xFF75D7F2)
val FrostBlue = Color(0xFFB7ECF8)
val MemoryAqua = Color(0xFF66E1D1)
val MemoryCoral = Color(0xFFFF806F)
val SaveMint = Color(0xFF62D99E)
val AdventureGold = Color(0xFFFFC65C)
val ErrorCoral = Color(0xFFFF6F78)
val CloudWhite = Color(0xFFF7FBFC)
val SoftCloud = Color(0xFFF1F5F6)
val IceMist = Color(0xFFE5EFF1)
val DeepInk = Color(0xFF10191D)
val MutedInk = Color(0xFF5E6C72)

// Compatibility aliases retained for existing feature surfaces and persisted enum values.
val RetraIndigo = RetraBlue
val SoftViolet = FrostBlue
val PrismCyan = RetraBlue
val MemoryViolet = FrostBlue

private val DarkBase = darkColorScheme(
    primary = RetraBlue,
    onPrimary = Color(0xFF002832),
    primaryContainer = Color(0xFF123C47),
    onPrimaryContainer = Color(0xFFCAF4FF),
    secondary = MemoryAqua,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = Color(0xFF123E3A),
    onSecondaryContainer = Color(0xFFCCF8F1),
    tertiary = MemoryCoral,
    onTertiary = Color(0xFF3B0904),
    tertiaryContainer = Color(0xFF5E302A),
    onTertiaryContainer = Color(0xFFFFDAD3),
    background = VoidBlack,
    onBackground = CloudWhite,
    surface = NightNavy,
    onSurface = CloudWhite,
    surfaceVariant = InkBlue,
    onSurfaceVariant = Color(0xFFC3D0D4),
    outline = Color(0xFF82949B),
    outlineVariant = Color(0xFF2A3A42),
    error = ErrorCoral,
    onError = Color(0xFF3B0710),
    errorContainer = Color(0xFF5B2430),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightBase = lightColorScheme(
    primary = Color(0xFF00677B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EBF6),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Color(0xFF006B60),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA4F2E5),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFFA43D31),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD4),
    onTertiaryContainer = Color(0xFF410001),
    background = SoftCloud,
    onBackground = DeepInk,
    surface = Color(0xFFFAFDFD),
    onSurface = DeepInk,
    surfaceVariant = IceMist,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFF6D7B80),
    outlineVariant = Color(0xFFC8D5D8),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

private data class Palette(val primary: Color, val secondary: Color, val tertiary: Color)

/**
 * Enum names are intentionally preserved so existing DataStore values keep loading.
 * The visible colors no longer contain violet or purple.
 */
private fun palette(value: AccentPalette, dark: Boolean): Palette = when (value) {
    AccentPalette.RETRA_INDIGO -> Palette(
        primary = if (dark) RetraBlue else Color(0xFF00677B),
        secondary = if (dark) MemoryAqua else Color(0xFF006B60),
        tertiary = if (dark) MemoryCoral else Color(0xFFA43D31)
    )
    AccentPalette.GRAPHITE -> Palette(
        primary = if (dark) Color(0xFFD4E0E3) else Color(0xFF4D5A60),
        secondary = if (dark) RetraBlue else Color(0xFF00677B),
        tertiary = if (dark) AdventureGold else Color(0xFF775A00)
    )
    AccentPalette.SOFT_VIOLET -> Palette(
        primary = if (dark) FrostBlue else Color(0xFF286A78),
        secondary = if (dark) MemoryAqua else Color(0xFF006B60),
        tertiary = if (dark) MemoryCoral else Color(0xFFA43D31)
    )
    AccentPalette.CLASSIC_GRAY -> Palette(
        primary = if (dark) Color(0xFFE0E6E8) else Color(0xFF50595D),
        secondary = if (dark) Color(0xFFBAC7CA) else Color(0xFF5C686C),
        tertiary = if (dark) AdventureGold else Color(0xFF775A00)
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
        fontSize = (40 * scale).sp,
        lineHeight = (43 * scale).sp,
        letterSpacing = (-1.15 * scale).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = (31 * scale).sp,
        lineHeight = (35 * scale).sp,
        letterSpacing = (-0.65 * scale).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = (25 * scale).sp,
        lineHeight = (30 * scale).sp,
        letterSpacing = (-0.25 * scale).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = (20 * scale).sp,
        lineHeight = (25 * scale).sp,
        letterSpacing = (-0.1 * scale).sp
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
    extraLarge = RoundedCornerShape((30 * scale).dp)
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
        settings.themeMode == ThemeMode.OLED -> DarkBase.copy(
            background = Color.Black,
            surface = Color(0xFF05090B),
            surfaceVariant = Color(0xFF0E171C)
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
