# Brief: KorGE Migration Step 3 — 2.5D HD-2D Stage

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step3-hd2d-stage`
**BASE_SHA:** `af78bf491a0a631e1cd6fca56f1210cace53331d`

---

## Aufgabe

`demos/korge-hd2d/Hd2dStage.kt` ist ein Scaffold der HD-2D-Rendering-Technik
(Schicht-Parallax, Tiefenunschärfe via BlurFilter, additives Bloom-Glow,
Pixel-Sampling). Es wurde für KorGE 5.x geschrieben und kompiliert daher nicht
gegen die im Projekt verwendete KorGE 6.0.0.

Ziel: `Hd2dStage` in das `:game`-Modul überführen, alle Importpfade auf KorGE 6.0
korrigieren, und in `Main.kt` als Startszene eintragen. Akzeptanzkriterium ist
ausschließlich **Kompilierung** — kein GL-Fenster, keine laufende App (Sandbox
hat kein Display).

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/Main.kt

create:
  - game/src/desktopMain/kotlin/game/Hd2dStage.kt
```

---

## DO_NOT_TOUCH

```
- core/                          # reine Logik, kein KorGE
- composeApp/                    # Interim-UI, Throwaway
- settings.gradle.kts            # Modul bereits registriert
- game/build.gradle.kts          # KorGE 6.0.0 als Library bereits konfiguriert
- demos/                         # Quelldatei bleibt als historische Referenz
- assets/                        # Assets werden nicht verschoben oder kopiert
```

---

## Schritt 0 — Donor-Repo-Recherche (vor dem Coden)

Vor der Implementierung GitHub nach KorGE-6.0-Beispielen durchsuchen.
Suchziele:

- Korrekte Importpfade für `BlendMode`, `BlurFilter`, `sceneContainer`, `changeTo`
  in KorGE 6.0 (die Demo nutzt 5.x-Pfade die nicht mehr existieren).
- Wie HD-2D-Schichten in KorGE 6.0 aufgebaut werden (Scene → Container-Hierarchie,
  Filter-API, additive Blending).
- Ob `BlurFilter(radius = 6.0)` in 6.0 noch so heißt oder umbenannt wurde.

**Donor-Policy** (aus `docs/KORGE_MIGRATION_PLAN.md §1.3`): Externe Repos sind
**ausschließlich Referenz** ("wie funktioniert das"). Kein Code übernehmen, keine
Notices entfernen. Aus dem verstandenen Verhalten neu implementieren — so entstehen
keine Lizenz-Obligations. Nur KorGE selbst ist eine erlaubte Code-Dependency.

Suchanfragen (Beispiele, nicht abschließend):
- `korge BlendMode ADD site:github.com`
- `korge BlurFilter scene 2.5D site:github.com`
- `korge sceneContainer changeTo 6.0 site:github.com`
- KorGE-eigene Samples unter `https://github.com/korlibs/korge-samples`

Gefundene API-Korrekturen im Result-Report und in `docs/KNOWN_BUGS.md` dokumentieren.

---

## Schritt 1 — `game/src/desktopMain/kotlin/game/Hd2dStage.kt` erstellen

Ausgangspunkt ist `demos/korge-hd2d/Hd2dStage.kt`. Übernehmen mit diesen Anpassungen:

**Paket:** `package game` (statt `package demos.korge`)

**KorGE-6.0-Importpfade recherchieren und korrigieren.** Der Demo-Code enthält
Importe die aus KorGE 5.x stammen. Bekannte Baustellen:

- `korlibs.korge.view.BlendMode` und `korlibs.korge.view.blendMode` —
  in KorGE 6.0 ist `BlendMode` möglicherweise unter `korlibs.image.color.BlendMode`
  oder als Enum-Property direkt auf `View`. Korrekte Pfade in der 6.0-Quelle nachschlagen
  (Donor-Recherche aus Schritt 0 nutzen).
- `BlurFilter(radius = 6.0)` — Konstruktor-Signatur prüfen; Parameter könnte
  anders heißen oder ein anderer Typ erwartet werden.
- `korlibs.korge.scene.Scene` / `SContainer` — in 6.0 unverändert, aber bestätigen.
- `korlibs.korge.input.keys` — `views.input.keys.pressing(Key.A)` prüfen;
  API ist in 6.0 geringfügig umstrukturiert.
