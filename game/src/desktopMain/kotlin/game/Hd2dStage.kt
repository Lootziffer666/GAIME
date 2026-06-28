package game

// =============================================================================
// GAIME - KorGE 2.5D HD-2D stage (migration Step 3).
// -----------------------------------------------------------------------------
// Ported from demos/korge-hd2d/Hd2dStage.kt (a KorGE 5.x-era scaffold) to the
// :game module against KorGE 6.0.0. Only the import paths and the per-frame
// updater signature were adjusted for the 6.0 API; the rendering structure and
// the procedural placeholder bitmaps are kept verbatim (real assets arrive in
// Step 4b — see briefs/2026-06-28-korge-step3-hd2d-stage.md).
//
// 6.0 API corrections vs. the 5.x scaffold (verified against the KorGE 6.0.0
// -sources.jar, the only allowed code dependency, read as reference only):
//   - `blendMode` / `alpha` are member `var`s on View, not importable symbols.
//   - `BlendMode` is a typealias in korlibs.korge.view -> korlibs.korge.blend.
//   - the `container()` builder must be imported from korlibs.korge.view.
//   - addUpdater's lambda receives a kotlin.time.Duration (not TimeSpan);
//     korlibs.time.seconds provides Duration.seconds: Double.
//   - BlurFilter's primary ctor parameter is genuinely named `radius` in 6.0.
//
// HD-2D techniques shown here:
//   1. Depth-sorted sprite layers (far -> near) with parallax scroll factors.
//   2. Pixel-perfect sampling (nearest-neighbour) for crisp pixel art.
//   3. Tilt-shift depth-of-field via BlurFilter on the far + near bands.
//   4. Bloom-like glow: additive-blended light sprites over emissive objects.
//   5. A vignette overlay + animated dust motes for atmosphere.
// =============================================================================

import korlibs.image.bitmap.Bitmap
import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import korlibs.korge.input.keys
import korlibs.korge.scene.Scene
import korlibs.korge.view.BlendMode
import korlibs.korge.view.Container
import korlibs.korge.view.SContainer
import korlibs.korge.view.View
import korlibs.korge.view.addUpdater
import korlibs.korge.view.anchor
import korlibs.korge.view.container
import korlibs.korge.view.filter.BlurFilter
import korlibs.korge.view.filter.filter
import korlibs.korge.view.image
import korlibs.korge.view.position
import korlibs.korge.view.scale
import korlibs.korge.view.solidRect
import korlibs.math.geom.Anchor
import korlibs.time.seconds
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Main HD-2D stage. KorGE calls [sceneMain] once the scene is attached.
 *
 * Layer model (back to front), each parallax-scrolled at a different factor:
 *   skyLayer (0.0) -> farLayer (0.2, blurred) -> midLayer (0.5) ->
 *   playLayer (1.0, sharp) -> nearLayer (1.4, blurred) -> fxLayer (glow/motes) ->
 *   vignette
 */
class Hd2dStage : Scene() {

    private lateinit var camera: Container
    private lateinit var farLayer: Container
    private lateinit var midLayer: Container
    private lateinit var playLayer: Container
    private lateinit var nearLayer: Container
    private lateinit var fxLayer: Container

    private lateinit var player: View
    private lateinit var questbookGlow: View

    private var time = 0.0

