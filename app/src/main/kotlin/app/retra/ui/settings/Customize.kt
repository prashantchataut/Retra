package app.retra.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.AppContainer
import app.retra.core.GamepadBindings
import app.retra.core.OpenRouterCheats
import app.retra.core.PadAction
import app.retra.data.AppPalette
import app.retra.data.ControlSkin
import app.retra.data.DriveAuth
import app.retra.data.DriveStatus
import app.retra.data.MessageTone
import app.retra.data.Settings
import app.retra.emu.InputRouter
import app.retra.link.RoomCode
import app.retra.link.RoomPhase
import app.retra.ui.LocalAppActions
import app.retra.ui.components.ButtonStyle
import app.retra.ui.components.Divider
import app.retra.ui.components.GlassGroup
import app.retra.ui.components.GlassSheet
import app.retra.ui.components.GlassSurface
import app.retra.ui.components.InfoRow
import app.retra.ui.components.NavRow
import app.retra.ui.components.RetraButton
import app.retra.ui.components.RetraChip
import app.retra.ui.components.RetraTextField
import app.retra.ui.components.Segmented
import app.retra.ui.components.SwitchRow
import app.retra.ui.components.Tag
import app.retra.ui.player.SkinConfig
import app.retra.ui.player.SkinPreview
import app.retra.ui.theme.Motion
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import app.retra.ui.theme.paletteColors
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- palette

