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

    // --- Chapter 2: Stokeport Market ---

    const val TRIGGER_MARKET_EXIT     = "market_exit"
    const val TRIGGER_FOREST_SHRINE   = "forest_shrine"
    const val TRIGGER_FOREST_BOSS     = "forest_boss"

    private val marketLegend = mapOf(
        '#' to TileSpec(atlasIndex = 14, blocked = true),
        '.' to TileSpec(atlasIndex = 48),
        '@' to TileSpec(atlasIndex = 48, spawn = true),
        'M' to TileSpec(atlasIndex = 48, trigger = "merchant_npc"),
        'G' to TileSpec(atlasIndex = 48, trigger = "guard_npc"),
        'E' to TileSpec(atlasIndex = 45, trigger = TRIGGER_MARKET_EXIT)
    )

    // 25 wide x 20 tall. Spawn at (2,2). Merchant at (12,10). Guard at (6,15). Exit on east.
    private val marketRows = listOf(
        "#########################",
        "#.......................#",
        "#.@.....................#",
        "#.......................#",
        "#...####...####...####..#",
        "#...#..#...#..#...#..#..#",
        "#...####...####...####..#",
        "#.......................#",
        "#.......................#",
        "#.......................#",
        "#...........M...........#",
        "#.......................#",
        "#.......................#",
        "#.......................#",
        "#.......................#",
        "#.....G.................#",
        "#.......................#",
        "#.......................#",
        "#.......................E",
        "#########################"
    )

    /** Stokeport Market: merchant NPC, guard NPC, exit to forest trail on east side. */
    fun stokeportMarket(): TileMap = TileMapParser.fromAscii(marketRows, marketLegend)

    // --- Chapter 2: Forest Trail ---

    private val forestLegend = mapOf(
        '#' to TileSpec(atlasIndex = 14, blocked = true),
        '.' to TileSpec(atlasIndex = 48),
        '@' to TileSpec(atlasIndex = 48, spawn = true),
        'H' to TileSpec(atlasIndex = 48, trigger = TRIGGER_FOREST_SHRINE),
        'B' to TileSpec(atlasIndex = 48, trigger = TRIGGER_FOREST_BOSS)
    )

    // 30 wide x 20 tall. Spawn at (1,10). Main path goes east. Branch north to shrine.
    // Boss area at east end. Page pickup near boss.
    private val forestRows = listOf(
        "##############################",
        "#........#..........##########",
        "#........#....H.....##########",
        "#........#..........##########",
        "#........#..........##########",
        "#........#...########........#",
        "#........#...#...............#",
        "#........#...#...............#",
        "#............#...............#",
        "#............#...............#",
        "#@...........................#",
        "#............#...............#",
        "#............#...............#",
        "#........#...#...............#",
        "#........#...#...............#",
        "#........#...########........#",
        "#........#..........#........#",
        "#........#..........#........#",
        "#........#..........#...B....#",
        "##############################"
    )

    /** Forest Trail: linear main path with shrine branch (north) and boss area (east). */
    fun forestTrail(): TileMap = TileMapParser.fromAscii(forestRows, forestLegend)

    // --- World connectors (open exterior maps) ---

    const val TRIGGER_GUILDHALL_ENTER   = "guildhall_enter"
    const val TRIGGER_CHAPEL_ENTER      = "chapel_enter"
    const val TRIGGER_VILLAGE_ENTER     = "village_enter"
    const val TRIGGER_TEMPLE_ENTER      = "temple_enter"
    const val TRIGGER_BRIDGE_EAST       = "bridge_east"
    const val TRIGGER_BRIDGE_WEST       = "bridge_west"
    const val TRIGGER_GLASSBLOWERS_ENTER = "glassblowers_enter"

    private val extLegend = mapOf(
        '#' to TileSpec(atlasIndex = 14, blocked = true),
        '.' to TileSpec(atlasIndex = 48),
        '@' to TileSpec(atlasIndex = 48, spawn = true),
        'G' to TileSpec(atlasIndex = 45, trigger = TRIGGER_GUILDHALL_ENTER),
        'C' to TileSpec(atlasIndex = 45, trigger = TRIGGER_CHAPEL_ENTER),
        'V' to TileSpec(atlasIndex = 45, trigger = TRIGGER_VILLAGE_ENTER),
        'T' to TileSpec(atlasIndex = 45, trigger = TRIGGER_TEMPLE_ENTER),
        'B' to TileSpec(atlasIndex = 45, trigger = TRIGGER_BRIDGE_EAST),
        'W' to TileSpec(atlasIndex = 45, trigger = TRIGGER_BRIDGE_WEST),
        'L' to TileSpec(atlasIndex = 45, trigger = TRIGGER_GLASSBLOWERS_ENTER)
    )

    // Heroes' home village exterior — 20×14 open yard. Spawn centre-left.
    // Door to tavern at (10,12); road to guild hall at east edge.
    private val heroesHomeExtRows = listOf(
        "####################",
        "#..................#",
        "#..................#",
        "#....@.............#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#..................#",
        "#.........V........#",
        "####################"
    )

    /** Heroes' home village exterior — approach the tavern door or head east. */
    fun heroesHomeExt(): TileMap = TileMapParser.fromAscii(heroesHomeExtRows, extLegend)

    // Guild-hall exterior — 16×12 courtyard. Spawn west; door north-centre.
    private val guildHallExtRows = listOf(
        "################",
        "########G#######",
        "#..............#",
        "#..............#",
        "#....@.........#",
        "#..............#",
        "#..............#",
        "#..............#",
        "#..............#",
        "#..............#",
        "#..............#",
        "################"
    )

    /** Adventurers' guild exterior — door at top leads inside. */
    fun guildHallExt(): TileMap = TileMapParser.fromAscii(guildHallExtRows, extLegend)

    // Chapel exterior — 22×16 courtyard. Spawn south; chapel door north-centre.
    private val chapelExtRows = listOf(
        "######################",
        "##########C###########",
        "#....................#",
        "#....................#",
        "#....................#",
        "#....................#",
        "#....................#",
        "#....................#",
        "#....................#",
        "#....................#",
        "#..........@.........#",
        "#....................#",
        "#....................#",
        "#....................#",
        "#....................#",
        "######################"
    )

    /** Chapel exterior — approach the chapel doors heading north. */
    fun chapelExt(): TileMap = TileMapParser.fromAscii(chapelExtRows, extLegend)

    // Ruined temple exterior — 18×14 overgrown clearing. Spawn east.
    private val templeExtRows = listOf(
        "##################",
        "#####T############",
        "#................#",
        "#................#",
        "#................#",
        "#................#",
        "#................#",
        "#................#",
        "#................#",
        "#...........@....#",
        "#................#",
        "#................#",
        "#................#",
        "##################"
    )

    /** Ruined temple exterior — wolves roam here; temple entrance to the north. */
    fun templeExt(): TileMap = TileMapParser.fromAscii(templeExtRows, extLegend)

    // Glassblowers' district exterior — 18×12 street.
    private val glassblowersExtRows = listOf(
        "##################",
        "#................#",
        "#....@...........#",
        "#................#",
        "#................#",
        "#.......L........#",
        "#................#",
        "#................#",
        "#................#",
        "#................#",
        "#................#",
        "##################"
    )

    /** Glassblowers' district exterior — shop entrance mid-row. */
    fun glassblowersExt(): TileMap = TileMapParser.fromAscii(glassblowersExtRows, extLegend)

    // Bridge — 35×18 stone bridge crossing. Spawn west; east exit on right.
    private val bridgeRows = listOf(
        "###################################",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "#.@...............................B",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "#.................................#",
        "W.................................#",
        "#.................................#",
        "#.................................#",
        "###################################"
    )

    /** Stone bridge overworld crossing — east/west exits connect regions. */
    fun bridge(): TileMap = TileMapParser.fromAscii(bridgeRows, extLegend)
}
