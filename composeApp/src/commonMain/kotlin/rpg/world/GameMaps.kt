package rpg.world

/**
 * Hand-authored slice maps. Tile atlas indices refer to the Kenney tiny-dungeon
 * tileset (12-column atlas): 48 = floor, 14 = brick wall, 45 = cellar door.
 */
object GameMaps {

    private val tavernLegend = mapOf(
        '#' to TileSpec(atlasIndex = 14, blocked = true),
        '.' to TileSpec(atlasIndex = 48),
        '@' to TileSpec(atlasIndex = 48, spawn = true),
        'D' to TileSpec(atlasIndex = 45, trigger = TRIGGER_CELLAR_DOOR)
    )

    private val tavernRows = listOf(
        "#############",
        "#...........#",
        "#...........#",
        "#....@......#",
        "#...........#",
        "#....D......#",
        "#...........#",
        "#...........#",
        "#############"
    )

    /** The Limping Cockatrice tavern: walk to the cellar door to trigger a bark. */
    fun tavern(): TileMap = TileMapParser.fromAscii(tavernRows, tavernLegend)

    const val TRIGGER_CELLAR_DOOR = "cellar_door"
}
