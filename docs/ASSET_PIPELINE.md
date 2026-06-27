# Asset Pipeline — what to upload (for absurdly high quality)

This maps the **already-built `:core` systems** to the exact art/audio they need,
so the ~44k-asset library (`treekram.txt`) can be mined precisely instead of
uploaded wholesale. Renderer target: **KorGE 2.5D HD-2D**
(`.kiro/steering/rendering-engine.md`).

## Conventions (so quality stays high and consistent)

- **Pixel art / in-world sprites:** PNG with transparency, kept crisp with
  nearest-neighbour (`smoothing = false`). Prefer packed **sprite atlases**
  (sheet + `.json`/`.xml`) over thousands of loose files.
- **HD close-ups (boss reveals/portraits):** high-res PNG (>= 1024 px tall),
  transparent. The "Parts HD" / "Poses HD" character art in the library is ideal.
- **Audio:** `.ogg` (music/ambience loops), `.wav` (short SFX). Loops must be
  seamless.
- **Target paths in repo:**
  - sprites → `composeApp/src/commonMain/composeResources/files/sprites/...`
    and later `game/src/.../resources/sprites/...` (KorGE).
  - music → `assets/audio/music/<baseName>.ogg`
  - sfx → `assets/audio/sfx/...`
  - fonts → `.../files/fonts/...`
- **Git note:** `.gitignore` ignores `*.png`/`*.ogg`/`*.mp3`. Large binaries
  should go via **Git LFS** (recommended now that art is arriving) or be
  force-added like the existing bark WAVs. Tell me which and I'll set up LFS.

---

## P0 — unblocks a visible vertical slice (Prologue + Ch1 + Ch2 in KorGE)

### Party (Nib, Brugg, Vellum)
Per character: idle, walk (4-dir), attack, cast, hurt, down/heal.
- **In-world:** a top-down RPG character set (the library's adventurer / RPG
  character sheets). 16/32 px, atlas + frame data.
- **HD portrait/close-up** per character (Parts HD / Poses HD) for dialogue and
  the `CLOSE_UP` entrance beats.

### Enemies (Chapters 1-2)
`Sewer Rat`, `Sludge Blob`, `Forest Wolf`, `Kobold Scout`, `Quest Wisp`,
`Pirate Clerk`, `Barrel Mimic`. Top-down sprites (idle + a hit/attack frame).
The library's "Enemy sprites" / zombie/skeleton/orc/ghost sets cover most.

### Boss close-up + reveal sprites
These exact `spriteKey`s are referenced by `rpg.staging.EntranceLibrary` and
must be supplied (close-up = HD, reveal = in-world sprite):

| spriteKey | what | source idea |
|---|---|---|
| `sewer_rat` | tiny reveal for "The Dread Shadow" gag | enemy rat sprite |
| `rat_accountant` + `rat_accountant_spectacles` | boss + spectacles close-up | rat + HD parts (glasses) |
| `guard_captain` | frozen mid-stride boss | HD armored human pose |
| `helpful_tree` | over-helpful tree boss | plant/tree asset, upscaled |
| `captain_formbeard` + `formbeard_beard` | pirate + beard-of-forms close-up | HD pirate pose + paper texture |
| `administragon` + `administragon_eye` | final dragon + glowing eye close-up | dragon/monster art + HD eye |

### Tiles (maps)
- **Tavern interior** (counter, tables, cellar trapdoor), **Sewer** (pipes,
  grates, water, rubble), **Market/Town** (stalls, guardhouse), **Forest trail**.
- Top-down tilesets (the library's `topdownTile_*`, RPG/medieval sheets, PNG
  terrain packs: Grass/Dirt/Sand/Snow). Atlas + collision-friendly grid.

### Audio (P0)
- **Chapter ambience loops** (non-boss): tavern, sewer, market, forest — from
  the library's Audio/Sounds music pack (`battle_mode`, `arcade_mode`, ambient
  loops). Map to chapters; I'll wire a `ChapterAmbience` registry like
  `MusicTrack`.
- **Core SFX:** Questbook stamp, page pickup, quest-accept sting, hit, heal,
  UI open/close, footstep — the repo already has many under `assets/audio/sfx/`;
  send anything notably better.
- **Boss themes** (`title_quest_accepted`, `boss_rat_accountant`,
  `boss_wardens_mandate`, `boss_helpful_tree`, `boss_formbeard`,
  `boss_administragon`): these are your **custom songs**, not from the library —
  drop the rendered `.ogg`s at `assets/audio/music/<baseName>.ogg` (see
  docs/SONGBOOK.md).

### UI + font
- A readable **pixel/bitmap font** + one "official stamp/typewriter" style font
  for Questbook bureaucratese.
- UI frames: dialogue box, Questbook page, quest-pressure meter, quest markers
  (normal + glitchy false marker). The library's UI / "Flairs & Overlays" /
  game-icons packs.

---

## P1 — effect storms, polish, later chapters

- **Effect-storm FX** for `rpg.staging` (`EFFECT_STORM` beats): lightning,
  embers, fog, bloom/glow sprites, confetti, sparkles, sparks, smoke. The
  library's "particles" / Flairs & Overlays packs + radial-glow PNGs for
  additive bloom.
- **Ch3-5 art:** deeper forest, harbour/ship/barrels/crates, island cave + gold
  chamber.
- **Backgrounds** for parallax HD-2D layers (the "Backgrounds" pack).
- **Title/credits art** for the absurd end-credits crawl.

---

## How to send it (minimal effort)

Don't upload everything. Per P0 line above, zip just the relevant **pack
subfolders** and upload those (e.g. one RPG top-down character pack, one enemy
pack, one top-down tileset, the Parts HD / Poses HD character pack, the audio
pack, one UI pack + a font). I'll:
1. extract + organise into the resource layout above,
2. write the atlas/registry glue (`SpriteRegistry`, `ChapterAmbience`) in `:core`,
3. wire it into the renderer.

If you prefer, upload a broad "RPG top-down + HD characters + audio + UI" bundle
and I'll curate down to what the systems reference.
