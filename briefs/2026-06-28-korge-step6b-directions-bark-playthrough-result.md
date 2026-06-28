# Result: Step 6b/c/d — Directional Sprites, Bark-Pipeline, Scripted Playthrough

**Brief:** `briefs/2026-06-28-korge-step6b-directions-bark-playthrough.md`
**Branch:** `kiro/korge-step6b-directions-bark-playthrough`
**PR:** https://github.com/Lootziffer666/GAIME/pull/40
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

---

## Geliefert

### 6b — Directional Sprite Rows ✅

`SpriteLoader.sliceAllRows(bitmap, frameSize)` und `loadAllRows(assetPath)` hinzugefügt.
`CharacterSprite.animations` ist jetzt `Map<SpriteAnimation, List<List<BmpSlice>>>`.
`Facing.rowIndex()` (DOWN=0, LEFT=1, UP=2, RIGHT=3) und `framesForCurrent()` implementiert.
`updateFacing()` setzt `scaleX` Flip nur noch bei Single-Row-Sheets.
`loadSwordsman()`, `loadVampire()`, `loadFromSheet()` rufen alle `loadAllRows()` auf.

### 6c — Bark-Pipeline mit Audio ✅

`GameAudioPlayer.kt` neu erstellt — implementiert `AudioPlayer` via `CoroutineScope.launch`,
prepended `"assets/audio/"` zu resourcePath.
`NpcDefinition.barkEvent: BarkEvent? = null` hinzugefügt.
`MapConfig`: BARKEEP_SPEND_SOME_COIN, PATRON_HE_SURE_IS_SLOW, GUARD_BACK_ALREADY verdrahtet.
`WorldScene`: SliceDirector + `enterRoom()` + `fireBark()` bei E-key Interaktion.
`BattleScene`: SliceDirector + `director.combatAction()` ersetzt `engine.tick()` + BRUGG_ATTACK.

### 6d — Scripted Playthrough ✅

4 neue Captures in `ScreenshotHarness.kt`:
- `captureInteriorDialog()` → `interior_dialog.png`
- `captureExteriorDialog()` → `exterior_dialog.png`
- `captureBattleMidway()` → `battle_midway.png` (Vampire 36/60 HP)
- `captureBattleVictory()` → `battle_victory.png` (VICTORY!, Vampire 0/60)

**B007 korrekt:** `localCurrentDirVfs` wurde NICHT angerührt. Erstmals kein B007-Revert! ✅

---

## Integration-Anmerkungen

`GameAudioPlayer.play()` ruft `readSound().play()` ohne `.await()` auf — `onComplete` feuert
sofort nach Playback-Start statt nach Ende. Für Fire-and-forget Barks akzeptabel;
könnte in Step 7+ präzisiert werden wenn Bark-Sequenzierung nötig wird.

Remote-Branch `kiro/korge-step6b-directions-bark-playthrough` konnte nicht per CLI gelöscht
werden (403 Proxy). → GitHub Web UI.

---

## Stand nach Merge

main @ `a96f99d2` — 8 Dateien geändert, 269 Insertions, 43 Deletions, 1 neue Datei.

