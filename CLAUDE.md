# CLAUDE.md — Session Protocol for Claude Code

## Pflichtlektüre vor jeder Arbeit

Folgende Dateien **immer lesen** bevor Code geschrieben oder Kiro beauftragt wird:

| Datei | Inhalt |
|---|---|
| `CLAUDE.md` (diese Datei) | Session-Protokoll, Kiro-Auftrag-Format, Branch-Strategie |
| `.kiro/steering/handoff-protocol.md` | Bindende Regeln für Kiro-Aufträge |
| `.kiro/steering/rendering-engine.md` | Architekturentscheidung KorGE 2.5D (locked) |
| `docs/KORGE_MIGRATION_PLAN.md` | Stufenplan KorGE-Migration (aktueller Stand) |
| `docs/KNOWN_BUGS.md` | Bekannte Bugs und Pitfalls — vor Arbeit lesen, nach Fund ergänzen |
| `docs/STORY_EXPANSION.md` | Offene Dialogslots, unbelegte Szenen, Vorlage für neue Quests |

---

## Session-Start Ritual (immer, ohne Ausnahme)

```bash
git fetch --all --prune
git log --oneline --graph origin/main -5
git branch -r | grep -v "origin/main"
```

Dann via GitHub MCP: offene PRs prüfen (`mcp__github__list_pull_requests`).

**Erst wenn main sauber und alle Branches bekannt sind, mit der Arbeit beginnen.**

### Branch-Klassifikation

| Zustand | Aktion |
|---|---|
| Branch ≥ main, PR offen | reviewen, integrieren, Branch löschen |
| Branch ≥ main, kein PR | herausfinden was drin ist, entscheiden |
| Branch ≤ main | direkt löschen |
| Konflikt mit main | Integration-Branch erstellen, auflösen, testen, mergen |

---

## Kiro-Aufträge schreiben

**Niemals Kiro beauftragen solange Branches existieren, die nicht in main sind.**

### Brief-Workflow

1. Brief als `briefs/YYYY-MM-DD-<name>.md` schreiben (Template unten)
2. Brief in `main` committen **bevor** Kiro startet — so ist der Plan versioniert
3. Kiro verlinkt den Brief im PR und schreibt nach Abschluss `briefs/YYYY-MM-DD-<name>-result.md`
4. Result-File kommt mit dem PR-Merge in main — Plan vs. Realität dauerhaft vergleichbar
5. Entdeckte Bugs → `docs/KNOWN_BUGS.md` ergänzen (Kiro im Result-File, Claude beim Review)

### Brief-Template (`briefs/YYYY-MM-DD-<name>.md`)

```markdown
# Brief: <Titel>

**Datum:** YYYY-MM-DD
**Branch:** kiro/<name>
**BASE_SHA:** <aktueller main-SHA>

## Aufgabe
<Was soll erreicht werden, und warum>

## SCOPE
modify:
  - <bestehende Datei>
create:
  - <neue Datei oder verzeichnis/*>

## DO_NOT_TOUCH
  - <explizit verbotene Dateien>

## ACCEPTANCE
  - ./gradlew :core:desktopTest → grün
  - ./gradlew :game:compileKotlinDesktop → grün   # falls :game berührt
  - README.md und betroffene docs/ aktualisiert

## Kontext
<Hinweise, Hintergründe, Querverweise zu anderen Docs>
```

Ein Kiro-Auftrag muss folgende Felder enthalten:

```
BASE_SHA: <aktueller main-SHA aus git log>
BRANCH_NAME: kiro/<kurzer-name>
SCOPE:
  modify:
    - <bestehende Datei die geändert werden darf>
  create:
    - <neuer Dateipfad der erstellt werden darf>       # expliziter Pfad
    - <verzeichnis/>*                                   # ODER: Verzeichnis-Wildcard
DO_NOT_TOUCH:
  - <Datei/Verzeichnis das explizit verboten ist>
ACCEPTANCE:
  - ./gradlew :core:desktopTest  → grün
  - ./gradlew :composeApp:compileKotlinDesktop → grün (falls composeApp berührt)
  - ./gradlew :game:compileKotlinDesktop → grün (falls :game berührt)
PR_TITLE: <präziser Titel, max 70 Zeichen>
```

