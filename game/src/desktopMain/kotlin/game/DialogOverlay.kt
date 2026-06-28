package game

import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.view.Container
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.solidRect
import korlibs.korge.view.text

/**
 * Full-width dialog box anchored to the bottom of the screen.
 *
 * Usage:
 *   val dialog = DialogOverlay(sceneRoot, virtualWidth, virtualHeight)
 *   dialog.show(listOf(DialogLine("Barkeep", "Spend some coin or get out.")))
 *   // In addUpdater: if (keys.justPressed(Key.RETURN)) dialog.advance()
 *   // WorldScene pauses movement input while dialog.isActive == true
 *
 * All child views are added directly to [parent] and individually visibility-toggled.
 * (KorGE's SolidRect is a leaf View, not a Container — addChild would not compile.)
 */
class DialogOverlay(
    parent: Container,
    private val vw: Double,
    private val vh: Double,
) {
    private val panelHeight = vh * 0.28
    private val panelY = vh - panelHeight - 8.0

    private val panel: SolidRect
    private val border: SolidRect
    private val speakerLabel: Text
    private val bodyLabel: Text
    private val promptLabel: Text

    private var lines: List<DialogLine> = emptyList()
    private var lineIndex: Int = 0

    val isActive: Boolean get() = panel.visible

    init {
        panel = parent.solidRect(vw - 16.0, panelHeight, RGBA(0x0a, 0x0a, 0x14, 0xdd))
            .apply { x = 8.0; y = panelY; visible = false }

        border = parent.solidRect(vw - 16.0, 2.0, Colors["#886644"])
            .apply { x = 8.0; y = panelY; visible = false }

        speakerLabel = parent.text("", textSize = 14.0, color = Colors["#ffdd88"])
            .apply { x = 20.0; y = panelY + 10.0; visible = false }

        bodyLabel = parent.text("", textSize = 13.0, color = Colors.WHITE)
            .apply { x = 20.0; y = panelY + 30.0; visible = false }

        promptLabel = parent.text("[ENTER]", textSize = 11.0, color = Colors["#888888"])
            .apply { x = vw - 80.0; y = panelY + panelHeight - 20.0; visible = false }
    }

    /**
     * Starts showing [newLines] from the beginning.
     * If [newLines] is empty, does nothing.
     */
    fun show(newLines: List<DialogLine>) {
        if (newLines.isEmpty()) return
        lines = newLines
        lineIndex = 0
        display(lines[0])
    }

    /**
     * Advances to the next line. Hides the overlay after the last line.
     */
    fun advance() {
        lineIndex++
        if (lineIndex >= lines.size) {
            hide()
        } else {
            display(lines[lineIndex])
        }
    }

    private fun display(line: DialogLine) {
        setVisible(true)
        speakerLabel.text = if (line.speaker.isNotEmpty()) line.speaker else ""
        bodyLabel.text = line.text
    }

    private fun hide() {
        setVisible(false)
        lines = emptyList()
        lineIndex = 0
    }

    private fun setVisible(v: Boolean) {
        panel.visible = v
        border.visible = v
        speakerLabel.visible = v
        bodyLabel.visible = v
        promptLabel.visible = v
    }
}
