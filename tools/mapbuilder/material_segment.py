"""
Material Segmentation Pipeline for GAIME Weather System.

Takes a painted HD background (village, dungeon, forest) and produces:
1. A Material-Grid (2D array of material IDs per tile)
2. A Material-Bitmap (1px=1tile, color-coded PNG) for visualization
3. A Material-Data file (JSON) for KorGE runtime shader consumption

Material classes for village/outdoor scenes:
- GRASS: green vegetation at ground level
- STONE_PATH: cobblestone paths (warm beige, high value)
- WOOD: building walls, doors, fachwerk (medium brown)
- ROOF: roof tiles/shingles (reddish-brown, upper building regions)
- FOLIAGE: tree canopy, bushes (darker greens)
- WATER: ponds, rivers (blue hues)
- FLOWERS: decorative plants (pink/magenta)

Weather effects per material (applied by KorGE MaterialWeatherFilter):
- STONE_PATH → puddles form in rain, darkens uniformly, wet reflections
- GRASS → saturates MORE in rain (greens pop), slight darkening
- WOOD → darkens significantly, subtle wet sheen, no puddles
- ROOF → water beads/runs off, specular streaks (Abperlen)
- FOLIAGE → drips, slight movement, color deepens

This replaces the fragile runtime-HSV-classification in the shader with
a pre-baked material map. The shader reads material IDs from a uniform
array (768 values for 64x36 grid fit easily in a UBO).

Architecture note (KorGE constraint):
KorGE filters can only bind ONE texture. Therefore the material map
cannot be passed as a second sampler. Instead, the material grid is
passed as a uniform float array (packed as vec4s) that the shader
indexes by fragment coordinate → grid cell.
"""

import cv2
import numpy as np
import json
import os
from enum import IntEnum
from pathlib import Path
from collections import Counter


class Material(IntEnum):
    """
    Material IDs — these values are used directly in the shader uniform array.
    The shader uses: if (materialId == 0.0) → grass effect, etc.
    """
    GRASS = 0
    STONE_PATH = 1
    WOOD = 2
    ROOF = 3
    FOLIAGE = 4
    WATER = 5
    FLOWERS = 6
    UNKNOWN = 7


# Color coding for the material bitmap (BGR format for OpenCV)
MATERIAL_COLORS_BGR = {
    Material.GRASS: (80, 200, 80),         # green
    Material.STONE_PATH: (180, 180, 180),  # light grey
    Material.WOOD: (60, 100, 140),         # brown
    Material.ROOF: (40, 60, 160),          # dark red-brown
    Material.FOLIAGE: (40, 120, 20),       # dark green
    Material.WATER: (200, 140, 60),        # blue
    Material.FLOWERS: (180, 100, 200),     # pink/magenta
    Material.UNKNOWN: (128, 128, 128),     # grey
}

# RGB for the JSON export (for debug/preview rendering)
MATERIAL_COLORS_RGB = {
    Material.GRASS: [80, 200, 80],
    Material.STONE_PATH: [180, 180, 180],
    Material.WOOD: [140, 100, 60],
    Material.ROOF: [160, 60, 40],
    Material.FOLIAGE: [20, 120, 40],
    Material.WATER: [60, 140, 200],
    Material.FLOWERS: [200, 100, 180],
    Material.UNKNOWN: [128, 128, 128],
}


