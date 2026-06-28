# Brief: KorGE Migration Step 3b + Step 4 — Android-Target & Tiled-Loader

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step4-tiled-android`
**BASE_SHA:** `dbf238bec3d7ac2cd5508546e4d2aecc5dfbdc8e`

---

## Aufgabe

Zwei zusammenhängende Deliverables in einem Branch:

**Step 3b** — `:game` erhält neben dem bestehenden `jvm("desktop")`-Target ein
`androidTarget()`, sodass Android und Desktop dieselbe KorGE-Szene bauen. Scope:
ausschließlich `game/build.gradle.kts` + ein minimales `androidMain`-Stub.

**Step 4** — Eigener Tiled-Tilemap-Loader mit tile-abgeleiteter Kollision in `:core`.
Die 17 `.tmx`-Karten in `assets/HD/locations/` haben bereits eine Profi-Komposition
mit echten Layernamen. Der Loader liest diese Struktur, modelliert sie in Kotlin
und erzeugt daraus ein `CollisionGrid`. Renderer-agnostisch, unit-getestet in `:core`.
Kein neuer Build-Dep (nur Kotlin-Stdlib).

Akzeptanzkriterium für beide Steps: **ausschließlich Kompilierung + Tests grün.**
Kein GL-Fenster, kein laufendes Gerät.

---

## SCOPE

```
modify:
  - game/build.gradle.kts                          # Step 3b: Android-Target ergänzen

