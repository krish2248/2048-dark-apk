package com.personal.game2048.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.game2048.game.GameMode
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.game.Rank
import com.personal.game2048.game.allCountries
import com.personal.game2048.game.countryName
import com.personal.game2048.game.flag
import com.personal.game2048.game.fmt
import com.personal.game2048.game.tileLabel
import com.personal.game2048.net.ApiException
import com.personal.game2048.net.CountryEntry
import com.personal.game2048.net.CountryPage
import com.personal.game2048.net.Entry
import com.personal.game2048.net.Page
import com.personal.game2048.net.Scope
import com.personal.game2048.ui.components.CardShape
import com.personal.game2048.ui.components.Chips
import com.personal.game2048.ui.components.PrimaryButton
import com.personal.game2048.ui.components.RankBadge
import com.personal.game2048.ui.components.ScreenFrame
import com.personal.game2048.ui.components.SecondaryButton
import com.personal.game2048.ui.components.Segments
import com.personal.game2048.ui.components.SquareIcon
import com.personal.game2048.ui.components.TopBar
import com.personal.game2048.ui.theme.App
import com.personal.game2048.util.guessCountry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(vm: GameViewModel, onBack: () -> Unit) {
    val lb = vm.leaderboard
    val scope = rememberCoroutineScope()
    val profile by vm.store.profile.collectAsState()
    val settings by vm.store.settings.collectAsState()
    val standings by lb.standings.collectAsState()
    var modeIndex by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Entry?>(null) }
    val mode = GameMode.rankedModes[modeIndex]
    val view = Scope.entries[tab]

    var page by remember(mode, view) { mutableStateOf(if (view == Scope.NATIONS) null else lb.cachedPage(mode, view)) }
    var nations by remember(mode, view) { mutableStateOf(if (view == Scope.NATIONS) lb.cachedNations(mode) else null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember(mode, view) { mutableStateOf<String?>(null) }

    suspend fun load() {
        if (!lb.configured) return
        loading = true
        error = null
        try {
            if (view == Scope.NATIONS) nations = lb.nations(mode) else page = lb.page(mode, view)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            error = e.message
        } catch (e: Exception) {
            error = "You're offline. Showing the last results we have."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { lb.sync() }
    LaunchedEffect(mode, view, profile, standings) { if (view != Scope.COUNTRY || profile != null) load() }

    ScreenFrame {
        TopBar("World ranking", onBack) {
            if (lb.configured) SquareIcon(Icons.Rounded.Edit, "Edit profile") { editing = true } else Spacer(Modifier.size(48.dp))
        }
        if (!lb.configured) {
            Message(Icons.Rounded.CloudOff, "Ranking isn't live yet", "This build isn't connected to a leaderboard server. Your scores are saved and will be sent once it is.")
            return@ScreenFrame
        }
        Segments(GameMode.rankedModes.map { it.title }, modeIndex) { modeIndex = it }
        Spacer(Modifier.height(10.dp))
        Chips(Scope.entries.map { it.title }, tab) { tab = it }
        Spacer(Modifier.height(10.dp))

        if (profile == null) JoinCard { editing = true }
        error?.let { Text(it, style = App.type.bodySmall, color = App.colors.muted, modifier = Modifier.padding(vertical = 6.dp)) }

        PullToRefreshBox(loading && (page != null || nations != null), { scope.launch { load() } }, Modifier.weight(1f).fillMaxWidth()) {
            when {
                view == Scope.COUNTRY && profile == null -> Message(Icons.Rounded.Public, "Pick your flag first", "Join the ranking to see how you compare in your country.")
                view == Scope.NATIONS -> NationsList(nations, loading, error)
                else -> PlayersList(page, settings.hidden, profile?.id, loading, error) { selected = it }
            }
        }
        val me = page?.me
        if (profile != null && me != null && view != Scope.NATIONS) {
            Box(Modifier.padding(vertical = 10.dp)) { PlayerRow(me, mine = true) {} }
        }
    }

    if (editing) ProfileSheet(vm) { editing = false }
    selected?.let { e ->
        var sent by remember(e.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("${flag(e.country)}  ${e.name}", style = App.type.titleLarge) },
            text = {
                Text(
                    "${countryName(e.country)}  ·  ${Rank.of(e.classic).title}\n${fmt(e.score)} points, top tile ${tileLabel(e.exp)}" +
                        if (sent) "\n\nThanks, this name has been reported for review." else "",
                    style = App.type.bodyLarge,
                )
            },
            confirmButton = { TextButton({ selected = null }) { Text("Close", style = App.type.labelLarge) } },
            dismissButton = {
                if (e.id != profile?.id) Row {
                    if (!sent && profile != null) TextButton({
                        scope.launch { runCatching { lb.report(e.id) }.onSuccess { sent = true } }
                    }) { Text("Report name", style = App.type.labelLarge, color = App.colors.danger) }
                    TextButton({
                        vm.store.updateSettings { it.copy(hidden = it.hidden + e.id) }
                        selected = null
                    }) { Text("Hide", style = App.type.labelLarge, color = App.colors.muted) }
                }
            },
            containerColor = App.colors.surface,
        )
    }
}

@Composable
private fun JoinCard(onJoin: () -> Unit) {
    val c = App.colors
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(CardShape).background(c.accentSoft).border(1.dp, c.accent.copy(alpha = 0.5f), CardShape).padding(16.dp)) {
        Text("Put your flag on the board", style = App.type.titleMedium, color = c.text)
        Text("Choose a name and country. No email or password needed.", style = App.type.bodyMedium, color = c.muted)
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Join the ranking", Modifier.fillMaxWidth(), onClick = onJoin)
    }
}

@Composable
private fun PlayersList(page: Page?, hidden: Set<String>, me: String?, loading: Boolean, error: String?, onSelect: (Entry) -> Unit) {
    val rows = page?.entries?.filter { it.id !in hidden }
    when {
        rows == null && (loading || error == null) -> Skeleton()
        rows == null -> Message(Icons.Rounded.CloudOff, "Couldn't load the ranking", "Check your connection and pull down to try again.")
        rows.isEmpty() -> Message(Icons.Rounded.Public, "No scores here yet", "Finish a run and you'll be number one.")
        else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(rows, key = { it.id }) { e -> PlayerRow(e, e.id == me) { onSelect(e) } }
            item {
                Text(
                    "${fmt(page.total)} ranked players. Tap a player for details.", style = App.type.bodySmall, color = App.colors.muted,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun PlayerRow(e: Entry, mine: Boolean, onClick: () -> Unit) {
    val c = App.colors
    Row(
        Modifier.fillMaxWidth().clip(CardShape).background(if (mine) c.accentSoft else c.surface)
            .border(1.dp, if (mine) c.accent else c.border, CardShape)
            .combinedClickable(onClick = onClick, onLongClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Position(e.rank)
        Text(flag(e.country), fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Column(Modifier.weight(1f)) {
            Text(e.name, style = App.type.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${countryName(e.country)}  ·  ${Rank.of(e.classic).title}", style = App.type.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
            Text(fmt(e.score), style = App.type.titleSmall, color = c.accent, maxLines = 1)
            Text("tile ${tileLabel(e.exp)}", style = App.type.bodySmall, color = c.muted, maxLines = 1)
        }
        Spacer(Modifier.width(10.dp))
        RankBadge(Rank.of(e.classic), 32.dp)
    }
}

@Composable
private fun Position(rank: Int) {
    val c = App.colors
    val medal = when (rank) { 1 -> Color(0xFFF2C14E); 2 -> Color(0xFFC3CCD3); 3 -> Color(0xFFCB8B5C); else -> null }
    Box(
        Modifier.width(if (rank > 999) 46.dp else 34.dp).height(28.dp).clip(RoundedCornerShape(9.dp)).background(medal ?: Color.Transparent),
        contentAlignment = Alignment.Center,
    ) { Text("$rank", style = App.type.titleSmall, color = if (medal != null) Color(0xFF1A1405) else c.muted, maxLines = 1) }
}

@Composable
private fun NationsList(page: CountryPage?, loading: Boolean, error: String?) {
    val c = App.colors
    when {
        page == null && (loading || error == null) -> Skeleton()
        page == null -> Message(Icons.Rounded.CloudOff, "Couldn't load the nations", "Check your connection and pull down to try again.")
        page.entries.isEmpty() -> Message(Icons.Rounded.Public, "No nations yet", "Be the first to raise a flag.")
        else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(page.entries, key = { it.country }) { e: CountryEntry ->
                Row(
                    Modifier.fillMaxWidth().clip(CardShape).background(c.surface).border(1.dp, c.border, CardShape).padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Position(e.rank)
                    Text(flag(e.country), fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(countryName(e.country), style = App.type.titleSmall, color = c.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${fmt(e.players)} player${if (e.players == 1) "" else "s"}, led by ${e.champion}", style = App.type.bodySmall, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(fmt(e.score), style = App.type.titleSmall, color = c.accent)
                }
            }
        }
    }
}

@Composable
private fun Skeleton() {
    val c = App.colors
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(7) { Box(Modifier.fillMaxWidth().height(56.dp).clip(CardShape).background(c.surface)) }
    }
}

@Composable
private fun Message(icon: ImageVector, title: String, text: String) {
    val c = App.colors
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillParentMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = c.muted, modifier = Modifier.size(44.dp))
                Spacer(Modifier.height(14.dp))
                Text(title, style = App.type.titleLarge, color = c.text, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text(text, style = App.type.bodyMedium, color = c.muted, textAlign = TextAlign.Center)
            }
        }
    }
}

// ---- profile ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(vm: GameViewModel, onClose: () -> Unit) {
    val c = App.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile = remember { vm.store.profile.value }
    var name by rememberSaveable { mutableStateOf(profile?.name ?: "") }
    var country by rememberSaveable { mutableStateOf(profile?.country ?: guessCountry(context)) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = name.trim().length in 3..16

    ModalBottomSheet(onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = c.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            if (picking) {
                OutlinedTextField(
                    query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, textStyle = App.type.bodyLarge,
                    placeholder = { Text("Search countries", style = App.type.bodyLarge) }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(8.dp))
                val list = remember(query) { allCountries.filter { countryName(it).contains(query.trim(), ignoreCase = true) || it.equals(query.trim(), ignoreCase = true) }
                        .sortedByDescending { query.isNotBlank() && countryName(it).startsWith(query.trim(), ignoreCase = true) } }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(list, key = { it }) { code ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { country = code; picking = false; query = "" }.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(flag(code), fontSize = 24.sp)
                            Spacer(Modifier.width(14.dp))
                            Text(countryName(code), style = App.type.bodyLarge, color = if (code == country) c.accent else c.text)
                        }
                    }
                }
                return@Column
            }
            Text(if (profile == null) "Join the ranking" else "Your profile", style = App.type.titleLarge, color = c.text)
            Spacer(Modifier.height(4.dp))
            Text("Your name and flag are public. Please don't use your real full name.", style = App.type.bodyMedium, color = c.muted)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                name, { v -> name = v.filter { it.isLetterOrDigit() || it in " _-." }.take(16); error = null }, Modifier.fillMaxWidth(),
                singleLine = true, textStyle = App.type.bodyLarge, label = { Text("Player name", style = App.type.bodyMedium) },
                supportingText = { Text("3 to 16 letters or numbers", style = App.type.bodySmall) }, isError = error != null, shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton("${flag(country)}   ${countryName(country)}", Modifier.fillMaxWidth(), sub = "Tap to change your flag") { picking = true }
            error?.let { Text(it, style = App.type.bodyMedium, color = c.danger, modifier = Modifier.padding(top = 10.dp)) }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(if (busy) "One moment" else if (profile == null) "Join" else "Save", Modifier.fillMaxWidth(), enabled = valid && !busy) {
                busy = true
                scope.launch {
                    try {
                        vm.leaderboard.join(name.trim(), country)
                        onClose()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: ApiException) {
                        error = e.message
                    } catch (e: Exception) {
                        error = "Couldn't reach the server. Check your connection and try again."
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }
}
