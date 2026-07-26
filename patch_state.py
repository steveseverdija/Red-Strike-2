import sys

with open('app/src/main/java/com/example/game/Models.kt', 'r') as f:
    content = f.read()

content = content.replace("val difficulty: Int = 1, // 0 = Easy, 1 = Normal, 2 = Hard", "val difficulty: Int = 1,\n    val playerFaction: String = \"GDI\",")

with open('app/src/main/java/com/example/game/Models.kt', 'w') as f:
    f.write(content)
