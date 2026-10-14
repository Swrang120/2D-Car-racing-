package com.example.physics

import androidx.compose.ui.graphics.Color
import com.example.model.CoinItem
import com.example.model.EnvironmentType
import com.example.model.FinishGate
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
    constructor(
        vehicleType: VehicleType,
        upgrades: VehicleUpgradeState,
        stage: EnvironmentType
    ) : this(vehicleType, upgrades, LevelInfo(1, stage, 1000))

    val stage: EnvironmentType get() = levelInfo.environment

    // Ground baseline elevation
    val baseGroundY = 480f

    // Completely flat and stable starting runway (first 55+ meters = 1500px)
    val flatSpawnRunwayLength = 1500f

    // One-wheeler or two-wheeler configuration
    val isOneWheeler = vehicleType.isOneWheeler
    val halfBase = if (isOneWheeler) 0f else vehicleType.wheelBase * 0.5f

    // Wheel dimensions
    val rearWheelRadius: Float = vehicleType.wheelRadius
    val frontWheelRadius: Float = vehicleType.frontWheelRadius

    // Spawn parameters: Vehicle resting stably at equilibrium on flat ground
    var chassisX: Float = 180f
    var chassisAngle: Float = 0f
    var chassisVx: Float = 0f
    var chassisVy: Float = 0f
    var chassisAngularVel: Float = 0f

    // Center of mass rest height directly above wheels
    var chassisY: Float = baseGroundY - rearWheelRadius - vehicleType.suspensionLength

    // Wheel parameters
    var rearWheelX: Float = chassisX - halfBase
    var rearWheelY: Float = baseGroundY - rearWheelRadius
    var rearWheelVx: Float = 0f
    var rearWheelVy: Float = 0f
    var rearWheelSpin: Float = 0f
    var rearWheelOnGround: Boolean = true

    var frontWheelX: Float = chassisX + halfBase
    var frontWheelY: Float = baseGroundY - frontWheelRadius
    var frontWheelVx: Float = 0f
    var frontWheelVy: Float = 0f
    var frontWheelSpin: Float = 0f
    var frontWheelOnGround: Boolean = true

    // Spawn stability & Grace period (prevents any instant start flipping or false crash)
    var spawnGraceTimer: Float = 3.0f

    // Gauges / Engine state
    var rpm: Float = 950f
    var currentSpeedKmh: Float = 0f
    var boostValue: Float = 0f

    // Fuel State
    val maxFuel: Float = 100f + (upgrades.fuelLevel - 1) * 25f
    var currentFuel: Float = maxFuel
    val fuelBurnRate: Float = (0.018f / (1f + (upgrades.fuelLevel - 1) * 0.16f))
    var outOfFuelTimer: Float = 0f

    // Distance & Score in Endless Mode
    var distanceMeters: Int = 0
    var coinsCollectedCount: Int = 0
    var coinsCollectedScore: Int = 0
    var totalFuelCollected: Int = 0
    var perfectRunEligible: Boolean = true

    // Current Level Progress along endless road
    var currentLevelNumber: Int = levelInfo.levelNumber
    val finishGates = mutableListOf<FinishGate>()

    val trackEndWorldX: Float
        get() = finishGates.firstOrNull { !it.isCrossed }?.worldX ?: (chassisX + 2500f)

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
        (260f + (upgrades.suspensionLevel - 1) * 32f)
    private val suspensionDamping: Float =
        (28f + (upgrades.suspensionLevel - 1) * 4f)

    init {
        // Generate Level Finish Gates:
        // Level 1 at 100m, Level 2 at 250m (+150m), Level 3 at 450m (+200m), Level 4 at 700m (+250m), Level 5 at 1000m (1km)...
        var gateDist = 100
        var distStep = 150
        for (lvl in 1..100) {
            val gateWorldX = 180f + gateDist * 26f
            finishGates.add(FinishGate(levelNumber = lvl, distanceMeters = gateDist, worldX = gateWorldX))
            gateDist += distStep
            distStep = min(350, distStep + 25)
        }

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
                val sag = sin(t * PI.toFloat()) * 32f
                val baseline = baseGroundY + 15f
                return baseline + sag
            }
        }

        // Smooth wave modulation for endless hills
        val relX = (x - flatSpawnRunwayLength) * 0.0032f
        val w1 = sin(relX * 1.0f) * 65f
        val w2 = sin(relX * 2.3f + 1.2f) * 35f
        val w3 = cos(relX * 0.42f) * 45f
        val w4 = sin(relX * 4.8f + 0.5f) * 12f

        val hillSteepness = when (stage) {
            EnvironmentType.COUNTRYSIDE -> 0.85f
            EnvironmentType.DESERT -> 1.05f
            EnvironmentType.ARCTIC -> 0.95f
            EnvironmentType.MOON -> 1.25f
            EnvironmentType.CAVE -> 1.15f
            EnvironmentType.HIGHWAY -> 0.55f
        }

        return baseGroundY - (w1 + w2 + w3 + w4) * hillSteepness
    }

    /**
     * Terrain Slope Angle in Radians.
     */
    fun getTerrainSlope(x: Float): Float {
        val delta = 4.0f
        val y1 = getTerrainElevation(x - delta)
        val y2 = getTerrainElevation(x + delta)
        return atan2(y2 - y1, delta * 2.0f)
    }

    /**
     * Infinite procedural world generator that spawns hills, coins, fuel cans, bridges, and obstacles.
     */
    private fun expandWorldFrontier(targetX: Float) {
        while (generatedFrontierX < targetX) {
            val curX = generatedFrontierX
            val groundY = getTerrainElevation(curX)

            // Spawn Bridge Chasm occasionally
            if (curX > 1800f && proceduralRng.nextFloat() < 0.06f && bridges.none { curX in it.first..it.second }) {
                val bStart = curX + 120f
                val bEnd = bStart + 350f
                bridges.add(Pair(bStart, bEnd))
                generatedFrontierX = bEnd + 150f
                continue
            }

            // Spawn Fuel Can every 800 - 1400px (approx 30 - 55 meters)
            if (curX - lastFuelCanSpawnX > 900f && proceduralRng.nextFloat() < 0.45f) {
                val fuelX = curX + 60f
                val fuelY = getTerrainElevation(fuelX) - 28f
                fuelCans.add(FuelCanItem(nextFuelId++, fuelX, fuelY))
                lastFuelCanSpawnX = curX
            }

            // Spawn Coins
            when (proceduralRng.nextInt(5)) {
                0, 1 -> {
                    // Arc of 4-6 coins
                    val count = proceduralRng.nextInt(3, 6)
                    val baseVal = when {
                        curX > 12000f -> 100
                        curX > 6000f -> 50
                        curX > 2500f -> 10
                        else -> 5
                    }
                    for (i in 0 until count) {
                        val cx = curX + i * 42f
                        val arcT = i.toFloat() / count
                        val arcY = sin(arcT * PI.toFloat()) * 48f
                        val cy = getTerrainElevation(cx) - 26f - arcY
                        coins.add(CoinItem(nextCoinId++, cx, cy, baseVal))
                    }
                    generatedFrontierX += count * 45f + 80f
                }
                2 -> {
                    // Super jackpot coin (500 value)
                    val cx = curX + 50f
                    val cy = getTerrainElevation(cx) - 34f
                    coins.add(CoinItem(nextCoinId++, cx, cy, 500))
                    generatedFrontierX += 140f
                }
                3 -> {
                    // Obstacles (Barrels or Crates) after 80 meters
                    if (curX > 2200f && proceduralRng.nextFloat() < 0.35f) {
                        val isBarrel = proceduralRng.nextBoolean()
                        obstacles.add(
                            ObstacleItem(
                                id = nextObstacleId++,
                                x = curX + 40f,
                                y = groundY - 18f,
                                width = if (isBarrel) 20f else 26f,
                                height = if (isBarrel) 32f else 26f,
                                type = if (isBarrel) ObstacleType.OIL_BARREL else ObstacleType.WOOD_CRATE
                            )
                        )
                    }
                    generatedFrontierX += 160f
                }
                else -> {
                    generatedFrontierX += 120f
                }
            }
        }

        // Memory cleanup: prune past items that are far behind the camera
        val pruneThresholdX = chassisX - 1800f
        coins.removeAll { it.collected || it.x < pruneThresholdX }
        fuelCans.removeAll { it.collected || it.x < pruneThresholdX }
        obstacles.removeAll { it.isDestroyed || it.x < pruneThresholdX }
    }

    /**
     * Primary 2D Physics Update Step (Fixed 60Hz dt).
     * Guaranteed: Wheels and chassis CANNOT sink into ground or fall below terrain.
     */
    fun update(
        dt: Float,
        isGasPressed: Boolean,
        isBrakePressed: Boolean,
        onSoundEvent: (String) -> Unit
    ): GameState {
        val gravityAcc = stage.gravity * 65.0f

        if (spawnGraceTimer > 0f) {
            spawnGraceTimer = max(0f, spawnGraceTimer - dt)
        }

        // 1. Procedural Expansion ahead of car
        expandWorldFrontier(chassisX + 3500f)

        // 2. Fuel Consumption
        if (currentFuel > 0f) {
            val burnRate = fuelBurnRate * (if (isGasPressed) 4.8f else 1.0f)
            currentFuel = max(0f, currentFuel - burnRate * dt)
        }
        val hasFuel = currentFuel > 0.001f

        // RPM simulation for cockpit sound & gauge
        val targetRpm = if (isGasPressed && hasFuel) 7200f else if (isBrakePressed) 3200f else 950f
        rpm += (targetRpm - rpm) * min(1f, dt * 10f)

        // 3. Wheel & Suspension Constraints
        val cosA = cos(chassisAngle)
        val sinA = sin(chassisAngle)
        val suspLen = vehicleType.suspensionLength

        // Rear wheel anchor on chassis
        val rearAnchorX = chassisX - cosA * halfBase
        val rearAnchorY = chassisY - sinA * halfBase
        rearWheelX = rearAnchorX

        // Rest position of rear wheel along suspension normal
        val restRearY = rearAnchorY + cosA * suspLen
        val rearGroundY = getTerrainElevation(rearWheelX)
        val rearLimitY = rearGroundY - rearWheelRadius

        val rearCompression: Float
        if (restRearY >= rearLimitY) {
            rearWheelY = rearLimitY
            rearWheelOnGround = true
            rearCompression = restRearY - rearLimitY
        } else {
            rearWheelY = restRearY
            rearWheelOnGround = false
            rearCompression = 0f
        }

        // Front wheel anchor and rest position
        val frontCompression: Float
        if (!isOneWheeler) {
            val frontAnchorX = chassisX + cosA * halfBase
            val frontAnchorY = chassisY + sinA * halfBase
            frontWheelX = frontAnchorX

            val restFrontY = frontAnchorY + cosA * suspLen
            val frontGroundY = getTerrainElevation(frontWheelX)
            val frontLimitY = frontGroundY - frontWheelRadius

            if (restFrontY >= frontLimitY) {
                frontWheelY = frontLimitY
                frontWheelOnGround = true
                frontCompression = restFrontY - frontLimitY
            } else {
                frontWheelY = restFrontY
                frontWheelOnGround = false
                frontCompression = 0f
            }
        } else {
            frontWheelX = chassisX
            frontWheelY = rearWheelY
            frontWheelOnGround = rearWheelOnGround
            frontCompression = rearCompression
        }

        val onGround = rearWheelOnGround || frontWheelOnGround

        // 4. Suspension spring forces pushing UP on chassis
        val rearSpringForce = rearCompression * suspensionStiffness - max(0f, chassisVy) * suspensionDamping
        val frontSpringForce = frontCompression * suspensionStiffness - max(0f, chassisVy) * suspensionDamping
        val totalSuspensionUpForce = max(0f, rearSpringForce) + max(0f, frontSpringForce)

        // Apply Gravity
        chassisVy += gravityAcc * dt

        // 5. Ground Traction & Pitch Stabilization
        if (onGround) {
            chassisVy -= (totalSuspensionUpForce / vehicleType.baseMass) * dt

            // Ground Pitch Alignment
            val targetAngle = if (!isOneWheeler && rearWheelOnGround && frontWheelOnGround) {
                atan2(frontWheelY - rearWheelY, frontWheelX - rearWheelX)
            } else {
                getTerrainSlope(chassisX)
            }
            val pitchDiff = chassisAngle - targetAngle
            chassisAngularVel -= pitchDiff * 16.0f * dt
            chassisAngularVel *= (1.0f - dt * 6.0f)

            // Forward Acceleration / Braking
            val terrainSlope = getTerrainSlope(chassisX)

            if (hasFuel && isGasPressed) {
                val driveAcc = 460.0f * enginePower * tireGrip
                chassisVx += driveAcc * dt * cos(terrainSlope)
                chassisVy += driveAcc * dt * sin(terrainSlope)

                val spinDelta = (chassisVx / rearWheelRadius) * dt * 2.5f
                rearWheelSpin += spinDelta
                frontWheelSpin += spinDelta

                // Tire particles
                if (proceduralRng.nextFloat() < 0.35f) {
                    spawnTireSmoke(rearWheelX, rearWheelY + rearWheelRadius)
                }

                // Wheelie tilt on intense acceleration
                if (!isOneWheeler && chassisAngle > -0.16f) {
                    chassisAngularVel -= 0.14f * dt * enginePower
                }
            } else if (isBrakePressed) {
                if (chassisVx > 5.0f) {
                    chassisVx = max(0f, chassisVx - 620.0f * dt * tireGrip)
                } else {
                    val revAcc = 200.0f * enginePower * tireGrip
                    chassisVx -= revAcc * dt * cos(terrainSlope)
                    chassisVy -= revAcc * dt * sin(terrainSlope)
                }
            } else {
                // Natural Rolling Friction
                chassisVx *= (1.0f - dt * 0.40f)
            }
        } else {
            // Airborne Pitch Control (Flips & Stunts)
            if (isGasPressed) {
                chassisAngularVel -= 4.4f * dt // Tilt back / Backflip
            }
            if (isBrakePressed) {
                chassisAngularVel += 4.4f * dt // Tilt forward / Frontflip
            }
            chassisAngularVel *= (1.0f - dt * 0.9f)
        }

        chassisAngularVel = max(-8.5f, min(8.5f, chassisAngularVel))

        // Position Integration
        chassisX += chassisVx * dt
        chassisY += chassisVy * dt
        chassisAngle += chassisAngularVel * dt

        // 6. HARD GROUND COLLISION CLAMP (Guarantees car NEVER falls below terrain)
        val centerGroundY = getTerrainElevation(chassisX)
        val minClearance = 16f
        if (chassisY > centerGroundY - minClearance) {
            chassisY = centerGroundY - minClearance
            if (chassisVy > 0f) chassisVy = 0f
        }

        // Speed and distance
        val speedMagnitude = sqrt(chassisVx * chassisVx + chassisVy * chassisVy)
        currentSpeedKmh = speedMagnitude * 0.16f
        distanceMeters = max(0, ((chassisX - 180f) / 26f).toInt())

        // 7. Level Finish Gates Crossing (Endless Continuous Mode)
        // Check if car passes a level finish line gate
        for (gate in finishGates) {
            if (!gate.isCrossed && chassisX >= gate.worldX) {
                gate.isCrossed = true
                currentLevelNumber = gate.levelNumber + 1
                onSoundEvent("level_win")

                // Reward Level Completion Bonus
                val bonus = 1000 * gate.levelNumber
                coinsCollectedScore += bonus

                // Fully refill fuel tank
                currentFuel = maxFuel

                floatingTexts.add(
                    FloatingNotification(
                        id = nextCoinId++,
                        text = "🏆 LEVEL ${gate.levelNumber} COMPLETED! +${bonus} 🪙 FUEL REFILLED!",
                        color = Color(0xFF10B981),
                        x = chassisX,
                        y = chassisY - 55f,
                        life = 2.5f
                    )
                )

                // Spawn celebratory fireworks & confetti
                spawnFinishConfetti(gate.worldX, getTerrainElevation(gate.worldX))
            }
        }

        // 8. Stunts & Airtime
        if (!onGround) {
            if (!isAirborne) {
                isAirborne = true
                airtimeSeconds = 0f
                airborneRotationAccumulator = 0f
            }
            airtimeSeconds += dt
            airborneRotationAccumulator += chassisAngularVel * dt

            // Full Backflip detection
            if (airborneRotationAccumulator <= -2f * PI.toFloat() + 0.3f) {
                airborneRotationAccumulator = 0f
                val flipBonus = 350
                coinsCollectedScore += flipBonus
                onSoundEvent("flip")
                floatingTexts.add(
                    FloatingNotification(
                        id = nextCoinId++,
                        text = "BACKFLIP! +$flipBonus",
                        color = Color(0xFFFFD700),
                        x = chassisX,
                        y = chassisY - 45f,
                        life = 1.6f
                    )
                )
            } else if (airborneRotationAccumulator >= 2f * PI.toFloat() - 0.3f) {
                airborneRotationAccumulator = 0f
                val flipBonus = 400
                coinsCollectedScore += flipBonus
                onSoundEvent("flip")
                floatingTexts.add(
                    FloatingNotification(
                        id = nextCoinId++,
                        text = "FRONTFLIP! +$flipBonus",
                        color = Color(0xFF00E5FF),
                        x = chassisX,
                        y = chassisY - 45f,
                        life = 1.6f
                    )
                )
            }
        } else {
            if (isAirborne) {
                if (airtimeSeconds > 1.2f) {
                    val airBonus = (airtimeSeconds * 75f).toInt()
                    coinsCollectedScore += airBonus
                    floatingTexts.add(
                        FloatingNotification(
                            id = nextCoinId++,
                            text = "AIRTIME ${String.format("%.1f", airtimeSeconds)}s! +$airBonus",
                            color = Color(0xFFE040FB),
                            x = chassisX,
                            y = chassisY - 40f,
                            life = 1.5f
                        )
                    )
                }
                isAirborne = false
                airtimeSeconds = 0f
            }
        }

        // 9. Driver Crash Detection
        // Completely IMMUNE during spawn grace timer and on the first 35m runway
        if (spawnGraceTimer <= 0f && distanceMeters >= 35) {
            val normalizedAngle = ((chassisAngle % (2f * PI.toFloat())) + 2f * PI.toFloat()) % (2f * PI.toFloat())
            val isUpsideDown = normalizedAngle in 2.2f..4.1f

            if (isUpsideDown) {
                val driverHeadOffsetX = sin(chassisAngle) * 24f
                val driverHeadOffsetY = -cos(chassisAngle) * 24f
                val driverHeadX = chassisX + driverHeadOffsetX
                val driverHeadY = chassisY + driverHeadOffsetY
                val groundAtHead = getTerrainElevation(driverHeadX)

                if (driverHeadY >= groundAtHead - 2f) {
                    perfectRunEligible = false
                    onSoundEvent("crash")
                    spawnCrashExplosion(chassisX, chassisY)
                    return GameState.GAME_OVER_FLIPPED
                }

                if (onGround && speedMagnitude < 15f) {
                    perfectRunEligible = false
                    onSoundEvent("crash")
                    return GameState.GAME_OVER_FLIPPED
                }
            }
        }

        // 10. Collectibles & Obstacles
        checkCoinPickups(onSoundEvent)
        checkFuelPickups(onSoundEvent)
        updateObstacles(dt, onSoundEvent)
        updateParticles(dt)
        updateFloatingTexts(dt)

        // 11. Out of Fuel Check (Endless Mode)
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
        }

        return GameState.PLAYING
    }

    private fun checkCoinPickups(onSoundEvent: (String) -> Unit) {
        val pickupRadius = 42f
        for (coin in coins) {
            if (!coin.collected) {
                val dx = coin.x - chassisX
                val dy = coin.y - chassisY
                if (dx * dx + dy * dy < pickupRadius * pickupRadius) {
                    coin.collected = true
                    coinsCollectedCount++
                    coinsCollectedScore += coin.value
                    onSoundEvent("coin")
                    spawnCoinSparkles(coin.x, coin.y)
                    floatingTexts.add(
                        FloatingNotification(
                            id = nextCoinId++,
                            text = "+${coin.value}",
                            color = when (coin.value) {
                                500 -> Color(0xFFFFD700)
                                100 -> Color(0xFF00E5FF)
                                50 -> Color(0xFF10B981)
                                else -> Color(0xFFFFEB3B)
                            },
                            x = coin.x,
                            y = coin.y - 15f,
                            life = 1.0f
                        )
                    )
                }
            }
        }
    }

    private fun checkFuelPickups(onSoundEvent: (String) -> Unit) {
        val pickupRadius = 46f
        for (fuel in fuelCans) {
            if (!fuel.collected) {
                val dx = fuel.x - chassisX
                val dy = fuel.y - chassisY
                if (dx * dx + dy * dy < pickupRadius * pickupRadius) {
                    fuel.collected = true
                    totalFuelCollected++
                    currentFuel = maxFuel
                    onSoundEvent("fuel")
                    spawnFuelSparkles(fuel.x, fuel.y)
                    floatingTexts.add(
                        FloatingNotification(
                            id = nextCoinId++,
                            text = "FUEL REFILL!",
                            color = Color(0xFFEF4444),
                            x = fuel.x,
                            y = fuel.y - 20f,
                            life = 1.4f
                        )
                    )
                }
            }
        }
    }

    private fun updateObstacles(dt: Float, onSoundEvent: (String) -> Unit) {
        for (obs in obstacles) {
            if (obs.isDestroyed) continue

            val dx = obs.x - chassisX
            val dy = obs.y - chassisY
            val hitDist = (obs.width + obs.height) * 0.5f + 25f

            if (dx * dx + dy * dy < hitDist * hitDist) {
                obs.isDestroyed = true
                onSoundEvent("obstacle_hit")
                val impactSpeed = sqrt(chassisVx * chassisVx + chassisVy * chassisVy)
                chassisVx *= 0.85f

                val isBarrel = obs.type == ObstacleType.OIL_BARREL
                spawnObstacleDebris(obs.x, obs.y, isBarrel)

                if (impactSpeed > 180f) {
                    val bonus = if (isBarrel) 75 else 50
                    coinsCollectedScore += bonus
                    floatingTexts.add(
                        FloatingNotification(
                            id = nextCoinId++,
                            text = if (isBarrel) "BARREL SMASH! +$bonus" else "CRATE SMASH! +$bonus",
                            color = if (isBarrel) Color(0xFFFF5722) else Color(0xFF8D6E63),
                            x = obs.x,
                            y = obs.y - 25f,
                            life = 1.2f
                        )
                    )
                }
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            if (p.life <= 0f) {
                iterator.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.currentRadius = p.initialRadius * (p.life / p.maxLife)
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val iterator = floatingTexts.iterator()
        while (iterator.hasNext()) {
            val t = iterator.next()
            t.life -= dt
            t.y -= 22f * dt
            if (t.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun spawnTireSmoke(x: Float, y: Float) {
        particles.add(
            Particle(
                x = x,
                y = y,
                vx = proceduralRng.nextFloat() * -50f - 20f,
                vy = proceduralRng.nextFloat() * -30f,
                color = Color(0x66FFFFFF),
                initialRadius = 6f,
                currentRadius = 6f,
                life = 0.45f,
                maxLife = 0.45f
            )
        )
    }

    private fun spawnCoinSparkles(x: Float, y: Float) {
        for (i in 0 until 8) {
            val angle = proceduralRng.nextFloat() * 2f * PI.toFloat()
            val speed = proceduralRng.nextFloat() * 80f + 40f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = Color(0xFFFFD700),
                    initialRadius = 4f,
                    currentRadius = 4f,
                    life = 0.5f,
                    maxLife = 0.5f
                )
            )
        }
    }

    private fun spawnFuelSparkles(x: Float, y: Float) {
        for (i in 0 until 10) {
            val angle = proceduralRng.nextFloat() * 2f * PI.toFloat()
            val speed = proceduralRng.nextFloat() * 90f + 40f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = Color(0xFFEF4444),
                    initialRadius = 5f,
                    currentRadius = 5f,
                    life = 0.6f,
                    maxLife = 0.6f
                )
            )
        }
    }

    private fun spawnObstacleDebris(x: Float, y: Float, isBarrel: Boolean) {
        val debrisColor = if (isBarrel) Color(0xFFD84315) else Color(0xFF8D6E63)
        for (i in 0 until 12) {
            val angle = proceduralRng.nextFloat() * 2f * PI.toFloat()
            val speed = proceduralRng.nextFloat() * 120f + 50f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 40f,
                    color = debrisColor,
                    initialRadius = proceduralRng.nextFloat() * 4f + 3f,
                    currentRadius = 5f,
                    life = 0.7f,
                    maxLife = 0.7f
                )
            )
        }
    }

    private fun spawnFinishConfetti(x: Float, y: Float) {
        val colors = listOf(
            Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676),
            Color(0xFF2979FF), Color(0xFFD500F9), Color(0xFFFF9100)
        )
        for (i in 0 until 35) {
            val angle = proceduralRng.nextFloat() * 2f * PI.toFloat()
            val speed = proceduralRng.nextFloat() * 220f + 80f
            particles.add(
                Particle(
                    x = x,
                    y = y - 70f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 90f,
                    color = colors[proceduralRng.nextInt(colors.size)],
                    initialRadius = proceduralRng.nextFloat() * 5f + 4f,
                    currentRadius = 6f,
                    life = 1.6f,
                    maxLife = 1.6f
                )
            )
        }
    }

    private fun spawnCrashExplosion(x: Float, y: Float) {
        for (i in 0 until 30) {
            val angle = proceduralRng.nextFloat() * 2f * PI.toFloat()
            val speed = proceduralRng.nextFloat() * 160f + 60f
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 50f,
                    color = if (proceduralRng.nextBoolean()) Color(0xFFFF5722) else Color(0xFFFFEB3B),
                    initialRadius = 7f,
                    currentRadius = 7f,
                    life = 0.9f,
                    maxLife = 0.9f
                )
            )
        }
    }
}
