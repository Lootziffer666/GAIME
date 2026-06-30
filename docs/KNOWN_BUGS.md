# Known Bugs & Pitfalls

Inkrementell geführt von Claude Code und Kiro. Pflichtlektüre vor jedem Auftrag.
Einträge nie löschen — nur als FIXED markieren, damit der Fehler nicht wiederholt wird.

Format: `[ID] Kurzbeschreibung — gefunden von, Datum, betroffene Datei(en), Status`

---

## Offen

- **B004 — Infinite-TMX-Maps haben NEGATIVE Chunk-Offsets; deklarierte `width`/`height` sind irreführend.**
  Gefunden: Claude, 2026-06-28. Betrifft: `MapConfig.kt`, jede `:game`-Szene die Tile-Koordinaten
  hartkodiert. Status: für heroes-home Interior1/Exterior **behoben** (echte Koordinaten verifiziert),
  als Klasse aber offen für alle künftigen Maps.
  Interior1.tmx deklariert `width="16" height="24"`, aber als `infinite="1"`-Map liegen die echten
  Tile-Daten bei `tileX -17..8 / tileY -10..11` (begehbarer Raum nur `tileX -7..2 / tileY -2..4`).
  Step 5a's Spawn `(8,12)` und alle Step-5b-Koordinaten aus dem Brief waren BLOCKED/out-of-bounds —
  der Spieler stand im Nichts. Compile-only-Acceptance fängt das NICHT.
  **Regel:** Vor dem Hartkodieren von Tile-Koordinaten für eine Map immer `CollisionGrid.from(map)`
  laden und das Grid dumpen (offsetX/Y + WALKABLE-Bereich), Koordinaten gegen `grid[x-offX, y-offY]`
  prüfen. `offsetX/Y` sind bei Infinite-Maps fast immer negativ.

- **B005 — CraftPix-Charakter-Sheets sind 64×64-RASTER, kein height-großer Einzelstreifen.**
  Gefunden: Claude, 2026-06-28 (per Offscreen-Screenshot). Betrifft: `SpriteLoader.kt`. **Behoben.**
  Der Step-5a-Brief behauptete „alle Frames quadratisch, Framebreite = Sheet-Höhe". Falsch: das
  Swordsman-Idle-Sheet ist 768×256 = **12×4-Raster aus 64×64-Frames** (4 Reihen = Richtungen,
  Spalten = Animationsframes). Die alte `sliceFrames` (frameSize = 256) schnitt 3 Riesen-Slices à
  256², jeder mit mehreren Charakteren → ein Sprite rendert als Duplikat-Reihe. Fix: `sliceFrames`
  nimmt `frameSize = 64` und liefert Reihe 0 (Front-Animation). Visuell per `:game:screenshot`
  verifiziert (interior/exterior/battle). **Enhancement erledigt (Step 6b):** `SpriteLoader.sliceAllRows()`/
  `loadAllRows()` schneiden alle 4 Reihen; `CharacterSprite` wählt die Reihe per `Facing.rowIndex()`
  (DOWN=0, LEFT=1, UP=2, RIGHT=3), scaleX-Flip nur noch als Single-Row-Fallback.

- **B009 — In den Battle-Captures rendert nur EIN Sprite (Vampir), der Hero-Swordsman fehlt.**
  Gefunden: Claude, 2026-06-28 (per Offscreen-Screenshot, Step-6b-Integration). Betrifft:
  `BattleScene.kt` / `ScreenshotHarness.kt` (`captureBattle*`). Status: **offen**, kein Blocker.
  Sowohl die bestehende `battle.png` (vor Step 6b) als auch die neuen `battle_midway.png`/
  `battle_victory.png` zeigen nur den Vampir mittig — der Hero bei `gridX=2,gridY=3` ist unsichtbar.
  HP-Bars + Labels stimmen, also ist es kein Logik-, sondern ein reines Render-/Positionierungsproblem
  (vermutlich landet der Hero bei 48px-Tiles außerhalb des sichtbaren Bereichs oder hinter dem
  Hintergrund-`solidRect`). **Vorbestehend** — keine Regression aus PR#40. Zu untersuchen wenn die
  BattleScene das nächste Mal angefasst wird (Step 7b Combat-Tiefe).

