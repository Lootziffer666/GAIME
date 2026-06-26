#!/usr/bin/env python3
"""tmx_render.py — bake an artist-authored Tiled .tmx scene into a flat PNG.

The CraftPix packs ship professionally-composed scenes as Tiled `.tmx` maps
(multi-layer, multi-tileset, "infinite"/chunked, 16px). This tool flattens such
a scene into one (or two) PNG(s) the game can blit cheaply, plus a collision
grid derived from the "solid" layers, plus a small meta JSON. That lets us drop
in Chrono-Trigger-class environment art while keeping the existing movement /
trigger / entity logic on top.

Handles:
- infinite maps: layer data in 16x16 <chunk> blocks with negative origins
- multiple <tileset firstgid=.. columns=.. > with external <image source=..>
- gid flip bits (H/V/diagonal)
- layer draw order (document order)
- animated tiles: the base tile image is used (animation ignored for a still)

Usage:
  python3 scripts/tmx_render.py MAP.tmx --out OUTDIR --name tavern \
      --solid Walls --solid Boxes --solid Objects --solid Objects2 --solid Windows \
      [--overhead Objects2]   # layers drawn ABOVE the player (occlusion)

Outputs (in OUTDIR):
  <name>.png            flattened scene (ground + below-player layers)
  <name>_overhead.png   only if --overhead given (drawn after entities)
  <name>.json           { tileSize, cols, rows, blocked: [[x,y],...] }
"""
from __future__ import annotations

import argparse
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

try:
    from PIL import Image
except ImportError:  # pragma: no cover
    sys.exit("Pillow required: pip install Pillow")

FLIP_H = 0x80000000
FLIP_V = 0x40000000
FLIP_D = 0x20000000
GID_MASK = 0x1FFFFFFF


class Tileset:
    def __init__(self, firstgid: int, columns: int, tilecount: int, tw: int, th: int, image: Image.Image):
        self.firstgid = firstgid
        self.columns = columns
        self.tilecount = tilecount
        self.tw = tw
        self.th = th
        self.image = image

    def contains(self, gid: int) -> bool:
        return self.firstgid <= gid < self.firstgid + self.tilecount

    def tile(self, gid: int) -> Image.Image:
        local = gid - self.firstgid
        col = local % self.columns
        row = local // self.columns
        box = (col * self.tw, row * self.th, (col + 1) * self.tw, (row + 1) * self.th)
        return self.image.crop(box)


