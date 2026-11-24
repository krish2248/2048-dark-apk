package com.personal.game2048.net

import com.personal.game2048.BuildConfig
import com.personal.game2048.data.PendingRun
import com.personal.game2048.data.Profile
import com.personal.game2048.data.Store
import com.personal.game2048.game.GameMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class Entry(
    val rank: Int,
    val id: String,
    val name: String,
    val country: String,
    val score: Long,
    val exp: Int,
    /** Best all-time Classic score, which decides the tier badge. */
    val classic: Long = 0,
)

@Serializable
data class Page(val entries: List<Entry> = emptyList(), val total: Int = 0, val me: Entry? = null)

@Serializable
data class CountryEntry(val rank: Int, val country: String, val players: Int, val score: Long, val champion: String)

@Serializable
data class CountryPage(val entries: List<CountryEntry> = emptyList())

@Serializable
data class SubmitResult(val best: Long, val improved: Boolean, val rank: Int, val total: Int, val countryRank: Int)

@Serializable
private data class NewPlayer(val id: String, val secret: String)

@Serializable
private data class PlayerBody(val name: String, val country: String)

@Serializable
private data class ScoreBody(val mode: String, val seed: Int, val log: String, val ms: Long)

@Serializable
private data class ReportBody(val id: String)

@Serializable
private data class ErrorBody(val error: String = "")

class ApiException(val code: Int, message: String) : IOException(message)

enum class Scope(val id: String, val title: String) {
    WORLD("world", "World"), COUNTRY("country", "My country"), WEEK("week", "This week"), NATIONS("nations", "Nations")
}

class Leaderboard(private val store: Store) {
    private val base = BuildConfig.LEADERBOARD_URL
    val configured: Boolean = base.isNotEmpty()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()

    private val _last = MutableStateFlow<Map<String, SubmitResult>>(emptyMap())
    /** Latest verified standing per mode, filled in as submissions land. */
    val standings: StateFlow<Map<String, SubmitResult>> = _last

    /** Sends any queued runs. Safe to call often; silently retries next time when offline. */
    fun sync() {
        if (!configured || store.profile.value == null) return
        scope.launch {
            syncLock.withLock {
                for (run in store.pending.value.values) {
                    val profile = store.profile.value ?: return@withLock
                    try {
                        val r: SubmitResult = json.decodeFromString(
                            call("POST", "/v1/scores", json.encodeToString(ScoreBody.serializer(), ScoreBody(run.mode, run.seed, run.log, run.ms)), profile)
                        )
                        _last.value = _last.value + (run.mode to r)
                        store.dequeueRun(run)
                    } catch (e: ApiException) {
                        // 429 and 5xx are worth retrying; anything else will never be accepted
                        if (e.code in 400..499 && e.code != 429 && e.code != 401) store.dequeueRun(run)
                    } catch (e: IOException) {
                        return@withLock
                    } catch (e: IllegalArgumentException) {
                        return@withLock
                    }
                }
            }
        }
    }

    fun submit(run: PendingRun) {
        store.queueRun(run)
        sync()
    }

    suspend fun join(name: String, country: String): Profile {
        val body = json.encodeToString(PlayerBody.serializer(), PlayerBody(name, country))
        val current = store.profile.value
        val profile = if (current == null) {
            val p = json.decodeFromString(NewPlayer.serializer(), call("POST", "/v1/players", body))
            Profile(p.id, p.secret, name, country)
        } else {
            call("PUT", "/v1/players/me", body, current)
            current.copy(name = name, country = country)
        }
        store.setProfile(profile)
        sync()
        return profile
    }

    suspend fun deleteProfile() {
        val p = store.profile.value ?: return
        try {
            call("DELETE", "/v1/players/me", null, p)
        } catch (e: ApiException) {
            if (e.code != 401) throw e // already gone on the server
        }
        _last.value = emptyMap()
        store.setProfile(null)
    }

    suspend fun page(mode: GameMode, scope: Scope): Page {
        val profile = store.profile.value
        val q = StringBuilder("/v1/leaderboard?mode=${mode.id}")
        if (scope == Scope.WEEK) q.append("&period=week")
        if (scope == Scope.COUNTRY && profile != null) q.append("&country=${profile.country}")
        if (profile != null) q.append("&me=${profile.id}")
        val text = call("GET", q.toString())
        store.writeCache("lb_${mode.id}_${scope.id}", text)
        return json.decodeFromString(text)
    }

    fun cachedPage(mode: GameMode, scope: Scope): Page? =
        store.readCache("lb_${mode.id}_${scope.id}")?.let { runCatching { json.decodeFromString<Page>(it) }.getOrNull() }

    suspend fun nations(mode: GameMode): CountryPage {
        val text = call("GET", "/v1/countries?mode=${mode.id}")
        store.writeCache("lb_${mode.id}_nations", text)
        return json.decodeFromString(text)
    }

    fun cachedNations(mode: GameMode): CountryPage? =
        store.readCache("lb_${mode.id}_nations")?.let { runCatching { json.decodeFromString<CountryPage>(it) }.getOrNull() }

    suspend fun report(id: String) {
        call("POST", "/v1/report", json.encodeToString(ReportBody.serializer(), ReportBody(id)), store.profile.value)
    }

    private suspend fun call(method: String, path: String, body: String? = null, auth: Profile? = null): String =
        withContext(Dispatchers.IO) {
            val c = URL(base + path).openConnection() as HttpURLConnection
            try {
                c.requestMethod = method
                c.connectTimeout = 8_000
                c.readTimeout = 12_000
                c.setRequestProperty("Accept", "application/json")
                if (auth != null) c.setRequestProperty("Authorization", "Bearer ${auth.id}.${auth.secret}")
                if (body != null) {
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    val bytes = body.encodeToByteArray()
                    c.setFixedLengthStreamingMode(bytes.size)
                    c.outputStream.use { it.write(bytes) }
                }
                val code = c.responseCode
                val text = (if (code in 200..299) c.inputStream else c.errorStream)
                    ?.use { it.readBytes().decodeToString() }.orEmpty()
                if (code !in 200..299) {
                    val msg = runCatching { json.decodeFromString(ErrorBody.serializer(), text).error }.getOrDefault("")
                    throw ApiException(code, msg.ifBlank { "Server error ($code)" })
                }
                text
            } finally {
                c.disconnect()
            }
        }
}
