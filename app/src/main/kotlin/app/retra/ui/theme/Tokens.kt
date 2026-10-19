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

/** Semantic colour layer; light ("cloud cream") and dark ("night indigo") are separate palettes. */
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
)

val LightColors = RetraColors(
    isDark = false,
    sky = Color(0xFFFBF5F1),
    skyGlowA = Color(0xFFFFD3E1),
    skyGlowB = Color(0xFFFFE6C7),
    skyGlowC = Color(0xFFD7E4FF),
    glassFill = Color.White.copy(alpha = 0.62f),
    glassFillStrong = Color(0xFFFFFCFA).copy(alpha = 0.90f),
    glassRimTop = Color.White.copy(alpha = 0.95f),
    glassRimBottom = Color.White.copy(alpha = 0.10f),
    glassSheen = Color.White.copy(alpha = 0.55f),
    ink = Color(0xFF2B2130),
    inkMuted = Color(0xFF6B5E70),
    inkFaint = Color(0xFF2B2130).copy(alpha = 0.10f),
    accent = Color(0xFFC7366B),
    onAccent = Color.White,
    accentSoft = Color(0xFFFFE1EA),
    onAccentSoft = Color(0xFF6E1739),
    success = Color(0xFF23794F),
    warning = Color(0xFF965C0A),
    danger = Color(0xFFC4364A),
    shadow = Color(0xFF7A4A60).copy(alpha = 0.16f),
    scrim = Color(0xFF2B2130).copy(alpha = 0.32f),
    divider = Color(0xFF2B2130).copy(alpha = 0.08f),
)

val DarkColors = RetraColors(
    isDark = true,
    sky = Color(0xFF15132B),
    skyGlowA = Color(0xFF3B2C66),
    skyGlowB = Color(0xFF2A3A6E),
    skyGlowC = Color(0xFF4A2A55),
    glassFill = Color(0xFF2C2950).copy(alpha = 0.62f),
    glassFillStrong = Color(0xFF232044).copy(alpha = 0.94f),
    glassRimTop = Color.White.copy(alpha = 0.32f),
    glassRimBottom = Color.White.copy(alpha = 0.04f),
    glassSheen = Color.White.copy(alpha = 0.10f),
    ink = Color(0xFFF4EFFB),
    inkMuted = Color(0xFFB9B0CE),
    inkFaint = Color(0xFFF4EFFB).copy(alpha = 0.12f),
    accent = Color(0xFFFF8FB4),
    onAccent = Color(0xFF2A0E1D),
    accentSoft = Color(0xFF4A2846),
    onAccentSoft = Color(0xFFFFD6E4),
    success = Color(0xFF7FE0AE),
    warning = Color(0xFFF6C56E),
    danger = Color(0xFFFF8F9C),
    shadow = Color.Black.copy(alpha = 0.40f),
    scrim = Color(0xFF07060F).copy(alpha = 0.55f),
    divider = Color.White.copy(alpha = 0.08f),
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

val Fredoka = FontFamily(
    Font(R.font.fredoka_medium, FontWeight.Medium),
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
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
    val display = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.3).sp, lineHeightStyle = Trim)
    val title = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp, lineHeightStyle = Trim)
    val headline = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 26.sp, lineHeightStyle = Trim)
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
