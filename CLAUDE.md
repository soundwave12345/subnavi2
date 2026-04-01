# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Subnavi** is a native Android music streaming app (Kotlin + Jetpack Compose) that connects to a self-hosted Navidrome server using the Subsonic API. It is currently in the planning/bootstrap phase.

The full development spec is in `subnavi_prompt_v3.md` — read it before starting any work.

## Build & Run Commands

```bash
# Build (debug)
.\gradlew.bat :app:assembleDebug

# Lint
.\gradlew.bat :app:lintDebug

# Unit tests
.\gradlew.bat :app:testDebugUnitTest

# Instrumented tests (requires running emulator)
.\gradlew.bat :app:connectedDebugAndroidTest
```

> The original spec targets Windows (`.\gradlew.bat`). On Linux/macOS, use `./gradlew` instead.

## Technology Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt via KSP (not KAPT)
- **Networking**: Retrofit + OkHttp
- **Image loading**: Coil 3.x
- **Playback**: Media3 / ExoPlayer
- **Chromecast**: Google Cast Android Sender SDK
- **Android Auto**: Media3 MediaLibraryService
- **Settings**: DataStore
- **Local cache**: Room
- **Code quality**: ktlint
- **Package**: `com.example.subnavi`
- **minSdk**: 23, **targetSdk**: 35

All dependency versions are pinned in `libs.versions.toml` (Gradle Version Catalogs) — do not deviate without explicit approval.

## Core Development Rules

### Incremental delivery
- Maximum ~150 lines of new/modified code per step
- Split into sub-steps (e.g., Phase 3a, 3b) if a step exceeds that
- One narrowly scoped feature per step — never "implement all screens"

### Build-first discipline
- Build must pass after every step before continuing
- Never leave the project in a red/uncompilable state
- Commit after every green build: `git commit -m "Phase X: <feature> — BUILD GREEN"`
- If build fails: stop feature expansion, fix the build first, explain the root cause

### No hallucinated APIs
- Verify every library API, class, constructor, and manifest entry exists in the pinned version
- If unsure, do not guess — inspect dependency source/docs or stop and flag the uncertainty

### Mandatory progress tracking
- Update `SUBNAVI_PROGRESS.md` at the end of every completed phase
- At the start of every new session, read `SUBNAVI_PROGRESS.md` first
- Never assume project state from memory — always read files before modifying

### Emulator acceptance testing
- AVD name: `Subnavi_AVD` (API 35, Pixel 6)
- Run minimum emulator tests per phase (defined in spec)
- Save screenshots in `screenshots/` with naming convention `phase_XX_description.png`
- Some features (Chromecast, Android Auto) require physical hardware and are marked `DONE WITH MISSING MANUAL TEST`

### Status reporting
Use only these statuses: `DONE`, `PARTIAL`, `DONE WITH MISSING MANUAL TEST`, `NOT STARTED`, `BLOCKED`.

### File awareness
- Read a file before modifying it — never assume content from memory
- Check if a file exists before creating a new one

## Architecture

### Layers
- **UI**: Jetpack Compose screens with ViewModels
- **ViewModel**: Holds UI state, communicates with repositories
- **Repository**: Mediates between data sources (API, local cache)
- **Network**: Retrofit interfaces for Subsonic API endpoints
- **Data**: Room database, DataStore preferences
- **Service**: Media3 playback service, Chromecast integration

### Subsonic API endpoints (to implement)
`ping`, `getAlbumList2`, `getAlbum`, `getSong`, `search3`, `getPlaylists`, `getPlaylist`, `createPlaylist`, `deletePlaylist`, `updatePlaylist`, `star`/`unstar`, `stream`, `getCoverArt`, `getRandomSongs`, `getLyrics`, `scrobble`

### Auth
Token-based: `t = md5(password + salt)`. Test server URL for emulator: `http://10.0.2.2:4533`. Credentials go in `local.properties` only.

## Development Phases (in order)

0. Project bootstrap / compile baseline
1. Networking + Subsonic authentication
2. Navigation shell
3. Home data (recent albums, playlists)
4. Albums screen (grid, search)
5. Songs screen
6. Album detail
7. Playlists CRUD
8. Local playback service (Media3)
9. Persistent mini-player
10. Full player (normal mode)
11. Karaoke / Lyrics mode
12. Chromecast
13. Settings
14. Android Auto

Do not skip phases. Do not start a phase if the previous one is not build-green.
