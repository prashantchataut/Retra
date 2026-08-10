package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.core.model.LibraryLayout
import app.retra.emulator.ui.components.RetraEmptyState
import app.retra.emulator.ui.components.RetraGlassFilterChip
import app.retra.emulator.ui.components.RetraPageTitle

internal enum class LibraryFilter(val label: String) {
    ALL("All"),
    CONTINUE("Played"),
    FAVORITES("Favorites"),
    PATCHED("Patched"),
    HOMEBREW("Homebrew"),
    UNPLAYED("Unplayed")
}

@Composable
internal fun RetraLibrary(
    games: List<GameRecord>,
    layout: LibraryLayout,
    onLayout: (LibraryLayout) -> Unit,
    onGame: (GameRecord) -> Unit,
    onImport: () -> Unit,
    onFolder: () -> Unit,
    onInstallDemo: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(games, query, filter) {
        games.filter { game ->
            val matchesText = query.isBlank() || listOf(
                game.title,
                game.displayName,
                game.gameCode,
                game.creator.orEmpty(),
                game.tags.joinToString(" ")
            ).any { it.contains(query, ignoreCase = true) }

            val matchesFilter = when (filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.CONTINUE -> game.lastPlayedAtEpochMillis != null
                LibraryFilter.FAVORITES -> game.favorite
                LibraryFilter.PATCHED -> game.patchSha256 != null
                LibraryFilter.HOMEBREW -> game.origin.contains("HOMEBREW") || game.origin.contains("CATALOG")
                LibraryFilter.UNPLAYED -> game.lastPlayedAtEpochMillis == null
            }
            matchesText && matchesFilter
        }.sortedWith(
            compareByDescending<GameRecord> { it.favorite }
                .thenByDescending { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Page Title & Import Action
        RetraPageTitle(
            title = "Library",
            subtitle = "${games.size} game${if (games.size == 1) "" else "s"} in your archive",
            actionIcon = Icons.Default.Add,
            actionLabel = "Import",
            onAction = onImport
        )

        if (games.isNotEmpty()) {
            // Floating Search Input & Layout Toggle
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(120) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, "Clear search")
                            }
                        }
                    },
                    placeholder = { Text("Search title, code, tag...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                FilledIconButton(
                    onClick = {
                        onLayout(if (layout == LibraryLayout.DETAILED_LIST) LibraryLayout.LARGE_GRID else LibraryLayout.DETAILED_LIST)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (layout == LibraryLayout.DETAILED_LIST) Icons.Default.GridView else Icons.Default.List,
                        "Change layout"
                    )
                }
            }

            // Filter Chips
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val visible = if (filtersExpanded) LibraryFilter.entries else listOf(
                    LibraryFilter.ALL,
                    LibraryFilter.CONTINUE,
                    LibraryFilter.FAVORITES
                )
                for (item in visible) {
                    RetraGlassFilterChip(
                        label = item.label,
                        selected = filter == item,
                        onClick = { filter = item }
                    )
                }
                RetraGlassFilterChip(
                    label = if (filtersExpanded) "Fewer filters" else "More filters",
                    selected = filtersExpanded,
                    onClick = { filtersExpanded = !filtersExpanded }
                )
            }

            // Quick Folder Scan & Count
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onFolder,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Scan folder")
                }
                Text(
                    "${filtered.size} displayed",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Content Area: Grid / List / Empty State
        if (filtered.isEmpty()) {
            if (query.isNotBlank()) {
                RetraEmptyState(
                    title = "No matches for \"$query\"",
                    body = "Check spelling or reset active filters.",
                    primaryLabel = "Clear search",
                    onPrimary = { query = ""; filter = LibraryFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
            } else if (filter != LibraryFilter.ALL && games.isNotEmpty()) {
                RetraEmptyState(
                    title = "No ${filter.label.lowercase()} games",
                    body = "No items match the ${filter.label} filter in your library.",
                    primaryLabel = "View all games",
                    onPrimary = { filter = LibraryFilter.ALL },
                    modifier = Modifier.weight(1f)
                )
            } else {
                RetraEmptyState(
                    title = "Archive is empty",
                    body = "Import a Game Boy Advance ROM (.gba or .zip), or start playing the built-in Retra Drift demo.",
                    primaryLabel = "Import Game File",
                    onPrimary = onImport,
                    secondaryLabel = "Restore Retra Drift Demo",
                    onSecondary = onInstallDemo,
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (layout != LibraryLayout.DETAILED_LIST) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(156.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filtered, key = { it.id }) { game ->
                    RetraPosterCard(
                        game = game,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        onGame(game)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.id }) { game ->
                    RetraLibraryRow(game) {
                        onGame(game)
                    }
                }
            }
        }
    }
}
