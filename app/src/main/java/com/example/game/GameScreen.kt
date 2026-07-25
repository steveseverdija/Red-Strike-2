package com.example.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.R
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

@Composable
fun GameScreen(startingCredits: Int, onExit: () -> Unit, viewModel: GameViewModel = viewModel()) {
    val state by viewModel.gameState.collectAsState()
    val context = LocalContext.current
    
    val mediaPlayers = remember {
        Array(5) { 
            android.media.MediaPlayer.create(context, R.raw.cannon)
        }
    }
    val mpIndex = remember { intArrayOf(0) }
    
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayers.forEach { it?.release() }
        }
    }

    LaunchedEffect(state.fireSoundEventCount) {
        if (state.fireSoundEventCount > 0) {
            val idx = mpIndex[0]
            mediaPlayers[idx]?.seekTo(0)
            mediaPlayers[idx]?.start()
            mpIndex[0] = (idx + 1) % mediaPlayers.size
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initGame(startingCredits)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(BaseBackground)) {
            // Main Area (Map + Sidebar)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Map Area
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(MapBackground)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { offset ->
                                if (!state.isSelectionMode) {
                                    viewModel.handleTap(offset, Offset(size.width.toFloat(), size.height.toFloat()))
                                }
                            }
                        )
                    }
                    .pointerInput(state.isSelectionMode) {
                        if (state.isSelectionMode) {
                            detectDragGestures(
                                onDragStart = { offset -> viewModel.startSelectionBox(offset) },
                                onDrag = { change, _ -> change.consume(); viewModel.updateSelectionBox(change.position) },
                                onDragEnd = { viewModel.endSelectionBox() },
                                onDragCancel = { viewModel.endSelectionBox() }
                            )
                        } else {
                            detectTransformGestures { _, pan, zoom, _ ->
                                viewModel.handlePanAndZoom(pan, zoom)
                            }
                        }
                    }
            ) {
                val viewportSize = Offset(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
                GameCanvas(state)
                
                // HUD Overlay
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha=0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$${state.credits}", color = AccentAmber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("| PWR: [====  ]", color = AccentGreen, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                
                MinimapOverlay(
                    state = state,
                    viewportSize = viewportSize,
                    onMinimapTap = { worldPos ->
                        viewModel.centerCameraOnMap(worldPos, viewportSize)
                    },
                    modifier = Modifier.align(Alignment.BottomStart)
                )
                
                val placingType = state.placingBuildingType
                if (placingType != null) {
                    Text(
                        "TAP TO PLACE ${placingType.name}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha=0.7f), RoundedCornerShape(8.dp)).padding(16.dp)
                    )
                }
            }
            
            // Sidebar
            Sidebar(viewModel, state)
        }
        
        // Bottom Bar
        GameBottomBar(state, viewModel, onExit)
    }

    if (state.status != GameStatus.PLAYING) {
        GameOverOverlay(
            state = state,
            onRematch = { viewModel.initGame(startingCredits) },
            onMainMenu = onExit
        )
    }
}
}

