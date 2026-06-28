# Screenshots

All KorGE shots below are rendered headlessly via the offscreen GL screenshot
harness — no window, no manual capture. Reproduce with:

```bash
bash scripts/setup-gl.sh          # one-time: install Mesa EGL (headless GL)
./gradlew :game:screenshot        # writes build/screenshots/*.png
```

The harness (`game/src/desktopMain/kotlin/game/ScreenshotHarness.kt`) renders the
real game classes (`TiledMapView`, `CharacterSprite`, `HudOverlay`, `DialogOverlay`,
the `shader.*` filters) through KorGE's offscreen renderer, so these are genuine
engine output, not mockups.

## KorGE engine (Step 5b — world layer)

| File | Scene |
|---|---|
| `step5b-interior.png` | Heroes' Home interior — tilemap, player + NPCs (barkeep, patron), HUD, dialog box |
| `step5b-exterior.png` | Village exterior — outdoor tilemap, player + guard NPC, HUD |
| `step5b-battle.png` | Battle — swordsman vs vampire, HP bars, controls |

> These first real renders also surfaced two runtime bugs that compile-only CI had
> hidden (see `docs/KNOWN_BUGS.md` B005 sprite-sheet slicing, B006 asset resolution) —
> both fixed before these shots were taken.

## KorGE engine (Step 6b/c/d — directional sprites + scripted playthrough)

| File | Scene |
|---|---|
| `step6-interior-dialog.png` | Tavern interior with the barkeep dialog open ("Spend some coin or get out.") |
| `step6-exterior-dialog.png` | Village exterior with the guard dialog open ("Who goes there?") |
| `step6-battle-midway.png` | Combat mid-fight — vampire at 36/60 HP |
| `step6-battle-victory.png` | Combat won — VICTORY!, vampire at 0/60 HP |

## KorGE engine (Step 7a — screen-space shader effects)

GLSL `ShaderFilter` effects rendered over the world. See
[`docs/HORROR_SHADER_CONCEPT.md`](../HORROR_SHADER_CONCEPT.md) and
[`docs/SHADER_GAME_CONCEPT.md`](../SHADER_GAME_CONCEPT.md) for the design intent.

| File | Effect |
|---|---|
| `step7a-shader-poison.png` | Chromatic aberration + vignette tunneling (poison) |
| `step7a-shader-beer-goggle.png` | Box blur + warm amber tint + sway (drunk) |
| `step7a-shader-lighting.png` | Point lights with flicker + ambient darkness |
| `step7a-shader-rain.png` | Procedural rain streaks with wind |
| `step7a-shader-heat-shimmer.png` | Sinusoidal UV heat distortion |

## Compose engine (pre-migration, historical)

`01_title_screen` … `07_combat_rat_fight`, `world_*` — the interim Compose-Canvas
gameplay engine, retained for the migration record.
