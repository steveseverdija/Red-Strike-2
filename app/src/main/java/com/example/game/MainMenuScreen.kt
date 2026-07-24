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
import com.example.R
import com.example.ui.theme.*

enum class MenuScreenState { MAIN, LOBBY, SETTINGS }

@Composable
fun MainMenuScreen(onStartGame: (Int, String, String) -> Unit) {
    var screenState by remember { mutableStateOf(MenuScreenState.MAIN) }

    // Lobby State
    var selectedFaction by remember { mutableStateOf("GDI") }
    var selectedMap by remember { mutableStateOf("Desert") }
    var startingResources by remember { mutableStateOf(5000) }

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
                        ChoiceButton("Desert", selectedMap == "Desert") { selectedMap = "Desert" }
                        ChoiceButton("Forest", selectedMap == "Forest") { selectedMap = "Forest" }
                    }

                    Text("> Starting Resources: $$startingResources", color = TextSecondary, fontFamily = FontFamily.Monospace)
                    Slider(
                        value = startingResources.toFloat(),
                        onValueChange = { startingResources = it.toInt() },
                        valueRange = 1000f..10000f,
                        steps = 8
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        MenuButton("[ BACK ]", modifier = Modifier.weight(1f)) { screenState = MenuScreenState.MAIN }
                        Spacer(modifier = Modifier.width(16.dp))
                        MenuButton("[ START ]", modifier = Modifier.weight(1f), color = AccentRedDark) { 
                            onStartGame(startingResources, selectedFaction, selectedMap) 
                        }
                    }
                }
            }
            MenuScreenState.SETTINGS -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text("---[ SETTINGS ]---", fontSize = 24.sp, color = TextPrimary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Volume: [======    ]", color = TextSecondary, fontFamily = FontFamily.Monospace)
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

