# SUBNAVI_PROGRESS.md

## Last completed phase
- **Phase 11 — Karaoke / Lyrics mode**: BUILD GREEN, MISSING MANUAL TEST

### Bug fixes
- **Cover art fix**: cover art IDs were passed raw to Coil. Now `SubsonicApiClient.getCoverArtUrl()` constructs `{baseUrl}/rest/getCoverArt?id={id}&size=300` and MusicRepository maps all DTOs.

### Phase history
| Phase | Status | Manual tests pending |
|---|---|---|
| 0 — Bootstrap | DONE WITH MISSING MANUAL TEST | 1) App lancia senza crash 2) Placeholder screen visibile 3) Screenshot: `phase_00_launch.png` |
| 1 — Networking + Auth | DONE WITH MISSING MANUAL TEST | 1) Inserire URL `http://10.0.2.2:4533` + credenziali valide → verificare successo 2) Password sbagliata → verificare messaggio errore 3) Disabilitare rete → verificare errore di rete 4) Screenshots: `phase_01_onboarding.png`, `phase_01_auth_success.png`, `phase_01_auth_error_bad_creds.png`, `phase_01_auth_error_network.png` |
| 2 — Navigation shell | DONE WITH MISSING MANUAL TEST | 1) Tappare ogni tab bottom nav (Home, Albums, Songs, Playlists) → verificare cambio schermata 2) Screenshots: `phase_02_nav_home.png`, `phase_02_nav_albums.png`, `phase_02_nav_songs.png`, `phase_02_nav_playlists.png` |
| 3 — Home data | DONE WITH MISSING MANUAL TEST | 1) Scroll home → verificare sezioni "Recently Played", "Recently Added", "Playlists" caricano dati reali 2) Disabilitare rete → verificare stato errore (no crash) 3) Tappare album → verifica navigazione a detail 4) **Verificare artwork visibile** 5) Screenshots: `phase_03_home_loaded.png`, `phase_03_home_error.png` |
| 4 — Albums screen | DONE WITH MISSING MANUAL TEST | 1) Verificare griglia album caricata **con artwork visibile** 2) Digitare query nella search bar → verificare filtraggio risultati 3) Cancellare ricerca → verificare ritorno tutti gli album 4) Tappare album → verificare navigazione detail 5) Screenshots: `phase_04_albums_grid.png`, `phase_04_albums_search.png` |
| 5 — Songs screen | DONE WITH MISSING MANUAL TEST | 1) Verificare lista canzoni caricata **con artwork visibile** 2) Digitare query nella search bar → verificare filtraggio 3) Cancellare ricerca → verificare ritorno tutte le canzoni 4) Screenshots: `phase_05_songs_list.png`, `phase_05_songs_search.png` |
| 6 — Album detail | DONE WITH MISSING MANUAL TEST | 1) Tappare album da Albums screen o Home → verificare apertura detail 2) Verificare artwork, titolo, artista, anno visibili 3) Verificare lista canzoni con numero traccia e durata 4) Verificare bottoni Play/Shuffle presenti (placeholder) 5) Tappare freccia back → verificare ritorno alla schermata precedente 6) Screenshot: `phase_06_album_detail.png` |
| 7 — Playlists CRUD | DONE WITH MISSING MANUAL TEST | 1) Tappare tab Playlists → verificare griglia caricata 2) Tappare + → inserire nome → verificare playlist creata 3) Tappare playlist → verificare detail con lista canzoni 4) Tappare cestino → verificare playlist eliminata 5) Screenshots: `phase_07_playlists_grid.png`, `phase_07_playlist_detail.png`, `phase_07_create.png` |
| 8 — Playback | DONE WITH MISSING MANUAL TEST | 1) Tappare canzone da Songs → verificare riproduzione audio 2) Verificare notifica con titolo traccia 3) Tappare Play in Album Detail → verificare riproduzione 4) Tappare Shuffle → verificare ordine casuale 5) Tappare canzone in Playlist Detail → verificare riproduzione 6) Screenshot: `phase_08_notification.png` |
| 9 — Mini-player | DONE WITH MISSING MANUAL TEST | 1) Avviare riproduzione canzone 2) Verificare mini-player visibile sopra bottom bar su Home/Albums/Songs/Playlists 3) Tappare play/pause nel mini-player → verificare cambio stato 4) Tappare skip next → verificare cambio traccia 5) Tappare mini-player → verificare navigazione a player 6) Screenshot: `phase_09_mini_player.png` |
| 10 — Full player | DONE WITH MISSING MANUAL TEST | 1) Tappare mini-player → verificare apertura full player 2) Verificare artwork grande, titolo, artista 3) Verificare seek bar funzionante 4) Verificare controlli prev/play-pause/next 5) Verificare coda visibile sotto i controlli 6) Screenshot: `phase_10_full_player.png` |
| 11 — Lyrics | DONE WITH MISSING MANUAL TEST | 1) Aprire full player con traccia in riproduzione 2) Tappare icona Lyrics nella top bar → verificare overlay scuro con testo 3) Se testo disponibile su Navidrome → verificare testo mostrato 4) Se testo non disponibile → verificare messaggio "No lyrics available" 5) Tappare di nuovo icona → verificare chiusura overlay 6) Screenshot: `phase_11_lyrics_overlay.png` |

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
- **Phase 12 — Chromecast**
  - Google Cast sender integration

## Emulator AVD
- Name: `Subnavi_AVD` (API 35, Pixel 6)
- Status: NOT CREATED (no emulator on this machine)