def classify_pixel_village(h: int, s: int, v: int, row: int, col: int,
                           grid_h: int, grid_w: int,
                           edge_strength: float = 0.0) -> Material:
    """
    Classify a single tile by its average HSV color + spatial position.
    
    Tuned for the GAIME village image (pixel-art isometric, warm palette):
    - Stone path: H=15-22, S=120-140, V>220 (warm bright beige)
    - Roof: H=8-14, S>135, V=150-225 (reddish-brown, darker than path)
    - Wood: H=8-20, S>100, V=80-165 (medium brown, building walls)
    - Grass: H=25-85, S>50, V>100 (greens)
    - Foliage: H=25-100, S>30, V<100 OR dark with green tint
    - Flowers: H=150-175 or H<5, S>80 (pink/magenta)
    - Water: H=90-130, S>50 (blue)
    """
    # Very dark = foliage (tree shadows, deep canopy)
    if v < 45:
        return Material.FOLIAGE
    
    # Flowers: pink/magenta hues (H near 0 or > 150, high saturation)
    if (h > 150 or h < 5) and s > 80 and v > 50:
        return Material.FLOWERS
    
    # Water: blue hues
    if 90 <= h <= 130 and s > 50 and v > 50:
        return Material.WATER
    
    # Green classification (grass vs foliage based on brightness)
    if 25 <= h <= 85 and s > 50:
        if v > 110:
            return Material.GRASS
        else:
            return Material.FOLIAGE
    
    # Cyan-ish foliage (teal bushes in the village image)
    if 85 < h <= 105 and s > 50:
        return Material.FOLIAGE
    
    # Warm browns: differentiate stone_path vs roof vs wood
    # This is the critical distinction for the village image:
    # - Path: H≈17-19, S≈120-135, V>220 (bright warm beige)
    # - Roof: H≈9-13, S>135, V=150-225 (reddish, darker)
    # - Wood: H≈8-20, S>100, V=80-165 (medium brown)
    if 5 <= h <= 25 and s > 80:
        # High value (>220) = STONE PATH (bright beige/sand)
        if v > 220:
            return Material.STONE_PATH
        
        # Reddish hue (H<14) + high sat + medium-high value = ROOF
        if h < 14 and s > 135 and v > 150:
            return Material.ROOF
        
        # Medium value, reddish = could be darker roof tiles
        if h < 14 and s > 130 and 100 <= v <= 165:
            return Material.ROOF
        
        # Medium value (80-165) = WOOD (building walls)
        if v <= 165:
            return Material.WOOD
        
        # Remaining: between 165-220, warm hue
        # Use spatial hint: upper image = likely roof, lower = path
        normalized_y = row / max(grid_h - 1, 1)
        if normalized_y < 0.4 and h < 15:
            return Material.ROOF
        
        return Material.STONE_PATH
    
    # Low saturation, high value = stone (desaturated bright)
    if s < 40 and v > 160:
        return Material.STONE_PATH
    
    # Low saturation, medium value = grey (likely stone or building shadow)
    if s < 40 and 50 <= v <= 160:
        return Material.WOOD
    
    # Remaining warm hues with lower saturation
    if 5 <= h <= 25 and 40 <= s <= 80 and v > 50:
        return Material.WOOD
    
    # Default: grass (most common ground material in village scenes)
    return Material.GRASS


def segment_materials(image_path: str, grid_width: int = 64, grid_height: int = 36) -> np.ndarray:
    """
    Segment an image into a material grid.
    
    Args:
        image_path: Path to input image (JPG/PNG).
        grid_width: Number of tile columns.
        grid_height: Number of tile rows.
    
    Returns:
        2D numpy array of Material IDs [row][col] (dtype=uint8).
    """
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"Cannot read image: {image_path}")
    
    # Resize to grid dimensions (each pixel = one tile)
    small = cv2.resize(img, (grid_width, grid_height), interpolation=cv2.INTER_AREA)
    hsv = cv2.cvtColor(small, cv2.COLOR_BGR2HSV)
    
    # Edge detection for structural hints (walls, building contours)
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 50, 150)
    edges_small = cv2.resize(edges, (grid_width, grid_height), interpolation=cv2.INTER_AREA)
    
    # Build material grid
    grid = np.zeros((grid_height, grid_width), dtype=np.uint8)
    
    for y in range(grid_height):
        for x in range(grid_width):
            h, s, v = int(hsv[y, x, 0]), int(hsv[y, x, 1]), int(hsv[y, x, 2])
            edge_val = float(edges_small[y, x]) / 255.0
            material = classify_pixel_village(h, s, v, y, x, grid_height, grid_width, edge_val)
            grid[y, x] = int(material)
    
    # Post-process: morphological cleanup (2 passes)
    grid = cleanup_grid(grid)
    
    return grid


