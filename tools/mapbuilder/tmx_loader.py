"""
TMX Loader for the Map Builder — loads existing Tiled TMX maps into the editor.

Parses the TMX XML, extracts tile data from all layers, and classifies them
into the editor's layer system (ground, weather, exits, npc_routes, interactable,
decorative) based on layer names and tile content.

Handles both finite and infinite (chunk-based) TMX formats.
"""

import re
from typing import Tuple


# Layer name classification (mirrors CollisionGrid.layerType in :core)
def classify_layer(name: str) -> str:
    """Classify a TMX layer name into an editor layer category."""
    lower = name.lower()
    
    # Ground materials (walkable surfaces)
    if (lower.startswith("floor") or lower == "ground" or lower.startswith("road") or
        lower.startswith("grass") or lower.startswith("carpet") or
        lower.startswith("spots") or lower.startswith("plates") or
        lower.startswith("stairs") or lower == "site"):
        return "ground:floor"
    
    # Water
    if lower.startswith("water"):
        return "ground:water"
    
    # Walls / solid structures
    if (lower.startswith("wall") or lower.startswith("house") or
        lower.startswith("roof") or lower.startswith("fence") or
        lower.startswith("columns") or lower.startswith("bricks")):
        return "ground:wall"
    
    # Bridges (should be floor!)
    if lower.startswith("bridge"):
        return "ground:floor"
    
    # Trees and vegetation (decorative in editor)
    if lower.startswith("tree") or lower.startswith("flower") or lower.startswith("plant"):
        return "decorative:tree"
    
    # Interactive triggers
    if (lower.startswith("door") or lower.startswith("ladder") or
        lower.startswith("entrance") or lower.startswith("chest") or
        lower.startswith("lever")):
        return "ground:door"
    
    # Everything else: decorative (no collision)
    return "decorative:none"


