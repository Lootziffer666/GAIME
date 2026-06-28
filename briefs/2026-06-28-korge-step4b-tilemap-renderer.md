# Brief: KorGE Migration Step 4b — TiledMap Renderer + Player Sprite

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step4b-tilemap-renderer`
**BASE_SHA:** `a2e37aadc5e4fbd784248fa27c5ca0620bafbac0`

---

## Aufgabe

Step 4 lieferte den `TmxLoader` + `CollisionGrid` in `:core` (reine Datenmodelle,
keine Rendering-Abhängigkeit). Step 4b verbindet diese Modelle mit KorGE-Rendering:

1. **TiledMap-Renderer** — lädt `Interior1.tmx` via KorGE `resourcesVfs`, rendert
   alle Tile-Layer pixel-exakt, inkl. Flip-Bits und animierten Tiles.
2. **PlayerSprite** — lädt den Swordsman-Sprite-Sheet (Idle + Walk), animiert ihn,
   und bewegt ihn kachelweise auf dem Grid — mit `CollisionGrid`-Kollisionsprüfung.
3. **Kamera** — folgt dem Spieler (simples Camera-Follow auf dem TiledMap-Container).

Akzeptanzkriterium bleibt **Kompilierung** (kein GL-Fenster in der Sandbox).
Visuelles Ergebnis nur per manuellem Lauf lokal.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/Main.kt
  - game/build.gradle.kts            # falls KorGE-Importe neue Sub-Artefakte brauchen

create:
  - game/src/desktopMain/kotlin/game/TilesetAtlas.kt
  - game/src/desktopMain/kotlin/game/TiledMapView.kt
  - game/src/desktopMain/kotlin/game/PlayerSprite.kt
  - game/src/desktopMain/kotlin/game/TiledMapScene.kt
```

---

## DO_NOT_TOUCH

```
- core/                                     # fertig; keine Änderungen
- composeApp/                               # Interim, throwaway
- settings.gradle.kts                       # kein neues Modul
- game/src/desktopMain/kotlin/game/Hd2dStage.kt   # bleibt als historische Referenz
- game/src/androidMain/                     # Android-Stub bleibt leer
- assets/                                   # nur lesen, nie kopieren/verschieben
```

---

## Schritt 1 — `TilesetAtlas.kt`

Lädt ein Tileset-PNG und stellt Slices bereit.

```
class TilesetAtlas(val tileset: rpg.tiled.Tileset, val bitmap: Bitmap) {
    fun sliceFor(localTileId: Int): BitmapSlice<Bitmap>
        → col = localTileId % tileset.columns
        → row = localTileId / tileset.columns
        → bitmap.slice(RectangleInt(col * tw, row * th, tw, th))

    companion object {
        suspend fun load(tileset: Tileset, dirPath: String): TilesetAtlas
            → pngPath = "$dirPath/${tileset.imageSource}"
            → bitmap = resourcesVfs[pngPath].readBitmap()
    }
}
```

Hilfsmethode auf `List<TilesetAtlas>`:
```
fun List<TilesetAtlas>.resolveGid(gid: Int): Pair<TilesetAtlas, Int>?
    → atlas = lastOrNull { it.tileset.firstGid <= gid } ?: return null
    → localId = gid - atlas.tileset.firstGid
    → Pair(atlas, localId)
```

---

## Schritt 2 — `TiledMapView.kt`

KorGE-`Container` der alle Tile-Layer einer geladenen `TiledMap` rendert.

```
class TiledMapView(map: TiledMap, atlases: List<TilesetAtlas>) : Container() {
    init {
        smoothing = false

        // Pro Layer ein Kind-Container (Reihenfolge = Zeichenreihenfolge)
        for (layer in map.layers) {
            val layerContainer = container { smoothing = false }

            for (cell in layer.cells) {
                if (cell.gid == 0) continue
                val (atlas, localId) = atlases.resolveGid(cell.gid) ?: continue
                val slice = atlas.sliceFor(localId)

                layerContainer.image(slice) {
                    x = (cell.gridX * map.tileWidth).toDouble()
                    y = (cell.gridY * map.tileHeight).toDouble()
                    // Flip-Bits (Pivot ist die Kachelmitte)
                    if (cell.flipH) { scaleX = -1.0; x += map.tileWidth }
                    if (cell.flipV) { scaleY = -1.0; y += map.tileHeight }
                    // flipD = diagonale Spiegelung = 90°-Rotation + flipH
                    if (cell.flipD) {
                        rotation = Angle.fromDegrees(90.0)
                        scaleX *= -1.0
                    }
                }

                // Animierte Tiles merken (s. Updater unten)
                …
            }
        }
    }

    // Animiertes-Tile-State: (atlas, frameFrames, Images) intern verwaltet
    // addUpdater { dt } → Timers hochzählen, bei Ablauf nächsten Frame setzen
}
```

