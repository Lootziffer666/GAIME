# Handoff-Protocol: Kiro ↔ Claude Code

**Status:** BINDING. Diese Regeln gelten für alle Kiro-Aufträge in diesem Repo.

---

## Vor dem Start eines Auftrags

1. Den `BASE_SHA` aus dem Auftrag prüfen: `git log --oneline origin/main | head -1`
2. Falls der aktuelle main-HEAD vom `BASE_SHA` abweicht: **Auftrag abbrechen, Abweichung im PR melden.**
3. Immer von `main` branchen, niemals von einem anderen Feature-Branch.

```bash
git fetch origin
git checkout -b <BRANCH_NAME> origin/main
```

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

## PR erstellen

- **Ein PR pro Auftrag** — keine Sammel-PRs.
- Branch-Name: genau wie im Auftrag angegeben (`BRANCH_NAME`).
- PR-Base: immer `main`.
- PR-Body muss enthalten:
  - Kurze Zusammenfassung was geändert wurde
  - Test-Ergebnis (Anzahl Tests, 0 Failures)
  - Was **nicht** geändert wurde (explizit)
  - Abweichungen vom Auftrag und Begründung (oder "keine")

---

## Was Kiro nie tun darf

- Eigenen PR mergen
- Branches anderer löschen
- `main` direkt pushen
- `settings.gradle.kts` oder `build.gradle.kts` ändern ohne expliziten Auftrag
- Konflikte mit main eigenmächtig auflösen — das macht Claude Code

---

## Modul-Grenzen

| Modul | Sprache/Framework | Kiro darf anfassen wenn... |
|---|---|---|
| `:core` | Pure Kotlin, kein Framework | immer sicher |
| `:game` | KorGE | explizit im Auftrag |
| `:composeApp` | Compose Multiplatform | explizit, mit Vorsicht (Throwaway) |

`:core` ist die bevorzugte Arbeitszone für Kiro-Aufträge.
`:composeApp` wird bei KorGE-Parität entfernt — keinen großen Aufwand dort.
