package hr.kotwave.gameslibrary.di

import org.koin.core.module.Module

/** composeApp platform bindings — the Steam OpenID browser leg (ADR 0001 keeps it out of `:shared`). */
expect val platformAppModule: Module
