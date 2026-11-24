package com.personal.game2048.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.personal.game2048.game.Direction
import com.personal.game2048.game.GameMode
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.game.Notice
import com.personal.game2048.game.NoticeKind
import com.personal.game2048.game.Rank
import com.personal.game2048.game.clock
import com.personal.game2048.game.fmt
import com.personal.game2048.game.tileLabel
import com.personal.game2048.ui.board.BoardView
import com.personal.game2048.ui.board.swipes
import com.personal.game2048.ui.components.PrimaryButton
import com.personal.game2048.ui.components.ProgressBar
import com.personal.game2048.ui.components.RankBadge
import com.personal.game2048.ui.components.ScoreBox
import com.personal.game2048.ui.components.SecondaryButton
import com.personal.game2048.ui.components.SquareIcon
import com.personal.game2048.ui.components.pressable
import com.personal.game2048.ui.components.rankColor
import com.personal.game2048.ui.theme.App
import com.personal.game2048.util.shareText
import kotlinx.coroutines.delay

@Composable
fun GameScreen(vm: GameViewModel, onHome: () -> Unit, onStats: () -> Unit, onRanks: () -> Unit) {
    var confirmNew by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { vm.onPause() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val requestNew = { if (vm.moves > 10 && vm.gameOver == null) confirmNew = true else if (vm.gameOver != null) vm.playAgain() else vm.newGame() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionUp, Key.W -> vm.move(Direction.UP)
                    Key.DirectionDown, Key.S -> vm.move(Direction.DOWN)
                    Key.DirectionLeft, Key.A -> vm.move(Direction.LEFT)
                    Key.DirectionRight, Key.D -> vm.move(Direction.RIGHT)
                    Key.Z -> vm.undo()
                    else -> return@onKeyEvent false
                }
                true
            }
            .swipes(vm) { vm.move(it) },
    ) {
        val wide = maxWidth > maxHeight
        if (wide) {
            Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Board(vm, Modifier.fillMaxHeight())
                Column(Modifier.widthIn(max = 380.dp).weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Header(vm, onHome)
                    Scores(vm, requestNew)
                    GoalLine(vm)
                    Actions(vm, onStats, onRanks)
                }
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxSize().padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(8.dp))
                    Header(vm, onHome)
                    Spacer(Modifier.height(14.dp))
                    Scores(vm, requestNew)
                    Spacer(Modifier.height(12.dp))
                    GoalLine(vm)
                    Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) { Board(vm, Modifier) }
                    Actions(vm, onStats, onRanks)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        NoticeHost(vm, Modifier.align(Alignment.TopCenter))
    }

    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            title = { Text("Start a new game?", style = App.type.titleLarge) },
            text = { Text("This run ends here with ${fmt(vm.score)} points.", style = App.type.bodyLarge) },
            confirmButton = { TextButton({ confirmNew = false; vm.newGame() }) { Text("New game", style = App.type.labelLarge) } },
            dismissButton = { TextButton({ confirmNew = false }) { Text("Keep playing", style = App.type.labelLarge) } },
            containerColor = App.colors.surface,
        )
    }
}

@Composable
private fun Header(vm: GameViewModel, onHome: () -> Unit) {
    val c = App.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SquareIcon(Icons.Rounded.Home, "Home", onClick = onHome)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(vm.mode.title, style = App.type.headlineMedium, color = c.text)
        }
        Timer(vm)
    }
}

@Composable
private fun Timer(vm: GameViewModel) {
    val c = App.colors
    val urgent = vm.mode.timed && vm.seconds <= 15
    Row(
        Modifier.height(48.dp).clip(RoundedCornerShape(14.dp)).background(c.surface)
            .border(1.dp, if (urgent) c.danger else c.border, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Timer, "Time", tint = if (urgent) c.danger else c.muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(clock(vm.seconds * 1000L), style = App.type.titleMedium, color = if (urgent) c.danger else c.text)
    }
}

@Composable
private fun Scores(vm: GameViewModel, onNew: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ScoreBox("SCORE", vm.score, Modifier.weight(1f).fillMaxHeight(), showGain = vm.animator.effects)
        ScoreBox("BEST", vm.best, Modifier.weight(1f).fillMaxHeight())
        SquareIcon(Icons.Rounded.Refresh, "New game", size = 62.dp, onClick = onNew)
    }
}

