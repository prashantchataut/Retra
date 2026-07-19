
package app.retra.emulator

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.retra.core.emulation.SaveKind
import app.retra.core.emulation.VaultSaveRecord
import app.retra.core.cheats.CheatFormat
import app.retra.core.model.AccentPalette
import app.retra.core.model.AppSettings
import app.retra.core.model.ContentDensity
import app.retra.core.model.CatalogEntry
import app.retra.core.model.GameRecord
import app.retra.core.model.LibraryLayout
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.StartupDestination
import app.retra.core.model.ThemeMode
import app.retra.core.rom.CatalogValidationResult
import app.retra.emulator.data.CatalogDownloadPhase
import app.retra.emulator.data.CatalogDownloadProgress
import app.retra.emulator.data.StoredCheatPack
import app.retra.emulator.data.StoredCatalog
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryViolet
import app.retra.emulator.ui.theme.PrismCyan
import app.retra.emulator.ui.theme.RetraIndigo
import app.retra.emulator.ui.theme.RetraTheme
import app.retra.emulator.ui.theme.SaveMint
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private enum class Destination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.LibraryBooks),
    DISCOVER("Discover", Icons.Default.Search),
    VAULT("Vault", Icons.Default.Save),
    SETTINGS("You", Icons.Default.AccountCircle)
}

