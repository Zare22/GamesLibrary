package hr.kotwave.gameslibrary.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Game
import hr.kotwave.gameslibrary.data.GameWithOwnerships
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.resources.Res
import hr.kotwave.gameslibrary.resources.action_delete
import hr.kotwave.gameslibrary.resources.cd_back
import hr.kotwave.gameslibrary.resources.cd_close
import hr.kotwave.gameslibrary.resources.cd_rating_clear
import hr.kotwave.gameslibrary.resources.cd_rating_lower
import hr.kotwave.gameslibrary.resources.cd_rating_raise
import hr.kotwave.gameslibrary.resources.cd_refresh_metadata
import hr.kotwave.gameslibrary.resources.cd_remove_store
import hr.kotwave.gameslibrary.resources.common_cancel
import hr.kotwave.gameslibrary.resources.detail_add_store
import hr.kotwave.gameslibrary.resources.detail_delete_body
import hr.kotwave.gameslibrary.resources.detail_delete_title
import hr.kotwave.gameslibrary.resources.detail_not_owned_body
import hr.kotwave.gameslibrary.resources.detail_not_owned_title
import hr.kotwave.gameslibrary.resources.detail_not_tracked
import hr.kotwave.gameslibrary.resources.detail_on_wishlist
import hr.kotwave.gameslibrary.resources.detail_orphan_body
import hr.kotwave.gameslibrary.resources.detail_orphan_title
import hr.kotwave.gameslibrary.resources.detail_ownership
import hr.kotwave.gameslibrary.resources.detail_platforms
import hr.kotwave.gameslibrary.resources.detail_rating_subtitle
import hr.kotwave.gameslibrary.resources.detail_rating_title
import hr.kotwave.gameslibrary.resources.detail_rematch
import hr.kotwave.gameslibrary.resources.detail_rematch_body
import hr.kotwave.gameslibrary.resources.detail_rematch_conflict
import hr.kotwave.gameslibrary.resources.detail_rematch_title
import hr.kotwave.gameslibrary.resources.detail_status
import hr.kotwave.gameslibrary.resources.detail_unrated
import hr.kotwave.gameslibrary.resources.igdb_unreachable_short
import hr.kotwave.gameslibrary.resources.library_title
import hr.kotwave.gameslibrary.igdb.IgdbImage
import hr.kotwave.gameslibrary.search.IgdbResultRow
import hr.kotwave.gameslibrary.search.IgdbSearchField
import hr.kotwave.gameslibrary.search.IgdbSearchStatus
import hr.kotwave.gameslibrary.ui.components.CircularButton
import hr.kotwave.gameslibrary.ui.components.ContentColumn
import hr.kotwave.gameslibrary.ui.components.CoverArt
import hr.kotwave.gameslibrary.ui.components.DetailMaxWidth
import hr.kotwave.gameslibrary.ui.components.actionWidth
import hr.kotwave.gameslibrary.ui.components.DestructiveButton
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.components.StatusDot
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.model.releaseYear
import hr.kotwave.gameslibrary.ui.shell.LocalIsCompact
import hr.kotwave.gameslibrary.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

private val RatingGreen = Color(0xFF7DF0B6)
private val RatingGreenBorder = Color(0xFF39D98A)
private val RatingBlue = Color(0xFFBCD4FF)
private val Amber = Color(0xFFFFD24A)
private val OrphanRed = Color(0xFFF4707A)
private val HeroHeight = 380.dp

/** Actions a detail screen invokes — wired to the [DetailViewModel], or faked in render tests. */
class DetailActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
    val onRematch: () -> Unit,
    val onDelete: () -> Unit,
    val setRating: (Double?) -> Unit,
    val setStatus: (Status) -> Unit,
    val addStore: (Store) -> Unit,
    val removeStore: (Store) -> Unit,
)

