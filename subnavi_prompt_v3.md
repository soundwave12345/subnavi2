# Subnavi — Android Navidrome Music App
## Incremental Build-First / No-Hallucination Coding Prompt

You are building **Subnavi**, a native Android music streaming app in **Kotlin + Jetpack Compose**
that connects to a self-hosted **Navidrome** server using the **Subsonic API**.

The application must be developed **incrementally, in very small steps**, and at **every single step**
you must verify that:

1. the project **still builds successfully**;
2. all used Android / Jetpack / Google SDK APIs are **real, currently valid, and correctly imported**;
3. the requested feature is **actually implemented**, not only scaffolded;
4. the implementation references the **exact files, classes, functions, and constructors** used;
5. recoverable runtime errors are **handled and logged**, not left to crash the app.

Do **not** implement the whole app in one pass.
Do **not** skip verification.
Do **not** claim a feature is complete if it is only partial.

---

## 0. Core Operating Rules (MANDATORY)

### Rule A — Incremental delivery only

Work in **small, verifiable iterations**.
Each iteration must implement **only one narrowly scoped feature or one tightly related vertical slice**.
**Maximum lines of new or modified code per single step: ~150 lines.**
If a step requires more, split it into sub-steps (e.g., Phase 3a, Phase 3b).
Always prefer multiple small steps over one large step.

**Examples of valid steps:**
- project bootstrap + compile
- onboarding UI only
- Subsonic auth token generation + ping verification
- home screen recent albums section
- albums screen search
- playback service local only
- persistent mini-player
- lyrics mode
- Chromecast sender queue sync
- Android Auto browsing

**Examples of invalid steps:**
- "implement all screens"
- "implement full player + cast + lyrics + auto in one step"

---

### Rule B — Build-first discipline

> **Windows environment: always use `.\gradlew.bat` instead of `./gradlew`.**
> **Shell: Command Prompt or PowerShell — never bash.**

After every step, you must run build verification and report the result.

**Minimum commands after each step:**
```
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```
If relevant:
```
.\gradlew.bat :app:testDebugUnitTest
```

**After every green build at the end of a phase:**
```
git add -A && git commit -m "Phase X: <feature> — BUILD GREEN"
```
Never commit on a red build.
These commits act as safe rollback points.

**If the build fails:**
- stop feature expansion immediately;
- fix the build first;
- explain the root cause;
- only continue when build is green.

Never leave the project in a red / uncompilable state.

---

### Rule C — No hallucinated APIs

Before using any library API, class, constructor, method, builder, callback, or manifest entry:

1. verify it exists in the currently used dependency/version;
2. verify package name and imports;
3. verify constructor signature / builder usage;
4. verify whether it is stable, experimental, deprecated, or replaced.

**If unsure:**
- do not guess;
- do not invent;
- either inspect existing dependency source/docs or explicitly mark the uncertainty and stop.

Never invent Android SDK methods, Media3 APIs, Google Cast APIs, Compose APIs,
Hilt annotations, or Gradle DSL entries.

---

### Rule D — Feature completion proof required

A feature is considered implemented **only if all of the following are provided:**

1. **Build result** — exact command executed + success/failure outcome
2. **Implementation proof** — exact files changed, class names, function names, relevant constructors/builders used
3. **Feature verification checklist** — what was supposed to be done, what was actually implemented, what remains TODO
4. **Runtime/manual validation instructions** — exact steps to test the feature manually if runtime verification is required

Do not say "done" if you cannot prove it with files + build + validation checklist.

---

### Rule E — Honest status only

For every requested feature, use one of these statuses only:

| Status | Meaning |
|---|---|
| `DONE` | Implementation proof exists, build passes, all required automatic + manual validations satisfied |
| `PARTIAL` | Feature is only partially implemented, or significant required behavior is still missing |
| `DONE WITH MISSING MANUAL TEST` | Implementation complete from code/build perspective, automatic checks pass, but runtime manual validation still pending |
| `NOT STARTED` | Feature not yet implemented |
| `BLOCKED` | Cannot proceed due to dependency, environment issue, missing credential, unsupported SDK, or unresolved technical blocker |

Never mark a feature `DONE` if any required behavior is missing.

---

