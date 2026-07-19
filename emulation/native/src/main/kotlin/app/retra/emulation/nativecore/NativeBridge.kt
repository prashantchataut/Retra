package app.retra.emulation.nativecore

internal object NativeBridge {
    init {
        System.loadLibrary("retra_native")
    }

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeLoadRom(handle: Long, rom: ByteArray, sha256: String): Boolean
    external fun nativeStep(handle: Long, inputMask: Int, speedMultiplier: Float): IntArray
    external fun nativeSerialize(handle: Long): ByteArray
    external fun nativeDeserialize(handle: Long, state: ByteArray): Boolean
    external fun nativeReset(handle: Long)
}
