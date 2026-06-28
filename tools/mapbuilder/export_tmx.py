"""
Phase 3: TMX Export — Multi-Layer Grid → Tiled-compatible .tmx file.

Exports ALL layers from the editor:
- Ground → Floor/Walls/Water TMX layers (CollisionGrid-compatible names)
- Weather Zones → custom "Weather" layer with zone properties
- Exits/Entrances → "Exits" layer (spawn + exit markers with direction)
- Decorative → "Decorative" layer

Generated TMX is loadable by GAIME's TmxLoader. Layer names follow
CollisionGrid conventions so collision works out of the box.
"""

import xml.etree.ElementTree as ET
from xml.dom import minidom
from segment import TileLabel


# Ground labels → Tile IDs
GROUND_TILE_IDS = {
    "floor": 1,
    "wall": 2,
    "water": 3,
    "grass": 4,
    "stone": 5,
    "door": 6,
    "empty": 0,
}

# Weather zone IDs (stored as tile IDs in a custom layer)
WEATHER_TILE_IDS = {
    "rain_zone": 20,
    "snow_zone": 21,
    "leaves_zone": 22,
    "wind_zone": 23,
    "fog_zone": 24,
    "none": 0,
}

# Exit/entrance marker IDs
EXIT_TILE_IDS = {
    "spawn": 30,
    "exit_north": 31,
    "exit_south": 32,
    "exit_east": 33,
    "exit_west": 34,
    "none": 0,
}

# NPC Route marker IDs
NPC_ROUTE_TILE_IDS = {
    "npc_waypoint": 50,
    "npc_patrol_a": 51,
    "npc_patrol_b": 52,
    "npc_patrol_c": 53,
    "npc_idle_spot": 54,
    "npc_spawn": 55,
    "none": 0,
}

# Interactive object IDs
INTERACT_TILE_IDS = {
    "lever": 60,
    "button": 61,
    "gate": 62,
    "npc_talk": 63,
    "shop": 64,
    "save_point": 65,
    "pickup": 66,
    "trap": 67,
    "none": 0,
}

# Decorative IDs
DECO_TILE_IDS = {
    "tree": 40,
    "rock": 41,
    "flower": 42,
    "torch": 43,
    "chest": 44,
    "sign": 45,
    "none": 0,
}

# Ground labels → which TMX layer they belong to
GROUND_LAYER_MAPPING = {
    "floor": "Floor",
    "stone": "Floor",
    "grass": "Floor",
    "door": "Floor",
    "wall": "Walls",
    "water": "Water",
    "empty": None,
}


def export_tmx(
    layers: dict,
    output_path: str,
    tileset_image: str = "tileset.png",
    tile_width: int = 16,
    tile_height: int = 16,
    tileset_columns: int = 8,
    tileset_tilecount: int = 64,
) -> str:
    """
    Export multi-layer grids to a Tiled-compatible TMX file.
    
    Args:
        layers: Dict with keys "ground", "weather", "exits", "decorative".
                Each value is a 2D list of label strings [row][col].
                Can also be a plain 2D list (legacy: treated as ground only).
        output_path: Where to write the .tmx file.
    
    Returns:
        The output path.
    """
    # Handle legacy format (plain grid = ground only)
    if isinstance(layers, list):
        layers = {"ground": layers}
    
    ground = layers.get("ground", [])
    weather = layers.get("weather")
    exits = layers.get("exits")
    npc_routes = layers.get("npc_routes")
    interactable = layers.get("interactable")
    decorative = layers.get("decorative")
    
    height = len(ground)
    width = len(ground[0]) if ground else 0
    
    if height == 0 or width == 0:
        raise ValueError("Ground layer is empty")
    
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
    
    # --- Ground layers (Floor, Walls, Water) ---
    floor_data = [[0] * width for _ in range(height)]
    wall_data = [[0] * width for _ in range(height)]
    water_data = [[0] * width for _ in range(height)]
    
    for y, row in enumerate(ground):
        for x, label in enumerate(row):
            tile_id = GROUND_TILE_IDS.get(label, 0)
            target = GROUND_LAYER_MAPPING.get(label)
            if target == "Floor":
                floor_data[y][x] = tile_id
            elif target == "Walls":
                wall_data[y][x] = tile_id
            elif target == "Water":
                water_data[y][x] = tile_id
    
    layer_id = 1
    layer_id = _write_layer(map_el, "Floor", floor_data, width, height, layer_id)
    layer_id = _write_layer(map_el, "Walls", wall_data, width, height, layer_id)
    layer_id = _write_layer(map_el, "Water", water_data, width, height, layer_id)
    
    # --- Weather Zones layer ---
    if weather:
        weather_data = [[WEATHER_TILE_IDS.get(cell, 0) for cell in row] for row in weather]
        layer_id = _write_layer(map_el, "Weather_Zones", weather_data, width, height, layer_id)
    
    # --- Exits / Entrances layer ---
    if exits:
        exit_data = [[EXIT_TILE_IDS.get(cell, 0) for cell in row] for row in exits]
        layer_id = _write_layer(map_el, "Exits", exit_data, width, height, layer_id)
    
    # --- NPC Routes layer ---
    if npc_routes:
        npc_data = [[NPC_ROUTE_TILE_IDS.get(cell, 0) for cell in row] for row in npc_routes]
        layer_id = _write_layer(map_el, "NPC_Routes", npc_data, width, height, layer_id)
    
    # --- Interactive Objects layer ---
    if interactable:
        interact_data = [[INTERACT_TILE_IDS.get(cell, 0) for cell in row] for row in interactable]
        layer_id = _write_layer(map_el, "Interactive", interact_data, width, height, layer_id)
    
    # --- Decorative layer ---
    if decorative:
        deco_data = [[DECO_TILE_IDS.get(cell, 0) for cell in row] for row in decorative]
        layer_id = _write_layer(map_el, "Decorative", deco_data, width, height, layer_id)
    
    # --- Write file ---
    rough_string = ET.tostring(map_el, encoding="unicode")
    reparsed = minidom.parseString(rough_string)
    pretty = reparsed.toprettyxml(indent="  ")
    
    lines = pretty.split("\n")
    if lines[0].startswith("<?xml"):
        lines = lines[1:]
    
    with open(output_path, "w") as f:
        f.write('<?xml version="1.0" encoding="UTF-8"?>\n')
        f.write("\n".join(lines))
    
    return output_path


def _write_layer(map_el, name: str, data: list, width: int, height: int, layer_id: int) -> int:
    """Write a single tile layer. Skips if all zeros. Returns next layer_id."""
    if all(all(t == 0 for t in row) for row in data):
        return layer_id
    
    layer_el = ET.SubElement(map_el, "layer", {
        "id": str(layer_id),
        "name": name,
        "width": str(width),
        "height": str(height),
    })
    
    data_el = ET.SubElement(layer_el, "data", {"encoding": "csv"})
    csv_rows = [",".join(str(t) for t in row) for row in data]
    data_el.text = "\n" + "\n".join(csv_rows) + "\n"
    
    return layer_id + 1
