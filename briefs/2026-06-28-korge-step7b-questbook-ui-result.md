# Result: Step 7b — Questbook-UI sichtbar (QuestbookOverlay)

**Brief:** `briefs/2026-06-28-korge-step7b-questbook-ui.md`
**Branch:** `kiro/korge-step7b-questbook-ui`
**PR:** https://github.com/Lootziffer666/GAIME/pull/41
**Datum:** 2026-06-28
**Autor:** Kiro (Opus)
**Status:** ✅ Vollständig geliefert

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL |
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:game:screenshot` | ✅ 13 PNGs (inkl. `questbook_reaction.png`) |

---

## Geliefert

### QuestbookOverlay.kt (neu) ✅
- **Reaction-Toast** oben mittig: `reaction.questbookText`, 4s Auto-Dismiss über `update(dtSeconds)`.
- **Pressure-Pill** oben rechts: farbcodiert LOW (grün) / MEDIUM (amber) / HIGH (rot), immer sichtbar.
- **Quest-Marker-Liste** rechts: echte + falsche Marker optisch identisch (Spieldesign-Kanon),
  bis zu 5 Einträge, „QUESTS (N)"-Header, blendet bei leerer Liste aus.
- Alle Views direkt an `parent`, einzeln `.visible` (SolidRect-Leaf-Regel beachtet).

### WorldScene.kt ✅
- `import rpg.BarkOutcome` + `import korlibs.time.seconds`.
- Overlay nach HUD angelegt + initial `refresh()`.
- E-Interaktion wertet `BarkOutcome.Fired` aus → `showReaction(...)`.
- Toast-Timer in `addUpdater { dt -> questbook.update(dt.seconds.toFloat()) }`.
- Bewegungs- und SPACE-Trigger unangetastet.

### ScreenshotHarness.kt ✅
- `captureQuestbookReaction()` — feuert `NIB_SMELL_TREASURE` mit `hasInteractableTarget = true`
  durch einen echten `SliceDirector` und rendert das Overlay. Genuine Pipeline-Ausgabe.

**Visuell verifiziert:** `questbook_reaction.png` zeigt den Toast „Official Quest Registered:
Locate subterranean valuables (Priority: Mandatory)", PRESSURE: LOW (grün), QUESTS (1) mit
„• nearest interactable". Committed als `docs/screenshots/step7b-questbook-reaction.png`.

---

## DO_NOT_TOUCH — eingehalten ✅
- B007 `localCurrentDirVfs`: **intakt** (zweites Mal in Folge kein Revert).
- `core/`, `composeApp/`, `game/shader/`, `BattleScene.kt`, `HudOverlay.kt`: unmodifiziert.

---

## Anmerkungen
- **Kosmetik (kein Blocker):** Der Word-Wrap nutzt `text.chunked(50)` und trennt mitten im Wort
  („val/uables"). Bei nächster Overlay-Berührung auf Wort-Grenzen umstellen.
- **Build-Quirk:** `:game:screenshot` schlug im ersten Lauf transient fehl (GL-Software-Rasterizer-
  Timing, „Too many callbacks"), beim Re-Run BUILD SUCCESSFUL mit allen 13 PNGs. Bekanntes
  Headless-GL-Verhalten, kein Code-Problem.
- Remote-Branch `kiro/korge-step7b-questbook-ui` → per GitHub Web UI löschen (403 blockt CLI).

---

## Stand nach Merge
main aktualisiert — QuestbookOverlay live in `WorldScene`. Die Schleife
`fireBark() → BarkOutcome.Fired → sichtbare UI` ist bewiesen. Damit ist das Fundament für den
Shader-First-Schritt (7c) gelegt: dieselbe `BarkOutcome.Fired`-Schleife treibt dort die Shader.
