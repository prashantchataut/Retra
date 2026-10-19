package app.retra.ui.nav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.retra.AppContainer
import app.retra.data.AppMessage
import app.retra.data.IncomingFile
import app.retra.data.MessageTone
import app.retra.ui.AppActions
import app.retra.ui.LocalAppActions
import app.retra.ui.LocalDockPadding
import app.retra.ui.Tab
import app.retra.ui.components.AmbientSky
import app.retra.ui.components.Dock
import app.retra.ui.components.DockItem
import app.retra.ui.components.LocalHazeState
import app.retra.ui.components.LocalNavAnimatedScope
import app.retra.ui.components.LocalSharedScope
import app.retra.ui.components.ToastHost
import app.retra.ui.games.GameDetailsScreen
import app.retra.ui.games.MyGamesScreen
import app.retra.ui.home.HomeScreen
import app.retra.ui.onboarding.OnboardingScreen
import app.retra.ui.patcher.PatcherScreen
import app.retra.ui.player.PlayerScreen
import app.retra.ui.settings.SettingsScreen
import app.retra.ui.store.StoreScreen
import app.retra.ui.theme.Motion
import app.retra.ui.theme.RetraTheme
import app.retra.ui.theme.isDarkTheme
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object GamesRoute
@Serializable data class StoreRoute(val entryId: String? = null)
@Serializable data class PatcherRoute(val baseGameId: String? = null, val patchPath: String? = null, val patchName: String? = null)
@Serializable data object SettingsRoute
@Serializable data class DetailsRoute(val gameId: String, val origin: String)
@Serializable data class PlayerRoute(val gameId: String, val slot: Int, val origin: String)

private val dockItems = listOf(
    DockItem("Home", Icons.Outlined.Home, Icons.Rounded.Home),
    DockItem("My Games", Icons.Outlined.SportsEsports, Icons.Rounded.SportsEsports),
    DockItem("Library", Icons.Outlined.Storefront, Icons.Rounded.Storefront),
    DockItem("Patcher", Icons.Outlined.AutoFixHigh, Icons.Rounded.AutoFixHigh),
    DockItem("Settings", Icons.Outlined.Settings, Icons.Rounded.Settings),
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RetraRoot(container: AppContainer) {
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val dark = isDarkTheme(settings.theme)
    RetraTheme(dark = dark, reduceMotion = settings.reduceMotion) {
        val nav = rememberNavController()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haze = rememberHazeState()
        var message by remember { mutableStateOf<AppMessage?>(null) }
        LaunchedEffect(Unit) { container.messages.flow.collect { message = it } }

        val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) scope.launchImport(container, uris, nav)
        }
        val actions = remember(nav) {
            object : AppActions {
                override fun importGames() = importLauncher.launch(arrayOf("*/*"))
                override fun openGame(id: String, origin: String) = nav.navigate(DetailsRoute(id, origin)) { launchSingleTop = true }
                override fun playSlot(id: String, slot: Int, origin: String) = nav.navigate(PlayerRoute(id, slot, origin)) { launchSingleTop = true }
                override fun openStore(entryId: String?) = nav.navigateTab(StoreRoute(entryId))
                override fun openPatcher(baseGameId: String?) = nav.navigateTab(PatcherRoute(baseGameId))
                override fun openTab(tab: Tab) = nav.navigateTab(tab.route())
                override fun openUrl(url: String) = openUrl(context, url)
                override fun back() { nav.popBackStack() }
            }
        }

        // Files shared with Retra from other apps.
        val incoming by container.incoming.pending.collectAsStateWithLifecycle()
        LaunchedEffect(incoming) {
            when (val f = incoming) {
                is IncomingFile.Patch -> nav.navigateTab(PatcherRoute(patchPath = f.path, patchName = f.name))
                is IncomingFile.Rom -> nav.navigate(DetailsRoute(f.gameId, "incoming"))
                null -> Unit
            }
            if (incoming != null) container.incoming.consume()
        }

        val entry by nav.currentBackStackEntryAsState()
        val dest = entry?.destination
        val tabIndex = when {
            dest == null -> 0
            dest.hasRoute<HomeRoute>() -> 0
            dest.hasRoute<GamesRoute>() -> 1
            dest.hasRoute<StoreRoute>() -> 2
            dest.hasRoute<PatcherRoute>() -> 3
            dest.hasRoute<SettingsRoute>() -> 4
            else -> -1
        }
        val showDock = tabIndex >= 0 && settings.onboardingDone
        val navBottom = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }
        val dockPadding = PaddingValues(bottom = navBottom + 68.dp + 32.dp)
        val reduce = settings.reduceMotion

        CompositionLocalProvider(LocalAppActions provides actions, LocalDockPadding provides dockPadding) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().hazeSource(haze)) {
                    AmbientSky()
                    SharedTransitionLayout {
                        CompositionLocalProvider(LocalSharedScope provides this) {
                            NavHost(
                                navController = nav,
                                startDestination = HomeRoute,
                                enterTransition = { if (reduce) EnterTransition.None else fadeIn(tween(220, 60)) + scaleIn(spring(Motion.DAMPING, Motion.SMOOTH), initialScale = 0.98f) },
                                exitTransition = { if (reduce) ExitTransition.None else fadeOut(tween(160)) },
                                popEnterTransition = { if (reduce) EnterTransition.None else fadeIn(tween(220, 60)) },
                                popExitTransition = { if (reduce) ExitTransition.None else fadeOut(tween(160)) + scaleOut(tween(220), targetScale = 0.98f) },
                            ) {
                                composable<HomeRoute> { Scoped { HomeScreen(container) } }
                                composable<GamesRoute> { Scoped { MyGamesScreen(container) } }
                                composable<StoreRoute> { e -> Scoped { StoreScreen(container, e.toRoute<StoreRoute>().entryId) } }
                                composable<PatcherRoute> { e ->
                                    val r = e.toRoute<PatcherRoute>()
                                    Scoped { PatcherScreen(container, r.baseGameId, r.patchPath, r.patchName) }
                                }
                                composable<SettingsRoute> { Scoped { SettingsScreen(container) } }
                                composable<DetailsRoute> { e ->
                                    val r = e.toRoute<DetailsRoute>()
                                    Scoped { GameDetailsScreen(container, r.gameId, r.origin) }
                                }
                                composable<PlayerRoute> { e ->
                                    val r = e.toRoute<PlayerRoute>()
                                    Scoped { PlayerScreen(container, r.gameId, r.slot, r.origin) }
                                }
                            }
                        }
                    }
                }
                CompositionLocalProvider(LocalHazeState provides haze) {
                    AnimatedVisibility(
                        showDock,
                        modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.navigationBars).padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        enter = slideInVertically(spring(Motion.DAMPING, Motion.SMOOTH)) { it * 2 } + fadeIn(),
                        exit = slideOutVertically(spring(1f, Motion.SMOOTH)) { it * 2 } + fadeOut(),
                    ) {
                        Dock(dockItems, tabIndex.coerceAtLeast(0), onSelect = { i -> actions.openTab(Tab.entries[i]) })
                    }
                    ToastHost(message) { message = null }
                }
                AnimatedVisibility(!settings.onboardingDone, enter = fadeIn(), exit = fadeOut(tween(300))) {
                    OnboardingScreen(
                        onFinish = { container.settings.update { it.copy(onboardingDone = true) } },
                        onImport = { actions.importGames() },
                        onExplore = { actions.openStore() },
                    )
                }
            }
        }
    }
}

