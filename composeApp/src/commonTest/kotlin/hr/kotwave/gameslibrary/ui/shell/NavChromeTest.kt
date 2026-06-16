package hr.kotwave.gameslibrary.ui.shell

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class NavChromeTest {

    @Test
    fun compact_widths_use_bottom_nav() {
        assertEquals(NavChrome.BottomNav, navChromeFor(360.dp))
        assertEquals(NavChrome.BottomNav, navChromeFor(440.dp))
        assertEquals(NavChrome.BottomNav, navChromeFor(719.dp))
    }

    @Test
    fun wide_widths_use_rail() {
        assertEquals(NavChrome.Rail, navChromeFor(720.dp))
        assertEquals(NavChrome.Rail, navChromeFor(1280.dp))
    }
}
