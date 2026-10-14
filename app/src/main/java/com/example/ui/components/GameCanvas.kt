package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.model.CoinItem
import com.example.model.EnvironmentType
import com.example.model.FinishGate
import com.example.model.FloatingNotification
import com.example.model.FuelCanItem
import com.example.model.ObstacleItem
import com.example.model.ObstacleType
import com.example.model.Particle
import com.example.model.VehicleType
import com.example.physics.PhysicsEngine
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameCanvas(
    engine: PhysicsEngine,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height

        // Camera tracking: follow vehicle center with smooth offset
        val cameraOffsetX = screenW * 0.32f - engine.chassisX
        val cameraOffsetY = screenH * 0.62f - engine.chassisY

        // 1. Draw Environment Sky & Parallax Backdrops
        drawEnvironmentBackground(engine.stage, cameraOffsetX, cameraOffsetY, screenW, screenH)

        // 2. Draw Terrain & Bridges
        drawTerrain(engine, cameraOffsetX, cameraOffsetY, screenW, screenH)

        // 3. Draw Level Finish Line Gates
        drawFinishGates(engine.finishGates, engine, cameraOffsetX, cameraOffsetY)

        // 4. Draw Obstacles (Crates/Barrels)
        drawObstacles(engine.obstacles, cameraOffsetX, cameraOffsetY)

        // 5. Draw Collectibles (Coins & Fuel Cans)
        drawCollectibles(engine.coins, engine.fuelCans, cameraOffsetX, cameraOffsetY)

        // 6. Draw Vehicle (Suspension, Wheels, Chassis, Driver, Exhaust)
        drawVehicle(engine, cameraOffsetX, cameraOffsetY)

        // 7. Draw Particles (Tire dirt, explosion sparks, finish confetti)
        drawParticles(engine.particles, cameraOffsetX, cameraOffsetY)

        // 8. Draw Floating Notifications ("+50", "AIRTIME", "LEVEL 1 COMPLETE", etc.)
        drawFloatingNotifications(engine.floatingTexts, cameraOffsetX, cameraOffsetY)
    }
}

/**
 * High-quality interactive Vehicle Showcase Canvas for Garage / Home Page.
 * Displays vehicle on glowing futuristic turntable pedestal with environment lighting and animated wheels/exhaust.
 */
@Composable
fun VehicleShowcaseCanvas(
    vehicle: VehicleType,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "showcase")
    val bobbing by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.5f
        val pedestalY = h * 0.72f

        // 1. Glowing Pedestal Platform
        val pedestalWidth = w * 0.82f
        val pedestalHeight = 22f

        // Radial glow under platform
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    vehicle.primaryColor.copy(alpha = 0.35f * glowPulse),
                    Color.Transparent
                ),
                center = Offset(centerX, pedestalY + 6f),
                radius = pedestalWidth * 0.65f
            ),
            topLeft = Offset(centerX - pedestalWidth * 0.65f, pedestalY - 20f),
            size = Size(pedestalWidth * 1.3f, 55f)
        )

        // Platform Base (Metallic)
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
            ),
            topLeft = Offset(centerX - pedestalWidth * 0.5f, pedestalY),
            size = Size(pedestalWidth, pedestalHeight),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Neon Edge on platform
        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(vehicle.primaryColor, vehicle.secondaryColor, vehicle.primaryColor)
            ),
            topLeft = Offset(centerX - pedestalWidth * 0.5f, pedestalY),
            size = Size(pedestalWidth, 4f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // 2. Render Vehicle Centered on Pedestal
        val halfBase = if (vehicle.isOneWheeler) 0f else vehicle.wheelBase * 0.5f
        val rearWheelY = pedestalY - vehicle.wheelRadius + 2f
        val frontWheelY = pedestalY - vehicle.frontWheelRadius + 2f
        val chassisY = pedestalY - vehicle.wheelRadius - vehicle.suspensionLength + bobbing

        val rearWheelX = centerX - halfBase
        val frontWheelX = centerX + halfBase

        // Shadow under wheels
        drawOval(
            color = Color(0x66000000),
            topLeft = Offset(rearWheelX - vehicle.wheelRadius * 0.9f, pedestalY - 2f),
            size = Size(vehicle.wheelRadius * 1.8f, 7f)
        )
        if (!vehicle.isOneWheeler) {
            drawOval(
                color = Color(0x66000000),
                topLeft = Offset(frontWheelX - vehicle.frontWheelRadius * 0.9f, pedestalY - 2f),
                size = Size(vehicle.frontWheelRadius * 1.8f, 7f)
            )
        }

        // Suspension Coils
        drawSuspensionCoil(rearWheelX, chassisY + 8f, rearWheelX, rearWheelY)
        if (!vehicle.isOneWheeler) {
            drawSuspensionCoil(frontWheelX, chassisY + 8f, frontWheelX, frontWheelY)
        }

        // Wheels
        drawWheel(rearWheelX, rearWheelY, vehicle.wheelRadius, 0f)
        if (!vehicle.isOneWheeler) {
            drawWheel(frontWheelX, frontWheelY, vehicle.frontWheelRadius, 0f)
        }

        // Chassis
        drawVehicleChassisByType(vehicle, centerX, chassisY, false)

        // Headlight glow cone
        val hlX = centerX + halfBase + 28f
        val hlY = chassisY + 4f
        val beamPath = Path().apply {
            moveTo(hlX, hlY)
            lineTo(hlX + 75f, hlY - 14f)
            lineTo(hlX + 75f, hlY + 24f)
            close()
        }
        drawPath(
            beamPath,
            brush = Brush.horizontalGradient(
                listOf(Color(0x55FFF59D), Color(0x00FFF59D)),
                startX = hlX,
                endX = hlX + 75f
            )
        )
    }
}

