import sys

with open('app/src/main/java/com/example/game/GameScreen.kt', 'r') as f:
    content = f.read()

old_sig = "fun GameScreen(startingCredits: Int, mapSize: Int = 40, difficulty: Int = 1, onExit: () -> Unit, viewModel: GameViewModel = viewModel()) {"
new_sig = "fun GameScreen(startingCredits: Int, playerFaction: String = \"GDI\", mapSize: Int = 40, difficulty: Int = 1, onExit: () -> Unit, viewModel: GameViewModel = viewModel()) {"

content = content.replace(old_sig, new_sig)

old_init = "    LaunchedEffect(Unit) { viewModel.initGame(startingCredits, mapSize, difficulty) }"
new_init = "    LaunchedEffect(Unit) { viewModel.initGame(startingCredits, playerFaction, mapSize, difficulty) }"

content = content.replace(old_init, new_init)

with open('app/src/main/java/com/example/game/GameScreen.kt', 'w') as f:
    f.write(content)