### Rule F — Referencing format is mandatory

At the end of every step, output this exact structure:

```
#### Step Summary
- Goal:
- Status:
- Build:
- Tests:
- Emulator acceptance tests run:
- Files changed:

#### API / Constructor Verification
- Dependency / SDK used:
- Verified classes:
- Verified constructors/builders:
- Deprecated or risky APIs found:
- Mitigation:

#### Feature Verification Matrix
- Requirement:
- Implemented behavior:
- Files:
- Classes / functions:
- Remaining gaps:

#### Error Handling Verification
- Expected error cases handled:
- Where each error is caught:
- Where each error is logged:
- UI behavior on failure:
- Remaining unhandled risks:

#### Emulator Acceptance Test Results
- Tests executed:
- Commands run:
- Results (pass/fail/screenshot path):
- Remaining behaviors not testable on emulator:

#### Manual Validation Required
- Required manual checks:
- How to execute them:
- Current state:

#### Next Smallest Safe Step
- Proposed next step:
- Why this should be next:
- Risks/dependencies:
```

---

### Rule G — Automatic vs Manual Validation

For features that include runtime UX behaviors that cannot be fully proven by build/tests alone
(e.g. lyric sync quality, real Chromecast session behavior, playback UX fluidity, device discovery,
or timing-sensitive UI behavior), split validation into:

- **Automatic acceptance criteria**
- **Emulator acceptance criteria** (see Rule J)
- **Manual validation criteria**

A feature may be marked:
- `PARTIAL` if key functionality is still missing
- `DONE WITH MISSING MANUAL TEST` if code/build/emulator checks pass but physical device manual validation is still pending
- `DONE` only when both implementation proof and required manual validation are satisfied

---

### Rule H — Session Continuity (MANDATORY)

**At the end of every completed phase**, create or update this file at the project root:

```
SUBNAVI_PROGRESS.md
```

This file must always contain:
- Last completed phase and its status
- All dependency versions currently in use (copy from `libs.versions.toml`)
- Known issues and TODOs
- The next planned step with its exact scope
- Emulator AVD name in use (so the next session can reuse it)

**At the start of every new session:**
1. Read `SUBNAVI_PROGRESS.md` before doing anything else.
2. Confirm the current phase status out loud before proceeding.
3. Check if the emulator AVD is still available: `%ANDROID_HOME%\emulator\emulator.exe -list-avds`
4. Never assume the state of the project from memory.

---

### Rule I — File Awareness

Before writing any new file, check if it already exists.
Before modifying any file, read its current content first.
Never assume a file's content from memory — always read it first.

**Useful orientation commands (Windows):**
```cmd
:: List all Kotlin files
dir /s /b *.kt

:: Read a file before editing
type src\main\java\com\example\subnavi\MyFile.kt

:: List all files in a directory
dir src\main\java\com\example\subnavi\
```

---

### Rule J — Emulator-Driven Acceptance Testing (MANDATORY)

After every phase that produces runtime-verifiable behavior, you must run acceptance tests
on the Android emulator using ADB. This replaces many "manual validation" steps with
automated, repeatable checks.

#### J.1 — Emulator setup (one-time, Phase 0)

Check if a suitable AVD already exists:
```cmd
%ANDROID_HOME%\emulator\emulator.exe -list-avds
```

If no AVD exists, create one:
```cmd
%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat "system-images;android-35;google_apis;x86_64"
%ANDROID_HOME%\cmdline-tools\latest\bin\avdmanager.bat create avd ^
  -n Subnavi_AVD ^
  -k "system-images;android-35;google_apis;x86_64" ^
  --device "pixel_6"
```

Start the emulator (headless for CI speed, or windowed for visual checks):
```cmd
:: Headless (faster, no window)
start /B %ANDROID_HOME%\emulator\emulator.exe -avd Subnavi_AVD -no-window -no-audio -gpu swiftshader_indirect

:: Windowed (for visual verification)
start /B %ANDROID_HOME%\emulator\emulator.exe -avd Subnavi_AVD -gpu host
```

Wait for the emulator to be ready:
```cmd
%ANDROID_HOME%\platform-tools\adb.exe wait-for-device
%ANDROID_HOME%\platform-tools\adb.exe shell getprop sys.boot_completed
:: Wait until output is "1"
```

