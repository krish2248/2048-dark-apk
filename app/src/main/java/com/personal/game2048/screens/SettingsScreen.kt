package com.personal.game2048.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.personal.game2048.BuildConfig
import com.personal.game2048.data.SlideSpeed
import com.personal.game2048.data.ThemeMode
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.game.Rank
import com.personal.game2048.game.countryName
import com.personal.game2048.game.flag
import com.personal.game2048.ui.components.CardShape
import com.personal.game2048.ui.components.Panel
import com.personal.game2048.ui.components.ScreenFrame
import com.personal.game2048.ui.components.SecondaryButton
import com.personal.game2048.ui.components.SectionTitle
import com.personal.game2048.ui.components.Segments
import com.personal.game2048.ui.components.TopBar
import com.personal.game2048.ui.components.pressable
import com.personal.game2048.ui.theme.App
import com.personal.game2048.ui.theme.Palettes
import com.personal.game2048.util.openUrl
import kotlinx.coroutines.launch

const val PRIVACY_URL = "https://github.com/krish2248/2048-dark-apk/blob/master/PRIVACY.md"

@Composable
fun SettingsScreen(vm: GameViewModel, onBack: () -> Unit) {
    val c = App.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by vm.store.settings.collectAsState()
    val stats by vm.store.stats.collectAsState()
    val profile by vm.store.profile.collectAsState()
    val rank = Rank.of(stats.classicBest)
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    ScreenFrame {
        TopBar("Settings", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            SectionTitle("Theme", Modifier)
            Segments(ThemeMode.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, s.theme.ordinal) { i ->
                vm.store.updateSettings { it.copy(theme = ThemeMode.entries[i]) }
            }

            SectionTitle("Tile colors")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Palettes.forEach { p ->
                    val unlocked = rank.ordinal >= p.unlock.ordinal
                    val on = s.palette == p.id
                    Row(
                        Modifier.pressable(unlocked) { vm.store.updateSettings { it.copy(palette = p.id) } }.fillMaxWidth().clip(CardShape).background(c.surface)
                            .border(if (on) 1.5.dp else 1.dp, if (on) c.accent else c.border, CardShape).padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            p.preview.forEach { Box(Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(it.copy(alpha = if (unlocked) 1f else 0.35f))) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.title, style = App.type.titleSmall, color = if (unlocked) c.text else c.muted)
                            if (!unlocked) Text("Unlocks at ${p.unlock.title}", style = App.type.bodySmall, color = c.muted)
                        }
                        if (on) Icon(Icons.Rounded.Check, "Selected", tint = c.accent) else if (!unlocked) Icon(Icons.Rounded.Lock, "Locked", tint = c.muted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            SectionTitle("Slide speed")
            Segments(SlideSpeed.entries.map { it.name.lowercase().replaceFirstChar(Char::uppercase) }, s.speed.ordinal) { i ->
                vm.store.updateSettings { it.copy(speed = SlideSpeed.entries[i]) }
            }

            SectionTitle("Feel")
            Panel(Modifier.fillMaxWidth(), padding = 4.dp) {
                Toggle("Sound", "Merges ring higher as tiles grow", s.sound) { v -> vm.store.updateSettings { it.copy(sound = v) } }
                Toggle("Vibration", "Light taps as tiles move and merge", s.haptics) { v -> vm.store.updateSettings { it.copy(haptics = v) } }
                Toggle("Effects", "Particles, confetti and points earned", s.effects) { v -> vm.store.updateSettings { it.copy(effects = v) } }
            }

            SectionTitle("World ranking profile")
            val p = profile
            if (!vm.leaderboard.configured) {
                Text("This build isn't connected to a leaderboard server.", style = App.type.bodyMedium, color = c.muted)
            } else if (p == null) {
                SecondaryButton("Join the ranking", Modifier.fillMaxWidth()) { editing = true }
            } else {
                SecondaryButton("${flag(p.country)}   ${p.name}", Modifier.fillMaxWidth(), sub = "${countryName(p.country)}. Tap to edit") { editing = true }
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Delete online profile", Modifier.fillMaxWidth(), danger = true) { confirmDelete = true }
            }
            if (s.hidden.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SecondaryButton("Show ${s.hidden.size} hidden player${if (s.hidden.size == 1) "" else "s"}", Modifier.fillMaxWidth()) { vm.store.updateSettings { it.copy(hidden = emptySet()) } }
            }

            SectionTitle("Data")
            SecondaryButton("Reset stats on this device", Modifier.fillMaxWidth(), danger = true) { confirmReset = true }
            Spacer(Modifier.height(8.dp))
            SecondaryButton("Privacy policy", Modifier.fillMaxWidth()) { openUrl(context, PRIVACY_URL) }
            message?.let { Text(it, style = App.type.bodyMedium, color = c.muted, modifier = Modifier.padding(top = 10.dp)) }
            Text("Version ${BuildConfig.VERSION_NAME}", style = App.type.bodySmall, color = c.muted, modifier = Modifier.padding(vertical = 20.dp))
        }
    }

    if (editing) ProfileSheet(vm) { editing = false }
    if (confirmReset) Confirm(
        "Reset all stats?", "Scores, achievements, rank, level and saved games on this device are erased. Your online ranking stays.", "Reset",
        onDismiss = { confirmReset = false },
    ) {
        confirmReset = false
        vm.store.resetStats()
        vm.store.updateSettings { it.copy(palette = "emerald") }
        message = "Stats reset."
    }
    if (confirmDelete) Confirm(
        "Delete your online profile?", "Your name, flag and all ranked scores are permanently removed from the server.", "Delete",
        onDismiss = { confirmDelete = false },
    ) {
        confirmDelete = false
        scope.launch {
            message = runCatching { vm.leaderboard.deleteProfile() }
                .fold({ "Online profile deleted." }, { "Couldn't reach the server. Your profile was not deleted, please try again." })
        }
    }
}

@Composable
private fun Toggle(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).toggleable(on, role = Role.Switch, onValueChange = onChange).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = App.type.titleSmall, color = App.colors.text)
            Text(sub, style = App.type.bodySmall, color = App.colors.muted)
        }
        Switch(on, null)
    }
}

@Composable
private fun Confirm(title: String, text: String, action: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = App.type.titleLarge) },
        text = { Text(text, style = App.type.bodyLarge) },
        confirmButton = { TextButton(onConfirm) { Text(action, style = App.type.labelLarge, color = App.colors.danger) } },
        dismissButton = { TextButton(onDismiss) { Text("Cancel", style = App.type.labelLarge) } },
        containerColor = App.colors.surface,
    )
}
