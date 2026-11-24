# World ranking server

A Cloudflare Worker with a D1 database. It fits in the free plan.

Every submitted run carries its seed and move list. The server replays it with the same rules as the app (`src/engine.js` is a port of `Engine.kt`) and stores the score it computed itself, so a made-up score can't get in. This doesn't detect someone using a bot to play legal moves; ban those by hand.

## Go live (about 5 minutes, once)

```bash
cd server
npm install
npx wrangler login                  # opens the browser, free Cloudflare account
npx wrangler d1 create dark2048     # prints a database_id
# paste that id into wrangler.jsonc, then:
npm run deploy                      # prints https://dark2048.<you>.workers.dev
```

Put that URL in `gradle.properties` at the project root and rebuild:

```
leaderboardUrl=https://dark2048.<you>.workers.dev
```

## Develop

```bash
npm test          # engine vectors, cheating, names
npm run dev       # http://localhost:8787
```

A debug build reaches a local server from the emulator with `-PleaderboardUrl=http://10.0.2.2:8787`.

## Moderate

```bash
# most reported names
npx wrangler d1 execute dark2048 --remote --command "SELECT p.id, p.name, COUNT(*) n FROM reports r JOIN players p ON p.id = r.player_id GROUP BY p.id ORDER BY n DESC LIMIT 20"
# hide a player everywhere
npx wrangler d1 execute dark2048 --remote --command "UPDATE players SET banned = 1 WHERE id = 'PLAYER_ID'"
# delete a player on request
npx wrangler d1 execute dark2048 --remote --command "DELETE FROM scores WHERE player_id = 'PLAYER_ID'; DELETE FROM reports WHERE player_id = 'PLAYER_ID' OR reporter_id = 'PLAYER_ID'; DELETE FROM players WHERE id = 'PLAYER_ID'"
```

Changing the rules in `Engine.kt` means changing `src/engine.js` too, and regenerating `test/vectors.json`.
