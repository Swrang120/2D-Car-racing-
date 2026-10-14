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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EnvironmentType
import com.example.model.UpgradeType
import com.example.model.VehicleType
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

    var isMuted by remember { mutableStateOf(viewModel.soundManager.isSoundMuted()) }
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
                    .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GARAGE & TUNING",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Coins Bank Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Coins",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$totalCoins",
                                color = Color(0xFFFFE082),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Sound Toggle
                    IconButton(
                        onClick = { isMuted = viewModel.soundManager.toggleMute() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF334155))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Sound",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Scrollable Body Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Section: Vehicle Selection Carousel
                item {
                    Text(
                        text = "SELECT VEHICLE",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        items(VehicleType.values()) { vehicle ->
                            val isUnlocked = unlockedVehicles.contains(vehicle.name)
                            val isSelected = selectedVehicle == vehicle

                            VehicleCard(
                                vehicle = vehicle,
                                isUnlocked = isUnlocked,
                                isSelected = isSelected,
                                userCoins = totalCoins,
                                onSelect = { viewModel.selectVehicle(vehicle) },
                                onUnlock = { viewModel.unlockVehicle(vehicle) }
                            )
                        }
                    }
                }

                // Section: Vehicle Upgrades (Engine, Suspension, Tires, Fuel Tank)
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "UPGRADE PARTS: ${selectedVehicle.title.uppercase()}",
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

                // Section: Level Track Selector (1 to 100)
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRACK PROGRESSION (1-100 LEVELS)",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${levelConfig.environment.envName} - ${levelConfig.lengthMeters}m",
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
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = onStartRace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_race_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
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
private fun VehicleCard(
    vehicle: VehicleType,
    isUnlocked: Boolean,
    isSelected: Boolean,
    userCoins: Int,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    val borderColor = when {
        isSelected -> Color(0xFF10B981)
        isUnlocked -> Color(0xFF38BDF8)
        else -> Color(0xFF475569)
    }

    Box(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable {
                if (isUnlocked) onSelect()
            }
            .padding(12.dp)
    ) {
        Column {
            // Vehicle Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vehicle.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = vehicle.description,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Color bar badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(vehicle.primaryColor, vehicle.secondaryColor)
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Select or Unlock button
            if (isUnlocked) {
                Button(
                    onClick = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF10B981) else Color(0xFF334155)
                    )
                ) {
                    Text(
                        text = if (isSelected) "SELECTED" else "DRIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                val canAfford = userCoins >= vehicle.unlockCost
                Button(
                    onClick = onUnlock,
                    enabled = canAfford,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
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
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${vehicle.unlockCost} 🪙",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
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
    val isMaxed = currentLevel >= type.maxLevel
    val canAfford = !isMaxed && userCoins >= cost

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon & Info
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = type.title,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = type.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LVL $currentLevel/${type.maxLevel}",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { currentLevel.toFloat() / type.maxLevel.toFloat() },
                        modifier = Modifier
                            .width(110.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = iconColor,
                        trackColor = Color(0xFF334155),
                    )
                }
            }

            // Upgrade Button
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                modifier = Modifier.height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF59E0B),
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                if (isMaxed) {
                    Text("MAX", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF94A3B8))
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Coin",
                            tint = if (canAfford) Color(0xFFFFD700) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$cost",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
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
        else -> Color(0xFF334155)
    }

    Box(
        modifier = Modifier
            .size(width = 72.dp, height = 76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrent) Color(0xFF064E3B) else Color(0xFF1E293B))
            .border(if (isCurrent) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isUnlocked) { onClick() }
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isUnlocked) {
                Text(
                    text = "$level",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = env.envName.take(4),
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
                if (bestDistance > 0) {
                    Text(
                        text = "${bestDistance}m",
                        color = Color(0xFFFFD700),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$level",
                    color = Color(0xFF64748B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