**Flip-Bit-Korrektheit:** Tiled-Spec, Bits 31/30/29 aus GID (bereits von TmxLoader
extrahiert). `flipH` = horizontale Spiegelung (x-Achse). `flipV` = vertikale
Spiegelung (y-Achse). `flipD` = Anti-Diagonale = Rotation+Flip.

**Animationslogik:**
- Pro animiertem Tile: `frameIndex`, `elapsedMs` (Float)
- Im `addUpdater { dt }`: `elapsedMs += dt.milliseconds.toFloat()`
- Wenn `elapsedMs >= frames[frameIndex].durationMs` → nächster Frame, `elapsedMs -= duration`
- `image.bitmap = atlas.sliceFor(frames[newFrameIndex].tileId)`

---

## Schritt 3 — `PlayerSprite.kt`

Lädt Swordsman-Sprite-Sheets und rendert Idle/Walk-Animationen.

**Assets:**
```
assets/HD/characters/swordsman/PNG/Swordsman_lvl1/Without_shadow/
  Swordsman_lvl1_Idle_without_shadow.png   ← Idle-Sheet
  Swordsman_lvl1_Walk_without_shadow.png   ← Walk-Sheet
```

**Sprite-Sheet-Format** (CraftPix-Standard für diese Sheets):
- Frames horizontal angeordnet, alle gleich breit
- Kiro: PNG-Breite und Höhe einlesen (Bitmap.width / Bitmap.height),
  Framebreite = Höhe (quadratische Frames), Frameanzahl = width / height
- Beide Sheets getrennt laden

**Interface:**
```
class PlayerSprite(container: Container) {
    var gridX: Int; var gridY: Int     // Kachelposition
    var facing: Facing                 // UP/DOWN/LEFT/RIGHT

    fun setIdle()
    fun setWalking()

    // intern: addUpdater in container → Frame advance
    // intern: image.x/y = gridX * tileW, gridY * tileH (direkt, kein Tween in Step 4b)
}

enum class Facing { UP, DOWN, LEFT, RIGHT }
```

Facing-Flip: Walk/Idle-Sheets zeigen nach rechts. `LEFT` → `image.scaleX = -1.0`.
UP/DOWN: Bei Top-Down-Sprites üblicherweise separate Sheets oder Rotation.
Da nur ein Sheet vorhanden: UP/DOWN können vorerst DOWN-Frames verwenden (TODO-Kommentar).

**Startposition:** Tile (8, 12) — Mitte von `Interior1.tmx` (16×24 Kacheln).

---

## Schritt 4 — `TiledMapScene.kt`

Orchestriert den Aufbau.

```kotlin
package game

class TiledMapScene : Scene() {
    override suspend fun SContainer.sceneInit() {
        val tmxDir    = "assets/HD/locations/heroes-home/Tiled_files"
        val tmxPath   = "$tmxDir/Interior1.tmx"

        // 1. TiledMap laden + parsen
        val tmxContent = resourcesVfs[tmxPath].readString()
        val tiledMap   = TmxLoader.parse(tmxContent)
        val collision  = CollisionGrid.from(tiledMap)

        // 2. Tilesets laden
        val atlases = tiledMap.tilesets.map { TilesetAtlas.load(it, tmxDir) }

        // 3. Map rendern (Basismaßstab 1:1 — 16 px/Kachel)
        val mapView = TiledMapView(tiledMap, atlases)
        val mapScale = 3.0   // 16px × 3 = 48px-Kacheln am Desktop
        mapView.scale = mapScale
        addChild(mapView)

        // 4. Spieler-Sprite über die Map legen
        val player = PlayerSprite(this)
        player.gridX = 8; player.gridY = 12

        // 5. Keyboard-Input + Kollision
        addUpdater {
            val keys = views.input.keys
            var dx = 0; var dy = 0
            if (keys.pressing(Key.LEFT)  || keys.pressing(Key.A)) { dx = -1; player.facing = Facing.LEFT  }
            if (keys.pressing(Key.RIGHT) || keys.pressing(Key.D)) { dx =  1; player.facing = Facing.RIGHT }
            if (keys.pressing(Key.UP)    || keys.pressing(Key.W)) { dy = -1; player.facing = Facing.UP    }
            if (keys.pressing(Key.DOWN)  || keys.pressing(Key.S)) { dy =  1; player.facing = Facing.DOWN  }
            if (dx != 0 || dy != 0) {
                val nx = player.gridX + dx
                val ny = player.gridY + dy
                val cx = nx - collision.offsetX
                val cy = ny - collision.offsetY
                val cellType = collision[cx, cy]
                if (cellType == TileType.WALKABLE || cellType == TileType.TRIGGER) {
                    player.gridX = nx; player.gridY = ny
                    player.setWalking()
                }
            } else {
                player.setIdle()
            }
        }

        // 6. Kamera: Map-Container so verschieben, dass Spieler zentriert bleibt
        addUpdater {
            val px = player.gridX * tiledMap.tileWidth * mapScale
            val py = player.gridY * tiledMap.tileHeight * mapScale
            mapView.x = views.virtualWidth  / 2.0 - px
            mapView.y = views.virtualHeight / 2.0 - py
        }
    }
}
```

