# KorGE Migration & Tilemap World — Exact Plan

**Status:** Step 1 done (`:core` extracted). Step 2 in progress (`:game` KorGE
module). Steps 3–5 not started. This document is the agreed, recorded plan for a
larger, multi-step effort and is committed *before* the heavy work begins.

It extends `.kiro/steering/rendering-engine.md` (the locked KorGE 2.5D decision)
with the concrete tasks, the build setup actually used, the "donor code" policy,
and the tilemap/collision design.

---

## 1. Guiding principles (owner decisions)

These were decided by the project owner and govern every step below:

1. **KorGE is the engine — used, not rebuilt.** KorGE is an allowed dependency,
   the same way the JVM/Kotlin stdlib is. We build *on* it. We do not fork it and
   do not reimplement the engine.
2. **Everything above the engine is GAIME's own code.** Map loading, tile
   collision, prop placement, gameplay glue — original code in `:core`/`:game`.
3. **Donor-code policy (no forks, no third-party code in the tree).** External
   open-source projects (e.g. `korge-tiled`, LDtk loaders, HD-2D samples) are used
   **only as reference** ("how does it work"). We reimplement cleanly from the
   **open file-format specs** (Tiled TMX/TSX, LDtk JSON) and from observable
   behaviour — we do **not** copy source and strip notices (that would remain a
   derivative work). Result: the codebase carries **no code-level license or
   attribution obligations**.
4. **Only assets carry credits.** Art/audio (CraftPix etc.) keep their
   `license.txt`/attribution. No other part of the project requires credits.
5. **Tilemap-first world, collision derived from tiles.** The world is built from
   tiles, and collision is a property of the **tiles/layers**, not hand-painted
   per screen. The map is the data; collision is inherited from it.

The one allowed code dependency is therefore **KorGE** (permissive, no in-game
credit required). No other runtime code dependency is added by this plan.

---

## 2. Architecture (module split)

```
:core        Pure, engine-agnostic Kotlin. NO Compose, NO KorGE.
             Bark pipeline, Questbook, combat, world/grid model, chapters,
             finale/overload, state machine, i18n, items, save-state, settings,
             AND (new, Step 4) the Tiled tilemap model + loader + tile collision.
             All unit tests live here. Survives any renderer change.

:game        KorGE module. Depends on :core. The playable game: the 2.5D HD-2D
             stage, tilemap rendering, sprites/animation, input, audio playback.
             Desktop (JVM) first; Android/web later.

:composeApp  Interim. Menus / waitroom + the current Compose-Canvas gameplay
             engine (SliceScreen etc.). The Compose gameplay engine is retired
             once :game reaches parity (Step 5). Treated as throwaway.
```

---

## 3. Staged plan (each step independently buildable)

Acceptance is what **headless CI can verify**: compile `:core`/`:game`, run
`:core` tests. Opening a GL window is manual/local (the sandbox has no GL).

### Step 1 — Extract `:core` ✅ (done, PR #15)
Pure logic + tests moved to a KMP module; `:composeApp` depends on it.

### Step 2 — `:game` KorGE module (minimal compiling entry) — IN PROGRESS
- New `:game` module, `kotlin("multiplatform")`, `jvm("desktop")`, JVM 17.
- **KorGE 6.0.0 consumed as a LIBRARY** (`com.soywiz.korlibs.korge:korge:6.0.0`),
  **not** via the KorGE Gradle plugin.
  - *Rationale:* the KorGE plugin is opinionated and applies its own Kotlin
    Multiplatform plugin/version. This project already pins **Kotlin 2.1.21**
    (with the Compose plugin + AGP 8.2.2). Two Kotlin Gradle plugin versions in
    one build fail ("loaded multiple times"). Consuming KorGE as a library keeps
    the Kotlin plugin version consistent across `:core`/`:composeApp`/`:game` and
    keeps the existing green build safe. The plugin (asset packaging / native
    run-tasks) can be revisited locally when we actually launch the window.
- Minimal `suspend fun main() = Korge { ... }` that references a `:core` type to
  prove the dependency wiring.
- **Acceptance:** `./gradlew :game:compileKotlinDesktop` succeeds; existing
  `:core:desktopTest` and `:composeApp:compileKotlinDesktop` stay green.
- **Isolation/rollback:** all changes confined to `game/` + one `settings.gradle.kts`
  line. If KorGE↔Kotlin 2.1.21 compatibility blocks compilation, the `:game`
  module is reverted (build stays green) and the blocker + fix path (version
  alignment) is reported. Nothing else is touched.

