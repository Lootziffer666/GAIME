# Handoff-Protocol: Kiro ↔ Claude Code

**Status:** BINDING. Diese Regeln gelten für alle Kiro-Aufträge in diesem Repo.

## Modell-Anforderung (verbindlich, vor allem anderen)

**Kiro-Aufträge in diesem Repo laufen ausschließlich auf Opus.** Jeder Brief trägt im
Header eine Zeile `MODELL: Opus-only`. Wird ein Auftrag auf einem schwächeren Modell
ausgeführt, gilt das als nicht-konforme Lieferung.

- **Bei neuem oder reaktiviertem Thread zuerst das Modell prüfen**, bevor irgendeine
  Arbeit beginnt. Ein neuer Thread erbt die Modellwahl NICHT automatisch — sie muss
  ausgehandelt/bestätigt sein.
- Steht das Modell nicht fest auf Opus → **nicht starten.** Im Result-Report vermerken:
  „Abgebrochen — Modell nicht als Opus bestätigt. Claude Code/Owner muss Thread mit
  Opus aufsetzen."
- **KEINE Delegation an Sub-Threads/Sub-Agents (Schlupfloch-Fix, Step 16, 2026-06-29).**
  „Opus-only" gilt für **die ausführende Arbeit**, nicht nur für den Orchestrator-Thread.
  Der beauftragte Opus-Thread **führt die Arbeit selbst aus** — er darf sie NICHT an
  delegierte/untergeordnete Threads weiterreichen (die nicht garantiert Opus sind). Wenn
  ein Orchestrator behauptet, er „könne nicht selbst arbeiten, nur delegieren" → Auftrag
  abbrechen, nicht delegieren lassen, NICHTS am Repo zerstören (kein Repo-Löschen/Neuklonen).
- **Result-Report muss bestätigen:** „Vollständig auf Opus ohne Delegation ausgeführt."
  Fehlt diese Bestätigung, behandelt Claude Code den PR als provenance-unsicher und prüft
  das `:game`-Rendering verschärft (PNG ansehen) — ein PR ohne Opus-Nachweis wird nicht
  blind gemergt.
- Hintergrund (Erfahrung Step 8, 2026-06-28; Step 16, 2026-06-29): Bei degradiertem/totem
  oder **delegiertem** Thread kommt die test-gedeckte `:core`-Logik sauber durch, aber das
  `:game`-Rendering hat Flüchtigkeitsfehler (Filter-Clobber; in Step 16: Vollflächen-Overlay-
  Weißausfall, Wetter im Innenraum). Genau die Fehlerklasse eines Nicht-Opus-Laufs. Deshalb:
  Modell zuerst, selbst ausführen, dann Arbeit — und Render-Ergebnis immer im PNG prüfen.

## Pflichtlektüre vor Arbeitsbeginn

Diese Dateien **vor jedem Auftrag** lesen:

| Datei | Inhalt |
|---|---|
| `.kiro/steering/handoff-protocol.md` (diese Datei) | Bindende Handoff-Regeln |
| `.kiro/steering/rendering-engine.md` | KorGE 2.5D — locked, nicht diskutieren |
| `docs/KORGE_MIGRATION_PLAN.md` | Migrationsstufen, Modularchitektur, was Throwaway ist |
| `docs/KNOWN_BUGS.md` | Bekannte Bugs und Pitfalls — vor Arbeit lesen, nach Fund ergänzen |
| `docs/STORY_EXPANSION.md` | Dialogslots, offene Szenen — relevant für Content-Aufträge |
| `briefs/<datum>-<name>.md` | Der aktuelle Auftrag — maßgeblich für Scope und Ziel |

---

## Vor dem Start eines Auftrags

```bash
git fetch origin
git rev-parse origin/main   # diesen Wert mit BASE_SHA aus dem Brief vergleichen
```

