#!/usr/bin/env python3
"""
Sheet Normalizer — Gutter-detect frames, emit universal grid + descriptor.

Analyzes sprite sheets to find the actual frame grid (via fully-transparent
gutter columns/rows), measures the opaque body bounds per frame, computes a
uniform tight frame size + foot-anchor, and outputs:
  - <name>.normalized.png  — re-packed sheet (uniform cells, foot-aligned)
  - <name>.sheet.json      — descriptor (frameW, frameH, cols, rows, anchors)
  - <name>.normalized.debug.png — original with grid/anchor markers drawn

Usage:
  python normalize.py <sheet.png>
  python normalize.py --batch <directory>
  python normalize.py --batch <directory> --rows 4 --cols 12
"""

import argparse
import json
import sys
from pathlib import Path
from typing import Optional, Tuple, List

import numpy as np
from PIL import Image, ImageDraw


def find_grid(alpha: np.ndarray, hint_rows: Optional[int] = None, hint_cols: Optional[int] = None):
    """
    Detect the frame grid from fully-transparent gutter columns and rows.
    
    Returns (cols, rows, col_boundaries, row_boundaries) where boundaries are
    lists of (start, end) pixel ranges for each frame cell.
    
    If hints are provided AND gutter detection is ambiguous, uses hints.
    Returns None if detection fails and no hints given.
    """
    h, w = alpha.shape
    
    # Find fully-transparent columns (no opaque pixel in entire height)
    cols_opaque = np.any(alpha > 20, axis=0)
    # Find fully-transparent rows (no opaque pixel in entire width)
    rows_opaque = np.any(alpha > 20, axis=1)
    
    col_boundaries = _find_content_runs(cols_opaque, w)
    row_boundaries = _find_content_runs(rows_opaque, h)
    
    if not col_boundaries or not row_boundaries:
        # No content at all
        return None
    
    # Validate: are the content regions uniformly spaced?
    # If we find e.g. 12 content columns of similar width → that's our grid
    detected_cols = len(col_boundaries)
    detected_rows = len(row_boundaries)
    
    # Check if boundaries are roughly uniform (tolerance: ±4px)
    col_widths = [e - s + 1 for s, e in col_boundaries]
    row_heights = [e - s + 1 for s, e in row_boundaries]
    
    # If widths/heights vary too much, the grid detection might be wrong
    # (e.g., a dense packed sheet without clear gutters)
    col_variance = max(col_widths) - min(col_widths) if col_widths else 0
    row_variance = max(row_heights) - min(row_heights) if row_heights else 0
    
    # Heuristic: if content regions are too far apart or too varied, it's not
    # a clean grid. Try fixed-size detection based on sheet dimensions.
    if col_variance > 10 or row_variance > 10:
        # Try regular grid detection: assume square cells
        # Use the first content region height as reference
        if hint_cols and hint_rows:
            cell_w = w // hint_cols
            cell_h = h // hint_rows
            col_boundaries = [(i * cell_w, (i + 1) * cell_w - 1) for i in range(hint_cols)]
            row_boundaries = [(i * cell_h, (i + 1) * cell_h - 1) for i in range(hint_rows)]
            detected_cols = hint_cols
            detected_rows = hint_rows
        else:
            # Try common frame sizes (64, 48, 32, 128, 96)
            result = _try_regular_grid(alpha, w, h)
            if result:
                return result
            return None
    
    # Expand boundaries to cover the full cell (including gutters split evenly)
    col_boundaries = _expand_to_cells(col_boundaries, w)
    row_boundaries = _expand_to_cells(row_boundaries, h)
    
    return detected_cols, detected_rows, col_boundaries, row_boundaries


def _find_content_runs(opaque_mask: np.ndarray, total_size: int):
    """Find contiguous runs of True (content) values."""
    runs = []
    in_content = False
    start = 0
    for i in range(total_size):
        if opaque_mask[i] and not in_content:
            start = i
            in_content = True
        elif not opaque_mask[i] and in_content:
            runs.append((start, i - 1))
            in_content = False
    if in_content:
        runs.append((start, total_size - 1))
    return runs


