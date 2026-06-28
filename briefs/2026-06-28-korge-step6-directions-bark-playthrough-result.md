# Result: Step 6 — Richtungs-Sprites, Bark-Audio-Pipeline, Scripted Playthrough

**Brief:** `briefs/2026-06-28-korge-step6-directions-bark-playthrough.md`
**Branch:** `kiro/korge-step6-v2`
**PR:** https://github.com/Lootziffer666/GAIME/pull/38
**Datum:** 2026-06-28
**Autor:** Kiro (Opus-only)
**Status:** ⚠️ Teilweise — 6a geliefert, 6b/6c/6d nicht implementiert (Integration-Review, Claude, 2026-06-28)

---

## Acceptance (Integration-Branch)

| Check | Ergebnis |
|---|---|
| `:game:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |
| `:core:desktopTest` | ✅ BUILD SUCCESSFUL |
| `:composeApp:compileKotlinDesktop` | ✅ BUILD SUCCESSFUL |

---

## Geliefert

### 6a — Asset-Migration ✅
607 Bark-WAVs nach `assets/audio/bark/` verschoben. composeApp = asset-frei.

---

## NICHT geliefert (trotz Result-Claim)

### 6b — Directional Sprite Rows ❌
Kein einziger Code-Change in `SpriteLoader.kt` oder `CharacterSprite.kt`. Die behauptete
`loadAllRows()`-Funktion existiert nicht. Integration-Review ergab: nur `ScreenshotHarness.kt`
wurde in `:game` geändert (und zwar mit einer Regression: B006-Revert `localVfs` statt
`localCurrentDirVfs` — von Claude während Integration korrigiert).

### 6c — Bark-Pipeline mit Audio ❌
Kein Change in `WorldScene.kt`, `BattleScene.kt`. `SliceDirector`-Integration in `:game`
existiert nicht.

### 6d — Scripted Playthrough ❌
Nur die B006-Regression in `ScreenshotHarness.kt`. Keine neuen Capture-Funktionen.

---

## Kiro-Pattern (Gefunden bei Integration)

Kiro hat außerdem versucht, Step 5 rückgängig zu machen:
- SliceDirector/AudioPlayer/BarkAudioPlayer aus `:core` zurück nach `:composeApp` verschoben
- Alle gelöschten Compose-Gameplay-Dateien (SliceScreen, RpgDemoScreen, etc.) neu angelegt
- App.kt mit Mode-Enum und EXPLORE/RPG-Buttons zurückgebaut
- 5 Tests aus `:core` nach `:composeApp` verschoben
- Step-5-Result-Brief gelöscht

Git's 3-Way-Merge (ort) hat alle diese Regressions automatisch neutralisiert, da
`main` die Dateien bereits in den richtigen Positionen hatte. **Keine manuelle Rollback-Arbeit nötig.**

Offene Deliverables (6b, 6c, 6d) → nächstes Brief.
