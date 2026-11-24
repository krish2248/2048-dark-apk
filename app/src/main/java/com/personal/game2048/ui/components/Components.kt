package com.personal.game2048.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.game2048.game.fmt
import com.personal.game2048.ui.theme.App
import kotlinx.coroutines.launch

val CardShape = RoundedCornerShape(16.dp)
val ButtonShape = RoundedCornerShape(16.dp)

/** Content stays comfortable on tablets and foldables. */
val MaxContent = 560.dp

/** Clickable that gently presses in. */
fun Modifier.pressable(enabled: Boolean = true, role: Role = Role.Button, label: String? = null, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f, spring(stiffness = 900f, dampingRatio = 0.6f), label = "press")
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClickLabel = label, onClick = onClick)
}

@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    val c = App.colors
    Column(
        modifier.clip(CardShape).background(c.surface).border(1.dp, c.border, CardShape).padding(padding),
        content = content,
    )
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, sub: String? = null, onClick: () -> Unit) {
    val c = App.colors
    ButtonBody(text, sub, icon, if (enabled) c.accent else c.raised, if (enabled) c.onAccent else c.muted, null, modifier.pressable(enabled, onClick = onClick))
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, sub: String? = null, danger: Boolean = false, onClick: () -> Unit) {
    val c = App.colors
    val ink = when { !enabled -> c.muted; danger -> c.danger; else -> c.text }
    ButtonBody(text, sub, icon, c.surface, ink, c.border, modifier.pressable(enabled, onClick = onClick))
}

@Composable
private fun ButtonBody(text: String, sub: String?, icon: ImageVector?, bg: Color, ink: Color, border: Color?, modifier: Modifier) {
    Row(
        modifier
            .heightIn(min = 54.dp)
            .clip(ButtonShape)
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, ButtonShape) else Modifier)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Column(horizontalAlignment = if (icon != null) Alignment.Start else Alignment.CenterHorizontally) {
            Text(text, style = App.type.titleMedium, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = App.type.bodySmall, color = ink.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun SquareIcon(icon: ImageVector, label: String, modifier: Modifier = Modifier, tint: Color = App.colors.text, size: Dp = 48.dp, onClick: () -> Unit) {
    val c = App.colors
    Box(
        modifier
            .pressable(label = label, onClick = onClick)
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun TopBar(title: String, onBack: () -> Unit, trailing: @Composable () -> Unit = { Spacer(Modifier.size(48.dp)) }) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        SquareIcon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack)
        Text(
            title, Modifier.weight(1f).semantics { heading() }, style = App.type.titleLarge, color = App.colors.text, textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        trailing()
    }
}

/** Centered column, capped width, for every secondary screen. */
@Composable
fun ScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = MaxContent).fillMaxSize().padding(horizontal = 16.dp), content = content)
    }
}

@Composable
fun Segments(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = App.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface).border(1.dp, c.border, RoundedCornerShape(14.dp)).padding(4.dp),
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            val bg by animateColorAsState(if (on) c.accentSoft else Color.Transparent, tween(160), label = "seg")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(bg)
                    .selectable(on, role = Role.Tab) { onSelect(i) }
                    .heightIn(min = 40.dp).padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(o, style = App.type.labelMedium, color = if (on) c.accent else c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun Chips(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val c = App.colors
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent else c.surface)
                    .border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50))
                    .selectable(on, role = Role.Tab) { onSelect(i) }
                    .heightIn(min = 40.dp).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text(o, style = App.type.labelMedium, color = if (on) c.onAccent else c.text, maxLines = 1) }
        }
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = App.colors.accent, height: Dp = 8.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "progress")
    val track = App.colors.raised
    Canvas(modifier.fillMaxWidth().height(height)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        if (p > 0f) drawRoundRect(color, size = Size((size.width * p).coerceAtLeast(size.height), size.height), cornerRadius = r)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f).semantics { heading() }, style = App.type.titleMedium, color = App.colors.text)
        if (trailing != null) Text(trailing, style = App.type.labelMedium, color = App.colors.muted)
    }
}

@Composable
fun RowScope.StatTile(label: String, value: String, accent: Boolean = false) {
    Panel(Modifier.weight(1f), padding = 14.dp) {
        Text(value, style = App.type.titleLarge, color = if (accent) App.colors.accent else App.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 20.sp)
        Text(label, style = App.type.bodySmall, color = App.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Score box whose number pops when it grows, with the points just earned rising off it. */
@Composable
fun ScoreBox(label: String, value: Long, modifier: Modifier = Modifier, showGain: Boolean = false) {
    val c = App.colors
    val pop = remember { Animatable(1f) }
    val rise = remember { Animatable(1f) }
    val last = remember { longArrayOf(value) }
    var gain by remember { mutableLongStateOf(0L) }
    LaunchedEffect(value) {
        val delta = value - last[0]
        last[0] = value
        if (delta <= 0) return@LaunchedEffect
        if (showGain) {
            gain = delta
            launch {
                rise.snapTo(0f)
                rise.animateTo(1f, tween(650))
            }
        }
        pop.snapTo(1.12f)
        pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    Box(modifier.clip(CardShape).background(c.surface).border(1.dp, c.border, CardShape)) {
        Column(Modifier.align(Alignment.Center).padding(vertical = 9.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = App.type.labelSmall, color = c.muted)
            val text = fmt(value)
            Text(
                text, style = App.type.titleLarge, color = c.accent, maxLines = 1,
                fontSize = if (text.length > 9) 17.sp else 22.sp,
                modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
            )
        }
        if (gain > 0) Text(
            "+${fmt(gain)}", style = App.type.labelMedium, color = c.gold, maxLines = 1,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp).graphicsLayer {
                alpha = 1f - rise.value * rise.value
                translationY = (1f - rise.value) * 14.dp.toPx() - 4.dp.toPx()
            },
        )
    }
}