@Composable
fun Sidebar(viewModel: GameViewModel, state: GameState) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .fillMaxHeight()
            .background(Color.Black)
            .border(1.dp, Color.DarkGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Build Queue Progress
        if (state.productionQueue.isNotEmpty()) {
            val item = state.productionQueue.first()
            val progress = 1f - (item.remainingTimeMs.toFloat() / item.totalTimeMs)
            val pBars = (progress * 10).toInt()
            val barStr = "=".repeat(pBars) + " ".repeat(10 - pBars)
            Text("[$barStr]", color = AccentGreen, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            Text("BUILDING", fontSize = 10.sp, color = Color.White, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 4.dp))
        } else {
            Spacer(modifier = Modifier.height(30.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Units
            BuildButton("INF", UnitType.INFANTRY.cost, AccentGreen) { viewModel.queueUnit(UnitType.INFANTRY) }
            BuildButton("HVY INF", UnitType.HEAVY_INFANTRY.cost, AccentGreen) { viewModel.queueUnit(UnitType.HEAVY_INFANTRY) }
            BuildButton("L.TANK", UnitType.LIGHT_TANK.cost, AccentAmber) { viewModel.queueUnit(UnitType.LIGHT_TANK) }
            BuildButton("H.TANK", UnitType.HEAVY_TANK.cost, AccentAmber) { viewModel.queueUnit(UnitType.HEAVY_TANK) }
            BuildButton("HARV", UnitType.HARVESTER.cost, Color.LightGray) { viewModel.queueUnit(UnitType.HARVESTER) }
            
            // Buildings
            BuildButton("BARRACK", BuildingType.BARRACKS.cost, AccentRed) { viewModel.queueBuilding(BuildingType.BARRACKS) }
            BuildButton("FACTORY", BuildingType.WAR_FACTORY.cost, AccentRed) { viewModel.queueBuilding(BuildingType.WAR_FACTORY) }
        }
    }
}

@Composable
fun BuildButton(name: String, cost: Int, color: Color, onClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clickable(onClick=onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("[$name]", color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text("$$cost", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun GameCanvas(state: GameState) {
    val textMeasurer = rememberTextMeasurer()
    val frame = (state.timeElapsedMs / 200).toInt()
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cam = state.cameraOffset
        val scale = state.cameraScale

        withTransform({
            translate(left = cam.x, top = cam.y)
            scale(scale, scale, Offset.Zero)
        }) {
            val gridTextResult = textMeasurer.measure(
                text = ".",
                style = TextStyle(color = Color(0xFF334155), fontSize = 10.sp)
            )

            // Grid
            val gridSize = 60f
            val viewWorldLeft = -cam.x / scale
            val viewWorldTop = -cam.y / scale
            val viewWorldRight = viewWorldLeft + size.width / scale
            val viewWorldBottom = viewWorldTop + size.height / scale

            val startTx = Math.floor((viewWorldLeft / gridSize).toDouble()).toInt() - 1
            val endTx = Math.ceil((viewWorldRight / gridSize).toDouble()).toInt() + 1
            val startTy = Math.floor((viewWorldTop / gridSize).toDouble()).toInt() - 1
            val endTy = Math.ceil((viewWorldBottom / gridSize).toDouble()).toInt() + 1

            val satelliteGridTextResult = textMeasurer.measure(
                text = "#",
                style = TextStyle(color = Color(0xFF0F172A), fontSize = 10.sp) // nearly black
            )

            for (tx in startTx..endTx) {
                for (ty in startTy..endTy) {
                    val wx = tx * gridSize
                    val wy = ty * gridSize
                    
                    if (tx < 0 || tx > 39 || ty < 0 || ty > 39) {
                        drawText(
                            textLayoutResult = satelliteGridTextResult,
                            topLeft = Offset(wx, wy)
                        )
                    } else {
                        drawText(
                            textLayoutResult = gridTextResult,
                            topLeft = Offset(wx, wy)
                        )
                        // Map boundary ascii
                        if (tx == 0 || tx == 39 || ty == 0 || ty == 39) {
                            val boundaryText = textMeasurer.measure(
                                text = "X",
                                style = TextStyle(color = Color(0xFF1E293B), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            )
                            drawText(
                                textLayoutResult = boundaryText,
                                topLeft = Offset(wx + gridSize/2 - boundaryText.size.width/2f, wy + gridSize/2 - boundaryText.size.height/2f)
                            )
                        }
                    }
                }
            }

            // Draw Terrain
            state.terrainMap.forEach { (coord, tile) ->
                val tileX = coord.first * gridSize
                val tileY = coord.second * gridSize
                // cull out of bounds
                if (tileX > (-cam.x / scale) - gridSize*2 && tileX < viewWorldRight + gridSize && tileY > (-cam.y / scale) - gridSize*2 && tileY < viewWorldBottom + gridSize) {
                    val tileResult = textMeasurer.measure(
                        text = tile.ascii,
                        style = TextStyle(color = Color(tile.color), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    )
                    drawText(
                        textLayoutResult = tileResult,
                        topLeft = Offset(tileX + gridSize/2 - tileResult.size.width/2f, tileY + gridSize/2 - tileResult.size.height/2f)
                    )
                }
            }

            // Draw Buildings
            state.buildings.forEach { building ->
                val isProducing = state.productionQueue.any { it.isBuilding && building.type == BuildingType.CONSTRUCTION_YARD || (!it.isBuilding && ((it.type as UnitType) == UnitType.INFANTRY || (it.type as UnitType) == UnitType.HEAVY_INFANTRY) && building.type == BuildingType.BARRACKS) || (!it.isBuilding && ((it.type as UnitType) == UnitType.LIGHT_TANK || (it.type as UnitType) == UnitType.HEAVY_TANK || (it.type as UnitType) == UnitType.HARVESTER) && building.type == BuildingType.WAR_FACTORY) }

                val ascii = when (building.type) {
                    BuildingType.WAR_FACTORY -> {
                        if (isProducing && frame % 2 == 0) "██████████████████████╗ \n██╔══════════════════██╗\n██║  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  ██║\n██║  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓  ██║\n██╚══════════════════██║\n╚══════════════════════╝"
                        else "██████████████████████╗ \n██╔══════════════════██╗\n██║  ░░░░░░░░░░░░░░  ██║\n██║  ░░░░░░░░░░░░░░  ██║\n██╚══════════════════██║\n╚══════════════════════╝"
                    }
                    BuildingType.BARRACKS -> {
                        if (isProducing && frame % 2 == 0) "██████╗ \n██╔══██╗\n██║▓▓██║\n██╚══██║\n╚══════╝"
                        else "██████╗ \n██╔══██╗\n██║░░██║\n██╚══██║\n╚══════╝"
                    }
                    BuildingType.CONSTRUCTION_YARD -> {
                        if (isProducing && frame % 2 == 0) "██████████╗ \n██╔══════██╗\n██║ ▓▓▓▓ ██║\n██║ ▓▓▓▓ ██║\n██╚══════██║\n╚══════════╝"
                        else "██████████╗ \n██╔══════██╗\n██║ ░░░░ ██║\n██║ ░░░░ ██║\n██╚══════██║\n╚══════════╝"
                    }
                    BuildingType.POWER_PLANT -> "██████╗ \n██╔══██╗\n██║████║\n██╚══██║\n╚══════╝"
                    else -> " [  ] "
                }

                val baseColor = if (building.isEnemy) AccentRedDark else Color(0xFF1E3A8A)
                
                // Background for readability
                drawRect(
                    color = Color.Black.copy(alpha=0.5f),
                    topLeft = Offset(building.position.x - building.type.width/2, building.position.y - building.type.height/2),
                    size = Size(building.type.width, building.type.height)
                )

                val textLayoutResult = textMeasurer.measure(
                    text = ascii,
                    style = TextStyle(color = baseColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                )
                
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(building.position.x - textLayoutResult.size.width / 2f, building.position.y - textLayoutResult.size.height / 2f)
                )

                // Health bar
                val hpRatio = building.health / 1000f
                drawRect(color = Color.Red, topLeft = Offset(building.position.x - building.type.width/2, building.position.y - building.type.height/2 - 6f), size = Size(building.type.width, 4f))
                drawRect(color = Color.Green, topLeft = Offset(building.position.x - building.type.width/2, building.position.y - building.type.height/2 - 6f), size = Size(building.type.width * hpRatio, 4f))
            }

            // Draw Units
            state.units.forEach { unit ->
                val baseColor = if (unit.isEnemy) AccentRed else Color(0xFF3B82F6)
                
                val isMoving = unit.targetPosition != null
                val isAttacking = unit.targetUnitId != null || unit.targetBuildingId != null
                
                val ascii = when (unit.type) {
                    UnitType.INFANTRY -> {
                        if (isAttacking) {
                            if (frame % 2 == 0) " 0 \n██═*\n╚═╝ " else " 0 \n██═ \n╚═╝ "
                        } else if (isMoving) {
                            if (frame % 2 == 0) " 0 \n██║\n╚═╝" else " 0 \n██╗\n╚═╝"
                        } else {
                            " 0 \n██╗\n╚═╝"
                        }
                    }
                    UnitType.HEAVY_INFANTRY -> {
                        if (isAttacking) {
                            if (frame % 2 == 0) " [0] \n████═*\n╚═══╝ " else " [0] \n████═ \n╚═══╝ "
                        } else if (isMoving) {
                            if (frame % 2 == 0) " [0] \n████║\n╚═══╝" else " [0] \n████╗\n╚═══╝"
                        } else {
                            " [0] \n████╗\n╚═══╝"
                        }
                    }
                    UnitType.HEAVY_TANK -> {
                        if (isAttacking && frame % 2 == 0) "██████╗ \n██╔▓▓██╗\n██║██══*\n██╚▓▓██║\n╚══════╝"
                        else if (isMoving && frame % 2 == 0) "██████╗ \n██╔░░██╗\n██║██══╝\n██╚░░██║\n╚══════╝"
                        else "██████╗ \n██╔▓▓██╗\n██║██══╝\n██╚▓▓██║\n╚══════╝"
                    }
                    UnitType.LIGHT_TANK -> {
                        if (isAttacking && frame % 2 == 0) "████╗ \n██╔▓██╗\n██║█═* \n██╚▓██║\n╚═════╝"
                        else if (isMoving && frame % 2 == 0) "████╗ \n██╔░██╗\n██║█═╝ \n██╚░██║\n╚═════╝"
                        else "████╗ \n██╔▓██╗\n██║█═╝ \n██╚▓██║\n╚═════╝"
                    }
                    UnitType.HARVESTER -> {
                        if (isMoving && frame % 2 == 0) "██████╗ \n██╔░░██╗\n██║░░██║\n██╚░░██║\n╚══════╝"
                        else "██████╗ \n██╔▓▓██╗\n██║▓▓██║\n██╚▓▓██║\n╚══════╝"
                    }
                }
                
                val fontSize = 8.sp
                val textLayoutResult = textMeasurer.measure(
                    text = ascii,
                    style = TextStyle(color = baseColor, fontSize = fontSize, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                )

                // Background
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(unit.position.x - textLayoutResult.size.width / 2f, unit.position.y - textLayoutResult.size.height / 2f),
                    size = Size(textLayoutResult.size.width.toFloat(), textLayoutResult.size.height.toFloat())
                )
                
                if (unit.type == UnitType.HEAVY_TANK || unit.type == UnitType.LIGHT_TANK || unit.type == UnitType.HARVESTER) {
                   rotate(degrees = unit.rotation, pivot = unit.position) {
                       drawText(
                           textLayoutResult = textLayoutResult,
                           topLeft = Offset(unit.position.x - textLayoutResult.size.width / 2f, unit.position.y - textLayoutResult.size.height / 2f)
                       )
                   }
                } else {
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(unit.position.x - textLayoutResult.size.width / 2f, unit.position.y - textLayoutResult.size.height / 2f)
                    )
                }

                if (unit.isSelected) {
                    drawRect(color = Color.White, topLeft = Offset(unit.position.x - unit.type.radius - 4f/scale, unit.position.y - unit.type.radius - 4f/scale), size = Size((unit.type.radius + 4f/scale)*2, (unit.type.radius + 4f/scale)*2), style = Stroke(width = 2f/scale))
                }
                
                if (unit.targetPosition != null && unit.isSelected) {
                    drawLine(color = Color.White.copy(alpha = 0.5f), start = unit.position, end = unit.targetPosition!!, strokeWidth = 1f/scale)
                }
            }

            // Draw Particles
            state.particles.forEach { p ->
                val chars = listOf("*", "#", "@", "%", "+", ".")
                val char = chars[((p.size.toInt() + frame) % chars.size + chars.size) % chars.size]
                val particleResult = textMeasurer.measure(
                    text = char,
                    style = TextStyle(color = Color(p.color).copy(alpha = p.life), fontSize = (10f + p.size).sp, fontFamily = FontFamily.Monospace)
                )
                drawText(
                    textLayoutResult = particleResult,
                    topLeft = Offset(p.position.x - p.size, p.position.y - p.size)
                )
            }

            // Draw Selection Box
            if (state.selectionBoxStart != null && state.selectionBoxCurrent != null) {
                val start = state.selectionBoxStart
                val current = state.selectionBoxCurrent
                val rectSize = Size(current.x - start.x, current.y - start.y)
                drawRect(color = AccentGreen.copy(alpha = 0.3f), topLeft = start, size = rectSize)
                drawRect(color = AccentGreen, topLeft = start, size = rectSize, style = Stroke(width = 1f/scale))
            }
        }
    }
}

@Composable
fun GameBottomBar(state: GameState, viewModel: GameViewModel, onExit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Black)
            .border(1.dp, Color.DarkGray)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val selText = if (state.isSelectionMode) "[*SELECT*]" else "[ SELECT ]"
        val selColor = if (state.isSelectionMode) AccentAmber else Color.White
        Text(selText, color = selColor, fontFamily = FontFamily.Monospace, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewModel.toggleSelectionMode() })
        
        Text("[ STOP ]", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 18.sp, modifier = Modifier.clickable { viewModel.stopSelectedUnits() })
        
        Text("[ EXIT ]", color = AccentRed, fontFamily = FontFamily.Monospace, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onExit))
    }
}

@Composable
fun GameOverOverlay(state: GameState, onRematch: () -> Unit, onMainMenu: () -> Unit) {
    val isVictory = state.status == GameStatus.VICTORY
    val titleText = if (isVictory) "VICTORY" else "DEFEAT"
    val titleColor = if (isVictory) AccentGreen else AccentRed

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(enabled = false) {}, // Intercept clicks
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(PanelBackgroundAlpha, RoundedCornerShape(24.dp))
                .border(2.dp, titleColor.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(48.dp)
        ) {
            Text(
                "=============\n|| $titleText ||\n=============",
                color = titleColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats
            val durationSecs = state.timeElapsedMs / 1000
            val mins = durationSecs / 60
            val secs = durationSecs % 60
            val timeStr = String.format("%02d:%02d", mins, secs)

            StatRow("Time Elapsed", timeStr)
            StatRow("Units Built", "${state.unitsBuilt}")
            StatRow("Enemies Destroyed", "${state.enemiesDestroyed}")
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(
                    "| MAIN MENU |", 
                    color = TextSecondary, 
                    fontFamily = FontFamily.Monospace, 
                    fontSize = 20.sp, 
                    modifier = Modifier.clickable(onClick = onMainMenu).padding(8.dp)
                )
                
                Text(
                    "| *REMATCH* |", 
                    color = titleColor, 
                    fontFamily = FontFamily.Monospace, 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    modifier = Modifier.clickable(onClick = onRematch).padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.width(300.dp).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 18.sp)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MinimapOverlay(
    state: GameState,
    viewportSize: Offset,
    onMinimapTap: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val mapSize = 2000f
    val minimapSizeDp = if (isExpanded) 200.dp else 100.dp
    
    Box(
        modifier = modifier
            .padding(16.dp)
            .size(minimapSizeDp)
            .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .border(2.dp, BorderColorSecondary, RoundedCornerShape(8.dp))
            .pointerInput(isExpanded) {
                detectTapGestures(
                    onTap = { offset ->
                        val fractionX = offset.x / size.width
                        val fractionY = offset.y / size.height
                        val worldX = fractionX * mapSize
                        val worldY = fractionY * mapSize
                        onMinimapTap(Offset(worldX, worldY))
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            val scale = size.width / mapSize
            
            // Draw camera viewport
            val camScale = state.cameraScale
            val camW = viewportSize.x / camScale
            val camH = viewportSize.y / camScale
            val camX = -state.cameraOffset.x / camScale
            val camY = -state.cameraOffset.y / camScale
            
            drawRect(
                color = Color.White.copy(alpha = 0.2f),
                topLeft = Offset(camX * scale, camY * scale),
                size = Size(camW * scale, camH * scale)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(camX * scale, camY * scale),
                size = Size(camW * scale, camH * scale),
                style = Stroke(width = 1f)
            )
            
            state.terrainMap.forEach { (coord, tile) ->
                val color = if (tile.type == TerrainType.MOUNTAIN) Color(0xFF4B5563) else Color(0xFF22C55E)
                drawRect(
                    color = color,
                    topLeft = Offset(coord.first * 60f * scale, coord.second * 60f * scale),
                    size = Size(60f * scale, 60f * scale)
                )
            }
            
            state.buildings.forEach { b ->
                val color = if (b.isEnemy) AccentRedDark else Color(0xFF3B82F6)
                drawRect(
                    color = color,
                    topLeft = Offset((b.position.x - b.type.width/2) * scale, (b.position.y - b.type.height/2) * scale),
                    size = Size(b.type.width * scale, b.type.height * scale)
                )
            }
            state.units.forEach { u ->
                val color = if (u.isEnemy) AccentRedDark else AccentGreen
                drawCircle(
                    color = color,
                    radius = maxOf(2f, u.type.radius * scale),
                    center = Offset(u.position.x * scale, u.position.y * scale)
                )
            }
        }
        
        Text(
            if (isExpanded) "[_]" else "[M]",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).clickable { isExpanded = !isExpanded }.background(Color.Black)
        )
    }
}
