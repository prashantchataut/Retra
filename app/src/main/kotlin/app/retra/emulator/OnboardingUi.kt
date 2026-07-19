package app.retra.emulator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.retra.core.model.AppSettings
import app.retra.emulator.auth.AuthOperation
import app.retra.emulator.auth.RetraAccount

private const val ONBOARDING_STEPS = 3

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
        val wide = maxWidth >= 760.dp
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OnboardingBrandPanel(Modifier.weight(0.9f).fillMaxHeight())
                OnboardingCard(
                    step = step,
                    settings = settings,
                    account = account,
                    authOperation = authOperation,
                    googleConfigured = googleConfigured,
                    onGoogleSignIn = onGoogleSignIn,
                    onBack = { step = (step - 1).coerceAtLeast(0) },
                    onNext = { if (step == ONBOARDING_STEPS - 1) onComplete() else step++ },
                    onCompleteOffline = onComplete,
                    modifier = Modifier.weight(1.1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RetraLogoTile(size = 52.dp)
                    Column {
                        Text("Retra", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Relive the games that made you.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                OnboardingCard(
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
private fun OnboardingBrandPanel(modifier: Modifier = Modifier) {
    GlassPanel(
        modifier = modifier,
        cornerRadius = 36.dp,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(36.dp)
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RetraLogo(size = 170.dp)
            Spacer(Modifier.height(24.dp))
            Text("Retra", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(
                "A private, premium home for your retro library.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniPill(Icons.Default.Gamepad, "Play")
                MiniPill(Icons.Default.Save, "Protect")
                MiniPill(Icons.Default.Security, "Private")
            }
        }
    }
}

@Composable
private fun MiniPill(icon: ImageVector, label: String) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OnboardingCard(
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                        0 -> WelcomeStep()
                        1 -> LibraryStep()
                        else -> AccountStep(account, authOperation, googleConfigured, onGoogleSignIn, onCompleteOffline)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = {
                            feedback(FeedbackCue.TAP)
                            onBack()
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text("Back") }
                }
                Button(
                    onClick = {
                        feedback(if (step == ONBOARDING_STEPS - 1) FeedbackCue.CONFIRM else FeedbackCue.TAP)
                        onNext()
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(if (step == ONBOARDING_STEPS - 1) "Enter Retra" else "Continue")
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    StepHeading(
        "What Retra is",
        "Retra is a private Game Boy Advance home: fast library access, reliable saves, and controller-first play — without turning your collection into a file browser."
    )
    FeatureLine(Icons.Default.Security, "Private by default", "ROMs stay on your device unless you explicitly choose a personal backup provider.")
    FeatureLine(Icons.Default.Gamepad, "Built for play", "Touch, Bluetooth, USB controllers, keyboard input, fast-forward, and suspend/resume are first-class flows.")
    FeatureLine(Icons.Default.Save, "Saves you can trust", "Versioned, checksummed envelopes protect battery saves and states across sessions.")
}

@Composable
private fun LibraryStep() {
    StepHeading(
        "What files Retra imports",
        "Bring your own backups through Android's Storage Access Framework, or add legal, checksum-verified homebrew catalogs."
    )
    FeatureLine(Icons.Default.FolderOpen, "Verified imports", "Retra accepts .gba and .zip libraries, parses the GBA header, computes SHA-256, and never silently edits the source ROM.")
    FeatureLine(Icons.Default.Code, "Patches, not pirated copies", "IPS, UPS, and BPS patches are applied locally to a compatible base ROM and stored as separate library entries.")
    FeatureLine(Icons.Default.Lock, "Legal online catalogs", "Downloads require HTTPS, a declared license, distribution permission, size limits, and an exact checksum.")
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
        "Optional Google identity",
        "An account is optional. Local play, saves, patches, cheats, and achievements work without signing in."
    )
    if (account != null) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Text(account.initials, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(account.displayName ?: "Google account", style = MaterialTheme.typography.titleMedium)
                    Text(account.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Connected on this device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Default.CloudDone, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text("Cloud features remain disabled until a Retra backend verifies the Google ID token and nonce.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        FilledTonalButton(
            onClick = onGoogleSignIn,
            enabled = googleConfigured && operation == AuthOperation.IDLE,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Default.Login, null)
            Spacer(Modifier.size(10.dp))
            Text(if (operation == AuthOperation.SIGNING_IN) "Connecting…" else "Continue with Google")
        }
        if (!googleConfigured) {
            Text("Google sign-in is ready in source but this build needs RETRA_GOOGLE_WEB_CLIENT_ID.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        OutlinedButton(onClick = onCompleteOffline, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Use Retra offline")
        }
    }
    FeatureLine(Icons.Default.Lock, "No ROM uploads", "Signing in never uploads ROM files. Cloud backup must remain opt-in and save-focused.")
}

@Composable
private fun StepHeading(title: String, body: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FeatureLine(icon: ImageVector, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
