# Brief: Step 8 — „The Frozen Approach": ein Testlevel, das Shader=State maximal beweist

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step8-frozen-approach`
**BASE_SHA:** `13b8101a`

---

## Vorbemerkung: RICHTIG großer Sprint, EIN PR

Dies ist bewusst ein sehr umfangreicher Auftrag (Teile A–F). Plane mehrere Stunden ein und
liefere **alles** mit je eigenem Beweis (Unit-Test ODER Screenshot), bevor du den PR öffnest.
Kein Teil-PR.

`docs/WORLD_PHYSICS_40_SYSTEMS.md` (von dir geschrieben) empfiehlt explizit den **idealen ersten
Testlevel: „Schnee + Fußspuren + Wind + Blut + Fackellicht"**. Genau den bauen wir — als
zusammenhängendes, spielbares Level **„The Frozen Approach"** (das Dorf bei Nacht, unter Schnee),
plus die offenen Punkte (Questbook-Pracht, Gold-API). Quellen, in dieser Reihenfolge nutzen:
`docs/WORLD_PHYSICS_40_SYSTEMS.md`, `docs/SHADER_VISION.md` (Katalog #6,#12–17,#37), `docs/SHADER_ACTORS_AND_AUDIOMANCER.md`.

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step8-frozen-approach origin/main
git log --oneline -3   # soll 13b8101a ganz oben zeigen
```

---

## Architektur-Leitplanke (wie gehabt, strikt einhalten)

- **Simulations-LOGIK → `:core`** (engine-agnostisch, unit-getestet). Schnee, Blut, Tageszeit,
  Temperatur, Nebel-Dichte = reine Kotlin-Klassen mit Tests.
- **`:game` rendert nur** + verbindet Input/Kamera.
- **Bestehende `:core`-States nutzen:** `DirtState`, `FootprintGrid`, `WindState` existieren bereits
  (Step 7d) aber werden NICHT gerendert. Dieser Sprint macht sie sichtbar.
- **Filter-Stacking:** Container hat ein `filter`. Für gleichzeitige Effekte (Nacht-Tint + Fackel +
  Nebel) KorGE-Filter-Komposition gegen `korge-6.0.0-sources.jar` verifizieren ODER Per-Layer
  (SHADER_VISION) trennen. Entscheidung im Result dokumentieren.
- **Determinismus:** kein `Random` in Sim-Logik (Tests müssen reproduzierbar sein); für Screenshots
  feste `time`-Werte + vorbefüllte Grids.

---

## Teil A — Neue `:core`-Simulationen (mit Unit-Tests)

Package `core/src/commonMain/kotlin/rpg/weather/` erweitern (Wildcard erlaubt):

### `SnowGrid.kt`
Analog `WaterGrid`: `snowDepth: Float` pro Zelle. `accumulate(rate)` (Schnee fällt, überall +),
`clearAt(x,y, amount)` (Spieler/Fußspur drückt Schnee weg → Spur), `regrow(rate)` (Spuren schneien
langsam zu), `depthAt(x,y)`. Bounding-Box wie WaterGrid (offsetX/Y).

