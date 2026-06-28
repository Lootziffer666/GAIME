package game

import korlibs.korge.view.Container
import game.shader.ShaderEffects
import rpg.questbook.QuestPressure

/**
 * Binds game state to screen shaders — the "shaders ARE the state" principle
 * (docs/SHADER_GAME_CONCEPT.md). Pressure and combat distress drive the poison
 * filter's intensity; the world destabilises visibly as bureaucratic chaos peaks.
 */
class ShaderStateBinder(
    private val effects: ShaderEffects,
    private val target: Container,
) {
    private var poisonAttached = false

    /** Maps quest pressure to a poison-shader intensity and attaches/detaches as needed. */
    fun applyPressure(pressure: QuestPressure) {
        val intensity = when (pressure) {
            QuestPressure.LOW -> 0.0f
            QuestPressure.MEDIUM -> 0.35f
            QuestPressure.HIGH -> 0.85f
        }
        applyPoison(intensity)
    }

    /**
     * Combat distress: hero HP fraction (1.0 = full, 0.0 = dead) adds disorientation.
     * Combined with any pressure-driven intensity (takes the max).
     */
    fun applyCombatDistress(heroHpFraction: Float, pressure: QuestPressure) {
        val fromPressure = when (pressure) {
            QuestPressure.LOW -> 0.0f
            QuestPressure.MEDIUM -> 0.35f
            QuestPressure.HIGH -> 0.85f
        }
        val fromHp = (1.0f - heroHpFraction) * 0.7f
        applyPoison(maxOf(fromPressure, fromHp))
    }

    private fun applyPoison(intensity: Float) {
        if (intensity <= 0.01f) {
            if (poisonAttached) { effects.detach(target); poisonAttached = false }
            effects.poisonFilter.intensity = 0f
            return
        }
        effects.poisonFilter.intensity = intensity.coerceIn(0f, 1f)
        if (!poisonAttached) { effects.attachPoison(target); poisonAttached = true }
    }
}
