package app.retra.emulator

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.core.model.LibraryLayout
import app.retra.emulator.ui.components.RetraEmptyState
import app.retra.emulator.ui.components.RetraPageTitle

internal enum class V3Filter(val label: String) {
    ALL("All"),
    CONTINUE("Continue"),
    FAVORITES("Favorites"),
    PATCHED("Patched"),
    HOMEBREW("Homebrew"),
    UNPLAYED("Unplayed")
}

@Composable
internal fun V3Library(
    games: List<GameRecord>,
    layout: LibraryLayout,
    onLayout: (LibraryLayout) -> Unit,
    onGame: (GameRecord) -> Unit,
    onImport: () -> Unit,
    onFolder: () -> Unit,
    onInstallDemo: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(V3Filter.ALL) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val filtered = remember(games, query, filter) {
        games.filter { game ->
            val matchesText = query.isBlank() || listOf(game.title, game.displayName, game.gameCode, game.creator.orEmpty(), game.tags.joinToString(" "))
                .any { it.contains(query, ignoreCase = true) }
            val matchesFilter = when (filter) {
                V3Filter.ALL -> true
                V3Filter.CONTINUE -> game.lastPlayedAtEpochMillis != null
                V3Filter.FAVORITES -> game.favorite
                V3Filter.PATCHED -> game.patchSha256 != null
                V3Filter.HOMEBREW -> game.origin.contains("HOMEBREW") || game.origin.contains("CATALOG")
                V3Filter.UNPLAYED -> game.lastPlayedAtEpochMillis == null
            }
            matchesText && matchesFilter
        }.sortedWith(compareByDescending<GameRecord> { it.favorite }.thenByDescending { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis })
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RetraPageTitle(
            title = "Library",
            subtitle = "${games.size} game${if (games.size == 1) "" else "s"}",
            actionIcon = Icons.Default.Add,
            actionLabel = "Import",
            onAction = onImport
        )
        if (games.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(120) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    placeholder = { Text("Search") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
                IconButton(onClick = { onLayout(if (layout == LibraryLayout.DETAILED_LIST) LibraryLayout.LARGE_GRID else LibraryLayout.DETAILED_LIST) }) {
                    Icon(if (layout == LibraryLayout.DETAILED_LIST) Icons.Default.GridView else Icons.Default.List, "Change library layout")
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val visible = if (filtersExpanded) V3Filter.entries else listOf(V3Filter.ALL, V3Filter.CONTINUE, V3Filter.FAVORITES)
                visible.forEach { item ->
                    FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item.label) })
                }
                FilterChip(
                    selected = filtersExpanded,
                    onClick = { filtersExpanded = !filtersExpanded },
                    label = { Text(if (filtersExpanded) "Less" else "More") }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onFolder) {
                    Icon(Icons.Default.FolderOpen, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Scan folder")
                }
                Text(
                    "${filtered.size} shown",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (filtered.isEmpty()) {
            RetraEmptyState(
                title = if (games.isNotEmpty()) "Nothing matches" else "No games yet",
                body = if (games.isNotEmpty()) {
                    "Try another filter or clear search."
                } else {
                    "Import a GBA file you are allowed to use, or restore Retra Drift."
                },
                primaryLabel = "Import file",
                onPrimary = onImport,
                secondaryLabel = if (games.isEmpty()) "Restore Retra Drift" else null,
                onSecondary = if (games.isEmpty()) onInstallDemo else null,
                modifier = Modifier.weight(1f)
            )
        } else if (layout != LibraryLayout.DETAILED_LIST) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(154.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filtered, key = { it.id }) { game -> V3PosterCard(game, Modifier.fillMaxWidth()) { onGame(game) } }
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(filtered, key = { it.id }) { game -> V3LibraryRow(game) { onGame(game) } }
            }
        }
    }
}
