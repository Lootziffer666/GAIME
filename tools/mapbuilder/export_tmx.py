"""
Phase 3: TMX Export — Label Grid / Tile-ID Grid → Tiled-compatible .tmx file.

Generates a TMX file that GAIME's TmxLoader can directly load.
Uses the infinite map format with 16x16 chunks, matching the existing CraftPix maps.

Layer naming follows CollisionGrid conventions:
- "Floor" → WALKABLE
- "Walls" → SOLID (BLOCKED)
- "Water" → WATER
- "Decorative" → no collision
"""

import xml.etree.ElementTree as ET
from xml.dom import minidom
from segment import TileLabel


# Tile IDs mapping (using a simple tileset where each material = one tile ID)
# In a real tileset these would map to specific tiles in the PNG
LABEL_TO_TILE_ID = {
    TileLabel.FLOOR: 1,
    TileLabel.WALL: 2,
    TileLabel.WATER: 3,
    TileLabel.GRASS: 4,
    TileLabel.STONE: 5,
    TileLabel.DOOR: 6,
    TileLabel.EMPTY: 0,
}

# Which layer each label belongs to
LABEL_TO_LAYER = {
    TileLabel.FLOOR: "Floor",
    TileLabel.STONE: "Floor",
    TileLabel.GRASS: "Floor",
    TileLabel.DOOR: "Floor",
    TileLabel.WALL: "Walls",
    TileLabel.WATER: "Water",
    TileLabel.EMPTY: None,  # not placed
}


def export_tmx(
    grid: list[list[str]],
    output_path: str,
    tileset_image: str = "tileset.png",
    tile_width: int = 16,
    tile_height: int = 16,
    tileset_columns: int = 8,
    tileset_tilecount: int = 64,
) -> str:
    """
    Export a label grid to a Tiled-compatible TMX file.
    
    Args:
        grid: 2D list of TileLabel strings [row][col].
        output_path: Where to write the .tmx file.
        tileset_image: Relative path to the tileset PNG.
        tile_width: Tile width in pixels.
        tile_height: Tile height in pixels.
    
    Returns:
        The output path.
    """
    height = len(grid)
    width = len(grid[0]) if grid else 0
    
    # Root map element
    map_el = ET.Element("map", {
        "version": "1.10",
        "tiledversion": "1.10.2",
        "orientation": "orthogonal",
        "renderorder": "right-down",
        "width": str(width),
        "height": str(height),
        "tilewidth": str(tile_width),
        "tileheight": str(tile_height),
        "infinite": "0",
    })
    
    # Tileset reference
    tileset_el = ET.SubElement(map_el, "tileset", {
        "firstgid": "1",
        "name": "mapbuilder_tiles",
        "tilewidth": str(tile_width),
        "tileheight": str(tile_height),
        "tilecount": str(tileset_tilecount),
        "columns": str(tileset_columns),
    })
    ET.SubElement(tileset_el, "image", {
        "source": tileset_image,
        "width": str(tileset_columns * tile_width),
        "height": str((tileset_tilecount // tileset_columns) * tile_height),
    })
    
    # Build layers from the grid
    layers = {"Floor": [], "Walls": [], "Water": []}
    
    for y, row in enumerate(grid):
        floor_row = []
        wall_row = []
        water_row = []
        for x, label in enumerate(row):
            tile_id = LABEL_TO_TILE_ID.get(label, 0)
            target_layer = LABEL_TO_LAYER.get(label)
            
            floor_row.append(tile_id if target_layer == "Floor" else 0)
            wall_row.append(tile_id if target_layer == "Walls" else 0)
            water_row.append(tile_id if target_layer == "Water" else 0)
        
        layers["Floor"].append(floor_row)
        layers["Walls"].append(wall_row)
        layers["Water"].append(water_row)
    
    # Write each layer
    layer_id = 1
    for layer_name, tile_data in layers.items():
        # Skip empty layers
        if all(all(t == 0 for t in row) for row in tile_data):
            continue
        
        layer_el = ET.SubElement(map_el, "layer", {
            "id": str(layer_id),
            "name": layer_name,
            "width": str(width),
            "height": str(height),
        })
        layer_id += 1
        
        # CSV data
        data_el = ET.SubElement(layer_el, "data", {"encoding": "csv"})
        csv_rows = []
        for row in tile_data:
            csv_rows.append(",".join(str(t) for t in row))
        data_el.text = "\n" + "\n".join(csv_rows) + "\n"
    
    # Write to file
    tree = ET.ElementTree(map_el)
    rough_string = ET.tostring(map_el, encoding="unicode")
    reparsed = minidom.parseString(rough_string)
    pretty = reparsed.toprettyxml(indent="  ")
    
    # Remove extra XML declaration from minidom
    lines = pretty.split("\n")
    if lines[0].startswith("<?xml"):
        lines = lines[1:]
    
    with open(output_path, "w") as f:
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        f.write("\n".join(lines))
    
    return output_path
