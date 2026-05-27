# 2048 Dark Edition — Android APK (Android Studio / Kotlin)

> This README is written for **Claude Code** running in a terminal.
> Read every section fully before writing a single line of code.
> Follow all sections in order. Do not skip any step.

---

## Project Overview

Build a fully playable, release-ready **2048 game APK** using **Android Studio + Kotlin + Jetpack Compose**.

- Dark green–themed UI matching the exact reference design (see Design section)
- Light/dark theme toggle (default: dark)
- Undo move support
- Continue game after app exit
- All data stored locally using **SharedPreferences** — no internet, no accounts
- Share Stats as a **PDF** via Android share sheet (WhatsApp, Snapchat, Discord, etc.)
- Shareable `.apk` for personal use — no Play Store

---

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Local Storage | SharedPreferences (via `androidx.datastore` or plain `SharedPreferences`) |
| PDF Generation | Android built-in `android.graphics.pdf.PdfDocument` |
| File Sharing | `FileProvider` + `Intent.ACTION_SEND` |
| Min SDK | API 21 (Android 5.0) — covers ~99% of Android phones |
| Target SDK | API 34 |
| Build | Gradle (Kotlin DSL) |

---

## Prerequisites — What Must Be Installed

Claude Code must verify these before starting:

```bash
# 1. Android Studio installed (Hedgehog 2023.1.1 or newer)
#    Download: https://developer.android.com/studio

# 2. JDK 17 (bundled with recent Android Studio — usually already set)
java -version   # Must show openjdk 17.x

# 3. Android SDK installed via Android Studio SDK Manager:
#    - Android 14 (API 34) — compile target
#    - Android 5.0 (API 21) — minimum supported
#    - Android Build Tools 34.x
#    - Android Emulator (optional, for testing)

# 4. ANDROID_HOME environment variable set:
echo $ANDROID_HOME   # Should print SDK path, e.g. /Users/name/Library/Android/sdk

# 5. Gradle wrapper (comes with project — no separate install needed)
```

> If any of these are missing, stop and tell the user exactly what's missing.

---

## Project Creation

```bash
# In Android Studio:
# File → New → New Project → Empty Activity (Compose)
# Name: 2048 Dark Edition
# Package: com.personal.game2048
# Save location: wherever you like
# Language: Kotlin
# Minimum SDK: API 21
# Build configuration language: Kotlin DSL

# OR via command line if Android Studio CLI is available:
# Use Android Studio GUI — it's simpler for this project
```

---

## Project File Structure

```
app/
└── src/main/
    ├── java/com/personal/game2048/
    │   ├── MainActivity.kt                  # Entry point, theme setup, nav host
    │   ├── ui/theme/
    │   │   ├── Theme.kt                     # Dark + Light MaterialTheme definitions
    │   │   ├── Color.kt                     # All color constants
    │   │   └── Type.kt                      # Typography (Space Mono / Roboto Mono)
    │   ├── game/
    │   │   ├── GameBoard.kt                 # 4x4 grid state, move logic, merge logic
    │   │   ├── GameViewModel.kt             # ViewModel: game state, undo stack, timer
    │   │   └── Tile.kt                      # Data class: value, row, col, id, isNew, isMerged
    │   ├── storage/
    │   │   ├── StatsStorage.kt              # SharedPreferences read/write for all stats
    │   │   └── GameSaveStorage.kt           # Save/load mid-game board state (for continue)
    │   ├── pdf/
    │   │   └── StatsPdfGenerator.kt         # Builds PdfDocument from stats data
    │   └── screens/
    │       ├── HomeScreen.kt                # Home: Continue/New Game/Stats/Share Stats
    │       ├── GameScreen.kt                # Main game UI
    │       └── StatsScreen.kt               # Stats + Achievements display
    ├── res/
    │   ├── xml/
    │   │   └── file_paths.xml               # FileProvider paths for PDF sharing
    │   └── values/
    │       └── strings.xml
    └── AndroidManifest.xml
```

---

## UI Design — Match Reference Image Exactly

### Layout Reference
The reference image shows (top to bottom):
1. **Top bar**: Stats icon (top-left), "2048" title (center), Theme toggle icon (top-right)
2. **Score row**: SCORE card (left), BEST card (center), Restart/undo button (right)
3. **Subtitle text**: "Join the numbers and get to the 2048 tile!"
4. **Game grid**: 4×4 rounded tile grid with green gradient tiles
5. **New Game button**: Full-width pill button below the grid
6. **Bottom navigation bar**: Undo | Hint | Stats | Settings (4 icon buttons with labels)

