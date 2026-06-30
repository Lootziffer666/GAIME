# Brief: Sheet-Normalizer — Sprite-Sheets auf ein universelles Raster bringen (Vorstufe zu Step 17)

**MODELL: Opus-only — SELBST ausführen, NICHT delegieren** (`.kiro/steering/handoff-protocol.md`).
Result MUSS bestätigen: „Vollständig auf Opus ohne Delegation ausgeführt."
**Pflichtlektüre:** `docs/MAP_ART_DIRECTION.md` (opake-Bounds-Prinzip, Figuren + Terrain),
`.claude/skills/gaime-shaders/SKILL.md` (Pro-Sheet-Normalisierung, Referenz-Pose, kein
Cross-Sheet-Raster), `docs/KNOWN_BUGS.md`.

**Datum:** 2026-06-30
**Branch:** `kiro/tool-sheet-normalizer`
**BASE_SHA:** `db06cd5b`

---

## Ziel

Ein **Offline-Preprocessing-Tool**, das aus einem rohen CraftPix-Sheet die **verwirrende
Transparenz entfernt** und ein **sauberes, uniformes Raster** + einen **Deskriptor** erzeugt —
„Zauberstab auf gefüllte Pixel, lite". Damit muss die Runtime (Step 17) Frame-Größe/Anker nicht
mehr raten, und das Problem „jedes Sheet hat ein anderes Raster" ist an der Wurzel gelöst.

**Belegt machbar (am echten Asset getestet):** Auf Charakter-/Animations-Sheets liegen die
voll-transparenten Spalten-/Zeilen-Bänder genau auf den Frame-Grenzen (Swordsman-Idle 768×256 →
12×4 erkannt; Dragon_head 480×64 → 10 Frames). **Dicht gepackte Objekt-Tilesets** (Dungeon
`Objects.png`) haben KEINE sauberen Gutter → die sind **nicht** Ziel dieses Tools (sie nutzen
Tiled-Metadaten). Dieses Tool ist für **Charakter-/Kreatur-/Animations-Sheets**.

---

## Tool — `tools/sheet-normalizer/` (Python, wie mapbuilder; PIL/numpy)

Eingabe: ein Sheet-PNG (+ optionaler Hinweis `--rows R --cols C` als Fallback). Schritte:

