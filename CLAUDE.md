# CLAUDE.md — Session Protocol for Claude Code

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

Ein Kiro-Auftrag muss folgende Felder enthalten:

```
BASE_SHA: <aktueller main-SHA aus git log>
BRANCH_NAME: kiro/<kurzer-name>
TOUCH_ONLY:
  - <Datei/Modul 1>
  - <Datei/Modul 2>
DO_NOT_TOUCH:
  - <alle anderen relevanten Dateien explizit nennen>
ACCEPTANCE:
  - ./gradlew :core:desktopTest  → grün
  - ./gradlew :composeApp:compileKotlinDesktop → grün (falls composeApp berührt)
  - ./gradlew :game:compileKotlinDesktop → grün (falls :game berührt)
PR_TITLE: <präziser Titel, max 70 Zeichen>
```

**Scope:** Kiro arbeitet in genau einem Modul oder einer Querschnittsaufgabe.
Nie mehrere unabhängige Dinge in einem Auftrag mischen.

---

## Nach einem Kiro-PR

1. `git fetch --all --prune`
2. PR-Diff lesen (alle geänderten Dateien)
3. Bei Konflikten mit main: Integration-Branch (`claude/integration-<datum>`), nie direkt auf main mergen solange Konflikte
4. Tests lokal laufen lassen
5. Mergen → Branch löschen (remote + lokal)
6. `git push -u origin main`

---

## Branches löschen

Remote-Delete läuft ggf. nur über GitHub-Web (Proxy blockt `git push --delete`).
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
