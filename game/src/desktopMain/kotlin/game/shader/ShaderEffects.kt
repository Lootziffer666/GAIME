package game.shader

import korlibs.korge.view.Container
import korlibs.korge.view.addUpdater
import korlibs.korge.view.filter.ComposedFilter
import korlibs.korge.view.filter.Filter
import korlibs.korge.view.filter.filter
import korlibs.time.seconds

/**
 * Central registry and time-driver for all custom shader effects.
 *
 * Supports **filter composition**: multiple filters can be active on the same
 * container simultaneously via KorGE's [ComposedFilter]. Internally tracks
 * which filters are enabled per container and sets the composed result.
 *
 * Usage:
 * ```
 * val effects = ShaderEffects()
 * effects.enable(mapView, effects.lightingFilter)
 * effects.enable(mapView, effects.fogFilter)
 * // both are now active simultaneously
 * effects.disable(mapView, effects.fogFilter)
 * effects.startTimeUpdater(sceneRoot)
 * ```
 */
class ShaderEffects {

    val poisonFilter = PoisonFilter()
    val beerGoggleFilter = BeerGoggleFilter()
    val lightingFilter = LightingFilter()
    val heatShimmerFilter = HeatShimmerFilter()
    val rainFilter = RainFilter()
    val fogFilter = FogFilter()
    val doodleLineFilter = DoodleLineFilter()
    val worldMaterialFilter = WorldMaterialFilter()

    private var time = 0f

    /**
     * Tracks active filters per container. Ordered: insertion order determines
     * render sequence (first = applied first, last = applied last / on top).
     */
    private val activeFilters = mutableMapOf<Container, MutableList<Filter>>()

    /**
     * Enables [filter] on [target]. If the filter is already active on that
     * target, this is a no-op. Multiple filters compose via [ComposedFilter].
     */
    fun enable(target: Container, filter: Filter) {
        val list = activeFilters.getOrPut(target) { mutableListOf() }
        if (filter !in list) {
            list.add(filter)
        }
        applyFilters(target)
    }

    /**
     * Disables [filter] on [target]. If no filters remain, clears the filter.
     */
    fun disable(target: Container, filter: Filter) {
        val list = activeFilters[target] ?: return
        list.remove(filter)
        applyFilters(target)
    }

    /**
     * Removes all filters from [target].
     */
    fun detach(target: Container) {
        activeFilters.remove(target)
        target.filter = null
    }

    /**
     * Applies the current filter list to the container.
     * Single filter = direct assignment. Multiple = ComposedFilter.
     */
    private fun applyFilters(target: Container) {
        val list = activeFilters[target]
        if (list == null || list.isEmpty()) {
            target.filter = null
            return
        }
        target.filter = if (list.size == 1) {
            list[0]
        } else {
            ComposedFilter(list.toList())
        }
    }

    // =========================================================================
    // Convenience wrappers (backwards compatible with existing call sites)
    // =========================================================================

    /** Applies the poison filter to [target]. */
    fun attachPoison(target: Container) {
        enable(target, poisonFilter)
    }

    /** Applies the beer goggle filter to [target]. */
    fun attachBeerGoggle(target: Container) {
        enable(target, beerGoggleFilter)
    }

    /** Applies the lighting filter to [target] with the given [lights]. */
    fun attachLighting(target: Container, lights: List<LightSource>, tilePixelSize: Float = 48f) {
        lightingFilter.lights = lights
        lightingFilter.tilePixelSize = tilePixelSize
        enable(target, lightingFilter)
    }

    /** Applies heat shimmer to [target]. */
    fun attachHeatShimmer(target: Container) {
        enable(target, heatShimmerFilter)
    }

    /** Applies rain to [target]. */
    fun attachRain(target: Container) {
        enable(target, rainFilter)
    }

    /** Applies fog to [target]. */
    fun attachFog(target: Container) {
        enable(target, fogFilter)
    }

    // =========================================================================
    // Time driver
    // =========================================================================

    /**
     * Attaches an addUpdater to [root] that advances [time] for all managed filters.
     * Call once per scene after setup.
     */
    fun startTimeUpdater(root: Container) {
        root.addUpdater { dt ->
            time += dt.seconds.toFloat()
            poisonFilter.time = time
            beerGoggleFilter.time = time
            lightingFilter.time = time
            heatShimmerFilter.time = time
            rainFilter.time = time
            fogFilter.time = time
            doodleLineFilter.time = time
            worldMaterialFilter.time = time
        }
    }
}
