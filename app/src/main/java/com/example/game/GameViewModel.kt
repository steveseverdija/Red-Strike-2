package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameViewModel : ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private val MAX_UNITS = 100

    fun initGame(startingCredits: Int) {
        val newTerrainMap = mutableMapOf<Pair<Int, Int>, TerrainTile>()
        for (x in 0..39) {
            for (y in 0..39) {
                val rand = Random.nextFloat()
                if (rand < 0.05f) {
                    val ascii = if (Random.nextBoolean()) " /\\ \n/  \\\n/    \\" else " /\\ \n/  \\"
                    newTerrainMap[Pair(x, y)] = TerrainTile(TerrainType.MOUNTAIN, ascii, 0xFF4B5563) // dark gray
                } else if (rand < 0.12f) {
                    val ascii = if (Random.nextBoolean()) " (  ) \n(    )\n  ||  " else " () \n )( \n || "
                    newTerrainMap[Pair(x, y)] = TerrainTile(TerrainType.TREE, ascii, 0xFF4D7C0F) // dark olive
                }
            }
        }
        
        // clear base areas
        for (x in 2..8) for (y in 2..8) newTerrainMap.remove(Pair(x, y))
        for (x in 20..28) for (y in 20..28) newTerrainMap.remove(Pair(x, y))

        _gameState.value = GameState(credits = startingCredits, terrainMap = newTerrainMap)
        
        // Spawn Player Base
        spawnBuilding(BuildingType.CONSTRUCTION_YARD, Offset(300f, 300f), false)
        spawnBuilding(BuildingType.BARRACKS, Offset(400f, 300f), false)
        spawnUnit(UnitType.INFANTRY, Offset(350f, 350f), false)

        // Spawn Enemy Base
        spawnBuilding(BuildingType.CONSTRUCTION_YARD, Offset(1500f, 1500f), true)
        spawnBuilding(BuildingType.WAR_FACTORY, Offset(1400f, 1500f), true)
        spawnUnit(UnitType.LIGHT_TANK, Offset(1450f, 1450f), true)
        
        startGameLoop()
    }

    private var gameLoopJob: kotlinx.coroutines.Job? = null

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                if (_gameState.value.status == GameStatus.PLAYING) {
                    updateGameLogic(16L) // 16ms
                    if (tick % 60 == 0) {
                        updateEnemyAI()
                    }
                }
                tick++
                delay(16) // ~60fps
            }
        }
    }

    private fun distSq(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun isCollidingWithBuilding(pos: Offset, radius: Float, buildings: List<GameBuilding>): Boolean {
        for (b in buildings) {
            val halfW = b.type.width / 2f
            val halfH = b.type.height / 2f
            val minX = b.position.x - halfW
            val maxX = b.position.x + halfW
            val minY = b.position.y - halfH
            val maxY = b.position.y + halfH

            val closestX = pos.x.coerceIn(minX, maxX)
            val closestY = pos.y.coerceIn(minY, maxY)

            val dx = pos.x - closestX
            val dy = pos.y - closestY
            if (dx * dx + dy * dy < radius * radius) {
                return true
            }
        }
        return false
    }

    private fun isCollidingWithMountain(pos: Offset, radius: Float, terrainMap: Map<Pair<Int, Int>, TerrainTile>): Boolean {
        val tx = (pos.x / 60f).toInt()
        val ty = (pos.y / 60f).toInt()
        for (dx in -1..1) {
            for (dy in -1..1) {
                val tile = terrainMap[Pair(tx + dx, ty + dy)]
                if (tile?.type == TerrainType.MOUNTAIN) {
                    val centerX = (tx + dx) * 60f + 30f
                    val centerY = (ty + dy) * 60f + 30f
                    val minX = centerX - 30f
                    val maxX = centerX + 30f
                    val minY = centerY - 30f
                    val maxY = centerY + 30f
                    
                    val closestX = pos.x.coerceIn(minX, maxX)
                    val closestY = pos.y.coerceIn(minY, maxY)
                    val diffX = pos.x - closestX
                    val diffY = pos.y - closestY
                    if (diffX * diffX + diffY * diffY < radius * radius) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun updateGameLogic(deltaMs: Long) {
        _gameState.update { state ->
            // Process production queue
            var newCredits = state.credits
            var newQueue = state.productionQueue.toMutableList()
            val unitsToSpawn = mutableListOf<UnitType>()
            val updatedTerrainMap = state.terrainMap.toMutableMap()
            
            if (newQueue.isNotEmpty()) {
                var barracksAvailable = state.buildings.count { it.type == BuildingType.BARRACKS && !it.isEnemy && it.health > 0 }
                var factoriesAvailable = state.buildings.count { it.type == BuildingType.WAR_FACTORY && !it.isEnemy && it.health > 0 }
                val itemsToRemove = mutableListOf<ProductionItem>()

                for (item in newQueue) {
                    if (item.isBuilding) {
                        item.remainingTimeMs -= deltaMs
                        if (item.remainingTimeMs <= 0) {
                            itemsToRemove.add(item)
                        }
                        break // Only process one building at a time for now? Or process if we want.
                    } else {
                        val type = item.type as UnitType
                        if ((type == UnitType.INFANTRY || type == UnitType.HEAVY_INFANTRY) && barracksAvailable > 0) {
                            barracksAvailable--
                            item.remainingTimeMs -= deltaMs
                            if (item.remainingTimeMs <= 0) {
                                itemsToRemove.add(item)
                                unitsToSpawn.add(type)
                            }
                        } else if ((type == UnitType.LIGHT_TANK || type == UnitType.HEAVY_TANK || type == UnitType.HARVESTER) && factoriesAvailable > 0) {
                            factoriesAvailable--
                            item.remainingTimeMs -= deltaMs
                            if (item.remainingTimeMs <= 0) {
                                itemsToRemove.add(item)
                                unitsToSpawn.add(type)
                            }
                        }
                    }
                }
                newQueue.removeAll(itemsToRemove)
            }

            val updatedUnits = state.units.toMutableList()
            val updatedBuildings = state.buildings.toMutableList()
            
            // Spawn newly built units
            unitsToSpawn.forEach { type ->
                if (updatedUnits.count { !it.isEnemy } < MAX_UNITS) {
                    val barrack = state.buildings.firstOrNull { it.type == BuildingType.BARRACKS && !it.isEnemy }
                    val factory = state.buildings.firstOrNull { it.type == BuildingType.WAR_FACTORY && !it.isEnemy }
                    val spawnBuilding = if (type == UnitType.INFANTRY || type == UnitType.HEAVY_INFANTRY) barrack else factory
                    
                    val spawnPos = spawnBuilding?.position?.plus(Offset(0f, 60f)) ?: Offset(300f, 300f)
                    updatedUnits.add(GameUnit(type = type, position = spawnPos, isEnemy = false))
                }
            }

            val currentTime = System.currentTimeMillis()
            val newParticles = mutableListOf<Particle>()
            var newFireSoundEventCount = 0

            for (i in updatedUnits.indices) {
                var unit = updatedUnits[i]
                if (unit.health <= 0) continue

                var targetEnemy: GameUnit? = null
                var targetBuilding: GameBuilding? = null

                // Auto-acquire target in range
                val rangeSq = unit.type.attackRange * unit.type.attackRange
                targetEnemy = updatedUnits.firstOrNull { it.isEnemy != unit.isEnemy && it.health > 0 && distSq(it.position, unit.position) <= rangeSq }
                
                if (targetEnemy == null) {
                    targetBuilding = updatedBuildings.firstOrNull { b -> 
                        if (b.isEnemy == unit.isEnemy || b.health <= 0) return@firstOrNull false
                        val closestX = unit.position.x.coerceIn(b.position.x - b.type.width / 2f, b.position.x + b.type.width / 2f)
                        val closestY = unit.position.y.coerceIn(b.position.y - b.type.height / 2f, b.position.y + b.type.height / 2f)
                        distSq(Offset(closestX, closestY), unit.position) <= rangeSq
                    }
                }

                var newTurretRotation = unit.turretRotation
                var newRotation = unit.rotation
                var fired = false

                if (targetEnemy != null) {
                    if (currentTime - unit.lastFireTimeMs >= unit.type.attackCooldownMs) {
                        unit.lastFireTimeMs = currentTime
                        fired = true
                        val enemyIndex = updatedUnits.indexOfFirst { it.id == targetEnemy!!.id }
                        if (enemyIndex != -1) {
                            val enemy = updatedUnits[enemyIndex]
                            updatedUnits[enemyIndex] = enemy.copy(health = enemy.health - unit.type.damage)
                        }
                    }
                    val targetAngle = atan2(targetEnemy!!.position.y - unit.position.y, targetEnemy!!.position.x - unit.position.x)
                    newTurretRotation = Math.toDegrees(targetAngle.toDouble()).toFloat()
                } else if (targetBuilding != null) {
                    if (currentTime - unit.lastFireTimeMs >= unit.type.attackCooldownMs) {
                        unit.lastFireTimeMs = currentTime
                        fired = true
                        val bIndex = updatedBuildings.indexOfFirst { it.id == targetBuilding!!.id }
                        if (bIndex != -1) {
                            val b = updatedBuildings[bIndex]
                            updatedBuildings[bIndex] = b.copy(health = b.health - unit.type.damage)
                        }
                    }
                    val targetAngle = atan2(targetBuilding!!.position.y - unit.position.y, targetBuilding!!.position.x - unit.position.x)
                    newTurretRotation = Math.toDegrees(targetAngle.toDouble()).toFloat()
                }

                if (fired) {
                    val angleRad = Math.toRadians(newTurretRotation.toDouble()).toFloat()
                    val pDx = cos(angleRad)
                    val pDy = sin(angleRad)
                    val nozzlePos = unit.position + Offset(pDx * unit.type.radius * 1.5f, pDy * unit.type.radius * 1.5f)
                    val pVelocity = Offset(pDx * 5f, pDy * 5f)
                    
                    newFireSoundEventCount++
                    newParticles.add(Particle(position = nozzlePos, velocity = pVelocity, color = 0xFFFDE047, size = 4f))
                }

                if (unit.type == UnitType.HARVESTER && !unit.isEnemy && unit.targetPosition == null) {
                    var closestDistSq = Float.MAX_VALUE
                    var closestTree: Pair<Int, Int>? = null
                    
                    for ((coord, tile) in updatedTerrainMap) {
                        if (tile.type == TerrainType.TREE) {
                            val centerX = coord.first * 60f + 30f
                            val centerY = coord.second * 60f + 30f
                            val dx = centerX - unit.position.x
                            val dy = centerY - unit.position.y
                            val dSq = dx * dx + dy * dy
                            if (dSq < closestDistSq) {
                                closestDistSq = dSq
                                closestTree = coord
                            }
                        }
                    }
                    if (closestTree != null) {
                        unit = unit.copy(targetPosition = Offset(closestTree.first * 60f + 30f, closestTree.second * 60f + 30f))
                    }
                }

                if (unit.targetPosition != null) {
                    val target = unit.targetPosition!!
                    val dx = target.x - unit.position.x
                    val dy = target.y - unit.position.y
                    val distanceSq = dx * dx + dy * dy
                    
                    if (distanceSq > unit.type.speed * unit.type.speed) {
                        val angle = atan2(dy, dx)
                        newRotation = Math.toDegrees(angle.toDouble()).toFloat()
                        if (targetEnemy == null && targetBuilding == null) {
                            newTurretRotation = newRotation
                        }
                        
                        val moveX = cos(angle) * unit.type.speed
                        val moveY = sin(angle) * unit.type.speed
                        
                        var newPosX = unit.position.x + moveX
                        var newPosY = unit.position.y + moveY
                        
                        val mapMax = 40f * 60f
                        newPosX = newPosX.coerceIn(0f, mapMax)
                        newPosY = newPosY.coerceIn(0f, mapMax)
                        
                        val collidesWithBuilding = isCollidingWithBuilding(Offset(newPosX, newPosY), unit.type.radius, updatedBuildings)
                        val collidesWithMountain = isCollidingWithMountain(Offset(newPosX, newPosY), unit.type.radius, updatedTerrainMap)
                        
                        if (collidesWithBuilding || collidesWithMountain) {
                            if (!isCollidingWithBuilding(Offset(newPosX, unit.position.y), unit.type.radius, updatedBuildings) && !isCollidingWithMountain(Offset(newPosX, unit.position.y), unit.type.radius, updatedTerrainMap)) {
                                newPosY = unit.position.y
                            } else if (!isCollidingWithBuilding(Offset(unit.position.x, newPosY), unit.type.radius, updatedBuildings) && !isCollidingWithMountain(Offset(unit.position.x, newPosY), unit.type.radius, updatedTerrainMap)) {
                                newPosX = unit.position.x
                            } else {
                                newPosX = unit.position.x
                                newPosY = unit.position.y
                            }
                        }
                        unit = unit.copy(position = Offset(newPosX, newPosY), rotation = newRotation, turretRotation = newTurretRotation)
                    } else {
                        unit = unit.copy(position = target, targetPosition = null, rotation = newRotation, turretRotation = newTurretRotation)
                    }
                } else {
                    unit = unit.copy(rotation = newRotation, turretRotation = newTurretRotation)
                }
                
                if (unit.type == UnitType.HARVESTER && !unit.isEnemy) {
                    val tx = (unit.position.x / 60f).toInt()
                    val ty = (unit.position.y / 60f).toInt()
                    var harvested = false
                    for (dx in -1..1) {
                        for (dy in -1..1) {
                            if (harvested) break
                            val key = Pair(tx + dx, ty + dy)
                            if (updatedTerrainMap[key]?.type == TerrainType.TREE) {
                                val centerX = (tx + dx) * 60f + 30f
                                val centerY = (ty + dy) * 60f + 30f
                                if (distSq(unit.position, Offset(centerX, centerY)) < 60f * 60f) {
                                    updatedTerrainMap.remove(key)
                                    newCredits += 50
                                    harvested = true
                                    
                                    newParticles.add(Particle(position = Offset(centerX, centerY), velocity = Offset(0f, -2f), color = 0xFF22C55E, size = 6f))
                                }
                            }
                        }
                    }
                }
                
                updatedUnits[i] = unit
            }
            
            // Simple unit collision resolution
            for (i in updatedUnits.indices) {
                if (updatedUnits[i].health <= 0) continue
                for (j in i + 1 until updatedUnits.size) {
                    if (updatedUnits[j].health <= 0) continue
                    val u1 = updatedUnits[i]
                    val u2 = updatedUnits[j]
                    val dx = u2.position.x - u1.position.x
                    val dy = u2.position.y - u1.position.y
                    val distSq = dx * dx + dy * dy
                    val minRadius = u1.type.radius + u2.type.radius
                    if (distSq > 0f && distSq < minRadius * minRadius) {
                        val dist = sqrt(distSq)
                        val overlap = minRadius - dist
                        val pushX = (dx / dist) * overlap * 0.5f
                        val pushY = (dy / dist) * overlap * 0.5f
                        
                        var newPos1X = u1.position.x - pushX
                        var newPos1Y = u1.position.y - pushY
                        var newPos2X = u2.position.x + pushX
                        var newPos2Y = u2.position.y + pushY
                        
                        if (!isCollidingWithBuilding(Offset(newPos1X, newPos1Y), u1.type.radius, updatedBuildings)) {
                            updatedUnits[i] = u1.copy(position = Offset(newPos1X, newPos1Y))
                        }
                        if (!isCollidingWithBuilding(Offset(newPos2X, newPos2Y), u2.type.radius, updatedBuildings)) {
                            updatedUnits[j] = u2.copy(position = Offset(newPos2X, newPos2Y))
                        }
                    }
                }
            }

            val deadEnemiesCount = updatedUnits.count { it.health <= 0 && it.isEnemy } + updatedBuildings.count { it.health <= 0 && it.isEnemy }

            // Remove dead
            updatedUnits.removeAll { it.health <= 0 }
            updatedBuildings.removeAll { it.health <= 0 }
            
            val updatedParticles = state.particles.map { 
                it.copy(
                    position = it.position + it.velocity,
                    life = it.life - (deltaMs / 200f) // particles last ~200ms
                )
            }.filter { it.life > 0f }.toMutableList()
            updatedParticles.addAll(newParticles)
            
            val enemiesDestroyed = state.enemiesDestroyed + deadEnemiesCount
            val timeElapsedMs = state.timeElapsedMs + deltaMs
            val unitsBuilt = state.unitsBuilt + unitsToSpawn.size

            var status = state.status
            if (status == GameStatus.PLAYING) {
                val hasEnemy = updatedUnits.any { it.isEnemy } || updatedBuildings.any { it.isEnemy }
                val hasPlayer = updatedUnits.any { !it.isEnemy } || updatedBuildings.any { !it.isEnemy }
                
                if (!hasEnemy) {
                    status = GameStatus.VICTORY
                } else if (!hasPlayer) {
                    status = GameStatus.DEFEAT
                }
            }

            state.copy(
                status = status,
                units = updatedUnits, 
                buildings = updatedBuildings, 
                productionQueue = newQueue,
                particles = updatedParticles,
                terrainMap = updatedTerrainMap,
                credits = newCredits,
                enemiesDestroyed = enemiesDestroyed,
                timeElapsedMs = timeElapsedMs,
                unitsBuilt = unitsBuilt,
                fireSoundEventCount = state.fireSoundEventCount + newFireSoundEventCount
            )
        }
    }

    private fun updateEnemyAI() {
        val state = _gameState.value
        val enemyUnits = state.units.filter { it.isEnemy }
        
        if (state.enemyCredits >= UnitType.LIGHT_TANK.cost && enemyUnits.size < 20) {
            _gameState.update { it.copy(enemyCredits = it.enemyCredits - UnitType.LIGHT_TANK.cost) }
            val spawnPos = state.buildings.firstOrNull { it.isEnemy && it.type == BuildingType.WAR_FACTORY }?.position ?: Offset(1500f, 1500f)
            spawnUnit(UnitType.LIGHT_TANK, spawnPos + Offset(0f, 60f), true)
        }
        
        if (enemyUnits.isNotEmpty() && Random.nextFloat() < 0.1f) {
            val playerTarget = state.buildings.firstOrNull { !it.isEnemy }?.position 
                ?: state.units.firstOrNull { !it.isEnemy }?.position
                ?: Offset(300f, 300f)
                
            _gameState.update { s ->
                val newUnits = s.units.map { 
                    if (it.isEnemy && it.targetPosition == null) {
                        val offsetX = Random.nextFloat() * 200f - 100f
                        val offsetY = Random.nextFloat() * 200f - 100f
                        it.copy(targetPosition = playerTarget + Offset(offsetX, offsetY))
                    } else it 
                }
                s.copy(units = newUnits)
            }
        }
    }

    fun handlePanAndZoom(pan: Offset, zoom: Float) {
        _gameState.update { 
            val newScale = (it.cameraScale * zoom).coerceIn(0.5f, 3f)
            it.copy(cameraOffset = it.cameraOffset + pan, cameraScale = newScale)
        }
    }

    fun handleTap(screenPos: Offset, viewportSize: Offset) {
        val state = _gameState.value
        val worldPos = (screenPos - state.cameraOffset) / state.cameraScale

        if (state.placingBuildingType != null) {
            val type = state.placingBuildingType
            // Check if placing overlaps with any existing building
            val halfW = type.width / 2f
            val halfH = type.height / 2f
            val minX1 = worldPos.x - halfW
            val maxX1 = worldPos.x + halfW
            val minY1 = worldPos.y - halfH
            val maxY1 = worldPos.y + halfH
            
            var overlaps = false
            for (b in state.buildings) {
                val bHalfW = b.type.width / 2f
                val bHalfH = b.type.height / 2f
                val minX2 = b.position.x - bHalfW
                val maxX2 = b.position.x + bHalfW
                val minY2 = b.position.y - bHalfH
                val maxY2 = b.position.y + bHalfH
                
                if (minX1 < maxX2 && maxX1 > minX2 && minY1 < maxY2 && maxY1 > minY2) {
                    overlaps = true
                    break
                }
            }

            if (!overlaps && state.credits >= type.cost) {
                _gameState.update { it.copy(credits = it.credits - type.cost, placingBuildingType = null) }
                spawnBuilding(type, worldPos, false)
            } else if (overlaps) {
                // If it overlaps, maybe we just cancel placing or allow them to tap somewhere else?
                // The request says "Buildings cannot build on another building". 
                // Let's just do nothing on tap so they have to tap a valid spot.
                return
            }
            return
        }

        val hitRadius = 40f / state.cameraScale
        val tappedUnit = state.units.find { distSq(it.position, worldPos) <= (it.type.radius + hitRadius) * (it.type.radius + hitRadius) }

        if (tappedUnit != null) {
            if (!tappedUnit.isEnemy) {
                _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = it.id == tappedUnit.id) }) }
            } else {
                commandSelectedToMove(worldPos)
            }
        } else {
            val tappedBuilding = state.buildings.find {
                val dx = worldPos.x - it.position.x
                val dy = worldPos.y - it.position.y
                dx > -(it.type.width/2 + hitRadius) && dx < (it.type.width/2 + hitRadius) && dy > -(it.type.height/2 + hitRadius) && dy < (it.type.height/2 + hitRadius)
            }
            if (tappedBuilding != null && tappedBuilding.isEnemy) {
                commandSelectedToMove(worldPos)
                _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = false) }) }
            } else {
                // Tapped ground
                val hasSelected = state.units.any { it.isSelected }
                if (hasSelected) {
                    commandSelectedToMove(worldPos)
                    _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = false) }) }
                } else {
                    // Touch elsewhere when no units selected: do nothing or ensure deselect
                    _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = false) }) }
                }
            }
        }
    }
    
    private fun commandSelectedToMove(target: Offset) {
        _gameState.update { state ->
            val updated = state.units.map { 
                if (it.isSelected && !it.isEnemy) {
                    val offsetX = Random.nextFloat() * 40f - 20f
                    val offsetY = Random.nextFloat() * 40f - 20f
                    it.copy(targetPosition = target + Offset(offsetX, offsetY))
                } else it
            }
            state.copy(units = updated)
        }
    }

    fun spawnUnit(type: UnitType, position: Offset, isEnemy: Boolean) {
        _gameState.update { s ->
            if (s.units.count { it.isEnemy == isEnemy } >= MAX_UNITS) return@update s
            val newUnits = s.units.toMutableList()
            newUnits.add(GameUnit(type = type, position = position, isEnemy = isEnemy))
            s.copy(units = newUnits)
        }
    }

    fun spawnBuilding(type: BuildingType, position: Offset, isEnemy: Boolean) {
        _gameState.update { s ->
            val newBuildings = s.buildings.toMutableList()
            newBuildings.add(GameBuilding(type = type, position = position, isEnemy = isEnemy))
            s.copy(buildings = newBuildings)
        }
    }
    
    fun queueUnit(type: UnitType) {
        val state = _gameState.value
        if (state.credits >= type.cost) {
            _gameState.update { 
                val newQueue = it.productionQueue.toMutableList()
                newQueue.add(ProductionItem(type = type, isBuilding = false, totalTimeMs = type.buildTimeMs, remainingTimeMs = type.buildTimeMs))
                it.copy(credits = it.credits - type.cost, productionQueue = newQueue) 
            }
        }
    }

    fun queueBuilding(type: BuildingType) {
        val state = _gameState.value
        if (state.credits >= type.cost) {
            _gameState.update { 
                it.copy(placingBuildingType = type) // Directly place for now
            }
        }
    }

    fun toggleSelectionMode() {
        _gameState.update { it.copy(isSelectionMode = !it.isSelectionMode) }
    }

    fun stopSelectedUnits() {
        _gameState.update { state ->
            val updated = state.units.map { 
                if (it.isSelected && !it.isEnemy) {
                    it.copy(targetPosition = null)
                } else it
            }
            state.copy(units = updated)
        }
    }

    fun startSelectionBox(screenPos: Offset) {
        val state = _gameState.value
        val worldPos = (screenPos - state.cameraOffset) / state.cameraScale
        _gameState.update { it.copy(selectionBoxStart = worldPos, selectionBoxCurrent = worldPos) }
    }

    fun updateSelectionBox(screenPos: Offset) {
        val state = _gameState.value
        val worldPos = (screenPos - state.cameraOffset) / state.cameraScale
        _gameState.update { it.copy(selectionBoxCurrent = worldPos) }
    }

    fun endSelectionBox() {
        _gameState.update { state ->
            val start = state.selectionBoxStart ?: return@update state
            val end = state.selectionBoxCurrent ?: return@update state
            
            val minX = minOf(start.x, end.x)
            val maxX = maxOf(start.x, end.x)
            val minY = minOf(start.y, end.y)
            val maxY = maxOf(start.y, end.y)

            val updatedUnits = state.units.map { unit ->
                if (!unit.isEnemy) {
                    val inBox = unit.position.x in minX..maxX && unit.position.y in minY..maxY
                    unit.copy(isSelected = inBox)
                } else unit
            }

            state.copy(
                units = updatedUnits,
                selectionBoxStart = null,
                selectionBoxCurrent = null,
                isSelectionMode = false
            )
        }
    }

    fun centerCameraOnMap(worldPos: Offset, viewportSize: Offset) {
        _gameState.update {
            val newOffset = Offset(viewportSize.x / 2f, viewportSize.y / 2f) - worldPos * it.cameraScale
            it.copy(cameraOffset = newOffset)
        }
    }
}
