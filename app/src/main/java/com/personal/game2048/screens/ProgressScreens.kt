package com.personal.game2048.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.game2048.data.DailyState
import com.personal.game2048.data.GameRecord
import com.personal.game2048.data.Levels
import com.personal.game2048.data.combined
import com.personal.game2048.game.Achievement
import com.personal.game2048.game.Achievements
import com.personal.game2048.game.Direction
import com.personal.game2048.game.GameMode
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.game.Goals
import com.personal.game2048.game.Rank
import com.personal.game2048.game.clock
import com.personal.game2048.game.fmt
import com.personal.game2048.game.hoursMinutes
import com.personal.game2048.game.tileLabel
import com.personal.game2048.ui.components.CardShape
import com.personal.game2048.ui.components.Chips
import com.personal.game2048.ui.components.Panel
import com.personal.game2048.ui.components.ProgressBar
import com.personal.game2048.ui.components.RankBadge
import com.personal.game2048.ui.components.ScreenFrame
import com.personal.game2048.ui.components.SectionTitle
import com.personal.game2048.ui.components.SquareIcon
import com.personal.game2048.ui.components.StatTile
import com.personal.game2048.ui.components.TopBar
import com.personal.game2048.ui.components.rankColor
import com.personal.game2048.ui.theme.App
import com.personal.game2048.util.shareStatsPdf
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

// ---------------------------------------------------------------- stats

@Composable
fun StatsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val stats by vm.store.stats.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val mode = if (tab == 0) null else GameMode.entries[tab - 1]
    val m = if (mode == null) stats.combined() else stats.mode(mode)
    val history = stats.history.filter { mode == null || it.mode == mode.id }

    ScreenFrame {
        TopBar("Stats", onBack) { SquareIcon(Icons.Rounded.Share, "Share as PDF") { shareStatsPdf(context, stats) } }
        Chips(listOf("All") + GameMode.entries.map { it.title }, tab) { tab = it }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Tiles("Best score" to fmt(m.bestScore), "Top tile" to if (m.bestExp > 0) tileLabel(m.bestExp) else "None", accent = true) }
            item { Tiles("Games" to fmt(m.games), "Average score" to fmt(if (m.games > 0) m.totalScore / m.games else 0)) }
            item { Tiles("Reached 2048" to fmt(m.wins), "Win rate" to "${if (m.games > 0) m.wins * 100 / m.games else 0}%") }
            item { Tiles("Time played" to hoursMinutes(m.timeMs), "Average run" to clock(if (m.games > 0) m.timeMs / m.games else 0)) }
            item { Tiles("Moves" to fmt(m.moves), "Merges" to fmt(m.merges)) }
            item { Tiles("Moves per minute" to if (m.timeMs > 30_000) fmt(m.moves * 60_000 / m.timeMs) else "0", "Points per move" to if (m.moves > 0) "%.1f".format(m.totalScore.toDouble() / m.moves) else "0") }
            item { Tiles("Longest run" to clock(m.longestMs), "Most moves in a run" to fmt(m.mostMoves)) }
            item { Tiles("Best score time" to if (m.bestScoreMs > 0) clock(m.bestScoreMs) else "None", "Undos used" to fmt(m.undos)) }
            if (mode == null) {
                item { Tiles("Day streak" to "${stats.currentStreak}", "Best streak" to "${stats.longestStreak}") }
                item { Tiles("Days played" to fmt(stats.daysPlayed), "Daily goals done" to fmt(stats.goalsDone)) }
                item { Tiles("Biggest swipe" to "+${fmt(stats.biggestGain)}", "Most merges in a swipe" to "${stats.biggestCombo}") }
                item { Tiles("Level" to "${stats.level}", "Total XP" to fmt(stats.xp)) }
            }

            item { SectionTitle("Score trend", trailing = if (history.size > 1) "Last ${minOf(history.size, 20)} runs" else null) }
            item { HistoryChart(history) }

            if (m.reached.isNotEmpty()) {
                item { SectionTitle("Tiles reached") }
                item {
                    Panel {
                        val top = m.reached.values.max()
                        m.reached.toSortedMap(compareByDescending { it }).forEach { (exp, n) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(66.dp).clip(RoundedCornerShape(8.dp)).background(App.colors.palette.tile(exp)).padding(vertical = 5.dp), contentAlignment = Alignment.Center) {
                                    Text(tileLabel(exp), style = App.type.labelMedium, color = App.colors.palette.ink(exp))
                                }
                                Spacer(Modifier.width(12.dp))
                                ProgressBar(n.toFloat() / top, Modifier.weight(1f))
                                Text(
                                    m.fastestMs[exp]?.let { clock(it) } ?: "", style = App.type.bodySmall, color = App.colors.muted,
                                    textAlign = TextAlign.End, modifier = Modifier.width(62.dp),
                                )
                                Text("×$n", style = App.type.labelMedium, color = App.colors.text, textAlign = TextAlign.End, modifier = Modifier.width(48.dp))
                            }
                        }
                        Text("Times shown are your fastest", style = App.type.bodySmall, color = App.colors.muted, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }

            if (mode == null && stats.swipes.sum() > 0) {
                item { SectionTitle("Swipe directions") }
                item {
                    Panel {
                        val total = stats.swipes.sum()
                        Direction.entries.forEachIndexed { i, d ->
                            val n = stats.swipes.getOrElse(i) { 0 }
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(d.name.lowercase().replaceFirstChar { it.uppercase() }, style = App.type.labelMedium, color = App.colors.text, modifier = Modifier.width(66.dp))
                                ProgressBar(n.toFloat() / total, Modifier.weight(1f))
                                Text("${n * 100 / total}%", style = App.type.labelMedium, color = App.colors.text, textAlign = TextAlign.End, modifier = Modifier.width(52.dp))
                            }
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                item { SectionTitle("Recent games") }
                items(history.take(30)) { HistoryRow(it) }
            }
        }
    }
}

