# 2048 Dark Edition — Session Tracker

## Last Session
- **Date**: 2026-05-27
- **Session #**: 3

## Current Status: APK BUILT AND TESTED ON DEVICE
- App compiles, installs, and runs on physical Android device
- User has played multiple games and confirmed everything works
- All requested changes applied and verified

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

## GitHub Stats
- **Total commits**: ~63
- **Pull requests**: 7 (all merged)
- **Branches created**: 7 feature branches

## What's Next (Session 4)
- [ ] Any UI tweaks after more playtesting
- [ ] Play Store preparation if desired (privacy policy, screenshots, AAB build)
- [ ] Consider: haptic feedback on swipe, sound effects, or other polish

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
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **GitHub**: https://github.com/krish2248/2048-dark-apk
- **Portfolio**: https://sonikrish.com
