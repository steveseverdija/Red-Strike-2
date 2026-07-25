package com.example.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.R
import com.example.ui.theme.*

enum class MenuScreenState { MAIN, LOBBY, SETTINGS }

@Composable
fun MainMenuScreen(onStartGame: (Int, String, String, Int, Int) -> Unit) {
    var screenState by remember { mutableStateOf(MenuScreenState.MAIN) }

    // Lobby State
    var selectedFaction by remember { mutableStateOf("GDI") }
    var selectedMap by remember { mutableStateOf("Winter") }
    var startingResources by remember { mutableStateOf(5000) }
    var mapSize by remember { mutableStateOf(40) }
    var difficulty by remember { mutableStateOf(1) } // 0=Easy, 1=Normal, 2=Hard

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (screenState) {
            MenuScreenState.MAIN -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val titleAscii = """
██████╗ ███████╗██████╗     ███████╗████████╗██████╗ ██╗██╗  ██╗███████╗
██╔══██╗██╔════╝██╔══██╗    ██╔════╝╚══██╔══╝██╔══██╗██║██║ ██╔╝██╔════╝
██████╔╝█████╗  ██║  ██║    ███████╗   ██║   ██████╔╝██║█████╔╝ █████╗  
██╔══██╗██╔══╝  ██║  ██║    ╚════██║   ██║   ██╔══██╗██║██╔═██╗ ██╔══╝  
██║  ██║███████╗██████╔╝    ███████║   ██║   ██║  ██║██║██║  ██╗███████╗
╚═╝  ╚═╝╚══════╝╚═════╝     ╚══════╝   ╚═╝   ╚═╝  ╚═╝╚═╝╚═╝  ╚═╝╚══════╝
                     
          >_ TACTICAL DEPLOYMENT SYSTEM ACTIVE
                    """.trimIndent()
                    
                    Text(
                        text = titleAscii,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentRed,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 10.sp,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    MenuButton("[ CAMPAIGN ]") { /* TODO */ }
                    MenuButton("[ NEW GAME ]") { screenState = MenuScreenState.LOBBY }
                    MenuButton("[ LOAD GAME ]") { /* TODO */ }
                    MenuButton("[ SETTINGS ]") { screenState = MenuScreenState.SETTINGS }
                    MenuButton("[ EXIT ]") { /* Exit app */ }
                }
            }
            MenuScreenState.LOBBY -> {
                Column(
                    modifier = Modifier.width(350.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("---[ GAME LOBBY ]---", fontSize = 24.sp, color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    
                    Text("> Faction:", color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceButton("GDI", selectedFaction == "GDI") { selectedFaction = "GDI" }
                        ChoiceButton("NOD", selectedFaction == "NOD") { selectedFaction = "NOD" }
                    }
                    
                    Text("> Map:", color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceButton("Winter", selectedMap == "Winter") { selectedMap = "Winter" }
                        ChoiceButton("Forest", selectedMap == "Forest") { selectedMap = "Forest" }
                    }

                    Column(horizontalAlignment = Alignment.Start) {
                        Text("> Starting Resources: ₿$startingResources", color = TextSecondary, fontFamily = FontFamily.Monospace)
                        
                        val maxBars = 20
                        val fraction = (startingResources - 1000f) / 9000f
                        val activeBars = (fraction * maxBars).toInt().coerceIn(0, maxBars)
                        val asciiSlider = "[" + "█".repeat(activeBars) + "-".repeat(maxBars - activeBars) + "]"
                        
                        Text(
                            text = asciiSlider,
                            color = AccentRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val touchFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        val rawValue = (touchFraction * 9000f) + 1000f
                                        startingResources = ((rawValue + 500f) / 1000f).toInt() * 1000
                                        startingResources = startingResources.coerceIn(1000, 10000)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val touchFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        val rawValue = (touchFraction * 9000f) + 1000f
                                        startingResources = ((rawValue + 500f) / 1000f).toInt() * 1000
                                        startingResources = startingResources.coerceIn(1000, 10000)
                                    }
                                }
                        )
                    }

                    Column(horizontalAlignment = Alignment.Start) {
                        val sizeStr = if (mapSize == 20) "Small (20x20)" else if (mapSize == 40) "Medium (40x40)" else if (mapSize == 60) "Large (60x60)" else if (mapSize == 80) "Huge (80x80)" else "${mapSize}x${mapSize}"
                        Text("> Map Size: $sizeStr", color = TextSecondary, fontFamily = FontFamily.Monospace)
                        
                        val maxBars = 20
                        val fraction = (mapSize - 20f) / 60f
                        val activeBars = (fraction * maxBars).toInt().coerceIn(0, maxBars)
                        val asciiSlider = "[" + "█".repeat(activeBars) + "-".repeat(maxBars - activeBars) + "]"
                        
                        Text(
                            text = asciiSlider,
                            color = AccentRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val touchFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        val rawValue = (touchFraction * 60f) + 20f
                                        mapSize = ((rawValue + 10f) / 20f).toInt() * 20
                                        mapSize = mapSize.coerceIn(20, 80)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val touchFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        val rawValue = (touchFraction * 60f) + 20f
                                        mapSize = ((rawValue + 10f) / 20f).toInt() * 20
                                        mapSize = mapSize.coerceIn(20, 80)
                                    }
                                }
                        )
                    }
                    
                                        Column(horizontalAlignment = Alignment.Start) {
                        val diffStr = if (difficulty == 0) "Easy" else if (difficulty == 1) "Normal" else "Hard"
                        Text("> Enemy Difficulty: $diffStr", color = TextSecondary, fontFamily = FontFamily.Monospace)
                        
                        val maxBars = 20
                        val fraction = difficulty / 2f
                        val activeBars = (fraction * maxBars).toInt().coerceIn(0, maxBars)
                        val asciiSlider = "[" + "█".repeat(activeBars) + "-".repeat(maxBars - activeBars) + "]"
                        
                        Text(
                            text = asciiSlider,
                            color = AccentRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val touchFraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        difficulty = (touchFraction * 2).toInt().coerceIn(0, 2)
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val touchFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        difficulty = (touchFraction * 2).toInt().coerceIn(0, 2)
                                    }
                                }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        MenuButton("[ BACK ]", modifier = Modifier.weight(1f)) { screenState = MenuScreenState.MAIN }
                        Spacer(modifier = Modifier.width(16.dp))
                        MenuButton("[ START ]", modifier = Modifier.weight(1f), color = AccentRedDark) { 
                            onStartGame(startingResources, selectedFaction, selectedMap, mapSize, difficulty) 
                        }
                    }
                }
            }
            MenuScreenState.SETTINGS -> {
                var volume by remember { mutableStateOf(50f) }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("---[ SETTINGS ]---", fontSize = 24.sp, color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("> Volume: ${volume.toInt()}%", color = TextSecondary, fontFamily = FontFamily.Monospace)
                        
                        val maxBars = 20
                        val activeBars = ((volume / 100f) * maxBars).toInt()
                        val asciiSlider = "[" + "█".repeat(activeBars) + "-".repeat(maxBars - activeBars) + "]"
                        
                        Text(
                            text = asciiSlider,
                            color = AccentRed,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                        volume = fraction * 100f
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, _ ->
                                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                        volume = fraction * 100f
                                    }
                                }
                        )
                    }

                    MenuButton("[ BACK ]") { screenState = MenuScreenState.MAIN }
                }
            }
        }
    }
}

@Composable
fun MenuButton(text: String, modifier: Modifier = Modifier, color: Color = Color.Transparent, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = if (color == Color.Transparent) TextPrimary else color,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    )
}

@Composable
fun ChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val display = if (selected) "[* $text *]" else "[  $text  ]"
    val color = if (selected) AccentRed else TextPrimary
    Text(
        text = display,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp)
    )
}

