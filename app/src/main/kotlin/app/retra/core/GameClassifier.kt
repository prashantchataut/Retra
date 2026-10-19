package app.retra.core

import app.retra.data.GameCategory
import app.retra.data.GameSource

object GameClassifier {
    /** Main-series GBA Pokémon cartridge code prefixes (all regions). */
    private val POKEMON_PREFIXES = listOf("AXV", "AXP", "BPE", "BPR", "BPG")

    fun isPokemon(gameCode: String, romTitle: String): Boolean =
        POKEMON_PREFIXES.any { gameCode.uppercase().startsWith(it) } || romTitle.uppercase().startsWith("POKEMON")

    fun classify(gameCode: String, romTitle: String, source: GameSource, homebrew: Boolean): GameCategory = when {
        source == GameSource.PATCHED -> GameCategory.FAN_GAME
        homebrew || source == GameSource.BUNDLED -> GameCategory.HOMEBREW
        isPokemon(gameCode, romTitle) -> GameCategory.POKEMON
        else -> GameCategory.OTHER
    }

    /** "POKEMON EMER" -> "Pokemon Emer"; used when the file name is not helpful. */
    fun prettyTitle(romTitle: String, fileName: String?): String {
        val fromFile = fileName?.substringBeforeLast('.')
            ?.replace('_', ' ')
            ?.replace(Regex("\\s*[\\[(][^\\])]*[\\])]"), "")
            ?.trim()
        if (!fromFile.isNullOrBlank() && fromFile.length >= 3) return fromFile
        if (romTitle.isBlank()) return "Untitled game"
        return romTitle.lowercase().split(' ').filter { it.isNotEmpty() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
    }
}
