# Brief: Step 16 — Pfeiler 2: Alle Grid-Physik sichtbar im Unified Runtime + Harness-Daten-Refactor

**MODELL: Opus-only** (vor Start Modell prüfen — `.kiro/steering/handoff-protocol.md`).
**Pflichtlektüre VOR der Arbeit:**
- `.claude/skills/gaime-shaders/SKILL.md` — **„WorldSystem + GridOverlay architecture"**
  (das Muster, das du hier 5× anwendest), Alpha-Floor, „render ≠ logic — PNG ansehen",
  B007, B004, Step-13-Skala-Regel.
- `docs/ENGINE_VISION.md` — dies ist **Pfeiler 2** („Sichtbarkeit = Fertigkeit").
- `docs/WORLD_PHYSICS_40_SYSTEMS.md` — die ⚠️-Systeme, die hier auf den Schirm kommen.
- `docs/KNOWN_BUGS.md` — vor Arbeit lesen, neue Funde ergänzen.

**Datum:** 2026-06-29
**Branch:** `kiro/korge-step16-pfeiler2-visible-physics`
**BASE_SHA:** `c0de32b9`

---

## Ziel (groß — ein Lauf, nicht ein Beweis-System)

Step 15 hat die Spine gebaut (`WorldSystem` + `GridOverlay` + `SystemRegistry`) und mit
Water+Drunk bewiesen. **Jetzt kommen ALLE grid-basierten Physik-Systeme sichtbar ins Unified
Runtime** — nach exakt demselben Muster, plus der Harness-Daten-Refactor, damit jedes neue
System billig Screenshots bekommt.

Nach diesem Brief lebt die Umwelt-Physik nicht mehr nur als getestete `:core`-Logik, sondern
ist **im gemalten 1440p-Doodle-Runtime zu sehen**: Schnee, Blut, Fußspuren, Jahreszeiten,
Material-Ermüdung — alle über die Registry getickt und gerendert.

**Bewusst NICHT hier (Pfeiler 2b, Folge-Brief):** die *shader-getriebenen*/globalen Systeme
(Lighting aus DayNight, Fog, BeerGoggles aus Drunk, Poison aus Pressure, Wind/Temperature/
Wetness/Dirt). Die brauchen die `ShaderEffects`/`ShaderStateBinder`-Kette ins Unified Runtime —
eigene Querschnittsaufgabe. Hier nur **Grid-Overlays**.

---

## Teil A — `:core`: 5 neue WorldSystems (je mit Tests)

Package `rpg/systems/*` (neben `WaterSystem`/`DrunkSystem`). Jedes besitzt sein bestehendes
`:core`-Grid und tickt dessen Simulation. Reine Logik, kein Renderer.

| System | besitzt Grid | `tick(dt, ctx)` treibt |
|---|---|---|
| `SnowSystem` | `SnowGrid` | Akkumulation bei Schnee-Wetter; Schmelze über Temperatur; `clearAt` an Spieler-Zelle (Trampeln) |
| `BloodSystem` | `BloodGrid` | Frische klingt über Zeit ab (`freshnessAt`); Eintrocknen |
| `FootprintSystem` | `FootprintGrid` | Spur an `ctx.playerCell` setzen wenn der Spieler sich bewegt; Verblassen über Zeit |
| `SeasonSystem` | `SeasonalGrid` | Jahreszeit-Fortschritt (Blüten wachsen/trampeln, Laub) — Saison als Parameter |
| `MaterialFatigueSystem` | `MaterialFatigue` | Stress-Akkumulation/Risswachstum über Zeit |

- Wo eine `:core/weather`-Klasse eine Methode für `tick` zu brauchen scheint, die **fehlt**:
  im Result begründen + nachfragen — `core/rpg/weather/*` bleibt **DO_NOT_TOUCH** (nur konsumieren).
  Falls die Grids bereits Step-/Tick-Methoden haben (wie `WaterGrid.flowStep`), die nutzen.
- **Tests** (`core/src/desktopTest/.../systems/*`), je System mind. 4: Akkumulation, Abklingen,
  Spieler-Interaktion (Trampeln/Spur), Null-Effekt-Fall. Gesamt ≥ 20 neue Tests.

---

## Teil B — `:game`: Alle Systeme in die Registry des Unified Runtime

In `DoodleWorldScene`:
1. Für jedes System ein `GridOverlay` (Snow/Blood/Footprint/Season nutzen die in Step 15
   eingedampften Overlay-Klassen bzw. direkt `GridOverlay`-Konfigs; MaterialFatigue nutzt
   seinen manuellen Renderer — bleibt so). **Overlays in `worldLayer`** (scrollen mit Kamera).
2. Alle Systeme in der `SystemRegistry` registrieren (`register(system) { overlay.update(system.grid) }`).
   `tickAll` + `renderAll` laufen schon im `addUpdater` (Step 15) — nur erweitern.
3. **Render-Reihenfolge** bewusst setzen (z.B. Wasser/Blut unter Fußspuren unter Jahreszeit),
   damit sich Overlays sinnvoll überlagern (Blut auf Schnee etc. — die 40-Doc-These).
4. Saison/Wetter pro Map aus einer Quelle ableiten (z.B. ein Feld in `ImageWorldDef`, additiv
   ergänzen — `ImageWorldDef` darf erweitert werden) statt hartkodiert; Default sinnvoll.

> DEBUG_HOTSPOTS-Marker und Systeme dürfen sich nicht gegenseitig verdecken — Marker zuletzt zeichnen.

---

## Teil C — Harness-Daten-Refactor (#3) — das 2012-Zeilen-Monster zähmen

Neuer Mechanismus in `ScreenshotHarness.kt` (B007: `localCurrentDirVfs`-Zeile + Import NICHT ändern):

```kotlin
data class UnifiedSceneSpec(
    val name: String,
    val map: ImageMapId,
    val season: String? = null,
    val weather: Weather? = null,
    val prefill: (SystemRegistry) -> Unit = {},   // Grids für den Shot vorbefüllen
    val playerCell: Pair<Int,Int>? = null,
)
```
- **Eine** generische `renderUnifiedScene(spec)` baut den DoodleWorldScene-Zustand auf
  (Bild + Grid + Doodle-Figur + Registry mit allen Systemen + Overlays), wendet `prefill` an,
  `save(spec.name)`. Deterministisch (fixe Werte, **kein** `Random`).
- **Alle neuen Pfeiler-2-Captures laufen über diese eine Funktion** — jeder Shot = 1 `UnifiedSceneSpec`,
  nicht 50 Zeilen Copy-Paste. Das ist die #3-Lösung für die Zukunft.
- **Legacy nicht anfassen:** die 42 bestehenden Tile-basierten Captures bleiben unverändert
  (sterben mit `WorldScene`). KEIN Big-Bang-Rewrite — Regressionsrisiko.

---

## Teil D — Screenshot-Beweise (alle über `renderUnifiedScene`)

Mind. diese Specs (je 2560×1440), danach **jedes PNG ansehen** — Effekt unmissverständlich sichtbar:
1. `unified_winter` — Schnee akkumuliert + Fußspuren des Spielers im Schnee.
2. `unified_blood_snow` — frisches Blut auf Schnee (Überlagerung, die 40-These).
3. `unified_spring` / `unified_autumn` — Jahreszeit-Overlay im gemalten Runtime.
4. `unified_material_fatigue` — Risse sichtbar.
5. `unified_all` — möglichst viele Systeme gleichzeitig aktiv (der „lebendige Welt"-Shot).

Alle bestehenden Screenshots müssen **unverändert** weiterlaufen (Regressionsprobe für die
Step-15-Overlays).

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/DoodleWorldScene.kt     (alle Systeme registrieren + Render-Order)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt    (renderUnifiedScene + neue Specs anfügen)
  - game/src/desktopMain/kotlin/game/world/ImageWorldDef.kt  (optionales season/weather-Feld, additiv)
  - game/src/desktopMain/kotlin/game/{Snow,Blood,Footprint,Spring,Summer,Autumn}Overlay.kt
    (nur falls nötig, um sie sauber aus der Registry zu treiben — öffentliche API erhalten)
create:
  - core/src/commonMain/kotlin/rpg/systems/*   (SnowSystem, BloodSystem, FootprintSystem,
                                                 SeasonSystem, MaterialFatigueSystem)
  - core/src/desktopTest/kotlin/rpg/systems/*  (Tests, ≥20)
  - briefs/2026-06-29-korge-step16-pfeiler2-visible-physics-result.md
```

## DO_NOT_TOUCH

```
- game/.../ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- core/rpg/weather/*   (Grids NUR konsumieren — nicht ändern; fehlende Methode → nachfragen)
- core/rpg/combat/*, core/rpg/items/*   (nur konsumieren)
- game/.../WorldScene.kt, MapConfig.kt   (stirbt separat; nur lesen)
- game/.../shader/*, ShaderEffects.kt, ShaderStateBinder.kt   (Pfeiler 2b, NICHT hier)
- game/.../MaterialFatigueOverlay.kt → manueller Riss-Renderer bleibt (nur aus Registry aufrufen)
- game/.../overlay/GridOverlay.kt, systems/SystemRegistry.kt   (konsumieren, nicht umbauen)
- composeApp/, tools/, assets/, settings.gradle.kts, docs/ (nur KNOWN_BUGS ergänzen)
- Die 42 bestehenden Legacy-Captures NICHT umschreiben
```

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL (inkl. ≥20 neuer systems-Tests)
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot
   → inkl. unified_winter / unified_blood_snow / unified_spring / unified_autumn /
     unified_material_fatigue / unified_all (alle 2560×1440)
   → ALLE bestehenden Screenshots weiterhin erzeugt, KEIN visueller Regress
```
Bei GL „Too many callbacks" einmal wiederholen. Sandbox ggf. `LD_LIBRARY_PATH=/usr/lib64`.

---

## Kontext / Querverweise

- **Das Muster ist fertig** (Step 15, im Skill): 1 `WorldSystem` (core, getestet) +
  1 Overlay-Konfig + 1 Registry-Zeile pro System. Hier 5× anwenden + Harness zähmen.
- **Grid-förmig** (GridOverlay-fähig): SnowGrid (`depthAt/clearAt`), BloodGrid
  (`amountAt/freshnessAt`), FootprintGrid (`get`), SeasonalGrid (`flowerAt`), MaterialFatigue
  (`stressAt`, manueller Renderer). Water/Drunk sind schon drin.
- **Render-Order zählt** für die Überlagerungs-These (Blut auf Schnee, Laub über Boden).
- **Nächste Stufen (NICHT hier):** Pfeiler 2b (Shader-Kette: Lighting/Fog/BeerGoggles/Poison +
  Wind/Temperature/Wetness/Dirt ins Unified Runtime); `WorldDef`-Konvergenz + WorldScene/
  composeApp-Retirement (#4).
```
