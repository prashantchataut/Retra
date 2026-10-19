package app.retra.engine

/** Result of [GbaCore.loadRom]. */
sealed interface LoadResult {
    data class Ok(val info: RomInfo) : LoadResult
    data class Error(val message: String) : LoadResult
}
