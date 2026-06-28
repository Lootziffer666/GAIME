package game

import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import rpg.combat.EnemyArchetype   // :core reachability proof

/**
 * KorGE entry point for the :game module (migration Step 3).
 *
 * Step 2 proved the toolchain + :core wiring with a minimal Korge block. Step 3
 * boots straight into the ported 2.5D HD-2D scene ([Hd2dStage]). Acceptance is
 * compilation only — the GL window is opened manually/locally (the sandbox has
 * no display). Step 4 brings the :core tilemap/gameplay into KorGE scenes.
 */
suspend fun main() = Korge {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<Hd2dStage>()
}
