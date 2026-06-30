# Brief: Step 17 — Gerenderte Doodle-Figuren in korrekter Größe (Spieler + NPCs), Raster-Fix

**MODELL: Opus-only — SELBST ausführen, NICHT delegieren** (`.kiro/steering/handoff-protocol.md`,
Schlupfloch-Fix). Result MUSS bestätigen: „Vollständig auf Opus ohne Delegation ausgeführt."
**Pflichtlektüre VOR der Arbeit:**
- `.claude/skills/gaime-shaders/SKILL.md` — Figuren-Render, „render ≠ logic — PNG ansehen" (im
  Screenshot baked-in Figuren NICHT mit unseren verwechseln!), Doodle-Filter, B007, B004.
- `docs/MAP_ART_DIRECTION.md` — Figuren gerendert nicht baked-in; **Qualitätsmarker**; 3:1.
- `docs/KNOWN_BUGS.md` — B005 (64×64-Slicing) lesen; neue Funde ergänzen.

**Datum:** 2026-06-30
**Branch:** `kiro/korge-step17-rendered-figures`
**BASE_SHA:** `5eac9ea1`

---

## Problem (am Pixel gemessen)

Unsere gerenderte Figur ist **~3× zu klein** und **schwebt**. Ursache: wir skalieren die Figur
über `charScale = tilesTall * screenTile / 64` — also gegen die **64px-Framegröße**. Der opake
Charakter füllt aber nur **~24px Höhe × ~9px Körperbreite (≈3:1, Schwert macht die Bbox breiter)**
in diesem 64er-Frame. Folglich rendert der Körper bei `tilesTall=5` nur ~30px statt der gewollten
~96px, und weil der transparente Rand nicht zur Zelle ausgerichtet ist, steht die Figur nicht auf
der Zelle.

**Zielgröße (Owner-Spec):** Körper-Zielhöhe **96px bei 1254²-Map** (≈ **6 Zellen** im 78er-Grid),
Breite ~**32px** → **3:1**. **Das ist eine physische Ziel-Höhe in Map-Pixeln — KEINE Frame-/
Zellen-Annahme.** Pro Map aus dem Raster abgeleitet (Reise-Maps kleiner), nie hartkodiert.

---

## Teil A — Pro-Sheet-Normalisierung (KEINE Cross-Sheet-Raster-Annahme!)

**Kernprinzip (vom Owner, gestern + heute bekräftigt): Das Raster eines Sheets passt NICHT auf das
nächste.** `SpriteLoader` verdrahtet aktuell `DEFAULT_FRAME_SIZE = 64` und nimmt quadratische 64er-
Frames für ALLE Sheets an — falsch. Jeder Charakter-Sheet (Swordsman, Vampire, jeder NPC) kann eine
**andere Frame-Größe** und eine **andere Figurenhöhe im Frame** haben. Die Größe darf nie aus „64"
oder aus den Werten eines anderen Sheets abgeleitet werden — sie wird **pro Sheet gemessen und auf
die physische Zielhöhe normalisiert**.

1. **Frame-Größe pro Sheet deklarieren, nicht annehmen.** Eine `SpriteSheetSpec` einführen
   (`sheetPath`, `frameW`, `frameH`, `rows`, `cols` — bzw. `rows`/`cols` aus den deklarierten
   Frame-Maßen + Bitmap-Größe abgeleitet). Jeder Charakter referenziert seine eigene Spec.
   `SpriteLoader.load/sliceAllRows` bekommt die Frame-Maße **aus der Spec** (Default 64 nur als
   Fallback, nicht als Wahrheit). Falsche Frame-Größe ⇒ falsche opake Messung ⇒ falsche Größe —
   deshalb zuerst korrekt slicen.
2. **Opake Bounds pro Sheet messen.** Aus dem Idle-Frame des jeweiligen Sheets die opaken Pixel-
   Grenzen bestimmen → `opaqueHeight`, `opaqueWidth`, **Fuß-Offset** (opake Unterkante ↔ Frame-
   Unterkante). Diese Werte sind **pro Sheet verschieden** und werden gemessen, nicht angenommen.
3. **Auf physische Zielhöhe normalisieren:**
   `charScale = targetBodyPx_screen / opaqueHeight`,
   wobei `targetBodyPx_screen = targetBodyPx_map * (OUTPUT_H / mapHeightPx)` und
   `targetBodyPx_map = 96` für die Gameplay-Skala (1254). So rendert **jeder** Charakter — egal von
   welchem Sheet, egal welche Frame-Größe — den **Körper auf dieselbe physische Höhe**.