    override suspend fun SContainer.sceneMain() {
        val vw = width
        val vh = height

        // --- Sky: vertical gradient backdrop -------------------------------
        // (KorGE can draw gradients via a generated bitmap; here a solid dusk.)
        solidRect(vw, vh, RGBA(0x1a, 0x0f, 0x28, 0xff))

        // The camera container holds all world layers so we can pan/sway it.
        camera = container().apply { position(0.0, 0.0) }

        // --- FAR layer (blurred for depth-of-field) ------------------------
        farLayer = camera.container().apply {
            filter = BlurFilter(radius = 6.0) // tilt-shift: distant blur
        }
        repeat(7) { i ->
            farLayer.image(buildHillBitmap(RGBA(0x2b, 0x3a, 0x67, 0xff))) {
                anchor(Anchor.BOTTOM_CENTER)
                position(i * (vw / 5.0), vh * 0.55)
                scale(2.0 + (i % 3) * 0.3)
                smoothing = false // pixel-perfect
            }
        }

        // --- MID layer (buildings, sharp-ish) ------------------------------
        midLayer = camera.container()
        repeat(6) { i ->
            midLayer.image(buildBuildingBitmap()) {
                anchor(Anchor.BOTTOM_CENTER)
                position(i * (vw / 5.0) + 40.0, vh * 0.72)
                scale(3.0)
                smoothing = false
            }
        }

        // --- PLAY layer (party + questbook, fully sharp) -------------------
        playLayer = camera.container()

        // Glowing questbook (emissive) on a pedestal
        questbookGlow = playLayer.image(buildBookBitmap()) {
            anchor(Anchor.BOTTOM_CENTER)
            position(vw * 0.5, vh * 0.74)
            scale(3.0)
            smoothing = false
        }

        // Party sprites (Nib = player)
        player = playLayer.image(buildHeroBitmap(RGBA(0x7e, 0x25, 0x53, 0xff))) {
            anchor(Anchor.BOTTOM_CENTER)
            position(vw * 0.5, vh * 0.80)
            scale(3.0)
            smoothing = false
        }
        playLayer.image(buildHeroBitmap(RGBA(0xc2, 0xc3, 0xc7, 0xff))) {
            anchor(Anchor.BOTTOM_CENTER); position(vw * 0.42, vh * 0.82); scale(3.5); smoothing = false
        }
        playLayer.image(buildHeroBitmap(RGBA(0x5b, 0x6e, 0xe1, 0xff))) {
            anchor(Anchor.BOTTOM_CENTER); position(vw * 0.58, vh * 0.82); scale(3.2); smoothing = false
        }

        // --- NEAR layer (foreground occluders, blurred) -------------------
        nearLayer = camera.container().apply {
            filter = BlurFilter(radius = 4.0) // tilt-shift: foreground blur
        }
        nearLayer.image(buildHillBitmap(RGBA(0x12, 0x0a, 0x18, 0xff))) {
            anchor(Anchor.BOTTOM_CENTER); position(vw * 0.15, vh + 20.0); scale(5.0); smoothing = false
        }
        nearLayer.image(buildHillBitmap(RGBA(0x12, 0x0a, 0x18, 0xff))) {
            anchor(Anchor.BOTTOM_CENTER); position(vw * 0.9, vh + 20.0); scale(5.0); smoothing = false
        }

        // --- FX layer: additive glow + motes (bloom-like) -----------------
        fxLayer = container()

        // Warm lantern glow (additive)
        fxLayer.image(buildRadialGlow(RGBA(0xff, 0xb3, 0x5e, 0xff))) {
            anchor(Anchor.CENTER); position(vw * 0.22, vh * 0.30); scale(6.0)
            blendMode = BlendMode.ADD; alpha = 0.5
        }
        // Questbook glow (additive, animated)
        val bookGlowFx = fxLayer.image(buildRadialGlow(RGBA(0x7b, 0xe0, 0xff, 0xff))) {
            anchor(Anchor.CENTER); position(vw * 0.5, vh * 0.70); scale(5.0)
            blendMode = BlendMode.ADD; alpha = 0.6
        }

        // Dust motes
        val motes = ArrayList<View>()
        repeat(40) {
            val m = fxLayer.solidRect(2.0, 2.0, Colors["#ffe9b0"]).apply {
                position(Random.nextDouble(0.0, vw), Random.nextDouble(0.0, vh))
                blendMode = BlendMode.ADD
                alpha = Random.nextDouble(0.2, 0.8)
            }
            motes.add(m)
        }

        // --- Vignette overlay ---------------------------------------------
        image(buildVignette(vw.toInt(), vh.toInt())) {
            position(0.0, 0.0); alpha = 0.85
        }

        // --- Input: move the player ---------------------------------------
        keys {
            // Per-frame polling done in updater below
        }

        // --- Game loop -----------------------------------------------------
        addUpdater { dt ->
            val s = dt.seconds
            time += s

            // Camera sway (auto-pan idle parallax)
            val sway = sin(time * 0.25) * 30.0
            farLayer.x = sway * 0.2
            midLayer.x = sway * 0.5
            nearLayer.x = sway * 1.4

            // Player movement (WASD) - KorGE exposes views.input
            val input = views.input
            var dx = 0.0; var dz = 0.0
            if (input.keys.pressing(korlibs.event.Key.A) || input.keys.pressing(korlibs.event.Key.LEFT)) dx -= 1.0
            if (input.keys.pressing(korlibs.event.Key.D) || input.keys.pressing(korlibs.event.Key.RIGHT)) dx += 1.0
            if (input.keys.pressing(korlibs.event.Key.W) || input.keys.pressing(korlibs.event.Key.UP)) dz -= 1.0
            if (input.keys.pressing(korlibs.event.Key.S) || input.keys.pressing(korlibs.event.Key.DOWN)) dz += 1.0
            val speed = 160.0 * s
            player.x += dx * speed
            // Moving "up/down" changes scale slightly to fake depth + Y position
            player.y += dz * speed * 0.5
            // Walk bob
            val moving = dx != 0.0 || dz != 0.0
            player.scaleY = 3.0 + if (moving) abs(sin(time * 12)) * 0.18 else sin(time * 2) * 0.04

            // Questbook bob + glow pulse
            val pulse = 0.5 + 0.4 * sin(time * 2.5)
            bookGlowFx.alpha = pulse
            bookGlowFx.scale = 4.5 + sin(time * 2.5) * 0.6
            questbookGlow.y = height * 0.74 + sin(time * 2) * 4.0

            // Motes drift up + twinkle
            for (m in motes) {
                m.y -= (10.0 + (m.alpha * 20.0)) * s
                if (m.y < -4.0) m.y = height + 4.0
            }
        }
    }

