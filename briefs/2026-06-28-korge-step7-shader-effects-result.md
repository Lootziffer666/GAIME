# Result: Steps 7a + 7b + 7c — Shader Effects Pipeline

**Brief:** `briefs/2026-06-28-korge-step7-shader-effects.md`
**Branch:** `kiro/korge-step7a-shader-effects`
**PR:** https://github.com/Lootziffer666/GAIME/pull/39
**Datum:** 2026-06-28
**Autor:** Kiro (Opus-only)
**Status:** ✅ Abgeschlossen — compile + tests + screenshots grün

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ 8 PNGs rendered (640×360) |

---

## Gelieferte Shader-Effekte (5 total)

| # | Filter | Uniforms | Screenshot-Beweis |
|---|---|---|---|
| 1 | PoisonFilter | intensity, time | 282 KB (11.3× Referenz) |
| 2 | BeerGoggleFilter | drunkLevel, time | 187 KB (7.2×) |
| 3 | LightingFilter | ambient, 3×light(pos,radius,intensity), flicker | 120 KB (4.6×) |
| 4 | RainFilter | intensity, windAngle, time | 119 KB (4.7×) |
| 5 | HeatShimmerFilter | intensity, frequency, time | 142 KB (5.5×) |

---

## Technische Findings

- KorGE `UniformBlock` hat keine Array-Uniforms → individuelle vec4s pro Licht
- `FragmentShaderDefault {}` (nicht `FragmentShader {}`) für Zugang zu `fragmentCoords`, `tex()`, `createTemp()`
- `createTemp(Float1)` + `SET(temp, expr)` für lokale Shader-Variablen
- Uniform-Vec4s setzen via `it.set(u_MyVec4, f1, f2, f3, f4)` (nicht `it[u_MyVec4] = ...`)
- `mapView.filter = myFilter` braucht `import korlibs.korge.view.filter.filter`
- Mesa EGL Software-Rendering funktioniert in Kiro-Sandbox (dnf install mesa-*)

---

## Kreative Dokumente (im selben Branch)

| Dokument | Inhalt |
|---|---|
| SHADER_VISION.md | 23-Effekt-Roadmap + Philosophie |
| SHADER_GAME_CONCEPT.md | Folgeprojekt-Konzept |
| SHADER_ACTORS_AND_AUDIOMANCER.md | Shader als Akteure + AUDIOMANCER |
| HORROR_SHADER_CONCEPT.md | "Das Haus hat dich falsch erkannt" |
| KLECKS_KINDERSPIEL_CONCEPT.md | Kinderspiel "Klecks & die Welt die sich verguckt hat" |
