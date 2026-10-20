package app.retra.data

/** Retra's own games: shipped inside the APK (assets/games) and added to My Games on first launch. */
object Originals {
    data class Original(val catalogId: String, val title: String, val romAsset: String, val coverAsset: String)

    val all = listOf(
        Original("mochi-hop", "Mochi Hop", "games/mochi_hop.gba", "games/mochi_hop_cover.png"),
        Original("mochi-monsters", "Mochi Monsters", "games/mochi_monsters.gba", "games/mochi_monsters_cover.png"),
        Original("turbo-drift", "Turbo Drift", "games/turbo_drift.gba", "games/turbo_drift_cover.png"),
        Original("mochi-pop", "Mochi Pop", "games/mochi_pop.gba", "games/mochi_pop_cover.png"),
        Original("sky-sprout", "Sky Sprout", "games/sky_sprout.gba", "games/sky_sprout_cover.png"),
    )

    fun byCatalogId(id: String): Original? = all.firstOrNull { it.catalogId == id }
}
