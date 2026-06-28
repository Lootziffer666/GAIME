# Known Bugs & Pitfalls

Inkrementell geführt von Claude Code und Kiro. Pflichtlektüre vor jedem Auftrag.
Einträge nie löschen — nur als FIXED markieren, damit der Fehler nicht wiederholt wird.

Format: `[ID] Kurzbeschreibung — gefunden von, Datum, betroffene Datei(en), Status`

---

## Offen

*(noch keine Einträge)*

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
