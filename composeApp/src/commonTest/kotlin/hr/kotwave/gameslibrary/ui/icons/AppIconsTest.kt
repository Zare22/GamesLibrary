package hr.kotwave.gameslibrary.ui.icons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconsTest {

    @Test
    fun all_backbone_icons_parse_and_build() {
        val icons = listOf(
            AppIcons.Grid, AppIcons.Heart, AppIcons.HeartFilled, AppIcons.Import, AppIcons.Settings,
            AppIcons.Plus, AppIcons.Search, AppIcons.Sliders, AppIcons.ChevronDown, AppIcons.ChevronLeft,
            AppIcons.ChevronRight, AppIcons.Close, AppIcons.Check, AppIcons.Trash, AppIcons.Edit, AppIcons.Sync,
        )
        assertEquals(16, icons.size)
        assertTrue(icons.all { it.name.isNotBlank() })
    }
}