private fun DrawScope.drawEnvironmentBackground(
    env: EnvironmentType,
    camX: Float,
    camY: Float,
    w: Float,
    h: Float
) {
    // Gradient Sky
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(env.skyTopColor, env.skyBottomColor),
            startY = 0f,
            endY = h
        ),
        topLeft = Offset.Zero,
        size = Size(w, h)
    )

    when (env) {
        EnvironmentType.COUNTRYSIDE -> {
            // Sun
            drawCircle(
                color = Color(0xFFFFF176).copy(alpha = 0.9f),
                radius = 38f,
                center = Offset(w * 0.82f, h * 0.18f)
            )
            // Parallax Mountains (0.08x scroll)
            val pX = (camX * 0.08f) % 400f
            val mountainPath = Path().apply {
                moveTo(-200f, h)
                var mx = -200f + pX
                while (mx < w + 300f) {
                    lineTo(mx + 120f, h * 0.42f)
                    lineTo(mx + 240f, h * 0.62f)
                    mx += 240f
                }
                lineTo(w + 300f, h)
                close()
            }
            drawPath(mountainPath, color = Color(0xFFB0BEC5).copy(alpha = 0.45f))

            // Parallax Midground Green Hills (0.22x scroll)
            val hillPX = (camX * 0.22f) % 360f
            val hillPath = Path().apply {
                moveTo(-100f, h)
                var hx = -100f + hillPX
                while (hx < w + 200f) {
                    quadraticBezierTo(hx + 90f, h * 0.58f, hx + 180f, h * 0.72f)
                    hx += 180f
                }
                lineTo(w + 200f, h)
                close()
            }
            drawPath(hillPath, color = Color(0xFF81C784).copy(alpha = 0.55f))
        }

        EnvironmentType.DESERT -> {
            // Scorching Desert Sun
            drawCircle(
                color = Color(0xFFFFCC80),
                radius = 50f,
                center = Offset(w * 0.22f, h * 0.20f)
            )
            // Distant Pyramids & Dunes (0.10x scroll)
            val pX = (camX * 0.10f) % 450f
            val pyramidPath = Path().apply {
                moveTo(-150f, h)
                var px = -150f + pX
                while (px < w + 300f) {
                    lineTo(px + 140f, h * 0.48f)
                    lineTo(px + 280f, h)
                    px += 280f
                }
                lineTo(w + 300f, h)
                close()
            }
            drawPath(pyramidPath, color = Color(0xFFFFB74D).copy(alpha = 0.40f))
        }

        EnvironmentType.ARCTIC -> {
            // Ice Mountains
            val pX = (camX * 0.12f) % 380f
            val icePath = Path().apply {
                moveTo(-150f, h)
                var ix = -150f + pX
                while (ix < w + 300f) {
                    lineTo(ix + 110f, h * 0.40f)
                    lineTo(ix + 220f, h * 0.65f)
                    ix += 220f
                }
                lineTo(w + 300f, h)
                close()
            }
            drawPath(icePath, color = Color(0xFFB2EBF2).copy(alpha = 0.60f))
        }

        EnvironmentType.MOON -> {
            // Giant Earth in sky
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF64B5F6), Color(0xFF1E88E5), Color(0xFF0D47A1)),
                    center = Offset(w * 0.78f, h * 0.24f),
                    radius = 48f
                ),
                radius = 48f,
                center = Offset(w * 0.78f, h * 0.24f)
            )
            // Moon crater rims
            val pX = (camX * 0.08f) % 350f
            val craterPath = Path().apply {
                moveTo(-100f, h)
                var cx = -100f + pX
                while (cx < w + 200f) {
                    quadraticBezierTo(cx + 80f, h * 0.50f, cx + 160f, h * 0.68f)
                    cx += 160f
                }
                lineTo(w + 200f, h)
                close()
            }
            drawPath(craterPath, color = Color(0xFF546E7A).copy(alpha = 0.50f))
        }

        EnvironmentType.CAVE -> {
            // Stalactites hanging from ceiling
            val pX = (camX * 0.25f) % 250f
            val roofPath = Path().apply {
                moveTo(-50f, 0f)
                var rx = -50f + pX
                while (rx < w + 150f) {
                    lineTo(rx + 40f, 65f)
                    lineTo(rx + 80f, 0f)
                    rx += 80f
                }
                close()
            }
            drawPath(roofPath, color = Color(0xFF311B92).copy(alpha = 0.70f))
        }

        EnvironmentType.HIGHWAY -> {
            // Distant City Skyline
            val pX = (camX * 0.10f) % 300f
            val bldgPath = Path().apply {
                moveTo(-100f, h)
                var bx = -100f + pX
                while (bx < w + 200f) {
                    lineTo(bx, h * 0.44f)
                    lineTo(bx + 40f, h * 0.44f)
                    lineTo(bx + 40f, h * 0.36f)
                    lineTo(bx + 75f, h * 0.36f)
                    lineTo(bx + 75f, h * 0.52f)
                    lineTo(bx + 110f, h * 0.52f)
                    lineTo(bx + 110f, h)
                    bx += 110f
                }
                close()
            }
            drawPath(bldgPath, color = Color(0xFF455A64).copy(alpha = 0.45f))
        }
    }
}

