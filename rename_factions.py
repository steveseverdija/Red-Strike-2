import os

def replace_in_file(filepath, replacements):
    with open(filepath, 'r') as f:
        content = f.read()
    
    for old, new in replacements:
        content = content.replace(old, new)
        
    with open(filepath, 'w') as f:
        f.write(content)

replacements = [
    ('"GDI"', '"BTX"'),
    ('"NOD"', '"COM"'),
    ('playerFaction == "NOD"', 'playerFaction == "COM"'),
    ('playerFaction == "GDI"', 'playerFaction == "BTX"'),
]

replace_in_file('app/src/main/java/com/example/game/MainMenuScreen.kt', [
    ('"GDI"', '"BTX"'),
    ('"NOD"', '"COM"'),
    ('ChoiceButton("GDI"', 'ChoiceButton("BTX"'),
    ('ChoiceButton("NOD"', 'ChoiceButton("COM"'),
])

replace_in_file('app/src/main/java/com/example/game/Models.kt', [
    ('playerFaction: String = "GDI"', 'playerFaction: String = "BTX"'),
])

replace_in_file('app/src/main/java/com/example/MainActivity.kt', [
    ('playerFaction by remember { mutableStateOf("GDI") }', 'playerFaction by remember { mutableStateOf("BTX") }'),
])

replace_in_file('app/src/main/java/com/example/game/GameScreen.kt', [
    ('playerFaction: String = "GDI"', 'playerFaction: String = "BTX"'),
])

replace_in_file('app/src/main/java/com/example/game/GameViewModel.kt', [
    ('playerFaction: String = "GDI"', 'playerFaction: String = "BTX"'),
    ('playerFaction == "NOD"', 'playerFaction == "COM"'),
])
