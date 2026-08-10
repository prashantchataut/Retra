package app.retra.emulator

import androidx.compose.animation.AnimatedVisibility
import app.retra.emulator.auth.AuthOperation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.retra.core.model.AccentPalette
import app.retra.emulator.ui.components.RetraBadge
import app.retra.emulator.ui.components.RetraPanel
import app.retra.emulator.ui.theme.AdventureGold
import app.retra.emulator.ui.theme.ElectricLilac
import app.retra.emulator.ui.theme.MemoryCoral
import app.retra.emulator.ui.theme.PeachGlow
import app.retra.emulator.ui.theme.RaspberryPink
import app.retra.emulator.ui.theme.SaveMint
import app.retra.emulator.ui.theme.SurfaceMidnight
import app.retra.emulator.ui.theme.NightPlum
import app.retra.emulator.ui.theme.VoidBlack
import app.retra.emulator.ui.theme.WarmCream
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun RetraOnboarding(viewModel: RetraViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 5
    val context = LocalContext.current
    val account by viewModel.account.collectAsStateWithLifecycle()
    val authOperation by viewModel.authOperation.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val wide = maxWidth >= 760.dp
            if (wide) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .weight(0.50f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        OnboardingVisualSurface(step, viewModel)
                    }
                    Column(
                        Modifier
                            .weight(0.50f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        OnboardingHeader(step, totalSteps)
                        OnboardingCopy(step, account?.displayName)
                        OnboardingNavigation(
                            step = step,
                            totalSteps = totalSteps,
                            authOperation = authOperation,
                            onBack = { if (step > 0) step-- },
                            onNext = { if (step < totalSteps - 1) step++ else viewModel.finishOnboarding() },
                            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                            onSkipSignIn = { viewModel.finishOnboarding() }
                        )
                    }
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 22.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    OnboardingHeader(step, totalSteps)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        OnboardingVisualSurface(step, viewModel)
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OnboardingCopy(step, account?.displayName)
                        OnboardingNavigation(
                            step = step,
                            totalSteps = totalSteps,
                            authOperation = authOperation,
                            onBack = { if (step > 0) step-- },
                            onNext = { if (step < totalSteps - 1) step++ else viewModel.finishOnboarding() },
                            onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                            onSkipSignIn = { viewModel.finishOnboarding() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader(step: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RetraLogoTile(size = 40.dp)
            Text(
                "retra",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Text(
                "${step + 1} of $totalSteps",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OnboardingVisualSurface(step: Int, viewModel: RetraViewModel) {
    RetraPanel(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 280.dp, max = 420.dp),
        tier = GlassTier.ELEVATED,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (step) {
                0 -> StepOneHeroVisual()
                1 -> StepTwoLibraryVisual()
                2 -> StepThreePatchVisual()
                3 -> StepFourPersonalizeVisual(viewModel)
                4 -> StepFiveIdentityVisual()
            }
        }
    }
}

@Composable
private fun StepOneHeroVisual() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        RetraMascot(
            size = 140.dp,
            state = MascotState.IDLE,
            interactive = true
        )
        Surface(
            shape = CircleShape,
            color = ElectricLilac.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ElectricLilac.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.TouchApp, null, Modifier.size(16.dp), tint = ElectricLilac)
                Text("Touch or drag sprite", style = MaterialTheme.typography.labelSmall, color = ElectricLilac)
            }
        }
    }
}

@Composable
private fun StepTwoLibraryVisual() {
    val coroutineScope = rememberCoroutineScope()
    val parallaxX = remember { Animatable(0f) }
    val parallaxY = remember { Animatable(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch { parallaxX.animateTo(0f, spring(dampingRatio = 0.6f)) }
                        coroutineScope.launch { parallaxY.animateTo(0f, spring(dampingRatio = 0.6f)) }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch { parallaxX.snapTo(parallaxX.value + dragAmount.x * 0.35f) }
                        coroutineScope.launch { parallaxY.snapTo(parallaxY.value + dragAmount.y * 0.35f) }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Game Card 1 (Top Left)
        Surface(
            modifier = Modifier
                .offset { IntOffset((-80 + parallaxX.value * 0.8f).roundToInt(), (-70 + parallaxY.value * 0.8f).roundToInt()) }
                .size(width = 86.dp, height = 114.dp)
                .rotate(-8f),
            shape = MaterialTheme.shapes.medium,
            color = SurfaceMidnight.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, ElectricLilac.copy(alpha = 0.45f)),
            shadowElevation = 8.dp
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(ElectricLilac.copy(alpha = 0.3f), Color.Transparent))), contentAlignment = Alignment.Center) {
                Text("GBA", fontWeight = FontWeight.Black, color = ElectricLilac, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Floating Game Card 2 (Bottom Right)
        Surface(
            modifier = Modifier
                .offset { IntOffset((85 + parallaxX.value * 0.6f).roundToInt(), (65 + parallaxY.value * 0.6f).roundToInt()) }
                .size(width = 92.dp, height = 120.dp)
                .rotate(9f),
            shape = MaterialTheme.shapes.medium,
            color = SurfaceMidnight.copy(alpha = 0.88f),
            border = BorderStroke(1.dp, SaveMint.copy(alpha = 0.45f)),
            shadowElevation = 8.dp
        ) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(SaveMint.copy(alpha = 0.25f), Color.Transparent))), contentAlignment = Alignment.Center) {
                Text("ZIP", fontWeight = FontWeight.Black, color = SaveMint, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Center Mascot
        RetraMascot(
            size = 110.dp,
            state = MascotState.IDLE,
            interactive = false
        )
    }
}

