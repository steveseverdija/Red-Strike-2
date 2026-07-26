import sys

with open('app/src/main/java/com/example/game/GameViewModel.kt', 'r') as f:
    content = f.read()

old_init = """    fun initGame(startingCredits: Int, mapSize: Int = 40, difficulty: Int = 1) {"""
new_init = """    fun initGame(startingCredits: Int, playerFaction: String = "GDI", mapSize: Int = 40, difficulty: Int = 1) {"""
content = content.replace(old_init, new_init)

old_spawn = """        // clear base areas
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
        spawnUnit(UnitType.HARVESTER, Offset(eBaseX, eBaseY + 100f), true)"""

new_spawn = """        // clear base areas
        for (x in 2..8) for (y in 2..8) newTerrainMap.remove(Pair(x, y))
        val eStartX = (mapSize - 10).coerceAtLeast(10)
        val eStartY = (mapSize - 10).coerceAtLeast(10)
        for (x in eStartX..(eStartX+8)) for (y in eStartY..(eStartY+8)) newTerrainMap.remove(Pair(x, y))

        _gameState.value = GameState(credits = startingCredits, terrainMap = newTerrainMap, mapSize = mapSize, difficulty = difficulty, playerFaction = playerFaction)
        
        val topLeftX = 300f
        val topLeftY = 300f
        val botRightX = eStartX * 60f + 100f
        val botRightY = eStartY * 60f + 100f
        
        val pBaseX = if (playerFaction == "NOD") botRightX else topLeftX
        val pBaseY = if (playerFaction == "NOD") botRightY else topLeftY
        val eBaseX = if (playerFaction == "NOD") topLeftX else botRightX
        val eBaseY = if (playerFaction == "NOD") topLeftY else botRightY
        
        // Spawn Player Base
        spawnBuilding(BuildingType.COMMAND, Offset(pBaseX, pBaseY), false)
        spawnUnit(UnitType.BUILDER, Offset(pBaseX + 50f, pBaseY + 50f), false)
        spawnUnit(UnitType.HARVESTER, Offset(pBaseX, pBaseY + 100f), false)

        // Spawn Enemy Base
        spawnBuilding(BuildingType.COMMAND, Offset(eBaseX, eBaseY), true)
        spawnBuilding(BuildingType.FACTORY, Offset(eBaseX - 100f, eBaseY), true)
        spawnUnit(UnitType.BUILDER, Offset(eBaseX - 50f, eBaseY - 50f), true)
        spawnUnit(UnitType.HARVESTER, Offset(eBaseX, eBaseY + 100f), true)"""

content = content.replace(old_spawn, new_spawn)

with open('app/src/main/java/com/example/game/GameViewModel.kt', 'w') as f:
    f.write(content)