### Neue Dateien im SCOPE

`create` kennt zwei Formen:

- **Expliziter Pfad** — genau eine Datei: `game/src/desktopMain/kotlin/game/TitleScene.kt`
- **Verzeichnis-Wildcard** — freie Erstellung innerhalb eines Verzeichnisses: `core/src/commonMain/kotlin/rpg/tiled/*`

Verzeichnis-Wildcard nur vergeben wenn der Auftrag eine neue Package/Modul-Struktur aufbaut
(z.B. Tiled-Loader mit 5–10 Klassen). Für 1–3 Dateien immer explizite Pfade.

Dateien **außerhalb** von `SCOPE.modify` und `SCOPE.create` sind implizit verboten —
`DO_NOT_TOUCH` nur für Fälle wo Kiro erfahrungsgemäß reingreifen würde (z.B. `settings.gradle.kts`
wenn ein neues Modul entsteht, aber das Modul nicht im Auftrag ist).

**Scope:** Kiro arbeitet in genau einem Modul oder einer Querschnittsaufgabe.
Nie mehrere unabhängige Dinge in einem Auftrag mischen.

---

## Branch-Strategie (Pflicht für alle größeren Änderungen)

```
main  ←──────────────────────────────  niemals direktes Ziel von Feature-Arbeit
  ↑
claude/integration-<datum>  ←──────── Claude Code: merged, resolved, testet hier
  ↑
kiro/<name>  ───────────────────────── Kiro: arbeitet und pusht hier
```

- Kiro pusht **immer** auf den eigenen Feature-Branch (`kiro/<name>`)
- Claude Code öffnet einen **Integration-Branch** (`claude/integration-<datum>`)
  und merged Kiros Branch dort hinein
- Erst nach grünen Tests wird der Integration-Branch in `main` gemergt
- Bei konfliktfreien, kleinen Änderungen (Docs, reine `:core`-Additions ohne
  Berührung bestehender Dateien) darf Claude Code direkt auf `main` pushen —
  aber nur nach expliziter Prüfung

## Nach einem Kiro-PR

1. `git fetch --all --prune`
2. PR-Diff lesen (alle geänderten Dateien)
3. Integration-Branch erstellen: `git checkout -b claude/integration-<datum> origin/main`
4. Kiros Branch mergen: `git merge --no-ff origin/kiro/<name>`
5. Konflikte auflösen (immer: unsere kanonische API bevorzugen)
6. Tests: `./gradlew :core:desktopTest` (+ compile-checks falls composeApp/:game berührt)
7. In main mergen → `git push -u origin main`
8. Kiros Branch + Integration-Branch löschen

## Branches löschen

`git push origin --delete <branch>` — falls Proxy 403 zurückgibt: GitHub-Web nutzen.
Nach jedem Merge sofort löschen — keine "ich mach das später"-Branches.

---

## Modul-Architektur (Stand: KorGE-Migration Step 2)

```
:core        Reine Kotlin-Logik. Kein Compose, kein KorGE.
             Tests: ./gradlew :core:desktopTest
:game        KorGE-Renderer. Hängt von :core ab.
             Compile-Check: ./gradlew :game:compileKotlinDesktop
:composeApp  Interim-UI (wird bei KorGE-Parität entfernt).
             Compile-Check: ./gradlew :composeApp:compileKotlinDesktop
```

Kiro-Aufträge für **:core** sind sicher (keine Compose/KorGE-Abhängigkeiten).
Aufträge für **:game** oder **:composeApp** müssen vorsichtig scoped sein.
