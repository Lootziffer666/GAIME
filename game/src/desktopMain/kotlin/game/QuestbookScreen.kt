package game

import korlibs.event.Key
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.scene.Scene
import korlibs.korge.scene.sceneContainer
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.korge.view.SolidRect
import korlibs.korge.view.Text
import korlibs.korge.view.addUpdater
import korlibs.korge.view.solidRect
import korlibs.korge.view.text
import korlibs.math.interpolation.Easing
import kotlinx.coroutines.launch
import rpg.questbook.QuestPressure
import rpg.questbook.QuestbookReaction
import kotlin.time.Duration.Companion.milliseconds

/**
 * Full-screen Questbook view -- the bureaucratic heart of the game, fully illustrated.
 *
 * Shows an "open book" with aged parchment, ornamental binding/spine,
 * framed pages, dog-ears, shadows. Animated open via scale tween,
 * page-turn via scaleX flip on LEFT/RIGHT arrows.
 *
 * Open: J key in WorldScene. Close: J or ESC -> back to WorldScene.
 */
class QuestbookScreen : Scene() {

    companion object {
        var entries: List<QuestbookReaction> = emptyList()
        var markers: List<String> = emptyList()
        var partyName: String? = null
        var pressure: QuestPressure = QuestPressure.LOW
    }

    // Custom easeOutBack: overshoots then settles
    private val easeOutBack: Easing = Easing.Companion.invoke({ "easeOutBack" }) { t: Float ->
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val tm1 = t - 1f
        1f + c3 * tm1 * tm1 * tm1 + c1 * tm1 * tm1
    }