@Composable
fun RetraRoot(viewModel: RetraViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val games by viewModel.games.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()
    val vaultRecords by viewModel.vaultRecords.collectAsStateWithLifecycle()
    val cheatPacksByGame by viewModel.cheatPacks.collectAsStateWithLifecycle()
    val catalogDownloads by viewModel.catalogDownloads.collectAsStateWithLifecycle()
    val catalogSources by viewModel.catalogSources.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val authOperation by viewModel.authOperation.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RetraTheme(settings) {
        val snackbarHostState = remember { SnackbarHostState() }
        LaunchedEffect(viewModel) {
            viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
        }

        if (!settings.onboardingComplete) {
            OnboardingScreen(
                settings = settings,
                account = account,
                authOperation = authOperation,
                googleConfigured = viewModel.googleAuthConfigured,
                onThemeChanged = viewModel::setThemeMode,
                onAccentChanged = viewModel::setAccentPalette,
                onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                onComplete = viewModel::finishOnboarding
            )
        } else if (activeGame != null) {
            PlayerScreen(game = activeGame!!, viewModel = viewModel, onExit = viewModel::closePlayer)
        } else if (selectedGame != null) {
            BackHandler { viewModel.selectGame(null) }
            GameDetailsScreen(
                game = selectedGame!!,
                coreAvailable = viewModel.coreAvailable,
                coreStatus = viewModel.coreStatus,
                gameplayAvailable = viewModel.gameplayAvailable,
                cheatPacks = cheatPacksByGame[selectedGame!!.sha256.lowercase()].orEmpty(),
                onBack = { viewModel.selectGame(null) },
                onPlay = { viewModel.launchGame(selectedGame!!) },
                onApplyPatch = { uri -> viewModel.applyPatch(selectedGame!!, uri) },
                onImportCheatPack = { uri -> viewModel.importCheatPack(selectedGame!!, uri) },
                onCreateCustomCheat = { name, format, codes -> viewModel.createCustomCheat(selectedGame!!, name, format, codes) },
                onDownloadCheatPack = { url, hash -> viewModel.downloadCheatPack(selectedGame!!, url, hash) },
                onDeleteCheatPack = viewModel::deleteCheatPack,
                onToggleFavorite = { viewModel.toggleFavorite(selectedGame!!) },
                onUpdateMetadata = { title, notes -> viewModel.updateGameMetadata(selectedGame!!, title, notes) },
                onImportCoverArt = { uri -> viewModel.importCoverArt(selectedGame!!, uri) },
                onRemoveCoverArt = { viewModel.removeCoverArt(selectedGame!!) },
                onDelete = { viewModel.deleteGame(selectedGame!!) },
                snackbarHostState = snackbarHostState
            )
        } else {
            val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                uri?.let(viewModel::importFile)
            }
            val openFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
                uri?.let(viewModel::importFolder)
            }
            val openCatalog = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                uri?.let(viewModel::importCatalog)
            }
            MainShell(
                viewModel = viewModel,
                games = games,
                settings = settings,
                catalogSources = catalogSources,
                catalogValidation = viewModel.catalogRepository.validation,
                catalogDownloads = catalogDownloads,
                downloadableCatalogHashes = catalogSources
                    .flatMap { it.manifest.games }
                    .filter(viewModel.catalogRepository::isDownloadable)
                    .mapTo(mutableSetOf()) { it.sha256.lowercase() },
                coreAvailable = viewModel.coreAvailable,
                coreStatus = viewModel.coreStatus,
                vaultRecords = vaultRecords,
                snackbarHostState = snackbarHostState,
                onImportFile = { openFile.launch(arrayOf("application/octet-stream", "*/*")) },
                onImportFolder = { openFolder.launch(null) },
                onImportCatalog = { openCatalog.launch(arrayOf("application/json", "text/json", "text/plain", "*/*")) },
                onGameSelected = viewModel::selectGame,
                onThemeChanged = viewModel::setThemeMode,
                onLayoutChanged = viewModel::setLibraryLayout,
                onDynamicColorChanged = viewModel::setDynamicColor,
                onReduceMotionChanged = viewModel::setReduceMotion,
                onReduceTransparencyChanged = viewModel::setReduceTransparency,
                onFastForwardChanged = viewModel::setFastForwardSpeed,
                onPerformanceChanged = viewModel::setPerformanceProfile,
                onDownloadCatalogEntry = viewModel::downloadCatalogEntry,
                onDeleteCatalog = viewModel::deleteCatalog,
                onDeleteVaultRecord = viewModel::deleteVaultRecord
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    viewModel: RetraViewModel,
    games: List<GameRecord>,
    settings: AppSettings,
    catalogSources: List<StoredCatalog>,
    catalogValidation: CatalogValidationResult,
    catalogDownloads: Map<String, CatalogDownloadProgress>,
    downloadableCatalogHashes: Set<String>,
    coreAvailable: Boolean,
    coreStatus: String,
    vaultRecords: List<VaultSaveRecord>,
    snackbarHostState: SnackbarHostState,
    onImportFile: () -> Unit,
    onImportFolder: () -> Unit,
    onImportCatalog: () -> Unit,
    onGameSelected: (GameRecord) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onLayoutChanged: (LibraryLayout) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onReduceTransparencyChanged: (Boolean) -> Unit,
    onFastForwardChanged: (Float) -> Unit,
    onPerformanceChanged: (PerformanceProfile) -> Unit,
    onDownloadCatalogEntry: (CatalogEntry) -> Unit,
    onDeleteCatalog: (StoredCatalog) -> Unit,
    onDeleteVaultRecord: (VaultSaveRecord) -> Unit
) {
    val initialDestination = when (settings.startupDestination) {
        StartupDestination.HOME -> Destination.HOME
        StartupDestination.LIBRARY -> Destination.LIBRARY
        StartupDestination.CONTINUE_PLAYING -> if (games.isEmpty()) Destination.LIBRARY else Destination.HOME
    }
    var destinationName by rememberSaveable { mutableStateOf(initialDestination.name) }
    val destination = Destination.valueOf(destinationName)

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp
        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    header = { LogoMarkSmall() }
                ) {
                    Spacer(Modifier.height(12.dp))
                    Destination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { destinationName = item.name },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
                MainScaffold(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                    destination = destination,
                    showBottomNavigation = false,
                    games = games,
                    settings = settings,
                    catalogSources = catalogSources,
                    catalogValidation = catalogValidation,
                    catalogDownloads = catalogDownloads,
                    downloadableCatalogHashes = downloadableCatalogHashes,
                    coreAvailable = coreAvailable,
                    coreStatus = coreStatus,
                    vaultRecords = vaultRecords,
                    snackbarHostState = snackbarHostState,
                    onDestination = { destinationName = it.name },
                    onImportFile = onImportFile,
                    onImportFolder = onImportFolder,
                    onImportCatalog = onImportCatalog,
                    onGameSelected = onGameSelected,
                    onThemeChanged = onThemeChanged,
                    onLayoutChanged = onLayoutChanged,
                    onDynamicColorChanged = onDynamicColorChanged,
                    onReduceMotionChanged = onReduceMotionChanged,
                    onReduceTransparencyChanged = onReduceTransparencyChanged,
                    onFastForwardChanged = onFastForwardChanged,
                    onPerformanceChanged = onPerformanceChanged,
                    onDownloadCatalogEntry = onDownloadCatalogEntry,
                    onDeleteCatalog = onDeleteCatalog,
                    onDeleteVaultRecord = onDeleteVaultRecord
                )
            }
        } else {
            MainScaffold(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize(),
                destination = destination,
                showBottomNavigation = true,
                games = games,
                settings = settings,
                catalogSources = catalogSources,
                catalogValidation = catalogValidation,
                catalogDownloads = catalogDownloads,
                downloadableCatalogHashes = downloadableCatalogHashes,
                coreAvailable = coreAvailable,
                coreStatus = coreStatus,
                vaultRecords = vaultRecords,
                snackbarHostState = snackbarHostState,
                onDestination = { destinationName = it.name },
                onImportFile = onImportFile,
                onImportFolder = onImportFolder,
                onImportCatalog = onImportCatalog,
                onGameSelected = onGameSelected,
                onThemeChanged = onThemeChanged,
                onLayoutChanged = onLayoutChanged,
                onDynamicColorChanged = onDynamicColorChanged,
                onReduceMotionChanged = onReduceMotionChanged,
                onReduceTransparencyChanged = onReduceTransparencyChanged,
                onFastForwardChanged = onFastForwardChanged,
                onPerformanceChanged = onPerformanceChanged,
                onDownloadCatalogEntry = onDownloadCatalogEntry,
                onDeleteCatalog = onDeleteCatalog,
                onDeleteVaultRecord = onDeleteVaultRecord
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    viewModel: RetraViewModel,
    modifier: Modifier,
    destination: Destination,
    showBottomNavigation: Boolean,
    games: List<GameRecord>,
    settings: AppSettings,
    catalogSources: List<StoredCatalog>,
    catalogValidation: CatalogValidationResult,
    catalogDownloads: Map<String, CatalogDownloadProgress>,
    downloadableCatalogHashes: Set<String>,
    coreAvailable: Boolean,
    coreStatus: String,
    vaultRecords: List<VaultSaveRecord>,
    snackbarHostState: SnackbarHostState,
    onDestination: (Destination) -> Unit,
    onImportFile: () -> Unit,
    onImportFolder: () -> Unit,
    onImportCatalog: () -> Unit,
    onGameSelected: (GameRecord) -> Unit,
    onThemeChanged: (ThemeMode) -> Unit,
    onLayoutChanged: (LibraryLayout) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onReduceTransparencyChanged: (Boolean) -> Unit,
    onFastForwardChanged: (Float) -> Unit,
    onPerformanceChanged: (PerformanceProfile) -> Unit,
    onDownloadCatalogEntry: (CatalogEntry) -> Unit,
    onDeleteCatalog: (StoredCatalog) -> Unit,
    onDeleteVaultRecord: (VaultSaveRecord) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        RetraLogoTile(size = 34.dp)
                        Text(destination.label)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        bottomBar = {
            if (showBottomNavigation) {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { onDestination(item) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AnimatedContent(
            targetState = destination,
            label = "destination",
            modifier = Modifier.fillMaxSize().consumeWindowInsets(innerPadding).padding(innerPadding)
        ) { target ->
            when (target) {
                Destination.HOME -> HomeScreen(games, settings, coreAvailable, coreStatus, onImportFile, onGameSelected)
                Destination.LIBRARY -> LibraryScreen(games, settings.libraryLayout, settings.contentDensity, onImportFile, onImportFolder, onGameSelected)
                Destination.DISCOVER -> DiscoverScreen(
                    catalogSources,
                    catalogValidation,
                    catalogDownloads,
                    downloadableCatalogHashes,
                    onImportCatalog,
                    onDownloadCatalogEntry,
                    onDeleteCatalog,
                    settings.showOnlineRecommendations,
                    viewModel
                )
                Destination.VAULT -> VaultScreen(games, vaultRecords, onDeleteVaultRecord)
                Destination.SETTINGS -> SettingsScreen(
                    settings,
                    onThemeChanged,
                    onLayoutChanged,
                    onDynamicColorChanged,
                    onReduceMotionChanged,
                    onReduceTransparencyChanged,
                    onFastForwardChanged,
                    onPerformanceChanged,
                    viewModel
                )
            }
        }
    }
}

@Composable
private fun LogoMarkSmall() {
    RetraLogoTile(modifier = Modifier.padding(10.dp), size = 52.dp)
}

@Composable
private fun HomeScreen(
    games: List<GameRecord>,
    settings: AppSettings,
    coreAvailable: Boolean,
    coreStatus: String,
    onImportFile: () -> Unit,
    onGameSelected: (GameRecord) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeroCard(game = games.firstOrNull(), coreAvailable = coreAvailable, coreStatus = coreStatus, onImportFile, onGameSelected)
        }
        if (settings.showStatistics) {
            item { SectionTitle("Your Retra") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Games", games.size.toString(), Icons.Default.Gamepad, Modifier.weight(1f))
                    StatCard("Core", if (coreAvailable) "Native" else "Pending", Icons.Default.Memory, Modifier.weight(1f))
                    StatCard("Favorites", games.count(GameRecord::favorite).toString(), Icons.Default.Star, Modifier.weight(1f))
                }
            }
        }
        val favorites = games.filter(GameRecord::favorite)
        if (favorites.isNotEmpty()) {
            item { SectionTitle("Favorites") }
            items(favorites.take(4), key = { "favorite:${it.id}" }) { game ->
                GameListRow(game, onGameSelected)
            }
        }
        item { SectionTitle("Recently added") }
        if (games.isEmpty()) {
            item { EmptyLibraryCard(onImportFile) }
        } else {
            items(games.take(6), key = { it.id }) { game ->
                GameListRow(game, onGameSelected)
            }
        }
        item { StatusCard() }
    }
}

