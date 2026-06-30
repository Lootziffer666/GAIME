"""
Full-Resolution Material Map Generator for GAIME.

Takes the tile-level segmentation (from material_segment.py) and produces a
FULL-RESOLUTION material map (same dimensions as the source image) where:
- Each pixel gets a Material ID
- At tile boundaries, Nearest-Neighbor lookup in the HD image determines
  where the actual contour lies (not the blocky tile grid)

This produces the texture that gets bound as the second sampler in the
MaterialWeatherFilter shader.

Encoding: Each pixel's R channel = Material ID (0-7), scaled to 0-255.
  R=0   → GRASS (0)
  R=36  → STONE_PATH (1)
  R=73  → WOOD (2)
  R=109 → ROOF (3)
  R=146 → FOLIAGE (4)
  R=182 → WATER (5)
  R=219 → FLOWERS (6)
  R=255 → UNKNOWN (7)

The shader reads: materialId = floor(texelR / 255.0 * 7.0 + 0.5)
"""

import cv2
import numpy as np
import json
import os
from pathlib import Path
from collections import Counter

from material_segment import Material, segment_materials, classify_pixel_village


# R-channel values for each material (evenly spaced in 0-255)
MATERIAL_R_VALUES = {
    Material.GRASS: 0,
    Material.STONE_PATH: 36,
    Material.WOOD: 73,
    Material.ROOF: 109,
    Material.FOLIAGE: 146,
    Material.WATER: 182,
    Material.FLOWERS: 219,
    Material.UNKNOWN: 255,
}


def generate_fullres_material_map(
    image_path: str,
    grid_width: int = 64,
    grid_height: int = 36,
    boundary_radius: int = 5,
) -> np.ndarray:
    """
    Generate a full-resolution material map with nearest-neighbor boundary snapping.
    
    Process:
    1. Segment the image into a tile grid (coarse classification)
    2. Upscale the grid to full resolution (nearest-neighbor = blocky)
    3. At tile boundaries: re-classify using the HD pixel's HSV values,
       consulting neighbors to decide which material the pixel belongs to
    
    Args:
        image_path: Path to the HD source image
        grid_width: Tile grid width for initial segmentation
        grid_height: Tile grid height for initial segmentation
        boundary_radius: Pixels from tile edge to consider as "boundary zone"
    
    Returns:
        Full-resolution single-channel image (uint8) with Material IDs encoded in R values
    """
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"Cannot read: {image_path}")
    
    img_h, img_w = img.shape[:2]
    print(f"[fullres] Source: {img_w}x{img_h}")
    
    # Step 1: Coarse segmentation
    print(f"[fullres] Segmenting at {grid_width}x{grid_height}...")
    grid = segment_materials(image_path, grid_width, grid_height)
    
    # Step 2: Calculate tile dimensions
    tile_w = img_w / grid_width    # e.g. 2560/64 = 40px per tile
    tile_h = img_h / grid_height   # e.g. 1440/36 = 40px per tile
    print(f"[fullres] Tile size: {tile_w:.1f}x{tile_h:.1f}px")
    
    # Step 3: Generate full-res map
    # Start with blocky upscale
    material_map = np.zeros((img_h, img_w), dtype=np.uint8)
    
    # Fill with coarse grid values
    for gy in range(grid_height):
        for gx in range(grid_width):
            y0 = int(gy * tile_h)
            y1 = int((gy + 1) * tile_h)
            x0 = int(gx * tile_w)
            x1 = int((gx + 1) * tile_w)
            mat_id = int(grid[gy, gx])
            material_map[y0:y1, x0:x1] = MATERIAL_R_VALUES[Material(mat_id)]
    
    # Step 4: Boundary refinement
    # At tile edges, use the actual pixel color to decide which material it belongs to
    print(f"[fullres] Refining boundaries (radius={boundary_radius}px)...")
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    
    # Find all boundary pixels (within boundary_radius of a tile edge)
    boundary_count = 0
    refined_count = 0
    
    for gy in range(grid_height):
        for gx in range(grid_width):
            # Only process tiles that have a different neighbor
            current_mat = grid[gy, gx]
            has_different_neighbor = False
            neighbor_mats = set()
            
            for dy, dx in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                ny, nx = gy + dy, gx + dx
                if 0 <= ny < grid_height and 0 <= nx < grid_width:
                    if grid[ny, nx] != current_mat:
                        has_different_neighbor = True
                        neighbor_mats.add(int(grid[ny, nx]))
            
            if not has_different_neighbor:
                continue
            
            # Process boundary zone within this tile
            y0 = int(gy * tile_h)
            y1 = int((gy + 1) * tile_h)
            x0 = int(gx * tile_w)
            x1 = int((gx + 1) * tile_w)
            
            # Only pixels near the edges (within boundary_radius of tile border)
            for py in range(y0, y1):
                for px in range(x0, x1):
                    # Distance to nearest tile edge
                    dist_to_edge = min(
                        py - y0, y1 - 1 - py,
                        px - x0, x1 - 1 - px
                    )
                    
                    if dist_to_edge > boundary_radius:
                        continue
                    
                    boundary_count += 1
                    
                    # Re-classify this pixel using its actual HSV value
                    h, s, v = int(hsv[py, px, 0]), int(hsv[py, px, 1]), int(hsv[py, px, 2])
                    pixel_mat = classify_pixel_village(
                        h, s, v, gy, gx, grid_height, grid_width
                    )
                    
                    # Only accept if the pixel's material matches either this tile
                    # or one of its neighbors (prevents noise from creating new regions)
                    acceptable = {int(current_mat)} | neighbor_mats
                    if int(pixel_mat) in acceptable:
                        material_map[py, px] = MATERIAL_R_VALUES[pixel_mat]
                        if int(pixel_mat) != int(current_mat):
                            refined_count += 1
    
    print(f"[fullres] Boundary pixels: {boundary_count}, refined: {refined_count}")
    
    # Step 5: Light smoothing pass on material boundaries (median filter on 3x3)
    # This prevents single-pixel noise at transitions without destroying edges
    # We only smooth boundary regions
    # (skip for now — the nearest-neighbor result should be clean enough)
    
    return material_map


