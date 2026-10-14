package com.example.model

import androidx.compose.ui.graphics.Color

enum class VehicleType(
    val title: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val unlockCost: Int,
    val baseMass: Float,
    val wheelRadius: Float,
    val frontWheelRadius: Float = wheelRadius,
    val wheelBase: Float,
    val suspensionLength: Float,
    val baseEngineTorque: Float,
    val baseTireGrip: Float,
    val isOneWheeler: Boolean = false,
    val speedRating: Int = 60,
    val powerRating: Int = 60,
    val suspensionRating: Int = 60,
    val gripRating: Int = 60
) {
    JEEP(
        title = "Hill Climber 4x4",
        description = "Classic starter 4WD jeep. Reliable climbing stability and balanced suspension.",
        primaryColor = Color(0xFFD32F2F),
        secondaryColor = Color(0xFF1E293B),
        unlockCost = 0,
        baseMass = 120f,
        wheelRadius = 18f,
        frontWheelRadius = 18f,
        wheelBase = 68f,
        suspensionLength = 24f,
        baseEngineTorque = 1.0f,
        baseTireGrip = 1.0f,
        speedRating = 60,
        powerRating = 65,
        suspensionRating = 70,
        gripRating = 65
    ),
    BIKE(
        title = "Motocross Dirt Bike",
        description = "Lightweight stunt 2-wheeler. Extreme airtime, fast frontflips and backflips.",
        primaryColor = Color(0xFFF57C00),
        secondaryColor = Color(0xFFE65100),
        unlockCost = 500,
        baseMass = 65f,
        wheelRadius = 16f,
        frontWheelRadius = 16f,
        wheelBase = 56f,
        suspensionLength = 26f,
        baseEngineTorque = 1.25f,
        baseTireGrip = 0.95f,
        speedRating = 75,
        powerRating = 70,
        suspensionRating = 65,
        gripRating = 60
    ),
    MONSTER_TRUCK(
        title = "Bigfoot Monster Truck",
        description = "Massive bouncy balloon tires. Crushes rough bumps, rocks, and wooden crates.",
        primaryColor = Color(0xFF1976D2),
        secondaryColor = Color(0xFF0D47A1),
        unlockCost = 1500,
        baseMass = 190f,
        wheelRadius = 28f,
        frontWheelRadius = 28f,
        wheelBase = 84f,
        suspensionLength = 34f,
        baseEngineTorque = 1.45f,
        baseTireGrip = 1.25f,
        speedRating = 70,
        powerRating = 90,
        suspensionRating = 95,
        gripRating = 85
    ),
    TRACTOR(
        title = "Farm Power Tractor",
        description = "Iconic farm tractor with huge rear mud wheels, small front steers, and immense torque.",
        primaryColor = Color(0xFF2E7D32),
        secondaryColor = Color(0xFFFBC02D),
        unlockCost = 3000,
        baseMass = 210f,
        wheelRadius = 26f,
        frontWheelRadius = 15f,
        wheelBase = 72f,
        suspensionLength = 22f,
        baseEngineTorque = 1.70f,
        baseTireGrip = 1.40f,
        speedRating = 55,
        powerRating = 95,
        suspensionRating = 60,
        gripRating = 92
    ),
    TRUCK(
        title = "Heavy Mining Truck",
        description = "Multi-ton heavy cargo hauler. Heavy armored frame with giant momentum.",
        primaryColor = Color(0xFFE65100),
        secondaryColor = Color(0xFF37474F),
        unlockCost = 5500,
        baseMass = 250f,
        wheelRadius = 22f,
        frontWheelRadius = 22f,
        wheelBase = 88f,
        suspensionLength = 26f,
        baseEngineTorque = 1.80f,
        baseTireGrip = 1.30f,
        speedRating = 65,
        powerRating = 92,
        suspensionRating = 75,
        gripRating = 80
    ),
    QUAD_BIKE(
        title = "4x4 Desert Quad ATV",
        description = "All-terrain 4-wheeler ATV with wide knobby tires and low center of gravity.",
        primaryColor = Color(0xFF00897B),
        secondaryColor = Color(0xFF004D40),
        unlockCost = 8500,
        baseMass = 85f,
        wheelRadius = 18f,
        frontWheelRadius = 18f,
        wheelBase = 60f,
        suspensionLength = 24f,
        baseEngineTorque = 1.55f,
        baseTireGrip = 1.35f,
        speedRating = 80,
        powerRating = 80,
        suspensionRating = 78,
        gripRating = 85
    ),
    RALLY_CAR(
        title = "Turbo Rally Car",
        description = "Low-slung aerodynamic racer with high-speed turbo and responsive downforce.",
        primaryColor = Color(0xFF00ACC1),
        secondaryColor = Color(0xFF006064),
        unlockCost = 12000,
        baseMass = 95f,
        wheelRadius = 17f,
        frontWheelRadius = 17f,
        wheelBase = 68f,
        suspensionLength = 20f,
        baseEngineTorque = 1.75f,
        baseTireGrip = 1.35f,
        speedRating = 90,
        powerRating = 85,
        suspensionRating = 70,
        gripRating = 88
    ),
    HIPPIE_VAN(
        title = "Classic Hippie Van",
        description = "Vintage surf bus with retro styling, roof rack, and soft smooth suspension.",
        primaryColor = Color(0xFF8E24AA),
        secondaryColor = Color(0xFFFFB300),
        unlockCost = 18000,
        baseMass = 145f,
        wheelRadius = 19f,
        frontWheelRadius = 19f,
        wheelBase = 76f,
        suspensionLength = 26f,
        baseEngineTorque = 1.35f,
        baseTireGrip = 1.15f,
        speedRating = 68,
        powerRating = 72,
        suspensionRating = 85,
        gripRating = 70
    ),
    TANK(
        title = "Armored Assault Tank",
        description = "Heavy military tracked tank with armor turret. Conquers vertical 70-degree cliffs.",
        primaryColor = Color(0xFF4E342E),
        secondaryColor = Color(0xFF2E7D32),
        unlockCost = 25000,
        baseMass = 280f,
        wheelRadius = 20f,
        frontWheelRadius = 20f,
        wheelBase = 92f,
        suspensionLength = 18f,
        baseEngineTorque = 2.10f,
        baseTireGrip = 1.60f,
        speedRating = 60,
        powerRating = 100,
        suspensionRating = 70,
        gripRating = 98
    ),
    SUPERCAR(
        title = "Formula F1 Hypercar",
        description = "Twin-turbo V12 hypercar. Blistering top speed and track-hugging ground suction.",
        primaryColor = Color(0xFFFFD600),
        secondaryColor = Color(0xFF212121),
        unlockCost = 35000,
        baseMass = 105f,
        wheelRadius = 18f,
        frontWheelRadius = 18f,
        wheelBase = 72f,
        suspensionLength = 19f,
        baseEngineTorque = 2.40f,
        baseTireGrip = 1.50f,
        speedRating = 100,
        powerRating = 95,
        suspensionRating = 65,
        gripRating = 95
    ),
    ONE_WHEELER(
        title = "Bonus Unicycle",
        description = "Agile single wheel unicycle. Stunt master with agile flip controls.",
        primaryColor = Color(0xFFE91E63),
        secondaryColor = Color(0xFF880E4F),
        unlockCost = 50000,
        baseMass = 55f,
        wheelRadius = 22f,
        frontWheelRadius = 22f,
        wheelBase = 0f,
        suspensionLength = 26f,
        baseEngineTorque = 1.5f,
        baseTireGrip = 1.3f,
        isOneWheeler = true,
        speedRating = 70,
        powerRating = 65,
        suspensionRating = 80,
        gripRating = 75
    )
}

