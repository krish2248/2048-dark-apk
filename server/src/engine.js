// Port of app/.../game/Engine.kt. The rules and the order of random draws must stay identical:
// test/vectors.json is checked by both this package and the Android unit tests.

export const MODES = {
  classic: { size: 4, undos: 5, maxMoves: 150000 },
  grand: { size: 5, undos: 5, maxMoves: 150000 },
  // 3 minutes; nobody swipes 10 times a second for the whole round
  blitz: { size: 4, undos: 0, maxMoves: 1800 },
};
const UNDO_DEPTH = 5;

export class Engine {
  constructor(size, seed) {
    this.size = size;
    this.n = size * size;
    this.cells = new Uint8Array(this.n);
    this.rng = seed | 0;
    this.score = 0;
    this.spawn();
    this.spawn();
  }

  next() {
    this.rng = (this.rng + 0x6d2b79f5) | 0;
    let t = this.rng;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= (t + Math.imul(t ^ (t >>> 7), t | 61)) | 0;
    return (t ^ (t >>> 14)) >>> 0;
  }

  spawn() {
    const { cells, n } = this;
    let empty = 0;
    for (let i = 0; i < n; i++) if (cells[i] === 0) empty++;
    if (empty === 0) return;
    let pick = this.next() % empty;
    const exp = this.next() % 10 === 0 ? 2 : 1;
    for (let i = 0; i < n; i++) {
      if (cells[i] === 0 && pick-- === 0) {
        cells[i] = exp;
        return;
      }
    }
  }

  /** @param dir char code of U, D, L or R. Returns false if nothing moved. */
  move(dir) {
    const { cells, size } = this;
    let changed = false;
    for (let line = 0; line < size; line++) {
      let start, step;
      if (dir === 76) { start = line * size; step = 1; }
      else if (dir === 82) { start = line * size + size - 1; step = -1; }
      else if (dir === 85) { start = line; step = size; }
      else { start = (size - 1) * size + line; step = -size; }
      let write = 0;
      let mergeable = false;
      for (let k = 0; k < size; k++) {
        const from = start + k * step;
        const exp = cells[from];
        if (exp === 0) continue;
        const prev = start + (write - 1) * step;
        if (mergeable && cells[prev] === exp) {
          cells[from] = 0;
          cells[prev] = exp + 1;
          mergeable = false;
          this.score += 2 ** (exp + 1);
          changed = true;
        } else {
          const to = start + write * step;
          if (to !== from) {
            cells[to] = exp;
            cells[from] = 0;
            changed = true;
          }
          write++;
          mergeable = true;
        }
      }
    }
    if (changed) this.spawn();
    return changed;
  }
}

/** Replays a whole run. Returns the true result, or null if any step is illegal. */
export function replay(mode, seed, log) {
  const rules = MODES[mode];
  if (!rules || log.length > rules.maxMoves) return null;
  const e = new Engine(rules.size, seed);
  const ring = [];
  for (let i = 0; i < UNDO_DEPTH; i++) ring.push({ cells: new Uint8Array(e.n), score: 0, rng: 0 });
  let head = 0;
  let depth = 0;
  let undos = 0;
  let top = 0;
  let moves = 0;
  for (let i = 0; i < log.length; i++) {
    const c = log.charCodeAt(i);
    if (c === 90) {
      if (depth === 0 || ++undos > rules.undos) return null;
      head = (head + UNDO_DEPTH - 1) % UNDO_DEPTH;
      depth--;
      const s = ring[head];
      e.cells.set(s.cells);
      e.score = s.score;
      e.rng = s.rng;
      continue;
    }
    if (c !== 76 && c !== 82 && c !== 85 && c !== 68) return null;
    const s = ring[head];
    s.cells.set(e.cells);
    s.score = e.score;
    s.rng = e.rng;
    if (!e.move(c)) return null;
    head = (head + 1) % UNDO_DEPTH;
    if (depth < UNDO_DEPTH) depth++;
    moves++;
  }
  for (const v of e.cells) if (v > top) top = v;
  return { score: e.score, exp: top, moves, cells: Array.from(e.cells) };
}
