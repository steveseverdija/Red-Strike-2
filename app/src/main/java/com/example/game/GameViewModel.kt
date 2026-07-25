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

    fun initGame(startingCredits: Int, mapSize: Int = 40, difficulty: Int = 1) {
        val newTerrainMap = mutableMapOf<Pair<Int, Int>, TerrainTile>()
        for (x in 0 until mapSize) {
            for (y in 0 until mapSize) {
                val rand = Random.nextFloat()
                if (rand < 0.05f) {
                    val mountains = listOf(
                        "   ██████╗   \n ██╔════██╗ \n██╔╝    ╚██╗\n██║      ██║\n╚═╝      ╚═╝",
                        "  ████████╗ \n ██╔════██╗\n██╔╝    ╚██╗\n██║      ██║\n╚═╝      ╚═╝",
                        "    ████╗   \n  ██╔══██╗  \n ██╔╝  ╚██╗ \n██╔╝    ╚██╗\n╚═╝      ╚═╝",
                        "  ████╗██╗  \n ██╔═██╔██╗ \n██╔╝ ╚═╝╚██╗\n██║      ██║\n╚═╝      ╚═╝"
                    )
                    val ascii = mountains.random()
                    newTerrainMap[Pair(x, y)] = TerrainTile(TerrainType.MOUNTAIN, ascii, 0xFF4B5563) // dark gray
                } else if (rand < 0.12f) {
                    val trees = listOf(
                        "  ████╗  \n ██╔═██╗ \n ██║ ██║ \n ╚████╔╝ \n  ╚═══╝  ",
                        "  ████╗  \n██╔══██╗\n╚██╗██╔╝\n  ████║  \n  ╚═══╝  ",
                        "   ██╗   \n ██╔██╗ \n██╔╝╚██╗\n╚═████╔╝\n  ╚═══╝  ",
                        "  ████╗  \n ██╔═██╗ \n██╔╝ ╚██╗\n╚██████╔╝\n ╚═════╝ "
                    )
                    val ascii = trees.random()
                    newTerrainMap[Pair(x, y)] = TerrainTile(TerrainType.TREE, ascii, 0xFF064E3B) // dark green
                }
            }
        }
        
        // clear base areas
        for (x in 2..8) for (y in 2..8) newTerrainMap.remove(Pair(x, y))
        val eStartX = (mapSize - 10).coerceAtLeast(10)
        val eStartY = (mapSize - 10).coerceAtLeast(10)
        for (x in eStartX..(eStartX+8)) for (y in eStartY..(eStartY+8)) newTerrainMap.remove(Pair(x, y))

        _gameState.value = GameState(credits = startingCredits, terrainMap = newTerrainMap, mapSize = mapSize, difficulty = difficulty)
        
        // Spawn Player Base
        spawnBuilding(BuildingType.COMMAND, Offset(300f, 300f), false)
        
        spawnUnit(UnitType.BUILDER, Offset(350f, 350f), false)
        spawnUnit(UnitType.HARVESTER, Offset(300f, 400f), false)

        // Spawn Enemy Base
        val eBaseX = eStartX * 60f + 100f
        val eBaseY = eStartY * 60f + 100f
        spawnBuilding(BuildingType.COMMAND, Offset(eBaseX, eBaseY), true)
        spawnBuilding(BuildingType.FACTORY, Offset(eBaseX - 100f, eBaseY), true)
        spawnUnit(UnitType.BUILDER, Offset(eBaseX - 50f, eBaseY - 50f), true)
        spawnUnit(UnitType.HARVESTER, Offset(eBaseX, eBaseY + 100f), true)
        
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

    private fun isCollidingWithBuilding(pos: Offset, radius: Float, buildings: List<GameBuilding>, isFlying: Boolean = false): Boolean {
        if (isFlying) return false
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

    private fun isCollidingWithMountain(pos: Offset, radius: Float, terrainMap: Map<Pair<Int, Int>, TerrainTile>, isFlying: Boolean = false): Boolean {
        if (isFlying) return false
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
                var commandBuildingsAvailable = state.buildings.count { it.type == BuildingType.COMMAND && !it.isEnemy && it.health > 0 }
                var factoriesAvailable = state.buildings.count { it.type == BuildingType.FACTORY && !it.isEnemy && it.health > 0 }
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
                        if ((type == UnitType.SF_SOLDIER || type == UnitType.BUILDER) && commandBuildingsAvailable > 0) {
                            commandBuildingsAvailable--
                            item.remainingTimeMs -= deltaMs
                            if (item.remainingTimeMs <= 0) {
                                itemsToRemove.add(item)
                                unitsToSpawn.add(type)
                            }
                        } else if ((type == UnitType.L_A_V || type == UnitType.TANK || type == UnitType.HARVESTER || type == UnitType.DRONE) && factoriesAvailable > 0) {
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
                    val cmd = state.buildings.firstOrNull { it.type == BuildingType.COMMAND && !it.isEnemy && it.isSelected } ?: state.buildings.firstOrNull { it.type == BuildingType.COMMAND && !it.isEnemy }
                    val factory = state.buildings.firstOrNull { it.type == BuildingType.FACTORY && !it.isEnemy && it.isSelected } ?: state.buildings.firstOrNull { it.type == BuildingType.FACTORY && !it.isEnemy }
                    val spawnBuilding = if (type == UnitType.SF_SOLDIER || type == UnitType.BUILDER) cmd else factory
                    
                    var validPos: Offset? = null
                    if (spawnBuilding != null) {
                        for (r in listOf(60f, 80f, 100f)) {
                            for (angle in 0 until 360 step 45) {
                                val rad = Math.toRadians(angle.toDouble())
                                val testPos = spawnBuilding.position + Offset(r * kotlin.math.cos(rad).toFloat(), r * kotlin.math.sin(rad).toFloat())
                                if (!isCollidingWithBuilding(testPos, type.radius, updatedBuildings, type.isFlying) && !isCollidingWithMountain(testPos, type.radius, updatedTerrainMap, type.isFlying)) {
                                    validPos = testPos
                                    break
                                }
                            }
                            if (validPos != null) break
                        }
                    }
                    val spawnPos = validPos ?: (spawnBuilding?.position?.plus(Offset(0f, 60f)) ?: Offset(300f, 300f))
                    var targetPos: Offset? = null
                    var path: List<Offset> = emptyList()
                    if (spawnBuilding?.rallyPoint != null) {
                        targetPos = spawnBuilding.rallyPoint
                        val p = findPath(spawnPos, targetPos!!, updatedTerrainMap, updatedBuildings, state.mapSize, type.isFlying)
                        targetPos = p.firstOrNull()
                        path = if (p.isNotEmpty()) p.drop(1) else emptyList()
                    }
                    updatedUnits.add(GameUnit(type = type, position = spawnPos, isEnemy = false, targetPosition = targetPos, path = path))
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
                targetEnemy = updatedUnits.firstOrNull { it.isEnemy != unit.isEnemy && it.health > 0 && distSq(it.position, unit.position) <= rangeSq && !((unit.type == UnitType.TANK || unit.type == UnitType.L_A_V) && it.type.isFlying) }
                
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

                if (unit.type == UnitType.HARVESTER && !unit.isEnemy && unit.targetPosition == null && unit.path.isEmpty()) {
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
                        val target = Offset(closestTree.first * 60f + 30f, closestTree.second * 60f + 30f)
                        val p = findPath(unit.position, target, updatedTerrainMap, updatedBuildings, state.mapSize, unit.type.isFlying)
                        val nextTarget = p.firstOrNull()
                        val remainingPath = if (p.isNotEmpty()) p.drop(1) else emptyList()
                        unit = unit.copy(targetPosition = nextTarget, path = remainingPath)
                    }
                }

                if (unit.targetPosition != null) {
                    val target = unit.targetPosition!!
                    val dx = target.x - unit.position.x
                    val dy = target.y - unit.position.y
                    val distanceSq = dx * dx + dy * dy
                    
                    val reachThresholdSq = if (unit.path.isNotEmpty()) 400f else (unit.type.speed * unit.type.speed)
                    if (distanceSq > reachThresholdSq) {
                        val angle = atan2(dy, dx)
                        newRotation = Math.toDegrees(angle.toDouble()).toFloat()
                        if (targetEnemy == null && targetBuilding == null) {
                            newTurretRotation = newRotation
                        }
                        
                        val moveX = cos(angle) * unit.type.speed
                        val moveY = sin(angle) * unit.type.speed
                        
                        var newPosX = unit.position.x + moveX
                        var newPosY = unit.position.y + moveY
                        
                        val mapMax = state.mapSize * 60f
                        newPosX = newPosX.coerceIn(0f, mapMax)
                        newPosY = newPosY.coerceIn(0f, mapMax)
                        
                        val wasCollidingBuilding = isCollidingWithBuilding(unit.position, unit.type.radius, updatedBuildings, unit.type.isFlying)
                        val wasCollidingMountain = isCollidingWithMountain(unit.position, unit.type.radius, updatedTerrainMap, unit.type.isFlying)

                        val collidesWithBuilding = isCollidingWithBuilding(Offset(newPosX, newPosY), unit.type.radius, updatedBuildings, unit.type.isFlying)
                        val collidesWithMountain = isCollidingWithMountain(Offset(newPosX, newPosY), unit.type.radius, updatedTerrainMap, unit.type.isFlying)
                        
                        val hitBuilding = collidesWithBuilding && !wasCollidingBuilding
                        val hitMountain = collidesWithMountain && !wasCollidingMountain

                        if (hitBuilding || hitMountain) {
                            val newXHitBuilding = isCollidingWithBuilding(Offset(newPosX, unit.position.y), unit.type.radius, updatedBuildings, unit.type.isFlying) && !wasCollidingBuilding
                            val newXHitMountain = isCollidingWithMountain(Offset(newPosX, unit.position.y), unit.type.radius, updatedTerrainMap, unit.type.isFlying) && !wasCollidingMountain
                            val newYHitBuilding = isCollidingWithBuilding(Offset(unit.position.x, newPosY), unit.type.radius, updatedBuildings, unit.type.isFlying) && !wasCollidingBuilding
                            val newYHitMountain = isCollidingWithMountain(Offset(unit.position.x, newPosY), unit.type.radius, updatedTerrainMap, unit.type.isFlying) && !wasCollidingMountain

                            if (!newXHitBuilding && !newXHitMountain) {
                                newPosY = unit.position.y
                            } else if (!newYHitBuilding && !newYHitMountain) {
                                newPosX = unit.position.x
                            } else {
                                newPosX = unit.position.x
                                newPosY = unit.position.y
                            }
                        }
                        
                        var currentStuckTime = unit.stuckTimeMs
                        if (newPosX == unit.position.x && newPosY == unit.position.y) {
                            currentStuckTime += deltaMs
                            val nextTarget = unit.path.firstOrNull()
                            val remainingPath = if (unit.path.isNotEmpty()) unit.path.drop(1) else emptyList()
                            if (currentStuckTime > 1000L && unit.type == UnitType.HARVESTER && !unit.isEnemy) {
                                val randomOffsetX = kotlin.random.Random.nextFloat() * 200f - 100f
                                val randomOffsetY = kotlin.random.Random.nextFloat() * 200f - 100f
                                val escapeTarget = Offset((unit.position.x + randomOffsetX).coerceIn(0f, mapMax), (unit.position.y + randomOffsetY).coerceIn(0f, mapMax))
                                unit = unit.copy(targetPosition = escapeTarget, path = emptyList(), stuckTimeMs = 0L, rotation = newRotation, turretRotation = newTurretRotation)
                            } else if (currentStuckTime > 500L) {
                                if (nextTarget == null) {
                                    unit = unit.copy(targetPosition = null, path = emptyList(), stuckTimeMs = 0L, rotation = newRotation, turretRotation = newTurretRotation)
                                } else {
                                    unit = unit.copy(targetPosition = nextTarget, path = remainingPath, stuckTimeMs = 0L, rotation = newRotation, turretRotation = newTurretRotation)
                                }
                            } else {
                                unit = unit.copy(stuckTimeMs = currentStuckTime, rotation = newRotation, turretRotation = newTurretRotation)
                            }
                        } else {
                            unit = unit.copy(position = Offset(newPosX, newPosY), stuckTimeMs = 0L, rotation = newRotation, turretRotation = newTurretRotation)
                        }
                    } else {
                        val nextTarget = unit.path.firstOrNull()
                        val remainingPath = if (unit.path.isNotEmpty()) unit.path.drop(1) else emptyList()
                        unit = unit.copy(position = if (unit.path.isEmpty()) target else unit.position, targetPosition = nextTarget, path = remainingPath, rotation = newRotation, turretRotation = newTurretRotation)
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
            
            // Simple unit collision resolution - do a few passes for stability
            for (pass in 0 until 3) {
                for (i in updatedUnits.indices) {
                    if (updatedUnits[i].health <= 0) continue
                    for (j in i + 1 until updatedUnits.size) {
                        if (updatedUnits[j].health <= 0) continue
                        val u1 = updatedUnits[i]
                        val u2 = updatedUnits[j]
                        if (u1.type.isFlying != u2.type.isFlying) continue
                        var dx = u2.position.x - u1.position.x
                        var dy = u2.position.y - u1.position.y
                        if (dx == 0f && dy == 0f) {
                            dx = kotlin.random.Random.nextFloat() - 0.5f
                            dy = kotlin.random.Random.nextFloat() - 0.5f
                        }
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
                            
                            if (!isCollidingWithBuilding(Offset(newPos1X, newPos1Y), u1.type.radius, updatedBuildings, u1.type.isFlying) && !isCollidingWithMountain(Offset(newPos1X, newPos1Y), u1.type.radius, updatedTerrainMap, u1.type.isFlying)) {
                                updatedUnits[i] = u1.copy(position = Offset(newPos1X, newPos1Y))
                            }
                            if (!isCollidingWithBuilding(Offset(newPos2X, newPos2Y), u2.type.radius, updatedBuildings, u2.type.isFlying) && !isCollidingWithMountain(Offset(newPos2X, newPos2Y), u2.type.radius, updatedTerrainMap, u2.type.isFlying)) {
                                updatedUnits[j] = u2.copy(position = Offset(newPos2X, newPos2Y))
                            }
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
        
        if (state.enemyCredits >= UnitType.L_A_V.cost && enemyUnits.size < 20) {
            _gameState.update { it.copy(enemyCredits = it.enemyCredits - UnitType.L_A_V.cost) }
            val spawnPos = state.buildings.firstOrNull { it.isEnemy && it.type == BuildingType.FACTORY }?.position ?: Offset(state.mapSize * 60f - 300f, state.mapSize * 60f - 300f)
            spawnUnit(UnitType.L_A_V, spawnPos + Offset(0f, 60f), true)
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

            if (!overlaps) {
                for (u in state.units) {
                    if (u.health <= 0 || u.type.isFlying) continue
                    val closestX = u.position.x.coerceIn(minX1, maxX1)
                    val closestY = u.position.y.coerceIn(minY1, maxY1)
                    val dx = u.position.x - closestX
                    val dy = u.position.y - closestY
                    if (dx * dx + dy * dy < u.type.radius * u.type.radius) {
                        overlaps = true
                        break
                    }
                }
            }
            
            if (!overlaps) {
                val txMin = (minX1 / 60f).toInt()
                val txMax = (maxX1 / 60f).toInt()
                val tyMin = (minY1 / 60f).toInt()
                val tyMax = (maxY1 / 60f).toInt()
                for (tx in txMin..txMax) {
                    for (ty in tyMin..tyMax) {
                        if (state.terrainMap[Pair(tx, ty)] != null) {
                            overlaps = true
                            break
                        }
                    }
                    if (overlaps) break
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
                _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = it.id == tappedUnit.id) }, buildings = s.buildings.map { it.copy(isSelected = false) }) }
            } else {
                commandSelectedToMove(worldPos)
            }
        } else {
            val tappedBuilding = state.buildings.find {
                val dx = worldPos.x - it.position.x
                val dy = worldPos.y - it.position.y
                dx > -(it.type.width/2 + hitRadius) && dx < (it.type.width/2 + hitRadius) && dy > -(it.type.height/2 + hitRadius) && dy < (it.type.height/2 + hitRadius)
            }
            if (tappedBuilding != null) {
                if (tappedBuilding.isEnemy) {
                    commandSelectedToMove(worldPos)
                    _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = false) }, buildings = s.buildings.map { it.copy(isSelected = false) }) }
                } else {
                    _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = false) }, buildings = s.buildings.map { it.copy(isSelected = it.id == tappedBuilding.id) }) }
                }
            } else {
                // Tapped ground
                val hasSelectedUnit = state.units.any { it.isSelected }
                val hasSelectedBuilding = state.buildings.any { it.isSelected && !it.isEnemy && (it.type == BuildingType.FACTORY || it.type == BuildingType.COMMAND) }
                if (hasSelectedUnit) {
                    commandSelectedToMove(worldPos)
                    _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = false) }, buildings = s.buildings.map { it.copy(isSelected = false) }) }
                } else if (hasSelectedBuilding) {
                    _gameState.update { s -> s.copy(buildings = s.buildings.map { if (it.isSelected) it.copy(rallyPoint = worldPos) else it }) }
                } else {
                    _gameState.update { s -> s.copy(units = s.units.map { it.copy(isSelected = false) }, buildings = s.buildings.map { it.copy(isSelected = false) }) }
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
                    val t = target + Offset(offsetX, offsetY)
                    val p = findPath(it.position, t, state.terrainMap, state.buildings, state.mapSize, it.type.isFlying)
                    val nextTarget = p.firstOrNull()
                    val remainingPath = if (p.isNotEmpty()) p.drop(1) else emptyList()
                    it.copy(targetPosition = nextTarget, path = remainingPath)
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

data class AStarNode(val x: Int, val y: Int, var g: Float = Float.MAX_VALUE, var h: Float = 0f, var parent: AStarNode? = null) {
    val f: Float get() = g + h
}

fun findPath(
    startPos: Offset, 
    targetPos: Offset, 
    terrainMap: Map<Pair<Int, Int>, TerrainTile>, 
    buildings: List<GameBuilding>,
    mapSize: Int,
    isFlying: Boolean = false
): List<Offset> {
    if (isFlying) return listOf(targetPos)
    val maxCoord = mapSize - 1
    val startX = (startPos.x / 60f).toInt().coerceIn(0, maxCoord)
    val startY = (startPos.y / 60f).toInt().coerceIn(0, maxCoord)
    val targetX = (targetPos.x / 60f).toInt().coerceIn(0, maxCoord)
    val targetY = (targetPos.y / 60f).toInt().coerceIn(0, maxCoord)
    
    if (startX == targetX && startY == targetY) return listOf(targetPos)
    
    val openSet = java.util.PriorityQueue<AStarNode>(compareBy { it.f })
    val closedSet = mutableSetOf<Pair<Int, Int>>()
    val nodes = mutableMapOf<Pair<Int, Int>, AStarNode>()
    
    val startNode = AStarNode(startX, startY, 0f, distanceAStar(startX, startY, targetX, targetY))
    openSet.add(startNode)
    nodes[Pair(startX, startY)] = startNode
    
    val obstacles = Array(mapSize) { BooleanArray(mapSize) }
    for ((coord, tile) in terrainMap) {
        if (tile.type == TerrainType.MOUNTAIN) {
            if (coord.first in 0..maxCoord && coord.second in 0..maxCoord) {
                obstacles[coord.first][coord.second] = true
            }
        }
    }
    for (b in buildings) {
        val halfW = b.type.width / 2f
        val halfH = b.type.height / 2f
        val minX = ((b.position.x - halfW) / 60f).toInt()
        val maxX = ((b.position.x + halfW) / 60f).toInt()
        val minY = ((b.position.y - halfH) / 60f).toInt()
        val maxY = ((b.position.y + halfH) / 60f).toInt()
        for (cx in minX..maxX) {
            for (cy in minY..maxY) {
                if (cx in 0..maxCoord && cy in 0..maxCoord) obstacles[cx][cy] = true
            }
        }
    }
    
    obstacles[targetX][targetY] = false
    
    while (openSet.isNotEmpty()) {
        val current = openSet.poll()!!
        if (current.x == targetX && current.y == targetY) {
            val path = mutableListOf<Offset>()
            var curr: AStarNode? = current
            while (curr != null) {
                if (curr.x == targetX && curr.y == targetY) {
                    path.add(0, targetPos)
                } else if (curr.x == startX && curr.y == startY) {
                    // skip
                } else {
                    path.add(0, Offset(curr.x * 60f + 30f, curr.y * 60f + 30f))
                }
                curr = curr.parent
            }
            return path
        }
        
        closedSet.add(Pair(current.x, current.y))
        
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                
                val nx = current.x + dx
                val ny = current.y + dy
                
                if (nx !in 0..maxCoord || ny !in 0..maxCoord) continue
                if (obstacles[nx][ny]) continue
                if (closedSet.contains(Pair(nx, ny))) continue
                
                if (dx != 0 && dy != 0) {
                    if (obstacles[current.x + dx][current.y] || obstacles[current.x][current.y + dy]) continue
                }
                
                val cost = current.g + if (dx == 0 || dy == 0) 1f else 1.414f
                val neighborNode = nodes.getOrPut(Pair(nx, ny)) { AStarNode(nx, ny) }
                
                if (cost < neighborNode.g) {
                    neighborNode.g = cost
                    neighborNode.h = distanceAStar(nx, ny, targetX, targetY)
                    neighborNode.parent = current
                    
                    if (!openSet.contains(neighborNode)) {
                        openSet.add(neighborNode)
                    } else {
                        openSet.remove(neighborNode)
                        openSet.add(neighborNode)
                    }
                }
            }
        }
    }
    return emptyList()
}

private fun distanceAStar(x1: Int, y1: Int, x2: Int, y2: Int): Float {
    val dx = (x1 - x2).toFloat()
    val dy = (y1 - y2).toFloat()
    return kotlin.math.sqrt(dx * dx + dy * dy)
}
