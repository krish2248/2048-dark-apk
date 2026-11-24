import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { Engine, replay } from '../src/engine.js';
import { cleanName, isCountry } from '../src/names.js';
import { week } from '../src/index.js';

const vectors = JSON.parse(readFileSync(new URL('./vectors.json', import.meta.url)));

test('replays match the vectors the Android engine is tested against', () => {
  for (const v of vectors) {
    const r = replay(v.mode, v.seed, v.log);
    assert.deepEqual({ score: r.score, exp: r.exp, cells: r.cells }, { score: v.score, exp: v.exp, cells: v.cells });
  }
});

test('illegal runs are rejected', () => {
  const v = vectors.find((x) => x.mode === 'classic');
  assert.equal(replay('classic', v.seed, v.log + 'X'), null, 'unknown symbol');
  assert.equal(replay('classic', v.seed, 'Z'), null, 'undo with no history');
  assert.equal(replay('nope', v.seed, v.log), null, 'unknown mode');
  assert.equal(replay('blitz', v.seed, 'L'.repeat(2000)), null, 'too many moves for blitz');
  // a swipe that changes nothing can't appear in a real log
  const e = new Engine(4, v.seed);
  let log = '';
  while (e.move(76)) log += 'L';
  assert.notEqual(replay('classic', v.seed, log), null);
  assert.equal(replay('classic', v.seed, log + 'L'), null, 'no-op move');
});

test('undo is limited to five per run', () => {
  const v = vectors.find((x) => x.mode === 'classic');
  const first = [...'LRUD'].find((d) => new Engine(4, v.seed).move(d.charCodeAt(0)));
  assert.notEqual(replay('classic', v.seed, (first + 'Z').repeat(5)), null);
  assert.equal(replay('classic', v.seed, (first + 'Z').repeat(6)), null);
  assert.equal(replay('blitz', v.seed, first + 'Z'), null, 'blitz has no undo');
});

test('a very long run verifies quickly', () => {
  const e = new Engine(5, 99);
  let log = '';
  const order = [76, 68, 82, 85];
  while (log.length < 60000) {
    const d = order.find((x) => e.move(x));
    if (!d) break;
    log += String.fromCharCode(d);
  }
  const t = performance.now();
  assert.ok(replay('grand', 99, log));
  console.log(`replayed ${log.length} moves in ${(performance.now() - t).toFixed(1)} ms`);
});

test('names', () => {
  assert.equal(cleanName('  Amr   Salah '), 'Amr Salah');
  assert.equal(cleanName('عمرو'), 'عمرو');
  for (const bad of ['ab', 'x'.repeat(17), 'f.u.c.k', 'Sh1tty', '<b>hi</b>', '....', 'Admin', null, 'a‮b c']) assert.equal(cleanName(bad), null, String(bad));
  for (const ok of ['Dickson', 'Hitchcock', 'Krish_22', 'José']) assert.equal(cleanName(ok), ok);
});

test('countries and weeks', () => {
  assert.ok(isCountry('EG') && isCountry('IN'));
  assert.ok(!isCountry('ZZ') && !isCountry('eg') && !isCountry(undefined));
  assert.equal(week(Date.UTC(2026, 10, 24)), '2026-W48');
  assert.equal(week(Date.UTC(2027, 0, 1)), '2026-W53');
});
