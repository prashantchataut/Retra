package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.model.AccentPalette
import app.retra.core.model.AppSettings
import app.retra.core.model.ContentDensity
import app.retra.core.model.GameRecord
import app.retra.core.model.LibraryLayout
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.StartupDestination
import app.retra.core.model.ThemeMode
import app.retra.emulator.auth.AuthOperation
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryAqua
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.SaveMint
import java.text.DateFormat
import java.util.Date

private enum class FinalLibraryFilter(val label: String) {
    ALL("All"),
    CONTINUE("Continue"),
    FAVORITES("Favorites"),
    PATCHED("Patched"),
    HOMEBREW("Homebrew"),
    UNPLAYED("Unplayed")
}

private enum class FinalSettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    APPEARANCE("Look & feel", "Theme, glass, type, and motion", Icons.Default.Palette),
    LIBRARY("Library", "Layout, startup, and discovery", Icons.Default.LibraryBooks),
    GAMEPLAY("Gameplay", "Performance, display, and audio", Icons.Default.Speed),
    CONTROLS("Controls", "Touch layout and controller behavior", Icons.Default.Gamepad),
    FEEDBACK("Sound & alerts", "Haptics, interface sound, notifications", Icons.Default.VolumeUp),
    CONTENT("Content sources", "Metadata, homebrew, and cheat indexes", Icons.Default.Storage),
    PRIVACY("Privacy", "Local data and network boundaries", Icons.Default.Security),
    ABOUT("About Retra", "Version, credits, and developer", Icons.Default.VerifiedUser)
}

@Composable
internal fun RetraHomeScreenV3(
    games: List<GameRecord>,
    settings: AppSettings,
    coreAvailable: Boolean,
    coreStatus: String,
    onImportFile: () -> Unit,
    onGameSelected: (GameRecord) -> Unit
) {
    val continueGame = remember(games) {
        games.maxByOrNull { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis }
    }
    val recentGames = remember(games, continueGame) {
        games.sortedByDescending { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis }
            .filterNot { it.id == continueGame?.id }
            .take(10)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("YOUR ARCHIVE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Pick up where you left off.", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "A private home for the games and saves that still matter.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FinalContinueCard(
                game = continueGame,
                coreAvailable = coreAvailable,
                coreStatus = coreStatus,
                onImportFile = onImportFile,
                onOpen = onGameSelected
            )
        }

        if (recentGames.isNotEmpty()) {
            item { FinalSectionHeader("Back on the shelf", "Recent and favorite worlds") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(recentGames, key = { it.id }) { game ->
                        FinalShelfCard(game = game, onOpen = onGameSelected)
                    }
                }
            }
        }

        if (games.isNotEmpty() && settings.showStatistics) {
            item {
                GlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FinalInlineStat(games.size.toString(), "games")
                        FinalInlineStat(games.count { it.lastPlayedAtEpochMillis != null }.toString(), "played")
                        FinalInlineStat(games.count(GameRecord::favorite).toString(), "favorites")
                    }
                }
            }
        }

        item {
            GlassPanel(cornerRadius = 24.dp, contentPadding = PaddingValues(18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MemoryAqua.copy(alpha = 0.14f)) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MemoryAqua,
                            modifier = Modifier.padding(12.dp).size(22.dp)
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Local by design", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Retra does not upload ROMs or saves. Online discovery is limited to creator pages and checksum-verified releases.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalContinueCard(
    game: GameRecord?,
    coreAvailable: Boolean,
    coreStatus: String,
    onImportFile: () -> Unit,
    onOpen: (GameRecord) -> Unit
) {
    val feedback = LocalRetraFeedback.current
    val shape = RoundedCornerShape(30.dp)
    GlassPanel(cornerRadius = 30.dp) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 310.dp)
                .clip(shape)
        ) {
            if (game != null) {
                GameArtwork(game, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f)))
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    RetraLogoTile(size = 122.dp)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = if (game == null) 0.18f else 0.52f),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Row(
                        Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (game == null) Icons.Default.Storage else Icons.Default.History, null, Modifier.size(15.dp))
                        Text(if (game == null) "Private library" else "Continue", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text(
                    game?.title ?: "Bring your first game home",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        game == null -> "Import a game backup you own. Retra organizes it locally and keeps the original untouched."
                        coreAvailable -> "Your latest save and per-game settings are ready."
                        else -> coreStatus
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.84f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Button(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        if (game == null) onImportFile() else onOpen(game)
                    },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Icon(if (game == null) Icons.Default.Add else Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (game == null) "Import a game" else "Resume")
                }
            }
        }
    }
}

