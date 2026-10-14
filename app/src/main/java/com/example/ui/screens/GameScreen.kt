package com.example.ui.screens

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameState
import com.example.ui.components.AnalogGauge
import com.example.ui.components.GameCanvas
import com.example.ui.components.GameHud
import com.example.ui.components.PedalButton
import com.example.ui.components.PedalType
import com.example.viewmodel.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToGarage: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val physicsEngine by viewModel.physicsEngine.collectAsState()
    val totalCoins by viewModel.repository.totalCoins.collectAsState()
    val isNewRecord by viewModel.isNewRecord.collectAsState()

    var keyboardGasPressed by remember { mutableStateOf(false) }
    var keyboardBrakePressed by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    BackHandler {
        if (gameState == GameState.PLAYING) {
            viewModel.pauseGame()
        } else {
            onNavigateToGarage()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                val isDown = keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown
                when (keyEvent.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_D -> {
                        keyboardGasPressed = isDown
                        viewModel.setGas(isDown)
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_A -> {
                        keyboardBrakePressed = isDown
                        viewModel.setBrake(isDown)
                        true
                    }
                    KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_P -> {
                        if (isDown) {
                            if (gameState == GameState.PLAYING) viewModel.pauseGame()
                            else if (gameState == GameState.PAUSED) viewModel.resumeGame()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_R -> {
                        if (isDown) {
                            viewModel.restartRace()
                        }
                        true
                    }
                    else -> false
                }
            },
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val engine = physicsEngine
            if (engine != null) {
                // 1. Live 2D Game Simulation Canvas
                GameCanvas(
                    engine = engine,
                    modifier = Modifier.fillMaxSize()
                )

                // 2. Heads-Up Display (Top)
                val nextGate = engine.finishGates.firstOrNull { !it.isCrossed }
                GameHud(
                    distanceMeters = engine.distanceMeters,
                    bestDistanceMeters = viewModel.repository.getBestDistance(engine.stage),
                    stageName = engine.stage.stageName,
                    currentFuel = engine.currentFuel,
                    maxFuel = engine.maxFuel,
                    coinsEarnedInRun = engine.coinsCollectedScore,
                    totalCoinsBank = totalCoins,
                    airtimeSeconds = engine.airtimeSeconds,
                    isAirborne = engine.isAirborne,
                    currentLevelNumber = engine.currentLevelNumber,
                    nextFinishDistance = nextGate?.distanceMeters ?: (engine.distanceMeters + 250),
                    onPauseClick = { viewModel.pauseGame() },
                    onRestartClick = { viewModel.restartRace() }
                )

                // 3. Bottom Racing Control Cockpit: BRAKE Pedal, RPM Dial, SPEED Dial, GAS Pedal
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // Left: BRAKE / REVERSE Pedal
                        PedalButton(
                            type = PedalType.BRAKE,
                            isPressedExternal = keyboardBrakePressed,
                            onPressChanged = { pressed ->
                                viewModel.setBrake(pressed)
                            }
                        )

                        // Center: Dual Analog Gauges (RPM and SPEED)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            AnalogGauge(
                                title = "RPM",
                                value = engine.rpm,
                                maxValue = 8000f,
                                size = 84.dp,
                                warningThreshold = 0.85f
                            )

                            AnalogGauge(
                                title = "SPEED",
                                value = engine.currentSpeedKmh,
                                maxValue = 140f,
                                size = 84.dp,
                                unit = "",
                                warningThreshold = 0.8f
                            )
                        }

                        // Right: GAS / ACCELERATOR Pedal
                        PedalButton(
                            type = PedalType.GAS,
                            isPressedExternal = keyboardGasPressed,
                            onPressChanged = { pressed ->
                                viewModel.setGas(pressed)
                            }
                        )
                    }
                }
            }

            // 4. Overlays & Dialogs based on GameState
            when (gameState) {
                GameState.PAUSED -> {
                    PauseOverlay(
                        onResume = { viewModel.resumeGame() },
                        onRestart = { viewModel.restartRace() },
                        onGarage = {
                            viewModel.returnToGarage()
                            onNavigateToGarage()
                        },
                        isSoundMuted = viewModel.soundManager.isSoundMuted(),
                        onToggleSound = { viewModel.soundManager.toggleMute() }
                    )
                }

                GameState.GAME_OVER_FLIPPED, GameState.GAME_OVER_FUEL -> {
                    GameOverOverlay(
                        isFlipped = gameState == GameState.GAME_OVER_FLIPPED,
                        distanceMeters = physicsEngine?.distanceMeters ?: 0,
                        coinsEarned = physicsEngine?.coinsCollectedScore ?: 0,
                        stageName = physicsEngine?.stage?.stageName ?: "",
                        isNewRecord = isNewRecord,
                        onRetry = { viewModel.restartRace() },
                        onGarage = {
                            viewModel.returnToGarage()
                            onNavigateToGarage()
                        }
                    )
                }

                else -> {}
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onGarage: () -> Unit,
    isSoundMuted: Boolean,
    onToggleSound: () -> Unit
) {
    var mutedState by remember { mutableStateOf(isSoundMuted) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "GAME PAUSED",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                // Sound toggle row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            onToggleSound()
                            mutedState = !mutedState
                        }
                    ) {
                        Icon(
                            imageVector = if (mutedState) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Toggle Mute",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                    Text(
                        text = if (mutedState) "Sound: Muted" else "Sound: On",
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp
                    )
                }

                // Resume Button
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESUME", fontWeight = FontWeight.Bold)
                }

                // Restart Button
                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restart_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restart")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RESTART", fontWeight = FontWeight.Bold)
                }

                // Garage Button
                OutlinedButton(
                    onClick = onGarage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("garage_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = "Garage")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GARAGE & UPGRADES", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    isFlipped: Boolean,
    distanceMeters: Int,
    coinsEarned: Int,
    stageName: String = "",
    isNewRecord: Boolean,
    onRetry: () -> Unit,
    onGarage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B))
                .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Icon & Title
                Icon(
                    imageVector = if (isFlipped) Icons.Default.SentimentVeryDissatisfied else Icons.Default.LocalGasStation,
                    contentDescription = "Game Over",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(46.dp)
                )

                Text(
                    text = if (isFlipped) "DRIVER DOWN!" else "OUT OF FUEL!",
                    color = Color(0xFFEF4444),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                if (stageName.isNotEmpty()) {
                    Text(
                        text = "STAGE: ${stageName.uppercase()}",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isFlipped) "Vehicle flipped on rough terrain" else "Fuel tank completely drained",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                if (isNewRecord) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFB300))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "NEW DISTANCE RECORD!",
                            color = Color(0xFF3E2723),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Stats Summary Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DISTANCE", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${distanceMeters}m", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("COINS", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, "Coins", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("+$coinsEarned", color = Color(0xFFFFE082), fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Retry Button
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("retry_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TRY AGAIN", fontWeight = FontWeight.Bold)
                }

                // Garage Button
                OutlinedButton(
                    onClick = onGarage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("game_over_garage_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = "Garage")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GARAGE & UPGRADES", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