@Composable
private fun HeroCard(
    game: GameRecord?,
    coreAvailable: Boolean,
    coreStatus: String,
    onImportFile: () -> Unit,
    onGameSelected: (GameRecord) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(RetraIndigo.copy(alpha = 0.52f), PrismCyan.copy(alpha = 0.14f)))
            ).padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = if (game == null) "YOUR LIBRARY" else "CONTINUE PLAYING",
                        icon = Icons.Default.AutoAwesome
                    )
                    RetraLogoTile(size = 58.dp)
                }
                Text(game?.title ?: "Bring your memories home", style = MaterialTheme.typography.headlineLarge)
                Text(
                    if (game == null) "Import a personal GBA backup and Retra will verify its header, calculate its SHA-256, and add it to your private library."
                    else if (coreAvailable) "Your most recent title is ready for verified native-pipeline testing."
                    else coreStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = { if (game == null) onImportFile() else onGameSelected(game) }) {
                    Icon(if (game == null) Icons.Default.Add else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (game == null) "Import a game" else "Open details")
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LibraryScreen(
    games: List<GameRecord>,
    layout: LibraryLayout,
    density: ContentDensity,
    onImportFile: () -> Unit,
    onImportFolder: () -> Unit,
    onGameSelected: (GameRecord) -> Unit
) {
    val edgePadding = when (density) {
        ContentDensity.COMFORTABLE -> 24.dp
        ContentDensity.BALANCED -> 20.dp
        ContentDensity.COMPACT -> 12.dp
    }
    val itemSpacing = when (density) {
        ContentDensity.COMFORTABLE -> 14.dp
        ContentDensity.BALANCED -> 10.dp
        ContentDensity.COMPACT -> 6.dp
    }
    var query by rememberSaveable { mutableStateOf("") }
    val visibleGames = remember(games, query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) games else games.filter { game ->
            listOf(game.title, game.displayName, game.gameCode, game.makerCode)
                .any { it.lowercase().contains(normalized) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = edgePadding, vertical = itemSpacing),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onImportFile) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("File") }
            FilledTonalButton(onClick = onImportFolder) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Folder") }
        }
        if (games.isNotEmpty()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(80) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = edgePadding),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Search title, game code, or file") },
                label = { Text("Find in library") }
            )
            Spacer(Modifier.height(itemSpacing))
        }
        if (games.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(edgePadding), contentAlignment = Alignment.Center) { EmptyLibraryCard(onImportFile) }
        } else if (visibleGames.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(edgePadding), contentAlignment = Alignment.Center) {
                Card {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(38.dp))
                        Text("No matching games", style = MaterialTheme.typography.titleLarge)
                        Text("Try a different title, game code, or file name.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else when (layout) {
            LibraryLayout.DETAILED_LIST -> LazyColumn(
                contentPadding = PaddingValues(edgePadding),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(visibleGames, key = { it.id }) { GameListRow(it, onGameSelected) }
            }
            LibraryLayout.LARGE_GRID, LibraryLayout.COMPACT_GRID -> LazyVerticalGrid(
                columns = GridCells.Adaptive(if (layout == LibraryLayout.LARGE_GRID) 172.dp else 132.dp),
                contentPadding = PaddingValues(edgePadding),
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(visibleGames, key = { it.id }) { GameGridCard(it, onGameSelected) }
            }
        }
    }
}

