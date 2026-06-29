package rpg.systems

import rpg.combat.Combatant
import rpg.items.Inventory

/**
 * Slim interface through which WorldSystems interact with the game state
 * without knowing anything about the renderer. Pure data contract.
 */
interface WorldContext {
    val player: Combatant
    val inventory: Inventory
    val playerCellX: Int
    val playerCellY: Int
    val isPlayerIdle: Boolean
}