    // =========================================================================
    // Procedural pixel-art bitmaps (so the scaffold compiles without assets).
    // In production these are replaced by real sprite sheets loaded with
    //   resourcesVfs["sprites/nib.png"].readBitmap()  (NearestNeighbour sampling)
    // =========================================================================

    private fun buildHeroBitmap(cloak: RGBA): Bitmap {
        val w = 12; val h = 14
        val bmp = Bitmap32(w, h, premultiplied = true)
        val skin = RGBA(0xe0, 0xa8, 0x68, 0xff)
        val hair = RGBA(0x3a, 0x24, 0x17, 0xff)
        // head
        for (x in 4..7) { bmp[x, 1] = hair; bmp[x, 2] = skin; bmp[x, 3] = skin }
        bmp[5, 2] = RGBA(0x1a,0x1a,0x1a,0xff); bmp[6, 2] = RGBA(0x1a,0x1a,0x1a,0xff) // eyes
        // body (cloak)
        for (y in 4..10) for (x in 3..8) bmp[x, y] = cloak
        // legs
        for (y in 11..13) { bmp[4, y] = hair; bmp[7, y] = hair }
        return bmp
    }

    private fun buildBookBitmap(): Bitmap {
        val w = 10; val h = 8
        val bmp = Bitmap32(w, h, premultiplied = true)
        val book = RGBA(0x7b, 0xe0, 0xff, 0xff)
        val pageHi = RGBA(0xbf, 0xe9, 0xff, 0xff)
        val gold = RGBA(0xff, 0xd9, 0x8a, 0xff)
        for (y in 0 until h) for (x in 0 until w) bmp[x, y] = book
        for (y in 1 until h - 1) for (x in 1 until w - 1) bmp[x, y] = pageHi
        bmp[3, 2] = gold; bmp[6, 2] = gold; bmp[3, 5] = gold; bmp[6, 5] = gold
        return bmp
    }

    private fun buildHillBitmap(color: RGBA): Bitmap {
        val s = 24
        val bmp = Bitmap32(s, s, premultiplied = true)
        val cx = s / 2.0
        for (y in 0 until s) for (x in 0 until s) {
            val dx = x - cx
            val rim = (s - y).toDouble()
            if (dx * dx < rim * rim * 0.9) bmp[x, y] = color
        }
        return bmp
    }

    private fun buildBuildingBitmap(): Bitmap {
        val w = 18; val h = 24
        val bmp = Bitmap32(w, h, premultiplied = true)
        val wall = RGBA(0x1c, 0x12, 0x24, 0xff)
        val win = RGBA(0xff, 0xc2, 0x6b, 0xff)
        for (y in 0 until h) for (x in 0 until w) bmp[x, y] = wall
        // lit windows pattern
        for (wy in 0 until 4) for (wx in 0 until 2) {
            if ((wx + wy) % 2 == 0) {
                val ox = 3 + wx * 8; val oy = 3 + wy * 5
                for (yy in 0..2) for (xx in 0..3) bmp[ox + xx, oy + yy] = win
            }
        }
        return bmp
    }

    private fun buildRadialGlow(color: RGBA): Bitmap {
        val s = 64
        val bmp = Bitmap32(s, s, premultiplied = true)
        val c = s / 2.0
        for (y in 0 until s) for (x in 0 until s) {
            val dx = (x - c) / c; val dy = (y - c) / c
            val d = dx * dx + dy * dy
            val a = ((1.0 - d).coerceIn(0.0, 1.0) * 255).toInt()
            bmp[x, y] = RGBA(color.r, color.g, color.b, a)
        }
        return bmp
    }

    private fun buildVignette(w: Int, h: Int): Bitmap {
        val bmp = Bitmap32(w, h, premultiplied = true)
        val cx = w / 2.0; val cy = h / 2.0
        val maxR = kotlin.math.hypot(cx, cy)
        for (y in 0 until h) for (x in 0 until w) {
            val d = kotlin.math.hypot(x - cx, y - cy) / maxR
            val a = ((d - 0.55).coerceIn(0.0, 1.0) / 0.45 * 200).toInt()
            bmp[x, y] = RGBA(0, 0, 0, a)
        }
        return bmp
    }
}
