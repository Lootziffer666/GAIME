# Brief: KorGE Migration Step 5 — Retire the Compose Gameplay Engine

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step5-retire-compose-gameplay`
**BASE_SHA:** `f866b54eebbc96b8f7990de80ce7cca805350b87`

---

## Aufgabe

`:game` (KorGE) hat mit Step 5b Parität für den spielbaren Loop erreicht (Welt,
Bewegung, NPCs/Dialog, HUD, Kartenübergänge, Kampf). Step 5 mustert die **Compose-
Canvas-Gameplay-Engine** in `:composeApp` aus — aber **nicht** den Waitroom und
**nicht** wertvolle, getestete Logik.

**Architekturentscheidung (Claude, festgelegt):** `:composeApp` bleibt als
**Waitroom-only**-Modul bestehen. `:game` ist das eigenständige Spiel (zwei
Einstiegspunkte). Das entspricht `docs/KORGE_MIGRATION_PLAN.md` Step 5
("keep :composeApp only for non-gameplay UI if still useful").

Akzeptanz: alle Module kompilieren, **keine Test-Coverage geht verloren**, der
Screenshot-Beweis läuft, und CUE-AGENT erzeugt Video + QA.

---

## Abhängigkeits-Befunde (vor dem Auftrag verifiziert — bitte respektieren)

1. **Der Waitroom hängt von `engine/` ab.** `ui/WaitroomScreen.kt` importiert
   `engine.SceneEngine` + `engine.scenes.{Hd2dDemoScene, LetterSwarmScene,
   SpriteIdleScene}`. Das `engine/`-Paket ist **kein** Gameplay-Müll → **bleibt**.
2. **`rpg.SliceDirector` ist Compose-frei** (keine `androidx`/`compose`-Importe) und
   trägt dichte Test-Coverage (Bark-/Slice-Pipeline). Das ist engine-agnostische
   Spiel-Logik → **nach `:core` verschieben, nicht löschen.** `rpg.BarkOutcome`
   (in `composeApp/.../rpg/`) gehört dazu.
3. Die composeApp-Tests zerfallen in zwei Gruppen:
   - testen `rpg.SliceDirector`/`rpg.bark` (Pipeline-Logik) → **mit nach `:core`**
   - `ParticleEngineTest` testet `engine/` → **bleibt in composeApp** (engine bleibt)

---

## SCOPE

### KEEP (composeApp = Waitroom)
```
- composeApp/src/commonMain/kotlin/ui/WaitroomScreen.kt
- composeApp/src/commonMain/kotlin/engine/**            # vom Waitroom genutzt
- composeApp/src/commonMain/kotlin/app/App.kt           # auf Waitroom-only reduzieren (s.u.)
- composeApp/src/desktopMain/kotlin/main.kt             # Titel/Fenster bleiben
- composeApp/src/androidMain/.../MainActivity.kt
- composeApp/src/*/.../rpg/bark/audio/**                # Audio-Plattformcode (s. Schritt 4)
- composeApp/src/*/.../rpg/gamepad/**                   # Gamepad-Plattformcode (s. Schritt 4)
- composeApp/src/commonTest/kotlin/ParticleEngineTest.kt
```

### REMOVE (Compose-Gameplay-UI — von `:game` ersetzt)
```
- composeApp/src/commonMain/kotlin/ui/rpg/SliceScreen.kt
- composeApp/src/commonMain/kotlin/ui/rpg/RpgDemoScreen.kt
- composeApp/src/commonMain/kotlin/ui/rpg/RpgWorldScreen.kt
- composeApp/src/commonMain/kotlin/ui/rpg/WorldScene.kt        # die COMPOSE-WorldScene, nicht die in :game!
- composeApp/src/commonMain/kotlin/ui/rpg/SceneAtmosphere.kt
- composeApp/src/commonMain/kotlin/ui/rpg/DialogueLine.kt
- composeApp/src/commonMain/kotlin/ui/GameCanvas.kt           # nur wenn ausschließlich vom Gameplay genutzt — vorher prüfen
```

### MOVE → :core (Compose-frei, Coverage erhalten)
```
- composeApp/.../rpg/SliceDirector.kt   → core/src/commonMain/kotlin/rpg/SliceDirector.kt
- composeApp/.../rpg/BarkOutcome.kt     → core/src/commonMain/kotlin/rpg/BarkOutcome.kt
  (+ alle weiteren rpg.*-Typen aus composeApp, die SliceDirector braucht und die
   Compose-frei sind — vor dem Move prüfen)
- composeApp/src/commonTest/.../{BarkCooldownBypassTest, Chapter2PipelineTest,
  SliceAcceptanceCriteriaTest, SlicePipelineTest}.kt  → core/src/commonTest/kotlin/
```

### create
```
- briefs/2026-06-28-korge-step5-retire-compose-gameplay-result.md
```

---

## DO_NOT_TOUCH
```
- game/                          # KorGE-Spiel ist fertig; nicht anfassen
- core/ (bestehende Dateien)     # nur die oben gelisteten MOVE-Ziele hinzufügen
- assets/                        # read-only
- .devcontainer/, scripts/setup-gl.sh
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt   # Beweis-Werkzeug, unverändert
```

---

## Schritte

### Schritt 1 — Logik nach :core retten (zuerst!)
`SliceDirector.kt` + `BarkOutcome.kt` (+ benötigte Compose-freie `rpg.*`-Typen) nach
`core/src/commonMain/kotlin/rpg/` verschieben. Pakете bleiben `rpg.*` (kein Rename).
Die vier Pipeline-Tests nach `core/src/commonTest/kotlin/` verschieben. `:core` hat
keine Compose-Abhängigkeit — falls ein verschobener Typ doch etwas Compose-spezifisches
zieht, im Result dokumentieren und nur den Compose-freien Kern extrahieren.

### Schritt 2 — Gameplay-UI entfernen
Die REMOVE-Liste löschen. Vorher `GameCanvas.kt` prüfen: wird es vom Waitroom oder
von `engine/` referenziert? Wenn ja → behalten; wenn nur vom Gameplay → löschen.

### Schritt 3 — `App.kt` auf Waitroom reduzieren
Den `Mode`-Enum + die EXPLORE/RPG-Buttons entfernen; `App` rendert direkt
`WaitroomScreen`. Keine Referenzen mehr auf `SliceScreen`/`RpgDemoScreen`.

### Schritt 4 — Tote Plattformpfade prüfen
`rpg/bark/audio/**` und `rpg/gamepad/**`: nur noch vom Gameplay genutzt? Wenn der
Waitroom + `engine/` sie nicht braucht und auch `:core`-Tests sie nicht mehr
referenzieren (nach dem Test-Move), als tot markieren — aber **im Zweifel behalten**
und im Result-File auflisten, statt blind zu löschen.

### Schritt 5 — Doku
- `docs/KORGE_MIGRATION_PLAN.md` → Step 5 ✅
- `docs/KNOWN_BUGS.md` → falls etwas auffällt
- Result-File mit KEEP/REMOVE/MOVE-Bilanz + was tatsächlich entfernt wurde

---

## ACCEPTANCE

```
./gradlew :core:desktopTest                 → grün, inkl. der 4 verschobenen Tests
./gradlew :game:compileKotlinDesktop        → grün (unverändert)
./gradlew :composeApp:compileKotlinDesktop  → grün (Waitroom-only)
```

**Visueller/spielbarer Beweis (PFLICHT, nicht nur Compile):**
```
bash scripts/setup-gl.sh        # einmalig: headless-GL-Stack
./gradlew :game:screenshot      # erzeugt build/screenshots/{interior,exterior,battle}.png
```
Die drei PNGs müssen weiterhin korrekt rendern (Map + Einzel-Sprites + HUD/Dialog) —
Beweis, dass das Ausmustern von `:composeApp` `:game` nicht beschädigt hat.

**Video + QA (PFLICHT): CUE-AGENT.**
CUE-AGENT (https://github.com/lootziffer666/CUE-AGENT) direkt ins Repo installieren
(folge dessen README) und für Video-Capture + QA des spielbaren Loops ausführen.
Das Ergebnis (Video/QA-Report) im Result-File verlinken. **Hinweis:** CUE-AGENT wird
von Kiro/der Pipeline ausgeführt — Claude führt es nicht aus.

---

## Kontext

- `:game`-Einstieg: `game.MainKt` (`./gradlew :game:run`, manuell/lokal mit Display).
- `:composeApp`-Einstieg bleibt `main.kt` (Waitroom).
- Screenshot-Harness + GL-Setup existieren bereits (Step davor); nicht neu bauen.
- Branch-Strategie: Kiro pusht auf `kiro/korge-step5-retire-compose-gameplay`;
  Claude integriert über einen `claude/integration-*`-Branch nach `main`.
