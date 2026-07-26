import sys

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_vars = """        var startingCredits by remember { mutableStateOf(5000) }
        var mapSize by remember { mutableStateOf(40) }
        var difficulty by remember { mutableStateOf(1) } // 0=Easy, 1=Normal, 2=Hard"""

new_vars = """        var startingCredits by remember { mutableStateOf(5000) }
        var mapSize by remember { mutableStateOf(40) }
        var difficulty by remember { mutableStateOf(1) }
        var playerFaction by remember { mutableStateOf("GDI") }"""

content = content.replace(old_vars, new_vars)

old_start = """                if (isPlaying) {
            GameScreen(startingCredits = startingCredits, mapSize = mapSize, difficulty = difficulty, onExit = { isPlaying = false })
        } else {
            MainMenuScreen(onStartGame = { credits, _, _, size, diff -> 
                startingCredits = credits
                mapSize = size
                difficulty = diff
                isPlaying = true 
            })
        }"""

new_start = """                if (isPlaying) {
            GameScreen(startingCredits = startingCredits, playerFaction = playerFaction, mapSize = mapSize, difficulty = difficulty, onExit = { isPlaying = false })
        } else {
            MainMenuScreen(onStartGame = { credits, faction, _, size, diff -> 
                startingCredits = credits
                playerFaction = faction
                mapSize = size
                difficulty = diff
                isPlaying = true 
            })
        }"""

content = content.replace(old_start, new_start)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
