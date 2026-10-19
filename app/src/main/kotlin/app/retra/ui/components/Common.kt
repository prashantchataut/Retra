package app.retra.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.Motion
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import app.retra.ui.theme.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Big screen title on the sky (no box): display type + optional muted subtitle. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = Retra.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = RetraType.display, color = c.ink, modifier = Modifier.semantics { heading() })
            if (subtitle != null) {
                Spacer(Modifier.height(Space.xxs))
                Text(subtitle, style = RetraType.body, color = c.inkMuted)
            }
        }
        trailing()
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = Retra.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = RetraType.headline, color = c.ink, modifier = Modifier.semantics { heading() })
            if (subtitle != null) Text(subtitle, style = RetraType.label, color = c.inkMuted)
        }
        action?.invoke()
    }
}

/** Mochi + one sentence + one action. Used for empty, error and offline states. */
@Composable
fun MochiMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    mood: MochiMood = MochiMood.SLEEPY,
    action: (@Composable () -> Unit)? = null,
) {
    val c = Retra.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = Space.l),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mochi(mood = mood, size = 112.dp)
        Spacer(Modifier.height(Space.m))
        Text(title, style = RetraType.headline, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space.xs))
        Text(body, style = RetraType.body, color = c.inkMuted, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 320.dp))
        if (action != null) {
            Spacer(Modifier.height(Space.l))
            action()
        }
    }
}

/** Tiny tag ("GBA code", "Beta", "UPS"). */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier, color: Color = Retra.colors.inkMuted, background: Color = Retra.colors.inkFaint, icon: ImageVector? = null) {
    Row(
        modifier.clip(Radius.pillShape).background(background).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, style = RetraType.caption, color = color, maxLines = 1)
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Retra.colors.divider))
}

@Composable
fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    val c = Retra.colors
    Row(modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = RetraType.label, color = c.inkMuted, modifier = Modifier.padding(end = Space.m))
        Text(value, style = RetraType.label, color = c.ink, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}

/**
 * Staggered entrance: fade + 16 dp rise, 40 ms apart, only for the first 8 items of a list.
 * Runs once per [key]; instant with reduced motion.
 */
@Composable
fun Modifier.staggerIn(index: Int, key: Any = Unit): Modifier {
    val reduce = Retra.reduceMotion
    if (reduce || index >= Motion.MAX_STAGGERED) return this
    val progress = remember(key) { Animatable(0f) }
    val rise = with(LocalDensity.current) { 16.dp.toPx() }
    LaunchedEffect(key) {
        delay(index * Motion.STAGGER_MS.toLong())
        launch { progress.animateTo(1f, spring(Motion.DAMPING, Motion.GENTLE, visibilityThreshold = 0.001f)) }
    }
    return graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * rise
    }
}

// ---- Shared element plumbing (no-ops when the scopes are absent, e.g. in screenshot tests) ----

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
private val sharedBoundsSpring = BoundsTransform { _, _ -> spring(Motion.DAMPING, 380f, visibilityThreshold = Rect.VisibilityThreshold) }

/** Container transform between the same game shown on two screens. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedGameBounds(key: String): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    if (Retra.reduceMotion) return this
    return with(shared) {
        this@sharedGameBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animated,
            boundsTransform = sharedBoundsSpring,
            resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
        )
    }
}

@Suppress("unused")
private val unusedSpring = Spring.StiffnessLow
