import { MODES, replay } from './engine.js';
import { cleanName, isCountry } from './names.js';

const PAGE = 100;
const MAX_BODY = 256 * 1024;

export default {
  async fetch(request, env, ctx) {
    try {
      return await route(request, env, ctx);
    } catch (e) {
      if (e instanceof HttpError) return json({ error: e.message }, e.status);
      console.error(e);
      return json({ error: 'Something went wrong on our side.' }, 500);
    }
  },
};

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function route(request, env, ctx) {
  const url = new URL(request.url);
  switch (`${request.method} ${url.pathname}`) {
    case 'GET /': return json({ ok: true });
    case 'POST /v1/players': return createPlayer(request, env);
    case 'PUT /v1/players/me': return updatePlayer(request, env);
    case 'DELETE /v1/players/me': return deletePlayer(request, env);
    case 'POST /v1/scores': return submit(request, env);
    case 'GET /v1/leaderboard': return leaderboard(url, env);
    case 'GET /v1/countries': return countries(request, url, env, ctx);
    case 'POST /v1/report': return report(request, env);
    default: throw new HttpError(404, 'Not found');
  }
}

// ---- players ----

async function createPlayer(request, env) {
  await limit(env.NEW_PLAYERS, request.headers.get('cf-connecting-ip') ?? 'local');
  const { name, country } = await profile(request);
  const id = crypto.randomUUID().replaceAll('-', '').slice(0, 16);
  const secret = hex(crypto.getRandomValues(new Uint8Array(32)));
  await env.DB.prepare('INSERT INTO players (id, secret_hash, name, country, created_at) VALUES (?, ?, ?, ?, ?)')
    .bind(id, await sha256(secret), name, country, Date.now()).run();
  return json({ id, secret }, 201);
}

async function updatePlayer(request, env) {
  const player = await auth(request, env);
  await limit(env.WRITES, player.id);
  const { name, country } = await profile(request);
  await env.DB.batch([
    env.DB.prepare('UPDATE players SET name = ?, country = ? WHERE id = ?').bind(name, country, player.id),
    env.DB.prepare('UPDATE scores SET country = ? WHERE player_id = ?').bind(country, player.id),
  ]);
  return json({ ok: true });
}

async function deletePlayer(request, env) {
  const player = await auth(request, env);
  await env.DB.batch([
    env.DB.prepare('DELETE FROM scores WHERE player_id = ?').bind(player.id),
    env.DB.prepare('DELETE FROM reports WHERE player_id = ? OR reporter_id = ?').bind(player.id, player.id),
    env.DB.prepare('DELETE FROM players WHERE id = ?').bind(player.id),
  ]);
  return json({ ok: true });
}

async function profile(request) {
  const body = await readJson(request);
  const name = cleanName(body.name);
  if (!name) throw new HttpError(400, 'Please pick a different name (3 to 16 letters or numbers).');
  if (!isCountry(body.country)) throw new HttpError(400, 'Please pick a country.');
  return { name, country: body.country };
}

// ---- scores ----

async function submit(request, env) {
  const player = await auth(request, env);
  await limit(env.WRITES, player.id);
  const { mode, seed, log, ms } = await readJson(request);
  if (!Object.hasOwn(MODES, mode) || !Number.isInteger(seed) || typeof log !== 'string') throw new HttpError(400, 'Invalid run.');
  const run = replay(mode, seed, log);
  if (!run) throw new HttpError(422, 'This run could not be verified.');

  const now = Date.now();
  const time = Number.isInteger(ms) && ms >= 0 ? Math.min(ms, 1e10) : 0;
  const upsert = env.DB.prepare(`
    INSERT INTO scores (player_id, mode, period, country, score, exp, moves, ms, at)
    VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)
    ON CONFLICT (player_id, mode, period) DO UPDATE SET
      score = excluded.score, exp = excluded.exp, moves = excluded.moves, ms = excluded.ms, at = excluded.at
    WHERE excluded.score > scores.score`);
  const row = (period) => upsert.bind(player.id, mode, period, player.country, run.score, run.exp, run.moves, time, now);
  const [all] = await env.DB.batch([row('all'), row(week(now))]);

  const best = await env.DB.prepare('SELECT score, at FROM scores WHERE player_id = ? AND mode = ? AND period = ?')
    .bind(player.id, mode, 'all').first();
  const [rank, total, countryRank] = await Promise.all([
    position(env, mode, 'all', best.score, best.at),
    count(env, mode, 'all'),
    position(env, mode, 'all', best.score, best.at, player.country),
  ]);
  return json({ best: best.score, improved: all.meta.changes > 0, rank, total, countryRank });
}

async function position(env, mode, period, score, at, country) {
  const r = await env.DB.prepare(`
    SELECT COUNT(*) AS n FROM scores s JOIN players p ON p.id = s.player_id
    WHERE s.mode = ?1 AND s.period = ?2 AND p.banned = 0
      AND (s.score > ?3 OR (s.score = ?3 AND s.at < ?4)) AND (?5 IS NULL OR s.country = ?5)`)
    .bind(mode, period, score, at, country ?? null).first();
  return r.n + 1;
}

async function count(env, mode, period, country) {
  const r = await env.DB.prepare(`
    SELECT COUNT(*) AS n FROM scores s JOIN players p ON p.id = s.player_id
    WHERE s.mode = ?1 AND s.period = ?2 AND p.banned = 0 AND (?3 IS NULL OR s.country = ?3)`)
    .bind(mode, period, country ?? null).first();
  return r.n;
}

