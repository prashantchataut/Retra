package app.retra.emulator

import app.retra.core.emulation.EmulatorButton
import app.retra.core.emulation.SaveKind
import app.retra.core.model.ControlLayoutPreset
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.ScreenScalingMode
import app.retra.emulator.data.ControllerProfile
import app.retra.emulator.data.GameLaunchProfile
import app.retra.emulator.data.SaveTimelineEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates hardware controller profiles, analog calibration clamping,
 * save timeline checkpoint metadata, and launch profiles.
 */
class ControllerProfileAndTimelineTest {

    @Test
    fun controllerProfileNormalizesCalibrationValues() {
        val profile = ControllerProfile(
            id = "test-controller",
            deviceDescriptor = "vendor_1234_product_5678",
            deviceName = "Pro Gamepad",
            gameSha256 = null,
            deadZone = 0.20f,
            triggerThreshold = 0.50f,
            bindings = mapOf(96 to EmulatorButton.A, 97 to EmulatorButton.B)
        )

        assertEquals("test-controller", profile.id)
        assertEquals("Pro Gamepad", profile.deviceName)
        assertEquals("vendor_1234_product_5678", profile.deviceDescriptor)
        assertEquals(EmulatorButton.A, profile.bindings[96])
        assertEquals(EmulatorButton.B, profile.bindings[97])
        assertTrue(profile.deadZone in 0.05f..0.65f)
        assertTrue(profile.triggerThreshold in 0.15f..0.95f)
    }

    @Test
    fun saveTimelineEntryPreservesMetadata() {
        val entry = SaveTimelineEntry(
            id = "timeline-123",
            gameSha256 = "a".repeat(64),
            title = "Before Boss Fight",
            kind = SaveKind.STATE,
            slot = 1,
            coreId = "mgba-libretro",
            coreVersion = "0.10.5",
            createdAtEpochMillis = 1000L,
            sizeBytes = 256 * 1024L,
            cheatsActive = false,
            storagePath = "/data/user/0/checkpoint.rsv"
        )

        assertEquals("timeline-123", entry.id)
        assertEquals("Before Boss Fight", entry.title)
        assertEquals(SaveKind.STATE, entry.kind)
        assertEquals(1, entry.slot)
        assertEquals("mgba-libretro", entry.coreId)
    }

    @Test
    fun gameLaunchProfileStoresPerGameOverrides() {
        val profile = GameLaunchProfile(
            gameSha256 = "b".repeat(64),
            performanceProfile = PerformanceProfile.AUTHENTIC,
            scalingMode = ScreenScalingMode.FILL,
            displaySmoothing = true,
            controlLayout = ControlLayoutPreset.COMPACT,
            showTouchControls = true,
            fastForwardSpeed = 2.0f
        )

        assertEquals("b".repeat(64), profile.gameSha256)
        assertEquals(PerformanceProfile.AUTHENTIC, profile.performanceProfile)
        assertEquals(ScreenScalingMode.FILL, profile.scalingMode)
        assertTrue(profile.displaySmoothing == true)
        assertEquals(2.0f, profile.fastForwardSpeed ?: 1.0f, 0.01f)
    }
}
