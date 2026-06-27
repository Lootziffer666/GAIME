package game

import korlibs.korge.Korge
import rpg.combat.EnemyArchetype

/**
 * Minimal KorGE entry point for the :game module (migration Step 2).
 *
 * Goal of this step: a minimal *compiling* KorGE entry that depends on :core,
 * confirming the toolchain resolves. Step 3 ports demos/korge-hd2d/Hd2dStage.kt
 * to render the 2.5D scene; Step 4 brings the :core gameplay into KorGE scenes.
 *
 * The reference to a :core type below proves the pure logic module is on the
 * classpath and usable from the renderer.
 */
suspend fun main() = Korge {
    val coreArchetypes = EnemyArchetype.entries.size
    println("GAIME :game (KorGE) booted — :core reachable ($coreArchetypes enemy archetypes).")
}