create:
  - game/src/androidMain/kotlin/game/.gitkeep      # Step 3b: androidMain-Stub (leeres Marker-File)
  - core/src/commonMain/kotlin/rpg/tiled/*         # Step 4: TiledMap-Modell + Loader + Collision
```

Testdateien in `core/src/commonTest/kotlin/rpg/tiled/` sind implizit erlaubt
(Protokoll-Regel: Testdateien für SCOPE-Code sind immer erlaubt).

---

## DO_NOT_TOUCH

```
- core/                              # außer dem neuen rpg/tiled/-Package
- composeApp/                        # Throwaway, nicht anfassen
- settings.gradle.kts               # :game ist bereits registriert
- game/src/desktopMain/             # Hd2dStage.kt + Main.kt bleiben unverändert
- demos/                            # historische Referenz
- assets/                           # .tmx-Dateien nur lesen, nie modifizieren
```

---

## Schritt 0 — Recherche vor dem Coden

### 0a. Donor-Policy (LESEN, bevor GitHub durchsucht wird)
Aus `docs/KORGE_MIGRATION_PLAN.md §1.3`: Externe Repos sind **ausschließlich
Referenz**. Kein Code übernehmen. Aus beobachtetem Verhalten und der
TMX-Spec neu implementieren.

### 0b. TMX-Struktur verstehen

Das Projekt enthält bereits `scripts/tmx_render.py` — eine funktionierende
Python-Referenzimplementation desselben TMX-Subsets, das hier geparst werden soll.
**Diese Datei vollständig lesen**, bevor irgendetwas implementiert wird. Sie zeigt:

- Wie `infinite`-Maps mit `<chunk x y width>` in `<data encoding="csv">` aufgebaut sind
- Wie `firstgid`/`columns`/`tilecount`/`tilewidth`/`tileheight` aus `<tileset>` gelesen werden
- Die drei GID-Flip-Bits (`FLIP_H = 0x80000000`, `FLIP_V = 0x40000000`, `FLIP_D = 0x20000000`)
  und die GID-Maske (`GID_MASK = 0x1FFFFFFF`)
- Wie `<animation><frame tileid="..." duration="..."/>` in Tilesets eingebettet ist
- Das Floor-basierte Kollisionsmodell: Zellen ohne Floor-Tile sind BLOCKED;
  Solid-Layer addieren weitere BLOCKED-Zellen

### 0c. Echte Layer-Namen kennen

Alle 16 relevanten `.tmx`-Dateien befinden sich unter `assets/HD/locations/`:

```
bridges/PNG_n_Tiled/Bridges.tmx
chapel/Tiled_files/Exterior.tmx
chapel/Tiled_files/Interior.tmx
glassblowers-workshop/Tiled_files/Exterior.tmx
glassblowers-workshop/Tiled_files/Interior.tmx
guild-hall/Tiled_files/Exterior.tmx
guild-hall/Tiled_files/Interior_1st_floor.tmx
guild-hall/Tiled_files/Interior_2nd_floor.tmx
heroes-home/Tiled_files/Exterior.tmx
heroes-home/Tiled_files/Interior1.tmx
ruined-temple/Tiled_files/Ruined_temple_exterior.tmx
ruined-temple/Tiled_files/Ruined_temple_interior.tmx
```

(Characters.tmx-Dateien enthalten nur NPC-Sprites — kein Terrain, kein Blocker.)

Die vollständige Menge der tatsächlichen Layer-Namen aus diesen Dateien ist im
Abschnitt „Kollisionsklassifikation" unten aufgeführt. **Diese Liste ist aus den
realen Dateien abgeleitet** — keine Erfindungen.

### 0d. KorGE-Android-Abhängigkeit prüfen

`com.soywiz.korge:korge:6.0.0` ist ein KMP-Artefakt. Prüfen ob es Android-
Artefakte mitliefert (auf Maven Central oder in der lokalen Gradle-Cache unter
`~/.gradle/caches/`). Wenn nein: Blockernotiz im Result-Report, kein Absturz.

---

## Teil A — Step 3b: Android-Target für `:game`

### A.1 `game/build.gradle.kts` anpassen

Aktuelle Datei hat nur `jvm("desktop")`. Erweitern um `androidTarget()`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")          // neu — für androidTarget()
}

android {
    namespace = "game"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)   // Android → Dex, kein JVM_21-Zwang
        }
    }

    jvm("desktop") {
        compilerOptions {
            // KorGE 6.0.0 JVM-Artefakte sind Bytecode-Major 65 (Java 21).
            // Inline-Builder (container, image, sceneContainer, changeTo) lassen
            // sich nicht in ein JVM-17-Ziel inlinen. Desktop-only → kein Android-Impact.
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation("com.soywiz.korge:korge:6.0.0")
            }
        }
        // desktopMain: Main.kt + Hd2dStage.kt bleiben genau dort
        val desktopMain by getting { /* keine eigenen Deps nötig */ }
        val androidMain by getting { /* KorGE aus commonMain geerbt */ }
    }
}
```

**Wichtig:** KorGE aus `desktopMain.dependencies` nach `commonMain.dependencies`
verschieben — so bekommt androidMain KorGE ohne eigene Deklaration.

`id("com.android.library")` ist in `build.gradle.kts` (root) bereits als
`"8.2.2"` deklariert (`apply false`) — kein neuer Eintrag nötig.

### A.2 `game/src/androidMain/kotlin/game/.gitkeep`

Leere Datei anlegen damit das `androidMain`-Source-Set ein existierendes Verzeichnis
hat. Git trackt leere Verzeichnisse nicht — Marker-File nötig.

Keine Kotlin-Klassen in `androidMain` für Step 3b — das Android-Entry-Point
(KorGE-Activity) kommt in Step 4b.

### A.3 Akzeptanz Step 3b

```
./gradlew :game:compileKotlinAndroid   → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop   → BUILD SUCCESSFUL (unverändert)
```

Falls `com.soywiz.korge:korge:6.0.0` kein Android-Artefakt hat → Blocker im
Result-Report, `androidMain`-Dependency auf KorGE auskommentieren, Kompilierung
ohne KorGE-API in `androidMain` als Zwischenlösung akzeptieren.

---

## Teil B — Step 4: Tiled-Tilemap-Loader in `:core`

### B.1 Package-Struktur

Alle neuen Dateien in `core/src/commonMain/kotlin/rpg/tiled/`:

```
rpg/tiled/
  TiledMap.kt          # Datenmodell (reine data classes / enums)
  TmxLoader.kt         # XML-Parser (Kotlin-Stdlib, kein Dep)
  CollisionGrid.kt     # Tile-abgeleitetes Kollisionsraster
```

**Kein neuer Build-Dependency.** Der Parser nutzt ausschließlich
`kotlin.text.*` (Regex, String-Operationen). Keine externe XML-Bibliothek.
Begründung: `:core` ist framework-frei, KMP-kompatibel, und das TMX-Subset
ist regulär genug für einen hand-geschriebenen Parser.

### B.2 Datenmodell (`TiledMap.kt`)

```kotlin
package rpg.tiled

data class TiledMap(
    val tileWidth: Int,
    val tileHeight: Int,
    val tilesets: List<Tileset>,
    val layers: List<TileLayer>,        // Dokumentreihenfolge
)

