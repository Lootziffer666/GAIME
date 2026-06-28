"""
Phase 2: Wave Function Collapse — adds variety to uniform tile regions.

Given a label grid where large areas are all the same label (e.g. "floor"),
WFC introduces tile VARIANTS (floor_1, floor_2, floor_cracked, etc.) based
on adjacency rules. This prevents the "flat checkerboard" look.

Simplified WFC for tile variants (not full constraint propagation):
- For each cell, pick a tile variant compatible with its neighbors
- Deterministic seed for reproducibility
"""

import random
from typing import Optional


# Tile variant rules: for each label, which tile IDs are available
# and what they can be adjacent to
TILE_VARIANTS = {
    "floor": {
        "ids": [1, 7, 8],  # plain, cracked, mossy
        "weights": [0.7, 0.15, 0.15],
    },
    "wall": {
        "ids": [2, 9, 10],  # solid, brick, damaged
        "weights": [0.6, 0.25, 0.15],
    },
    "grass": {
        "ids": [4, 11, 12],  # short, tall, flowers
        "weights": [0.5, 0.3, 0.2],
    },
    "stone": {
        "ids": [5, 13, 14],  # clean, cracked, mossy
        "weights": [0.6, 0.2, 0.2],
    },
    "water": {
        "ids": [3],  # just water (animated in-game)
        "weights": [1.0],
    },
    "door": {
        "ids": [6],
        "weights": [1.0],
    },
    "empty": {
        "ids": [0],
        "weights": [1.0],
    },
}


def apply_variants(
    grid: list[list[str]],
    seed: int = 42,
) -> list[list[int]]:
    """
    Convert a label grid into a tile-ID grid with variants.
    
    Args:
        grid: 2D list of label strings.
        seed: Random seed for reproducibility.
    
    Returns:
        2D list of tile IDs (integers).
    """
    rng = random.Random(seed)
    height = len(grid)
    width = len(grid[0]) if grid else 0
    
    result = []
    for y in range(height):
        row = []
        for x in range(width):
            label = grid[y][x]
            variants = TILE_VARIANTS.get(label, {"ids": [0], "weights": [1.0]})
            tile_id = rng.choices(variants["ids"], weights=variants["weights"], k=1)[0]
            row.append(tile_id)
        result.append(row)
    
    return result


def apply_edge_tiles(
    label_grid: list[list[str]],
    tile_grid: list[list[int]],
) -> list[list[int]]:
    """
    Replace border tiles with appropriate edge variants.
    E.g., a grass tile next to water becomes a shore tile.
    
    This is a simplified auto-tiling pass (bitmask-based edge detection).
    """
    height = len(label_grid)
    width = len(label_grid[0]) if label_grid else 0
    result = [row[:] for row in tile_grid]
    
    # Edge tile IDs (hypothetical — depends on actual tileset)
    EDGE_TILES = {
        ("grass", "water"): 15,  # shore
        ("floor", "wall"): 16,   # floor-wall border
        ("stone", "grass"): 17,  # path edge
    }
    
    for y in range(height):
        for x in range(width):
            current = label_grid[y][x]
            # Check right and bottom neighbors for transitions
            for dy, dx in [(0, 1), (1, 0)]:
                ny, nx = y + dy, x + dx
                if 0 <= ny < height and 0 <= nx < width:
                    neighbor = label_grid[ny][nx]
                    if current != neighbor:
                        edge_key = (current, neighbor)
                        if edge_key in EDGE_TILES:
                            result[y][x] = EDGE_TILES[edge_key]
    
    return result