### Step 3 — Port the 2.5D stage
Bring `demos/korge-hd2d/Hd2dStage.kt` into `:game`, fix `korlibs.*` imports for
6.0, render a non-blank 2.5D scene (depth-sorted parallax + per-band `BlurFilter`
DoF + additive bloom + `smoothing = false` pixel sampling). Verify: compiles.

### Step 4 — Own Tiled tilemap loader + tile collision (in `:core`)
The visual quality lever: render the **artist-authored Tiled scenes** (17 `.tmx`
already in `assets/HD/locations/...`) with their real layers and animated tiles,
instead of a flat baked backdrop.
- **Donor reference:** `scripts/tmx_render.py` (already in repo) shows exactly how
  these `.tmx` are structured — infinite/chunked CSV layers, multi-tileset
  `firstgid`/`columns`/`tilecount`, GID flip bits (H/V/D), document-order layers,
  animated-tile `<animation><frame>`. We reimplement that understanding as Kotlin.
- **New in `:core` (own code, no deps):** a `TiledMap` model + a TMX loader
  (minimal XML reading for the subset these files use) producing: tile layers
  (with gids + flips), animated-tile definitions, object/entity layers, and a
  **tile-derived collision grid**.
- **Collision-from-tiles:** classify by layer/tile semantics —
  `Floor/Ground/Road/Grass/Carpet → WALKABLE`, `Walls*/House*/Roof/Fence/Statues/
  trees* → BLOCKED`, `Water* → WATER`, `Door*/stairs/entrance/Sign → TRIGGER`,
  `*details*/Shadow*/Blood → DECORATIVE (no collision)`. Mixed `Objects*` layers
  fall back to per-tile/explicit overrides. Reuses the floor-derived rule from
  the donor (cells without a floor tile are blocked) + solid layers.
- Renderer-agnostic and **unit-tested** in `:core`. `:game` renders it with KorGE;
  the interim Compose renderer can also consume it.
- **Format choice:** **Tiled** (the 17 maps already exist; the donor parses them).
  LDtk (IntGrid collision + auto-layer scatter) remains a future option; both are
  used only as **editors** (tools), their output data is owned by us.

### Step 4b — Gameplay into KorGE
World/grid movement, HD animated sprites (Idle/Walk/Attack sheets from the packs),
dialogue, combat UI, bark audio playback — all driven by `:core` logic, rendered
in KorGE scenes. Animated props/VFX (magic book, fire/smoke/explosions) wired here
(KorGE sprite-animation/particles), since the assets already exist.

### Step 5 — Retire the Compose gameplay engine
Once `:game` reaches parity, remove the Compose-Canvas gameplay engine; keep
`:composeApp` only for non-gameplay UI if still useful.

---

## 4. Risks & mitigations

| Risk | Mitigation |
|------|------------|
| KorGE 6.0 ↔ Kotlin 2.1.21 incompatibility | Library-consumption (not plugin) keeps one Kotlin version; if klib/metadata is incompatible, report + align versions. `:game` is isolated and revertible. |
| No GL in headless sandbox | Acceptance is **compile** for `:game`; window run is manual/local. |
| KorGE API shift 5.x→6.0 (`korlibs.*` paths) | Resolve imports against 6.0 during Steps 2–3; keep the entry minimal first. |
| Scope creep | This doc fixes the steps; each is independently buildable and reviewable. |

## 5. Explicitly out of scope (separate efforts)

- **"Photo/sketch → Location Recipe" AI pipeline.** A separate, standalone tool
  (own project), not part of GAIME. Even its downstream (recipe → tilemap +
  collision + scatter) is largely covered by tile-based authoring (Tiled/LDtk
  auto-layers), so it is deferred and not a prerequisite for anything above.
- True 3D. The HD-2D look is achieved by 2.5D layering (per the locked decision).

---

## 6. Verification matrix

| Artifact | Headless check |
|----------|----------------|
| `:core` (logic, tilemap loader, collision) | `./gradlew :core:desktopTest` (unit-tested) |
| `:game` (KorGE entry, ported stage) | `./gradlew :game:compileKotlinDesktop` (compile) |
| `:composeApp` (interim) | `./gradlew :composeApp:compileKotlinDesktop` (stays green) |
| GL window / visuals | Manual/local run only |
