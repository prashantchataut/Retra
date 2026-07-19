package app.retra.emulation.nativecore

internal object MgbaBridge {
    init {
        System.loadLibrary("retra_native")
    }

    external fun nativeIsAvailable(): Boolean
    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeLoadRom(handle: Long, rom: ByteArray): Boolean
    external fun nativeStep(handle: Long, inputMask: Int, speedMultiplier: Float): IntArray
    external fun nativeDrainAudio(handle: Long): ShortArray
    external fun nativeSerialize(handle: Long): ByteArray
    external fun nativeDeserialize(handle: Long, state: ByteArray): Boolean
    external fun nativeBatterySave(handle: Long): ByteArray
    external fun nativeRestoreBattery(handle: Long, save: ByteArray): Boolean
    external fun nativeSetCheats(handle: Long, codes: Array<String>): Boolean
    external fun nativeClearCheats(handle: Long)
    external fun nativeReset(handle: Long)
    external fun nativeWidth(handle: Long): Int
    external fun nativeHeight(handle: Long): Int
    external fun nativeSampleRate(handle: Long): Int
}
