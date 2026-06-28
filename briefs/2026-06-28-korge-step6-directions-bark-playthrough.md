# Brief: Step 6 — Richtungs-Sprites, Bark-Audio-Pipeline, Scripted Playthrough

**Datum:** 2026-06-28
**Branch:** `kiro/korge-step6-v2`
**Autor:** Kiro (selbst entworfen, Owner-freigegeben)

---

## Aufgabe (4 Deliverables)

### 6a — Asset-Migration: Bark-WAVs nach `assets/audio/bark/`

607 WAVs + 1 JSON aus `composeApp/src/commonMain/composeResources/files/bark/` nach
`assets/audio/bark/` verschieben (`git mv`). Verzeichnisstruktur bleibt identisch.
composeApp verliert die WAVs — Waitroom spielt keine Barks.

### 6b — Richtungs-Sprite-Reihen (B005 Enhancement)

`SpriteLoader.sliceSheet()` liefert alle 4 Reihen (Map<row, List<BmpSlice>>).
`CharacterSprite` nutzt beim Facing die passende Reihe statt reinem scaleX-Flip.

### 6c — Bark-Pipeline mit Audio in `:game`

- `WorldScene` + `BattleScene` erhalten einen `SliceDirector`.
- NPC-Dialog feuert Barks (`fireBark()`), Questbook reagiert.
- `BattleScene` nutzt `sliceDirector.combatAction()` statt direktem `engine.tick()`.
- Audio: `AudioManager.playSfx("assets/audio/" + BarkAudioRegistry.pathFor(event))`.
- `:core` bleibt unverändert — Pfad-Prefix liegt in `:game`.

### 6d — Scripted Playthrough (ScreenshotHarness)

ScreenshotHarness erweitert: geskripteter Spielablauf (spawn → walk → dialog →
map-exit → battle → victory), 5+ Screenshots als CUE-AGENT-Input.

---

## SCOPE

```
modify:
  - game/src/desktopMain/kotlin/game/SpriteLoader.kt
  - game/src/desktopMain/kotlin/game/CharacterSprite.kt
  - game/src/desktopMain/kotlin/game/WorldScene.kt
  - game/src/desktopMain/kotlin/game/BattleScene.kt
  - game/src/desktopMain/kotlin/game/ScreenshotHarness.kt
  - assets/audio/bark/ (create — WAVs hierhin verschoben)

delete:
  - composeApp/src/commonMain/composeResources/files/bark/
```

## ACCEPTANCE

```
./gradlew :game:compileKotlinDesktop → BUILD SUCCESSFUL
./gradlew :core:desktopTest → BUILD SUCCESSFUL
./gradlew :composeApp:compileKotlinDesktop → BUILD SUCCESSFUL
```