/** The goal always sits one tile above your best in this run, so there is never a last one. */
@Composable
private fun GoalLine(vm: GameViewModel) {
    val c = App.colors
    if (vm.mode.timed) {
        val track = c.raised
        val normal = c.accent
        val danger = c.danger
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            drawRoundRect(if (vm.timeLeft < 0.084f) danger else normal, size = Size(size.width * vm.timeLeft, size.height), cornerRadius = r)
        }
        return
    }
    val target = maxOf(vm.topExp + 1, 11)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Flag, null, tint = c.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        AnimatedContent(target, transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) }, label = "goal") {
            Text("Goal ${tileLabel(it)}", style = App.type.labelMedium, color = c.text)
        }
        Spacer(Modifier.width(12.dp))
        ProgressBar(vm.topExp.toFloat() / target, Modifier.weight(1f), height = 6.dp)
        Spacer(Modifier.width(12.dp))
        Text("${fmt(vm.moves)} moves", style = App.type.labelMedium, color = c.muted)
    }
}

@Composable
private fun Board(vm: GameViewModel, modifier: Modifier) {
    val c = App.colors
    Box(
        modifier.semantics {
            contentDescription = "${vm.mode.size} by ${vm.mode.size} board, score ${vm.score}, top tile ${tileLabel(vm.topExp)}"
            customActions = Direction.entries.map { d ->
                CustomAccessibilityAction("Move ${d.name.lowercase()}") { vm.move(d); true }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        BoardView(vm.animator, Modifier)

        AnimatedVisibility(vm.paused, Modifier.matchParentSize(), enter = fadeIn(tween(120)), exit = fadeOut(tween(160))) {
            Column(
                Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(c.board).border(1.dp, c.border, RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Text("Paused", style = App.type.headlineMedium, color = c.text)
                Text("${clock(vm.seconds * 1000L)} left on the clock", style = App.type.bodyMedium, color = c.muted)
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Resume", Modifier.widthIn(min = 180.dp), Icons.Rounded.PlayArrow) { vm.resume() }
            }
        }
        AnimatedVisibility(
            vm.gameOver != null, Modifier.matchParentSize(),
            enter = fadeIn(tween(380, delayMillis = 380)) + scaleIn(tween(380, delayMillis = 380), initialScale = 0.96f), exit = fadeOut(tween(140)),
        ) { GameOverPanel(vm) }
    }
}

@Composable
private fun GameOverPanel(vm: GameViewModel) {
    val c = App.colors
    val context = LocalContext.current
    val standings by vm.leaderboard.standings.collectAsState()
    val stats by vm.store.stats.collectAsState()
    val over = remember { vm.gameOver } ?: return
    val place = standings[vm.mode.id]
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(c.board.copy(alpha = 0.97f)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(
            when { over.newBest -> "New personal best"; over.timeUp -> "Time's up"; else -> "Game over" },
            style = App.type.titleLarge, color = if (over.newBest) c.gold else c.text,
        )
        Text(fmt(vm.score), style = App.type.displayMedium, color = c.accent)
        Text("Top tile ${tileLabel(vm.topExp)}  ·  ${fmt(vm.moves)} moves", style = App.type.bodyMedium, color = c.muted)
        val line = when {
            vm.mode == GameMode.CLASSIC && place == null -> Rank.of(stats.classicBest).title
            place != null -> "World #${fmt(place.rank)} of ${fmt(place.total)}"
            else -> null
        }
        if (line != null) Text(line, style = App.type.labelLarge, color = c.gold, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Play again", Modifier.fillMaxWidth(), Icons.Rounded.Refresh) { vm.playAgain() }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!over.timeUp && vm.canUndo) SecondaryButton("Undo", Modifier.weight(1f), Icons.AutoMirrored.Rounded.Undo) { vm.undo() }
            SecondaryButton("Share", Modifier.weight(1f), Icons.Rounded.Share) {
                shareText(context, "I scored ${fmt(vm.score)} with a ${tileLabel(vm.topExp)} tile in 2048 Dark Edition (${vm.mode.title}). Can you beat it?")
            }
        }
    }
}

@Composable
private fun Actions(vm: GameViewModel, onStats: () -> Unit, onRanks: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        if (vm.mode.undoLimit > 0) Action(
            Icons.AutoMirrored.Rounded.Undo, "Undo",
            badge = when { vm.mode.undoLimit == Int.MAX_VALUE -> "∞"; else -> vm.undosLeft.toString() },
            enabled = vm.canUndo,
        ) { vm.undo() }
        Action(Icons.Rounded.Leaderboard, "Ranking", onClick = onRanks)
        Action(Icons.Rounded.BarChart, "Stats", onClick = onStats)
    }
}

@Composable
private fun Action(icon: ImageVector, label: String, badge: String? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val c = App.colors
    val ink = if (enabled) c.text else c.muted.copy(alpha = 0.6f)
    Column(Modifier.pressable(enabled, label = label, onClick = onClick).padding(horizontal = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Box(
                Modifier.padding(4.dp).size(54.dp).clip(CircleShape).background(c.surface).border(1.dp, c.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = ink, modifier = Modifier.size(24.dp)) }
            if (badge != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(if (enabled) c.accent else c.raised),
                    contentAlignment = Alignment.Center,
                ) { Text(badge, style = App.type.labelSmall, color = if (enabled) c.onAccent else c.muted) }
            }
        }
        Text(label, style = App.type.labelMedium, color = c.muted)
    }
}

/** One banner at a time, over the header; never blocks a swipe. */
@Composable
private fun NoticeHost(vm: GameViewModel, modifier: Modifier) {
    val current = vm.notices.firstOrNull()
    LaunchedEffect(current) {
        if (current != null) {
            delay(if (vm.notices.size > 1) 1500 else 2400)
            vm.dismissNotice(current)
        }
    }
    AnimatedContent(
        current, modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        transitionSpec = { (slideInVertically(spring(dampingRatio = 0.75f, stiffness = 380f)) { -it * 2 } + fadeIn()) togetherWith (slideOutVertically { -it * 2 } + fadeOut()) },
        contentAlignment = Alignment.TopCenter, label = "notice",
    ) { n -> if (n == null) Spacer(Modifier.height(1.dp)) else NoticeCard(n) }
}

@Composable
private fun NoticeCard(n: Notice) {
    val c = App.colors
    val tint: Color = when (n.kind) {
        NoticeKind.RANK -> n.rank?.let { rankColor(it.family) } ?: c.accent
        NoticeKind.ACHIEVEMENT, NoticeKind.RECORD -> c.gold
        else -> c.accent
    }
    Row(
        Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.raised)
            .border(1.5.dp, tint.copy(alpha = 0.7f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            n.rank != null -> RankBadge(n.rank, 40.dp)
            n.kind == NoticeKind.TILE -> Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(c.palette.tile(n.exp)), contentAlignment = Alignment.Center,
            ) { Text(tileLabel(n.exp), style = App.type.labelSmall, color = c.palette.ink(n.exp), textAlign = TextAlign.Center, maxLines = 1) }
            else -> Icon(
                when (n.kind) {
                    NoticeKind.GOAL -> Icons.Rounded.Flag
                    NoticeKind.LEVEL, NoticeKind.RECORD -> Icons.Rounded.Star
                    else -> Icons.Rounded.EmojiEvents
                },
                null, tint = tint, modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(n.title, style = App.type.titleMedium, color = c.text)
            Text(n.text, style = App.type.bodyMedium, color = c.muted)
        }
    }
}
