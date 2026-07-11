package hr.kotwave.gameslibrary.psn

import androidx.compose.runtime.Composable

/**
 * The platform browser leg of PSN sign-in: opens [signInUrl], offers a tap-through to [npssoUrl]
 * (the page showing `{"npsso":"…"}` once signed in), and surfaces the pasted value via [onNpsso].
 * Both platforms open the system browser (a Custom Tab on Android, the desktop system browser) and
 * take the paste. Extracting the npsso out of the paste is `:shared` ([PsnAuth.extractNpsso]).
 */
@Composable
expect fun PsnConnectCapture(signInUrl: String, npssoUrl: String, onNpsso: (String) -> Unit)
