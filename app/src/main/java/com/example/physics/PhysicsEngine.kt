package com.example.physics

import androidx.compose.ui.graphics.Color
import com.example.model.CoinItem
import com.example.model.FloatingNotification
import com.example.model.FuelCanItem
import com.example.model.GameState
import com.example.model.ObstacleItem
import com.example.model.ObstacleType
import com.example.model.Particle
import com.example.model.StageType
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
    val stage: StageType
) {
    // Ground baseline elevation
    val baseGroundY = 480f

    // Completely flat and stable starting runway (first 55+ meters = 55 * 26f = 1430px)
    val flatSpawnRunwayLength = 1500f

    // One-wheeler or two-wheeler configuration
    val isOneWheeler = vehicleType.isOneWheeler
    val halfBase = if (isOneWheeler) 0f else vehicleType.wheelBase * 0.5f

    // Spawn parameters: Vehicle resting stably at equilibrium on flat ground
    var chassisX: Float = 180f
    var chassisAngle: Float = 0f
    var chassisVx: Float = 0f
    var chassisVy: Float = 0f
    var chassisAngularVel: Float = 0f

    // Wheel parameters
    var rearWheelX: Float = chassisX - halfBase
    var rearWheelY: Float = baseGroundY - vehicleType.wheelRadius
    var rearWheelVx: Float = 0f
    var rearWheelVy: Float = 0f
    var rearWheelSpin: Float = 0f
    var rearWheelOnGround: Boolean = true

    var frontWheelX: Float = chassisX + halfBase
    var frontWheelY: Float = baseGroundY - vehicleType.wheelRadius
    var frontWheelVx: Float = 0f
    var frontWheelVy: Float = 0f
    var frontWheelSpin: Float = 0f
    var frontWheelOnGround: Boolean = true

    // Center of mass rest height directly above wheels
    var chassisY: Float = baseGroundY - vehicleType.wheelRadius - vehicleType.suspensionLength

    // Spawn stability & Grace period (prevents any instant start flipping or false crash)
    var spawnGraceTimer: Float = 3.0f

    // Gauges / Engine state
    var rpm: Float = 950f
    var currentSpeedKmh: Float = 0f
    var boostValue: Float = 0f

    // Fuel State
    val maxFuel: Float = 100f + (upgrades.fuelLevel - 1) * 25f
    var currentFuel: Float = maxFuel
    val fuelBurnRate: Float = (0.019f / (1f + (upgrades.fuelLevel - 1) * 0.16f))
    var outOfFuelTimer: Float = 0f

    // Distance & Score in Endless Mode
    var distanceMeters: Int = 0
    var coinsCollectedCount: Int = 0
    var coinsCollectedScore: Int = 0
    var totalFuelCollected: Int = 0

    // Airtime & Stunt detection
    var isAirborne: Boolean = false
    var airtimeSeconds: Float = 0f
    var airborneRotationAccumulator: Float = 0f

    // Endless Procedural Generation Frontier
    private var generatedFrontierX: Float = 0f
    private var lastFuelCanSpawnX: Float = 0f
    private var nextCoinId: Long = 1L
    private var nextFuelId: Long = 1L
    private var nextObstacleId: Long = 1L
    private val proceduralRng = Random(stage.ordinal * 99991L + 42L)

    // World Collections
    val coins = mutableListOf<CoinItem>()
    val fuelCans = mutableListOf<FuelCanItem>()
    val obstacles = mutableListOf<ObstacleItem>()
    val bridges = mutableListOf<Pair<Float, Float>>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingNotification>()

    // Performance multipliers based on vehicle & upgrades
    private val enginePower: Float =
        vehicleType.baseEngineTorque * (1.0f + (upgrades.engineLevel - 1) * 0.20f)
    private val tireGrip: Float =
        vehicleType.baseTireGrip * (1.0f + (upgrades.tiresLevel - 1) * 0.16f) * stage.frictionMultiplier
    private val suspensionStiffness: Float =
        (220f + (upgrades.suspensionLevel - 1) * 28f)
    private val suspensionDamping: Float =
        (24f + (upgrades.suspensionLevel - 1) * 3f)

    init {
        // Pre-generate procedural features for the first 3500px ahead
        expandWorldFrontier(3500f)
    }

    /**
     * Procedural terrain elevation.
     * Guaranteed completely FLAT and STABLE for the first 50+ meters (x <= flatSpawnRunwayLength).
     */
    fun getTerrainElevation(x: Float): Float {
        if (x <= flatSpawnRunwayLength) {
            return baseGroundY
        }

        // Check if inside a suspension bridge chasm
        for (b in bridges) {
            if (x in b.first..b.second) {
                val span = b.second - b.first
                val t = (x - b.first) / span
                val y1 = getRawTerrainElevation(b.first)
                val y2 = getRawTerrainElevation(b.second)
                val sag = sin(t * PI.toFloat()) * 26f
                return (y1 * (1f - t) + y2 * t) + sag
            }
        }

        return getRawTerrainElevation(x)
    }

    private fun getRawTerrainElevation(x: Float): Float {
        val distPastRunway = (x - flatSpawnRunwayLength)
        // Smooth ease-in so hills emerge gently after flat runway
        val blend = min(1f, distPastRunway / 400f)

        // Endless dynamic difficulty curve
        val endlessDifficulty = 1.0f + min(0.65f, (x / 30000f))

        val w1 = sin(x * 0.0028f) * (80f * endlessDifficulty)
        val w2 = sin(x * 0.0075f) * (38f * endlessDifficulty)
        val w3 = sin(x * 0.017f) * (16f * endlessDifficulty)
        val w4 = sin(x * 0.0006f) * (120f * endlessDifficulty)

        return baseGroundY + (w1 + w2 + w3 + w4) * blend
    }

    fun getTerrainSlope(x: Float): Float {
        val dx = 3.0f
        val y1 = getTerrainElevation(x - dx)
        val y2 = getTerrainElevation(x + dx)
        return atan2(y2 - y1, dx * 2f)
    }

    /**
     * Expands the endless procedural world ahead of the car.
     */
    private fun expandWorldFrontier(targetAheadX: Float) {
        while (generatedFrontierX < targetAheadX) {
            val curX = generatedFrontierX

            // Gaps and Bridges after 70m
            if (curX > 1900f && proceduralRng.nextFloat() < 0.08f) {
                val span = 180f + proceduralRng.nextFloat() * 120f
                bridges.add(Pair(curX, curX + span))
                generatedFrontierX += span + 100f
                continue
            }

            // Fuel Jerrycans every ~280 to 360 meters (7200 to 9360 px)
            if ((curX - lastFuelCanSpawnX >= 7600f || lastFuelCanSpawnX == 0f) && curX > 800f) {
                val fy = getTerrainElevation(curX) - 26f
                fuelCans.add(FuelCanItem(nextFuelId++, curX, fy))
                lastFuelCanSpawnX = curX
                generatedFrontierX += 130f
                continue
            }

            // Scatter coins along path
            val groundY = getTerrainElevation(curX)
            val clusterType = proceduralRng.nextInt(6)

            when (clusterType) {
                0, 1 -> {
                    // Regular line of coins (5s and 10s)
                    val coinVal = if (proceduralRng.nextBoolean()) 5 else 10
                    for (i in 0 until 4) {
                        val cx = curX + i * 36f
                        val cy = getTerrainElevation(cx) - 26f
                        coins.add(CoinItem(nextCoinId++, cx, cy, coinVal))
                    }
                    generatedFrontierX += 4 * 36f + 50f
                }
                2 -> {
                    // High-flying parabolic jump arc (10s and 50s)
                    val coinVal = if (proceduralRng.nextBoolean()) 10 else 50
                    for (i in 0 until 5) {
                        val cx = curX + i * 40f
                        val gy = getTerrainElevation(cx)
                        val arcHeight = 48f * sin(i * PI.toFloat() / 4f)
                        coins.add(CoinItem(nextCoinId++, cx, gy - 34f - arcHeight, coinVal))
                    }
                    generatedFrontierX += 5 * 40f + 60f
                }
                3 -> {
                    // Golden 100 on hill crest
                    coins.add(CoinItem(nextCoinId++, curX, groundY - 32f, 100))
                    coins.add(CoinItem(nextCoinId++, curX + 42f, getTerrainElevation(curX + 42f) - 34f, 100))
                    generatedFrontierX += 110f
                }
                4 -> {
                    // RARE 500 Coin Diamond (every few hundred meters)
                    if (curX > 600f && proceduralRng.nextFloat() < 0.45f) {
                        coins.add(CoinItem(nextCoinId++, curX, groundY - 46f, 500))
                    }
                    generatedFrontierX += 90f
                }
                else -> {
                    // Obstacles (Crates/Barrels)
                    if (curX > 1600f && proceduralRng.nextFloat() < 0.35f) {
                        val type = if (proceduralRng.nextBoolean()) ObstacleType.WOOD_CRATE else ObstacleType.OIL_BARREL
                        val size = if (type == ObstacleType.WOOD_CRATE) 28f else 24f
                        obstacles.add(
                            ObstacleItem(
                                id = nextObstacleId++,
                                x = curX,
                                y = groundY - size * 0.5f,
                                width = size,
                                height = size,
                                type = type
                            )
                        )
                    }
                    generatedFrontierX += 120f
                }
            }
        }

        // Cleanup items far behind to keep endless performance silky smooth
        val pruneX = chassisX - 1400f
        coins.removeAll { it.collected || it.x < pruneX }
        fuelCans.removeAll { it.collected || it.x < pruneX }
        obstacles.removeAll { it.x < pruneX }
        bridges.removeAll { it.second < pruneX }
    }

    /**
     * Physics update tick (~60fps).
     */
    fun update(
        dt: Float,
        isGasPressed: Boolean,
        isBrakePressed: Boolean,
        onSoundEvent: (String) -> Unit
    ): GameState {
        val grav = stage.gravity * 65f

        if (spawnGraceTimer > 0f) {
            spawnGraceTimer = max(0f, spawnGraceTimer - dt)
        }

        // Expand endless procedural world ahead
        expandWorldFrontier(chassisX + 2800f)

        // Fuel consumption
        if (currentFuel > 0f) {
            val burn = fuelBurnRate * dt * (if (isGasPressed) 5.5f else 1.0f)
            currentFuel = max(0f, currentFuel - burn)
        }
        val hasFuel = currentFuel > 0.001f

        // RPM calculation
        val targetRpm = when {
            isGasPressed && hasFuel -> 7200f + Random.nextFloat() * 300f
            isBrakePressed -> 3200f
            else -> 950f + Random.nextFloat() * 60f
        }
        rpm += (targetRpm - rpm) * min(1f, dt * 10f)

        // Boost indicator
        if (isGasPressed && hasFuel) {
            boostValue = min(100f, boostValue + dt * 32f)
        } else {
            boostValue = max(0f, boostValue - dt * 40f)
        }

        // Wheel geometry anchors from chassis center
        val cosA = cos(chassisAngle)
        val sinA = sin(chassisAngle)
        val suspLen = vehicleType.suspensionLength

        // Wheel anchors attached to chassis
        val rearAnchorX = chassisX - cosA * halfBase
        val rearAnchorY = chassisY - sinA * halfBase
        val frontAnchorX = chassisX + cosA * halfBase
        val frontAnchorY = chassisY + sinA * halfBase

        // 1. Rear Wheel Ground Contact
        val rearGroundY = getTerrainElevation(rearWheelX)
        val rearPenetration = (rearWheelY + vehicleType.wheelRadius) - rearGroundY
        rearWheelOnGround = rearPenetration >= 0f

        if (rearWheelOnGround) {
            rearWheelY = rearGroundY - vehicleType.wheelRadius
            if (rearWheelVy > 0f) rearWheelVy = 0f
        } else {
            rearWheelVy += grav * dt
        }

        // 2. Front Wheel Ground Contact (if not One-Wheeler)
        if (!isOneWheeler) {
            val frontGroundY = getTerrainElevation(frontWheelX)
            val frontPenetration = (frontWheelY + vehicleType.wheelRadius) - frontGroundY
            frontWheelOnGround = frontPenetration >= 0f

            if (frontWheelOnGround) {
                frontWheelY = frontGroundY - vehicleType.wheelRadius
                if (frontWheelVy > 0f) frontWheelVy = 0f
            } else {
                frontWheelVy += grav * dt
            }
        } else {
            frontWheelOnGround = false
            frontWheelX = chassisX
            frontWheelY = rearWheelY
        }

        val onGround = rearWheelOnGround || frontWheelOnGround

        // 3. Driving, Braking & Engine Forces
        if (onGround) {
            val slopeAngle = getTerrainSlope(chassisX)

            if (hasFuel && isGasPressed) {
                // Forward drive acceleration along ground slope
                val driveAcc = 420f * enginePower * tireGrip
                chassisVx += driveAcc * dt * cos(slopeAngle)
                chassisVy += driveAcc * dt * sin(slopeAngle)

                rearWheelSpin += (chassisVx / vehicleType.wheelRadius) * dt * 2.5f
                spawnTireDirt(rearWheelX, rearWheelY, -cos(slopeAngle), -sin(slopeAngle))
            } else if (isBrakePressed) {
                // Braking friction
                chassisVx *= (1f - dt * 5.5f)
                if (chassisVx < 10f) {
                    // Reverse torque when stopped
                    val revAcc = 180f * enginePower * tireGrip
                    chassisVx -= revAcc * dt * cos(slopeAngle)
                    chassisVy -= revAcc * dt * sin(slopeAngle)
                }
            } else {
                // Natural rolling friction
                chassisVx *= (1f - dt * 0.35f)
            }

            // Normal gravity component on slopes
            chassisVy += grav * dt * 0.35f
        } else {
            // Free fall in air
            chassisVy += grav * dt
        }

        // 4. Suspension spring-damper supporting the chassis
        // Target vertical rest offset between chassis and wheels is suspLen
        val avgWheelY = if (isOneWheeler) rearWheelY else (rearWheelY + frontWheelY) * 0.5f
        val targetChassisY = avgWheelY - suspLen

        if (onGround) {
            val suspDeltaY = (targetChassisY - chassisY)
            val suspForceY = suspDeltaY * suspensionStiffness - chassisVy * suspensionDamping
            chassisVy += (suspForceY / vehicleType.baseMass) * dt
        }

        // 5. Angular Stabilization & Air Control
        if (onGround) {
            // Naturally righting torque: aligns chassis smoothly with the ground slope / wheel base angle
            val targetAngle = if (!isOneWheeler && rearWheelOnGround && frontWheelOnGround) {
                atan2(frontWheelY - rearWheelY, frontWheelX - rearWheelX)
            } else {
                getTerrainSlope(chassisX)
            }

            val angleDiff = chassisAngle - targetAngle
            // Natural spring righting torque: keeps car upright naturally at spawn and on road
            chassisAngularVel -= angleDiff * 14.0f * dt
            // Strong angular damping on ground to prevent flipping
            chassisAngularVel *= (1f - dt * 6.5f)

            // Very gentle squat under hard gas acceleration (authentic hill climb feel)
            if (isGasPressed && hasFuel && !isOneWheeler) {
                if (chassisAngle > -0.12f) {
                    chassisAngularVel -= 0.15f * dt * enginePower
                }
            }

            // Reset airborne timers when safely grounded
            if (isAirborne && airtimeSeconds > 0.8f) {
                val score = (airtimeSeconds * 100f).toInt() * (if (isOneWheeler) 2 else 1)
                coinsCollectedScore += score
                floatingTexts.add(
                    FloatingNotification(
                        System.currentTimeMillis(),
                        "AIRTIME ${(airtimeSeconds * 100).toInt()} +$score",
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
        } else {
            // IN THE AIR: Pitch controls for stunts!
            airtimeSeconds += dt
            if (airtimeSeconds > 0.35f) {
                isAirborne = true
            }

            // Air pitch controls: Gas pitches back (backflip), Brake pitches forward (frontflip)
            if (isGasPressed) {
                chassisAngularVel -= 4.2f * dt
            }
            if (isBrakePressed) {
                chassisAngularVel += 4.2f * dt
            }

            airborneRotationAccumulator += chassisAngularVel * dt

            // Flips detection
            if (airborneRotationAccumulator <= -(2f * PI.toFloat() - 0.25f)) {
                airborneRotationAccumulator += (2f * PI.toFloat())
                val bonus = if (isOneWheeler) 450 else 250
                coinsCollectedScore += bonus
                floatingTexts.add(
                    FloatingNotification(
                        System.currentTimeMillis(),
                        "BACKFLIP! +$bonus",
                        Color(0xFFFFD700),
                        chassisX,
                        chassisY - 60f,
                        1.5f
                    )
                )
                onSoundEvent("flip")
            } else if (airborneRotationAccumulator >= (2f * PI.toFloat() - 0.25f)) {
                airborneRotationAccumulator -= (2f * PI.toFloat())
                val bonus = if (isOneWheeler) 550 else 300
                coinsCollectedScore += bonus
                floatingTexts.add(
                    FloatingNotification(
                        System.currentTimeMillis(),
                        "FRONTFLIP! +$bonus",
                        Color(0xFFFFD700),
                        chassisX,
                        chassisY - 60f,
                        1.5f
                    )
                )
                onSoundEvent("flip")
            }

            // Gentle air drag
            chassisAngularVel *= (1f - dt * 0.8f)
        }

        // Clamp angular velocity
        chassisAngularVel = chassisAngularVel.coerceIn(-8.5f, 8.5f)

        // Integrate positions
        chassisX += chassisVx * dt
        chassisY += chassisVy * dt
        chassisAngle += chassisAngularVel * dt

        // Keep wheels synchronized with chassis position
        val curCosA = cos(chassisAngle)
        val curSinA = sin(chassisAngle)

        val targetRearX = chassisX - curCosA * halfBase
        val targetFrontX = chassisX + curCosA * halfBase

        rearWheelX = targetRearX
        if (!isOneWheeler) {
            frontWheelX = targetFrontX
        } else {
            frontWheelX = chassisX
        }

        rearWheelVx = chassisVx
        frontWheelVx = chassisVx

        // Update speed and distance from spawn point (180f)
        val speedMagnitude = sqrt(chassisVx * chassisVx + chassisVy * chassisVy)
        currentSpeedKmh = speedMagnitude * 0.16f
        distanceMeters = max(0, ((chassisX - 180f) / 26f).toInt())

        // 6. Driver Crash Detection
        // Completely IMMUNE during spawn grace timer and on the first 35m runway
        if (spawnGraceTimer <= 0f && distanceMeters >= 35) {
            val normalizedAngle = ((chassisAngle % (2f * PI.toFloat())) + 2f * PI.toFloat()) % (2f * PI.toFloat())
            // Only upside-down inverted poses (between ~125 deg and ~235 deg) can cause a flip crash
            val isUpsideDown = normalizedAngle in 2.2f..4.1f

            if (isUpsideDown) {
                val driverHeadOffsetX = sin(chassisAngle) * 24f
                val driverHeadOffsetY = -cos(chassisAngle) * 24f
                val driverHeadX = chassisX + driverHeadOffsetX
                val driverHeadY = chassisY + driverHeadOffsetY
                val groundAtHead = getTerrainElevation(driverHeadX)

                // Head actually firmly hitting the ground while upside down
                if (driverHeadY >= groundAtHead - 2f) {
                    onSoundEvent("crash")
                    spawnCrashExplosion(chassisX, chassisY)
                    return GameState.GAME_OVER_FLIPPED
                }

                // Or completely stuck upside down on ground
                if (onGround && speedMagnitude < 20f) {
                    onSoundEvent("crash")
                    return GameState.GAME_OVER_FLIPPED
                }
            }
        }

        // 7. Collectibles & Obstacles
        checkCoinPickups(onSoundEvent)
        checkFuelPickups(onSoundEvent)
        updateObstacles(dt, onSoundEvent)
        updateParticles(dt)
        updateFloatingTexts(dt)

        // 8. Out of Fuel Check (Endless Mode)
        if (currentFuel <= 0.001f) {
            if (speedMagnitude < 3.0f) {
                outOfFuelTimer += dt
                if (outOfFuelTimer > 1.8f) {
                    onSoundEvent("fuel_empty")
                    return GameState.GAME_OVER_FUEL
                }
            } else {
                outOfFuelTimer = 0f
            }
        } else {
            outOfFuelTimer = 0f
        }

        return GameState.PLAYING
    }

    private fun checkCoinPickups(onSoundEvent: (String) -> Unit) {
        val pickupRadiusSq = 48f * 48f
        for (coin in coins) {
            if (!coin.collected) {
                val dx = chassisX - coin.x
                val dy = chassisY - coin.y
                if (dx * dx + dy * dy <= pickupRadiusSq) {
                    coin.collected = true
                    coinsCollectedCount++
                    coinsCollectedScore += coin.value
                    spawnCoinSparkles(coin.x, coin.y, coin.value)
                    floatingTexts.add(
                        FloatingNotification(
                            System.currentTimeMillis() + coin.id,
                            "+${coin.value}",
                            if (coin.value >= 500) Color(0xFFFF4081) else Color(0xFFFFD700),
                            coin.x,
                            coin.y - 18f,
                            0.95f
                        )
                    )
                    onSoundEvent("coin")
                }
            }
        }
    }

    private fun checkFuelPickups(onSoundEvent: (String) -> Unit) {
        val pickupRadiusSq = 52f * 52f
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
                            can.y - 28f,
                            1.4f
                        )
                    )
                    spawnFuelGlow(can.x, can.y)
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
                val distSq = dx * dx + dy * dy
                val hitDist = (vehicleType.wheelRadius + obs.width * 0.5f)

                if (distSq < hitDist * hitDist) {
                    // Smash obstacle!
                    obs.vx = chassisVx * 0.85f + Random.nextFloat() * 60f
                    obs.vy = -120f - Random.nextFloat() * 100f
                    obs.vRot = (Random.nextFloat() - 0.5f) * 16f
                    obs.isDestroyed = true
                    chassisVx *= 0.85f

                    // Spawn splinter particles
                    for (i in 0 until 6) {
                        particles.add(
                            Particle(
                                x = obs.x,
                                y = obs.y,
                                vx = (Random.nextFloat() - 0.5f) * 140f,
                                vy = -Random.nextFloat() * 120f,
                                color = if (obs.type == ObstacleType.WOOD_CRATE) Color(0xFF8D6E63) else Color(0xFF455A64),
                                initialRadius = 3.5f,
                                currentRadius = 3.5f,
                                life = 0.6f,
                                maxLife = 0.6f
                            )
                        )
                    }
                    onSoundEvent("obstacle_hit")
                }
            } else {
                obs.x += obs.vx * dt
                obs.y += obs.vy * dt
                obs.vy += 450f * dt
                obs.angle += obs.vRot * dt
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.currentRadius = p.initialRadius * (p.life / p.maxLife)
            }
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val it = floatingTexts.iterator()
        while (it.hasNext()) {
            val fn = it.next()
            fn.life -= dt
            if (fn.life <= 0f) {
                it.remove()
            } else {
                fn.y -= 28f * dt
            }
        }
    }

    private fun spawnTireDirt(x: Float, y: Float, dirX: Float, dirY: Float) {
        if (particles.size < 50 && Random.nextFloat() < 0.45f) {
            particles.add(
                Particle(
                    x = x + (Random.nextFloat() - 0.5f) * 6f,
                    y = y + 4f,
                    vx = dirX * (40f + Random.nextFloat() * 40f),
                    vy = dirY * 30f - Random.nextFloat() * 40f,
                    color = stage.hillSubsurfaceColor.copy(alpha = 0.7f),
                    initialRadius = 3.5f,
                    currentRadius = 3.5f,
                    life = 0.45f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun spawnCoinSparkles(x: Float, y: Float, value: Int) {
        val count = if (value >= 100) 10 else 6
        for (i in 0 until count) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 50f + Random.nextFloat() * 70f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    color = if (value >= 500) Color(0xFFFF4081) else Color(0xFFFFD700),
                    initialRadius = 4f,
                    currentRadius = 4f,
                    life = 0.5f,
                    maxLife = 0.5f
                )
            )
        }
    }

    private fun spawnFuelGlow(x: Float, y: Float) {
        for (i in 0 until 8) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 40f + Random.nextFloat() * 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    color = Color(0xFFFF3D00),
                    initialRadius = 4.5f,
                    currentRadius = 4.5f,
                    life = 0.6f,
                    maxLife = 0.6f
                )
            )
        }
    }

    private fun spawnCrashExplosion(x: Float, y: Float) {
        for (i in 0 until 16) {
            val ang = Random.nextFloat() * 2f * PI.toFloat()
            val spd = 60f + Random.nextFloat() * 140f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    color = if (Random.nextBoolean()) Color(0xFFFF1744) else Color(0xFFFFD600),
                    initialRadius = 5f,
                    currentRadius = 5f,
                    life = 0.7f,
                    maxLife = 0.7f
                )
            )
        }
    }
}
