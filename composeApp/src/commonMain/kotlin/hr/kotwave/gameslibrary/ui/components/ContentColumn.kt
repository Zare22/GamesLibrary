package hr.kotwave.gameslibrary.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact

/** Max width of single-column form/reading screens on expanded layouts. */
val ContentMaxWidth = 820.dp

/** Max width of the two-pane Detail screen on expanded layouts. */
val DetailMaxWidth = 1080.dp

/**
 * Centers form/reading content at [maxWidth] on expanded layouts; passes through full-width on
 * compact (correct for touch). Grids opt out — they consume extra width with more columns.
 */
@Composable
fun ContentColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ContentMaxWidth,
    content: @Composable BoxScope.() -> Unit,
) {
    val compact = LocalIsCompact.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .fillMaxHeight()
                .then(if (compact) Modifier.fillMaxWidth() else Modifier.widthIn(max = maxWidth).fillMaxWidth()),
            content = content,
        )
    }
}

/** Full-width on compact (touch), intrinsic on expanded — for action buttons on wide form screens. */
@Composable
fun Modifier.actionWidth(): Modifier =
    if (LocalIsCompact.current) this.fillMaxWidth() else this
