"""
GAIME Map Builder — Web UI

A Flask web app that provides:
1. Upload a photo/sketch/floor plan
2. Automatic segmentation into tile labels
3. Interactive paint-over editor (change tiles by clicking/dragging)
4. Export as TMX (Tiled-compatible, loadable by GAIME's TmxLoader)

Run: python app.py
Open: http://localhost:5000
"""

import os
import json
import io
import base64
import tempfile
from pathlib import Path

import cv2
import numpy as np
from PIL import Image
from flask import Flask, render_template, request, jsonify, send_file

from segment import segment_image, grid_to_preview, TileLabel
from wfc import apply_variants
from export_tmx import export_tmx, GROUND_TILE_IDS
from learn import MapLearner, MapGenerator

# Repository root (for loading project TMX files)
REPO_ROOT = Path(__file__).parent.parent.parent

app = Flask(__name__,
            template_folder="editor/templates",
            static_folder="editor/static")

# Store current session state (single-user tool)
SESSION = {
    "grid": None,
    "layers": None,
    "original_image": None,
    "grid_width": 32,
    "grid_height": 24,
}

# AI learner (persists across requests)
LEARNER = MapLearner()
RULES_PATH = Path(__file__).parent / "learned_rules.json"
if RULES_PATH.exists():
    LEARNER.load(str(RULES_PATH))


@app.route("/")
def index():
    """Main editor page."""
    return render_template("index.html")


@app.route("/api/upload", methods=["POST"])
def upload_image():
    """Upload an image and run segmentation."""
    if "image" not in request.files:
        return jsonify({"error": "No image uploaded"}), 400
    
    file = request.files["image"]
    grid_width = int(request.form.get("grid_width", 32))
    grid_height = int(request.form.get("grid_height", 24))
    
    # Save to temp file
    with tempfile.NamedTemporaryFile(suffix=".png", delete=False) as tmp:
        file.save(tmp.name)
        tmp_path = tmp.name
    
    try:
        # Run segmentation
        grid = segment_image(tmp_path, grid_width, grid_height)
        SESSION["grid"] = grid
        SESSION["grid_width"] = grid_width
        SESSION["grid_height"] = grid_height
        
        # Generate preview image
        preview = grid_to_preview(grid, cell_size=16)
        _, png_buf = cv2.imencode(".png", preview)
        preview_b64 = base64.b64encode(png_buf).decode("utf-8")
        
        # Store original image as base64 for the editor ghost layer
        img = cv2.imread(tmp_path)
        img_resized = cv2.resize(img, (grid_width * 16, grid_height * 16))
        _, orig_buf = cv2.imencode(".png", img_resized)
        original_b64 = base64.b64encode(orig_buf).decode("utf-8")
        SESSION["original_image"] = original_b64
        
        return jsonify({
            "grid": grid,
            "preview": preview_b64,
            "original": original_b64,
            "width": grid_width,
            "height": grid_height,
        })
    finally:
        os.unlink(tmp_path)


@app.route("/api/update_grid", methods=["POST"])
def update_grid():
    """Update all layer grids after user paints tiles."""
    data = request.get_json()
    # Multi-layer format: {ground: [...], weather: [...], exits: [...], decorative: [...]}
    if "ground" in data:
        SESSION["layers"] = data
        SESSION["grid"] = data.get("ground")
    elif "grid" in data:
        # Legacy single-grid format
        SESSION["grid"] = data["grid"]
        SESSION["layers"] = {"ground": data["grid"]}
    return jsonify({"ok": True})


@app.route("/api/export_tmx", methods=["POST"])
def do_export_tmx():
    """Export all layers as TMX file and return for download."""
    layers = SESSION.get("layers") or {"ground": SESSION.get("grid")}
    if not layers or not layers.get("ground"):
        return jsonify({"error": "No grid loaded"}), 400
    
    with tempfile.NamedTemporaryFile(suffix=".tmx", delete=False, mode="w") as tmp:
        export_tmx(layers, tmp.name)
        tmp_path = tmp.name
    
    try:
        return send_file(
            tmp_path,
            mimetype="application/xml",
            as_attachment=True,
            download_name="generated_map.tmx",
        )
    finally:
        pass


