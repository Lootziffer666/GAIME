---
name: gaime-shaders
description: >
  GAIME's hard-won KorGE shader, overlay, collision and rendering knowledge. Use
  for ANY visual work in the :game module — writing/registering a ShaderFilter,
  composing multiple shaders, rendering tile/state overlays, CollisionGrid layer
  classification, the offscreen screenshot verification loop, or the 1440p +
  doodle-line + hi-res-image rendering direction. Read this BEFORE touching
  game/src/desktopMain/kotlin/game/shader/** or any *Overlay.kt, before writing a
  Kiro brief that involves :game visuals, and before debugging "the effect
  isn't visible".
keywords: ["korge", "shader", "shaderfilter", "composedfilter", "overlay", "collisiongrid", "anime4k", "doodle", "1440p", "screenshot", "tilemap"]
---

# GAIME Shaders & Rendering

Distilled from the Step 7a–11 shader/rendering work. The recurring failures here
were NEVER in `:core` logic (always tested, always solid) — they were in `:game`
rendering: filter clobber, overlay alpha/positioning, collision layer order,
unverified screenshots. This skill exists so those are never re-derived.

## The one rule that prevents most bugs

**Render ≠ logic. A screenshot rendering fine does NOT mean the logic works** — a
character sprite is drawn at its spawn even if that tile is BLOCKED or in water,
and an overlay can be "rendered" yet invisible (too-low alpha or off-camera).
**Always open the PNG and look.** Verify walkability/collision separately.

## Architecture (locked)

- **`:core` = engine-agnostic, grid-based logic** (`rpg.tiled.CollisionGrid`,
  `rpg.weather.*` grids — Water/Snow/Blood/Footprint/Temperature/MaterialFatigue,
  all on `offsetX/offsetY` tile coords). Renderer-agnostic, unit-tested. NEVER
  needs to know how the world is displayed.
- **`:game` = rendering only**: KorGE `ShaderFilter`s (`game/shader/`), `*Overlay`
  classes (rects from a `:core` grid), input/camera. Consume `:core`, don't
  reimplement logic here.
- **The grid is the unit of measure.** Image/tiles = skin; the grid = logic. Each
  reference image defines its OWN grid (own native tile size) → no fixed tile size,
  no hardcoded character scale (see Rendering vision below).

## Writing a ShaderFilter (KorGE 6.0)

Template = `game/shader/PoisonFilter.kt`. Each filter:
- extends `ShaderFilter()`, has an `object XUB : UniformBlock(fixedLocation = N)`
  with `by float()/vec2()/vec4()` uniforms, a `companion object : BaseProgramProvider()`
  with `override val fragment = FragmentShader { ... }`, and
  `override val programProvider get() = X`.
- Mutable knobs as `var` ctor params (e.g. `var intensity`, `var time`).
- Register it in `ShaderEffects` AND have `startTimeUpdater(root)` push `time`.
- Reproducible screenshots: drive animation from `u_Time`, set it to a fixed value
  in captures. NEVER `Random` in a shader.

### UniformBlock fixedLocation registry (avoid collisions)
```
6  PoisonFilter        7  BeerGoggleFilter     8  LightingFilter
9  RainFilter          10 HeatShimmerFilter    (Fog uses its own)
11+ free  → new filters (DoodleLineFilter etc.) take the next free slot
```

## Composing multiple shaders (the Step 8 clobber bug)

A `Container` has exactly ONE `filter`. `target.filter = x` followed by
`target.filter = y` **silently drops x**. This wrecked "The Frozen Approach"
(fog overwrote lighting → looked like day, not night).

- Use `ShaderEffects.enable(target, filter)` / `disable(...)` — it maintains an
  ordered active set and assigns a KorGE `ComposedFilter` (added Step 9). Multiple
  effects coexist.
- Or **per-layer**: different containers get different filters (background crisp,
  character layer doodled). This is how the render vision works.
- The legacy `attach*` helpers still exist as single-filter convenience wrappers.

## Rendering state overlays (the Step 7d/8/9 recurring bug)

`*Overlay` draws rects from a `:core` grid (`WaterOverlay.kt` is the reference).
Two failure modes that hit EVERY new overlay (seasons, material, puddles):
1. **Alpha too low** → invisible over busy art / under another filter. Use an
   alpha FLOOR: `RGBA(r,g,b, (110 + depth.coerceAtMost(1f)*140).toInt())`.
2. **Off-camera placement** → rects drawn at screen origin / fixed coords instead
   of in mapView space around the camera-centered player. Place at
   `(gridX + grid.offsetX) * tilePx` in the SAME container as the map, and in
   captures fill the grid AROUND the player's spawn tile (camera centers on the
   player) — else the effect is off-screen and you "see nothing".

When a brief adds an overlay, point Kiro at `WaterOverlay` as the binding template
and demand a screenshot where the effect is unmistakably visible.

## CollisionGrid (rpg.tiled)

Honours **Tiled layer order — topmost non-decorative layer wins** (matches how
Tiled renders the visible surface). Role → type: FLOOR/BRIDGE → WALKABLE,
WATER → WATER, SOLID → BLOCKED, TRIGGER → TRIGGER, DECORATIVE → skipped.
Consequences that were learned the hard way:
- **Water is a full base layer** in CraftPix maps (painted under everything). Land
  drawn on top must win → because water is declared first, floor-on-top overrides
  it. (A pond drawn on top of ground → water wins. Both correct via layer order.)
- **Bridges span water** → `bridge*` layers are WALKABLE and must beat water.
- **Trees block** (`trees*` = SOLID) — obstacles you walk around, not walk-through;
  destructible later via `BRUGG_ATTACK → ClearObstacle`. Heroes-home keeps foliage
  in DECORATIVE `Objects` layers, hence walkable.
- **B004 — infinite TMX has negative offsets.** NEVER hardcode spawn/exit tiles;
  derive from `CollisionGrid` (offset + WALKABLE bbox) and verify the cell is
  WALKABLE/TRIGGER. Spawns landing in water/blocked = player stuck.

## Screenshot verification loop (the only real proof)

```
bash scripts/setup-gl.sh          # one-time: Mesa EGL headless GL
./gradlew :game:screenshot        # writes build/screenshots/*.png
```
- **B007 (recurring!):** `ScreenshotHarness` MUST use
  `private val OUT = localCurrentDirVfs["build/screenshots"]`
  (`import korlibs.io.file.std.localCurrentDirVfs`). Kiro keeps reverting it to
  `localVfs("...")` which writes to `/build/screenshots`. Put this exact line in
  every brief's DO_NOT_TOUCH.
- The task may transiently fail with GL "Too many callbacks" — just re-run once.
- New captures: fixed `time`, pre-filled grids, tween end-states set directly.
- After rendering, **Read the PNG and look** — confirm the effect, not just that a
  file appeared.

## Rendering vision (north star)

- **1440p output.** Hi-res **painted image** as background (AI map art, e.g.
  `assets/HD/backgrounds/*.png`), drawn crisp (`smoothing = true`, no shader).
- **Character layer** through a **doodle line shader** (DoodleLineFilter:
  reimplemented classic Anime4K line-thinning/darken + `u_Time` jitter → "boil"),
  on its OWN container. Full **Anime4K HQ A+B** is a later shader-chain swap on
  this proven scaffold.
- **Grid-as-unit:** `screenTile = OUTPUT_H / gridRows`;
  `charScale = tilesTall * screenTile / 64` (CraftPix frame = 64px);
  `pos = cell * screenTile`. Same world proportion across maps despite different
  grids. No hardcoded pixel scales.
- **Invisible logic grid under the image:** segment the image (tools/mapbuilder →
  TMX) → `CollisionGrid` is the logic substrate beneath the painting.
- **Three map tiers:** World map (region, tiny figures, NO doodle) → local exterior
  → gameplay interior (full-size figures, doodle reads). Doodle is a near/gameplay
  feature only.
- **Donor policy:** Anime4K & co. reimplemented from concept, NEVER copy foreign
  code into the tree (KORGE_MIGRATION_PLAN §1).

### Pixel-art upscale — what actually works (Step 11 experiment)

Low resolution is NOT the blocker; the **upscale method** is. Verified by direct
comparison on a 64px CraftPix sprite scaled ~6×:
- **bilinear** (`smoothing = true`) → soft/blurry. This is why the first doodle
  shots looked washed out.
- **nearest** (`smoothing = false`) → crisp, blocky — sharp base.
- **The Step 11 `DoodleLineFilter` SOFTENS instead of adding crisp lines** — it
  samples bilinearly and only luminance-darkens, so it washes the sprite out
  rather than drawing outlines. It is NOT the right algorithm.
The real "nice upscale to clean doodle lines" needs an **edge-aware, point-sampled**
shader (true Anime4K/xBR/hqx concept): sample the low-res texels with nearest,
reconstruct smooth edges, then draw/dilate a dark outline. Build that on a
nearest base, not bilinear. Sprites load fine in the harness — earlier
"invisible character" was scale/off-screen placement, not a load failure.

## tools/mapbuilder (owner tool, Python)

Photo/sketch → OpenCV HSV segmentation → WFC → TMX (`Floor`→WALKABLE,
`Walls`→SOLID, `Water`→WATER). **Pixel-precise sizing:** `grid = image_px / 16`
so the TMX matches the source size (big maps preserve detail; 48×27 was lossy).
Known HSV limits: sky/blue-roof → false water; soft pixel-art buildings under-detected.
Don't touch it without being asked — it's the owner's.
