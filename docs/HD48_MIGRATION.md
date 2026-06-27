# 16 px → 48 px HD rebuild

The vertical slice shipped with **16 px** placeholder art that the renderer
upscaled 3× at draw time (`WorldScene` blits at `FilterQuality.None`, tile size
48). The art-direction target is **native 48 px** source frames sourced from the
HD packs in `assets/HD/` — same on-screen size, crisp source pixels, room for
real detail.

This doc is the repeatable template for that rebuild. Nothing here changes
gameplay or map data: the atlas grid and every `atlasIndex` in
`core/src/commonMain/kotlin/rpg/world/GameMaps.kt` are preserved.

## Single source of truth

Pixel metrics live in `core/src/commonMain/kotlin/rpg/render/RenderMetrics.kt`:

| constant | value | meaning |
|---|---|---|
| `LEGACY_TILE` | 16 | original native edge |
| `TARGET_TILE` | 48 | HD native edge |
| `SCREEN_TILE` | 48f | on-screen tile edge |
| `UPSCALE` | 3 | integer promote factor |

Render code references these instead of literals (e.g. `WorldScene`'s `srcTile`
/ `tilePx` defaults). The Python tools default to the same 16/48/3.

## Tooling

| script | purpose |
|---|---|
| `scripts/asset_upscale.py` | nearest-neighbour upscale a PNG or a whole folder (16→48, or `--to N`, or `--scale N`) |
| `scripts/atlas_rebuild.py upscale` | re-bake an existing atlas so each tile is native 48 px, grid preserved |
| `scripts/atlas_rebuild.py pack` | assemble individual 48 px tile PNGs into an indexed atlas (indices stay map-compatible) |
| `scripts/hd48_manifest.json` | mapping of each drawable / atlas index → chosen `assets/HD/...` source frame |

Requires Pillow: `pip install Pillow`.

## Workflow

There are two ways to land a native-48 asset; pick per target.

### A. Quick promote (keep the look, gain the pipeline)

Nearest-neighbour upscale the current placeholder. Visually identical to today
but now a real 48 px asset you can hand-paint over.

```bash
# whole dungeon atlas, grid + indices preserved
python3 scripts/atlas_rebuild.py upscale \
  composeApp/src/commonMain/composeResources/drawable/tileset_dungeon.png \
  -o build/hd48-preview/tileset_dungeon_48.png

# a single tile / sprite
python3 scripts/asset_upscale.py \
  composeApp/src/commonMain/composeResources/drawable/tile_floor.png \
  -o build/hd48-preview/tile_floor_48.png
```

### B. HD swap (the actual glow-up)

Pick crisp frames from `assets/HD/` (see `assets/HD/README.md` for the map of
which pack feeds which game area), resize/crop to 48 px, and pack them at the
indices the maps already use:

```bash
python3 scripts/atlas_rebuild.py pack --columns 12 --tile 48 \
  --place 48=assets/HD/tilesets/dungeon/PNG/floor.png \
  --place 14=assets/HD/tilesets/dungeon/PNG/wall.png \
  --place 45=assets/HD/tilesets/dungeon/PNG/door.png \
  -o build/hd48-preview/tileset_dungeon_48.png
```

Record each decision in `scripts/hd48_manifest.json`.

## Landing it in the game

1. Verify the preview PNGs look right (size + transparency).
2. Copy the rebuilt PNG over the drawable in
   `composeApp/src/commonMain/composeResources/drawable/` (same filename keeps
   the generated `Res.drawable.*` accessor stable).
3. For non-atlas sprites authored larger than one tile (bosses, portraits),
   keep them oversized and let the existing draw code scale to the destination
   rect — only the *source* resolution improves.
4. Rebuild; because the grid and indices are unchanged, maps, triggers and
   collision are unaffected.

## Git / LFS

PNGs (and `*.gif/.aseprite/.tmx/.tsx/.otf/.ttf/.wav/.ogg/.mp3`) are tracked via
**Git LFS** (`/.gitattributes`). Heavy editing sources (`*.psd`) and vendor
promo files are `.gitignore`d. `build/` is ignored, so the preview outputs above
are throwaway — only copy the chosen result into `composeResources/` to commit.
