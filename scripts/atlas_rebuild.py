#!/usr/bin/env python3
"""atlas_rebuild.py — rebuild a tile atlas at a new native tile size.

The slice renders `tileset_dungeon.png` (a 16px, 12-column atlas) by blitting
tiles scaled 3× to 48px. This tool re-bakes an atlas so each tile is *native*
48px, preserving the column/row grid and therefore every `atlasIndex` used in
`core/.../rpg/world/GameMaps.kt` (48 = floor, 14 = wall, 45 = door, ...).

Two modes:

1. UPSCALE an existing atlas in place (nearest-neighbour 16->48):
   python3 scripts/atlas_rebuild.py upscale \
       composeApp/src/commonMain/composeResources/drawable/tileset_dungeon.png \
       -o out/tileset_dungeon_48.png

2. PACK individual 48px tile PNGs (from assets/HD/...) into an atlas, placing
   each at a chosen index so existing maps keep working:
   python3 scripts/atlas_rebuild.py pack --columns 12 --tile 48 \
       --place 48=assets/HD/tilesets/dungeon/PNG/floor.png \
       --place 14=assets/HD/tilesets/dungeon/PNG/wall.png \
       --place 45=assets/HD/tilesets/dungeon/PNG/door.png \
       -o out/tileset_dungeon_48.png

The grid (columns × rows) is preserved/derived so indices are stable. Empty
cells stay transparent.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("Pillow is required: pip install Pillow")

LEGACY_TILE = 16
TARGET_TILE = 48


def cmd_upscale(args: argparse.Namespace) -> int:
    src_tile = args.src_tile
    dst_tile = args.tile
    atlas = Image.open(args.input).convert("RGBA")
    cols = atlas.width // src_tile
    rows = atlas.height // src_tile
    if cols == 0 or rows == 0:
        sys.exit(f"atlas {atlas.size} smaller than one {src_tile}px tile")
    out = Image.new("RGBA", (cols * dst_tile, rows * dst_tile), (0, 0, 0, 0))
    for r in range(rows):
        for c in range(cols):
            box = (c * src_tile, r * src_tile, (c + 1) * src_tile, (r + 1) * src_tile)
            tile = atlas.crop(box).resize((dst_tile, dst_tile), Image.NEAREST)
            out.paste(tile, (c * dst_tile, r * dst_tile))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    out.save(args.out)
    print(f"upscaled atlas: {cols}x{rows} tiles, {src_tile}px -> {dst_tile}px -> {args.out} ({out.size[0]}x{out.size[1]})")
    return 0


def cmd_pack(args: argparse.Namespace) -> int:
    tile = args.tile
    cols = args.columns
    placements: dict[int, Path] = {}
    for spec in args.place:
        if "=" not in spec:
            sys.exit(f"--place expects INDEX=path, got {spec!r}")
        idx_s, path_s = spec.split("=", 1)
        placements[int(idx_s)] = Path(path_s)
    if not placements:
        sys.exit("pack needs at least one --place INDEX=path")
    max_idx = max(placements)
    rows = args.rows or (max_idx // cols + 1)
    out = Image.new("RGBA", (cols * tile, rows * tile), (0, 0, 0, 0))
    for idx, path in sorted(placements.items()):
        img = Image.open(path).convert("RGBA")
        if img.size != (tile, tile):
            img = img.resize((tile, tile), Image.NEAREST)
        c, r = idx % cols, idx // cols
        out.paste(img, (c * tile, r * tile))
        print(f"  index {idx} -> ({c},{r})  {path.name}")
    args.out.parent.mkdir(parents=True, exist_ok=True)
    out.save(args.out)
    print(f"packed atlas: {cols}x{rows} tiles @ {tile}px -> {args.out} ({out.size[0]}x{out.size[1]})")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description="Rebuild a tile atlas at a new native tile size.")
    sub = ap.add_subparsers(dest="cmd", required=True)

    up = sub.add_parser("upscale", help="nearest-neighbour upscale an existing atlas")
    up.add_argument("input", type=Path)
    up.add_argument("-o", "--out", type=Path, required=True)
    up.add_argument("--src-tile", type=int, default=LEGACY_TILE)
    up.add_argument("--tile", type=int, default=TARGET_TILE)
    up.set_defaults(func=cmd_upscale)

    pk = sub.add_parser("pack", help="pack individual tile PNGs into an indexed atlas")
    pk.add_argument("-o", "--out", type=Path, required=True)
    pk.add_argument("--columns", type=int, default=12)
    pk.add_argument("--rows", type=int, default=0, help="0 = derive from highest index")
    pk.add_argument("--tile", type=int, default=TARGET_TILE)
    pk.add_argument("--place", action="append", default=[], metavar="INDEX=path")
    pk.set_defaults(func=cmd_pack)

    args = ap.parse_args()
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