#### J.2 — Install and launch after every phase

After a green build, always install and launch on the emulator:
```cmd
:: Install the debug APK
%ANDROID_HOME%\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk

:: Launch the app
%ANDROID_HOME%\platform-tools\adb.exe shell am start -n com.example.subnavi/.MainActivity

:: Watch logs in real time
%ANDROID_HOME%\platform-tools\adb.exe logcat -s Subnavi:V AndroidRuntime:E
```

#### J.3 — Emulator acceptance test toolkit

Use these ADB commands for acceptance testing. Always run the relevant ones after each phase:

```cmd
:: Take a screenshot (save to disk for proof)
%ANDROID_HOME%\platform-tools\adb.exe exec-out screencap -p > screenshots\phase_X_screen.png

:: Tap a UI element at coordinates (x, y)
%ANDROID_HOME%\platform-tools\adb.exe shell input tap 540 960

:: Swipe gesture (startX startY endX endY duration_ms)
%ANDROID_HOME%\platform-tools\adb.exe shell input swipe 540 1200 540 400 300

:: Type text into focused input
%ANDROID_HOME%\platform-tools\adb.exe shell input text "http://10.0.2.2:4533"

:: Press back button
%ANDROID_HOME%\platform-tools\adb.exe shell input keyevent 4

:: Press home button
%ANDROID_HOME%\platform-tools\adb.exe shell input keyevent 3

:: Check if app is running (no crash = good)
%ANDROID_HOME%\platform-tools\adb.exe shell pidof com.example.subnavi

:: Dump UI hierarchy (for verifying UI elements exist)
%ANDROID_HOME%\platform-tools\adb.exe shell uiautomator dump /sdcard/ui.xml
%ANDROID_HOME%\platform-tools\adb.exe pull /sdcard/ui.xml screenshots\phase_X_ui.xml

:: Run instrumented tests
.\gradlew.bat :app:connectedDebugAndroidTest

:: Simulate network loss
%ANDROID_HOME%\platform-tools\adb.exe shell svc wifi disable
%ANDROID_HOME%\platform-tools\adb.exe shell svc wifi enable

:: Clear app data (reset state)
%ANDROID_HOME%\platform-tools\adb.exe shell pm clear com.example.subnavi
```

#### J.4 — Per-phase emulator acceptance checklist

Each phase must include the following emulator tests at minimum:

| Phase | Minimum emulator tests |
|---|---|
| 0 — Bootstrap | App launches, no crash. Screenshot saved. |
| 1 — Auth | Onboarding screen visible. Fill URL + credentials. Tap "Test Connection". Verify success/error state. Screenshot. |
| 2 — Navigation | Tap each bottom nav item. Verify screen changes. Screenshot each. |
| 3 — Home | Scroll home screen. Verify sections load. Verify loading/error state on network loss. Screenshot. |
| 4 — Albums | Grid visible. Type search query. Verify results filter. Pull to refresh. Screenshot. |
| 5 — Songs | List visible. Pull to refresh. Screenshot. |
| 6 — Album detail | Tap album. Verify header + song list. Screenshot. |
| 7 — Playlists | Create playlist. Verify it appears. Delete it. Verify removal. Screenshot each step. |
| 8 — Playback | Tap a song. Verify playback starts (check via logcat). Verify notification appears. Tap pause. Tap next. Screenshot. |
| 9 — Mini-player | While playing, navigate to Albums. Verify mini-player visible. Tap play/pause. Screenshot. |
| 10 — Full player | Tap mini-player. Verify full player opens. Tap seek bar. Tap like. Screenshot. |
| 11 — Lyrics | Open lyrics mode. Verify toggle works. Screenshot. Synced line highlight: verify via logcat position logs. |
| 13 — Settings | Open settings. Change a value. Close. Reopen. Verify value persisted. Screenshot. |

#### J.5 — Screenshots as proof

Save all screenshots in a `screenshots/` folder at the project root with this naming:
```
screenshots/
  phase_00_launch.png
  phase_01_onboarding.png
  phase_01_auth_success.png
  phase_01_auth_error_bad_creds.png
  phase_02_home_tab.png
  phase_02_albums_tab.png
  ...
```

