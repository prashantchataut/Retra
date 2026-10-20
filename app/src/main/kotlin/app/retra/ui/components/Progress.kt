package app.retra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.Motion
import app.retra.ui.theme.Radius
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType

/** Thin pill progress bar (downloads). */
@Composable
fun ProgressPill(progress: Float, modifier: Modifier = Modifier) {
    val c = Retra.colors
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), Motion.smooth(Retra.reduceMotion), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(Radius.pillShape)
            .background(c.inkFaint)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(p)
                .clip(Radius.pillShape)
                .background(Brush.horizontalGradient(listOf(c.accent.copy(alpha = 0.7f), c.accent))),
        )
    }
}

/**
 * The patching moment: a ring fills around Mochi (bouncing while working, happy when done).
 */
@Composable
fun PatchProgressRing(progress: Float, done: Boolean, modifier: Modifier = Modifier, size: Dp = 168.dp) {
    val c = Retra.colors
    val p by animateFloatAsState(if (done) 1f else progress.coerceIn(0f, 1f), Motion.smooth(Retra.reduceMotion), label = "ring")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size).semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(size)) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2
                drawArc(c.inkFaint, 0f, 360f, false, topLeft = Offset(inset, inset), size = Size(this.size.width - stroke, this.size.height - stroke), style = Stroke(stroke))
                drawArc(
                    Brush.sweepGradient(listOf(c.accent.copy(alpha = 0.55f), c.accent, c.accent.copy(alpha = 0.55f))),
                    -90f, 360f * p, false,
                    topLeft = Offset(inset, inset), size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Mochi(mood = if (done) MochiMood.HAPPY else MochiMood.LOADING, size = size * 0.58f)
        }
        Spacer(Modifier.height(12.dp))
        Text(if (done) "Ready to play!" else "${(p * 100).toInt()} %", style = RetraType.headline, color = c.ink)
    }
}