/** Game detail: cover hero + IGDB metadata + editable local state, refresh, and Orphaned Re-match. */
@Composable
fun DetailScreen(onBack: () -> Unit, viewModel: DetailViewModel = koinViewModel()) {
    val owned = viewModel.game.collectAsState().value ?: return
    DetailContent(
        owned = owned,
        igdbUnreachable = viewModel.igdbUnreachable,
        actions = DetailActions(
            onBack = onBack,
            onRefresh = viewModel::refresh,
            onRematch = viewModel::startRematch,
            onDelete = { viewModel.delete(onBack) },
            setRating = viewModel::setUserRating,
            setStatus = viewModel::setStatus,
            addStore = viewModel::addOwnership,
            removeStore = viewModel::removeOwnership,
        ),
    )
    if (viewModel.rematching) {
        RematchOverlay(viewModel)
    }
}

/** The presentational detail layout — no ViewModel, so it is render-testable. */
@Composable
fun DetailContent(owned: GameWithOwnerships, igdbUnreachable: Boolean, actions: DetailActions) {
    if (LocalIsCompact.current) {
        PhoneDetail(owned, igdbUnreachable, actions)
    } else {
        DesktopDetail(owned, igdbUnreachable, actions)
    }
}

// ---- Phone: immersive hero over a scrolling body ----

@Composable
private fun PhoneDetail(owned: GameWithOwnerships, igdbUnreachable: Boolean, actions: DetailActions) {
    var confirmDelete by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            PhoneHero(owned, actions)
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                DetailBody(owned, igdbUnreachable, actions)
                DeleteAction(onClick = { confirmDelete = true })
                Spacer(Modifier.height(12.dp).navigationBarsPadding())
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            name = owned.game.name,
            onConfirm = { confirmDelete = false; actions.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun PhoneHero(owned: GameWithOwnerships, actions: DetailActions) {
    val tokens = AppTheme.tokens
    Box(Modifier.fillMaxWidth().height(HeroHeight)) {
        CoverArt(
            title = owned.game.name,
            coverImageId = owned.game.coverImageId,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(0.dp),
            imageSize = IgdbImage.HERO,
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color(0x59080A10),
                    0.32f to Color.Transparent,
                    0.72f to Color(0x99080A10),
                    1f to tokens.colors.bg,
                ),
            ),
        )
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CircularButton(AppIcons.ChevronLeft, onClick = actions.onBack, contentDescription = stringResource(Res.string.cd_back))
            if (owned.game.igdbId != null) {
                CircularButton(AppIcons.Sync, onClick = actions.onRefresh, contentDescription = stringResource(Res.string.cd_refresh_metadata))
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp)) {
            if (owned.game.wishlist) {
                WishlistPill()
                Spacer(Modifier.height(11.dp))
            }
            Text(owned.game.name, style = AppTheme.type.display, color = tokens.colors.text)
            Spacer(Modifier.height(9.dp))
            Subline(owned.game)
        }
    }
}

// ---- Desktop: two-pane (hero left, info right) ----

