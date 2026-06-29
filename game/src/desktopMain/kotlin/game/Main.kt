package game

import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.Size
import rpg.combat.EnemyArchetype   // :core reachability proof

/**
 * KorGE entry point for the :game module.
 *
 * Step 13: boots into DoodleWorldScene at 1440p (2560x1440).
 * The painted background + invisible grid + doodle-character loop.
 *
 * WorldScene (tilemap-based gameplay) remains available as a reference scene.
 */
suspend fun main() = Korge(
    virtualSize = Size(2560, 1440),
    windowSize = Size(2560, 1440),
    backgroundColor = Colors.BLACK,
) {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<DoodleWorldScene>()
}