data class Tileset(
    val firstGid: Int,
    val name: String,
    val tileWidth: Int,
    val tileHeight: Int,
    val columns: Int,
    val tileCount: Int,
    val imageSource: String,            // relativer Pfad aus <image source="...">
    val animatedTiles: Map<Int, List<AnimationFrame>>,  // localId → Frames
)

data class AnimationFrame(
    val tileId: Int,        // local tile id (0-based innerhalb des Tilesets)
    val durationMs: Int,
)

data class TileLayer(
    val name: String,
    val cells: List<TileCell>,
)

data class TileCell(
    val gridX: Int,
    val gridY: Int,
    val gid: Int,           // bereinigt (Flip-Bits entfernt, 1-basiert)
    val flipH: Boolean,
    val flipV: Boolean,
    val flipD: Boolean,
)
```

GID-Konstanten als `companion object` oder `private const` im `TmxLoader`:
```kotlin
const val FLIP_H   = 0x80000000.toInt()
const val FLIP_V   = 0x40000000.toInt()
const val FLIP_D   = 0x20000000.toInt()
const val GID_MASK = 0x1FFFFFFF
```

### B.3 TMX-Parser (`TmxLoader.kt`)

**Parser-Ansatz:** Minimaler State-Machine-Parser auf `String`-Basis.
Keine XML-Bibliothek — das TMX-Subset (Tiled 1.10, `encoding="csv"`,
`infinite="1"`) ist regulär genug.

**Öffentliche API:**

```kotlin
object TmxLoader {
    /** Parst einen TMX-String (Dateiinhalt als String übergeben, nicht Pfad). */
    fun parse(tmxContent: String): TiledMap
}
```

Kein Pfad-Parameter — der Aufrufer liest die Datei und übergibt den Inhalt
als String. So bleibt `:core` filesystem-unabhängig (Filesystem-Zugriff ist
Sache von `:game`).

**Was geparst werden muss:**

1. `<map tilewidth="..." tileheight="...">` — Tile-Dimensionen.
   `width`/`height`-Attribute der `<map>`-Zeile werden ignoriert (bei
   `infinite="1"` geben sie die Default-Chunk-Größe an, nicht die Karten-Ausdehnung).

2. `<tileset firstgid="..." name="..." tilewidth="..." tileheight="..."
    tilecount="..." columns="...">` mit Kind-Element
   `<image source="..."/>`.

3. Animierte Tiles: `<tile id="..."><animation><frame tileid="..." duration="..."/>
   ...</animation></tile>` — nur innerhalb von `<tileset>`.

4. `<layer name="...">` mit Kind-Element `<data encoding="csv">`.

5. Infinite-Chunks: `<chunk x="..." y="..." width="..." height="...">CSV</chunk>`.
   Falls keine Chunks vorhanden: der CSV-Text direkt in `<data>`, `width` aus
   dem `<layer>`-Attribut lesen.

6. GID aus CSV: kommagetrennte Integer, `0` = leere Zelle (überspringen).
   Flip-Bits extrahieren:
   ```kotlin
   val raw = csvValue.trim().toInt()
   val flipH = (raw and FLIP_H) != 0
   val flipV = (raw and FLIP_V) != 0
   val flipD = (raw and FLIP_D) != 0
   val gid   = raw and GID_MASK
   ```

**Was ignoriert werden darf:**
- Externe `.tsx`-Referenzen (`<tileset source="..."/>`) — keines der 16 Maps nutzt sie.
- `<objectgroup>` und `<imagelayer>` — nicht vorhanden in diesen Maps.
- `<properties>` — nicht ausgewertet.
- `encoding` != `csv` — Exception werfen wenn ein anderes Encoding gefunden wird.

**Parser-Struktur (empfohlen):**

Zeilenweise lesen, Tags mit Regex oder `substringAfter`/`substringBefore` extrahieren.
State-Variablen: `currentTileset`, `currentLayer`, `currentAnimationTileId`,
`inDataBlock`, `inChunk`, `inAnimation`. Kein Stack nötig — die relevante
Nesting-Tiefe ist maximal 4 (`map > tileset > tile > animation`).

Beispiel-Parsing eines Attribut-Wertes:
```kotlin
fun String.attr(name: String): String? =
    Regex("""$name="([^"]*)" """.trimEnd()).find(this)?.groupValues?.get(1)
```

### B.4 Kollisionsraster (`CollisionGrid.kt`)

```kotlin
package rpg.tiled

enum class TileType { WALKABLE, BLOCKED, WATER, TRIGGER, DECORATIVE }

