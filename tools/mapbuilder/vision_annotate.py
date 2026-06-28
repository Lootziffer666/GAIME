"""
Vision-LLM Annotation — Let a vision model annotate maps by looking at screenshots.

Flow:
1. Render the current map to a PNG (same pixel dimensions as the grid)
2. Send the PNG to a Vision LLM (GPT-4o / Claude Vision / local model)
3. Ask it to classify each tile region (hydrology, exits, NPCs, etc.)
4. Parse the response into layer data
5. User only has to CORRECT, not paint from scratch

The screenshot and the grid are the same size (1 pixel = 1 tile), so the LLM's
spatial reasoning maps directly to grid coordinates.

Supports multiple backends:
- OpenAI GPT-4o (OPENAI_API_KEY)
- Anthropic Claude (ANTHROPIC_API_KEY)
- Ollama local (no key needed, requires llava/bakllava)
"""

import os
import json
import base64
import re
from pathlib import Path
from typing import Optional

import cv2
import numpy as np


# --- Prompt Templates ---

HYDROLOGY_PROMPT = """You are analyzing a top-down 2D game map screenshot. The image is a {width}x{height} grid where each cell is one tile ({cell_size}x{cell_size} pixels).

Look at the map and determine the HYDROLOGY (water flow) for each region:

Tile types to assign:
- "sheltered" = under a roof/canopy/overhang (rain can't reach here)
- "exposed" = open sky (rain falls directly)
- "puddle_zone" = low point where water collects (depressions, courtyards)
- "drain" = where water flows away (near edges, gutters, cracks)
- "flow_south" / "flow_north" / "flow_east" / "flow_west" = water flows in this direction
- "slope_low" = lowest elevation (water pools here)
- "slope_high" = highest elevation (water runs off)
- "none" = not applicable (walls, solid objects)

Respond with a JSON grid (2D array of strings), {height} rows x {width} columns.
Only use the exact labels listed above. Respond ONLY with the JSON array, no other text.

Context about this map:
- Dark/covered areas = sheltered
- Open grass/ground areas = exposed  
- Areas near water bodies = drain
- Center of open areas = puddle_zone (water collects in low spots)
- Near walls/buildings = sheltered (overhang protection)
"""

FULL_ANNOTATION_PROMPT = """You are analyzing a top-down 2D game map screenshot. The image is a {width}x{height} grid where each cell is one tile.

Analyze the map and provide annotations for ALL layers. Return a JSON object with these keys:

1. "weather" - Hydrology layer ({height} rows x {width} cols):
   Values: "sheltered", "exposed", "puddle_zone", "drain", "flow_south", "flow_north", "flow_east", "flow_west", "slope_low", "slope_high", "none"

2. "exits" - Entry/exit points ({height} rows x {width} cols):
   Values: "spawn" (one central walkable spot), "exit_north", "exit_south", "exit_east", "exit_west", "none"
   Place exits at walkable edges, spawn near center.

3. "decorative" - Decorations you can identify ({height} rows x {width} cols):
   Values: "tree", "rock", "flower", "torch", "chest", "sign", "none"

4. "npc_routes" - Good NPC patrol/idle spots ({height} rows x {width} cols):
   Values: "npc_spawn", "npc_patrol_a", "npc_idle_spot", "none"
   Place NPCs on walkable floor, not in walls/water.

Return ONLY valid JSON. The outer object has 4 keys, each containing a 2D array.
"""


def render_grid_to_image(layers: dict, cell_size: int = 1) -> np.ndarray:
    """
    Render the ground layer to a tiny image where 1 pixel = 1 tile.
    This is what the Vision LLM will analyze.
    """
    ground = layers.get("ground", [])
    if not ground:
        return np.zeros((24, 32, 3), dtype=np.uint8)
    
    height = len(ground)
    width = len(ground[0])
    
    COLORS_BGR = {
        "floor": (140, 180, 200),
        "wall": (60, 60, 60),
        "water": (180, 120, 40),
        "grass": (80, 180, 80),
        "stone": (180, 180, 180),
        "door": (60, 60, 200),
        "empty": (0, 0, 0),
    }
    
    img = np.zeros((height * cell_size, width * cell_size, 3), dtype=np.uint8)
    for y, row in enumerate(ground):
        for x, label in enumerate(row):
            color = COLORS_BGR.get(label, (128, 128, 128))
            if cell_size == 1:
                img[y, x] = color
            else:
                img[y*cell_size:(y+1)*cell_size, x*cell_size:(x+1)*cell_size] = color
    
    return img


