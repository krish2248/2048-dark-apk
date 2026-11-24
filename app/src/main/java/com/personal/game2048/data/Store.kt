package com.personal.game2048.data

import android.content.Context
import android.util.AtomicFile
import com.personal.game2048.game.GameMode
import com.personal.game2048.game.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/**
 * All local state: in memory for the UI, mirrored to small JSON files.
 * Writes are debounced on a background thread and atomic, so a killed process can't corrupt a save.
 */
class Store private constructor(context: Context) {
    private val app = context.applicationContext
    private val dir = File(app.filesDir, "state").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val dirty = HashMap<String, () -> String>()
    private val wake = Channel<Unit>(Channel.CONFLATED)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _stats: MutableStateFlow<Stats>
    private val _settings: MutableStateFlow<Settings>
    private val _profile: MutableStateFlow<Profile?>
    private val _runs: MutableStateFlow<Map<String, SavedRun>>
    private val pendingRuns: MutableStateFlow<Map<String, PendingRun>>

    val stats: StateFlow<Stats> get() = _stats
    val settings: StateFlow<Settings> get() = _settings
    val profile: StateFlow<Profile?> get() = _profile
    val runs: StateFlow<Map<String, SavedRun>> get() = _runs
    val pending: StateFlow<Map<String, PendingRun>> get() = pendingRuns

    private val runsSerializer = MapSerializer(String.serializer(), SavedRun.serializer())
    private val pendingSerializer = MapSerializer(String.serializer(), PendingRun.serializer())

    init {
        val existing = read("stats", Stats.serializer())
        _stats = MutableStateFlow(existing ?: migrateStats())
        _settings = MutableStateFlow(read("settings", Settings.serializer()) ?: migrateSettings())
        _profile = MutableStateFlow(read("profile", Profile.serializer()))
        _runs = MutableStateFlow(read("runs", runsSerializer) ?: migrateRun())
        pendingRuns = MutableStateFlow(read("pending", pendingSerializer) ?: emptyMap())
        if (existing == null) {
            persist("stats", Stats.serializer(), _stats.value)
            persist("runs", runsSerializer, _runs.value)
            persist("settings", Settings.serializer(), _settings.value)
        }
        scope.launch {
            for (u in wake) {
                delay(350)
                flush()
            }
        }
    }

    fun updateStats(f: (Stats) -> Stats) {
        _stats.update(f)
        persist("stats", Stats.serializer(), _stats.value)
    }

    fun updateSettings(f: (Settings) -> Settings) {
        _settings.update(f)
        persist("settings", Settings.serializer(), _settings.value)
    }

    fun setProfile(p: Profile?) {
        _profile.value = p
        if (p == null) delete("profile") else persist("profile", Profile.serializer(), p)
    }

    fun saveRun(run: SavedRun) {
        _runs.update { it + (run.mode to run) }
        persist("runs", runsSerializer, _runs.value)
    }

    fun clearRun(mode: GameMode) {
        _runs.update { it - mode.id }
        persist("runs", runsSerializer, _runs.value)
    }

    /** Keeps only the best unsent run per mode. */
    fun queueRun(run: PendingRun) {
        pendingRuns.update { cur ->
            val old = cur[run.mode]
            if (old != null && old.score >= run.score) cur else cur + (run.mode to run)
        }
        persist("pending", pendingSerializer, pendingRuns.value)
    }

    fun dequeueRun(run: PendingRun) {
        pendingRuns.update { cur -> if (cur[run.mode] == run) cur - run.mode else cur }
        persist("pending", pendingSerializer, pendingRuns.value)
    }

    /** Small untyped cache, used for the last leaderboard page so the screen works offline. */
    fun readCache(key: String): String? = runCatching { AtomicFile(file("cache_$key")).readFully().decodeToString() }.getOrNull()
    fun writeCache(key: String, value: String) = enqueue("cache_$key") { value }

    fun resetStats() {
        updateStats { Stats() }
        _runs.value = emptyMap()
        persist("runs", runsSerializer, _runs.value)
    }