4. **Skala an EINE Referenz-Pose koppeln, NICHT pro Frame neu normalisieren.** `opaqueHeight` /
   `footOffset` **einmal aus der Idle-/Steh-Referenz** des Sheets messen; den daraus gewonnenen
   `charScale` + Fuß-Anker auf **alle** Frames desselben Sheets anwenden. Sonst „atmet" die Figur
   (schrumpft beim Ausholen), weil Attack-/Death-Frames eine andere opake Bbox haben.
5. **Multi-Zellen-Frames dürfen überhängen.** Manche Sheets haben Posen (Schwertschwung, Tod,
   große Kreaturen), deren Inhalt **ein Vielfaches des Rasters** überspannt. Bei fester Referenz-
   Skala ragt so ein Frame korrekt über die Nachbarzellen — **nicht** zurück in die Zelle quetschen.
   **Logisch belegt die Figur immer genau EINE Zelle** (Bewegung/Kollision/Fußspur = 1 Zelle),
   egal wie weit der Sprite visuell ragt (Bild = Haut, Grid = Logik — auf Figurenebene).
6. **Fuß-Verankerung:** Figur so positionieren, dass die (Referenz-)opake Unterkante auf der Grid-
   Zelle sitzt (Frame-Padding via `footOffset` herausrechnen) — kein Schweben, kein Springen
   zwischen Animationsframes.
7. Verifizieren: opake Körperhöhe im PNG ≈ Ziel (≈110px auf dem 1440-Screen bei der 1254-Map) —
   für Spieler **und** NPCs, auch wenn deren Sheets unterschiedliche Frame-Größen haben; und die
   Figur bleibt über die Animation **größenstabil** (kein Atmen).

> `CharacterSprite`/`SpriteLoader`-API kompatibel erweitern (Frame-Maße + Ziel-Höhe pro Sprite);
> alle Aufrufer (DoodleWorldScene, ScreenshotHarness) mitziehen — WorldScene/BattleScene NICHT
> brechen (Default-Fallback 64 behalten, nur nicht mehr als feste Wahrheit).

---

## Teil B — Kontrast: Figur muss lesen

Aktuell dunkelt der `DoodleLineFilter` die Fläche ab → Figur säuft auf dunklem Boden ab.
- Filter so justieren, dass er **Kontur zeichnet statt die Fläche abzudunkeln** (Outline betonen,
  Flächen-Multiplikator Richtung 1.0). Ggf. leichte Aufhellung/Sättigung der Füllung.
- Ziel: die Doodle-Figur liest klar gegen den gemalten Hintergrund, in der Helligkeits-Familie
  der gemalten Marker-Figuren (nicht dunkler).

---

## Teil C — NPCs als GERENDERTE Doodle-Figuren (nicht mehr Hotspots)

Die figurenfreien Maps haben keine gemalten Figuren mehr → NPCs müssen gerendert werden.
- `ImageNpcHotspot` → trägt zusätzlich eine **`SpriteSheetSpec`** (Sheet-Pfad + dessen eigene
  Frame-Maße) + Facing. Jeder NPC wird als `CharacterSprite` gerendert — **mit seiner eigenen
  Sheet-Spec**, opak gemessen und auf dieselbe physische Zielhöhe normalisiert (Teil A), Doodle +
  Kontrast. So sind NPCs gleich groß wie der Spieler, egal von welchem Sheet sie stammen.
- Interaktion bleibt grid-basiert (E auf Blickrichtungs-Zelle) wie bisher.
- Verschiedene Idle-Sheets nutzen, die schon im Repo liegen (`assets/HD/characters/*`), damit NPCs
  sich optisch unterscheiden (Barkeep/Patron/Guard).
- DEBUG_HOTSPOTS-Marker können bleiben (separat), aber die NPCs sind jetzt sichtbare Figuren.

---

## Teil D — Figurenfreie Taverne als Hintergrund

`ImageWorldDef.tavernInterior()` auf die figurenfreie Quelle umstellen:
`assets/HD/backgrounds/figurefree/tavern_interior.png` (1254², gleicher Grundriss → bestehende
`assets/HD/backgrounds/tavern_interior.tmx` als Grid weiterverwenden; Begehbarkeit der Spawn-/NPC-/
Exit-Zellen gegen das Grid verifizieren, B004).