def save_material_map(material_map: np.ndarray, output_path: str):
    """
    Save the full-resolution material map as a PNG.
    
    Stored as a grayscale image where R = Material ID encoding.
    The shader samples this and decodes: matId = floor(R/255 * 7 + 0.5)
    """
    cv2.imwrite(output_path, material_map)
    h, w = material_map.shape
    size_mb = os.path.getsize(output_path) / (1024 * 1024)
    print(f"[fullres] Saved: {output_path} ({w}x{h}, {size_mb:.2f} MB)")


def run_fullres_pipeline(
    image_path: str,
    output_dir: str,
    grid_width: int = 64,
    grid_height: int = 36,
    boundary_radius: int = 5,
) -> dict:
    """
    Run the full-resolution material map pipeline.
    
    Produces:
    - village_material_fullres.png: Full-res material map (R-channel encoded)
    - Also copies to assets/materials/ for KorGE consumption
    """
    os.makedirs(output_dir, exist_ok=True)
    
    material_map = generate_fullres_material_map(
        image_path, grid_width, grid_height, boundary_radius
    )
    
    # Save to output dir
    fullres_path = os.path.join(output_dir, "village_material_fullres.png")
    save_material_map(material_map, fullres_path)
    
    # Also save to assets/materials/ for KorGE
    assets_dir = Path(image_path).parent / "assets" / "materials"
    if (Path(image_path).parent / "assets").exists():
        assets_dir.mkdir(parents=True, exist_ok=True)
        asset_path = str(assets_dir / "village_material_fullres.png")
        save_material_map(material_map, asset_path)
    
    # Stats
    unique_values = np.unique(material_map)
    print(f"[fullres] Material values present: {len(unique_values)}")
    for val in unique_values:
        # Reverse lookup
        mat = None
        for m, rv in MATERIAL_R_VALUES.items():
            if rv == val:
                mat = m
                break
        count = np.sum(material_map == val)
        total = material_map.size
        mat_name = mat.name if mat else f"R={val}"
        print(f"  {mat_name:12s} (R={val:3d}): {count/total*100:5.1f}%")
    
    return {
        "material_map": material_map,
        "fullres_path": fullres_path,
    }


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Generate full-resolution material map with boundary snapping"
    )
    parser.add_argument("image", help="Path to source HD image")
    parser.add_argument("-o", "--output", default="output", help="Output directory")
    parser.add_argument("-W", "--width", type=int, default=64, help="Grid width")
    parser.add_argument("-H", "--height", type=int, default=36, help="Grid height")
    parser.add_argument("-r", "--radius", type=int, default=5, help="Boundary refinement radius (px)")
    
    args = parser.parse_args()
    
    run_fullres_pipeline(
        image_path=args.image,
        output_dir=args.output,
        grid_width=args.width,
        grid_height=args.height,
        boundary_radius=args.radius,
    )
    print("[fullres] Done.")
