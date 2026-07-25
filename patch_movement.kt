                        val wasCollidingBuilding = isCollidingWithBuilding(unit.position, unit.type.radius, updatedBuildings)
                        val wasCollidingMountain = isCollidingWithMountain(unit.position, unit.type.radius, updatedTerrainMap)

                        val collidesWithBuilding = isCollidingWithBuilding(Offset(newPosX, newPosY), unit.type.radius, updatedBuildings)
                        val collidesWithMountain = isCollidingWithMountain(Offset(newPosX, newPosY), unit.type.radius, updatedTerrainMap)
                        
                        val stuckInBuilding = collidesWithBuilding && !wasCollidingBuilding
                        val stuckInMountain = collidesWithMountain && !wasCollidingMountain

                        if (stuckInBuilding || stuckInMountain) {
                            if (!isCollidingWithBuilding(Offset(newPosX, unit.position.y), unit.type.radius, updatedBuildings) && !isCollidingWithMountain(Offset(newPosX, unit.position.y), unit.type.radius, updatedTerrainMap)) {
                                newPosY = unit.position.y
                            } else if (!isCollidingWithBuilding(Offset(unit.position.x, newPosY), unit.type.radius, updatedBuildings) && !isCollidingWithMountain(Offset(unit.position.x, newPosY), unit.type.radius, updatedTerrainMap)) {
                                newPosX = unit.position.x
                            } else {
                                newPosX = unit.position.x
                                newPosY = unit.position.y
                            }
                        }
