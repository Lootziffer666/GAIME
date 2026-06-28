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


@app.route("/api/labels")
def get_labels():
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


if __name__ == "__main__":
    print("\n  GAIME Map Builder")
    print("  Open http://localhost:5000 in your browser")
    print("  ----------------------------------------\n")
    app.run(host="0.0.0.0", port=5000, debug=True)
