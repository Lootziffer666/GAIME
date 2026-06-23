# Variant B — KorGE 2.5D (Kotlin-native, GPU-accelerated)

`Hd2dStage.kt` is a **scaffold** showing how to achieve a real HD-2D look while
staying in **Kotlin Multiplatform**, by swapping the rendering layer from Compose
Canvas to [KorGE](https://korge.org) — a Kotlin game engine that runs on
OpenGL / WebGL / Metal.

## Why KorGE for HD-2D?

Compose Canvas (Variant A) draws with a software-ish 2D API: no shaders, no
render-to-texture, no real blur, no additive-blend bloom without manual tricks.
KorGE gives you a **GPU pipeline** while keeping the whole project in Kotlin:

| Capability                  | Compose Canvas (A) | KorGE (B) | Three.js (C) |
|-----------------------------|:------------------:|:---------:|:------------:|
| Stays in Kotlin/KMP         | ✅ | ✅ | ❌ (JS/web) |
| GPU shader filters          | ❌ | ✅ | ✅ |
| Real blur / depth-of-field  | ❌ (faked) | ✅ `BlurFilter` | ✅ `BokehPass` |
| Additive-blend bloom        | partial | ✅ `BlendMode.ADD` | ✅ `UnrealBloomPass` |
| True 3D depth (sprites in 3D)| ❌ | ⚠️ pseudo (layers) | ✅ real |
| Reuse GAIME logic/state     | ✅ | ✅ | ⚠️ via shared backend |
| Android + Desktop targets   | ✅ | ✅ (+ web, iOS) | web only |

KorGE's HD-2D is **"2.5D by layering"**: depth-sorted parallax layers with
per-band blur, not a true 3D scene. That is exactly how the original 2D-HD games
(and most pixel "HD-2D" titles) are actually built, so it is faithful and far
cheaper than full 3D.

## What the scaffold demonstrates

1. **Depth-sorted layers** (`farLayer → midLayer → playLayer → nearLayer → fxLayer`),
   each parallax-scrolled at a different factor.
2. **Tilt-shift DoF** via `BlurFilter` applied to the far and near layers only —
   the play layer stays razor sharp (the Octopath miniature look).
3. **Bloom-like glow** through additive-blended (`BlendMode.ADD`) radial-glow
   sprites layered over the emissive Questbook and the lantern.
4. **Pixel-perfect sampling** (`smoothing = false`) so pixel art stays crisp at any scale.
5. **Atmosphere**: drifting additive dust motes + a vignette overlay.
6. **Player movement** (WASD/arrows) with a walk-bob and depth-scaling.

All bitmaps are painted procedurally so the scaffold has **no asset dependency**;
in production you replace `buildHeroBitmap(...)` etc. with:

```kotlin
val nib = resourcesVfs["sprites/nib.png"].readBitmap()
image(nib) { smoothing = false } // NearestNeighbour for pixel art
```

## How to wire it into GAIME

KorGE and Compose can't share one window, so pick one of:

### Option 1 — KorGE as a separate game module (recommended)
Add a `:game` Gradle module that depends on KorGE; keep `:composeApp` for menus /
the "waitroom" and launch the KorGE window for actual gameplay. Share the pure
Kotlin logic (BarkEvent, Questbook, GameStateMachine) via a `:core` common module.

`settings.gradle.kts`:
```kotlin
include(":composeApp", ":core", ":game")
```

`game/build.gradle.kts` (sketch):
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.soywiz.korge") version "5.4.0"   // check latest on korge.org
}
korge {
    id = "com.aiundb.gaime.game"
    targetJvm(); targetJs(); targetAndroid()
}
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))   // BarkEvent, Questbook, etc.
        }
    }
}
```

Entry point:
```kotlin
suspend fun main() = Korge(windowSize = Size(960, 540), backgroundColor = Colors["#05030a"]) {
    sceneContainer().changeTo { Hd2dStage() }
}
```

### Option 2 — Replace the Compose Canvas engine entirely
Migrate `Scene`/`SceneEngine`/`ParticleEngine` to KorGE equivalents
(`korlibs.korge.scene.Scene`, `addUpdater`, the view tree). Bigger refactor, but
unifies everything under one GPU engine.

## Verification status

⚠️ This file is an **API-faithful scaffold**, written against KorGE 5.x
conventions, but **not compiled in this environment** (KorGE is not yet a project
dependency and pulling the engine + native GL backends is a heavy first step).

Before merging gameplay on top of it:
1. Add the KorGE Gradle plugin + dependency (versions above — confirm latest).
2. Resolve exact import paths against your KorGE version (the `korlibs.*`
   package layout shifts slightly between 5.x minors).
3. Run `./gradlew :game:runJvm` and confirm a non-blank window + 60 FPS.

The intent is to show the **structure and feasibility**, so you can decide
between A (cheap, in Compose), B (Kotlin-native GPU), and C (max fidelity, web).
