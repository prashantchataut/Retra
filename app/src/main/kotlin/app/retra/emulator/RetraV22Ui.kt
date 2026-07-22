package app.retra.emulator

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.retra.core.achievements.AchievementIntegrityPolicy
import app.retra.core.model.AccentPalette
import app.retra.core.model.AppSettings
import app.retra.core.model.ContentDensity
import app.retra.core.model.ControlLayoutPreset
import app.retra.core.model.ControlVisualStyle
import app.retra.core.model.GameRecord
import app.retra.core.model.LibraryLayout
import app.retra.core.model.PerformanceProfile
import app.retra.core.model.ScreenScalingMode
import app.retra.core.model.StartupDestination
import app.retra.core.model.ThemeMode
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.data.HomebrewHubEntry
import app.retra.emulator.data.PendingPatch
import app.retra.emulator.ui.theme.MemoryAqua
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RetraBlue
import app.retra.emulator.ui.theme.RetraTheme
import app.retra.emulator.ui.theme.SaveMint
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.launch

private enum class V22Destination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.LibraryBooks),
    DISCOVER("Discover", Icons.Default.Search),
    PROFILE("You", Icons.Default.Person)
}

private enum class V22LibraryFilter(val label: String) {
    ALL("All"),
    FAVORITES("Favorites"),
    RECENT("Recent"),
    PATCHED("Patched"),
    HOMEBREW("Homebrew")
}

private data class OwnedGameGuide(
    val title: String,
    val edition: String,
    val platform: String,
    val action: String,
    val note: String,
    val artworkColor: Color,
    val supported: Boolean = true,
    val patchWorkflow: Boolean = false,
    val bundledPatch: Boolean = false
)

private val ownedGameGuides = listOf(
    OwnedGameGuide("Pokémon FireRed", "Kanto archive", "GBA", "Import owned backup", "Retra identifies the exact revision by checksum and can match compatible community cheat files.", Color(0xFF8A352D)),
    OwnedGameGuide("Pokémon LeafGreen", "Kanto archive", "GBA", "Import owned backup", "Use a legally obtained cartridge backup. No ROM is downloaded by Retra.", Color(0xFF286B4D)),
    OwnedGameGuide("Pokémon Emerald", "Hoenn archive", "GBA", "Import owned backup", "Compatible base for selected patch projects, including the reviewed Heart & Soul patch.", Color(0xFF087162)),
    OwnedGameGuide("Pokémon Ruby", "Hoenn archive", "GBA", "Import owned backup", "Checksum recognition keeps saves and cheats tied to the correct revision.", Color(0xFF8D3039)),
    OwnedGameGuide("Pokémon Sapphire", "Hoenn archive", "GBA", "Import owned backup", "Artwork is user-managed; game files remain local and content-addressed.", Color(0xFF235985)),
    OwnedGameGuide("Pokémon Radical Red", "Community patch", "GBA patch", "Open official patcher", "Retra does not redistribute a patched commercial ROM. Patch your own compatible FireRed backup.", Color(0xFF702D32), patchWorkflow = true),
    OwnedGameGuide("Pokémon Unbound", "Community patch", "GBA patch", "Import patch", "Apply a project-provided patch to your own compatible base backup.", Color(0xFF273C68), patchWorkflow = true),
    OwnedGameGuide("Pokémon AshGray", "Community patch", "GBA patch", "Import patch", "Retra validates IPS, UPS, and BPS files before applying them locally.", Color(0xFF6B4930), patchWorkflow = true),
    OwnedGameGuide("Pokémon Heart & Soul", "v1.2.1 reviewed patch", "GBA patch", "Prepare reviewed patch", "Patch credited to the Pokémon Heart & Soul development team. Requires your own compatible Emerald backup.", Color(0xFF315B58), patchWorkflow = true, bundledPatch = true),
    OwnedGameGuide("Pokémon Platinum", "Sinnoh archive", "Nintendo DS", "DS support planned", "Retra 2.2 does not pretend to emulate Nintendo DS games. This remains visible as a future-library guide only.", Color(0xFF4E5967), supported = false)
)

