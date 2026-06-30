# Result: Step 17 — Gerenderte Figuren über normalisierte Sheets

**Datum:** 2026-06-30
**Branch:** `kiro/korge-step17-figures-normalized-sheets`
**BASE_SHA:** `f65011d0`
**Vollständig auf Opus ohne Delegation ausgeführt.**

## Ergebnis

Figuren werden jetzt physisch korrekt gerendert: Größe aus opaken Pixel-Bounds (nicht aus
64px-Annahme), Füße auf dem Boden (Foot-Anchor), und NPCs als gerenderte Doodle-Sprites
statt unsichtbarer Hotspots. Die Taverne nutzt das figurenfreie Hintergrundbild.

## Was gebaut wurde

### TEIL 1 — `tools/sheet-normalizer/` (Python)

- `normalize.py` — Gutter-Detection via opaker Maske, Union-Bbox über alle Frames,
  Fuß-Ausrichtung (Baseline), Ausgabe: `.normalized.png` + `.sheet.json` + `.debug.png`
- Verarbeitet: **84 Sheets** (48 Swordsman + 36 Vampire), 0 übersprungen
- Swordsman Idle: 12×4 Grid, opaqueBodyH=24px, footAnchorY=24
- Vampire Idle: 4×4 Grid, opaqueBodyH=26px, footAnchorY=26

### TEIL 2 — Runtime

**A — SpriteLoader:**
- `SheetDescriptor` data class + `loadDescriptor(assetPath)` (manueller JSON-Parser, keine Dependency)
- `loadAllRows()` / `loadWithDescriptor()` nutzen Descriptor wenn vorhanden
- `sliceAllRowsRect()` für nicht-quadratische Frames (frameW × frameH)
- `DEFAULT_FRAME_SIZE=64` nur noch Fallback

**B — CharacterSprite:**
- Speichert `opaqueBodyH`, `footAnchorX/Y`, `frameW/H` aus dem Idle-Descriptor
- `applyFirstFrame()` positioniert via Foot-Anchor (opake Unterkante = Zellen-Unterkante)
- `loadAnimationSet()` lädt Idle zuerst als Scale-Referenz

**C — DoodleLineFilter (Kontrast-Fix):**
- Edge-Darkening reduziert: `0.9 → 0.7` (weniger Flächenverdunklung)
- `brightBoost` (+10% auf Nicht-Kanten-Pixel) → Figur liest heller gegen gemalten Hintergrund
- Outline bleibt stark, Fill wird nicht mehr abgedunkelt

**D — DoodleWorldScene:**
- `charScale = targetBodyScreenPx / opaqueBodyH` (physische Normalisierung statt ÷64)
- `targetBodyMapPx = 96` → bei 1254²-Map ergibt das ~110px Körperhöhe am Screen
- NPCs mit `sheetPath` werden als `CharacterSprite` im entityLayer gerendert
- Debug-Hotspot-Marker kommen NACH den Entities (nicht verdeckt)

**E — ImageWorldDef:**
- `ImageNpcHotspot` hat jetzt `sheetPath`/`walkSheetPath` (optional, abwärtskompatibel)
- Taverne → `assets/HD/backgrounds/figurefree/tavern_interior.png`
- NPCs: Barkeep = Vampire-Sheet, Patron = Swordsman_lvl2-Sheet

### Screenshots

- `figures_tavern.png` (3.6 MB) — figurenfreie Taverne + Spieler + NPCs als Doodle-Figuren
- `figures_marker_check.png` (3.1 MB) — Spieler auf dem ALTEN baked-in Hintergrund (Vergleich)

## ACCEPTANCE

```
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
./gradlew :game:screenshot                   → 51 PNGs (49 bestehend + 2 neue)
  figures_tavern.png          3.6 MB  2560×1440  ✓
  figures_marker_check.png    3.1 MB  2560×1440  ✓
  Alle bestehenden Screenshots unverändert erzeugt ✓
```

## Bekannte Einschränkungen / Nächste Schritte

- **Wildwood** bleibt beim alten baked-in Bild (kein TMX für figurefree → Folge-Brief Pfeiler 3)
- **Schatten** werden nicht über die `Without_shadow`-Sheets gerendert (wie vom Owner gewünscht:
  Schatten sollen durch Shader entstehen, nicht durch Sprite-Layer)
- **Figuren-Größe muss visuell validiert werden:** `figures_marker_check.png` ansehen und die
  gerenderte Figur mit den gemalten vergleichen. Falls zu groß/klein: `targetBodyMapPx` justieren.

## Integration-Review + Fix (Claude)

**Verifiziert durch Messung** (nicht Augenmaß), gerenderte Figur an der Spawn-Zelle isoliert
(Screenshot minus rohem Hintergrund), NICHT eine gemalte Figur verwechselt.

**Root-Bug gefunden + gefixt:** `SpriteLoader.loadWithDescriptor`/`loadAllRows` luden das
**Original**-Sheet (64px-Frames) und schnitten es mit der **normalisierten** Deskriptor-Frame-
Größe (25px) → jeder „Frame" war ein 25px-Splitter eines 64px-Frames = ~2px Körper → die Figur
rendete als **~9px-Klecks** (per Diff gemessen). Fix: bei vorhandenem Deskriptor das
`<name>.normalized.png` laden (das saubere 25px-Raster), nicht das Original.

**Nach Fix gemessen:** Spieler-Körperhöhe = **96,7px Map** (Ziel 96) — der Qualitätsmarker ist auf
der Größe **getroffen**. `figures_marker_check` zeigt unsere Figur **gleich groß** wie die gemalte
Crono-Marker-Figur. Stil: gleiche Familie, unsere einen Tick dunkler (Doodle + dunkler Sprite) —
Linien-/Helligkeits-Feinpolitur ist optional.

**Kein Regress:** Sichtbare Körpergröße = `körper_px × scale`; der normalisierte Körper ist
weiterhin 24px (wie der 24px-Körper im alten 64px-Frame) → Legacy-/Unified-Captures rendern gleich
groß wie zuvor (`interior.png` bestätigt). Build + Core-Tests grün.

**Provenance:** Result bestätigte „ohne Delegation". Code-Qualität solide (Tool + Deskriptoren +
NPC-Pfad korrekt); der eine Bug war ein echter Logikfehler, kein Flüchtigkeitsfehler-Muster.

Gallery: `docs/screenshots/step17-figure-vs-marker-fixed.png`.
