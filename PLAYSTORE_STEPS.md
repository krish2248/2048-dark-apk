# Getting 2048 Dark Edition on the Play Store

Do these in order. Google's rules below were checked on 25 November 2026. Run commands from the project folder.

## Part 1: Before you touch the Play Console

**1. Back up the signing key.** Copy `C:\Users\sonik\keystores\` (both files) to a USB drive or private cloud folder. Never put it on GitHub.

**2. Decide the app ID.** It is `com.personal.game2048` and can never change after the first upload. To change it, edit only `applicationId` in `app/build.gradle.kts` (leave `namespace` alone).

**3. Put the ranking server online** (free Cloudflare account):

```bash
cd server
npm install
npx wrangler login
npx wrangler d1 create dark2048
```

Paste the printed `database_id` into `server/wrangler.jsonc`, then:

```bash
npm run deploy
```

Copy the `https://dark2048.....workers.dev` address it prints.

**4. Connect the app to it.** In `gradle.properties`:

```
leaderboardUrl=https://dark2048.....workers.dev
```

**5. Build.**

```bash
gradlew.bat testDebugUnitTest
gradlew.bat assembleRelease bundleRelease
```

- For your phone: `app\build\outputs\apk\release\app-release.apk`
- For Play: `app\build\outputs\bundle\release\app-release.aab`

**6. Test on your phone.** Uninstall the old v1 first (its key was different, so this erases its data). Check:

- [ ] swipes feel right, sound and vibration work
- [ ] join the ranking, finish a game, and see yourself listed with your flag
- [ ] flight mode: the game still plays, and the score is sent once you're back online
- [ ] close the app mid-game and continue
- [ ] Settings > Delete online profile works

**7. Commit** the URL and `wrangler.jsonc` changes to GitHub (neither is secret).

## Part 2: Play Console account

**8. Register** at https://play.google.com/console. One-time US$25, you must be 18 or over, and prepaid cards aren't accepted. Google may ask for a government ID and a card in your legal name.

**9. Verify a device** with the Play Console mobile app. Personal accounts need this before publishing.

## Part 3: Create the app

**10. Create app:** name `2048 Dark Edition`, type Game, Free.

**11. Store listing:** copy the text from `store/LISTING.md` and upload from `store/`:

- [ ] `icon-512.png`
- [ ] `feature-graphic.png`
- [ ] `screenshots/1.png` to `7.png` (2 minimum, 8 maximum)

After launch, add a screenshot of the real world ranking.

**12. App content:** privacy policy, ads, app access, content rating, target audience (13+) and data safety. Every answer is in `store/LISTING.md`. Read the data safety answers yourself before submitting, because you are the one declaring them.

**13. Countries:** choose all, so every flag can join.

## Part 4: Closed test (required)

Personal accounts created after 13 November 2023 can't publish until this is done.

**14.** Testing > Closed testing > create a track, upload the AAB, and keep **Play App Signing** on.

**15.** Invite at least **12 testers** by Gmail address. Ask about 15, in case some drop out. Each must open the opt-in link and install from Play.

**16.** Keep 12 opted in for **14 days in a row**. Someone who leaves and rejoins starts their 14 days again.

**17.** Collect feedback and fix bugs. For every new upload, raise `versionCode` in `app/build.gradle.kts` and `versionName` when it makes sense.

## Part 5: Go live

**18.** Dashboard > **Apply for production**. You'll be asked how you tested. Review usually takes 7 days or less.

**19.** Production > Create release > choose the tested AAB > roll out.

**20.** After launch, each week:

- [ ] check Android vitals for crashes
- [ ] check reported names (commands in `server/README.md`)
- [ ] reply to reviews
- [ ] check Cloudflare usage against the free plan

## Good to know

- **Expect about 4 weeks** from registering to live; the 14-day test is most of it.
- **Target API:** new apps must target API 36. The app already does.
- **"2048" is a crowded name.** Many games use it, so ranks and the world ranking are what make yours different. Google can reject lookalikes; if it objects, a more distinctive name is the fix.
- **No server yet?** You can launch without it: leave `leaderboardUrl` blank, remove the ranking lines from the listing, replace the feature graphic (it says "World ranking"), and answer "no data collected".

Sources: [testing rules](https://support.google.com/googleplay/android-developer/answer/14151465), [target API](https://support.google.com/googleplay/android-developer/answer/11926878), [registration](https://support.google.com/googleplay/android-developer/answer/6112435), [graphics](https://support.google.com/googleplay/android-developer/answer/9866151)
