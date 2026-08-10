package app.retra.emulator

import android.content.Intent
import android.net.Uri
import app.retra.emulator.auth.AuthOperation
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.model.AppSettings
import app.retra.core.model.GameRecord
import app.retra.core.model.StartupDestination
import app.retra.emulator.data.ImportReport
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.NightPlum
import app.retra.emulator.ui.theme.RaspberryPink
import app.retra.emulator.ui.theme.RetraTheme
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SurfaceMidnight
import kotlinx.coroutines.launch

private enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    LIBRARY("Library", Icons.Default.LibraryBooks),
    PROFILE("Profile", Icons.Default.Person)
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
    val account by viewModel.account.collectAsStateWithLifecycle()
    val authOperation by viewModel.authOperation.collectAsStateWithLifecycle()
    val externalImport by viewModel.externalImport.collectAsStateWithLifecycle()
    val importReport by viewModel.importReport.collectAsStateWithLifecycle()
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
    var addSheetOpen by rememberSaveable { mutableStateOf(false) }
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

        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                if (expanded) {
                    RetraRail(
                        selected = destination,
                        onSelected = { destination = it },
                        onAdd = { addSheetOpen = true }
                    )
                }

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (destination) {
                        AppDestination.HOME -> RetraHome(
                            games = games,
                            achievements = achievements,
                            vaultCount = vaultRecords.size,
                            showStatistics = settings.showStatistics,
                            coreReady = viewModel.coreAvailable,
                            coreStatus = viewModel.coreStatus,
                            accountName = account?.displayName,
                            onContinue = viewModel::launchGame,
                            onGame = viewModel::selectGame,
                            onImport = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
                            onLibrary = { destination = AppDestination.LIBRARY },
                            onPatchStudio = viewModel::prepareHeartAndSoulPatch,
                            onProfile = { destination = AppDestination.PROFILE },
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
                        AppDestination.PROFILE -> RetraProfile(
                            games = games,
                            achievements = achievements,
                            accountName = account?.displayName,
                            saveCount = vaultRecords.size,
                            corruptedSaves = vaultHealth.corruptedRecords,
                            authOperation = authOperation,
                            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                            onGoogleSignOut = { viewModel.signOutGoogle(context) },
                            onSettings = { settingsOpen = true },
                            onGame = viewModel::selectGame
                        )
                    }
                }
            }

            // Floating Navigation Dock on Phones
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RetraFloatingDock(
                        selected = destination,
                        onSelected = {
                            viewModel.emitFeedback(FeedbackCue.TAP)
                            destination = it
                        },
                        onAdd = {
                            viewModel.emitFeedback(FeedbackCue.CONFIRM)
                            addSheetOpen = true
                        }
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (expanded) 24.dp else 90.dp)
            )
        }
    }

    // Add Action Sheet
    if (addSheetOpen) {
        RetraAddSheet(
            onDismiss = { addSheetOpen = false },
            onImportFile = {
                addSheetOpen = false
                importFile.launch(SUPPORTED_IMPORT_MIME_TYPES)
            },
            onImportFolder = {
                addSheetOpen = false
                importFolder.launch(null)
            },
            onPatchStudio = {
                addSheetOpen = false
                viewModel.prepareHeartAndSoulPatch()
            },
            onInstallDemo = {
                addSheetOpen = false
                viewModel.installBundledDemo()
            }
        )
    }

    // Game Details Sheet
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

    // Patch Dialog
    pendingPatch?.let { patch ->
        RetraPatchDialog(
            patch = patch,
            compatibleGames = compatibleBases,
            onImportBase = { importFile.launch(SUPPORTED_IMPORT_MIME_TYPES) },
            onApply = viewModel::applyPendingPatch,
            onDismiss = viewModel::dismissPendingPatch
        )
    }

    // External Intent Import Dialog
    externalImport?.let { uri ->
        AlertDialog(
            onDismissRequest = viewModel::dismissExternalImport,
            icon = { Icon(Icons.Default.Security, null, tint = SaveMint) },
            title = { Text("Inspect before importing", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

    // Import Report Dialog
    importReport?.let { report ->
        AlertDialog(
            onDismissRequest = viewModel::dismissImportReport,
            icon = {
                RetraMascot(
                    size = 54.dp,
                    state = if (report.rejectedReasons.isEmpty()) MascotState.SUCCESS else MascotState.ERROR,
                    interactive = false
                )
            },
            title = { Text(report.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(report.summary, style = MaterialTheme.typography.bodyMedium)
                    if (report.rejectedReasons.isNotEmpty()) {
                        Text("Details:", fontWeight = FontWeight.SemiBold)
                        for (reason in report.rejectedReasons.take(5)) {
                            Text("• $reason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (report.pendingPatches.isNotEmpty()) {
                        Text("Queued Patches:", fontWeight = FontWeight.SemiBold)
                        for (patch in report.pendingPatches.take(3)) {
                            Text("• ${patch.displayName} (requires matching base ROM)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = viewModel::dismissImportReport) { Text("OK") } }
        )
    }
}

/**
 * Floating Liquid-Glass Navigation Dock.
 */
@Composable
private fun RetraFloatingDock(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit,
    onAdd: () -> Unit
) {
    RetraGlassSurface(
        tier = GlassTier.STRONG,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.widthIn(max = 380.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home Destination
            DockItem(
                destination = AppDestination.HOME,
                selected = selected == AppDestination.HOME,
                onClick = { onSelected(AppDestination.HOME) }
            )

            // Library Destination
            DockItem(
                destination = AppDestination.LIBRARY,
                selected = selected == AppDestination.LIBRARY,
                onClick = { onSelected(AppDestination.LIBRARY) }
            )

            // Elevated Center Add Action (+)
            Surface(
                onClick = onAdd,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 4.dp).size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(24.dp))
                }
            }

            // Profile Destination
            DockItem(
                destination = AppDestination.PROFILE,
                selected = selected == AppDestination.PROFILE,
                onClick = { onSelected(AppDestination.PROFILE) }
            )
        }
    }
}

@Composable
private fun DockItem(
    destination: AppDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(destination.icon, destination.label, modifier = Modifier.size(20.dp))
            if (selected) {
                Text(
                    destination.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Adaptive Rail Navigation for Tablets / Landscape.
 */
@Composable
private fun RetraRail(
    selected: AppDestination,
    onSelected: (AppDestination) -> Unit,
    onAdd: () -> Unit
) {
    RetraGlassSurface(
        modifier = Modifier.fillMaxHeight().width(96.dp),
        tier = GlassTier.STRONG,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            Modifier
                .fillMaxHeight()
                .padding(vertical = 20.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RetraLogoTile(size = 48.dp)
            Spacer(Modifier.height(24.dp))

            Surface(
                onClick = onAdd,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, "Add", modifier = Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            for (item in AppDestination.entries) {
                val active = item == selected
                Surface(
                    onClick = { onSelected(item) },
                    modifier = Modifier.padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.80f) else Color.Transparent,
                    contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Column(
                        Modifier.width(72.dp).padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(item.icon, item.label, modifier = Modifier.size(22.dp))
                        Text(item.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

/**
 * Liquid-Glass Add Action Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetraAddSheet(
    onDismiss: () -> Unit,
    onImportFile: () -> Unit,
    onImportFolder: () -> Unit,
    onPatchStudio: () -> Unit,
    onInstallDemo: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        RetraPanel(
            modifier = Modifier.fillMaxWidth(),
            tier = GlassTier.STRONG,
            shape = MaterialTheme.shapes.extraLarge,
            contentPadding = PaddingValues(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RetraMascot(size = 36.dp, state = MascotState.IDLE, interactive = false)
                        Text(
                            "Add to Archive",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AddActionTile(
                        icon = Icons.Default.SportsEsports,
                        title = "Import Game File",
                        subtitle = "Select a local .gba or .zip backup archive",
                        accentColor = ElectricLilac,
                        onClick = onImportFile
                    )
                    AddActionTile(
                        icon = Icons.Default.FolderOpen,
                        title = "Scan Folder",
                        subtitle = "Scan directory for all valid GBA game files",
                        accentColor = SaveMint,
                        onClick = onImportFolder
                    )
                    AddActionTile(
                        icon = Icons.Default.AutoAwesome,
                        title = "Apply Patch / Patch Studio",
                        subtitle = "Pair UPS / IPS / BPS patch with local base ROM",
                        accentColor = MemoryCoral,
                        onClick = onPatchStudio
                    )
                    AddActionTile(
                        icon = Icons.Default.Gamepad,
                        title = "Play Retra Drift Demo",
                        subtitle = "Install built-in open-source GBA homebrew",
                        accentColor = AdventureGold,
                        onClick = onInstallDemo
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AddActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = SurfaceMidnight.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.50f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
            ) {
                Icon(icon, null, Modifier.padding(10.dp).size(22.dp), tint = accentColor)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val SUPPORTED_IMPORT_MIME_TYPES = arrayOf(
    "application/octet-stream",
    "application/zip",
    "application/x-gba-rom",
    "*/*"
)
