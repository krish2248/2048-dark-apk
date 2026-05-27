# 2048 Dark Edition — Session Tracker

## Last Session
- **Date**: 2026-05-27
- **Session #**: 2

## Current Status: APK BUILT AND WORKING
- APK compiles and installs successfully on device
- User has tested and confirmed gameplay works
- PDF stats report generates and shares correctly

---

## Session 2 — What Was Done (2026-05-27 afternoon)

### Setup & Build
- [x] Verified user's Android SDK setup at `C:\Users\sonik\AppData\Local\Android\Sdk`
- [x] Found JDK 21 bundled with Android Studio at `C:\Program Files\Android\Android Studio\jbr`
- [x] Created `local.properties` with SDK path
- [x] Created `gradlew.bat` and downloaded `gradle-wrapper.jar` for CLI builds
- [x] Fixed compilation errors:
  - Array type mismatch in `GameBoard.move()` grid copy
  - `LocalLifecycleOwner` import path (`compose.ui.platform` not `lifecycle.compose`)
  - Removed unused `beforeScore` variable
- [x] Successfully built release APK (10.4 MB)

### Features Added (PR #7 merged, 4 commits)
- [x] **Home screen footer**: "Dev - sonikrish.com" clickable link at bottom, opens portfolio in browser
- [x] **Dark PDF redesign**: Stats report now uses dark background (#0D0D0D), green accent text (#4CAF70), Typeface.MONOSPACE throughout, rounded stat cards, 3-column achievement grid
- [x] **Achievements expanded: 13 → 27**: Added Dedicated, Veteran, Century Club, Rising Star, Expert, Unstoppable, Champion, Lightning, Getting Hooked, Time Flies, On a Roll, Committed, Halfway There, Beyond Limits

### Bug Fixes
- [x] Fixed PDF header overlap ("2048" and "Dark Edition" text were colliding — measured width at wrong font size)

---

## Session 1 — What Was Done (2026-05-27 morning)

### Core Project (27 individual commits)
- [x] Read full project spec (README, Setup Guide, Play Store Guide, reference image)
- [x] Created GitHub repo: https://github.com/krish2248/2048-dark-apk
- [x] Complete Android project structure (Kotlin + Jetpack Compose)
- [x] All Gradle build configs, AndroidManifest.xml, resource files, adaptive icons

### Source Files Built
- [x] ui/theme/ — Color.kt, Type.kt, Theme.kt (dark + light themes)
- [x] game/ — Tile.kt, GameBoard.kt (full 2048 logic), GameViewModel.kt
- [x] storage/ — StatsStorage.kt (27 achievements, 11 stats, streaks), GameSaveStorage.kt
- [x] pdf/ — StatsPdfGenerator.kt (dark themed, monospace, green accents)
- [x] screens/ — HomeScreen.kt (centered), GameScreen.kt, StatsScreen.kt
- [x] MainActivity.kt — NavHost, theme toggle, edge-to-edge

### Feature PRs Merged (PRs #1–#6)
- [x] Tile scale/fade animations, edge-to-edge display, score bounce animation
- [x] Live game timer display, undo snackbar feedback, lifecycle-aware timer

### User Requests Applied
- [x] Hint button REMOVED from bottom nav
- [x] Homepage elements CENTERED
- [x] "Dev - sonikrish.com" footer added
- [x] PDF dark themed with monospace font
- [x] Achievements expanded to 27

---

## GitHub Stats
- **Total commits**: ~60
- **Pull requests**: 7 (all merged)
- **Branches created**: 7 feature branches

## What's Next (Session 3)
- [ ] Test thoroughly on physical device (all screens, edge cases)
- [ ] Generate proper PNG launcher icons (if vector icons look off on device)
- [ ] Play Store preparation if desired (privacy policy, screenshots, AAB build)
- [ ] Any UI polish or feature tweaks based on device testing

## Build Instructions
```
# Set these before building (or build from Android Studio):
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=C:\Users\sonik\AppData\Local\Android\Sdk

# Build release APK:
gradlew.bat assembleRelease

# APK output:
app\build\outputs\apk\release\app-release.apk
```

## Key Technical Decisions
- Hint button removed, bottom nav has 3 buttons (Undo, Stats, Settings)
- Homepage centered with Arrangement.Center + Alignment.CenterHorizontally
- FontFamily.Monospace (built-in) — no custom font files needed
- Theme persisted via SharedPreferences key "g2048_theme_dark"
- Core library desugaring enabled for java.time on API 21+
- Swipe fires once per gesture (accumulated drag, detect on end)
- Tile animations: spring bounce (0.7→1.0) + fade in (120ms)
- Score animation: bounce scale (1.15→1.0)
- Lifecycle observer pauses timer and auto-saves on background
- PDF: dark bg, green text, Typeface.MONOSPACE, 3-col achievements
- Footer: "Dev - sonikrish.com" opens portfolio via ACTION_VIEW

## Project Info
- **Package**: com.personal.game2048
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **GitHub**: https://github.com/krish2248/2048-dark-apk
- **Portfolio**: https://sonikrish.com
