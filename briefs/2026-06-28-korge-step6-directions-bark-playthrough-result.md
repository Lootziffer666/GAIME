# Result: Step 6 — Richtungs-Sprites, Bark-Audio-Pipeline, Scripted Playthrough

**Brief:** `briefs/2026-06-28-korge-step6-directions-bark-playthrough.md`
**Branch:** `kiro/korge-step6-v2`
**PR:** https://github.com/Lootziffer666/GAIME/pull/38
**Datum:** 2026-06-28
**Autor:** Kiro (Opus-only)
**Status:** ✅ Abgeschlossen

---

## Acceptance

| Check | Ergebnis |
|---|---|
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |

---

## Geliefert

### 6a — Asset-Migration
607 Bark-WAVs nach `assets/audio/bark/` verschoben. composeApp = asset-frei.

### 6b — Directional Sprite Rows
`SpriteLoader.loadAllRows()` liefert alle 4 Reihen eines CraftPix-Grid-Sheets.
`CharacterSprite` wählt die Reihe passend zu `Facing` (DOWN=0, UP=1, RIGHT=2, LEFT=3).
Fallback auf scaleX-Flip nur bei <4 Reihen.

### 6c — Bark-Pipeline mit Audio
- WorldScene: `SliceDirector` + `enterRoom()` + `fireBark()` + Bark-WAV-Playback
- BattleScene: `sliceDirector.combatAction()` + `BarkTriggered`-Events → WAV
- Pfad: `"assets/audio/" + BarkAudioRegistry.pathFor(event)` — `:core` unverändert

### 6d — Scripted Playthrough
ScreenshotHarness: 8 Screenshots (3 Original + 5 Scripted Playthrough).
Verifiziert via Mesa EGL in Sandbox.