@Composable
private fun DesktopDetail(owned: GameWithOwnerships, igdbUnreachable: Boolean, actions: DetailActions) {
    val tokens = AppTheme.tokens
    var confirmDelete by remember { mutableStateOf(false) }
    ContentColumn(maxWidth = DetailMaxWidth) {
        Column(Modifier.fillMaxSize().padding(start = 30.dp, end = 30.dp, top = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularButton(AppIcons.ChevronLeft, onClick = actions.onBack, contentDescription = stringResource(Res.string.cd_back))
                Text(stringResource(Res.string.library_title), style = AppTheme.type.bodyStrong, color = tokens.colors.muted)
                Text("/", style = AppTheme.type.bodyStrong, color = tokens.colors.faint)
                Text(owned.game.name, style = AppTheme.type.bodyStrong, color = tokens.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxSize()) {
                DesktopPoster(owned)
                Spacer(Modifier.width(34.dp))
                Column(
                    Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    Column {
                        Text(owned.game.name, style = AppTheme.type.display.copy(fontSize = 34.sp), color = tokens.colors.text)
                        Spacer(Modifier.height(11.dp))
                        Subline(owned.game)
                    }
                    DetailBody(owned, igdbUnreachable, actions)
                    DeleteAction(onClick = { confirmDelete = true })
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(
            name = owned.game.name,
            onConfirm = { confirmDelete = false; actions.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Desktop detail poster: a crisp contained cover over a pane-scoped blurred fill of the same art. */
@Composable
private fun DesktopPoster(owned: GameWithOwnerships) {
    val tokens = AppTheme.tokens
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.width(320.dp).fillMaxHeight().padding(bottom = 30.dp)
            .clip(shape).border(1.dp, tokens.colors.border, shape),
    ) {
        CoverArt(
            title = owned.game.name,
            coverImageId = owned.game.coverImageId,
            modifier = Modifier.matchParentSize().blur(34.dp),
            shape = shape,
        )
        Box(Modifier.matchParentSize().background(Color(0x73060810)))
        CoverArt(
            title = owned.game.name,
            coverImageId = owned.game.coverImageId,
            modifier = Modifier.align(Alignment.TopCenter).padding(18.dp).fillMaxWidth().aspectRatio(3f / 4f),
            shape = RoundedCornerShape(14.dp),
            imageSize = IgdbImage.HERO,
        )
        if (!owned.game.wishlist && owned.game.status != null) {
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(0.62f to Color.Transparent, 1f to Color(0xC7060810)),
                ),
            )
            Row(
                Modifier.align(Alignment.BottomStart).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusDot(status = owned.game.status!!)
                Text(owned.game.status!!.label(), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
            }
        }
    }
}

// ---- Shared body: rating / ownership / status (or not-owned), platforms, refresh + orphan state ----

@Composable
private fun DetailBody(owned: GameWithOwnerships, igdbUnreachable: Boolean, actions: DetailActions) {
    val game = owned.game
    if (game.orphaned) {
        OrphanedBanner(onRematch = actions.onRematch)
    }
    if (igdbUnreachable) {
        InlineNote(stringResource(Res.string.igdb_unreachable_short), OrphanRed)
    }
    if (game.wishlist) {
        NotOwnedSection()
    } else {
        RatingSection(value = game.userRating, onChange = actions.setRating)
        OwnershipSection(
            stores = owned.ownerships.map { it.store },
            onAdd = actions.addStore,
            onRemove = actions.removeStore,
        )
        StatusSection(selected = game.status, onSelect = actions.setStatus)
    }
    if (game.platforms.isNotEmpty()) {
        PlatformsSection(game.platforms.map { it.name })
    }
}

@Composable
private fun Subline(game: Game) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val parts = listOfNotNull(game.developer, releaseYear(game.firstReleaseDate)?.toString())
        parts.forEachIndexed { index, part ->
            if (index > 0) Text("•", style = AppTheme.type.body, color = tokens.colors.faint)
            Text(part, style = AppTheme.type.body, color = tokens.colors.muted)
        }
        igdbScore(game.totalRating)?.let { score ->
            Spacer(Modifier.weight(1f))
            IgdbRatingBadge(score)
        }
    }
}

@Composable
private fun IgdbRatingBadge(score: Int) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(RatingGreenBorder.copy(alpha = 0.18f), RatingGreenBorder.copy(alpha = 0.06f))))
            .border(1.dp, RatingGreenBorder.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(AppIcons.Star, null, Modifier.size(12.dp), tint = RatingGreen)
        Text("IGDB", style = AppTheme.type.caption.copy(fontSize = 10.sp), color = RatingGreen)
        Text("$score", style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = RatingGreen)
        Text("/100", style = AppTheme.type.caption.copy(fontSize = 10.sp), color = RatingGreen.copy(alpha = 0.7f))
    }
}

@Composable
private fun WishlistPill() {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Amber.copy(alpha = 0.14f))
            .border(1.dp, Amber.copy(alpha = 0.40f), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(AppIcons.HeartFilled, null, Modifier.size(13.dp), tint = Amber)
        Text(stringResource(Res.string.detail_on_wishlist), style = AppTheme.type.caption.copy(fontSize = 11.sp), color = Amber)
    }
}

// ---- Your rating ----

