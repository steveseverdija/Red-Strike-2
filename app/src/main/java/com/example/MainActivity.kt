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
        
        if (isPlaying) {
            GameScreen(startingCredits = startingCredits, onExit = { isPlaying = false })
        } else {
            MainMenuScreen(onStartGame = { credits, _, _ -> 
                startingCredits = credits
                isPlaying = true 
            })
        }
      }
    }
  }
}

// Removed Greeting
