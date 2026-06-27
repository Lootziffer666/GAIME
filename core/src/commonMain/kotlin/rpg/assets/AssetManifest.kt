package rpg.assets

/**
 * Compile-time asset registry — the "asset DB" that maps game-facing keys to drawable
 * resource names, frame metadata, source art-pack provenance, and scene definitions.
 *
 * All data is static and lives in the binary; no external DB is needed for this kind of
 * catalogue. Query via [sprites], [scenes], and the category helpers.
 *
 * Source art packs (assets/HD/):
 *   SWORDSMAN  — characters/swordsman          (Nib, Brugg)
 *   VAMPIRE    — characters/vampire             (Vellum)
 *   FIELD_FOE  — enemies/field-enemies          (Rat, Wolf)
 *   SLIME      — enemies/slimes                 (Blob)
 *   RPG_BOSS   — enemies/rpg-monsters           (Rat Accountant boss)
 *   GUILD_NPC  — locations/guild-hall           (Barkeep, Patron, Merchant, Guard, Citizens)
 *   DUNGEON    — tilesets/dungeon               (Tileset atlas)
 *   FANTASY_UI — ui/fantasy-icons               (Quest marker)
 *   RPG_UI     — ui/rpg-ui                      (UI panel)
 *   HEROES_HOME  — locations/heroes-home        (Tavern interior background)
 *   CHAPEL_LOC   — locations/chapel             (Boss arena / chapel backgrounds)
 *   GUILD_LOC    — locations/guild-hall         (Guild-hall exterior background)
 *   DUNGEON_LOC  — tilesets/dungeon             (Sewer background)
 *   GLASSBLOWERS — locations/glassblowers-workshop
 *   TEMPLE_LOC   — locations/ruined-temple
 *   BRIDGES_LOC  — locations/bridges
 */
object AssetManifest {

    enum class Category {
        HERO, ENEMY, BOSS,
        NPC_WORLD, NPC_PORTRAIT,
        TILE, BACKGROUND, UI
    }

    /**
     * @param key       Game-facing identifier used in spriteMap / entity definitions
     * @param drawable  Compose resource name (without "Res.drawable." prefix)
     * @param category  Broad asset category
     * @param frameW    Width of a single animation frame in pixels
     * @param frameH    Height of a single animation frame in pixels
     * @param idleFrames Number of idle animation frames (1 = static sprite)
     * @param sourcePack Art-pack origin path relative to assets/HD/
     */
    data class SpriteEntry(
        val key: String,
        val drawable: String,
        val category: Category,
        val frameW: Int,
        val frameH: Int,
        val idleFrames: Int = 1,
        val sourcePack: String = ""
    )