@Composable
private fun RatingSection(value: Double?, onChange: (Double?) -> Unit) {
    val tokens = AppTheme.tokens
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(tokens.colors.accent.copy(alpha = 0.10f), tokens.colors.accent.copy(alpha = 0.02f))))
            .border(1.dp, tokens.colors.accent.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                .background(tokens.colors.accent.copy(alpha = 0.14f))
                .border(1.dp, tokens.colors.accent.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Star, null, Modifier.size(17.dp), tint = RatingBlue)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.detail_rating_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
            Text(stringResource(Res.string.detail_rating_subtitle), style = AppTheme.type.caption.copy(fontSize = 11.5.sp), color = tokens.colors.faint)
        }
        RatingStepper(value, onChange)
    }
}

@Composable
private fun RatingStepper(value: Double?, onChange: (Double?) -> Unit) {
    val tokens = AppTheme.tokens
    var editing by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(value?.let(::formatUserRating) ?: "") }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StepButton(AppIcons.Minus, stringResource(Res.string.cd_rating_lower)) { editing = false; onChange(stepped(value, -0.5)) }
        Box(Modifier.widthIn(min = 56.dp), contentAlignment = Alignment.Center) {
            if (editing) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' }.take(4) },
                    singleLine = true,
                    textStyle = AppTheme.type.numeric.copy(color = RatingBlue, textAlign = TextAlign.Center),
                    cursorBrush = SolidColor(tokens.colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { editing = false; onChange(parseRating(text)) }),
                    modifier = Modifier.width(56.dp),
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                        text = value?.let(::formatUserRating) ?: ""
                        editing = true
                    },
                ) {
                    Text(value?.let(::formatUserRating) ?: "—", style = AppTheme.type.numeric, color = RatingBlue)
                    Text(if (value == null) stringResource(Res.string.detail_unrated) else "/ 10", style = AppTheme.type.caption.copy(fontSize = 10.sp), color = tokens.colors.faint)
                }
            }
        }
        StepButton(AppIcons.Plus, stringResource(Res.string.cd_rating_raise)) { editing = false; onChange(stepped(value, 0.5)) }
        StepButton(
            AppIcons.Close,
            stringResource(Res.string.cd_rating_clear),
            modifier = Modifier.alpha(if (value != null) 1f else 0f),
            enabled = value != null,
        ) { editing = false; onChange(null) }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val tokens = AppTheme.tokens
    Box(
        modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
            .background(tokens.colors.surfaceRaised)
            .border(1.dp, tokens.colors.borderStrong, RoundedCornerShape(9.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, Modifier.size(14.dp), tint = tokens.colors.text)
    }
}

// ---- Ownership ----

@Composable
private fun OwnershipSection(stores: List<Store>, onAdd: (Store) -> Unit, onRemove: (Store) -> Unit) {
    val tokens = AppTheme.tokens
    SectionHeader(stringResource(Res.string.detail_ownership))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(tokens.colors.surface).border(1.dp, tokens.colors.border, RoundedCornerShape(16.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (stores.isEmpty()) {
            Text(stringResource(Res.string.detail_not_tracked), style = AppTheme.type.caption, color = tokens.colors.faint)
        } else {
            stores.forEach { store -> OwnedStoreRow(store, onRemove = { onRemove(store) }) }
        }
        val addable = Store.entries.filter { it !in stores }
        if (addable.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(stringResource(Res.string.detail_add_store), style = AppTheme.type.section.copy(fontSize = 10.sp), color = tokens.colors.faint)
            AddStoreChips(addable, onAdd)
        }
    }
}

@Composable
private fun OwnedStoreRow(store: Store, onRemove: () -> Unit) {
    val tokens = AppTheme.tokens
    val accent = tokens.store.accent(store)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(Color(0x05FFFFFF)).border(1.dp, tokens.colors.border, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)).border(1.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(store.glyph, style = AppTheme.type.brand.copy(fontSize = 13.sp), color = tokens.store.glyph(store))
        }
        Text(store.label, style = AppTheme.type.bodyStrong, color = tokens.colors.text, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.Close, stringResource(Res.string.cd_remove_store, store.label), Modifier.size(15.dp), tint = tokens.colors.faint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddStoreChips(stores: List<Store>, onAdd: (Store) -> Unit) {
    val tokens = AppTheme.tokens
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stores.forEach { store ->
            Row(
                Modifier.clip(RoundedCornerShape(11.dp)).background(tokens.colors.surface)
                    .border(1.dp, tokens.colors.border, RoundedCornerShape(11.dp))
                    .clickable { onAdd(store) }.padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(AppIcons.Plus, null, Modifier.size(13.dp), tint = tokens.store.accent(store))
                Text(store.label, style = AppTheme.type.bodyStrong.copy(fontSize = 13.sp), color = tokens.colors.muted)
            }
        }
    }
}

// ---- Status ----

@Composable
private fun StatusSection(selected: Status?, onSelect: (Status) -> Unit) {
    SectionHeader(stringResource(Res.string.detail_status))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Status.entries.forEach { status ->
            StatusCell(Modifier.weight(1f), status = status, active = selected == status) { onSelect(status) }
        }
    }
}

