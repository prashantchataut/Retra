from pathlib import Path

src = Path(r"c:\projects\Retra\app\src\main\kotlin\app\retra\emulator\RetraV3Ui.kt")
out = Path(r"c:\projects\Retra\app\src\main\kotlin\app\retra\emulator")
lines = src.read_text(encoding="utf-8").splitlines(True)

def slice(a: int, b: int) -> str:
    return "".join(lines[a - 1 : b])

def to_internal(text: str) -> str:
    # Only convert private function/enum/data/val declarations
    out_lines = []
    for line in text.splitlines(True):
        if line.startswith("private fun ") or line.startswith("private enum ") or line.startswith("private data class ") or line.startswith("private val "):
            line = "internal " + line[len("private ") :]
        out_lines.append(line)
    return "".join(out_lines)

PKG = "package app.retra.emulator\n\n"

# --- Shared ---
shared_imports = '''import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryCoral

'''
shared = to_internal(
    slice(589, 599) + "\n" +
    slice(710, 761) + "\n" +
    slice(1034, 1051) + "\n" +
    slice(1148, 1154) + "\n" +
    slice(1226, 1236)
)
(out / "RetraSharedUi.kt").write_text(PKG + shared_imports + shared, encoding="utf-8")

# --- Home ---
home_imports = '''import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.SaveMint
import java.text.DateFormat
import java.util.Date

'''
home = to_internal(slice(420, 587))
(out / "RetraHomeUi.kt").write_text(PKG + home_imports + home, encoding="utf-8")

# --- Library ---
library_imports = '''import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.core.model.LibraryLayout
import app.retra.emulator.ui.components.RetraEmptyState
import app.retra.emulator.ui.components.RetraPageTitle

'''
# include filter enum + library function only (601-708), filter enum is 146-153
library = to_internal(slice(146, 153) + "\n" + slice(601, 708))
(out / "RetraLibraryUi.kt").write_text(PKG + library_imports + library, encoding="utf-8")

# --- Discover ---
discover_imports = '''import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.emulator.data.HomebrewHubEntry
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryAqua
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RetraBlue
import app.retra.emulator.ui.theme.SaveMint

'''
discover = to_internal(slice(763, 959))
(out / "RetraDiscoverUi.kt").write_text(PKG + discover_imports + discover, encoding="utf-8")

# --- Profile ---
profile_imports = '''import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.AchievementStatus
import app.retra.emulator.ui.components.RetraPageTitle
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.components.RetraSectionHeader
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.SaveMint

'''
profile = to_internal(slice(961, 1032))
(out / "RetraProfileUi.kt").write_text(PKG + profile_imports + profile, encoding="utf-8")

# --- Game sheet + patch ---
sheet_imports = '''import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.retra.core.model.GameRecord
import app.retra.emulator.data.KnownPatchHints
import app.retra.emulator.data.PendingPatch
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.RetraBlue
import app.retra.emulator.ui.theme.SaveMint

'''
sheet = to_internal(slice(1053, 1224))
(out / "RetraGameDetailsUi.kt").write_text(PKG + sheet_imports + sheet, encoding="utf-8")

# --- Onboarding ---
onboarding_imports = '''import androidx.compose.foundation.layout.Arrangement
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

'''
onboarding = to_internal(slice(1238, 1318))
(out / "RetraOnboardingUi.kt").write_text(PKG + onboarding_imports + onboarding, encoding="utf-8")

# --- Shell: keep root, app, nav, destination enum, mime types ---
shell_imports = '''import android.content.Intent
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

'''
shell_body = (
    to_internal(slice(139, 144)) + "\n" +
    # keep V3Destination private to shell - use private again
    slice(155, 418).replace("private enum class V3Destination", "private enum class V3Destination") + "\n" +
    slice(1320, 1326)
)
# Fix: destination enum was converted incorrectly if we used to_internal on 139-144
# Rebuild shell carefully
shell_body = slice(139, 144) + "\n" + slice(155, 418) + "\n" + slice(1320, 1326)
(out / "RetraV3Ui.kt").write_text(PKG + shell_imports + shell_body, encoding="utf-8")

print("Split complete")
for name in [
    "RetraSharedUi.kt", "RetraHomeUi.kt", "RetraLibraryUi.kt", "RetraDiscoverUi.kt",
    "RetraProfileUi.kt", "RetraGameDetailsUi.kt", "RetraOnboardingUi.kt", "RetraV3Ui.kt"
]:
    p = out / name
    print(name, sum(1 for _ in p.open(encoding="utf-8")))
