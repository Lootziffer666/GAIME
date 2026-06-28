# Brief: Step 7b — Questbook-UI sichtbar machen (SliceDirector → Bildschirm)

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step7b-questbook-ui`
**BASE_SHA:** `77462c5f`

---

## Hintergrund

Seit Step 6c läuft die Bark-Pipeline in `:game`: `WorldScene` ruft bei E-Interaktion
`director.fireBark(event)` auf. **Aber:** der Rückgabewert (`BarkOutcome`) wird verworfen,
und nichts von der Questbook-Reaktion erreicht den Bildschirm. Der `SliceDirector` führt
intern bereits Buch über `questMarkers`, `falseMarkers`, `partyName`, `pressure` — alles
unsichtbar.

Dieses Brief ist die **De-Risking-Stufe** vor dem Shader-First-Sprung (Step 7c): Wir beweisen
zuerst, dass die Schleife `fireBark() → Reaktion → sichtbare UI` funktioniert, mit
konventionellem HUD. In 7c wird genau dieselbe Schleife dann (zusätzlich/alternativ) Shader
treiben.

**Der Kern-Moment des Spiels** ist genau diese bürokratische Fehlinterpretation, die auf dem
Bildschirm erscheint — „Official Quest Registered: Locate subterranean valuables (Priority:
Mandatory)". Bisher ist dieser Moment unsichtbar. Das ändern wir.

**Wichtig vor dem Start:**
```bash
git fetch origin main
git checkout -b kiro/korge-step7b-questbook-ui origin/main
git log --oneline -3   # soll 77462c5f ganz oben zeigen
```

---

## Aufgabe

Ein neues `QuestbookOverlay` in `:game`, das drei Dinge sichtbar macht, plus die Verdrahtung
in `WorldScene`, plus ein Screenshot-Beweis.

`:core` wird **nicht** angefasst — die Pipeline ist komplett. Wir konsumieren nur, was
`SliceDirector` / `QuestbookProcessor` schon liefern.

### Verfügbare `:core`-API (nur lesen, nicht ändern)

```kotlin
// rpg.SliceDirector — nach fireBark():
sealed class BarkOutcome {
    data class Fired(val reaction: QuestbookReaction) : BarkOutcome()
    data class Suppressed(val remainingMillis: Long) : BarkOutcome()
}
director.fireBark(event): BarkOutcome      // feuert + wendet Effekt auf lokalen State an
director.pressure: QuestPressure           // LOW / MEDIUM / HIGH
director.questMarkers: List<String>        // echte Marker (targetHint)
director.falseMarkers: List<String>        // falsche Marker (label) — sehen für Spieler IDENTISCH aus
director.partyName: String?                // gesetzt nach RegisterPartyName

// rpg.questbook.QuestbookReaction:
//   .bark, .questbookText (der Anzeige-Text!), .effect, .pressureBefore, .pressureAfter, .pressureChanged
// rpg.questbook.QuestPressure: enum LOW, MEDIUM, HIGH
```

---

### 7b-1 — Neues File: `game/src/desktopMain/kotlin/game/QuestbookOverlay.kt`

Screen-fixed Overlay (wie `HudOverlay`/`DialogOverlay`: alle Views direkt an `parent`,
einzeln `.visible`-getoggelt — `SolidRect` ist ein Leaf-View, kein Container, siehe
KNOWN_BUGS Step 5b „SolidRect ist kein Container").

Drei Bestandteile:

**(a) Reaction-Toast** — oben mittig. Wenn eine Reaktion feuert, erscheint ein Panel mit
`reaction.questbookText`. Auto-Dismiss nach ~4 Sekunden (über `update(dtSeconds)` getickt).
Stil: dunkles Panel (`RGBA(0x0a,0x0a,0x14,0xee)`), bürokratisch-goldener Rand
(`Colors["#886644"]`), Text `Colors["#ffe9b0"]`, ~13px. Header-Zeile „📋 QUESTBOOK" oder
schlicht „QUESTBOOK ENTRY" in `Colors["#ffdd88"]`.

**(b) Pressure-Pill** — oben rechts. Kleiner farbiger Block + Label „PRESSURE: LOW/MEDIUM/HIGH".
Farbe nach Level: LOW `#22cc22`, MEDIUM `#ddaa22`, HIGH `#cc2222`. Immer sichtbar.

**(c) Quest-Marker-Liste** — rechte Seite unter der Pill. Listet die aktiven Marker
(echte + falsche **zusammen und optisch identisch** — das ist Spieldesign-Kanon: der Spieler
darf falsche nicht erkennen). Header „QUESTS (N)". Max 5 Einträge zeigen, je `• <hint>`,
~11px, `Colors["#cccccc"]`. Leere Liste → Panel ausblenden.

