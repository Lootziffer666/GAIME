# Screenshots

## KorGE engine (Step 5b — world layer)

Rendered headlessly via the offscreen GL screenshot harness — no window, no
manual capture. Reproduce with:

```bash
bash scripts/setup-gl.sh          # one-time: install Mesa EGL (headless GL)
./gradlew :game:screenshot        # writes build/screenshots/{interior,exterior,battle}.png
```

The harness (`game/src/desktopMain/kotlin/game/ScreenshotHarness.kt`) renders the
real game classes (`TiledMapView`, `CharacterSprite`, `HudOverlay`, `DialogOverlay`)
through KorGE's offscreen renderer, so these are genuine engine output, not mockups.

| File | Scene |
|---|---|
| `step5b-interior.png` | Heroes' Home interior — tilemap, player + NPCs (barkeep, patron), HUD, dialog box |
| `step5b-exterior.png` | Village exterior — outdoor tilemap, player + guard NPC, HUD |
| `step5b-battle.png` | Battle — swordsman vs vampire, HP bars, controls |

> These first real renders also surfaced two runtime bugs that compile-only CI had
> hidden (see `docs/KNOWN_BUGS.md` B005 sprite-sheet slicing, B006 asset resolution) —
> both fixed before these shots were taken.

## Compose engine (pre-migration, historical)

`01_title_screen` … `07_combat_rat_fight`, `world_*` — the interim Compose-Canvas
gameplay engine, retained for the migration record.
