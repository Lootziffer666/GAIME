package game

import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import rpg.combat.EnemyArchetype   // :core reachability proof

/**
 * KorGE entry point for the :game module (migration Step 4b).
 *
 * Boots into [TiledMapScene] which loads a Tiled TMX map, renders it with
 * [TiledMapView], and provides grid-based player movement with collision.
 * [Hd2dStage] remains as historical reference but is no longer the start scene.
 */
suspend fun main() = Korge {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<TiledMapScene>()
}
