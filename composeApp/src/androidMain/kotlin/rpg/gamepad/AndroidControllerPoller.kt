package rpg.gamepad

// Android controllers route through the standard key-event system and are
// handled automatically by the existing onKeyEvent handler in SliceScreen.
actual fun createControllerPoller(): ControllerPoller? = null
