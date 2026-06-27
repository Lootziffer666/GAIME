# Rendering Engine Decision: KorGE 2.5D

**Status:** DECIDED (locked). Supersedes the "three variants" comparison in
`demos/HD2D_VARIANTS.md`.

## Decision

GAIME's renderer is **KorGE 2.5D** (Variant B from the HD-2D demos).

- Kotlin-native, GPU-accelerated (OpenGL / WebGL / Metal), stays in Kotlin
  Multiplatform — no separate web/JS stack.
- HD-2D look is achieved by **2.5D layering**: depth-sorted parallax layers with
  per-band `BlurFilter` (tilt-shift depth-of-field), additive-blend bloom glow,
  and pixel-perfect (`smoothing = false`) sprite sampling. This is how real
  HD-2D / "2D-HD" titles are built — faithful and far cheaper than true 3D.
- Targets: Desktop (JVM) + Android (+ web/iOS available later).

Rejected: Variant A (Compose Canvas — no GPU shaders, faked DoF/bloom) and
Variant C (Three.js — would be a separate web app, not in KMP).

## Agreed Architecture (Option 1 from `demos/korge-hd2d/README.md`)

KorGE and Compose cannot share one window, so the project splits into modules:

```
:core        Pure, engine-agnostic Kotlin logic. NO Compose, NO KorGE deps.
             BarkEvent / BarkRegistry / BarkEventBus, QuestbookProcessor,
             QuestPressure, RoomContext, CombatEngine + controllers,
             EnemyArchetype, Chapter, QuestbookOverload, GameStateMachine, etc.
             All commonTest unit tests move here.

:game        KorGE module. Depends on :core. The actual playable game:
             scenes, the 2.5D HD-2D stage, sprites, input, audio playback.
             Entry point launches the KorGE window.

:composeApp  Stays for menus / the "waitroom" / non-gameplay UI. Depends on
             :core. The Compose Canvas gameplay engine (Scene/SceneEngine/
             ParticleEngine/SliceScreen) is retired in favour of :game.
```

The KorGE scaffold to port lives at `demos/korge-hd2d/Hd2dStage.kt`.

## Version

- KorGE Gradle plugin: `com.soywiz.korlibs.korge.plugins:korge-gradle-plugin`.
  Latest line is 6.x (5.4.0 also available). Confirm the exact latest before
  pinning, and resolve `korlibs.*` import paths against the chosen version.

## Migration Plan (staged, each step independently buildable)

1. **Extract `:core`** — move pure logic + tests out of `:composeApp` into a new
   KMP `:core` module; `:composeApp` depends on `:core`. No behaviour change.
   (De-risks everything; needed regardless of renderer.)
2. **Add `:game` KorGE module** — KorGE plugin + dependency, minimal compiling
   entry point, depends on `:core`. Confirm toolchain resolves.
3. **Port the 2.5D stage** — bring `Hd2dStage.kt` in, compile it against the
   pinned KorGE version, render a non-blank scene.
4. **Port gameplay** — world/grid, sprites, dialogue, combat UI, bark audio
   playback into KorGE scenes, driven by `:core` logic.
5. **Retire the Compose Canvas engine** once `:game` reaches parity.

## Constraints

- The pure-logic pipeline (bark -> Questbook -> effect, pressure, combat) is the
  source of truth and must remain renderer-agnostic in `:core`. Renderers only
  visualise it.
- Pixel art stays crisp: always `smoothing = false` (nearest-neighbour).
- Headless CI can compile and unit-test `:core` and compile `:game`, but cannot
  open a GL window — visual verification is manual/local.