Include screenshot paths in the "Emulator Acceptance Test Results" section of Rule F output.

#### J.6 — What the emulator CANNOT test (be explicit)

Always explicitly state which validations require a physical device or real hardware:
- Real Chromecast discovery and casting (Phase 12) — requires real Chromecast on LAN
- Real Android Auto UI (Phase 14) — requires Android Auto head unit or Desktop Head Unit (DHU)
- Bluetooth audio focus edge cases — emulator audio support is limited
- Real-device playback latency and seek precision — emulator audio is simulated

These remain `DONE WITH MISSING MANUAL TEST` until confirmed on real hardware.

---

## 1. Architecture & Technology Constraints

### Required stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (using KSP, not KAPT) |
| Networking | Retrofit + OkHttp |
| Image loading | Coil |
| Playback | Media3 / ExoPlayer |
| Chromecast | Google Cast Android Sender SDK |
| Android Auto | Media3 MediaLibraryService |
| Settings/prefs | DataStore |
| Local cache | Room |
| Theme | Dynamic palette from artwork when feasible |

### Pinned dependency versions (do not deviate without explicit approval)

```toml
# libs.versions.toml
[versions]
kotlin                = "2.1.10"
agp                   = "8.8.2"
ksp                   = "2.1.10-1.0.31"
compose_bom           = "2025.02.00"
media3                = "1.9.3"
hilt                  = "2.57.1"
hilt_androidx         = "1.2.0"
room                  = "2.7.0"
retrofit              = "2.11.0"
okhttp                = "4.12.0"
coil                  = "3.1.0"
cast                  = "21.5.0"
navigation_compose    = "2.8.9"
datastore             = "1.1.3"
coroutines            = "1.10.1"
lifecycle             = "2.9.0"
```

> ⚠️ **Important**: Hilt must use **KSP**, not KAPT. KAPT is deprecated.
> In `build.gradle.kts`, use `ksp(...)` instead of `kapt(...)` for all Hilt compiler entries.

Use **Gradle Version Catalogs** (`libs.versions.toml`) for all dependencies.
Do **not** use hardcoded version strings in `build.gradle.kts` files.

### Important implementation guardrails

- Prefer **stable** APIs over deprecated ones.
- If a requirement requests a deprecated approach, flag it and propose a current alternative before implementing.
- Keep package naming coherent and production-ready: `com.example.subnavi`
- Configure **ktlint** with a minimal ruleset in Phase 0 and run it at every build.
- `minSdk = 23` — required by Media3 1.9.x and many AndroidX libraries as of mid-2025.
- `targetSdk = 35` (Android 15).

---

## 2. Development Process (MANDATORY ORDER)

Do not skip phases.
Do not jump ahead if the previous phase is not build-green.
Do not implement business logic in a phase that is scoped to structure only.
After every phase: run emulator acceptance tests, save screenshots, update `SUBNAVI_PROGRESS.md`.

---

### Phase 0 — Project bootstrap / compile baseline

**Goal:**
- Create a minimal Android project named **Subnavi**
- Configure Gradle Version Catalogs with all pinned versions
- Set up Compose, Material 3, Hilt (KSP), Retrofit, Room, DataStore, Coil, Media3
- Configure ktlint with a minimal ruleset
- Create the emulator AVD: `Subnavi_AVD` (API 35, Pixel 6 profile)
- App launches to a simple placeholder screen
- Project builds and lints successfully

**Deliverable:** A clean baseline that compiles and runs on the emulator before any business feature is added.

**Test server config:**
- For emulator testing, the default Navidrome URL is `http://10.0.2.2:4533`
  (Android emulator localhost tunnel to the host machine)
- Store test credentials only in `local.properties` (already git-ignored by default)
- Never hardcode real credentials anywhere in source files

**Emulator acceptance tests:**
- App launches without crash
- Placeholder screen visible
- Screenshot: `screenshots/phase_00_launch.png`

---

### Phase 1 — Networking foundation + Subsonic authentication