private fun DrawScope.drawTerrain(
    engine: PhysicsEngine,
    camX: Float,
    camY: Float,
    w: Float,
    h: Float
) {
    val stepPx = 18f
    val startWorldX = -camX - 60f
    val endWorldX = -camX + w + 80f

    val terrainPath = Path()
    val topStripePath = Path()

    var worldX = startWorldX
    var firstPoint = true

    while (worldX <= endWorldX) {
        val groundY = engine.getTerrainElevation(worldX)
        val screenX = worldX + camX
        val screenY = groundY + camY

        if (firstPoint) {
            terrainPath.moveTo(screenX, screenY)
            topStripePath.moveTo(screenX, screenY)
            firstPoint = false
        } else {
            terrainPath.lineTo(screenX, screenY)
            topStripePath.lineTo(screenX, screenY)
        }
        worldX += stepPx
    }

    // Close bottom of terrain to screen bottom
    terrainPath.lineTo(endWorldX + camX, h + 100f)
    terrainPath.lineTo(startWorldX + camX, h + 100f)
    terrainPath.close()

    // 1. Draw Subsurface Deep Earth Fill
    drawPath(
        terrainPath,
        brush = Brush.verticalGradient(
            colors = listOf(engine.stage.hillSubsurfaceColor, Color(0xFF1E1A17)),
            startY = camY + engine.baseGroundY - 60f,
            endY = h
        )
    )

    // 2. Draw Top Surface Grass / Soil Stripe (14px thickness)
    drawPath(
        topStripePath,
        color = engine.stage.hillSurfaceColor,
        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // 3. Draw Starting Runway 50m marker line
    val spawnRunwaySX = engine.flatSpawnRunwayLength + camX
    val spawnRunwaySY = engine.baseGroundY + camY
    if (spawnRunwaySX in -50f..w + 50f) {
        drawLine(
            color = Color(0xFFFBBF24),
            start = Offset(spawnRunwaySX, spawnRunwaySY - 35f),
            end = Offset(spawnRunwaySX, spawnRunwaySY + 2f),
            strokeWidth = 4f
        )
    }

    // 4. Draw Bridges
    for (bridge in engine.bridges) {
        val bStartX = bridge.first + camX
        val bEndX = bridge.second + camX
        if (bEndX >= 0f && bStartX <= w) {
            drawSuspensionBridge(bridge.first, bridge.second, engine, camX, camY)
        }
    }
}

private fun DrawScope.drawSuspensionBridge(
    bStartWorldX: Float,
    bEndWorldX: Float,
    engine: PhysicsEngine,
    camX: Float,
    camY: Float
) {
    val startSX = bStartWorldX + camX
    val endSX = bEndWorldX + camX
    val p1Y = engine.getTerrainElevation(bStartWorldX) + camY
    val p2Y = engine.getTerrainElevation(bEndWorldX) + camY

    // Bridge Pylons (Wooden/Steel posts at both edges)
    drawRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(startSX - 8f, p1Y - 45f),
        size = Size(16f, 50f)
    )
    drawRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(endSX - 8f, p2Y - 45f),
        size = Size(16f, 50f)
    )

    // Main Suspension Cable
    val cablePath = Path().apply {
        moveTo(startSX, p1Y - 45f)
        val midSX = (startSX + endSX) * 0.5f
        val midSY = (p1Y + p2Y) * 0.5f - 10f
        quadraticBezierTo(midSX, midSY, endSX, p2Y - 45f)
    }
    drawPath(cablePath, color = Color(0xFFD7CCC8), style = Stroke(width = 3f))

    // Wooden Bridge Planks
    val steps = 14
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val wx = bStartWorldX + (bEndWorldX - bStartWorldX) * t
        val wy = engine.getTerrainElevation(wx)
        val sx = wx + camX
        val sy = wy + camY

        // Vertical suspension wire
        val topCableY = (p1Y - 45f) * (1f - t) + (p2Y - 45f) * t - sin(t * PI.toFloat()) * 35f
        drawLine(
            color = Color(0x88FFFFFF),
            start = Offset(sx, topCableY),
            end = Offset(sx, sy),
            strokeWidth = 1.5f
        )

        // Wooden Plank
        drawRoundRect(
            color = Color(0xFF8D6E63),
            topLeft = Offset(sx - 7f, sy - 4f),
            size = Size(14f, 8f),
            cornerRadius = CornerRadius(2f, 2f)
        )
    }
}

