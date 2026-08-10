package app.retra.emulator

import app.retra.core.model.AppSettings
import app.retra.core.model.ThemeMode
import app.retra.emulator.data.GameEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates core app settings defaults and CSV serialization helpers on JVM.
 */
class RetraSanityUnitTest {

    @Test
    fun appSettingsHasExpectedDefaults() {
        val settings = AppSettings()
        assertFalse(settings.onboardingComplete)
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.showTouchControls)
        assertTrue(settings.audioEnabled)
        assertEquals(1f, settings.masterVolume, 0.01f)
    }

    @Test
    fun csvTagSerializationRoundTripsCorrectly() {
        val original = listOf("rpg", "favorites", "gba-classic")
        val csv = GameEntity.encodeCsv(original)
        val parsed = GameEntity.decodeCsv(csv)
        assertEquals(original, parsed)
    }

    @Test
    fun emptyCsvReturnsEmptyList() {
        val parsed = GameEntity.decodeCsv("")
        assertTrue(parsed.isEmpty())
    }

    @Test
    fun formatBytesFormatsProperUnits() {
        assertEquals("0 B", formatBytes(0L))
        assertEquals("512 B", formatBytes(512L))
        assertEquals("1.0 KiB", formatBytes(1024L))
        assertEquals("16.0 MiB", formatBytes(16 * 1024 * 1024L))
    }
}
