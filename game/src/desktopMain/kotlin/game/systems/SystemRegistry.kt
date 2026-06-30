package game.systems

import rpg.systems.WorldContext
import rpg.systems.WorldSystem

/**
 * Holds all active WorldSystems and their optional render bindings.
 *
 * Usage in DoodleWorldScene:
 *   val registry = SystemRegistry()
 *   registry.register(waterSystem) { waterOverlay.update(waterSystem.grid) }
 *   // In addUpdater:
 *   registry.tickAll(dtSec, ctx)
 *   registry.renderAll()
 *
 * Adding a new system = 1 register() call. That's the Pfeiler 2 promise.
 */
class SystemRegistry {
    private data class Entry(val system: WorldSystem, val render: (() -> Unit)?)

    private val entries = mutableListOf<Entry>()

    /** Register a system with an optional render binding. */
    fun register(system: WorldSystem, render: (() -> Unit)? = null) {
        entries.add(Entry(system, render))
    }

    /** Tick all systems. Call once per frame. */
    fun tickAll(dtSeconds: Float, ctx: WorldContext) {
        for (entry in entries) {
            entry.system.tick(dtSeconds, ctx)
        }
    }

    /** Call all render bindings. Call once per frame after tickAll. */
    fun renderAll() {
        for (entry in entries) {
            entry.render?.invoke()
        }
    }

    /** Get a system by ID (for reading state, e.g. drunk level for shader). */
    @Suppress("UNCHECKED_CAST")
    fun <T : WorldSystem> get(id: String): T? =
        entries.firstOrNull { it.system.id == id }?.system as? T
}