def parse_tmx_to_layers(content: str) -> Tuple[dict, int, int]:
    """
    Parse a TMX file content into editor-compatible layers.
    
    Returns:
        (layers_dict, width, height) where layers_dict has keys:
        ground, weather, exits, npc_routes, interactable, decorative
    """
    # Normalize line endings (Windows TMX files have \r\n)
    content = content.replace("\r\n", "\n").replace("\r", "\n")
    
    # Extract map dimensions
    width_match = re.search(r'<map[^>]*\swidth="(\d+)"', content)
    height_match = re.search(r'<map[^>]*\sheight="(\d+)"', content)
    infinite_match = re.search(r'infinite="1"', content)
    
    is_infinite = infinite_match is not None
    
    if not is_infinite and width_match and height_match:
        map_width = int(width_match.group(1))
        map_height = int(height_match.group(1))
    else:
        # For infinite maps, determine size from chunk extents
        map_width, map_height = 32, 24  # default, will be recalculated
    
    # Parse all layers
    layer_pattern = re.compile(
        r'<layer[^>]*\sname="([^"]*)"[^>]*>(.*?)</layer>',
        re.DOTALL
    )
    
    # For infinite maps, track the bounding box
    all_cells = {}  # (layer_name) -> {(x,y): has_tile}
    min_x, min_y = 9999, 9999
    max_x, max_y = -9999, -9999
    
    for layer_match in layer_pattern.finditer(content):
        layer_name = layer_match.group(1)
        layer_content = layer_match.group(2)
        
        if is_infinite:
            # Parse chunks — CSV data is directly inside <chunk> (no <data> wrapper in some TMX)
            chunk_pattern = re.compile(
                r'<chunk\s+x="(-?\d+)"\s+y="(-?\d+)"\s+width="(\d+)"\s+height="(\d+)"[^>]*>\s*'
                r'(?:<data[^>]*>)?\s*([\d,\s\r\n]+?)\s*(?:</data>)?\s*</chunk>',
                re.DOTALL
            )
            cells = {}
            for chunk_match in chunk_pattern.finditer(layer_content):
                cx = int(chunk_match.group(1))
                cy = int(chunk_match.group(2))
                cw = int(chunk_match.group(3))
                ch = int(chunk_match.group(4))
                csv_data = chunk_match.group(5).strip()
                
                rows = [r.strip().rstrip(",") for r in csv_data.split("\n") if r.strip() and r.strip() != ","]
                for row_idx, row in enumerate(rows):
                    tile_ids = [int(t.strip()) for t in row.split(",") if t.strip()]
                    for col_idx, tid in enumerate(tile_ids):
                        if tid > 0:
                            gx = cx + col_idx
                            gy = cy + row_idx
                            cells[(gx, gy)] = tid
                            min_x = min(min_x, gx)
                            min_y = min(min_y, gy)
                            max_x = max(max_x, gx)
                            max_y = max(max_y, gy)
            
            all_cells[layer_name] = cells
        else:
            # Finite map: parse CSV data
            data_match = re.search(r'<data[^>]*>(.*?)</data>', layer_content, re.DOTALL)
            if data_match:
                csv_data = data_match.group(1).strip()
                cells = {}
                rows = [r.strip() for r in csv_data.split("\n") if r.strip()]
                for row_idx, row in enumerate(rows):
                    tile_ids = [int(t.strip()) for t in row.split(",") if t.strip()]
                    for col_idx, tid in enumerate(tile_ids):
                        if tid > 0:
                            cells[(col_idx, row_idx)] = tid
                            min_x = min(min_x, col_idx)
                            min_y = min(min_y, row_idx)
                            max_x = max(max_x, col_idx)
                            max_y = max(max_y, row_idx)
                all_cells[layer_name] = cells
    
    # Calculate final grid dimensions
    if min_x > max_x:
        # No tiles found at all
        return _empty_layers(32, 24), 32, 24
    
    grid_width = max_x - min_x + 1
    grid_height = max_y - min_y + 1
    
    # Cap at reasonable size for the editor
    if grid_width > 128:
        grid_width = 128
    if grid_height > 96:
        grid_height = 96
    
    # Build editor layers
    ground = [["empty"] * grid_width for _ in range(grid_height)]
    weather = [["none"] * grid_width for _ in range(grid_height)]
    exits = [["none"] * grid_width for _ in range(grid_height)]
    npc_routes = [["none"] * grid_width for _ in range(grid_height)]
    interactable = [["none"] * grid_width for _ in range(grid_height)]
    decorative = [["none"] * grid_width for _ in range(grid_height)]
    
    # Fill ground layer from all TMX layers
    for layer_name, cells in all_cells.items():
        category = classify_layer(layer_name)
        target_layer, tile_label = category.split(":", 1)
        
        for (gx, gy), tid in cells.items():
            lx = gx - min_x
            ly = gy - min_y
            if 0 <= lx < grid_width and 0 <= ly < grid_height:
                if target_layer == "ground":
                    # Don't overwrite floor with wall (floor wins if both present)
                    current = ground[ly][lx]
                    if current == "empty" or (current == "wall" and tile_label in ("floor", "door")):
                        ground[ly][lx] = tile_label
                    elif current != "floor" and current != "door":
                        ground[ly][lx] = tile_label
                elif target_layer == "decorative":
                    if tile_label != "none":
                        decorative[ly][lx] = "tree"  # generic decorative
    
    # Mark cells that are still "empty" but have any tile data as "stone" (generic ground)
    for y in range(grid_height):
        for x in range(grid_width):
            if ground[y][x] == "empty":
                # Check if ANY layer had a tile here
                has_any = any(
                    (x + min_x, y + min_y) in cells
                    for cells in all_cells.values()
                )
                if has_any:
                    ground[y][x] = "stone"
    
    layers = {
        "ground": ground,
        "weather": weather,
        "exits": exits,
        "npc_routes": npc_routes,
        "interactable": interactable,
        "decorative": decorative,
    }
    
    return layers, grid_width, grid_height


def _empty_layers(width: int, height: int) -> dict:
    return {
        "ground": [["floor"] * width for _ in range(height)],
        "weather": [["none"] * width for _ in range(height)],
        "exits": [["none"] * width for _ in range(height)],
        "npc_routes": [["none"] * width for _ in range(height)],
        "interactable": [["none"] * width for _ in range(height)],
        "decorative": [["none"] * width for _ in range(height)],
    }