private fun DrawScope.drawFinishGates(
    gates: List<FinishGate>,
    engine: PhysicsEngine,
    camX: Float,
    camY: Float
) {
    for (gate in gates) {
        val gateSX = gate.worldX + camX
        if (gateSX < -120f || gateSX > size.width + 120f) continue

        val groundY = engine.getTerrainElevation(gate.worldX) + camY
        val archH = 130f
        val archW = 44f

        // Checkered Arch Posts
        val postLeft = gateSX - archW * 0.5f
        val postRight = gateSX + archW * 0.5f

        // Left Pole
        drawCheckeredPole(postLeft, groundY - archH, 12f, archH)
        // Right Pole
        drawCheckeredPole(postRight, groundY - archH, 12f, archH)

        // Overhead Banner Board
        val bannerTop = groundY - archH
        val bannerHeight = 36f
        val bannerWidth = archW + 60f
        val bannerLeft = gateSX - bannerWidth * 0.5f

        drawRoundRect(
            brush = Brush.horizontalGradient(
                listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))
            ),
            topLeft = Offset(bannerLeft, bannerTop),
            size = Size(bannerWidth, bannerHeight),
            cornerRadius = CornerRadius(6f, 6f)
        )
        drawRoundRect(
            color = if (gate.isCrossed) Color(0xFF10B981) else Color(0xFFFFD700),
            topLeft = Offset(bannerLeft, bannerTop),
            size = Size(bannerWidth, bannerHeight),
            cornerRadius = CornerRadius(6f, 6f),
            style = Stroke(width = 2.5f)
        )

        // Checkerboard striping across top of banner
        val checks = 8
        val checkW = bannerWidth / checks
        for (c in 0 until checks) {
            val checkColor = if (c % 2 == 0) Color.White else Color.Black
            drawRect(
                color = checkColor,
                topLeft = Offset(bannerLeft + c * checkW, bannerTop + 2f),
                size = Size(checkW, 7f)
            )
        }

        // Native canvas text for crisp readability
        val bannerPaint = android.graphics.Paint().apply {
            color = if (gate.isCrossed) android.graphics.Color.GREEN else android.graphics.Color.YELLOW
            textSize = 20f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val gateText = if (gate.isCrossed) "★ COMPLETED ★" else "★ LEVEL ${gate.levelNumber} FINISH ★"
        drawContext.canvas.nativeCanvas.drawText(
            gateText,
            gateSX,
            bannerTop + 26f,
            bannerPaint
        )

        // Road Finish Line (Checkered pattern on ground)
        val groundStripes = 6
        for (s in 0 until groundStripes) {
            val color = if (s % 2 == 0) Color.White else Color(0xFF1E293B)
            drawRect(
                color = color,
                topLeft = Offset(gateSX - 15f + s * 5f, groundY - 3f),
                size = Size(5f, 6f)
            )
        }
    }
}

private fun DrawScope.drawCheckeredPole(x: Float, y: Float, w: Float, h: Float) {
    val segments = 9
    val segH = h / segments
    for (i in 0 until segments) {
        val color = if (i % 2 == 0) Color(0xFFD32F2F) else Color.White
        drawRect(
            color = color,
            topLeft = Offset(x, y + i * segH),
            size = Size(w, segH)
        )
    }
}