def _expand_to_cells(boundaries, total_size):
    """
    Given content boundaries, compute uniform cell boundaries that divide
    the full width/height evenly based on the number of detected cells.
    """
    n = len(boundaries)
    if n == 0:
        return []
    cell_size = total_size // n
    return [(i * cell_size, (i + 1) * cell_size - 1) for i in range(n)]


def _try_regular_grid(alpha, w, h):
    """Try common cell sizes to see if the sheet divides evenly."""
    for cell_size in [64, 48, 32, 128, 96, 80]:
        if w % cell_size == 0 and h % cell_size == 0:
            cols = w // cell_size
            rows = h // cell_size
            # Verify that at least half the cells have content
            occupied = 0
            for r in range(rows):
                for c in range(cols):
                    cell_alpha = alpha[r * cell_size:(r + 1) * cell_size,
                                       c * cell_size:(c + 1) * cell_size]
                    if np.any(cell_alpha > 20):
                        occupied += 1
            if occupied > (cols * rows) * 0.3:
                col_boundaries = [(i * cell_size, (i + 1) * cell_size - 1) for i in range(cols)]
                row_boundaries = [(i * cell_size, (i + 1) * cell_size - 1) for i in range(rows)]
                return cols, rows, col_boundaries, row_boundaries
    return None


def measure_opaque_bounds(frame_rgba: np.ndarray):
    """
    Measure the opaque bounding box within a frame.
    Returns (left, top, right, bottom) or None if empty.
    Coordinates are relative to the frame's top-left.
    """
    alpha = frame_rgba[:, :, 3]
    opaque = alpha > 20
    if not np.any(opaque):
        return None
    
    rows_with = np.any(opaque, axis=1)
    cols_with = np.any(opaque, axis=0)
    
    top = int(np.argmax(rows_with))
    bottom = len(rows_with) - int(np.argmax(rows_with[::-1])) - 1
    left = int(np.argmax(cols_with))
    right = len(cols_with) - int(np.argmax(cols_with[::-1])) - 1
    
    return left, top, right, bottom


