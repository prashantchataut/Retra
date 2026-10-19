package app.retra.engine

/** Information read from the ROM header plus detected hardware features. */
data class RomInfo(
    val title: String,
    val gameCode: String,
    val makerCode: String,
    val version: Int,
    val saveType: SaveType,
    val hasRtc: Boolean,
)
