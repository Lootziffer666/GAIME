# GAIME Map Builder

**Photo -> Room Segmentation -> Paint Editor -> TMX Export**

A web-based tool that converts photos, sketches, or floor plans into playable Tiled-compatible (TMX) tilemaps for GAIME.

## What it does

1. **Upload** a photo, sketch, or floor plan
2. **Auto-segment** — OpenCV analyzes the image and classifies each tile cell:
   - Walls (dark edges, boundaries)
   - Floor (warm brown/wood tones)
   - Water (blue areas)
   - Grass (green areas)
   - Stone (light grey, low saturation)
3. **Paint / Correct** — Interactive canvas editor to fix/refine the auto-detection:
   - Brush tool (click & drag)
   - Fill tool (flood-fill)
   - Eraser
   - Ghost image overlay (original photo visible underneath)
   - Keyboard shortcuts: B=Brush, F=Fill, E=Eraser
4. **Export TMX** — Download a Tiled-compatible .tmx file loadable by GAIME's `TmxLoader`

## Quick Start

```bash
cd tools/mapbuilder
pip install -r requirements.txt
python app.py
```

Open **http://localhost:5000** in your browser.

## Architecture

```
tools/mapbuilder/
+-- app.py               Flask web server (entry point)
+-- segment.py           Phase 1: Image -> Label Grid (OpenCV)
+-- wfc.py               Phase 2: WFC tile variant assignment
+-- export_tmx.py        Phase 3: Label Grid -> TMX file
+-- requirements.txt     Python dependencies
+-- editor/
|   +-- templates/
|   |   +-- index.html   Main UI page
|   +-- static/
|       +-- painter.js   Canvas-based tile painter
|       +-- style.css    Dark-theme styling
+-- examples/            Sample inputs/outputs
```

## How segmentation works

No ML models or GPUs required. Pure OpenCV heuristics:

1. **Resize** input image to target grid dimensions (1 pixel = 1 tile)
2. **HSV color analysis** — classify each cell by dominant hue/saturation/value:
   - Blue (H:90-130) -> Water
   - Green (H:35-85) -> Grass
   - Dark (V<40) -> Wall
   - Light unsaturated (V>200, S<30) -> Stone
   - Warm brown (H:10-35) -> Floor
3. **Edge detection** (Canny) — strong edges -> Wall candidates
4. **Cleanup** — remove isolated single-tile noise (majority-neighbor filter)

## TMX Compatibility

Generated maps use layer names compatible with GAIME's `CollisionGrid`:
- `Floor` layer -> WALKABLE tiles
- `Walls` layer -> BLOCKED tiles (SOLID)
- `Water` layer -> WATER tiles

The TMX format is standard Tiled XML with CSV encoding — editable in Tiled afterwards.

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| B | Brush tool |
| F | Fill tool |
| E | Eraser |

## License

Same as GAIME (open source). Uses only permissively-licensed dependencies:
- OpenCV (Apache 2.0)
- NumPy (BSD)
- Pillow (HPND)
- Flask (BSD)
