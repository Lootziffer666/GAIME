package game

import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import rpg.combat.EnemyArchetype   // :core reachability proof

/**
 * KorGE entry point for the :game module (migration Step 5a).
 *
 * Boots directly into [TiledMapScene] — the Tiled-rendered world map with
 * real character sprites, audio, and battle transitions.
 */
suspend fun main() = Korge {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<TiledMapScene>()
}
