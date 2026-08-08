package app.retra.emulator

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.model.AppSettings
import app.retra.core.model.GameRecord
import app.retra.core.model.StartupDestination
import app.retra.emulator.ui.theme.RetraTheme
import kotlinx.coroutines.launch

private enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.LibraryBooks),
    DISCOVER("Discover", Icons.Default.Search),
    PROFILE("You", Icons.Default.Person)
}

@Composable
fun RetraApp(viewModel: RetraViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeGame by viewModel.activeGame.collectAsStateWithLifecycle()

    RetraTheme(settings) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalRetraFeedback provides viewModel::emitFeedback,
            LocalRetraSettings provides settings
        ) {
            RetraBackdrop(settings) {
                when {
                    !settings.onboardingComplete -> RetraOnboarding(viewModel)
                    activeGame != null -> PlayerScreen(
                        game = requireNotNull(activeGame),
                        viewModel = viewModel,
                        onExit = viewModel::closePlayer
                    )
                    else -> RetraShell(viewModel, settings)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetraShell(viewModel: RetraViewModel, settings: AppSettings) {
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
    val snackbarHost = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var destination by rememberSaveable {
        mutableStateOf(
            when (settings.startupDestination) {
                StartupDestination.LIBRARY -> AppDestination.LIBRARY
                StartupDestination.CONTINUE_PLAYING, StartupDestination.HOME -> AppDestination.HOME
            }
        )
    }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var artworkTarget by remember { mutableStateOf<GameRecord?>(null) }

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
    val importArtwork = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = artworkTarget
        if (uri != null && target != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.importCoverArt(target, uri)
        }
        artworkTarget = null
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHost.showSnackbar(it) }
    }

    if (settingsOpen) {
        RetraSettingsScreen(settings, viewModel) { settingsOpen = false }
        return
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = maxWidth >= 760.dp
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                if (!expanded) RetraBottomDock(destination) { destination = it }
            }
        ) { scaffoldPadding ->
            Row(Modifier.fillMaxSize().padding(scaffoldPadding)) {
                if (expanded) RetraRail(destination) { destination = it }
                when (destination) {
                    AppDestination.HOME -> RetraHome(
                        games = games,
                        achievements = achievements,
                        vaultCount = vaultRecords.size,
                        showStatistics = settings.showStatistics,
                        coreReady = viewModel.coreAvailable,
                        coreStatus = viewModel.coreStatus,
                        onContinue = viewModel::launchGame,
                        onGame = viewModel::selectGame,
                        onImport = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
                        onLibrary = { destination = AppDestination.LIBRARY },
                        onPatchStudio = viewModel::prepareHeartAndSoulPatch,
                        onSettings = { settingsOpen = true }
                    )
                    AppDestination.LIBRARY -> RetraLibrary(
                        games = games,
                        layout = settings.libraryLayout,
                        onLayout = viewModel::setLibraryLayout,
                        onGame = viewModel::selectGame,
                        onImport = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
                        onFolder = { importFolder.launch(null) },
                        onInstallDemo = viewModel::installBundledDemo
                    )
                    AppDestination.DISCOVER -> RetraDiscover(
                        onlineEnabled = settings.showOnlineRecommendations,
                        entries = homebrew.page.entries,
                        loading = homebrew.loading,
                        installingSlug = homebrew.installingSlug,
                        patchGuides = viewModel.catalogRepository.curatedLinks.filter {
                            "featured-patch" in it.tags
                        },
                        onRefresh = { viewModel.refreshHomebrewHub() },
                        onInstall = viewModel::installHomebrew,
                        loadArtwork = viewModel::loadHomebrewPreview,
                        onPatchStudio = viewModel::prepareHeartAndSoulPatch,
                        onImport = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
                        onOpenUrl = { url ->
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                                .onFailure { scope.launch { snackbarHost.showSnackbar("No browser could open the creator page.") } }
                        }
                    )
                    AppDestination.PROFILE -> RetraProfile(
                        games = games,
                        achievements = achievements,
                        accountName = account?.displayName,
                        saveCount = vaultRecords.size,
                        corruptedSaves = vaultHealth.corruptedRecords,
                        onSettings = { settingsOpen = true },
                        onGame = viewModel::selectGame
                    )
                }
            }
        }
    }

    selectedGame?.let { game ->
        RetraGameSheet(
            game = game,
            coreReady = viewModel.coreAvailable,
            coreStatus = viewModel.coreStatus,
            onDismiss = { viewModel.selectGame(null) },
            onPlay = { viewModel.launchGame(game) },
            onFavorite = { viewModel.toggleFavorite(game) },
            onCheats = { viewModel.installLibretroCheats(game) },
            onArtwork = {
                artworkTarget = game
                importArtwork.launch(arrayOf("image/png", "image/jpeg", "image/webp"))
            },
            onDelete = { viewModel.deleteGame(game) }
        )
    }

    pendingPatch?.let { patch ->
        RetraPatchDialog(
            patch = patch,
            compatibleGames = compatibleBases,
            onImportBase = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
            onApply = viewModel::applyPendingPatch,
            onDismiss = viewModel::dismissPendingPatch
        )
    }

    externalImport?.let { uri ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExternalImport,
            icon = { Icon(Icons.Default.Security, null) },
            title = { Text("Inspect before importing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(uri.lastPathSegment?.substringAfterLast('/') ?: "Shared file", fontWeight = FontWeight.Bold)
                    Text(
                        "Retra copies the file into private storage, validates its format and checksum, and never runs a patch directly from a share intent.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { Button(onClick = viewModel::confirmExternalImport) { Text("Inspect file") } },
            dismissButton = { TextButton(onClick = viewModel::dismissExternalImport) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RetraBottomDock(selected: AppDestination, onSelected: (AppDestination) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AppDestination.entries.forEach { item ->
                val active = item == selected
                Surface(
                    onClick = { onSelected(item) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f) else Color.Transparent,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(
                        Modifier.widthIn(min = 68.dp).padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(item.icon, item.label, Modifier.size(22.dp))
                        Text(item.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RetraRail(selected: AppDestination, onSelected: (AppDestination) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxHeight().width(104.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            Modifier.fillMaxHeight().padding(vertical = 18.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RetraLogoTile(size = 50.dp)
            Spacer(Modifier.weight(1f))
            AppDestination.entries.forEach { item ->
                val active = item == selected
                Surface(
                    onClick = { onSelected(item) },
                    modifier = Modifier.padding(vertical = 5.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f) else Color.Transparent,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(
                        Modifier.width(78.dp).padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(item.icon, item.label)
                        Text(item.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

private val SUPPORTED_IMPORT_MIME_TYPES = arrayOf(
    "application/octet-stream",
    "application/zip",
    "application/x-gba-rom",
    "*/*"
)
