
package app.retra.emulator

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import app.retra.core.model.CatalogContentKind
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
import app.retra.emulator.ui.theme.Graphite
import app.retra.emulator.ui.theme.RetraIndigo
import app.retra.emulator.ui.theme.RetraTheme
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SoftViolet
import app.retra.emulator.data.PendingPatch
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private enum class Destination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.LibraryBooks),
    DISCOVER("Discover", Icons.Default.Search),
    SETTINGS("Settings", Icons.Default.Settings)
}

private enum class SettingsCategory(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Default.Palette),
    LIBRARY("Library", Icons.Default.LibraryBooks),
    VAULT("Vault", Icons.Default.Save),
    COMMUNITY("Community", Icons.Default.Groups),
    PLAYER("Player", Icons.Default.Memory),
    FEEDBACK("Feel", Icons.Default.VolumeUp),
    NOTIFICATIONS("Alerts", Icons.Default.Notifications),
    CONTROLS("Controls", Icons.Default.Gamepad),
    BOOST("Boost", Icons.Default.Speed),
    PRIVACY("Privacy", Icons.Default.Security)
}

private enum class LibraryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
    UNPLAYED("Unplayed"),
    PATCHED("Patched"),
    HOMEBREW("Homebrew")
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
    val pendingPatch by viewModel.pendingPatch.collectAsStateWithLifecycle()
    val compatibleBases by viewModel.compatibleBases.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RetraTheme(settings) {
        CompositionLocalProvider(
            LocalRetraFeedback provides viewModel::emitFeedback,
            LocalRetraSettings provides settings
        ) {
            RetraBackdrop(settings) {
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
                onUpdateOrganization = { collections, tags -> viewModel.updateGameOrganization(selectedGame!!, collections, tags) },
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
            Box(Modifier.fillMaxSize()) {
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
                if (pendingPatch != null) {
                    PendingPatchSheet(
                        pending = pendingPatch!!,
                        compatibleBases = compatibleBases,
                        onDismiss = viewModel::dismissPendingPatch,
                        onApply = viewModel::applyPendingPatch
                    )
                }
            }
        }
            }
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
    val destination = runCatching { Destination.valueOf(destinationName) }.getOrElse {
        destinationName = Destination.HOME.name
        Destination.HOME
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp
        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                PremiumNavigationRail(
                    settings = settings,
                    selected = destination,
                    onSelected = { item ->
                        viewModel.emitFeedback(FeedbackCue.TAP)
                        destinationName = item.name
                    }
                )
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
        containerColor = Color.Transparent,
        topBar = {
            PremiumTopBar(
                settings = settings,
                title = destination.label,
                coreAvailable = coreAvailable
            )
        },
        bottomBar = {
            if (showBottomNavigation) {
                PremiumBottomBar(
                    settings = settings,
                    selected = destination,
                    onSelected = { item ->
                        viewModel.emitFeedback(FeedbackCue.TAP)
                        onDestination(item)
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            RetraAnimatedContent(
                targetState = destination,
                reduceMotion = settings.reduceMotion,
                label = "destination",
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp)
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
                    Destination.SETTINGS -> SettingsScreen(
                        settings,
                        games,
                        vaultRecords,
                        onThemeChanged,
                        onLayoutChanged,
                        onDynamicColorChanged,
                        onReduceMotionChanged,
                        onReduceTransparencyChanged,
                        onFastForwardChanged,
                        onPerformanceChanged,
                        onDeleteVaultRecord,
                        viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumTopBar(
    settings: AppSettings,
    title: String,
    coreAvailable: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            settings = settings,
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RetraLogoTile(size = 34.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (coreAvailable) "Ready to play" else "Library mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (coreAvailable) SaveMint.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(if (coreAvailable) SaveMint else MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumBottomBar(
    settings: AppSettings,
    selected: Destination,
    onSelected: (Destination) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier.fillMaxWidth(),
            settings = settings,
            cornerRadius = 28.dp,
            contentPadding = PaddingValues(6.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Destination.entries.forEach { item ->
                    val selectedItem = selected == item
                    GlassPill(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelected(item) }
                            .semantics { contentDescription = item.label },
                        selected = selectedItem
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                fontWeight = if (selectedItem) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selectedItem) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumNavigationRail(
    settings: AppSettings,
    selected: Destination,
    onSelected: (Destination) -> Unit
) {
    Box(
        Modifier
            .fillMaxHeight()
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            settings = settings,
            cornerRadius = 30.dp,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RetraLogoTile(size = 46.dp)
                Spacer(Modifier.height(8.dp))
                Destination.entries.forEach { item ->
                    GlassPill(
                        modifier = Modifier
                            .width(78.dp)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onSelected(item) },
                        selected = selected == item
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp))
                            Text(item.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
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
            HeroCard(game = games.firstOrNull(), settings = settings, coreAvailable = coreAvailable, coreStatus = coreStatus, onImportFile, onGameSelected)
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
    settings: AppSettings,
    coreAvailable: Boolean,
    coreStatus: String,
    onImportFile: () -> Unit,
    onGameSelected: (GameRecord) -> Unit
) {
    val feedback = LocalRetraFeedback.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        settings = settings,
        cornerRadius = 30.dp,
        contentPadding = PaddingValues(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(
                    text = if (game == null) "PRIVATE LIBRARY" else "CONTINUE PLAYING",
                    icon = if (game == null) Icons.Default.Security else Icons.Default.PlayArrow
                )
                RetraLogoTile(size = 50.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    game?.title ?: "Private library, ready when you are.",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    if (game == null) {
                        "Import a personal GBA backup. Retra checks the header, hashes the file, and keeps everything on-device by default."
                    } else if (coreAvailable) {
                        "Resume with your display, audio, controls, cheats, and save profile intact."
                    } else {
                        coreStatus
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        if (game == null) onImportFile() else onGameSelected(game)
                    }
                ) {
                    Icon(if (game == null) Icons.Default.Add else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (game == null) "Import game" else "Continue")
                }
                Text(
                    if (coreAvailable) "Core ready" else "Library available",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (coreAvailable) SaveMint else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, cornerRadius = 22.dp, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var filterName by rememberSaveable { mutableStateOf(LibraryFilter.ALL.name) }
    val filter = LibraryFilter.valueOf(filterName)
    val visibleGames = remember(games, query, filter) {
        val normalized = query.trim().lowercase()
        games.asSequence()
            .filter { game ->
                when (filter) {
                    LibraryFilter.ALL -> true
                    LibraryFilter.FAVORITES -> game.favorite
                    LibraryFilter.RECENT -> game.lastPlayedAtEpochMillis != null
                    LibraryFilter.UNPLAYED -> game.lastPlayedAtEpochMillis == null
                    LibraryFilter.PATCHED -> game.origin == "LOCAL_PATCH" || !game.patchSha256.isNullOrBlank() ||
                        game.tags.any { it.equals("patched", ignoreCase = true) }
                    LibraryFilter.HOMEBREW -> game.origin == "LEGAL_CATALOG" ||
                        game.tags.any { it.equals("homebrew", ignoreCase = true) }
                }
            }
            .filter { game ->
                if (normalized.isBlank()) true else {
                    buildList {
                        add(game.title)
                        add(game.displayName)
                        add(game.gameCode)
                        addAll(game.tags)
                        addAll(game.collections)
                    }.any { it.lowercase().contains(normalized) }
                }
            }
            .sortedByDescending { game ->
                when (filter) {
                    LibraryFilter.RECENT -> game.lastPlayedAtEpochMillis ?: 0L
                    else -> game.importedAtEpochMillis
                }
            }
            .toList()
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
                placeholder = { Text("Search title, tags, or collections") },
                label = { Text("Find in library") }
            )
            Spacer(Modifier.height(itemSpacing))
            LazyRow(
                contentPadding = PaddingValues(horizontal = edgePadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(LibraryFilter.entries, key = { it.name }) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filterName = item.name },
                        label = { Text(item.label) }
                    )
                }
            }
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
                        Text("Try a different title, tag, collection, or filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val feedback = LocalRetraFeedback.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth().clickable {
            feedback(FeedbackCue.TAP)
            onGameSelected(game)
        },
        cornerRadius = 24.dp
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.86f)) {
                GameArtwork(game = game, modifier = Modifier.fillMaxSize())
                if (game.favorite) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(9.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = AdventureGold,
                            modifier = Modifier.padding(7.dp).size(18.dp)
                        )
                    }
                }
            }
            Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(game.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(
                    game.gameCode.ifBlank { "Unknown code" },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun GameListRow(game: GameRecord, onGameSelected: (GameRecord) -> Unit) {
    val feedback = LocalRetraFeedback.current
    GlassPanel(
        modifier = Modifier.fillMaxWidth().clickable {
            feedback(FeedbackCue.TAP)
            onGameSelected(game)
        },
        cornerRadius = 22.dp,
        contentPadding = PaddingValues(13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GameArtwork(
                game = game,
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(16.dp))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${formatBytes(game.sizeBytes)} · ${game.gameCode.ifBlank { "No game code" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (game.favorite) Icon(Icons.Default.Star, contentDescription = "Favorite", tint = AdventureGold, modifier = Modifier.size(19.dp))
            Icon(Icons.Default.PlayArrow, contentDescription = "Open ${game.title}", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun EmptyLibraryCard(onImportFile: () -> Unit) {
    val feedback = LocalRetraFeedback.current
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp, contentPadding = PaddingValues(26.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RetraLogoTile(size = 64.dp)
            Text("Start your private library", style = MaterialTheme.typography.titleLarge)
            Text(
                "Choose one of your own .gba backups. Retra never bundles commercial games.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    feedback(FeedbackCue.CONFIRM)
                    onImportFile()
                },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose a game")
            }
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
    val feedback = LocalRetraFeedback.current
    val context = LocalContext.current
    val curated by viewModel.curatedReleases.collectAsStateWithLifecycle()
    val visibleCatalogs = if (showOnlineRecommendations) catalogs else catalogs.filterNot(StoredCatalog::builtIn)
    LaunchedEffect(Unit) { viewModel.refreshCuratedReleases() }
    fun openExternal(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(17.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Retra Curated", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pinned community and homebrew destinations. Retra never bundles commercial ROMs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    viewModel.catalogRepository.curatedLinks.forEach { link ->
                        OutlinedButton(
                            onClick = {
                                feedback(FeedbackCue.CONFIRM)
                                openExternal(link.sourcePageUrl)
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(link.title)
                        }
                    }
                }
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(17.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Official creator releases", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "One-tap download appears only when GitHub publishes a SHA-256 digest. Otherwise open the creator page.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                feedback(FeedbackCue.TAP)
                                viewModel.refreshCuratedReleases()
                            },
                            enabled = !curated.refreshing
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh official releases")
                        }
                    }
                    curated.links.forEach { link ->
                        OutlinedButton(
                            onClick = {
                                feedback(FeedbackCue.CONFIRM)
                                openExternal(link.sourcePageUrl)
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.OpenInNew, null)
                            Spacer(Modifier.width(8.dp))
                            Text(link.title)
                        }
                    }
                    if (curated.refreshing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    curated.lastError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    curated.downloadableEntries.forEach { entry ->
                        CatalogCard(
                            entry = entry,
                            progress = downloads[entry.sha256.lowercase()],
                            downloadable = true,
                            onDownload = onDownload,
                            onOpenSource = { openExternal(it) }
                        )
                    }
                }
            }
        }
        item { OnlineCatalogImportCard(viewModel) }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(17.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = SaveMint.copy(alpha = 0.13f)) {
                            Icon(Icons.Default.Security, null, tint = SaveMint, modifier = Modifier.padding(9.dp).size(20.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("SHA-256 pinned custom manifests", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Import reviewed public-domain, open-source, licensed homebrew, demos, and synthetic fixtures only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            feedback(FeedbackCue.CONFIRM)
                            onImportCatalog()
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import catalog")
                    }
                }
            }
        }
        item {
            val valid = validation is CatalogValidationResult.Valid
            StatusPill(
                text = if (valid) "${visibleCatalogs.size} validated catalog source${if (visibleCatalogs.size == 1) "" else "s"}" else "Catalog validation failed",
                icon = if (valid) Icons.Default.VerifiedUser else Icons.Default.Info
            )
        }
        visibleCatalogs.forEach { source ->
            item(key = "catalog:${source.manifest.catalogId}") {
                GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(source.manifest.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${source.manifest.owner} · ${source.manifest.games.size} entries · ${if (source.builtIn) "Built in" else "Imported"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(source.manifest.description, style = MaterialTheme.typography.bodySmall)
                        }
                        if (!source.builtIn) {
                            IconButton(onClick = {
                                feedback(FeedbackCue.TAP)
                                onDeleteCatalog(source)
                            }) {
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
                    onDownload = onDownload,
                    onOpenSource = { openExternal(it) }
                )
            }
        }
        item {
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, contentPadding = PaddingValues(17.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Secure by construction", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Catalogs are strict UTF-8 JSON. Retra blocks private targets, cross-host redirects, unknown fields, oversized files, invalid hashes, and missing license provenance. External creator links open in the browser.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPill("Downloader ready", Icons.Default.Security)
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
    onDownload: (CatalogEntry) -> Unit,
    onOpenSource: (String) -> Unit = {}
) {
    val feedback = LocalRetraFeedback.current
    val external = entry.contentKind == CatalogContentKind.EXTERNAL || (!downloadable && !entry.sourcePageUrl.isNullOrBlank())
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleLarge)
            Text(entry.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(entry.license)
                if (entry.contentKind != CatalogContentKind.EXTERNAL) StatusPill(formatBytes(entry.fileSize))
                StatusPill(entry.contentKind.name.humanize())
            }
            Text("By ${entry.creator}", style = MaterialTheme.typography.labelLarge)
            Text(entry.distributionPermission, style = MaterialTheme.typography.bodySmall, color = SaveMint)
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
            if (external) {
                val page = entry.sourcePageUrl
                OutlinedButton(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        if (!page.isNullOrBlank()) onOpenSource(page)
                    },
                    enabled = !page.isNullOrBlank(),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open creator page")
                }
            } else {
                OutlinedButton(
                    onClick = {
                        feedback(FeedbackCue.CONFIRM)
                        onDownload(entry)
                    },
                    enabled = downloadable && !active,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Icon(if (downloadable) Icons.Default.Download else Icons.Default.CloudOff, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (downloadable) "Download and verify" else "Preview only")
                }
            }
            progress?.message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun VaultSection(
    games: List<GameRecord>,
    records: List<VaultSaveRecord>,
    onDelete: (VaultSaveRecord) -> Unit
) {
    val feedback = LocalRetraFeedback.current
    val titlesByHash = remember(games) { games.associateBy({ it.sha256.lowercase() }, GameRecord::title) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp, contentPadding = PaddingValues(22.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                    Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(28.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Retra Vault", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Checksummed states, automatic suspend snapshots, and rotating local backups. Session save menus in the player still work the same way.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        VaultFeature("Atomic state storage", "Bound to ROM identity and emulator core version", Icons.Default.Storage)
        VaultFeature("Rotating backups", "Three local generations before replacement", Icons.Default.Security)
        if (records.isEmpty()) {
            GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(22.dp)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("No snapshots yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Open a game and use Quick Save. Automatic background suspends also appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            SectionTitle("Local timeline")
            records.forEach { record ->
                GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (record.kind == SaveKind.SUSPEND) Icons.Default.Pause else Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
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
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.createdAtEpochMillis))} · ${formatBytes(record.sizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            feedback(FeedbackCue.TAP)
                            onDelete(record)
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete snapshot")
                        }
                    }
                }
            }
        }
        VaultFeature("Cloud synchronization", "Optional save-only provider planned; ROM upload remains disabled", Icons.Default.CloudOff)
    }
}

@Composable
private fun VaultFeature(title: String, status: String, icon: ImageVector) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, contentPadding = PaddingValues(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    games: List<GameRecord>,
    vaultRecords: List<VaultSaveRecord>,
    onThemeChanged: (ThemeMode) -> Unit,
    onLayoutChanged: (LibraryLayout) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
    onReduceMotionChanged: (Boolean) -> Unit,
    onReduceTransparencyChanged: (Boolean) -> Unit,
    onFastForwardChanged: (Float) -> Unit,
    onPerformanceChanged: (PerformanceProfile) -> Unit,
    onDeleteVaultRecord: (VaultSaveRecord) -> Unit,
    viewModel: RetraViewModel
) {
    var selectedName by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE.name) }
    val selected = SettingsCategory.valueOf(selectedName)
    val feedback = LocalRetraFeedback.current

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProfileOverviewCard(viewModel)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Preferences", style = MaterialTheme.typography.titleLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SettingsCategory.entries, key = { it.name }) { category ->
                    FilterChip(
                        selected = category == selected,
                        onClick = {
                            feedback(FeedbackCue.TAP)
                            selectedName = category.name
                        },
                        leadingIcon = { Icon(category.icon, null, modifier = Modifier.size(18.dp)) },
                        label = { Text(category.label) }
                    )
                }
            }
        }

        when (selected) {
            SettingsCategory.APPEARANCE -> SettingsSection("Appearance", Icons.Default.Palette) {
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

            SettingsCategory.LIBRARY -> SettingsSection("Library & home", Icons.Default.LibraryBooks) {
                Text("Default layout", style = MaterialTheme.typography.titleMedium)
                FlowChips(LibraryLayout.entries, settings.libraryLayout, { it.name.humanize() }, onLayoutChanged)
                Text("Content density", style = MaterialTheme.typography.titleMedium)
                FlowChips(ContentDensity.entries, settings.contentDensity, { it.name.humanize() }, viewModel::setContentDensity)
                Text("Startup destination", style = MaterialTheme.typography.titleMedium)
                FlowChips(StartupDestination.entries, settings.startupDestination, { it.name.humanize() }, viewModel::setStartupDestination)
                ToggleSetting("Show library statistics", settings.showStatistics, viewModel::setShowStatistics)
                ToggleSetting("Show legal online recommendations", settings.showOnlineRecommendations, viewModel::setShowOnlineRecommendations)
            }

            SettingsCategory.VAULT -> {
                VaultSection(games, vaultRecords, onDeleteVaultRecord)
            }

            SettingsCategory.COMMUNITY -> {
                CommunityHub(viewModel)
            }

            SettingsCategory.PLAYER -> SettingsSection("Display & audio", Icons.Default.Memory) {
                ToggleSetting("Integer pixel scaling", settings.integerScaling, viewModel::setIntegerScaling)
                ToggleSetting("Smooth display filtering", settings.displaySmoothing, viewModel::setDisplaySmoothing)
                ToggleSetting("Show performance overlay", settings.showPerformanceOverlay, viewModel::setShowPerformanceOverlay)
                ToggleSetting("Game audio", settings.audioEnabled, viewModel::setAudioEnabled)
                Text("Master volume: ${(settings.masterVolume * 100).toInt()}%")
                Slider(settings.masterVolume, viewModel::setMasterVolume, valueRange = 0f..1f, enabled = settings.audioEnabled)
                Text(
                    "Integer scaling preserves sharp source pixels. Smoothing is optional and disabled by default.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCategory.FEEDBACK -> SettingsSection("Feel & sound", Icons.Default.VolumeUp) {
                ToggleSetting("Haptic feedback", settings.hapticsEnabled, viewModel::setHapticsEnabled)
                ToggleSetting("Retra interface sounds", settings.soundEffectsEnabled, viewModel::setSoundEffectsEnabled)
                Text("Interface sound volume: ${(settings.soundEffectsVolume * 100).toInt()}%")
                Slider(
                    settings.soundEffectsVolume,
                    viewModel::setSoundEffectsVolume,
                    valueRange = 0f..1f,
                    enabled = settings.soundEffectsEnabled
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { viewModel.emitFeedback(FeedbackCue.CONFIRM) }) { Text("Test tap") }
                    OutlinedButton(onClick = { viewModel.emitFeedback(FeedbackCue.ACHIEVEMENT) }) { Text("Test celebration") }
                }
                Text(
                    "Short original cues and subtle semantic haptics reinforce actions without becoming noisy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCategory.NOTIFICATIONS -> SettingsSection("Notifications", Icons.Default.Notifications) {
                NotificationPreferences(settings, viewModel)
            }

            SettingsCategory.CONTROLS -> SettingsSection("Controls", Icons.Default.Gamepad) {
                ToggleSetting("Show touch controls", settings.showTouchControls, viewModel::setShowTouchControls)
                Text("Touch-control opacity: ${(settings.touchControlOpacity * 100).toInt()}%")
                Slider(settings.touchControlOpacity, viewModel::setTouchControlOpacity, valueRange = 0.25f..1f, enabled = settings.showTouchControls)
                ToggleSetting("Create suspend state in background", settings.autoSuspendOnBackground, viewModel::setAutoSuspendOnBackground)
                ToggleSetting("Pause when headphones disconnect", settings.pauseOnHeadphoneDisconnect, viewModel::setPauseOnHeadphoneDisconnect)
                ControllerInputTester(viewModel)
            }

            SettingsCategory.BOOST -> SettingsSection("Retra Boost", Icons.Default.Speed) {
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
                Text(
                    "Fast-forward changes emulation speed; it is not presented as native game FPS enhancement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsCategory.PRIVACY -> SettingsSection("Privacy & diagnostics", Icons.Default.Security) {
                InfoRow("ROM uploads", "Disabled")
                InfoRow("Crash reports", "Not configured")
                InfoRow("Anonymous diagnostics", "Off")
                InfoRow("Social data", "Local public labels only")
                InfoRow("Core status", "Gameplay only when the reviewed mGBA core loads")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun <T> FlowChips(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    val feedback = LocalRetraFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(3).forEach { rowValues ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowValues.forEach { value ->
                    FilterChip(
                        selected = value == selected,
                        onClick = {
                            feedback(FeedbackCue.TAP)
                            onSelected(value)
                        },
                        label = { Text(label(value)) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ToggleSetting(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val feedback = LocalRetraFeedback.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            Modifier.weight(1f),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { value ->
                feedback(FeedbackCue.TAP)
                onCheckedChange(value)
            }
        )
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
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 26.dp, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(8.dp).size(19.dp))
                }
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
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
    onUpdateOrganization: (List<String>, List<String>) -> Unit,
    onImportCoverArt: (Uri) -> Unit,
    onRemoveCoverArt: () -> Unit,
    onDelete: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var metadataDialog by rememberSaveable { mutableStateOf(false) }
    var organizationDialog by rememberSaveable { mutableStateOf(false) }
    var editedTitle by rememberSaveable(game.id, game.title) { mutableStateOf(game.title) }
    var editedNotes by rememberSaveable(game.id, game.notes) { mutableStateOf(game.notes.orEmpty()) }
    var editedCollections by rememberSaveable(game.id, game.collections.joinToString(",")) {
        mutableStateOf(game.collections.joinToString(", "))
    }
    var editedTags by rememberSaveable(game.id, game.tags.joinToString(",")) {
        mutableStateOf(game.tags.joinToString(", "))
    }
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
                    IconButton(onClick = { organizationDialog = true }) {
                        Icon(Icons.Default.LibraryBooks, "Edit collections and tags")
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
                if (game.collections.isNotEmpty() || game.tags.isNotEmpty()) {
                    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (game.collections.isNotEmpty()) {
                            Text("Collections", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(game.collections) { StatusPill(it) }
                            }
                        }
                        if (game.tags.isNotEmpty()) {
                            Text("Tags", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(game.tags) { StatusPill(it) }
                            }
                        }
                    }
                }
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
    if (organizationDialog) {
        AlertDialog(
            onDismissRequest = { organizationDialog = false },
            title = { Text("Collections and tags") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editedCollections,
                        onValueChange = { editedCollections = it.take(400) },
                        label = { Text("Collections") },
                        supportingText = { Text("Comma-separated, for example Favorites shelf, Speedruns") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedTags,
                        onValueChange = { editedTags = it.take(400) },
                        label = { Text("Tags") },
                        supportingText = { Text("Comma-separated, for example homebrew, patched") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val collections = editedCollections.split(',').map(String::trim).filter(String::isNotEmpty)
                        val tags = editedTags.split(',').map(String::trim).filter(String::isNotEmpty)
                        onUpdateOrganization(collections, tags)
                        organizationDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { organizationDialog = false }) { Text("Cancel") } }
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
    GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 24.dp, contentPadding = PaddingValues(18.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = SaveMint.copy(alpha = 0.13f)) {
                Icon(Icons.Default.Security, null, tint = SaveMint, modifier = Modifier.padding(9.dp).size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Private by default", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Your local ROMs and saves stay on this device unless you explicitly choose a verified catalog or future sync provider.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        listOf(RetraIndigo.copy(alpha = 0.82f), SoftViolet.copy(alpha = 0.28f)),
        listOf(RetraIndigo.copy(alpha = 0.58f), Graphite.copy(alpha = 0.55f)),
        listOf(SaveMint.copy(alpha = 0.42f), RetraIndigo.copy(alpha = 0.28f)),
        listOf(AdventureGold.copy(alpha = 0.40f), SoftViolet.copy(alpha = 0.22f))
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
            onGoogleSignIn = {},
            onComplete = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingPatchSheet(
    pending: PendingPatch,
    compatibleBases: List<GameRecord>,
    onDismiss: () -> Unit,
    onApply: (GameRecord) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Apply patch", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                pending.displayName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Format ${pending.descriptor.format.name}. Choose a compatible base ROM from your library. Retra keeps the original file and creates a separate patched entry.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            pending.knownHint?.let { hint ->
                StatusPill("Suggested base: $hint", Icons.Default.AutoAwesome)
            }
            if (compatibleBases.isEmpty()) {
                GlassPanel(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, contentPadding = PaddingValues(18.dp)) {
                    Text(
                        "No compatible base ROMs are in the library yet. Import the matching .gba first, then re-open this patch.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                compatibleBases.forEach { base ->
                    GlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onApply(base) },
                        cornerRadius = 20.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(base.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${formatBytes(base.sizeBytes)} · ${base.gameCode.ifBlank { "No code" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Apply to ${base.title}", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text("Dismiss")
            }
        }
    }
}
