# 2048 Dark Edition: Session Tracker

## Last Session
- **Date**: 2026-11-25
- **Session #**: 4

## Current Status: v2.0.0 BUILT, SIGNED AND TESTED ON EMULATOR
- Signed AAB and APK build cleanly; 13 app tests and 6 server tests pass
- World ranking is built and tested against a local server, but **not deployed yet**: `leaderboardUrl` in `gradle.properties` is blank, so this build shows "Ranking isn't live yet"
- Not yet run on a physical phone

## To Do Before Uploading To Play
Full ordered list: `PLAYSTORE_STEPS.md`. Project context: `CONTEXT.md`.
- [ ] Deploy the ranking server (steps in `server/README.md`, needs a Cloudflare login)
- [ ] Put the URL in `gradle.properties`, rebuild with `gradlew.bat bundleRelease`
- [ ] Decide whether to keep the application ID `com.personal.game2048` (permanent after first upload)
- [ ] Back up the upload key: `C:\Users\sonik\keystores\` (never commit it)
- [ ] Try it on a real phone: feel of the swipes, sound, vibration
- [ ] Fill in the Play Console using `store/LISTING.md`
- [ ] Closed test: 12 testers opted in for 14 days in a row (required for new personal accounts)

---

## Session 4: v2.0.0 rewrite (2026-11-25)

### Gameplay
- [x] Endless: tiles stored as exponents, no win dialog, goal always moves to the next tile
- [x] Modes: Classic 4x4, Grand 5x5, Blitz 3:00, Zen (unlimited undo, unranked)
- [x] Seeded deterministic engine with a move log; 5 undos per ranked run, undo restores the RNG
- [x] Game over can be rescued with a remaining undo
- [x] Blitz hides the board while paused

### Smoothness
- [x] New `BoardAnimator` + canvas `BoardView`: tiles really glide, merge, pop, spawn
- [x] Swipe fires at 16dp instead of on finger lift; tracks each finger, so two-thumb play works
- [x] Mid-slide swipes retarget tiles from where they are: nothing queued or dropped
- [x] Highest refresh rate requested; baseline profile rule; R8 (10.4 MB to 2.2 MB)
- [x] Emulator, release build: 1 janky frame of 312 during rapid swiping, 0 missed vsync

### Progression
- [x] 18 ranks (Pebble to Infinite) from best Classic score, with drawn badges
- [x] 74 achievements (v1 unlocks carry over), XP levels, 3 daily goals, streaks
- [x] Stats per mode, score trend, tiles reached with fastest times, swipe directions, history, 2-page PDF
- [x] 5 tile palettes unlocked by rank

### World ranking
- [x] `server/`: Cloudflare Worker + D1; replays every run to verify it; name filter, reports, bans, rate limits, deletion
- [x] App: flag + name profile, World / My country / This week / Nations tabs, own row pinned, offline queue and cache

### Play readiness
- [x] targetSdk 36, minSdk 24, adaptive + monochrome icon, splash, edge-to-edge, predictive back, landscape/tablet
- [x] Real upload key (v1 was debug-signed), backup rules, privacy policy, listing text, icon, feature graphic, screenshots
- [x] v1 stats, theme and saved game are imported on first launch
- [x] Added `PLAYSTORE_STEPS.md` and `CONTEXT.md`; store icon converted to the required 32-bit PNG

### v1 bugs fixed
- [x] "Keep Playing" cleared the save and double-counted stats
- [x] Best streak was never recorded for a one-day streak; achievements only unlocked when a game ended
- [x] Settings button only toggled the theme

### Key decisions
- All local state lives in `Store` (JSON files, atomic, debounced). Run log is the source of truth for ranked scores.
- `Engine.kt` and `server/src/engine.js` must change together; `server/test/vectors.json` pins both.
- Zen never counts towards records, ranks or achievements.
- Migrated v1 saves can be finished but not ranked (no move log).
- Rank names are original on purpose (no CS names or art), to stay clear of Play IP claims.
- Font is Space Grotesk (OFL); hint button stays removed; footer unchanged.

---

## Session 3 — What Was Done (2026-05-27 evening)

### Changes
- [x] Custom app icon: user's 2048 grid logo image set as launcher icon (all mipmap densities: 48–192px)
- [x] Removed adaptive icon XMLs so PNG icon is used on all Android versions
- [x] Footer text changed from "Dev - sonikrish.com" to "Developed ~ sonikrish.com"
- [x] Removed "Join the numbers and get to the 2048 tile!" subtitle from home screen

---

## Session 2 — What Was Done (2026-05-27 afternoon)

### Setup & Build
- [x] Verified Android SDK at `C:\Users\sonik\AppData\Local\Android\Sdk`
- [x] JDK 21 found at `C:\Program Files\Android\Android Studio\jbr`
- [x] Created `local.properties`, `gradlew.bat`, downloaded `gradle-wrapper.jar`
- [x] Fixed compilation errors (Array type mismatch, LocalLifecycleOwner import, unused variable)
- [x] First successful release APK build (10.4 MB)

### Features Added (PR #7)
- [x] "Developed ~ sonikrish.com" clickable footer on home screen (opens portfolio)
- [x] Dark-themed PDF stats report (dark bg, green text, Typeface.MONOSPACE, rounded cards)
- [x] Achievements expanded from 13 to 27

### Bug Fixes
- [x] Fixed PDF header overlap ("2048" and "Dark Edition" text collision)

---

## Session 1 — What Was Done (2026-05-27 morning)

### Core Project (27 individual commits)
- [x] Created GitHub repo: https://github.com/krish2248/2048-dark-apk
- [x] Complete Android project structure (Kotlin + Jetpack Compose)
- [x] All build configs, manifest, resources, adaptive icons

### Source Files Built
- [x] ui/theme/ — Color.kt, Type.kt, Theme.kt (dark + light themes)
- [x] game/ — Tile.kt, GameBoard.kt (full 2048 logic), GameViewModel.kt
- [x] storage/ — StatsStorage.kt (27 achievements, 11 stats, streaks), GameSaveStorage.kt
- [x] pdf/ — StatsPdfGenerator.kt (dark themed, monospace, green accents)
- [x] screens/ — HomeScreen.kt (centered), GameScreen.kt, StatsScreen.kt
- [x] MainActivity.kt — NavHost, theme toggle, edge-to-edge

### Feature PRs Merged (#1–#7)
- [x] Tile scale/fade animations, edge-to-edge display, score bounce animation
- [x] Live game timer, undo snackbar feedback, lifecycle-aware timer
- [x] Dev footer + dark PDF + 27 achievements

### All User Requests Applied
- [x] Hint button REMOVED from bottom nav
- [x] Homepage elements CENTERED
- [x] "Developed ~ sonikrish.com" footer (opens portfolio)
- [x] Subtitle quote REMOVED from home screen
- [x] PDF dark themed with monospace font
- [x] Achievements expanded to 27
- [x] Custom 2048 grid logo as app icon

---

## GitHub
- https://github.com/krish2248/2048-dark-apk

## Build Instructions
```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
gradlew.bat bundleRelease     (app\build\outputs\bundle\release\app-release.aab)
```
JDK 17, Android SDK at `C:\Users\sonik\AppData\Local\Android\Sdk`. Signing comes from the git-ignored `keystore.properties`.

## Key Technical Decisions (v1, superseded where Session 4 differs)
- Hint button removed, bottom nav: Undo, Stats, Settings
- Homepage centered, no subtitle quote
- Footer: "Developed ~ sonikrish.com" opens portfolio via ACTION_VIEW
- FontFamily.Monospace (built-in), no custom font files
- Theme persisted via SharedPreferences "g2048_theme_dark"
- Core library desugaring for java.time on API 21+
- Swipe fires once per gesture (accumulated drag, detect on end)
- Tile animations: spring bounce (0.7→1.0) + fade in (120ms)
- Score animation: bounce scale (1.15→1.0)
- Lifecycle observer pauses timer and auto-saves on background
- PDF: dark bg, green text, Typeface.MONOSPACE, 3-col achievements
- Custom PNG launcher icon (user's 2048 grid image) at all densities

## Project Info
- **Package**: com.personal.game2048
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 16)
- **GitHub**: https://github.com/krish2248/2048-dark-apk
- **Portfolio**: https://sonikrish.com
