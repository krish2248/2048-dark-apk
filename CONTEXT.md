# Context: 2048 Dark Edition

Read this first when picking the project up again. `SESSION.md` is the history, `PLAYSTORE_STEPS.md` is what to do next.

## What it is

An endless sliding-tile Android game (Kotlin, Jetpack Compose) with four modes, 18 ranks, 74 achievements and an optional world ranking by country flag. Owner: Krish Soni, https://sonikrish.com. Repo: https://github.com/krish2248/2048-dark-apk (branch `master`).

## Where it stands (25 November 2026)

- v2.0.0 (`versionCode` 2) is on master. The signed AAB and APK build cleanly.
- 13 app tests, 6 server tests and lint pass. Tested on an Android 15 emulator, in debug and minified release builds.
- **Not done:** the ranking server isn't deployed, no physical phone has run it, and nothing has been uploaded to Play.

## Waiting on the owner

1. `npx wrangler login`, then deploy the server
2. Keep or change the app ID `com.personal.game2048` (permanent after first upload)
3. Phone test, Play Console account, 12 testers for 14 days

## Facts that aren't obvious from the code

- **Two engines, one set of rules.** `app/.../game/Engine.kt` and `server/src/engine.js` must match exactly, including the order of random draws. `server/test/vectors.json` is checked by both test suites. Change one, change both.
- **Ranked scores come from replays.** The app sends a seed and move log; the server works out the score itself. Breaking the log breaks the ranking.
- **5 undos per ranked run, none in Blitz.** Enforced on both sides. Undo restores the random state, so it can't be used to re-roll a spawn.
- **Zen is unranked** and excluded from records, ranks and achievements.
- **Tier = best Classic score.** The badge on the ranking uses the verified score; the badge in the app uses the local one.
- **Imported v1 saves can't be ranked,** since they have no move log.
- **No server URL is fine.** With `leaderboardUrl` blank the app runs offline and queues each mode's best run.
- **Signing:** `keystore.properties` (git-ignored) points at `C:\Users\sonik\keystores\`. Without it, release builds are unsigned on purpose.
- **Animation:** `BoardAnimator.frame` must only be read while drawing. Reading it in composition would recompose every frame.

## Owner's preferences

- Smooth and quick above all; dark green look; light theme stays available
- No hint button. Footer "Developed ~ sonikrish.com" stays
- Keep `SESSION.md` and this file updated every session and push to GitHub
- Rank names stay original (inspired by CS:GO tiers, no CS names or art)

## Commands

```
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug -PleaderboardUrl=http://10.0.2.2:8787    (emulator + local server)
gradlew.bat assembleRelease bundleRelease
cd server && npm test
cd server && npm run dev
```
