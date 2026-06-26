package rpg.render

/**
 * Canonical pixel metrics for the HD-2D top-down renderer.
 *
 * The slice originally shipped with **16 px** native art that the renderer
 * upscaled 3× at draw time (blocky). The art direction target is **native
 * 48 px** source frames (see `docs/HD48_MIGRATION.md`), so the on-screen size is
 * unchanged but the source pixels are crisp.
 *
 * All render code (e.g. `WorldScene`) and the asset-rebuild tooling
 * (`scripts/atlas_rebuild.py`, `scripts/asset_upscale.py`) reference these
 * constants instead of hard-coding `16` / `48` / `3` so the migration stays
 * consistent in one place.
 */
object RenderMetrics {

    /** Legacy native tile/sprite edge length of the original placeholder art. */
    const val LEGACY_TILE: Int = 16

    /** Art-direction target native tile/sprite edge length. */
    const val TARGET_TILE: Int = 48

    /** On-screen tile edge length in px (kept equal to [TARGET_TILE]). */
    const val SCREEN_TILE: Float = TARGET_TILE.toFloat()

    /** Integer upscale factor applied when promoting legacy art to HD. */
    const val UPSCALE: Int = TARGET_TILE / LEGACY_TILE // 3

    /**
     * Source edge length to read frames from a given atlas/sheet. HD art that is
     * already authored at [TARGET_TILE] should pass [TARGET_TILE]; legacy 16 px
     * sheets still in use pass [LEGACY_TILE].
     */
    fun sourceTileFor(isHd: Boolean): Int = if (isHd) TARGET_TILE else LEGACY_TILE
}