### Color Palette — Dark Theme (Default)

Match these EXACTLY from the reference image:

```kotlin
// Color.kt

// Backgrounds
val AppBackground       = Color(0xFF0D0D0D)   // near-black app bg
val GridBackground      = Color(0xFF1A1A1A)   // dark grid container
val TileEmpty           = Color(0xFF1E1E1E)   // empty cell
val CardBackground      = Color(0xFF1A1A1A)   // score cards, nav buttons

// Score / accent text
val ScoreGreen          = Color(0xFF4CAF70)   // bright green for score numbers
val SubtitleText        = Color(0xFF666666)   // muted subtitle

// Tile colors — green gradient low to high (matching image)
val Tile2               = Color(0xFF1B2D1E)
val Tile4               = Color(0xFF1E3A24)
val Tile8               = Color(0xFF1F4D2C)
val Tile16              = Color(0xFF256338)
val Tile32              = Color(0xFF2E7A47)
val Tile64              = Color(0xFF389157)
val Tile128             = Color(0xFF44A866)
val Tile256             = Color(0xFF52C078)
val Tile512             = Color(0xFF63D48B)
val Tile1024            = Color(0xFF82E4A5)
val Tile2048            = Color(0xFFB0F0C8)   // brightest — near white-green
val TileAbove2048       = Color(0xFFCCF7DC)

// Tile text
val TileTextDark        = Color(0xFFCCFFDD)   // light green text on dark tiles
val TileTextLight       = Color(0xFF0A2010)   // dark text on bright tiles (1024, 2048)

// Bottom nav
val NavBarBackground    = Color(0xFF111111)
val NavButtonBg         = Color(0xFF1E1E1E)   // circular button bg
val NavIconActive       = Color(0xFF4CAF70)
val NavIconDefault      = Color(0xFFAAAAAA)

// New Game button
val NewGameButtonBg     = Color(0xFF1A1A1A)
val NewGameButtonText   = Color(0xFF4CAF70)
val NewGameButtonBorder = Color(0xFF2E2E2E)
```

### Color Palette — Light Theme

```kotlin
val LightAppBackground  = Color(0xFFF5F5F0)
val LightGridBackground = Color(0xFFE8E8E0)
val LightTileEmpty      = Color(0xFFDDDDD5)
val LightCardBg         = Color(0xFFFFFFFF)
val LightScoreGreen     = Color(0xFF2E7D45)
// Tile colors remain the same — they work on both themes
```

### Typography

```kotlin
// Use Space Mono or Roboto Mono — add to res/font/ or use Google Fonts
// Tile numbers: FontWeight.Bold
// Font sizes by tile digit count:
//   1 digit (2,4,8):      36sp
//   2 digits (16–64):     30sp
//   3 digits (128–512):   24sp
//   4 digits (1024–2048): 18sp
//   5+ digits:            14sp
```

### Grid & Tile Style

```kotlin
// Grid container
RoundedCornerShape(16.dp)
padding = 8.dp between tiles
GridBackground color

// Individual tile
RoundedCornerShape(10.dp)
aspect ratio 1:1 (square)
Subtle elevation: shadow offset 0,2dp, blur 4dp, color black@30%

// 2048 tile: add glowing effect
// BoxShadow: color = Tile2048 @ 60% opacity, blur = 12dp
```

### Top Bar

```
[Stats icon button]     2048     [Theme toggle icon button]
```
- Stats icon: `Icons.Default.BarChart` or a bar chart drawable
- Theme toggle: Sun icon (☀) for dark mode (clicking switches to light), Moon icon (🌙) for light mode

### Score Row

```
[ SCORE          ] [ BEST           ] [ ↺ ]
[ 4832 (green)   ] [ 12356 (green)  ] [btn]
```
- Three cards side by side
- Restart (↺) button is a square icon button card matching score card height
- Score numbers in `ScoreGreen` color, labels in subdued gray

### Bottom Navigation Bar

```
[  ↩ Undo  ]  [  💡 Hint (badge) ]  [  📊 Stats  ]  [  ⚙ Settings  ]
```
- Each: circular dark button + label underneath
- Hint badge: shows count of available hints (green circle, number inside)
- Active icon: `NavIconActive` green
- Hint feature: show a subtle highlight on the best possible merge (optional, can be a placeholder for v1)

---

## Home Screen Layout

