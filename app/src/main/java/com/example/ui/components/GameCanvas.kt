package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import com.example.model.CoinItem
import com.example.model.EnvironmentType
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
        drawEnvironmentBackground(engine.levelInfo.environment, cameraOffsetX, cameraOffsetY, screenW, screenH)

        // 2. Draw Terrain & Bridges
        drawTerrain(engine, cameraOffsetX, cameraOffsetY, screenW, screenH)

        // 3. Draw Finish Line Banner
        drawFinishLine(engine.trackEndWorldX, engine, cameraOffsetX, cameraOffsetY)

        // 4. Draw Obstacles (Crates/Barrels)
        drawObstacles(engine.obstacles, cameraOffsetX, cameraOffsetY)

        // 5. Draw Collectibles (Coins & Fuel Cans)
        drawCollectibles(engine.coins, engine.fuelCans, cameraOffsetX, cameraOffsetY)

        // 6. Draw Vehicle (Suspension, Wheels, Chassis, Driver, Exhaust)
        drawVehicle(engine, cameraOffsetX, cameraOffsetY)

        // 7. Draw Particles (Tire dirt, explosion sparks)
        drawParticles(engine.particles, cameraOffsetX, cameraOffsetY)

        // 8. Draw Floating Notifications ("+50", "AIRTIME", etc.)
        drawFloatingNotifications(engine.floatingTexts, cameraOffsetX, cameraOffsetY)
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
            // Parallax Mountain range (0.08x scroll)
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

            // Parallax Midground rolling green hills (0.22x scroll)
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
            drawPath(hillPath, color = Color(0xFF81C784).copy(alpha = 0.6f))
        }

        EnvironmentType.DESERT -> {
            // Bright Desert Sun
            drawCircle(
                color = Color(0xFFFFD54F),
                radius = 45f,
                center = Offset(w * 0.78f, h * 0.16f)
            )
            // Distant Pyramids & Dunes (0.12x scroll)
            val pyrX = (camX * 0.12f) % 500f
            val pyrPath = Path().apply {
                moveTo(-100f, h)
                var px = -100f + pyrX
                while (px < w + 250f) {
                    lineTo(px + 100f, h * 0.46f)
                    lineTo(px + 200f, h * 0.72f)
                    px += 280f
                }
                lineTo(w + 250f, h)
                close()
            }
            drawPath(pyrPath, color = Color(0xFFFFB74D).copy(alpha = 0.45f))
        }

        EnvironmentType.ARCTIC -> {
            // Distant Icy Peaks (0.1x scroll)
            val iceX = (camX * 0.1f) % 450f
            val icePath = Path().apply {
                moveTo(-150f, h)
                var ix = -150f + iceX
                while (ix < w + 300f) {
                    lineTo(ix + 140f, h * 0.40f)
                    lineTo(ix + 280f, h * 0.68f)
                    ix += 280f
                }
                lineTo(w + 300f, h)
                close()
            }
            drawPath(icePath, color = Color(0xFFB2EBF2).copy(alpha = 0.5f))
        }

        EnvironmentType.MOON -> {
            // Distant Earth in black sky
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF29B6F6), Color(0xFF1565C0), Color(0xFF0D47A1)),
                    center = Offset(w * 0.82f, h * 0.22f),
                    radius = 36f
                ),
                radius = 36f,
                center = Offset(w * 0.82f, h * 0.22f)
            )
            // Twinkling stars
            val starRand = kotlin.random.Random(42)
            for (i in 0 until 40) {
                val sx = starRand.nextFloat() * w
                val sy = starRand.nextFloat() * (h * 0.5f)
                val sr = starRand.nextFloat() * 2f + 1f
                drawCircle(color = Color.White.copy(alpha = 0.85f), radius = sr, center = Offset(sx, sy))
            }
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
    val step = 14f
    val startWorldX = (-camX - 60f).coerceAtLeast(-100f)
    val endWorldX = (-camX + w + 60f)

    // Build the terrain polygon: top surface points down to bottom of screen
    val terrainPath = Path()
    var firstPoint = true
    var currentX = startWorldX

    while (currentX <= endWorldX) {
        val groundY = engine.getTerrainElevation(currentX)
        val screenX = currentX + camX
        val screenY = groundY + camY

        if (firstPoint) {
            terrainPath.moveTo(screenX, screenY)
            firstPoint = false
        } else {
            terrainPath.lineTo(screenX, screenY)
        }
        currentX += step
    }

    // Seal the polygon at the screen bottom
    terrainPath.lineTo(w + 100f, h + 300f)
    terrainPath.lineTo(-100f, h + 300f)
    terrainPath.close()

    // 1. Subsurface Earth
    drawPath(
        path = terrainPath,
        color = engine.levelInfo.environment.hillSubsurfaceColor
    )

    // 2. Subsurface Texture Strata (decorative wavy layers beneath dirt)
    val strataY = h * 0.72f
    drawLine(
        color = Color.Black.copy(alpha = 0.12f),
        start = Offset(0f, strataY),
        end = Offset(w, strataY),
        strokeWidth = 14f
    )

    // 3. Top Grass / Surface Trim
    val surfaceStrokePath = Path()
    firstPoint = true
    currentX = startWorldX
    while (currentX <= endWorldX) {
        val groundY = engine.getTerrainElevation(currentX)
        val screenX = currentX + camX
        val screenY = groundY + camY

        if (firstPoint) {
            surfaceStrokePath.moveTo(screenX, screenY)
            firstPoint = false
        } else {
            surfaceStrokePath.lineTo(screenX, screenY)
        }
        currentX += step
    }

    drawPath(
        path = surfaceStrokePath,
        color = engine.levelInfo.environment.hillSurfaceColor,
        style = Stroke(width = 10f, cap = StrokeCap.Round)
    )

    // 4. Draw Wooden Bridges (if in visible view)
    for (bridge in engine.bridges) {
        val bStartX = bridge.first + camX
        val bEndX = bridge.second + camX
        if (bEndX >= -50f && bStartX <= w + 50f) {
            drawSuspensionBridge(bridge.first, bridge.second, engine, camX, camY)
        }
    }
}