def annotate_with_vision(
    layers: dict,
    mode: str = "hydrology",
    backend: str = "openai",
    model: str = None,
    api_key: str = None,
    cell_size: int = 4,
) -> dict:
    """
    Send the map screenshot to a Vision LLM and get back layer annotations.
    
    Args:
        layers: Current map layers (ground must be populated)
        mode: "hydrology" (just water) or "full" (all overlay layers)
        backend: "openai", "anthropic", or "ollama"
        model: Model name (default: gpt-4o / claude-3-5-sonnet / llava)
        api_key: API key (reads from env if None)
        cell_size: Pixels per tile in the screenshot (bigger = easier for LLM to see)
    
    Returns:
        Dict of layer annotations to merge into the editor
    """
    ground = layers.get("ground", [])
    height = len(ground)
    width = len(ground[0]) if ground else 0
    
    # Render map to image
    img = render_grid_to_image(layers, cell_size=cell_size)
    _, png_buf = cv2.imencode(".png", img)
    img_b64 = base64.b64encode(png_buf).decode("utf-8")
    
    # Choose prompt
    if mode == "full":
        prompt = FULL_ANNOTATION_PROMPT.format(width=width, height=height, cell_size=cell_size)
    else:
        prompt = HYDROLOGY_PROMPT.format(width=width, height=height, cell_size=cell_size)
    
    # Call the LLM
    if backend == "openai":
        response_text = _call_openai(prompt, img_b64, model or "gpt-4o", api_key)
    elif backend == "anthropic":
        response_text = _call_anthropic(prompt, img_b64, model or "claude-sonnet-4-20250514", api_key)
    elif backend == "ollama":
        response_text = _call_ollama(prompt, img_b64, model or "llava")
    else:
        raise ValueError(f"Unknown backend: {backend}")
    
    # Parse response
    return _parse_response(response_text, mode, width, height)


def _call_openai(prompt: str, img_b64: str, model: str, api_key: str = None) -> str:
    """Call OpenAI Vision API."""
    import urllib.request
    
    key = api_key or os.environ.get("OPENAI_API_KEY")
    if not key:
        raise ValueError("Set OPENAI_API_KEY environment variable")
    
    payload = {
        "model": model,
        "messages": [{
            "role": "user",
            "content": [
                {"type": "text", "text": prompt},
                {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{img_b64}"}},
            ],
        }],
        "max_tokens": 16000,
    }
    
    req = urllib.request.Request(
        "https://api.openai.com/v1/chat/completions",
        data=json.dumps(payload).encode(),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
    )
    
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = json.loads(resp.read())
    
    return data["choices"][0]["message"]["content"]


def _call_anthropic(prompt: str, img_b64: str, model: str, api_key: str = None) -> str:
    """Call Anthropic Claude Vision API."""
    import urllib.request
    
    key = api_key or os.environ.get("ANTHROPIC_API_KEY")
    if not key:
        raise ValueError("Set ANTHROPIC_API_KEY environment variable")
    
    payload = {
        "model": model,
        "max_tokens": 16000,
        "messages": [{
            "role": "user",
            "content": [
                {"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": img_b64}},
                {"type": "text", "text": prompt},
            ],
        }],
    }
    
    req = urllib.request.Request(
        "https://api.anthropic.com/v1/messages",
        data=json.dumps(payload).encode(),
        headers={
            "x-api-key": key,
            "Content-Type": "application/json",
            "anthropic-version": "2023-06-01",
        },
    )
    
    with urllib.request.urlopen(req, timeout=120) as resp:
        data = json.loads(resp.read())
    
    return data["content"][0]["text"]


def _call_ollama(prompt: str, img_b64: str, model: str) -> str:
    """Call local Ollama with a vision model (llava, bakllava, etc.)."""
    import urllib.request
    
    payload = {
        "model": model,
        "prompt": prompt,
        "images": [img_b64],
        "stream": False,
    }
    
    req = urllib.request.Request(
        "http://localhost:11434/api/generate",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"},
    )
    
    with urllib.request.urlopen(req, timeout=300) as resp:
        data = json.loads(resp.read())
    
    return data["response"]


def _parse_response(text: str, mode: str, width: int, height: int) -> dict:
    """Parse the LLM's JSON response into layer dicts."""
    # Extract JSON from response (LLM might wrap it in markdown code blocks)
    json_match = re.search(r'[\[\{].*[\]\}]', text, re.DOTALL)
    if not json_match:
        raise ValueError(f"Could not find JSON in LLM response:\n{text[:500]}")
    
    raw = json_match.group(0)
    data = json.loads(raw)
    
    result = {}
    
    if mode == "hydrology":
        # Response is a 2D array directly
        if isinstance(data, list):
            result["weather"] = _validate_grid(data, width, height, "none")
    else:
        # Response is a dict with layer keys
        if isinstance(data, dict):
            for key in ["weather", "exits", "npc_routes", "decorative"]:
                if key in data:
                    result[key] = _validate_grid(data[key], width, height, "none")
    
    return result


def _validate_grid(grid: list, width: int, height: int, default: str) -> list:
    """Ensure grid has correct dimensions, pad/clip as needed."""
    result = []
    for y in range(height):
        if y < len(grid) and isinstance(grid[y], list):
            row = []
            for x in range(width):
                if x < len(grid[y]) and isinstance(grid[y][x], str):
                    row.append(grid[y][x])
                else:
                    row.append(default)
            result.append(row)
        else:
            result.append([default] * width)
    return result
