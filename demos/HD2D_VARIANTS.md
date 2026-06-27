# HD-2D Upgrade — Three Variants for GAIME

> **DECIDED:** The project uses **Variant B — KorGE 2.5D**. See
> `.kiro/steering/rendering-engine.md` for the locked decision, the agreed
> `:core` / `:game` / `:composeApp` module architecture, and the migration plan.
> This document is kept for historical comparison only.

The GAIME design (`docs/GAME_CONCEPT_LOCK.md`) calls for an "8/16-bit top-down
pixel RPG". This folder contains **three working demonstrations** of how to push
that look toward a premium **HD-2D** feel (think *Octopath Traveler*: pixel art +
3D depth, dynamic lighting, depth-of-field, bloom).

Each variant is a real, inspectable artifact so you can compare before committing
the project to one rendering direction.

| | **A — Compose Canvas** | **B — KorGE** | **C — Three.js (web)** |
|---|---|---|---|
| **Folder** | `composeApp/.../scenes/Hd2dDemoScene.kt` | `demos/korge-hd2d/` | `demos/web-hd2d/` |
| **Language / stack** | Kotlin + Compose (current) | Kotlin Multiplatform (KorGE) | JavaScript + Three.js |
| **Effort to adopt** | 🟢 Tiny (already wired in) | 🟡 Medium (new module) | 🔴 Large (parallel web app) |
| **Stays in current project** | ✅ yes | ✅ yes (Kotlin) | ❌ separate web build |
| **GPU shaders** | ❌ | ✅ | ✅ |
| **Real depth-of-field** | faked (gradient bands) | ✅ `BlurFilter` | ✅ `BokehPass` |
| **Bloom** | partial (screen-blend) | ✅ additive | ✅ `UnrealBloomPass` |
| **True 3D depth** | ❌ (2D layers) | ⚠️ pseudo (layers) | ✅ sprites in 3D |
| **Visual ceiling** | "polished 2D" | "real HD-2D (layered)" | "max fidelity HD-2D" |
| **Runs on Android + Desktop** | ✅ | ✅ (+ web, iOS) | web only |
| **Build verified here** | ⚠️ API-reviewed¹ | ⚠️ scaffold² | ✅ syntax + deps³ |

¹ No Android SDK in the build sandbox, so a full Compose-MP build was not run.
Code was reviewed against the existing engine's API conventions (it reuses the
same `Scene`, `DrawScope`, `Brush`, `BlendMode`, and `0xFF_..` color patterns
already compiling in the project).
² KorGE is not yet a project dependency; the file is an API-faithful scaffold,
not a compiled build. See `korge-hd2d/README.md`.
³ `node --check` passes and all CDN modules resolve (HTTP 200); run in a browser
to view pixels.

---

## Variant A — Compose Canvas (cheapest, ships today)

**File:** `composeApp/src/commonMain/kotlin/engine/scenes/Hd2dDemoScene.kt`
**Try it:** run the app → scene picker → **"HD-2D Demo"** button.

Pushes the *existing* Compose Canvas engine as far as it goes toward HD-2D using
only 2D primitives:

- 4 parallax layers (sky → hills → buildings → ground) with camera sway
- diegetic lighting: warm flickering lantern + pulsing Questbook glow (screen blend)
- drifting dust motes that catch the light
- fake tilt-shift depth-of-field (top/bottom darkened bands)
- vignette + CRT scanlines

**Verdict:** zero new dependencies, already wired into the scene picker. Looks
like a *nicely polished 2D pixel RPG*, but it is a stylisation — there is no real
blur, no GPU bloom, no true depth.

## Variant B — KorGE (Kotlin-native, GPU)

**Files:** `demos/korge-hd2d/Hd2dStage.kt` + `README.md`

Keeps everything in Kotlin Multiplatform but swaps the renderer to
[KorGE](https://korge.org), unlocking a GPU pipeline:

- depth-sorted parallax layers with **real `BlurFilter`** on far/near bands (DoF)
- **additive-blend** glow sprites over the Questbook & lantern (bloom-like)
- pixel-perfect (`smoothing = false`) sprite sampling
- WASD movement with walk-bob and depth scaling

**Verdict:** the sweet spot for a Kotlin team — real HD-2D layering, reuses the
GAIME logic/`core` layer, still targets Android + Desktop. Requires adding a
KorGE module (see its README).

## Variant C — Three.js (max fidelity, web)

**Files:** `demos/web-hd2d/index.html` + `hd2d.js` + `README.md`
**Try it:** `cd demos/web-hd2d && python3 -m http.server 8099` → open localhost:8099

The genuine article: pixel sprites as **billboarded planes inside a real 3D
scene**, with 3D lights + shadows, fog, a tilted perspective camera, and a full
post-processing stack (UnrealBloom + Bokeh DoF + vignette + optional pixelation).
Live FX toggles so you can A/B each effect.

**Verdict:** highest visual ceiling and the most faithful HD-2D, but it is a
**web** stack — it would run alongside GAIME (sharing only the pure-logic backend),
not inside the Compose app.

---

## Recommendation

- **Ship now / lowest risk:** Variant **A** — it's already in the app.
- **Best long-term Kotlin path:** Variant **B** (KorGE) — real HD-2D without
  leaving KMP.
- **If a web release / max fidelity is on the table:** Variant **C**.

All three reuse the same locked design and pixel palette, so the choice is purely
about how much rendering power vs. integration effort you want.
