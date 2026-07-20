package app.retra.emulator

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RetraLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    contentDescription: String = "Retra"
) {
    Image(
        painter = painterResource(R.drawable.retra_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription }
    )
}

@Composable
fun RetraLogoTile(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    RetraLogo(modifier = modifier, size = size, contentDescription = "Retra")
}

@Composable
fun RetraBrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    RetraLogo(modifier = modifier, size = size, contentDescription = "Retra")
}
