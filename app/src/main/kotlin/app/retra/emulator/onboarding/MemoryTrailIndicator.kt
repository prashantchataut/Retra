package app.retra.emulator.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Luminous Memory Trail:
 *
 * Replaces generic pagination dots with four kinetic glowing marks that
 * stretch and migrate accent color dynamically across chapter transitions.
 */
@Composable
fun MemoryTrailIndicator(
    chapterIndex: Int,
    totalChapters: Int = 4,
    activeColor: Color = OnboardingTokens.ElectricLavender,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalChapters) {
            val isActive = i == chapterIndex
            val animatedWidth by animateDpAsState(
                targetValue = if (isActive) 32.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                label = "trailWidth_$i"
            )
            val animatedColor by animateColorAsState(
                targetValue = if (isActive) activeColor else OnboardingTokens.TextMuted.copy(alpha = 0.35f),
                label = "trailColor_$i"
            )

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(animatedWidth)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }
    }
}