**Goal:**
- Implement Subsonic base HTTP client (Retrofit + OkHttp)
- Implement auth token generation: `t = md5(password + salt)`
- Implement `ping` endpoint
- Implement server config storage with DataStore
- Implement onboarding/server setup screen with:
  - Server URL field
  - Username field
  - Password field
  - Test connection button
  - Loading state
  - Clear error states (bad URL / bad credentials / network failure)

**Acceptance criteria:**
- Connection test works end-to-end against a real Navidrome instance
- All three error scenarios handled explicitly with user-visible messages
- Credentials stored securely — justify the storage strategy chosen
- Build green

**Emulator acceptance tests:**
- Fill server URL with `http://10.0.2.2:4533`
- Fill valid credentials → tap "Test Connection" → verify success state
- Fill wrong password → verify error message shown
- Disable network (`adb shell svc wifi disable`) → verify network error handled
- Screenshots: `phase_01_onboarding.png`, `phase_01_auth_success.png`, `phase_01_auth_error_bad_creds.png`, `phase_01_auth_error_network.png`

---

### Phase 2 — Navigation shell

**Goal:**
- Create app navigation structure using Navigation Compose
- Bottom navigation with: Home, Playlists, Albums, Songs
- Full-screen player route (no bottom bar)
- Settings entry point
- App still builds

**Scope:** Only navigation wiring and screen shells. No business logic yet.

**Emulator acceptance tests:**
- Tap each bottom nav item → verify active tab changes
- Screenshot each tab: `phase_02_nav_home.png`, `phase_02_nav_albums.png`, etc.

---

### Phase 3 — Home data vertical slice

**Goal:** Home screen with real API data only.

- Recently Played (`getAlbumList2?type=recent`)
- Recently Added (`getAlbumList2?type=newest`)
- Playlists (`getPlaylists`)

**Acceptance criteria:**
- Each section renders real API data
- Loading / empty / error states implemented
- Tap handlers wired (destination screens may still be minimal shells)
- Build green

**Emulator acceptance tests:**
- Scroll home → verify all three sections load with real data
- Disable network → verify error state shown (not crash)
- Screenshot: `phase_03_home_loaded.png`, `phase_03_home_error.png`

---

### Phase 4 — Albums screen

**Goal:**
- Albums grid
- Search bar with 300ms debounce
- Live search via `search3` or local filter when cached
- Pull to refresh
- Album tap → navigate to album detail shell

**Emulator acceptance tests:**
- Verify grid loads
- Type `a` in search bar → verify results filter
- Clear search → verify all albums return
- Pull to refresh → verify reload
- Screenshot: `phase_04_albums_grid.png`, `phase_04_albums_search.png`

---

### Phase 5 — Songs screen

**Goal:**
- Songs list
- Search bar
- Pull to refresh
- Song tap → placeholder (playback wired in Phase 8)

**Emulator acceptance tests:**
- List loads
- Search filters results
- Screenshot: `phase_05_songs.png`

---

### Phase 6 — Album detail screen

**Goal:**
- Album header (artwork, title, artist, year)
- Play button (placeholder until Phase 8)
- Shuffle button (placeholder until Phase 8)
- Songs list

**Emulator acceptance tests:**
- Tap album from Albums screen → detail opens
- Artwork and metadata visible
- Screenshot: `phase_06_album_detail.png`

---

### Phase 7 — Playlist screens

**Goal:**
- Playlists grid
- Playlist detail screen
- Create / delete / update playlist via API
- Management bottom sheet

**Emulator acceptance tests:**
- Create a playlist via UI → verify it appears in grid
- Open playlist detail
- Delete playlist → verify it disappears
- Screenshot each step: `phase_07_playlists_grid.png`, `phase_07_create.png`, `phase_07_delete.png`

---

### Phase 8 — Local playback service

**Goal:**
- Implement local audio playback using Media3 / ExoPlayer
- Background `MediaLibraryService`
- Queue management
- Notification + MediaSession integration
- Wire song taps from Songs, Albums, and Playlists screens to actual playback

**Emulator acceptance tests:**
- Tap a song → verify playback starts (check via logcat: `adb logcat -s ExoPlayer`)
- Verify notification appears with track title
- Tap pause in notification → verify playback stops
- Tap next → verify track changes
- Lock screen → verify controls still work
- Screenshot notification: `phase_08_notification.png`

---

### Phase 9 — Persistent Mini-Player