/** A grid of colour worlds; each card previews its ground and accent in the current light/dark mode. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalettePicker(selected: AppPalette, onSelect: (AppPalette) -> Unit, modifier: Modifier = Modifier) {
    val c = Retra.colors
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(Space.xs), verticalArrangement = Arrangement.spacedBy(Space.xs), maxItemsInEachRow = 3) {
        AppPalette.entries.forEach { p ->
            val look = paletteColors(p, c.isDark)
            val isSel = p == selected
            val ring by animateColorAsState(if (isSel) c.accent else c.divider, Motion.smooth(Retra.reduceMotion), label = "ring")
            val lift by animateDpAsState(if (isSel) 0.dp else 2.dp, Motion.smooth(Retra.reduceMotion), label = "lift")
            Column(
                Modifier
                    .width(104.dp)
                    .padding(top = lift)
                    .clip(Radius.mediumShape)
                    .background(look.sky)
                    .border(BorderStroke(if (isSel) 2.dp else 1.dp, ring), Radius.mediumShape)
                    .clickable { onSelect(p) }
                    .semantics { contentDescription = "${p.label} palette${if (isSel) ", selected" else ""}" }
                    .padding(Space.xs),
            ) {
                Box(Modifier.fillMaxWidth().height(44.dp).clip(Radius.smallShape).background(look.skyGlowA)) {
                    Box(Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).size(28.dp).clip(CircleShape).background(look.accent))
                    Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp).size(14.dp).clip(CircleShape).background(look.brand))
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(p.label, style = RetraType.caption, color = look.ink, modifier = Modifier.weight(1f), maxLines = 1)
                    if (isSel) Icon(Icons.Rounded.Check, null, tint = look.accent, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- button skins

private val tintChoices = listOf(0xFF17302A, 0xFFE8628F, 0xFF7C5CFF, 0xFFFF8A3D, 0xFF1FA67A, 0xFF2D9CDB, 0xFFF2C94C)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinPicker(settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, modifier: Modifier = Modifier) {
    val c = Retra.colors
    val cfg = SkinConfig(settings.controlSkin, settings.controlTint.takeIf { it != 0L }?.let { Color(it.toInt()) }, settings.controlLabels)
    Column(modifier) {
        GlassSurface(Modifier.fillMaxWidth(), shape = Radius.mediumShape, strong = true) {
            SkinPreview(cfg, Modifier.fillMaxWidth().padding(vertical = Space.s, horizontal = Space.xs))
        }
        Spacer(Modifier.height(Space.xs))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            ControlSkin.entries.forEach { s -> RetraChip(s.label, s == settings.controlSkin, { onUpdate { it.copy(controlSkin = s) } }) }
        }
        Spacer(Modifier.height(Space.xs))
        Text("Pressed colour", style = RetraType.label, color = c.inkMuted)
        Spacer(Modifier.height(Space.xxs))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).border(BorderStroke(if (settings.controlTint == 0L) 2.dp else 1.dp, if (settings.controlTint == 0L) c.accent else c.divider), CircleShape)
                    .background(c.accent).clickable { onUpdate { it.copy(controlTint = 0) } }
                    .semantics { contentDescription = "Follow the palette" },
                contentAlignment = Alignment.Center,
            ) { Text("A", style = RetraType.caption, color = c.onAccent) }
            tintChoices.forEach { v ->
                val sel = settings.controlTint == v
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(Color(v.toInt()))
                        .border(BorderStroke(if (sel) 3.dp else 1.dp, if (sel) c.ink else c.divider), CircleShape)
                        .clickable { onUpdate { it.copy(controlTint = v) } }
                        .semantics { contentDescription = "Custom colour${if (sel) ", selected" else ""}" },
                )
            }
        }
        Spacer(Modifier.height(Space.xs))
        SwitchRow("Button labels", settings.controlLabels, { v -> onUpdate { it.copy(controlLabels = v) } }, subtitle = "Show A, B, L, R on the buttons")
    }
}

// ---------------------------------------------------------------- gamepad mapping

@Composable
fun GamepadMappingSheet(visible: Boolean, settings: Settings, onUpdate: ((Settings) -> Settings) -> Unit, onDismiss: () -> Unit) {
    val c = Retra.colors
    val map = GamepadBindings.resolve(settings.padBindings)
    var capturing by remember { mutableStateOf<PadAction?>(null) }
    GlassSheet(visible, { capturing = null; onDismiss() }, title = "Gamepad mapping") {
        // While a row is waiting, the next controller button press is the binding.
        val target = capturing
        DisposableEffect(target) {
            if (target == null) return@DisposableEffect onDispose {}
            val handler = object : InputRouter.Handler {
                override fun onKeyEvent(event: android.view.KeyEvent): Boolean {
                    if (event.action != android.view.KeyEvent.ACTION_DOWN) return true
                    if (event.keyCode == android.view.KeyEvent.KEYCODE_BACK) { capturing = null; return true }
                    val next = GamepadBindings.bind(GamepadBindings.resolve(settings.padBindings), event.keyCode, target)
                    onUpdate { it.copy(padBindings = GamepadBindings.encode(next)) }
                    capturing = null
                    return true
                }
                override fun onMotionEvent(event: android.view.MotionEvent) = false
            }
            InputRouter.handler = handler
            onDispose { if (InputRouter.handler === handler) InputRouter.handler = null }
        }
        Text("Pick a layout, or tap a row and press the button you want on your controller.", style = RetraType.body, color = c.inkMuted)
        Spacer(Modifier.height(Space.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            GamepadBindings.Preset.entries.forEach { p ->
                RetraChip(p.label, map == GamepadBindings.preset(p), { onUpdate { it.copy(padBindings = GamepadBindings.encode(GamepadBindings.preset(p))) }; capturing = null })
            }
        }
        Spacer(Modifier.height(Space.s))
        if (target != null) {
            GlassSurface(Modifier.fillMaxWidth(), shape = Radius.mediumShape, wash = c.accentSoft) {
                Column(Modifier.padding(Space.m), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Press a button for “${target.label}”", style = RetraType.headline, color = c.onAccentSoft, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    Text("Waiting for your controller…", style = RetraType.label, color = c.onAccentSoft)
                    Spacer(Modifier.height(Space.xs))
                    RetraButton("Cancel", { capturing = null }, style = ButtonStyle.TERTIARY, compact = true)
                }
            }
            Spacer(Modifier.height(Space.s))
        }
        PadAction.entries.groupBy { it.group }.forEach { (group, actions) ->
            Text(group, style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(top = Space.xs, bottom = Space.xxs))
            GlassGroup {
                actions.forEachIndexed { i, a ->
                    val keys = GamepadBindings.keysFor(map, a)
                    NavRow(
                        a.label, { capturing = a },
                        trailing = if (keys.isEmpty()) "Not set" else keys.joinToString(" · ") { GamepadBindings.keyName(it) },
                    )
                    if (i < actions.lastIndex) Divider()
                }
            }
        }
        Spacer(Modifier.height(Space.s))
        RetraButton("Reset to defaults", { onUpdate { it.copy(padBindings = emptyMap()) }; capturing = null }, icon = Icons.Rounded.Restore, style = ButtonStyle.TERTIARY, compact = true)
    }
}

// ---------------------------------------------------------------- OpenRouter

@Composable
fun OpenRouterSheet(container: AppContainer, visible: Boolean, settings: Settings, onDismiss: () -> Unit) {
    val c = Retra.colors
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var has by remember(visible) { mutableStateOf(container.openRouter.hasKey) }
    var checking by remember { mutableStateOf(false) }
    var result by remember(visible) { mutableStateOf<String?>(null) }
    GlassSheet(visible, onDismiss, title = "Online cheat search") {
        Text(
            "Retra can ask an AI model with web search to find cheat codes for the game you're playing. It runs on your own OpenRouter account, so you stay in control of cost and privacy.",
            style = RetraType.body, color = c.inkMuted,
        )
        Spacer(Modifier.height(Space.s))
        if (has) {
            GlassSurface(Modifier.fillMaxWidth(), shape = Radius.mediumShape, wash = c.accentSoft) {
                Row(Modifier.padding(Space.m), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Key, null, tint = c.onAccentSoft)
                    Spacer(Modifier.width(Space.xs))
                    Column(Modifier.weight(1f)) {
                        Text("API key saved", style = RetraType.bodyStrong, color = c.onAccentSoft)
                        Text("Stored encrypted on this device", style = RetraType.caption, color = c.onAccentSoft)
                    }
                    RetraButton("Remove", { container.openRouter.removeKey(); has = false; result = null }, style = ButtonStyle.TERTIARY, compact = true)
                }
            }
        } else {
            RetraTextField(key, { key = it.trim() }, "Paste your OpenRouter key (sk-or-…)", leadingIcon = Icons.Rounded.Key, monospace = true)
            Spacer(Modifier.height(Space.xs))
            RetraButton(
                if (checking) "Checking…" else "Save and check key",
                {
                    scope.launch {
                        checking = true
                        val info = container.openRouter.verifyKey(key)
                        if (info != null && container.openRouter.saveKey(key)) { has = true; key = ""; result = info }
                        else result = if (info == null) "OpenRouter didn't accept that key." else "This device can't store keys securely."
                        checking = false
                    }
                },
                Modifier.fillMaxWidth(), enabled = key.length > 10 && !checking,
            )
        }
        result?.let { Text(it, style = RetraType.label, color = if (it.startsWith("Key works")) c.success else c.danger, modifier = Modifier.padding(top = Space.xs)) }
        Spacer(Modifier.height(Space.m))
        Text("Model", style = RetraType.label, color = c.inkMuted)
        Spacer(Modifier.height(Space.xxs))
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
            OpenRouterCheats.models.forEach { (id, label) ->
                RetraChip(label, settings.openRouterModel == id, { container.settings.update { it.copy(openRouterModel = id) } })
            }
        }
        Spacer(Modifier.height(Space.s))
        Text(
            "Codes from an AI can be wrong. Retra checks their format, adds them switched off, and the cheat editor says so. Try them on a backed-up save first.",
            style = RetraType.label, color = c.inkMuted,
        )
    }
}

// ---------------------------------------------------------------- Google Drive

@Composable
fun DriveSheet(container: AppContainer, visible: Boolean, settings: Settings, onDismiss: () -> Unit) {
    val c = Retra.colors
    val scope = rememberCoroutineScope()
    val activity = LocalActivity.current
    val status by container.drive.status.collectAsStateWithLifecycle()
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        val act = activity ?: return@rememberLauncherForActivityResult
        scope.launch {
            when (val a = container.drive.finishConsent(act, r.data)) {
                is DriveAuth.Token -> container.drive.connect(a.accessToken)
                is DriveAuth.Failed -> container.messages.show(a.message, MessageTone.ERROR)
                is DriveAuth.NeedsConsent -> Unit
            }
        }
    }
    fun connect() = scope.launch {
        when (val a = container.drive.authorize(activity)) {
            is DriveAuth.Token -> container.drive.connect(a.accessToken)
            is DriveAuth.NeedsConsent -> consent.launch(IntentSenderRequest.Builder(a.sender).build())
            is DriveAuth.Failed -> container.messages.show(a.message, MessageTone.ERROR)
        }
    }
    GlassSheet(visible, onDismiss, title = "Google Drive saves") {
        Text(
            "Back your game saves up to your own Google Drive and pick up on another phone. Retra only gets a hidden app folder: it can't see anything else in your Drive.",
            style = RetraType.body, color = c.inkMuted,
        )
        Spacer(Modifier.height(Space.s))
        val account = settings.driveAccount
        if (account == null) {
            RetraButton("Connect Google Drive", { connect() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.Cloud, enabled = status !is DriveStatus.Syncing)
        } else {
            GlassGroup {
                InfoRow("Account", account)
                Divider()
                InfoRow("Last sync", if (settings.driveLastSync == 0L) "Never" else android.text.format.DateUtils.getRelativeTimeSpanString(settings.driveLastSync).toString())
                Divider()
                InfoRow(
                    "Status",
                    when (val s = status) {
                        DriveStatus.Syncing -> "Syncing…"
                        is DriveStatus.Failed -> s.message
                        else -> "Ready"
                    },
                )
            }
            Spacer(Modifier.height(Space.xs))
            SwitchRow("Sync automatically", settings.driveAutoSync, { v -> container.settings.update { it.copy(driveAutoSync = v) } }, subtitle = "After each play session and when Retra opens", icon = Icons.Rounded.CloudSync)
            Spacer(Modifier.height(Space.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                RetraButton("Sync now", { scope.launch { container.drive.syncNow() } }, Modifier.weight(1f), icon = Icons.Rounded.CloudSync, enabled = status !is DriveStatus.Syncing)
                RetraButton("Disconnect", { container.drive.disconnect() }, style = ButtonStyle.SECONDARY, icon = Icons.Rounded.LinkOff)
            }
        }
        Spacer(Modifier.height(Space.s))
        Text("When a save differs, the newest copy wins and the one it replaces is kept in Retra's local backups.", style = RetraType.label, color = c.inkMuted)
    }
}

// ---------------------------------------------------------------- Link rooms

@Composable
fun LinkRoomSheet(container: AppContainer, visible: Boolean, settings: Settings, onDismiss: () -> Unit) {
    val c = Retra.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val actions = LocalAppActions.current
    val room by container.link.state.collectAsStateWithLifecycle()
    val games by container.library.games.collectAsStateWithLifecycle()
    var name by remember(settings.playerName) { mutableStateOf(settings.playerName) }
    var gameId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var chat by remember { mutableStateOf("") }
    val game = games.firstOrNull { it.id == (gameId ?: games.firstOrNull()?.id) }
    GlassSheet(visible, { onDismiss() }, title = "Link rooms") {
        when (room.phase) {
            RoomPhase.IDLE, RoomPhase.ERROR, RoomPhase.CLOSED -> {
                Text(
                    "Play together: open a room, share its code, and your friends join from their phones on the same Wi-Fi or a VPN like Tailscale. No account, no server.",
                    style = RetraType.body, color = c.inkMuted,
                )
                room.message?.let { Text(it, style = RetraType.label, color = c.danger, modifier = Modifier.padding(top = Space.xs)) }
                Spacer(Modifier.height(Space.s))
                RetraTextField(name, { name = it.take(20) }, "Your name")
                Spacer(Modifier.height(Space.xs))
                if (games.isNotEmpty()) {
                    Text("Playing", style = RetraType.label, color = c.inkMuted)
                    Spacer(Modifier.height(Space.xxs))
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        games.take(8).forEach { g -> RetraChip(g.title, g.id == game?.id, { gameId = g.id }) }
                    }
                    Spacer(Modifier.height(Space.s))
                }
                RetraButton(
                    "Host a room",
                    {
                        container.settings.update { it.copy(playerName = name) }
                        scope.launch { container.link.host(name, game?.title.orEmpty(), game?.gameCode.orEmpty()) }
                    },
                    Modifier.fillMaxWidth(), icon = Icons.Rounded.Groups,
                )
                Spacer(Modifier.height(Space.m))
                Text("Or join a friend", style = RetraType.label, color = c.inkMuted)
                Spacer(Modifier.height(Space.xxs))
                RetraTextField(code, { code = it.uppercase().take(11) }, "Room code, e.g. K3F9A-0Q7ZM", monospace = true, leadingIcon = Icons.Rounded.Wifi)
                Spacer(Modifier.height(Space.xs))
                RetraButton(
                    "Join room",
                    {
                        container.settings.update { it.copy(playerName = name) }
                        scope.launch { container.link.join(code, name, game?.title.orEmpty(), game?.gameCode.orEmpty()) }
                    },
                    Modifier.fillMaxWidth(), style = ButtonStyle.SECONDARY, enabled = RoomCode.decode(code) != null,
                )
            }
            RoomPhase.OPENING -> Text("Opening the room…", style = RetraType.body, color = c.inkMuted)
            RoomPhase.LOBBY, RoomPhase.STARTING -> {
                if (room.isHost) {
                    Text("Room code", style = RetraType.label, color = c.inkMuted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(room.code.ifBlank { "No network" }, style = RetraType.display, color = c.ink, modifier = Modifier.weight(1f))
                        RetraButton("Copy", {
                            (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                                .setPrimaryClip(android.content.ClipData.newPlainText("Retra room", room.code))
                            container.messages.show("Room code copied", MessageTone.SUCCESS)
                        }, icon = Icons.Rounded.ContentCopy, style = ButtonStyle.TERTIARY, compact = true)
                        RetraButton("Share", {
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(android.content.Intent.EXTRA_TEXT, "Join my Retra room: ${room.code}")
                            context.startActivity(android.content.Intent.createChooser(send, "Invite a friend").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }, icon = Icons.Rounded.Share, style = ButtonStyle.TERTIARY, compact = true)
                    }
                    Spacer(Modifier.height(Space.s))
                }
                GlassGroup {
                    room.players.forEachIndexed { i, p ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
                                Text(p.name.take(1).uppercase(), style = RetraType.bodyStrong, color = c.onAccentSoft)
                            }
                            Spacer(Modifier.width(Space.s))
                            Column(Modifier.weight(1f)) {
                                Text(p.name + if (p.id == room.you) " (you)" else "", style = RetraType.bodyStrong, color = c.ink)
                                Text(p.game.ifBlank { "No game picked" }, style = RetraType.caption, color = c.inkMuted, maxLines = 1)
                            }
                            if (p.host) Tag("Host") else Spacer(Modifier.width(0.dp))
                            Spacer(Modifier.width(Space.xs))
                            Tag(if (p.ready) "Ready" else "Waiting", color = if (p.ready) c.success else c.inkMuted)
                        }
                        if (i < room.players.lastIndex) Divider()
                    }
                }
                if (!room.sameGame) Text("Heads up: you're not all playing the same game.", style = RetraType.label, color = c.warning, modifier = Modifier.padding(top = Space.xs))
                Spacer(Modifier.height(Space.s))
                val me = room.players.firstOrNull { it.id == room.you }
                if (room.phase == RoomPhase.STARTING) {
                    Text("Everyone's in. Launch your game and play!", style = RetraType.headline, color = c.ink)
                    Spacer(Modifier.height(Space.xs))
                    RetraButton("Launch ${game?.title ?: "game"}", { game?.let { g -> onDismiss(); actions.play(g.id, true, "link") } }, Modifier.fillMaxWidth(), enabled = game != null)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                        RetraButton(if (me?.ready == true) "Not ready" else "I'm ready", { container.link.setReady(me?.ready != true) }, Modifier.weight(1f), style = if (me?.ready == true) ButtonStyle.SECONDARY else ButtonStyle.PRIMARY)
                        if (room.isHost) RetraButton("Start", { container.link.start() }, enabled = room.everyoneReady)
                    }
                }
                Spacer(Modifier.height(Space.s))
                room.chat.takeLast(4).forEach { Text("${it.from}: ${it.text}", style = RetraType.label, color = c.inkMuted) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RetraTextField(chat, { chat = it }, "Say something", Modifier.weight(1f), onImeAction = { if (chat.isNotBlank()) { container.link.say(chat); chat = "" } })
                    RetraButton("", { if (chat.isNotBlank()) { container.link.say(chat); chat = "" } }, icon = Icons.Rounded.Send, style = ButtonStyle.TERTIARY, compact = true)
                }
                Spacer(Modifier.height(Space.xs))
                RetraButton("Leave room", { container.link.close() }, style = ButtonStyle.TERTIARY, compact = true)
                Spacer(Modifier.height(Space.xs))
                Text(
                    "Beta: rooms, ready checks and chat work today. Emulated link-cable play (trades and battles) needs serial-port support in the engine and is next on the roadmap.",
                    style = RetraType.caption, color = c.inkMuted,
                )
            }
        }
        LaunchedEffect(visible) { if (!visible && room.phase == RoomPhase.ERROR) container.link.close() }
    }
}
