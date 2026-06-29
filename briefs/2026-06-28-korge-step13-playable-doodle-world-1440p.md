# Brief: Step 13 — Spielbare Doodle-Welt @ 1440p (Bild-Hintergrund + Grid + Doodle-Figur)

**MODELL: Opus-only** (vor Start Modell prüfen — `.kiro/steering/handoff-protocol.md`).
**Pflichtlektüre:** `.claude/skills/gaime-shaders/SKILL.md` — Filter-Muster, B007,
„render ≠ logic: PNG ansehen", `tex()` braucht Pixel-Koordinaten, grid-as-unit.

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step13-playable-doodle-world`
**BASE_SHA:** `607c4378`

---

## Ziel

Alles zusammenführen, was die letzten Schritte bewiesen haben — als **eine spielbare Szene**:
- **Hochauflösender, gemalter Hintergrund** (`tavern_interior.png`), scharf, kein Shader.
- **Unsichtbares Logik-Raster** darunter (`tavern_interior.tmx` → `CollisionGrid`): Bewegung +
  Begehbarkeit laufen über das Grid (Bild = Haut, Grid = Logik).
- **Steuerbare Figur** mit dem **DoodleLineFilter** (Step 12) auf dem Charakter-Layer — getuschter
  Cartoon-Look, Hintergrund bleibt scharf.
- **Volle Auflösung: 1440p** als echte Fenster-Ausgabe (`:game:run`).

Das ist der erste echte „so soll das Spiel aussehen"-Moment. Bestehende `WorldScene` (Tilemap,
NPCs, Dialog, Kampf) bleibt unangetastet als Referenz — ihre Feature-Migration ist ein Folge-Brief.

---

## Teil A — 1440p-Ausgabe (`Main.kt`)

KorGE-Entry auf 1440p konfigurieren. Exakte `Korge(...)`-Parameter (z.B. `virtualSize`/`windowSize`
in KorGE 6.0) gegen `korge-6.0.0-sources.jar` verifizieren — nicht raten.
- Virtuelle Größe + Fenstergröße auf **2560×1440** (16:9).
- In die neue `DoodleWorldScene` booten (statt `WorldScene`).
- `backgroundColor` dunkel (Letterbox-Ränder).

---

## Teil B — `DoodleWorldScene.kt` (neue Scene)

Aufbau in `sceneMain()`:
1. **Hintergrund:** `assets/HD/backgrounds/tavern_interior.png` vollflächig, `smoothing = true`,
   **seitenverhältnis-erhaltend auf die 1440-Höhe einpassen** (das Bild ist quadratisch 1254×1254 →
   zentriert, dunkle Letterbox-Ränder; NICHT auf 16:9 verzerren). Kein Shader auf dem Hintergrund.
2. **Logik-Raster:** `tavern_interior.tmx` laden (`TmxLoader.parse(resourcesVfs[...])`),
   `CollisionGrid.from(map)`. Das ist die unsichtbare Bewegungs-/Kollisionsebene.
   - `screenTile = bgHeightOnScreen / gridRows` (gridRows = 78). Grid-Zelle (gx,gy) →
     Bildschirmposition relativ zum eingepassten Bild (Offset der Letterbox berücksichtigen).
3. **Charakter-Layer:** eigener `Container`; darin der Spieler (`CharacterSprite`, `loadSwordsman()`).
   - **Grid-abgeleitete Skala** (NICHT hartkodiert): `charScale = tilesTall * screenTile / 64`
     (`tilesTall` so wählen, dass die Figur zu den gemalten NPCs passt, ~5).
   - Startposition: eine **verifiziert begehbare** Zelle (gegen `CollisionGrid` prüfen — B004).
   - `DoodleLineFilter` auf den Charakter-Layer (`charLayer.filter = doodleFilter`), und die
     Filter-`time` pro Frame ticken (Boil lebt) — via `addUpdater { dt -> doodleFilter.time += dt.seconds.toFloat() }`.
4. **Bewegung:** WASD/Pfeile → Zielzelle = aktuelle Zelle + Richtung; nur bewegen wenn
   `CollisionGrid[zelle]` WALKABLE/TRIGGER ist (Wände/Wasser/Möbel blockieren). Sanfte
   Tile-zu-Tile-Bewegung wie in `CharacterSprite.startMove()` (existiert). Position der Figur =
   Grid-Zelle → Bildschirm.
5. (Optional) `HudOverlay` oben — nur wenn ohne großen Aufwand; sonst weglassen.

**Keine** NPC-/Dialog-/Kampf-Logik in dieser Szene (bewusst minimal — erst die Kern-Schleife
„Doodle-Figur bewegt sich über gemaltem Hintergrund, Kollision aus dem Grid").

---

## Teil C — Screenshot-Beweis

**B007:** `localCurrentDirVfs`-Zeile + Import NICHT ändern.

Neue Capture `captureDoodleWorld()` bei `Size(2560.0, 1440.0)`, registriert in `main()`: baut
denselben visuellen Zustand wie `DoodleWorldScene` (Bild eingepasst + Doodle-Figur auf begehbarer
Zelle), `save("doodle_world_1440p")`. (Bewegung/Input ist manuell via `:game:run` — headless reicht
das Standbild.)

**Acceptance-Bild:** `doodle_world_1440p.png` zeigt die **scharfe gemalte Taverne** + eine
**Doodle-Figur mit getuschten Konturen** auf dem Boden, korrekt eingepasst (kein Verzerren), Figur
in sinnvoller Größe (passt zu den gemalten Figuren).

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/Main.kt                  (1440p Korge-Config + Boot in DoodleWorldScene)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt     (1 Capture anfügen + registrieren)

create:
  - game/src/desktopMain/kotlin/game/DoodleWorldScene.kt
  - briefs/2026-06-28-korge-step13-playable-doodle-world-1440p-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- game/src/desktopMain/kotlin/game/WorldScene.kt, BattleScene.kt  (bleiben als Referenz unberührt)
- game/src/desktopMain/kotlin/game/shader/*  (DoodleLineFilter etc. nur konsumieren)
- game/  CharacterSprite, MapConfig, alle Overlays, ShaderEffects, ShaderStateBinder  (nur konsumieren)
- core/, composeApp/, tools/mapbuilder/, assets/ (nur lesen), settings.gradle.kts
- docs/KNOWN_BUGS.md  nur lesen
```

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot   → inkl. doodle_world_1440p.png (2560x1440)
```
`:game:run` startet im 1440p-Fenster in der DoodleWorldScene (manuell/lokal verifizierbar). Bei
GL-„Too many callbacks" einmal wiederholen. Bestehende Screenshots müssen weiter funktionieren.

---

## Kontext / Querverweise

- **gaime-shaders Skill** (Pflicht): `tex()` braucht Pixel-Koordinaten; DoodleLineFilter ist fertig
  (nur `charLayer.filter = DoodleLineFilter(...)` + `time` ticken); grid-as-unit; PNG ansehen.
- **Bewiesen vorhanden:** `tavern_interior.png` + `tavern_interior.tmx` (78×78, Floor/Walls/Water),
  `CollisionGrid` (Layer-Order, B004), `CharacterSprite.startMove()` (sanfte Tile-Bewegung),
  `DoodleLineFilter` (EPX + Outline + Boil).
- **B004:** Spawn aus dem `CollisionGrid` ableiten (begehbare Zelle), nicht raten.
- **Aspect:** quadratisches Bild NICHT auf 16:9 zerren — auf Höhe einpassen, zentrieren, Letterbox.
- **Nächste Stufe (NICHT hier):** NPCs/Dialog/Kampf/Bark-Pipeline aus `WorldScene` in die
  Bild+Grid-Welt migrieren; Welt-/Außen-/Innen-Übergänge; Anime4K HQ A+B als Filter-Austausch;
  Kamera/Scrolling für Karten größer als der Bildschirm.
```
