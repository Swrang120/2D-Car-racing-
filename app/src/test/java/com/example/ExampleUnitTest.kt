package com.example

import com.example.model.EnvironmentType
import com.example.model.LevelInfo
import com.example.model.UpgradeType
import com.example.model.VehicleType
import com.example.model.VehicleUpgradeState
import com.example.physics.PhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testVehicleUpgradeCosts() {
        val upgrades = VehicleUpgradeState(engineLevel = 1, suspensionLevel = 2, tiresLevel = 3, fuelLevel = 4)
        assertEquals(150, upgrades.getUpgradeCost(UpgradeType.ENGINE))
        assertEquals(300, upgrades.getUpgradeCost(UpgradeType.SUSPENSION))
        assertEquals(450, upgrades.getUpgradeCost(UpgradeType.TIRES))
        assertEquals(600, upgrades.getUpgradeCost(UpgradeType.FUEL_TANK))
    }

    @Test
    fun testTerrainElevationAndSlope() {
        val levelInfo = LevelInfo(
            levelNumber = 1,
            environment = EnvironmentType.COUNTRYSIDE,
            lengthMeters = 800,
            targetCoins = 100,
            steepnessFactor = 1.0f,
            gapFrequency = 0.5f
        )
        val engine = PhysicsEngine(
            vehicleType = VehicleType.JEEP,
            upgrades = VehicleUpgradeState(),
            levelInfo = levelInfo
        )

        // Elevation at x=0 should be valid
        val y0 = engine.getTerrainElevation(0f)
        assertTrue(y0 > 200f)

        // Elevation at another point should also be computed smoothly
        val y100 = engine.getTerrainElevation(100f)
        assertTrue(y100 > 200f)

        // Slope should be a finite float
        val slope = engine.getTerrainSlope(100f)
        assertTrue(!slope.isNaN())
    }

    @Test
    fun testPhysicsEngineUpdate() {
        val levelInfo = LevelInfo(
            levelNumber = 1,
            environment = EnvironmentType.COUNTRYSIDE,
            lengthMeters = 800,
            targetCoins = 100,
            steepnessFactor = 1.0f,
            gapFrequency = 0.5f
        )
        val engine = PhysicsEngine(
            vehicleType = VehicleType.JEEP,
            upgrades = VehicleUpgradeState(),
            levelInfo = levelInfo
        )

        val initialFuel = engine.currentFuel
        // Step physics with gas pressed
        engine.update(1f / 60f, isGasPressed = true, isBrakePressed = false) {}

        // Fuel should decrease
        assertTrue(engine.currentFuel < initialFuel)
        // Wheels and chassis should be initialized
        assertNotNull(engine.chassisX)
        assertNotNull(engine.rearWheelX)
        assertNotNull(engine.frontWheelX)
    }
}
