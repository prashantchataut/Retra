package app.retra.emulator

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.model.CatalogContentKind
import app.retra.core.model.CatalogEntry
import app.retra.core.rom.CatalogValidationResult
import app.retra.emulator.data.CatalogDownloadPhase
import app.retra.emulator.data.HomebrewHubEntry
import app.retra.emulator.data.CatalogDownloadProgress
import app.retra.emulator.data.StoredCatalog
import app.retra.emulator.ui.theme.SaveMint

@Composable
internal fun RetraDiscoverScreenV3(
    catalogs: List<StoredCatalog>,
    validation: CatalogValidationResult,
    downloads: Map<String, CatalogDownloadProgress>,
    downloadableHashes: Set<String>,
    onImportCatalog: () -> Unit,
    onDownload: (CatalogEntry) -> Unit,
    onDeleteCatalog: (StoredCatalog) -> Unit,
    showOnlineRecommendations: Boolean,
    viewModel: RetraViewModel
) {
    val feedback = LocalRetraFeedback.current
    val context = LocalContext.current
    val curated by viewModel.curatedReleases.collectAsStateWithLifecycle()
    val hub by viewModel.homebrewHub.collectAsStateWithLifecycle()
    var hubQuery by rememberSaveable { mutableStateOf("") }
    val customCatalogs = catalogs.filterNot(StoredCatalog::builtIn)
    LaunchedEffect(Unit) {
        viewModel.refreshCuratedReleases()
        if (hub.page.entries.isEmpty()) viewModel.refreshHomebrewHub()
    }

    fun openExternal(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 10.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassPanel(cornerRadius = 30.dp) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.94f))
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FinalDiscoverPill("Reviewable sources", Icons.Default.Verified)
                        Text(
                            "Find worlds with provenance",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Browse legal homebrew, public releases, and official patch projects. Commercial ROMs are never bundled or indexed.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            GlassPanel(cornerRadius = 24.dp, contentPadding = PaddingValues(17.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(20.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Your base game stays yours", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Patch projects open their official page or patcher. You provide the compatible base ROM locally; Retra preserves the original.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showOnlineRecommendations) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FinalDiscoverSectionHeader("Playable homebrew", "Real GBA releases from Homebrew Hub")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = hubQuery,
                            onValueChange = { hubQuery = it.take(120) },
                            label = { Text("Search title") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.refreshHomebrewHub(hubQuery, 1) },
                            enabled = !hub.loading,
                            modifier = Modifier.heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Search")
                        }
                    }
                    Text(
                        "Only entries with a creator-published playable .gba file and usable license metadata can be installed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (hub.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            hub.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            items(hub.page.entries, key = { "homebrew:${it.slug}" }) { entry ->
                FinalHomebrewHubCard(
                    entry = entry,
                    installing = hub.installingSlug == entry.slug,
                    onInstall = { viewModel.installHomebrew(entry) },
                    onOpenSource = { openExternal(entry.sourcePageUrl()) }
                )
            }
            if (!hub.loading && hub.page.entries.isEmpty()) {
                item {
                    GlassPanel(cornerRadius = 24.dp, contentPadding = PaddingValues(18.dp)) {
                        Text(
                            "No playable GBA homebrew matched this search. Retra does not replace empty results with unverified mirrors.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (hub.page.pageTotal > 1) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { viewModel.refreshHomebrewHub(hub.query, hub.page.pageCurrent - 1) },
                            enabled = !hub.loading && hub.page.pageCurrent > 1,
                            modifier = Modifier.weight(1f)
                        ) { Text("Previous") }
                        Text("${hub.page.pageCurrent} / ${hub.page.pageTotal}", style = MaterialTheme.typography.labelLarge)
                        OutlinedButton(
                            onClick = { viewModel.refreshHomebrewHub(hub.query, hub.page.pageCurrent + 1) },
                            enabled = !hub.loading && hub.page.pageCurrent < hub.page.pageTotal,
                            modifier = Modifier.weight(1f)
                        ) { Text("Next") }
                    }
                }
            }
        }

        if (showOnlineRecommendations && viewModel.catalogRepository.curatedLinks.isNotEmpty()) {
            item { FinalDiscoverSectionHeader("Creator spotlights", "Read the source page before importing") }
            items(viewModel.catalogRepository.curatedLinks, key = { "creator:${it.id}" }) { link ->
                FinalCreatorSpotlightCard(
                    title = link.title,
                    creator = link.creator,
                    description = link.description,
                    onOpen = {
                        feedback(FeedbackCue.CONFIRM)
                        openExternal(link.sourcePageUrl)
                    }
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Verified one-tap releases", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "HTTPS · published SHA-256 · permission · supported type",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.refreshCuratedReleases() }, enabled = !curated.refreshing) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh releases")
                }
            }
        }

        if (curated.refreshing) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        curated.lastError?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
        if (!curated.refreshing && curated.downloadableEntries.isEmpty()) {
            item {
                GlassPanel(cornerRadius = 24.dp, contentPadding = PaddingValues(18.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "No official release currently exposes both a supported asset and a published checksum. Creator pages remain available above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        items(curated.downloadableEntries, key = { "verified:${it.id}" }) { entry ->
            FinalReleaseCard(
                entry = entry,
                progress = downloads[entry.sha256.lowercase()],
                downloadable = true,
                onDownload = onDownload,
                onOpenSource = ::openExternal
            )
        }

        customCatalogs.forEach { source ->
            item(key = "catalog-title:${source.manifest.catalogId}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(source.manifest.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${source.manifest.owner} · ${source.manifest.games.size} entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteCatalog(source) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${source.manifest.name}")
                    }
                }
            }
            items(source.manifest.games, key = { "${source.manifest.catalogId}:${it.id}" }) { entry ->
                FinalReleaseCard(
                    entry = entry,
                    progress = downloads[entry.sha256.lowercase()],
                    downloadable = entry.sha256.lowercase() in downloadableHashes,
                    onDownload = onDownload,
                    onOpenSource = ::openExternal
                )
            }
        }

        item {
            OutlinedButton(onClick = onImportCatalog, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import a trusted catalog")
            }
        }

        item {
            Text(
                finalCatalogValidationLabel(validation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FinalHomebrewHubCard(
    entry: HomebrewHubEntry,
    installing: Boolean,
    onInstall: () -> Unit,
    onOpenSource: () -> Unit
) {
    val playable = entry.defaultPlayableGba
    GlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = SaveMint.copy(alpha = 0.14f)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SaveMint, modifier = Modifier.padding(12.dp).size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleLarge)
                    Text("By ${entry.developer}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FinalDiscoverPill(entry.license.ifBlank { "License unavailable" })
                FinalDiscoverPill(entry.platform)
                playable?.let { FinalDiscoverPill(it.filename.substringAfterLast('.').uppercase()) }
            }
            if (entry.tags.isNotEmpty()) {
                Text(entry.tags.take(5).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onInstall,
                enabled = entry.directInstallEligible && !installing,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        installing -> "Downloading and validating…"
                        entry.directInstallEligible -> "Install and add to library"
                        else -> "Source page only"
                    }
                )
            }
            OutlinedButton(onClick = onOpenSource, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View source entry")
            }
        }
    }
}

