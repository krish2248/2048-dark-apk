package com.personal.game2048.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.personal.game2048.data.Stats
import com.personal.game2048.game.Achievements
import com.personal.game2048.game.GameMode
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.game.Goals
import com.personal.game2048.game.Rank
import com.personal.game2048.game.flag
import com.personal.game2048.game.fmt
import com.personal.game2048.ui.components.CardShape
import com.personal.game2048.ui.components.PrimaryButton
import com.personal.game2048.ui.components.ProgressBar
import com.personal.game2048.ui.components.RankBadge
import com.personal.game2048.ui.components.SecondaryButton
import com.personal.game2048.ui.components.SquareIcon
import com.personal.game2048.ui.components.pressable
import com.personal.game2048.ui.components.rankColor
import com.personal.game2048.ui.theme.App
import com.personal.game2048.util.openUrl
import java.time.LocalDate

enum class Dest(val route: String) {
    HOME("home"), GAME("game"), LEADERBOARD("leaderboard"), STATS("stats"), ACHIEVEMENTS("achievements"), GOALS("goals"), RANKS("ranks"), SETTINGS("settings")
}

@Composable
fun HomeScreen(vm: GameViewModel, onPlay: (GameMode, Boolean) -> Unit, go: (Dest) -> Unit) {
    val c = App.colors
    val context = LocalContext.current
    val stats by vm.store.stats.collectAsState()
    val settings by vm.store.settings.collectAsState()
    val profile by vm.store.profile.collectAsState()
    val runs by vm.store.runs.collectAsState()
    val mode = GameMode.of(settings.lastMode)
    val saved = runs[mode.id]

    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val h = maxHeight
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = h).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.pressable { go(Dest.LEADERBOARD) }.height(48.dp).clip(RoundedCornerShape(14.dp)).background(c.surface)
                        .border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val p = profile
                    Text(
                        if (p != null) "${flag(p.country)}  ${p.name}" else "Join the world ranking",
                        style = App.type.labelLarge, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 190.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (stats.currentStreak > 0 && stats.lastPlayedDay >= LocalDate.now().toEpochDay() - 1) {
                    Icon(Icons.Rounded.LocalFireDepartment, "Day streak", tint = c.gold, modifier = Modifier.size(20.dp))
                    Text("${stats.currentStreak}", style = App.type.titleMedium, color = c.gold, modifier = Modifier.padding(start = 2.dp, end = 10.dp))
                }
                SquareIcon(Icons.Rounded.Settings, "Settings") { go(Dest.SETTINGS) }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 14.dp)) {
                Text("2048", style = App.type.displayLarge, color = c.text)
                Text(
                    if (stats.classicBest > 0) "Best ${fmt(stats.classicBest)}" else "Swipe. Merge. Never stop.",
                    style = App.type.titleMedium, color = c.accent,
                )
                Spacer(Modifier.height(16.dp))
                RankCard(stats) { go(Dest.RANKS) }
            }

            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameMode.entries.forEach { m -> ModeCard(m, m == mode, runs.containsKey(m.id), Modifier.weight(1f)) { vm.store.updateSettings { it.copy(lastMode = m.id) } } }
                }
                Text(
                    mode.tagline, style = App.type.bodyMedium, color = c.muted, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp),
                )
                if (saved != null) {
                    PrimaryButton("Continue", Modifier.fillMaxWidth(), Icons.Rounded.PlayArrow, sub = "${fmt(saved.state.score)} points so far") { onPlay(mode, false) }
                    Spacer(Modifier.height(10.dp))
                    SecondaryButton("New game", Modifier.fillMaxWidth(), Icons.Rounded.Add) { onPlay(mode, true) }
                } else {
                    PrimaryButton("Play ${mode.title}", Modifier.fillMaxWidth(), Icons.Rounded.PlayArrow) { onPlay(mode, true) }
                }
            }

            val today = LocalDate.now().toEpochDay()
            val daily = if (stats.daily.day == today) stats.daily else com.personal.game2048.data.DailyState(today)
            val goals = Goals.forDay(today)
            Column(Modifier.fillMaxWidth().padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuTile(Icons.Rounded.Leaderboard, "World ranking", "By flag and tier", Modifier.weight(1f)) { go(Dest.LEADERBOARD) }
                    MenuTile(Icons.Rounded.Flag, "Daily goals", "${goals.count { it.done(daily) }} of ${goals.size} done", Modifier.weight(1f)) { go(Dest.GOALS) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuTile(Icons.Rounded.EmojiEvents, "Achievements", "${stats.achievements.count { id -> Achievements.all.any { it.id == id.key } }} of ${Achievements.all.size}", Modifier.weight(1f)) { go(Dest.ACHIEVEMENTS) }
                    MenuTile(Icons.Rounded.BarChart, "Stats", if (stats.games == 1) "1 game" else "${fmt(stats.games)} games", Modifier.weight(1f)) { go(Dest.STATS) }
                }
            }

            Text(
                "Developed ~ sonikrish.com", style = App.type.bodySmall, color = c.muted,
                modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { openUrl(context, "https://sonikrish.com") }.padding(14.dp),
            )
        }
    }
}

@Composable
private fun RankCard(stats: Stats, onClick: () -> Unit) {
    val c = App.colors
    val rank = Rank.of(stats.classicBest)
    Row(
        Modifier.pressable(label = "All ranks", onClick = onClick).fillMaxWidth().clip(CardShape).background(c.surface).border(1.dp, c.border, CardShape).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(rank, 54.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rank.title, Modifier.weight(1f), style = App.type.titleMedium, color = c.text)
                Text("Level ${stats.level}", style = App.type.labelMedium, color = c.accent)
            }
            Spacer(Modifier.height(7.dp))
            ProgressBar(Rank.progress(stats.classicBest), color = rankColor(rank.family))
            Spacer(Modifier.height(5.dp))
            val next = rank.next
            Text(
                if (next != null) "${fmt(next.minScore - stats.classicBest)} more in one run for ${next.title}" else "You are at the very top",
                style = App.type.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ModeCard(m: GameMode, on: Boolean, inProgress: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = App.colors
    Column(
        modifier.clip(CardShape).background(if (on) c.accentSoft else c.surface).border(1.5.dp, if (on) c.accent else c.border, CardShape)
            .selectable(on, role = Role.RadioButton, onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            when (m) { GameMode.CLASSIC -> "4×4"; GameMode.GRAND -> "5×5"; GameMode.BLITZ -> "3:00"; GameMode.ZEN -> "∞" },
            style = App.type.titleLarge, color = if (on) c.accent else c.text, maxLines = 1,
        )
        Text(m.title, style = App.type.labelMedium, color = if (on) c.text else c.muted, maxLines = 1)
        Box(Modifier.padding(top = 5.dp).size(5.dp).clip(RoundedCornerShape(50)).background(if (inProgress) c.accent else androidx.compose.ui.graphics.Color.Transparent))
    }
}

@Composable
private fun MenuTile(icon: ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    val c = App.colors
    Row(
        modifier.pressable(onClick = onClick).clip(CardShape).background(c.surface).border(1.dp, c.border, CardShape).padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = c.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = App.type.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, style = App.type.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