    val sprites: Map<String, SpriteEntry> = mapOf(
        // ── Heroes ──────────────────────────────────────────────────────────
        "hero_nib"   to SpriteEntry("hero_nib",   "hero_nib",   Category.HERO, 48, 48, 1, "characters/swordsman"),
        "hero_brugg" to SpriteEntry("hero_brugg", "hero_brugg", Category.HERO, 48, 48, 1, "characters/swordsman"),
        "hero_vellum" to SpriteEntry("hero_vellum", "hero_vellum", Category.HERO, 48, 48, 1, "characters/vampire"),

        // ── Enemies ─────────────────────────────────────────────────────────
        "enemy_rat"  to SpriteEntry("enemy_rat",  "enemy_rat",  Category.ENEMY, 48, 48, 1, "enemies/field-enemies"),
        "enemy_blob" to SpriteEntry("enemy_blob", "enemy_blob", Category.ENEMY, 48, 48, 1, "enemies/slimes"),
        "enemy_wolf" to SpriteEntry("enemy_wolf", "enemy_wolf", Category.ENEMY, 48, 48, 1, "enemies/field-enemies"),

        // ── Boss ────────────────────────────────────────────────────────────
        "boss_rat_accountant" to SpriteEntry(
            "boss_rat_accountant", "boss_rat_accountant", Category.BOSS, 96, 96, 1, "enemies/rpg-monsters"
        ),

        // ── NPC world sprites (48×48 game-world walkers) ────────────────────
        "npc_barkeep"  to SpriteEntry("npc_barkeep",  "npc_world_barkeep",  Category.NPC_WORLD, 48, 48, 1, "locations/guild-hall"),
        "npc_patron"   to SpriteEntry("npc_patron",   "npc_world_patron",   Category.NPC_WORLD, 48, 48, 1, "locations/guild-hall"),
        "npc_merchant" to SpriteEntry("npc_merchant", "npc_world_merchant", Category.NPC_WORLD, 48, 48, 1, "locations/guild-hall"),
        "npc_guard"    to SpriteEntry("npc_guard",    "npc_world_guard",    Category.NPC_WORLD, 48, 48, 1, "locations/guild-hall"),
        "npc_citizen1" to SpriteEntry("npc_citizen1", "npc_world_citizen1", Category.NPC_WORLD, 48, 48, 1, "locations/guild-hall"),
        "npc_citizen2" to SpriteEntry("npc_citizen2", "npc_world_citizen2", Category.NPC_WORLD, 48, 48, 1, "locations/guild-hall"),

        // ── NPC portrait busts (128×128 dialogue overlays) ──────────────────
        "portrait_barkeep"  to SpriteEntry("portrait_barkeep",  "npc_portrait_barkeep",  Category.NPC_PORTRAIT, 128, 128, 1, "locations/guild-hall"),
        "portrait_patron"   to SpriteEntry("portrait_patron",   "npc_portrait_patron",   Category.NPC_PORTRAIT, 128, 128, 1, "locations/guild-hall"),
        "portrait_guard"    to SpriteEntry("portrait_guard",    "npc_portrait_guard",    Category.NPC_PORTRAIT, 128, 128, 1, "locations/guild-hall"),
        "portrait_merchant" to SpriteEntry("portrait_merchant", "npc_portrait_merchant", Category.NPC_PORTRAIT, 128, 128, 1, "locations/guild-hall"),

        // ── Tiles ────────────────────────────────────────────────────────────
        "tile_floor"    to SpriteEntry("tile_floor",    "tile_floor",    Category.TILE, 48, 48, 1, "tilesets/dungeon"),
        "tile_wall"     to SpriteEntry("tile_wall",     "tile_wall",     Category.TILE, 48, 48, 1, "tilesets/dungeon"),
        "marker_quest"  to SpriteEntry("marker_quest",  "marker_quest",  Category.TILE, 48, 48, 1, "ui/fantasy-icons"),

        // ── UI ───────────────────────────────────────────────────────────────
        "ui_panel"      to SpriteEntry("ui_panel",      "ui_panel",      Category.UI, 48, 48, 1, "ui/rpg-ui"),
    )

    // ── Scene placement helpers ──────────────────────────────────────────────

    data class NpcPlacement(val entityKey: String, val spriteKey: String, val x: Int, val y: Int)
    data class EnemyPlacement(val entityKey: String, val spriteKey: String, val x: Int, val y: Int)

    /**
     * @param key         Scene identifier
     * @param background  Drawable resource name for the pre-rendered background PNG
     * @param atmosphere  SceneAtmosphere preset name: TAVERN | SEWER | BOSS | CHAPEL |
     *                    GUILD_HALL | MARKET | FOREST | BRIDGE
     * @param npcs        NPC entities present in this scene
     * @param enemies     Enemy entities present in this scene
     * @param connections Keys of scenes reachable from this one (logical adjacency graph)
     */
    data class SceneEntry(
        val key: String,
        val background: String,
        val atmosphere: String,
        val npcs: List<NpcPlacement> = emptyList(),
        val enemies: List<EnemyPlacement> = emptyList(),
        val connections: List<String> = emptyList()
    )