private fun DrawScope.drawSuspensionBridge(
    startX: Float,
    endX: Float,
    engine: PhysicsEngine,
    camX: Float,
    camY: Float
) {
    val plankStep = 18f
    var px = startX
    while (px < endX) {
        val y = engine.getTerrainElevation(px)
        val screenX = px + camX
        val screenY = y + camY

        // Wooden plank
        drawRoundRect(
            color = Color(0xFF8D6E63),
            topLeft = Offset(screenX - 7f, screenY - 4f),
            size = Size(14f, 8f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // Plank bolt dots
        drawCircle(
            color = Color(0xFF3E2723),
            radius = 1.2f,
            center = Offset(screenX - 4f, screenY)
        )
        drawCircle(
            color = Color(0xFF3E2723),
            radius = 1.2f,
            center = Offset(screenX + 4f, screenY)
        )
        px += plankStep
    }

    // Suspension bridge cable
    val y1 = engine.getTerrainElevation(startX) + camY
    val y2 = engine.getTerrainElevation(endX) + camY
    val midX = (startX + endX) / 2f + camX
    val midY = engine.getTerrainElevation((startX + endX) / 2f) + camY - 20f

    val cablePath = Path().apply {
        moveTo(startX + camX, y1 - 25f)
        quadraticBezierTo(midX, midY, endX + camX, y2 - 25f)
    }
    drawPath(cablePath, color = Color(0xFFB0BEC5), style = Stroke(width = 2.5f))
}

private fun DrawScope.drawFinishLine(
    finishX: Float,
    engine: PhysicsEngine,
    camX: Float,
    camY: Float
) {
    val screenX = finishX + camX
    val groundY = engine.getTerrainElevation(finishX) + camY

    // Flag pole
    drawLine(
        color = Color(0xFFECEFF1),
        start = Offset(screenX, groundY),
        end = Offset(screenX, groundY - 110f),
        strokeWidth = 5f,
        cap = StrokeCap.Round
    )

    // Checkered Flag
    val flagW = 44f
    val flagH = 28f
    val flagTop = groundY - 110f
    drawRect(
        color = Color.White,
        topLeft = Offset(screenX, flagTop),
        size = Size(flagW, flagH)
    )
    // Checkered pattern
    val rows = 3
    val cols = 4
    val cw = flagW / cols
    val ch = flagH / rows
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if ((r + c) % 2 == 0) {
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(screenX + c * cw, flagTop + r * ch),
                    size = Size(cw, ch)
                )
            }
        }
    }

    // FINISH text
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 24f
        isFakeBoldText = true
        setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
    }
    drawContext.canvas.nativeCanvas.drawText("FINISH", screenX - 18f, flagTop - 10f, paint)
}