/** Provides the destination's AnimatedVisibilityScope for shared-element transitions. */
@Composable
private fun androidx.compose.animation.AnimatedContentScope.Scoped(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavAnimatedScope provides this, content = content)
}

private fun Tab.route(): Any = when (this) {
    Tab.HOME -> HomeRoute
    Tab.GAMES -> GamesRoute
    Tab.STORE -> StoreRoute()
    Tab.PATCHER -> PatcherRoute()
    Tab.SETTINGS -> SettingsRoute
}

private fun NavHostController.navigateTab(route: Any) {
    navigate(route) {
        popUpTo(HomeRoute) { saveState = route !is StoreRoute || (route.entryId == null) }
        launchSingleTop = true
        restoreState = (route as? StoreRoute)?.entryId == null && (route as? PatcherRoute)?.let { it.baseGameId == null && it.patchPath == null } != false
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchImport(container: AppContainer, uris: List<Uri>, nav: NavHostController) {
    kotlinx.coroutines.launch(this.coroutineContext) {
        val outcome = container.importer.importUris(uris)
        val patch = outcome.patch
        when {
            patch != null -> nav.navigateTab(PatcherRoute(patchPath = patch.path, patchName = patch.name))
            outcome.added.size == 1 -> nav.navigate(DetailsRoute(outcome.added[0].id, "import"))
            outcome.added.size > 1 -> nav.navigateTab(GamesRoute)
        }
    }.let { }
}

fun openUrl(context: Context, url: String) {
    if (!url.startsWith("https://")) return
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(url))
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: ActivityNotFoundException) {
            (context.applicationContext as? app.retra.RetraApp)?.container?.messages?.show("No browser found to open $url", MessageTone.ERROR)
        }
    }
}