- Stimmt `origin/main` **nicht** mit `BASE_SHA` überein → **Auftrag abbrechen.**
  Im Result-Report (`briefs/...-result.md`) vermerken: "Abgebrochen — main hat sich
  seit Brief-Erstellung verändert. Neuer SHA: <aktuell>. Claude Code muss Brief neu ausstellen."
- Stimmt der SHA → von `origin/main` branchen:

```bash
git checkout -b <BRANCH_NAME> origin/main
```

Niemals von einem anderen Feature-Branch branchen.

## Nach dem Push — Verifizieren dass die Arbeit auf GitHub gelandet ist

Nach `git push -u origin <BRANCH_NAME>` zwingend prüfen:

```bash
# Branch wirklich auf Remote?
git ls-remote --heads origin <BRANCH_NAME>
# Gibt leere Ausgabe → Push hat lautlos versagt → Blocker im Result-Report
```

Außerdem: PR über GitHub MCP anlegen und die zurückgegebene PR-URL im Result-Report
eintragen. Nur wenn eine gültige PR-URL vorliegt, gilt der Auftrag als abgeliefert.

Wenn weder Push noch PR-Erstellung funktionieren (Netzwerk/Proxy), im Result-Report
unter "Abweichungen" vermerken: "Arbeit lokal fertig, aber nicht auf GitHub gelandet.
Branch-Name: <name>, letzter lokaler Commit: <sha>. Manuelle Übertragung nötig."

---

## Während der Arbeit

Der Auftrag enthält ein `SCOPE`-Feld mit zwei Listen:

- `SCOPE.modify` — bestehende Dateien, die geändert werden dürfen
- `SCOPE.create` — neue Dateien/Verzeichnisse, die erstellt werden dürfen

**Alles außerhalb von `SCOPE.modify` + `SCOPE.create` ist verboten**, auch wenn es
technisch sinnvoll wirkt.

### Neue Dateien erstellen

Erlaubt nur wenn der Pfad unter `SCOPE.create` fällt:

- Expliziter Pfad (`game/src/.../TitleScene.kt`) → genau diese Datei
- Verzeichnis-Wildcard (`core/src/commonMain/kotlin/rpg/tiled/*`) → beliebig viele
  Dateien innerhalb dieses Verzeichnisses, aber nicht in Unterverzeichnissen davon
  und nicht außerhalb

Neue Testdateien (`*Test.kt`) sind innerhalb der entsprechenden `*Test`-Source-Sets
des betroffenen Moduls immer erlaubt — auch ohne expliziten Pfad in `SCOPE.create` —
sofern sie ausschließlich Code aus `SCOPE.modify`/`SCOPE.create` testen.

### Weitere Einschränkungen

- Wenn eine Datei außerhalb des Scopes geändert werden müsste: im PR-Body als Blocker vermerken, nicht eigenmächtig anfassen.
- Keine Reformatierungen, Renames oder Cleanups außerhalb des Scopes.
- Keine neuen Abhängigkeiten in `build.gradle.kts` ohne explizite Erwähnung im Auftrag.
- Keine neuen Branches erstellen — genau ein Branch pro Auftrag.

---

## Vor dem Push

Alle Acceptance-Kriterien aus dem Auftrag müssen grün sein:

```bash
./gradlew :core:desktopTest
# + ggf. :game:compileKotlinDesktop oder :composeApp:compileKotlinDesktop
```

Wenn ein Kriterium nicht erfüllbar ist: **Push trotzdem, aber PR-Body erklärt warum
und was der Blocker ist.** Nie still fehlschlagen.

---

## Dokumentation aktualisieren (Pflicht, nicht optional)

Vor dem Push folgende Dateien prüfen und bei Bedarf aktualisieren:

- **`README.md`** — immer wenn sich etwas Nutzersichtbares ändert (neue Features,
  neue Szenen, neue Befehle, geänderte Architektur). Zweifel → aktualisieren.
