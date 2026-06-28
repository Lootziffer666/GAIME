package game

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import rpg.combat.Combatant
import rpg.items.Inventory

/**
 * Screen-fixed HUD overlay showing hero HP, gold, and current location.
 * Must be added to the scene root (not mapView) so it stays fixed on screen.
 * Call [update] every frame to refresh the dynamic values.
 */
class HudOverlay(
    parent: Container,
    private val hero: Combatant,
    private val inventory: Inventory,
    locationName: String,
) {
    private val barMaxWidth = 80.0
    private val barHeight = 8.0

    private val hpBarFg: SolidRect
    private val hpLabel: Text
    private val goldLabel: Text
    private val locationLabel: Text

    init {
        // Semi-transparent background panel
        parent.solidRect(150.0, 52.0, RGBA(0x00, 0x00, 0x00, 0xaa))
            .apply { x = 4.0; y = 4.0 }

        // HP bar background
        parent.solidRect(barMaxWidth, barHeight, Colors["#444444"])
            .apply { x = 8.0; y = 10.0 }

        // HP bar foreground
        hpBarFg = parent.solidRect(barMaxWidth, barHeight, Colors["#22cc22"])
            .apply { x = 8.0; y = 10.0 }

        // HP label
        hpLabel = parent.text("HP ${hero.hp}/${hero.maxHp}", textSize = 10.0, color = Colors.WHITE)
            .apply { x = 8.0; y = 20.0 }

        // Gold label
        goldLabel = parent.text("Gold: ${inventory.gold}", textSize = 10.0, color = Colors["#ffdd44"])
            .apply { x = 8.0; y = 34.0 }

        // Location label
        locationLabel = parent.text(locationName, textSize = 10.0, color = Colors["#aaaaaa"])
            .apply { x = 8.0; y = 46.0 }
    }

    /**
     * Refreshes all dynamic values. Call once per frame from WorldScene's addUpdater.
     */
    fun update(locationName: String) {
        hpBarFg.width = barMaxWidth * hero.hpFraction
        hpLabel.text = "HP ${hero.hp}/${hero.maxHp}"
        goldLabel.text = "Gold: ${inventory.gold}"
        locationLabel.text = locationName
    }
}