@app.route("/api/load_tmx", methods=["POST"])
def load_tmx():
    """
    Load an existing TMX map into the editor.
    Parses the TMX, extracts layers, and returns them for editing.
    Supports both uploaded TMX files and project-internal TMX paths.
    """
    # Option 1: uploaded file
    if "tmx_file" in request.files:
        content = request.files["tmx_file"].read().decode("utf-8")
    # Option 2: path to a project TMX (relative to repo root)
    elif request.is_json and "path" in request.get_json():
        tmx_path = request.get_json()["path"]
        full_path = REPO_ROOT / tmx_path
        if not full_path.exists():
            return jsonify({"error": f"TMX not found: {tmx_path}"}), 404
        content = full_path.read_text()
    else:
        return jsonify({"error": "No TMX file provided"}), 400
    
    try:
        from tmx_loader import parse_tmx_to_layers
        layers, width, height = parse_tmx_to_layers(content)
    except Exception as e:
        return jsonify({"error": f"Parse failed: {str(e)}"}), 400
    
    SESSION["layers"] = layers
    SESSION["grid"] = layers.get("ground")
    SESSION["grid_width"] = width
    SESSION["grid_height"] = height
    
    return jsonify({
        "layers": layers,
        "width": width,
        "height": height,
    })


@app.route("/api/project_maps")
def list_project_maps():
    """List all TMX maps available in the GAIME project."""
    maps = []
    locations_dir = REPO_ROOT / "assets" / "HD" / "locations"
    if locations_dir.exists():
        for loc in sorted(locations_dir.iterdir()):
            if not loc.is_dir():
                continue
            for tmx in sorted(loc.rglob("*.tmx")):
                rel_path = str(tmx.relative_to(REPO_ROOT))
                name = f"{loc.name}/{tmx.name}"
                maps.append({"name": name, "path": rel_path})
    return jsonify(maps)
    """Return available tile labels for the painter palette."""
    labels = [
        {"id": TileLabel.FLOOR, "name": "Floor", "color": "#c8b48c"},
        {"id": TileLabel.WALL, "name": "Wall", "color": "#3c3c3c"},
        {"id": TileLabel.WATER, "name": "Water", "color": "#2878b4"},
        {"id": TileLabel.GRASS, "name": "Grass", "color": "#50b450"},
        {"id": TileLabel.STONE, "name": "Stone", "color": "#b4b4b4"},
        {"id": TileLabel.DOOR, "name": "Door", "color": "#c83c3c"},
        {"id": TileLabel.EMPTY, "name": "Empty", "color": "#000000"},
    ]
    return jsonify(labels)


@app.route("/api/learn", methods=["POST"])
def learn_from_map():
    """
    Feed the current map to the AI learner.
    Call this on 1-2 hand-painted example maps, then generate new ones.
    """
    layers = SESSION.get("layers") or {"ground": SESSION.get("grid")}
    if not layers or not layers.get("ground"):
        return jsonify({"error": "No map to learn from. Paint a map first."}), 400
    
    LEARNER.learn(layers)
    LEARNER.save(str(RULES_PATH))
    
    # Stats about what was learned
    ground_tiles = len(LEARNER.frequency.get("ground", {}))
    total_rules = sum(
        sum(len(neighbors) for neighbors in dirs.values())
        for tiles in LEARNER.adjacency.values()
        for tile, dirs in tiles.items()
    )
    
    return jsonify({
        "ok": True,
        "message": f"Learned! {ground_tiles} tile types, {total_rules} adjacency rules extracted.",
        "examples_fed": sum(LEARNER.frequency["ground"].values()) if "ground" in LEARNER.frequency else 0,
    })


@app.route("/api/generate", methods=["POST"])
def generate_map():
    """
    Generate a new map using the learned rules (WFC).
    Requires at least one map to have been fed via /api/learn first.
    """
    if not LEARNER.frequency:
        return jsonify({"error": "No rules learned yet. Paint 1-2 maps and click 'Teach AI' first."}), 400
    
    data = request.get_json() or {}
    width = data.get("width") or SESSION.get("grid_width", 32)
    height = data.get("height") or SESSION.get("grid_height", 24)
    seed = data.get("seed")
    
    generator = MapGenerator(LEARNER, seed=seed)
    layers = generator.generate(width=width, height=height)
    
    SESSION["layers"] = layers
    SESSION["grid"] = layers.get("ground")
    SESSION["grid_width"] = width
    SESSION["grid_height"] = height
    
    return jsonify({
        "layers": layers,
        "width": width,
        "height": height,
    })


@app.route("/api/learn_status")
def learn_status():
    """Check if the AI has learned any rules."""
    if not LEARNER.frequency:
        return jsonify({"learned": False, "message": "No rules yet. Paint a map and click 'Teach AI'."})
    
    ground_tiles = len(LEARNER.frequency.get("ground", {}))
    total_cells = sum(LEARNER.frequency["ground"].values()) if "ground" in LEARNER.frequency else 0
    
    return jsonify({
        "learned": True,
        "tile_types": ground_tiles,
        "cells_analyzed": total_cells,
        "dimensions": {
            "min_width": LEARNER.min_width,
            "max_width": LEARNER.max_width,
            "min_height": LEARNER.min_height,
            "max_height": LEARNER.max_height,
        },
    })


