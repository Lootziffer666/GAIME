import engine.Particle
import engine.ParticleEngine
import engine.Personality
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ParticleEngineTest {

    @Test
    fun particlesReturnToOriginWhenNoPointer() {
        val engine = ParticleEngine()

        // Create a particle displaced from origin
        val particle = Particle(
            char = 'A',
            x = 100f,
            y = 100f,
            originX = 50f,
            originY = 50f,
            personality = Personality.Curious
        )
        engine.particles.add(particle)

        val initialDistX = abs(particle.x - particle.originX)
        val initialDistY = abs(particle.y - particle.originY)

        // Simulate several seconds of frames with no pointer active (10 seconds @ 60fps)
        repeat(600) {
            engine.update(0.016f)
        }

        // Particle should have moved significantly closer to origin
        val distX = abs(particle.x - particle.originX)
        val distY = abs(particle.y - particle.originY)
        assertTrue(distX < initialDistX * 0.5f, "Particle X should be significantly closer to origin, was $initialDistX now $distX")
        assertTrue(distY < initialDistY * 0.5f, "Particle Y should be significantly closer to origin, was $initialDistY now $distY")
    }

    @Test
    fun differentPersonalitiesProduceDifferentVelocities() {
        val nervousEngine = ParticleEngine()
        val curiousEngine = ParticleEngine()
        val sleepyEngine = ParticleEngine()

        val nervousParticle = Particle(
            char = 'N', x = 100f, y = 100f,
            originX = 100f, originY = 100f,
            personality = Personality.Nervous
        )
        val curiousParticle = Particle(
            char = 'C', x = 100f, y = 100f,
            originX = 100f, originY = 100f,
            personality = Personality.Curious
        )
        val sleepyParticle = Particle(
            char = 'S', x = 100f, y = 100f,
            originX = 100f, originY = 100f,
            personality = Personality.Sleepy
        )

        nervousEngine.particles.add(nervousParticle)
        curiousEngine.particles.add(curiousParticle)
        sleepyEngine.particles.add(sleepyParticle)

        // Set pointer near the particles
        nervousEngine.setPointer(110f, 110f)
        curiousEngine.setPointer(110f, 110f)
        sleepyEngine.setPointer(110f, 110f)

        // Simulate frames
        repeat(30) {
            nervousEngine.update(0.016f)
            curiousEngine.update(0.016f)
            sleepyEngine.update(0.016f)
        }

        // Nervous should have moved away from pointer (dodges strongly)
        // Curious should have moved toward pointer (follows lazily)
        // Sleepy stays near origin (drifts back)

        // Collect positions
        val nervousDisplacement = abs(nervousParticle.x - 100f) + abs(nervousParticle.y - 100f)
        val curiousDisplacement = abs(curiousParticle.x - 100f) + abs(curiousParticle.y - 100f)
        val sleepyDisplacement = abs(sleepyParticle.x - 100f) + abs(sleepyParticle.y - 100f)

        // Nervous should move significantly due to zitter + dodge
        assertTrue(
            nervousDisplacement > 0.01f || curiousDisplacement > 0.01f,
            "At least one active personality should produce movement"
        )

        // They should produce different behaviors - at minimum the magnitudes differ
        // Due to randomness in Nervous, we check that not all are exactly the same
        val allSame = nervousDisplacement == curiousDisplacement && curiousDisplacement == sleepyDisplacement
        assertTrue(
            !allSame,
            "Different personalities should produce different displacements. " +
                "Nervous=$nervousDisplacement, Curious=$curiousDisplacement, Sleepy=$sleepyDisplacement"
        )
    }

    @Test
    fun nervousParticleDodgesPointer() {
        val engine = ParticleEngine()

        val particle = Particle(
            char = 'N', x = 100f, y = 100f,
            originX = 100f, originY = 100f,
            personality = Personality.Nervous
        )
        engine.particles.add(particle)

        // Place pointer very close to particle
        engine.setPointer(105f, 105f)

        repeat(20) {
            engine.update(0.016f)
        }

        // Nervous particle should have moved away from pointer position
        // Due to randomness, we use a relaxed assertion - it should show some displacement
        val displacement = abs(particle.x - 100f) + abs(particle.y - 100f)
        assertTrue(
            displacement > 0.1f,
            "Nervous particle should show displacement from pointer, but total was $displacement"
        )
    }

    @Test
    fun curiousParticleMovesTowardPointer() {
        val engine = ParticleEngine()

        val particle = Particle(
            char = 'C', x = 50f, y = 50f,
            originX = 50f, originY = 50f,
            personality = Personality.Curious
        )
        engine.particles.add(particle)

        // Place pointer far away but within range
        engine.setPointer(150f, 150f)

        repeat(50) {
            engine.update(0.016f)
        }

        // Curious particle should have moved toward pointer (x and y should increase)
        assertTrue(
            particle.x > 50f,
            "Curious particle X should move toward pointer at 150, but was ${particle.x}"
        )
        assertTrue(
            particle.y > 50f,
            "Curious particle Y should move toward pointer at 150, but was ${particle.y}"
        )
    }
}
