package app.retra.emulation.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates emulation core contracts and core tier differentiation.
 */
class EmulationCoreSelectionTest {

    @Test
    fun mgbaCoreDescriptorDeclaresGameplayTierAndFeatures() {
        val descriptor = CoreDescriptor(
            id = "mgba-libretro",
            displayName = "mGBA",
            version = "0.10.5",
            tier = CoreTier.GBA_GAMEPLAY,
            supportsBatterySaves = true,
            supportsSaveStates = true,
            supportsAudio = true,
            supportsCheats = true,
            supportsRewind = true,
            legalNotice = "mGBA is distributed under MPL-2.0. Retra requires the corresponding bundled source notices and source offer."
        )

        assertEquals("mgba-libretro", descriptor.id)
        assertEquals("mGBA", descriptor.displayName)
        assertEquals("0.10.5", descriptor.version)
        assertEquals(CoreTier.GBA_GAMEPLAY, descriptor.tier)
        assertTrue(descriptor.supportsBatterySaves)
        assertTrue(descriptor.supportsSaveStates)
        assertTrue(descriptor.supportsAudio)
        assertTrue(descriptor.supportsCheats)
        assertTrue(descriptor.supportsRewind)
        assertTrue(descriptor.legalNotice.contains("MPL-2.0"))
    }

    @Test
    fun referenceCoreDescriptorDeclaresDiagnosticsTier() {
        val descriptor = CoreDescriptor(
            id = "native-reference",
            displayName = "Retra Reference Diagnostic",
            version = "1.0",
            tier = CoreTier.DIAGNOSTIC_PIPELINE,
            supportsBatterySaves = false,
            supportsSaveStates = true,
            supportsAudio = false,
            supportsCheats = false,
            supportsRewind = true,
            legalNotice = "Retra Reference core is for internal diagnostics only and does not emulate commercial Game Boy Advance hardware."
        )

        assertEquals("native-reference", descriptor.id)
        assertEquals(CoreTier.DIAGNOSTIC_PIPELINE, descriptor.tier)
        assertFalse(descriptor.supportsBatterySaves)
        assertFalse(descriptor.supportsCheats)
        assertFalse(descriptor.supportsAudio)
        assertTrue(descriptor.legalNotice.contains("diagnostics only"))
    }

    @Test
    fun coreTiersAreDistinctAndEnforceGameplayBoundary() {
        val gameplayTier = CoreTier.GBA_GAMEPLAY
        val diagnosticTier = CoreTier.DIAGNOSTIC_PIPELINE

        assertTrue(gameplayTier != diagnosticTier)
        assertEquals("GBA_GAMEPLAY", gameplayTier.name)
        assertEquals("DIAGNOSTIC_PIPELINE", diagnosticTier.name)
    }
}