def normalize_sheet(sheet_path: Path, hint_rows=None, hint_cols=None):
    """
    Normalize a single sprite sheet.
    
    Returns the descriptor dict on success, or None on failure (with message).
    """
    img = Image.open(sheet_path).convert("RGBA")
    arr = np.array(img)
    alpha = arr[:, :, 3]
    h, w = alpha.shape
    
    name = sheet_path.stem
    out_dir = sheet_path.parent
    
    # Step 1: Detect grid
    grid = find_grid(alpha, hint_rows, hint_cols)
    if grid is None:
        print(f"  SKIP: {sheet_path.name} — cannot determine grid (provide --rows/--cols)")
        return None
    
    num_cols, num_rows, col_bounds, row_bounds = grid
    print(f"  Grid: {num_cols}x{num_rows} frames")
    
    # Step 2: Measure opaque bounds per frame (row 0 only for reference)
    frame_bounds = []
    for row_idx in range(num_rows):
        ry_start, ry_end = row_bounds[row_idx]
        for col_idx in range(num_cols):
            cx_start, cx_end = col_bounds[col_idx]
            frame = arr[ry_start:ry_end + 1, cx_start:cx_end + 1]
            bounds = measure_opaque_bounds(frame)
            if bounds:
                frame_bounds.append(bounds)
    
    if not frame_bounds:
        print(f"  SKIP: {sheet_path.name} — no opaque content found in frames")
        return None
    
    # Step 3: Compute union bbox across ALL frames (tight, covers worst-case pose)
    all_lefts = [b[0] for b in frame_bounds]
    all_tops = [b[1] for b in frame_bounds]
    all_rights = [b[2] for b in frame_bounds]
    all_bottoms = [b[3] for b in frame_bounds]
    
    union_left = min(all_lefts)
    union_top = min(all_tops)
    union_right = max(all_rights)
    union_bottom = max(all_bottoms)
    
    # Uniform frame size = union bbox dimensions (covers all poses)
    frame_w = union_right - union_left + 1
    frame_h = union_bottom - union_top + 1
    
    # Step 4: Compute foot anchor (baseline alignment)
    # Foot = opaque bottom edge. We align all frames so their opaque bottom
    # sits at the same Y position in the output frame.
    # The foot anchor Y in output = frame_h - 1 (bottom of output cell)
    # foot anchor X = center of the body (average center X)
    body_centers_x = [(b[0] + b[2]) / 2 for b in frame_bounds]
    avg_center_x = sum(body_centers_x) / len(body_centers_x)
    foot_anchor_x = int(round(avg_center_x - union_left))
    foot_anchor_y = frame_h - 1  # bottom edge of the uniform cell
    
    # Measure body height from IDLE reference (row 0, frame 0) for scale normalization
    # The "opaque body height" that the runtime uses for scaling
    idle_bounds = None
    ry_start, ry_end = row_bounds[0]
    cx_start, cx_end = col_bounds[0]
    idle_frame = arr[ry_start:ry_end + 1, cx_start:cx_end + 1]
    idle_bounds = measure_opaque_bounds(idle_frame)
    opaque_body_h = (idle_bounds[3] - idle_bounds[1] + 1) if idle_bounds else frame_h
    
    print(f"  Union bbox: {frame_w}x{frame_h}, body height (idle): {opaque_body_h}")
    print(f"  Foot anchor: ({foot_anchor_x}, {foot_anchor_y})")
    
    # Step 5: Build normalized sheet (foot-aligned, uniform cells)
    out_img = Image.new("RGBA", (frame_w * num_cols, frame_h * num_rows), (0, 0, 0, 0))
    
    for row_idx in range(num_rows):
        ry_start, ry_end = row_bounds[row_idx]
        for col_idx in range(num_cols):
            cx_start, cx_end = col_bounds[col_idx]
            frame = arr[ry_start:ry_end + 1, cx_start:cx_end + 1]
            bounds = measure_opaque_bounds(frame)
            if bounds is None:
                continue
            
            fl, ft, fr, fb = bounds
            # Crop to opaque region
            opaque_region = frame[ft:fb + 1, fl:fr + 1]
            opaque_img = Image.fromarray(opaque_region)
            
            # Place in output cell: align bottom edge (foot) to foot_anchor_y
            # and center horizontally relative to the avg body center
            opaque_w = fr - fl + 1
            opaque_h = fb - ft + 1
            
            # Horizontal: center the opaque region around foot_anchor_x
            opaque_center_x = (fl + fr) / 2 - union_left
            paste_x = int(round(opaque_center_x - opaque_w / 2))
            paste_x = max(0, min(paste_x, frame_w - opaque_w))
            
            # Vertical: align opaque bottom to foot_anchor_y
            paste_y = foot_anchor_y - opaque_h + 1
            paste_y = max(0, min(paste_y, frame_h - opaque_h))
            
            # Paste into output
            out_x = col_idx * frame_w + paste_x
            out_y = row_idx * frame_h + paste_y
            out_img.paste(opaque_img, (out_x, out_y))
    
    # Save normalized sheet
    normalized_path = out_dir / f"{name}.normalized.png"
    out_img.save(normalized_path)
    print(f"  Saved: {normalized_path.name}")
    
    # Step 6: Build debug image (original + grid + anchor markers)
    debug_img = img.copy()
    draw = ImageDraw.Draw(debug_img)
    
    # Draw cell grid
    for col_idx in range(num_cols + 1):
        if col_idx < len(col_bounds):
            x = col_bounds[col_idx][0]
        else:
            x = col_bounds[-1][1] + 1
        draw.line([(x, 0), (x, h - 1)], fill=(0, 255, 0, 128), width=1)
    for row_idx in range(num_rows + 1):
        if row_idx < len(row_bounds):
            y = row_bounds[row_idx][0]
        else:
            y = row_bounds[-1][1] + 1
        draw.line([(0, y), (w - 1, y)], fill=(0, 255, 0, 128), width=1)
    
    # Draw foot anchor on each frame
    for row_idx in range(num_rows):
        ry_start, ry_end = row_bounds[row_idx]
        for col_idx in range(num_cols):
            cx_start, cx_end = col_bounds[col_idx]
            frame = arr[ry_start:ry_end + 1, cx_start:cx_end + 1]
            bounds = measure_opaque_bounds(frame)
            if bounds is None:
                continue
            fl, ft, fr, fb = bounds
            # Draw opaque bbox in red
            draw.rectangle(
                [cx_start + fl, ry_start + ft, cx_start + fr, ry_start + fb],
                outline=(255, 0, 0, 200), width=1
            )
            # Draw foot line in cyan
            foot_y = ry_start + fb
            draw.line([(cx_start + fl, foot_y), (cx_start + fr, foot_y)],
                      fill=(0, 255, 255, 200), width=2)
            # Draw center axis in yellow
            center_x = cx_start + (fl + fr) // 2
            draw.line([(center_x, ry_start + ft), (center_x, ry_start + fb)],
                      fill=(255, 255, 0, 128), width=1)
    
    debug_path = out_dir / f"{name}.normalized.debug.png"
    debug_img.save(debug_path)
    print(f"  Saved: {debug_path.name}")
    
    # Step 7: Write descriptor JSON
    descriptor = {
        "frameW": frame_w,
        "frameH": frame_h,
        "cols": num_cols,
        "rows": num_rows,
        "footAnchorX": foot_anchor_x,
        "footAnchorY": foot_anchor_y,
        "opaqueBodyH": opaque_body_h,
        "source": sheet_path.name,
    }
    
    json_path = out_dir / f"{name}.sheet.json"
    with open(json_path, "w") as f:
        json.dump(descriptor, f, indent=2)
    print(f"  Saved: {json_path.name}")
    
    return descriptor


