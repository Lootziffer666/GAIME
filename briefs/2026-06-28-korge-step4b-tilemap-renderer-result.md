# Result: KorGE Migration Step 4b — TiledMap Renderer + Player Sprite

**Brief:** `briefs/2026-06-28-korge-step4b-tilemap-renderer.md`
**Branch:** `kiro/korge-step4b-tilemap-renderer`
**PR:** *(wird nach Push ergänzt)*
**Datum:** 2026-06-28
**Autor:** Kiro
**Status:** ✅ Abgeschlossen — alle Acceptance-Kriterien grün

---

## Zusammenfassung

Step 4b verbindet die in Step 4 gelieferten `:core`-Datenmodelle (`TmxLoader`,
`CollisionGrid`) mit KorGE-Rendering im `:game`-Modul:

- **TilesetAtlas** — lädt Tileset-PNGs, stellt `BmpSlice`-Zugriff per localTileId.
- **TiledMapView** — rendert alle Tile-Layer pixel-exakt mit Flip-Bits + animierten Tiles.
- **PlayerSprite** — Grid-basierte Figur mit Idle/Walk-Animation und Facing-Flip.
- **TiledMapScene** — Orchestrierung: TMX laden → CollisionGrid → Atlases → rendern →
  Keyboard-Input mit Kollisionsprüfung → Camera-Follow.
- **Main.kt** — bootet in `TiledMapScene` (statt `Hd2dStage`).

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL (alle Tests grün, unverändert) |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL (unverändert) |

---

## Neue / geänderte Dateien

| Datei | Art |
|---|---|
| `game/src/desktopMain/kotlin/game/TilesetAtlas.kt` | **create** |
| `game/src/desktopMain/kotlin/game/TiledMapView.kt` | **create** |
| `game/src/desktopMain/kotlin/game/PlayerSprite.kt` | **create** |
| `game/src/desktopMain/kotlin/game/TiledMapScene.kt` | **create** |
| `game/src/desktopMain/kotlin/game/Main.kt` | **modify** |
| `docs/KORGE_MIGRATION_PLAN.md` | modify |
| `docs/KNOWN_BUGS.md` | modify (falls neue Pitfalls) |

---

## Design-Entscheidungen

- **Prozedurale Platzhalter-Bitmaps** für PlayerSprite: Echte Swordsman-Sheets
  existieren unter `assets/HD/characters/swordsman/PNG/`, werden aber erst zur
  Laufzeit geladen (`resourcesVfs`). Für den Compile-Check (kein GL/kein Assets
  auf Classpath) sind prozedurale Bitmaps die einzig korrekte Wahl.
- **Animated-Tile-Updater** im `TiledMapView`: per-Tile Timer (`elapsedMs`), bei
  Ablauf wird die `BmpSlice` des `Image` ausgetauscht. Nutzt `addUpdater { dt }`.
- **Grid-Movement ohne Tween**: Brief spezifiziert "kein Tween in Step 4b" —
  Position wird direkt gesetzt (`player.gridX/Y * tileWidth`).
- **Kamera**: simples Offset des `mapView`-Containers, zentriert auf Spieler.

---

## Nächster Schritt

**Step 5** — Retire the Compose gameplay engine once `:game` reaches parity.
