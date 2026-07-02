package hr.kotwave.gameslibrary.ui.model

import androidx.compose.runtime.Composable
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.status_backlog
import hr.kotwave.gameslibrary.resources.status_completed
import hr.kotwave.gameslibrary.resources.status_dropped
import hr.kotwave.gameslibrary.resources.status_playing
import org.jetbrains.compose.resources.stringResource

/** Display name for a [Status]. */
@Composable
fun Status.label(): String = when (this) {
    Status.BACKLOG -> stringResource(Res.string.status_backlog)
    Status.PLAYING -> stringResource(Res.string.status_playing)
    Status.COMPLETED -> stringResource(Res.string.status_completed)
    Status.DROPPED -> stringResource(Res.string.status_dropped)
}