**Goal:** Persistent mini-player bar above bottom navigation, visible across the app whenever playback is active.

**Requirements:**
- Visible on every screen except the full-screen player while playback is active
- Hidden on the full-screen player
- Displays: current track thumbnail, title, artist
- Controls: play/pause, skip next, tap to open full player
- State synchronized with local playback

**Emulator acceptance tests:**
- Start playback → navigate to Albums tab → verify mini-player visible above nav bar
- Navigate to Songs tab → verify mini-player still visible
- Tap play/pause on mini-player → verify playback state changes (check logcat)
- Tap mini-player body → verify full player opens
- Open full player → verify mini-player hidden
- Screenshot: `phase_09_miniplayer_albums.png`, `phase_09_miniplayer_songs.png`

---

### Phase 10 — Full player (normal mode)

**Goal:**
- Full-screen player UI
- Artwork area
- Title / artist
- Seek bar
- Previous / play-pause / next
- Shuffle / repeat
- Like / unlike (`star` / `unstar`)
- Add to playlist
- Blurred/darkened artwork background
- No bottom navigation

**Emulator acceptance tests:**
- Open full player → verify all controls visible
- Tap seek bar to a new position → verify position changes (logcat)
- Tap shuffle → verify shuffle state changes in UI
- Tap repeat → verify repeat mode cycles
- Tap like → verify star state changes (check UI + logcat API call)
- Screenshot: `phase_10_player.png`

---

### Phase 11 — Karaoke / Lyrics mode

**Goal:**
- Microphone toggle in player
- Artwork mode ↔ Lyrics mode switch
- LRC parsing
- Current line highlighting
- Auto-scroll synchronized with playback
- Fallback to plain lyrics when synced lyrics unavailable
- Empty state when no lyrics found

**Emulator acceptance tests:**
- Tap lyrics toggle → verify mode switches
- Open track with known LRC lyrics → verify lyrics list displayed
- Check logcat for highlighted line updates during playback
- Open track with no lyrics → verify empty state shown
- Screenshot: `phase_11_lyrics.png`, `phase_11_lyrics_empty.png`

---

### Phase 12 — Chromecast full support

> ⚠️ This phase has **limited emulator testability**. Chromecast requires a real device on LAN.
> All emulator tests are structural; runtime casting requires physical hardware.

**Emulator acceptance tests (structural only):**
- Verify Cast button appears in player toolbar (UI dump: `adb shell uiautomator dump`)
- Verify Cast SDK initializes without crash (logcat: no CastContext errors)
- Screenshot: `phase_12_cast_button.png`

**Mark as:** `DONE WITH MISSING MANUAL TEST` until real Chromecast session is validated.

---

### Phase 13 — Settings

**Goal:**
- Server settings (URL, username, password, test connection)
- Lyrics providers: enable/disable + priority order
- Playback preferences
- Theme preferences
- Cache management

**Emulator acceptance tests:**
- Open settings → verify all sections visible
- Change a preference value → close settings → reopen → verify value persisted
- Tap "Clear cache" → verify confirmation dialog + action
- Screenshot: `phase_13_settings.png`

---

### Phase 14 — Android Auto

> ⚠️ Full Android Auto UI testing requires the Desktop Head Unit (DHU) tool or a real head unit.
> Structural tests are possible on emulator; full browsing/playback tests require DHU.

**Emulator acceptance tests (structural only):**
- Verify `MediaLibraryService` declared in manifest: `adb shell dumpsys package com.example.subnavi | findstr Service`
- Verify service starts without crash: `adb shell am start-service com.example.subnavi/.media.MusicService`
- Screenshot: `phase_14_service_running.png`

**Mark as:** `DONE WITH MISSING MANUAL TEST` until DHU or real Auto validation is confirmed.

---

## 3. Product Requirements

### App identity
- App name: **Subnavi**

### UI design direction
- Modern, polished music app
- Warm dark theme
- Orange accent (`#FF6D00` range)
- Rounded artwork and cards
- Cohesive, immersive player experience
- Clean Compose Material 3 implementation
- Dynamic color palette extracted from artwork where feasible

### Navigation
- Home, Playlists, Albums, Songs (bottom nav)
- Full-screen Player (no bottom nav)
- Settings (accessible from Home or nav drawer)

