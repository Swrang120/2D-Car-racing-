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
    val wheelBase: Float,
    val suspensionLength: Float,
    val baseEngineTorque: Float,
    val baseTireGrip: Float,
    val isOneWheeler: Boolean = false
) {
    JEEP(
        title = "Classic Red Jeep",
        description = "Free starter. Reliable 4x4 with balanced climbing stability.",
        primaryColor = Color(0xFFD32F2F),
        secondaryColor = Color(0xFF1E293B),
        unlockCost = 0,
        baseMass = 120f,
        wheelRadius = 18f,
        wheelBase = 68f,
        suspensionLength = 24f,
        baseEngineTorque = 1.0f,
        baseTireGrip = 1.0f
    ),
    BIKE(
        title = "Motocross Dirt Bike",
        description = "Lightweight two-wheeler. Extreme airtime, frontflips & backflips.",
        primaryColor = Color(0xFFF57C00),
        secondaryColor = Color(0xFFE65100),
        unlockCost = 500,
        baseMass = 65f,
        wheelRadius = 16f,
        wheelBase = 56f,
        suspensionLength = 26f,
        baseEngineTorque = 1.25f,
        baseTireGrip = 0.95f
    ),
    MONSTER_TRUCK(
        title = "Heavy Monster Truck",
        description = "Massive bouncy tires. Easily smashes bumps, rocks, and obstacles.",
        primaryColor = Color(0xFF1976D2),
        secondaryColor = Color(0xFF0D47A1),
        unlockCost = 1200,
        baseMass = 190f,
        wheelRadius = 28f,
        wheelBase = 84f,
        suspensionLength = 34f,
        baseEngineTorque = 1.45f,
        baseTireGrip = 1.25f
    ),
    HIPPIE_VAN(
        title = "Hippie Van",
        description = "Iconic flower-power bus with bouncy shocks and high roof clearance.",
        primaryColor = Color(0xFF8E24AA),
        secondaryColor = Color(0xFFFFB300),
        unlockCost = 2500,
        baseMass = 145f,
        wheelRadius = 19f,
        wheelBase = 76f,
        suspensionLength = 26f,
        baseEngineTorque = 1.3f,
        baseTireGrip = 1.1f
    ),
    ONE_WHEELER(
        title = "One-wheeler",
        description = "Unicycle balance master! Extreme stunt bonus and agile flipping.",
        primaryColor = Color(0xFFE91E63),
        secondaryColor = Color(0xFF880E4F),
        unlockCost = 4000,
        baseMass = 55f,
        wheelRadius = 22f,
        wheelBase = 0f,
        suspensionLength = 26f,
        baseEngineTorque = 1.5f,
        baseTireGrip = 1.3f,
        isOneWheeler = true
    ),
    RALLY_CAR(
        title = "Turbo Rally Car",
        description = "Aerodynamic rally machine with low center of gravity and fast acceleration.",
        primaryColor = Color(0xFF00ACC1),
        secondaryColor = Color(0xFF006064),
        unlockCost = 6500,
        baseMass = 95f,
        wheelRadius = 17f,
        wheelBase = 68f,
        suspensionLength = 20f,
        baseEngineTorque = 1.7f,
        baseTireGrip = 1.35f
    ),
    TANK(
        title = "Heavy Army Tank",
        description = "Armored powerhouse with continuous tracks. Climbs any steep mountain.",
        primaryColor = Color(0xFF388E3C),
        secondaryColor = Color(0xFF1B5E20),
        unlockCost = 9000,
        baseMass = 260f,
        wheelRadius = 20f,
        wheelBase = 92f,
        suspensionLength = 18f,
        baseEngineTorque = 2.0f,
        baseTireGrip = 1.55f
    ),
    SUPERCAR(
        title = "Luxury Supercar",
        description = "Twin-turbo hypercar. Extreme top speed and active downforce.",
        primaryColor = Color(0xFFFFD600),
        secondaryColor = Color(0xFF212121),
        unlockCost = 15000,
        baseMass = 105f,
        wheelRadius = 18f,
        wheelBase = 72f,
        suspensionLength = 19f,
        baseEngineTorque = 2.2f,
        baseTireGrip = 1.45f
    )
}

enum class StageType(
    val stageName: String,
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
        stageName = "Countryside",
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
        stageName = "Desert Dunes",
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
        stageName = "Arctic Snow",
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
        stageName = "Moon Space",
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
        stageName = "Cave Cavern",
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
        stageName = "Highway Bridge",
        description = "Long smooth asphalt highway with flyovers, streetlights, and speed ramps.",
        unlockCost = 8000,
        skyTopColor = Color(0xFF37474F),
        skyBottomColor = Color(0xFF78909C),
        hillSurfaceColor = Color(0xFF263238),
        hillSubsurfaceColor = Color(0xFF1E293B),
        gravity = 9.8f,
        frictionMultiplier = 1.25f
    )
}

enum class UpgradeType(
    val title: String,
    val description: String,
    val maxLevel: Int = 10
) {
    ENGINE("Engine", "Increases horsepower, acceleration & climbing torque"),
    SUSPENSION("Suspension", "Reduces bouncing, improves landing cushion & stability"),
    TIRES("Tires", "Increases surface traction & hill grip"),
    FUEL_4WD("4WD & Fuel", "Expands fuel capacity, fuel efficiency & 4-wheel drive power")
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
            UpgradeType.FUEL_4WD -> fuelLevel
        }
        return if (currentLevel >= type.maxLevel) 0 else currentLevel * 120
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
    GAME_OVER_FLIPPED,
    GAME_OVER_FUEL
}