    @Synchronized
    fun flush() {
        val batch = synchronized(dirty) { HashMap(dirty).also { dirty.clear() } }
        for ((key, encode) in batch) {
            val f = AtomicFile(file(key))
            val out = runCatching { f.startWrite() }.getOrNull() ?: continue
            try {
                out.write(encode().encodeToByteArray())
                f.finishWrite(out)
            } catch (e: Exception) {
                f.failWrite(out)
            }
        }
    }

    private fun <T> persist(key: String, s: KSerializer<T>, value: T) = enqueue(key) { json.encodeToString(s, value) }

    private fun enqueue(key: String, encode: () -> String) {
        synchronized(dirty) { dirty[key] = encode }
        wake.trySend(Unit)
    }

    private fun delete(key: String) {
        synchronized(dirty) { dirty.remove(key) }
        AtomicFile(file(key)).delete()
    }

    private fun file(key: String) = File(dir, "$key.json")

    private fun <T> read(key: String, s: KSerializer<T>): T? = runCatching {
        json.decodeFromString(s, AtomicFile(file(key)).readFully().decodeToString())
    }.getOrNull()

    // ---- one-time import of v1 SharedPreferences ----

    private fun expOf(value: Int) = if (value <= 1) 0 else 31 - Integer.numberOfLeadingZeros(value)

    private fun migrateStats(): Stats {
        val p = app.getSharedPreferences("g2048_stats", Context.MODE_PRIVATE)
        if (p.all.isEmpty()) return Stats()
        val top = expOf(p.getInt("g2048_highest_tile", 0))
        val winSecs = p.getInt("g2048_best_win_time_secs", 0)
        val now = System.currentTimeMillis()
        val lastDay = runCatching { LocalDate.parse(p.getString("g2048_last_played_date", "")).toEpochDay() }.getOrDefault(0L)
        return Stats(
            modes = mapOf(
                GameMode.CLASSIC.id to ModeStats(
                    games = p.getInt("g2048_total_games", 0),
                    finished = p.getInt("g2048_games_lost", 0),
                    wins = p.getInt("g2048_games_won", 0),
                    bestScore = p.getInt("g2048_best_score", 0).toLong(),
                    bestExp = top,
                    timeMs = p.getLong("g2048_total_seconds", 0) * 1000,
                    bestScoreMs = p.getInt("g2048_best_score_time_secs", 0) * 1000L,
                    fastestMs = if (winSecs > 0) mapOf(11 to winSecs * 1000L) else emptyMap(),
                    reached = if (top >= 7) mapOf(top to 1) else emptyMap(),
                )
            ),
            achievements = (p.getStringSet("g2048_achievements", emptySet()) ?: emptySet()).associateWith { now },
            currentStreak = p.getInt("g2048_current_streak", 0),
            longestStreak = maxOf(p.getInt("g2048_longest_streak", 0), p.getInt("g2048_current_streak", 0)),
            lastPlayedDay = lastDay,
        )
    }

    private fun migrateSettings(): Settings {
        val p = app.getSharedPreferences("g2048_settings", Context.MODE_PRIVATE)
        return Settings(theme = if (p.getBoolean("g2048_theme_dark", true)) ThemeMode.DARK else ThemeMode.LIGHT)
    }

    private fun migrateRun(): Map<String, SavedRun> {
        val p = app.getSharedPreferences("g2048_save", Context.MODE_PRIVATE)
        if (!p.getBoolean("g2048_save_active", false)) return emptyMap()
        val cells = runCatching {
            json.decodeFromString<List<Int>>(p.getString("g2048_save_board", "") ?: "").map(::expOf)
        }.getOrNull()?.takeIf { it.size == 16 } ?: return emptyMap()
        val score = p.getInt("g2048_save_score", 0).toLong()
        return mapOf(
            GameMode.CLASSIC.id to SavedRun(
                mode = GameMode.CLASSIC.id,
                seed = 0,
                state = Snapshot(cells, score, System.nanoTime().toInt()),
                elapsedMs = p.getInt("g2048_save_elapsed_secs", 0) * 1000L,
                moves = 1,
                verifiable = false,
                topExp = cells.max(),
                peakScore = score,
            )
        )
    }

    companion object {
        @Volatile private var instance: Store? = null
        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context).also { instance = it }
        }
    }
}
