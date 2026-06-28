# Brief: Step 10 — Stokeport öffnet sich: ungenutzte Orts-Karten einbinden + Brücke als Verbinder

**MODELL: Opus-only** (vor Start Modell prüfen — neuer Thread erbt die Wahl NICHT;
`.kiro/steering/handoff-protocol.md` → „Modell-Anforderung").

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step10-stokeport-maps`
**BASE_SHA:** `5f48a697`

---

## Hintergrund & Ziel

Das Asset-Pack enthält **6 fertig authorte HD-Locations**, aber die KorGE-Engine rendert nur
**eine** (heroes-home interior+exterior). Vier weitere Exterior-Karten + die Brücke liegen ungenutzt.
Der alte Compose-Renderer konnte die Brücke nicht zusammensetzen (kaputter `world_bridge.png`) —
KorGE kann es (TmxLoader/TiledMapView sind seit Step 4b erprobt, inkl. animierter Tiles & mehrerer
Tilesets). Dieser Auftrag bindet die Karten ein und **setzt die Brücke als Übergang ZWISCHEN zwei
Orten** — so überbrückt sie echten Weg (nicht freigestellt).

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step10-stokeport-maps origin/main
git log --oneline -3   # soll 5f48a697 ganz oben zeigen
```

---

## Die einzubindenden Karten (alle Tileset-PNGs liegen bereits daneben — verifiziert)

| MapId (neu) | tmxDir | tmxFile |
|---|---|---|
| `CHAPEL` | `assets/HD/locations/chapel/Tiled_files` | `Exterior.tmx` |
| `GUILD_HALL` | `assets/HD/locations/guild-hall/Tiled_files` | `Exterior.tmx` |
| `GLASSBLOWERS` | `assets/HD/locations/glassblowers-workshop/Tiled_files` | `Exterior.tmx` |
| `RUINED_TEMPLE` | `assets/HD/locations/ruined-temple/Tiled_files` | `Ruined_temple_exterior.tmx` |
| `BRIDGE` | `assets/HD/locations/bridges/PNG_n_Tiled` | `Bridges.tmx` |

**Interiors sind NICHT Teil dieses Briefs** (bewusst aufgespart, um den Scope zu halten).

---

## ⚠️ DAS zentrale Risiko: Spawn-/Exit-Koordinaten (B004)

Jede dieser TMX ist `infinite="1"` mit **negativen Chunk-Offsets**. Hartkodierte Tile-Koordinaten
landen sonst im Nichts/Blocked (genau B004, der heroes-home schon getroffen hat).

**Pflicht-Vorgehen je Karte, bevor Spawn/Exits gesetzt werden:**
1. TMX laden, `CollisionGrid.from(map)` bauen.
2. `offsetX/offsetY` + die WALKABLE-Bounding-Box dumpen (kurzer Print/Debug).
3. Spawn = eine **verifiziert begehbare** Zelle nahe der Kartenmitte (gegen `grid[x-offX, y-offY]`
   prüfen — muss WALKABLE/TRIGGER sein).
4. Exit-Tiles ebenfalls aus begehbaren Randzellen ableiten, nicht raten.
5. Die ermittelten Koordinaten je Karte im Result-File dokumentieren (offset + spawn + exits).

Der Screenshot-Beweis (unten) zeigt, ob der Spieler auf Boden steht (nicht im Void/Fallback).

---

## Teil A — Karten als MapConfigs einbinden

`MapConfig.kt`:
- `MapId` um `CHAPEL, GUILD_HALL, GLASSBLOWERS, RUINED_TEMPLE, BRIDGE` erweitern.
- Je eine Factory (`chapel()`, `guildHall()`, `glassblowers()`, `ruinedTemple()`, `bridge()`) nach
  dem Muster von `exterior()`: tmxDir/tmxFile, verifizierter spawnX/Y, displayName, bgmPath
  (bestehende Musik wiederverwenden — keine neuen Assets), `atmosphere = WorldAtmosphere.CLEAR`
  (Default), NPCs vorerst leer (`emptyList()`), exits siehe Teil C.
- `forId(MapId)` + `forId(MapId, spawnX, spawnY)` um die neuen Fälle erweitern.

`BRIDGE` nutzt die animierten Wasser-Tiles — der TmxLoader behandelt animierte Tiles bereits
(Step 4). Falls eine Tile-/Tileset-Auflösung scheitert, im Result als Blocker dokumentieren, nicht
stumm überspringen.

---

## Teil B — Jede Karte rendert korrekt (Screenshot-Beweis)

Pro neuer Karte eine Capture im `ScreenshotHarness` (Muster: bestehende `captureWorld`/
`captureFrozenApproach`): TMX laden, Atlases, `TiledMapView`, Spieler auf den **verifizierten
Spawn**, Kamera zentriert. Captures + Registrierung in `fun main()`:
```kotlin
captureChapel(); captureGuildHall(); captureGlassblowers(); captureRuinedTemple(); captureBridge()
```

**KRITISCH — B007:** `private val OUT = localCurrentDirVfs["build/screenshots"]` + Import NICHT
ändern (siebtes Mal als DO_NOT_TOUCH).

**Acceptance B (Screenshots):** `map_chapel.png`, `map_guildhall.png`, `map_glassblowers.png`,
`map_ruined_temple.png`, `map_bridge.png` — jede zeigt eine **zusammenhängende Karte** (kohärentes
Terrain, KEIN rohes Tileset-Raster wie der alte `world_bridge.png`, kein schwarzes Bild), Spieler
sichtbar auf begehbarem Boden. `map_bridge.png` zeigt die Brücke über animiertem Wasser.

---

## Teil C — Overworld verbinden, Brücke als Verbinder

Die Orte zu einem kleinen Graphen verdrahten (Exits über begehbare Randzellen, B004-Methode):

```
            heroes-home EXTERIOR  (Dorf, Hub)
             /        |        \
        CHAPEL   GUILD_HALL   GLASSBLOWERS
                     |
                  BRIDGE   ← Brücke über den Fluss
                     |
               RUINED_TEMPLE
```

- Vom Dorf (`EXTERIOR`) je ein Exit zu CHAPEL, GUILD_HALL, GLASSBLOWERS (direkt).
- Der Weg zum RUINED_TEMPLE führt **über die BRIDGE**: `EXTERIOR` → `BRIDGE` (West-Ende) →
  überqueren → `BRIDGE` Ost-Exit → `RUINED_TEMPLE`. So überbrückt die Brücke einen echten Übergang.
- Jeder Ziel-Ort hat einen Rück-Exit zum Dorf bzw. zur Brücke.
- Exits sind `MapExit(tileX, tileY, destination, spawnX, spawnY)` — alle Koordinaten aus den
  begehbaren Randzellen der jeweiligen Karte ableiten, im Result dokumentieren.

`WorldScene` lädt bereits beliebige `MapConfig` über `pendingConfig` + `forId` — die bestehende
Exit-/Transition-Logik sollte ohne Änderung greifen. Falls eine kleine Anpassung nötig ist
(z.B. `forId` für die neuen Ziele), ist `WorldScene.kt` im Scope.

**Acceptance C:** Im Result eine Tabelle „Karte → offset → spawn → exits". (Spielbarer Rundgang ist
manuell/lokal; headless reicht der Screenshot je Karte + die dokumentierten Koordinaten.)

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/MapConfig.kt
  - game/src/desktopMain/kotlin/game/WorldScene.kt          (nur falls forId/Transition-Anpassung nötig)
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt   (5 Captures anfügen + registrieren)

