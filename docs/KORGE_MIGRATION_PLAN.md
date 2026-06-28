# KorGE Migration & Tilemap World — Exact Plan

**Status:** Step 1 done (`:core` extracted). Step 2 done (`:game` KorGE module).
Step 3 done (2.5D HD-2D stage ported & compiling against KorGE 6.0.0).
Step 3b done (Android target für `:game`). Step 4 done (Tiled tilemap loader +
tile-derived collision in `:core`). Step 4b done (TiledMap renderer + PlayerSprite
in `:game`). Step 5a done (real sprites, BattleScene, audio). Step 5b done (world layer: smooth
movement, NPCs, dialog, HUD, map transitions). Step 5 done (Compose gameplay
engine retired; `:composeApp` = Waitroom-only). This document is the agreed, recorded plan for a
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

### Step 2 — `:game` KorGE module (minimal compiling entry) — ✅ done
- New `:game` module, `kotlin("multiplatform")`, `jvm("desktop")`.
- **KorGE 6.0.0 consumed as a LIBRARY** (`com.soywiz.korge:korge:6.0.0`),
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

### Step 3 — Port the 2.5D stage — ✅ done
Brought `demos/korge-hd2d/Hd2dStage.kt` into `:game` as
`game/src/desktopMain/kotlin/game/Hd2dStage.kt`, fixed the `korlibs.*` imports
for KorGE 6.0, and made `Main.kt` boot into it via
`sceneContainer().changeTo<Hd2dStage>()`. The scene renders a depth-sorted
parallax stack with per-band `BlurFilter` DoF, additive bloom glow, and
`smoothing = false` pixel sampling. Procedural placeholder bitmaps are kept (real
assets land in Step 4b). **Acceptance met:** `:game:compileKotlinDesktop`,
`:core:desktopTest`, `:composeApp:compileKotlinDesktop` all green.

**JVM target finding (build config):** KorGE 6.0.0 publishes its JVM artifacts as
**JVM target 21** bytecode (verified: all 948 classes in `korge-core-jvm` and the
inline builders in `korge-jvm` are class-file major 65 = Java 21). The first
*inline* KorGE calls (`container()`, `image()`, `solidRect()`, `addUpdater {}`,
`sceneContainer()`, `changeTo<>()`) therefore cannot be inlined into a JVM-17
target. `:game` was raised from `JVM_17` to **`JVM_21`** (desktop-only module, no
Android target, so `:core`/`:composeApp` stay at 17 for Android). This is the
blocker the §4 risk table anticipated; Step 2's minimal `Korge { println }` never
exercised it because `Korge(...)` itself is not inline.

### Step 3b — Android target für `:game` — ✅ done
`:game` erhielt neben `jvm("desktop")` ein `androidTarget()` + `id("com.android.library")`
(AGP 8.2.2, compileSdk 34, minSdk 24). KorGE-Dependency von `desktopMain` nach
`commonMain` verschoben → beide Targets erben sie. Android-Target kompiliert lokal
mit SDK; Sandbox hat kein SDK (identisch mit `:composeApp`-Android-Target). Desktop
weiterhin grün. Minimales `androidMain/kotlin/game/.gitkeep` als Marker.

### Step 4 — Own Tiled tilemap loader + tile collision (in `:core`) — ✅ done
`core/src/commonMain/kotlin/rpg/tiled/` enthält:
- **`TiledMap.kt`** — reine data classes (TiledMap, Tileset, AnimationFrame,
  TileLayer, TileCell). Keine Dependencies.
- **`TmxLoader.kt`** — minimaler Zeilenweise-Parser für das TMX-Subset der
  CraftPix-Karten (CSV, infinite-Chunks, Flip-Bits, animierte Tiles). Keine
  externe XML-Bibliothek.
- **`CollisionGrid.kt`** — Tile-abgeleitetes Kollisionsraster; Strategie:
  Floor → WALKABLE, kein Floor → BLOCKED, SOLID-Layer → BLOCKED, WATER → WATER,
  TRIGGER → TRIGGER, DECORATIVE → ignoriert. Bounding-Box über alle Zellen;
  negative Grid-Koordinaten normalisiert.