@Composable
fun RetraV22Root(viewModel: RetraViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()

    RetraTheme(settings) {
        CompositionLocalProvider(
            LocalRetraFeedback provides viewModel::emitFeedback,
            LocalRetraSettings provides settings
        ) {
            RetraBackdrop(settings) {
                when {
                    !settings.onboardingComplete -> V22Onboarding(viewModel)
                    activeGame != null -> PlayerScreen(
                        game = requireNotNull(activeGame),
                        viewModel = viewModel,
                        onExit = viewModel::closePlayer
                    )
                    else -> V22App(viewModel, settings)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V22App(viewModel: RetraViewModel, settings: AppSettings) {
    val games by viewModel.games.collectAsStateWithLifecycle()
    val selectedGame by viewModel.selectedGame.collectAsStateWithLifecycle()
    val pendingPatch by viewModel.pendingPatch.collectAsStateWithLifecycle()
    val compatibleBases by viewModel.compatibleBases.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val vaultRecords by viewModel.vaultRecords.collectAsStateWithLifecycle()
    val vaultHealth by viewModel.vaultHealth.collectAsStateWithLifecycle()
    val homebrew by viewModel.homebrewHub.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val externalImport by viewModel.externalImport.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var destination by rememberSaveable {
        mutableStateOf(
            when (settings.startupDestination) {
                StartupDestination.LIBRARY -> V22Destination.LIBRARY
                StartupDestination.CONTINUE_PLAYING, StartupDestination.HOME -> V22Destination.HOME
            }
        )
    }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }

    val importFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.importFile(it)
        }
    }
    val importFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.importFolder(it)
        }
    }
    var artworkTarget by remember { mutableStateOf<GameRecord?>(null) }
    val importArtwork = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = artworkTarget
        if (uri != null && target != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.importCoverArt(target, uri)
        }
        artworkTarget = null
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    if (settingsOpen) {
        V22SettingsScreen(
            settings = settings,
            viewModel = viewModel,
            onBack = { settingsOpen = false }
        )
    } else {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expanded = maxWidth >= 760.dp
            Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!expanded) {
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) {
                            V22Destination.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = destination == item,
                                    onClick = { destination = item },
                                    icon = { Icon(item.icon, null) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Row(Modifier.fillMaxSize().padding(padding)) {
                    if (expanded) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                            header = {
                                RetraLogoTile(Modifier.padding(vertical = 18.dp), size = 46.dp)
                            }
                        ) {
                            Spacer(Modifier.weight(1f))
                            V22Destination.entries.forEach { item ->
                                NavigationRailItem(
                                    selected = destination == item,
                                    onClick = { destination = item },
                                    icon = { Icon(item.icon, null) },
                                    label = { Text(item.label) }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                        }
                    }

                    when (destination) {
                        V22Destination.HOME -> V22HomeScreen(
                            games = games,
                            achievements = achievements,
                            vaultCount = vaultRecords.size,
                            onContinue = viewModel::launchGame,
                            onGame = viewModel::selectGame,
                            onImport = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
                            onOpenLibrary = { destination = V22Destination.LIBRARY },
                            onOpenSettings = { settingsOpen = true }
                        )
                        V22Destination.LIBRARY -> V22LibraryScreen(
                            games = games,
                            layout = settings.libraryLayout,
                            onLayout = viewModel::setLibraryLayout,
                            onGame = viewModel::selectGame,
                            onImportFile = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
                            onImportFolder = { importFolder.launch(null) }
                        )
                        V22Destination.DISCOVER -> V22DiscoverScreen(
                            homebrewEntries = homebrew.page.entries,
                            homebrewLoading = homebrew.loading,
                            installingSlug = homebrew.installingSlug,
                            onRefreshHomebrew = { viewModel.refreshHomebrewHub() },
                            onInstallHomebrew = viewModel::installHomebrew,
                            loadHomebrewArtwork = viewModel::loadHomebrewPreview,
                            onImportOwned = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
                            onPrepareHeartAndSoul = viewModel::prepareHeartAndSoulPatch,
                            onOpenUrl = { url ->
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                    .onFailure { scope.launch { snackbarHostState.showSnackbar("No browser could open that creator page.") } }
                            }
                        )
                        V22Destination.PROFILE -> V22ProfileScreen(
                            games = games,
                            achievements = achievements,
                            accountName = account?.displayName,
                            onSettings = { settingsOpen = true },
                            onGame = viewModel::selectGame
                        )
                    }
                }
            }
        }
    }

    selectedGame?.let { game ->
        V22GameDetailSheet(
            game = game,
            achievementCount = achievements.count { it.progress.unlockedAtEpochMillis != null },
            coreAvailable = viewModel.coreAvailable,
            onDismiss = { viewModel.selectGame(null) },
            onPlay = { viewModel.launchGame(game) },
            onFavorite = { viewModel.toggleFavorite(game) },
            onInstallCheats = { viewModel.installLibretroCheats(game) },
            onImportArtwork = {
                artworkTarget = game
                importArtwork.launch(arrayOf("image/png", "image/jpeg", "image/webp"))
            },
            onDelete = { viewModel.deleteGame(game) }
        )
    }

    pendingPatch?.let { patch ->
        V22PatchDialog(
            patch = patch,
            compatibleGames = compatibleBases,
            onApply = viewModel::applyPendingPatch,
            onDismiss = viewModel::dismissPendingPatch
        )
    }

    externalImport?.let { uri ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExternalImport,
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            title = { Text("Review external file") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(uri.lastPathSegment?.substringAfterLast('/') ?: "Shared file")
                    Text(
                        "Retra will copy this file into its private archive, validate its type and checksum, and then ask again before applying any patch. Nothing is executed directly from the share intent.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmExternalImport) { Text("Review and import") }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissExternalImport) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V22HomeScreen(
    games: List<GameRecord>,
    achievements: List<AchievementStatus>,
    vaultCount: Int,
    onContinue: (GameRecord) -> Unit,
    onGame: (GameRecord) -> Unit,
    onImport: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val recent = games.sortedByDescending { it.lastPlayedAtEpochMillis ?: it.importedAtEpochMillis }
    val continueGame = recent.firstOrNull { it.lastPlayedAtEpochMillis != null }
    val unlocked = achievements.count { it.progress.unlockedAtEpochMillis != null }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Your archive", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text(
                        "Resume quickly. Keep every save understandable.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings") }
            }
        }

        item {
            if (continueGame != null) {
                ContinueCard(game = continueGame, onPlay = { onContinue(continueGame) }, onDetails = { onGame(continueGame) })
            } else {
                GlassPanel(contentPadding = PaddingValues(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        RetraLogoTile(size = 54.dp)
                        Text("Start with one game you own", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Retra copies it into a content-addressed local library, verifies the file, and keeps patches, cheats, saves, and artwork attached to that exact ROM.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onImport) {
                            Icon(Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Import game or patch")
                        }
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 12.dp)) {
                item { MetricCard("Library", games.size.toString(), "verified items", Icons.Default.LibraryBooks, Modifier.width(156.dp)) }
                item { MetricCard("Vault", vaultCount.toString(), "save records", Icons.Default.Save, Modifier.width(156.dp)) }
                item { MetricCard("Goals", "$unlocked/${achievements.size}", "unlocked", Icons.Default.Star, Modifier.width(156.dp)) }
            }
        }

        if (recent.isNotEmpty()) {
            item { SectionHeader("Recently played", "Open library", onOpenLibrary) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 12.dp)) {
                    items(recent.take(8), key = { it.id }) { game ->
                        CompactGameCard(game, onClick = { onGame(game) })
                    }
                }
            }
        }

        item {
            GlassPanel(contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                        Icon(Icons.Default.Shield, null, Modifier.padding(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Save Health Center", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (vaultCount > 0) "$vaultCount save records indexed. Open a game to create or restore state slots."
                            else "No save records yet. Retra will index manual and automatic states as you play.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(game: GameRecord, onPlay: () -> Unit, onDetails: () -> Unit) {
    GlassPanel {
        Row(Modifier.fillMaxWidth().height(210.dp)) {
            GameArtwork(
                game = game,
                modifier = Modifier.width(142.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 23.dp, bottomStart = 23.dp)),
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("CONTINUE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(game.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, maxLines = 2)
                Text(
                    game.lastPlayedAtEpochMillis?.let { "Played ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))}" }
                        ?: "Ready to play",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPlay) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Resume")
                    }
                    FilledTonalButton(onClick = onDetails) { Text("Details") }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, helper: String, icon: ImageVector, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(helper, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V22LibraryScreen(
    games: List<GameRecord>,
    layout: LibraryLayout,
    onLayout: (LibraryLayout) -> Unit,
    onGame: (GameRecord) -> Unit,
    onImportFile: () -> Unit,
    onImportFolder: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(V22LibraryFilter.ALL) }
    val filtered = remember(games, query, filter) {
        games.filter { game ->
            val matchesQuery = query.isBlank() || listOf(game.title, game.canonicalTitle, game.gameCode, game.creator)
                .filterNotNull().any { it.contains(query, ignoreCase = true) }
            val matchesFilter = when (filter) {
                V22LibraryFilter.ALL -> true
                V22LibraryFilter.FAVORITES -> game.favorite
                V22LibraryFilter.RECENT -> game.lastPlayedAtEpochMillis != null
                V22LibraryFilter.PATCHED -> game.origin == "LOCAL_PATCH"
                V22LibraryFilter.HOMEBREW -> game.origin == "HOMEBREW_HUB" || game.origin.startsWith("LEGAL_CATALOG")
            }
            matchesQuery && matchesFilter
        }.sortedWith(compareByDescending<GameRecord> { it.favorite }.thenBy { it.title.lowercase() })
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Library", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text("${games.size} local ${if (games.size == 1) "item" else "items"}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onImportFolder) {
                Icon(Icons.Default.FolderOpen, null)
                Spacer(Modifier.width(6.dp))
                Text("Folder")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onImportFile) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Import")
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(120) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text("Search title, code, creator…") },
            singleLine = true
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(V22LibraryFilter.entries) { item ->
                FilterChip(selected = filter == item, onClick = { filter = item }, label = { Text(item.label) })
            }
            item {
                Spacer(Modifier.width(8.dp))
                LibraryLayout.entries.forEach { item ->
                    FilterChip(
                        selected = layout == item,
                        onClick = { onLayout(item) },
                        label = {
                            Text(
                                when (item) {
                                    LibraryLayout.LARGE_GRID -> "Large"
                                    LibraryLayout.COMPACT_GRID -> "Compact"
                                    LibraryLayout.DETAILED_LIST -> "List"
                                }
                            )
                        },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            LibraryEmptyState(
                hasAnyGames = games.isNotEmpty(),
                onImport = onImportFile,
                modifier = Modifier.weight(1f)
            )
        } else if (layout == LibraryLayout.DETAILED_LIST) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(filtered, key = { it.id }) { game -> DetailedGameRow(game, onClick = { onGame(game) }) }
            }
        } else {
            val minWidth = if (layout == LibraryLayout.LARGE_GRID) 170.dp else 130.dp
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minWidth),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered, key = { it.id }) { game ->
                    LibraryGameCard(game, compact = layout == LibraryLayout.COMPACT_GRID, onClick = { onGame(game) })
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(hasAnyGames: Boolean, onImport: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassPanel(modifier = Modifier.widthIn(max = 520.dp), contentPadding = PaddingValues(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RetraLogoTile(size = 62.dp)
                Text(if (hasAnyGames) "Nothing matches" else "A library, not a storefront", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    if (hasAnyGames) "Change the filter or search text."
                    else "Import games you own, creator-licensed homebrew, or patch files. Retra does not hide commercial ROM downloads behind a library card.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!hasAnyGames) Button(onClick = onImport) { Text("Import your first item") }
            }
        }
    }
}

@Composable
private fun LibraryGameCard(game: GameRecord, compact: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
    ) {
        GameArtwork(game, Modifier.fillMaxWidth().aspectRatio(if (compact) 1f else 0.78f))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(game.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(game.gameCode.ifBlank { "GBA" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (game.favorite) Icon(Icons.Default.Favorite, null, Modifier.size(15.dp), tint = MemoryCoral)
                if (game.origin == "LOCAL_PATCH") Text("PATCHED", style = MaterialTheme.typography.labelSmall, color = SaveMint)
                if (game.metadataSource != null) Icon(Icons.Default.Verified, "Metadata verified", Modifier.size(15.dp), tint = MemoryAqua)
            }
        }
    }
}

@Composable
private fun CompactGameCard(game: GameRecord, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.width(148.dp), shape = RoundedCornerShape(18.dp)) {
        GameArtwork(game, Modifier.fillMaxWidth().aspectRatio(0.82f))
        Text(game.title, Modifier.padding(11.dp), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailedGameRow(game: GameRecord, onClick: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), cornerRadius = 18.dp) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GameArtwork(game, Modifier.size(width = 72.dp, height = 88.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f)) {
                Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(game.canonicalTitle, game.gameCode.takeIf(String::isNotBlank), game.creator).joinToString(" · ").ifBlank { "GBA game" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            if (game.favorite) Icon(Icons.Default.Favorite, null, tint = MemoryCoral)
        }
    }
}

@Composable
private fun V22DiscoverScreen(
    homebrewEntries: List<HomebrewHubEntry>,
    homebrewLoading: Boolean,
    installingSlug: String?,
    onRefreshHomebrew: () -> Unit,
    onInstallHomebrew: (HomebrewHubEntry) -> Unit,
    loadHomebrewArtwork: suspend (HomebrewHubEntry) -> ByteArray?,
    onImportOwned: () -> Unit,
    onPrepareHeartAndSoul: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Discover", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                Text(
                    "Legal homebrew, official patch workflows, and guides for games already in your collection.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null)
                    Column(Modifier.weight(1f)) {
                        Text("Source-aware by design", fontWeight = FontWeight.Bold)
                        Text("A credit line does not grant redistribution rights. Commercial ROM mirrors are therefore not wired into Retra.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { SectionHeader("Your game guides") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 18.dp)) {
                items(ownedGameGuides) { guide ->
                    OwnedGameGuideCard(
                        guide = guide,
                        onAction = {
                            when {
                                guide.bundledPatch -> onPrepareHeartAndSoul()
                                guide.title == "Pokémon Radical Red" -> onOpenUrl("https://patch.radicalred.net/")
                                guide.supported -> onImportOwned()
                            }
                        }
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Playable homebrew", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Creator-published GBA files with usable license metadata.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = onRefreshHomebrew, enabled = !homebrewLoading) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (homebrewLoading) "Loading" else "Refresh")
                }
            }
        }

        if (homebrewEntries.isEmpty()) {
            item {
                GlassPanel(contentPadding = PaddingValues(18.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Homebrew Hub is ready when you are", fontWeight = FontWeight.Bold)
                        Text("Refresh to load current GBA homebrew. Network content is never represented as installed until its bytes have been downloaded, parsed, and hashed locally.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onRefreshHomebrew, enabled = !homebrewLoading) { Text("Load homebrew") }
                    }
                }
            }
        } else {
            items(homebrewEntries, key = { it.slug }) { entry ->
                HomebrewCard(
                    entry = entry,
                    installing = installingSlug == entry.slug,
                    onInstall = { onInstallHomebrew(entry) },
                    onSource = { onOpenUrl(entry.sourcePageUrl()) },
                    loadArtwork = loadHomebrewArtwork
                )
            }
        }

        item {
            GlassPanel(contentPadding = PaddingValues(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Artwork policy", fontWeight = FontWeight.Bold)
                    Text(
                        "Imported games use your own cover image or Retra's original cartridge-style placeholder. Retra does not silently scrape copyrighted box art from ROM sites.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnedGameGuideCard(guide: OwnedGameGuide, onAction: () -> Unit) {
    Card(
        modifier = Modifier.width(224.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f))
    ) {
        GuideArtwork(guide, Modifier.fillMaxWidth().height(156.dp))
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(guide.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
            Text("${guide.edition} · ${guide.platform}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(guide.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4, overflow = TextOverflow.Ellipsis)
            FilledTonalButton(onClick = onAction, enabled = guide.supported, modifier = Modifier.fillMaxWidth()) {
                Icon(if (guide.patchWorkflow) Icons.Default.Memory else Icons.Default.FolderOpen, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(guide.action, maxLines = 1)
            }
        }
    }
}

@Composable
private fun GuideArtwork(guide: OwnedGameGuide, modifier: Modifier = Modifier) {
    Box(modifier.background(guide.artworkColor), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(Color.White.copy(alpha = 0.08f), radius = w * 0.42f, center = Offset(w * 0.86f, h * 0.08f))
            drawCircle(Color.Black.copy(alpha = 0.12f), radius = w * 0.28f, center = Offset(w * 0.16f, h * 0.94f))
            val cartridge = Path().apply {
                moveTo(w * 0.22f, h * 0.18f)
                lineTo(w * 0.78f, h * 0.18f)
                lineTo(w * 0.84f, h * 0.32f)
                lineTo(w * 0.84f, h * 0.82f)
                lineTo(w * 0.16f, h * 0.82f)
                lineTo(w * 0.16f, h * 0.32f)
                close()
            }
            drawPath(cartridge, Color.White.copy(alpha = 0.12f))
            drawRoundRect(
                color = Color.White.copy(alpha = 0.16f),
                topLeft = Offset(w * 0.28f, h * 0.34f),
                size = androidx.compose.ui.geometry.Size(w * 0.44f, h * 0.29f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RetraLogo(size = 52.dp, markColor = Color.White.copy(alpha = 0.94f), cutoutColor = guide.artworkColor)
            Text(guide.platform, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun HomebrewCard(
    entry: HomebrewHubEntry,
    installing: Boolean,
    onInstall: () -> Unit,
    onSource: () -> Unit,
    loadArtwork: suspend (HomebrewHubEntry) -> ByteArray?
) {
    GlassPanel(cornerRadius = 20.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            HomebrewArtwork(entry = entry, loadArtwork = loadArtwork, modifier = Modifier.size(76.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${entry.developer} · ${entry.license}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(entry.tags.take(4).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onInstall, enabled = entry.directInstallEligible && !installing) {
                    Icon(Icons.Default.Download, null)
                    Spacer(Modifier.width(5.dp))
                    Text(if (installing) "Installing" else "Install")
                }
                IconButton(onClick = onSource) { Icon(Icons.Default.OpenInNew, "Creator page") }
            }
        }
    }
}

@Composable
private fun HomebrewArtwork(
    entry: HomebrewHubEntry,
    loadArtwork: suspend (HomebrewHubEntry) -> ByteArray?,
    modifier: Modifier = Modifier
) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = entry.slug) {
        val bytes = loadArtwork(entry)
        value = bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.clip(RoundedCornerShape(16.dp))
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "${entry.title} screenshot",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) { RetraLogo(size = 44.dp) }
        }
    }
}


@Composable
private fun V22ProfileScreen(
    games: List<GameRecord>,
    achievements: List<AchievementStatus>,
    accountName: String?,
    onSettings: () -> Unit,
    onGame: (GameRecord) -> Unit
) {
    val unlocked = achievements.filter { it.progress.unlockedAtEpochMillis != null }
    val favorites = games.filter { it.favorite }
    val totalPoints = unlocked.sumOf { it.definition.points }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) { RetraLogo(size = 38.dp) }
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(accountName ?: "Local player", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Your profile stays useful without an account.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledIconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings") }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 12.dp)) {
                item { MetricCard("Games", games.size.toString(), "in archive", Icons.Default.Gamepad, Modifier.width(156.dp)) }
                item { MetricCard("Achievements", unlocked.size.toString(), "$totalPoints points", Icons.Default.Star, Modifier.width(172.dp)) }
                item { MetricCard("Favorites", favorites.size.toString(), "quick access", Icons.Default.Favorite, Modifier.width(156.dp)) }
            }
        }

        item { SectionHeader("Achievements") }
        items(achievements, key = { it.definition.id }) { status -> AchievementRow(status) }

        if (favorites.isNotEmpty()) {
            item { SectionHeader("Favorites") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(favorites, key = { it.id }) { game -> CompactGameCard(game, onClick = { onGame(game) }) }
                }
            }
        }

        item {
            GlassPanel(contentPadding = PaddingValues(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("Retra 2.2", fontWeight = FontWeight.Bold)
                        Text("Designed and developed by Prashant Chataut", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(status: AchievementStatus) {
    val unlocked = status.progress.unlockedAtEpochMillis != null
    GlassPanel(cornerRadius = 18.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = CircleShape,
                color = if (unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (unlocked) Icons.Default.CheckCircle else Icons.Default.Lock, null)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (status.definition.hidden && !unlocked) "Hidden achievement" else status.definition.title,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${status.definition.points} pts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    if (status.definition.hidden && !unlocked) "Keep playing to reveal this goal." else status.definition.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(progress = { status.completionRatio }, modifier = Modifier.fillMaxWidth())
                if (status.definition.integrityPolicy != AchievementIntegrityPolicy.ANY) {
                    Text("Integrity: ${status.definition.integrityPolicy.name.lowercase().replace('_', ' ')}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V22GameDetailSheet(
    game: GameRecord,
    achievementCount: Int,
    coreAvailable: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onInstallCheats: () -> Unit,
    onImportArtwork: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GameArtwork(game, Modifier.width(132.dp).aspectRatio(0.78f).clip(RoundedCornerShape(18.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(game.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    game.canonicalTitle?.takeIf { it != game.title }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    MetadataPill(game.gameCode.ifBlank { "GBA" })
                    MetadataPill(game.origin.lowercase().replace('_', ' '))
                    if (game.metadataSource != null) MetadataPill("checksum matched")
                }
            }

            Button(onClick = onPlay, enabled = coreAvailable, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (coreAvailable) "Play" else "Gameplay core unavailable")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onFavorite, modifier = Modifier.weight(1f)) {
                    Icon(if (game.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (game.favorite) "Favorited" else "Favorite")
                }
                FilledTonalButton(onClick = onInstallCheats, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Code, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Match cheats")
                }
            }
            FilledTonalButton(onClick = onImportArtwork, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Palette, null)
                Spacer(Modifier.width(8.dp))
                Text(if (game.coverArtPath == null) "Choose cover artwork" else "Replace cover artwork")
            }

            DetailSection("File identity") {
                DetailLine("SHA-256", game.sha256.chunked(8).joinToString(" "))
                game.sha1?.let { DetailLine("SHA-1", it.chunked(8).joinToString(" ")) }
                game.crc32?.let { DetailLine("CRC-32", it.toString(16).uppercase().padStart(8, '0')) }
                DetailLine("Revision", game.softwareVersion.toString())
            }
            DetailSection("Progress") {
                DetailLine("Retra achievements unlocked", achievementCount.toString())
                DetailLine("Last played", game.lastPlayedAtEpochMillis?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Not played yet")
            }
            game.patchDisplayName?.let {
                DetailSection("Patch lineage") {
                    DetailLine("Patch", it)
                    DetailLine("Format", game.patchFormat ?: "Unknown")
                    DetailLine("Base hash", game.baseSha256 ?: "Unknown")
                }
            }
            game.creator?.let { creator ->
                DetailSection("Provenance") {
                    DetailLine("Creator", creator)
                    DetailLine("License", game.license ?: "Not recorded")
                    DetailLine("Source", game.sourceUrl ?: "Not recorded")
                }
            }

            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DeleteOutline, null)
                Spacer(Modifier.width(8.dp))
                Text("Remove from library")
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove ${game.title}?") },
            text = { Text("Retra removes its managed copy and library metadata. Save records remain separately protected in the vault.") },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    onDelete()
                    onDismiss()
                }) { Text("Remove") }
            },
            dismissButton = { FilledTonalButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun MetadataPill(label: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    GlassPanel(cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, Modifier.width(92.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun V22PatchDialog(
    patch: PendingPatch,
    compatibleGames: List<GameRecord>,
    onApply: (GameRecord) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose the base game") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(patch.displayName, fontWeight = FontWeight.Bold)
                Text(
                    "Retra verified the patch container. The patch is applied locally only after the selected base file matches its expected size and checksum.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (compatibleGames.isEmpty()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text(
                            "No compatible base is currently in your library. Import the legally obtained base backup first.",
                            Modifier.padding(12.dp)
                        )
                    }
                } else {
                    compatibleGames.forEach { game ->
                        FilledTonalButton(onClick = { onApply(game) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Gamepad, null)
                            Spacer(Modifier.width(8.dp))
                            Text(game.title, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { FilledTonalButton(onClick = onDismiss) { Text("Close") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V22SettingsScreen(settings: AppSettings, viewModel: RetraViewModel, onBack: () -> Unit) {
    var category by rememberSaveable { mutableStateOf("Appearance") }
    val categories = listOf("Appearance", "Library", "Player", "Progress", "Privacy", "About")

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            )
        }
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val expanded = maxWidth >= 720.dp
            if (expanded) {
                Row(Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.width(220.dp).fillMaxHeight().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categories) { item ->
                            Surface(
                                onClick = { category = item },
                                shape = RoundedCornerShape(14.dp),
                                color = if (category == item) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ) {
                                Text(item, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), fontWeight = if (category == item) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                    SettingsCategoryContent(category, settings, viewModel, Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categories) { item ->
                            FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
                        }
                    }
                    SettingsCategoryContent(category, settings, viewModel, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsCategoryContent(category: String, settings: AppSettings, viewModel: RetraViewModel, modifier: Modifier = Modifier) {
    val vaultRecords by viewModel.vaultRecords.collectAsStateWithLifecycle()
    val vaultHealth by viewModel.vaultHealth.collectAsStateWithLifecycle()
    val exportBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val importBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::importBackup) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(category, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        when (category) {
            "Appearance" -> {
                item {
                    SettingsCard("Theme", Icons.Default.Palette) {
                        ChoiceRow(ThemeMode.entries, settings.themeMode, { it.name.lowercase().replaceFirstChar(Char::titlecase) }, viewModel::setThemeMode)
                        ToggleSetting("Dynamic color", "Use the device color system when supported.", settings.dynamicColor, viewModel::setDynamicColor)
                        ToggleSetting("Reduce transparency", "Use opaque surfaces and remove glass depth.", settings.reduceTransparency, viewModel::setReduceTransparency)
                        ToggleSetting("Reduce motion", "Replace animated transitions with immediate changes.", settings.reduceMotion, viewModel::setReduceMotion)
                        SliderSettingV22("Glass depth", settings.glassIntensity, 0f..1f, viewModel::setGlassIntensity)
                        SliderSettingV22("Text scale", settings.fontScale, 0.85f..1.3f, viewModel::setFontScale)
                    }
                }
                item {
                    SettingsCard("Accent", Icons.Default.AutoAwesome) {
                        ChoiceRow(AccentPalette.entries, settings.accentPalette, {
                            when (it) {
                                AccentPalette.RETRA_INDIGO -> "Archive blue"
                                AccentPalette.GRAPHITE -> "Graphite"
                                AccentPalette.SOFT_VIOLET -> "Frost"
                                AccentPalette.CLASSIC_GRAY -> "Classic"
                            }
                        }, viewModel::setAccentPalette)
                        ToggleSetting("High contrast", "Strengthen text and panel boundaries.", settings.highContrast, viewModel::setHighContrast)
                    }
                }
            }
            "Library" -> {
                item {
                    SettingsCard("Library layout", Icons.Default.LibraryBooks) {
                        ChoiceRow(LibraryLayout.entries, settings.libraryLayout, {
                            when (it) {
                                LibraryLayout.LARGE_GRID -> "Large grid"
                                LibraryLayout.COMPACT_GRID -> "Compact"
                                LibraryLayout.DETAILED_LIST -> "List"
                            }
                        }, viewModel::setLibraryLayout)
                        ChoiceRow(ContentDensity.entries, settings.contentDensity, { it.name.lowercase().replaceFirstChar(Char::titlecase) }, viewModel::setContentDensity)
                        ChoiceRow(StartupDestination.entries, settings.startupDestination, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, viewModel::setStartupDestination)
                        ToggleSetting("Online recommendations", "Show legal homebrew and official patch sources.", settings.showOnlineRecommendations, viewModel::setShowOnlineRecommendations)
                        ToggleSetting("Statistics", "Show archive and progress summaries.", settings.showStatistics, viewModel::setShowStatistics)
                    }
                }
            }
            "Player" -> {
                item {
                    SettingsCard("Controls", Icons.Default.Gamepad) {
                        ChoiceRow(ControlLayoutPreset.entries, settings.controlLayoutPreset, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, viewModel::setControlLayoutPreset)
                        ChoiceRow(ControlVisualStyle.entries, settings.controlVisualStyle, { it.name.lowercase().replaceFirstChar(Char::titlecase) }, viewModel::setControlVisualStyle)
                        SliderSettingV22("Control size", settings.touchControlScale, 0.72f..1.35f, viewModel::setTouchControlScale)
                        SliderSettingV22("Control spacing", settings.touchControlSpacing, 0.72f..1.4f, viewModel::setTouchControlSpacing)
                        SliderSettingV22("Opacity", settings.touchControlOpacity, 0.25f..1f, viewModel::setTouchControlOpacity)
                        ToggleSetting("Touch controls", "Show on-screen controls.", settings.showTouchControls, viewModel::setShowTouchControls)
                        ToggleSetting("Shoulder buttons", "Keep L and R visible.", settings.showShoulderButtons, viewModel::setShowShoulderButtons)
                        ToggleSetting("Haptics", "Use short tactile confirmation for game buttons.", settings.hapticsEnabled, viewModel::setHapticsEnabled)
                        HorizontalDivider()
                        ControllerInputTester(viewModel)
                    }
                }
                item {
                    SettingsCard("Display and speed", Icons.Default.Speed) {
                        ChoiceRow(ScreenScalingMode.entries, settings.screenScalingMode, { it.name.lowercase().replaceFirstChar(Char::titlecase) }, viewModel::setScreenScalingMode)
                        ChoiceRow(PerformanceProfile.entries, settings.performanceProfile, { it.name.lowercase().replace('_', ' ').replaceFirstChar(Char::titlecase) }, viewModel::setPerformanceProfile)
                        SliderSettingV22("Fast-forward", settings.fastForwardSpeed, 1f..8f, viewModel::setFastForwardSpeed, suffix = "×")
                        ToggleSetting("Image smoothing", "Smooth scaled output rather than preserving hard pixel edges.", settings.displaySmoothing, viewModel::setDisplaySmoothing)
                        ToggleSetting("Performance overlay", "Show FPS and speed percentage.", settings.showPerformanceOverlay, viewModel::setShowPerformanceOverlay)
                        ToggleSetting("Immersive mode", "Hide system bars during play.", settings.playerImmersiveMode, viewModel::setPlayerImmersiveMode)
                    }
                }
            }
            "Progress" -> {
                item {
                    SettingsCard("Save Health Center", Icons.Default.Shield) {
                        Text(
                            if (vaultHealth.readableRecords == 0 && vaultHealth.corruptedRecords == 0) {
                                "No save records yet. Start a game and create a manual or automatic state."
                            } else {
                                "${vaultHealth.readableRecords} readable · ${vaultHealth.backupFiles} previous versions · ${vaultHealth.corruptedRecords} damaged" +
                                    (vaultHealth.latestWriteAtEpochMillis?.let { " · latest ${DateFormat.getDateTimeInstance().format(Date(it))}" } ?: "")
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        vaultRecords.take(5).forEach { record ->
                            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                                Row(Modifier.fillMaxWidth().padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${record.kind.name.lowercase().replace('_', ' ')} · slot ${record.slot}", fontWeight = FontWeight.Medium)
                                        Text(DateFormat.getDateTimeInstance().format(Date(record.createdAtEpochMillis)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("${record.sizeBytes / 1024} KiB", style = MaterialTheme.typography.labelSmall)
                                        if (record.backupCount > 0) {
                                            IconButton(onClick = { viewModel.restorePreviousVaultRecord(record) }, modifier = Modifier.size(32.dp)) {
                                                Icon(Icons.Default.Refresh, "Restore previous version", Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { exportBackup.launch("Retra-backup-${System.currentTimeMillis()}.retra.zip") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Export")
                            }
                            OutlinedButton(
                                onClick = { importBackup.launch(arrayOf("application/zip", "application/octet-stream")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FolderOpen, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Restore")
                            }
                        }
                        Text(
                            "Portable backups contain saves, settings, local achievements, artwork, and checksum metadata. ROM files are never included.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    SettingsCard("Save protection", Icons.Default.Save) {
                        ChoiceRow(listOf(0, 3, 5, 10, 15), settings.autoSaveIntervalMinutes, { if (it == 0) "Off" else "$it min" }, viewModel::setAutoSaveIntervalMinutes)
                        ToggleSetting("Quick save actions", "Show save and load controls in the player.", settings.quickSaveEnabled, viewModel::setQuickSaveEnabled)
                        ToggleSetting("Suspend in background", "Create a suspend state before the app leaves the foreground.", settings.autoSuspendOnBackground, viewModel::setAutoSuspendOnBackground)
                        ToggleSetting("Pause on headphone disconnect", "Avoid unexpected audio in public.", settings.pauseOnHeadphoneDisconnect, viewModel::setPauseOnHeadphoneDisconnect)
                    }
                }
                item {
                    SettingsCard("Audio", Icons.Default.Memory) {
                        ToggleSetting("Audio", "Enable emulator audio output.", settings.audioEnabled, viewModel::setAudioEnabled)
                        SliderSettingV22("Master volume", settings.masterVolume, 0f..1f, viewModel::setMasterVolume)
                    }
                }
            }
            "Privacy" -> {
                item {
                    SettingsCard("Privacy and alerts", Icons.Default.Security) {
                        Text("ROMs, patches, cheats, saves, and screenshots remain local unless you explicitly choose an Android share or export action.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ToggleSetting("Notifications", "Allow Retra notifications.", settings.notificationsEnabled, viewModel::setNotificationsEnabled)
                        ToggleSetting("Achievement alerts", "Notify when a local achievement unlocks.", settings.notifyAchievements, viewModel::setNotifyAchievements)
                        ToggleSetting("Download alerts", "Notify when a verified homebrew installation finishes.", settings.notifyDownloads, viewModel::setNotifyDownloads)
                    }
                }
            }
            "About" -> {
                item {
                    SettingsCard("Retra", Icons.Default.Info) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            RetraLogoTile(size = 58.dp)
                            Column {
                                Text("Retra 2.2.0", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text("Android Game Boy Advance archive and emulator")
                            }
                        }
                        HorizontalDivider()
                        DetailLine("Developer", "Prashant Chataut")
                        DetailLine("Design system", "Material 3 · Archive Glass")
                        DetailLine("Content model", "Owned backups, patches, and licensed homebrew")
                        DetailLine("Homebrew catalog", "Homebrew Hub · gbdev community")
                        Text(
                            "Homebrew remains credited to its original creator and license. Retra records source provenance with every installed release.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Retra is not affiliated with Nintendo, The Pokémon Company, Game Freak, or community patch projects. Project names are shown only to identify user-owned files and patch workflows.",
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
private fun SettingsCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    GlassPanel(cornerRadius = 20.dp, contentPadding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun ToggleSetting(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(onClick = { onChecked(!checked) }, shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun SliderSettingV22(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    suffix: String = "%"
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title)
            Text(if (suffix == "%") "${(value * 100).toInt()}%" else "${"%.1f".format(value)}$suffix", style = MaterialTheme.typography.labelLarge)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun <T> ChoiceRow(values: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values) { value ->
            FilterChip(selected = value == selected, onClick = { onSelected(value) }, label = { Text(label(value)) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun V22Onboarding(viewModel: RetraViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        OnboardingPage(
            title = "Your games. Your saves. One quiet archive.",
            body = "Retra is designed around returning to play, not feeds, streaks, or fake optimization switches.",
            icon = Icons.Default.Gamepad,
            accent = RetraBlue
        ),
        OnboardingPage(
            title = "Bring files you are allowed to use",
            body = "Import a cartridge backup, licensed homebrew, or an IPS, UPS, or BPS patch. Retra does not download commercial ROMs for you.",
            icon = Icons.Default.FolderOpen,
            accent = MemoryAqua
        ),
        OnboardingPage(
            title = "Make the player fit your hands",
            body = "Choose classic, compact, left-handed, or controller-first layouts. Tune size, spacing, opacity, screen scaling, and quick actions at any time.",
            icon = Icons.Default.Settings,
            accent = MemoryCoral
        ),
        OnboardingPage(
            title = "Progress should be recoverable",
            body = "Manual states, automatic states, screenshots, patch lineage, and checksum identity stay understandable instead of disappearing behind a cloud promise.",
            icon = Icons.Default.Shield,
            accent = SaveMint
        )
    )
    val current = pages[page]

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RetraLogoTile(size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Text("Retra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text("${page + 1} / ${pages.size}", style = MaterialTheme.typography.labelLarge)
            }

            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Surface(shape = RoundedCornerShape(30.dp), color = current.accent.copy(alpha = 0.18f), modifier = Modifier.size(112.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(current.icon, null, Modifier.size(52.dp), tint = current.accent) }
                }
                Text(current.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text(current.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (page == 1) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("Commercial ROM mirror integrations are deliberately excluded, even when a site offers attribution text.", Modifier.padding(14.dp))
                    }
                }
                if (page == 2) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("Classic", "Compact", "Left-handed", "Controller first")) { label ->
                            Text(
                                label,
                                Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(progress = { (page + 1f) / pages.size }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (page > 0) OutlinedButton(onClick = { page-- }, modifier = Modifier.weight(1f)) { Text("Back") }
                    Button(
                        onClick = { if (page == pages.lastIndex) viewModel.finishOnboarding() else page++ },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (page == pages.lastIndex) "Enter Retra" else "Continue") }
                }
            }
        }
    }
}

private data class OnboardingPage(val title: String, val body: String, val icon: ImageVector, val accent: Color)

private val SUPPORTED_IMPORT_MIME_TYPES = arrayOf(
    "application/octet-stream",
    "application/zip",
    "application/x-gba-rom",
    "*/*"
)
