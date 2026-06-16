package hr.kotwave.gameslibrary.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.kotwave.gameslibrary.ui.components.AppIconButton
import hr.kotwave.gameslibrary.ui.components.BrandWordmark
import hr.kotwave.gameslibrary.ui.components.CircularButton
import hr.kotwave.gameslibrary.ui.components.CloseButton
import hr.kotwave.gameslibrary.ui.components.DestructiveButton
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.components.PrimaryButton
import hr.kotwave.gameslibrary.ui.components.SecondaryButton
import hr.kotwave.gameslibrary.ui.components.StatusDot
import hr.kotwave.gameslibrary.ui.components.StoreBadge
import hr.kotwave.gameslibrary.ui.components.StoreBadgeRow
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.model.Status
import hr.kotwave.gameslibrary.ui.model.Store
import hr.kotwave.gameslibrary.ui.theme.AppTheme

/** Renders every backbone component with its variants. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentGalleryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CircularButton(AppIcons.ChevronLeft, onBack, contentDescription = "Back")
            Text("Component gallery", style = AppTheme.type.display.copy(fontSize = 24.sp), color = tokens.colors.text)
        }
        Spacer(Modifier.height(24.dp))

        GallerySection("Brand") {
            BrandWordmark()
        }

        GallerySection("Buttons") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton("Add to library", onClick = {}, leadingIcon = AppIcons.Check)
                SecondaryButton("Edit", onClick = {}, leadingIcon = AppIcons.Edit)
                DestructiveButton(onClick = {})
            }
        }

        GallerySection("Icon buttons") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(AppIcons.Plus, onClick = {}, accent = true)
                AppIconButton(AppIcons.Sliders, onClick = {})
                CircularButton(AppIcons.ChevronLeft, onClick = {})
                CloseButton(onClick = {})
            }
        }

        GallerySection("Icons") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AllIcons.forEach { (name, icon) -> IconCell(name, icon) }
            }
        }

        GallerySection("Status pips") {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Status.entries.forEach { status ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        StatusDot(status)
                        Text(status.label, style = AppTheme.type.caption, color = tokens.colors.muted)
                    }
                }
            }
        }

        GallerySection("Store badges") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Store.entries.forEach { StoreBadge(it) }
                }
                StoreBadgeRow(stores = Store.entries.toList(), max = 2)
            }
        }

        GallerySection("Glass surface") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassSurface(modifier = Modifier.size(140.dp, 88.dp), shape = RoundedCornerShape(18.dp)) {
                    Text("Plain", style = AppTheme.type.bodyStrong, color = tokens.colors.text, modifier = Modifier.padding(16.dp))
                }
                GlassSurface(
                    modifier = Modifier.size(140.dp, 88.dp),
                    shape = RoundedCornerShape(18.dp),
                    borderColor = tokens.colors.borderStrong,
                    glow = tokens.colors.accent,
                ) {
                    Text("Glow", style = AppTheme.type.bodyStrong, color = tokens.colors.text, modifier = Modifier.padding(16.dp))
                }
            }
        }

        GallerySection("Typography") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeSample("Display", AppTheme.type.display, "Baldur's Gate 3")
                TypeSample("Tile title", AppTheme.type.tileTitle, "Red Dead Redemption 2")
                TypeSample("Section", AppTheme.type.section, "COMPLETION STATUS")
                TypeSample("Body", AppTheme.type.body, "Track even if you already own it.")
                TypeSample("Body strong", AppTheme.type.bodyStrong, "Owned on Steam · PlayStation")
                TypeSample("Caption", AppTheme.type.caption, "2022 · FromSoftware")
                TypeSample("Numeric", AppTheme.type.numeric, "8.5")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private val AllIcons: List<Pair<String, ImageVector>> = listOf(
    "grid" to AppIcons.Grid,
    "heart" to AppIcons.Heart,
    "heartFill" to AppIcons.HeartFilled,
    "import" to AppIcons.Import,
    "settings" to AppIcons.Settings,
    "plus" to AppIcons.Plus,
    "search" to AppIcons.Search,
    "sliders" to AppIcons.Sliders,
    "chevDown" to AppIcons.ChevronDown,
    "chevLeft" to AppIcons.ChevronLeft,
    "chevRight" to AppIcons.ChevronRight,
    "close" to AppIcons.Close,
    "check" to AppIcons.Check,
    "trash" to AppIcons.Trash,
    "edit" to AppIcons.Edit,
    "sync" to AppIcons.Sync,
)

@Composable
private fun GallerySection(title: String, content: @Composable () -> Unit) {
    val tokens = AppTheme.tokens
    Column(Modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text(
            title.uppercase(),
            style = AppTheme.type.section,
            color = tokens.colors.faint,
            modifier = Modifier.padding(bottom = 14.dp),
        )
        content()
    }
}

@Composable
private fun IconCell(name: String, icon: ImageVector) {
    val tokens = AppTheme.tokens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(64.dp),
    ) {
        GlassSurface(modifier = Modifier.size(46.dp), shape = RoundedCornerShape(12.dp)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, name, Modifier.size(20.dp), tint = tokens.colors.text)
            }
        }
        Text(name, style = AppTheme.type.caption.copy(fontSize = 10.sp), color = tokens.colors.faint)
    }
}

@Composable
private fun TypeSample(label: String, style: TextStyle, sample: String) {
    val tokens = AppTheme.tokens
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(sample, style = style, color = tokens.colors.text, modifier = Modifier.weight(1f))
        Text(label, style = AppTheme.type.caption, color = tokens.colors.faint)
    }
}