@Composable
private fun StepThreePatchVisual() {
    val infiniteTransition = rememberInfiniteTransition(label = "PatchMerge")
    val mergeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mergeProgress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy((16 * (1f - mergeProgress)).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Base ROM Card
            Surface(
                modifier = Modifier
                    .size(width = 88.dp, height = 110.dp)
                    .scale(0.9f + 0.1f * (1f - mergeProgress)),
                shape = MaterialTheme.shapes.large,
                color = SurfaceMidnight,
                border = BorderStroke(1.5.dp, ElectricLilac.copy(alpha = 0.6f))
            ) {
                Column(
                    Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Gamepad, null, tint = ElectricLilac, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Base ROM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            // Plus Sign / Spark
            Surface(
                shape = CircleShape,
                color = (if (mergeProgress > 0.7f) SaveMint else MemoryCoral).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (mergeProgress > 0.7f) SaveMint else MemoryCoral)
            ) {
                Icon(
                    if (mergeProgress > 0.7f) Icons.Default.AutoAwesome else Icons.Default.Add,
                    null,
                    Modifier.padding(8.dp).size(18.dp),
                    tint = if (mergeProgress > 0.7f) SaveMint else MemoryCoral
                )
            }

            // Patch Card
            Surface(
                modifier = Modifier
                    .size(width = 88.dp, height = 110.dp)
                    .scale(0.9f + 0.1f * (1f - mergeProgress)),
                shape = MaterialTheme.shapes.large,
                color = SurfaceMidnight,
                border = BorderStroke(1.5.dp, MemoryCoral.copy(alpha = 0.6f))
            ) {
                Column(
                    Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Tune, null, tint = MemoryCoral, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("UPS / IPS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = (if (mergeProgress > 0.75f) SaveMint else ElectricLilac).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, (if (mergeProgress > 0.75f) SaveMint else ElectricLilac).copy(alpha = 0.35f))
        ) {
            Text(
                if (mergeProgress > 0.75f) "✨ Patched game created locally!" else "Pair patch with locally provided base",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (mergeProgress > 0.75f) SaveMint else ElectricLilac
            )
        }
    }
}

@Composable
private fun StepFourPersonalizeVisual(viewModel: RetraViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var previewPressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Mini Interactive Controller Preview
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(110.dp),
            shape = MaterialTheme.shapes.large,
            color = SurfaceMidnight.copy(alpha = 0.90f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini D-Pad Button
                Surface(
                    onClick = {
                        previewPressed = !previewPressed
                        viewModel.emitFeedback(FeedbackCue.TAP)
                    },
                    shape = CircleShape,
                    color = if (previewPressed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Gamepad, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    }
                }

                // Center Screen Pill
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = VoidBlack,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Text("60 FPS", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = SaveMint)
                }

                // Mini Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            viewModel.emitFeedback(FeedbackCue.CONFIRM)
                        },
                        shape = CircleShape,
                        color = MemoryCoral.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, MemoryCoral.copy(alpha = 0.6f))
                    ) {
                        Text("B", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Black, color = MemoryCoral)
                    }
                    Surface(
                        onClick = {
                            viewModel.emitFeedback(FeedbackCue.CONFIRM)
                        },
                        shape = CircleShape,
                        color = SaveMint.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, SaveMint.copy(alpha = 0.6f))
                    ) {
                        Text("A", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Black, color = SaveMint)
                    }
                }
            }
        }

        // Palette Selector
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (palette in AccentPalette.entries) {
                FilterChip(
                    selected = settings.accentPalette == palette,
                    onClick = { viewModel.setAccentPalette(palette) },
                    label = { Text(palette.name.lowercase().replaceFirstChar(Char::uppercase)) }
                )
            }
        }
    }
}

@Composable
private fun StepFiveIdentityVisual() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        RetraMascot(
            size = 110.dp,
            state = MascotState.SUCCESS,
            interactive = true
        )
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = SaveMint.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, SaveMint.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Security, null, Modifier.size(18.dp), tint = SaveMint)
                Text("Private on device · Offline always enabled", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = SaveMint)
            }
        }
    }
}

@Composable
private fun OnboardingCopy(step: Int, accountName: String?) {
    val titles = listOf(
        "A beloved handheld memory rebuilt.",
        "Your games, private and local.",
        "ROM hacks and patches made simple.",
        "Make the handheld yours.",
        if (accountName != null) "Welcome back, $accountName!" else "Welcome to Retra."
    )
    val bodies = listOf(
        "Retra is an original, tactile emulator and archive crafted to bring your favorite adventures back to life.",
        "Import your own GBA ROM backups or ZIP archives. Everything stays private on this device with zero external dependencies.",
        "Retra intelligently pairs IPS, UPS, and BPS patches with your local base ROMs, verifying checksums with pinpoint accuracy.",
        "Tailor liquid-glass touch controls, haptic vibration, display scaling, and themes to match your personal rhythm.",
        "Connect optional Google identity for profile personalization, or jump straight into offline play without creating an account."
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            titles[step],
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            bodies[step],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OnboardingNavigation(
    step: Int,
    totalSteps: Int,
    authOperation: AuthOperation,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSkipSignIn: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LinearProgressIndicator(
            progress = { (step + 1f) / totalSteps },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape)
        )

        if (step == totalSteps - 1) {
            // Screen 5: Identity & Sign-In Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onGoogleSignIn,
                    enabled = authOperation == AuthOperation.IDLE,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (authOperation == AuthOperation.SIGNING_IN) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Connecting...")
                    } else {
                        Icon(Icons.Default.Security, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Continue with Google", fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onSkipSignIn,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Use Retra offline (No account)")
                }
            }
        } else {
            // Screens 1 to 4 Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Back")
                    }
                }
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(if (step > 0) 1f else 2f).heightIn(min = 48.dp)
                ) {
                    Text(if (step == 0) "Get Started" else "Continue", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}