### `BloodGrid.kt`
`bloodAmount: Float` + `freshness: Float` pro Zelle. `spill(x,y, amount)` (frisch, glänzt),
`age(rate)` (frisch→dunkel über Zeit, #17 „Blut als Information"). `isFresh(x,y)`. Auf Schnee
besonders sichtbar (für das Rendering relevant).

### `DayNightClock.kt`
`timeOfDay: Float` (0..1, 0=Mitternacht, 0.5=Mittag). `advance(dt, speed)`. Liefert
`ambientColor(): Triple<Float,Float,Float>` (warm tagsüber, kühl-blau nachts) und
`darkness(): Float` (0 tags, ~0.6 nachts) — treibt LightingFilter.ambientDarkness.

### `TemperatureField.kt`
Pro Zelle `temp: Float` (−1 kalt .. +1 heiß). `addHeatSource(x,y, radius, strength)` (Fackel/Feuer),
`tempAt(x,y)`. Treibt: Trocknen (warm), sichtbarer Atem (kalt), Eis (sehr kalt). Reine Berechnung
aus den registrierten Quellen (kein Grid-Step nötig, on-demand).

### `FogState.kt`
`density: Float` (0..1) + optional Wind-Drift-Offset (nutzt `WindState`). `setDensity`, `drift(dt)`.

### Gold-API in `:core` (offener Punkt aus 7d)
`rpg/items/Inventory.kt` erweitern: `fun spend(amount: Int): Boolean` (genug Gold? abziehen→true,
sonst false) und `fun steal(amount: Int): Int` (zieht bis zu `amount` ab, gibt tatsächlich
gestohlene Menge zurück). `gold`-Setter bleibt `private`. Das behebt die 7d-Limitation
(„Einschlafen → beklaut werden" kann jetzt echtes Gold abziehen).

### Tests `core/src/commonTest/kotlin/rpg/weather/` + `rpg/items/`
- `SnowGridTest`: accumulate hebt, clearAt senkt (Spur), regrow füllt wieder.
- `BloodGridTest`: spill setzt fresh, age dunkelt, isFresh-Schwelle.
- `DayNightClockTest`: advance läuft 0..1 zyklisch, darkness nachts hoch/tags niedrig.
- `TemperatureFieldTest`: nahe Quelle warm, fern kalt, mehrere Quellen addieren.
- `FogStateTest`: setDensity/drift.
- `InventoryTest` (erweitern oder neu): spend mit genug/zu wenig Gold; steal capped.

**Acceptance A:** `./gradlew :core:desktopTest` grün (alle neuen Tests).

---

## Teil B — Das Level „The Frozen Approach" (`:game`)

Reuse der **Exterior-TMX** (kein neues Tileset nötig) als verschneite Nachtszene.

### `MapConfig` erweitern
Ein `WorldAtmosphere`-Feld (data class: `season`, `timeOfDay`, `weather`-Enum {CLEAR, RAIN, SNOW},
`fog: Float`). Bestehende `interior()/exterior()` bekommen `WorldAtmosphere.CLEAR_DAY` (Default,
ändert nichts). Neue Factory `frozenApproach()`: nutzt die Exterior-TMX, aber
`weather = SNOW`, Nacht-`timeOfDay`, `fog = 0.4`. In `MapId` einen Wert `FROZEN_APPROACH`
ergänzen + in `forId(...)` verdrahten. (Erreichbarkeit im Spiel: optional ein Exit/Trigger;
für diesen Sprint reicht, dass `frozenApproach()` von Harness + einem Debug-Key erreichbar ist.)

### Rendering-Overlays (`:game`, neue Dateien analog `WaterOverlay`)
- `SnowOverlay.kt` — weiße Akkumulation pro Zelle (Alpha ∝ snowDepth); Fußspuren erscheinen als
  Lücken (clearAt-Zellen dunkler/Boden sichtbar). Liest `SnowGrid` + `FootprintGrid`.
- `BloodOverlay.kt` — rote Flecken (frisch hell/glänzend, alt dunkel) aus `BloodGrid`. Auf Schnee
  besonders kräftig.
- `FootprintOverlay.kt` ODER in SnowOverlay integriert — die bereits existierende `FootprintGrid`
  sichtbar machen (Spur hinter dem laufenden Spieler).

### `FogFilter.kt` (neuer Shader in `game/shader/` — CREATE erlaubt, bestehende NICHT ändern)
Perlin-/Noise-basierter Nebel-Overlay (SHADER_VISION #37): `density`-Uniform, `time`, Wind-Drift.
Silhouetten weiter weg verschwimmen. Falls vollwertiger GLSL-Perlin zu teuer: ein animierter
Halbtransparenz-Schleier mit sin-Drift genügt für den ersten Wurf.

### Atmosphäre-Verdrahtung in `WorldScene`
- **Tag/Nacht:** `DayNightClock` → `LightingFilter.ambientDarkness` + globaler Farb-Tint.
- **Wind:** `WindState` → Regen/Schnee-Schräge, Fackel-Flacker-Radius, (optional) Gras-Sway.
- **Temperatur:** Fackel/Laterne als Heat-Source → trocknet (7d) + unterdrückt Atem.
- **Schnee + Fußspuren + Blut** über die Overlays.
- **Fackellicht:** die 7d-Laterne (`L`) ist hier Pflicht-Lichtquelle.

### Screenshots (Teil B)
- `frozen_approach.png` — die Nacht-Schnee-Szene: Schnee, Nebel, Fackellicht, dunkler Nacht-Tint.
- `frozen_footprints.png` — Fußspuren-Schleppe im Schnee hinter dem Spieler.
- `frozen_blood.png` — frische + alte Blutspuren auf Schnee (kräftig sichtbar).

---

## Teil C — Das Questbook in seiner GANZEN Pracht (offener Punkt aus 7d)

`QuestbookScreen.kt` aufwerten (modify erlaubt). Aktuell: zwei flache Rechtecke. Jetzt:
- **Buch-Optik:** Pergament-Farbverlauf, dunkle Bindung/Spine in der Mitte, gerahmte Seiten,
  Eselsohren/Schatten. KorGE-Primitive (RoundRect/Graphics/Image) — wenn ein Buch-Sprite in
  `assets/` existiert, suchen + nutzen, sonst prozedural.
- **Aufklapp-Animation:** beim Öffnen entfaltet sich das Buch (scale/rotation tween ~0.4s).
- **Umblättern:** ←/→ blättern; eine Seitenhälfte „klappt" über (scaleX-Flip-Tween ~0.3s).
- **Inhalt** weiter aus echten Daten (`SliceDirector.questbook.log`, Marker, Party-Name, Pressure).
- Tween-API gegen KorGE-Sources verifizieren (`korlibs.korge.tween.tween`, `korlibs.time` Easing).

### Screenshot (Teil C)
- `questbook_glory.png` — das aufgeschlagene, verzierte Buch (Bindung, Rahmen, echte Einträge).
  Endzustand der Aufklapp-Animation.

---

## Teil D — Kälte-/Atmosphäre-Politur

- **Sichtbarer Atem** bei Kälte (`TemperatureField.tempAt(player) < 0`): kleine weiße
  Partikel-/Alpha-Wölkchen vor dem Spieler, unterdrückt nahe Feuer/Fackel.
- **Nacht-Farbgrading** global (kühl-blau) via DayNightClock — konsistent mit Teil B.
- (Optional, Polish) Dampf beim Trocknen am Feuer.

Beweis fließt in `frozen_approach.png` ein (Atem sichtbar) — kein separater Pflicht-Screenshot.

---

## Teil E — Screenshots-Harness

**KRITISCH — B007:** `private val OUT = localCurrentDirVfs["build/screenshots"]` + Import NICHT
ändern (fünftes Mal als DO_NOT_TOUCH).

Neue Captures anfügen + in `fun main()` registrieren:
```kotlin
captureFrozenApproach()
captureFrozenFootprints()
captureFrozenBlood()
captureQuestbookGlory()
```
Reproduzierbar: feste `time`, vorbefüllte SnowGrid/BloodGrid/FootprintGrid, Tween-Endzustand
direkt setzen. Bei GL-„Too many callbacks" einmal wiederholen.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/WorldScene.kt
  - game/src/desktopMain/kotlin/game/MapConfig.kt          (WorldAtmosphere + frozenApproach + MapId.FROZEN_APPROACH)
  - game/src/desktopMain/kotlin/game/QuestbookScreen.kt    (Pracht + Animationen)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt  (4 Captures anfügen + registrieren)
  - core/src/commonMain/kotlin/rpg/items/Inventory.kt      (spend/steal)

create:
  - core/src/commonMain/kotlin/rpg/weather/*               (SnowGrid, BloodGrid, DayNightClock, TemperatureField, FogState)
  - core/src/commonTest/kotlin/rpg/weather/*               (zugehörige Tests)
  - core/src/commonTest/kotlin/rpg/items/InventoryTest.kt  (falls noch nicht vorhanden — sonst modify)
  - game/src/desktopMain/kotlin/game/SnowOverlay.kt
  - game/src/desktopMain/kotlin/game/BloodOverlay.kt
  - game/src/desktopMain/kotlin/game/FootprintOverlay.kt
  - game/src/desktopMain/kotlin/game/shader/FogFilter.kt   (NEU; bestehende Shader NICHT ändern)
  - briefs/2026-06-28-korge-step8-frozen-approach-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/shader/  BESTEHENDE Filter (Poison/BeerGoggle/Lighting/Rain/HeatShimmer)
        NUR konsumieren; NEU erlaubt: ausschließlich FogFilter.kt
- core/ bestehende Packages außer den genannten (rpg.bark, rpg.questbook, rpg.combat, rpg.tiled):
        NUR konsumieren. NEU erlaubt: rpg.weather/*; MODIFY erlaubt: rpg.items.Inventory
- game/src/desktopMain/kotlin/game/  HudOverlay, QuestbookOverlay, BattleScene, CharacterSprite,
        SpriteLoader, NpcDefinition, WaterOverlay, ShaderStateBinder  (unberührt)
- composeApp/
- settings.gradle.kts
- docs/KNOWN_BUGS.md   nur lesen
```

---

## ACCEPTANCE (gesamt)

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL (alle neuen Tests)
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot   → 20 bestehende + 4 neue = 24 PNGs
```
Neue PNGs müssen die Effekte DEUTLICH zeigen (Lehre aus 7d: Pfützen waren erst unsichtbar wegen zu
schwachem Alpha + Off-Camera-Koordinaten — Overlays mit kräftigem Alpha rendern UND die Capture so
positionieren, dass die Effekte um den kamerazentrierten Spieler liegen). Kein schwarzes Rechteck,
kein leeres Buch.

---

## Kontext / Querverweise

- **Lehre aus Step 7d (WICHTIG):** Overlay-Alpha mit Boden-Wert (z.B. `110 + depth*140`), und in
  den Captures die Grids um die Spieler-Kachel füllen (Kamera ist auf den Spieler zentriert), sonst
  liegt der Effekt off-screen. Siehe `WaterOverlay` + die korrigierten `captureWorldRainPuddles/
  PuddleDrain` als Vorlage.
- **KNOWN_BUGS B007:** `localCurrentDirVfs` korrekt lassen.
- **KNOWN_BUGS Step 5b:** `SolidRect` Leaf-View; Text kein Auto-Wrap.
- **KNOWN_BUGS Step 3:** `addUpdater { dt -> }` Param `kotlin.time.Duration`; `dt.seconds`.
- **Bestehende Bausteine zum Konsumieren:** `LightingFilter`/`LightSource` (Fackel), `RainFilter`
  (→ als Schnee-Schräge wiederverwendbar/oder eigener Schnee-Fall im SnowOverlay), `ShaderEffects`,
  `WaterOverlay`-Muster, `ShaderStateBinder`. `FootprintGrid`/`DirtState`/`WindState` aus 7d.
- **Donor-Policy:** KorGE nur als Referenz lesen, kein Fremdcode ins Repo (KORGE_MIGRATION_PLAN §1).
- **Nicht in diesem Brief (Folge):** Material-Ermüdung (#3), Rost (#9), Geruchs-Wolken (#6),
  Insekten (#16), Jahreszeiten-Migration (#27), AUDIOMANCER. Bewusst aufgespart.
```
