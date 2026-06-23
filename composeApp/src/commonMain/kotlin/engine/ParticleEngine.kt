package engine

import kotlin.math.sqrt
import kotlin.random.Random

class ParticleEngine {
    val particles = mutableListOf<Particle>()

    private var pointerX: Float? = null
    private var pointerY: Float? = null
    private var pointerActive: Boolean = false

    private val springStrength = 8.0f
    private val damping = 0.9f
    private val pointerRadius = 120f

    fun setPointer(x: Float, y: Float) {
        pointerX = x
        pointerY = y
        pointerActive = true
    }

    fun clearPointer() {
        pointerActive = false
        pointerX = null
        pointerY = null
    }

    fun update(deltaTime: Float) {
        val dt = deltaTime.coerceAtMost(0.05f) // cap delta to avoid explosion
        val px = pointerX
        val py = pointerY

        for (particle in particles) {
            // Compute distance to pointer
            var pointerDx = 0f
            var pointerDy = 0f
            var pointerDist = Float.MAX_VALUE

            if (pointerActive && px != null && py != null) {
                pointerDx = px - particle.x
                pointerDy = py - particle.y
                pointerDist = sqrt(pointerDx * pointerDx + pointerDy * pointerDy)
            }

            when (particle.personality) {
                Personality.Nervous -> {
                    // Zitter: small random displacement
                    particle.vx += (Random.nextFloat() - 0.5f) * 200f * dt
                    particle.vy += (Random.nextFloat() - 0.5f) * 200f * dt

                    // Dodge pointer strongly
                    if (pointerActive && pointerDist < pointerRadius) {
                        val repelStrength = (1f - pointerDist / pointerRadius) * 800f
                        particle.vx -= (pointerDx / pointerDist) * repelStrength * dt
                        particle.vy -= (pointerDy / pointerDist) * repelStrength * dt
                    }
                }

                Personality.Curious -> {
                    // Follow pointer lazily
                    if (pointerActive && pointerDist < pointerRadius * 2f) {
                        particle.vx += pointerDx * 0.8f * dt
                        particle.vy += pointerDy * 0.8f * dt
                        // Grow slightly bigger when near pointer
                        particle.size = (14f + (1f - (pointerDist / (pointerRadius * 2f)).coerceIn(0f, 1f)) * 4f)
                    } else {
                        particle.size = 14f
                    }
                }

                Personality.Sleepy -> {
                    // Drifts slowly back to origin
                    val driftX = particle.originX - particle.x
                    val driftY = particle.originY - particle.y
                    particle.vx += driftX * 0.5f * dt
                    particle.vy += driftY * 0.5f * dt
                    // Extra damping for sleepy
                    particle.vx *= 0.92f
                    particle.vy *= 0.92f
                }
            }

            // Spring force returns all to origin when no pointer active
            val springX = particle.originX - particle.x
            val springY = particle.originY - particle.y
            particle.vx += springX * springStrength * dt
            particle.vy += springY * springStrength * dt

            // Apply damping
            particle.vx *= damping
            particle.vy *= damping

            // Integrate position
            particle.x += particle.vx * dt
            particle.y += particle.vy * dt
        }
    }
}