**Hinweis `addUpdater` und Input:** In KorGE 6.0 liefert der Lambda `dt: Duration`
(kotlin.time.Duration, wie in Step 3 dokumentiert). `keys.pressing(Key.X)` ist die
korrekte 6.0-API (kein import nötig, `keys` ist `views.input.keys`).

---

## Schritt 5 — `Main.kt` anpassen

```kotlin
package game

import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import rpg.combat.EnemyArchetype

suspend fun main() = Korge {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<TiledMapScene>()
}
```

`Hd2dStage.kt` bleibt als Datei erhalten — wird nur nicht mehr als Startszene genutzt.

---

## Schritt 6 — `game/build.gradle.kts` (nur falls nötig)

KorGE 6.0.0 (`com.soywiz.korge:korge:6.0.0`) enthält `korlibs-image`, `korlibs-io`
und alle Rendering-APIs. Kein weiteres Dependency-Update erwartet.
Falls ein `ClassNotFoundException` oder fehlender Import auftritt → bitte im Result
dokumentieren und die minimale Dependency-Ergänzung vornehmen.

---

## ACCEPTANCE

```
./gradlew :game:compileKotlinDesktop        → BUILD SUCCESSFUL (0 errors)
./gradlew :core:desktopTest                 → BUILD SUCCESSFUL (206 Tests, unverändert)
./gradlew :composeApp:compileKotlinDesktop  → BUILD SUCCESSFUL (unverändert)
```

Kein GL-Fenster, kein Runtime-Test — reine Kompilierung genügt.

Erwartetes visuelles Ergebnis (manuelle lokale Ausführung, nicht Teil der CI-Akzeptanz):
- Interior1.tmx wird auf Desktop-Fenster angezeigt, 48 px/Kachel (3× Scale)
- Animierte Türen/Fenster animieren (6-Frame-Cycles)
- Swordsman-Sprite ist sichtbar, Idle-Animation läuft
- Arrow-Keys / WASD: Swordsman bewegt sich, wird durch Wände (BLOCKED) gestoppt
- Kamera folgt dem Spieler

---

## Doku-Pflicht nach Abschluss

- `docs/KORGE_MIGRATION_PLAN.md` → Step 4b als ✅ markieren
- `docs/KNOWN_BUGS.md` → alle neu entdeckten KorGE-6.0-Pitfalls eintragen
- `briefs/2026-06-28-korge-step4b-tilemap-renderer-result.md` → Result-Report

---

## Kontext

- Vorarbeit Step 4: `core/src/commonMain/kotlin/rpg/tiled/` — `TiledMap`, `TmxLoader`,
  `CollisionGrid`, `TileType` (alle public, keine Änderung nötig)
- KorGE-6.0-Importe die in Step 3 geprüft wurden: `korlibs.korge.scene.*`,
  `korlibs.korge.view.*`, `korlibs.image.bitmap.*`, `korlibs.io.file.std.resourcesVfs`
- `resourcesVfs` auf JVM Desktop löst Pfade relativ zum Arbeitsverzeichnis auf —
  wenn das Spiel via `./gradlew :game:runJvm` (oder äquivalent) gestartet wird,
  ist das Arbeitsverzeichnis das Projektroot. Pfade `"assets/HD/..."` funktionieren
  direkt.
- Pixel-Sampling: `smoothing = false` auf jedem Container und Image (wie in `Hd2dStage`).
- Donor-Policy: keine externe Bibliothek für Sprite-Animation — KorGE's `addUpdater`
  ist alles was gebraucht wird.
- `game/build.gradle.kts` ist in SCOPE.modify, falls nötig — nicht mehr DO_NOT_TOUCH
  (Lehre aus Step 3, dokumentiert in `docs/KORGE_MIGRATION_PLAN.md §3/Step3`).
