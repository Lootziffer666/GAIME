package rpg.world

/**
 * HD worlds whose visuals are a pre-rendered background image baked from an
 * artist-authored Tiled scene (scripts/tmx_render.py), with collision and the
 * game's logical elements (spawn, triggers, entity slots) "translated" onto the
 * new layout. The background PNG lives in composeApp composeResources/drawable
 * (e.g. world_tavern.png); here we only model collision + spawn + triggers so
 * the pure movement/trigger logic stays unit-testable and renderer-agnostic.
 *
 * Collision grids are emitted by the bake tool ('#' = blocked, '.' = walkable)
 * and embedded verbatim, so they match the image pixel-for-pixel.
 */
object BakedMaps {

    // --- The Limping Cockatrice (tavern interior) ---
    // Baked from assets/HD/locations/heroes-home/Tiled_files/Interior1.tmx
    // 26x22 tiles. Walkable interior cols 2..23, rows 2..18; a counter/table
    // block sits at (16-17,9-10) and (19-20,9-10).
    private val tavernGrid = listOf(
        "##########################",
        "##########################",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##..............##.##...##",
        "##..............##.##...##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##......................##",
        "##########################",
        "##########################",
        "##########################"
    )

    /**
     * The tavern: spawn near the lower-centre, walk up to the cellar trapdoor to
     * descend into the sewer (same [GameMaps.TRIGGER_CELLAR_DOOR] as before, so
     * the host's transition logic is unchanged).
     */
    fun tavern(): TileMap = TileMapParser.fromCollision(
        rows = tavernGrid,
        spawnX = 12,
        spawnY = 17,
        triggers = mapOf((12 to 4) to GameMaps.TRIGGER_CELLAR_DOOR)
    )

    /** Suggested NPC slots on the tavern floor (walkable, away from the counter). */
    val TAVERN_BARKEEP_X = 19
    val TAVERN_BARKEEP_Y = 12
    val TAVERN_PATRON_X = 6
    val TAVERN_PATRON_Y = 14
}