def load_tilesets(root: ET.Element, base: Path) -> list[Tileset]:
    out: list[Tileset] = []
    for ts in root.findall("tileset"):
        firstgid = int(ts.get("firstgid"))
        img_el = ts.find("image")
        if img_el is None:
            # external .tsx not expected in these packs; skip gracefully
            continue
        src = base / img_el.get("source")
        image = Image.open(src).convert("RGBA")
        columns = int(ts.get("columns") or (image.width // int(ts.get("tilewidth"))))
        tilecount = int(ts.get("tilecount") or (columns * (image.height // int(ts.get("tileheight")))))
        out.append(Tileset(firstgid, columns, tilecount,
                           int(ts.get("tilewidth")), int(ts.get("tileheight")), image))
    out.sort(key=lambda t: t.firstgid)
    return out


def tileset_for(tilesets: list[Tileset], gid: int) -> Tileset | None:
    chosen = None
    for ts in tilesets:
        if ts.firstgid <= gid:
            chosen = ts
    return chosen if chosen and chosen.contains(gid) else chosen


def parse_layer_cells(layer: ET.Element):
    """Yield (gx, gy, raw_gid) for every non-empty cell, handling chunks."""
    data = layer.find("data")
    if data is None:
        return
    if data.get("encoding") != "csv":
        raise SystemExit("only csv-encoded layers are supported")
    chunks = data.findall("chunk")
    if chunks:
        for ch in chunks:
            cx, cy = int(ch.get("x")), int(ch.get("y"))
            cw = int(ch.get("width"))
            vals = [v for v in ch.text.replace("\n", "").split(",") if v.strip() != ""]
            for i, v in enumerate(vals):
                gid = int(v)
                if gid != 0:
                    yield cx + (i % cw), cy + (i // cw), gid
    else:
        w = int(layer.get("width"))
        vals = [v for v in data.text.replace("\n", "").split(",") if v.strip() != ""]
        for i, v in enumerate(vals):
            gid = int(v)
            if gid != 0:
                yield i % w, i // w, gid


def apply_flips(tile: Image.Image, gid: int) -> Image.Image:
    if gid & FLIP_D:
        tile = tile.transpose(Image.TRANSPOSE)
    if gid & FLIP_H:
        tile = tile.transpose(Image.FLIP_LEFT_RIGHT)
    if gid & FLIP_V:
        tile = tile.transpose(Image.FLIP_TOP_BOTTOM)
    return tile


def main() -> int:
    ap = argparse.ArgumentParser(description="Bake a Tiled .tmx scene to a flat PNG + collision.")
    ap.add_argument("tmx", type=Path)
    ap.add_argument("--out", type=Path, required=True, help="output directory")
    ap.add_argument("--name", required=True, help="basename for outputs")
    ap.add_argument("--floor", action="append", default=[], help="walkable floor layer (repeatable). If given, collision = (cells without floor) + solid layers.")
    ap.add_argument("--solid", action="append", default=[], help="layer name that blocks movement (repeatable)")
    ap.add_argument("--overhead", action="append", default=[], help="layer drawn above the player (repeatable)")
    args = ap.parse_args()

    root = ET.parse(args.tmx).getroot()
    base = args.tmx.parent
    tw = int(root.get("tilewidth"))
    th = int(root.get("tileheight"))
    tilesets = load_tilesets(root, base)

    layers = [l for l in root.iter("layer")]

    # 1) bounding box over all non-empty cells across all layers
    minx = miny = 10**9
    maxx = maxy = -10**9
    any_cell = False
    for layer in layers:
        for gx, gy, _ in parse_layer_cells(layer):
            any_cell = True
            minx, miny = min(minx, gx), min(miny, gy)
            maxx, maxy = max(maxx, gx), max(maxy, gy)
    if not any_cell:
        raise SystemExit("map has no tiles")
    cols = maxx - minx + 1
    rows = maxy - miny + 1

    base_img = Image.new("RGBA", (cols * tw, rows * th), (0, 0, 0, 0))
    over_img = Image.new("RGBA", (cols * tw, rows * th), (0, 0, 0, 0))
    blocked: set[tuple[int, int]] = set()
    floor_cells: set[tuple[int, int]] = set()
    solid = set(args.solid)
    floor = set(args.floor)
    overhead = set(args.overhead)

    # 2) draw layers in document order; collect collision + floor coverage
    for layer in layers:
        name = layer.get("name")
        target = over_img if name in overhead else base_img
        is_solid = name in solid
        is_floor = name in floor
        for gx, gy, raw in parse_layer_cells(layer):
            gid = raw & GID_MASK
            ts = tileset_for(tilesets, gid)
            if ts is None:
                continue
            tile = apply_flips(ts.tile(gid), raw)
            px = (gx - minx) * tw
            py = (gy - miny) * th
            target.alpha_composite(tile, (px, py))
            cell = (gx - minx, gy - miny)
            if is_solid:
                blocked.add(cell)
            if is_floor:
                floor_cells.add(cell)

    # Floor-based collision: anything without a walkable floor tile is blocked,
    # plus any explicitly-solid furniture. Falls back to solid-only if no floor given.
    if floor:
        for y in range(rows):
            for x in range(cols):
                if (x, y) not in floor_cells:
                    blocked.add((x, y))

    args.out.mkdir(parents=True, exist_ok=True)
    base_path = args.out / f"{args.name}.png"
    base_img.save(base_path)
    outputs = [str(base_path)]
    if overhead:
        over_path = args.out / f"{args.name}_overhead.png"
        over_img.save(over_path)
        outputs.append(str(over_path))

    meta = {
        "tileSize": tw,
        "cols": cols,
        "rows": rows,
        "blocked": sorted([list(b) for b in blocked]),
    }
    meta_path = args.out / f"{args.name}.json"
    meta_path.write_text(json.dumps(meta))

    # collision grid as compact text ('#' blocked, '.' walkable) — easy to embed
    # in Kotlin / diff in review, no JSON dependency needed at runtime.
    grid_lines = [
        "".join('#' if (x, y) in blocked else '.' for x in range(cols))
        for y in range(rows)
    ]
    grid_path = args.out / f"{args.name}.collision.txt"
    grid_path.write_text("\n".join(grid_lines) + "\n")

    # coverage sanity
    bbox = base_img.getbbox()
    opaque = sum(1 for a in base_img.getchannel("A").getdata() if a > 0)
    total = base_img.width * base_img.height
    print(f"baked {args.tmx.name}: {cols}x{rows} tiles -> {base_img.width}x{base_img.height}px")
    print(f"  layers={len(layers)} tilesets={len(tilesets)} solidLayers={sorted(solid)}")
    print(f"  non-empty bbox={bbox}  opaque coverage={opaque/total*100:.1f}%  blocked cells={len(blocked)}")
    print(f"  -> {', '.join(outputs)} + {meta_path.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
