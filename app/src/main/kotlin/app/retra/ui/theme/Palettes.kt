package app.retra.ui.theme

import androidx.compose.ui.graphics.Color
import app.retra.data.AppPalette

/**
 * Colour worlds. Each palette overrides the same tokens as the default "mint mist" / "deep matcha" pair and keeps
 * the semantic colours (success / warning / danger) so state still reads the same everywhere.
 * Text pairs keep at least 4.5:1 against their ground (checked in `PaletteTest`).
 */
private class Tone(
    val sky: Long, val glowA: Long, val glowB: Long, val glowC: Long,
    val glass: Long, val glassStrong: Long,
    val ink: Long, val inkMuted: Long,
    val accent: Long, val onAccent: Long, val accentSoft: Long, val onAccentSoft: Long,
    val brand: Long, val shadow: Long,
)

private fun c(v: Long) = Color(v or 0xFF000000L)

private fun build(base: RetraColors, t: Tone): RetraColors = base.copy(
    sky = c(t.sky), skyGlowA = c(t.glowA), skyGlowB = c(t.glowB), skyGlowC = c(t.glowC),
    glassFill = if (base.isDark) c(t.glass).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.66f),
    glassFillStrong = c(t.glassStrong).copy(alpha = if (base.isDark) 0.96f else 0.92f),
    ink = c(t.ink), inkMuted = c(t.inkMuted), inkFaint = c(t.ink).copy(alpha = if (base.isDark) 0.10f else 0.08f),
    accent = c(t.accent), onAccent = c(t.onAccent), accentSoft = c(t.accentSoft), onAccentSoft = c(t.onAccentSoft),
    brand = c(t.brand),
    shadow = c(t.shadow).copy(alpha = if (base.isDark) 0.45f else 0.14f),
    scrim = c(if (base.isDark) 0x030806 else t.ink).copy(alpha = if (base.isDark) 0.60f else 0.30f),
    divider = if (base.isDark) Color.White.copy(alpha = 0.07f) else c(t.ink).copy(alpha = 0.07f),
)

private val light = mapOf(
    AppPalette.SAKURA to Tone(0xFBF1F4, 0xFFE0EA, 0xFFEFD8, 0xE6E2FA, 0xFFFFFF, 0xFFFAFB, 0x2A1720, 0x6B4F5B, 0x8A1F4B, 0xFFFFFF, 0xFAD7E4, 0x6E1239, 0xE8628F, 0x7A3B52),
    AppPalette.LAVENDER to Tone(0xF3F0FB, 0xE0D7FA, 0xD3E4FA, 0xFADFEF, 0xFFFFFF, 0xFBFAFF, 0x1D1830, 0x5A5478, 0x3F2E8F, 0xFFFFFF, 0xDDD5F8, 0x2E2070, 0xB490F0, 0x4B3C8A),
    AppPalette.SUNSET to Tone(0xFFF5EC, 0xFFDFC2, 0xFFD2D2, 0xFFF0B6, 0xFFFFFF, 0xFFFBF7, 0x2B1B10, 0x6B5444, 0xA13D0B, 0xFFFFFF, 0xFFDDC2, 0x7A2C05, 0xF28B4E, 0x8A4A24),
    AppPalette.OCEAN to Tone(0xEDF5FA, 0xC4E4F7, 0xCEDFFA, 0xC6F1EC, 0xFFFFFF, 0xF8FCFF, 0x0F2230, 0x4C6677, 0x0B4F7A, 0xFFFFFF, 0xCFE8F7, 0x063A5C, 0x3FA7D6, 0x2A5F80),
    AppPalette.POCKET to Tone(0xEEF2D8, 0xD3E398, 0xC6DB86, 0xE5EEB6, 0xFFFFFF, 0xFAFCEF, 0x0F2A0F, 0x45602E, 0x0F380F, 0xE9F0C2, 0xD2E08E, 0x1E4A12, 0x8BAC0F, 0x2F4A1C),
)

private val dark = mapOf(
    AppPalette.SAKURA to Tone(0x160E12, 0x2A1220, 0x1E1424, 0x24160F, 0x2A1B22, 0x1F1419, 0xF5E8ED, 0xBCA0AC, 0xFFB3CD, 0x3B0A1F, 0x45202F, 0xFFD6E4, 0xFF9DBB, 0x000000),
    AppPalette.LAVENDER to Tone(0x0F0D1A, 0x1C1740, 0x131A33, 0x241438, 0x221E38, 0x181528, 0xECE9F8, 0xA9A3C8, 0xC4B5FD, 0x1B1140, 0x2D2557, 0xE0D8FF, 0xC9B3FF, 0x000000),
    AppPalette.SUNSET to Tone(0x17100B, 0x2D1A0E, 0x2A1313, 0x2A220C, 0x2B1F17, 0x201710, 0xF8EBDF, 0xC2A894, 0xFFB27A, 0x3A1600, 0x4A2A14, 0xFFD9BC, 0xFFB27A, 0x000000),
    AppPalette.OCEAN to Tone(0x0A1218, 0x0C2535, 0x0D1C38, 0x0B2A2B, 0x16252F, 0x101C25, 0xE6F1F8, 0x9AB4C4, 0x8FD3FF, 0x03263D, 0x123A52, 0xC4E8FF, 0x7CCBF5, 0x000000),
    AppPalette.POCKET to Tone(0x0B1A0B, 0x12300F, 0x1A3A12, 0x0F2A14, 0x1A2E15, 0x132410, 0xD7EBA0, 0x93AE62, 0x9BBC0F, 0x0B1A0B, 0x264016, 0xD0E68A, 0xB4D43A, 0x000000),
)

/** The token set for [palette] in light or dark. Mint is the base tokens untouched. */
fun paletteColors(palette: AppPalette, isDark: Boolean): RetraColors {
    val base = if (isDark) DarkColors else LightColors
    val tone = (if (isDark) dark else light)[palette] ?: return base
    return build(base, tone)
}

/** Two swatches that preview a palette in the picker: the ground and the accent. */
fun paletteSwatch(palette: AppPalette, isDark: Boolean): Pair<Color, Color> =
    paletteColors(palette, isDark).let { it.skyGlowA to it.accent }
