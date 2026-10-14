package com.example.physics

import androidx.compose.ui.graphics.Color
import com.example.model.CoinItem
import com.example.model.EnvironmentType
import com.example.model.FloatingNotification
import com.example.model.FuelCanItem
import com.example.model.GameState
import com.example.model.LevelInfo
import com.example.model.ObstacleItem
import com.example.model.ObstacleType
import com.example.model.Particle
import com.example.model.VehicleType
import com.example.model.VehicleUpgradeState
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class PhysicsEngine(
    val vehicleType: VehicleType,
    val upgrades: VehicleUpgradeState,
    val levelInfo: LevelInfo
) {
    // Terrain base parameters
    val baseGroundY = 460f
    val trackEndWorldX = levelInfo.lengthMeters * 26f

    // Vehicle state
    var chassisX: Float = 120f
    var chassisY: Float = baseGroundY - 60f
    var chassisAngle: Float = 0f // in radians
    var chassisVx: Float = 0f
    var chassisVy: Float = 0f
    var chassisAngularVel: Float = 0f

    // Wheel positions and velocities
    var rearWheelX: Float = 120f - vehicleType.wheelBase * 0.5f
    var rearWheelY: Float = baseGroundY - vehicleType.wheelRadius
    var rearWheelVx: Float = 0f
    var rearWheelVy: Float = 0f
    var rearWheelSpin: Float = 0f

    var frontWheelX: Float = 120f + vehicleType.wheelBase * 0.5f
    var frontWheelY: Float = baseGroundY - vehicleType.wheelRadius
    var frontWheelVx: Float = 0f
    var frontWheelVy: Float = 0f
    var frontWheelSpin: Float = 0f

    var rearWheelOnGround: Boolean = false
    var frontWheelOnGround: Boolean = false

    // Gauges / Engine state
    var rpm: Float = 900f // idle RPM
    var currentSpeedKmh: Float = 0f
    var boostValue: Float = 0f

    // Fuel state
    val maxFuel: Float = 100f + (upgrades.fuelLevel - 1) * 18f
    var currentFuel: Float = maxFuel
    val fuelConsumptionRate: Float = (0.025f / (1f + (upgrades.fuelLevel - 1) * 0.12f))

    // Stunt & Airtime tracking
    var isAirborne: Boolean = false
    var airtimeSeconds: Float = 0f
    var totalAirtimeAccumulated: Float = 0f
    var airborneRotationAccumulator: Float = 0f
    var lastSafeAngle: Float = 0f

    // Game stats
    var distanceMeters: Int = 0
    var coinsCollectedCount: Int = 0
    var coinsCollectedScore: Int = 0
    var totalFuelCollected: Int = 0
    var perfectRunEligible: Boolean = true

    // World Items
    val coins = mutableListOf<CoinItem>()
    val fuelCans = mutableListOf<FuelCanItem>()
    val obstacles = mutableListOf<ObstacleItem>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingNotification>()

    // Bridges definition: (startX, endX)
    val bridges = mutableListOf<Pair<Float, Float>>()

    // Performance & Tunables
    private val enginePowerMultiplier: Float =
        vehicleType.baseEngineTorque * (1.0f + (upgrades.engineLevel - 1) * 0.16f)
    private val tireGripMultiplier: Float =
        vehicleType.baseTireGrip * (1.0f + (upgrades.tiresLevel - 1) * 0.12f) *
                levelInfo.environment.frictionMultiplier
    private val suspensionStiffness: Float =
        (220f + (upgrades.suspensionLevel - 1) * 25f)
    private val suspensionDamping: Float =
        (16f + (upgrades.suspensionLevel - 1) * 1.5f)

    init {
        generateLevelTrack()
    }

    private fun generateLevelTrack() {
        val rand = Random(levelInfo.levelNumber * 1007L)
        val step = 140f
        var curX = 350f

        // Procedurally place bridges at certain intervals
        if (levelInfo.gapFrequency > 0f) {
            var bridgeX = 650f + rand.nextFloat() * 200f
            while (bridgeX < trackEndWorldX - 300f) {
                val bridgeLen = 160f + rand.nextFloat() * 100f * levelInfo.gapFrequency
                bridges.add(Pair(bridgeX, bridgeX + bridgeLen))
                bridgeX += 700f + rand.nextFloat() * 600f
            }
        }

        // Spawn coins and fuel cans along track
        var lastFuelCanX = 150f
        var coinId = 0
        var fuelId = 0
        var obsId = 0

        while (curX < trackEndWorldX) {
            // Fuel cans every ~450 to 650 meters
            if (curX - lastFuelCanX >= 480f) {
                val groundY = getTerrainElevation(curX)
                fuelCans.add(FuelCanItem(fuelId++, curX, groundY - 26f))
                lastFuelCanX = curX
                curX += 90f
                continue
            }

            // Coin clusters
            val clusterType = rand.nextInt(4)
            val groundY = getTerrainElevation(curX)

            when (clusterType) {
                0 -> {
                    // Parabolic arc (jump arc)
                    val coinVal = if (rand.nextBoolean()) 5 else 10
                    for (i in 0 until 5) {
                        val cx = curX + i * 36f
                        val gy = getTerrainElevation(cx)
                        val arcHeight = 40f * sin(i * PI.toFloat() / 4f)
                        coins.add(CoinItem(coinId++, cx, gy - 30f - arcHeight, coinVal))
                    }
                    curX += 5 * 36f + 60f
                }
                1 -> {
                    // High value coins on hillcrest
                    val coinVal = if (rand.nextInt(3) == 0) 100 else 50
                    coins.add(CoinItem(coinId++, curX, groundY - 32f, coinVal))
                    coins.add(CoinItem(coinId++, curX + 38f, getTerrainElevation(curX + 38f) - 34f, coinVal))
                    curX += 100f
                }
                2 -> {
                    // Ground line of 25s
                    for (i in 0 until 4) {
                        val cx = curX + i * 32f
                        coins.add(CoinItem(coinId++, cx, getTerrainElevation(cx) - 24f, 25))
                    }
                    curX += 4 * 32f + 50f
                }
                else -> {
                    // Obstacle placement (crates/barrels)
                    if (curX > 400f && rand.nextFloat() < 0.45f) {
                        val obsY = getTerrainElevation(curX)
                        val type = if (rand.nextBoolean()) ObstacleType.WOOD_CRATE else ObstacleType.OIL_BARREL
                        val size = if (type == ObstacleType.WOOD_CRATE) 28f else 24f
                        obstacles.add(
                            ObstacleItem(
                                id = obsId++,
                                x = curX,
                                y = obsY - size * 0.5f,
                                width = size,
                                height = size,
                                type = type
                            )
                        )
                    }
                    curX += step
                }
            }
        }
    }

    /**
     * Smooth procedural terrain elevation function.
     * Returns the ground surface Y coordinate in screen/world space.
     */
    fun getTerrainElevation(x: Float): Float {
        if (x < 0f) return baseGroundY

        // Check if inside a wooden bridge span
        for (bridge in bridges) {
            if (x in bridge.first..bridge.second) {
                val span = bridge.second - bridge.first
                val progress = (x - bridge.first) / span
                val y1 = getRawTerrainElevation(bridge.first)
                val y2 = getRawTerrainElevation(bridge.second)
                // Catenary sag in the middle of suspension bridge
                val sag = sin(progress * PI.toFloat()) * 26f
                return (y1 * (1f - progress) + y2 * progress) + sag
            }
        }

        return getRawTerrainElevation(x)
    }

    private fun getRawTerrainElevation(x: Float): Float {
        val s = levelInfo.steepnessFactor
        val w1 = sin(x * 0.0032f) * (90f * s)
        val w2 = sin(x * 0.0084f) * (42f * s)
        val w3 = sin(x * 0.019f) * (18f * s)
        val w4 = sin(x * 0.00075f) * (140f * s)
        // Gentle start platform for initial 220px
        val startBlend = min(1f, max(0f, x / 240f))
        return baseGroundY + (w1 + w2 + w3 + w4) * startBlend
    }

    /**
     * Compute terrain slope angle (in radians) and normal vector at x
     */
    fun getTerrainSlope(x: Float): Float {
        val delta = 2.0f
        val y1 = getTerrainElevation(x - delta)
        val y2 = getTerrainElevation(x + delta)
        return atan2(y2 - y1, delta * 2f)
    }

    fun isBridgeSection(x: Float): Boolean {
        for (b in bridges) {
            if (x in b.first..b.second) return true
        }
        return false
    }

    /**
     * Primary physics tick (called at fixed ~60 FPS dt = 1/60s).
     */
    fun update(
        dt: Float,
        isGasPressed: Boolean,
        isBrakePressed: Boolean,
        onSoundEvent: (String) -> Unit
    ): GameState {
        val grav = levelInfo.environment.gravity * 70f

        // Fuel logic
        if (currentFuel > 0f) {
            val burn = fuelConsumptionRate * dt * (if (isGasPressed) 6.2f else 1.2f)
            currentFuel = max(0f, currentFuel - burn)
        }
        val hasFuel = currentFuel > 0.01f

        // Engine & RPM
        val targetRpm = when {
            isGasPressed && hasFuel -> 6800f + Random.nextFloat() * 400f
            isBrakePressed -> 3200f
            else -> 950f + Random.nextFloat() * 100f
        }
        rpm += (targetRpm - rpm) * min(1f, dt * 10f)

        // Boost accumulation indicator
        if (isGasPressed && hasFuel) {
            boostValue = min(100f, boostValue + dt * 25f)
        } else {
            boostValue = max(0f, boostValue - dt * 35f)
        }

        // 1. Suspension & Wheel Physics
        val cosA = cos(chassisAngle)
        val sinA = sin(chassisAngle)
        val halfBase = vehicleType.wheelBase * 0.5f
        val suspLen = vehicleType.suspensionLength

        // Wheel anchor points on chassis
        val rearAnchorX = chassisX - cosA * halfBase - sinA * suspLen * 0.4f
        val rearAnchorY = chassisY - sinA * halfBase + cosA * suspLen * 0.4f
        val frontAnchorX = chassisX + cosA * halfBase - sinA * suspLen * 0.4f
        val frontAnchorY = chassisY + sinA * halfBase + cosA * suspLen * 0.4f

        // Rear Wheel Ground Collision
        val rearGroundY = getTerrainElevation(rearWheelX)
        val rearDistToGround = (rearWheelY + vehicleType.wheelRadius) - rearGroundY
        rearWheelOnGround = rearDistToGround >= 0f

        if (rearWheelOnGround) {
            rearWheelY = rearGroundY - vehicleType.wheelRadius
            if (rearWheelVy > 0f) {
                rearWheelVy = -rearWheelVy * 0.12f
            }
            val slopeAngle = getTerrainSlope(rearWheelX)
            val slopeNormalX = -sin(slopeAngle)
            val slopeNormalY = -cos(slopeAngle)

            // Normal support force
            rearWheelVy -= (grav * dt) * 0.85f

            // Drive torque / traction
            if (hasFuel && isGasPressed) {
                val driveAcc = 360f * enginePowerMultiplier * tireGripMultiplier
                rearWheelVx += driveAcc * dt * cos(slopeAngle)
                rearWheelVy += driveAcc * dt * sin(slopeAngle)
                rearWheelSpin += 22f * dt
                // Exhaust & tire dirt particles
                spawnTireDirt(rearWheelX, rearWheelY, -cos(slopeAngle), -sin(slopeAngle))
            } else if (isBrakePressed) {
                // Braking friction
                rearWheelVx *= (1f - dt * 6.5f)
                if (chassisVx < 5f) {
                    // Reverse gear
                    val revAcc = 180f * enginePowerMultiplier * tireGripMultiplier
                    rearWheelVx -= revAcc * dt * cos(slopeAngle)
                    rearWheelVy -= revAcc * dt * sin(slopeAngle)
                }
            } else {
                // Rolling resistance
                rearWheelVx *= (1f - dt * 0.4f)
            }
        } else {
            rearWheelVy += grav * dt
        }

        // Front Wheel Ground Collision
        val frontGroundY = getTerrainElevation(frontWheelX)
        val frontDistToGround = (frontWheelY + vehicleType.wheelRadius) - frontGroundY
        frontWheelOnGround = frontDistToGround >= 0f

        if (frontWheelOnGround) {
            frontWheelY = frontGroundY - vehicleType.wheelRadius
            if (frontWheelVy > 0f) {
                frontWheelVy = -frontWheelVy * 0.12f
            }
            val slopeAngle = getTerrainSlope(frontWheelX)
            frontWheelVy -= (grav * dt) * 0.85f

            if (hasFuel && isGasPressed && vehicleType == VehicleType.MONSTER_TRUCK) {
                // 4WD for monster truck
                val driveAcc = 200f * enginePowerMultiplier * tireGripMultiplier
                frontWheelVx += driveAcc * dt * cos(slopeAngle)
                frontWheelVy += driveAcc * dt * sin(slopeAngle)
            }
            if (isBrakePressed) {
                frontWheelVx *= (1f - dt * 6.5f)
            } else {
                frontWheelVx *= (1f - dt * 0.4f)
            }
        } else {
            frontWheelVy += grav * dt
        }

        // 2. Spring-damper forces connecting chassis to wheels
        // Rear suspension vector
        val rdx = rearWheelX - rearAnchorX
        val rdy = rearWheelY - rearAnchorY
        val rDist = sqrt(rdx * rdx + rdy * rdy)
        val rDelta = rDist - suspLen
        val rSpringForce = rDelta * suspensionStiffness

        // Front suspension vector
        val fdx = frontWheelX - frontAnchorX
        val fdy = frontWheelY - frontAnchorY
        val fDist = sqrt(fdx * fdx + fdy * fdy)
        val fDelta = fDist - suspLen
        val fSpringForce = fDelta * suspensionStiffness

        // Chassis Gravity
        chassisVy += grav * dt

        // Forces on chassis from springs
        if (rDist > 0.001f) {
            val rDirX = rdx / rDist
            val rDirY = rdy / rDist
            chassisVx += (rDirX * rSpringForce / vehicleType.baseMass) * dt
            chassisVy += (rDirY * rSpringForce / vehicleType.baseMass) * dt

            // Torque on chassis: cross product
            val rArmX = rearAnchorX - chassisX
            val rArmY = rearAnchorY - chassisY
            val torqueR = (rArmX * (rDirY * rSpringForce) - rArmY * (rDirX * rSpringForce))
            chassisAngularVel += (torqueR / (vehicleType.baseMass * 35f)) * dt
        }

        if (fDist > 0.001f) {
            val fDirX = fdx / fDist
            val fDirY = fdy / fDist
            chassisVx += (fDirX * fSpringForce / vehicleType.baseMass) * dt
            chassisVy += (fDirY * fSpringForce / vehicleType.baseMass) * dt

            val fArmX = frontAnchorX - chassisX
            val fArmY = frontAnchorY - chassisY
            val torqueF = (fArmX * (fDirY * fSpringForce) - fArmY * (fDirX * fSpringForce))
            chassisAngularVel += (torqueF / (vehicleType.baseMass * 35f)) * dt
        }

        // 3. Air Control (Stunt Mechanics)
        val currentlyAirborne = !rearWheelOnGround && !frontWheelOnGround
        if (currentlyAirborne) {
            airtimeSeconds += dt
            if (airtimeSeconds > 0.45f) {
                isAirborne = true
            }
            // In air: Gas pitches car back (up), Brake pitches car forward (down)
            if (isGasPressed) {
                chassisAngularVel += 4.5f * dt
            }
            if (isBrakePressed) {
                chassisAngularVel -= 4.5f * dt
            }
            airborneRotationAccumulator += chassisAngularVel * dt

            // Detect full 360 flips
            if (airborneRotationAccumulator >= (2f * PI.toFloat() - 0.2f)) {
                airborneRotationAccumulator -= (2f * PI.toFloat())
                coinsCollectedScore += 250
                floatingTexts.add(
                    FloatingNotification(
                        System.currentTimeMillis(),
                        "BACKFLIP! +250",
                        Color(0xFFFFD700),
                        chassisX,
                        chassisY - 60f,
                        1.5f
                    )
                )
                onSoundEvent("flip")
            } else if (airborneRotationAccumulator <= -(2f * PI.toFloat() - 0.2f)) {
                airborneRotationAccumulator += (2f * PI.toFloat())
                coinsCollectedScore += 300
                floatingTexts.add(
                    FloatingNotification(
                        System.currentTimeMillis(),
                        "FRONTFLIP! +300",
                        Color(0xFFFFD700),
                        chassisX,
                        chassisY - 60f,
                        1.5f
                    )
                )
                onSoundEvent("flip")
            }
        } else {
            // Landed!
            if (isAirborne && airtimeSeconds > 0.9f) {
                val airtimeScore = (airtimeSeconds * 120f).toInt()
                coinsCollectedScore += airtimeScore
                totalAirtimeAccumulated += airtimeSeconds
                floatingTexts.add(
                    FloatingNotification(
                        System.currentTimeMillis(),
                        "AIRTIME ${"%.1f".format(airtimeSeconds)}s +$airtimeScore",
                        Color(0xFF00E676),
                        chassisX,
                        chassisY - 50f,
                        1.4f
                    )
                )
                onSoundEvent("airtime")
            }
            isAirborne = false
            airtimeSeconds = 0f
            airborneRotationAccumulator = 0f
        }

        // Angular damping
        chassisAngularVel *= (1f - dt * 2.8f)

        // Integrate velocities to positions
        chassisX += chassisVx * dt
        chassisY += chassisVy * dt
        chassisAngle += chassisAngularVel * dt

        rearWheelX += rearWheelVx * dt
        rearWheelY += rearWheelVy * dt
        frontWheelX += frontWheelVx * dt
        frontWheelY += frontWheelVy * dt

        // Keep wheels constrained close to anchor
        keepWheelInTether(rearAnchorX, rearAnchorY, isRear = true)
        keepWheelInTether(frontAnchorX, frontAnchorY, isRear = false)

        // Update speed reading (km/h)
        val speedMagnitude = sqrt(chassisVx * chassisVx + chassisVy * chassisVy)
        currentSpeedKmh = speedMagnitude * 0.16f

        // Distance updated
        distanceMeters = max(distanceMeters, (chassisX / 26f).toInt())

        // 4. Driver Head Position & Crash Detection
        val driverHeadOffsetX = -sinA * 28f
        val driverHeadOffsetY = -cosA * 28f
        val driverHeadX = chassisX + driverHeadOffsetX
        val driverHeadY = chassisY + driverHeadOffsetY

        val groundAtDriverHead = getTerrainElevation(driverHeadX)
        if (driverHeadY >= groundAtDriverHead - 4f) {
            // Head hit ground! Fatal crash
            perfectRunEligible = false
            onSoundEvent("crash")
            spawnCrashExplosion(chassisX, chassisY)
            return GameState.GAME_OVER_FLIPPED
        }

        // Extreme tilt crash (e.g. resting completely inverted on roof)
        val normalizedAngle = ((chassisAngle % (2f * PI.toFloat())) + 2f * PI.toFloat()) % (2f * PI.toFloat())
        if (normalizedAngle in (1.9f)..(4.4f) && rearWheelOnGround && frontWheelOnGround) {
            perfectRunEligible = false
            onSoundEvent("crash")
            return GameState.GAME_OVER_FLIPPED
        }

        // 5. Collectibles & Obstacle Collisions
        checkCoinPickups(onSoundEvent)
        checkFuelPickups(onSoundEvent)
        updateObstacles(dt, onSoundEvent)
        updateParticles(dt)
        updateFloatingTexts(dt)

        // 6. Finish line reached?
        if (chassisX >= trackEndWorldX) {
            onSoundEvent("win")
            return GameState.LEVEL_COMPLETED
        }

        // 7. Out of fuel & stopped?
        if (currentFuel <= 0.001f && speedMagnitude < 3.5f) {
            onSoundEvent("fuel_empty")
            return GameState.GAME_OVER_FUEL
        }

        return GameState.PLAYING
    }

    private fun keepWheelInTether(anchorX: Float, anchorY: Float, isRear: Boolean) {
        val maxExtent = vehicleType.suspensionLength * 1.5f
        val minExtent = vehicleType.suspensionLength * 0.35f
        val wx = if (isRear) rearWheelX else frontWheelX
        val wy = if (isRear) rearWheelY else frontWheelY

        val dx = wx - anchorX
        val dy = wy - anchorY
        val d = sqrt(dx * dx + dy * dy)

        if (d > maxExtent && d > 0.001f) {
            val clampedX = anchorX + (dx / d) * maxExtent
            val clampedY = anchorY + (dy / d) * maxExtent
            if (isRear) {
                rearWheelX = clampedX
                rearWheelY = clampedY
            } else {
                frontWheelX = clampedX
                frontWheelY = clampedY
            }
        }
    }

    private fun checkCoinPickups(onSoundEvent: (String) -> Unit) {
        val pickupRadiusSq = 44f * 44f
        for (coin in coins) {
            if (!coin.collected) {
                val dx = chassisX - coin.x
                val dy = chassisY - coin.y
                if (dx * dx + dy * dy <= pickupRadiusSq) {
                    coin.collected = true
                    coinsCollectedCount++
                    coinsCollectedScore += coin.value
                    spawnCoinSparkles(coin.x, coin.y)
                    floatingTexts.add(
                        FloatingNotification(
                            System.currentTimeMillis() + coin.id,
                            "+${coin.value}",
                            Color(0xFFFFD700),
                            coin.x,
                            coin.y - 15f,
                            0.9f
                        )
                    )
                    onSoundEvent("coin")
                }
            }
        }
    }

    private fun checkFuelPickups(onSoundEvent: (String) -> Unit) {
        val pickupRadiusSq = 48f * 48f
        for (can in fuelCans) {
            if (!can.collected) {
                val dx = chassisX - can.x
                val dy = chassisY - can.y
                if (dx * dx + dy * dy <= pickupRadiusSq) {
                    can.collected = true
                    totalFuelCollected++
                    currentFuel = maxFuel
                    floatingTexts.add(
                        FloatingNotification(
                            System.currentTimeMillis() + can.id,
                            "FUEL +100%!",
                            Color(0xFFFF3D00),
                            can.x,
                            can.y - 25f,
                            1.4f
                        )
                    )
                    onSoundEvent("fuel")
                }
            }
        }
    }

    private fun updateObstacles(dt: Float, onSoundEvent: (String) -> Unit) {
        for (obs in obstacles) {
            if (!obs.isDestroyed) {
                val dx = chassisX - obs.x
                val dy = chassisY - obs.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 46f) {
                    // Impact! Break or launch obstacle
                    obs.vx = chassisVx * 0.8f + (Random.nextFloat() * 120f - 60f)
                    obs.vy = -140f - Random.nextFloat() * 100f
                    obs.vRot = Random.nextFloat() * 15f - 7.5f
                    obs.isDestroyed = true
                    onSoundEvent("hit")
                    spawnObstacleDebris(obs.x, obs.y, obs.type)
                }
            } else {
                // Flying debris physics
                obs.vy += 450f * dt
                obs.x += obs.vx * dt
                obs.y += obs.vy * dt
                obs.angle += obs.vRot * dt
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt
            if (p.life <= 0f) {
                iter.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.currentRadius = p.initialRadius * (p.life / p.maxLife)
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val iter = floatingTexts.iterator()
        while (iter.hasNext()) {
            val ft = iter.next()
            ft.life -= dt
            if (ft.life <= 0f) {
                iter.remove()
            } else {
                ft.y -= 26f * dt
            }
        }
    }

    private fun spawnTireDirt(x: Float, y: Float, dirX: Float, dirY: Float) {
        if (particles.size > 80) return
        for (i in 0 until 2) {
            particles.add(
                Particle(
                    x = x + Random.nextFloat() * 8f - 4f,
                    y = y + Random.nextFloat() * 4f,
                    vx = dirX * (40f + Random.nextFloat() * 70f) + Random.nextFloat() * 30f - 15f,
                    vy = -20f - Random.nextFloat() * 40f,
                    color = levelInfo.environment.hillSubsurfaceColor,
                    initialRadius = 3.5f,
                    currentRadius = 3.5f,
                    life = 0.45f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun spawnCoinSparkles(x: Float, y: Float) {
        for (i in 0 until 8) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 40f + Random.nextFloat() * 70f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    color = Color(0xFFFFEB3B),
                    initialRadius = 4.0f,
                    currentRadius = 4.0f,
                    life = 0.6f,
                    maxLife = 0.6f
                )
            )
        }
    }

    private fun spawnCrashExplosion(x: Float, y: Float) {
        for (i in 0 until 20) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 60f + Random.nextFloat() * 120f
            val col = if (Random.nextBoolean()) Color(0xFFFF5722) else Color(0xFFFFC107)
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    color = col,
                    initialRadius = 6.0f,
                    currentRadius = 6.0f,
                    life = 0.8f,
                    maxLife = 0.8f
                )
            )
        }
    }

    private fun spawnObstacleDebris(x: Float, y: Float, type: ObstacleType) {
        val col = if (type == ObstacleType.WOOD_CRATE) Color(0xFF8D6E63) else Color(0xFF455A64)
        for (i in 0 until 10) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 50f + Random.nextFloat() * 90f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    color = col,
                    initialRadius = 4.5f,
                    currentRadius = 4.5f,
                    life = 0.7f,
                    maxLife = 0.7f
                )
            )
        }
    }
}
