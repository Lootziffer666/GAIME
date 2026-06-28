package game

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookReaction

/**
 * Screen-fixed overlay showing Questbook reactions, Pressure level, and active markers.
 *
 * Three components:
 * (a) Reaction-Toast — appears on fireBark(), auto-dismisses after ~4s.
 * (b) Pressure-Pill — always visible, color-coded LOW/MEDIUM/HIGH.
 * (c) Quest-Marker-List — active markers (real + false, visually identical).
 *
 * All views added directly to [parent] with individual `.visible` toggling
 * (SolidRect is a leaf View, not a Container — KNOWN_BUGS Step 5b).
 */
class QuestbookOverlay(
    parent: Container,
    private val vw: Double,
    private val vh: Double,
) {
    // --- (a) Reaction Toast ---
    private val toastPanel: SolidRect
    private val toastBorder: SolidRect
    private val toastHeader: Text
    private val toastBody: Text
    private var toastRemaining: Float = 0f

    // --- (b) Pressure Pill ---
    private val pillBg: SolidRect
    private val pillLabel: Text

    // --- (c) Marker List ---
    private val markerPanel: SolidRect
    private val markerHeader: Text
    private val markerLines: List<Text>

    init {
        // (a) Toast — top center
        val toastW = vw * 0.6
        val toastH = 60.0
        val toastX = (vw - toastW) / 2.0
        val toastY = 8.0

        toastPanel = parent.solidRect(toastW, toastH, RGBA(0x0a, 0x0a, 0x14, 0xee))
            .apply { x = toastX; y = toastY; visible = false }
        toastBorder = parent.solidRect(toastW, 2.0, Colors["#886644"])
            .apply { x = toastX; y = toastY; visible = false }
        toastHeader = parent.text("QUESTBOOK ENTRY", textSize = 11.0, color = Colors["#ffdd88"])
            .apply { x = toastX + 10.0; y = toastY + 6.0; visible = false }
        toastBody = parent.text("", textSize = 13.0, color = Colors["#ffe9b0"])
            .apply { x = toastX + 10.0; y = toastY + 22.0; visible = false }

        // (b) Pressure Pill — top right
        val pillW = 130.0
        val pillH = 18.0
        val pillX = vw - pillW - 8.0
        val pillY = 8.0

        pillBg = parent.solidRect(pillW, pillH, Colors["#22cc22"])
            .apply { x = pillX; y = pillY }
        pillLabel = parent.text("PRESSURE: LOW", textSize = 10.0, color = Colors.WHITE)
            .apply { x = pillX + 6.0; y = pillY + 3.0 }

        // (c) Marker List — right side below pill
        val listX = vw - 180.0
        val listY = 32.0
        val listW = 172.0
        val listH = 90.0

        markerPanel = parent.solidRect(listW, listH, RGBA(0x0a, 0x0a, 0x14, 0xbb))
            .apply { x = listX; y = listY; visible = false }
        markerHeader = parent.text("QUESTS (0)", textSize = 10.0, color = Colors["#ffdd88"])
            .apply { x = listX + 6.0; y = listY + 4.0; visible = false }
        markerLines = List(5) { i ->
            parent.text("", textSize = 11.0, color = Colors["#cccccc"])
                .apply { x = listX + 6.0; y = listY + 18.0 + i * 14.0; visible = false }
        }
    }

    /**
     * Shows a reaction toast + refreshes pressure and markers.
     */
    fun showReaction(reaction: QuestbookReaction, pressure: QuestPressure, markers: List<String>) {
        // Toast
        val text = reaction.questbookText
        // Simple line-wrap at ~50 chars
        val wrapped = if (text.length > 50) text.chunked(50).joinToString("\n") else text
        toastBody.text = wrapped
        setToastVisible(true)
        toastRemaining = 4f

        // Refresh persistent elements
        refresh(pressure, markers)
    }

    /**
     * Shows a simple message toast (e.g. combat events) without marker update.
     */
    fun showMessage(text: String, pressure: QuestPressure) {
        val wrapped = if (text.length > 50) text.chunked(50).joinToString("\n") else text
        toastBody.text = wrapped
        setToastVisible(true)
        toastRemaining = 3f
        val (color, label) = when (pressure) {
            QuestPressure.LOW -> Colors["#22cc22"] to "PRESSURE: LOW"
            QuestPressure.MEDIUM -> Colors["#ddaa22"] to "PRESSURE: MEDIUM"
            QuestPressure.HIGH -> Colors["#cc2222"] to "PRESSURE: HIGH"
        }
        pillBg.color = color
        pillLabel.text = label
    }

    /**
     * Refreshes pressure pill + marker list without showing a toast.
     */
    fun refresh(pressure: QuestPressure, markers: List<String>) {
        // Pressure pill
        val (color, label) = when (pressure) {
            QuestPressure.LOW -> Colors["#22cc22"] to "PRESSURE: LOW"
            QuestPressure.MEDIUM -> Colors["#ddaa22"] to "PRESSURE: MEDIUM"
            QuestPressure.HIGH -> Colors["#cc2222"] to "PRESSURE: HIGH"
        }
        pillBg.color = color
        pillLabel.text = label

        // Marker list
        val hasMarkers = markers.isNotEmpty()
        markerPanel.visible = hasMarkers
        markerHeader.visible = hasMarkers
        markerHeader.text = "QUESTS (${markers.size})"

        for (i in markerLines.indices) {
            if (i < markers.size) {
                markerLines[i].text = "\u2022 ${markers[i]}"
                markerLines[i].visible = true
            } else {
                markerLines[i].visible = false
            }
        }
    }

    /**
     * Call per frame to tick the toast auto-dismiss timer.
     */
    fun update(dtSeconds: Float) {
        if (toastRemaining > 0f) {
            toastRemaining -= dtSeconds
            if (toastRemaining <= 0f) {
                setToastVisible(false)
            }
        }
    }

    private fun setToastVisible(v: Boolean) {
        toastPanel.visible = v
        toastBorder.visible = v
        toastHeader.visible = v
        toastBody.visible = v
    }
}