@Composable
private fun Tiles(a: Pair<String, String>, b: Pair<String, String>, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(a.first, a.second, accent)
        StatTile(b.first, b.second, accent)
    }
}

@Composable
private fun HistoryChart(history: List<GameRecord>) {
    val c = App.colors
    Panel {
        if (history.size < 2) {
            Text("Play a few games and your score trend shows up here.", style = App.type.bodyMedium, color = c.muted)
            return@Panel
        }
        val runs = history.take(20).reversed()
        val top = runs.maxOf { it.score }.coerceAtLeast(1)
        val avg = runs.sumOf { it.score } / runs.size
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text("Peak ${fmt(top)}", Modifier.weight(1f), style = App.type.labelMedium, color = c.accent)
            Text("Average ${fmt(avg)}", style = App.type.labelMedium, color = c.muted)
        }
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val gap = 4.dp.toPx()
            val w = ((size.width - gap * (runs.size - 1)) / runs.size).coerceAtMost(28.dp.toPx())
            runs.forEachIndexed { i, r ->
                val h = (size.height * (r.score.toFloat() / top)).coerceAtLeast(3.dp.toPx())
                drawRoundRect(
                    if (r.score == top) c.accent else c.accent.copy(alpha = 0.45f),
                    Offset(i * (w + gap), size.height - h), Size(w, h), CornerRadius(minOf(w / 3, 5.dp.toPx())),
                )
            }
            val y = size.height * (1 - avg.toFloat() / top)
            drawLine(c.muted.copy(alpha = 0.7f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
        }
    }
}

@Composable
private fun HistoryRow(r: GameRecord) {
    val c = App.colors
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(c.surface).border(1.dp, c.border, CardShape).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(58.dp).clip(RoundedCornerShape(8.dp)).background(c.palette.tile(r.exp)).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
            Text(tileLabel(r.exp), style = App.type.labelMedium, color = c.palette.ink(r.exp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(fmt(r.score), style = App.type.titleSmall, color = c.text)
            Text("${GameMode.of(r.mode).title}  ·  ${fmt(r.moves)} moves  ·  ${clock(r.ms)}", style = App.type.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(r.at)), style = App.type.bodySmall, color = c.muted)
    }
}

// ---------------------------------------------------------------- achievements