- Alle anderen Importe aus der Demo-Datei gegen KorGE-6.0-Quellen validieren.

**Prozedurale Bitmaps beibehalten** (`buildHeroBitmap`, `buildBookBitmap`, etc.).
Sie sind bewusst Platzhalter für diesen Compile-Check-Schritt und werden in
Step 4b durch echte Assets ersetzt. Die echten Assets existieren bereits:

```
composeApp/src/commonMain/composeResources/drawable/
  hero_nib.png, hero_brugg.png, hero_vellum.png
  enemy_blob.png, enemy_rat.png, enemy_wolf.png
  questbook_open.png, questbook_closed.png
  tile_floor.png, tile_wall.png, tileset_dungeon.png
  world_tavern.png, world_chapel_ext.png, ...

assets/HD/characters/swordsman/PNG/   ← animierte Sprites
assets/HD/characters/vampire/PNG/
assets/HD/tilesets/village/
assets/HD/locations/...
```

Für Step 3 (Kompilierung ohne GL-Fenster) sind die prozeduralen Bitmaps die
richtige Wahl — `resourcesVfs[...].readBitmap()` wäre ein suspend-Aufruf der erst
zur Laufzeit fehlschlägt, nicht zur Compilezeit. Die Ressourcen-Verkabelung kommt
in Step 4b.

---

## Schritt 2 — `game/src/desktopMain/kotlin/game/Main.kt` anpassen

Aktueller Inhalt:
```kotlin
suspend fun main() = Korge {
    val coreArchetypes = EnemyArchetype.entries.size
    println("GAIME :game (KorGE) booted — :core reachable ($coreArchetypes enemy archetypes).")
}
```

Neuer Inhalt (`:core`-Dependency-Proof bleibt als Kommentar erhalten):
```kotlin
package game

import korlibs.korge.Korge
import korlibs.korge.scene.sceneContainer
import rpg.combat.EnemyArchetype   // :core reachability proof

suspend fun main() = Korge {
    // :core reachable: ${EnemyArchetype.entries.size} enemy archetypes
    sceneContainer().changeTo<Hd2dStage>()
}
```

Exakte Import-Pfade für `sceneContainer` und `changeTo` in KorGE 6.0 bestätigen
(aus der Donor-Recherche in Schritt 0).

---

## ACCEPTANCE

```
./gradlew :game:compileKotlinDesktop   → BUILD SUCCESSFUL (0 errors)
./gradlew :core:desktopTest            → weiterhin grün, unverändert
./gradlew :composeApp:compileKotlinDesktop → weiterhin grün, unverändert
```

Kein GL-Fenster, kein Runtime-Test — reine Kompilierung genügt.

---

## Doku-Pflicht nach Abschluss

- `docs/KORGE_MIGRATION_PLAN.md` → Step 3 als ✅ markieren
- `README.md` → KorGE-Sektion aktualisieren (Step 3 abgeschlossen, Step 4 nächstes)
- `docs/KNOWN_BUGS.md` → alle während der Arbeit entdeckten API-Inkompatibilitäten
  oder Fallstricke eintragen (insbesondere 5.x→6.0-Importe)
- `briefs/2026-06-28-korge-step3-hd2d-stage-result.md` → Result-Report schreiben

---

## Kontext

- Rendering-Entscheidung: `.kiro/steering/rendering-engine.md` (KorGE 2.5D, locked)
- Migrationsstufen: `docs/KORGE_MIGRATION_PLAN.md`
- `:game`-Modul wurde in Step 2 angelegt: `game/build.gradle.kts` +
  `game/src/desktopMain/kotlin/game/Main.kt` (KorGE 6.0.0 als Library, kein Plugin)
- Fallstrick: KorGE-Gradle-Plugin **nicht** verwenden — das Projekt nutzt bewusst
  KorGE als Library (`com.soywiz.korge:korge:6.0.0`) um Kotlin-Plugin-Versionskonflikt
  mit Compose/AGP zu vermeiden.
- Die prozeduralen Bitmaps in `Hd2dStage` sind Platzhalter für den Compile-Check.
  In Step 4b werden sie durch echte Assets ersetzt (Pfade siehe Schritt 1 oben).
