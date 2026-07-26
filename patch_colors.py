import sys

with open('app/src/main/java/com/example/game/GameScreen.kt', 'r') as f:
    content = f.read()

# Building on map (around 326)
old_b_color = "val baseColor = if (building.isEnemy) AccentRedDark else Color(0xFF1E3A8A)"
new_b_color = """val isCom = state.playerFaction == "COM"
                val pColor = if (isCom) AccentRedDark else Color(0xFF1E3A8A)
                val eColor = if (isCom) Color(0xFF1E3A8A) else AccentRedDark
                val baseColor = if (building.isEnemy) eColor else pColor"""
content = content.replace(old_b_color, new_b_color)

# Unit on map (around 353)
old_u_color = "val baseColor = if (unit.isEnemy) AccentRed else Color(0xFF3B82F6)"
new_u_color = """val isCom = state.playerFaction == "COM"
                val pColor = if (isCom) AccentRed else Color(0xFF3B82F6)
                val eColor = if (isCom) Color(0xFF3B82F6) else AccentRed
                val baseColor = if (unit.isEnemy) eColor else pColor"""
content = content.replace(old_u_color, new_u_color)

# Building on minimap (around 616)
old_min_b = "val color = if (b.isEnemy) AccentRedDark else Color(0xFF3B82F6)"
new_min_b = """val isCom = state.playerFaction == "COM"
                val pColor = if (isCom) AccentRedDark else Color(0xFF3B82F6)
                val eColor = if (isCom) Color(0xFF3B82F6) else AccentRedDark
                val color = if (b.isEnemy) eColor else pColor"""
content = content.replace(old_min_b, new_min_b)

# Unit on minimap (around 624)
old_min_u = "val color = if (u.isEnemy) AccentRedDark else AccentGreen"
new_min_u = """val isCom = state.playerFaction == "COM"
                val pColor = if (isCom) AccentRedDark else Color(0xFF3B82F6)
                val eColor = if (isCom) Color(0xFF3B82F6) else AccentRedDark
                val color = if (u.isEnemy) eColor else pColor"""
content = content.replace(old_min_u, new_min_u)

with open('app/src/main/java/com/example/game/GameScreen.kt', 'w') as f:
    f.write(content)
