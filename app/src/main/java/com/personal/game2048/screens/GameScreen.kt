package com.personal.game2048.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.game2048.game.Direction
import com.personal.game2048.game.GameViewModel
import com.personal.game2048.ui.theme.*
import kotlin.math.abs

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val bgColor = if (isDarkTheme) AppBackground else LightAppBackground
    val cardBg = if (isDarkTheme) CardBackground else LightCardBg
    val gridBg = if (isDarkTheme) GridBackground else LightGridBackground
    val emptyTileBg = if (isDarkTheme) TileEmpty else LightTileEmpty
    val accentColor = if (isDarkTheme) ScoreGreen else LightScoreGreen
    val subtitleColor = if (isDarkTheme) SubtitleText else LightSubtitleText
    val navBg = if (isDarkTheme) NavBarBackground else LightNavBarBackground
    val navBtnBg = if (isDarkTheme) NavButtonBg else LightNavButtonBg
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1A1A)
    val labelColor = if (isDarkTheme) Color(0xFF888888) else Color(0xFF999999)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.achievementMessage) {
        viewModel.achievementMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissAchievement()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToStats) {
                    Icon(
                        Icons.Filled.BarChart,
                        contentDescription = "Stats",
                        tint = accentColor
                    )
                }
                Text(
                    text = "2048",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = textColor
                )
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Score row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScoreCard(
                    label = "SCORE",
                    value = viewModel.score,
                    bgColor = cardBg,
                    accentColor = accentColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f)
                )
                ScoreCard(
                    label = "BEST",
                    value = viewModel.bestScore,
                    bgColor = cardBg,
                    accentColor = accentColor,
                    labelColor = labelColor,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { viewModel.startNewGame() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Restart",
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Join the numbers and get to the 2048 tile!",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = subtitleColor,
                    modifier = Modifier.weight(1f)
                )
                val minutes = viewModel.elapsedSeconds / 60
                val seconds = viewModel.elapsedSeconds % 60
                Text(
                    text = "%d:%02d".format(minutes, seconds),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = accentColor.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Game grid
            val density = LocalDensity.current
            val minSwipe = with(density) { 50.dp.toPx() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(gridBg)
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        var totalX = 0f
                        var totalY = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalX = 0f
                                totalY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                            },
                            onDragEnd = {
                                if (abs(totalX) >= minSwipe || abs(totalY) >= minSwipe) {
                                    val direction = if (abs(totalX) > abs(totalY)) {
                                        if (totalX > 0) Direction.RIGHT else Direction.LEFT
                                    } else {
                                        if (totalY > 0) Direction.DOWN else Direction.UP
                                    }
                                    viewModel.move(direction)
                                }
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (r in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until 4) {
                                val value = viewModel.grid[r][c]
                                TileCell(
                                    value = value,
                                    emptyColor = emptyTileBg,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // New Game button
            OutlinedButton(
                onClick = { viewModel.startNewGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDarkTheme) NewGameButtonBorder else MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isDarkTheme) NewGameButtonBg else LightCardBg
                )
            ) {
                Text(
                    text = "New Game",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom nav — Undo | Stats | Settings (NO Hint)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(navBg)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavButton(
                    icon = Icons.Filled.Undo,
                    label = "Undo",
                    tint = accentColor,
                    bgColor = navBtnBg,
                    onClick = {
                        if (!viewModel.undo()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "Nothing to undo",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                )
                NavButton(
                    icon = Icons.Filled.BarChart,
                    label = "Stats",
                    tint = NavIconDefault,
                    bgColor = navBtnBg,
                    onClick = onNavigateToStats
                )
                NavButton(
                    icon = Icons.Filled.Settings,
                    label = "Settings",
                    tint = NavIconDefault,
                    bgColor = navBtnBg,
                    onClick = onToggleTheme
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Snackbar host for achievements
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data ->
            Snackbar(
                containerColor = ScoreGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(data.visuals.message, fontFamily = FontFamily.Monospace)
            }
        }

        // Win dialog
        if (viewModel.showWinDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        "You Won!",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Congratulations! You reached 2048!",
                        fontFamily = FontFamily.Monospace
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.onWinKeepPlaying() }) {
                        Text("Keep Playing", color = accentColor, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onWinNewGame() }) {
                        Text("New Game", color = accentColor, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = if (isDarkTheme) CardBackground else LightCardBg
            )
        }

        // Game over dialog
        if (viewModel.showGameOverDialog) {
            AlertDialog(
                onDismissRequest = {},
                title = {
                    Text(
                        "Game Over",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "No more moves! Score: ${viewModel.score}",
                        fontFamily = FontFamily.Monospace
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.dismissGameOver()
                        viewModel.startNewGame()
                    }) {
                        Text("Try Again", color = accentColor, fontFamily = FontFamily.Monospace)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.dismissGameOver()
                        onNavigateHome()
                    }) {
                        Text("Home", color = accentColor, fontFamily = FontFamily.Monospace)
                    }
                },
                containerColor = if (isDarkTheme) CardBackground else LightCardBg
            )
        }
    }
}

@Composable
private fun ScoreCard(
    label: String,
    value: Int,
    bgColor: Color,
    accentColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    var prevValue by remember { mutableIntStateOf(value) }
    val scoreFlash = remember { Animatable(1f) }

    LaunchedEffect(value) {
        if (value != prevValue && value > 0) {
            prevValue = value
            scoreFlash.snapTo(1.15f)
            scoreFlash.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = labelColor,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "$value",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = accentColor,
            modifier = Modifier.graphicsLayer {
                scaleX = scoreFlash.value
                scaleY = scoreFlash.value
            }
        )
    }
}

@Composable
private fun TileCell(
    value: Int,
    emptyColor: Color,
    modifier: Modifier = Modifier
) {
    val bg = if (value == 0) emptyColor else tileColor(value)
    val is2048 = value == 2048

    val animatedScale = remember { Animatable(1f) }
    LaunchedEffect(value) {
        if (value != 0) {
            animatedScale.snapTo(0.7f)
            animatedScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (value != 0) 1f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "tileAlpha"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = if (value != 0) animatedScale.value else 1f
                scaleY = if (value != 0) animatedScale.value else 1f
                alpha = if (value != 0) animatedAlpha else 1f
            }
            .then(
                if (is2048) Modifier.shadow(12.dp, RoundedCornerShape(10.dp), ambientColor = Tile2048.copy(alpha = 0.6f))
                else Modifier.shadow(2.dp, RoundedCornerShape(10.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
            )
            .clip(RoundedCornerShape(10.dp))
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                text = "$value",
                fontSize = tileFontSize(value).sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = tileTextColor(value)
            )
        }
    }
}

@Composable
private fun NavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(bgColor)
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = tint
        )
    }
}
