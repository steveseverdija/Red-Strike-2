package com.example.game

import androidx.compose.ui.geometry.Offset
import java.util.UUID

enum class UnitType(val speed: Float, val radius: Float, val color: Long, val cost: Int, val buildTimeMs: Long, val maxHealth: Float, val damage: Float, val attackRange: Float, val attackCooldownMs: Long) {
    INFANTRY(2f, 8f, 0xFF4ADE80, 100, 2000L, 50f, 10f, 60f, 1000L),
    HEAVY_INFANTRY(1.5f, 10f, 0xFF4ADE80, 200, 3000L, 100f, 25f, 80f, 1500L),
    LIGHT_TANK(3.5f, 15f, 0xFFFBBF24, 300, 4000L, 200f, 40f, 120f, 2000L),
    HEAVY_TANK(2.5f, 20f, 0xFFFBBF24, 600, 6000L, 400f, 80f, 150f, 2500L),
    HARVESTER(2f, 18f, 0xFF94A3B8, 500, 5000L, 300f, 0f, 0f, 1000L)
}

enum class BuildingType(val width: Float, val height: Float, val color: Long, val cost: Int, val buildTimeMs: Long) {
    CONSTRUCTION_YARD(60f, 60f, 0xFF3E0000, 0, 0L),
    BARRACKS(40f, 40f, 0xFF1E293B, 300, 3000L),
    WAR_FACTORY(120f, 65f, 0xFF1E293B, 800, 6000L),
    POWER_PLANT(40f, 40f, 0xFF1E293B, 200, 2000L)
}

data class GameBuilding(
    val id: String = UUID.randomUUID().toString(),
    val type: BuildingType,
    val position: Offset,
    val isEnemy: Boolean = false,
    var health: Float = 1000f,
    var isSelected: Boolean = false
)

data class GameUnit(
    val id: String = UUID.randomUUID().toString(),
    var position: Offset,
    var targetPosition: Offset? = null,
    var targetUnitId: String? = null,
    var targetBuildingId: String? = null,
    val type: UnitType,
    var isSelected: Boolean = false,
    val isEnemy: Boolean = false,
    var health: Float = type.maxHealth,
    var lastFireTimeMs: Long = 0L,
    var rotation: Float = 0f,
    var turretRotation: Float = 0f
)

data class ProductionItem(
    val id: String = UUID.randomUUID().toString(),
    val type: Any, // UnitType or BuildingType
    val isBuilding: Boolean,
    val totalTimeMs: Long,
    var remainingTimeMs: Long
)

enum class GameStatus { PLAYING, VICTORY, DEFEAT }

enum class TerrainType { EMPTY, TREE, MOUNTAIN }

data class TerrainTile(
    val type: TerrainType,
    val ascii: String,
    val color: Long
)

data class Particle(
    val id: String = UUID.randomUUID().toString(),
    var position: Offset,
    var velocity: Offset,
    val color: Long,
    val size: Float,
    var life: Float = 1f // 1f down to 0f
)

data class GameState(
    val status: GameStatus = GameStatus.PLAYING,
    val unitsBuilt: Int = 0,
    val enemiesDestroyed: Int = 0,
    val timeElapsedMs: Long = 0L,
    val credits: Int = 1000,
    val enemyCredits: Int = 1000,
    val units: List<GameUnit> = emptyList(),
    val buildings: List<GameBuilding> = emptyList(),
    val productionQueue: List<ProductionItem> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val terrainMap: Map<Pair<Int, Int>, TerrainTile> = emptyMap(),
    val cameraOffset: Offset = Offset.Zero,
    val cameraScale: Float = 1f,
    val selectionBoxStart: Offset? = null,
    val selectionBoxCurrent: Offset? = null,
    val isSelectionMode: Boolean = false,
    val placingBuildingType: BuildingType? = null,
    val fireSoundEventCount: Int = 0
)