Unit-Tests (`core/src/commonTest/kotlin/rpg/tiled/`): TmxLoaderTest (finite map,
infinite chunks, flip bits, animated tiles, multiple tilesets, empty cells) +
CollisionGridTest (floor rule, solid override, water, trigger, no-floor, negative
coords). Alle grün.

### Step 4b — TiledMap renderer + PlayerSprite in `:game` — ✅ done
Connected `:core`'s `TmxLoader`/`CollisionGrid` with KorGE rendering:
- **`TilesetAtlas.kt`** — loads tileset PNGs, provides `sliceFor(localTileId)`.
- **`TiledMapView.kt`** — renders all tile layers with flip bits + animated tiles.
- **`PlayerSprite.kt`** — grid-based player with Idle/Walk animation (procedural
  placeholder bitmaps for compile-check; real sheets in Step 5).
- **`TiledMapScene.kt`** — orchestrates: TMX load → collision → atlases → render →
  keyboard input with collision check → camera follow.
- `Main.kt` boots into `TiledMapScene`. `Hd2dStage` stays as reference.
**Acceptance met:** all 3 compile checks green.

### Step 5a — Real Sprites + BattleScene + Audio — ✅ done
Replaces procedural placeholders with real HD sprite sheets, adds a turn-based
BattleScene wired to `:core`'s `CombatEngine`, and introduces background music
+ SFX via KorGE Audio API:
- **`SpriteLoader.kt`** — loads CraftPix horizontal sheets, slices into frames.
- **`CharacterSprite.kt`** — loads Swordsman/Vampire sheets, Idle/Walk/Attack/
  Hurt/Death animations, directional facing, fallback to procedural bitmaps.
- **`AudioManager.kt`** — `readMusic()` for streaming BGM, `readSound()` for SFX,
  graceful degradation in headless environments.
- **`BattleScene.kt`** — side-by-side combat, HP bars, turn-based via
  `CombatEngine.tick()`, Attack/Heal/Flee controls, VICTORY/DEFEAT state.
- **`TiledMapScene.kt`** — upgraded: uses `CharacterSprite`, plays BGM, SPACE
  triggers `BattleScene`.
**Acceptance met:** all 3 compile checks green.

### Step 5b — World Layer: Smooth Movement, NPCs, Dialog, HUD, Map Transitions — ✅ done
Full gameplay world layer replacing TiledMapScene as boot target:
- **`DialogLine.kt`** + **`NpcDefinition.kt`** — data models for dialog + NPCs.
- **`MapConfig.kt`** — location configs (Interior + Exterior) with NPCs, exits, BGM.
- **`DialogOverlay.kt`** — screen-fixed dialog box, line-by-line advance, pauses world.
- **`HudOverlay.kt`** — HP bar, gold counter, location name (screen-fixed).
- **`WorldScene.kt`** — orchestrates everything: TMX load, NPCs, smooth movement,
  dialog interaction (E key), collision + NPC blocking, map transitions via exit tiles,
  battle trigger (SPACE), camera follow with interpolation.
- **`CharacterSprite.kt`** extended: smooth 160ms tile-to-tile interpolation
  (`startMove`/`isMoving`/`visualGridX/Y`), generic `loadFromSheet()` for NPCs,
  `Facing.dx/dy` extensions.
- `Main.kt` → `WorldScene`, `BattleScene` Q → `WorldScene`.
**Acceptance met:** all 3 compile checks green.

### Step 5 — Retire the Compose gameplay engine — ✅ done
`:composeApp` reduced to Waitroom-only:
- **Moved to `:core`:** `SliceDirector.kt` (incl. `BarkOutcome`, `CombatTurn`),
  `AudioPlayer.kt`, `BarkAudioPlayer.kt`, + 4 pipeline tests.
- **Removed:** 6 Compose-gameplay-UI files (`SliceScreen`, `RpgDemoScreen`,
  `RpgWorldScreen`, `WorldScene`, `SceneAtmosphere`, `DialogueLine`) +
  `NpcDialogueTest` (tested deleted UI).
- **Kept:** `engine/` package (Waitroom uses it), `GameCanvas.kt` (Waitroom uses it),
  `PlatformAudioPlayer` expect/actual, gamepad code (in-doubt-keep rule).
- `App.kt` reduced to render only `WaitroomScreen`.
**Acceptance met:** all modules compile, all tests green (incl. moved tests in `:core`).

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
