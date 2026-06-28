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
from export_tmx import export_tmx, LABEL_TO_TILE_ID

app = Flask(__name__,
            template_folder="editor/templates",
            static_folder="editor/static")

# Store current session state (single-user tool)
SESSION = {
    "grid": None,
    "original_image": None,
    "grid_width": 32,
    "grid_height": 24,
}


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
    """Update the grid after user paints tiles."""
    data = request.get_json()
    SESSION["grid"] = data["grid"]
    return jsonify({"ok": True})


@app.route("/api/export_tmx", methods=["POST"])
def do_export_tmx():
    """Export current grid as TMX file and return for download."""
    grid = SESSION.get("grid")
    if not grid:
        return jsonify({"error": "No grid loaded"}), 400
    
    with tempfile.NamedTemporaryFile(suffix=".tmx", delete=False, mode="w") as tmp:
        export_tmx(grid, tmp.name)
        tmp_path = tmp.name
    
    try:
        return send_file(
            tmp_path,
            mimetype="application/xml",
            as_attachment=True,
            download_name="generated_map.tmx",
        )
    finally:
        # Cleanup after send (small delay ok for temp files)
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


if __name__ == "__main__":
    print("\n  GAIME Map Builder")
    print("  Open http://localhost:5000 in your browser")
    print("  ----------------------------------------\n")
    app.run(host="0.0.0.0", port=5000, debug=True)
