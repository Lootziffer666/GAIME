# Result: Step 11 — Render-Gerüst: Doodle-Figuren vor hi-res Hintergrund @ 1440p

**Brief:** briefs/2026-06-28-korge-step11-doodle-render-scaffold.md
**Branch:** kiro/korge-step11-doodle-render-scaffold
**Datum:** 2026-06-28

## Was wurde umgesetzt

### Teil A — DoodleLineFilter
- `game/src/desktopMain/kotlin/game/shader/DoodleLineFilter.kt` erstellt
- GLSL-Fragment-Shader: Luminanz-Gradient-Edge-Detection (4 Nachbar-Samples, Sobel-like)
  → Kanten abdunkeln (85% darken at full edge) → feiner Cartoon-Linien-Look
- Time-driven boil/jitter: sin-basierter UV-Offset pro Frame → Linien zittern lebendig
- Uniforms: `u_Time` (Float), `u_LineStrength` (0..1), `u_Jitter` (0..1)
- `fixedLocation = 13` (keine Kollision mit bestehenden Filtern 7-12)
- In `ShaderEffects.kt` registriert + time-driven via `startTimeUpdater`
- Anime4K-Konzept als Referenz (Gradient→Linien), KEIN Fremdcode (Donor-Policy)

### Teil B — 1440p Doodle Capture
- `doodle_1440p.png` (2560x1440, 4.6MB)
- Hintergrund: `tavern_interior.png` vollflächig, `smoothing=true`, KEIN Shader
- Charakter-Layer: eigener `Container` mit DoodleLineFilter
- Grid-abgeleitete Skalierung: `gridRows=78`, `screenTile=1440/78≈18.5`, `charScale=(5*18.5)/64≈1.44`
- Swordsman + Vampire mit deutlichem Linien-/Doodle-Look

### Teil C — Docs aktualisiert
- `docs/SHADER_VISION.md`: "Kein Runtime-Upscaler"-Zeile ersetzt durch neue Richtung (hi-res Hintergründe + DoodleLineFilter + Anime4K HQ als späterer Austausch)
- `docs/KORGE_MIGRATION_PLAN.md`: Step 11 Eintrag ergänzt

### Teil D — Grid-Overlay Debug
- `grid_overlay_debug.png` (2560x1440, 4.3MB)
- tavern_interior.png vollflächig + CollisionGrid halbtransparent überlagert
- BLOCKED = roter Tint (Möbel/Wände/Rand), WALKABLE = leicht grüner Tint (Boden)
- Beweist: das unsichtbare Raster passt zum gemalten Hintergrund (Bild=Haut, Grid=Logik)

## Testergebnis

```
./gradlew :core:desktopTest             → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop    → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :game:screenshot              → 42 PNGs (40 bestehende @640x360 + 2 neue @2560x1440)
```

Regressions-Check: alle bestehenden Screenshots funktionieren unverändert.

## Abweichungen vom Brief

- **CharacterSprite hat kein `view`-Property:** Statt individuelle Sprite-Views zu skalieren, wird der gesamte `charLayer` Container skaliert (`charScale = (tilesTall * screenTile) / 64`). Gleiches Ergebnis (Grid-derived sizing), anderer Mechanismus.
- **BASE_SHA `e7641993` vs HEAD `6787803d`:** Branch wurde von `origin/main` (HEAD = `6787803d`, der letzte Brief-Commit) erstellt. Der Brief wurde nach BASE_SHA nochmals aktualisiert.

## Neu entdeckte Bugs / Pitfalls

- **Image.width/height vs scaledWidth/scaledHeight:** KorGE's `View.width` setter ändert `unscaledSize` (was intern die scaleX/Y beeinflusst). Für Bilder die auf eine bestimmte Pixelgröße skaliert werden sollen ist `scaledWidth`/`scaledHeight` korrekt.
- **Container.filter + container {}:** `container {}` erstellt einen neuen Container als Kind. Der DoodleLineFilter darauf wirkt auf ALLE Kinder (Sprites) — genau das gewünschte Per-Layer-Verhalten.

## Was nicht angefasst wurde

DO_NOT_TOUCH komplett eingehalten:
- ScreenshotHarness `localCurrentDirVfs`-Zeile + Import unverändert (B007)
- Bestehende Shader-Filter nur konsumiert, nicht geändert
- WorldScene, BattleScene, Overlays, MapConfig, CharacterSprite etc. unberührt
- assets/ nur gelesen
- tools/mapbuilder/ unberührt
- composeApp/, settings.gradle.kts unberührt