private fun DrawScope.drawObstacles(
    obstacles: List<ObstacleItem>,
    camX: Float,
    camY: Float
) {
    for (obs in obstacles) {
        if (obs.isDestroyed) continue

        val sx = obs.x + camX
        val sy = obs.y + camY

        if (sx < -50f || sx > size.width + 50f) continue

        rotate(degrees = obs.angle * 180f / PI.toFloat(), pivot = Offset(sx, sy)) {
            when (obs.type) {
                ObstacleType.WOOD_CRATE -> {
                    // Wooden Crate
                    drawRoundRect(
                        color = Color(0xFF8D6E63),
                        topLeft = Offset(sx - obs.width * 0.5f, sy - obs.height * 0.5f),
                        size = Size(obs.width, obs.height),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = Color(0xFF4E342E),
                        topLeft = Offset(sx - obs.width * 0.5f, sy - obs.height * 0.5f),
                        size = Size(obs.width, obs.height),
                        cornerRadius = CornerRadius(3f, 3f),
                        style = Stroke(width = 2f)
                    )
                    // X cross on crate
                    drawLine(
                        color = Color(0xFF4E342E),
                        start = Offset(sx - obs.width * 0.5f, sy - obs.height * 0.5f),
                        end = Offset(sx + obs.width * 0.5f, sy + obs.height * 0.5f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color(0xFF4E342E),
                        start = Offset(sx + obs.width * 0.5f, sy - obs.height * 0.5f),
                        end = Offset(sx - obs.width * 0.5f, sy + obs.height * 0.5f),
                        strokeWidth = 2f
                    )
                }

                ObstacleType.OIL_BARREL -> {
                    // Red Hazard Oil Barrel
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFB71C1C), Color(0xFFE53935), Color(0xFFB71C1C))
                        ),
                        topLeft = Offset(sx - obs.width * 0.5f, sy - obs.height * 0.5f),
                        size = Size(obs.width, obs.height),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Yellow Hazard Stripe
                    drawRect(
                        color = Color(0xFFFFD600),
                        topLeft = Offset(sx - obs.width * 0.5f, sy - 3f),
                        size = Size(obs.width, 6f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawCollectibles(
    coins: List<CoinItem>,
    fuelCans: List<FuelCanItem>,
    camX: Float,
    camY: Float
) {
    // 1. Coins
    for (coin in coins) {
        if (coin.collected) continue
        val sx = coin.x + camX
        val sy = coin.y + camY
        if (sx < -30f || sx > size.width + 30f) continue

        val coinRadius = when (coin.value) {
            500 -> 16f
            100 -> 14f
            else -> 12f
        }

        val baseColor = when (coin.value) {
            500 -> Color(0xFFFFD700) // Gold
            100 -> Color(0xFF00E5FF) // Diamond Cyan
            50 -> Color(0xFF10B981) // Emerald Green
            10 -> Color(0xFFB0BEC5) // Silver
            else -> Color(0xFFFFB300) // Bronze Amber
        }

        // Outer Glow
        drawCircle(
            color = baseColor.copy(alpha = 0.35f),
            radius = coinRadius + 4f,
            center = Offset(sx, sy)
        )
        // Coin Face
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, baseColor, baseColor.copy(alpha = 0.85f)),
                center = Offset(sx - 3f, sy - 3f),
                radius = coinRadius
            ),
            radius = coinRadius,
            center = Offset(sx, sy)
        )
        // Coin Rim
        drawCircle(
            color = Color(0x88000000),
            radius = coinRadius - 2f,
            center = Offset(sx, sy),
            style = Stroke(width = 1.5f)
        )

        // Inner value symbol
        val coinPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = if (coin.value >= 100) 10f else 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText(
            coin.value.toString(),
            sx,
            sy + 4f,
            coinPaint
        )
    }

    // 2. Red Fuel Jerrycans
    for (fuel in fuelCans) {
        if (fuel.collected) continue
        val sx = fuel.x + camX
        val sy = fuel.y + camY
        if (sx < -30f || sx > size.width + 30f) continue

        // Fuel Can Body
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C))),
            topLeft = Offset(sx - 12f, sy - 14f),
            size = Size(24f, 28f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Handle on top
        drawRoundRect(
            color = Color(0xFF7F1D1D),
            topLeft = Offset(sx - 8f, sy - 20f),
            size = Size(16f, 6f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Spout
        drawRect(
            color = Color(0xFFFBBF24),
            topLeft = Offset(sx + 5f, sy - 21f),
            size = Size(5f, 4f)
        )
        // White "GAS" label
        val gasPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("GAS", sx, sy + 3f, gasPaint)
    }
}

private fun DrawScope.drawVehicle(
    engine: PhysicsEngine,
    camX: Float,
    camY: Float
) {
    val chassisSX = engine.chassisX + camX
    val chassisSY = engine.chassisY + camY
    val chassisDeg = engine.chassisAngle * 180f / PI.toFloat()

    val rearSX = engine.rearWheelX + camX
    val rearSY = engine.rearWheelY + camY
    val frontSX = engine.frontWheelX + camX
    val frontSY = engine.frontWheelY + camY

    val cosA = cos(engine.chassisAngle)
    val sinA = sin(engine.chassisAngle)
    val halfBase = if (engine.isOneWheeler) 0f else engine.vehicleType.wheelBase * 0.5f
    val suspLen = engine.vehicleType.suspensionLength

    val rearAnchorSX = chassisSX - cosA * halfBase - sinA * suspLen * 0.35f
    val rearAnchorSY = chassisSY - sinA * halfBase + cosA * suspLen * 0.35f
    val frontAnchorSX = chassisSX + cosA * halfBase - sinA * suspLen * 0.35f
    val frontAnchorSY = chassisSY + sinA * halfBase + cosA * suspLen * 0.35f

    // 1. Suspension Springs
    drawSuspensionCoil(rearAnchorSX, rearAnchorSY, rearSX, rearSY)
    if (!engine.isOneWheeler) {
        drawSuspensionCoil(frontAnchorSX, frontAnchorSY, frontSX, frontSY)
    }

    // 2. Wheels
    drawWheel(rearSX, rearSY, engine.rearWheelRadius, engine.rearWheelSpin)
    if (!engine.isOneWheeler) {
        drawWheel(frontSX, frontSY, engine.frontWheelRadius, engine.frontWheelSpin)
    }

    // 3. Vehicle Body / Chassis & Driver
    rotate(degrees = chassisDeg, pivot = Offset(chassisSX, chassisSY)) {
        drawVehicleChassisByType(engine.vehicleType, chassisSX, chassisSY, engine.boostValue > 20f)
    }
}

private fun DrawScope.drawSuspensionCoil(x1: Float, y1: Float, x2: Float, y2: Float) {
    val coilPath = Path()
    coilPath.moveTo(x1, y1)
    val coils = 4
    for (i in 1..coils) {
        val t1 = (i - 0.5f) / coils
        val t2 = i.toFloat() / coils
        val px = x1 + (x2 - x1) * t1
        val py = y1 + (y2 - y1) * t1
        val dx = -(y2 - y1) * 0.12f
        val dy = (x2 - x1) * 0.12f
        val side = if (i % 2 == 0) 1f else -1f
        coilPath.lineTo(px + dx * side, py + dy * side)
        coilPath.lineTo(x1 + (x2 - x1) * t2, y1 + (y2 - y1) * t2)
    }
    drawPath(coilPath, color = Color(0xFF94A3B8), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawWheel(cx: Float, cy: Float, radius: Float, spinAngle: Float) {
    // 1. Deep Tread Outer Rubber Tire
    drawCircle(
        color = Color(0xFF1E293B),
        radius = radius,
        center = Offset(cx, cy)
    )
    // Tire tread teeth
    val teeth = 10
    for (i in 0 until teeth) {
        val ang = spinAngle + i * (2f * PI.toFloat() / teeth)
        val tx = cx + cos(ang) * (radius - 2f)
        val ty = cy + sin(ang) * (radius - 2f)
        drawCircle(
            color = Color(0xFF0F172A),
            radius = 3.2f,
            center = Offset(tx, ty)
        )
    }

    // 2. Shiny Metallic Rim
    val rimRadius = radius * 0.58f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8), Color(0xFF475569)),
            center = Offset(cx - 2f, cy - 2f),
            radius = rimRadius
        ),
        radius = rimRadius,
        center = Offset(cx, cy)
    )

    // 3. Central Hub & Spokes
    drawCircle(
        color = Color(0xFFD32F2F),
        radius = rimRadius * 0.32f,
        center = Offset(cx, cy)
    )
    val spokes = 5
    for (i in 0 until spokes) {
        val ang = spinAngle + i * (2f * PI.toFloat() / spokes)
        drawLine(
            color = Color(0xFF334155),
            start = Offset(cx, cy),
            end = Offset(cx + cos(ang) * (rimRadius - 2f), cy + sin(ang) * (rimRadius - 2f)),
            strokeWidth = 2.5f
        )
    }
}

