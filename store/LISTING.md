# Play Store submission

## Text

**App name** (30 max): 2048 Dark Edition

**Short description** (80 max): Endless 2048 with silky swipes, 18 ranks and a world ranking by flag.

**Full description**

> Swipe, merge and keep going. 2048 is only the warm-up: there is no last tile, no forced ending and no ads.
>
> FOUR WAYS TO PLAY
> • Classic: the 4×4 board, endless
> • Grand: a 5×5 board that goes into the millions
> • Blitz: three minutes, highest score
> • Zen: unlimited undo, no pressure
>
> CLIMB
> • 18 ranks, from Pebble to Infinite
> • World ranking with your flag: worldwide, your country, this week, and nation against nation
> • Every ranked score is verified by replaying the game
> • 74 achievements, levels and three new goals every day
>
> FEELS RIGHT
> • Tiles glide the instant you swipe, at your screen's full refresh rate
> • Never drops a swipe, however fast you play
> • Dark and light themes, five tile palettes to unlock
> • Deep stats, score trends and a shareable PDF report
>
> Plays fully offline. No ads, no tracking, no account. The world ranking is optional.

Remove the two world-ranking lines (and "by flag" from the short description) if you publish before the server is live.

## Graphics

- `icon-512.png`, `feature-graphic.png`
- `screenshots/1.png` to `7.png` (1080×1920). The boards in 1 and 3 are set up for illustration.

## Console answers

| Section | Answer |
|---|---|
| Category | Game, Puzzle |
| Ads | No |
| App access | All functionality available without special access |
| Target audience | 13 and over |
| Content rating | Puzzle, no violence; users can interact: yes (public player names) |
| Privacy policy | https://github.com/krish2248/2048-dark-apk/blob/master/PRIVACY.md |
| Account deletion | In app: Settings > Delete online profile. Web: the privacy policy URL |

**Data safety** (with the world ranking on). Collected, not shared, encrypted in transit, deletion available, collection optional:

- Personal info > Name (the player name) : app functionality
- Personal info > User IDs (random player ID) : app functionality, fraud prevention
- App activity > Other actions (scores and moves) : app functionality, fraud prevention

The chosen country is not device location. If you publish without a server, answer "no data collected".

## Upload

1. `gradlew.bat bundleRelease` gives `app/build/outputs/bundle/release/app-release.aab`.
2. Play Console > Create app > upload the AAB, keep Play App Signing on.
3. New personal developer accounts must run a closed test first: 12 testers for 14 days in a row. Full steps in `PLAYSTORE_STEPS.md`.
4. Raise `versionCode` in `app/build.gradle.kts` for every upload.

The application ID `com.personal.game2048` can never change after the first upload.