    val scenes: Map<String, SceneEntry> = mapOf(

        // ── Chapter 1 ────────────────────────────────────────────────────────
        "tavern" to SceneEntry(
            key = "tavern", background = "world_tavern", atmosphere = "TAVERN",
            npcs = listOf(
                NpcPlacement("barkeep",  "npc_barkeep",  13,  5),
                NpcPlacement("patron",   "npc_patron",    8, 14),
                NpcPlacement("citizen1", "npc_citizen1",  5,  9),
                NpcPlacement("citizen2", "npc_citizen2", 18, 11),
            ),
            connections = listOf("heroes_home_ext")
        ),
        "sewer" to SceneEntry(
            key = "sewer", background = "world_sewer", atmosphere = "SEWER",
            enemies = listOf(
                EnemyPlacement("rat_1",    "enemy_rat",   5, 12),
                EnemyPlacement("blob_mini","enemy_blob",  7, 15),
            ),
            connections = listOf("boss")
        ),
        "boss" to SceneEntry(
            key = "boss", background = "world_boss", atmosphere = "CHAPEL",
            enemies = listOf(
                EnemyPlacement("rat_accountant", "boss_rat_accountant", 12, 8),
            ),
            connections = listOf("chapel_ext")
        ),

        // ── Chapter 2 ────────────────────────────────────────────────────────
        "market" to SceneEntry(
            key = "market", background = "world_market", atmosphere = "MARKET",
            npcs = listOf(
                NpcPlacement("merchant",  "npc_merchant",  12, 10),
                NpcPlacement("guard",     "npc_guard",      6, 15),
                NpcPlacement("citizen1",  "npc_citizen1",  10,  7),
            ),
            connections = listOf("glassblowers_ext", "guildhall_ext")
        ),
        "forest" to SceneEntry(
            key = "forest", background = "world_forest", atmosphere = "FOREST",
            enemies = listOf(
                EnemyPlacement("wolf_1", "enemy_wolf", 16,  8),
                EnemyPlacement("wolf_2", "enemy_wolf", 18, 11),
                EnemyPlacement("wolf_3", "enemy_wolf", 20,  9),
            ),
            connections = listOf("temple_ext", "guildhall_ext")
        ),

        // ── World connectors ─────────────────────────────────────────────────
        "heroes_home_ext" to SceneEntry(
            key = "heroes_home_ext", background = "world_heroes_home_ext", atmosphere = "MARKET",
            npcs = listOf(
                NpcPlacement("villager1", "npc_citizen1",  8, 12),
                NpcPlacement("villager2", "npc_citizen2", 14,  9),
                NpcPlacement("merchant",  "npc_merchant",  6,  7),
            ),
            connections = listOf("tavern", "guildhall_ext")
        ),
        "guildhall_ext" to SceneEntry(
            key = "guildhall_ext", background = "world_guildhall_ext", atmosphere = "GUILD_HALL",
            npcs = listOf(
                NpcPlacement("guildmaster", "npc_guard",    10,  6),
                NpcPlacement("guard_post",  "npc_guard",     4, 10),
                NpcPlacement("citizen1",    "npc_citizen1", 14,  9),
            ),
            connections = listOf("heroes_home_ext", "market", "forest")
        ),
        "chapel_ext" to SceneEntry(
            key = "chapel_ext", background = "world_chapel_ext", atmosphere = "CHAPEL",
            npcs = listOf(
                NpcPlacement("chapel_guard",  "npc_guard",    15, 16),
                NpcPlacement("devotee1",      "npc_citizen1",  8, 12),
                NpcPlacement("devotee2",      "npc_citizen2", 22, 14),
            ),
            connections = listOf("boss")
        ),
        "temple_ext" to SceneEntry(
            key = "temple_ext", background = "world_temple_ext", atmosphere = "FOREST",
            enemies = listOf(
                EnemyPlacement("wolf_a", "enemy_wolf", 10, 8),
                EnemyPlacement("wolf_b", "enemy_wolf", 14, 5),
            ),
            connections = listOf("forest")
        ),
        "glassblowers_ext" to SceneEntry(
            key = "glassblowers_ext", background = "world_glassblowers_ext", atmosphere = "MARKET",
            npcs = listOf(
                NpcPlacement("glassblower",  "npc_merchant",  9,  7),
                NpcPlacement("apprentice",   "npc_citizen1", 13,  9),
            ),
            connections = listOf("market")
        ),
        "bridge" to SceneEntry(
            key = "bridge", background = "world_bridge", atmosphere = "BRIDGE",
            npcs = listOf(
                NpcPlacement("traveller1", "npc_citizen1", 20, 30),
                NpcPlacement("traveller2", "npc_citizen2", 30, 28),
            ),
            connections = listOf("heroes_home_ext", "market")
        ),
    )

    // ── Query helpers ────────────────────────────────────────────────────────

    fun spritesForCategory(cat: Category): List<SpriteEntry> =
        sprites.values.filter { it.category == cat }

    fun heroSprites(): List<SpriteEntry> = spritesForCategory(Category.HERO)
    fun enemySprites(): List<SpriteEntry> = spritesForCategory(Category.ENEMY) + spritesForCategory(Category.BOSS)
    fun npcWorldSprites(): List<SpriteEntry> = spritesForCategory(Category.NPC_WORLD)
    fun portraitSprites(): List<SpriteEntry> = spritesForCategory(Category.NPC_PORTRAIT)

    fun scenesWithAtmosphere(atmo: String): List<SceneEntry> =
        scenes.values.filter { it.atmosphere == atmo }
}