private fun DrawScope.drawCollectibles(
    coins: List<CoinItem>,
    fuelCans: List<FuelCanItem>,
    camX: Float,
    camY: Float
) {
    // 1. Coins
    for (coin in coins) {
        if (!coin.collected) {
            val sx = coin.x + camX
            val sy = coin.y + camY

            // Outer gold coin rim
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEA00), Color(0xFFFFB300), Color(0xFFFF8F00)),
                    center = Offset(sx, sy),
                    radius = 16f
                ),
                radius = 16f,
                center = Offset(sx, sy)
            )
            // Inner coin ring
            drawCircle(
                color = Color(0xFFFFE082),
                radius = 12f,
                center = Offset(sx, sy),
                style = Stroke(width = 1.5f)
            )

            // Coin Value number
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#4E342E")
                textSize = if (coin.value >= 100) 14f else 16f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                "${coin.value}",
                sx,
                sy + 5f,
                paint
            )
        }
    }

    // 2. Red Fuel Cans
    for (can in fuelCans) {
        if (!can.collected) {
            val sx = can.x + camX
            val sy = can.y + camY

            // Jerrycan main red body
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFFFF1744), Color(0xFFD50000))),
                topLeft = Offset(sx - 14f, sy - 18f),
                size = Size(28f, 36f),
                cornerRadius = CornerRadius(5f, 5f)
            )
            // Handle on top
            drawRoundRect(
                color = Color(0xFFB71C1C),
                topLeft = Offset(sx - 8f, sy - 24f),
                size = Size(16f, 6f),
                cornerRadius = CornerRadius(2f, 2f)
            )
            // Spout
            drawLine(
                color = Color(0xFFFF5252),
                start = Offset(sx + 6f, sy - 20f),
                end = Offset(sx + 12f, sy - 26f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            // Fuel drop / Flame icon on can
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 4.5f,
                center = Offset(sx, sy)
            )
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 10f
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }
            drawContext.canvas.nativeCanvas.drawText("GAS", sx, sy + 12f, paint)
        }
    }
}

