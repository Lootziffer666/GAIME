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
    GAME_OVER            // Defeat screen
}