- **B006 — `resourcesVfs` löst über den Classpath auf, nicht über das Dateisystem.**
  Gefunden: Claude, 2026-06-28. Betrifft: jeden Asset-Load (`resourcesVfs["assets/..."]`).
  `assets/` liegt im Repo-Root, NICHT im Classpath → zur Laufzeit `InvalidOperationException:
  Can't find 'assets/...'`. So wie der Code steht, lädt das echte Spiel **keine** Assets.
  Compile-only-Acceptance fängt das NICHT (erneut: grün ≠ läuft). **Behoben für die Run-/Screenshot-
  Tasks:** `game/build.gradle.kts` legt via `desktopRuntime()` das Repo-Root auf den Classpath, dann
  wird `assets/...` als Classpath-Ressource gefunden. Jeder neue Launcher muss das ebenso tun.

- **B007 — Kiro reverts B006-Fix (`localCurrentDirVfs`) bei jedem neuen Branch.**
  Gefunden: Claude, 2026-06-28. Betrifft: `game/src/desktopMain/kotlin/game/ScreenshotHarness.kt`.
  Drittes Mal in Folge: Kiro ändert `localCurrentDirVfs["build/screenshots"]` zurück zu
  `localVfs("build/screenshots")` — das schreibt nach `/build/screenshots` statt ins CWD.
  **Fix (jedes Mal):** `import korlibs.io.file.std.localCurrentDirVfs` + `localCurrentDirVfs["build/screenshots"]`.
  **Regel für Briefs:** `ScreenshotHarness.kt` in `DO_NOT_TOUCH` aufnehmen. Alternativ:
  Kommentar direkt über der Zeile lassen (war bei Step 5-Integration vorhanden, Kiro hat ihn gelöscht).

- **B008 — Kiro reverts Step-5-Architektur wenn der Branch-Base-Commit nicht stimmt.**
  Gefunden: Claude, 2026-06-28. Betrifft: `composeApp/`, `core/`, Briefs.
  PR#38 (step6-v2) versuchte, SliceDirector/AudioPlayer/BarkAudioPlayer zurück nach composeApp
  zu verschieben, alle gelöschten Compose-Gameplay-Dateien neu anzulegen und App.kt mit Mode-Enum
  zurückzubauen. Ursache: Kiro arbeitete intern von einem Commit vor Step-5-Integration.
  **Neutralisiert:** Git's 3-Way-Merge (ort) hat alle Regressions automatisch ignoriert, da main
  die Dateien korrekt positioniert hatte.
  **Regel für Briefs:** `BASE_SHA` immer setzen, step5-Ergebnis explizit in SCOPE.DO_NOT_TOUCH
  erwähnen. Kiro muss `git log --oneline -3` am Anfang jedes Auftrags ausführen.

---

## Behoben

