package game

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.view.SContainer
import korlibs.korge.view.addUpdater
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import kotlinx.coroutines.launch
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookReaction

/**
 * Full-screen Questbook view — the bureaucratic heart of the game, finally visible.
 *
 * Shows an "open book" with registered quests (left page) and active markers +
 * party info (right page). Animated open/close via scale tween.
 *
 * Open: J key in WorldScene. Close: J or ESC → back to WorldScene.
 */
class QuestbookScreen : Scene() {

    companion object {
        var entries: List<QuestbookReaction> = emptyList()
        var markers: List<String> = emptyList()
        var partyName: String? = null
        var pressure: QuestPressure = QuestPressure.LOW
    }

    override suspend fun SContainer.sceneMain() {
        val vw = width
        val vh = height

        // Background (dark parchment)
        solidRect(vw, vh, RGBA(0x1a, 0x12, 0x08, 0xff))

        // Book frame
        val bookW = vw * 0.85
        val bookH = vh * 0.8
        val bookX = (vw - bookW) / 2.0
        val bookY = (vh - bookH) / 2.0

        // Parchment pages
        solidRect(bookW, bookH, RGBA(0xf5, 0xe6, 0xc8, 0xff))
            .apply { x = bookX; y = bookY }

        // Binding (center line)
        solidRect(4.0, bookH, Colors["#5c3a1e"])
            .apply { x = bookX + bookW / 2.0 - 2.0; y = bookY }

        // Border
        solidRect(bookW, 3.0, Colors["#886644"]).apply { x = bookX; y = bookY }
        solidRect(bookW, 3.0, Colors["#886644"]).apply { x = bookX; y = bookY + bookH - 3.0 }
        solidRect(3.0, bookH, Colors["#886644"]).apply { x = bookX; y = bookY }
        solidRect(3.0, bookH, Colors["#886644"]).apply { x = bookX + bookW - 3.0; y = bookY }

        // --- Left page: Quest entries ---
        val leftX = bookX + 16.0
        val leftY = bookY + 16.0

        text("OFFICIAL QUEST LOG", textSize = 12.0, color = Colors["#5c3a1e"])
            .apply { x = leftX; y = leftY }

        val displayEntries = entries.takeLast(8)
        for ((i, entry) in displayEntries.withIndex()) {
            val entryText = if (entry.questbookText.length > 45)
                entry.questbookText.take(45) + "..." else entry.questbookText
            text("\u2022 $entryText", textSize = 10.0, color = Colors["#3a2a1a"])
                .apply { x = leftX; y = leftY + 20.0 + i * 16.0 }
        }

        if (entries.isEmpty()) {
            text("(No entries recorded yet)", textSize = 10.0, color = Colors["#888866"])
                .apply { x = leftX; y = leftY + 24.0 }
        }

        // --- Right page: Markers + Party + Pressure ---
        val rightX = bookX + bookW / 2.0 + 16.0
        val rightY = bookY + 16.0

        text("ACTIVE ASSIGNMENTS", textSize = 12.0, color = Colors["#5c3a1e"])
            .apply { x = rightX; y = rightY }

        for ((i, marker) in markers.take(6).withIndex()) {
            val markerText = if (marker.length > 40) marker.take(40) + "..." else marker
            text("\u25B6 $markerText", textSize = 10.0, color = Colors["#3a2a1a"])
                .apply { x = rightX; y = rightY + 20.0 + i * 16.0 }
        }

        // Party name
        val partyY = rightY + 130.0
        text("REGISTERED PARTY:", textSize = 10.0, color = Colors["#5c3a1e"])
            .apply { x = rightX; y = partyY }
        text(partyName ?: "(unregistered)", textSize = 11.0, color = Colors["#3a2a1a"])
            .apply { x = rightX; y = partyY + 14.0 }

        // Pressure indicator
        val pressureY = partyY + 40.0
        val pressureColor = when (pressure) {
            QuestPressure.LOW -> Colors["#22cc22"]
            QuestPressure.MEDIUM -> Colors["#ddaa22"]
            QuestPressure.HIGH -> Colors["#cc2222"]
        }
        text("BUREAUCRATIC PRESSURE: ${pressure.name}", textSize = 10.0, color = pressureColor)
            .apply { x = rightX; y = pressureY }

        // Footer
        text("[J / ESC to close]", textSize = 9.0, color = Colors["#888866"])
            .apply { x = bookX + bookW / 2.0 - 40.0; y = bookY + bookH - 16.0 }

        // --- Input: close ---
        addUpdater {
            val keys = views.input.keys
            if (keys.justPressed(Key.J) || keys.justPressed(Key.ESCAPE)) {
                launch { sceneContainer.changeTo<WorldScene>() }
            }
        }
    }
}