@Composable
private fun GameGridCard(game: GameRecord, onGameSelected: (GameRecord) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onGameSelected(game) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.86f)) {
            GameArtwork(game = game, modifier = Modifier.fillMaxSize())
            if (game.favorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Favorite",
                    tint = AdventureGold,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                )
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(game.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
            Text(game.gameCode.ifBlank { "Unknown code" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun GameListRow(game: GameRecord, onGameSelected: (GameRecord) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onGameSelected(game) }) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GameArtwork(
                game = game,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
            )
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${formatBytes(game.sizeBytes)} • ${game.gameCode.ifBlank { "No game code" }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (game.favorite) Icon(Icons.Default.Star, contentDescription = "Favorite", tint = AdventureGold)
            Icon(Icons.Default.PlayArrow, contentDescription = "Open ${game.title}")
        }
    }
}

@Composable
private fun EmptyLibraryCard(onImportFile: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Storage, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.secondary)
            Text("Your library is waiting", style = MaterialTheme.typography.titleLarge)
            Text("Choose a personal .gba backup. Retra never bundles commercial ROMs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onImportFile) { Text("Choose a file") }
        }
    }
}

@Composable
private fun DiscoverScreen(
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
    val visibleCatalogs = if (showOnlineRecommendations) catalogs else catalogs.filterNot(StoredCatalog::builtIn)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { OnlineCatalogImportCard(viewModel) }
        item { CommunityHub(viewModel) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null)
                        Column(Modifier.weight(1f)) {
                            Text("Reviewed legal catalogs", style = MaterialTheme.typography.titleMedium)
                            Text("Only public-domain, open-source, licensed homebrew, demos, and synthetic test content are accepted.")
                        }
                    }
                    OutlinedButton(onClick = onImportCatalog, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import restricted JSON catalog")
                    }
                }
            }
        }
        item {
            val valid = validation is CatalogValidationResult.Valid
            StatusPill(
                text = if (valid) "${visibleCatalogs.size} visible validated catalog source${if (visibleCatalogs.size == 1) "" else "s"}" else "Built-in manifest validation failed",
                icon = if (valid) Icons.Default.VerifiedUser else Icons.Default.Info
            )
        }
        visibleCatalogs.forEach { source ->
            item(key = "catalog:${source.manifest.catalogId}") {
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(source.manifest.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${source.manifest.owner} · ${source.manifest.games.size} entries · ${if (source.builtIn) "Built in" else "Imported"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(source.manifest.description, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!source.builtIn) {
                            IconButton(onClick = { onDeleteCatalog(source) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete ${source.manifest.name}")
                            }
                        }
                    }
                }
            }
            items(
                items = source.manifest.games,
                key = { entry -> "${source.manifest.catalogId}:${entry.id}" }
            ) { entry ->
                CatalogCard(
                    entry = entry,
                    progress = downloads[entry.sha256.lowercase()],
                    downloadable = entry.sha256.lowercase() in downloadableHashes,
                    onDownload = onDownload
                )
            }
        }
        item {
            Card {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Network provider status", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Imported manifests use strict UTF-8 JSON, reject duplicate or unknown fields, enforce bounded sizes, block local/private targets, and require HTTPS, exact length, SHA-256, GBA validation, and licensing provenance.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPill("Secure downloader ready", Icons.Default.Security)
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(
    entry: CatalogEntry,
    progress: CatalogDownloadProgress?,
    downloadable: Boolean,
    onDownload: (CatalogEntry) -> Unit
) {
    Card {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleLarge)
            Text(entry.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(entry.license)
                StatusPill(formatBytes(entry.fileSize))
            }
            Text("Creator: ${entry.creator}", style = MaterialTheme.typography.bodyMedium)
            Text(entry.distributionPermission, style = MaterialTheme.typography.bodyMedium, color = SaveMint)
            Text(entry.sha256, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (progress != null && progress.phase in setOf(CatalogDownloadPhase.CONNECTING, CatalogDownloadPhase.DOWNLOADING, CatalogDownloadPhase.VERIFYING, CatalogDownloadPhase.IMPORTING)) {
                val fraction = if (progress.totalBytes > 0) (progress.bytesDownloaded.toFloat() / progress.totalBytes).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text(progress.phase.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase), style = MaterialTheme.typography.bodySmall)
            }
            val active = progress?.phase in setOf(
                CatalogDownloadPhase.CONNECTING,
                CatalogDownloadPhase.DOWNLOADING,
                CatalogDownloadPhase.VERIFYING,
                CatalogDownloadPhase.IMPORTING
            )
            OutlinedButton(onClick = { onDownload(entry) }, enabled = downloadable && !active) {
                Icon(if (downloadable) Icons.Default.Download else Icons.Default.CloudOff, null)
                Spacer(Modifier.width(8.dp))
                Text(if (downloadable) "Download and verify" else "Preview endpoint not published")
            }
            progress?.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun VaultScreen(
    games: List<GameRecord>,
    records: List<VaultSaveRecord>,
    onDelete: (VaultSaveRecord) -> Unit
) {
    val titlesByHash = remember(games) { games.associateBy({ it.sha256.lowercase() }, GameRecord::title) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(38.dp))
                    Text("Retra Vault", style = MaterialTheme.typography.headlineMedium)
                    Text("State snapshots are bound to the ROM hash and core version, checksummed, atomically replaced, and backed up locally.")
                }
            }
        }
        item { VaultFeature("Atomic state storage", "Implemented for diagnostic and gameplay-core snapshots", Icons.Default.Storage) }
        item { VaultFeature("Rotating backups", "Implemented · three generations", Icons.Default.Security) }
        if (records.isEmpty()) {
            item {
                Card {
                    Column(
                        Modifier.fillMaxWidth().padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("No local snapshots yet", style = MaterialTheme.typography.titleLarge)
                        Text("Open a title and use Quick Save. Compatible background suspend snapshots also appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item { SectionTitle("Local timeline") }
            items(records, key = VaultSaveRecord::relativePath) { record ->
                Card {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            if (record.kind == SaveKind.SUSPEND) Icons.Default.Pause else Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                titlesByHash[record.gameSha256.lowercase()] ?: "ROM ${record.gameSha256.take(8)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                when (record.kind) {
                                    SaveKind.STATE -> "Save state · Slot ${record.slot}"
                                    SaveKind.SUSPEND -> "Automatic suspend"
                                    SaveKind.BATTERY -> "Battery save"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.createdAtEpochMillis))} · ${formatBytes(record.sizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${record.coreId} ${record.coreVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(record) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete snapshot")
                        }
                    }
                }
            }
        }
        item { VaultFeature("Cloud synchronization", "Not started; ROM upload remains disabled by design", Icons.Default.CloudOff) }
    }
}

@Composable
private fun VaultFeature(title: String, status: String, icon: ImageVector) {
    Card {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onThemeChanged: (ThemeMode) -> Unit,
    onLayoutChanged: (LibraryLayout) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onReduceTransparencyChanged: (Boolean) -> Unit,
    onFastForwardChanged: (Float) -> Unit,
    onPerformanceChanged: (PerformanceProfile) -> Unit,
    viewModel: RetraViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ProfileOverviewCard(viewModel)
        SettingsSection("Appearance", Icons.Default.Palette) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            FlowChips(ThemeMode.entries, settings.themeMode, { it.name.humanize() }, onThemeChanged)
            Text("Accent palette", style = MaterialTheme.typography.titleMedium)
            FlowChips(AccentPalette.entries, settings.accentPalette, { it.name.humanize() }, viewModel::setAccentPalette)
            ToggleSetting("Use Android dynamic color", settings.dynamicColor, onDynamicColorChanged)
            ToggleSetting("High contrast", settings.highContrast, viewModel::setHighContrast)
            ToggleSetting("Reduce transparency", settings.reduceTransparency, onReduceTransparencyChanged)
            ToggleSetting("Reduce motion", settings.reduceMotion, onReduceMotionChanged)
            Text("Glass intensity: ${(settings.glassIntensity * 100).toInt()}%")
            Slider(settings.glassIntensity, viewModel::setGlassIntensity, valueRange = 0f..1f)
            Text("Corner scale: ${settings.cornerScale.formatOne()}×")
            Slider(settings.cornerScale, viewModel::setCornerScale, valueRange = 0.75f..1.35f)
            Text("Font scale: ${settings.fontScale.formatOne()}×")
            Slider(settings.fontScale, viewModel::setFontScale, valueRange = 0.85f..1.3f)
        }
        SettingsSection("Library & home", Icons.Default.LibraryBooks) {
            Text("Default layout", style = MaterialTheme.typography.titleMedium)
            FlowChips(LibraryLayout.entries, settings.libraryLayout, { it.name.humanize() }, onLayoutChanged)
            Text("Content density", style = MaterialTheme.typography.titleMedium)
            FlowChips(ContentDensity.entries, settings.contentDensity, { it.name.humanize() }, viewModel::setContentDensity)
            Text("Startup destination", style = MaterialTheme.typography.titleMedium)
            FlowChips(StartupDestination.entries, settings.startupDestination, { it.name.humanize() }, viewModel::setStartupDestination)
            ToggleSetting("Show library statistics", settings.showStatistics, viewModel::setShowStatistics)
            ToggleSetting("Show legal online recommendations", settings.showOnlineRecommendations, viewModel::setShowOnlineRecommendations)
        }
        SettingsSection("Display & audio", Icons.Default.Memory) {
            ToggleSetting("Integer pixel scaling", settings.integerScaling, viewModel::setIntegerScaling)
            ToggleSetting("Smooth display filtering", settings.displaySmoothing, viewModel::setDisplaySmoothing)
            ToggleSetting("Show performance overlay", settings.showPerformanceOverlay, viewModel::setShowPerformanceOverlay)
            ToggleSetting("Game audio", settings.audioEnabled, viewModel::setAudioEnabled)
            Text("Master volume: ${(settings.masterVolume * 100).toInt()}%")
            Slider(settings.masterVolume, viewModel::setMasterVolume, valueRange = 0f..1f, enabled = settings.audioEnabled)
            Text("Integer scaling preserves sharp source pixels when the available surface is large enough. Smoothing is optional and disabled by default.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsSection("Controls", Icons.Default.Gamepad) {
            ToggleSetting("Show touch controls", settings.showTouchControls, viewModel::setShowTouchControls)
            Text("Touch-control opacity: ${(settings.touchControlOpacity * 100).toInt()}%")
            Slider(settings.touchControlOpacity, viewModel::setTouchControlOpacity, valueRange = 0.25f..1f, enabled = settings.showTouchControls)
            ToggleSetting("Haptic feedback", settings.hapticsEnabled, viewModel::setHapticsEnabled)
            ToggleSetting("Create suspend state in background", settings.autoSuspendOnBackground, viewModel::setAutoSuspendOnBackground)
            ToggleSetting("Pause when headphones disconnect", settings.pauseOnHeadphoneDisconnect, viewModel::setPauseOnHeadphoneDisconnect)
            ControllerInputTester(viewModel)
        }
        SettingsSection("Retra Boost", Icons.Default.Speed) {
            Text("Performance profile", style = MaterialTheme.typography.titleMedium)
            FlowChips(PerformanceProfile.entries, settings.performanceProfile, { it.name.humanize() }, onPerformanceChanged)
            Text("Default fast-forward: ${settings.fastForwardSpeed.formatOne()}×")
            Slider(
                value = settings.fastForwardSpeed,
                onValueChange = onFastForwardChanged,
                valueRange = 1f..16f,
                steps = 14,
                modifier = Modifier.semantics { contentDescription = "Default fast-forward speed" }
            )
            Text("Fast-forward changes emulation speed; it is not advertised as native game FPS enhancement.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsSection("Privacy & diagnostics", Icons.Default.Security) {
            InfoRow("ROM uploads", "Disabled")
            InfoRow("Crash reports", "Not configured")
            InfoRow("Anonymous diagnostics", "Off")
            InfoRow("Social data", "Local public labels only")
            InfoRow("Core status", "Runtime-selected; gameplay only when reviewed mGBA loads")
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun <T> FlowChips(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(3).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) })
                }
            }
        }
    }
}