- **`docs/KORGE_MIGRATION_PLAN.md`** — Step-Status anpassen falls ein Schritt
  abgeschlossen oder blockiert wurde.
- **`docs/KNOWN_BUGS.md`** — jeden neu entdeckten Bug oder Pitfall eintragen,
  auch wenn er außerhalb des eigenen Scopes liegt. Format:
  `| B<nnn> | Beschreibung | Kiro, YYYY-MM-DD | Datei(en) | OPEN |`
- Weitere `docs/`-Dateien die inhaltlich vom Auftrag berührt werden.

Diese Updates gehören zum gleichen Commit/Branch wie der Feature-Code.

## Result-Report erstellen (Pflicht)

Nach Abschluss der Arbeit, vor dem Push, `briefs/YYYY-MM-DD-<name>-result.md` schreiben:

```markdown
# Result: <Titel des Briefs>

**Brief:** briefs/YYYY-MM-DD-<name>.md
**Branch:** kiro/<name>
**Datum:** YYYY-MM-DD

## Was wurde umgesetzt
<Kurze Zusammenfassung, Datei für Datei>

## Testergebnis
<Anzahl Tests, 0 Failures — oder: Blocker + Begründung>

## Abweichungen vom Brief
<Liste der Abweichungen und Begründung — oder: "Keine">

## Neu entdeckte Bugs / Pitfalls
<Was ist aufgefallen — oder: "Keine">

## Was nicht angefasst wurde
<Explizit bestätigen dass DO_NOT_TOUCH eingehalten wurde>
```

Der Result-Report wird mit dem Branch gepusht und erscheint im PR-Diff.
Claude Code vergleicht Brief und Result beim Review.

## PR erstellen

- **Ein PR pro Auftrag** — keine Sammel-PRs.
- Branch-Name: genau wie im Auftrag angegeben (`BRANCH_NAME`).
- PR-Base: immer `main`.
- PR-Body verlinkt: den Brief (`briefs/...md`) und den Result-Report (`briefs/...-result.md`).
- PR-Body enthält außerdem: Test-Ergebnis, Abweichungen, was NICHT geändert wurde.

---

## Was Kiro nie tun darf

- Eigenen PR mergen
- Branches anderer löschen
- `main` direkt pushen
- Auf Claude's `claude/integration-*`-Branch pushen — der gehört Claude Code
- `settings.gradle.kts` oder `build.gradle.kts` ändern ohne expliziten Auftrag
- Konflikte mit main eigenmächtig auflösen — das macht Claude Code

### Binärdateien niemals als Inhalt öffnen

PNG, WAV, MP3, PDF, ZIP und andere Binärdateien **niemals** per Read-Tool als
Dateiinhalt lesen. Base64-Inhalt eines 2 MB PNG = ~2,7 MB Text = Kontext geflutet,
Thread unbrauchbar.

**Stattdessen:** Nur Metadaten abfragen. Das korrekte Format:

```json
{"message":"Image: /projects/sandbox/GAIME/assets/HD/ui/file.png\nFormat: png\nSize: <n> bytes\nMediaType: image/png"}
```

Für Asset-Entscheidungen (welches Sprite passt?) → Pfad und Dateiname lesen,
Größe prüfen, nie den Inhalt öffnen. Wenn der Inhalt eines Bildes tatsächlich
gebraucht wird: im PR-Body als Blocker vermerken, Claude Code entscheidet.

---

## Modul-Grenzen

| Modul | Sprache/Framework | Kiro darf anfassen wenn... |
|---|---|---|
| `:core` | Pure Kotlin, kein Framework | immer sicher |
| `:game` | KorGE | explizit im Auftrag |
| `:composeApp` | Compose Multiplatform | explizit, mit Vorsicht (Throwaway) |

`:core` ist die bevorzugte Arbeitszone für Kiro-Aufträge.
`:composeApp` wird bei KorGE-Parität entfernt — keinen großen Aufwand dort.
