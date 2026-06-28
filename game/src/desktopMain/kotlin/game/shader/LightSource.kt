package game.shader

/**
 * Defines a point light source in tile-space.
 *
 * [tileX], [tileY] — position in the TMX grid.
 * [radius] — light reach in tiles (e.g. 5.0 = illuminates a 5-tile radius).
 * [r], [g], [b] — light color (0.0–1.0). Warm candle = (1.0, 0.8, 0.4).
 * [intensity] — base brightness (0.0–1.0). Flicker modulates this.
 * [flickerSpeed] — how fast the light flickers (0 = steady, 3.0 = candle).
 * [flickerAmount] — how much intensity varies (0.0–0.3 typical for candles).
 */
data class LightSource(
    val tileX: Int,
    val tileY: Int,
    val radius: Float = 5f,
    val r: Float = 1.0f,
    val g: Float = 0.8f,
    val b: Float = 0.4f,
    val intensity: Float = 0.9f,
    val flickerSpeed: Float = 3.0f,
    val flickerAmount: Float = 0.15f,
)
