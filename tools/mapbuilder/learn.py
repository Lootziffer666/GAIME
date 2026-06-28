"""
AI Map Generator — learns rules from 1-2 example maps, generates new ones.

The core idea: Wave Function Collapse doesn't need training data or GPUs.
It extracts ADJACENCY RULES from example maps and uses constraint propagation
to generate new maps that follow the same patterns.

Pipeline:
1. LEARN: Load 1-2 hand-painted example maps (from the editor's JSON/layers)
2. EXTRACT: For each layer, extract which tiles can be neighbors (adjacency rules)
3. GENERATE: Use WFC to create new maps of arbitrary size following those rules
4. HYDROLOGY: Auto-derive drain/puddle/flow from elevation + ground materials
5. EXITS: Place exits at walkable edges, spawn at center
6. NPCs/INTERACTABLES: Place based on learned density + ground-type affinity

No neural network. No GPU. Pure constraint satisfaction.
"""

import json
import random
from collections import defaultdict, Counter
from typing import Optional


class MapLearner:
    """
    Learns adjacency rules + placement patterns from example maps.
    
    Given 1-2 example multi-layer maps (as produced by the editor), extracts:
    - Per-layer tile adjacency rules (which tiles can be N/S/E/W of each other)
    - Tile frequency (how common each tile is)
    - Cross-layer correlations (e.g., "torch" appears on "floor" not "water")
    - Hydrology patterns (drains near walls, puddles in open areas)
    - NPC placement affinity (NPCs prefer floor tiles, not walls)
    """
    
    def __init__(self):
        # Per-layer adjacency rules: {layer: {tile: {direction: {neighbor_tile: count}}}}
        self.adjacency = defaultdict(lambda: defaultdict(lambda: defaultdict(Counter)))
        # Per-layer tile frequency: {layer: Counter}
        self.frequency = defaultdict(Counter)
        # Cross-layer correlation: {(upper_layer, upper_tile): {lower_layer_tile: count}}
        self.cross_layer = defaultdict(Counter)
        # Learned dimensions range
        self.min_width = 999
        self.max_width = 0
        self.min_height = 999
        self.max_height = 0
    
    def learn(self, layers: dict):
        """
        Learn rules from one example map.
        Call multiple times for multiple examples (rules accumulate).
        
        Args:
            layers: Dict of {layer_name: 2D list of labels}
        """
        ground = layers.get("ground", [])
        if not ground:
            return
        
        height = len(ground)
        width = len(ground[0]) if ground else 0
        
        self.min_width = min(self.min_width, width)
        self.max_width = max(self.max_width, width)
        self.min_height = min(self.min_height, height)
        self.max_height = max(self.max_height, height)
        
        DIRECTIONS = [("N", 0, -1), ("S", 0, 1), ("E", 1, 0), ("W", -1, 0)]
        
        for layer_name, grid in layers.items():
            if not grid or not grid[0]:
                continue
            h = len(grid)
            w = len(grid[0])
            
            for y in range(h):
                for x in range(w):
                    tile = grid[y][x]
                    self.frequency[layer_name][tile] += 1
                    
                    # Adjacency rules
                    for dir_name, dx, dy in DIRECTIONS:
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < w and 0 <= ny < h:
                            neighbor = grid[ny][nx]
                            self.adjacency[layer_name][tile][dir_name][neighbor] += 1
            
            # Cross-layer correlation (other layers vs ground)
            if layer_name != "ground" and "ground" in layers:
                for y in range(min(h, height)):
                    for x in range(min(w, width)):
                        upper_tile = grid[y][x]
                        if upper_tile != "none" and upper_tile != "empty":
                            ground_tile = ground[y][x]
                            self.cross_layer[(layer_name, upper_tile)][ground_tile] += 1
    
    def get_allowed_neighbors(self, layer: str, tile: str, direction: str) -> list:
        """Get tiles that can be adjacent to [tile] in [direction]."""
        counts = self.adjacency[layer][tile][direction]
        if not counts:
            # Fallback: allow anything from this layer's known tiles
            return list(self.frequency[layer].keys())
        return list(counts.keys())
    
    def get_weighted_neighbors(self, layer: str, tile: str, direction: str) -> list:
        """Get (tile, weight) pairs for neighbors."""
        counts = self.adjacency[layer][tile][direction]
        if not counts:
            freq = self.frequency[layer]
            total = sum(freq.values())
            return [(t, c / total) for t, c in freq.items()]
        total = sum(counts.values())
        return [(t, c / total) for t, c in counts.items()]
    
    def get_ground_affinity(self, layer: str, tile: str) -> dict:
        """Which ground tiles does this upper-layer tile prefer?"""
        counts = self.cross_layer.get((layer, tile), Counter())
        if not counts:
            return {}
        total = sum(counts.values())
        return {t: c / total for t, c in counts.items()}
    
    def save(self, path: str):
        """Save learned rules to JSON."""
        data = {
            "adjacency": {
                layer: {
                    tile: {
                        direction: dict(neighbors)
                        for direction, neighbors in dirs.items()
                    }
                    for tile, dirs in tiles.items()
                }
                for layer, tiles in self.adjacency.items()
            },
            "frequency": {layer: dict(counts) for layer, counts in self.frequency.items()},
            "cross_layer": {
                f"{k[0]}|{k[1]}": dict(v)
                for k, v in self.cross_layer.items()
            },
            "dimensions": {
                "min_width": self.min_width, "max_width": self.max_width,
                "min_height": self.min_height, "max_height": self.max_height,
            },
        }
        with open(path, "w") as f:
            json.dump(data, f, indent=2)
    
    def load(self, path: str):
        """Load previously saved rules."""
        with open(path) as f:
            data = json.load(f)
        
        for layer, tiles in data.get("adjacency", {}).items():
            for tile, dirs in tiles.items():
                for direction, neighbors in dirs.items():
                    for n, count in neighbors.items():
                        self.adjacency[layer][tile][direction][n] = count
        
        for layer, counts in data.get("frequency", {}).items():
            for tile, count in counts.items():
                self.frequency[layer][tile] = count
        
        for key_str, counts in data.get("cross_layer", {}).items():
            parts = key_str.split("|", 1)
            if len(parts) == 2:
                key = (parts[0], parts[1])
                for tile, count in counts.items():
                    self.cross_layer[key][tile] = count
        
        dims = data.get("dimensions", {})
        self.min_width = dims.get("min_width", 20)
        self.max_width = dims.get("max_width", 40)
        self.min_height = dims.get("min_height", 15)
        self.max_height = dims.get("max_height", 30)


