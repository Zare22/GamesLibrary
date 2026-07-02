package hr.kotwave.gameslibrary.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.IgdbGame
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.add_already
import hr.kotwave.gameslibrary.resources.add_already_stores
import hr.kotwave.gameslibrary.resources.add_change
import hr.kotwave.gameslibrary.resources.add_loading_details
import hr.kotwave.gameslibrary.resources.add_manual_link
import hr.kotwave.gameslibrary.resources.add_manual_query
import hr.kotwave.gameslibrary.resources.add_own
import hr.kotwave.gameslibrary.resources.add_search_prompt
import hr.kotwave.gameslibrary.resources.add_section_adding
import hr.kotwave.gameslibrary.resources.add_step_own
import hr.kotwave.gameslibrary.resources.add_step_status
import hr.kotwave.gameslibrary.resources.add_subtitle
import hr.kotwave.gameslibrary.resources.add_subtitle_desktop
import hr.kotwave.gameslibrary.resources.add_title
import hr.kotwave.gameslibrary.resources.add_title_hint
import hr.kotwave.gameslibrary.resources.add_to_library
import hr.kotwave.gameslibrary.resources.add_to_wishlist
import hr.kotwave.gameslibrary.resources.add_wishlist
import hr.kotwave.gameslibrary.resources.add_wishlist_hint
import hr.kotwave.gameslibrary.resources.common_done
import hr.kotwave.gameslibrary.resources.similar_title_warning
import hr.kotwave.gameslibrary.search.IgdbResultRow
import hr.kotwave.gameslibrary.search.IgdbSearchField
import hr.kotwave.gameslibrary.search.IgdbSearchStatus
import hr.kotwave.gameslibrary.ui.components.CloseButton
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.GlowBox
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.StatusDot
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.gameMeta
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

private val Amber = Color(0xFFFFD24A)

/** Phone full-screen manual Add. */
@Composable
fun AddGameScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = AppTheme.tokens
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        CloseButton(onClick = onClose)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(Res.string.add_title), style = AppTheme.type.display, color = tokens.colors.text)
        Spacer(Modifier.height(3.dp))
        Text(stringResource(Res.string.add_subtitle), style = AppTheme.type.body, color = tokens.colors.faint)
        Spacer(Modifier.height(18.dp))
        AddGameContent(onDismiss = onClose, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}

/** Desktop Add: centered modal card over a dimmed scrim. */
@Composable
fun AddGameModal(onDismiss: () -> Unit) {
    val tokens = AppTheme.tokens
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x9E06080C))
            .clickable(interactionSource = scrimInteraction, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier
                .width(560.dp)
                .heightIn(max = 760.dp)
                .clickable(interactionSource = cardInteraction, indication = null, onClick = {}),
            shape = RoundedCornerShape(22.dp),
            fill = tokens.colors.bg2,
            borderColor = tokens.colors.borderStrong,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(Res.string.add_title), style = AppTheme.type.display, color = tokens.colors.text)
                        Spacer(Modifier.height(3.dp))
                        Text(stringResource(Res.string.add_subtitle_desktop), style = AppTheme.type.body, color = tokens.colors.faint)
                    }
                    Spacer(Modifier.width(12.dp))
                    CloseButton(onClick = onDismiss)
                }
                HorizontalDivider(color = tokens.colors.border)
                AddGameContent(
                    onDismiss = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                )
            }
        }
    }
}

/** IGDB search up top, then the adding card for the picked result (or the manual fallback). */
@Composable
fun AddGameContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    state: AddGameState = rememberAddGameState(),
) {
    Column(modifier) {
        IgdbSearchField(value = state.query, onValueChange = state::updateQuery)

        if (state.query.isNotBlank() && !state.collapsed) {
            Spacer(Modifier.height(12.dp))
            IgdbSearchStatus(searching = state.searching, count = state.results.size, failed = state.searchFailed)
            Spacer(Modifier.height(6.dp))
            state.results.forEach { result ->
                IgdbResultRow(
                    result = result,
                    selected = state.selected?.igdbId == result.igdbId,
                    onClick = { state.selectResult(result) },
                )
            }
            if (!state.searching && state.results.isEmpty() && !state.searchFailed) {
                Spacer(Modifier.height(8.dp))
                ManualLink(text = stringResource(Res.string.add_manual_query, state.query.trim()), onClick = state::addManually)
            }
        } else if (state.query.isBlank() && !state.configuring) {
            SearchPrompt(onAddManually = state::addManually)
        }

        if (state.loadingSelection) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.add_loading_details), style = AppTheme.type.caption, color = AppTheme.tokens.colors.faint)
        }

        if (state.collapsed) {
            Spacer(Modifier.height(16.dp))
            SectionDivider(stringResource(Res.string.add_section_adding))
            Spacer(Modifier.height(12.dp))
            AddingCard(state = state, onDismiss = onDismiss)
        }
    }
}

