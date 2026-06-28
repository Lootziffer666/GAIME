# Brief: Step 9 — Filter-Komposition als Fundament + volle Atmosphäre + Season-Politur

**MODELL: Opus-only** (vor Start Modell prüfen — neuer Thread erbt die Wahl NICHT; siehe
`.kiro/steering/handoff-protocol.md` → „Modell-Anforderung").

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step9-compose-atmosphere`
**BASE_SHA:** `b26b57e8`

---

## Vorbemerkung: Fundament zuerst, dann trägt alles

Großer Auftrag (Teile A–E), EIN PR. **Teil A (Filter-Komposition) ist das Fundament** — B, C, D
bauen darauf auf. Liefere alles mit Beweis (Test/Screenshot), bevor du den PR öffnest.

Warum: In Step 8 hatte jeder Effekt `target.filter = x` gesetzt → ein `Container` hat nur EIN
`filter`, also überschrieben sich Nebel, Fackellicht, Nacht gegenseitig (Clobber). „The Frozen
Approach" konnte nie gleichzeitig Nacht + Fackel + Nebel zeigen. Das lösen wir an der Wurzel.

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step9-compose-atmosphere origin/main
git log --oneline -3   # soll b26b57e8 ganz oben zeigen
```

---

## Teil A — Filter-Komposition (das Fundament)

`game/src/desktopMain/kotlin/game/shader/ShaderEffects.kt` so umbauen, dass **mehrere Filter
gleichzeitig** auf einem Container wirken.

- KorGE-6.0-API für Filter-Verkettung gegen `korge-6.0.0-sources.jar` verifizieren
  (`korlibs.korge.view.filter.ComposedFilter` o.ä. — exakten Typ/Konstruktor aus den Sources
  bestätigen, nicht raten). Donor-Policy: nur lesen, kein Fremdcode kopieren.
- Neues Modell: `ShaderEffects` führt pro Ziel-Container eine **geordnete aktive Filter-Liste**
  und setzt `target.filter = ComposedFilter(<liste>)` (bzw. den verifizierten Mechanismus).
  Reihenfolge sinnvoll wählen (z.B. Lighting/Tint zuletzt).
- API: `enable(target, filter)` / `disable(target, filter)` (oder vergleichbar), plus die
  bestehenden `attach*`/`detach`-Methoden **als Komfort-Wrapper beibehalten**, damit
  PoisonFilter/BeerGoggle/Lighting/Rain/HeatShimmer/Fog weiter funktionieren.
- `startTimeUpdater` muss weiterhin alle aktiven Filter mit `time` versorgen.

**REGRESSIONS-PFLICHT:** Nach dem Umbau müssen ALLE bestehenden Shader-Screenshots unverändert
funktionieren (`shader_poison`, `shader_beer_goggle`, `shader_lighting`, `shader_rain`,
`shader_heat_shimmer`, `world_pressure_high`, `world_drunk`, `world_lantern`,
`battle_boss_phase2`). Im Result bestätigen, dass keiner kaputt ging.

**Acceptance A (Screenshot):** `compose_lighting_fog.png` — eine Szene mit **Lighting UND Fog
gleichzeitig** sichtbar (dunkler Rand + warmer Lichtkegel UND Nebelschleier zugleich). Beweist die
Komposition.

---

## Teil B — „The Frozen Approach" mit voller Atmosphäre

Jetzt, wo Komposition existiert, die Szene richtig bauen — verschneite **Nacht** mit allem:
- **Nacht-Ambient** (DayNightClock.darkness) + **Fackel** (LightingFilter, folgt Spieler) +
  **Nebel** (FogFilter) + **Schnee/Fußspuren/Blut** (Overlays) — alle gleichzeitig.
- In `WorldScene`: den Clobber beseitigen — Fog und Lighting über die neue Komposition zusammen
  aktivieren, nicht nacheinander `attach`en. Fackel (`L`) ergänzt das Licht, statt Nebel zu ersetzen.
- `ScreenshotHarness.captureFrozenApproach()`: ebenfalls komponieren (nicht ein Filter gewinnt).
  Ambient als echte Nacht (~0.15–0.25), Fackel kräftig, Nebel ~0.3 — alle drei lesbar.

**Acceptance B (Screenshot):** `frozen_approach.png` (überschreibt das bisherige) zeigt
gleichzeitig: dunkle verschneite Nacht + warmen Fackel-Lichtkegel + Nebelschleier + Schnee. Das ist
der Money-Shot, der in Step 8 nicht ging.

---

## Teil C — Season-Overlays reparieren

Die Bonus-Overlays aus Step 8 rendern Blüten/Gras/Laub als **graue Quadrate am unteren Bildrand**
(falsche Position + Farbe). Reparieren (`SpringOverlay`, `SummerOverlay`, `AutumnOverlay`):
- **Lehre aus 7d/8 anwenden:** Zellen aus `SeasonalGrid` über `(gx+offsetX, gy+offsetY) * tilePx`
  in **mapView-Koordinaten** platzieren (nicht Screen-Bottom), Grid um die kamerazentrierte
  Spieler-Kachel füllen, kräftiges Alpha.
- **Farben korrekt:** Frühling rosa/gelb (Blüten), Sommer sattgrün (Grasbüschel), Herbst
  orange/braun (Laub). Nicht grau.
- Walkover-Reaktion (zertreten/wegbiegen) beibehalten falls vorhanden.

**Acceptance C (Screenshots, überschreiben):** `spring_approach.png`, `summer_approach.png`,
`autumn_approach.png` — farbige Vegetation klar auf der Karte verteilt (nicht am Bildrand, nicht grau).

---

## Teil D — Zwei neue Atmosphäre-Systeme (nutzen die Komposition)

Aus `docs/WORLD_PHYSICS_40_SYSTEMS.md`, bewusst zwei gut abgegrenzte:

### D1 — Mond-/Sternenlicht (#38) — `:core` + `:game`
`DayNightClock` erweitern: bei Nacht ein **kühler Silber-Tint** + (optional) Stern-Punkte. Reine
Logik (`moonIntensity()` aus timeOfDay) in `:core` mit Test; Rendering als kühler Tint-Filter ODER
über die bestehende Lighting-Ambient-Farbe komponiert. Verstärkt die Frozen-Nacht.

### D2 — Material-Ermüdung (#3) — `:core` + `:game`
Neues `rpg.weather` (oder `rpg.world`) `MaterialFatigue`: pro Objekt/Tile `stress: Float`,
`stressAt`, `addStress`, Schwellen `cracked`/`broken`. Reine Logik, unit-getestet. Rendering:
ein einfaches Riss-Overlay (helle Bruchadern, Alpha ∝ stress) auf markierten Tiles — Risse
ERSCHEINEN bevor etwas bricht (kein HP-Balken).

**Acceptance D:** `:core`-Tests grün; Screenshot `material_fatigue.png` (sichtbare Risse je nach
Stress-Stufe). Mondlicht fließt in `frozen_approach.png` ein (kein eigener Pflicht-Shot).

---

## Teil E — Harness

**KRITISCH — B007:** `localCurrentDirVfs["build/screenshots"]` + Import NICHT ändern (sechstes Mal).

Neue Captures + Registrierung: `captureComposeLightingFog()`, `captureMaterialFatigue()`.
`captureFrozenApproach` + `captureSpring/Summer/AutumnApproach` werden überarbeitet (überschreiben
ihre PNGs). Reproduzierbar: feste `time`, vorbefüllte Grids, Effekte um die Spieler-Kachel.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/shader/ShaderEffects.kt   (Komposition — Fundament)
  - game/src/desktopMain/kotlin/game/WorldScene.kt             (Frozen-Atmosphäre komponieren)
  - game/src/desktopMain/kotlin/game/SpringOverlay.kt
  - game/src/desktopMain/kotlin/game/SummerOverlay.kt
  - game/src/desktopMain/kotlin/game/AutumnOverlay.kt
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt
  - core/src/commonMain/kotlin/rpg/weather/DayNightClock.kt    (Mondlicht)

create:
  - core/src/commonMain/kotlin/rpg/weather/MaterialFatigue.kt
  - core/src/commonTest/kotlin/rpg/weather/MaterialFatigueTest.kt
  - core/src/commonTest/kotlin/rpg/weather/DayNightClockMoonTest.kt   (oder bestehenden erweitern)
  - game/src/desktopMain/kotlin/game/MaterialFatigueOverlay.kt
  - briefs/2026-06-28-korge-step9-compose-atmosphere-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/shader/  EINZELNE Filter (Poison/BeerGoggle/Lighting/Rain/
        HeatShimmer/Fog) NICHT ändern — nur ShaderEffects.kt (der Manager) wird angefasst
- core/ bestehende Packages außer den genannten: nur konsumieren
- game/  HudOverlay, QuestbookOverlay, QuestbookScreen, BattleScene, CharacterSprite, SpriteLoader,
        NpcDefinition, WaterOverlay, SnowOverlay, BloodOverlay, FootprintOverlay, ShaderStateBinder,
        MapConfig  (unberührt — Season-Configs existieren bereits)
- composeApp/
- settings.gradle.kts
- docs/KNOWN_BUGS.md   nur lesen
```

---

## ACCEPTANCE (gesamt)

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL (neue Tests)
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot   → bestehende + neue PNGs
```
Pflicht-Screenshots: `compose_lighting_fog.png` (Komposition beweisen), `frozen_approach.png`
(Nacht+Fackel+Nebel+Schnee gleichzeitig), `spring/summer/autumn_approach.png` (farbige Vegetation
verteilt), `material_fatigue.png` (Risse). **Regression:** alle bestehenden Shader-Shots noch ok.

---

## Kontext / Querverweise

- **Filter-Komposition** ist der Kern — ohne sie bleibt Atmosphäre flach. Erst A, dann der Rest.
- **Lehre 7d/8:** Overlays mit kräftigem Alpha (`110 + depth*140`) UND Grid-Zellen um die
  kamerazentrierte Spieler-Kachel füllen, sonst off-screen/unsichtbar. Vorlage: `WaterOverlay`
  + korrigierte `captureWorldRainPuddles`. Genau dieser Fehler steckt aktuell in den Season-Overlays.
- **KNOWN_BUGS B007** (localCurrentDirVfs), **Step 5b** (SolidRect Leaf / kein Auto-Wrap),
  **Step 3** (`dt: Duration`, `dt.seconds`).
- **Bestehende Bausteine:** `FogFilter`, `LightingFilter`/`LightSource`, `DayNightClock`,
  `SeasonalGrid`, `ShaderEffects`. Alles in main seit Step 8.
- **Nicht in diesem Brief (Folge):** Rost (#9), Geruchs-Wolken (#6), Insekten (#16),
  Jahreszeiten-Migration (#27), AUDIOMANCER. Aufgespart.
```
