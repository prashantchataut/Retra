package app.retra.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.retra.ui.theme.Space

/** Vertical padding for a full-screen scrolling list: status bar + [top] above, dock below. */
@Composable
fun listPadding(top: Dp = Space.m, bottomExtra: Dp = 0.dp): PaddingValues {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val dock = LocalDockPadding.current
    return PaddingValues(top = insets.calculateTopPadding() + top, bottom = dock.calculateBottomPadding() + bottomExtra)
}

/** Horizontal gutter including display cutouts (landscape). */
@Composable
fun gutter(): PaddingValues {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val dir = LocalLayoutDirection.current
    return PaddingValues(start = insets.calculateStartPadding(dir) + Space.gutter, end = insets.calculateEndPadding(dir) + Space.gutter)
}
