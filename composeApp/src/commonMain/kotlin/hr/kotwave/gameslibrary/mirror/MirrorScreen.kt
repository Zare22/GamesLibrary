package hr.kotwave.gameslibrary.mirror

import androidx.compose.runtime.Composable

/** The Mirror route's screen: the hosting screen on desktop, the pairing flow on the phone. */
@Composable
expect fun MirrorScreen(onBack: () -> Unit)

/** The Settings › Mirror card: "Host a Mirror" on desktop; pair row / paired card on the phone. */
@Composable
expect fun MirrorSettingsSection(onOpenMirror: () -> Unit)
