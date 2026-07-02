package hr.kotwave.gameslibrary.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.IgdbSearchResult
import hr.kotwave.gameslibrary.igdb.IgdbClient
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.igdb_unreachable_short
import hr.kotwave.gameslibrary.resources.no_matches
import hr.kotwave.gameslibrary.resources.search_hint
import hr.kotwave.gameslibrary.resources.search_result_count
import hr.kotwave.gameslibrary.resources.search_searching
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.gameMeta
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Debounced IGDB title search, shared by the Add flow and detail Re-match. Holds only the query and
 * its results; what happens on pick is the caller's concern.
 */
@Stable
class IgdbSearchState(
    private val igdbClient: IgdbClient,
    private val scope: CoroutineScope,
) {
    var query by mutableStateOf("")
        private set
    var results by mutableStateOf<List<IgdbSearchResult>>(emptyList())
        private set
    var searching by mutableStateOf(false)
        private set
    var searchFailed by mutableStateOf(false)
        private set

    private var job: Job? = null

    fun updateQuery(value: String) {
        query = value
        job?.cancel()
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            results = emptyList()
            searching = false
            searchFailed = false
            return
        }
        job = scope.launch {
            delay(300.milliseconds)
            searching = true
            searchFailed = false
            try {
                results = igdbClient.searchGames(trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                results = emptyList()
                searchFailed = true
            } finally {
                searching = false
            }
        }
    }

    /** Cancels a pending debounced search — e.g. once a result has been picked. */
    fun cancelPending() {
        job?.cancel()
    }

    fun clear() {
        job?.cancel()
        query = ""
        results = emptyList()
        searching = false
        searchFailed = false
    }
}

/** The IGDB search input box. */
@Composable
fun IgdbSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    GlassSurface(
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(15.dp),
        borderColor = tokens.colors.borderStrong,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Icon(AppIcons.Search, null, Modifier.size(18.dp), tint = tokens.colors.accent)
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(stringResource(Res.string.search_hint), style = AppTheme.type.body, color = tokens.colors.faint)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = AppTheme.type.body.copy(color = tokens.colors.text),
                    cursorBrush = SolidColor(tokens.colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text("IGDB", style = AppTheme.type.caption.copy(fontSize = 10.sp), color = tokens.colors.faint)
        }
    }
}

/** The "Searching… / N results / No matches / couldn't reach IGDB" status line. */
@Composable
fun IgdbSearchStatus(searching: Boolean, count: Int, failed: Boolean) {
    val tokens = AppTheme.tokens
    val text = when {
        searching -> stringResource(Res.string.search_searching)
        failed -> stringResource(Res.string.igdb_unreachable_short)
        count > 0 -> pluralStringResource(Res.plurals.search_result_count, count, count)
        else -> stringResource(Res.string.no_matches)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (failed) Color(0xFFF4707A) else tokens.colors.accent))
        Text(text, style = AppTheme.type.caption, color = tokens.colors.faint)
    }
}

/** A single IGDB search result row: cover, name, year · developer, and a trailing add/picked icon. */
@Composable
fun IgdbResultRow(
    result: IgdbSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.background(tokens.colors.surface).border(1.dp, tokens.colors.border, shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        CoverArt(
            title = result.name,
            coverImageId = result.coverImageId,
            modifier = Modifier.size(width = 46.dp, height = 61.dp),
            shape = RoundedCornerShape(9.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                result.name,
                style = AppTheme.type.bodyStrong,
                color = tokens.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            gameMeta(result.firstReleaseDate, result.developer)?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = AppTheme.type.caption, color = tokens.colors.faint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tokens.colors.surface)
                .border(1.dp, tokens.colors.border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (selected) AppIcons.Check else AppIcons.Plus,
                null,
                Modifier.size(15.dp),
                tint = if (selected) tokens.colors.accent else tokens.colors.muted,
            )
        }
    }
}
