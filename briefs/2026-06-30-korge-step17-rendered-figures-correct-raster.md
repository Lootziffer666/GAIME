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

**Zielgröße (Owner-Spec):** Körperhöhe **96px bei 1254²-Map** (≈ **6 Zellen** im 78er-Grid),
Körperbreite ~**32px** → **3:1**, klassischer 16-Bit-Standard. Pro Map aus dem Raster abgeleitet
(Reise-Maps 1366×768 → kleiner), nie hartkodiert.

---

## Teil A — Figuren-Skalierung nach OPAKEN Bounds (Kern-Fix)

In `CharacterSprite` (oder einem schlanken Helfer): beim Laden die **opaken Pixel-Grenzen** des
Idle-Frames bestimmen (transparenten Rand wegmessen) → `opaqueHeight`, `opaqueWidth`, und den
**Fuß-Offset** (Abstand opake Unterkante ↔ Frame-Unterkante).

- **Skala:** `charScale = (targetCellsTall * screenTile) / opaqueHeight`
  mit `targetCellsTall ≈ 6` (96px/16px-Zelle bei 1254). Damit wird der **Körper** 96px hoch,
  nicht der leere 64-Frame. (Nicht mehr `/64`.)
- **Fuß-Verankerung:** Figur so positionieren, dass die **opake Unterkante** auf der Grid-Zelle
  sitzt (Frame-Padding über den `footOffset` herausrechnen) — kein Schweben mehr.
- **Pro Map:** `targetCellsTall` bzw. die Ableitung gilt für die Gameplay-Skala; Reise-Maps
  (kleinere Figuren) bekommen entsprechend weniger — aus dem jeweiligen Raster ableiten.
- Verifizieren: gerenderte opake Körperhöhe ≈ 96px (bei 1254-Map) bzw. ~110px auf dem 1440-Screen.

> `CharacterSprite`-Konstruktor/-API möglichst kompatibel halten; wenn eine Signatur-Erweiterung
> nötig ist (z.B. Ziel-Höhe statt tileWidth), sauber umstellen und alle Aufrufer (DoodleWorldScene,
> ScreenshotHarness) mitziehen — aber WorldScene/BattleScene nicht brechen.

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
- `ImageNpcHotspot` → trägt zusätzlich einen **Sprite** (Idle-Sheet-Pfad + Facing). Jeder NPC wird
  als `CharacterSprite` (gleiche opake Skala + Doodle-Filter + Kontrast) auf seiner Zelle gerendert.
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
  - game/src/desktopMain/kotlin/game/CharacterSprite.kt        (opake Bounds: Skala + Fuß-Anker)
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

- **Gemessen:** Idle-Frame opak ≈ 9px Körper × 24px Höhe (Schwert macht Bbox bis 19 breit) in der
  64×64-Zelle. `÷64`-Skala war der Fehler → nach opaker Höhe skalieren.
- **Zielspec (Owner):** 96×32 bei 1254², 3:1. ≈6 Zellen hoch im 78er-Grid.
- **Qualitätsmarker:** gemalte (baked-in) Figuren sind der Maßstab — `figures_marker_check` ist die
  wiederholbare Prüfung. Aktuell: zu klein/zu dunkel → dieser Brief behebt beides.
- **Nächste Stufen (NICHT hier):** figurenfreie Wildwood/Exterior/Village + Grids (mapbuilder,
  Pfeiler 3); Doodle-Linien-Feinjustierung; Pfeiler 2b (Shader-Kette).
```
