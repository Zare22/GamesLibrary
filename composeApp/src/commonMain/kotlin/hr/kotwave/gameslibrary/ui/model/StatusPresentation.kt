package hr.kotwave.gameslibrary.ui.model

import hr.kotwave.gameslibrary.data.Status

/** Display name for a [Status]. */
val Status.label: String
    get() = when (this) {
        Status.BACKLOG -> "Backlog"
        Status.PLAYING -> "Playing"
        Status.COMPLETED -> "Completed"
        Status.DROPPED -> "Dropped"
    }
