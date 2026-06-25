package rpg.world

/**
 * Hand-authored slice maps. Tile atlas indices refer to the Kenney tiny-dungeon
 * tileset (12-column atlas): 48 = floor, 14 = brick wall, 45 = cellar door.
 */
object GameMaps {

    // --- Trigger IDs ---

    const val TRIGGER_CELLAR_DOOR   = "cellar_door"
    const val TRIGGER_SEWER_EXIT    = "sewer_exit"
    const val TRIGGER_RUBBLE        = "rubble"
    const val TRIGGER_VELLUM_PUZZLE = "vellum_puzzle"
    const val TRIGGER_PAGE_PICKUP   = "page_pickup"
    const val TRIGGER_BOSS_EXIT     = "boss_exit"

    // --- Tavern ---

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

    // --- Sewer ---

    private val sewerLegend = mapOf(
        '#' to TileSpec(atlasIndex = 14, blocked = true),
        '.' to TileSpec(atlasIndex = 48),
        '@' to TileSpec(atlasIndex = 48, spawn = true),
        // Rubble: wall-like, blocked by default; cleared by BRUGG_ATTACK utility bark.
        'R' to TileSpec(atlasIndex = 14, blocked = true, trigger = TRIGGER_RUBBLE),
        // Vellum puzzle tile: stepping here auto-fires VELLUM_KNOWLEDGE_IS_THE_ANSWER.
        'V' to TileSpec(atlasIndex = 48, trigger = TRIGGER_VELLUM_PUZZLE),
        'S' to TileSpec(atlasIndex = 45, trigger = TRIGGER_SEWER_EXIT)
    )

    // 15 wide × 22 tall. Spawn at (7,2).
    // Corridor (rows 0-8): two rat entities pre-placed by host at (7,5) and (7,6).
    // Row 8: rubble wall — only passable after BRUGG_ATTACK clears it.
    // Mini-dungeon (rows 9-21): vellum puzzle tile at (7,12); four enemy entities
    // at (7,13)-(7,16) pre-placed by host; sewer exit at (7,20).
    private val sewerRows = listOf(
        "###############",
        "#.............#",
        "#......@......#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#######R#######",
        "#.............#",
        "#.............#",
        "#.............#",
        "#......V......#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#.............#",
        "#......S......#",
        "###############"
    )

    /** Sewers of Bad Decisions: corridor + branching room, enemies placed by host. */
    fun sewer(): TileMap = TileMapParser.fromAscii(sewerRows, sewerLegend)

    // --- Boss Room ---

    private val bossLegend = mapOf(
        '#' to TileSpec(atlasIndex = 14, blocked = true),
        '.' to TileSpec(atlasIndex = 48),
        '@' to TileSpec(atlasIndex = 48, spawn = true),
        // Chokepoint gap tile (same as floor; entry only through here from north half).
        'P' to TileSpec(atlasIndex = 48, trigger = TRIGGER_PAGE_PICKUP),
        'X' to TileSpec(atlasIndex = 45, trigger = TRIGGER_BOSS_EXIT)
    )

    // 13 wide × 14 tall. Spawn at (6,2).
    // Chokepoint wall at row 5 with single gap at (6,5) — the only path south.
    // Boss entity pre-placed at (6,7) by host; blocks access to page until defeated.
    // Page pickup at (6,10); boss exit door at (6,12).
    private val bossRows = listOf(
        "#############",
        "#...........#",
        "#.....@.....#",
        "#...........#",
        "#...........#",
        "######.######",
        "#...........#",
        "#...........#",
        "#...........#",
        "#...........#",
        "#.....P.....#",
        "#...........#",
        "#.....X.....#",
        "#############"
    )

    /** The Rat Accountant's arena: chokepoint wall forces engagement, page pickup post-boss. */
    fun bossRoom(): TileMap = TileMapParser.fromAscii(bossRows, bossLegend)
}