Empfohlene API:
```kotlin
class QuestbookOverlay(parent: Container, private val vw: Double, private val vh: Double) {
    /** Zeigt eine frische Reaktion als Toast + aktualisiert Pressure-Pill + Marker-Liste. */
    fun showReaction(reaction: QuestbookReaction, pressure: QuestPressure, markers: List<String>)

    /** Aktualisiert nur Pressure-Pill + Marker-Liste ohne Toast (z.B. initial). */
    fun refresh(pressure: QuestPressure, markers: List<String>)

    /** Pro Frame aufrufen; blendet den Toast nach Ablauf der Anzeigedauer aus. */
    fun update(dtSeconds: Float)
}
```

Toast-Timer: intern `private var toastRemaining: Float = 0f`. `showReaction` setzt ihn auf
`4f` und macht das Toast-Panel sichtbar. `update(dt)` zieht `dt` ab; bei `<= 0` Panel ausblenden.

Lange `questbookText` ggf. auf ~50 Zeichen/Zeile mit `\n` umbrechen (manuell, wie DialogOverlay —
kein Auto-Word-Wrap in KorGE, siehe KNOWN_BUGS Step 5b „Text-Wrapping").

---

### 7b-2 — `WorldScene.kt` verdrahten

1. Import ergänzen: `import rpg.BarkOutcome`.
2. Nach dem HUD (Punkt 6) das Overlay anlegen:
   ```kotlin
   val questbook = QuestbookOverlay(this, width, height)
   questbook.refresh(director.pressure, director.questMarkers + director.falseMarkers)
   ```
3. E-Key-Interaktion: den `BarkOutcome` auswerten statt verwerfen:
   ```kotlin
   if (npc != null) {
       dialog.show(npc.first.dialog)
       npc.first.barkEvent?.let { event ->
           launch {
               val outcome = director.fireBark(event)
               if (outcome is BarkOutcome.Fired) {
                   questbook.showReaction(
                       outcome.reaction,
                       director.pressure,
                       director.questMarkers + director.falseMarkers,
                   )
               }
           }
       }
       return@addUpdater
   }
   ```
4. Im Input-`addUpdater` den Toast-Timer ticken. Der Lambda-Parameter ist `kotlin.time.Duration`
   (siehe KNOWN_BUGS Step 3 „addUpdater-Lambda"). Benenne ihn und nutze `.seconds`:
   ```kotlin
   addUpdater { dt ->
       questbook.update(dt.seconds.toFloat())
       val keys = views.input.keys
       ...
   }
   ```
   `import korlibs.time.seconds` ergänzen (falls nicht schon da).

**Nicht** in den Bewegungs-Trigger oder SPACE-Battle-Trigger eingreifen. Nur die E-Interaktion
und der Toast-Tick.

**Hinweis:** Die meisten verdrahteten NPC-Barks (`BARKEEP_SPEND_SOME_COIN`,
`PATRON_HE_SURE_IS_SLOW`, `GUARD_BACK_ALREADY`) liefern „Atmospheric observation noted"
(FlavorText). Das ist korrekt und soll auch als Toast erscheinen — der bürokratische
Flavor-Text IST der Witz. Marker-erzeugende Barks sind im Slice an andere Trigger gebunden;
der Marker-Liste-Pfad wird im Screenshot (7b-3) mit `NIB_SMELL_TREASURE` bewiesen.

---

### 7b-3 — Screenshot-Beweis (`ScreenshotHarness.kt`)

**KRITISCH — B007:** Die Zeile
```kotlin
private val OUT = localCurrentDirVfs["build/screenshots"]
```
und ihr Import `import korlibs.io.file.std.localCurrentDirVfs` dürfen NICHT geändert werden.
Dreimal von Kiro revertiert (KNOWN_BUGS B007). Steht in DO_NOT_TOUCH.

Eine neue Capture-Funktion `captureQuestbookReaction()` ans Ende anfügen und in `fun main()`
registrieren (nach `captureBattleVictory()`):

```kotlin
captureQuestbookReaction()
```

Inhalt: Interior-Map wie `captureInteriorDialog()` aufbauen, dann eine **echte** Reaktion über
einen SliceDirector erzeugen und das Overlay rendern — so ist es genuine Pipeline-Ausgabe,
kein Mock:

```kotlin
private fun captureQuestbookReaction() {
    val config = MapConfig.interior()
    korgeScreenshotTest(Size(VW, VH)) {
        // ... gleicher Map-/Player-/NPC-Aufbau wie captureInteriorDialog() ...
        val hero = Combatant(id = "nib", name = "Nib", maxHp = 80, side = Side.PLAYER, attackPower = 12)
        HudOverlay(this, hero, Inventory(initialGold = 50), config.displayName)

        // Echte Reaktion aus der Pipeline ziehen (kein Mock):
        val director = SliceDirector { 0L }
        director.enterRoom(RoomContext(mapId = "tavern", roomId = RoomContext.ROOM_TAVERN, hasInteractableTarget = true))
        val outcome = director.fireBark(BarkEvent.NIB_SMELL_TREASURE)

        val questbook = QuestbookOverlay(this, VW, VH)
        if (outcome is BarkOutcome.Fired) {
            questbook.showReaction(outcome.reaction, director.pressure, director.questMarkers + director.falseMarkers)
        }
        save("questbook_reaction")
    }
}
```

**Prüfe** die `RoomContext`-Parameternamen gegen `rpg.questbook.RoomContext` (das Feld heißt
`hasInteractableTarget`). `NIB_SMELL_TREASURE` mit `hasInteractableTarget = true` ergibt
„Official Quest Registered: Locate subterranean valuables (Priority: Mandatory)" + einen
QuestMarker → Toast UND Marker-Liste sind im Screenshot belegt.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/WorldScene.kt
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt

create:
  - game/src/desktopMain/kotlin/game/QuestbookOverlay.kt
  - briefs/2026-06-28-korge-step7b-questbook-ui-result.md
```

## DO_NOT_TOUCH

```
- game/src/desktopMain/kotlin/game/ScreenshotHarness.kt  → Zeile `localCurrentDirVfs["build/screenshots"]` + deren Import NICHT ANRÜHREN (B007)
- core/                          KEINE Änderungen — Pipeline ist komplett, nur konsumieren
- composeApp/                    Throwaway, nicht anfassen
- game/src/desktopMain/kotlin/game/shader/   Shader-Dateien unberührt (das ist Step 7c)
- game/src/desktopMain/kotlin/game/BattleScene.kt   Combat-Reaktions-UI ist Step 3 (Combat-Tiefe), NICHT hier
- game/src/desktopMain/kotlin/game/HudOverlay.kt    bleibt wie es ist
- settings.gradle.kts
- docs/KNOWN_BUGS.md             nur lesen
```

---

## ACCEPTANCE

```bash
./gradlew :core:desktopTest                  → BUILD SUCCESSFUL
./gradlew :game:compileKotlinDesktop         → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop   → BUILD SUCCESSFUL
```

**Visueller Beweis (PFLICHT):**
```bash
bash scripts/setup-gl.sh   # falls nicht schon installiert
./gradlew :game:screenshot
```
Erwartet: die bestehenden 12 PNGs + **`questbook_reaction.png`** = 13 Stück in `build/screenshots/`.

`questbook_reaction.png` muss zeigen:
- den Reaction-Toast mit lesbarem Text „Official Quest Registered: Locate subterranean valuables …"
- die Pressure-Pill (hier: LOW, grün — `NIB_SMELL_TREASURE` ändert Pressure nicht)
- die Quest-Marker-Liste mit mindestens einem Eintrag

Kein schwarzes Rechteck, kein leeres Overlay.

---

## Kontext / Querverweise

- **KNOWN_BUGS B007:** `localCurrentDirVfs` ist der korrekte Fix — NICHT zu `localVfs` zurück.
- **KNOWN_BUGS Step 5b:** `SolidRect` ist Leaf-View (keine Kinder); Text kein Auto-Wrap (`\n` manuell).
- **KNOWN_BUGS Step 3:** `addUpdater { dt -> }` Lambda-Param ist `kotlin.time.Duration`; `dt.seconds`.
- **BarkOutcome** liegt in Package `rpg` (nicht `rpg.bark`): `import rpg.BarkOutcome`.
- **Falsche Marker:** optisch identisch zu echten rendern — Spieldesign-Kanon (der Spieler
  darf sie nicht unterscheiden). Deshalb `questMarkers + falseMarkers` als eine Liste.
- **Pipeline-Determinismus:** gleiche Bark + gleicher Context → gleiche Reaktion. Der Screenshot
  ist daher reproduzierbar.
- **Nächste Schritte (NICHT in diesem Brief):**
  - **7c (Shader = State):** dieselbe `BarkOutcome.Fired`-Schleife treibt Shader-Effekte
    (PoisonFilter bei HIGH pressure, etc.). Baut auf der hier bewiesenen Schleife auf.
  - **Step 3 (Combat-Tiefe):** BattleScene zeigt BossPhase/Adds-Reaktionen.