def cleanup_grid(grid: np.ndarray, iterations: int = 2) -> np.ndarray:
    """
    Remove isolated single-tile noise using majority-neighbor voting.
    Uses 8-connectivity (Moore neighborhood) for smoother results.
    """
    h, w = grid.shape
    result = grid.copy()
    
    for _ in range(iterations):
        new_result = result.copy()
        for y in range(h):
            for x in range(w):
                neighbors = []
                for dy in [-1, 0, 1]:
                    for dx in [-1, 0, 1]:
                        if dy == 0 and dx == 0:
                            continue
                        ny, nx = y + dy, x + dx
                        if 0 <= ny < h and 0 <= nx < w:
                            neighbors.append(result[ny, nx])
                
                if not neighbors:
                    continue
                
                # If this cell's material appears in fewer than 2 of 8 neighbors, replace
                current = result[y, x]
                count_same = sum(1 for n in neighbors if n == current)
                if count_same < 2:
                    most_common = Counter(neighbors).most_common(1)[0][0]
                    new_result[y, x] = most_common
        
        result = new_result
    
    return result


def grid_to_bitmap(grid: np.ndarray) -> np.ndarray:
    """
    Convert material grid to color-coded bitmap (1px = 1 tile).
    This is the MATERIAL MAP — each pixel encodes a material type by color.
    """
    h, w = grid.shape
    bitmap = np.zeros((h, w, 3), dtype=np.uint8)
    
    for mat in Material:
        mask = grid == int(mat)
        color = MATERIAL_COLORS_BGR.get(mat, (128, 128, 128))
        bitmap[mask] = color
    
    return bitmap


def grid_to_preview(grid: np.ndarray, cell_size: int = 16) -> np.ndarray:
    """Generate a larger color-coded preview image for human inspection."""
    h, w = grid.shape
    preview = np.zeros((h * cell_size, w * cell_size, 3), dtype=np.uint8)
    
    for y in range(h):
        for x in range(w):
            mat = Material(grid[y, x])
            color = MATERIAL_COLORS_BGR.get(mat, (128, 128, 128))
            cv2.rectangle(
                preview,
                (x * cell_size, y * cell_size),
                ((x + 1) * cell_size - 1, (y + 1) * cell_size - 1),
                color, -1
            )
            # Grid lines for clarity
            cv2.rectangle(
                preview,
                (x * cell_size, y * cell_size),
                ((x + 1) * cell_size - 1, (y + 1) * cell_size - 1),
                (40, 40, 40), 1
            )
    
    return preview


def grid_to_json(grid: np.ndarray, image_path: str = "") -> dict:
    """
    Export grid as JSON for KorGE runtime consumption.
    
    The KorGE shader loads this and passes the materials[] array as a
    uniform to the fragment shader. The shader maps fragment coordinates
    → grid cell index → material ID → applies per-material weather effect.
    
    Format:
    {
        "version": 1,
        "source_image": "...",
        "width": 64,
        "height": 36,
        "materials": [0,1,2,3,...],  // flat row-major array of Material IDs
        "legend": {"GRASS": 0, "STONE_PATH": 1, ...},
        "colors_rgb": {"GRASS": [80,200,80], ...}
    }
    """
    h, w = grid.shape
    return {
        "version": 1,
        "source_image": os.path.basename(image_path) if image_path else "",
        "width": w,
        "height": h,
        "materials": grid.flatten().tolist(),
        "legend": {mat.name: int(mat) for mat in Material},
        "colors_rgb": {mat.name: MATERIAL_COLORS_RGB[mat] for mat in Material},
    }


