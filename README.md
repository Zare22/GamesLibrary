# GamesLibrary

A household game-library tracker for Android and Desktop, built with Kotlin Multiplatform and Compose.

## Beta releases (CI)

Installers are built by GitHub Actions ([`.github/workflows/beta-release.yml`](.github/workflows/beta-release.yml)) and attached to a GitHub Release.

### Cut a beta

Push a tag matching `v*`:

```
git tag v1.0.0-beta1
git push origin v1.0.0-beta1
```

The workflow builds on a 3-OS matrix and publishes a Release for the tag with:

- **Windows** — `.msi` (jpackage)
- **macOS** — `.dmg` (jpackage; Apple Silicon)
- **Android** — debug-signed `.apk` (sideload-installable)

It also runs on **manual dispatch** (Actions → *beta-release* → *Run workflow*) for iterating on the pipeline. A dispatch run builds and uploads the artifacts but only *publishes a Release* when the trigger is a tag.

### Required secrets

Add these under **Settings → Secrets and variables → Actions** so the beta can reach the game APIs. The build still succeeds without them — those sync features just won't work in the resulting beta.

| Secret | Purpose |
| --- | --- |
| `IGDB_CLIENT_ID` | IGDB — game metadata and cover art |
| `IGDB_CLIENT_SECRET` | IGDB |
| `STEAM_API_KEY` | Steam library sync |

The desktop installers are **unsigned** and the Android APK is **debug-signed**, so expect the OS "unknown developer" prompts on first install. Code-signing and notarization are out of scope for beta.
