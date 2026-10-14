package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.EnvironmentType
import com.example.model.LevelInfo
import com.example.model.UpgradeType
import com.example.model.VehicleType
import com.example.model.VehicleUpgradeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("hill_climb_save", Context.MODE_PRIVATE)

    private val _totalCoins = MutableStateFlow(prefs.getInt("total_coins", 150))
    val totalCoins: StateFlow<Int> = _totalCoins.asStateFlow()

    private val _selectedVehicle = MutableStateFlow(
        try {
            VehicleType.valueOf(prefs.getString("selected_vehicle", VehicleType.JEEP.name) ?: VehicleType.JEEP.name)
        } catch (_: Exception) {
            VehicleType.JEEP
        }
    )
    val selectedVehicle: StateFlow<VehicleType> = _selectedVehicle.asStateFlow()

    private val _unlockedVehicles = MutableStateFlow(
        prefs.getStringSet("unlocked_vehicles", setOf(VehicleType.JEEP.name))
            ?: setOf(VehicleType.JEEP.name)
    )
    val unlockedVehicles: StateFlow<Set<String>> = _unlockedVehicles.asStateFlow()

    private val _currentLevel = MutableStateFlow(prefs.getInt("current_level", 1))
    val currentLevel: StateFlow<Int> = _currentLevel.asStateFlow()

    private val _highestLevelUnlocked = MutableStateFlow(prefs.getInt("highest_level_unlocked", 1))
    val highestLevelUnlocked: StateFlow<Int> = _highestLevelUnlocked.asStateFlow()

    fun getUpgradeState(vehicle: VehicleType): VehicleUpgradeState {
        val eng = prefs.getInt("upgrade_${vehicle.name}_engine", 1)
        val susp = prefs.getInt("upgrade_${vehicle.name}_suspension", 1)
        val tires = prefs.getInt("upgrade_${vehicle.name}_tires", 1)
        val fuel = prefs.getInt("upgrade_${vehicle.name}_fuel", 1)
        return VehicleUpgradeState(eng, susp, tires, fuel)
    }

    fun addCoins(amount: Int) {
        val updated = _totalCoins.value + amount
        _totalCoins.value = updated
        prefs.edit().putInt("total_coins", updated).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        if (_totalCoins.value >= amount) {
            val updated = _totalCoins.value - amount
            _totalCoins.value = updated
            prefs.edit().putInt("total_coins", updated).apply()
            return true
        }
        return false
    }

    fun selectVehicle(vehicle: VehicleType) {
        _selectedVehicle.value = vehicle
        prefs.edit().putString("selected_vehicle", vehicle.name).apply()
    }

    fun unlockVehicle(vehicle: VehicleType): Boolean {
        if (spendCoins(vehicle.unlockCost)) {
            val current = _unlockedVehicles.value.toMutableSet()
            current.add(vehicle.name)
            _unlockedVehicles.value = current
            prefs.edit().putStringSet("unlocked_vehicles", current).apply()
            selectVehicle(vehicle)
            return true
        }
        return false
    }

    fun upgradeVehicle(vehicle: VehicleType, type: UpgradeType): Boolean {
        val current = getUpgradeState(vehicle)
        val cost = current.getUpgradeCost(type)
        if (cost > 0 && spendCoins(cost)) {
            when (type) {
                UpgradeType.ENGINE -> prefs.edit().putInt("upgrade_${vehicle.name}_engine", current.engineLevel + 1).apply()
                UpgradeType.SUSPENSION -> prefs.edit().putInt("upgrade_${vehicle.name}_suspension", current.suspensionLevel + 1).apply()
                UpgradeType.TIRES -> prefs.edit().putInt("upgrade_${vehicle.name}_tires", current.tiresLevel + 1).apply()
                UpgradeType.FUEL_TANK, UpgradeType.FUEL_4WD -> prefs.edit().putInt("upgrade_${vehicle.name}_fuel", current.fuelLevel + 1).apply()
            }
            return true
        }
        return false
    }

    fun setLevel(level: Int) {
        _currentLevel.value = level.coerceIn(1, 100)
        prefs.edit().putInt("current_level", _currentLevel.value).apply()
    }

    fun completeLevel(level: Int) {
        if (level >= _highestLevelUnlocked.value && level < 100) {
            val nextLevel = level + 1
            _highestLevelUnlocked.value = nextLevel
            prefs.edit().putInt("highest_level_unlocked", nextLevel).apply()
        }
    }

    fun getBestDistance(level: Int): Int {
        return prefs.getInt("best_distance_lvl_$level", 0)
    }

    fun getBestDistance(stage: EnvironmentType): Int {
        return prefs.getInt("best_distance_stage_${stage.name}", prefs.getInt("best_distance_lvl_${stage.ordinal + 1}", 0))
    }

    fun updateBestDistance(level: Int, distance: Int): Boolean {
        val currentBest = getBestDistance(level)
        if (distance > currentBest) {
            prefs.edit().putInt("best_distance_lvl_$level", distance).apply()
            return true
        }
        return false
    }

    fun updateBestDistance(stage: EnvironmentType, distance: Int): Boolean {
        val currentBest = getBestDistance(stage)
        if (distance > currentBest) {
            prefs.edit().putInt("best_distance_stage_${stage.name}", distance).apply()
            return true
        }
        return false
    }

    fun getLevelConfig(level: Int): LevelInfo {
        val env = when (((level - 1) / 5) % 6) {
            0 -> EnvironmentType.COUNTRYSIDE
            1 -> EnvironmentType.DESERT
            2 -> EnvironmentType.ARCTIC
            3 -> EnvironmentType.MOON
            4 -> EnvironmentType.CAVE
            else -> EnvironmentType.HIGHWAY
        }
        val length = 1000 + (level - 1) * 200
        val steepness = 0.85f + (level.toFloat() / 100f) * 0.75f
        val gapFreq = if (level > 2) (0.4f + (level.toFloat() / 100f) * 0.8f).coerceAtMost(1.5f) else 0f
        val targetCoins = (length / 8).coerceAtLeast(30)

        return LevelInfo(
            levelNumber = level,
            environment = env,
            lengthMeters = length,
            targetCoins = targetCoins,
            steepnessMultiplier = steepness,
            gapFrequency = gapFreq
        )
    }
}