The Home Screen is shown when:
- App is opened fresh (no saved game)
- User explicitly exits a game

### Button Logic

**If no saved game in progress:**
```
[ New Game     ]   ← full width
[ Stats        ]   ← full width
[ Share Stats  ]   ← full width
```

**If a game was saved mid-session (user exited while playing):**
```
[ Continue Game ]   ← full width, PRIMARY (highlighted green border)
[ New Game      ]   ← full width
[ Stats         ]   ← full width
[ Share Stats   ]   ← full width
```

### Home Screen Details
- App title "2048" large, centered
- Subtitle: "Join the numbers and get to the 2048 tile!"
- Theme toggle icon button in top-right corner
- Best score shown below subtitle: "Best: 12356"
- Button style: full-width, rounded pill shape, outlined (border `NewGameButtonBorder`), text `NewGameButtonText`
- "Continue Game" has a brighter green border to distinguish it as primary action

---

## Game Screen Layout

Matches reference image exactly:
1. Top bar (Stats icon | "2048" | Theme toggle)
2. Score row (SCORE | BEST | Restart ↺)
3. Subtitle line
4. 4×4 grid (fills most of screen)
5. "New Game" button (full-width, below grid)
6. Bottom nav bar (Undo | Hint | Stats | Settings)

### Back Navigation
- Hardware back button or swipe-back → save game state → go to Home Screen
- Game is saved automatically on every move (so "Continue Game" is always current)

---

## Game Logic

### Core Rules
- 4×4 grid, tiles slide: Up / Down / Left / Right
- Swipe gestures (touch) + on-screen bottom nav Undo button
- Two equal adjacent tiles merge into their sum (each pair merges once per move)
- After every valid move, spawn one new tile: 90% = value 2, 10% = value 4
- Win condition: any tile reaches **2048**
- After winning: show "You won! 🎉" dialog — options: **Keep Playing** or **New Game**
- If Keep Playing: game continues beyond 2048 (no second win popup)
- Lose condition: board is full AND no adjacent equal tiles exist
- Lose dialog: "Game Over" — options: **Try Again** or **Home**

### Undo
- Keep a stack of the last **5 board states** in memory (not persisted)
- Each state = full 4×4 array + score at that moment
- Undo button in bottom nav pops one state off the stack
- If stack is empty: show brief snackbar "Nothing to undo"
- Undo does NOT affect stats (doesn't count as a new game, doesn't reset timer)

### Gesture Detection
```kotlin
// Use Modifier.pointerInput with detectDragGestures
// Minimum swipe distance threshold: 50dp
// Directions: calculate dx, dy — whichever axis has larger absolute value wins
val direction = if (abs(dx) > abs(dy)) {
    if (dx > 0) Direction.RIGHT else Direction.LEFT
} else {
    if (dy > 0) Direction.DOWN else Direction.UP
}
```

---

## Theme Toggle

```kotlin
// In MainActivity.kt
var isDarkTheme by remember { mutableStateOf(true) }  // default: dark

// Persist theme choice:
// SharedPreferences key: "g2048_theme_dark" (Boolean)
// Load on app start, save on toggle

// Toggle button: top-right of every screen
// Icon: if isDarkTheme → show Sun icon (☀)
//       if !isDarkTheme → show Moon icon (🌙)
// Tapping toggles theme immediately across the whole app
```

---

## Save / Continue Game

```kotlin
// GameSaveStorage.kt — keys in SharedPreferences:
"g2048_save_active"        // Boolean — is there a game to continue?
"g2048_save_board"         // String — JSON array of 16 ints (row-major)
"g2048_save_score"         // Int — current score when saved
"g2048_save_elapsed_secs"  // Int — seconds elapsed in this session

// Save: called automatically after every valid move
// Load: called when user taps "Continue Game"
// Clear: called when user starts New Game or game ends (win/loss)
```

---

## Stats & Achievements System

### SharedPreferences Keys (all prefixed `g2048_`)

```
g2048_best_score            Int   — all-time highest score
g2048_total_games           Int   — total games started
g2048_games_won             Int   — games where 2048 was reached
g2048_games_lost            Int   — games ended (no moves left)
g2048_total_seconds         Long  — cumulative play time in seconds
g2048_last_played_date      String — ISO date YYYY-MM-DD
g2048_current_streak        Int   — consecutive days with ≥1 game
g2048_longest_streak        Int   — best streak ever
g2048_best_win_time_secs    Int   — fastest time to 2048 (0 = never won)
g2048_best_score_time_secs  Int   — time taken when best score was set
g2048_highest_tile          Int   — highest tile value ever reached
g2048_achievements          String — comma-separated unlocked achievement IDs
```

