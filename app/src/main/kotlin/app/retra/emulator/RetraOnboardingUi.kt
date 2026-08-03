package app.retra.emulator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.emulator.ui.theme.MemoryAqua
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RetraBlue
import app.retra.emulator.ui.theme.SaveMint

@Composable
internal fun V3Onboarding(viewModel: RetraViewModel) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        V3OnboardingPage("A private archive for games you return to", "Retra is built around resuming play, understanding your saves, and keeping every imported file exact.", Icons.Default.SportsEsports, RetraBlue),
        V3OnboardingPage("A real homebrew game is already here", "Retra Drift is an original GBA mini-game included to verify the player without bundling copyrighted commercial ROMs.", Icons.Default.Gamepad, SaveMint),
        V3OnboardingPage("Patches need the exact base", "UPS, IPS, and BPS files are transformations, not games. Retra checks size and checksum before creating a separate patched copy.", Icons.Default.AutoAwesome, MemoryCoral),
        V3OnboardingPage("Your progress stays recoverable", "Manual states, rotating backups, screenshots, patch lineage, and checksum identity remain local and visible.", Icons.Default.Shield, MemoryAqua)
    )
    val current = pages[page]

    Scaffold(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val wide = maxWidth >= 720.dp
            if (wide) {
                Row(Modifier.fillMaxSize().padding(36.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                    V3OnboardingVisual(current, Modifier.weight(0.46f).fillMaxHeight())
                    V3OnboardingCopy(page, pages.size, current, { if (page > 0) page-- }, { if (page == pages.lastIndex) viewModel.finishOnboarding() else page++ }, Modifier.weight(0.54f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RetraLogoTile(size = 48.dp)
                        Spacer(Modifier.width(11.dp))
                        Text("Retra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Spacer(Modifier.weight(1f))
                        Text("${page + 1}/${pages.size}", style = MaterialTheme.typography.labelLarge)
                    }
                    V3OnboardingVisual(current, Modifier.fillMaxWidth().weight(0.42f))
                    V3OnboardingCopy(page, pages.size, current, { if (page > 0) page-- }, { if (page == pages.lastIndex) viewModel.finishOnboarding() else page++ }, Modifier.weight(0.58f))
                }
            }
        }
    }
}

@Composable
internal fun V3OnboardingVisual(page: V3OnboardingPage, modifier: Modifier = Modifier) {
    GlassPanel(modifier, shape = MaterialTheme.shapes.extraLarge) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = page.accent.copy(alpha = 0.14f), contentColor = page.accent) {
                Icon(page.icon, null, Modifier.padding(38.dp).size(74.dp))
            }
        }
    }
}

@Composable
internal fun V3OnboardingCopy(
    pageIndex: Int,
    pageCount: Int,
    page: V3OnboardingPage,
    onBack: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.SpaceBetween) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${pageIndex + 1} of $pageCount", style = MaterialTheme.typography.labelMedium, color = page.accent)
            Text(page.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(page.body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (pageIndex == 1) {
                Surface(shape = MaterialTheme.shapes.medium, color = SaveMint.copy(alpha = 0.12f)) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = SaveMint)
                        Text("Source and build script live under tools/demo-rom.")
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(progress = { (pageIndex + 1f) / pageCount }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pageIndex > 0) OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ArrowBack, null); Spacer(Modifier.width(5.dp)); Text("Back") }
                Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text(if (pageIndex == pageCount - 1) "Enter Retra" else "Continue") }
            }
        }
    }
}

internal data class V3OnboardingPage(val title: String, val body: String, val icon: ImageVector, val accent: Color)
