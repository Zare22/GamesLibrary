package hr.kotwave.gameslibrary.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import hr.kotwave.gameslibrary.ui.components.GlassSurface
import hr.kotwave.gameslibrary.ui.icons.AppIcons
import hr.kotwave.gameslibrary.ui.theme.AppTheme

/** Settings; the developer entry to the component gallery. */
@Composable
fun SettingsScreen(
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = AppTheme.tokens
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text("Settings", style = AppTheme.type.display, color = tokens.colors.text)
        Spacer(Modifier.height(20.dp))
        Text(
            "DEVELOPER",
            style = AppTheme.type.section,
            color = tokens.colors.faint,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )
        SettingsRow(
            icon = AppIcons.Grid,
            title = "Component gallery",
            subtitle = "Every reusable component",
            onClick = onOpenGallery,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val tokens = AppTheme.tokens
    GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                Modifier.size(36.dp),
                contentAlignment = Alignment.Center,
            ) {
                GlassSurface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(11.dp),
                    fill = tokens.colors.surfaceRaised,
                ) {}
                Icon(icon, null, Modifier.size(18.dp), tint = tokens.colors.muted)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = AppTheme.type.bodyStrong, color = tokens.colors.text)
                Text(subtitle, style = AppTheme.type.caption, color = tokens.colors.faint)
            }
            Icon(AppIcons.ChevronRight, null, Modifier.size(17.dp), tint = tokens.colors.faint)
        }
    }
}
