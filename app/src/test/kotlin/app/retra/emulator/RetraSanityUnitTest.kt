package app.retra.emulator

import app.retra.core.model.AppSettings
import app.retra.core.model.ThemeMode
import app.retra.emulator.data.GameEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM unit test sanity verification for the app module.
 */
class RetraSanityUnitTest {

    @Test
    fun appSanityCheck() {
        assertTrue(true)
    }

    @Test
    fun appSettingsDefaultsAreValid() {
        val settings = AppSettings()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.audioEnabled)
    }

    @Test
    fun csvEncodingAndDecodingInGameEntity() {
        val list = listOf("RPG", "Action")
        val encoded = GameEntity.encodeCsv(list)
        assertEquals("RPG|Action", encoded)
        val decoded = GameEntity.decodeCsv(encoded)
        assertEquals(list, decoded)
    }

    @Test
    fun formatBytesHelperWorksCorrectly() {
        assertEquals("1.0 KiB", formatBytes(1024L))
        assertEquals("1.0 MiB", formatBytes(1024L * 1024L))
        assertEquals("500 B", formatBytes(500L))
    }
}
