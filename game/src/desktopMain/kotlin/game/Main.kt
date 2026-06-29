package game

import korlibs.image.color.Colors
import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import korlibs.math.geom.Size
import rpg.combat.EnemyArchetype   // :core reachability proof

/**
 * KorGE entry point for the :game module.
 *
 * Step 14: boots into DoodleWorldScene at 1440p (2560x1440) — the Unified World Runtime.
 * Full gameplay: Bild+Grid, Doodle character, NPC hotspots, Dialog, Bark, HUD,
 * Questbook, Battle, Camera, Map transitions (Tavern <-> Wildwood).
 *
 * WorldScene (tilemap-based gameplay) is retired as boot path — deletion is a follow-up.
 */
suspend fun main() = Korge(
    virtualSize = Size(2560, 1440),
    windowSize = Size(2560, 1440),
    backgroundColor = Colors.BLACK,
) {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<DoodleWorldScene>()
}