/** The gradient/glow card holding the cover header (or manual title) and the own/wishlist form. */
@Composable
private fun AddingCard(state: AddGameState, onDismiss: () -> Unit) {
    val tokens = AppTheme.tokens
    val wishlist = state.mode == AddMode.WISHLIST
    val game = state.selected
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        tokens.colors.accent.copy(alpha = 0.12f),
                        tokens.colors.brandGradient.last().copy(alpha = 0.06f),
                    ),
                ),
            )
            .border(1.dp, tokens.colors.accent.copy(alpha = 0.30f), shape)
            .padding(16.dp),
    ) {
        if (game != null) {
            MatchedHeader(game = game, onChange = state::changeSelection)
        } else {
            TitleField(value = state.title, onValueChange = state::updateTitle)
            state.similarTitle?.let { existing ->
                Spacer(Modifier.height(10.dp))
                SimilarTitleWarning(existing)
            }
        }
        Spacer(Modifier.height(16.dp))

        OwnWishlistSegment(mode = state.mode, onSelect = state::selectMode)
        Spacer(Modifier.height(8.dp))
        if (wishlist) {
            WishlistHint()
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(8.dp))
            StepLabel(1, stringResource(Res.string.add_step_own))
            Spacer(Modifier.height(9.dp))
            StorePicker(selected = state.selectedStores, onToggle = state::toggleStore)
            Spacer(Modifier.height(18.dp))
            StepLabel(2, stringResource(Res.string.add_step_status))
            Spacer(Modifier.height(9.dp))
            StatusSelector(selected = state.status, onSelect = state::selectStatus)
            Spacer(Modifier.height(18.dp))
        }

        if (state.alreadyInLibrary) {
            AlreadyInLibraryBanner(wishlist = wishlist, stores = state.selectedStores)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = stringResource(Res.string.common_done), onClick = onDismiss, leadingIcon = AppIcons.Check, modifier = Modifier.fillMaxWidth())
        } else {
            PrimaryButton(
                text = if (wishlist) stringResource(Res.string.add_to_wishlist) else stringResource(Res.string.add_to_library),
                onClick = { state.save(onDismiss) },
                leadingIcon = AppIcons.Check,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MatchedHeader(game: IgdbGame, onChange: () -> Unit) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        CoverArt(
            title = game.name,
            coverImageId = game.coverImageId,
            modifier = Modifier.size(width = 54.dp, height = 72.dp),
            shape = RoundedCornerShape(10.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                game.name,
                style = AppTheme.type.bodyStrong,
                color = tokens.colors.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            gameMeta(game.firstReleaseDate, game.developer)?.let {
                Spacer(Modifier.height(3.dp))
                Text(it, style = AppTheme.type.caption, color = tokens.colors.muted)
            }
        }
        Text(
            stringResource(Res.string.add_change),
            style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
            color = tokens.colors.accent,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onChange).padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SectionDivider(label: String) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = tokens.colors.border)
        Text(label.uppercase(), style = AppTheme.type.section.copy(fontSize = 11.sp), color = tokens.colors.faint)
        HorizontalDivider(Modifier.weight(1f), color = tokens.colors.border)
    }
}

@Composable
private fun SearchPrompt(onAddManually: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.add_search_prompt), style = AppTheme.type.body, color = tokens.colors.faint)
        Spacer(Modifier.height(8.dp))
        ManualLink(text = stringResource(Res.string.add_manual_link), onClick = onAddManually)
    }
}

@Composable
private fun ManualLink(text: String, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(AppIcons.Edit, null, Modifier.size(14.dp), tint = tokens.colors.accent)
        Text(text, style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = tokens.colors.accent)
    }
}

@Composable
private fun AlreadyInLibraryBanner(wishlist: Boolean, stores: Set<Store>) {
    val tokens = AppTheme.tokens
    val message = if (wishlist || stores.isEmpty()) {
        stringResource(Res.string.add_already)
    } else {
        stringResource(Res.string.add_already_stores, stores.joinToString { it.label })
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.colors.accent.copy(alpha = 0.10f))
            .border(1.dp, tokens.colors.accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = tokens.colors.accent)
        Text(message, style = AppTheme.type.caption, color = tokens.colors.muted)
    }
}