### Stats Screen Display (11 stats in 2-column card grid)

| Stat | Display Label |
|---|---|
| Best Score | "Best Score" |
| Highest Tile | "Highest Tile" |
| Total Games | "Games Played" |
| Games Won | "Games Won" |
| Games Lost | "Games Lost" |
| Win Rate | "Win Rate" (show as "XX%") |
| Total Play Time | "Hours Played" (format: "Xh Ym") |
| Current Streak | "Current Streak" (show as "X days 🔥") |
| Longest Streak | "Best Streak" |
| Best Win Time | "Fastest Win" (format: "mm:ss", or "—" if never won) |
| Best Score Time | "Best Score Time" (format: "mm:ss") |

### Achievements (13 total — displayed as badge grid, locked = greyed out)

| ID | Name | Condition |
|---|---|---|
| `first_game` | First Move | Play 1 game |
| `score_100` | Centurion | Score ≥ 100 |
| `score_1000` | High Roller | Score ≥ 1,000 |
| `score_10000` | Legend | Score ≥ 10,000 |
| `first_win` | First Win | Win 1 game |
| `win_3` | Hat Trick | Win 3 games |
| `win_under_5min` | Speed Demon | Win in < 5 minutes |
| `hours_10` | Marathon | 10+ total hours played |
| `streak_7` | Devoted | 7-day streak |
| `streak_30` | Obsessed | 30-day streak |
| `tile_1024` | Tile Hunter | Reach tile 1024 |
| `tile_2048` | Master | Reach tile 2048 |
| `tile_4096` | Grandmaster | Reach tile 4096 |

Achievement check: run after every game end event and after every stats update.
Newly unlocked achievement: show a brief animated "Achievement Unlocked!" snackbar.

---

## Session & Streak Logic

### Timer

```kotlin
// GameViewModel.kt
// Use a coroutine with 1-second tick interval (viewModelScope + delay(1000))
// Pause: when onStop() is called (app backgrounded)
// Resume: when onStart() is called
// On game end (win/loss):
//   - add session seconds to g2048_total_seconds
//   - if win AND (best_win_time == 0 OR session < best_win_time): update best_win_time
//   - if score > best_score: update best_score AND best_score_time
```

### Daily Streak

```kotlin
// Called once per app launch (and after each game):
val today = LocalDate.now().toString()          // "2026-05-27"
val last  = prefs.getString("g2048_last_played_date", "")

when {
    last == today      -> { /* already counted today */ }
    last == yesterday  -> { streak++; if (streak > longest) longest = streak }
    else               -> { streak = 1 }  // streak broken
}
prefs.edit { putString("g2048_last_played_date", today) }
```

---

## PDF Stats Report

### StatsPdfGenerator.kt

Use Android's built-in `android.graphics.pdf.PdfDocument`. No external library needed.

```kotlin
// Page: A4 (595 x 842 points)
// Layout (top to bottom):
//   - Header: "2048 Dark Edition" + "Stats Report" + generation date
//   - Divider line
//   - Stats section: two-column table of all 11 stats
//   - Divider line
//   - Achievements section: list of unlocked achievements with checkmarks
//   - Footer: "Generated on [device date]"

// Color scheme in PDF: white background, dark text, green accents for values
// Use android.graphics.Paint for text rendering
// Use android.graphics.Canvas for drawing

fun generateStatsPdf(context: Context, stats: StatsData): File {
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas
    // ... draw everything using Paint objects ...
    pdf.finishPage(page)
    
    val file = File(context.cacheDir, "2048_stats_report.pdf")
    pdf.writeTo(FileOutputStream(file))
    pdf.close()
    return file
}
```

### Share Intent

```kotlin
// In HomeScreen.kt — "Share Stats" button onClick:
fun shareStatsPdf(context: Context, pdfFile: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "My 2048 Stats Report")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Stats via..."))
    // This opens the Android share sheet: WhatsApp, Discord, Snapchat, Gmail, etc.
}
```

### FileProvider Setup

**AndroidManifest.xml** — add inside `<application>`:
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