@Composable
private fun FinalCreatorSpotlightCard(
    title: String,
    creator: String,
    description: String,
    onOpen: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(17.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(13.dp).size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("By $creator", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    description,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.OpenInNew, contentDescription = "Open $title")
        }
    }
}

@Composable
private fun FinalReleaseCard(
    entry: CatalogEntry,
    progress: CatalogDownloadProgress?,
    downloadable: Boolean,
    onDownload: (CatalogEntry) -> Unit,
    onOpenSource: (String) -> Unit
) {
    val feedback = LocalRetraFeedback.current
    val active = progress?.phase in setOf(
        CatalogDownloadPhase.CONNECTING,
        CatalogDownloadPhase.DOWNLOADING,
        CatalogDownloadPhase.VERIFYING,
        CatalogDownloadPhase.IMPORTING
    )
    val external = entry.contentKind == CatalogContentKind.EXTERNAL || (!downloadable && !entry.sourcePageUrl.isNullOrBlank())

    GlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(18.dp), color = SaveMint.copy(alpha = 0.14f)) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = SaveMint, modifier = Modifier.padding(12.dp).size(24.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.title, style = MaterialTheme.typography.titleLarge)
                    Text("By ${entry.creator}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(entry.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FinalDiscoverPill(entry.license)
                FinalDiscoverPill(entry.contentKind.name.lowercase().replaceFirstChar(Char::titlecase))
                if (entry.fileSize > 0) FinalDiscoverPill(finalFormatBytes(entry.fileSize))
            }
            Text(entry.distributionPermission, style = MaterialTheme.typography.bodySmall, color = SaveMint)

            if (active && progress != null) {
                val fraction = if (progress.totalBytes > 0) {
                    (progress.bytesDownloaded.toFloat() / progress.totalBytes).coerceIn(0f, 1f)
                } else {
                    0f
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(
                    progress.phase.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (external) {
                OutlinedButton(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        entry.sourcePageUrl?.let(onOpenSource)
                    },
                    enabled = !entry.sourcePageUrl.isNullOrBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open creator page")
                }
            } else {
                Button(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        onDownload(entry)
                    },
                    enabled = downloadable && !active,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) {
                    Icon(if (downloadable) Icons.Default.Download else Icons.Default.CloudOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (downloadable) "Download, verify, and add" else "Checksum required")
                }
            }
            progress?.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FinalDiscoverSectionHeader(title: String, supporting: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FinalDiscoverPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
    ) {
        Row(
            Modifier.padding(horizontal = if (text.isBlank()) 8.dp else 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun finalFormatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824f)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576f)
    bytes >= 1_024L -> "%.1f KB".format(bytes / 1_024f)
    else -> "$bytes B"
}

private fun finalCatalogValidationLabel(validation: CatalogValidationResult): String = when (validation) {
    is CatalogValidationResult.Valid -> "Catalog validation: passed"
    is CatalogValidationResult.Invalid -> "Catalog validation: ${validation.reasons.joinToString(limit = 2)}"
}