@Composable
fun AchievementsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val stats by vm.store.stats.collectAsState()
    val done = Achievements.all.count { it.id in stats.achievements }
    ScreenFrame {
        TopBar("Achievements", onBack)
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$done of ${Achievements.all.size} unlocked", Modifier.weight(1f), style = App.type.titleMedium, color = App.colors.text)
                Text("${done * 100 / Achievements.all.size}%", style = App.type.titleMedium, color = App.colors.gold)
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(done.toFloat() / Achievements.all.size, color = App.colors.gold)
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Achievements.groups.forEach { g ->
                val list = Achievements.all.filter { it.group == g }
                item { SectionTitle(g, trailing = "${list.count { it.id in stats.achievements }} / ${list.size}") }
                items(list.chunked(2)) { pair ->
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { a -> AchievementCard(a, a.id in stats.achievements, a.progress(stats), Modifier.weight(1f).fillMaxHeight()) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(a: Achievement, done: Boolean, progress: Long, modifier: Modifier) {
    val c = App.colors
    Column(
        modifier.clip(CardShape).background(c.surface).border(1.dp, if (done) c.gold.copy(alpha = 0.55f) else c.border, CardShape).padding(14.dp),
    ) {
        Icon(if (done) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock, if (done) "Unlocked" else "Locked", tint = if (done) c.gold else c.muted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(8.dp))
        Text(a.title, style = App.type.titleSmall, color = if (done) c.text else c.muted)
        Text(a.desc, style = App.type.bodySmall, color = c.muted)
        if (!done && a.target > 1) {
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(8.dp))
            ProgressBar(progress.toFloat() / a.target, height = 5.dp, color = c.gold)
        }
    }
}

// ---------------------------------------------------------------- goals

@Composable
fun GoalsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val c = App.colors
    val stats by vm.store.stats.collectAsState()
    val today = LocalDate.now().toEpochDay()
    val daily = if (stats.daily.day == today) stats.daily else DailyState(today)
    val goals = Goals.forDay(today)
    ScreenFrame {
        TopBar("Goals", onBack)
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Level ${stats.level}", Modifier.weight(1f), style = App.type.titleLarge, color = c.text)
                        Text("${fmt(Levels.xpFor(stats.level + 1) - stats.xp)} XP to level ${stats.level + 1}", style = App.type.labelMedium, color = c.muted)
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressBar(Levels.progress(stats.xp))
                }
            }
            item { SectionTitle("Today", trailing = "New goals at midnight") }
            items(goals) { g ->
                val p = g.progress(daily).coerceAtMost(g.target)
                val done = g.done(daily)
                Panel(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (done) Icons.Rounded.CheckCircle else Icons.Rounded.Flag, if (done) "Done" else null, tint = if (done) c.accent else c.muted, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(g.text, Modifier.weight(1f), style = App.type.titleSmall, color = c.text)
                        Text("+${g.xp} XP", style = App.type.labelMedium, color = c.gold)
                    }
                    Spacer(Modifier.height(10.dp))
                    ProgressBar(p.toFloat() / g.target)
                    if (!g.id.startsWith("tile_")) Text("${fmt(p)} / ${fmt(g.target)}", style = App.type.bodySmall, color = c.muted, modifier = Modifier.padding(top = 6.dp))
                }
            }
            item { SectionTitle("Next milestones") }
            val rank = Rank.of(stats.classicBest)
            rank.next?.let { n ->
                item { Milestone("Reach ${n.title}", "${fmt(stats.classicBest)} / ${fmt(n.minScore)} best Classic score", Rank.progress(stats.classicBest)) { RankBadge(n, 34.dp) } }
            }
            val nextTile = maxOf(stats.bestExp + 1, 7)
            item {
                Milestone("Build ${tileLabel(nextTile)}", if (stats.bestExp > 0) "Your best so far is ${tileLabel(stats.bestExp)}" else "Your first big tile", if (stats.bestExp > 0) (1L shl stats.bestExp).toFloat() / (1L shl nextTile) else 0f) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(c.palette.tile(nextTile)), contentAlignment = Alignment.Center) {
                        Text(tileLabel(nextTile), style = App.type.labelSmall, color = c.palette.ink(nextTile), maxLines = 1)
                    }
                }
            }
            val near = Achievements.all.filter { it.id !in stats.achievements && it.target > 1 }
                .sortedByDescending { it.progress(stats).toFloat() / it.target }.take(3)
            items(near) { a ->
                Milestone(a.title, a.desc, a.progress(stats).toFloat() / a.target) { Icon(Icons.Rounded.EmojiEvents, null, tint = c.gold, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}

@Composable
private fun Milestone(title: String, sub: String, progress: Float, icon: @Composable () -> Unit) {
    val c = App.colors
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = App.type.titleSmall, color = c.text)
                Text(sub, style = App.type.bodySmall, color = c.muted)
                Spacer(Modifier.height(7.dp))
                ProgressBar(progress, height = 5.dp)
            }
        }
    }
}

// ---------------------------------------------------------------- ranks

@Composable
fun RanksScreen(vm: GameViewModel, onBack: () -> Unit) {
    val c = App.colors
    val stats by vm.store.stats.collectAsState()
    val current = Rank.of(stats.classicBest)
    val list = Rank.entries.reversed()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (list.indexOf(current) - 2).coerceAtLeast(0))
    ScreenFrame {
        TopBar("Ranks", onBack)
        Text("Your best Classic score sets your rank.", style = App.type.bodyMedium, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
        LazyColumn(Modifier.weight(1f).fillMaxSize(), state, contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list) { r ->
                val reached = r.ordinal <= current.ordinal
                val mine = r == current
                Row(
                    Modifier.fillMaxWidth().clip(CardShape).background(if (mine) c.accentSoft else c.surface)
                        .border(if (mine) 1.5.dp else 1.dp, if (mine) rankColor(r.family) else c.border, CardShape).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RankBadge(r, 46.dp, locked = !reached)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = App.type.titleMedium, color = if (reached) c.text else c.muted)
                        Text(if (r.minScore == 0L) "Where everyone starts" else "${fmt(r.minScore)} points", style = App.type.bodySmall, color = c.muted)
                    }
                    if (mine) Text("You", style = App.type.labelLarge, color = c.accent)
                    else if (reached) Icon(Icons.Rounded.Check, "Reached", tint = c.accent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