**res/xml/file_paths.xml**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="shared_pdfs" path="." />
</paths>
```

---

## build.gradle (app level — Kotlin DSL)

```kotlin
android {
    namespace = "com.personal.game2048"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.personal.game2048"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompileExtension = "1.5.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // fine for personal sideloading
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.code.gson:gson:2.10.1")  // for board JSON serialization
    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

---

## Navigation (Jetpack Compose Nav)

```kotlin
// Three destinations:
// "home"   → HomeScreen
// "game"   → GameScreen
// "stats"  → StatsScreen

// NavHost in MainActivity.kt
// HomeScreen:
//   "New Game" → navigate("game"), clear saved game
//   "Continue Game" → navigate("game"), load saved game
//   "Stats" → navigate("stats")
//   "Share Stats" → generate PDF + share intent (no navigation)

// GameScreen:
//   Back press → save game + navigate("home")
//   Stats icon (top-left) → navigate("stats")

// StatsScreen:
//   Back press / back button → popBackStack()
```

---

## Animations (Jetpack Compose)

```kotlin
// Tile slide: use animateFloatAsState for offset, 100ms EaseOut
// Tile merge pop: scale 1f → 1.15f → 1f using keyframes, 150ms total
// New tile appear: animateFloatAsState for scale 0.5f → 1f + alpha 0f → 1f, 120ms
// Score increase: brief color flash on score text (green → white → green), 200ms
// Achievement unlocked: SnackbarHost at bottom, green bg, 2.5 second duration
```

---

## Building the APK

### In Android Studio (GUI):
```
Build → Generate Signed Bundle / APK
→ APK
→ Key store: use debug keystore (for personal use this is fine)
→ Build Variant: release
→ Finish
```

### Via Terminal (from project root):
```bash
./gradlew assembleRelease

# APK output:
# app/build/outputs/apk/release/app-release.apk
```

---

## Installing on Android

```
1. Transfer app-release.apk to Android phone
   (USB, WhatsApp to yourself, Google Drive, Bluetooth — anything works)

2. On the phone:
   Settings → Apps → Special app access → Install unknown apps
   → Allow for Files / Browser / WhatsApp (whichever app you'll use to open it)

3. Tap the APK file → Install

4. Open "2048 Dark Edition" from your app drawer
```

---

## Step-by-Step Build Order for Claude Code

Follow these in exact order:

1. Verify prerequisites (`java -version`, `echo $ANDROID_HOME`, `flutter doctor` not needed here)
2. Create new Android Studio project (Kotlin + Compose, API 21 min)
3. Set up `build.gradle` with all dependencies above
4. Create `ui/theme/Color.kt` with full dark + light palettes
5. Create `ui/theme/Theme.kt` with dark/light `MaterialTheme` using those colors
6. Create `Tile.kt` data class
7. Create `GameBoard.kt` — pure logic: move, merge, spawn, win/loss detection, undo stack
8. Create `StatsStorage.kt` — all SharedPreferences read/write
9. Create `GameSaveStorage.kt` — save/load/clear mid-game board
10. Create `GameViewModel.kt` — wires board logic + storage + timer + undo
11. Create `StatsPdfGenerator.kt` — full PDF layout using `PdfDocument`
12. Set up `AndroidManifest.xml` with FileProvider
13. Create `res/xml/file_paths.xml`
14. Build `HomeScreen.kt` — conditional Continue/New Game buttons, theme toggle
15. Build `GameScreen.kt` — matches reference image layout exactly
16. Build `StatsScreen.kt` — stats cards grid + achievements badges
17. Wire navigation in `MainActivity.kt`
18. Run `./gradlew assembleRelease`
19. Confirm APK at `app/build/outputs/apk/release/app-release.apk`
20. Print full path to APK for the user

---

## Critical Rules for Claude Code

- Do NOT add Firebase, AdMob, or any network library
- Do NOT use any permission that requires internet (`android.permission.INTERNET` must NOT be in manifest)
- Do NOT use any Google Play Services API
- Game must work 100% offline on any Android 5.0+ phone
- All SharedPreferences writes must be `apply()` (async) except on game end where `commit()` is safer
- Board state JSON: serialize as a flat list of 16 ints, row-major order, 0 = empty cell
- The undo stack lives only in memory (ViewModel) — it resets when app is killed, which is fine
- Theme preference MUST persist across app restarts (saved in SharedPreferences)
- PDF is generated fresh every time "Share Stats" is tapped — no caching needed
- The reference image shows a 5-row grid display — that is a 4×4 grid with an extra bottom tile row visible; implement strictly as 4×4

---

*Personal sideload APK — not intended for Play Store distribution.*
