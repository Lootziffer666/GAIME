# Brief: Steps 7a + 7b + 7c — Shader Effects Pipeline

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step7a-shader-effects`
**Autor:** Kiro (selbst entworfen, Owner-freigegeben)

---

## Aufgabe

Prozedurale visuelle Effekte via KorGE `ShaderFilter` — parametrisch,
zeitgesteuert, gameplay-getrieben. Jeder Effekt ist an Spielzustand gebunden,
nicht handgesetzte Dekoration. Screenshot-verifiziert via Mesa EGL in der Sandbox.

---

## Step 7a — Shader-Infrastruktur + BeerGoggle + Poison

### Neue Dateien
- `game/src/desktopMain/kotlin/game/shader/ShaderEffects.kt` — Manager + Time-Driver
- `game/src/desktopMain/kotlin/game/shader/PoisonFilter.kt` — Chromatic Aberration + Vignette
- `game/src/desktopMain/kotlin/game/shader/BeerGoggleFilter.kt` — Blur + Warmth + Sway

### Architektur-Entscheidung
Jeder Filter: erbt `ShaderFilter()`, hat ein `UniformBlock` mit Parametern,
wird über `container.filter = MyFilter()` zugewiesen, `time`-Uniform vom
`ShaderEffects`-Manager getrieben.

---

## Step 7b — 2D-Lighting (3 Punkt-Lichter, Flicker, Ambient)

### Neue Dateien
- `game/src/desktopMain/kotlin/game/shader/LightSource.kt` — Datenklasse
- `game/src/desktopMain/kotlin/game/shader/LightingFilter.kt` — 3 radiale Lichter,
  quadratischer Falloff, sin-Flicker, Ambient-Multiply

### Technische Besonderheit
KorGE's `UniformBlock` unterstützt keine Uniform-Arrays. Lichter werden als
individuelle `vec4`-Uniforms deklariert (je Light: px, py, radiusPx, intensity).
`FragmentShaderDefault` + `createTemp(Float1)` Pattern für lokale Variablen.

---

## Step 7c — Wetter: Regen + Hitzeflimmern

### Neue Dateien
- `game/src/desktopMain/kotlin/game/shader/RainFilter.kt` — Prozedurale
  Regen-Streaks (fract-basiert, Wind-Shear, Dichte parametrisch)
- `game/src/desktopMain/kotlin/game/shader/HeatShimmerFilter.kt` — UV-Distortion
  per sin(y+time), stärker nach oben (hot air rises)

---

## Alle Screenshot-Beweise

| Screenshot | Größe | Referenz | Effekt |
|---|---|---|---|
| `shader_beer_goggle.png` | 187 KB | 26 KB | Blur + Warmth |
| `shader_poison.png` | 282 KB | 25 KB | Chromatic Aberration |
| `shader_lighting.png` | 120 KB | 26 KB | Lichtpools + Dunkelheit |
| `shader_rain.png` | 119 KB | 25 KB | Regen-Streaks |
| `shader_heat_shimmer.png` | 142 KB | 26 KB | UV-Distortion (Hitze) |

Alle 4.6×–11.3× größer als Referenz = Beweis dass Shader Pixel transformieren.

---

## SCOPE

```
create:
  - game/src/desktopMain/kotlin/game/shader/ShaderEffects.kt
  - game/src/desktopMain/kotlin/game/shader/PoisonFilter.kt
  - game/src/desktopMain/kotlin/game/shader/BeerGoggleFilter.kt
  - game/src/desktopMain/kotlin/game/shader/LightSource.kt
  - game/src/desktopMain/kotlin/game/shader/LightingFilter.kt
  - game/src/desktopMain/kotlin/game/shader/RainFilter.kt
  - game/src/desktopMain/kotlin/game/shader/HeatShimmerFilter.kt

modify:
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt
```

## ACCEPTANCE

```
./gradlew :game:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :core:desktopTest → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :game:screenshot → 8 PNGs rendered (640×360, verified)
```

---

## Kontext-Dokumente (im selben Branch)

- `docs/SHADER_VISION.md` — 23-Effekt-Roadmap + Rendering-Philosophie
- `docs/SHADER_GAME_CONCEPT.md` — Shader-First-Folgeprojekt-Vision
- `docs/SHADER_ACTORS_AND_AUDIOMANCER.md` — Shader als Akteure + AUDIOMANCER-Konzept
- `docs/HORROR_SHADER_CONCEPT.md` — "Das Haus hat dich falsch erkannt"
- `docs/KLECKS_KINDERSPIEL_CONCEPT.md` — Shader-First Kinderspiel
