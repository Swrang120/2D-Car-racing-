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
    val baseTireGrip: Float
) {
    JEEP(
        title = "Hill Climber Jeep",
        description = "The classic reliable 4x4. Great balance and steady climbing.",
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
        title = "Motocross Bike",
        description = "Ultra agile and lightweight. Massive airtime and backflips!",
        primaryColor = Color(0xFFF57C00),
        secondaryColor = Color(0xFFE65100),
        unlockCost = 500,
        baseMass = 70f,
        wheelRadius = 16f,
        wheelBase = 58f,
        suspensionLength = 26f,
        baseEngineTorque = 1.25f,
        baseTireGrip = 0.95f
    ),
    MONSTER_TRUCK(
        title = "Monster 4x4",
        description = "Enormous high-cushion tires. Easily crushes bumps and obstacles.",
        primaryColor = Color(0xFF1976D2),
        secondaryColor = Color(0xFF0D47A1),
        unlockCost = 1500,
        baseMass = 180f,
        wheelRadius = 26f,
        wheelBase = 84f,
        suspensionLength = 34f,
        baseEngineTorque = 1.35f,
        baseTireGrip = 1.2f
    ),
    TANK(
        title = "Steel Tank",
        description = "Armored powerhouse with continuous tracks. Unstoppable steep torque.",
        primaryColor = Color(0xFF388E3C),
        secondaryColor = Color(0xFF2E7D32),
        unlockCost = 3500,
        baseMass = 250f,
        wheelRadius = 20f,
        wheelBase = 90f,
        suspensionLength = 18f,
        baseEngineTorque = 1.6f,
        baseTireGrip = 1.45f
    )
}

enum class EnvironmentType(
    val envName: String,
    val skyTopColor: Color,
    val skyBottomColor: Color,
    val hillSurfaceColor: Color,
    val hillSubsurfaceColor: Color,
    val gravity: Float,
    val frictionMultiplier: Float,
    val description: String
) {
    COUNTRYSIDE(
        envName = "Countryside",
        skyTopColor = Color(0xFF4FC3F7),
        skyBottomColor = Color(0xFFE1F5FE),
        hillSurfaceColor = Color(0xFF7CB342),
        hillSubsurfaceColor = Color(0xFF5D4037),
        gravity = 9.8f,
        frictionMultiplier = 1.0f,
        description = "Rolling lush green meadows, trees and wooden bridges."
    ),
    DESERT(
        envName = "Desert",
        skyTopColor = Color(0xFF29B6F6),
        skyBottomColor = Color(0xFFFFF3E0),
        hillSurfaceColor = Color(0xFFFBC02D),
        hillSubsurfaceColor = Color(0xFFE65100),
        gravity = 9.8f,
        frictionMultiplier = 0.88f,
        description = "Steep sand dunes, pyramids and scorching sunshine."
    ),
    ARCTIC(
        envName = "Arctic Ice",
        skyTopColor = Color(0xFF81D4FA),
        skyBottomColor = Color(0xFFE0F7FA),
        hillSurfaceColor = Color(0xFFE0F2F1),
        hillSubsurfaceColor = Color(0xFF37474F),
        gravity = 9.8f,
        frictionMultiplier = 0.65f,
        description = "Slick frozen glaciers with icy snow peaks."
    ),
    MOON(
        envName = "Moon Space",
        skyTopColor = Color(0xFF0D1B2A),
        skyBottomColor = Color(0xFF1B263B),
        hillSurfaceColor = Color(0xFFB0BEC5),
        hillSubsurfaceColor = Color(0xFF263238),
        gravity = 3.6f,
        frictionMultiplier = 0.95f,
        description = "Low gravity craters. Epic high jumps in the void!"
    )
}

enum class UpgradeType(
    val title: String,
    val iconDescription: String,
    val maxLevel: Int = 10
) {
    ENGINE("Engine", "More climbing torque, top speed & power"),
    SUSPENSION("Suspension", "Better shock absorption & stability"),
    TIRES("Tires", "Higher traction & grip on steep inclines"),
    FUEL_TANK("Fuel Tank", "Larger capacity & lower fuel burn rate")
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
            UpgradeType.FUEL_TANK -> fuelLevel
        }
        return if (currentLevel >= type.maxLevel) 0 else currentLevel * 150
    }
}

data class CoinItem(
    val id: Int,
    val x: Float,
    val y: Float,
    val value: Int,
    var collected: Boolean = false
)

data class FuelCanItem(
    val id: Int,
    val x: Float,
    val y: Float,
    var collected: Boolean = false
)

enum class ObstacleType {
    WOOD_CRATE,
    OIL_BARREL
}

data class ObstacleItem(
    val id: Int,
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
    GAME_OVER_FUEL,
    LEVEL_COMPLETED
}

data class LevelInfo(
    val levelNumber: Int,
    val environment: EnvironmentType,
    val lengthMeters: Int,
    val targetCoins: Int,
    val steepnessFactor: Float,
    val gapFrequency: Float
)
