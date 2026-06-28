package game.shader

import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.filter.filter
import korlibs.time.seconds

/**
 * Central registry and time-driver for all custom shader effects.
 *
 * Usage in a scene:
 * ```
 * val effects = ShaderEffects()
 * effects.attachPoison(mapView)
 * effects.poisonFilter.intensity = 0.5f
 * effects.startTimeUpdater(sceneRoot)
 * ```
 *
 * Future effects (Step 7b/c): lighting, rain, fog, heat shimmer, sparks, dust.
 */
class ShaderEffects {

    val poisonFilter = PoisonFilter()
    val beerGoggleFilter = BeerGoggleFilter()

    private var time = 0f

    /**
     * Attaches an addUpdater to [root] that advances [time] for all managed filters.
     * Call once per scene after setup.
     */
    fun startTimeUpdater(root: Container) {
        root.addUpdater { dt ->
            time += dt.seconds.toFloat()
            poisonFilter.time = time
            beerGoggleFilter.time = time
        }
    }

    /** Applies the poison filter to [target]. */
    fun attachPoison(target: Container) {
        target.filter = poisonFilter
    }

    /** Applies the beer goggle filter to [target]. */
    fun attachBeerGoggle(target: Container) {
        target.filter = beerGoggleFilter
    }

    /** Removes all filters from [target]. */
    fun detach(target: Container) {
        target.filter = null
    }
}
