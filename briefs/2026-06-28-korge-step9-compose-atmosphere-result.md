# Result: Step 9 — Filter-Komposition als Fundament + volle Atmosphäre + Season-Politur

**Brief:** briefs/2026-06-28-korge-step9-compose-atmosphere.md
**Branch:** kiro/korge-step9-compose-atmosphere
**Datum:** 2026-06-28

## Was wurde umgesetzt

### Teil A — Filter-Komposition (Fundament)
- `ShaderEffects.kt` komplett umgebaut: neues `enable(target, filter)` / `disable(target, filter)` System
- Tracks aktive Filter pro Container in geordneter Liste
- Nutzt KorGE's `ComposedFilter(List<Filter>)` für gleichzeitige Effekte
- Bestehende `attach*`/`detach`-Methoden als Komfort-Wrapper beibehalten (volle Rückwärtskompatibilität)
- **Beweis:** `compose_lighting_fog.png` zeigt Lighting + Fog gleichzeitig (69KB)

### Teil B — Frozen Approach mit voller Atmosphäre
- `captureFrozenApproach` überarbeitet: nutzt jetzt Komposition (Lighting + Fog gleichzeitig)
- `frozen_approach.png` zeigt Nacht + warmen Fackelkegel + Nebelschleier + Schnee (103KB)

### Teil C — Season-Overlays repariert
- `SpringOverlay.kt`: kräftigeres Alpha (110 + intensity*140), `scaledWidth`/`scaledHeight` statt `width`/`height`, rosa/gelb Farben
- `SummerOverlay.kt`: kräftigeres Alpha (140/200), `scaledWidth`/`scaledHeight`, sattgrüne Grasbüschel
- `AutumnOverlay.kt`: kräftigeres Alpha (110 + leaves*140), `scaledWidth`/`scaledHeight`, orange/braun/rot Laub
- `captureAutumnApproach` nutzt jetzt automatisch die Komposition (Lighting + Rain + Fog alle gleichzeitig)
- Farben korrekt: Frühling rosa/gelb, Sommer sattgrün, Herbst orange/braun/rot (nicht grau)

### Teil D — Neue Atmosphäre-Systeme

**D1 — Mondlicht (#38):**
- `DayNightClock.kt` erweitert: `moonIntensity()` (0 am Mittag, 0.7 um Mitternacht) + `moonColor()` (silber-blau-Tint)
- Fließt über die bestehende `ambientColor()`-Logik in die Frozen-Nacht ein
- Test: `DayNightClockMoonTest.kt` (5 Tests)

**D2 — Material-Ermüdung (#3):**
- `MaterialFatigue.kt` in `:core`: Per-Tile stress-Grid mit Schwellen (cracked 0.3, broken 0.7)
- Methoden: `addStress`, `addStressRadius`, `isCracked`, `isBroken`, `repair`, `heal`
- `MaterialFatigueOverlay.kt` in `:game`: Sichtbare Risse (hairline bei cracked, major fractures bei broken)
- Tests: `MaterialFatigueTest.kt` (10 Tests)
- Screenshot: `material_fatigue.png` (19KB, deutliche Riss-Linien bei verschiedenen Stress-Stufen)

### Teil E — Screenshots-Harness
- Neue Captures registriert: `captureComposeLightingFog()`, `captureMaterialFatigue()`
- Bestehende Season-Captures überarbeitet (nutzen jetzt Komposition)
- `frozen_approach.png` überschrieben (jetzt mit Fog + Lighting)
- B007 eingehalten: `localCurrentDirVfs`-Zeile + Import unverändert

## Testergebnis

```
./gradlew :core:desktopTest        → BUILD SUCCESSFUL (alle Tests grün)
./gradlew :game:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :game:screenshot          → 37 PNGs, alle > 1KB
```

**Regressions-Check:** Alle bestehenden Shader-Screenshots funktionieren unverändert:
- shader_poison (281KB), shader_beer_goggle (185KB), shader_lighting (118KB)
- shader_rain (118KB), shader_heat_shimmer (142KB)
- world_pressure_high (215KB), world_drunk (198KB), world_lantern (54KB)
- battle_boss_phase2 (30KB)

## Abweichungen vom Brief

- **Teil A Filter-Komposition:** Brief erwähnte "Reihenfolge sinnvoll wählen (z.B. Lighting/Tint zuletzt)" — implementiert als Insertionsreihenfolge (wer zuerst `enable()` aufruft, wird zuerst angewendet). Aufrufer bestimmt die Reihenfolge durch Reihenfolge der `enable()`-Calls.
- **Gold-API (Inventory.spend/steal):** Bereits in Step 7d implementiert — kein neuer Code nötig.
- **D1 Mondlicht:** Mondintensität berechnet, aber noch nicht als separater Tint-Filter gerendert (fließt über das bestehende ambientColor-System). Kann in einem Folge-Step als eigenständiger Silver-Tint-Shader erweitert werden.

## Neu entdeckte Bugs / Pitfalls

- **SolidRect.width/height vs. scaledWidth/scaledHeight:** KorGE's `View.width` setter skaliert ungleichmäßig (beeinflusst scaleX/Y). Für Overlay-Rects die ihre Größe ändern sollen ist `scaledWidth`/`scaledHeight` das korrekte API.
- **ComposedFilter und UniformBlock fixedLocation:** Wenn zwei Filter denselben `fixedLocation`-Index verwenden, kollidieren sie. Die bestehenden Filter nutzen 7-12 — keine Kollision derzeit.

## Was nicht angefasst wurde

DO_NOT_TOUCH-Liste vollständig eingehalten:
- ScreenshotHarness `localCurrentDirVfs`-Zeile + Import unverändert (B007)
- Bestehende Shader-Filter (Poison/BeerGoggle/Lighting/Rain/HeatShimmer/Fog) nur konsumiert, nicht geändert
- HudOverlay, QuestbookOverlay, QuestbookScreen, BattleScene, CharacterSprite, SpriteLoader, NpcDefinition, WaterOverlay, SnowOverlay, BloodOverlay, FootprintOverlay, ShaderStateBinder, MapConfig unberührt
- composeApp/ unberührt
- settings.gradle.kts unberührt