@app.route("/api/vision_annotate", methods=["POST"])
def vision_annotate():
    """
    Use a Vision LLM to auto-annotate the current map.
    Renders the map, sends it to the LLM, returns layer annotations.
    
    Requires one of: OPENAI_API_KEY, ANTHROPIC_API_KEY, or local Ollama.
    """
    from vision_annotate import annotate_with_vision
    
    layers = SESSION.get("layers") or {"ground": SESSION.get("grid")}
    if not layers or not layers.get("ground"):
        return jsonify({"error": "No map loaded. Load a map first."}), 400
    
    data = request.get_json() or {}
    mode = data.get("mode", "hydrology")
    
    # Detect available backend
    backend = _detect_vision_backend()
    if not backend:
        return jsonify({
            "error": "No Vision AI available. Set OPENAI_API_KEY or ANTHROPIC_API_KEY, or run Ollama locally with a vision model (llava)."
        }), 400
    
    try:
        annotations = annotate_with_vision(layers, mode=mode, backend=backend)
        return jsonify({"annotations": annotations, "backend": backend})
    except Exception as e:
        return jsonify({"error": f"Vision annotation failed: {str(e)}"}), 500


@app.route("/api/full_auto", methods=["POST"])
def full_auto():
    """
    FULL AUTO MODE: Zero human interaction.
    
    1. Loads all specified project maps
    2. For each: parses TMX → Vision-annotates hydrology/exits/NPCs
    3. Feeds all annotated maps to the learner (extracts rules)
    4. Generates a brand new map using the learned rules
    5. Returns the generated map ready for export
    
    The user just clicks one button. That's it.
    """
    from vision_annotate import annotate_with_vision
    from tmx_loader import parse_tmx_to_layers
    
    data = request.get_json() or {}
    map_paths = data.get("maps", [])
    gen_width = data.get("generate_width", 32)
    gen_height = data.get("generate_height", 24)
    
    if not map_paths:
        return jsonify({"error": "No maps specified"}), 400
    
    # Detect vision backend
    backend = _detect_vision_backend()
    
    # Reset learner for fresh run
    learner = MapLearner()
    maps_analyzed = 0
    
    for tmx_path in map_paths:
        full_path = REPO_ROOT / tmx_path
        if not full_path.exists():
            continue
        
        try:
            # Parse TMX
            content = full_path.read_text()
            layers, w, h = parse_tmx_to_layers(content)
            
            # Vision annotate (if backend available)
            if backend and w <= 64 and h <= 64:  # skip huge maps for speed
                try:
                    annotations = annotate_with_vision(layers, mode="full", backend=backend)
                    for key, layer_data in annotations.items():
                        if key in layers:
                            layers[key] = layer_data
                except Exception:
                    pass  # Vision failed for this map, continue without
            else:
                # No vision backend: auto-derive hydrology heuristically
                from learn import MapGenerator
                temp_gen = MapGenerator(MapLearner())
                layers["weather"] = temp_gen._auto_hydrology(layers["ground"], w, h)
            
            # Learn from this annotated map
            learner.learn(layers)
            maps_analyzed += 1
            
        except Exception:
            continue  # Skip broken maps
    
    if maps_analyzed == 0:
        return jsonify({"error": "Could not process any maps"}), 400
    
    # Save learned rules
    learner.save(str(RULES_PATH))
    global LEARNER
    LEARNER = learner
    
    # Generate new map
    generator = MapGenerator(learner, seed=None)
    new_layers = generator.generate(width=gen_width, height=gen_height)
    
    # Store in session
    SESSION["layers"] = new_layers
    SESSION["grid"] = new_layers.get("ground")
    SESSION["grid_width"] = gen_width
    SESSION["grid_height"] = gen_height
    
    # Count rules
    total_rules = sum(
        sum(len(neighbors) for neighbors in dirs.values())
        for tiles in learner.adjacency.values()
        for tile, dirs in tiles.items()
    )
    
    return jsonify({
        "layers": new_layers,
        "width": gen_width,
        "height": gen_height,
        "maps_analyzed": maps_analyzed,
        "rules_extracted": total_rules,
    })


def _detect_vision_backend() -> str:
    """Detect which Vision AI backend is available."""
    if os.environ.get("OPENAI_API_KEY"):
        return "openai"
    if os.environ.get("ANTHROPIC_API_KEY"):
        return "anthropic"
    try:
        import urllib.request
        urllib.request.urlopen("http://localhost:11434/api/tags", timeout=2)
        return "ollama"
    except Exception:
        return ""


if __name__ == "__main__":
    print("\n  GAIME Map Builder")
    print("  Open http://localhost:5000 in your browser")
    print("  ----------------------------------------\n")
    app.run(host="0.0.0.0", port=5000, debug=True)
