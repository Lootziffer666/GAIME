# `assets/HD/` — HD pixel-art source library

Curated, **48 px-target** art sourced from 30 free CraftPix / GandalfHardcore
packs. Each pack keeps its original `license.txt` / `readme.txt` for attribution.
Vendor promo files (`COUPON.*`, `*.url`) were stripped; heavy editing sources
(`*.psd`) are `.gitignore`d (recoverable from the original zips). Game-usable
`*.png` and the Tiled `*.tmx/.tsx` maps are tracked via **Git LFS**
(see `/.gitattributes`).

> Pipeline + naming conventions: `docs/ASSET_PIPELINE.md`.
> 16 px → 48 px rebuild workflow: `docs/HD48_MIGRATION.md`.

## Layout

```
assets/HD/
├── characters/   playable / NPC humanoid sprite sheets (4-dir, anim states)
│   ├── swordsman          → party in-world sprites (Nib / Brugg / Vellum base)
│   └── vampire            → robed 4-direction caster (Vellum / undead NPC)
├── enemies/      hostile mobs (idle + hit/attack frames)
│   ├── rpg-monsters       → demon/orc/etc. — Ch1-2 boss reveals & elites
│   ├── slimes             → Sludge Blob (Ch1)
│   └── field-enemies      → Sewer Rat / Kobold Scout / Quest Wisp fodder
├── creatures/    neutral / ambient fauna
│   ├── farm-animals       → Stokeport market & tavern ambience
│   └── hunt-animals       → Forest Trail wildlife (Ch2)
├── tilesets/     terrain & structure atlases (the 16→48 atlas sources)
│   ├── dungeon            → Sewer / boss arena floors, walls, doors
│   ├── village            → Stokeport Market / town
│   ├── fields             → Forest Trail ground
│   ├── paths              → roads / trails connecting areas
│   └── platformer         → GandalfHardcore-style parallax (P1)
├── locations/    themed top-down building/interior props
│   ├── ruined-temple      → forest shrine (Ch2)
│   ├── chapel             → holy-wisdom gag set
│   ├── guild-hall         → questbook HQ
│   ├── glassblowers-workshop → market shop interior
│   ├── heroes-home        → tavern interior (The Limping Cockatrice)
│   └── bridges            → map connectors
├── props/        interactables & set dressing
│   ├── dungeon-objects    → barrels, crates, chests (Barrel Mimic)
│   ├── dungeon-props      → rubble, pipes, grates (Sewer)
│   ├── plants             → market produce / forest flora
│   ├── magic-and-traps    → effect-storm FX, hazard tiles (P1)
│   └── magic-book         → animated Questbook artefact
├── ui/           HUD / dialogue / icon frames
│   ├── rpg-ui             → dialogue box, quest-pressure meter, panels
│   ├── fantasy-icons      → 16×16 item/quest icons
│   ├── pirate-icons       → Ch3 harbour iconography
│   └── npc-avatars        → dialogue portraits (medieval NPCs)
├── kits/         broad multi-purpose game kits (mine selectively)
│   ├── roguelike-topdown  → catch-all top-down tiles/sprites/UI
│   └── roguelike-shmup    → projectiles, sparks, explosions (FX, P1)
└── platformer/
    └── gandalf-hardcore   → platformer tileset/props (HD-2D backdrops, P1)
```

## How this feeds the game

The in-world renderer (`composeApp/.../ui/rpg/WorldScene.kt`) blits a tile
atlas and per-entity sprites at **48 px** (3× of the legacy 16 px placeholders).
To replace a placeholder with HD art:

1. Pick the source frame(s) from the relevant pack folder above.
2. Run the rebuild tooling in `docs/HD48_MIGRATION.md`
   (`scripts/asset_upscale.py`, `scripts/atlas_rebuild.py`) to land a native
   48 px PNG / atlas.
3. Drop it into `composeApp/src/commonMain/composeResources/drawable/` and point
   the `SpriteRegistry` / `Res.drawable.*` reference at it.

Per-pack mapping decisions are tracked in `scripts/hd48_manifest.json`.