enum class EnvironmentType(
    val envName: String,
    val description: String,
    val unlockCost: Int,
    val skyTopColor: Color,
    val skyBottomColor: Color,
    val hillSurfaceColor: Color,
    val hillSubsurfaceColor: Color,
    val gravity: Float,
    val frictionMultiplier: Float,
    val isUnderground: Boolean = false
) {
    COUNTRYSIDE(
        envName = "Countryside",
        description = "Rolling lush green meadows, trees, and bridges. Free starter track.",
        unlockCost = 0,
        skyTopColor = Color(0xFF4FC3F7),
        skyBottomColor = Color(0xFFE1F5FE),
        hillSurfaceColor = Color(0xFF7CB342),
        hillSubsurfaceColor = Color(0xFF5D4037),
        gravity = 9.8f,
        frictionMultiplier = 1.0f
    ),
    DESERT(
        envName = "Desert Dunes",
        description = "Endless undulating sand dunes, pyramids, and scorching sun.",
        unlockCost = 600,
        skyTopColor = Color(0xFF29B6F6),
        skyBottomColor = Color(0xFFFFF3E0),
        hillSurfaceColor = Color(0xFFFBC02D),
        hillSubsurfaceColor = Color(0xFFE65100),
        gravity = 9.8f,
        frictionMultiplier = 0.88f
    ),
    ARCTIC(
        envName = "Arctic Snow",
        description = "Slick frozen ice glaciers and snowy peaks with reduced tire traction.",
        unlockCost = 1500,
        skyTopColor = Color(0xFF81D4FA),
        skyBottomColor = Color(0xFFE0F7FA),
        hillSurfaceColor = Color(0xFFE0F2F1),
        hillSubsurfaceColor = Color(0xFF37474F),
        gravity = 9.8f,
        frictionMultiplier = 0.65f
    ),
    MOON(
        envName = "Moon Space",
        description = "Low gravity craters (3.6 m/s²). Skyrocket into the stars with giant jumps!",
        unlockCost = 3000,
        skyTopColor = Color(0xFF0D1B2A),
        skyBottomColor = Color(0xFF1B263B),
        hillSurfaceColor = Color(0xFFB0BEC5),
        hillSubsurfaceColor = Color(0xFF263238),
        gravity = 3.6f,
        frictionMultiplier = 0.95f
    ),
    CAVE(
        envName = "Cave Cavern",
        description = "Dark underground rocky caverns with bumpy terrain and glowing crystals.",
        unlockCost = 5000,
        skyTopColor = Color(0xFF1A1A24),
        skyBottomColor = Color(0xFF2D2235),
        hillSurfaceColor = Color(0xFF7E57C2),
        hillSubsurfaceColor = Color(0xFF21172B),
        gravity = 9.8f,
        frictionMultiplier = 0.95f,
        isUnderground = true
    ),
    HIGHWAY(
        envName = "Highway Bridge",
        description = "Long smooth asphalt highway with flyovers, streetlights, and speed ramps.",
        unlockCost = 8000,
        skyTopColor = Color(0xFF37474F),
        skyBottomColor = Color(0xFF78909C),
        hillSurfaceColor = Color(0xFF263238),
        hillSubsurfaceColor = Color(0xFF1E293B),
        gravity = 9.8f,
        frictionMultiplier = 1.25f
    );

    val stageName: String get() = envName
}

