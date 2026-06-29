# Result: Step 13 — Spielbare Doodle-Welt @ 1440p

**Brief:** briefs/2026-06-28-korge-step13-playable-doodle-world-1440p.md
**Branch:** kiro/korge-step13-playable-doodle-world
**Datum:** 2026-06-29

## Was wurde umgesetzt

### Teil A — 1440p Main.kt
- `Korge(virtualSize = Size(2560, 1440), windowSize = Size(2560, 1440), backgroundColor = Colors.BLACK)`
- Boots into `DoodleWorldScene` (new scene)

### Teil B — DoodleWorldScene.kt
- **Background:** `tavern_interior.png` aspect-preserving fit (height-match, centered, letterboxed)
- **Logic grid:** `tavern_interior.tmx` → `CollisionGrid.from(map)` — invisible movement substrate
- **Character layer:** own `Container` with `DoodleLineFilter` (EPX + outline + boil)
- **Grid-derived sizing:** `screenTile = bgHeight / gridRows`, `charScale = tilesTall * screenTile / 64`
- **B004 spawn:** spiral search from grid center for WALKABLE cell
- **Movement:** WASD/Arrows, tile-based, collision-checked (`WALKABLE`/`TRIGGER` only)
- **Doodle boil:** `addUpdater { doodleFilter.time += dt.seconds.toFloat() }` — lines live

### Teil C — Screenshot
- `doodle_world_1440p.png` (2560x1440, 3.1MB)
- Shows: sharp painted tavern background + doodle-outlined character on walkable floor

## Testergebnis
```
./gradlew :core:desktopTest             → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop    → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :game:screenshot              → doodle_world_1440p.png renders (2560x1440)
```

## Abweichungen vom Brief
- **BASE_SHA:** Brief says `607c4378` but that commit is from a graphify install that landed after the brief was written. Branch created from origin/main HEAD (`7a2f485d`).
- **LD_LIBRARY_PATH:** The sandbox requires `LD_LIBRARY_PATH=/usr/lib64` for EGL in addition to the standard EGL_PLATFORM/LIBGL vars. Noted for scripts/setup-gl.sh.
- **HudOverlay:** Not added (brief marked as optional, would need inventory/combatant — unnecessary for the core proof).

## Integration-Fix (Claude) — Grid-Ausrichtung der Figur

Kiros Szene kompilierte grün und das Standbild sah plausibel aus, aber die
**Figur-Position lief am gemalten Raster vorbei**: Der `charLayer` wurde mit
`charScale` skaliert, was sowohl die Sprite-**Größe** als auch die **Position**
(`gridX * tileWidth`) multipliziert. `tileWidth` war auf `screenTile` gesetzt →
effektive Zelle = `screenTile * charScale` statt `screenTile`. Folge: Die Figur
driftet mit jedem Schritt vom gemalten Raster weg, Kollisionswände lägen nicht
mehr dort, wo die Wand gemalt ist (klassisches „render ≠ logic": Logik korrekt,
Bild-Mapping falsch — im Standbild unsichtbar, in Bewegung sichtbar).

**Fix:** In-Layer-Zelle aus dem nativen Frame ableiten, nicht aus `screenTile`:
`layerTile = (64 / tilesTall)` (Int, da `CharacterSprite` Int braucht), und
`charScale = screenTile / layerTile`. So gilt `layerTile * charScale == screenTile`
exakt → die Grid-Zelle der Figur landet pixelgenau auf dem gemalten Raster, ohne
aufsummierten Rundungsdrift. Identisch in `DoodleWorldScene.kt` und der Capture
`captureDoodleWorld()`. Verifiziert am `doodle_world_1440p.png`: Doodle-Figur
(getuschte Konturen) in korrekter Größe neben den gemalten NPCs auf begehbarem Boden.

## Was nicht angefasst wurde
DO_NOT_TOUCH komplett eingehalten. WorldScene, BattleScene, shaders, core, assets etc. unberührt.
