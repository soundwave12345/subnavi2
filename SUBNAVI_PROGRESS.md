# SUBNAVI_PROGRESS.md

## Last completed phase
- **Phase 0 — Project bootstrap / compile baseline**: DONE
- Build: GREEN (`./gradlew :app:assembleDebug` + `:app:lintDebug` pass)
- Emulator acceptance tests: NOT RUN (no emulator available on this machine)

## Dependency versions (from `libs.versions.toml`)
| Dependency | Version |
|---|---|
| Kotlin | 2.1.10 |
| AGP | 8.8.2 |
| KSP | 2.1.10-1.0.31 |
| Compose BOM | 2025.02.00 |
| Media3 | 1.9.3 |
| Hilt | 2.57.1 |
| Room | 2.7.0 |
| Retrofit | 2.11.0 |
| OkHttp | 4.12.0 |
| Coil | 3.1.0 |
| Navigation Compose | 2.8.9 |
| DataStore | 1.1.3 |
| Coroutines | 1.10.1 |
| Lifecycle | 2.9.0 |
| ktlint-gradle | 12.2.0 |
| Gradle | 8.10.2 |

## Deviations from spec
- ktlint version changed from 12.1.2 → **12.2.0** (12.1.2 doesn't exist on Gradle Plugin Portal)
- `media3-service` artifact removed (doesn't exist; service functionality is in `media3-session`)
- Plugin ID corrected: `org.jlleitschuh.ktlint` → `org.jlleitschuh.gradle.ktlint`

## Known issues / TODOs
- No emulator available for acceptance testing — needs physical device or emulator setup
- Launcher icon is a placeholder (orange rectangle)
- Android SDK installed at `~/Android/Sdk` (non-standard location)
- JAVA_HOME must be set to `~/.sdkman/candidates/java/current`

## Next planned step
- **Phase 1 — Networking + Subsonic authentication**
  - Subsonic HTTP client (Retrofit + OkHttp)
  - Auth token generation: `t = md5(password + salt)`
  - `ping` endpoint
  - DataStore for server config
  - Onboarding screen (URL, username, password, test connection)

## Emulator AVD
- Name: `Subnavi_AVD` (API 35, Pixel 6)
- Status: NOT CREATED (no emulator on this machine)