create:
  - briefs/2026-06-28-korge-step10-stokeport-maps-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt → localCurrentDirVfs-Zeile + Import (B007)
- core/                          NUR konsumieren (TmxLoader/CollisionGrid/TileType unverändert)
- game/src/desktopMain/kotlin/game/shader/   unberührt
- game/  alle Overlays, HudOverlay, QuestbookOverlay/Screen, BattleScene, CharacterSprite,
        SpriteLoader, NpcDefinition, ShaderStateBinder  (unberührt)
- assets/   KEINE Asset-Dateien ändern/verschieben (TMX/PNG nur lesen)
- composeApp/ , settings.gradle.kts
- docs/KNOWN_BUGS.md   nur lesen
```

---

## ACCEPTANCE (gesamt)

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
bash scripts/setup-gl.sh; ./gradlew :game:screenshot   → bestehende + 5 neue PNGs
```
5 neue Karten-Screenshots, jede eine kohärente Karte mit Spieler auf Boden. `map_bridge.png` zeigt
Brücke über Wasser. Bei GL-„Too many callbacks" einmal wiederholen.

---

## Kontext / Querverweise

- **KNOWN_BUGS B004** (Infinite-TMX negative Offsets → Spawn/Exit aus CollisionGrid ableiten, NIE
  hartkodiert raten) — DAS ist hier die Hauptfehlerquelle. **B006** (Tileset-Auflösung via Classpath,
  `desktopRuntime()` — nicht anfassen). **B007** (localCurrentDirVfs). **B005** (Sprites 64er-Raster).
- **Erprobt seit Step 4b:** `TmxLoader` (infinite chunks, CSV, flip bits, animierte Tiles, mehrere
  Tilesets), `TilesetAtlas.load(tileset, tmxDir)`, `TiledMapView`. Alle Tileset-PNGs liegen neben den
  TMX (verifiziert: chapel 60, guild-hall 50, glassblowers 18, ruined-temple 76, bridges inkl.
  Water_animation*).
- **Warum kein Hand-Authoring:** Die Karten sind fertig designt — wir LADEN sie nur. Die Brücke wird
  nicht neu gebaut, sondern als Verbinder zwischen zwei fertigen Orten gesetzt.
- **Offen, NICHT in diesem Brief:** die Interiors der 5 Locations; NPCs/Dialoge in den neuen Orten;
  der separat dokumentierte Overlay-Rendering-Fix aus Step 9 (Seasons/Material/Frozen-Tuning) bleibt
  ein eigener Folge-Brief.
```