class CollisionGrid(
    val cols: Int,
    val rows: Int,
    private val grid: Array<Array<TileType>>,
) {
    operator fun get(x: Int, y: Int): TileType = grid[y][x]

    companion object {
        /**
         * Leitet das Kollisionsraster aus den Layer-Namen der [map] ab.
         *
         * Strategie:
         * 1. Alle Zellen mit Floor-Layer-Kachel → WALKABLE
         * 2. Alle übrigen Zellen (kein Floor-Treffer) → BLOCKED (Boden-Regel)
         * 3. Solid-Layer überschreiben mit BLOCKED
         * 4. Water-Layer überschreiben mit WATER
         * 5. Trigger-Layer überschreiben mit TRIGGER
         * 6. Decorative-Layer werden ignoriert (keine Kollision)
         *
         * Bounding-Box wird aus allen nicht-leeren Zellen aller Layer ermittelt.
         */
        fun from(map: TiledMap): CollisionGrid
    }
}
```

**Layer-Klassifikation** (aus den realen Layer-Namen der 16 Maps abgeleitet):

```kotlin
private fun layerType(name: String): LayerRole = when {
    // --- FLOOR (Basis für Walkable) ---
    name.matches(Regex("Floor.*|floor.*|Ground|ground|Road|road|Grass.*|grass.*|Carpet|carpet|Spots|spots|Plates|plates|Stairs|stairs")) -> LayerRole.FLOOR

    // --- WATER ---
    name.matches(Regex("(?i)water.*")) -> LayerRole.WATER

    // --- SOLID / BLOCKED ---
    name.matches(Regex("Walls.*|walls.*|Wall.*|wall.*|House.*|house.*|Roof.*|roof.*|Fence.*|fence.*|Statues|Columns|Bricks|bricks|trees.*|Trees.*|Boxes|boxes|Bridges")) -> LayerRole.SOLID

    // --- TRIGGER ---
    name.matches(Regex("(?i)door.*|ladder.*|entrance.*|sign.*|chest.*|lever.*")) -> LayerRole.TRIGGER

    // --- DECORATIVE (Kein Kollisions-Einfluss) ---
    else -> LayerRole.DECORATIVE
}

private enum class LayerRole { FLOOR, WATER, SOLID, TRIGGER, DECORATIVE }
```

**Bounding-Box-Ermittlung:** Wie in `tmx_render.py` — min/max aller `gridX`/`gridY`
über alle Layer. Negative Koordinaten sind möglich bei infinite-Maps.

**Algorithmus:**

```
cols = maxX - minX + 1
rows = maxY - minY + 1
grid = Array(rows) { Array(cols) { BLOCKED } }   // Default: alles geblockt