| ID | Bug | Gefunden | Datei(en) | Behoben in |
|---|---|---|---|---|
| B001 | Kiro implementierte `rpg.items.*` mit inkompatiblem API (`displayName`, `attackBonus`, `PurchaseResult`, `Inventory(party, gold)`) — kollidierte mit bereits verdrahteter `SliceScreen.kt`-ShopView | Claude, 2026-06-28 | `rpg/items/Item.kt`, `Inventory.kt`, `ItemCatalog.kt` | Integration-Branch `claude/integration` (Merge PR#29) |
| B002 | Kiro öffnete PNG-Assets als Base64-Dateiinhalt → Kontext-Flood, Thread unbrauchbar | User, 2026-06-28 | `assets/HD/ui/fantasy-icons/PNG/Gui_icons2.png` | Protokoll-Regel (handoff-protocol.md) |
| B003 | `gruff/raspy/whiny .7z.part*` lagen im Repo-Root und wurden versehentlich getrackt | Claude, 2026-06-28 | Repo-Root | Commit 4ccfa781 |

---

## Pitfalls (kein Bug, aber Falle)

- **Kiro briefen während Branches offen sind**: führt zu 3-Way-Divergenz. Immer erst `git fetch --all --prune` + Branch-Audit, dann brief.
- **`settings.gradle.kts` anfassen ohne Auftrag**: Kiro hat dieses File mehrfach unaufgefordert geändert. Immer explizit in `DO_NOT_TOUCH` setzen wenn ein neues Modul im Auftrag ist.
- **Compose-UI-Features investieren**: `composeApp/` ist Throwaway (KorGE-Migration). Keinen Aufwand in SliceScreen/DialogueLine/BarkAudioPlayer stecken.
- **Step-6-Fehllieferung**: 6b (Richtungs-Sprites), 6c (Bark-Pipeline), 6d (Scripted Playthrough) aus PR#38 wurden nicht implementiert, trotz Claim im Result-Brief. **Erledigt:** PR#40 (Step 6b/c/d) hat alle drei sauber nachgeliefert — und erstmals KEIN B007-Revert. Das explizite, dateigenaue Brief + `localCurrentDirVfs`-Zeile in `DO_NOT_TOUCH` hat funktioniert; dieses Muster für künftige Briefs beibehalten.

---

## KorGE 5.x → 6.0 Migrationsnotizen (Step 3, Kiro 2026-06-28)

Beim Portieren von `demos/korge-hd2d/Hd2dStage.kt` nach `:game` gefundene
API-/Build-Fallstricke. Verifiziert gegen die `korge-6.0.0-sources.jar` (KorGE ist
die einzige erlaubte Code-Dependency; nur als Referenz gelesen). Quellbasiert, nicht
aus Doku — die online-Doku war teils veraltet.

| Thema | 5.x-Demo | 6.0-Realität | Fix |
|---|---|---|---|
| **JVM-Target (Build)** | `:game` = `JVM_17` | KorGE-6.0.0-JVM-Artefakte sind **Bytecode-Major 65 = Java 21** (948/948 Klassen in `korge-core-jvm`; Inline-Builder in `korge-jvm`). Inlinen in JVM-17-Ziel scheitert. | `:game` auf **`JVM_21`** (desktop-only, kein Android → `:core`/`:composeApp` bleiben 17). Tritt erst bei der **ersten inline-KorGE-Funktion** auf; `Korge(...)` selbst ist nicht inline (deshalb kompilierte Step 2 bei 17). |
| **`blendMode` / `alpha`** | `import korlibs.korge.view.blendMode` / `.alpha` | sind **Member-`var` auf `View`**, keine Top-Level-Symbole | Imports entfernen; als Property nutzen (`blendMode = BlendMode.ADD`, `alpha = 0.5`). |
| **`addUpdater`-Lambda** | `addUpdater { dt: TimeSpan -> }` + `import korlibs.time.TimeSpan` | Lambda erhält **`kotlin.time.Duration`** | `addUpdater { dt -> }` (Typ inferieren), `dt.seconds` via `import korlibs.time.seconds`. TimeSpan-Import raus. |
| **`container()`** | nutzt `container()` ohne Import | Builder muss importiert werden | `import korlibs.korge.view.container` ergänzen. |
| **`BlurFilter(radius = …)`** | `BlurFilter(radius = 6.0)` | online-Doku zeigte `initialRadius` (veraltet); 6.0.0-ctor heißt wirklich `radius` | **unverändert korrekt** — nicht auf veraltete Doku hereinfallen, Quelle prüfen. |
| **`BlendMode`** | `import korlibs.korge.view.BlendMode` | ist `typealias` → `korlibs.korge.blend.BlendMode`; `BlendMode.ADD` existiert | unverändert korrekt. |
| **`filter` / `keys` / `Scene` / `sceneContainer` / `changeTo`** | div. | Pfade in 6.0 stabil (`korlibs.korge.view.filter.filter`, `korlibs.korge.input.keys`, `korlibs.korge.scene.*`) | unverändert korrekt. |

- **Doku-vs-Build-Diskrepanz (notiert, nicht „gefixt"):** `KORGE_MIGRATION_PLAN.md`
  nannte die Koordinate `com.soywiz.korlibs.korge:korge:6.0.0`; die echte (in
  `game/build.gradle.kts` verwendete und auf Maven Central existierende) ist
  `com.soywiz.korge:korge:6.0.0`. Plan-Text in Step 2 entsprechend korrigiert.
- **Binär-Assets nie als Inhalt lesen** (siehe B002): Die KorGE-API-Recherche lief
  ausschließlich über Text (`-sources.jar` entpackt + grep, Bytecode-Major über
  8-Byte-Header), kein PNG/WAV wurde eingelesen.

---

## TMX-Parser-Fallstricke (Step 4, Kiro 2026-06-28)

Beim Implementieren des `TmxLoader.kt` in `:core` gefundene Fallstricke, die für
zukünftige Parser-Erweiterungen (z. B. Object-Layers, Properties) relevant sind.

| Fallstrick | Erklärung | Lösung |
|---|---|---|
| **Inline-CSV auf derselben Zeile wie `<data>`/`<chunk>`** | Tiled schreibt oft `<data encoding="csv">1,2,3,4</data>` auf eine Zeile. Ein naiver „nächste-Zeile-ist-CSV"-Ansatz verpasst das. | `substringAfter(">")` nach dem Tag-Match; prüfe `</data>`/`</chunk>` auf derselben Zeile. |
| **GID > Int.MAX_VALUE durch Flip-Bits** | Flip-Bits setzen die oberen 3 Bits eines unsigned 32-bit Int. Kotlin's `String.toInt()` wirft `NumberFormatException` für Werte > 2³¹-1. | `String.toLong()` + `and 0xFFFFFFFFL` → dann `.toInt()` für die Bit-Operationen. |
| **`</chunk>` auf derselben Zeile wie letzter CSV-Block** | Z. B. `7,8</chunk>`. Wenn der Parser nur `startsWith("</chunk>")` prüft, geht der CSV-Rest verloren. | `line.contains("</chunk>")` + `substringBefore(…)` für den CSV-Anteil vor dem Close-Tag. |
| **Layer-Width bei finite Maps nötig** | Finite Maps (`infinite="0"`) haben kein `<chunk>`; der CSV-Block hängt direkt in `<data>`. Die Zeilenpositionen leiten sich vom `width`-Attribut des `<layer>` ab. | `<layer width="...">` mitlesen + als `currentLayerWidth` an `parseCsvCells` übergeben. |
| **Negative Chunk-Koordinaten (infinite Maps)** | `<chunk x="-16" y="-16" ...>` ist normal. Der Kollisionsraster muss erst die Bounding-Box aller Zellen berechnen und dann ins 0-basierte Grid normalisieren. | `CollisionGrid.offsetX/Y` speichert das Offset; Grid-Zugriff über normalisierte Indizes. |
| **Kein Android SDK in der Sandbox** | `:game:compileDebugKotlinAndroid` kann in der Kiro-Sandbox nicht verifiziert werden (identisch mit `:composeApp` Android-Target). Build-Config ist korrekt; lokaler Build/CI mit SDK bestätigt Funktionstüchtigkeit. | Result-Report dokumentiert es als Sandbox-Limitation, nicht als Bug. |

---

## KorGE-6.0-Pitfalls: Scenes + Audio (Step 5a, Kiro 2026-06-28)

| Pitfall | Erklärung | Fix |
|---|---|---|
| **`changeTo<>()` ist suspend** | `addUpdater {}`-Lambda ist *nicht* suspend (es wird synchron im Frame-Loop aufgerufen). `sceneContainer.changeTo<>()` direkt aufrufen ergibt „Suspension functions can only be called within coroutine body". | `launch { sceneContainer.changeTo<T>() }` — `Scene` erbt `CoroutineScope`, daher ist `launch` verfügbar. |
| **`sceneContainer` (Property) vs. `sceneContainer()` (Factory)** | Innerhalb einer `Scene` ist `sceneContainer` eine Property, die den aktuell aktiven Container liefert. `sceneContainer()` (mit Klammern) ist der *inline Builder* der einen neuen anlegt — darf in einer bestehenden Scene **nicht** aufgerufen werden. | Property ohne Klammern verwenden: `sceneContainer.changeTo<>()`. |
| **KorGE Audio hat kein Backend in headless/CI** | `readMusic()`/`readSound()` werfen zur Laufzeit Exceptions wenn kein Audio-Output existiert (Sandbox, CI-Container). Compile gelingt, Runtime crasht. | try/catch in `AudioManager` mit graceful no-op — Audio ist optional. |
| **`SpriteLoader.buildFallbackBitmap()` Visibility** | `CharacterSprite` muss den Fallback-Bitmap-Builder aufrufen; als `private` deklariert war er nicht erreichbar. | `internal` Visibility auf der Methode. |
| **`justPressed` vs. `pressing` für Einmal-Aktionen** | `pressing(Key.X)` feuert **jeden Frame** solange die Taste gehalten wird — für Angriffs-Befehle fatal (5+ Angriffe pro Tastendruck). | `justPressed(Key.X)` — true nur im ersten Frame nach dem Key-Down-Event. |

---

## KorGE-6.0-Pitfalls: World Layer (Step 5b, Kiro 2026-06-28)

| Pitfall | Erklärung | Fix |
|---|---|---|
| **Enum Redeclaration** | `Facing` war sowohl in `PlayerSprite.kt` (DO_NOT_TOUCH) als auch in `CharacterSprite.kt` deklariert → Compiler-Fehler. | `Facing`-Deklaration nur einmal behalten (in `PlayerSprite.kt`); `CharacterSprite.kt` nutzt sie aus demselben Package. |
| **SolidRect ist kein Container** | `solidRect().addChild(text(...))` kompiliert nicht — `SolidRect` ist ein Leaf-View. | Alle Overlay-Views als direkte Kinder des `parent`-Containers hinzufügen, `.visible` einzeln steuern. |
| **Text-Wrapping** | `text().width = 200.0` begrenzt den Render-Bereich nicht automatisch. Lange Zeilen überlaufen. | Manuelles `\n` in `DialogLine.text` (max ~60 Zeichen/Zeile). Word-Wrap → späteres Feature. |
| **`moveProgress`-Tick vor Animation-Check** | Wenn `advanceAnimation()` mit `val frames = ... ?: return` beginnt und keine Animation geladen ist, friert `moveProgress` ein → Sprite bleibt mitten im Schritt stehen. | Move-Tick **vor** dem `frames`-early-return platzieren. |
| **Property-Setter in `startMove()`** | `gridX = toGridX` ruft `updatePosition()` auf, das `visualGridX` liest. Bei `moveProgress=0` ergibt `visualGridX = fromGridX` (korrekt). `moveProgress` muss vor `gridX/Y` auf 0 gesetzt werden. | `moveProgress = 0f` zuerst, dann `gridX/Y` setzen. |
| **`companion object var pendingConfig`** | Scene-Wechsel in KorGE ist async (suspend). Parameter-Übergabe an die neue Scene geht nicht über Konstruktor-Argumente. Workaround: `companion object var` das vor `changeTo` gesetzt wird. | Akzeptabel für aktuelle Architektur; spätere Alternative: DI/Inject-System. |

---

## Overlay-Render + AI-Collision (Step 16, Claude-Integration 2026-06-29)

| Pitfall | Erklärung | Fix |
|---|---|---|
| **Alpha-Floor nur für Sparse-Overlays** | Der `110+v*140`-Alpha-Floor ist für *wenige* Zellen gedacht (Pfützen/Blut/Spuren). Auf Vollflächen-Systeme (Schnee auf jeder Zelle, Frühling bei hoher Dichte) angewendet → opake Decke, die das gemalte Bild **weiß ausbleicht**. | Vollflächen-Overlays: KEIN Floor, niedrig gedeckelt (Schnee `v*130`, Cap 150) + sparsame Dichte (`initFlowers(0.25)` statt `0.8`). |
| **Overlays übermalen das gemalte Bild** | Ground-Overlays zeichneten über *alle* Zellen, auch über gemalte Wände/Wasser/Bäume/Dächer → „Bild = Haut" verletzt. | `GridOverlay.mask` (Floor-Maske auf WALKABLE-Zellen) — Wrapper exponieren `var floorMask`, aus der `CollisionGrid` gebaut. |
| **AI-Maps sind kollisions-zu-permissiv** | `sylvanoria_wildwood.tmx` (mapbuilder/HSV) markiert fast alles WALKABLE — Bäume/Wasser/Gebäude unter-erkannt. Floor-Maske greift dort kaum → Effekte können trotzdem über „begehbares" Wasser/Bäume liegen. | Kurzfristig: Dichte+Alpha senken. Mittelfristig: mapbuilder-Segmentierung härten (Pfeiler-3-Thema) oder TMX manuell nachschärfen. |
| **Wetter im Innenraum** | Schnee/Regen/Jahreszeit in der Taverne (interior) ist Unsinn. | Wetter-Szenen auf den Exterior (Wildwood); Innenräume bekommen strukturelle Effekte (Material-Ermüdung), kein Wetter. |
