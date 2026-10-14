package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EnvironmentType
import com.example.model.UpgradeType
import com.example.model.VehicleType
import com.example.ui.components.VehicleShowcaseCanvas
import com.example.viewmodel.GameViewModel

@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    onStartRace: () -> Unit
) {
    val totalCoins by viewModel.repository.totalCoins.collectAsState()
    val selectedVehicle by viewModel.repository.selectedVehicle.collectAsState()
    val unlockedVehicles by viewModel.repository.unlockedVehicles.collectAsState()
    val currentLevel by viewModel.repository.currentLevel.collectAsState()
    val highestLevelUnlocked by viewModel.repository.highestLevelUnlocked.collectAsState()

    var previewVehicle by remember(selectedVehicle) { mutableStateOf(selectedVehicle) }
    var isMuted by remember { mutableStateOf(viewModel.soundManager.isSoundMuted()) }

    val isPreviewUnlocked = unlockedVehicles.contains(previewVehicle.name)
    val upgradeState = viewModel.repository.getUpgradeState(selectedVehicle)
    val levelConfig = viewModel.repository.getLevelConfig(currentLevel)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Garage",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "HILL CLIMB RACING",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "10 STYLES GARAGE & TUNING",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Coins Bank Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalCoins",
                                color = Color(0xFFFFE082),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Free Coins Reward (+1000 Coins)
                    Button(
                        onClick = { viewModel.repository.addCoins(1000) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Coins",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = "+1000", fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Sound Toggle
                    IconButton(
                        onClick = { isMuted = viewModel.soundManager.toggleMute() },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Sound",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 2. Scrollable Body Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(14.dp)
            ) {
                // SECTION A: Interactive Live 2D Vehicle Showcase Turntable
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .border(1.5.dp, previewVehicle.primaryColor.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            // Header inside stage
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = previewVehicle.title.uppercase(),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = previewVehicle.description,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }

                                // Status Badge
                                if (isPreviewUnlocked) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (selectedVehicle == previewVehicle) "ACTIVE CAR" else "OWNED",
                                            color = Color(0xFF34D399),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Locked",
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "PREMIUM LOCK • ${previewVehicle.unlockCost} 🪙",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Canvas Stage: Vehicle rendered with suspension, wheels, headlights, and glowing floor
                            VehicleShowcaseCanvas(
                                vehicle = previewVehicle,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Vehicle Performance Spec Bars
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SpecStatBar(label = "SPEED", ratio = previewVehicle.speedRating / 100f, color = Color(0xFF00E5FF), modifier = Modifier.weight(1f))
                                SpecStatBar(label = "POWER", ratio = previewVehicle.powerRating / 100f, color = Color(0xFFFF5722), modifier = Modifier.weight(1f))
                                SpecStatBar(label = "SHOCKS", ratio = previewVehicle.suspensionRating / 100f, color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                                SpecStatBar(label = "GRIP", ratio = previewVehicle.gripRating / 100f, color = Color(0xFFFFD600), modifier = Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Showcase CTA Action
                            if (isPreviewUnlocked) {
                                if (selectedVehicle != previewVehicle) {
                                    Button(
                                        onClick = { viewModel.selectVehicle(previewVehicle) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                    ) {
                                        Text(text = "DRIVE THIS VEHICLE", fontWeight = FontWeight.Black, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                val canAfford = totalCoins >= previewVehicle.unlockCost
                                Button(
                                    onClick = {
                                        if (canAfford) {
                                            viewModel.unlockVehicle(previewVehicle)
                                        }
                                    },
                                    enabled = canAfford,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFF59E0B),
                                        disabledContainerColor = Color(0xFF334155)
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Buy",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (canAfford) "UNLOCK & BUY FOR ${previewVehicle.unlockCost} 🪙" else "NEED ${previewVehicle.unlockCost - totalCoins} MORE 🪙",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION B: 10 Vehicle Styles Carousel
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHOOSE VEHICLE (10 STYLES)",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${unlockedVehicles.size}/10 UNLOCKED",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        items(VehicleType.values()) { vehicle ->
                            val isUnlocked = unlockedVehicles.contains(vehicle.name)
                            val isSelected = selectedVehicle == vehicle
                            val isPreviewed = previewVehicle == vehicle

                            VehicleCard(
                                vehicle = vehicle,
                                isUnlocked = isUnlocked,
                                isSelected = isSelected,
                                isPreviewed = isPreviewed,
                                userCoins = totalCoins,
                                onPreview = {
                                    previewVehicle = vehicle
                                    if (isUnlocked) viewModel.selectVehicle(vehicle)
                                },
                                onUnlock = {
                                    if (viewModel.unlockVehicle(vehicle)) {
                                        previewVehicle = vehicle
                                    }
                                }
                            )
                        }
                    }
                }

                // SECTION C: Vehicle Tuning & Upgrades (Engine, Suspension, Tires, Fuel Tank)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TUNING UPGRADES: ${selectedVehicle.title.uppercase()}",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        UpgradeRow(
                            type = UpgradeType.ENGINE,
                            currentLevel = upgradeState.engineLevel,
                            cost = upgradeState.getUpgradeCost(UpgradeType.ENGINE),
                            userCoins = totalCoins,
                            icon = Icons.Default.Fireplace,
                            iconColor = Color(0xFFFF5722),
                            onUpgrade = { viewModel.upgradeVehicle(UpgradeType.ENGINE) }
                        )
                        UpgradeRow(
                            type = UpgradeType.SUSPENSION,
                            currentLevel = upgradeState.suspensionLevel,
                            cost = upgradeState.getUpgradeCost(UpgradeType.SUSPENSION),
                            userCoins = totalCoins,
                            icon = Icons.Default.Build,
                            iconColor = Color(0xFF00E676),
                            onUpgrade = { viewModel.upgradeVehicle(UpgradeType.SUSPENSION) }
                        )
                        UpgradeRow(
                            type = UpgradeType.TIRES,
                            currentLevel = upgradeState.tiresLevel,
                            cost = upgradeState.getUpgradeCost(UpgradeType.TIRES),
                            userCoins = totalCoins,
                            icon = Icons.Default.Speed,
                            iconColor = Color(0xFFFFC107),
                            onUpgrade = { viewModel.upgradeVehicle(UpgradeType.TIRES) }
                        )
                        UpgradeRow(
                            type = UpgradeType.FUEL_TANK,
                            currentLevel = upgradeState.fuelLevel,
                            cost = upgradeState.getUpgradeCost(UpgradeType.FUEL_TANK),
                            userCoins = totalCoins,
                            icon = Icons.Default.LocalGasStation,
                            iconColor = Color(0xFF00B0FF),
                            onUpgrade = { viewModel.upgradeVehicle(UpgradeType.FUEL_TANK) }
                        )
                    }
                }

                // SECTION D: Level Track Progression (1 to 100 Levels)
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ROAD PROGRESSION (LEVELS 1 - 100)",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${levelConfig.environment.envName} • ${levelConfig.lengthMeters}m",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal list of levels
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items((1..100).toList()) { lvl ->
                            val isUnlocked = lvl <= highestLevelUnlocked
                            val isCurrent = lvl == currentLevel
                            val bestDist = viewModel.repository.getBestDistance(lvl)
                            val envType = viewModel.repository.getLevelConfig(lvl).environment

                            LevelChip(
                                level = lvl,
                                env = envType,
                                isUnlocked = isUnlocked,
                                isCurrent = isCurrent,
                                bestDistance = bestDist,
                                onClick = {
                                    if (isUnlocked) {
                                        viewModel.selectLevel(lvl)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 3. Bottom Big Start Race Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onStartRace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_race_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start Race",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "START RACE! (LEVEL $currentLevel)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecStatBar(
    label: String,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(text = "${(ratio * 100).toInt()}%", color = color, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0.1f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Color(0xFF334155),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun VehicleCard(
    vehicle: VehicleType,
    isUnlocked: Boolean,
    isSelected: Boolean,
    isPreviewed: Boolean,
    userCoins: Int,
    onPreview: () -> Unit,
    onUnlock: () -> Unit
) {
    val borderColor = when {
        isSelected -> Color(0xFF10B981)
        isPreviewed -> Color(0xFF38BDF8)
        isUnlocked -> Color(0xFF475569)
        else -> Color(0xFFF59E0B)
    }

    Box(
        modifier = Modifier
            .width(170.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onPreview() }
            .padding(10.dp)
    ) {
        Column {
            // Header with Title & Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                } else if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Mini Vehicle Canvas Illustration preview inside card
            VehicleShowcaseCanvas(
                vehicle = vehicle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Price / Status Label
            if (isUnlocked) {
                Button(
                    onClick = onPreview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF10B981) else Color(0xFF334155)
                    )
                ) {
                    Text(
                        text = if (isSelected) "SELECTED" else "DRIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                val canAfford = userCoins >= vehicle.unlockCost
                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        disabledContainerColor = Color(0xFF334155)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${vehicle.unlockCost} 🪙",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeRow(
    type: UpgradeType,
    currentLevel: Int,
    cost: Int,
    userCoins: Int,
    icon: ImageVector,
    iconColor: Color,
    onUpgrade: () -> Unit
) {
    val isMax = currentLevel >= type.maxLevel
    val canAfford = userCoins >= cost && !isMax

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = type.title,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = type.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LVL $currentLevel/${type.maxLevel}",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 10-step progress pip bar
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 1..type.maxLevel) {
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (i <= currentLevel) iconColor else Color(0xFF475569)
                                    )
                            )
                        }
                    }
                }
            }

            // Upgrade Button
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                modifier = Modifier
                    .height(34.dp)
                    .testTag("upgrade_${type.name.lowercase()}_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF334155)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                if (isMax) {
                    Text(text = "MAXED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Cost",
                            tint = if (canAfford) Color(0xFFFFD700) else Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$cost",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (canAfford) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelChip(
    level: Int,
    env: EnvironmentType,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    bestDistance: Int,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCurrent -> Color(0xFF10B981)
        isUnlocked -> Color(0xFF38BDF8)
        else -> Color(0xFF475569)
    }

    Box(
        modifier = Modifier
            .width(86.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0xFF064E3B) else Color(0xFF1E293B))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isUnlocked) { onClick() }
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = "LVL $level",
                    color = if (isUnlocked) Color.White else Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = env.envName.take(7),
                color = if (isUnlocked) Color(0xFF38BDF8) else Color(0xFF475569),
                fontSize = 9.sp,
                maxLines = 1
            )

            if (bestDistance > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${bestDistance}m",
                    color = Color(0xFFFFD54F),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
