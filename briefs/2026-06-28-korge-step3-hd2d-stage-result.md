# Result: KorGE Migration Step 3 — 2.5D HD-2D Stage

**Brief:** `briefs/2026-06-28-korge-step3-hd2d-stage.md`
**Branch:** `kiro/korge-step3-hd2d-stage`
**PR:** https://github.com/Lootziffer666/GAIME/pull/31
**Datum:** 2026-06-28
**Autor:** Kiro
**Status:** ✅ Abgeschlossen — alle Acceptance-Kriterien grün

---

## Zusammenfassung

`demos/korge-hd2d/Hd2dStage.kt` (ein KorGE-5.x-Scaffold) wurde ins `:game`-Modul
portiert, alle `korlibs.*`-Importe auf KorGE 6.0 korrigiert und in `Main.kt` als
Startszene eingetragen. Akzeptanz war ausschließlich Kompilierung (Sandbox ohne
GL-Display) — erfüllt.

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `./gradlew :game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `./gradlew :core:desktopTest` | ✅ BUILD SUCCESSFUL (unverändert) |
| `./gradlew :composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL (unverändert) |

---

## Geänderte / neue Dateien

| Datei | Art | Inhalt |
|---|---|---|
| `game/src/desktopMain/kotlin/game/Hd2dStage.kt` | **create** | Portierte HD-2D-Szene; 6.0-Importe korrigiert; prozedurale Platzhalter-Bitmaps beibehalten |
| `game/src/desktopMain/kotlin/game/Main.kt` | **modify** | `sceneContainer().changeTo<Hd2dStage>()`; `:core`-Proof als Kommentar (lt. Brief) |
| `game/build.gradle.kts` | **modify (Scope-Abweichung, freigegeben)** | `jvmTarget` `JVM_17 → JVM_21` (1 Zeile + Kommentar) |
| `docs/KORGE_MIGRATION_PLAN.md` | modify | Step 2+3 als ✅ markiert, JVM-Target-Finding, Koordinaten-Korrektur |
| `README.md` | modify | KorGE-Sektion (Step 3 done, Step 4 next) + JVM-21-Hinweis |
| `docs/KNOWN_BUGS.md` | modify | 5.x→6.0-Migrationsnotizen + JVM-Target-Fallstrick |

---

## Schritt 0 — Donor-Recherche (Ergebnis)

Recherche gegen die **`korge-6.0.0-sources.jar`** durchgeführt (KorGE ist die
einzige erlaubte Code-Dependency; ausschließlich als Referenz gelesen, kein Code
übernommen). Die Sources lagen **außerhalb** des Repos (`/projects/sandbox/.korge-ref`)
und wurden nicht committet. Die online-Doku war teils veraltet — die Quelle war
maßgeblich.

Korrekturen 5.x → 6.0:

- **`blendMode` / `alpha`** sind Member-`var` auf `View`, keine Top-Level-Symbole
  → Demo-Importe `korlibs.korge.view.blendMode` / `.alpha` entfernt, als Property
  genutzt.
- **`addUpdater {}`**-Lambda erhält `kotlin.time.Duration` (nicht `TimeSpan`)
  → `dt: TimeSpan`-Annotation + `import korlibs.time.TimeSpan` entfernt; `dt.seconds`
  über `import korlibs.time.seconds`.
- **`container()`**-Builder muss importiert werden (`import korlibs.korge.view.container`)
  — fehlte im Scaffold.
- **`BlurFilter(radius = 6.0)`** ist in 6.0.0 **korrekt** (online-Doku zeigte
  veraltetes `initialRadius`; die 6.0.0-Quelle hat `class BlurFilter(radius: Double = 4.0, …)`).
- **`BlendMode`** (typealias → `korlibs.korge.blend.BlendMode`, `ADD` vorhanden),
  **`filter`**, **`keys`**, **`Scene`**, **`sceneContainer`**, **`changeTo`**:
  Pfade in 6.0 stabil → unverändert übernommen.

---

## Scope-Abweichung (freigegeben durch Owner)

`game/build.gradle.kts` stand im `DO_NOT_TOUCH`. Die Compile-Acceptance war ohne
diese Datei jedoch **nicht erfüllbar**:

- KorGE 6.0.0 veröffentlicht seine JVM-Artefakte als **JVM-Target-21-Bytecode**
  (verifiziert: alle 948 Klassen in `korge-core-jvm` und die Inline-Builder in
  `korge-jvm` sind Class-File-Major **65 = Java 21**).
- Die ersten *inline*-KorGE-Funktionen der portierten Szene (`container()`,
  `image()`, `solidRect()`, `addUpdater {}`, `sceneContainer()`, `changeTo<>()`)
  lassen sich nicht in ein JVM-17-Ziel inlinen.

Der Blocker wurde dem Owner **vor** der Änderung gemeldet (inkl. Beweis + Fix-Pfad,
gemäß `KORGE_MIGRATION_PLAN.md §3/Step 2`). Nach **expliziter Freigabe** wurde
`:game` von `JVM_17` auf `JVM_21` gehoben.

**Sicher**, weil `:game` desktop-only ist (kein Android-Target) — `:core` und
`:composeApp` bleiben bei JVM 17 (Android `compileSdk`). Die Gradle-Toolchain läuft
bereits auf JDK 21. Das ist exakt der in der §4-Risiko-Tabelle vorgesehene Fall.
Warum erst jetzt: Step 2 nutzte mit `Korge { println }` keine Inline-Funktion
(`Korge(...)` ist nicht inline) → kompilierte bei 17; `jvmTarget=17` war gegen echte
KorGE-Inlines nie getestet.

---

## Einschränkungen / Hinweise

- **Kein GL-Fenster / kein Laufzeittest** — reine Kompilierung, wie im Brief
  vorgegeben. Visuelle Verifikation bleibt manuell/lokal.
- **`BASE_SHA af78bf49…` nicht kryptografisch verifizierbar:** Shallow-Clone
  (depth 1, BASE_SHA-Objekt nicht vorhanden) + kein Fetch-Auth über die CLI. Der
  `main`-HEAD war zum Start exakt der Brief-Commit `4b57c9d9`; der Branch wurde von
  dort gezogen — keine Code-Divergenz (der Brief-Commit fügt nur Doku/Briefs hinzu).
- **Doku-vs-Build-Diskrepanz korrigiert:** Der Plan nannte
  `com.soywiz.korlibs.korge:korge:6.0.0`; real (und auflösbar) ist
  `com.soywiz.korge:korge:6.0.0` — Plan-Text angepasst, `build.gradle.kts`-Koordinate
  war bereits korrekt und unverändert.
- **Keine Binärdateien als Inhalt gelesen** (B002): API-Recherche lief rein über Text
  (entpackte `-sources.jar` + grep, Bytecode-Version über 8-Byte-Header).

---

## Nächster Schritt

**Step 4** — eigener Tiled-Tilemap-Loader + tile-abgeleitete Kollision in `:core`
(siehe `docs/KORGE_MIGRATION_PLAN.md`). Die prozeduralen Platzhalter-Bitmaps in
`Hd2dStage` werden in **Step 4b** durch echte Assets ersetzt.