    override suspend fun SContainer.sceneMain() {
        val vw = width
        val vh = height

        // Dark background behind the book
        solidRect(vw, vh, RGBA(0x12, 0x0c, 0x06, 0xff))

        // --- Book container (will be animated) ---
        val bookContainer = Container()
        addChild(bookContainer)

        val bookW = vw * 0.85
        val bookH = vh * 0.8
        val bookX = (vw - bookW) / 2.0
        val bookY = (vh - bookH) / 2.0

        // Position the book container at center for scale pivot
        bookContainer.x = vw / 2.0
        bookContainer.y = vh / 2.0

        // Offset children so they render centered around (0,0)
        val offX = -bookW / 2.0
        val offY = -bookH / 2.0

        // --- Parchment gradient (multiple overlapping rects for aged paper look) ---
        val parchmentColors = listOf(
            RGBA(0xf8, 0xf0, 0xd8, 0xff),  // lightest center
            RGBA(0xf2, 0xe4, 0xc4, 0xff),
            RGBA(0xe8, 0xd8, 0xb0, 0xff),
            RGBA(0xde, 0xcc, 0x9c, 0xff),  // darker edges
        )
        // Base outer shadow
        bookContainer.solidRect(bookW + 8.0, bookH + 8.0, RGBA(0x2a, 0x1a, 0x0a, 0xcc))
            .apply { x = offX - 4.0; y = offY - 4.0 }

        // Gradient layers from outer (dark) to inner (light)
        for ((i, color) in parchmentColors.reversed().withIndex()) {
            val inset = i * 3.0
            bookContainer.solidRect(bookW - inset * 2, bookH - inset * 2, color)
                .apply { x = offX + inset; y = offY + inset }
        }

        // Worn edge stains (subtle darker patches along edges)
        bookContainer.solidRect(bookW, 6.0, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = offX; y = offY }
        bookContainer.solidRect(bookW, 6.0, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = offX; y = offY + bookH - 6.0 }
        bookContainer.solidRect(6.0, bookH, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = offX; y = offY }
        bookContainer.solidRect(6.0, bookH, RGBA(0xc8, 0xb0, 0x88, 0x44))
            .apply { x = offX + bookW - 6.0; y = offY }

        // --- Binding / Spine (3D depth illusion with multiple narrow rects) ---
        val spineX = offX + bookW / 2.0
        bookContainer.solidRect(2.0, bookH, RGBA(0x2a, 0x18, 0x08, 0xff))
            .apply { x = spineX - 5.0; y = offY }
        bookContainer.solidRect(3.0, bookH, RGBA(0x3a, 0x24, 0x10, 0xff))
            .apply { x = spineX - 3.0; y = offY }
        bookContainer.solidRect(6.0, bookH, RGBA(0x4a, 0x30, 0x18, 0xff))
            .apply { x = spineX - 3.0; y = offY }
        bookContainer.solidRect(3.0, bookH, RGBA(0x3a, 0x24, 0x10, 0xff))
            .apply { x = spineX + 3.0; y = offY }
        bookContainer.solidRect(2.0, bookH, RGBA(0x2a, 0x18, 0x08, 0xff))
            .apply { x = spineX + 5.0; y = offY }
        // Highlight line on spine
        bookContainer.solidRect(1.0, bookH, RGBA(0x6a, 0x50, 0x30, 0xff))
            .apply { x = spineX; y = offY }

        // --- Framed pages (ornamental borders) ---
        val frameInset = 10.0
        val pageW = bookW / 2.0 - 20.0
        val pageH = bookH - frameInset * 2

        // Left page frame (outer)
        val leftPageX = offX + frameInset
        val leftPageY = offY + frameInset
        bookContainer.solidRect(pageW + 4.0, pageH + 4.0, RGBA(0x8a, 0x6a, 0x44, 0xff))
            .apply { x = leftPageX - 2.0; y = leftPageY - 2.0 }
        // Left page frame (inner)
        bookContainer.solidRect(pageW, pageH, RGBA(0xf5, 0xe8, 0xcc, 0xff))
            .apply { x = leftPageX; y = leftPageY }
        // Inner ornamental border
        bookContainer.solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + 4.0; y = leftPageY + 4.0 }
        bookContainer.solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + 4.0; y = leftPageY + pageH - 5.0 }
        bookContainer.solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + 4.0; y = leftPageY + 4.0 }
        bookContainer.solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = leftPageX + pageW - 5.0; y = leftPageY + 4.0 }

        // Right page frame (outer)
        val rightPageX = offX + bookW / 2.0 + 10.0
        val rightPageY = offY + frameInset
        bookContainer.solidRect(pageW + 4.0, pageH + 4.0, RGBA(0x8a, 0x6a, 0x44, 0xff))
            .apply { x = rightPageX - 2.0; y = rightPageY - 2.0 }
        // Right page frame (inner)
        bookContainer.solidRect(pageW, pageH, RGBA(0xf5, 0xe8, 0xcc, 0xff))
            .apply { x = rightPageX; y = rightPageY }
        // Inner ornamental border
        bookContainer.solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + 4.0; y = rightPageY + 4.0 }
        bookContainer.solidRect(pageW - 8.0, 1.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + 4.0; y = rightPageY + pageH - 5.0 }
        bookContainer.solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + 4.0; y = rightPageY + 4.0 }
        bookContainer.solidRect(1.0, pageH - 8.0, RGBA(0xaa, 0x88, 0x55, 0x88))
            .apply { x = rightPageX + pageW - 5.0; y = rightPageY + 4.0 }

        // --- Page edge shadows ---
        // Right edge of left page (shadow)
        bookContainer.solidRect(3.0, pageH, RGBA(0x44, 0x33, 0x22, 0x44))
            .apply { x = leftPageX + pageW; y = leftPageY }
        // Left edge of right page (shadow)
        bookContainer.solidRect(3.0, pageH, RGBA(0x44, 0x33, 0x22, 0x44))
            .apply { x = rightPageX - 3.0; y = rightPageY }

        // --- Dog-ear (top-right corner fold) ---
        val dogEarSize = 14.0
        // Cover triangle: small rect angled to suggest fold
        bookContainer.solidRect(dogEarSize, dogEarSize, RGBA(0xd8, 0xc4, 0xa0, 0xff))
            .apply { x = rightPageX + pageW - dogEarSize; y = rightPageY; rotation = korlibs.math.geom.Angle.ZERO }
        // Shadow beneath dog-ear
        bookContainer.solidRect(dogEarSize, 2.0, RGBA(0x66, 0x50, 0x33, 0x88))
            .apply { x = rightPageX + pageW - dogEarSize; y = rightPageY + dogEarSize }
        bookContainer.solidRect(2.0, dogEarSize, RGBA(0x66, 0x50, 0x33, 0x88))
            .apply { x = rightPageX + pageW - dogEarSize - 2.0; y = rightPageY }
        // Folded triangle overlay (darker shade to simulate fold)
        bookContainer.solidRect(dogEarSize * 0.7, dogEarSize * 0.7, RGBA(0xc0, 0xaa, 0x80, 0xff))
            .apply { x = rightPageX + pageW - dogEarSize * 0.7; y = rightPageY }

        // --- Content rendering ---
        // Pagination: entries are shown 8 per page pair. Pages are navigated with LEFT/RIGHT.
        val entriesPerPage = 8
        var currentPagePair = 0 // 0 = first spread

        // Content container (holds text that can be swapped during page turn)
        val contentContainer = Container()
        bookContainer.addChild(contentContainer)

        // Page-turn overlay for animation
        val turnOverlay = bookContainer.solidRect(bookW / 2.0, bookH, RGBA(0xf5, 0xe8, 0xcc, 0xdd))
            .apply { x = spineX; y = offY; scaleX = 0.0; visible = false }

        fun totalPagePairs(): Int {
            val entryPages = if (entries.isEmpty()) 1 else ((entries.size - 1) / entriesPerPage) + 1
            return entryPages.coerceAtLeast(1)
        }

        fun renderContent(pagePair: Int) {
            // Clear previous content
            contentContainer.removeChildren()

            val contentLeftX = leftPageX + 14.0
            val contentLeftY = leftPageY + 14.0
            val contentRightX = rightPageX + 14.0
            val contentRightY = rightPageY + 14.0

            // --- Left page: Quest entries ---
            contentContainer.text("OFFICIAL QUEST LOG", textSize = 12.0, color = RGBA(0x5c, 0x3a, 0x1e, 0xff))
                .apply { x = contentLeftX; y = contentLeftY }

            // Page number
            val totalPages = totalPagePairs()
            contentContainer.text("Page ${pagePair + 1}/$totalPages", textSize = 9.0, color = RGBA(0x88, 0x77, 0x55, 0xff))
                .apply { x = contentLeftX + pageW - 70.0; y = contentLeftY }

            val startIdx = pagePair * entriesPerPage
            val displayEntries = entries.drop(startIdx).take(entriesPerPage)

            if (displayEntries.isEmpty() && pagePair == 0) {
                contentContainer.text("(No entries recorded yet)", textSize = 10.0, color = RGBA(0x88, 0x88, 0x66, 0xff))
                    .apply { x = contentLeftX; y = contentLeftY + 24.0 }
            } else {
                for ((i, entry) in displayEntries.withIndex()) {
                    val entryText = if (entry.questbookText.length > 42)
                        entry.questbookText.take(42) + "..." else entry.questbookText
                    contentContainer.text("\u2022 $entryText", textSize = 10.0, color = RGBA(0x3a, 0x2a, 0x1a, 0xff))
                        .apply { x = contentLeftX; y = contentLeftY + 22.0 + i * 16.0 }
                }
            }

            // --- Right page: Markers + Party + Pressure ---
            contentContainer.text("ACTIVE ASSIGNMENTS", textSize = 12.0, color = RGBA(0x5c, 0x3a, 0x1e, 0xff))
                .apply { x = contentRightX; y = contentRightY }

            for ((i, marker) in markers.take(6).withIndex()) {
                val markerText = if (marker.length > 38) marker.take(38) + "..." else marker
                contentContainer.text("\u25B6 $markerText", textSize = 10.0, color = RGBA(0x3a, 0x2a, 0x1a, 0xff))
                    .apply { x = contentRightX; y = contentRightY + 22.0 + i * 16.0 }
            }

            // Party name
            val partyY = contentRightY + 130.0
            contentContainer.text("REGISTERED PARTY:", textSize = 10.0, color = RGBA(0x5c, 0x3a, 0x1e, 0xff))
                .apply { x = contentRightX; y = partyY }
            contentContainer.text(partyName ?: "(unregistered)", textSize = 11.0, color = RGBA(0x3a, 0x2a, 0x1a, 0xff))
                .apply { x = contentRightX; y = partyY + 14.0 }

            // Pressure indicator
            val pressureY = partyY + 40.0
            val pressureColor = when (pressure) {
                QuestPressure.LOW -> RGBA(0x22, 0xcc, 0x22, 0xff)
                QuestPressure.MEDIUM -> RGBA(0xdd, 0xaa, 0x22, 0xff)
                QuestPressure.HIGH -> RGBA(0xcc, 0x22, 0x22, 0xff)
            }
            contentContainer.text("BUREAUCRATIC PRESSURE: ${pressure.name}", textSize = 10.0, color = pressureColor)
                .apply { x = contentRightX; y = pressureY }

            // Navigation hint
            val navHint = if (totalPages > 1) "[LEFT/RIGHT: pages]  " else ""
            contentContainer.text("${navHint}[J / ESC to close]", textSize = 9.0, color = RGBA(0x88, 0x88, 0x66, 0xff))
                .apply { x = offX + bookW / 2.0 - 80.0; y = offY + bookH - 22.0 }
        }

        // Initial render
        renderContent(currentPagePair)

        // --- Open animation: scale from 0,0.8 to 1,1 ---
        bookContainer.scaleX = 0.0
        bookContainer.scaleY = 0.8

        launch {
            bookContainer.tween(
                bookContainer::scaleX[0.0, 1.0],
                bookContainer::scaleY[0.8, 1.0],
                time = 400.milliseconds,
                easing = easeOutBack
            )
        }

        // --- Input: close + page turn ---
        var animating = false

        addUpdater {
            val keys = views.input.keys
            if (keys.justPressed(Key.J) || keys.justPressed(Key.ESCAPE)) {
                launch { sceneContainer.changeTo<WorldScene>() }
                return@addUpdater
            }

            // Page turn
            if (!animating) {
                val total = totalPagePairs()
                if (keys.justPressed(Key.RIGHT) && currentPagePair < total - 1) {
                    animating = true
                    currentPagePair++
                    launch {
                        // Flip: scaleX 1.0 -> 0.0 (fold shut)
                        turnOverlay.visible = true
                        turnOverlay.scaleX = 1.0
                        turnOverlay.tween(
                            turnOverlay::scaleX[1.0, 0.0],
                            time = 150.milliseconds,
                            easing = Easing.SMOOTH
                        )
                        // Update content at midpoint
                        renderContent(currentPagePair)
                        // Flip: scaleX 0.0 -> 1.0 (unfold)
                        turnOverlay.tween(
                            turnOverlay::scaleX[0.0, 1.0],
                            time = 150.milliseconds,
                            easing = Easing.SMOOTH
                        )
                        turnOverlay.visible = false
                        animating = false
                    }
                } else if (keys.justPressed(Key.LEFT) && currentPagePair > 0) {
                    animating = true
                    currentPagePair--
                    launch {
                        turnOverlay.visible = true
                        turnOverlay.scaleX = 1.0
                        turnOverlay.tween(
                            turnOverlay::scaleX[1.0, 0.0],
                            time = 150.milliseconds,
                            easing = Easing.SMOOTH
                        )
                        renderContent(currentPagePair)
                        turnOverlay.tween(
                            turnOverlay::scaleX[0.0, 1.0],
                            time = 150.milliseconds,
                            easing = Easing.SMOOTH
                        )
                        turnOverlay.visible = false
                        animating = false
                    }
                }
            }
        }
    }
}
