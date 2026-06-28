"""
Phase 1: Image Segmentation — Foto/Sketch → Semantic Label Grid.

Takes an input image (photo, sketch, floor plan) and segments it into
a grid of material labels: FLOOR, WALL, WATER, GRASS, STONE, EMPTY.

Pipeline:
1. Resize to target grid dimensions (each cell = 1 tile)
2. Color-space analysis (HSV) → classify dominant material per cell
3. Edge detection (Canny) → wall candidates
4. Morphological cleanup → consistent regions
5. Output: 2D grid of label strings

No ML models required — pure OpenCV + heuristic color classification.
"""

import cv2
import numpy as np
from enum import Enum


class TileLabel(str, Enum):
    FLOOR = "floor"
    WALL = "wall"
    WATER = "water"
    GRASS = "grass"
    STONE = "stone"
    DOOR = "door"
    EMPTY = "empty"


# HSV ranges for material detection (tunable)
# Format: (H_low, S_low, V_low), (H_high, S_high, V_high)
MATERIAL_RANGES = {
    TileLabel.WATER: ((90, 50, 50), (130, 255, 255)),      # Blue hues
    TileLabel.GRASS: ((35, 40, 40), (85, 255, 255)),       # Green hues
    TileLabel.WALL: ((0, 0, 0), (180, 50, 80)),            # Dark / low saturation
    TileLabel.FLOOR: ((10, 20, 100), (35, 200, 255)),      # Warm brown/wood tones
    TileLabel.STONE: ((0, 0, 130), (180, 40, 220)),        # Light grey, low saturation
}


def segment_image(image_path: str, grid_width: int = 32, grid_height: int = 24) -> list[list[str]]:
    """
    Segment an image into a tile-label grid.
    
    Args:
        image_path: Path to input image (JPG/PNG).
        grid_width: Number of tile columns.
        grid_height: Number of tile rows.
    
    Returns:
        2D list of TileLabel strings [row][col].
    """
    img = cv2.imread(image_path)
    if img is None:
        raise FileNotFoundError(f"Cannot read image: {image_path}")
    
    # Resize to grid dimensions (each pixel = one tile)
    small = cv2.resize(img, (grid_width, grid_height), interpolation=cv2.INTER_AREA)
    hsv = cv2.cvtColor(small, cv2.COLOR_BGR2HSV)
    
    # Edge detection on original for wall detection
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 50, 150)
    edges_small = cv2.resize(edges, (grid_width, grid_height), interpolation=cv2.INTER_AREA)
    
    # Build label grid
    grid = []
    for y in range(grid_height):
        row = []
        for x in range(grid_width):
            label = classify_pixel(hsv[y, x], edges_small[y, x])
            row.append(label)
        grid.append(row)
    
    # Post-process: morphological cleanup (remove isolated single-tile noise)
    grid = cleanup_grid(grid, grid_width, grid_height)
    
    return grid


def classify_pixel(hsv_pixel: np.ndarray, edge_value: int) -> str:
    """Classify a single pixel/tile by HSV color + edge strength."""
    h, s, v = int(hsv_pixel[0]), int(hsv_pixel[1]), int(hsv_pixel[2])
    
    # Strong edge = likely wall
    if edge_value > 128:
        return TileLabel.WALL
    
    # Very dark = wall/empty
    if v < 40:
        return TileLabel.WALL
    
    # Very light + low saturation = stone/floor
    if v > 200 and s < 30:
        return TileLabel.STONE
    
    # Check color ranges
    for label, ((h_lo, s_lo, v_lo), (h_hi, s_hi, v_hi)) in MATERIAL_RANGES.items():
        if h_lo <= h <= h_hi and s_lo <= s <= s_hi and v_lo <= v <= v_hi:
            return label
    
    # Default: floor (most common in game maps)
    return TileLabel.FLOOR


def cleanup_grid(grid: list[list[str]], width: int, height: int) -> list[list[str]]:
    """
    Remove isolated single-tile labels (noise).
    If a tile's label differs from all 4 neighbors, replace with majority neighbor.
    """
    result = [row[:] for row in grid]
    
    for y in range(height):
        for x in range(width):
            neighbors = []
            for dy, dx in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                ny, nx = y + dy, x + dx
                if 0 <= ny < height and 0 <= nx < width:
                    neighbors.append(grid[ny][nx])
            
            if neighbors and grid[y][x] not in neighbors:
                # All neighbors differ from this cell — take majority
                from collections import Counter
                most_common = Counter(neighbors).most_common(1)[0][0]
                result[y][x] = most_common
    
    return result


def grid_to_preview(grid: list[list[str]], cell_size: int = 16) -> np.ndarray:
    """Generate a color-coded preview image of the label grid."""
    COLORS = {
        TileLabel.FLOOR: (200, 180, 140),   # warm beige
        TileLabel.WALL: (60, 60, 60),       # dark grey
        TileLabel.WATER: (180, 120, 40),    # blue (BGR)
        TileLabel.GRASS: (80, 180, 80),     # green
        TileLabel.STONE: (180, 180, 180),   # light grey
        TileLabel.DOOR: (60, 60, 200),      # red-ish
        TileLabel.EMPTY: (0, 0, 0),         # black
    }
    
    height = len(grid)
    width = len(grid[0]) if grid else 0
    img = np.zeros((height * cell_size, width * cell_size, 3), dtype=np.uint8)
    
    for y, row in enumerate(grid):
        for x, label in enumerate(row):
            color = COLORS.get(label, (128, 128, 128))
            cv2.rectangle(
                img,
                (x * cell_size, y * cell_size),
                ((x + 1) * cell_size - 1, (y + 1) * cell_size - 1),
                color, -1
            )
    
    return img
