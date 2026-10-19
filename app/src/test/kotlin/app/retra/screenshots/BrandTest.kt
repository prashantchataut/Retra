package app.retra.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.retra.R
import app.retra.ui.components.Mochi
import app.retra.ui.components.MochiMood
import app.retra.ui.components.Wordmark
import app.retra.ui.theme.Retra
import app.retra.ui.theme.RetraType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Launcher icon (full, circle mask, themed/monochrome), wordmark and Mochi's moods. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = android.app.Application::class, qualifiers = "w393dp-h560dp-xxhdpi")
class BrandTest(private val dark: Boolean) {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }

    @get:Rule val compose = createComposeRule()

    @Test fun brand() = compose.snap("brand", dark) {
        val c = Retra.colors
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Wordmark(tileSize = 56.dp)
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(RoundedCornerShape(28.dp))
                Icon(CircleShape)
                Box(Modifier.size(96.dp).clip(CircleShape).background(if (c.isDark) Color(0xFF2C2A3A) else Color(0xFFE6E0F2)), contentAlignment = Alignment.Center) {
                    Image(
                        painterResource(R.drawable.ic_launcher_monochrome), "Themed icon", Modifier.size(144.dp),
                        colorFilter = ColorFilter.tint(if (c.isDark) Color(0xFFD8CCFF) else Color(0xFF4A3A78)),
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MochiMood.entries.forEach { m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Mochi(mood = m, size = 76.dp)
                        Text(m.name.lowercase(), style = RetraType.caption, color = c.inkMuted)
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Icon(shape: androidx.compose.ui.graphics.Shape) {
        Box(Modifier.size(96.dp).clip(shape), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(144.dp))
            Image(painterResource(R.drawable.ic_launcher_foreground), "Retra icon", Modifier.size(144.dp))
        }
    }
}
