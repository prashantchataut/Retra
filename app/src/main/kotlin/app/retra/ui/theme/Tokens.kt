package app.retra.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.retra.R

/*
 * Retra design tokens. See app/DESIGN.md. Components consume only these semantic tokens.
 */

/** Semantic colour layer; light ("mint mist") and dark ("deep matcha") are separate palettes. */
@Immutable
data class RetraColors(
    val isDark: Boolean,
    val sky: Color,
    val skyGlowA: Color,
    val skyGlowB: Color,
    val skyGlowC: Color,
    val glassFill: Color,
    val glassFillStrong: Color,
    val glassRimTop: Color,
    val glassRimBottom: Color,
    val glassSheen: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val shadow: Color,
    val scrim: Color,
    val divider: Color,
    /** Mochi pink, for the few brand moments (favourites, the logo); never for text. */
    val brand: Color,
)

// Light: a cool mint-white ground, deep pine ink, and ink-coloured primary pills (Mochi stays the colour).
val LightColors = RetraColors(
    isDark = false,
    sky = Color(0xFFEEF5F2),
    skyGlowA = Color(0xFFC6EEDC),
    skyGlowB = Color(0xFFCBE6F5),
    skyGlowC = Color(0xFFFBE1E8),
    glassFill = Color.White.copy(alpha = 0.66f),
    glassFillStrong = Color(0xFFFBFEFC).copy(alpha = 0.92f),
    glassRimTop = Color.White.copy(alpha = 0.95f),
    glassRimBottom = Color.White.copy(alpha = 0.12f),
    glassSheen = Color.White.copy(alpha = 0.5f),
    ink = Color(0xFF14231E),
    inkMuted = Color(0xFF52645C),
    inkFaint = Color(0xFF14231E).copy(alpha = 0.08f),
    accent = Color(0xFF17302A),
    onAccent = Color.White,
    accentSoft = Color(0xFFD2F0E3),
    onAccentSoft = Color(0xFF0D4A35),
    success = Color(0xFF1E7A50),
    warning = Color(0xFF8F5A0A),
    danger = Color(0xFFC0364A),
    shadow = Color(0xFF2E5A4C).copy(alpha = 0.14f),
    scrim = Color(0xFF14231E).copy(alpha = 0.30f),
    divider = Color(0xFF14231E).copy(alpha = 0.07f),
    brand = Color(0xFFE8628F),
)

// Dark: a green-black ground with lighter glass steps for depth (no coloured glows), soft mint actions.
val DarkColors = RetraColors(
    isDark = true,
    sky = Color(0xFF0C1412),
    skyGlowA = Color(0xFF10291F),
    skyGlowB = Color(0xFF0D2329),
    skyGlowC = Color(0xFF11201E),
    glassFill = Color(0xFF1A2824).copy(alpha = 0.70f),
    glassFillStrong = Color(0xFF141F1C).copy(alpha = 0.96f),
    glassRimTop = Color.White.copy(alpha = 0.16f),
    glassRimBottom = Color.White.copy(alpha = 0.03f),
    glassSheen = Color.White.copy(alpha = 0.05f),
    ink = Color(0xFFE8F1ED),
    inkMuted = Color(0xFF9EB1A9),
    inkFaint = Color(0xFFE8F1ED).copy(alpha = 0.10f),
    accent = Color(0xFF9BE8C9),
    onAccent = Color(0xFF06201A),
    accentSoft = Color(0xFF173A30),
    onAccentSoft = Color(0xFFB9F2DA),
    success = Color(0xFF7FE0AE),
    warning = Color(0xFFF2C46E),
    danger = Color(0xFFFF8F9C),
    shadow = Color.Black.copy(alpha = 0.45f),
    scrim = Color(0xFF030806).copy(alpha = 0.60f),
    divider = Color.White.copy(alpha = 0.07f),
    brand = Color(0xFFFF9DBB),
)

/** Mochi's own palette (mascot + brand art); not used for UI chrome. */
object MochiPalette {
    val outline = Color(0xFF5A2A4E)
    val shade = Color(0xFFE8849F)
    val base = Color(0xFFF9B3C9)
    val light = Color(0xFFFFD6E2)
    val cream = Color(0xFFFFF6EC)
    val blush = Color(0xFFFF7B9C)
    val eye = Color(0xFF35203F)
    val leafDark = Color(0xFF3D8A58)
    val leaf = Color(0xFF6FCF7A)
    val leafLight = Color(0xFFB9F2A2)
    val mouth = Color(0xFFB8486A)
}

/** Soft serif for display text (static Fraunces instances: SOFT 100, WONK 0). */
val Fraunces = FontFamily(
    Font(R.font.fraunces_medium, FontWeight.Medium),
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

/** The 7-step type scale. */
@Immutable
object RetraType {
    val display = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.6).sp, lineHeightStyle = Trim)
    val title = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.3).sp, lineHeightStyle = Trim)
    val headline = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.1).sp, lineHeightStyle = Trim)
    val bodyStrong = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 22.sp, lineHeightStyle = Trim)
    val body = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp, lineHeightStyle = Trim)
    val label = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp, lineHeightStyle = Trim)
    val caption = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp, lineHeightStyle = Trim)
}

/** 4 dp spacing grid. */
object Space {
    val xxs = 4.dp
    val xs = 8.dp
    val s = 12.dp
    val m = 16.dp
    val l = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val gutter = 20.dp
    val touch = 48.dp
}

/** One radius family. */
object Radius {
    val small = 12.dp
    val medium = 20.dp
    val large = 28.dp
    val smallShape = RoundedCornerShape(small)
    val mediumShape = RoundedCornerShape(medium)
    val largeShape = RoundedCornerShape(large)
    val pillShape = RoundedCornerShape(percent = 50)
}

/** Three elevation levels. */
object Elevation {
    val raised = 6.dp
    val floating = 16.dp
    val overlay = 28.dp
}

/** One spring family (damping 0.82) at three stiffnesses. Reduced motion turns them into snaps. */
object Motion {
    const val DAMPING = 0.82f
    const val SNAPPY = 1400f
    const val SMOOTH = 500f
    const val GENTLE = 200f

    fun <T> snappy(reduce: Boolean): FiniteAnimationSpec<T> = if (reduce) snap() else spring(DAMPING, SNAPPY)
    fun <T> smooth(reduce: Boolean): FiniteAnimationSpec<T> = if (reduce) snap() else spring(DAMPING, SMOOTH)
    fun <T> gentle(reduce: Boolean): FiniteAnimationSpec<T> = if (reduce) snap() else spring(DAMPING, GENTLE)

    /** Visibility threshold-friendly spring for layout/size animations. */
    fun <T> smoothSpring(visibilityThreshold: T? = null) = spring(DAMPING, SMOOTH, visibilityThreshold)
    const val STAGGER_MS = 40
    const val MAX_STAGGERED = 8
    val NoBounce = Spring.DampingRatioNoBouncy
}
