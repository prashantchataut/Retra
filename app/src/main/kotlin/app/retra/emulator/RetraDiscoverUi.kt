package app.retra.emulator

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.emulator.data.HomebrewHubEntry
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryAqua
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RetraBlue
import app.retra.emulator.ui.theme.SaveMint

@Composable
internal fun V3Discover(
    onlineEnabled: Boolean,
    entries: List<HomebrewHubEntry>,
    loading: Boolean,
    installingSlug: String?,
    onRefresh: () -> Unit,
    onInstallDemo: () -> Unit,
    onInstall: (HomebrewHubEntry) -> Unit,
    loadArtwork: suspend (HomebrewHubEntry) -> ByteArray?,
    onPatchStudio: () -> Unit,
    onImport: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            RetraPageTitle(
                title = "Discover",
                subtitle = "Homebrew and local patches.",
                actionIcon = Icons.Default.Refresh,
                actionLabel = "Refresh homebrew",
                onAction = onRefresh.takeIf { onlineEnabled }
            )
        }
        item {
            GlassPanel(shape = MaterialTheme.shapes.extraLarge) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val stacked = maxWidth < 560.dp
                    if (stacked) {
                        Column { V3DemoArt(Modifier.fillMaxWidth().height(190.dp)); V3DemoCopy(onInstallDemo = onInstallDemo) }
                    } else {
                        Row(Modifier.heightIn(min = 240.dp)) { V3DemoArt(Modifier.weight(0.42f).fillMaxHeight()); V3DemoCopy(Modifier.weight(0.58f), onInstallDemo) }
                    }
                }
            }
        }
        item {
            RetraPanel(shape = MaterialTheme.shapes.extraLarge, contentPadding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = MaterialTheme.shapes.medium, color = MemoryCoral.copy(alpha = 0.15f), contentColor = MemoryCoral) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.padding(13.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Patch Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Import a UPS, IPS, or BPS file. Retra matches it to a base ROM already in your library.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onImport) { Text("Import patch") }
                        OutlinedButton(onClick = onPatchStudio) { Text("Prepare known patch") }
                    }
                }
            }
        }
        item { RetraSectionHeader("Homebrew") }
        if (!onlineEnabled) {
            item {
                RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(18.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MemoryAqua)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Online recommendations are off", fontWeight = FontWeight.Bold)
                            Text("Retra Drift and all local import/patch features remain available. Re-enable the gallery in Settings → Appearance.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            if (loading && entries.isEmpty()) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
            if (!loading && entries.isEmpty()) {
                item {
                    RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(18.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("No network gallery yet", fontWeight = FontWeight.Bold)
                            Text("Retra Drift is not bundled in this build. Refresh when you have a connection to browse creator releases.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Refresh") }
                        }
                    }
                }
            }
            items(entries.take(12), key = { it.slug }) { entry ->
                V3HomebrewCard(entry, installingSlug == entry.slug, loadArtwork, { onInstall(entry) }, { onOpenUrl(entry.sourcePageUrl()) })
            }
            item {
                RetraPanel(shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(18.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = SaveMint)
                        Text("Direct install requires a playable GBA file, explicit redistribution permission, a published SHA-256, a bounded size, a named creator, and license metadata. Everything else stays creator-page only.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
internal fun V3DemoArt(modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF07141B)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(13.dp)) {
            RetraLogo(size = 90.dp, markColor = Color.White, cutoutColor = Color(0xFF07141B))
            Text("RETRA DRIFT", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            Text("ORIGINAL GBA HOMEBREW", color = SaveMint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun V3DemoCopy(modifier: Modifier = Modifier, onInstallDemo: () -> Unit) {
    Column(modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RetraBadge("BUILT IN", SaveMint)
        Text("Retra Drift", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Original GBA homebrew intended to verify play without commercial ROMs. This build still requires an imported homebrew file.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RetraBadge("OFFLINE", RetraBlue)
            RetraBadge("OPEN SOURCE", MemoryAqua)
            RetraBadge("64 KiB", AdventureGold)
        }
        Button(onClick = onInstallDemo) {
            Icon(Icons.Default.Download, null)
            Spacer(Modifier.width(6.dp))
            Text("Check offline demo")
        }
    }
}

@Composable
internal fun V3HomebrewCard(
    entry: HomebrewHubEntry,
    installing: Boolean,
    loadArtwork: suspend (HomebrewHubEntry) -> ByteArray?,
    onInstall: () -> Unit,
    onSource: () -> Unit
) {
    val bytes by produceState<ByteArray?>(initialValue = null, key1 = entry.slug) { value = loadArtwork(entry) }
    val bitmap = remember(bytes) { bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }.getOrNull() } }
    GlassPanel(shape = MaterialTheme.shapes.large) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stacked = maxWidth < 560.dp
            val art: @Composable BoxScope.() -> Unit = {
                if (bitmap != null) Image(bitmap!!, entry.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Box(Modifier.fillMaxSize().background(Color(0xFF10232B)), contentAlignment = Alignment.Center) { RetraLogo(size = 62.dp, markColor = Color.White, cutoutColor = Color(0xFF10232B)) }
            }
            if (stacked) {
                Column {
                    Box(Modifier.fillMaxWidth().height(180.dp), content = art)
                    V3HomebrewCopy(entry, installing, onInstall, onSource)
                }
            } else {
                Row(Modifier.heightIn(min = 210.dp)) {
                    Box(Modifier.weight(0.36f).fillMaxHeight(), content = art)
                    V3HomebrewCopy(entry, installing, onInstall, onSource, Modifier.weight(0.64f))
                }
            }
        }
    }
}

@Composable
internal fun V3HomebrewCopy(entry: HomebrewHubEntry, installing: Boolean, onInstall: () -> Unit, onSource: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(entry.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("by ${entry.developer}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            RetraBadge(entry.license.take(18), SaveMint)
            RetraBadge(entry.platform.uppercase(), RetraBlue)
            if (!entry.directInstallEligible) RetraBadge("SOURCE ONLY", AdventureGold)
        }
        if (entry.directInstallEligible) {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Button(onClick = onInstall, enabled = !installing) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (installing) "Installing…" else "Verified install")
                }
                OutlinedButton(onClick = onSource) { Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(5.dp)); Text("Creator page") }
            }
        } else {
            OutlinedButton(onClick = onSource) {
                Icon(Icons.Default.OpenInNew, null)
                Spacer(Modifier.width(5.dp))
                Text("Open creator page")
            }
            Text(
                "Direct install stays off until explicit redistribution permission, a published SHA-256, and a bounded file size are available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