@Composable
private fun FinalShelfCard(game: GameRecord, onOpen: (GameRecord) -> Unit) {
    val feedback = LocalRetraFeedback.current
    Column(
        modifier = Modifier
            .width(150.dp)
            .clickable {
                feedback(FeedbackCue.TAP)
                onOpen(game)
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlassPanel(cornerRadius = 22.dp) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.74f)) {
                GameArtwork(game, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (game.favorite) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.58f)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = AdventureGold,
                            modifier = Modifier.padding(7.dp).size(16.dp)
                        )
                    }
                }
            }
        }
        Text(game.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            if (game.lastPlayedAtEpochMillis == null) "Ready to start" else "Save ready",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FinalInlineStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun RetraLibraryScreenV3(
    games: List<GameRecord>,
    layout: LibraryLayout,
    density: ContentDensity,
    onImportFile: () -> Unit,
    onImportFolder: () -> Unit,
    onGameSelected: (GameRecord) -> Unit
) {
    val pagePadding = when (density) {
        ContentDensity.COMFORTABLE -> 20.dp
        ContentDensity.BALANCED -> 16.dp
        ContentDensity.COMPACT -> 12.dp
    }
    val itemSpacing = when (density) {
        ContentDensity.COMFORTABLE -> 16.dp
        ContentDensity.BALANCED -> 12.dp
        ContentDensity.COMPACT -> 9.dp
    }
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(FinalLibraryFilter.ALL.name) }
    val filter = FinalLibraryFilter.valueOf(filterName)
    val visibleGames = remember(games, query, filter) {
        val normalized = query.trim().lowercase()
        games.asSequence()
            .filter { game ->
                when (filter) {
                    FinalLibraryFilter.ALL -> true
                    FinalLibraryFilter.CONTINUE -> game.lastPlayedAtEpochMillis != null
                    FinalLibraryFilter.FAVORITES -> game.favorite
                    FinalLibraryFilter.PATCHED -> game.origin == "LOCAL_PATCH" || !game.patchSha256.isNullOrBlank()
                    FinalLibraryFilter.HOMEBREW -> game.origin == "LEGAL_CATALOG" || game.tags.any { it.equals("homebrew", true) }
                    FinalLibraryFilter.UNPLAYED -> game.lastPlayedAtEpochMillis == null
                }
            }
            .filter { game ->
                normalized.isBlank() || listOf(
                    game.title,
                    game.displayName,
                    game.gameCode,
                    game.creator.orEmpty()
                ).plus(game.tags).plus(game.collections).any { it.lowercase().contains(normalized) }
            }
            .sortedByDescending { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis }
            .toList()
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.padding(horizontal = pagePadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Library", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        if (games.isEmpty()) "Your archive starts here." else "${games.size} ${if (games.size == 1) "game" else "games"}, stored locally",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onImportFolder, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Scan a folder")
                }
                FilledIconButton(onClick = onImportFile, modifier = Modifier.size(50.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Import a game or patch")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search games, tags, or collections") },
                shape = RoundedCornerShape(20.dp)
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FinalLibraryFilter.entries, key = { it.name }) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filterName = item.name },
                        label = { Text(item.label) }
                    )
                }
            }
        }

        when {
            games.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.Center) {
                    FinalEmptyLibrary(onImportFile, onImportFolder)
                }
            }
            visibleGames.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(pagePadding), contentAlignment = Alignment.Center) {
                    GlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(24.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(36.dp))
                            Text("Nothing matches", style = MaterialTheme.typography.titleLarge)
                            Text("Try another title or filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            layout == LibraryLayout.DETAILED_LIST -> {
                LazyColumn(
                    contentPadding = PaddingValues(start = pagePadding, end = pagePadding, top = 6.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    items(visibleGames, key = { it.id }) { game ->
                        FinalGameListRow(game, onGameSelected)
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (layout == LibraryLayout.LARGE_GRID) 172.dp else 136.dp),
                    contentPadding = PaddingValues(start = pagePadding, end = pagePadding, top = 6.dp, bottom = 112.dp),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                    verticalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    items(visibleGames, key = { it.id }) { game ->
                        FinalGameGridCard(game, onGameSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalGameGridCard(game: GameRecord, onOpen: (GameRecord) -> Unit) {
    val feedback = LocalRetraFeedback.current
    Column(
        Modifier
            .fillMaxWidth()
            .clickable {
                feedback(FeedbackCue.TAP)
                onOpen(game)
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlassPanel(cornerRadius = 22.dp) {
            Box(Modifier.fillMaxWidth().aspectRatio(0.74f)) {
                GameArtwork(game, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                if (game.favorite) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.56f)
                    ) {
                        Icon(Icons.Default.Favorite, "Favorite", tint = MemoryCoral, modifier = Modifier.padding(7.dp).size(16.dp))
                    }
                }
            }
        }
        Text(game.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            when {
                !game.patchDisplayName.isNullOrBlank() -> "Patched"
                game.origin == "LEGAL_CATALOG" -> "Homebrew"
                game.lastPlayedAtEpochMillis != null -> "Continue"
                else -> "Unplayed"
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FinalGameListRow(game: GameRecord, onOpen: (GameRecord) -> Unit) {
    val feedback = LocalRetraFeedback.current
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                feedback(FeedbackCue.TAP)
                onOpen(game)
            },
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GameArtwork(game, Modifier.size(width = 70.dp, height = 92.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(game.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(
                    if (game.lastPlayedAtEpochMillis == null) "Ready to start" else "Last played ${formatShortDate(game.lastPlayedAtEpochMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.PlayArrow, contentDescription = "Open ${game.title}", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun FinalEmptyLibrary(onImportFile: () -> Unit, onImportFolder: () -> Unit) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RetraLogoTile(size = 82.dp)
            Text("Build your private archive", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Import .gba backups you own, scan a folder, or apply IPS, UPS, and BPS patches to a compatible base game.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onImportFile, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Import a file")
            }
            OutlinedButton(onClick = onImportFolder, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan a folder")
            }
        }
    }
}

@Composable
internal fun RetraProfileScreenV3(
    viewModel: RetraViewModel,
    games: List<GameRecord>,
    onOpenSettings: () -> Unit
) {
    val account by viewModel.account.collectAsStateWithLifecycle()
    val authOperation by viewModel.authOperation.collectAsStateWithLifecycle()
    val profile by viewModel.socialProfile.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val vaultRecords by viewModel.vaultRecords.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val feedback = LocalRetraFeedback.current
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }
    val totalPoints = achievements.filter { it.progress.unlockedAtEpochMillis != null }.sumOf { it.definition.points }
    val identifiedGames = games.count { !it.canonicalTitle.isNullOrBlank() }
    val favorites = games.count(GameRecord::favorite)
    val homebrewGames = games.count { it.origin == "HOMEBREW_HUB" }
    val patchedGames = games.count { it.origin == "LOCAL_PATCH" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            GlassPanel(cornerRadius = 30.dp, contentPadding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                            Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    account?.initials ?: profile.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(account?.displayName ?: profile.displayName, style = MaterialTheme.typography.titleLarge)
                            Text(account?.email ?: "Offline player", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(profile.friendCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium)
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Open settings")
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FinalProfileStat(games.count { it.lastPlayedAtEpochMillis != null }.toString(), "played", Modifier.weight(1f))
                        FinalProfileStat("$unlocked/${achievements.size}", "unlocked", Modifier.weight(1f))
                        FinalProfileStat(totalPoints.toString(), "points", Modifier.weight(1f))
                    }

                    if (account == null) {
                        FilledTonalButton(
                            onClick = {
                                feedback(FeedbackCue.CONFIRM)
                                viewModel.signInWithGoogle(context)
                            },
                            enabled = viewModel.googleAuthConfigured && authOperation == AuthOperation.IDLE,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                        ) {
                            Icon(Icons.Default.Login, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (authOperation == AuthOperation.SIGNING_IN) "Connecting…" else "Connect optional identity")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                feedback(FeedbackCue.TAP)
                                viewModel.signOutGoogle(context)
                            },
                            enabled = authOperation == AuthOperation.IDLE,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                        ) {
                            Icon(Icons.Default.Logout, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (authOperation == AuthOperation.SIGNING_OUT) "Disconnecting…" else "Disconnect Google")
                        }
                    }
                    Text(
                        "Identity is optional. Games, saves, patches, cheats, and settings remain usable offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { FinalSectionHeader("Library health", "Useful signals, not vanity metrics") }
        item {
            GlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FinalProfileStat(identifiedGames.toString(), "identified", Modifier.weight(1f))
                        FinalProfileStat(favorites.toString(), "favorites", Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FinalProfileStat(homebrewGames.toString(), "homebrew", Modifier.weight(1f))
                        FinalProfileStat(patchedGames.toString(), "patched", Modifier.weight(1f))
                    }
                    Text(
                        "Identification uses exact checksums. A title guess never changes the underlying ROM hash.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { FinalSectionHeader("Achievement shelf", "Progress that stays with this device") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(achievements, key = { it.definition.id }) { status ->
                    val isUnlocked = status.progress.unlockedAtEpochMillis != null
                    GlassPanel(
                        modifier = Modifier.width(220.dp),
                        cornerRadius = 24.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = if (isUnlocked) AdventureGold.copy(alpha = 0.17f) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    if (isUnlocked) Icons.Default.AutoAwesome else Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) AdventureGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
                            }
                            Text(status.definition.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                status.definition.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(
                                progress = { if (isUnlocked) 1f else status.completionRatio },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                if (isUnlocked) "Unlocked · ${status.definition.points} points" else "${(status.completionRatio * 100).toInt()}% complete",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUnlocked) AdventureGold else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { FinalSectionHeader("Save vault", "Protected snapshots and suspend states") }
        item {
            GlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = SaveMint.copy(alpha = 0.14f)) {
                        Icon(Icons.Default.Save, null, tint = SaveMint, modifier = Modifier.padding(12.dp).size(24.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Checksummed and local", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (vaultRecords.isEmpty()) {
                                "Quick saves and automatic suspend snapshots will appear here."
                            } else {
                                "${vaultRecords.size} snapshots · latest ${formatShortDate(vaultRecords.maxOf { it.createdAtEpochMillis })}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(vaultRecords.size.toString(), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item {
            GlassPanel(cornerRadius = 24.dp, contentPadding = PaddingValues(18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.VerifiedUser, null, tint = MaterialTheme.colorScheme.primary)
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Share identity, not your files", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Only your chosen display name, friend code, and public achievements are designed for sharing. ROMs and saves are excluded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.64f)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun RetraSettingsScreenV3(
    settings: AppSettings,
    games: List<GameRecord>,
    onThemeChanged: (ThemeMode) -> Unit,
    onLayoutChanged: (LibraryLayout) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onReduceTransparencyChanged: (Boolean) -> Unit,
    onFastForwardChanged: (Float) -> Unit,
    onPerformanceChanged: (PerformanceProfile) -> Unit,
    viewModel: RetraViewModel
) {
    var selectedName by rememberSaveable { mutableStateOf(FinalSettingsCategory.APPEARANCE.name) }
    val selected = FinalSettingsCategory.valueOf(selectedName)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 760.dp
        if (twoPane) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                GlassPanel(
                    modifier = Modifier.width(290.dp).fillMaxHeight(),
                    cornerRadius = 28.dp,
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(10.dp))
                        FinalSettingsCategory.entries.forEach { category ->
                            FinalSettingsNavRow(
                                category = category,
                                selected = selected == category,
                                onClick = { selectedName = category.name }
                            )
                        }
                    }
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FinalSettingsHeading(selected)
                    FinalSettingsDetail(
                        selected, settings, games, onThemeChanged, onLayoutChanged,
                        onDynamicColorChanged, onReduceMotionChanged, onReduceTransparencyChanged,
                        onFastForwardChanged, onPerformanceChanged, viewModel
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Settings", style = MaterialTheme.typography.headlineLarge)
                    Text("Tune the experience without turning it into a control panel.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(FinalSettingsCategory.entries, key = { it.name }) { category ->
                            FilterChip(
                                selected = selected == category,
                                onClick = { selectedName = category.name },
                                leadingIcon = { Icon(category.icon, null, Modifier.size(18.dp)) },
                                label = { Text(category.title) }
                            )
                        }
                    }
                }
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FinalSettingsHeading(selected)
                    FinalSettingsDetail(
                        selected, settings, games, onThemeChanged, onLayoutChanged,
                        onDynamicColorChanged, onReduceMotionChanged, onReduceTransparencyChanged,
                        onFastForwardChanged, onPerformanceChanged, viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun FinalSettingsHeading(category: FinalSettingsCategory) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(category.title, style = MaterialTheme.typography.headlineMedium)
        Text(category.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FinalSettingsNavRow(category: FinalSettingsCategory, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(category.icon, contentDescription = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(category.title, style = MaterialTheme.typography.titleSmall)
                Text(category.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun FinalSettingsDetail(
    selected: FinalSettingsCategory,
    settings: AppSettings,
    games: List<GameRecord>,
    onThemeChanged: (ThemeMode) -> Unit,
    onLayoutChanged: (LibraryLayout) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onReduceTransparencyChanged: (Boolean) -> Unit,
    onFastForwardChanged: (Float) -> Unit,
    onPerformanceChanged: (PerformanceProfile) -> Unit,
    viewModel: RetraViewModel
) {
    val metadataState by viewModel.metadataSync.collectAsStateWithLifecycle()
    when (selected) {
        FinalSettingsCategory.APPEARANCE -> {
            FinalSettingsGroup("Theme", Icons.Default.Palette) {
                FinalSettingLabel("Color mode")
                FinalChoiceRows(ThemeMode.entries, settings.themeMode, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, onThemeChanged)
                FinalSettingLabel("Accent")
                FinalChoiceRows(AccentPalette.entries, settings.accentPalette, ::accentLabel, viewModel::setAccentPalette)
                FinalToggle("Use Android dynamic color", "Let the system replace Retra's accent palette.", settings.dynamicColor, onDynamicColorChanged)
                FinalToggle("High contrast", "Strengthen outlines and secondary text.", settings.highContrast, viewModel::setHighContrast)
            }
            FinalSettingsGroup("Liquid glass", Icons.Default.AutoAwesome) {
                FinalToggle("Reduce transparency", "Use fully opaque surfaces.", settings.reduceTransparency, onReduceTransparencyChanged)
                FinalSlider("Glass intensity", settings.glassIntensity, viewModel::setGlassIntensity, 0f..1f, enabled = !settings.reduceTransparency)
                FinalToggle("Reduce motion", "Replace animated transitions with immediate state changes.", settings.reduceMotion, onReduceMotionChanged)
                FinalSlider("Corner scale", settings.cornerScale, viewModel::setCornerScale, 0.75f..1.35f)
                FinalSlider("Text scale", settings.fontScale, viewModel::setFontScale, 0.85f..1.3f)
            }
        }

        FinalSettingsCategory.LIBRARY -> {
            FinalSettingsGroup("Library behavior", Icons.Default.LibraryBooks) {
                FinalSettingLabel("Default layout")
                FinalChoiceRows(LibraryLayout.entries, settings.libraryLayout, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, onLayoutChanged)
                FinalSettingLabel("Content density")
                FinalChoiceRows(ContentDensity.entries, settings.contentDensity, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, viewModel::setContentDensity)
                FinalSettingLabel("Open Retra to")
                FinalChoiceRows(StartupDestination.entries, settings.startupDestination, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, viewModel::setStartupDestination)
                FinalToggle("Show archive statistics", "Display the compact library summary on Home.", settings.showStatistics, viewModel::setShowStatistics)
                FinalToggle("Show legal online discovery", "Show creator pages and verified public releases.", settings.showOnlineRecommendations, viewModel::setShowOnlineRecommendations)
                FinalInfoRow("Managed games", games.size.toString())
            }
        }

        FinalSettingsCategory.GAMEPLAY -> {
            FinalSettingsGroup("Performance", Icons.Default.Speed) {
                FinalSettingLabel("Profile")
                FinalChoiceRows(PerformanceProfile.entries, settings.performanceProfile, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, onPerformanceChanged)
                FinalSlider("Fast-forward speed", settings.fastForwardSpeed, onFastForwardChanged, 1f..16f, steps = 14, suffix = "×")
            }
            FinalSettingsGroup("Picture & audio", Icons.Default.Tune) {
                FinalToggle("Integer pixel scaling", "Keep pixel edges aligned when the screen size allows.", settings.integerScaling, viewModel::setIntegerScaling)
                FinalToggle("Smooth display filtering", "Soften scaling between source pixels.", settings.displaySmoothing, viewModel::setDisplaySmoothing)
                FinalToggle("Performance overlay", "Show timing and frame diagnostics while playing.", settings.showPerformanceOverlay, viewModel::setShowPerformanceOverlay)
                FinalToggle("Game audio", "Enable sound from the emulator core.", settings.audioEnabled, viewModel::setAudioEnabled)
                FinalSlider("Master volume", settings.masterVolume, viewModel::setMasterVolume, 0f..1f, enabled = settings.audioEnabled, percent = true)
            }
        }

        FinalSettingsCategory.CONTROLS -> {
            FinalSettingsGroup("Touch controls", Icons.Default.Gamepad) {
                FinalToggle("Show touch controls", "Display the on-screen D-pad and buttons.", settings.showTouchControls, viewModel::setShowTouchControls)
                FinalSlider("Control opacity", settings.touchControlOpacity, viewModel::setTouchControlOpacity, 0.25f..1f, enabled = settings.showTouchControls, percent = true)
                FinalToggle("Suspend in background", "Create a protected suspend state when Retra leaves the foreground.", settings.autoSuspendOnBackground, viewModel::setAutoSuspendOnBackground)
                FinalToggle("Pause on headphone disconnect", "Prevent game audio from suddenly playing through speakers.", settings.pauseOnHeadphoneDisconnect, viewModel::setPauseOnHeadphoneDisconnect)
            }
            FinalSettingsGroup("Controller check", Icons.Default.Tune) {
                ControllerInputTester(viewModel)
            }
        }

        FinalSettingsCategory.FEEDBACK -> {
            FinalSettingsGroup("Feel & sound", Icons.Default.VolumeUp) {
                FinalToggle("Haptic feedback", "Use restrained taps for important actions.", settings.hapticsEnabled, viewModel::setHapticsEnabled)
                FinalToggle("Interface sounds", "Play Retra's short original sound cues.", settings.soundEffectsEnabled, viewModel::setSoundEffectsEnabled)
                FinalSlider("Interface volume", settings.soundEffectsVolume, viewModel::setSoundEffectsVolume, 0f..1f, enabled = settings.soundEffectsEnabled, percent = true)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { viewModel.emitFeedback(FeedbackCue.CONFIRM) }) { Text("Test tap") }
                    OutlinedButton(onClick = { viewModel.emitFeedback(FeedbackCue.ACHIEVEMENT) }) { Text("Test celebration") }
                }
            }
            FinalSettingsGroup("Notifications", Icons.Default.Notifications) {
                FinalToggle("Allow Retra notifications", "Master switch for optional alerts.", settings.notificationsEnabled, viewModel::setNotificationsEnabled)
                FinalToggle("Achievements", "Celebrate unlocks outside the app.", settings.notifyAchievements, viewModel::setNotifyAchievements, enabled = settings.notificationsEnabled)
                FinalToggle("Verified downloads", "Report download, checksum, and import results.", settings.notifyDownloads, viewModel::setNotifyDownloads, enabled = settings.notificationsEnabled)
                FinalToggle("Multiplayer", "Show invitations and connection alerts.", settings.notifyMultiplayer, viewModel::setNotifyMultiplayer, enabled = settings.notificationsEnabled)
            }
        }

        FinalSettingsCategory.CONTENT -> {
            FinalSettingsGroup("Exact game identification", Icons.Default.Storage) {
                FinalInfoRow("Identified games", games.count { !it.canonicalTitle.isNullOrBlank() }.toString())
                FinalInfoRow("Indexed records", metadataState.indexedRecords.toString())
                FinalInfoRow("Last sync matches", metadataState.matchedGames.toString())
                Button(
                    onClick = viewModel::syncGameMetadata,
                    enabled = !metadataState.syncing,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Default.VerifiedUser, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (metadataState.syncing) "Syncing metadata…" else "Sync official checksum metadata")
                }
                metadataState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "Retra matches SHA-1 or CRC-32 plus file size against the Libretro No-Intro database. It does not identify games by filename alone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FinalSettingsGroup("Playable sources", Icons.Default.LibraryBooks) {
                FinalInfoRow("Legal homebrew", "Homebrew Hub")
                FinalInfoRow("Community cheats", "Libretro .cht")
                FinalInfoRow("Commercial ROM catalog", "Disabled")
                Text(
                    "Discover may install creator-published GBA homebrew. Commercial games such as Pokémon FireRed must be imported from a backup you are legally entitled to use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        FinalSettingsCategory.PRIVACY -> {
            FinalSettingsGroup("Local-first boundary", Icons.Default.Security) {
                FinalInfoRow("ROM uploads", "Disabled")
                FinalInfoRow("Anonymous analytics", "Off")
                FinalInfoRow("Crash reporting", "Not configured")
                FinalInfoRow("Account required for play", "No")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
                Text(
                    "Retra connects only when you explicitly open a creator page, browse Homebrew Hub, sync Libretro metadata, install a community cheat file, download a checksum-pinned public release, or use optional Google identity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Local gameplay remains fully available offline.", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        FinalSettingsCategory.ABOUT -> {
            FinalSettingsGroup("Retra", Icons.Default.VerifiedUser) {
                FinalInfoRow("Version", "2.0.0")
                FinalInfoRow("Developer", "Prashant Chataut")
                FinalInfoRow("Product direction", "Local-first handheld archive")
                FinalInfoRow("Primary core", "mGBA / libretro bridge")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f))
                Text(
                    "Retra is built to preserve lawful personal backups, saves, patches, homebrew, and player-created metadata without turning the library into a storefront for copyrighted ROMs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FinalSettingsGroup("Open-source data credits", Icons.Default.LibraryBooks) {
                FinalInfoRow("Game metadata", "Libretro Database")
                FinalInfoRow("Cheat definitions", "Libretro Database")
                FinalInfoRow("Homebrew discovery", "Homebrew Hub")
                Text(
                    "Each source remains optional and network access happens only after an explicit action in Retra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FinalSettingsGroup(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(cornerRadius = 26.dp, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(19.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f))
            content()
        }
    }
}

@Composable
private fun FinalToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun FinalSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true,
    steps: Int = 0,
    suffix: String = "",
    percent: Boolean = false
) {
    val display = if (percent) "${(value * 100).toInt()}%" else "${"%.1f".format(value)}$suffix"
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(display, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = label }
        )
    }
}

@Composable
private fun <T> FinalChoiceRows(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) })
                }
            }
        }
    }
}

@Composable
private fun FinalSettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun FinalInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FinalSectionHeader(title: String, supporting: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun accentLabel(value: AccentPalette): String = when (value) {
    AccentPalette.RETRA_INDIGO -> "Ice blue"
    AccentPalette.GRAPHITE -> "Graphite"
    AccentPalette.SOFT_VIOLET -> "Frost"
    AccentPalette.CLASSIC_GRAY -> "Classic"
}

private fun formatShortDate(epochMillis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(epochMillis))
