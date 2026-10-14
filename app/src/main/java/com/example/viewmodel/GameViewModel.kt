package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.model.EnvironmentType
import com.example.model.GameState
import com.example.model.LevelInfo
import com.example.model.UpgradeType
import com.example.model.VehicleType
import com.example.model.VehicleUpgradeState
import com.example.physics.PhysicsEngine
import com.example.sound.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val repository = GameRepository(application)
    val soundManager = SoundManager(application)

    private val _gameState = MutableStateFlow(GameState.GARAGE)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _currentPhysicsEngine = MutableStateFlow<PhysicsEngine?>(null)
    val physicsEngine: StateFlow<PhysicsEngine?> = _currentPhysicsEngine.asStateFlow()

    // Pedal Input States
    var isGasPressed = false
    var isBrakePressed = false

    // Run Summary Info
    private val _runDistance = MutableStateFlow(0)
    val runDistance: StateFlow<Int> = _runDistance.asStateFlow()

    private val _runCoins = MutableStateFlow(0)
    val runCoins: StateFlow<Int> = _runCoins.asStateFlow()

    private val _isNewRecord = MutableStateFlow(false)
    val isNewRecord: StateFlow<Boolean> = _isNewRecord.asStateFlow()

    private val _perfectRunBonusAwarded = MutableStateFlow(0)
    val perfectRunBonusAwarded: StateFlow<Int> = _perfectRunBonusAwarded.asStateFlow()

    private var gameLoopJob: Job? = null

    init {
        // Prepare initial engine for the selected vehicle and level
        setupNewRun(repository.currentLevel.value)
    }

    fun startRace() {
        val currentLevel = repository.currentLevel.value
        setupNewRun(currentLevel)
        _gameState.value = GameState.PLAYING
        startGameLoop()
    }

    fun restartRace() {
        val currentLevel = repository.currentLevel.value
        setupNewRun(currentLevel)
        _gameState.value = GameState.PLAYING
        startGameLoop()
    }

    fun nextLevel() {
        val nextLvl = (repository.currentLevel.value + 1).coerceAtMost(100)
        repository.setLevel(nextLvl)
        setupNewRun(nextLvl)
        _gameState.value = GameState.PLAYING
        startGameLoop()
    }

    fun returnToGarage() {
        stopGameLoop()
        _gameState.value = GameState.GARAGE
    }

    fun pauseGame() {
        if (_gameState.value == GameState.PLAYING) {
            _gameState.value = GameState.PAUSED
            stopGameLoop()
        }
    }

    fun resumeGame() {
        if (_gameState.value == GameState.PAUSED) {
            _gameState.value = GameState.PLAYING
            startGameLoop()
        }
    }

    fun setGas(pressed: Boolean) {
        isGasPressed = pressed
        if (pressed) {
            soundManager.vibrate(15)
        }
    }

    fun setBrake(pressed: Boolean) {
        isBrakePressed = pressed
        if (pressed) {
            soundManager.vibrate(15)
        }
    }

    fun selectVehicle(vehicle: VehicleType) {
        repository.selectVehicle(vehicle)
        setupNewRun(repository.currentLevel.value)
    }

    fun unlockVehicle(vehicle: VehicleType): Boolean {
        val success = repository.unlockVehicle(vehicle)
        if (success) {
            soundManager.playSound("coin")
            setupNewRun(repository.currentLevel.value)
        }
        return success
    }

    fun upgradeVehicle(type: UpgradeType): Boolean {
        val vehicle = repository.selectedVehicle.value
        val success = repository.upgradeVehicle(vehicle, type)
        if (success) {
            soundManager.playSound("fuel")
            setupNewRun(repository.currentLevel.value)
        }
        return success
    }

    fun selectLevel(level: Int) {
        repository.setLevel(level)
        setupNewRun(level)
    }

    private fun setupNewRun(level: Int) {
        val vehicle = repository.selectedVehicle.value
        val upgrades = repository.getUpgradeState(vehicle)
        val levelConfig = repository.getLevelConfig(level)

        _currentPhysicsEngine.value = PhysicsEngine(vehicle, upgrades, levelConfig)
        isGasPressed = false
        isBrakePressed = false
        _runDistance.value = 0
        _runCoins.value = 0
        _isNewRecord.value = false
        _perfectRunBonusAwarded.value = 0
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            val dt = 1f / 60f
            while (isActive && _gameState.value == GameState.PLAYING) {
                val engine = _currentPhysicsEngine.value
                if (engine != null) {
                    val nextState = engine.update(
                        dt = dt,
                        isGasPressed = isGasPressed,
                        isBrakePressed = isBrakePressed,
                        onSoundEvent = { event -> soundManager.playSound(event) }
                    )

                    _runDistance.value = engine.distanceMeters
                    _runCoins.value = engine.coinsCollectedScore

                    if (nextState != GameState.PLAYING) {
                        handleGameOverOrWin(nextState, engine)
                        break
                    }
                }
                delay(16) // ~60fps
            }
        }
    }

    private fun handleGameOverOrWin(finalState: GameState, engine: PhysicsEngine) {
        _gameState.value = finalState
        stopGameLoop()

        // Commit earned coins to bank
        var coinsEarned = engine.coinsCollectedScore

        // Perfect run bonus: if completed level without flipping and collected all fuel cans
        if (finalState == GameState.LEVEL_COMPLETED) {
            repository.completeLevel(engine.levelInfo.levelNumber)
            if (engine.perfectRunEligible && engine.totalFuelCollected >= engine.fuelCans.size) {
                val perfectBonus = 500 + engine.levelInfo.levelNumber * 10
                coinsEarned += perfectBonus
                _perfectRunBonusAwarded.value = perfectBonus
            }
        }

        repository.addCoins(coinsEarned)

        // Record update
        val isNew = repository.updateBestDistance(engine.levelInfo.levelNumber, engine.distanceMeters)
        _isNewRecord.value = isNew
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
