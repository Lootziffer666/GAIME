# Brief: Step 17 — Gerenderte Figuren über normalisierte Sheets (Normalizer-Tool + Runtime)

**MODELL: Opus-only — SELBST ausführen, NICHT delegieren** (`.kiro/steering/handoff-protocol.md`,
Schlupfloch-Fix). Result MUSS bestätigen: „Vollständig auf Opus ohne Delegation ausgeführt."
**Pflichtlektüre VOR der Arbeit:**
- `.claude/skills/gaime-shaders/SKILL.md` — opake-Bounds, **kein Cross-Sheet-Raster**, Skala an
  Referenz-Pose koppeln (kein „Atmen"), Multi-Zellen-Überhang/1 logische Zelle, Doodle-Kontrast,
  „render ≠ logic — PNG ansehen" (baked-in Figuren NICHT mit unseren verwechseln), B007, B004.
- `docs/MAP_ART_DIRECTION.md` — Figuren gerendert nicht baked-in; Qualitätsmarker; 3:1.
- `docs/KNOWN_BUGS.md` — B005 (Frame-Slicing); neue Funde ergänzen.

**Datum:** 2026-06-30
**Branch:** `kiro/korge-step17-figures-normalized-sheets`
**BASE_SHA:** `99652264`

Ein zusammenhängender Auftrag in zwei Stufen: **(1) Offline-Tool** normalisiert die Sheets zu
einem universellen Raster + Deskriptor; **(2) Runtime** liest die Deskriptoren und rendert Spieler
+ NPCs in korrekter physischer Größe. Reihenfolge im PR: erst Tool laufen lassen (Deskriptoren
committen), dann Kotlin darauf aufbauen.

---

## Problem

Unsere Figur rendert **~3× zu klein** und **schwebt**, weil wir gegen die 64px-Framegröße skalieren
(`÷64`), obwohl der opake Körper nur ~24px der 64 füllt — und weil `SpriteLoader` `DEFAULT_FRAME_SIZE
=64` für **alle** Sheets annimmt. **Das Raster eines Sheets passt nicht aufs nächste** (Owner). Lösung:
Sheets offline auf ein sauberes, uniformes Raster bringen + Deskriptor, dann normalisiert rendern.

**Zielspec (Owner):** Körper-Zielhöhe **96px bei 1254²-Map** (≈6 Zellen im 78er-Grid), Breite ~32px
→ **3:1**. Physische Map-Pixel, keine Frame-Annahme. Pro Map aus dem Raster abgeleitet, nie hartkodiert.

---

## TEIL 1 — Tool `tools/sheet-normalizer/` (Python, PIL/numpy; wie mapbuilder)

„Zauberstab auf gefüllte Pixel, lite": verwirrende Transparenz weg, universelles Raster + Deskriptor.

1. **Gutter-Erkennung:** opake Maske (`alpha>20`). Voll-transparente **Spalten** (kein opakes Pixel
   über die ganze Höhe) und **Zeilen** (ganze Breite) finden → Runs → Inhalts-Bereiche dazwischen =
   Frame-Spalten/-Zeilen → `cols`, `rows`, Zellgrenzen. (Belegt: Swordsman-Idle-Gutter liegen auf
   63/127/191… = den 64er-Grenzen → 12×4 erkannt; Dragon_head 480×64 → 10 Frames.)
   - **Plausibilität:** stark variierende Zellgrößen / kein sauberes Gitter (dichtes Objekt-Sheet)
     → **nicht raten**: optionaler `--rows/--cols`-Hinweis; fehlt er → Sheet **überspringen + melden**.
2. **Opake Bounds pro Zelle** → **Union-Bbox** = uniforme, tighte Frame-Größe (groß genug für die
   ausladendste Pose: Schwertschwung/Tod, die ein **Vielfaches des Rasters** überspannen darf).
3. **Fuß-/Baseline-Ausrichtung:** alle Frames an der **opaken Unterkante** (Füße) ausrichten,
   horizontal an der Körperachse zentriert, in die uniforme Zelle einsetzen → stabiler Anker über
   alle Frames (kein „Atmen"/Springen).
4. **Ausgabe neben dem Quell-Sheet (additiv, Quelle NICHT überschreiben):**
   - `<name>.normalized.png` — sauberes uniformes Sheet (`rows×cols`, tight, fuß-ausgerichtet).
   - `<name>.sheet.json` — **funktionaler Output:** `{ frameW, frameH, cols, rows, footAnchorX,
     footAnchorY, opaqueBodyH, source }`.
   - `<name>.normalized.debug.png` — Original mit eingezeichnetem Raster + Fuß-/Achsen-Markern.
5. `--batch <dir>` für alle Sheets eines Charakters.

**Anwenden + committen:** Tool auf `assets/HD/characters/swordsman/**` + `vampire/**`
(Idle/Walk/Attack) laufen lassen; Dragon_head als Multi-Frame-Härtetest. **Debug-PNGs ansehen** —
sitzt das Raster? Füße ausgerichtet? `frameW×frameH` = ausladendste Pose?

---

## TEIL 2 — Runtime liest die Deskriptoren

### A — `SpriteLoader`: Deskriptor statt Annahme
- Beim Laden eines Sheets das zugehörige `<name>.sheet.json` lesen, falls vorhanden → `frameW/frameH/
  cols/rows/footAnchor` **daraus** nehmen. `DEFAULT_FRAME_SIZE=64` nur noch **Fallback**, nicht Wahrheit.
- Bevorzugt das `<name>.normalized.png` rendern (uniformes Raster); sonst Original mit Deskriptor-Maßen.

### B — `CharacterSprite`: physische Normalisierung + Anker
- **Skala (an Referenz-Pose gekoppelt):** `charScale = targetBodyPx_screen / opaqueBodyH` (aus dem
  Deskriptor), mit `targetBodyPx_screen = 96 * (OUTPUT_H / mapHeightPx)` für die Gameplay-Skala (1254);
  pro Map abgeleitet (Reise-Maps kleiner). **Einmal aus der Idle-Referenz**, auf ALLE Frames anwenden.
- **Fuß-Anker:** `footAnchor` aus dem Deskriptor → opake Unterkante sitzt auf der Grid-Zelle (kein
  Schweben, kein Springen zwischen Frames).
- **Multi-Zellen-Überhang erlaubt:** ausladende Frames ragen über Nachbarzellen; **logisch belegt die
  Figur immer EINE Zelle** (Bewegung/Kollision/Fußspur). Nicht zurückquetschen.
- So rendern **Spieler und jeder NPC gleich groß**, egal von welchem Sheet sie stammen.

### C — Kontrast: Figur muss lesen
- `DoodleLineFilter` so justieren, dass er **Kontur zeichnet statt die Fläche abzudunkeln** (Flächen-
  Multiplikator → ~1.0, Outline betonen, ggf. leichte Aufhellung). Ziel: Figur liest klar gegen den
  gemalten Hintergrund, in der Helligkeits-Familie der gemalten Marker-Figuren — nicht dunkler.

### D — NPCs als GERENDERTE Doodle-Figuren (nicht mehr Hotspots)
- `ImageNpcHotspot` trägt zusätzlich Sheet-Pfad (+ dessen Deskriptor) + Facing. Jeder NPC =
  `CharacterSprite` mit **seinem eigenen** Deskriptor, gleicher physischer Zielhöhe, Doodle+Kontrast.
- Interaktion bleibt grid-basiert (E auf Blickrichtungs-Zelle). Verschiedene Sheets (Barkeep/Patron/
  Guard) aus `assets/HD/characters/*`.

### E — Figurenfreie Taverne
- `ImageWorldDef.tavernInterior()` → `assets/HD/backgrounds/figurefree/tavern_interior.png` (1254²,
  gleicher Grundriss → bestehendes `tavern_interior.tmx` weiternutzen; Spawn/NPC/Exit-Zellen gegen das
  Grid verifizieren, B004). **Nur die Taverne** (sie hat ein verlässliches `.tmx`); Wildwood/Exterior/
  Village + Grids = Folge-Brief (mapbuilder, Pfeiler 3).

---

## TEIL 3 — Screenshot-Beweise

**B007:** `localCurrentDirVfs`-Zeile + Import NICHT ändern.
1. `figures_tavern` — figurenfreie Taverne + Spieler + NPCs als gerenderte Doodle-Figuren, korrekt
   skaliert (Körper ~110px auf dem 1440-Screen) und **auf dem Boden stehend**.
2. `figures_marker_check` — **denselben Spieler-Sprite** zusätzlich auf der ALTEN baked-in
   `tavern_interior.png` rendern und daneben stellen → Größen-/Stil-Vergleich gegen den Marker
   (Körperhöhe ~ wie die gemalten Figuren?).

**Ansehen + nachmessen:** gerenderte Figur ~96px(Map)/~110px(Screen), 3:1, steht auf der Zelle, liest
klar, größenstabil über die Animation, und in `figures_marker_check` vergleichbar groß wie die gemalten
Figuren. (Die gerenderte ist die mit Doodle-Kontur an der Spawn-Zelle — NICHT eine gemalte verwechseln.)

---

## SCOPE

```
create:
  - tools/sheet-normalizer/*                                  (Python-Tool + README)
  - assets/HD/characters/swordsman/**/*.sheet.json + *.normalized.png + *.normalized.debug.png
  - assets/HD/characters/vampire/**/*.sheet.json   + *.normalized.png + *.normalized.debug.png
  - briefs/2026-06-30-korge-step17-figures-via-normalized-sheets-result.md
modify:
  - game/src/desktopMain/kotlin/game/SpriteLoader.kt          (Deskriptor lesen; 64 nur Fallback)
  - game/src/desktopMain/kotlin/game/CharacterSprite.kt       (physische Skala + Fuß-Anker, Referenz-Pose)
  - game/src/desktopMain/kotlin/game/shader/DoodleLineFilter.kt (Kontrast: Outline statt Flächen-Abdunklung)
  - game/src/desktopMain/kotlin/game/DoodleWorldScene.kt      (Spieler+NPC gerendert, neue Skala, figurefree bg)
  - game/src/desktopMain/kotlin/game/world/ImageWorldDef.kt   (NPC-Sheet-Feld; tavern → figurefree)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt     (2 Captures anfügen)
```

## DO_NOT_TOUCH

```
- ORIGINAL-Sheets (*.png ohne .normalized) — nur lesen, nicht überschreiben
- game/.../ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/.../WorldScene.kt, BattleScene.kt, MapConfig.kt   (SpriteLoader/CharacterSprite-Änderung NUR additiv/kompatibel — nicht brechen)
- game/.../overlay/*, systems/*, *Overlay.kt             (Step 15/16 — nicht hier)
- core/, composeApp/, tools/mapbuilder/, settings.gradle.kts
- assets/ außerhalb characters/ + figurefree/tavern_interior  (Objekt-Tilesets/andere Maps = Folge-Brief)
- docs/ (nur KNOWN_BUGS ergänzen)
```

---

## ACCEPTANCE

```bash
# Teil 1
cd tools/sheet-normalizer && python normalize.py --batch ../../assets/HD/characters/swordsman
python normalize.py --batch ../../assets/HD/characters/vampire
   → .normalized.png + .sheet.json + .normalized.debug.png je Sheet; uneindeutige werden gemeldet
# Teil 2
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

- **Gemessen (NUR Swordsman):** Idle opak ≈ 9px Körper × 24px Höhe (Schwert macht Bbox bis 19) in
  einer 64×64-Zelle — gilt nur für dieses Sheet; jedes andere neu messen (deshalb der Normalizer).
- **Owner-Spec:** 96×32 @1254², 3:1 (physisch). ≈6 Zellen hoch.
- **Qualitätsmarker:** gemalte (baked-in) Figuren sind der Maßstab — `figures_marker_check` ist die
  wiederholbare Prüfung.
- **Nächste Stufen (NICHT hier):** figurenfreie Wildwood/Exterior/Village + Grids (mapbuilder, Pfeiler
  3); Objekt-Tilesets als Multi-Zellen-Objekte (opake Bounds, Pfeiler 3); Pfeiler 2b (Shader-Kette).
```