> **Nur die Taverne** in diesem Brief (sie hat ein verlässliches `.tmx`). Wildwood/Exterior/Village
> bekommen ihre figurenfreien Maps + Grids in einem Folge-Brief (mapbuilder-Segmentierung der neuen
> Bilder = Pfeiler 3) — hier NICHT, um nicht auf Map-Segmentierung zu blockieren.

---

## Teil E — Screenshot-Beweise

**B007:** `localCurrentDirVfs`-Zeile + Import NICHT ändern.

Über `renderUnifiedScene`/eine Capture bei `Size(2560,1440)`:
1. `figures_tavern` — figurenfreie Taverne + Spieler + NPCs als gerenderte Doodle-Figuren, korrekt
   skaliert (Körper ~110px auf dem Screen) und **auf dem Boden stehend** (kein Schweben).
2. `figures_marker_check` — **denselben Spieler-Sprite** zusätzlich auf der ALTEN baked-in
   `tavern_interior.png` rendern (die hat gemalte Figuren) und daneben stellen → Größen-/Stil-
   Vergleich gegen den Marker (Körperhöhe ~ gleich wie die gemalten Figuren?).

**Beweis-Kriterium:** Im PNG nachmessen/ansehen — gerenderte Figur ~96px (Map) / ~110px (Screen)
hoch, 3:1, steht auf der Zelle, liest klar, und ist in `figures_marker_check` **vergleichbar groß**
wie die gemalten Marker-Figuren. (NICHT wieder eine gemalte Figur als unsere ausgeben — die
gerenderte ist die mit Doodle-Kontur an der Spawn-Zelle.)

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/SpriteLoader.kt           (Frame-Maße pro Sheet, 64 nur Fallback)
  - game/src/desktopMain/kotlin/game/CharacterSprite.kt        (opake Bounds: Skala + Fuß-Anker, pro Sheet)
  - game/src/desktopMain/kotlin/game/DoodleWorldScene.kt       (Spieler+NPC gerendert, neue Skala, figurefree bg)
  - game/src/desktopMain/kotlin/game/world/ImageWorldDef.kt    (NPC-Sprite-Feld; tavern → figurefree)
  - game/src/desktopMain/kotlin/game/shader/DoodleLineFilter.kt(Kontrast: Outline statt Flächen-Abdunklung)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt      (2 Captures anfügen)
create:
  - briefs/2026-06-30-korge-step17-rendered-figures-correct-raster-result.md
```

## DO_NOT_TOUCH

```
- game/.../ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/.../WorldScene.kt, BattleScene.kt, MapConfig.kt   (CharacterSprite-Änderung darf sie NICHT brechen — nur additive/kompatible API)
- game/.../overlay/*, systems/*, andere *Overlay.kt      (Step 15/16 — nicht hier)
- core/, composeApp/, tools/mapbuilder/, settings.gradle.kts
- assets/ (nur lesen; figurefree/ + characters/ konsumieren)
- docs/ (nur KNOWN_BUGS ergänzen)
- figurenfreie Wildwood/Exterior/Village + deren Grids — Folge-Brief, NICHT hier
```

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot
   → inkl. figures_tavern.png + figures_marker_check.png (2560×1440)
   → ALLE bestehenden Screenshots weiterhin erzeugt, KEIN Regress
```
Bei GL „Too many callbacks" einmal wiederholen. Sandbox ggf. `LD_LIBRARY_PATH=/usr/lib64`.

---

## Kontext / Querverweise

- **Gemessen (NUR Swordsman):** Idle-Frame opak ≈ 9px Körper × 24px Höhe (Schwert macht Bbox bis 19
  breit) in einer 64×64-Zelle. Diese Zahlen gelten **nur für dieses Sheet** — für jedes andere Sheet
  neu messen. `÷64`-Skala + Cross-Sheet-Annahme war der Fehler → pro Sheet opak messen + auf die
  physische Zielhöhe normalisieren.
- **Zielspec (Owner):** 96×32 bei 1254², 3:1 — physische Map-Pixel, keine Frame-Annahme.
- **Qualitätsmarker:** gemalte (baked-in) Figuren sind der Maßstab — `figures_marker_check` ist die
  wiederholbare Prüfung. Aktuell: zu klein/zu dunkel → dieser Brief behebt beides.
- **Nächste Stufen (NICHT hier):** figurenfreie Wildwood/Exterior/Village + Grids (mapbuilder,
  Pfeiler 3); Doodle-Linien-Feinjustierung; Pfeiler 2b (Shader-Kette).
```