für jede Zelle in FLOOR-Layern: grid[y-minY][x-minX] = WALKABLE
für jede Zelle in WATER-Layern: grid[y-minY][x-minX] = WATER
für jede Zelle in SOLID-Layern: grid[y-minY][x-minX] = BLOCKED
für jede Zelle in TRIGGER-Layern: grid[y-minY][x-minX] = TRIGGER
// DECORATIVE: ignorieren
// Leere Zellen (gid=0 wurden beim Parsen übersprungen) bleiben BLOCKED
```

### B.5 Tests (`core/src/commonTest/kotlin/rpg/tiled/`)

Tests nutzen **Inline-TMX-Strings** — kein Dateisystem-Zugriff (Sandbox hat
keine Assets). Mindestens folgende Test-Cases:

**`TmxLoaderTest.kt`**

1. **Minimale Map** — eine `<layer>` mit 4 Zellen, kein chunk:
   ```xml
   <?xml version="1.0"?>
   <map tilewidth="16" tileheight="16" width="2" height="2" infinite="0">
     <tileset firstgid="1" name="Base" tilewidth="16" tileheight="16"
              tilecount="4" columns="2">
       <image source="base.png" width="32" height="32"/>
     </tileset>
     <layer name="Floor" width="2" height="2">
       <data encoding="csv">1,2,3,4</data>
     </layer>
   </map>
   ```
   - `map.tileWidth == 16`, `map.tileHeight == 16`
   - `map.tilesets.size == 1`, `map.tilesets[0].firstGid == 1`
   - `map.layers.size == 1`, `map.layers[0].cells.size == 4`
   - Zelle (0,0) hat `gid=1`, keine Flip-Bits

2. **Infinite-Map mit Chunks** — zwei `<chunk>` in einem `<layer>`:
   - Positive und negative Chunk-Koordinaten (z.B. `x="-16"`)
   - Korrekte Berechnung von `gridX = chunkX + (i % chunkWidth)`
   - Beide Chunks liefern gemeinsam alle Zellen

3. **Flip-Bits** — GID `0x80000003` (FLIP_H gesetzt, gid=3):
   - `cell.flipH == true`, `cell.flipV == false`, `cell.flipD == false`
   - `cell.gid == 3`

4. **Animierte Tiles** — Tileset mit `<tile id="0"><animation>` (2 Frames):
   - `tileset.animatedTiles[0]` enthält Liste mit 2 `AnimationFrame`-Einträgen
   - `frames[0].tileId == 0`, `frames[0].durationMs == 150`

5. **Mehrere Tilesets** — zwei `<tileset>`-Blöcke mit unterschiedlichen `firstgid`:
   - Beide landen in `map.tilesets` sortiert nach `firstGid`

6. **Leere Zellen** (gid=0) werden übersprungen:
   - CSV `"1,0,0,2"` → 2 Zellen, nicht 4

**`CollisionGridTest.kt`**

7. **Floor-Regel** — Map mit Floor-Layer (alle Zellen belegt) und keinem Solid-Layer:
   - Alle belegten Zellen → WALKABLE
   - Unbelegte Positionen (außerhalb der Map) nicht abfragbar

8. **Solid überschreibt Floor** — Zelle in Floor und Walls gleichzeitig gesetzt:
   - Ergebnis: BLOCKED

9. **Water-Layer** — `water`-Layer:
   - Zellen → WATER

10. **Trigger-Layer** — `door`-Layer (oder `Ladder`):
    - Zellen → TRIGGER

11. **Reine Solid-Map ohne Floor** (z.B. Walls-only):
    - Alle Zellen → BLOCKED (Default-Regel greift)

12. **Bounding-Box bei negativen Koordinaten** — Chunk mit `x="-16"`:
    - `grid.cols` und `grid.rows` korrekt berechnet
    - `grid[-16, 0]` wird zu `grid[0, 0]` nach Normalisierung

---

## ACCEPTANCE

```
./gradlew :game:compileKotlinAndroid        → BUILD SUCCESSFUL     (Step 3b)
./gradlew :game:compileKotlinDesktop        → BUILD SUCCESSFUL     (unverändert)
./gradlew :core:desktopTest                 → BUILD SUCCESSFUL, 0 Failures
                                              (193 vorher + neue tiled-Tests)
./gradlew :composeApp:compileKotlinDesktop  → BUILD SUCCESSFUL     (unverändert)
```

---

## Doku-Pflicht nach Abschluss

- `docs/KORGE_MIGRATION_PLAN.md` → Step 3b ✅ + Step 4 ✅ markieren
- `README.md` → KorGE-Sektion: Step 4 done, Step 4b next
- `docs/KNOWN_BUGS.md` → alle entdeckten Parser-Fallstricke (z.B. Chunk-Koordinaten,
  Flip-Bit-Vorzeichen, fehlende `tilecount`-Attribute)
- `briefs/2026-06-28-korge-step3b-step4-tiled-android-result.md` → Result-Report

---

## Kontext

- Rendering-Entscheidung: `.kiro/steering/rendering-engine.md` (KorGE 2.5D, locked)
- Migrationsstufen: `docs/KORGE_MIGRATION_PLAN.md`
- Donor-Referenz für TMX-Struktur: `scripts/tmx_render.py` (in diesem Repo, Python)
- KorGE 6.0 API-Migrationsnotizen: `docs/KNOWN_BUGS.md` (aus Step 3)
- AGP-Version für `com.android.library`: `8.2.2` (bereits in `build.gradle.kts` root)
- Kotlin-Version: `2.1.21` (bereits gepinnt, nicht ändern)
- JVM_21 nur für `:game`-Desktop — `:core`, `:composeApp`, `:game`-Android bleiben JVM_17
- Kein GL-Fenster, kein Device-Test — reine Kompilierung und unit-Tests genügen
- Die `Characters.tmx`-Dateien (chapel, glassblowers, guild-hall, ruined-temple)
  enthalten nur NPC-Sprite-Layers — kein Terrain, keine Kollisionsdaten.
  Sie müssen **nicht** geladen werden (kein `TiledMap.kt`-Test dafür nötig).
- Datei `heroes-home/Tiled_files/Exterior — копия.tmx` enthält ein Leerzeichen
  und kyrillische Zeichen im Dateinamen — **nicht parsen**, nur ignorieren.