class MapGenerator:
    """
    Generates new maps using learned rules via Wave Function Collapse.
    
    WFC simplified for tile-label grids:
    1. Start with all cells in superposition (all possible labels)
    2. Collapse the cell with lowest entropy (fewest possibilities)
    3. Propagate constraints to neighbors
    4. Repeat until all cells are collapsed
    5. Apply cross-layer rules for upper layers
    """
    
    def __init__(self, learner: MapLearner, seed: int = None):
        self.learner = learner
        self.rng = random.Random(seed)
    
    def generate(
        self,
        width: Optional[int] = None,
        height: Optional[int] = None,
    ) -> dict:
        """
        Generate a complete multi-layer map.
        
        Args:
            width: Map width (None = random within learned range)
            height: Map height (None = random within learned range)
        
        Returns:
            Dict of {layer_name: 2D list of labels}
        """
        if width is None:
            width = self.rng.randint(self.learner.min_width, self.learner.max_width)
        if height is None:
            height = self.rng.randint(self.learner.min_height, self.learner.max_height)
        
        # Generate ground layer first (foundation)
        ground = self._generate_layer("ground", width, height)
        
        # Generate upper layers using cross-layer affinity
        result = {"ground": ground}
        
        for layer_name in ["weather", "exits", "npc_routes", "interactable", "decorative"]:
            if layer_name in self.learner.frequency and len(self.learner.frequency[layer_name]) > 1:
                upper = self._generate_upper_layer(layer_name, ground, width, height)
                result[layer_name] = upper
            else:
                result[layer_name] = [["none"] * width for _ in range(height)]
        
        # Auto-derive hydrology if not learned
        if "weather" not in self.learner.frequency or len(self.learner.frequency["weather"]) <= 1:
            result["weather"] = self._auto_hydrology(ground, width, height)
        
        # Ensure at least one spawn point
        self._ensure_spawn(result, width, height)
        
        return result
    
    def _generate_layer(self, layer: str, width: int, height: int) -> list:
        """Generate one layer using WFC with learned adjacency rules."""
        freq = self.learner.frequency.get(layer, Counter())
        if not freq:
            return [["floor"] * width for _ in range(height)]
        
        all_tiles = list(freq.keys())
        total = sum(freq.values())
        weights = {t: c / total for t, c in freq.items()}
        
        # Initialize: all cells can be anything
        possibilities = [[set(all_tiles) for _ in range(width)] for _ in range(height)]
        grid = [[None for _ in range(width)] for _ in range(height)]
        
        # WFC loop
        uncollapsed = width * height
        max_iterations = width * height * 3  # safety limit
        iterations = 0
        
        while uncollapsed > 0 and iterations < max_iterations:
            iterations += 1
            
            # Find cell with lowest entropy (fewest possibilities > 1)
            min_entropy = float('inf')
            min_cell = None
            
            for y in range(height):
                for x in range(width):
                    if grid[y][x] is not None:
                        continue
                    entropy = len(possibilities[y][x])
                    if entropy == 0:
                        # Contradiction! Reset this cell to all possibilities
                        possibilities[y][x] = set(all_tiles)
                        entropy = len(all_tiles)
                    if entropy < min_entropy:
                        min_entropy = entropy
                        min_cell = (x, y)
            
            if min_cell is None:
                break
            
            x, y = min_cell
            
            # Collapse: pick a tile weighted by frequency
            possible = list(possibilities[y][x])
            tile_weights = [weights.get(t, 0.01) for t in possible]
            total_w = sum(tile_weights)
            tile_weights = [w / total_w for w in tile_weights]
            
            chosen = self.rng.choices(possible, weights=tile_weights, k=1)[0]
            grid[y][x] = chosen
            possibilities[y][x] = {chosen}
            uncollapsed -= 1
            
            # Propagate constraints to neighbors
            self._propagate(layer, grid, possibilities, x, y, width, height)
        
        # Fill any remaining None cells with most common tile
        most_common = freq.most_common(1)[0][0] if freq else "floor"
        for y in range(height):
            for x in range(width):
                if grid[y][x] is None:
                    grid[y][x] = most_common
        
        return grid
    
    def _propagate(self, layer, grid, possibilities, x, y, width, height):
        """Propagate constraints from collapsed cell to neighbors."""
        DIRECTIONS = [("N", 0, -1, "S"), ("S", 0, 1, "N"), ("E", 1, 0, "W"), ("W", -1, 0, "E")]
        
        stack = [(x, y)]
        while stack:
            cx, cy = stack.pop()
            current_tile = grid[cy][cx]
            if current_tile is None:
                continue
            
            for dir_name, dx, dy, reverse_dir in DIRECTIONS:
                nx, ny = cx + dx, cy + dy
                if nx < 0 or nx >= width or ny < 0 or ny >= height:
                    continue
                if grid[ny][nx] is not None:
                    continue
                
                # Get allowed neighbors for current tile in this direction
                allowed = set(self.learner.get_allowed_neighbors(layer, current_tile, dir_name))
                
                old_count = len(possibilities[ny][nx])
                possibilities[ny][nx] &= allowed
                
                # If possibilities reduced, propagate further
                if len(possibilities[ny][nx]) < old_count and len(possibilities[ny][nx]) > 0:
                    stack.append((nx, ny))
    
    def _generate_upper_layer(self, layer: str, ground: list, width: int, height: int) -> list:
        """Generate an upper layer respecting cross-layer affinity with ground."""
        grid = [["none"] * width for _ in range(height)]
        
        freq = self.learner.frequency[layer]
        # Calculate placement density (how many non-none cells vs total)
        total = sum(freq.values())
        none_count = freq.get("none", 0)
        density = 1.0 - (none_count / total) if total > 0 else 0.1
        
        for y in range(height):
            for x in range(width):
                if self.rng.random() > density:
                    continue
                
                ground_tile = ground[y][x]
                
                # Pick a tile based on frequency + ground affinity
                candidates = []
                for tile, count in freq.items():
                    if tile == "none":
                        continue
                    affinity = self.learner.get_ground_affinity(layer, tile)
                    # Boost weight if this tile likes this ground type
                    ground_bonus = affinity.get(ground_tile, 0.1)
                    candidates.append((tile, count * ground_bonus))
                
                if candidates:
                    tiles, tile_weights = zip(*candidates)
                    chosen = self.rng.choices(list(tiles), weights=list(tile_weights), k=1)[0]
                    grid[y][x] = chosen
        
        return grid
    
    def _auto_hydrology(self, ground: list, width: int, height: int) -> list:
        """
        Auto-derive hydrology from ground material:
        - Floor/stone near walls → sheltered
        - Open grass/floor → exposed
        - Low points (surrounded by floor) → puddle_zone
        - Near water → drain (water flows into existing water bodies)
        """
        hydro = [["none"] * width for _ in range(height)]
        
        for y in range(height):
            for x in range(width):
                g = ground[y][x]
                
                if g in ("wall", "empty"):
                    continue
                
                # Check neighbors
                neighbors = []
                for dy, dx in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
                    ny, nx = y + dy, x + dx
                    if 0 <= ny < height and 0 <= nx < width:
                        neighbors.append(ground[ny][nx])
                
                wall_neighbors = sum(1 for n in neighbors if n == "wall")
                water_neighbors = sum(1 for n in neighbors if n == "water")
                
                if wall_neighbors >= 2:
                    hydro[y][x] = "sheltered"
                elif water_neighbors > 0 and g != "water":
                    hydro[y][x] = "drain"
                elif g == "grass" and wall_neighbors == 0:
                    hydro[y][x] = "exposed"
                elif g == "floor" and wall_neighbors == 0:
                    # Open floor = potential puddle zone
                    if self.rng.random() < 0.3:
                        hydro[y][x] = "puddle_zone"
                    else:
                        hydro[y][x] = "exposed"
        
        return hydro
    
    def _ensure_spawn(self, layers: dict, width: int, height: int):
        """Ensure at least one spawn point exists on walkable ground."""
        exits_layer = layers.get("exits")
        if exits_layer is None:
            exits_layer = [["none"] * width for _ in range(height)]
            layers["exits"] = exits_layer
        
        # Check if spawn already exists
        for row in exits_layer:
            if "spawn" in row:
                return
        
        # Find center-ish walkable tile
        ground = layers["ground"]
        cx, cy = width // 2, height // 2
        
        # Spiral outward from center to find walkable
        for radius in range(max(width, height)):
            for dy in range(-radius, radius + 1):
                for dx in range(-radius, radius + 1):
                    if abs(dx) + abs(dy) != radius:
                        continue
                    nx, ny = cx + dx, cy + dy
                    if 0 <= nx < width and 0 <= ny < height:
                        if ground[ny][nx] in ("floor", "grass", "stone"):
                            exits_layer[ny][nx] = "spawn"
                            return