@Composable
private fun ToggleSetting(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider()
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameDetailsScreen(
    game: GameRecord,
    coreAvailable: Boolean,
    coreStatus: String,
    gameplayAvailable: Boolean,
    cheatPacks: List<StoredCheatPack>,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onApplyPatch: (Uri) -> Unit,
    onImportCheatPack: (Uri) -> Unit,
    onCreateCustomCheat: (String, CheatFormat, String) -> Unit,
    onDownloadCheatPack: (String, String) -> Unit,
    onDeleteCheatPack: (StoredCheatPack) -> Unit,
    onToggleFavorite: () -> Unit,
    onUpdateMetadata: (String, String?) -> Unit,
    onImportCoverArt: (Uri) -> Unit,
    onRemoveCoverArt: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var metadataDialog by rememberSaveable { mutableStateOf(false) }
    var editedTitle by rememberSaveable(game.id, game.title) { mutableStateOf(game.title) }
    var editedNotes by rememberSaveable(game.id, game.notes) { mutableStateOf(game.notes.orEmpty()) }
    var customCheatDialog by rememberSaveable { mutableStateOf(false) }
    var onlinePackDialog by rememberSaveable { mutableStateOf(false) }
    var customName by rememberSaveable { mutableStateOf("") }
    var customCodes by rememberSaveable { mutableStateOf("") }
    var customFormatName by rememberSaveable { mutableStateOf(CheatFormat.CODEBREAKER.name) }
    var onlinePackUrl by rememberSaveable { mutableStateOf("") }
    var onlinePackHash by rememberSaveable { mutableStateOf("") }
    val patchPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(onApplyPatch)
    }
    val cheatPackPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(onImportCheatPack)
    }
    val artworkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(onImportCoverArt)
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(game.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { metadataDialog = true }) {
                        Icon(Icons.Default.Edit, "Edit library details")
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(if (game.favorite) Icons.Default.Star else Icons.Default.StarBorder, if (game.favorite) "Remove favorite" else "Add favorite")
                    }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.DeleteOutline, "Remove from library") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                GameArtwork(
                    game = game,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.65f).clip(RoundedCornerShape(28.dp))
                )
            }
            item {
                Text(game.title, style = MaterialTheme.typography.headlineLarge)
                Text(game.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                game.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Your notes", style = MaterialTheme.typography.labelLarge)
                            Text(notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                Button(onClick = onPlay, enabled = coreAvailable, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(when {
                        !coreAvailable -> "Native core unavailable"
                        gameplayAvailable -> "Play"
                        else -> "Open native diagnostics"
                    })
                }
                if (!coreAvailable) Text(coreStatus, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    onClick = { patchPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Apply IPS, UPS, or BPS patch")
                }
                Text(
                    "Retra verifies the patch, preserves the base ROM, and creates a separate local library entry.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { artworkPicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (game.coverArtPath == null) "Add cover art" else "Replace cover art") }
                    if (game.coverArtPath != null) {
                        OutlinedButton(onClick = onRemoveCoverArt, modifier = Modifier.weight(1f)) { Text("Remove cover") }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Verified ROM identity", style = MaterialTheme.typography.titleLarge)
                        DetailRow("Game code", game.gameCode.ifBlank { "Unavailable" })
                        DetailRow("Maker code", game.makerCode.ifBlank { "Unavailable" })
                        DetailRow("Revision", game.softwareVersion.toString())
                        DetailRow("Size", formatBytes(game.sizeBytes))
                        Text("SHA-256", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(game.sha256, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
                        if (game.origin == "LOCAL_PATCH") {
                            HorizontalDivider()
                            Text("Patch provenance", style = MaterialTheme.typography.titleMedium)
                            DetailRow("Format", game.patchFormat ?: "Unknown")
                            DetailRow("Patch file", game.patchDisplayName ?: "Unknown")
                            DetailRow("Base SHA-256", game.baseSha256?.take(16)?.plus("…") ?: "Unknown")
                            DetailRow("Patch SHA-256", game.patchSha256?.take(16)?.plus("…") ?: "Unknown")
                        }
                        if (game.origin == "LEGAL_CATALOG") {
                            HorizontalDivider()
                            Text("Catalog provenance", style = MaterialTheme.typography.titleMedium)
                            DetailRow("Creator", game.creator ?: "Unknown")
                            DetailRow("License", game.license ?: "Unknown")
                            DetailRow("Distribution", game.distributionPermission ?: "Unknown")
                            DetailRow(
                                "Source",
                                game.sourceUrl?.let { runCatching { java.net.URI(it).host }.getOrNull() }
                                    ?: "Unknown"
                            )
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("Retra Codes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        }
                        Text(
                            "Only declarative, ROM-hash-bound code packs are accepted. Retra never executes downloaded scripts.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { cheatPackPicker.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import Retra Codes pack")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { customCheatDialog = true }, modifier = Modifier.weight(1f)) {
                                Text("Create custom")
                            }
                            OutlinedButton(onClick = { onlinePackDialog = true }, modifier = Modifier.weight(1f)) {
                                Text("Download pack")
                            }
                        }
                        if (cheatPacks.isEmpty()) {
                            Text("No compatible local code packs are stored for this exact ROM.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            cheatPacks.forEach { stored ->
                                HorizontalDivider()
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(stored.provider, style = MaterialTheme.typography.titleMedium)
                                        Text("${stored.cheatCount} cheats · ${stored.fileName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        stored.pack.cheats.take(3).forEach { cheat ->
                                            Text("• ${cheat.name} · ${cheat.risk.name.lowercase().replaceFirstChar(Char::titlecase)}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (stored.cheatCount > 3) Text("+ ${stored.cheatCount - 3} more", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { onDeleteCheatPack(stored) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Retra Codes pack")
                                    }
                                }
                            }
                        }
                        Text(
                            if (gameplayAvailable) "Launch the game, open the session menu, and choose Retra Codes to activate a verified code. A protected pre-cheat state is created automatically."
                            else "Pack management works now; activation requires the gameplay core.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Feature readiness", style = MaterialTheme.typography.titleLarge)
                        DetailRow("Local metadata", "Implemented")
                        DetailRow("Duplicate detection", "Implemented")
                        DetailRow("Native frame pipeline", "Implemented")
                        DetailRow("Touch and gamepad input", "Implemented")
                        DetailRow("Atomic state snapshots", "Implemented")
                        DetailRow("IPS / UPS / BPS patching", "Implemented and host-tested")
                        DetailRow("Retra Codes pack import", "Implemented and host-tested")
                        DetailRow("Legal catalog downloads", "Implemented; live provider required")
                        DetailRow("Cheat activation", if (gameplayAvailable) "Implemented; device validation pending" else "Requires gameplay core")
                        DetailRow("GBA instruction execution", if (gameplayAvailable) "Gameplay core loaded" else "Awaiting reviewed mGBA library")
                        DetailRow("Battery saves", if (gameplayAvailable) "Gameplay core available; device validation pending" else "Awaiting reviewed mGBA library")
                    }
                }
            }
        }
    }
    if (metadataDialog) {
        AlertDialog(
            onDismissRequest = { metadataDialog = false },
            title = { Text("Edit library details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it.take(120) },
                        label = { Text("Display title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { editedNotes = it.take(4_000) },
                        label = { Text("Personal notes") },
                        minLines = 3,
                        maxLines = 7,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Editing library metadata never changes the ROM file or its verified SHA-256.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateMetadata(editedTitle, editedNotes)
                        metadataDialog = false
                    },
                    enabled = editedTitle.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { metadataDialog = false }) { Text("Cancel") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove from Retra?") },
            text = { Text("This removes only the library record. Retra will not delete your source ROM file.") },
            confirmButton = { Button(onClick = { confirmDelete = false; onDelete() }) { Text("Remove") } },
            dismissButton = { OutlinedButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
    if (customCheatDialog) {
        AlertDialog(
            onDismissRequest = { customCheatDialog = false },
            title = { Text("Create a custom Retra Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(customName, { customName = it.take(120) }, label = { Text("Cheat name") }, singleLine = true)
                    FlowChips(CheatFormat.entries, CheatFormat.valueOf(customFormatName), { it.name.humanize() }) { customFormatName = it.name }
                    OutlinedTextField(
                        customCodes,
                        { customCodes = it.take(16_000) },
                        label = { Text("One code per line") },
                        minLines = 4,
                        maxLines = 8,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    Text("The code is bound to this exact ROM SHA-256 and parsed as declarative data.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCustomCheat(customName, CheatFormat.valueOf(customFormatName), customCodes)
                        customCheatDialog = false
                    },
                    enabled = customName.isNotBlank() && customCodes.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = { OutlinedButton(onClick = { customCheatDialog = false }) { Text("Cancel") } }
        )
    }
    if (onlinePackDialog) {
        AlertDialog(
            onDismissRequest = { onlinePackDialog = false },
            title = { Text("Download a verified code pack") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(onlinePackUrl, { onlinePackUrl = it.take(1024) }, label = { Text("HTTPS .rcc or .txt URL") }, singleLine = true)
                    OutlinedTextField(
                        onlinePackHash,
                        { onlinePackHash = it.filter(Char::isLetterOrDigit).take(64) },
                        label = { Text("Expected SHA-256") },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    Text("Retra blocks local targets and cross-host redirects, verifies the exact hash, and rejects scripts or executable fields.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDownloadCheatPack(onlinePackUrl, onlinePackHash)
                        onlinePackDialog = false
                    },
                    enabled = onlinePackUrl.startsWith("https://") && onlinePackHash.length == 64
                ) { Text("Download") }
            },
            dismissButton = { OutlinedButton(onClick = { onlinePackDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatusCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Build honesty", style = MaterialTheme.typography.titleLarge)
            Text("The local library, Vault saves, diagnostic native pipeline, host-verified mGBA/libretro adapter, secure legal-catalog and cheat-pack downloads, IPS/UPS/BPS patching, in-session declarative cheats, local achievements, private-first profiles, and checksummed LAN transport are implemented in source. Android gameplay still requires a reviewed bundled mGBA library and device validation; provider OAuth, internet relay, cloud sync, rewind, and real GBA link play require external credentials or core callbacks.")
        }
    }
}

@Composable
private fun StatusPill(text: String, icon: ImageVector? = null) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) Icon(icon, null, modifier = Modifier.size(17.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleLarge) }

internal fun gameArtworkBrush(game: GameRecord): Brush {
    val seed = game.sha256.take(8).toLongOrNull(16) ?: game.title.hashCode().toLong()
    val palettes = listOf(
        listOf(RetraIndigo.copy(alpha = 0.82f), MemoryViolet.copy(alpha = 0.32f)),
        listOf(PrismCyan.copy(alpha = 0.58f), RetraIndigo.copy(alpha = 0.28f)),
        listOf(SaveMint.copy(alpha = 0.52f), PrismCyan.copy(alpha = 0.22f)),
        listOf(AdventureGold.copy(alpha = 0.48f), MemoryViolet.copy(alpha = 0.24f))
    )
    return Brush.linearGradient(palettes[kotlin.math.abs(seed % palettes.size).toInt()])
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MiB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024L -> "%.1f KiB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun String.humanize(): String = lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase)

private fun Float.formatOne(): String = "%.1f".format(this)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun OnboardingPreview() {
    RetraTheme(ThemeMode.DARK, false) {
        OnboardingScreen(
            settings = AppSettings(themeMode = ThemeMode.DARK),
            account = null,
            authOperation = app.retra.emulator.auth.AuthOperation.IDLE,
            googleConfigured = false,
            onThemeChanged = {},
            onAccentChanged = {},
            onGoogleSignIn = {},
            onComplete = {}
        )
    }
}
