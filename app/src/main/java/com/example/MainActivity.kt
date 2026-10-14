package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GameState
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GarageScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HillClimbApp()
            }
        }
    }
}

@Composable
fun HillClimbApp(
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .safeDrawingPadding()
    ) {
        when (gameState) {
            GameState.GARAGE -> {
                GarageScreen(
                    viewModel = viewModel,
                    onStartRace = { viewModel.startRace() }
                )
            }
            else -> {
                GameScreen(
                    viewModel = viewModel,
                    onNavigateToGarage = { viewModel.returnToGarage() }
                )
            }
        }
    }
}