1. **Gutter-Erkennung:** opake Maske (`alpha > 20`). Voll-transparente **Spalten** (kein opakes
   Pixel über die ganze Höhe) und **Zeilen** (über die ganze Breite) finden → Runs bilden. Die
   Inhalts-Bereiche zwischen den Runs = Frame-Spalten/-Zeilen. Daraus `cols`, `rows` und die
   Frame-Zellgrenzen ableiten.
   - **Plausibilitätsprüfung:** Wenn die erkannten Zell-Breiten/-Höhen stark variieren oder kein
     sauberes Gitter ergeben (dichtes Objekt-Sheet) → **nicht raten**: mit `--rows/--cols`-Hinweis
     erneut versuchen; fehlt der, das Sheet **überspringen + im Report melden** („uneindeutig,
     Hinweis nötig"). Niemals stillschweigend ein falsches Raster ausgeben.
2. **Opake Bounds pro Zelle** messen. **Union-Bbox** über alle Zellen = die **uniforme, tighte
   Frame-Größe** (groß genug für die ausladendste Pose — Schwertschwung/Tod). Multi-Zellen-Posen
   bestimmen also die einheitliche Frame-Größe; kleinere Posen werden in diese Zelle einsortiert.
3. **Baseline-/Fuß-Ausrichtung:** alle Frames an der **opaken Unterkante** (Füße) ausrichten und
   horizontal an der Körperachse zentrieren, in die uniforme Zelle einsetzen. So ist der Anker über
   alle Frames stabil (keine „atmende"/springende Figur).
4. **Ausgabe** (neben dem Quell-Sheet, additiv — Quelle NICHT überschreiben):
   - `<name>.normalized.png` — sauberes Sheet: `rows × cols` uniforme, tight-gecroppte, fuß-
     ausgerichtete Frames, konsistenter (minimaler) Rand. Das „universelle Raster".
   - `<name>.sheet.json` — Deskriptor: `{ frameW, frameH, cols, rows, footAnchorX, footAnchorY,
     opaqueBodyH, source }`. **Das ist der funktionale Output**, den Step 17 (Kotlin) liest, statt
     zur Laufzeit zu raten.
5. **Debug-Kontaktblatt** `<name>.normalized.debug.png` — Original mit eingezeichnetem erkanntem
   Raster + Achsen/Fuß-Markern, zum Drüberschauen („render ≠ logic — ansehen").

Ein `--batch <dir>`-Modus, der alle Idle/Walk/Attack-Sheets eines Charakters verarbeitet.

---

## Anwenden + Beweis

Tool auf die **aktuell genutzten Charakter-Sheets** laufen lassen und Ergebnisse committen:
`assets/HD/characters/swordsman/**` und `assets/HD/characters/vampire/**` (Idle/Walk/Attack).
Optional ein Kreatur-Animations-Sheet (`assets/HD/locations/chapel/Tiled_files/Dragon_head.png`)
als Härtetest für Multi-Frame.

**Ansehen (Pflicht):** die `*.debug.png` — sitzt das erkannte Raster auf den Frames? Sind die
Füße über die Frames ausgerichtet? Stimmt `frameW×frameH` mit der ausladendsten Pose?

---

## SCOPE

```
create:
  - tools/sheet-normalizer/*                       (Python-Tool + README)
  - assets/HD/characters/swordsman/**/*.sheet.json + *.normalized.png + *.normalized.debug.png
  - assets/HD/characters/vampire/**/*.sheet.json   + *.normalized.png + *.normalized.debug.png
  - briefs/2026-06-30-tool-sheet-normalizer-result.md
```

## DO_NOT_TOUCH

```
- Die ORIGINAL-Sheets (*.png ohne .normalized) — nur lesen, nicht überschreiben.
- tools/mapbuilder/  (Owner-Tool — nicht anfassen)
- core/, game/, composeApp/, settings.gradle.kts  (Kotlin-Konsum = Step 17, NICHT hier)
- assets/ außerhalb von characters/  (Objekt-Tilesets sind nicht Ziel dieses Tools)
- docs/  (nur KNOWN_BUGS ergänzen, falls Fund)
```

> Reines Offline-Tool. KEINE Kotlin-/Build-Änderung in diesem Brief — die Runtime liest die
> `.sheet.json` erst in Step 17.

---

## ACCEPTANCE

```bash
cd tools/sheet-normalizer && python normalize.py --batch ../../assets/HD/characters/swordsman
   → erzeugt für jedes Sheet .normalized.png + .sheet.json + .normalized.debug.png
python normalize.py --batch ../../assets/HD/characters/vampire        → dito
```
- Swordsman-Idle wird als **12×4** erkannt (oder via Hinweis), Frames uniform + fuß-ausgerichtet.
- Attack-/Walk-Sheets: Frame-Größe = ausladendste Pose, alle Frames gleich groß (kein „Atmen").
- `.sheet.json`-Werte plausibel (`frameW/H`, `cols/rows`, `footAnchor`).
- Debug-Kontaktblätter angesehen und korrekt.
- Uneindeutige Sheets ohne Hinweis werden **gemeldet, nicht falsch ausgegeben**.

---

## Kontext / Querverweise

- **Getestet:** Gutter-Zentren des Swordsman-Idle liegen auf 63/127/191… = den 64er-Grenzen →
  Erkennung trägt. Dragon_head 480×64 → 10 Frames à 48. Dungeon-Objects = kein Gutter → nicht Ziel.
- **Warum offline statt Runtime-Messung:** ein neues Sheet ohne verwirrende Transparenz + Deskriptor
  ergibt ein **universelles Raster**; die Runtime liest nur noch `frameW/H` + Fuß-Anker, statt pro
  Sheet zu raten (Owner-Vorschlag). Vereinfacht Step 17.
- **Nächster Schritt (NICHT hier):** Step 17 so anpassen, dass `SpriteLoader` die `.sheet.json`
  konsumiert (Frame-Maße + Fuß-Anker), statt `DEFAULT_FRAME_SIZE=64` anzunehmen; dann Skala auf die
  physische Zielhöhe (96px@1254, 3:1).
```
