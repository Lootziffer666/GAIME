package rpg.gamepad

import rpg.world.Direction

/**
 * Platform-agnostic interface for gamepad/controller input.
 * Call [poll] once per frame to update hardware state, then read [direction]
 * and [consumeInteract] for the current frame's input.
 */
interface ControllerPoller {
    /** Update hardware state. Returns false if the controller is disconnected. */
    fun poll(): Boolean
    /** Direction the player is pressing, or null if stick/d-pad is centered. */
    fun direction(): Direction?
    /** True exactly once per physical button press (south button / A / Cross). */
    fun consumeInteract(): Boolean
    /** True exactly once per physical button press (west face button / X / Square). */
    fun consumeAttack(): Boolean = false
    fun release() {}
}

expect fun createControllerPoller(): ControllerPoller?
