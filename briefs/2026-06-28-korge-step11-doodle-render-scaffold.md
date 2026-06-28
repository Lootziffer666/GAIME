# Brief: Step 11 — Render-Gerüst: Doodle-Figuren vor hi-res Hintergrund @ 1440p + unsichtbares Logik-Raster

**MODELL: Opus-only** (vor Start Modell prüfen — neuer Thread erbt die Wahl NICHT;
`.kiro/steering/handoff-protocol.md` → „Modell-Anforderung").

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step11-doodle-render-scaffold`
**BASE_SHA:** `e7641993`

---

## Ziel & Vision (Owner)

Endbild: **1440p-Ausgabe. Hochauflösende, gemalte Hintergründe** (KI-generierte Map-Bilder wie
`assets/HD/backgrounds/sylvanoria_wildwood.png`) — **davor Figuren mit feinen, lebendigen Linien
im Doodle-/Zeichentrick-Look**, die pro Frame minimal „boilen" (sich leicht verändern), klar
abgesetzt vom Hintergrund.

Dieser Auftrag baut **nicht** das volle Anime4K-CNN, sondern das **Render-Gerüst**, das den Look
beweist. Steht das Gerüst, ist „voll Anime4K HQ A+B" nur noch ein Austausch der Shader-Kette auf
bewiesenem Fundament (wie bei der Filter-Komposition in Step 9).

**Architektur-Modell (wichtig):**
- **Hintergrund-Layer:** das hochauflösende Bild direkt vollflächig gezeichnet (crisp, KEIN
  Shader, KEIN Pixel-Upscale-Look).
- **Charakter-Layer:** Figuren auf einem EIGENEN Container, durch den Doodle-Linien-Shader. So
  bleibt der Hintergrund scharf und nur die Figuren bekommen den Zeichentrick-Look (Per-Layer —
  geht seit Step 9 / ComposedFilter).

**Donor-Policy (gilt):** KEIN Anime4K-Fremdcode kopieren. Der Linien-Look wird aus dem
**klassischen Anime4K-Konzept neu implementiert** (Kanten/Gradient → Linien verdünnen + abdunkeln),
nur als Referenz gelesen. Kommentar im Shader nennt das Konzept, kopiert aber keinen Code.

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step11-doodle-render-scaffold origin/main
git log --oneline -3   # soll e7641993 ganz oben zeigen
```

---

## Teil A — `DoodleLineFilter` (neuer Shader)

Neue Datei `game/src/desktopMain/kotlin/game/shader/DoodleLineFilter.kt`, Muster wie die
bestehenden Filter (`PoisonFilter` etc.: `ShaderFilter`, `UniformBlock` mit eigenem
`fixedLocation`, `BaseProgramProvider`, `programProvider`). Bestehende Shader NICHT ändern.

Zweck: aus dem Charakter-Sprite einen **feinen Cartoon-Linien-Look** machen, der pro Frame leicht
variiert („boil"):
- **Linien:** lokale Luminanz-/Alpha-Gradienten (Nachbar-Samples) erkennen → Kanten **abdunkeln
  und verdünnen** (klassisches Anime4K-Prinzip, neu implementiert — keine CNN-Gewichte nötig).
- **Boil/Per-Frame-Jitter:** ein winziger zeitgetriebener Offset auf die Sample-Koordinaten
  (`u_Time`), sodass die Linien minimal zittern → lebendiger Doodle-Look. Der Jitter MUSS für den
  Screenshot mit festem `u_Time` reproduzierbar sein.

Uniforms (eigener `fixedLocation`, frei ab 11):
```
u_Time        Float   // Animationszeit (Boil)
u_LineStrength Float  // 0..1, wie kräftig die Linien
u_Jitter      Float   // 0..~1, Boil-Amplitude
u_TexSize     vec2    // Texturgröße für Texel-Schritt (TexInfoUB.u_TextureSize nutzen)
```
`startTimeUpdater` in `ShaderEffects` muss den Filter mit `time` versorgen (Filter dort
registrieren wie die anderen; `ShaderEffects.kt` ist im Scope).

---

## Teil B — 1440p-Capture: Doodle-Figur vor hi-res Hintergrund

Neue Capture im `ScreenshotHarness` bei **2560×1440** (`korgeScreenshotTest(Size(2560.0, 1440.0))`):
1. **Hintergrund:** `assets/HD/backgrounds/sylvanoria_wildwood.png` vollflächig zeichnen
   (`image(resourcesVfs[...].readBitmap())`, auf 2560×1440 skaliert, `smoothing = true` für den
   gemalten Look). Kein Shader auf dem Hintergrund.
2. **Charakter-Layer:** ein eigener `Container`; darin 2–3 `CharacterSprite` (Swordsman/Vampire),
   deutlich vergrößert (z.B. scale 4–6 → große Figuren wie im 1440p-Ziel), auf sinnvollen
   Positionen vor dem Hintergrund. Auf DIESEN Container `DoodleLineFilter` anwenden
   (`effects.enable(charLayer, doodleLineFilter)` oder `charLayer.filter = doodleLineFilter`),
   `u_LineStrength` ~0.8, `u_Jitter` ~0.4, `u_Time` fest (z.B. 1.5).
3. `save("doodle_1440p")`.

**KRITISCH — B007:** `localCurrentDirVfs["build/screenshots"]` + Import NICHT ändern (8. Mal).

**Acceptance B (Screenshot):** `doodle_1440p.png` (2560×1440) zeigt den **scharfen, gemalten
Sylvanoria-Hintergrund** UND **davor Figuren mit deutlichem feinen Linien-/Doodle-Look** — der
Kontrast „Zeichentrick vor hochauflösendem Hintergrund" muss klar erkennbar sein. Hintergrund darf
NICHT verwaschen/gefiltert sein; Figuren MÜSSEN den Linien-Effekt tragen.

---

## Teil C — Docs an die Vision angleichen

- **`docs/SHADER_VISION.md`:** die Zeile „Kein Runtime-Upscaler (Anime4K etc.) — die Shader SIND
  der Stil" ist überholt. Ersetzen durch die neue Richtung: **hi-res gemalte Hintergründe +
  Charakter-Layer durch einen (neu implementierten) Linien-/Upscale-Shader für den Doodle-Look;
  Ziel 1440p; Anime4K HQ A+B als späterer Austausch der Shader-Kette.** Donor-Policy bleibt: aus
  Konzept nachbauen, kein Fremdcode.
- **`docs/KORGE_MIGRATION_PLAN.md`:** kurzer Eintrag, dass Step 11 das Render-Gerüst (1440p +
  Doodle-Charakter-Layer) liefert.

---

## Teil D — Bild + unsichtbares Logik-Raster koppeln

Owner-Architektur: **„Unter dem hochauflösenden Bild liegt unsichtbar das Tile-Raster, die
gesamte Logik läuft über das Raster."** Genau das beweist dieser Teil — das Bild ist nur die Haut,
das Grid ist das Logik-Substrat.

Das Raster liegt schon bereit: `assets/HD/backgrounds/sylvanoria_wildwood.tmx` (86×48, vom
mapbuilder aus exakt diesem Bild segmentiert → Layer `Floor`/`Walls`/`Water`). Es deckt sich 1:1
mit dem Bild (16px/Tile), d.h. Grid-Zelle (gx,gy) ↔ Bild-Region.

1. TMX laden (`TmxLoader.parse(resourcesVfs[...])`), `CollisionGrid.from(map)` bauen — das ist die
   **unsichtbare Logik-Ebene** unter dem Bild. Die Physik-Grids in `:core` (Water/Snow/Blood/…)
   sind alle gitter-basiert und renderer-agnostisch — sie laufen über genau dieses Raster.
2. Die Figur(en) aus Teil B auf eine **verifiziert begehbare** Zelle setzen (gegen `CollisionGrid`
   prüfen — B004-Methode: offset + WALKABLE-bbox), Position aus Grid-Koordinate × Bild-Tile-Größe.
3. **Debug-Beweis-Capture** `grid_overlay_debug.png` (2560×1440): das Bild vollflächig + das
   CollisionGrid halbtransparent darübergelegt (WATER = blauer Tint, BLOCKED = roter Tint, WALKABLE
   = klar), auf passender Skala. Beweist, dass das unsichtbare Raster zum gemalten Hintergrund
   passt — der blaue Tint muss über dem gemalten Fluss/See liegen, roter über Dorf/Mauern.

**Hinweis:** Die Collision ist auto-segmentiert (grob — Wald = begehbar, Wasser = blockiert,
erkannte Gebäude = blockiert). Das reicht, um die **Kopplung** zu beweisen; Feinschliff der
Collision ist späterer Schritt. Im echten Spiel würde der Spieler sich auf diesem Grid bewegen,
während er das Bild sieht.

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/shader/ShaderEffects.kt   (DoodleLineFilter registrieren + time-driven)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt      (2 Captures anfügen + registrieren)
  - docs/SHADER_VISION.md
  - docs/KORGE_MIGRATION_PLAN.md

create:
  - game/src/desktopMain/kotlin/game/shader/DoodleLineFilter.kt
  - briefs/2026-06-28-korge-step11-doodle-render-scaffold-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/shader/  BESTEHENDE Filter (Poison/BeerGoggle/Lighting/Rain/
        HeatShimmer/Fog) NICHT ändern — NEU erlaubt: nur DoodleLineFilter.kt; ShaderEffects.kt MODIFY
- core/                          NUR konsumieren
- game/  WorldScene, BattleScene, alle Overlays, QuestbookScreen, MapConfig, CharacterSprite,
        SpriteLoader, NpcDefinition, ShaderStateBinder  (unberührt)
- assets/   nur lesen (Hintergrundbild + sylvanoria_wildwood.tmx sind schon committed)
- tools/mapbuilder/   NICHT anfassen (separates Owner-Tool)
- composeApp/ , settings.gradle.kts
- docs/KNOWN_BUGS.md   nur lesen
```

---

## ACCEPTANCE (gesamt)

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot   → bestehende + doodle_1440p.png + grid_overlay_debug.png (je 2560x1440)
```
Bei GL-„Too many callbacks" einmal wiederholen. Bestehende Screenshots müssen weiter funktionieren
(Regression — der neue Filter darf die anderen nicht beeinflussen).

`grid_overlay_debug.png` muss zeigen, dass das unsichtbare Raster zum Bild passt: blauer Tint über
dem gemalten Fluss/See, roter über Dorf/Mauern, der Rest klar/begehbar. Damit ist die Kopplung
„Bild = Haut, Grid = Logik" bewiesen.

---

## Kontext / Querverweise

- **Per-Layer-Shading** existiert seit Step 9 (`ShaderEffects` + KorGE `ComposedFilter`): den
  Doodle-Filter NUR auf den Charakter-Container, NICHT auf den Hintergrund.
- **Shader-Muster:** bestehende Filter in `game/shader/` (z.B. `PoisonFilter` für `UniformBlock`/
  `FragmentShader`/`BaseProgramProvider`-Struktur; `HeatShimmerFilter` für UV-Offset per `time`).
- **KNOWN_BUGS B007** (localCurrentDirVfs), **Step 3** (`addUpdater { dt -> }` = `Duration`, `dt.seconds`).
- **Reproduzierbarkeit:** Screenshot mit festem `u_Time`; kein `Random` im Shader.
- **Donor-Policy** (`KORGE_MIGRATION_PLAN §1`): Anime4K nur als Konzept-Referenz, kein Code kopieren.
- **Bild + Grid-Kopplung** ist jetzt Teil D dieses Briefs (war vorher Folge-Schritt).
- **Nächste Stufe (NICHT hier):** echte 1440p-Fenster-Ausgabe (`:game:run`, virtuelle Res/Scale),
  Anime4K HQ A+B CNN-Kette als Shader-Austausch, eine echte spielbare Szene (WorldScene) mit
  Bild-Hintergrund + diesem Grid (Bewegung/Physik live), Collision-Feinschliff.
```
