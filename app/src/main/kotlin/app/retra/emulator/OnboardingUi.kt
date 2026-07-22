package app.retra.emulator

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.retra.core.model.AppSettings
import app.retra.emulator.auth.AuthOperation
import app.retra.emulator.auth.RetraAccount
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryAqua
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.VoidBlack

private const val ONBOARDING_STEPS = 4

@Composable
fun OnboardingScreen(
    settings: AppSettings,
    account: RetraAccount?,
    authOperation: AuthOperation,
    googleConfigured: Boolean,
    onGoogleSignIn: () -> Unit,
    onComplete: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 780.dp
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(28.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                OnboardingStage(step = step, reduceMotion = settings.reduceMotion, modifier = Modifier.weight(0.94f).fillMaxHeight())
                OnboardingFlowCard(
                    step = step,
                    settings = settings,
                    account = account,
                    authOperation = authOperation,
                    googleConfigured = googleConfigured,
                    onGoogleSignIn = onGoogleSignIn,
                    onBack = { step = (step - 1).coerceAtLeast(0) },
                    onNext = { if (step == ONBOARDING_STEPS - 1) onComplete() else step++ },
                    onCompleteOffline = onComplete,
                    modifier = Modifier.weight(1.06f).fillMaxHeight()
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OnboardingCompactHeader(step)
                OnboardingFlowCard(
                    step = step,
                    settings = settings,
                    account = account,
                    authOperation = authOperation,
                    googleConfigured = googleConfigured,
                    onGoogleSignIn = onGoogleSignIn,
                    onBack = { step = (step - 1).coerceAtLeast(0) },
                    onNext = { if (step == ONBOARDING_STEPS - 1) onComplete() else step++ },
                    onCompleteOffline = onComplete,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OnboardingCompactHeader(step: Int) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RetraLogoTile(size = 48.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Retra", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(
                "A home for the worlds that raised you.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                "${step + 1} / $ONBOARDING_STEPS",
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OnboardingStage(step: Int, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, cornerRadius = 34.dp, contentPadding = PaddingValues(0.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(30.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                RetraLogoTile(size = 66.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("RETRA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(
                        "PORTAL / SAVE CORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            RetraAnimatedContent(
                targetState = step,
                reduceMotion = reduceMotion,
                label = "onboarding-stage",
                modifier = Modifier.fillMaxWidth()
            ) { current ->
                when (current) {
                    0 -> MemoryShelfVisual()
                    1 -> ImportPathVisual()
                    2 -> VerifiedSourcesVisual()
                    else -> LocalIdentityVisual()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    when (step) {
                        0 -> "RETURN,\nDON'T RESTART."
                        1 -> "BRING WHAT\nYOU OWN."
                        2 -> "TRUST THE\nSOURCE."
                        else -> "LOCAL FIRST.\nALWAYS YOURS."
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    when (step) {
                        0 -> "Resume the handheld adventures, saves, and side quests that still feel like home."
                        1 -> "Import your backups, preserve originals, and apply compatible patches without guesswork."
                        2 -> "Install legal homebrew, identify games by checksum, and attach cheats to the exact ROM—not a filename guess."
                        else -> "Play, save, customize, and earn progress offline. Connect identity only when it adds value."
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StagePill(Icons.Default.Gamepad, "GBA")
                    StagePill(Icons.Default.Save, "Atomic saves")
                    StagePill(Icons.Default.Lock, "Private")
                }
            }
        }
    }
}

@Composable
private fun MemoryShelfVisual() {
    Box(Modifier.fillMaxWidth().aspectRatio(1.25f), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f).fillMaxHeight(0.76f),
            shape = RoundedCornerShape(30.dp),
            color = VoidBlack,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp)) {
                RetraLogo(modifier = Modifier.align(Alignment.Center), size = 150.dp, markColor = Color.White, cutoutColor = VoidBlack)
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.10f)
                ) {
                    Text(
                        "CONTINUE · 18:42",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
        MemoryCard("SAVE 03", MemoryCoral, Modifier.align(Alignment.TopStart))
        MemoryCard("BADGE 07", AdventureGold, Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun ImportPathVisual() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ImportNode(Icons.Default.FolderOpen, "YOUR BACKUP", "Read-only source")
        Text("↓", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        ImportNode(Icons.Default.Code, "PATCH SAFELY", "IPS · UPS · BPS")
        Text("↓", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        ImportNode(Icons.Default.Verified, "READY TO PLAY", "Identity + checksum stored")
    }
}

@Composable
private fun VerifiedSourcesVisual() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ImportNode(Icons.Default.Verified, "EXACT METADATA", "SHA-1 · CRC-32 · file size")
        ImportNode(Icons.Default.AutoAwesome, "LEGAL HOMEBREW", "Creator-published GBA releases")
        ImportNode(Icons.Default.Code, "ROM-BOUND CHEATS", "Imported only for the exact SHA-256")
    }
}

@Composable
private fun LocalIdentityVisual() {
    Surface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1.25f),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    RetraLogoTile(modifier = Modifier.padding(8.dp), size = 62.dp)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Player One", style = MaterialTheme.typography.titleLarge)
                    Text("Offline player", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("RETRA · LOCAL", style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }
                Icon(Icons.Default.Security, contentDescription = null, tint = SaveMint)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IdentityStat("12", "games", Modifier.weight(1f))
                IdentityStat("48", "saves", Modifier.weight(1f))
                IdentityStat("0", "uploads", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MemoryCard(label: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        shadowElevation = 8.dp
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(9.dp).background(accent, CircleShape))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ImportNode(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.82f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                Icon(icon, null, Modifier.padding(10.dp).size(21.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun IdentityStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StagePill(icon: ImageVector, label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun OnboardingFlowCard(
    step: Int,
    settings: AppSettings,
    account: RetraAccount?,
    authOperation: AuthOperation,
    googleConfigured: Boolean,
    onGoogleSignIn: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onCompleteOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val feedback = LocalRetraFeedback.current
    GlassPanel(
        modifier = modifier,
        settings = settings,
        cornerRadius = 32.dp,
        contentPadding = PaddingValues(22.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            LinearProgressIndicator(
                progress = { (step + 1) / ONBOARDING_STEPS.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            RetraAnimatedContent(
                targetState = step,
                reduceMotion = settings.reduceMotion,
                label = "onboarding-step",
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { current ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (current) {
                        0 -> WhatRetraIsStep()
                        1 -> BringYourLibraryStep()
                        2 -> VerifiedSourcesStep()
                        else -> AccountStep(
                            account = account,
                            operation = authOperation,
                            googleConfigured = googleConfigured,
                            onGoogleSignIn = onGoogleSignIn,
                            onCompleteOffline = onCompleteOffline
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = {
                            feedback(FeedbackCue.TAP)
                            onBack()
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    ) { Text("Back") }
                }
                Button(
                    onClick = {
                        feedback(if (step == ONBOARDING_STEPS - 1) FeedbackCue.CONFIRM else FeedbackCue.TAP)
                        onNext()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                ) {
                    Text(if (step == ONBOARDING_STEPS - 1) "Enter Retra" else "Continue")
                }
            }
        }
    }
}

@Composable
private fun WhatRetraIsStep() {
    StepHeading(
        eyebrow = "Your memory player",
        title = "Return to the worlds that raised you",
        body = "Retra turns a folder of games into an artwork-led archive built for resuming—not file management."
    )
    FeatureLine(Icons.Default.Gamepad, "Familiar the moment play starts", "Touch, keyboard, Bluetooth, and USB controllers share one clear player model.")
    FeatureLine(Icons.Default.Save, "Saves treated as memories", "Battery saves, quick states, and suspend snapshots use protected, atomic writes.")
    FeatureLine(Icons.Default.AutoAwesome, "Calm outside the game", "The library prioritizes cover art, recency, and progress rather than emulator jargon.")
}

@Composable
private fun BringYourLibraryStep() {
    StepHeading(
        eyebrow = "Bring what you own",
        title = "From backup to adventure in a few taps",
        body = "Import your own GBA backups, preserve the original, and create patched variants with clear compatibility checks."
    )
    FeatureLine(Icons.Default.FolderOpen, "Automatic organization", "Import one .gba file or scan a folder; Retra detects identity, duplicates, and metadata.")
    FeatureLine(Icons.Default.Code, "Patch without overwriting", "Apply IPS, UPS, or BPS patches to a compatible base game while keeping the source untouched.")
    FeatureLine(Icons.Default.Verified, "Reviewable one-tap releases", "Homebrew installs are limited to Homebrew Hub HTTPS files, playable GBA entries, non-hack-ROM types, and declared license metadata. Retra records the imported SHA-256.")
}

@Composable
private fun VerifiedSourcesStep() {
    StepHeading(
        eyebrow = "Real data, clear provenance",
        title = "A library you can trust",
        body = "Retra uses public, reviewable sources for legal homebrew, canonical metadata, and community cheat definitions."
    )
    FeatureLine(Icons.Default.AutoAwesome, "Playable homebrew in one tap", "Browse creator-published GBA releases from Homebrew Hub and add them directly to your local library.")
    FeatureLine(Icons.Default.Verified, "Identity before artwork", "Canonical names are applied only after an exact SHA-1 match or a unique CRC-32 plus size match.")
    FeatureLine(Icons.Default.Code, "Cheats bound to one ROM", "Import RetroArch .cht files or install a matching Libretro file; Retra converts codes into its restricted, reversible format.")
    FeatureLine(Icons.Default.Security, "No commercial ROM storefront", "Pokémon and other commercial games are never bundled. Import a lawful personal backup, then Retra can identify and enhance it locally.")
}

@Composable
private fun AccountStep(
    account: RetraAccount?,
    operation: AuthOperation,
    googleConfigured: Boolean,
    onGoogleSignIn: () -> Unit,
    onCompleteOffline: () -> Unit
) {
    StepHeading(
        eyebrow = "Stay in control",
        title = "Offline first. Identity optional.",
        body = "Games, saves, settings, patches, cheats, and achievements work without an account or network connection."
    )

    if (account != null) {
        GlassPanel(cornerRadius = 24.dp, contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Text(account.initials, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(account.displayName ?: "Google account", style = MaterialTheme.typography.titleMedium)
                    Text(account.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Connected on this device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.CloudDone, contentDescription = null, tint = MemoryAqua)
            }
        }
    } else {
        FilledTonalButton(
            onClick = onGoogleSignIn,
            enabled = googleConfigured && operation == AuthOperation.IDLE,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Icon(Icons.Default.Login, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text(if (operation == AuthOperation.SIGNING_IN) "Connecting…" else "Connect optional identity")
        }
        OutlinedButton(onClick = onCompleteOffline, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text("Keep everything offline")
        }
        if (!googleConfigured) {
            Text(
                "Google sign-in is unavailable until RETRA_GOOGLE_WEB_CLIENT_ID is configured for this build.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    FeatureLine(Icons.Default.Lock, "No ROM uploads", "Connecting an identity never uploads game files. Any future save backup must remain separate and opt-in.")
    FeatureLine(Icons.Default.Security, "Clear network boundaries", "Discovery opens official creator pages or checksum-pinned authorized files; commercial ROMs are never bundled.")
}

@Composable
private fun StepHeading(eyebrow: String, title: String, body: String) {
    Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FeatureLine(icon: ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(17.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