### Home
- Recently Played section
- Recently Added section
- Playlists section

### Albums
- Grid layout
- Search with debounce
- Pull to refresh
- Album detail navigation

### Songs
- List layout
- Search
- Pull to refresh
- Tap → queue album songs + open player

### Playlists
- Grid layout
- Create / delete / manage
- Playlist detail with play and shuffle

### Album detail
- Header (artwork, title, artist, year)
- Play and Shuffle buttons
- Song list with tap-to-play from selected index

### Full player
- Artwork mode and Lyrics/Karaoke mode
- Like / unlike
- Add to playlist
- Seek bar
- Previous / Play-Pause / Next
- Shuffle / Repeat
- Cast integration
- No bottom navigation

### Persistent mini-player
- Bar above bottom navigation
- Visible on all screens except full player when playback is active
- Allows controlling playback while browsing

### Lyrics
- Provider chain with fallback logic
- Synced (LRC) preferred over plain text

### Subsonic API endpoints to implement

| Endpoint | Purpose |
|---|---|
| `ping` | Server health check |
| `getAlbumList2` | Home sections |
| `getAlbum` | Album detail |
| `getSong` | Single song metadata |
| `search3` | Search |
| `getPlaylists` | Playlists list |
| `getPlaylist` | Playlist detail |
| `createPlaylist` | Create playlist |
| `deletePlaylist` | Delete playlist |
| `updatePlaylist` | Update playlist |
| `star` / `unstar` | Like / unlike |
| `stream` | Audio streaming |
| `getCoverArt` | Artwork |
| `getRandomSongs` | Shuffle source |
| `getLyrics` | Lyrics |
| `scrobble` | Playback reporting |

---

## 4. Required Output Format For Every Iteration

For every implementation step, your response must contain:

### A. Plan for current step
- Exact scope of this step
- Why this is the smallest safe increment
- Files expected to change

### B. Code changes
- Concrete implementation
- No pseudo-code unless explicitly marked
- No placeholder APIs invented

### C. Build verification
- Command run (Windows: `.\gradlew.bat`)
- Result (success / failure)
- If failed: exact error summary and fix applied

### D. API verification
- Libraries / classes used
- Constructors / builders used
- Why they are valid for the pinned version
- Any deprecated APIs avoided or replaced

### E. Feature verification
For each requirement covered in this step:
- Requirement
- Implemented?
- Files and classes/functions
- Emulator test instructions

### F. Emulator acceptance test results
- ADB commands run
- Results (pass/fail)
- Screenshot paths saved

### G. Remaining gaps
Explicitly list what is still missing.

### H. Error Handling Verification
- Expected error cases handled
- Where each error is caught
- Where each error is logged
- UI behavior on failure
- Remaining unhandled risks

### I. Manual Validation Required
- Which behaviors still need manual validation beyond emulator
- Exact steps to validate them
- Current feature state: `DONE`, `PARTIAL`, or `DONE WITH MISSING MANUAL TEST`

---

## 5. Anti-Regressions / Quality Gates

At every step, verify all five gates before reporting completion:

### Gate 1 — Compile gate
- No Kotlin compile errors
- No unresolved references
- No manifest merge issues
- No dependency resolution issues
- ktlint passes

### Gate 2 — Feature gate
The feature must be actually callable and reachable from the running app.

### Gate 3 — Wiring gate
UI, ViewModel, Repository, and Service layers must be connected where required.
No layer may be implemented in isolation if the phase requires end-to-end wiring.

### Gate 4 — Honesty gate
Do not report `DONE` unless Gates 1–3 are satisfied.
Do not omit known issues from the Step Summary.
Do not omit TODOs from the Feature Verification Matrix.

### Gate 5 — Session continuity gate
`SUBNAVI_PROGRESS.md` must be updated at the end of every phase.
The next session must begin by reading it.

### Gate 6 — Emulator gate
At least the minimum emulator acceptance tests for the current phase must be run and reported.
Screenshots must be saved in `screenshots/` and referenced in the output.
Do not mark a phase `DONE` if emulator acceptance tests have not been run.

---

*End of Subnavi system prompt — v3.0*