def batch_normalize(directory: Path, hint_rows=None, hint_cols=None):
    """Process all .png sheets in directory (recursively), skipping already-normalized ones."""
    sheets = sorted(directory.rglob("*.png"))
    # Filter out already-normalized, debug, shadow-only, and parts files
    # Keep *_without_shadow.png and *_with_shadow.png (the composed sheets)
    # Skip: Parts/, Tiled_files/, shadow_*.png, *.normalized.*, *.debug.*
    sheets = [
        s for s in sheets
        if ".normalized" not in s.name
        and "debug" not in s.name.lower()
        and "/Parts/" not in str(s)
        and "/Tiled_files/" not in str(s)
        and "/Tiled/" not in str(s)
        and not s.name.startswith("shadow_")
    ]
    
    processed = 0
    skipped = 0
    
    for sheet in sheets:
        print(f"\n--- {sheet.relative_to(directory)} ---")
        result = normalize_sheet(sheet, hint_rows, hint_cols)
        if result:
            processed += 1
        else:
            skipped += 1
    
    print(f"\n{'=' * 40}")
    print(f"Done: {processed} normalized, {skipped} skipped")
    return processed, skipped


def main():
    parser = argparse.ArgumentParser(description="Normalize sprite sheets: detect grid, emit uniform frames + descriptor")
    parser.add_argument("input", nargs="?", help="Single sheet PNG to normalize")
    parser.add_argument("--batch", help="Directory to batch-process (recursive)")
    parser.add_argument("--rows", type=int, help="Hint: number of rows in grid")
    parser.add_argument("--cols", type=int, help="Hint: number of columns in grid")
    
    args = parser.parse_args()
    
    if args.batch:
        batch_dir = Path(args.batch)
        if not batch_dir.is_dir():
            print(f"Error: {args.batch} is not a directory")
            sys.exit(1)
        batch_normalize(batch_dir, args.rows, args.cols)
    elif args.input:
        sheet_path = Path(args.input)
        if not sheet_path.is_file():
            print(f"Error: {args.input} not found")
            sys.exit(1)
        result = normalize_sheet(sheet_path, args.rows, args.cols)
        if result is None:
            sys.exit(1)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
