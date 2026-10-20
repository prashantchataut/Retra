package app.retra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context

/** Every Retra Original must actually ship in the APK, and its catalog entry must agree. */
@org.junit.runner.RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class OriginalsTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun everyOriginalShipsWithRomCoverAndCatalogEntry() {
        val assets = context.assets.list("games")!!.toSet()
        val manifest = context.assets.open("catalog/manifest.json").bufferedReader().use { it.readText() }
        val catalog = (CatalogParser.parse(manifest) as CatalogParser.Result.Ok).catalog
        for (o in Originals.all) {
            assertTrue("${o.romAsset} missing from the APK", assets.contains(o.romAsset.substringAfterLast('/')))
            assertTrue("${o.coverAsset} missing from the APK", assets.contains(o.coverAsset.substringAfterLast('/')))
            val entry = catalog.byId[o.catalogId]
            assertNotNull("no catalog entry for ${o.catalogId}", entry)
            assertEquals("catalog entry for ${o.catalogId} should be playable offline", true, entry!!.bundled)
            assertEquals("catalog entry title", o.title, entry.title)
        }
    }

    @Test fun bundledRomSizesMatchTheCatalog() {
        val manifest = context.assets.open("catalog/manifest.json").bufferedReader().use { it.readText() }
        val catalog = (CatalogParser.parse(manifest) as CatalogParser.Result.Ok).catalog
        for (o in Originals.all) {
            val entry = catalog.byId[o.catalogId] ?: continue
            val d = entry.download ?: continue
            val size = context.assets.open(o.romAsset).use { it.available().toLong() }
            assertEquals("size of ${o.catalogId} in the catalog", size, d.size)
        }
    }
}
