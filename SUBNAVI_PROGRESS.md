# SUBNAVI_PROGRESS.md

## Last completed phase
- **Phase 4 — Albums screen**: BUILD GREEN, MISSING MANUAL TEST

### Phase history
| Phase | Status | Manual tests pending |
|---|---|---|
| 0 — Bootstrap | DONE WITH MISSING MANUAL TEST | 1) App lancia senza crash 2) Placeholder screen visibile 3) Screenshot: `phase_00_launch.png` |
| 1 — Networking + Auth | DONE WITH MISSING MANUAL TEST | 1) Inserire URL `http://10.0.2.2:4533` + credenziali valide → verificare successo 2) Password sbagliata → verificare messaggio errore 3) Disabilitare rete → verificare errore di rete 4) Screenshots: `phase_01_onboarding.png`, `phase_01_auth_success.png`, `phase_01_auth_error_bad_creds.png`, `phase_01_auth_error_network.png` |
| 2 — Navigation shell | DONE WITH MISSING MANUAL TEST | 1) Tappare ogni tab bottom nav (Home, Albums, Songs, Playlists) → verificare cambio schermata 2) Screenshots: `phase_02_nav_home.png`, `phase_02_nav_albums.png`, `phase_02_nav_songs.png`, `phase_02_nav_playlists.png` |
| 3 — Home data | DONE WITH MISSING MANUAL TEST | 1) Scroll home → verificare sezioni "Recently Played", "Recently Added", "Playlists" caricano dati reali 2) Disabilitare rete → verificare stato errore (no crash) 3) Tappare album → verifica navigazione a detail 4) Screenshots: `phase_03_home_loaded.png`, `phase_03_home_error.png` |
| 4 — Albums screen | DONE WITH MISSING MANUAL TEST | 1) Verificare griglia album caricata 2) Digitare query nella search bar → verificare filtraggio risultati 3) Cancellare ricerca → verificare ritorno tutti gli album 4) Tappare album → verificare navigazione detail 5) Screenshots: `phase_04_albums_grid.png`, `phase_04_albums_search.png` |

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
- Launcher icon is a placeholder (orange rectangle)
- Cover art URLs need server base URL prepended (currently passing raw coverArt ID)

## Next planned step
- **Phase 5 — Songs screen**
  - Songs list
  - Search bar
  - Pull to refresh
  - Song tap → placeholder (playback wired in Phase 8)

## Emulator AVD
- Name: `Subnavi_AVD` (API 35, Pixel 6)
- Status: NOT CREATED (no emulator on this machine)
