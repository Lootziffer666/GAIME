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
