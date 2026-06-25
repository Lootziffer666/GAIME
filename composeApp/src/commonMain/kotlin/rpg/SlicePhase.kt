package rpg

/** Tracks the narrative progression through the M3 vertical slice. */
enum class SlicePhase {
    INTRO_CUTSCENE,      // Opening text box before tavern
    TAVERN,              // Free-roam tavern
    NPC_DIALOGUE,        // NPC dialogue overlay active in the tavern
    FALLING_CUTSCENE,    // Cellar floor gives way
    SEWER,               // Free-roam sewer corridor + mini-dungeon
    SEWER_COMBAT,        // Corridor rat encounter
    MINI_DUNGEON_COMBAT, // Three rats + sludge blob encounter
    BOSS_ROOM,           // Free-roam boss arena
    BOSS_COMBAT,         // Rat Accountant three-phase fight
    POST_BOSS,           // Boss defeated, page pickup dialogue
    QUESTBOOK_FULL,      // Full-screen Questbook overlay (party name reveal)
    RETURN_CUTSCENE,     // Return to tavern dialogue
    VICTORY,             // End screen

    // ═══ Chapter 2 ═══════════════════════════════════════════════════════
    CHAPTER2_MARKET,           // Free-roam Stokeport Market
    CHAPTER2_MARKET_NPC,       // NPC dialogue overlay in market
    CHAPTER2_FOREST,           // Free-roam forest trail
    CHAPTER2_FOREST_COMBAT,    // Forest wolf encounter
    CHAPTER2_SHRINE,           // Lightning shrine activation
    CHAPTER2_BOSS_INTRO,       // Tax Collector Badger intro
    CHAPTER2_BOSS_COMBAT,      // Tax Collector Badger fight
    CHAPTER2_POST_BOSS,        // Boss defeated, page pickup
    CHAPTER2_QUESTBOOK_PAGE2,  // Full-screen Questbook page 2
    CHAPTER2_RETURN,           // Return to market

    GAME_OVER            // Defeat screen
}