typealias StageType = EnvironmentType

data class LevelInfo(
    val levelNumber: Int,
    val environment: EnvironmentType,
    val lengthMeters: Int,
    val targetCoins: Int = 30,
    val steepnessMultiplier: Float = 1.0f,
    val steepnessFactor: Float = steepnessMultiplier,
    val gapFrequency: Float = 0.5f
)

data class FinishGate(
    val levelNumber: Int,
    val distanceMeters: Int,
    val worldX: Float,
    var isCrossed: Boolean = false
)

enum class UpgradeType(
    val title: String,
    val description: String,
    val maxLevel: Int = 10
) {
    ENGINE("Engine", "Increases horsepower, acceleration & climbing torque"),
    SUSPENSION("Suspension", "Reduces bouncing, improves landing cushion & stability"),
    TIRES("Tires", "Increases surface traction & hill grip"),
    FUEL_TANK("Fuel & 4WD", "Expands fuel capacity, fuel efficiency & 4-wheel drive power"),
    FUEL_4WD("Fuel & 4WD", "Expands fuel capacity, fuel efficiency & 4-wheel drive power")
}

data class VehicleUpgradeState(
    val engineLevel: Int = 1,
    val suspensionLevel: Int = 1,
    val tiresLevel: Int = 1,
    val fuelLevel: Int = 1
) {
    fun getUpgradeCost(type: UpgradeType): Int {
        val currentLevel = when (type) {
            UpgradeType.ENGINE -> engineLevel
            UpgradeType.SUSPENSION -> suspensionLevel
            UpgradeType.TIRES -> tiresLevel
            UpgradeType.FUEL_TANK, UpgradeType.FUEL_4WD -> fuelLevel
        }
        return if (currentLevel >= type.maxLevel) 0 else currentLevel * 150
    }
}

data class CoinItem(
    val id: Long,
    val x: Float,
    val y: Float,
    val value: Int, // 5, 10, 50, 100, 500
    var collected: Boolean = false
)

data class FuelCanItem(
    val id: Long,
    val x: Float,
    val y: Float,
    var collected: Boolean = false
)

enum class ObstacleType {
    WOOD_CRATE,
    OIL_BARREL
}

data class ObstacleItem(
    val id: Long,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    var angle: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vRot: Float = 0f,
    val type: ObstacleType = ObstacleType.WOOD_CRATE,
    var isDestroyed: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val initialRadius: Float,
    var currentRadius: Float,
    var life: Float,
    val maxLife: Float
)

data class FloatingNotification(
    val id: Long,
    val text: String,
    val color: Color,
    var x: Float,
    var y: Float,
    var life: Float,
    val maxLife: Float = 1.4f
)

enum class GameState {
    GARAGE,
    PLAYING,
    PAUSED,
    LEVEL_COMPLETED,
    GAME_OVER_FLIPPED,
    GAME_OVER_FUEL
}
