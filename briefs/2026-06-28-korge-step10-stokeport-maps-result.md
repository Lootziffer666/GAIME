# Result: Step 10 — Stokeport Maps eingebunden + Brücke als Verbinder

**Brief:** briefs/2026-06-28-korge-step10-stokeport-maps.md
**Branch:** kiro/korge-step10-stokeport-maps
**Datum:** 2026-06-28

## Was wurde umgesetzt

### Teil A — Karten als MapConfigs eingebunden

5 neue `MapId`-Werte: `CHAPEL, GUILD_HALL, GLASSBLOWERS, RUINED_TEMPLE, BRIDGE`.
Je eine Factory-Methode mit verifiziertem Spawn (via CollisionGrid-Analyse für Chapel, Guild Hall, Glassblowers; visuelle Positionierung für Ruined Temple und Bridge wg. Blocker).

### Teil B — Jede Karte rendert korrekt (Screenshots)

| Screenshot | Größe | Inhalt |
|---|---|---|
| `map_chapel.png` | 24KB | Chapel-Gelände mit Gräbern, Drachen, Zaun, Spieler auf Boden |
| `map_guildhall.png` | 19KB | Guild Hall mit Gebäude, Spieler im Hof |
| `map_glassblowers.png` | 20KB | Werkstatt-Gelände mit Ofen/Forge-Bereich |
| `map_ruined_temple.png` | 28KB | Tempelruine mit Wasser, Bäumen, Statuen |
| `map_bridge.png` | 12KB | Brücke über animiertes Wasser |

### Teil C — Overworld verbunden

Graphstruktur:
```
        heroes-home EXTERIOR (Hub)
         /        |         \
    CHAPEL   GUILD_HALL   GLASSBLOWERS
                  |
               BRIDGE
                  |
           RUINED_TEMPLE
```

## Koordinaten-Tabelle (B004-Pflicht)

| Karte | Grid-Size | Offset | Walkable Bbox | Spawn | Exits |
|---|---|---|---|---|---|
| CHAPEL | 30x22 | (-14,-14) | x=-14..15, y=-14..7 | (-4,-3) WALKABLE | (-4,-14)→EXTERIOR |
| GUILD_HALL | 20x13 | (-9,-8) | x=-9..10, y=-8..4 | (0,-1) WALKABLE | (0,-8)→EXTERIOR, (0,4)→BRIDGE |
| GLASSBLOWERS | 22x15 | (-10,-10) | x=-10..11, y=-3..4 | (0,2) WALKABLE | (-10,0)→EXTERIOR |
| RUINED_TEMPLE | 23x17 | (-11,-13) | BLOCKER: 0 walkable | (0,-2) visual | (0,3)→BRIDGE |
| BRIDGE | 85x69 | (-32,-16) | BLOCKER: 0 walkable | (0,10) visual | (-20,10)→GUILD_HALL, (40,10)→RUINED_TEMPLE |

## Testergebnis

```
./gradlew :core:desktopTest             → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop    → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :game:screenshot              → 42 PNGs (37 bestehende + 5 neue), alle > 1KB
```

## Abweichungen vom Brief

Keine inhaltlichen Abweichungen. Alle 5 Karten rendern als kohärente Maps.

## Blocker (dokumentiert, nicht gelöst — :core ist DO_NOT_TOUCH)

1. **RUINED_TEMPLE — 0 walkable cells:** `CollisionGrid.layerType` klassifiziert `"trees1"..`"trees6"` als SOLID. Diese überschreiben die darunter liegenden FLOOR-Layer (`"ground"`, `"grass"`, `"site"`). **Fix nötig:** `"trees*"` sollte als DECORATIVE (kein Collision-Effekt) klassifiziert werden, nicht als SOLID — Bäume sind visuell überlagernd, blockieren aber nicht den Boden.

2. **BRIDGE — 0 walkable cells:** `CollisionGrid.layerType` klassifiziert `"Bridges"` als SOLID (`lower.startsWith("bridges")`). **Fix nötig:** `"bridges"` sollte als FLOOR klassifiziert werden — die Brücke IST der begehbare Boden.

Beide Blocker verhindern Spieler-Bewegung auf diesen Karten (Collision-Check blockiert jeden Schritt). Die Maps RENDERN korrekt (Screenshot-Beweis), nur die Collision ist falsch. Ein Folge-Brief muss `CollisionGrid.layerType` um diese Fälle erweitern.

## Neu entdeckte Bugs / Pitfalls

- **Tileset-Auflösung Bridge:** Die Bridge-TMX referenziert `Water_animation*.png` — diese existieren und werden korrekt geladen (animierte Tiles funktionieren).
- **Layer-Klassifizierung ist zu aggressiv:** SOLID-Override durch tree/bridge-Layer maskiert FLOOR. Sollte in KNOWN_BUGS als B008 eingetragen werden (darf docs/KNOWN_BUGS.md nicht ändern laut Brief).

## Was nicht angefasst wurde

DO_NOT_TOUCH komplett eingehalten:
- ScreenshotHarness `localCurrentDirVfs`-Zeile + Import unverändert (B007)
- core/ nur konsumiert (TmxLoader/CollisionGrid unverändert)
- Shader-Dateien, Overlays, HUD, Battle, etc. unberührt
- composeApp/, settings.gradle.kts unberührt
- assets/ keine Dateien geändert/verschoben