def run_pipeline(image_path: str, output_dir: str,
                 grid_width: int = 64, grid_height: int = 36) -> dict:
    """
    Run the complete material segmentation pipeline.
    
    Produces:
    - material_grid.json:    Machine-readable grid for KorGE shader uniform
    - material_bitmap.png:   1px=1tile color-coded bitmap (for debugging)
    - material_preview.png:  Human-readable 16x scaled preview
    
    Returns dict with paths to all output files + the grid array.
    """
    os.makedirs(output_dir, exist_ok=True)
    
    print(f"[material] Loading image: {image_path}")
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"Cannot read: {image_path}")
    print(f"[material] Image size: {img.shape[1]}x{img.shape[0]}")
    
    # 1. Segment
    print(f"[material] Segmenting to {grid_width}x{grid_height} grid...")
    grid = segment_materials(image_path, grid_width, grid_height)
    
    # Stats
    stats = Counter(grid.flatten().tolist())
    print("[material] Material distribution:")
    for mat_id, count in sorted(stats.items()):
        mat = Material(mat_id)
        pct = count / grid.size * 100
        print(f"  {mat.name:12s}: {count:4d} tiles ({pct:5.1f}%)")
    
    # 2. Material bitmap (1px = 1 tile)
    bitmap = grid_to_bitmap(grid)
    bitmap_path = os.path.join(output_dir, "material_bitmap.png")
    cv2.imwrite(bitmap_path, bitmap)
    print(f"[material] Bitmap saved: {bitmap_path} ({grid_width}x{grid_height}px)")
    
    # 3. Human-readable preview
    preview = grid_to_preview(grid, cell_size=16)
    preview_path = os.path.join(output_dir, "material_preview.png")
    cv2.imwrite(preview_path, preview)
    print(f"[material] Preview saved: {preview_path}")
    
    # 4. JSON data for KorGE
    json_data = grid_to_json(grid, image_path)
    json_path = os.path.join(output_dir, "material_grid.json")
    with open(json_path, 'w') as f:
        json.dump(json_data, f, indent=2)
    print(f"[material] JSON saved: {json_path}")
    
    # 5. Also copy the JSON to the game assets folder for KorGE resourcesVfs
    assets_dir = Path(image_path).parent / "assets" / "HD" / "locations"
    # If we're in the GAIME repo, place next to the source image
    gaime_assets = Path(image_path).parent / "assets" / "materials"
    if (Path(image_path).parent / "assets").exists():
        gaime_assets.mkdir(parents=True, exist_ok=True)
        asset_json_path = gaime_assets / "village_material_grid.json"
        with open(asset_json_path, 'w') as f:
            json.dump(json_data, f)
        asset_bitmap_path = gaime_assets / "village_material_bitmap.png"
        cv2.imwrite(str(asset_bitmap_path), bitmap)
        print(f"[material] KorGE asset: {asset_json_path}")
        print(f"[material] KorGE asset: {asset_bitmap_path}")
    
    return {
        "grid": grid,
        "json_data": json_data,
        "bitmap_path": bitmap_path,
        "preview_path": preview_path,
        "json_path": json_path,
    }


# ============================================================================
# CLI
# ============================================================================

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(
        description="GAIME Material Segmentation Pipeline — produces baked material maps for weather shaders"
    )
    parser.add_argument("image", help="Path to input image (PNG/JPG)")
    parser.add_argument("-o", "--output", default="output", help="Output directory (default: output)")
    parser.add_argument("-W", "--width", type=int, default=64, help="Grid width in tiles (default: 64)")
    parser.add_argument("-H", "--height", type=int, default=36, help="Grid height in tiles (default: 36)")
    
    args = parser.parse_args()
    
    result = run_pipeline(
        image_path=args.image,
        output_dir=args.output,
        grid_width=args.width,
        grid_height=args.height,
    )
    print(f"\n[material] Done. {args.width}x{args.height} = {args.width * args.height} tiles segmented.")
