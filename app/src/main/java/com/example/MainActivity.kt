package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.theme.MyApplicationTheme

import com.example.game.GameScreen
import com.example.game.MainMenuScreen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        var isPlaying by remember { mutableStateOf(false) }
        var startingCredits by remember { mutableStateOf(5000) }
        var mapSize by remember { mutableStateOf(40) }
        var difficulty by remember { mutableStateOf(1) }
        var playerFaction by remember { mutableStateOf("BTX") }
        
        if (isPlaying) {
            GameScreen(startingCredits = startingCredits, mapSize = mapSize, difficulty = difficulty, onExit = { isPlaying = false })
        } else {
            MainMenuScreen(onStartGame = { credits, _, _, size, diff -> 
                startingCredits = credits
                mapSize = size
                difficulty = diff
                isPlaying = true 
            })
        }
      }
    }
  }
}

// Removed Greeting
