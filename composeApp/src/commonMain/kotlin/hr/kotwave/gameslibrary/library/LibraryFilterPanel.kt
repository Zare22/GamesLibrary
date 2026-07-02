package hr.kotwave.gameslibrary.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.ui.components.AppIconButton
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact
import hr.kotwave.gameslibrary.ui.theme.AppTheme

/** Display name for a [LibrarySort]. */
private val LibrarySort.label: String
    get() = when (this) {
        LibrarySort.TITLE -> "Title"
        LibrarySort.RECENTLY_ADDED -> "Recently added"
        LibrarySort.RATING -> "Rating"
    }

/**
 * The Sliders button with an active-facet count badge, opening the sort/filter panel — a bottom sheet
 * on compact layouts, a popover anchored to the button on expanded ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryFilterButton(
    filter: LibraryFilter,
    onToggleStore: (Store) -> Unit,
    onToggleStatus: (Status?) -> Unit,
    onSetSort: (LibrarySort) -> Unit,
    onReset: () -> Unit,
) {
    val compact = LocalIsCompact.current
    var open by remember { mutableStateOf(false) }

    Box {
        Box {
            AppIconButton(AppIcons.Sliders, onClick = { open = true }, contentDescription = "Sort & filter")
            if (filter.activeFacetCount > 0) {
                FacetBadge(filter.activeFacetCount, Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp))
            }
        }

        if (compact) {
            if (open) {
                ModalBottomSheet(
                    onDismissRequest = { open = false },
                    containerColor = AppTheme.tokens.colors.bg2,
                ) {
                    FilterPanelContent(
                        filter, onToggleStore, onToggleStatus, onSetSort, onReset,
                        Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp),
                    )
                }
            }
        } else {
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                modifier = Modifier.width(320.dp),
            ) {
                FilterPanelContent(
                    filter, onToggleStore, onToggleStatus, onSetSort, onReset,
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun FacetBadge(count: Int, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    Box(
        modifier.size(16.dp).clip(CircleShape).background(Brush.linearGradient(tokens.colors.brandGradient)),
        contentAlignment = Alignment.Center,
    ) {
        Text("$count", style = AppTheme.type.caption.copy(fontSize = 9.sp), color = Color.White)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanelContent(
    filter: LibraryFilter,
    onToggleStore: (Store) -> Unit,
    onToggleStatus: (Status?) -> Unit,
    onSetSort: (LibrarySort) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sort & filter", style = AppTheme.type.bodyStrong, color = tokens.colors.text)
            Spacer(Modifier.weight(1f))
            if (filter.activeFacetCount > 0 || filter.sort != LibrarySort.TITLE) {
                Text(
                    "Reset",
                    style = AppTheme.type.caption,
                    color = tokens.colors.accent,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onReset).padding(4.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("Sort")
        ChipRow {
            LibrarySort.entries.forEach { sort ->
                FilterChip(selected = filter.sort == sort, label = sort.label, onClick = { onSetSort(sort) })
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("Store")
        ChipRow {
            Store.entries.forEach { store ->
                FilterChip(selected = store in filter.stores, label = store.label, onClick = { onToggleStore(store) })
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("Status")
        ChipRow {
            Status.entries.forEach { status ->
                FilterChip(selected = status in filter.statuses, label = status.label, onClick = { onToggleStatus(status) })
            }
            FilterChip(selected = null in filter.statuses, label = "No status", onClick = { onToggleStatus(null) })
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), style = AppTheme.type.caption, color = AppTheme.tokens.colors.faint)
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
private fun FilterChip(selected: Boolean, label: String, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(999.dp)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) tokens.colors.accent.copy(alpha = 0.16f) else tokens.colors.surface)
            .border(1.dp, if (selected) tokens.colors.accent else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) Icon(AppIcons.Check, null, Modifier.size(13.dp), tint = tokens.colors.accent)
        Text(label, style = AppTheme.type.caption, color = if (selected) tokens.colors.text else tokens.colors.muted)
    }
}
