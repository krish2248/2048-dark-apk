CREATE TABLE players (
  id          TEXT PRIMARY KEY,
  secret_hash TEXT NOT NULL,
  name        TEXT NOT NULL,
  country     TEXT NOT NULL,
  banned      INTEGER NOT NULL DEFAULT 0,
  created_at  INTEGER NOT NULL
);

-- one row per player, mode and period ('all' or an ISO week such as '2026-W48')
CREATE TABLE scores (
  player_id TEXT NOT NULL REFERENCES players (id),
  mode      TEXT NOT NULL,
  period    TEXT NOT NULL,
  country   TEXT NOT NULL,
  score     INTEGER NOT NULL,
  exp       INTEGER NOT NULL,
  moves     INTEGER NOT NULL,
  ms        INTEGER NOT NULL,
  at        INTEGER NOT NULL,
  PRIMARY KEY (player_id, mode, period)
) WITHOUT ROWID;

CREATE INDEX scores_board ON scores (mode, period, score DESC, at);
CREATE INDEX scores_country ON scores (mode, period, country, score DESC, at);

CREATE TABLE reports (
  player_id   TEXT NOT NULL,
  reporter_id TEXT NOT NULL,
  at          INTEGER NOT NULL,
  PRIMARY KEY (player_id, reporter_id)
);
