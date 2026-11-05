package app.retra.core

/**
 * Decides what a Google Drive sync does, without touching the network or disk.
 *
 * Saves live in the app's hidden Drive folder as `<gameId>.sav`. Content equality is decided by MD5 (Drive reports it
 * for every blob), and when both sides changed the newest copy wins; the losing local copy is always backed up by
 * the save repository before it's replaced, so nothing is ever destroyed silently.
 */
object DriveSyncPlanner {
    data class Local(val md5: String, val modifiedMs: Long)
    data class Remote(val fileId: String, val md5: String, val modifiedMs: Long)

    sealed interface Action {
        val gameId: String
        data class Upload(override val gameId: String, val updateFileId: String?) : Action
        data class Download(override val gameId: String, val fileId: String, val replacesLocal: Boolean) : Action
    }

    /**
     * @param local battery saves on this device, by game id
     * @param remote saves in Drive, by game id
     * @param known game ids that exist in this library (a save for a game you don't have is left alone)
     */
    fun plan(local: Map<String, Local>, remote: Map<String, Remote>, known: Set<String>): List<Action> {
        val actions = ArrayList<Action>()
        for ((id, l) in local) {
            val r = remote[id]
            when {
                r == null -> actions += Action.Upload(id, null)
                r.md5.equals(l.md5, ignoreCase = true) -> Unit
                l.modifiedMs >= r.modifiedMs -> actions += Action.Upload(id, r.fileId)
                else -> actions += Action.Download(id, r.fileId, replacesLocal = true)
            }
        }
        for ((id, r) in remote) {
            if (id !in local && id in known) actions += Action.Download(id, r.fileId, replacesLocal = false)
        }
        return actions
    }

    fun fileName(gameId: String) = "$gameId.sav"

    fun gameId(fileName: String): String? =
        fileName.takeIf { it.endsWith(".sav") }?.removeSuffix(".sav")?.takeIf { it.matches(Regex("[0-9a-f]{16}")) }

    data class Report(val uploaded: Int = 0, val downloaded: Int = 0, val failed: Int = 0, val upToDate: Int = 0) {
        fun summary(): String = when {
            uploaded == 0 && downloaded == 0 && failed == 0 -> "Everything's already in sync."
            else -> listOfNotNull(
                uploaded.takeIf { it > 0 }?.let { "$it uploaded" },
                downloaded.takeIf { it > 0 }?.let { "$it restored" },
                failed.takeIf { it > 0 }?.let { "$it failed" },
            ).joinToString(", ")
        }
    }
}
