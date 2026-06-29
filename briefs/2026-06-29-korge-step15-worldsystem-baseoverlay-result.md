# Result: Step 15 — WorldSystem-Register + BaseOverlay

**Datum:** 2026-06-29
**Branch:** `kiro/korge-step15-worldsystem-baseoverlay`
**BASE_SHA:** `d8383877`

## Ergebnis

Die Architektur-Spine für Pfeiler 2 steht. Ab jetzt kostet "ein System sichtbar machen" =
1 WorldSystem in `:core` (getestet) + 1 GridOverlay-Config + 1 Registry-Zeile.

## Was gebaut wurde

### Teil A — `:core`: WorldSystem-Interface + 2 Systeme

- `rpg/systems/WorldSystem.kt` — Interface (`id`, `tick(dt, ctx)`)
- `rpg/systems/WorldContext.kt` — schmale Brücke zum Spielzustand
- `rpg/systems/WaterSystem.kt` — besitzt WaterGrid, treibt Regen/Verdunstung/Flow
- `rpg/systems/DrunkSystem.kt` — besitzt DrunkState, orchestriert Sober-Tick/Schaden/Diebstahl
- Tests: 5 WaterSystem + 7 DrunkSystem = **12 Tests grün**

### Teil B — `:game`: GridOverlay + Overlay-Eindampfung

- `game/overlay/GridOverlay.kt` — generischer Renderer (Rect-Pooling, sizeFraction, colorOf)
- WaterOverlay: 59 → 24 Zeilen (delegiert an GridOverlay)
- FootprintOverlay: 68 → 25 Zeilen
- BloodOverlay: 75 → 30 Zeilen
- SnowOverlay: 68 → 30 Zeilen
- SpringOverlay: 80 → 28 Zeilen (2 GridOverlays: Blumen + Blossom)
- SummerOverlay: 82 → 50 Zeilen (behält manuelles Rect wegen Wind-Sway)
- AutumnOverlay: 76 → 50 Zeilen (behält manuelles Rect wegen Multi-Rect/Zelle)
- MaterialFatigueOverlay: 93 → 60 Zeilen (behält manuelles Rect wegen Crack-Orientierung)

**Öffentliche API aller Overlays UNVERÄNDERT.** WorldScene kompiliert ohne Änderung.

### Teil C — SystemRegistry im Unified Runtime

- `game/systems/SystemRegistry.kt` — hält Systems + optionale Render-Bindungen
- DoodleWorldScene: `WorldContext` aus hero/inventory/Spielerposition, `registry.tickAll()` +
  `registry.renderAll()` im addUpdater, DrunkSystem.resetIdle() bei Bewegung
- **Erste sichtbare Physik im Unified Runtime:** Pfützen (WaterOverlay in worldLayer)

### Teil D — Screenshot

- `unified_systems.png` (2560×1440, 3.1 MB) — Taverne mit sichtbaren blauen Pfützen
  um den Spieler-Spawn, Doodle-Character, HUD

## ACCEPTANCE

```
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL (12 neue Tests)
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
./gradlew :game:screenshot                   → 43 PNGs (1 neu, 42 bestehende unverändert)
  unified_systems.png  3.1 MB  2560×1440  ✓
  spring_approach.png  — weiterhin erzeugt, kein visueller Regress ✓
  world_rain_puddles.png — weiterhin erzeugt ✓
```

## Erfolgskriterium der Spine (Brief-Zitat)

> Nach diesem Brief kostet „ein weiteres System sichtbar machen" =
> 1 WorldSystem (core, getestet) + 1 GridOverlay-Konfig + 1 Registry-Zeile.

**Bestätigt.** Das ist Pfeiler 2.