@Composable
private fun StatusCell(modifier: Modifier, status: Status, active: Boolean, onClick: () -> Unit) {
    val tokens = AppTheme.tokens
    val color = tokens.status.color(status)
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (active) color.copy(alpha = 0.12f) else tokens.colors.surface)
            .border(1.dp, if (active) color.copy(alpha = 0.55f) else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StatusDot(status = status, size = 9.dp, bordered = false)
        Text(
            status.label(),
            style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
            color = if (active) tokens.colors.text else tokens.colors.muted,
        )
    }
}

// ---- Wishlist (not owned) ----

@Composable
private fun NotOwnedSection() {
    val tokens = AppTheme.tokens
    SectionHeader(stringResource(Res.string.detail_ownership))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Amber.copy(alpha = 0.10f), Amber.copy(alpha = 0.02f))))
            .border(1.dp, Amber.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                .background(Amber.copy(alpha = 0.14f)).border(1.dp, Amber.copy(alpha = 0.40f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(AppIcons.HeartFilled, null, Modifier.size(19.dp), tint = Amber)
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.detail_not_owned_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
            Text(
                stringResource(Res.string.detail_not_owned_body),
                style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                color = tokens.colors.faint,
            )
        }
    }
}

// ---- Platforms ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlatformsSection(platforms: List<String>) {
    val tokens = AppTheme.tokens
    SectionHeader(stringResource(Res.string.detail_platforms))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        platforms.forEach { name ->
            Text(
                name,
                style = AppTheme.type.bodyStrong.copy(fontSize = 12.sp),
                color = tokens.colors.muted,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(tokens.colors.surface)
                    .border(1.dp, tokens.colors.border, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

// ---- Orphaned banner + Re-match overlay ----

@Composable
private fun OrphanedBanner(onRematch: () -> Unit) {
    val tokens = AppTheme.tokens
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(OrphanRed.copy(alpha = 0.08f)).border(1.dp, OrphanRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(11.dp))
                    .background(OrphanRed.copy(alpha = 0.14f)).border(1.dp, OrphanRed.copy(alpha = 0.40f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AppIcons.Sync, null, Modifier.size(18.dp), tint = OrphanRed)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.detail_orphan_title), style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                Text(
                    stringResource(Res.string.detail_orphan_body),
                    style = AppTheme.type.caption.copy(fontSize = 11.5.sp),
                    color = tokens.colors.muted,
                )
            }
        }
        SecondaryButton(text = stringResource(Res.string.detail_rematch), onClick = onRematch, leadingIcon = AppIcons.Search, modifier = Modifier.actionWidth())
    }
}

