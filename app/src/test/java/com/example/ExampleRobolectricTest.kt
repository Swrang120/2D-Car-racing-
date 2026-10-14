package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRepository
import com.example.model.GameState
import com.example.model.StageType
import com.example.model.UpgradeType
import com.example.model.VehicleType
import com.example.physics.PhysicsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hill Climb Racing", appName)
    }

    @Test
    fun `verify all 8 vehicles and 6 stages are defined`() {
        val expectedVehicles = listOf(
            VehicleType.JEEP,
            VehicleType.BIKE,
            VehicleType.MONSTER_TRUCK,
            VehicleType.HIPPIE_VAN,
            VehicleType.ONE_WHEELER,
            VehicleType.RALLY_CAR,
            VehicleType.TANK,
            VehicleType.SUPERCAR
        )
        assertEquals(8, expectedVehicles.size)
        for (veh in expectedVehicles) {
            assertNotNull(veh.title)
            assertTrue(veh.wheelRadius > 0f)
            assertTrue(veh.baseEngineTorque > 0f)
        }

        val expectedStages = listOf(
            StageType.COUNTRYSIDE,
            StageType.DESERT,
            StageType.ARCTIC,
            StageType.MOON,
            StageType.CAVE,
            StageType.HIGHWAY
        )
        assertEquals(6, expectedStages.size)
        assertEquals(3.6f, StageType.MOON.gravity, 0.01f)
    }

    @Test
    fun `spawn platform is completely flat for first 50 meters and vehicle starts at 0m without flipping`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameRepository(context)
        val upgrades = repo.getUpgradeState(VehicleType.JEEP)
        val engine = PhysicsEngine(VehicleType.JEEP, upgrades, StageType.COUNTRYSIDE)

        // Verify terrain elevation is completely flat for first 50+ meters (x <= 1500px)
        assertEquals(engine.baseGroundY, engine.getTerrainElevation(0f), 0.001f)
        assertEquals(engine.baseGroundY, engine.getTerrainElevation(500f), 0.001f)
        assertEquals(engine.baseGroundY, engine.getTerrainElevation(1300f), 0.001f)
        assertEquals(engine.baseGroundY, engine.getTerrainElevation(1500f), 0.001f)

        // Verify initial distance starts at 0m
        assertEquals(0, engine.distanceMeters)

        // Verify initial vehicle pose
        assertEquals(0f, engine.chassisAngle, 0.001f)
        assertEquals(engine.baseGroundY - VehicleType.JEEP.wheelRadius, engine.rearWheelY, 0.001f)
        assertEquals(engine.baseGroundY - VehicleType.JEEP.wheelRadius, engine.frontWheelY, 0.001f)

        // Simulate 120 ticks (2 full seconds) of pressing GAS on the flat runway
        var currentState = GameState.PLAYING
        for (i in 0 until 120) {
            currentState = engine.update(
                dt = 1f / 60f,
                isGasPressed = true,
                isBrakePressed = false,
                onSoundEvent = {}
            )
        }

        // Must still be PLAYING without flipping or crashing
        assertEquals(GameState.PLAYING, currentState)
        assertTrue("Vehicle must move forward", engine.chassisX > 250f)
        assertTrue("Distance meters must increase", engine.distanceMeters > 0)
        assertTrue("Vehicle pitch angle must remain safe and stable", engine.chassisAngle in -0.3f..0.3f)
    }

    @Test
    fun `coins and economy persistence works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameRepository(context)
        val initialCoins = repo.totalCoins.value

        repo.addCoins(500)
        assertEquals(initialCoins + 500, repo.totalCoins.value)

        val spent = repo.spendCoins(200)
        assertTrue(spent)
        assertEquals(initialCoins + 300, repo.totalCoins.value)

        val upgradeResult = repo.upgradeVehicle(VehicleType.JEEP, UpgradeType.ENGINE)
        assertTrue(upgradeResult)
        val newUpgrades = repo.getUpgradeState(VehicleType.JEEP)
        assertEquals(2, newUpgrades.engineLevel)
    }
}
