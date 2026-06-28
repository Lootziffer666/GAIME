package game

import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import rpg.combat.EnemyArchetype   // :core reachability proof

/**
 * KorGE entry point for the :game module (migration Step 5b).
 * Boots into [WorldScene] — the full world layer with smooth movement,
 * NPCs, dialog, HUD, and map transitions.
 */
suspend fun main() = Korge {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<WorldScene>()
}