@Composable
private fun RematchOverlay(vm: DetailViewModel) {
    val tokens = AppTheme.tokens
    val scrim = remember { MutableInteractionSource() }
    val card = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Color(0xB2060810))
            .clickable(interactionSource = scrim, indication = null, onClick = vm::cancelRematch),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(20.dp)
                .clip(RoundedCornerShape(22.dp)).background(tokens.colors.bg2)
                .border(1.dp, tokens.colors.borderStrong, RoundedCornerShape(22.dp))
                .clickable(interactionSource = card, indication = null, onClick = {})
                .padding(20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.detail_rematch_title), style = AppTheme.type.bodyStrong.copy(fontSize = 16.sp), color = tokens.colors.text)
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = vm::cancelRematch),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(AppIcons.Close, stringResource(Res.string.cd_close), Modifier.size(16.dp), tint = tokens.colors.muted)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(Res.string.detail_rematch_body), style = AppTheme.type.caption, color = tokens.colors.faint)
            Spacer(Modifier.height(14.dp))
            IgdbSearchField(value = vm.rematchSearch.query, onValueChange = vm.rematchSearch::updateQuery)
            if (vm.rematchConflict) {
                Spacer(Modifier.height(10.dp))
                InlineNote(stringResource(Res.string.detail_rematch_conflict), Amber)
            }
            if (vm.rematchSearch.query.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                IgdbSearchStatus(
                    searching = vm.rematchSearch.searching || vm.rematchPicking,
                    count = vm.rematchSearch.results.size,
                    failed = vm.rematchSearch.searchFailed,
                )
                Spacer(Modifier.height(6.dp))
                vm.rematchSearch.results.forEach { result ->
                    IgdbResultRow(result = result, onClick = { vm.pickRematch(result) })
                }
            }
        }
    }
}

// ---- Delete ----

@Composable
private fun DeleteAction(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        DestructiveButton(onClick = onClick)
    }
}

@Composable
private fun ConfirmDeleteDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val tokens = AppTheme.tokens
    val scrim = remember { MutableInteractionSource() }
    val card = remember { MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Color(0xB2060810))
            .clickable(interactionSource = scrim, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 380.dp).padding(28.dp)
                .clip(RoundedCornerShape(20.dp)).background(tokens.colors.bg2)
                .border(1.dp, tokens.colors.borderStrong, RoundedCornerShape(20.dp))
                .clickable(interactionSource = card, indication = null, onClick = {})
                .padding(22.dp),
        ) {
            Text(stringResource(Res.string.detail_delete_title), style = AppTheme.type.bodyStrong.copy(fontSize = 17.sp), color = tokens.colors.text)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.detail_delete_body, name),
                style = AppTheme.type.body.copy(fontSize = 13.sp),
                color = tokens.colors.muted,
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                SecondaryButton(text = stringResource(Res.string.common_cancel), onClick = onDismiss, modifier = Modifier.weight(1f))
                PrimaryButton(text = stringResource(Res.string.action_delete), onClick = onConfirm, leadingIcon = AppIcons.Trash, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ---- Small shared pieces ----

@Composable
private fun SectionHeader(label: String) {
    Text(
        label.uppercase(),
        style = AppTheme.type.section.copy(fontSize = 12.sp),
        color = AppTheme.tokens.colors.faint,
        modifier = Modifier.padding(bottom = 11.dp),
    )
}

@Composable
private fun InlineNote(text: String, accent: Color) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.10f)).border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = AppTheme.type.caption, color = AppTheme.tokens.colors.muted)
    }
}

// ---- Formatting ----

/** One-decimal display of a 0.0–10.0 score, without platform String.format. */
private fun formatUserRating(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

/** Steps a nullable rating by ±0.5 within 0.0–10.0; stepping down from Unrated stays Unrated. */
private fun stepped(current: Double?, delta: Double): Double? {
    if (current == null) return if (delta > 0) 0.5 else null
    val tenths = ((current + delta).coerceIn(0.0, 10.0) * 10).roundToInt()
    return tenths / 10.0
}

private fun parseRating(text: String): Double? {
    val parsed = text.toDoubleOrNull() ?: return null
    return ((parsed.coerceIn(0.0, 10.0)) * 10).roundToInt() / 10.0
}

private fun igdbScore(totalRating: Double?): Int? = totalRating?.roundToInt()
