#!/usr/bin/env python3
"""asset_upscale.py — 16px -> 48px (or any N×) nearest-neighbour upscaler.

Template tool for the "rebuild legacy 16px art to native 48px" workflow
(docs/HD48_MIGRATION.md). Nearest-neighbour keeps pixel-art edges crisp, which
matches the renderer's FilterQuality.None blitting in WorldScene.

Examples
--------
# Upscale a single 16px tile to 48px (3x):
python3 scripts/asset_upscale.py tile_floor.png -o out/tile_floor.png

# Force an exact output edge (e.g. 48) regardless of source size:
python3 scripts/asset_upscale.py hero_nib.png --to 48 -o out/hero_nib.png

# Batch a whole folder (recursively), mirroring the tree into out/:
python3 scripts/asset_upscale.py assets/HD/ui/fantasy-icons --batch -o out/icons48

Defaults: scale factor = RenderMetrics.UPSCALE (3). Use --scale or --to.
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
DEFAULT_SCALE = TARGET_TILE // LEGACY_TILE  # 3 — mirrors core RenderMetrics


def upscale_image(src: Path, dst: Path, scale: int | None, to_edge: int | None) -> tuple[int, int, int, int]:
    img = Image.open(src).convert("RGBA")
    w, h = img.size
    if to_edge is not None:
        # Scale so the *longest* edge becomes `to_edge`, preserving aspect & integer-ness where possible.
        factor = max(1, round(to_edge / max(w, h)))
        new_w, new_h = w * factor, h * factor
        if max(new_w, new_h) != to_edge:
            # Fall back to an exact resize to the requested square-ish edge.
            new_w = to_edge if w >= h else round(w / h * to_edge)
            new_h = to_edge if h >= w else round(h / w * to_edge)
    else:
        s = scale or DEFAULT_SCALE
        new_w, new_h = w * s, h * s
    out = img.resize((new_w, new_h), Image.NEAREST)
    dst.parent.mkdir(parents=True, exist_ok=True)
    out.save(dst)
    return w, h, new_w, new_h


def main() -> int:
    ap = argparse.ArgumentParser(description="Nearest-neighbour pixel-art upscaler (16->48 by default).")
    ap.add_argument("input", type=Path, help="source PNG file or folder (with --batch)")
    ap.add_argument("-o", "--out", type=Path, required=True, help="output file (single) or folder (batch)")
    ap.add_argument("--scale", type=int, help=f"integer scale factor (default {DEFAULT_SCALE})")
    ap.add_argument("--to", type=int, dest="to_edge", help="target longest-edge px (e.g. 48)")
    ap.add_argument("--batch", action="store_true", help="recurse a folder of PNGs, mirroring the tree")
    args = ap.parse_args()

    if args.batch:
        if not args.input.is_dir():
            return f"--batch needs a directory, got {args.input}" and 2
        pngs = sorted(args.input.rglob("*.png"))
        if not pngs:
            print(f"no PNGs under {args.input}")
            return 0
        for p in pngs:
            rel = p.relative_to(args.input)
            dst = args.out / rel
            ow, oh, nw, nh = upscale_image(p, dst, args.scale, args.to_edge)
            print(f"{rel}: {ow}x{oh} -> {nw}x{nh}")
        print(f"done: {len(pngs)} files -> {args.out}")
    else:
        ow, oh, nw, nh = upscale_image(args.input, args.out, args.scale, args.to_edge)
        print(f"{args.input.name}: {ow}x{oh} -> {nw}x{nh}  ({args.out})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