private fun DrawScope.drawObstacles(
    obstacles: List<ObstacleItem>,
    camX: Float,
    camY: Float
) {
    for (obs in obstacles) {
        val sx = obs.x + camX
        val sy = obs.y + camY

        rotate(degrees = obs.angle * 180f / PI.toFloat(), pivot = Offset(sx, sy)) {
            val halfW = obs.width / 2f
            val halfH = obs.height / 2f

            if (obs.type == ObstacleType.WOOD_CRATE) {
                // Wooden Crate
                drawRoundRect(
                    color = Color(0xFF8D6E63),
                    topLeft = Offset(sx - halfW, sy - halfH),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(3f, 3f)
                )
                // Border frame
                drawRoundRect(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(sx - halfW, sy - halfH),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(3f, 3f),
                    style = Stroke(width = 2.5f)
                )
                // Diagonal wooden brace
                drawLine(
                    color = Color(0xFF5D4037),
                    start = Offset(sx - halfW, sy - halfH),
                    end = Offset(sx + halfW, sy + halfH),
                    strokeWidth = 2.5f
                )
            } else {
                // Steel Oil Barrel
                drawRoundRect(
                    color = Color(0xFF37474F),
                    topLeft = Offset(sx - halfW, sy - halfH),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                // Barrel ribs
                drawLine(
                    color = Color(0xFF78909C),
                    start = Offset(sx - halfW, sy - halfH * 0.3f),
                    end = Offset(sx + halfW, sy - halfH * 0.3f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color(0xFF78909C),
                    start = Offset(sx - halfW, sy + halfH * 0.3f),
                    end = Offset(sx + halfW, sy + halfH * 0.3f),
                    strokeWidth = 2f
                )
                // Hazard symbol
                drawCircle(
                    color = Color(0xFFFFD600),
                    radius = 3.5f,
                    center = Offset(sx, sy)
                )
            }
        }
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
    val halfBase = engine.vehicleType.wheelBase * 0.5f
    val suspLen = engine.vehicleType.suspensionLength

    val rearAnchorSX = chassisSX - cosA * halfBase - sinA * suspLen * 0.4f
    val rearAnchorSY = chassisSY - sinA * halfBase + cosA * suspLen * 0.4f
    val frontAnchorSX = chassisSX + cosA * halfBase - sinA * suspLen * 0.4f
    val frontAnchorSY = chassisSY + sinA * halfBase + cosA * suspLen * 0.4f

    // 1. Suspension Springs (zigzag coils between anchors and wheel centers)
    drawSuspensionCoil(rearAnchorSX, rearAnchorSY, rearSX, rearSY)
    drawSuspensionCoil(frontAnchorSX, frontAnchorSY, frontSX, frontSY)

    // 2. Wheels
    drawWheel(rearSX, rearSY, engine.vehicleType.wheelRadius, engine.rearWheelSpin)
    drawWheel(frontSX, frontSY, engine.vehicleType.wheelRadius, engine.frontWheelSpin)

    // 3. Vehicle Body / Chassis & Driver
    rotate(degrees = chassisDeg, pivot = Offset(chassisSX, chassisSY)) {
        when (engine.vehicleType) {
            VehicleType.JEEP -> drawJeepChassis(chassisSX, chassisSY, engine.boostValue > 20f)
            VehicleType.BIKE -> drawBikeChassis(chassisSX, chassisSY)
            VehicleType.MONSTER_TRUCK -> drawMonsterChassis(chassisSX, chassisSY)
            VehicleType.TANK -> drawTankChassis(chassisSX, chassisSY)
        }
    }
}

private fun DrawScope.drawSuspensionCoil(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float
) {
    val coilPath = Path()
    coilPath.moveTo(x1, y1)
    val coils = 4
    for (i in 1..coils) {
        val t1 = (i - 0.5f) / coils
        val t2 = i.toFloat() / coils
        val px = x1 + (x2 - x1) * t1
        val py = y1 + (y2 - y1) * t1
        val perpX = -(y2 - y1) * 0.14f
        val perpY = (x2 - x1) * 0.14f
        val sign = if (i % 2 == 0) 1f else -1f

        coilPath.lineTo(px + perpX * sign, py + perpY * sign)
        coilPath.lineTo(x1 + (x2 - x1) * t2, y1 + (y2 - y1) * t2)
    }

    drawPath(
        path = coilPath,
        color = Color(0xFF78909C),
        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawWheel(x: Float, y: Float, radius: Float, spinAngle: Float) {
    // Tire Rubber
    drawCircle(
        color = Color(0xFF1E293B),
        radius = radius,
        center = Offset(x, y)
    )

    // Tire Tread Studs
    val treadCount = 8
    val treadAngleDeg = spinAngle * 180f / PI.toFloat()
    rotate(degrees = treadAngleDeg, pivot = Offset(x, y)) {
        for (i in 0 until treadCount) {
            val ang = i * (2f * PI.toFloat() / treadCount)
            val tx = x + cos(ang) * (radius - 2f)
            val ty = y + sin(ang) * (radius - 2f)
            drawCircle(color = Color(0xFF334155), radius = 2.5f, center = Offset(tx, ty))
        }

        // Alloy Rim
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFECEFF1), Color(0xFF78909C)),
                center = Offset(x, y),
                radius = radius * 0.62f
            ),
            radius = radius * 0.62f,
            center = Offset(x, y)
        )

        // Rim Spokes
        drawLine(
            color = Color(0xFF37474F),
            start = Offset(x - radius * 0.55f, y),
            end = Offset(x + radius * 0.55f, y),
            strokeWidth = 2.5f
        )
        drawLine(
            color = Color(0xFF37474F),
            start = Offset(x, y - radius * 0.55f),
            end = Offset(x, y + radius * 0.55f),
            strokeWidth = 2.5f
        )

        // Center Lug Nut Hub
        drawCircle(color = Color(0xFFD32F2F), radius = 3.5f, center = Offset(x, y))
    }
}

private fun DrawScope.drawJeepChassis(cx: Float, cy: Float, isBoosting: Boolean) {
    // 1. Red Main Tub/Body
    val bodyPath = Path().apply {
        moveTo(cx - 38f, cy + 12f)
        lineTo(cx + 42f, cy + 12f)
        lineTo(cx + 42f, cy - 2f)
        lineTo(cx + 22f, cy - 8f) // Hood
        lineTo(cx + 2f, cy - 8f)
        lineTo(cx - 6f, cy - 2f)  // Cab cutout
        lineTo(cx - 38f, cy - 2f) // Rear cargo bed
        close()
    }
    drawPath(bodyPath, color = Color(0xFFD32F2F))

    // Body trim highlights
    drawRoundRect(
        color = Color(0xFFB71C1C),
        topLeft = Offset(cx - 38f, cy + 8f),
        size = Size(80f, 4f),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Number '7' decal (from original Hill Climb Racer jeep!)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 15f
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
    }
    drawContext.canvas.nativeCanvas.drawText("7", cx + 8f, cy + 8f, paint)

    // 2. Roll Cage Bars
    drawLine(
        color = Color(0xFF263238),
        start = Offset(cx - 30f, cy - 2f),
        end = Offset(cx - 18f, cy - 26f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF263238),
        start = Offset(cx - 18f, cy - 26f),
        end = Offset(cx + 8f, cy - 26f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF263238),
        start = Offset(cx + 8f, cy - 26f),
        end = Offset(cx + 14f, cy - 8f), // Windshield strut
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )

    // 3. Driver (Bill!)
    // Torso / Red shirt
    drawRoundRect(
        color = Color(0xFFC62828),
        topLeft = Offset(cx - 14f, cy - 14f),
        size = Size(14f, 14f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Driver Head / Face
    drawCircle(
        color = Color(0xFFFFCC80),
        radius = 7.5f,
        center = Offset(cx - 7f, cy - 20f)
    )
    // Driver Red Cap
    drawArc(
        color = Color(0xFFD32F2F),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - 15f, cy - 28f),
        size = Size(16f, 16f)
    )
    // Cap bill
    drawLine(
        color = Color(0xFFD32F2F),
        start = Offset(cx - 1f, cy - 20f),
        end = Offset(cx + 6f, cy - 20f),
        strokeWidth = 2.5f,
        cap = StrokeCap.Round
    )

    // 4. Spare Tire Mounted on Back
    drawCircle(
        color = Color(0xFF263238),
        radius = 11f,
        center = Offset(cx - 42f, cy + 2f)
    )
    drawCircle(
        color = Color(0xFF78909C),
        radius = 5.5f,
        center = Offset(cx - 42f, cy + 2f)
    )

    // 5. Headlight Glow
    drawCircle(
        color = Color(0xFFFFF59D).copy(alpha = 0.9f),
        radius = 4f,
        center = Offset(cx + 42f, cy + 3f)
    )

    // 6. Exhaust Pipe & Turbo Fire
    drawRoundRect(
        color = Color(0xFF455A64),
        topLeft = Offset(cx - 42f, cy + 10f),
        size = Size(8f, 4f),
        cornerRadius = CornerRadius(1f, 1f)
    )
    if (isBoosting) {
        // Boost Flame
        val flamePath = Path().apply {
            moveTo(cx - 42f, cy + 10f)
            lineTo(cx - 62f, cy + 12f)
            lineTo(cx - 42f, cy + 14f)
            close()
        }
        drawPath(flamePath, color = Color(0xFFFF9100))
        val innerFlame = Path().apply {
            moveTo(cx - 42f, cy + 11f)
            lineTo(cx - 52f, cy + 12f)
            lineTo(cx - 42f, cy + 13f)
            close()
        }
        drawPath(innerFlame, color = Color(0xFFFFEA00))
    }
}

private fun DrawScope.drawBikeChassis(cx: Float, cy: Float) {
    // Dirt Bike Frame (tubular orange bars)
    val frameColor = Color(0xFFF57C00)
    // Main frame backbone
    drawLine(
        color = frameColor,
        start = Offset(cx - 26f, cy + 8f),
        end = Offset(cx + 4f, cy - 14f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = frameColor,
        start = Offset(cx + 4f, cy - 14f),
        end = Offset(cx + 28f, cy + 8f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    // Handlebars
    drawLine(
        color = Color(0xFF263238),
        start = Offset(cx + 2f, cy - 14f),
        end = Offset(cx + 12f, cy - 24f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    // Orange gas tank & plastics
    drawRoundRect(
        color = frameColor,
        topLeft = Offset(cx - 6f, cy - 16f),
        size = Size(18f, 7f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    // Biker Driver (leaning forward)
    drawRoundRect(
        color = Color(0xFFE65100),
        topLeft = Offset(cx - 16f, cy - 20f),
        size = Size(15f, 16f),
        cornerRadius = CornerRadius(3f, 3f)
    )
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = 8f,
        center = Offset(cx - 4f, cy - 26f)
    )
    // Full face helmet visor
    drawArc(
        color = Color(0xFF212121),
        startAngle = 270f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = Offset(cx - 4f, cy - 30f),
        size = Size(8f, 8f)
    )
}

private fun DrawScope.drawMonsterChassis(cx: Float, cy: Float) {
    // Heavy blue pickup truck cab
    drawRoundRect(
        color = Color(0xFF1976D2),
        topLeft = Offset(cx - 36f, cy - 18f),
        size = Size(74f, 24f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Cab roof
    drawRoundRect(
        color = Color(0xFF0D47A1),
        topLeft = Offset(cx - 16f, cy - 30f),
        size = Size(36f, 14f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Windows
    drawRoundRect(
        color = Color(0xFFB3E5FC),
        topLeft = Offset(cx - 12f, cy - 27f),
        size = Size(30f, 9f),
        cornerRadius = CornerRadius(2f, 2f)
    )
    // High-rise monster suspension subframe
    drawLine(
        color = Color(0xFF455A64),
        start = Offset(cx - 30f, cy + 6f),
        end = Offset(cx + 30f, cy + 6f),
        strokeWidth = 6f
    )
}

private fun DrawScope.drawTankChassis(cx: Float, cy: Float) {
    // Heavy olive green armor body
    drawRoundRect(
        color = Color(0xFF388E3C),
        topLeft = Offset(cx - 42f, cy - 8f),
        size = Size(86f, 20f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    // Turret
    drawRoundRect(
        color = Color(0xFF2E7D32),
        topLeft = Offset(cx - 16f, cy - 22f),
        size = Size(36f, 15f),
        cornerRadius = CornerRadius(5f, 5f)
    )
    // Tank cannon barrel
    drawLine(
        color = Color(0xFF1B5E20),
        start = Offset(cx + 18f, cy - 16f),
        end = Offset(cx + 52f, cy - 16f),
        strokeWidth = 5f,
        cap = StrokeCap.Round
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
        val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        drawCircle(
            color = p.color.copy(alpha = alpha),
            radius = p.currentRadius,
            center = Offset(sx, sy)
        )
    }
}

private fun DrawScope.drawFloatingNotifications(
    notifications: List<FloatingNotification>,
    camX: Float,
    camY: Float
) {
    for (fn in notifications) {
        val sx = fn.x + camX
        val sy = fn.y + camY
        val alpha = (fn.life / fn.maxLife).coerceIn(0f, 1f)

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 22f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
            this.alpha = (alpha * 255).toInt()
        }
        drawContext.canvas.nativeCanvas.drawText(fn.text, sx, sy, paint)
    }
}
