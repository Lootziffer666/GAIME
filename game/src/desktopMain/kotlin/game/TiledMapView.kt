package game

import korlibs.image.bitmap.BmpSlice
import korlibs.korge.view.Container
import korlibs.korge.view.Image
import korlibs.korge.view.addUpdater
import korlibs.korge.view.container
import korlibs.korge.view.image
import korlibs.math.geom.Angle
import korlibs.time.milliseconds
import rpg.tiled.AnimationFrame
import rpg.tiled.TiledMap

/**
 * KorGE [Container] that renders all tile layers of a [TiledMap] using preloaded
 * [TilesetAtlas] instances.
 *
 * Features:
 * - Pixel-perfect rendering (smoothing = false)
 * - Flip bits (H, V, D) handled per Tiled spec
 * - Animated tiles driven by addUpdater
 */
class TiledMapView(map: TiledMap, atlases: List<TilesetAtlas>) : Container() {

    /**
     * Internal state for a single animated tile image.
     */
    private class AnimatedTile(
        val image: Image,
        val atlas: TilesetAtlas,
        val frames: List<AnimationFrame>,
    ) {
        var frameIndex: Int = 0
        var elapsedMs: Float = 0f
    }

    private val animatedTiles = mutableListOf<AnimatedTile>()

    init {
        for (layer in map.layers) {
            val layerContainer = container {
            }

            for (cell in layer.cells) {
                if (cell.gid == 0) continue
                val resolved = atlases.resolveGid(cell.gid) ?: continue
                val (atlas, localId) = resolved
                val slice = atlas.sliceFor(localId)

                val tileImage = layerContainer.image(slice) {
                    x = (cell.gridX * map.tileWidth).toDouble()
                    y = (cell.gridY * map.tileHeight).toDouble()
                    smoothing = false

                    // Flip bits (pivot is tile center per Tiled spec)
                    if (cell.flipH) {
                        scaleX = -1.0
                        x += map.tileWidth.toDouble()
                    }
                    if (cell.flipV) {
                        scaleY = -1.0
                        y += map.tileHeight.toDouble()
                    }
                    // flipD = anti-diagonal flip = 90-degree rotation + horizontal flip
                    if (cell.flipD) {
                        rotation = Angle.fromDegrees(90.0)
                        scaleX *= -1.0
                    }
                }

                // Check if this tile has animation frames
                val animFrames = atlas.tileset.animatedTiles[localId]
                if (animFrames != null && animFrames.isNotEmpty()) {
                    animatedTiles.add(AnimatedTile(tileImage, atlas, animFrames))
                }
            }
        }

        // Animation updater: advances animated tile frames based on elapsed time
        addUpdater { dt ->
            val dtMs = dt.milliseconds.toFloat()
            for (anim in animatedTiles) {
                anim.elapsedMs += dtMs
                val currentFrame = anim.frames[anim.frameIndex]
                if (anim.elapsedMs >= currentFrame.durationMs) {
                    anim.elapsedMs -= currentFrame.durationMs.toFloat()
                    anim.frameIndex = (anim.frameIndex + 1) % anim.frames.size
                    val newFrame = anim.frames[anim.frameIndex]
                    anim.image.bitmap = anim.atlas.sliceFor(newFrame.tileId)
                }
            }
        }
    }
}
