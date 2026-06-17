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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.data.Status
import hr.kotwave.gameslibrary.data.Store
import hr.kotwave.gameslibrary.ui.components.CloseButton
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.GlowBox
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.StatusDot
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.glyph
import hr.kotwave.gameslibrary.ui.model.label
import hr.kotwave.gameslibrary.ui.theme.AppTheme

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
        Text("Add a game", style = AppTheme.type.display, color = tokens.colors.text)
        Spacer(Modifier.height(3.dp))
        Text("Add a game by hand — match it to IGDB later.", style = AppTheme.type.body, color = tokens.colors.faint)
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
                        Text("Add a game", style = AppTheme.type.display, color = tokens.colors.text)
                        Spacer(Modifier.height(3.dp))
                        Text("Add a game by hand.", style = AppTheme.type.body, color = tokens.colors.faint)
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

/** Shared manual-add form: title, own/wishlist branch, store picker + status (own only), save. */
@Composable
fun AddGameContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    state: AddGameState = rememberAddGameState(),
) {
    val wishlist = state.mode == AddMode.WISHLIST
    Column(modifier) {
        TitleField(value = state.title, onValueChange = state::updateTitle)
        state.similarTitle?.let { existing ->
            Spacer(Modifier.height(10.dp))
            SimilarTitleWarning(existing)
        }
        Spacer(Modifier.height(16.dp))

        OwnWishlistSegment(mode = state.mode, onSelect = state::selectMode)
        Spacer(Modifier.height(8.dp))

        if (wishlist) {
            WishlistHint()
            Spacer(Modifier.height(8.dp))
        } else {
            Spacer(Modifier.height(8.dp))
            StepLabel(1, "Where do you own it?")
            Spacer(Modifier.height(9.dp))
            StorePicker(selected = state.selectedStores, onToggle = state::toggleStore)
            Spacer(Modifier.height(18.dp))
            StepLabel(2, "Initial status")
            Spacer(Modifier.height(9.dp))
            StatusSelector(selected = state.status, onSelect = state::selectStatus)
            Spacer(Modifier.height(18.dp))
        }

        PrimaryButton(
            text = if (wishlist) "Add to wishlist" else "Add to library",
            onClick = { state.save(onDismiss) },
            leadingIcon = AppIcons.Check,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )
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
                    Text("Game title", style = AppTheme.type.body, color = tokens.colors.faint)
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
            "“$existing” is already in your library — you can add it anyway.",
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
        SegmentCell(Modifier.weight(1f), selected = mode == AddMode.OWN, icon = AppIcons.Check, label = "I own it") {
            onSelect(AddMode.OWN)
        }
        SegmentCell(Modifier.weight(1f), selected = mode == AddMode.WISHLIST, icon = AppIcons.Heart, label = "Wishlist") {
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
            "Wishlist hides store & status — you don't own it yet.",
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
            status.label,
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