async function leaderboard(url, env) {
  const mode = modeOf(url);
  const period = url.searchParams.get('period') === 'week' ? week(Date.now()) : 'all';
  const country = url.searchParams.get('country');
  if (country !== null && !isCountry(country)) throw new HttpError(400, 'Unknown country.');
  const me = url.searchParams.get('me');

  const { results } = await env.DB.prepare(`
    SELECT s.player_id AS id, p.name, s.country, s.score, s.exp, COALESCE(c.score, 0) AS classic
    FROM scores s
    JOIN players p ON p.id = s.player_id
    LEFT JOIN scores c ON c.player_id = s.player_id AND c.mode = 'classic' AND c.period = 'all'
    WHERE s.mode = ?1 AND s.period = ?2 AND p.banned = 0 AND (?3 IS NULL OR s.country = ?3)
    ORDER BY s.score DESC, s.at ASC LIMIT ?4`)
    .bind(mode, period, country, PAGE).all();
  const entries = results.map((r, i) => ({ rank: i + 1, ...r }));

  let mine = entries.find((e) => e.id === me) ?? null;
  if (me && !mine && entries.length === PAGE) {
    const r = await env.DB.prepare(`
      SELECT s.player_id AS id, p.name, s.country, s.score, s.exp, s.at, COALESCE(c.score, 0) AS classic
      FROM scores s JOIN players p ON p.id = s.player_id
      LEFT JOIN scores c ON c.player_id = s.player_id AND c.mode = 'classic' AND c.period = 'all'
      WHERE s.player_id = ?1 AND s.mode = ?2 AND s.period = ?3 AND p.banned = 0 AND (?4 IS NULL OR s.country = ?4)`)
      .bind(me, mode, period, country).first();
    if (r) {
      const { at, ...entry } = r;
      mine = { rank: await position(env, mode, period, r.score, at, country), ...entry };
    }
  }
  const total = entries.length < PAGE ? entries.length : await count(env, mode, period, country);
  return json({ entries, total, me: mine });
}

async function countries(request, url, env, ctx) {
  const mode = modeOf(url);
  // touches every row, so share one result between all viewers for a minute
  const cache = caches.default;
  const key = new Request(`${url.origin}/v1/countries?mode=${mode}`);
  const hit = await cache.match(key);
  if (hit) return hit;
  const { results } = await env.DB.prepare(`
    SELECT country, players, score, name AS champion FROM (
      SELECT s.country, s.score, p.name,
        COUNT(*) OVER (PARTITION BY s.country) AS players,
        ROW_NUMBER() OVER (PARTITION BY s.country ORDER BY s.score DESC, s.at ASC) AS place
      FROM scores s JOIN players p ON p.id = s.player_id
      WHERE s.mode = ? AND s.period = 'all' AND p.banned = 0
    ) WHERE place = 1 ORDER BY score DESC`)
    .bind(mode).all();
  const res = json({ entries: results.map((r, i) => ({ rank: i + 1, ...r })) }, 200, { 'cache-control': 'public, max-age=60' });
  ctx.waitUntil(cache.put(key, res.clone()));
  return res;
}

async function report(request, env) {
  const player = await auth(request, env);
  await limit(env.WRITES, player.id);
  const { id } = await readJson(request);
  if (typeof id !== 'string' || !(await env.DB.prepare('SELECT 1 FROM players WHERE id = ?').bind(id).first())) throw new HttpError(404, 'Player not found.');
  await env.DB.prepare('INSERT OR IGNORE INTO reports (player_id, reporter_id, at) VALUES (?, ?, ?)').bind(id, player.id, Date.now()).run();
  return json({ ok: true });
}

// ---- helpers ----

async function auth(request, env) {
  const m = /^Bearer ([0-9a-f]{16})\.([0-9a-f]{64})$/.exec(request.headers.get('authorization') ?? '');
  const player = m && await env.DB.prepare('SELECT id, secret_hash, country, banned FROM players WHERE id = ?').bind(m[1]).first();
  if (!player || !equal(player.secret_hash, await sha256(m[2]))) throw new HttpError(401, 'Please join the ranking again.');
  if (player.banned) throw new HttpError(403, 'This profile has been removed from the ranking.');
  return player;
}

async function readJson(request) {
  const text = await request.text();
  if (text.length > MAX_BODY) throw new HttpError(413, 'Too large.');
  try {
    const body = JSON.parse(text);
    if (body && typeof body === 'object') return body;
  } catch { /* fall through */ }
  throw new HttpError(400, 'Invalid request.');
}

function modeOf(url) {
  const mode = url.searchParams.get('mode') ?? 'classic';
  if (!Object.hasOwn(MODES, mode)) throw new HttpError(400, 'Unknown mode.');
  return mode;
}

async function limit(limiter, key) {
  if (limiter && !(await limiter.limit({ key })).success) throw new HttpError(429, 'Too many requests, try again in a minute.');
}

/** ISO week, e.g. 2026-W48 (UTC). */
export function week(ms) {
  const d = new Date(ms);
  d.setUTCHours(0, 0, 0, 0);
  d.setUTCDate(d.getUTCDate() + 4 - (d.getUTCDay() || 7));
  const n = Math.ceil(((d - Date.UTC(d.getUTCFullYear(), 0, 1)) / 86400000 + 1) / 7);
  return `${d.getUTCFullYear()}-W${String(n).padStart(2, '0')}`;
}

const hex = (bytes) => Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');

async function sha256(text) {
  return hex(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))));
}

function equal(a, b) {
  let diff = a.length ^ b.length;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

function json(body, status = 200, headers = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', 'x-content-type-options': 'nosniff', ...headers },
  });
}
