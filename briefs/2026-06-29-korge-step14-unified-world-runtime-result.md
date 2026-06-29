# Result: Step 14 — Unified World Runtime (Spine, Pfeiler 1)

**Datum:** 2026-06-29
**Branch:** `kiro/korge-step14-unified-world-runtime`
**BASE_SHA:** `2a34040e`

## Ergebnis

**Pfeiler 1 ist implementiert.** Die drei halben Welt-Renderpfade wurden zu einem
einzigen Runtime zusammengeführt. `DoodleWorldScene` ist jetzt das vollständige
Gameplay-Runtime mit:

- Painted HD Background (crisp, kein Shader)
- Invisible CollisionGrid (TMX-basiert)
- Doodle-gefilterter Spieler (EPX + Boil, eigener Container)
- NPC-Hotspots (unsichtbare Interaktionspunkte auf dem Grid)
- Dialog (DialogOverlay, E-Taste)
- Bark-Pipeline (SliceDirector + BarkAudioPlayer)
- HUD (bildschirmfest)
- Questbook (J-Taste → QuestbookScreen)
- Kampf (SPACE → BattleScene)
- Kamera (rpg.world.Camera, clamped, follow)
- Map-Übergänge (Exit-Zellen → Taverne ↔ Wildwood)

## Dateien

### Erstellt
- `game/src/desktopMain/kotlin/game/world/ImageWorldDef.kt` — Datendefinitionen für
  die Bild-Welt (ImageMapId, ImageNpcHotspot, ImageMapExit, ImageWorldDef mit
  Companion-Factory für TAVERN_INTERIOR und SYLVANORIA_WILDWOOD)

### Modifiziert
- `game/src/desktopMain/kotlin/game/DoodleWorldScene.kt` — von reinem Render-Demo
  zum vollen Runtime ausgebaut (10-Punkte-Architektur lt. Brief)
- `game/src/desktopMain/kotlin/game/Main.kt` — Kommentar aktualisiert (Step 14,
  WorldScene retired als Boot-Pfad)
- `game/src/desktopMain/kotlin/game/ScreenshotHarness.kt` — 3 neue Captures:
  `captureUnifiedTavern()`, `captureUnifiedWildwood()`, `captureUnifiedDialog()`

## Designentscheidungen

1. **NPCs sind Hotspots, keine Sprites** — genau wie im Brief spezifiziert. Die
   gemalte Hintergrund-PNG liefert die NPC-Optik, das Grid liefert die Interaktion.
   Debug-Marker (halbtransparent orange) sind per `DEBUG_HOTSPOTS = true` aktiviert.

2. **Camera aus :core wiederverwendet** — `rpg.world.Camera.follow()` mit clamp-Logik.
   Quadratische Maps (Taverne) werden zentriert (Camera clamp → worldW < viewportW →
   centered). Breite Maps (Wildwood 86×48) scrollen horizontal.

3. **Separate Datenstruktur (`ImageWorldDef`)** statt MapConfig — MapConfig bleibt
   für die alte Tile-Welt unangetastet (DO_NOT_TOUCH). Die Bild-Welt hat eigene
   HD-Grid-Koordinaten (0-basiert, keine Offsets bei finiten Maps).

4. **Hotspot-Zellen verifiziert WALKABLE** — via TMX-Analyse bestätigt:
   - Taverne: Barkeep (25,30), Patron (40,40), Exit (35,73), Spawn (39,50)
   - Wildwood: Guard (62,4), Traveler (45,22), Exit (0,24), Spawn (40,24)

5. **pendingMap + pendingSpawn** als Companion-Vars (Pattern aus WorldScene.pendingConfig).
   Exit-Betreten setzt beides und lädt die Szene neu.

## ACCEPTANCE

```
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL ✓
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL ✓
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL ✓
./gradlew :game:screenshot                   → BUILD SUCCESSFUL ✓
  unified_tavern.png     3.1 MB  2560×1440 ✓
  unified_wildwood.png   7.1 MB  2560×1440 ✓
  unified_dialog.png     2.6 MB  2560×1440 ✓
  (alle 39 bestehenden Screenshots weiterhin erzeugt)
```

## Screenshot-Analyse

- **unified_tavern.png**: Tavern-Hintergrund zentriert (Kamera-Clamp bei quadratischer
  Map), Doodle-Spieler auf begehbarer Zelle sichtbar, HUD oben links, orange
  Hotspot-Debug-Marker auf den beiden NPC-Positionen.
- **unified_wildwood.png**: Breite Wildwood-Landschaft horizontal gescrollt (kein
  Letterbox — Bildränder zeigen Vegetation), Spieler zentriert mit Doodle-Filter.
- **unified_dialog.png**: Taverne mit aktivem DialogOverlay am unteren Bildrand,
  Barkeep-Textzeile sichtbar.

## Nicht-Änderungen (DO_NOT_TOUCH respektiert)

- WorldScene.kt, MapConfig.kt, BattleScene.kt, QuestbookScreen.kt — unberührt
- CharacterSprite.kt, HudOverlay.kt, DialogOverlay.kt, QuestbookOverlay.kt — nur konsumiert
- Alle Overlay/Shader-Dateien (Pfeiler 2) — unberührt
- core/, composeApp/, tools/, assets/, settings.gradle.kts — unberührt
- ScreenshotHarness: localCurrentDirVfs-Zeile + Import NICHT geändert (B007)

## Bekannte Einschränkungen (kein Bug, by design)

- **Kein Audio** im Unified Runtime (BGM/Bark-WAV abspielen erfordert AudioManager,
  der an WorldScene gebunden ist — Migration ist Folge-Brief)
- **QuestbookScreen close → WorldScene** (nicht DoodleWorldScene) — erfordert eine
  Änderung in QuestbookScreen.kt (DO_NOT_TOUCH). Empfehlung: im Folge-Brief
  QuestbookScreen.kt erlauben und changeTo-Ziel parametrisieren.
- **Hotspot-Platzierung ist approximativ** — pixelgenaue Ausrichtung auf die gemalten
  Figuren ist Art-Direction-Politur (Pfeiler 5).

## Nächste Schritte (nicht in diesem Brief)

1. Pfeiler 2: `BaseOverlay` + Render-Parität (sichtbare Physik im Unified Runtime)
2. QuestbookScreen-Rückweg zu DoodleWorldScene (parametrisiertes changeTo-Ziel)
3. AudioManager-Migration ins Unified Runtime
4. WorldScene + composeApp retirement (mechanischer Folge-Brief)
