package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameHud(
    distanceMeters: Int,
    bestDistanceMeters: Int,
    stageName: String = "",
    currentFuel: Float,
    maxFuel: Float,
    coinsEarnedInRun: Int,
    totalCoinsBank: Int,
    airtimeSeconds: Float,
    isAirborne: Boolean,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fuelRatio = (currentFuel / maxFuel).coerceIn(0f, 1f)
    val fuelPercent = (fuelRatio * 100f).toInt()
    val isFuelCritical = fuelRatio < 0.22f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Top Main HUD Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Distance, Best Distance, and Stage Name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xCC0F172A))
                    .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Distance",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        if (stageName.isNotEmpty()) {
                            Text(
                                text = stageName.uppercase(),
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${distanceMeters}m",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (bestDistanceMeters > 0) {
                            Text(
                                text = "BEST: ${bestDistanceMeters}m",
                                color = Color(0xFFFFD54F),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Center: Fuel Bar
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xCC0F172A))
                    .border(
                        1.5.dp,
                        if (isFuelCritical) Color(0xFFEF4444) else Color(0xFF10B981),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalGasStation,
                        contentDescription = "Fuel",
                        tint = if (isFuelCritical) Color(0xFFEF4444) else Color(0xFFFF5722),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.width(130.dp)
                        ) {
                            Text(
                                text = "FUEL",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "$fuelPercent%",
                                color = if (isFuelCritical) Color(0xFFEF4444) else Color(0xFF34D399),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        // Progress bar track
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color(0xFF334155))
                        ) {
                            val fuelGradient = if (isFuelCritical) {
                                Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFEF4444)))
                            } else if (fuelRatio < 0.5f) {
                                Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399)))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fuelRatio)
                                    .height(10.dp)
                                    .background(fuelGradient)
                            )
                        }
                    }
                }
            }

            // Right: Coins and Pause Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Coins badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xCC0F172A))
                        .border(1.5.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp))
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
                            text = "${totalCoinsBank + coinsEarnedInRun}",
                            color = Color(0xFFFFE082),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pause Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC0F172A))
                        .border(1.5.dp, Color(0xFF94A3B8), CircleShape)
                        .clickable { onPauseClick() }
                        .testTag("pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Stunt / Airtime Floating Banner
        AnimatedVisibility(
            visible = isAirborne && airtimeSeconds > 0.5f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xEE1E293B))
                    .border(2.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "AIRTIME ${(airtimeSeconds * 100).toInt()}",
                    color = Color(0xFFFFEB3B),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
