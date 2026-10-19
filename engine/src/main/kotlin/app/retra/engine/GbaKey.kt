package app.retra.engine

/** GBA button bits for [GbaCore.setKeys]; a set bit means the button is pressed. */
object GbaKey {
    const val A = 1
    const val B = 2
    const val SELECT = 4
    const val START = 8
    const val RIGHT = 16
    const val LEFT = 32
    const val UP = 64
    const val DOWN = 128
    const val R = 256
    const val L = 512
}
