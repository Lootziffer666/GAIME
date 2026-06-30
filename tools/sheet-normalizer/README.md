# Sheet Normalizer

Analyzes sprite sheets (CraftPix format): detects the frame grid via
fully-transparent gutters, measures opaque body bounds per frame, and outputs
a uniform tight frame layout + descriptor JSON for the runtime.

## Requirements

```bash
pip install Pillow numpy
```

## Usage

```bash
# Single sheet
python normalize.py path/to/sheet.png

# Batch (recursive, skips Parts/Tiled/shadow files)
python normalize.py --batch path/to/character/directory

# With grid hints (if auto-detection fails)
python normalize.py --batch path/to/dir --rows 4 --cols 12
```

## Output (per sheet)

Given `Foo.png`, produces:

- `Foo.normalized.png` — re-packed sheet (uniform cells, foot-aligned)
- `Foo.sheet.json` — descriptor for the runtime
- `Foo.normalized.debug.png` — original with grid + anchor markers overlay

## Descriptor Format (`*.sheet.json`)

```json
{
  "frameW": 25,
  "frameH": 25,
  "cols": 12,
  "rows": 4,
  "footAnchorX": 12,
  "footAnchorY": 24,
  "opaqueBodyH": 24,
  "source": "Swordsman_lvl1_Idle_without_shadow.png"
}
```

- `frameW/frameH` — uniform frame cell (covers worst-case pose)
- `cols/rows` — grid layout
- `footAnchorX/Y` — foot position in the frame (for baseline alignment)
- `opaqueBodyH` — body height of the idle reference (for scale normalization)

## How the Runtime Uses This

`SpriteLoader` reads `*.sheet.json` alongside the PNG. If present:
- Uses `frameW/frameH` for slicing (not the hardcoded 64px default)
- `CharacterSprite` uses `opaqueBodyH` for physical scale normalization
- `footAnchorY` positions the character's feet on the grid cell bottom