@Composable
private fun TitleField(value: String, onValueChange: (String) -> Unit) {
    val tokens = AppTheme.tokens
    GlassSurface(
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(15.dp),
        borderColor = tokens.colors.borderStrong,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Icon(AppIcons.Edit, null, Modifier.size(18.dp), tint = tokens.colors.accent)
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(stringResource(Res.string.add_title_hint), style = AppTheme.type.body, color = tokens.colors.faint)
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
        }
    }
}

@Composable
private fun SimilarTitleWarning(existing: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Amber.copy(alpha = 0.08f))
            .border(1.dp, Amber.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(AppIcons.Heart, null, Modifier.size(14.dp), tint = Amber)
        Text(
            stringResource(Res.string.similar_title_warning, existing),
            style = AppTheme.type.caption,
            color = AppTheme.tokens.colors.muted,
        )
    }
}

@Composable
private fun OwnWishlistSegment(mode: AddMode, onSelect: (AddMode) -> Unit) {
    val tokens = AppTheme.tokens
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x40000000))
            .border(1.dp, tokens.colors.border, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SegmentCell(Modifier.weight(1f), selected = mode == AddMode.OWN, icon = AppIcons.Check, label = stringResource(Res.string.add_own)) {
            onSelect(AddMode.OWN)
        }
        SegmentCell(Modifier.weight(1f), selected = mode == AddMode.WISHLIST, icon = AppIcons.Heart, label = stringResource(Res.string.add_wishlist)) {
            onSelect(AddMode.WISHLIST)
        }
    }
}

@Composable
private fun SegmentCell(
    modifier: Modifier,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val tokens = AppTheme.tokens
    val accent = tokens.colors.accent
    val shape = RoundedCornerShape(11.dp)
    val color = if (selected) tokens.colors.text else tokens.colors.muted
    Row(
        modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.18f), tokens.colors.brandGradient.last().copy(alpha = 0.10f))))
                        .border(1.dp, accent.copy(alpha = 0.45f), shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint = color)
        Text(label, style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = color)
    }
}

@Composable
private fun WishlistHint() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(AppIcons.HeartFilled, null, Modifier.size(13.dp), tint = Amber)
        Text(
            stringResource(Res.string.add_wishlist_hint),
            style = AppTheme.type.caption,
            color = AppTheme.tokens.colors.faint,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorePicker(selected: Set<Store>, onToggle: (Store) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Store.entries.forEach { store ->
            StoreChip(store = store, selected = store in selected) { onToggle(store) }
        }
    }
}

@Composable
private fun StoreChip(store: Store, selected: Boolean, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val accent = tokens.store.accent(store)
    val shape = RoundedCornerShape(12.dp)
    GlowBox(
        glow = if (selected && tokens.store.glows(store)) accent else null,
        shape = shape,
        glowRadius = 12.dp,
        glowAlpha = 0.35f,
    ) {
        Row(
            Modifier
                .clip(shape)
                .background(if (selected) accent.copy(alpha = 0.10f) else tokens.colors.surface)
                .border(1.dp, if (selected) accent.copy(alpha = 0.55f) else tokens.colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                Text(store.glyph, style = AppTheme.type.brand.copy(fontSize = 10.sp), color = tokens.store.glyph(store))
            }
            Text(
                store.label,
                style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp),
                color = if (selected) tokens.colors.text else tokens.colors.muted,
            )
            if (selected) {
                Icon(AppIcons.Check, null, Modifier.size(14.dp), tint = accent)
            }
        }
    }
}

@Composable
private fun StatusSelector(selected: Status, onSelect: (Status) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Status.entries.forEach { status ->
            StatusCell(Modifier.weight(1f), status = status, selected = selected == status) { onSelect(status) }
        }
    }
}

@Composable
private fun StatusCell(modifier: Modifier, status: Status, selected: Boolean, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (selected) Color(0x297C8696) else tokens.colors.surface)
            .border(1.dp, if (selected) Color(0x997C8696) else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusDot(status = status, size = 8.dp, bordered = false)
        Text(
            status.label(),
            style = AppTheme.type.caption.copy(fontSize = 11.sp),
            color = if (selected) tokens.colors.text else tokens.colors.muted,
        )
    }
}

@Composable
private fun StepLabel(number: Int, text: String) {
    val tokens = AppTheme.tokens
    val accent = tokens.colors.accent
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.20f))
                .border(1.dp, accent.copy(alpha = 0.50f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = AppTheme.type.caption.copy(fontSize = 10.sp), color = Color(0xFFBCD4FF))
        }
        Text(text.uppercase(), style = AppTheme.type.section.copy(fontSize = 11.sp), color = accent)
    }
}
