package app.retra.emulator.onboarding

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Retra Onboarding Design System: "MIDNIGHT MEMORY"
 *
 * Grounded in pure darkness (#0A0810) with near-black plum atmospheric hints,
 * uncompromising contrast for text (18.3:1), and distinct single-accent chapters.
 */
object OnboardingTokens {
    // Foundational Canvas
    val MidnightBlack = Color(0xFF0A0810)
    val PlumAtmosphere1 = Color(0xFF130D1C)
    val PlumAtmosphere2 = Color(0xFF1B1026)
    val PlumAtmosphere3 = Color(0xFF21132D)
    val GlassSurface = Color(0xFF161024)

    // Typography Colors
    val TextPrimary = Color(0xFFF7F4FF)     // 18.3:1 contrast against MidnightBlack
    val TextSecondary = Color(0xFFC8C2D8)   // 11.5:1 contrast against MidnightBlack
    val TextMuted = Color(0xFF7E7692)

    // Chapter Accent Palette
    val ElectricLavender = Color(0xFFB998FF) // Chapter 1 & 3 Hero
    val MemoryPink = Color(0xFFFF5CA8)       // Chapter 2 Hero
    val AcidNostalgia = Color(0xFFD7FF4F)    // Playful accents
    val DreamCyan = Color(0xFF64E6D2)        // Chapter 3 complementary
    val CartridgeCoral = Color(0xFFFF8A65)   // Warm nostalgia

    // Typography Scale
    val WordmarkHero = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 68.sp,
        lineHeight = 68.sp,
        letterSpacing = (-2.5).sp,
        color = TextPrimary
    )

    val ChapterTitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.2).sp,
        color = TextPrimary
    )

    val ChapterSubtitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        color = TextSecondary
    )

    val FloatingWord = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    )

    val ButtonCta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    )

    val MicroLabel = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}