private fun DrawScope.drawVehicleChassisByType(
    type: VehicleType,
    sx: Float,
    sy: Float,
    isBoostActive: Boolean
) {
    when (type) {
        VehicleType.JEEP -> drawJeepChassis(sx, sy, isBoostActive)
        VehicleType.BIKE -> drawBikeChassis(sx, sy)
        VehicleType.MONSTER_TRUCK -> drawMonsterChassis(sx, sy)
        VehicleType.TRACTOR -> drawTractorChassis(sx, sy)
        VehicleType.TRUCK -> drawTruckChassis(sx, sy)
        VehicleType.QUAD_BIKE -> drawQuadChassis(sx, sy)
        VehicleType.RALLY_CAR -> drawRallyCarChassis(sx, sy)
        VehicleType.HIPPIE_VAN -> drawHippieVanChassis(sx, sy)
        VehicleType.TANK -> drawTankChassis(sx, sy)
        VehicleType.SUPERCAR -> drawSupercarChassis(sx, sy)
        VehicleType.ONE_WHEELER -> drawOneWheelerChassis(sx, sy)
    }
}

// 1. Classic Red Jeep
private fun DrawScope.drawJeepChassis(sx: Float, sy: Float, isBoostActive: Boolean) {
    // Red Jeep Main Tub
    drawRoundRect(
        color = Color(0xFFD32F2F),
        topLeft = Offset(sx - 38f, sy - 14f),
        size = Size(76f, 22f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Black Roll Bar Cage
    val cagePath = Path().apply {
        moveTo(sx - 26f, sy - 14f)
        lineTo(sx - 18f, sy - 36f)
        lineTo(sx + 10f, sy - 36f)
        lineTo(sx + 16f, sy - 14f)
    }
    drawPath(cagePath, color = Color(0xFF1E293B), style = Stroke(width = 4f, cap = StrokeCap.Round))

    // Windshield frame
    drawLine(
        color = Color(0xFF81D4FA),
        start = Offset(sx + 16f, sy - 14f),
        end = Offset(sx + 10f, sy - 36f),
        strokeWidth = 3f
    )

    // Driver Bill with Cap
    drawDriverWithHelmet(sx - 4f, sy - 28f, Color(0xFFD32F2F))

    // Headlight
    drawCircle(
        color = Color(0xFFFFF59D),
        radius = 5f,
        center = Offset(sx + 36f, sy - 4f)
    )
}

// 2. Motocross Dirt Bike
private fun DrawScope.drawBikeChassis(sx: Float, sy: Float) {
    // Orange Motocross Frame
    val framePath = Path().apply {
        moveTo(sx - 24f, sy)
        lineTo(sx, sy - 12f)
        lineTo(sx + 22f, sy - 4f)
        lineTo(sx + 16f, sy - 28f) // Handlebars
    }
    drawPath(framePath, color = Color(0xFFF57C00), style = Stroke(width = 5f, cap = StrokeCap.Round))

    // Seat
    drawRoundRect(
        color = Color(0xFF212121),
        topLeft = Offset(sx - 16f, sy - 18f),
        size = Size(20f, 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Handlebars
    drawLine(
        color = Color(0xFF90A4AE),
        start = Offset(sx + 12f, sy - 28f),
        end = Offset(sx + 20f, sy - 28f),
        strokeWidth = 3.5f
    )

    // Biker Driver leaning forward
    drawDriverWithHelmet(sx - 2f, sy - 32f, Color(0xFFF57C00))
}

// 3. Monster Truck
private fun DrawScope.drawMonsterChassis(sx: Float, sy: Float) {
    // High Blue Pickup Body
    drawRoundRect(
        color = Color(0xFF1976D2),
        topLeft = Offset(sx - 44f, sy - 22f),
        size = Size(88f, 26f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Cab Roof
    drawRoundRect(
        color = Color(0xFF0D47A1),
        topLeft = Offset(sx - 22f, sy - 42f),
        size = Size(42f, 22f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Chrome Heavy Rollbar with Offroad Spotlights
    drawLine(
        color = Color(0xFFCFD8DC),
        start = Offset(sx - 18f, sy - 22f),
        end = Offset(sx - 18f, sy - 48f),
        strokeWidth = 4f
    )
    for (i in 0..2) {
        drawCircle(
            color = Color(0xFFFFEB3B),
            radius = 4f,
            center = Offset(sx - 18f + i * 16f, sy - 48f)
        )
    }

    drawDriverWithHelmet(sx - 4f, sy - 32f, Color(0xFF1976D2))
}

// 4. Farm Tractor ("Tractor")
private fun DrawScope.drawTractorChassis(sx: Float, sy: Float) {
    // Green Farm Tractor Engine Bonnet (John Deere Green)
    drawRoundRect(
        color = Color(0xFF2E7D32),
        topLeft = Offset(sx - 6f, sy - 18f),
        size = Size(46f, 24f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Yellow Stripe
    drawRect(
        color = Color(0xFFFBC02D),
        topLeft = Offset(sx - 6f, sy - 8f),
        size = Size(46f, 4f)
    )

    // Rear High Mudguard over big rear wheel
    val mudguardPath = Path().apply {
        moveTo(sx - 42f, sy + 6f)
        quadraticBezierTo(sx - 28f, sy - 28f, sx - 10f, sy - 14f)
    }
    drawPath(mudguardPath, color = Color(0xFF1B5E20), style = Stroke(width = 8f, cap = StrokeCap.Round))

    // Vertical Exhaust Chimney Smokestack
    drawLine(
        color = Color(0xFF212121),
        start = Offset(sx + 24f, sy - 18f),
        end = Offset(sx + 24f, sy - 44f),
        strokeWidth = 4.5f
    )
    // Rain cap flap on chimney
    drawLine(
        color = Color(0xFF424242),
        start = Offset(sx + 21f, sy - 44f),
        end = Offset(sx + 28f, sy - 47f),
        strokeWidth = 2.5f
    )

    // Tractor Steering Column & Wheel
    drawLine(
        color = Color(0xFF424242),
        start = Offset(sx + 8f, sy - 18f),
        end = Offset(sx + 2f, sy - 30f),
        strokeWidth = 3f
    )
    drawCircle(
        color = Color(0xFF212121),
        radius = 5f,
        center = Offset(sx + 2f, sy - 30f)
    )

    // Driver in Farmer Straw Hat
    drawDriverWithHelmet(sx - 14f, sy - 32f, Color(0xFFFBC02D))
}

// 5. Heavy Mining Semi Truck ("Track" / "Truck")
private fun DrawScope.drawTruckChassis(sx: Float, sy: Float) {
    // Big Square Orange/Red Cab
    drawRoundRect(
        color = Color(0xFFE65100),
        topLeft = Offset(sx + 6f, sy - 44f),
        size = Size(38f, 46f),
        cornerRadius = CornerRadius(5f, 5f)
    )

    // Front Chrome Grille
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFF78909C))),
        topLeft = Offset(sx + 36f, sy - 30f),
        size = Size(10f, 30f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    // Windshield & Sun Visor
    drawRect(
        color = Color(0xFF81D4FA),
        topLeft = Offset(sx + 16f, sy - 40f),
        size = Size(20f, 16f)
    )
    drawRect(
        color = Color(0xFFBF360C),
        topLeft = Offset(sx + 14f, sy - 44f),
        size = Size(26f, 4f)
    )

    // Rear Heavy Cargo Flatbed Frame
    drawRoundRect(
        color = Color(0xFF37474F),
        topLeft = Offset(sx - 46f, sy - 14f),
        size = Size(54f, 16f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Dual Tall Chrome Exhaust Stacks
    drawLine(
        color = Color(0xFFECEFF1),
        start = Offset(sx + 4f, sy - 20f),
        end = Offset(sx + 4f, sy - 54f),
        strokeWidth = 4f
    )

    drawDriverWithHelmet(sx + 22f, sy - 32f, Color(0xFFE65100))
}

// 6. 4x4 Desert Quad ATV
private fun DrawScope.drawQuadChassis(sx: Float, sy: Float) {
    // Teal/Black ATV Offroad body
    drawRoundRect(
        color = Color(0xFF00897B),
        topLeft = Offset(sx - 32f, sy - 14f),
        size = Size(64f, 18f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    // Mudguards over front & rear wheels
    drawArc(
        color = Color(0xFF004D40),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(sx - 36f, sy - 20f),
        size = Size(26f, 20f),
        style = Stroke(width = 4f)
    )
    drawArc(
        color = Color(0xFF004D40),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(sx + 10f, sy - 20f),
        size = Size(26f, 20f),
        style = Stroke(width = 4f)
    )

    // Handlebars
    drawLine(
        color = Color(0xFF37474F),
        start = Offset(sx + 8f, sy - 14f),
        end = Offset(sx + 12f, sy - 26f),
        strokeWidth = 3.5f
    )

    drawDriverWithHelmet(sx - 6f, sy - 30f, Color(0xFF00897B))
}

// 7. Turbo Rally Car
private fun DrawScope.drawRallyCarChassis(sx: Float, sy: Float) {
    // Cyan Aerodynamic Body
    val bodyPath = Path().apply {
        moveTo(sx - 38f, sy - 2f)
        lineTo(sx - 34f, sy - 16f)
        lineTo(sx - 12f, sy - 28f)
        lineTo(sx + 18f, sy - 28f)
        lineTo(sx + 36f, sy - 10f)
        lineTo(sx + 38f, sy - 2f)
        close()
    }
    drawPath(bodyPath, color = Color(0xFF00ACC1))

    // Windshield & windows
    val winPath = Path().apply {
        moveTo(sx - 10f, sy - 26f)
        lineTo(sx + 15f, sy - 26f)
        lineTo(sx + 28f, sy - 14f)
        lineTo(sx - 10f, sy - 14f)
        close()
    }
    drawPath(winPath, color = Color(0xFF80DEEA))

    // Rear Aero Wing
    drawLine(
        color = Color(0xFF006064),
        start = Offset(sx - 36f, sy - 16f),
        end = Offset(sx - 38f, sy - 30f),
        strokeWidth = 3f
    )
    drawRect(
        color = Color(0xFF006064),
        topLeft = Offset(sx - 44f, sy - 32f),
        size = Size(16f, 4f)
    )

    drawDriverWithHelmet(sx + 2f, sy - 24f, Color(0xFF00ACC1))
}

// 8. Classic Hippie Van
private fun DrawScope.drawHippieVanChassis(sx: Float, sy: Float) {
    // Retro Boxy Bus Body
    drawRoundRect(
        color = Color(0xFF8E24AA),
        topLeft = Offset(sx - 42f, sy - 36f),
        size = Size(84f, 40f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Lower White Section
    drawRect(
        color = Color(0xFFF5F5F5),
        topLeft = Offset(sx - 42f, sy - 16f),
        size = Size(84f, 20f)
    )
    // Windows
    drawRect(
        color = Color(0xFF81D4FA),
        topLeft = Offset(sx - 34f, sy - 32f),
        size = Size(18f, 14f)
    )
    drawRect(
        color = Color(0xFF81D4FA),
        topLeft = Offset(sx - 10f, sy - 32f),
        size = Size(20f, 14f)
    )
    drawRect(
        color = Color(0xFF81D4FA),
        topLeft = Offset(sx + 16f, sy - 32f),
        size = Size(20f, 14f)
    )

    // Roof Rack with Surfboard
    drawLine(
        color = Color(0xFF757575),
        start = Offset(sx - 36f, sy - 38f),
        end = Offset(sx + 36f, sy - 38f),
        strokeWidth = 3f
    )
    drawRoundRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(sx - 30f, sy - 44f),
        size = Size(60f, 6f),
        cornerRadius = CornerRadius(3f, 3f)
    )

    drawDriverWithHelmet(sx + 22f, sy - 26f, Color(0xFFFFB300))
}

// 9. Armored Military Tank
private fun DrawScope.drawTankChassis(sx: Float, sy: Float) {
    // Olive Green Armored Tank Hull
    drawRoundRect(
        color = Color(0xFF33691E),
        topLeft = Offset(sx - 48f, sy - 16f),
        size = Size(96f, 22f),
        cornerRadius = CornerRadius(5f, 5f)
    )
    // Armored Rotating Turret
    drawRoundRect(
        color = Color(0xFF1B5E20),
        topLeft = Offset(sx - 24f, sy - 34f),
        size = Size(48f, 18f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    // Long Cannon Barrel
    drawLine(
        color = Color(0xFF2E7D32),
        start = Offset(sx + 18f, sy - 26f),
        end = Offset(sx + 62f, sy - 26f),
        strokeWidth = 6f
    )
    // Muzzle Brake
    drawRect(
        color = Color(0xFF1B5E20),
        topLeft = Offset(sx + 58f, sy - 30f),
        size = Size(6f, 8f)
    )

    // Caterpillar Track loop around wheels
    drawRoundRect(
        color = Color(0xFF212121),
        topLeft = Offset(sx - 52f, sy - 4f),
        size = Size(104f, 12f),
        cornerRadius = CornerRadius(6f, 6f),
        style = Stroke(width = 3.5f)
    )

    drawDriverWithHelmet(sx - 6f, sy - 38f, Color(0xFF33691E))
}

// 10. Formula F1 Hypercar
private fun DrawScope.drawSupercarChassis(sx: Float, sy: Float) {
    // Sleek Yellow/Carbon Formula 1 Body
    val f1Path = Path().apply {
        moveTo(sx - 42f, sy - 4f)
        lineTo(sx - 36f, sy - 16f)
        lineTo(sx - 6f, sy - 22f)
        lineTo(sx + 26f, sy - 14f)
        lineTo(sx + 48f, sy - 4f)
        close()
    }
    drawPath(f1Path, color = Color(0xFFFFD600))

    // Front Wing
    drawRoundRect(
        color = Color(0xFF212121),
        topLeft = Offset(sx + 40f, sy - 6f),
        size = Size(14f, 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // High Downforce Rear Spoiler
    drawLine(
        color = Color(0xFF212121),
        start = Offset(sx - 38f, sy - 14f),
        end = Offset(sx - 42f, sy - 32f),
        strokeWidth = 3f
    )
    drawRoundRect(
        color = Color(0xFF212121),
        topLeft = Offset(sx - 48f, sy - 34f),
        size = Size(18f, 5f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Cockpit Halo & Racer Helmet
    drawDriverWithHelmet(sx + 4f, sy - 24f, Color(0xFFFFD600))
}

// 11. Stunt Unicycle
private fun DrawScope.drawOneWheelerChassis(sx: Float, sy: Float) {
    // Chrome Unicycle Fork
    drawLine(
        color = Color(0xFFECEFF1),
        start = Offset(sx, sy),
        end = Offset(sx, sy - 34f),
        strokeWidth = 4f
    )
    // Saddle Seat
    drawRoundRect(
        color = Color(0xFFE91E63),
        topLeft = Offset(sx - 12f, sy - 38f),
        size = Size(24f, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawDriverWithHelmet(sx, sy - 48f, Color(0xFFE91E63))
}

private fun DrawScope.drawDriverWithHelmet(hx: Float, hy: Float, helmetColor: Color) {
    // Driver Head / Helmet
    drawCircle(
        color = helmetColor,
        radius = 9f,
        center = Offset(hx, hy)
    )
    // Black Visor
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(hx + 1f, hy - 4f),
        size = Size(9f, 6f),
        cornerRadius = CornerRadius(2f, 2f)
    )
}

private fun DrawScope.drawParticles(
    particles: List<Particle>,
    camX: Float,
    camY: Float
) {
    for (p in particles) {
        val sx = p.x + camX
        val sy = p.y + camY
        if (sx in -20f..size.width + 20f && sy in -20f..size.height + 20f && p.currentRadius > 0.5f) {
            drawCircle(
                color = p.color.copy(alpha = (p.life / p.maxLife).coerceIn(0f, 1f)),
                radius = p.currentRadius,
                center = Offset(sx, sy)
            )
        }
    }
}

private fun DrawScope.drawFloatingNotifications(
    notifications: List<FloatingNotification>,
    camX: Float,
    camY: Float
) {
    for (n in notifications) {
        val sx = n.x + camX
        val sy = n.y + camY
        if (sx in -100f..size.width + 100f) {
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(
                    ((n.life / n.maxLife).coerceIn(0f, 1f) * 255).toInt(),
                    (n.color.red * 255).toInt(),
                    (n.color.green * 255).toInt(),
                    (n.color.blue * 255).toInt()
                )
                textSize = 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
            }
            drawContext.canvas.nativeCanvas.drawText(n.text, sx, sy, textPaint)
        }
    }
}
